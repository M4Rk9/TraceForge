package com.traceforge.analysis.application;

import com.traceforge.analysis.api.StackTraceAnalysisResponse;
import com.traceforge.analysis.domain.FrameMatchStatus;
import com.traceforge.analysis.domain.JavaMethodSymbol;
import com.traceforge.analysis.domain.JavaSymbolIndex;
import com.traceforge.analysis.domain.JavaTypeSymbol;
import com.traceforge.analysis.exception.StackTraceParseException;
import com.traceforge.analysis.infrastructure.JvmStackTraceParser;
import com.traceforge.repository.api.CloneRepositoryResponse;
import com.traceforge.repository.application.RepositoryCloneService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StackTraceAnalysisServiceTests {

    @Test
    void returnsRepositoryIdentityAndMappedFrames() {
        RepositoryCloneService repositoryCloneService =
                mock(RepositoryCloneService.class);
        JavaSymbolIndex index = JavaSymbolIndex.from(
                1,
                0,
                List.of(new JavaTypeSymbol(
                        "com.example.Service",
                        "CLASS",
                        "src/main/java/com/example/Service.java",
                        1,
                        20,
                        List.of(new JavaMethodSymbol(
                                "run",
                                "run()",
                                "void",
                                5,
                                10
                        ))
                ))
        );

        when(repositoryCloneService.cloneAndInspect(
                "https://github.com/owner/repository"
        )).thenReturn(new CloneRepositoryResponse(
                "https://github.com/owner/repository",
                "main",
                "a".repeat(40),
                100,
                1,
                "MAVEN",
                index
        ));

        StackTraceAnalysisService service = new StackTraceAnalysisService(
                new JvmStackTraceParser(),
                repositoryCloneService,
                new StackTraceSymbolMapper()
        );

        StackTraceAnalysisResponse response = service.analyze(
                "https://github.com/owner/repository",
                "at com.example.Service.run(Service.java:7)"
        );

        assertThat(response.repositoryUrl())
                .isEqualTo("https://github.com/owner/repository");
        assertThat(response.commitSha()).hasSize(40);
        assertThat(response.indexedTypeCount()).isEqualTo(1);
        assertThat(response.analysis().matches().getFirst().status())
                .isEqualTo(FrameMatchStatus.EXACT_METHOD);
    }

    @Test
    void rejectsMalformedStackTraceBeforeCloningRepository() {
        RepositoryCloneService repositoryCloneService =
                mock(RepositoryCloneService.class);
        StackTraceAnalysisService service = new StackTraceAnalysisService(
                new JvmStackTraceParser(),
                repositoryCloneService,
                new StackTraceSymbolMapper()
        );

        assertThatThrownBy(() -> service.analyze(
                "https://github.com/owner/repository",
                "not a JVM stack trace"
        )).isInstanceOf(StackTraceParseException.class);

        verifyNoInteractions(repositoryCloneService);
    }
}
