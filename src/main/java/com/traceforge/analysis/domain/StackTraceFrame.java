package com.traceforge.analysis.domain;

public record StackTraceFrame(
        int index,
        String runtimePrefix,
        String className,
        String methodName,
        String fileName,
        Integer lineNumber,
        String rawLine
) {
}
