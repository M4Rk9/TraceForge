# TraceForge

TraceForge is a local-first AI debugging and patch-validation platform for Java projects.

## Current milestone

TraceForge can securely clone a public Java Maven repository, index its source symbols with JavaParser, parse a JVM stack trace, and map each frame back to repository types and methods.

### Stack-trace analysis API

```http
POST /api/analyses/stack-trace
Content-Type: application/json

{
  "repositoryUrl": "https://github.com/owner/java-maven-project",
  "stackTrace": "java.lang.IllegalStateException\n    at com.example.OrderService.placeOrder(OrderService.java:42)"
}
```

Each parsed frame receives one result:

- `EXACT_METHOD`: class, method name, and source line identify one method
- `LINE_MATCH`: the source line identifies one method when the runtime name is synthetic
- `TYPE_ONLY`: the class is indexed but the method cannot be selected safely
- `UNMATCHED`: the frame does not belong to an indexed repository symbol

The response includes repository and commit identity, symbol counts, parsed-frame counts, match counts, source paths, method signatures, and source ranges.

### Repository inspection API

```http
POST /api/repositories/clone
Content-Type: application/json

{
  "repositoryUrl": "https://github.com/owner/java-maven-project"
}
```

This lower-level endpoint returns repository metadata and the complete Java symbol index.

## Repository safeguards

- only HTTPS URLs hosted on `github.com` are accepted
- credentials, custom ports, query strings, fragments, and encoded paths are rejected
- cloning is shallow and restricted to the default branch
- clone operations use a configurable timeout
- inspected repository size is limited to 100 MiB by default
- symbolic links are not followed during inspection
- temporary workspaces are removed after every request
- stack traces are limited to 100,000 characters and 512 parsed frames

## Java symbol index

JavaParser reads each Java source file using the Java 21 language level. The index contains fully qualified type names, type kinds, repository-relative paths, source ranges, and directly declared method signatures. Files that cannot be parsed are counted without preventing valid files from being indexed.

## Stack-trace mapping

The JVM parser supports ordinary frames, Java module prefixes, source lines, `Unknown Source`, `Native Method`, nested classes, anonymous classes, and generated proxy or lambda class names. Overloaded methods are selected only when their source ranges make the match unambiguous.

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

Create method-level source chunks and deterministic exact retrieval from stack-trace matches, including relevant tests and neighbouring types.

## License

A license will be selected before the first public release.
