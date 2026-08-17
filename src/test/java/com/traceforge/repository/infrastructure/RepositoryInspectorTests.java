package com.traceforge.repository.infrastructure;

import com.traceforge.repository.domain.RepositoryFacts;
import com.traceforge.repository.exception.RepositoryTooLargeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryInspectorTests {

    @TempDir
    Path repository;

    private final RepositoryInspector inspector = new RepositoryInspector();

    @Test
    void detectsMavenProjectAndCountsJavaFiles() throws Exception {
        Files.writeString(repository.resolve("pom.xml"), "<project/>");

        Path first = repository.resolve("src/main/java/example/App.java");
        Path second = repository.resolve("src/test/java/example/AppTests.java");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.writeString(first, "package example; class App {}");
        Files.writeString(second, "package example; class AppTests {}");

        RepositoryFacts facts = inspector.inspect(repository, 1_000_000);

        assertThat(facts.mavenProject()).isTrue();
        assertThat(facts.javaFileCount()).isEqualTo(2);
        assertThat(facts.sizeBytes()).isPositive();
    }

    @Test
    void rejectsRepositoryAboveConfiguredLimit() throws Exception {
        Files.writeString(repository.resolve("large.bin"), "0123456789");

        assertThatThrownBy(() -> inspector.inspect(repository, 5))
                .isInstanceOf(RepositoryTooLargeException.class);
    }
}
