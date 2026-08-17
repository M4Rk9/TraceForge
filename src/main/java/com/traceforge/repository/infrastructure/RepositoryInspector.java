package com.traceforge.repository.infrastructure;

import com.traceforge.repository.domain.RepositoryFacts;
import com.traceforge.repository.exception.RepositoryTooLargeException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

@Component
public class RepositoryInspector {

    public RepositoryFacts inspect(Path repositoryRoot, long maximumSizeBytes) {
        long sizeBytes = 0;
        long javaFileCount = 0;

        try (Stream<Path> paths = Files.walk(repositoryRoot)) {
            Iterator<Path> iterator = paths.iterator();

            while (iterator.hasNext()) {
                Path path = iterator.next();

                if (Files.isSymbolicLink(path)
                        || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }

                sizeBytes += Files.size(path);
                if (sizeBytes > maximumSizeBytes) {
                    throw new RepositoryTooLargeException(sizeBytes, maximumSizeBytes);
                }

                Path relativePath = repositoryRoot.relativize(path);
                boolean gitMetadata = relativePath.getNameCount() > 0
                        && ".git".equals(relativePath.getName(0).toString());

                if (!gitMetadata && path.getFileName().toString().endsWith(".java")) {
                    javaFileCount++;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not inspect the cloned repository.", exception);
        }

        boolean mavenProject = Files.isRegularFile(
                repositoryRoot.resolve("pom.xml"),
                LinkOption.NOFOLLOW_LINKS
        );

        return new RepositoryFacts(sizeBytes, javaFileCount, mavenProject);
    }
}
