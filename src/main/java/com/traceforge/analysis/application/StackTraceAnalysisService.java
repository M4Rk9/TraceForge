package com.traceforge.analysis.application;

import com.traceforge.analysis.api.StackTraceAnalysisResponse;
import com.traceforge.analysis.domain.ParsedStackTrace;
import com.traceforge.analysis.domain.StackTraceAnalysis;
import com.traceforge.analysis.infrastructure.JvmStackTraceParser;
import com.traceforge.repository.api.CloneRepositoryResponse;
import com.traceforge.repository.application.RepositoryCloneService;
import org.springframework.stereotype.Service;

@Service
public class StackTraceAnalysisService {

    private final JvmStackTraceParser stackTraceParser;
    private final RepositoryCloneService repositoryCloneService;
    private final StackTraceSymbolMapper stackTraceSymbolMapper;

    public StackTraceAnalysisService(
            JvmStackTraceParser stackTraceParser,
            RepositoryCloneService repositoryCloneService,
            StackTraceSymbolMapper stackTraceSymbolMapper
    ) {
        this.stackTraceParser = stackTraceParser;
        this.repositoryCloneService = repositoryCloneService;
        this.stackTraceSymbolMapper = stackTraceSymbolMapper;
    }

    public StackTraceAnalysisResponse analyze(
            String repositoryUrl,
            String stackTrace
    ) {
        ParsedStackTrace parsedStackTrace = stackTraceParser.parse(stackTrace);
        CloneRepositoryResponse repository =
                repositoryCloneService.cloneAndInspect(repositoryUrl);
        StackTraceAnalysis analysis = stackTraceSymbolMapper.map(
                parsedStackTrace,
                repository.symbolIndex()
        );

        return new StackTraceAnalysisResponse(
                repository.repositoryUrl(),
                repository.branch(),
                repository.commitSha(),
                repository.symbolIndex().typeCount(),
                repository.symbolIndex().methodCount(),
                analysis
        );
    }
}
