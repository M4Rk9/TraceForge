package com.traceforge.repository.domain;

public record RepositoryFacts(
        long sizeBytes,
        long javaFileCount,
        boolean mavenProject
) {
}
