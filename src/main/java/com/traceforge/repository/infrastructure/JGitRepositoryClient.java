package com.traceforge.repository.infrastructure;

import com.traceforge.repository.domain.RepositoryCloneMetadata;
import com.traceforge.repository.exception.RepositoryCloneException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

@Component
public class JGitRepositoryClient {

    private final int cloneTimeoutSeconds;

    public JGitRepositoryClient(
            @Value("${traceforge.repository.clone-timeout-seconds:30}")
            int cloneTimeoutSeconds
    ) {
        if (cloneTimeoutSeconds < 1) {
            throw new IllegalArgumentException("Clone timeout must be positive.");
        }
        this.cloneTimeoutSeconds = cloneTimeoutSeconds;
    }

    public RepositoryCloneMetadata cloneRepository(URI repositoryUri, Path targetDirectory) {
        try (Git git = Git.cloneRepository()
                .setURI(repositoryUri.toString())
                .setDirectory(targetDirectory.toFile())
                .setDepth(1)
                .setCloneAllBranches(false)
                .setTimeout(cloneTimeoutSeconds)
                .call()) {

            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            if (head == null) {
                throw new RepositoryCloneException(
                        "The repository does not contain a resolvable HEAD commit.",
                        null
                );
            }

            return new RepositoryCloneMetadata(
                    git.getRepository().getBranch(),
                    head.getName()
            );
        } catch (GitAPIException | IOException exception) {
            throw new RepositoryCloneException(
                    "TraceForge could not clone the requested public repository.",
                    exception
            );
        }
    }
}
