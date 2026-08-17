package com.traceforge.repository.domain;

import java.net.URI;

public record GitHubRepository(
        String owner,
        String name,
        URI cloneUri,
        URI webUri
) {
}
