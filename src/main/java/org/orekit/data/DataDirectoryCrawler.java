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
 * This class handles data files recursively starting from a root tree
 * specified by the java property <code>orekit.data.directory</code>.
 * If the property is not set or is null, no data will be available to the
 * library (for example no pole correction will be applied and only predefined
 * UTC steps will be taken into account). No errors will be triggered.
 * If the property is set, it must correspond to an existing root tree otherwise
 * an error will be triggered.
 * <p>
 * <p>
 * The organization of files in the tree is free, files are found by matching
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

    /** Name of the property defining the data root directory. */
    public static final String DATA_ROOT_DIRECTORY = "orekit.data.directory";

    /** IERS root hierarchy root. */
    private File root;

    /** Build an IERS files crawler.
     * @exception OrekitException if some data is missing or can't be read
     */
    public DataDirectoryCrawler() throws OrekitException {

        // check the root tree
        final String directoryName = System.getProperty(DATA_ROOT_DIRECTORY);
        if ((directoryName != null) && !"".equals(directoryName)) {

            // try to find the root directory either in filesystem or in classpath
            // (filesystem having higher priority)
            root = new File(directoryName);
            if (!(root.exists() && root.isDirectory())) {
                // not found in filesystem, try in classpath
                final URL url = getClass().getClassLoader().getResource(directoryName);
                if (url != null) {
                    root = new File(url.getPath());
                }
            }

            // safety checks
            if (!root.exists()) {
                throw new OrekitException("IERS root directory {0} does not exist",
                                          new Object[] {
                                              root.getAbsolutePath()
                                          });
            }
            if (!root.isDirectory()) {
                throw new OrekitException("{0} is not a directory",
                                          new Object[] {
                                              root.getAbsolutePath()
                                          });
            }

        }

    }

    /** Crawl the data root hierarchy.
     * @param visitor data file visitor to use
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     */
    public void crawl(final DataFileCrawler visitor) throws OrekitException {
        if (root != null) {
            crawl(visitor, root);
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
