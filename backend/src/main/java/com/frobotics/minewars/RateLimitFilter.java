package com.frobotics.minewars;

import io.quarkus.logging.Log;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
    import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP rate limiter for auth endpoints.
 * Uses an in-memory fixed-window counter per client IP.
 * Expired buckets are evicted every {@value #WINDOW_SECONDS} seconds.
 * Designed for single-instance deployments (desiredCount: 1).
 */
@Provider
@PreMatching
public class RateLimitFilter implements ContainerRequestFilter {

    private static final int WINDOW_SECONDS = 300; // 5 minutes

    private static final Map<String, RateConfig> LIMITS = Map.of(
            "/api/auth/register", new RateConfig(100),
            "/api/auth/login", new RateConfig(50)
    );

    /** Daemon thread that evicts expired buckets — won't prevent JVM shutdown. */
    private static final ScheduledExecutorService EVICTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limit-evictor");
        t.setDaemon(true);
        return t;
    });

    static {
        EVICTOR.scheduleAtFixedRate(
                RateLimitFilter::evictExpiredBuckets,
                WINDOW_SECONDS, WINDOW_SECONDS, TimeUnit.SECONDS
        );
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        if (!"POST".equalsIgnoreCase(ctx.getMethod())) return;

        String path = ctx.getUriInfo().getAbsolutePath().getPath();
        RateConfig config = LIMITS.get(path);
        if (config == null) return;

        String ip = clientIp(ctx);
        Bucket bucket = config.buckets.compute(ip, (_, existing) -> {
            if (existing == null || existing.isExpired()) return new Bucket();
            return existing;
        });

        if (!bucket.tryConsume(config.maxRequests)) {
            Log.warnf("Rate limit exceeded: ip=%s path=%s", ip, path);
            int retryAfter = bucket.retryAfterSeconds();
            ctx.abortWith(Response.status(429)
                    .header("Retry-After", retryAfter)
                    .entity(Map.of("error", "Too many requests. Try again later."))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build());
        }
    }

    /** Extract client IP from X-Forwarded-For (CloudFront/ALB) or remote address. */
    private String clientIp(ContainerRequestContext ctx) {
        String xff = ctx.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return "unknown";
    }

    /** Remove expired buckets from all rate-limit configs to prevent unbounded memory growth. */
    private static void evictExpiredBuckets() {
        for (RateConfig config : LIMITS.values()) {
            config.buckets.entrySet().removeIf(e -> e.getValue().isExpired());
        }
    }

    private record RateConfig(int maxRequests, ConcurrentHashMap<String, Bucket> buckets) {
        RateConfig(int maxRequests) {
            this(maxRequests, new ConcurrentHashMap<>());
        }
    }

    /** Fixed-window counter bucket. */
    private static class Bucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private final long windowStart = now();

        boolean tryConsume(int max) {
            return count.incrementAndGet() <= max;
        }

        boolean isExpired() {
            return now() - windowStart >= WINDOW_SECONDS;
        }

        int retryAfterSeconds() {
            return Math.max(1, WINDOW_SECONDS - (int) (now() - windowStart));
        }

        private static long now() {
            return Instant.now().getEpochSecond();
        }
    }
}

