package com.traceforge.analysis.domain;

import java.util.List;

public record StackTraceAnalysis(
        long parsedFrameCount,
        long ignoredLineCount,
        long matchedFrameCount,
        long exactMethodMatchCount,
        long lineMatchCount,
        long typeOnlyMatchCount,
        long unmatchedFrameCount,
        List<StackTraceFrameMatch> matches
) {

    public StackTraceAnalysis {
        matches = List.copyOf(matches);
    }

    public static StackTraceAnalysis from(
            ParsedStackTrace parsedStackTrace,
            List<StackTraceFrameMatch> matches
    ) {
        long exactMethodMatches = count(matches, FrameMatchStatus.EXACT_METHOD);
        long lineMatches = count(matches, FrameMatchStatus.LINE_MATCH);
        long typeOnlyMatches = count(matches, FrameMatchStatus.TYPE_ONLY);
        long unmatchedFrames = count(matches, FrameMatchStatus.UNMATCHED);

        return new StackTraceAnalysis(
                parsedStackTrace.frames().size(),
                parsedStackTrace.ignoredLineCount(),
                matches.size() - unmatchedFrames,
                exactMethodMatches,
                lineMatches,
                typeOnlyMatches,
                unmatchedFrames,
                matches
        );
    }

    private static long count(
            List<StackTraceFrameMatch> matches,
            FrameMatchStatus status
    ) {
        return matches.stream()
                .filter(match -> match.status() == status)
                .count();
    }
}
