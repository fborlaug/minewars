package com.frobotics.minewars;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AuthRequest(String username, String password) {

    /**
     * Validates and returns a copy with a trimmed username.
     * Throws a 400 WebApplicationException if invalid.
     */
    public AuthRequest validated() {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw Errors.badRequest("Username and password required");
        }
        String trimmedUsername = username.trim();
        if (trimmedUsername.length() < 3 || trimmedUsername.length() > 32) {
            throw Errors.badRequest("Username must be 3–32 characters");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw Errors.badRequest("Password must be 8–72 characters");
        }
        return new AuthRequest(trimmedUsername, password);
    }
}
