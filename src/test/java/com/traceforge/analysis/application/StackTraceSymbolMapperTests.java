package com.traceforge.analysis.application;

import com.traceforge.analysis.domain.FrameMatchStatus;
import com.traceforge.analysis.domain.JavaMethodSymbol;
import com.traceforge.analysis.domain.JavaSymbolIndex;
import com.traceforge.analysis.domain.JavaTypeSymbol;
import com.traceforge.analysis.domain.ParsedStackTrace;
import com.traceforge.analysis.domain.StackTraceAnalysis;
import com.traceforge.analysis.infrastructure.JvmStackTraceParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StackTraceSymbolMapperTests {

    private final JvmStackTraceParser parser = new JvmStackTraceParser();
    private final StackTraceSymbolMapper mapper = new StackTraceSymbolMapper();

    @Test
    void mapsNestedClassesAndDisambiguatesOverloadedMethodsByLine() {
        JavaSymbolIndex index = index();

        ParsedStackTrace trace = parser.parse("""
                java.lang.IllegalStateException
                    at com.example.OrderService.placeOrder(OrderService.java:60)
                    at com.example.Outer$Inner.run(Outer.java:15)
                    at java.base/java.util.Objects.requireNonNull(Objects.java:233)
                """);

        StackTraceAnalysis analysis = mapper.map(trace, index);

        assertThat(analysis.parsedFrameCount()).isEqualTo(3);
        assertThat(analysis.exactMethodMatchCount()).isEqualTo(2);
        assertThat(analysis.unmatchedFrameCount()).isEqualTo(1);

        assertThat(analysis.matches().get(0).status())
                .isEqualTo(FrameMatchStatus.EXACT_METHOD);
        assertThat(analysis.matches().get(0).methodSignature())
                .isEqualTo("placeOrder(int)");
        assertThat(analysis.matches().get(1).typeQualifiedName())
                .isEqualTo("com.example.Outer.Inner");
        assertThat(analysis.matches().get(2).status())
                .isEqualTo(FrameMatchStatus.UNMATCHED);
    }

    @Test
    void usesSourceLineForSyntheticRuntimeMethod() {
        ParsedStackTrace trace = parser.parse(
                "at com.example.OrderService.lambda$placeOrder$0(OrderService.java:40)"
        );

        StackTraceAnalysis analysis = mapper.map(trace, index());

        assertThat(analysis.lineMatchCount()).isEqualTo(1);
        assertThat(analysis.matches().getFirst().status())
                .isEqualTo(FrameMatchStatus.LINE_MATCH);
        assertThat(analysis.matches().getFirst().methodSignature())
                .isEqualTo("placeOrder(String)");
    }

    @Test
    void returnsTypeOnlyWhenMethodCannotBeSelectedSafely() {
        ParsedStackTrace trace = parser.parse(
                "at com.example.OrderService.missing(OrderService.java:90)"
        );

        StackTraceAnalysis analysis = mapper.map(trace, index());

        assertThat(analysis.typeOnlyMatchCount()).isEqualTo(1);
        assertThat(analysis.matches().getFirst().status())
                .isEqualTo(FrameMatchStatus.TYPE_ONLY);
        assertThat(analysis.matches().getFirst().sourcePath())
                .isEqualTo("src/main/java/com/example/OrderService.java");
    }

    private JavaSymbolIndex index() {
        JavaTypeSymbol orderService = new JavaTypeSymbol(
                "com.example.OrderService",
                "CLASS",
                "src/main/java/com/example/OrderService.java",
                10,
                100,
                List.of(
                        new JavaMethodSymbol(
                                "placeOrder",
                                "placeOrder(String)",
                                "void",
                                35,
                                50
                        ),
                        new JavaMethodSymbol(
                                "placeOrder",
                                "placeOrder(int)",
                                "void",
                                55,
                                70
                        )
                )
        );

        JavaTypeSymbol nested = new JavaTypeSymbol(
                "com.example.Outer.Inner",
                "CLASS",
                "src/main/java/com/example/Outer.java",
                10,
                25,
                List.of(
                        new JavaMethodSymbol("run", "run()", "void", 12, 20)
                )
        );

        return JavaSymbolIndex.from(2, 0, List.of(orderService, nested));
    }
}
