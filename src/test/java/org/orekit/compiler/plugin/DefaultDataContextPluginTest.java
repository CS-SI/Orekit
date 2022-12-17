/* Contributed in the public domain.
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.compiler.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

/**
 * Unit test for {@link DefaultDataContextPlugin}.
 *
 * @author Evan Ward
 */
public class DefaultDataContextPluginTest {

    /**
     * Check compiling an example program generates the expected number of warnings.
     *
     * @throws IOException on error.
     */
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
                "-Xmaxwarns", "9999",
                "-Xplugin:dataContextPlugin"));
        Files.list(Paths.get("src/test/resources/compiler-plugin"))
                .forEach(a -> arguments.add(a.toAbsolutePath().toString()));

        // action
        int retVal = javac.run(null, null, err, arguments.toArray(new String[0]));

        // verify
        String actual = err.toString();
        // count warnings ignoring duplicates
        long count = Arrays.stream(actual.split("\n"))
                .filter(s -> s.contains(DefaultDataContextPlugin.MESSAGE))
                .count();
        Assertions.assertEquals(count, 30, actual);
        Assertions.assertFalse(actual.contains(" error:"),actual);
        Assertions.assertEquals(0, retVal, actual);
    }

    /**
     * {@code rm -r path}.
     *
     * @param path to remove.
     * @throws IOException on error.
     */
    private void rmTree(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        Files.walkFileTree(
                path,
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
