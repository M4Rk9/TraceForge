package com.traceforge.repository.application;

import com.traceforge.analysis.infrastructure.JavaSourceIndexer;
import com.traceforge.repository.api.CloneRepositoryResponse;
import com.traceforge.repository.domain.RepositoryCloneMetadata;
import com.traceforge.repository.exception.InvalidRepositoryUrlException;
import com.traceforge.repository.infrastructure.GitHubRepositoryUrlValidator;
import com.traceforge.repository.infrastructure.JGitRepositoryClient;
import com.traceforge.repository.infrastructure.RepositoryInspector;
import com.traceforge.repository.infrastructure.WorkspaceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RepositoryCloneServiceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void clonesInspectsAndCleansWorkspace() throws Exception {
        Path workspaceRoot = temporaryDirectory.resolve("workspaces");
        JGitRepositoryClient repositoryClient = mock(JGitRepositoryClient.class);

        when(repositoryClient.cloneRepository(any(), any()))
                .thenAnswer(invocation -> {
                    Path workspace = invocation.getArgument(1);
                    Files.writeString(workspace.resolve("pom.xml"), "<project/>");

                    Path javaFile = workspace.resolve("src/main/java/example/App.java");
                    Files.createDirectories(javaFile.getParent());
                    Files.writeString(javaFile, "package example; class App {}");

                    return new RepositoryCloneMetadata("main", "a".repeat(40));
                });

        RepositoryCloneService service = new RepositoryCloneService(
                new GitHubRepositoryUrlValidator(),
                repositoryClient,
                new RepositoryInspector(),
                new JavaSourceIndexer(),
                new WorkspaceManager(workspaceRoot.toString()),
                1_000_000
        );

        CloneRepositoryResponse response = service.cloneAndInspect(
                "https://github.com/owner/repository"
        );

        assertThat(response.repositoryUrl())
                .isEqualTo("https://github.com/owner/repository");
        assertThat(response.branch()).isEqualTo("main");
        assertThat(response.commitSha()).hasSize(40);
        assertThat(response.javaFileCount()).isEqualTo(1);
        assertThat(response.buildSystem()).isEqualTo("MAVEN");
        assertThat(response.symbolIndex().parsedFileCount()).isEqualTo(1);
        assertThat(response.symbolIndex().typeCount()).isEqualTo(1);
        assertThat(response.symbolIndex().methodCount()).isZero();
        assertThat(response.symbolIndex().types())
                .extracting(type -> type.qualifiedName())
                .containsExactly("example.App");

        try (var workspaces = Files.list(workspaceRoot)) {
            assertThat(workspaces).isEmpty();
        }
    }

    @Test
    void rejectsInvalidUrlBeforeCreatingClone() {
        JGitRepositoryClient repositoryClient = mock(JGitRepositoryClient.class);

        RepositoryCloneService service = new RepositoryCloneService(
                new GitHubRepositoryUrlValidator(),
                repositoryClient,
                new RepositoryInspector(),
                new JavaSourceIndexer(),
                new WorkspaceManager(temporaryDirectory.resolve("workspaces").toString()),
                1_000_000
        );

        assertThatThrownBy(() -> service.cloneAndInspect("https://example.com/repository"))
                .isInstanceOf(InvalidRepositoryUrlException.class);

        verifyNoInteractions(repositoryClient);
    }
}
