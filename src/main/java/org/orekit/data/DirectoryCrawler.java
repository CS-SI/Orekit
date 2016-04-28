/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

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
 * Gzip-compressed files are supported.
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
     * @exception OrekitException if root is not a directory
     */
    public DirectoryCrawler(final File root) throws OrekitException {
        if (!root.isDirectory()) {
            throw new OrekitException(OrekitMessages.NOT_A_DIRECTORY, root.getAbsolutePath());
        }
        this.root = root;
    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported, final DataLoader visitor)
        throws OrekitException {
        try {
            return feed(supported, visitor, root);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        } catch (ParseException pe) {
            throw new OrekitException(pe, new DummyLocalizable(pe.getMessage()));
        }
    }

    /** Feed a data file loader by browsing a directory hierarchy.
     * @param supported pattern for file names supported by the visitor
     * @param visitor data file visitor to feed
     * @param directory current directory
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     * @return true if something has been loaded
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private boolean feed(final Pattern supported, final DataLoader visitor, final File directory)
        throws OrekitException, IOException, ParseException {

        // search in current directory
        final File[] list = directory.listFiles();
        Arrays.sort(list, new Comparator<File>() {
            @Override
            public int compare(final File o1, final File o2) {
                return o1.compareTo(o2);
            }
        });

        OrekitException delayedException = null;
        boolean loaded = false;
        for (int i = 0; i < list.length; ++i) {
            try {
                if (visitor.stillAcceptsData()) {
                    if (list[i].isDirectory()) {

                        // recurse in the sub-directory
                        loaded = feed(supported, visitor, list[i]) || loaded;

                    } else if (ZIP_ARCHIVE_PATTERN.matcher(list[i].getName()).matches()) {

                        // browse inside the zip/jar file
                        final DataProvider zipProvider = new ZipJarCrawler(list[i]);
                        loaded = zipProvider.feed(supported, visitor) || loaded;

                    } else {

                        // remove suffix from gzip files
                        final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(list[i].getName());
                        final String baseName =
                            gzipMatcher.matches() ? gzipMatcher.group(1) : list[i].getName();

                        if (supported.matcher(baseName).matches()) {

                            // visit the current file
                            InputStream input = new FileInputStream(list[i]);
                            if (gzipMatcher.matches()) {
                                input = new GZIPInputStream(input);
                            }
                            visitor.loadData(input, list[i].getPath());
                            input.close();
                            loaded = true;

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
