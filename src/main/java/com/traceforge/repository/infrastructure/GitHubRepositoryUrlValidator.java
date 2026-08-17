package com.traceforge.repository.infrastructure;

import com.traceforge.repository.domain.GitHubRepository;
import com.traceforge.repository.exception.InvalidRepositoryUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class GitHubRepositoryUrlValidator {

    private static final Pattern PATH_SEGMENT =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]{0,99}");

    public GitHubRepository validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw invalid("Repository URL is required.");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException exception) {
            throw invalid("Repository URL is malformed.");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw invalid("Only HTTPS GitHub URLs are supported.");
        }

        if (!"github.com".equalsIgnoreCase(uri.getHost())) {
            throw invalid("Only repositories hosted on github.com are supported.");
        }

        if (uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw invalid("Credentials, custom ports, query strings, and fragments are not allowed.");
        }

        String rawPath = uri.getRawPath();
        if (rawPath == null || rawPath.isBlank() || rawPath.contains("%")) {
            throw invalid("Repository path is invalid.");
        }

        List<String> segments = Arrays.stream(rawPath.split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();

        if (segments.size() != 2) {
            throw invalid("URL must identify exactly one GitHub owner and repository.");
        }

        String owner = segments.get(0);
        String repositoryName = stripGitSuffix(segments.get(1));

        if (!PATH_SEGMENT.matcher(owner).matches()
                || !PATH_SEGMENT.matcher(repositoryName).matches()) {
            throw invalid("GitHub owner or repository name contains unsupported characters.");
        }

        URI webUri = URI.create("https://github.com/" + owner + "/" + repositoryName);
        URI cloneUri = URI.create(webUri + ".git");

        return new GitHubRepository(owner, repositoryName, cloneUri, webUri);
    }

    private String stripGitSuffix(String repositoryName) {
        if (repositoryName.endsWith(".git")) {
            return repositoryName.substring(0, repositoryName.length() - 4);
        }
        return repositoryName;
    }

    private InvalidRepositoryUrlException invalid(String message) {
        return new InvalidRepositoryUrlException(message);
    }
}
