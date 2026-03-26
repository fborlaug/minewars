package com.frobotics.minewars;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

public final class Errors {

    private Errors() {}

    public static WebApplicationException badRequest(String message) {
        return error(Response.Status.BAD_REQUEST, message);
    }

    public static WebApplicationException conflict(String message) {
        return error(Response.Status.CONFLICT, message);
    }

    public static WebApplicationException unauthorized(String message) {
        return error(Response.Status.UNAUTHORIZED, message);
    }

    public static WebApplicationException serverError(String message) {
        return error(Response.Status.INTERNAL_SERVER_ERROR, message);
    }

    private static WebApplicationException error(Response.Status status, String message) {
        return new WebApplicationException(
                Response.status(status)
                        .entity(Map.of("error", message))
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build());
    }
}
