package com.frobotics.minewars;

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.elytron.security.common.BcryptUtil;

import java.util.Map;

@Path("/api/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    TokenService tokenService;

    @POST
    @Path("/register")
    @Transactional
    public Response register(AuthRequest raw) {
        AuthRequest request = raw.validated();
        if (Player.findByUsername(request.username()) != null) {
            throw Errors.conflict("Username taken");
        }
        try {
            Player player = new Player();
            player.username = request.username();
            player.passwordHash = BcryptUtil.bcryptHash(request.password());
            player.persist();
            Player.flush();
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("message", "Registered"))
                    .build();
        } catch (PersistenceException e) {
            Log.error("Registration failed", e);
            throw Errors.serverError("Registration failed");
        }
    }

    // Pre-hashed dummy to ensure constant-time login regardless of user existence
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("dummy");

    @POST
    @Path("/login")
    public Response login(AuthRequest raw) {
        AuthRequest request = raw.validated();
        Player player = Player.findByUsername(request.username());
        String hash = player != null ? player.passwordHash : DUMMY_HASH;
        if (!BcryptUtil.matches(request.password(), hash) || player == null) {
            throw Errors.unauthorized("Invalid credentials");
        }
        String token = tokenService.generate(player);
        return Response.ok(new AuthResponse(token, player.username)).build();
    }
}
