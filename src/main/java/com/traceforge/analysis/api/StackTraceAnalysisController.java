package com.traceforge.analysis.api;

import com.traceforge.analysis.application.StackTraceAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class StackTraceAnalysisController {

    private final StackTraceAnalysisService stackTraceAnalysisService;

    public StackTraceAnalysisController(
            StackTraceAnalysisService stackTraceAnalysisService
    ) {
        this.stackTraceAnalysisService = stackTraceAnalysisService;
    }

    @PostMapping("/stack-trace")
    public ResponseEntity<StackTraceAnalysisResponse> analyzeStackTrace(
            @Valid @RequestBody StackTraceAnalysisRequest request
    ) {
        return ResponseEntity.ok(
                stackTraceAnalysisService.analyze(
                        request.repositoryUrl(),
                        request.stackTrace()
                )
        );
    }
}
