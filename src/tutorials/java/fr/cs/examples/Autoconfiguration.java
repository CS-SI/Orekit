/* Copyright 2002-2013 CS Systèmes d'Information
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
package fr.cs.examples;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.orekit.data.DataProvidersManager;

/** Utility class for configuring the library for tutorials runs.
 * @author Luc Maisonobe
 */
public class Autoconfiguration {

    /** This is a utility class so its constructor is private.
     */
    private Autoconfiguration() {
    }

    /** Configure the library.
     * <p>Several configuration components are used here. They have been
     * chosen in order to simplify running the tutorials in either a
     * user home or local environment or in the development environment.
     *   <ul>
     *     <li>use a "orekit-data.zip" directory in current directory</li>
     *     <li>use a "orekit-data" directory in current directory</li>
     *     <li>use a ".orekit-data" directory in current directory</li>
     *     <li>use a "orekit-data.zip" directory in user home directory</li>
     *     <li>use a "orekit-data" directory in user home directory</li>
     *     <li>use a ".orekit-data" directory in user home directory</li>
     *     <li>use the "regular-data" directory from the test resources</li>
     *   </ul>
     * </p>
     */
    public static void configureOrekit() {
        final File home    = new File(System.getProperty("user.home"));
        final File current = new File(System.getProperty("user.dir"));
        StringBuffer pathBuffer = new StringBuffer();
        appendIfExists(pathBuffer, new File(current, "orekit-data.zip"));
        appendIfExists(pathBuffer, new File(current, "orekit-data"));
        appendIfExists(pathBuffer, new File(current, ".orekit-data"));
        appendIfExists(pathBuffer, new File(home,    "orekit-data.zip"));
        appendIfExists(pathBuffer, new File(home,    "orekit-data"));
        appendIfExists(pathBuffer, new File(home,    ".orekit-data"));
        appendIfExists(pathBuffer, "regular-data");
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, pathBuffer.toString());
    }

    /** Append a directory/zip archive to the path if it exists.
     * @param path placeholder where to put the directory/zip archive
     * @param file file to try
     */
    private static void appendIfExists(final StringBuffer path, final File file) {
        if (file.exists() && (file.isDirectory() || file.getName().endsWith(".zip"))) {
            if (path.length() > 0) {
                path.append(System.getProperty("path.separator"));
            }
            path.append(file.getAbsolutePath());
        }
    }

    /** Append a classpath-related directory to the path if the directory exists.
     * @param path placeholder where to put the directory
     * @param directory directory to try
     */
    private static void appendIfExists(final StringBuffer path, final String directory) {
        try {
            final URL url = Autoconfiguration.class.getClassLoader().getResource(directory);
            if (url != null) {
                if (path.length() > 0) {
                    path.append(System.getProperty("path.separator"));
                }
                path.append(url.toURI().getPath());
            }
        } catch (URISyntaxException use) {
            // display an error message and simply ignore the path
            System.err.println(use.getLocalizedMessage());
        }
    }

}
