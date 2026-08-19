package com.traceforge.analysis.application;

import com.traceforge.analysis.domain.FrameMatchStatus;
import com.traceforge.analysis.domain.JavaMethodSymbol;
import com.traceforge.analysis.domain.JavaSymbolIndex;
import com.traceforge.analysis.domain.JavaTypeSymbol;
import com.traceforge.analysis.domain.ParsedStackTrace;
import com.traceforge.analysis.domain.StackTraceAnalysis;
import com.traceforge.analysis.domain.StackTraceFrame;
import com.traceforge.analysis.domain.StackTraceFrameMatch;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StackTraceSymbolMapper {

    public StackTraceAnalysis map(
            ParsedStackTrace parsedStackTrace,
            JavaSymbolIndex symbolIndex
    ) {
        Map<String, JavaTypeSymbol> typesByQualifiedName = symbolIndex.types()
                .stream()
                .collect(Collectors.toMap(
                        JavaTypeSymbol::qualifiedName,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        List<StackTraceFrameMatch> matches = parsedStackTrace.frames()
                .stream()
                .map(frame -> matchFrame(frame, symbolIndex, typesByQualifiedName))
                .toList();

        return StackTraceAnalysis.from(parsedStackTrace, matches);
    }

    private StackTraceFrameMatch matchFrame(
            StackTraceFrame frame,
            JavaSymbolIndex symbolIndex,
            Map<String, JavaTypeSymbol> typesByQualifiedName
    ) {
        String normalizedClassName = normalizeClassName(frame.className());
        JavaTypeSymbol type = typesByQualifiedName.get(normalizedClassName);

        if (type == null) {
            type = uniqueSourceFileMatch(frame, symbolIndex);
        }

        if (type == null) {
            return unmatched(frame);
        }

        List<JavaMethodSymbol> namedMethods = type.methods()
                .stream()
                .filter(method -> method.name().equals(frame.methodName()))
                .toList();

        if (frame.lineNumber() != null) {
            List<JavaMethodSymbol> namedLineMatches = namedMethods.stream()
                    .filter(method -> containsLine(method, frame.lineNumber()))
                    .toList();

            if (namedLineMatches.size() == 1) {
                return methodMatch(
                        frame,
                        type,
                        namedLineMatches.getFirst(),
                        FrameMatchStatus.EXACT_METHOD
                );
            }

            List<JavaMethodSymbol> lineMatches = type.methods()
                    .stream()
                    .filter(method -> containsLine(method, frame.lineNumber()))
                    .toList();

            if (lineMatches.size() == 1) {
                return methodMatch(
                        frame,
                        type,
                        lineMatches.getFirst(),
                        FrameMatchStatus.LINE_MATCH
                );
            }
        } else if (namedMethods.size() == 1) {
            return methodMatch(
                    frame,
                    type,
                    namedMethods.getFirst(),
                    FrameMatchStatus.EXACT_METHOD
            );
        }

        return new StackTraceFrameMatch(
                frame,
                FrameMatchStatus.TYPE_ONLY,
                type.qualifiedName(),
                type.sourcePath(),
                null,
                null,
                null
        );
    }

    private JavaTypeSymbol uniqueSourceFileMatch(
            StackTraceFrame frame,
            JavaSymbolIndex symbolIndex
    ) {
        if (frame.fileName() == null) {
            return null;
        }

        List<JavaTypeSymbol> candidates = symbolIndex.types()
                .stream()
                .filter(type -> sourceFileName(type).equals(frame.fileName()))
                .toList();

        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private String sourceFileName(JavaTypeSymbol type) {
        int separator = type.sourcePath().lastIndexOf('/');
        return separator >= 0
                ? type.sourcePath().substring(separator + 1)
                : type.sourcePath();
    }

    private String normalizeClassName(String className) {
        String normalized = className;

        int generatedClassMarker = normalized.indexOf("$$");
        if (generatedClassMarker > 0) {
            normalized = normalized.substring(0, generatedClassMarker);
        }

        normalized = normalized.replaceFirst("\\$\\d+.*$", "");
        return normalized.replace('$', '.');
    }

    private boolean containsLine(JavaMethodSymbol method, int lineNumber) {
        return method.beginLine() <= lineNumber && lineNumber <= method.endLine();
    }

    private StackTraceFrameMatch methodMatch(
            StackTraceFrame frame,
            JavaTypeSymbol type,
            JavaMethodSymbol method,
            FrameMatchStatus status
    ) {
        return new StackTraceFrameMatch(
                frame,
                status,
                type.qualifiedName(),
                type.sourcePath(),
                method.signature(),
                method.beginLine(),
                method.endLine()
        );
    }

    private StackTraceFrameMatch unmatched(StackTraceFrame frame) {
        return new StackTraceFrameMatch(
                frame,
                FrameMatchStatus.UNMATCHED,
                null,
                null,
                null,
                null,
                null
        );
    }
}
