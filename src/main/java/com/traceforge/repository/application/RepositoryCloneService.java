package com.traceforge.repository.application;

import com.traceforge.analysis.domain.JavaSymbolIndex;
import com.traceforge.analysis.infrastructure.JavaSourceIndexer;
import com.traceforge.repository.api.CloneRepositoryResponse;
import com.traceforge.repository.domain.GitHubRepository;
import com.traceforge.repository.domain.RepositoryCloneMetadata;
import com.traceforge.repository.domain.RepositoryFacts;
import com.traceforge.repository.exception.UnsupportedRepositoryException;
import com.traceforge.repository.infrastructure.GitHubRepositoryUrlValidator;
import com.traceforge.repository.infrastructure.JGitRepositoryClient;
import com.traceforge.repository.infrastructure.RepositoryInspector;
import com.traceforge.repository.infrastructure.RepositoryWorkspace;
import com.traceforge.repository.infrastructure.WorkspaceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepositoryCloneService {

    private final GitHubRepositoryUrlValidator repositoryUrlValidator;
    private final JGitRepositoryClient repositoryClient;
    private final RepositoryInspector repositoryInspector;
    private final JavaSourceIndexer javaSourceIndexer;
    private final WorkspaceManager workspaceManager;
    private final long maximumRepositorySizeBytes;

    public RepositoryCloneService(
            GitHubRepositoryUrlValidator repositoryUrlValidator,
            JGitRepositoryClient repositoryClient,
            RepositoryInspector repositoryInspector,
            JavaSourceIndexer javaSourceIndexer,
            WorkspaceManager workspaceManager,
            @Value("${traceforge.repository.max-size-bytes:104857600}")
            long maximumRepositorySizeBytes
    ) {
        if (maximumRepositorySizeBytes < 1) {
            throw new IllegalArgumentException("Maximum repository size must be positive.");
        }

        this.repositoryUrlValidator = repositoryUrlValidator;
        this.repositoryClient = repositoryClient;
        this.repositoryInspector = repositoryInspector;
        this.javaSourceIndexer = javaSourceIndexer;
        this.workspaceManager = workspaceManager;
        this.maximumRepositorySizeBytes = maximumRepositorySizeBytes;
    }

    public CloneRepositoryResponse cloneAndInspect(String rawRepositoryUrl) {
        GitHubRepository repository =
                repositoryUrlValidator.validateAndNormalize(rawRepositoryUrl);

        try (RepositoryWorkspace workspace = workspaceManager.create()) {
            RepositoryCloneMetadata cloneMetadata = repositoryClient.cloneRepository(
                    repository.cloneUri(),
                    workspace.path()
            );

            RepositoryFacts facts = repositoryInspector.inspect(
                    workspace.path(),
                    maximumRepositorySizeBytes
            );

            validateSupportedProject(facts);
            JavaSymbolIndex symbolIndex = javaSourceIndexer.index(workspace.path());

            return new CloneRepositoryResponse(
                    repository.webUri().toString(),
                    cloneMetadata.branch(),
                    cloneMetadata.commitSha(),
                    facts.sizeBytes(),
                    facts.javaFileCount(),
                    "MAVEN",
                    symbolIndex
            );
        }
    }

    private void validateSupportedProject(RepositoryFacts facts) {
        if (!facts.mavenProject()) {
            throw new UnsupportedRepositoryException(
                    "The first TraceForge release supports Maven repositories with a root pom.xml."
            );
        }

        if (facts.javaFileCount() == 0) {
            throw new UnsupportedRepositoryException(
                    "The repository does not contain Java source files."
            );
        }
    }
}
