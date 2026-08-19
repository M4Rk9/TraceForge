package com.traceforge.analysis.infrastructure;

import com.traceforge.analysis.domain.ParsedStackTrace;
import com.traceforge.analysis.domain.StackTraceFrame;
import com.traceforge.analysis.exception.StackTraceParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JvmStackTraceParser {

    private static final int MAXIMUM_FRAME_COUNT = 512;
    private static final Pattern FRAME_PATTERN = Pattern.compile(
            "^\\s*at\\s+(?<callable>[^\\s(]+)\\((?<location>[^)]*)\\)\\s*$"
    );
    private static final Pattern LINE_LOCATION_PATTERN = Pattern.compile(
            "^(?<file>.+):(?<line>\\d+)$"
    );

    public ParsedStackTrace parse(String stackTrace) {
        if (stackTrace == null || stackTrace.isBlank()) {
            throw new StackTraceParseException("Stack trace is required.");
        }

        List<StackTraceFrame> frames = new ArrayList<>();
        long ignoredLineCount = 0;

        for (String line : stackTrace.split("\\R", -1)) {
            if (line.isBlank()) {
                continue;
            }

            StackTraceFrame frame = parseFrame(frames.size(), line);
            if (frame == null) {
                ignoredLineCount++;
                continue;
            }

            frames.add(frame);
            if (frames.size() > MAXIMUM_FRAME_COUNT) {
                throw new StackTraceParseException(
                        "Stack trace exceeds the maximum of "
                                + MAXIMUM_FRAME_COUNT + " frames."
                );
            }
        }

        if (frames.isEmpty()) {
            throw new StackTraceParseException(
                    "No valid JVM stack-trace frames were found."
            );
        }

        return new ParsedStackTrace(frames, ignoredLineCount);
    }

    private StackTraceFrame parseFrame(int index, String line) {
        Matcher matcher = FRAME_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        String callable = matcher.group("callable");
        int methodSeparator = callable.lastIndexOf('.');
        if (methodSeparator < 1 || methodSeparator == callable.length() - 1) {
            return null;
        }

        String classToken = callable.substring(0, methodSeparator);
        String methodName = callable.substring(methodSeparator + 1);

        int runtimeSeparator = classToken.lastIndexOf('/');
        String runtimePrefix = runtimeSeparator >= 0
                ? classToken.substring(0, runtimeSeparator)
                : null;
        String className = runtimeSeparator >= 0
                ? classToken.substring(runtimeSeparator + 1)
                : classToken;

        if (className.isBlank() || methodName.isBlank()) {
            return null;
        }

        SourceLocation location = parseLocation(matcher.group("location"));

        return new StackTraceFrame(
                index,
                runtimePrefix,
                className,
                methodName,
                location.fileName(),
                location.lineNumber(),
                line.strip()
        );
    }

    private SourceLocation parseLocation(String location) {
        if ("Native Method".equals(location) || "Unknown Source".equals(location)) {
            return new SourceLocation(null, null);
        }

        Matcher matcher = LINE_LOCATION_PATTERN.matcher(location);
        if (matcher.matches()) {
            return new SourceLocation(
                    matcher.group("file"),
                    Integer.parseInt(matcher.group("line"))
            );
        }

        return new SourceLocation(location.isBlank() ? null : location, null);
    }

    private record SourceLocation(String fileName, Integer lineNumber) {
    }
}
