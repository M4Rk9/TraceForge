package com.traceforge.analysis.infrastructure;

import com.traceforge.analysis.domain.ParsedStackTrace;
import com.traceforge.analysis.exception.StackTraceParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JvmStackTraceParserTests {

    private final JvmStackTraceParser parser = new JvmStackTraceParser();

    @Test
    void parsesStandardModuleUnknownAndNativeFrames() {
        ParsedStackTrace parsed = parser.parse("""
                java.lang.IllegalStateException: failed
                    at com.example.OrderService.placeOrder(OrderService.java:42)
                    at java.base/java.util.Objects.requireNonNull(Objects.java:233)
                    at com.example.Generated.call(Unknown Source)
                    at com.example.NativeBridge.call(Native Method)
                Caused by: java.lang.NullPointerException
                """);

        assertThat(parsed.frames()).hasSize(4);
        assertThat(parsed.ignoredLineCount()).isEqualTo(2);

        assertThat(parsed.frames().get(0).className())
                .isEqualTo("com.example.OrderService");
        assertThat(parsed.frames().get(0).methodName()).isEqualTo("placeOrder");
        assertThat(parsed.frames().get(0).fileName()).isEqualTo("OrderService.java");
        assertThat(parsed.frames().get(0).lineNumber()).isEqualTo(42);

        assertThat(parsed.frames().get(1).runtimePrefix()).isEqualTo("java.base");
        assertThat(parsed.frames().get(2).lineNumber()).isNull();
        assertThat(parsed.frames().get(3).fileName()).isNull();
    }

    @Test
    void keepsFrameIndexesStableWhileIgnoringOtherLines() {
        ParsedStackTrace parsed = parser.parse("""
                java.lang.RuntimeException
                    at com.example.First.run(First.java:10)
                    ... 2 more
                    at com.example.Second.run(Second.java:20)
                """);

        assertThat(parsed.frames())
                .extracting(frame -> frame.index())
                .containsExactly(0, 1);
        assertThat(parsed.ignoredLineCount()).isEqualTo(2);
    }

    @Test
    void rejectsInputWithoutJvmFrames() {
        assertThatThrownBy(() -> parser.parse("java.lang.RuntimeException: failed"))
                .isInstanceOf(StackTraceParseException.class)
                .hasMessageContaining("No valid JVM");
    }

    @Test
    void rejectsBlankInput() {
        assertThatThrownBy(() -> parser.parse(" "))
                .isInstanceOf(StackTraceParseException.class)
                .hasMessageContaining("required");
    }
}
