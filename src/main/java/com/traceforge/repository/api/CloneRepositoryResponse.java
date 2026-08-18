package com.traceforge.repository.api;

import com.traceforge.analysis.domain.JavaSymbolIndex;

public record CloneRepositoryResponse(
        String repositoryUrl,
        String branch,
        String commitSha,
        long repositorySizeBytes,
        long javaFileCount,
        String buildSystem,
        JavaSymbolIndex symbolIndex
) {
}
