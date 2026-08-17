package com.traceforge.repository.api;

public record CloneRepositoryResponse(
        String repositoryUrl,
        String branch,
        String commitSha,
        long repositorySizeBytes,
        long javaFileCount,
        String buildSystem
) {
}
