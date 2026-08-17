package com.traceforge.repository.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryWorkspaceTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void removesWorkspaceRecursivelyWhenClosed() throws Exception {
        Path workspacePath = temporaryDirectory.resolve("workspace");
        Path nestedFile = workspacePath.resolve("nested/file.txt");
        Files.createDirectories(nestedFile.getParent());
        Files.writeString(nestedFile, "temporary");

        RepositoryWorkspace workspace = new RepositoryWorkspace(workspacePath);
        workspace.close();

        assertThat(workspacePath).doesNotExist();
    }
}
