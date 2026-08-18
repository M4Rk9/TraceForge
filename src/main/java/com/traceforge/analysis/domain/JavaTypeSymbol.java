package com.traceforge.analysis.domain;

import java.util.List;

public record JavaTypeSymbol(
        String qualifiedName,
        String kind,
        String sourcePath,
        int beginLine,
        int endLine,
        List<JavaMethodSymbol> methods
) {

    public JavaTypeSymbol {
        methods = List.copyOf(methods);
    }
}
