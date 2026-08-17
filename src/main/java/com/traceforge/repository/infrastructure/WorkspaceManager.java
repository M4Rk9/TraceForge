package com.traceforge.repository.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class WorkspaceManager {

    private final Path workspaceRoot;

    public WorkspaceManager(
            @Value("${traceforge.repository.workspace-root:${java.io.tmpdir}/traceforge}")
            String workspaceRoot
    ) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    public RepositoryWorkspace create() {
        try {
            Files.createDirectories(workspaceRoot);
            Path workspace = Files.createTempDirectory(workspaceRoot, "repository-");
            return new RepositoryWorkspace(workspace);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create a repository workspace.", exception);
        }
    }
}
