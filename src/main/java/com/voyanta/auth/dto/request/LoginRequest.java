package com.voyanta.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email(message = "Düzgün e-poçt daxil edin")
        String email,

        @NotBlank
        String password
) {}