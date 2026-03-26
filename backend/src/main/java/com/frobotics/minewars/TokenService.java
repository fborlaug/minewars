package com.frobotics.minewars;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;

@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "minewars.jwt.expiry")
    Duration expiry;

    public String generate(Player player) {
        return Jwt.issuer(issuer)
                .upn(player.username)
                .subject(String.valueOf(player.id))
                .groups(Set.of("player"))
                .expiresIn(expiry)
                .sign();
    }
}

