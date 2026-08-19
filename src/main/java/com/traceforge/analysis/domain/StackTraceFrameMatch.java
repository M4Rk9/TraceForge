package com.traceforge.analysis.domain;

public record StackTraceFrameMatch(
        StackTraceFrame frame,
        FrameMatchStatus status,
        String typeQualifiedName,
        String sourcePath,
        String methodSignature,
        Integer methodBeginLine,
        Integer methodEndLine
) {
}
