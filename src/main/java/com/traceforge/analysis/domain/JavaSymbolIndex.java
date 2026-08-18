package com.traceforge.analysis.domain;

import java.util.List;

public record JavaSymbolIndex(
        long parsedFileCount,
        long failedFileCount,
        long typeCount,
        long methodCount,
        List<JavaTypeSymbol> types
) {

    public JavaSymbolIndex {
        types = List.copyOf(types);
    }

    public static JavaSymbolIndex from(
            long parsedFileCount,
            long failedFileCount,
            List<JavaTypeSymbol> types
    ) {
        long methodCount = types.stream()
                .mapToLong(type -> type.methods().size())
                .sum();

        return new JavaSymbolIndex(
                parsedFileCount,
                failedFileCount,
                types.size(),
                methodCount,
                types
        );
    }
}
