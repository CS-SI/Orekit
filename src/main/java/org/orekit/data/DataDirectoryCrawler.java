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
import java.net.URL;

import org.orekit.errors.OrekitException;


/** Helper class for loading data files.

 * <p>
 * This class handles data files recursively starting from root trees
 * specified by the java properties <code>orekit.data.directory.filesystem</code>
 * for data stored in filesystem and <code>orekit.data.directory.classpath</code>
 * for data stored in classpath.
 * If the properties are not set or are null, no data will be available to the
 * library (for example no pole correction will be applied and only predefined
 * UTC steps will be taken into account). No errors will be triggered.
 * If either property is set, it must correspond to an existing root tree otherwise
 * an error will be triggered.
 * </p>
 * <p>
 * The organization of files in the tree is free. Files are found by matching
 * name patterns while crawling into all sub-directories.
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

    /** Name of the property defining the data root directory in filesystem. */
    public static final String DATA_ROOT_DIRECTORY_FS = "orekit.data.directory.filesystem";

    /** Name of the property defining the data root directory in classpath. */
    public static final String DATA_ROOT_DIRECTORY_CP = "orekit.data.directory.classpath";

    /** IERS root hierarchy root in filesystem. */
    private final File rootInFileSystem;

    /** IERS root hierarchy root in classpath. */
    private final File rootInClasspath;

    /** Build an IERS files crawler.
     * @exception OrekitException if some data is missing or can't be read
     */
    public DataDirectoryCrawler() throws OrekitException {

        File fsRoot = null;
        File cpRoot = null;

        try {

            // set up the root tree in filesystem
            final String directoryNameFileSystem = System.getProperty(DATA_ROOT_DIRECTORY_FS);
            if ((directoryNameFileSystem != null) && !"".equals(directoryNameFileSystem)) {

                // find the root directory
                fsRoot = new File(directoryNameFileSystem);

                // safety checks
                checkRoot(fsRoot, directoryNameFileSystem);

            }

            // set up the root tree in classpath
            final String directoryNameClasspath = System.getProperty(DATA_ROOT_DIRECTORY_CP);
            if ((directoryNameClasspath != null) && !"".equals(directoryNameClasspath)) {

                // find the root directory
                final URL url =
                    DataDirectoryCrawler.class.getClassLoader().getResource(directoryNameClasspath);
                if (url != null) {
                    cpRoot = new File(url.getPath());
                }

                // safety checks
                checkRoot(cpRoot, directoryNameClasspath);

            }

        } finally {
            rootInFileSystem = fsRoot;
            rootInClasspath  = cpRoot;
        }

    }

    /** Check root directory.
     * @param root root directory to check (may be null)
     * @param name root directory name
     * @exception OrekitException if the root does not exist
     * or is not a directory
     */
    private void checkRoot(final File root, final String name)
        throws OrekitException {
        if ((root == null) || !root.exists()) {
            throw new OrekitException("data root directory {0} does not exist",
                                      new Object[] {
                                          name
                                      });
        }
        if (!root.isDirectory()) {
            throw new OrekitException("{0} is not a directory",
                                      new Object[] {
                                          name
                                      });
        }
    }

    /** Crawl the data root hierarchy.
     * @param visitor data file visitor to use
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     */
    public void crawl(final DataFileCrawler visitor) throws OrekitException {

        // first try in filesystem
        // (if a data root directory has been defined in filesystem)
        if (rootInFileSystem != null) {
            try {
                crawl(visitor, rootInFileSystem);
                return;
            } catch (OrekitException oe) {
                if (rootInClasspath == null) {
                    throw oe;
                }
            }
        }

        // then try in classpath
        // (if a data root directory has been defined in classpath and filesystem attempt failed)
        if (rootInClasspath != null) {
            crawl(visitor, rootInClasspath);
        }

    }

    /** Crawl a directory hierarchy.
     * @param visitor data file visitor to use
     * @param directory hierarchy root directory
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     */
    private void crawl(final DataFileCrawler visitor, final File directory)
        throws OrekitException {

        // search in current directory
        final File[] list = directory.listFiles();
        for (int i = 0; i < list.length; ++i) {
            if (list[i].isDirectory()) {

                // recurse in the sub-directory
                crawl(visitor, list[i]);

            } else  if (visitor.fileIsSupported(list[i].getName())) {

                // visit the current file
                visitor.visit(list[i]);

            }
        }

    }

}
