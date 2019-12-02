package org.orekit.annotation;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link DefaultDataContextPlugin}.
 *
 * @author Evan Ward
 */
public class DefaultDataContextPluginTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testWarnings() throws IOException {
        // setup
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        Path output = Paths.get("target/example/classes");
        rmTree(output);
        Files.createDirectories(output);
        List<String> arguments = new ArrayList<>(Arrays.asList(
                "-cp", System.getProperty("java.class.path"),
                "-source", "1.8", "-target", "1.8",
                "-d", output.toAbsolutePath().toString(),
                "-Xplugin:dataContextPlugin"));
        Files.list(Paths.get("src/example/java"))
                .forEach(a -> arguments.add(a.toAbsolutePath().toString()));

        // action
        int retVal = javac.run(null, null, err, arguments.toArray(new String[0]));

        // verify
        String actual = err.toString();
        // System.out.println(actual);
        // count warnings ignoring duplicates
        long count = Arrays.stream(actual.split("\n"))
                .filter(s -> s.contains(DefaultDataContextPlugin.MESSAGE))
                .count();
        Assert.assertEquals(actual, count, 16);
    }

    private void rmTree(Path output) throws IOException {
        if (!Files.exists(output)) {
            return;
        }
        Files.walkFileTree(
                output,
                new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        throw exc;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
        );
    }

}
