# TraceForge

TraceForge is a local-first AI debugging and patch-validation platform for Java projects.

## Current milestone

TraceForge can safely accept a public GitHub repository URL, perform a shallow clone with JGit, inspect the disposable workspace, and return repository metadata.

### Repository inspection API

```http
POST /api/repositories/clone
Content-Type: application/json

{
  "repositoryUrl": "https://github.com/owner/java-maven-project"
}
```

A successful response includes:

- normalized repository URL
- checked-out branch
- HEAD commit SHA
- repository size
- Java source-file count
- detected build system

The cloned workspace is deleted after inspection.

## Repository-cloning safeguards

- only HTTPS URLs hosted on `github.com` are accepted
- credentials, custom ports, query strings, fragments, and encoded paths are rejected
- cloning is shallow and restricted to the default branch
- clone operations use a configurable timeout
- inspected repository size is limited to 100 MiB by default
- symbolic links are not followed during inspection
- temporary workspaces are removed after every request

## Technology stack

- Java 21
- Spring Boot
- Maven
- HTML, CSS, and JavaScript
- JGit
- JavaParser
- Ollama
- Docker
- JUnit 5
- GitHub Actions

## Planned next milestone

Parse Java source files into class- and method-level symbols using JavaParser.

## License

A license will be selected before the first public release.
