package com.traceforge.analysis.api;

import com.traceforge.analysis.domain.StackTraceAnalysis;

public record StackTraceAnalysisResponse(
        String repositoryUrl,
        String branch,
        String commitSha,
        long indexedTypeCount,
        long indexedMethodCount,
        StackTraceAnalysis analysis
) {
}
