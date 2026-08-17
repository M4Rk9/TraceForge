package com.traceforge.repository.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class RepositoryWorkspace implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryWorkspace.class);

    private final Path path;

    RepositoryWorkspace(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() {
        if (!Files.exists(path)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(this::delete);
        } catch (IOException exception) {
            LOGGER.warn("Failed to clean repository workspace {}", path, exception);
        }
    }

    private void delete(Path candidate) {
        try {
            Files.deleteIfExists(candidate);
        } catch (IOException exception) {
            LOGGER.warn("Failed to delete workspace path {}", candidate, exception);
        }
    }
}
