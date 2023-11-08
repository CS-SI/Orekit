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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/**  Provider for data files stored in a directories tree on filesystem.
 * <p>
 * This class handles data files recursively starting from a root directories
 * tree. The organization of files in the directories is free. There may be
 * sub-directories to any level. All sub-directories are browsed and all terminal
 * files are checked for loading.
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
 * directory hierarchy crawling.
 * </p>
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public class DirectoryCrawler implements DataProvider {

    /** Root directory. */
    private final File root;

    /** Build a data files crawler.
     * @param root root of the directories tree (must be a directory)
     */
    public DirectoryCrawler(final File root) {
        if (!root.isDirectory()) {
            throw new OrekitException(OrekitMessages.NOT_A_DIRECTORY, root.getAbsolutePath());
        }
        this.root = root;
    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported,
                        final DataLoader visitor,
                        final DataProvidersManager manager) {
        try {
            return feed(supported, visitor, manager, root);
        } catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Feed a data file loader by browsing a directory hierarchy.
     * @param supported pattern for file names supported by the visitor
     * @param visitor data file visitor to feed
     * @param manager with the filters to apply.
     * @param directory current directory
     * @return true if something has been loaded
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private boolean feed(final Pattern supported,
                         final DataLoader visitor,
                         final DataProvidersManager manager,
                         final File directory)
        throws IOException, ParseException {

        // search in current directory
        final File[] list = directory.listFiles();
        if (list == null) {
            // notify about race condition if directory is removed by another program
            throw new OrekitException(OrekitMessages.NOT_A_DIRECTORY, directory.getAbsolutePath());
        }
        Arrays.sort(list, File::compareTo);

        OrekitException delayedException = null;
        boolean loaded = false;
        for (final File file : list) {
            try {
                if (visitor.stillAcceptsData()) {
                    if (file.isDirectory()) {

                        // recurse in the sub-directory
                        loaded = feed(supported, visitor, manager, file) || loaded;

                    } else if (ZIP_ARCHIVE_PATTERN.matcher(file.getName()).matches()) {

                        // browse inside the zip/jar file
                        final DataProvider zipProvider = new ZipJarCrawler(file);
                        loaded = zipProvider.feed(supported, visitor, manager) || loaded;

                    } else {

                        // apply all registered filters
                        DataSource data = new DataSource(file.getName(), () -> new FileInputStream(file));
                        data = manager.getFiltersManager().applyRelevantFilters(data);

                        if (supported.matcher(data.getName()).matches()) {
                            // visit the current file
                            try (InputStream input = data.getOpener().openStreamOnce()) {
                                visitor.loadData(input, file.getPath());
                                loaded = true;
                            }
                        }

                    }
                }
            } catch (OrekitException oe) {
                delayedException = oe;
            }

        }

        if (!loaded && delayedException != null) {
            throw delayedException;
        }

        return loaded;

    }

}
