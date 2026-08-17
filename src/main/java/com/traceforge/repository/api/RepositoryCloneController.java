package com.traceforge.repository.api;

import com.traceforge.repository.application.RepositoryCloneService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryCloneController {

    private final RepositoryCloneService repositoryCloneService;

    public RepositoryCloneController(RepositoryCloneService repositoryCloneService) {
        this.repositoryCloneService = repositoryCloneService;
    }

    @PostMapping("/clone")
    public ResponseEntity<CloneRepositoryResponse> cloneRepository(
            @Valid @RequestBody CloneRepositoryRequest request
    ) {
        return ResponseEntity.ok(
                repositoryCloneService.cloneAndInspect(request.repositoryUrl())
        );
    }
}
