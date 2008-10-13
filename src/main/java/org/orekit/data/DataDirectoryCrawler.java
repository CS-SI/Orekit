/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.orekit.errors.OrekitException;


/** Helper class for loading data files.

 * <p>
 * This class handles data files recursively starting from root trees
 * (or zip archives) specified by the java property <code>orekit.data.path</code>.
 * If the property is not set or is null, no data will be available to the
 * library (for example no pole corrections will be applied and only predefined
 * UTC steps will be taken into account). No errors will be triggered.
 * If the property is set, it must contains a colon or semicolon separated list
 * of existing directories or zip archives, which themselves contain the data files
 * (or other zip files).
 * </p>
 * <p>
 * The organization of files in the directories is free. Files are found by matching
 * name patterns while crawling into all sub-directories. If the date searched for
 * is found in one path component, the following path components will be ignored,
 * thus allowing users to overwrite system-wide data by prepending their own
 * components before system-wide ones.
 * </p>
 * <p>Gzip-compressed files are supported.</p>
 *
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for
 * directory hierarchy crawling.
 * </p>
 *
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class DataDirectoryCrawler {

    /** Name of the property defining the root directories or zip files path. */
    public static final String OREKIT_DATA_PATH = "orekit.data.path";

    /** Pattern for gzip files. */
    private static final Pattern GZIP_FILE_PATTERN = Pattern.compile("(.*)\\.gz$");

    /** Pattern for zip archives. */
    private static final Pattern ZIP_ARCHIVE_PATTERN = Pattern.compile("(.*)(?:(?:\\.zip)|(?:\\.jar))$");

    /** Error message for unknown path entries. */
    private static final String NEITHER_DIRECTORY_NOR_ZIP_ARCHIVE =
        "{0} is neither a directory nor a zip archive file";

    /** Path components. */
    private final List<File> pathComponents;

    /** Build a data files crawler.
     * @exception OrekitException if path contains inexistent components
     */
    public DataDirectoryCrawler() throws OrekitException {

        final List<File> components = new ArrayList<File>();

        try {

            // get the path containing all components
            final String path = System.getProperty(OREKIT_DATA_PATH);

            if ((path != null) && !"".equals(path)) {

                // extract the various components
                for (final String name : path.split("[:;]")) {
                    if (!"".equals(name)) {

                        final File component = new File(name);

                        // check component
                        if (!component.exists()) {
                            throw new OrekitException("data root directory {0} does not exist",
                                                      new Object[] {
                                                          name
                                                      });
                        }

                        components.add(component);

                    }
                }
            }
        } finally {
            pathComponents = components;
        }

    }

    /** Crawl the data root hierarchy.
     * @param visitor data file visitor to use
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     */
    public void crawl(final DataFileLoader visitor) throws OrekitException {

        OrekitException delayedException = null;

        for (File component : pathComponents) {
            try {

                // try to find data in one path component
                if (component.isDirectory()) {
                    crawl(visitor, component);
                } else if (!ZIP_ARCHIVE_PATTERN.matcher(component.getName()).matches()) {
                    throw new OrekitException(NEITHER_DIRECTORY_NOR_ZIP_ARCHIVE,
                                              new Object[] {
                                                  component.getAbsolutePath()
                                              });
                } else {
                    final ZipInputStream zip = new ZipInputStream(new FileInputStream(component));
                    crawl(visitor, zip);
                    zip.close();
                }

                // if we got here, we have found the data we wanted,
                // we explicitly ignore the following path components
                return;

            } catch (ZipException ze) {
                // this is an important configuration error, we report it immediately
                throw new OrekitException(NEITHER_DIRECTORY_NOR_ZIP_ARCHIVE,
                                          new Object[] {
                                              component.getAbsolutePath()
                                          });
            } catch (IOException ioe) {
                // maybe the next path component will be able to provide data
                // wait until all components have been tried
                delayedException = new OrekitException(ioe.getMessage(), ioe);
            } catch (ParseException pe) {
                // maybe the next path component will be able to provide data
                // wait until all components have been tried
                delayedException = new OrekitException(pe.getMessage(), pe);
            } catch (OrekitException oe) {
                // maybe the next path component will be able to provide data
                // wait until all components have been tried
                delayedException = oe;
            }
        }

        if (delayedException != null) {
            throw delayedException;
        }

    }

    /** Crawl a directory hierarchy.
     * @param visitor data file visitor to use
     * @param directory current directory
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private void crawl(final DataFileLoader visitor, final File directory)
        throws OrekitException, IOException, ParseException {

        // search in current directory
        final File[] list = directory.listFiles();

        for (int i = 0; i < list.length; ++i) {
            if (list[i].isDirectory()) {

                // recurse in the sub-directory
                crawl(visitor, list[i]);

            } else if (ZIP_ARCHIVE_PATTERN.matcher(list[i].getName()).matches()) {

                // crawl inside the zip file
                crawl(visitor, new ZipInputStream(new FileInputStream(list[i])));

            } else {

                // remove suffix from gzip files
                final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(list[i].getName());
                final String baseName =
                    gzipMatcher.matches() ? gzipMatcher.group(1) : list[i].getName();

                if (visitor.fileIsSupported(baseName)) {

                    // visit the current file
                    InputStream input = new FileInputStream(list[i]);
                    if (gzipMatcher.matches()) {
                        input = new GZIPInputStream(input);
                    }
                    visitor.loadData(input, list[i].getName());

                }

            }
        }

    }

    /** Crawl the files in a zip.
     * @param visitor data file visitor to use
     * @param zip zip input stream
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private void crawl(final DataFileLoader visitor, final ZipInputStream zip)
        throws OrekitException, IOException, ParseException {

        // loop over all zip entries
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {

            if (!entry.isDirectory()) {

                if (ZIP_ARCHIVE_PATTERN.matcher(entry.getName()).matches()) {

                    // recurse inside the zip file
                    crawl(visitor, new ZipInputStream(zip));

                } else {
                    // remove leading directories
                    String name = entry.getName();
                    final int lastSlash = name.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        name = name.substring(lastSlash + 1);
                    }

                    // remove suffix from gzip files
                    final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(name);
                    final String baseName = gzipMatcher.matches() ? gzipMatcher.group(1) : name;

                    if (visitor.fileIsSupported(baseName)) {

                        // visit the current file
                        if (gzipMatcher.matches()) {
                            visitor.loadData(new GZIPInputStream(zip), name);
                        } else {
                            visitor.loadData(zip, name);
                        }

                    }

                }

            }

            // prepare next entry processing
            zip.closeEntry();
            entry = zip.getNextEntry();

        }
    }

}
