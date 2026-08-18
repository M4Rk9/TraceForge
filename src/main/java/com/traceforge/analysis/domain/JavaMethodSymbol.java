package com.traceforge.analysis.domain;

public record JavaMethodSymbol(
        String name,
        String signature,
        String returnType,
        int beginLine,
        int endLine
) {
}
