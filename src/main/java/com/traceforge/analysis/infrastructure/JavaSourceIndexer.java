package com.traceforge.analysis.infrastructure;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.traceforge.analysis.domain.JavaMethodSymbol;
import com.traceforge.analysis.domain.JavaSymbolIndex;
import com.traceforge.analysis.domain.JavaTypeSymbol;
import com.traceforge.analysis.exception.JavaSourceIndexException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class JavaSourceIndexer {

    private final JavaParser javaParser;

    public JavaSourceIndexer() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        this.javaParser = new JavaParser(configuration);
    }

    public JavaSymbolIndex index(Path repositoryRoot) {
        List<Path> sourceFiles = findSourceFiles(repositoryRoot);
        List<JavaTypeSymbol> types = new ArrayList<>();
        long parsedFileCount = 0;
        long failedFileCount = 0;

        for (Path sourceFile : sourceFiles) {
            try {
                ParseResult<CompilationUnit> result = javaParser.parse(sourceFile);
                if (!result.isSuccessful() || result.getResult().isEmpty()) {
                    failedFileCount++;
                    continue;
                }

                parsedFileCount++;
                types.addAll(indexTypes(
                        repositoryRoot,
                        sourceFile,
                        result.getResult().orElseThrow()
                ));
            } catch (IOException exception) {
                failedFileCount++;
            }
        }

        types.sort(Comparator
                .comparing(JavaTypeSymbol::qualifiedName)
                .thenComparing(JavaTypeSymbol::sourcePath)
                .thenComparingInt(JavaTypeSymbol::beginLine));

        return JavaSymbolIndex.from(parsedFileCount, failedFileCount, types);
    }

    private List<Path> findSourceFiles(Path repositoryRoot) {
        Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        Path gitDirectory = normalizedRoot.resolve(".git");

        try (Stream<Path> paths = Files.walk(normalizedRoot)) {
            return paths
                    .filter(path -> !path.startsWith(gitDirectory))
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new JavaSourceIndexException(
                    "TraceForge could not scan Java source files.",
                    exception
            );
        }
    }

    private List<JavaTypeSymbol> indexTypes(
            Path repositoryRoot,
            Path sourceFile,
            CompilationUnit compilationUnit
    ) {
        String packageName = compilationUnit.getPackageDeclaration()
                .map(declaration -> declaration.getNameAsString())
                .orElse("");
        String sourcePath = repositoryRoot.toAbsolutePath().normalize()
                .relativize(sourceFile.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');

        return compilationUnit.findAll(TypeDeclaration.class).stream()
                .map(type -> toTypeSymbol(type, packageName, sourcePath))
                .toList();
    }

    private JavaTypeSymbol toTypeSymbol(
            TypeDeclaration<?> type,
            String packageName,
            String sourcePath
    ) {
        List<JavaMethodSymbol> methods = type.getMembers().stream()
                .filter(MethodDeclaration.class::isInstance)
                .map(MethodDeclaration.class::cast)
                .map(this::toMethodSymbol)
                .sorted(Comparator
                        .comparingInt(JavaMethodSymbol::beginLine)
                        .thenComparing(JavaMethodSymbol::signature))
                .toList();

        return new JavaTypeSymbol(
                qualifiedName(type, packageName),
                typeKind(type),
                sourcePath,
                beginLine(type),
                endLine(type),
                methods
        );
    }

    private JavaMethodSymbol toMethodSymbol(MethodDeclaration method) {
        return new JavaMethodSymbol(
                method.getNameAsString(),
                method.getSignature().asString(),
                method.getTypeAsString(),
                beginLine(method),
                endLine(method)
        );
    }

    private String qualifiedName(TypeDeclaration<?> type, String packageName) {
        List<String> names = new ArrayList<>();
        Optional<Node> current = Optional.of(type);

        while (current.isPresent()) {
            Node node = current.orElseThrow();
            if (node instanceof TypeDeclaration<?> currentType) {
                names.add(currentType.getNameAsString());
            }
            current = node.getParentNode();
        }

        java.util.Collections.reverse(names);
        String typeName = String.join(".", names);
        return packageName.isBlank() ? typeName : packageName + "." + typeName;
    }

    private String typeKind(TypeDeclaration<?> type) {
        if (type instanceof ClassOrInterfaceDeclaration declaration) {
            return declaration.isInterface() ? "INTERFACE" : "CLASS";
        }
        if (type instanceof EnumDeclaration) {
            return "ENUM";
        }
        if (type instanceof RecordDeclaration) {
            return "RECORD";
        }
        if (type instanceof AnnotationDeclaration) {
            return "ANNOTATION";
        }
        return type.getClass().getSimpleName()
                .replace("Declaration", "")
                .toUpperCase(Locale.ROOT);
    }

    private int beginLine(Node node) {
        return node.getBegin().map(position -> position.line).orElse(-1);
    }

    private int endLine(Node node) {
        return node.getEnd().map(position -> position.line).orElse(-1);
    }
}
