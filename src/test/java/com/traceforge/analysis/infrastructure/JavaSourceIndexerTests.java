package com.traceforge.analysis.infrastructure;

import com.traceforge.analysis.domain.JavaSymbolIndex;
import com.traceforge.analysis.domain.JavaTypeSymbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceIndexerTests {

    @TempDir
    Path repository;

    private final JavaSourceIndexer indexer = new JavaSourceIndexer();

    @Test
    void indexesTypesAndMethodsWithStableSourceLocations() throws Exception {
        Path source = repository.resolve("src/main/java/example/GreetingService.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package example;

                public class GreetingService {
                    public String greet(String name) {
                        return "Hello " + name;
                    }

                    static class Formatter {
                        String format(String value) {
                            return value.trim();
                        }
                    }
                }
                """);

        JavaSymbolIndex index = indexer.index(repository);

        assertThat(index.parsedFileCount()).isEqualTo(1);
        assertThat(index.failedFileCount()).isZero();
        assertThat(index.typeCount()).isEqualTo(2);
        assertThat(index.methodCount()).isEqualTo(2);

        JavaTypeSymbol service = index.types().stream()
                .filter(type -> type.qualifiedName().equals("example.GreetingService"))
                .findFirst()
                .orElseThrow();
        assertThat(service.kind()).isEqualTo("CLASS");
        assertThat(service.sourcePath())
                .isEqualTo("src/main/java/example/GreetingService.java");
        assertThat(service.beginLine()).isEqualTo(3);
        assertThat(service.methods())
                .extracting(method -> method.signature())
                .containsExactly("greet(String)");
        assertThat(service.methods().getFirst().returnType()).isEqualTo("String");

        assertThat(index.types())
                .extracting(JavaTypeSymbol::qualifiedName)
                .containsExactly(
                        "example.GreetingService",
                        "example.GreetingService.Formatter"
                );
    }

    @Test
    void recordsUnparseableFilesAndKeepsValidSymbols() throws Exception {
        Path sourceRoot = repository.resolve("src/main/java/example");
        Files.createDirectories(sourceRoot);
        Files.writeString(
                sourceRoot.resolve("Valid.java"),
                "package example; interface Valid { void run(); }"
        );
        Files.writeString(
                sourceRoot.resolve("Broken.java"),
                "package example; class Broken {"
        );

        JavaSymbolIndex index = indexer.index(repository);

        assertThat(index.parsedFileCount()).isEqualTo(1);
        assertThat(index.failedFileCount()).isEqualTo(1);
        assertThat(index.types())
                .extracting(JavaTypeSymbol::qualifiedName)
                .containsExactly("example.Valid");
        assertThat(index.types().getFirst().kind()).isEqualTo("INTERFACE");
        assertThat(index.methodCount()).isEqualTo(1);
    }
}
