/* Copyright 2002-2023 CS GROUP
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
package org.orekit.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/** Provider for data files stored as resources in the classpath.
 * <p>
 * This class handles a list of data files or zip/jar archives located in the
 * classpath. Since the classpath is not a tree structure the list elements
 * cannot be whole directories recursively browsed as in {@link
 * DirectoryCrawler}, they must be data files or zip/jar archives.
 * </p>
 * <p>
 * A typical use case is to put all data files in a single zip or jar archive
 * and to build an instance of this class with the single name of this zip/jar
 * archive. Two different instances may be used one for user or project specific
 * data and another one for system-wide or general data.
 * </p>
 * <p>
 * All {@link FiltersManager#addFilter(DataFilter) registered}
 * {@link DataFilter filters} are applied.
 * </p>
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for
 * list browsing.
 * </p>
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public class ClasspathCrawler implements DataProvider {

    /** List elements. */
    private final List<String> listElements;

    /** Class loader to use. */
    private final ClassLoader classLoader;

    /** Build a data classpath crawler.
     * <p>
     * Calling this constructor has the same effect as calling
     * {@link #ClasspathCrawler(ClassLoader, String...)} with
     * {@code ClasspathCrawler.class.getClassLoader()} as first
     * argument.
     * </p>
     * @param list list of data file names within the classpath
     */
    public ClasspathCrawler(final String... list) {
        this(ClasspathCrawler.class.getClassLoader(), list);
    }

    /** Build a data classpath crawler.
     * @param classLoader class loader to use to retrieve the resources
     * @param list list of data file names within the classpath
     */
    public ClasspathCrawler(final ClassLoader classLoader, final String... list) {

        listElements = new ArrayList<>();
        this.classLoader = classLoader;

        // check the resources
        for (final String name : list) {
            if (!"".equals(name)) {

                final String convertedName = name.replace('\\', '/');
                try (InputStream stream = classLoader.getResourceAsStream(convertedName)) {
                    if (stream == null) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_RESOURCE, name);
                    }
                    listElements.add(convertedName);
                } catch (IOException exc) {
                    // ignore this error
                }

            }
        }

    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported,
                        final DataLoader visitor,
                        final DataProvidersManager manager) {

        try {
            OrekitException delayedException = null;
            boolean loaded = false;
            for (final String name : listElements) {
                try {

                    if (visitor.stillAcceptsData()) {
                        if (ZIP_ARCHIVE_PATTERN.matcher(name).matches()) {

                            // browse inside the zip/jar file
                            final DataProvider zipProvider = new ZipJarCrawler(name);
                            loaded = zipProvider.feed(supported, visitor, manager) || loaded;

                        } else {

                            // match supported name against file name #618
                            final String fileName = name.substring(name.lastIndexOf('/') + 1);
                            DataSource data = new DataSource(fileName, () -> classLoader.getResourceAsStream(name));
                            // apply all registered filters
                            data = manager.getFiltersManager().applyRelevantFilters(data);

                            if (supported.matcher(data.getName()).matches()) {
                                // visit the current file
                                try (InputStream input = data.getOpener().openStreamOnce()) {
                                    final URI uri = classLoader.getResource(name).toURI();
                                    visitor.loadData(input, uri.toString());
                                    loaded = true;
                                }

                            }

                        }
                    }

                } catch (OrekitException oe) {
                    // maybe the next path component will be able to provide data
                    // wait until all components have been tried
                    delayedException = oe;
                } catch (URISyntaxException use) {
                    // this should bever happen
                    throw new OrekitException(use, LocalizedCoreFormats.SIMPLE_MESSAGE, use.getMessage());
                }
            }

            if (!loaded && delayedException != null) {
                throw delayedException;
            }

            return loaded;

        } catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

    }

}
