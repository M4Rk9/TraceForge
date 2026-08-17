package com.traceforge.repository.domain;

public record RepositoryCloneMetadata(
        String branch,
        String commitSha
) {
}
