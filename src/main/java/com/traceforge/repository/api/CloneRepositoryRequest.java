package com.traceforge.repository.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloneRepositoryRequest(
        @NotBlank(message = "repositoryUrl is required")
        @Size(max = 512, message = "repositoryUrl must not exceed 512 characters")
        String repositoryUrl
) {
}
