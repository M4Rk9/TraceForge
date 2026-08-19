package com.traceforge.analysis.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StackTraceAnalysisRequest(
        @NotBlank(message = "repositoryUrl is required")
        @Size(max = 512, message = "repositoryUrl must not exceed 512 characters")
        String repositoryUrl,

        @NotBlank(message = "stackTrace is required")
        @Size(max = 100_000, message = "stackTrace must not exceed 100,000 characters")
        String stackTrace
) {
}
