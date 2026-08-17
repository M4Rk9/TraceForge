package com.traceforge.repository.infrastructure;

import com.traceforge.repository.domain.RepositoryCloneMetadata;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JGitRepositoryClientTests {

    @TempDir
    Path temporaryDirectory;

    @Test
    void clonesRepositoryWithoutNetworkAccess() throws Exception {
        Path source = temporaryDirectory.resolve("source");
        RevCommit sourceCommit;

        try (Git git = Git.init().setDirectory(source.toFile()).call()) {
            Files.writeString(source.resolve("pom.xml"), "<project/>");
            Path javaFile = source.resolve("src/main/java/example/App.java");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, "package example; class App {}");

            git.add().addFilepattern(".").call();
            sourceCommit = git.commit()
                    .setMessage("Initial test commit")
                    .setAuthor("TraceForge Tests", "tests@traceforge.local")
                    .setCommitter("TraceForge Tests", "tests@traceforge.local")
                    .call();
        }

        Path target = temporaryDirectory.resolve("clone");
        JGitRepositoryClient client = new JGitRepositoryClient(10);

        RepositoryCloneMetadata metadata =
                client.cloneRepository(source.toUri(), target);

        assertThat(metadata.commitSha()).isEqualTo(sourceCommit.getName());
        assertThat(metadata.branch()).isNotBlank();
        assertThat(target.resolve("pom.xml")).isRegularFile();
        assertThat(target.resolve("src/main/java/example/App.java")).isRegularFile();
    }
}
