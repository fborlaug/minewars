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
import org.mindrot.jbcrypt.BCrypt;

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
            player.passwordHash = BCrypt.hashpw(request.password(), BCrypt.gensalt());
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

    @POST
    @Path("/login")
    public Response login(AuthRequest raw) {
        AuthRequest request = raw.validated();
        Player player = Player.findByUsername(request.username());
        if (player == null || !BCrypt.checkpw(request.password(), player.passwordHash)) {
            throw Errors.unauthorized("Invalid credentials");
        }
        String token = tokenService.generate(player);
        return Response.ok(new AuthResponse(token, player.username)).build();
    }
}
