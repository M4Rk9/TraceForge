package com.traceforge.repository.infrastructure;

import com.traceforge.repository.domain.GitHubRepository;
import com.traceforge.repository.exception.InvalidRepositoryUrlException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubRepositoryUrlValidatorTests {

    private final GitHubRepositoryUrlValidator validator =
            new GitHubRepositoryUrlValidator();

    @Test
    void normalizesValidGitHubRepositoryUrl() {
        GitHubRepository repository = validator.validateAndNormalize(
                "https://github.com/spring-projects/spring-petclinic.git"
        );

        assertThat(repository.owner()).isEqualTo("spring-projects");
        assertThat(repository.name()).isEqualTo("spring-petclinic");
        assertThat(repository.webUri().toString())
                .isEqualTo("https://github.com/spring-projects/spring-petclinic");
        assertThat(repository.cloneUri().toString())
                .isEqualTo("https://github.com/spring-projects/spring-petclinic.git");
    }

    @Test
    void acceptsTrailingSlash() {
        GitHubRepository repository = validator.validateAndNormalize(
                "https://github.com/owner/repository/"
        );

        assertThat(repository.name()).isEqualTo("repository");
    }

    @Test
    void rejectsNonHttpsUrl() {
        assertInvalid("http://github.com/owner/repository");
    }

    @Test
    void rejectsNonGitHubHost() {
        assertInvalid("https://example.com/owner/repository");
    }

    @Test
    void rejectsGitHubLookalikeHost() {
        assertInvalid("https://github.com.example.org/owner/repository");
    }

    @Test
    void rejectsCredentials() {
        assertInvalid("https://user:secret@github.com/owner/repository");
    }

    @Test
    void rejectsCustomPort() {
        assertInvalid("https://github.com:443/owner/repository");
    }

    @Test
    void rejectsQueryAndFragment() {
        assertInvalid("https://github.com/owner/repository?tab=readme");
        assertInvalid("https://github.com/owner/repository#readme");
    }

    @Test
    void rejectsNestedGitHubPage() {
        assertInvalid("https://github.com/owner/repository/issues");
    }

    @Test
    void rejectsEncodedPath() {
        assertInvalid("https://github.com/owner%2Frepository");
    }

    @Test
    void rejectsBlankUrl() {
        assertInvalid(" ");
    }

    private void assertInvalid(String url) {
        assertThatThrownBy(() -> validator.validateAndNormalize(url))
                .isInstanceOf(InvalidRepositoryUrlException.class);
    }
}
