package com.traceforge.analysis.domain;

import java.util.List;

public record ParsedStackTrace(
        List<StackTraceFrame> frames,
        long ignoredLineCount
) {

    public ParsedStackTrace {
        frames = List.copyOf(frames);
    }
}
