package com.rabtech.orderworkflow.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class DomainPurityTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final Path POM = Path.of("pom.xml");
    private static final List<String> ALLOWED_IMPORT_PREFIXES = List.of(
            "java.math.", "java.util.", "com.rabtech.orderworkflow.domain.");
    private static final List<String> FRAMEWORK_MARKERS = List.of(
            "org.springframework.context.ApplicationContext",
            "org.springframework.boot.SpringApplication",
            "jakarta.persistence.Entity",
            "javax.persistence.Entity",
            "org.hibernate.Session",
            "org.postgresql.Driver",
            "com.mysql.cj.jdbc.Driver");

    @Test
    void sourcesImportOnlyPlainJavaAndTheDomainItself() throws IOException {
        List<String> violations;
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            violations = files
                    .filter(file -> file.toString().endsWith(".java"))
                    .flatMap(DomainPurityTest::importsOf)
                    .filter(imported -> ALLOWED_IMPORT_PREFIXES.stream().noneMatch(imported::startsWith))
                    .collect(Collectors.toList());
        }

        assertTrue(violations.isEmpty(), "Domain imports outside plain Java: " + violations);
    }

    @Test
    void pomDeclaresNothingButTestDependencies() throws Exception {
        NodeList dependencies = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(POM.toFile())
                .getElementsByTagName("dependency");

        assertTrue(dependencies.getLength() > 0, "expected at least the JUnit test dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            String groupId = text(dependency, "groupId");
            String scope = text(dependency, "scope");

            assertEquals("org.junit.jupiter", groupId, "unexpected dependency in domain pom");
            assertTrue(scope.isEmpty() || scope.equals("test"), groupId + " must be test scoped");
        }
    }

    @Test
    void noFrameworkOrDriverClassesAreOnTheClasspath() {
        for (String marker : FRAMEWORK_MARKERS) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(marker), marker);
        }
    }

    @Test
    void domainDoesNotReferenceTheOtherModules() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            boolean referencesOuterLayers = files
                    .filter(file -> file.toString().endsWith(".java"))
                    .anyMatch(DomainPurityTest::mentionsOuterLayer);

            assertFalse(referencesOuterLayers, "domain must not know the application or adapters packages");
        }
    }

    private static Stream<String> importsOf(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::strip)
                    .filter(line -> line.startsWith("import "))
                    .map(line -> line.replace("import static ", "").replace("import ", "").replace(";", "").strip());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static boolean mentionsOuterLayer(Path file) {
        try {
            String source = Files.readString(file);
            return source.contains("orderworkflow.application") || source.contains("orderworkflow.adapters");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + file, e);
        }
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().strip();
    }
}
