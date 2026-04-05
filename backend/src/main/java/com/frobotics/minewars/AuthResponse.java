package com.frobotics.minewars;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AuthResponse(String token, String username) {}
