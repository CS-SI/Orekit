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
package fr.cs.examples;

import java.io.File;
import java.net.URL;

import org.orekit.data.DataDirectoryCrawler;

/** Utility class for configuring the library for tutorials runs.
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class Autoconfiguration {

    /** This is a utility class so its constructor is private.
     */
    private Autoconfiguration() {
    }

    /** Configure the library.
     * <p>Several configuration attempts are used here. They have been
     * chosen in order to simplify running the tutorials in several
     * environments: in the development environment, or in a user home
     * environment, or in a system-wide configured environment.
     *   <ul>
     *     <li>use a ".orekit-data" directory in user home directory</li>
     *     <li>use a ".orekit-data" directory in current directory</li>
     *     <li>use a "orekit-data" directory in user home directory</li>
     *     <li>use a "orekit-data" directory in current directory</li>
     *     <li>use "orekit-data.jar" if it is in the classpath</li>
     *     <li>use the "regular-data" directory from the test resources</li>
     *   </ul>
     * </p>
     */
    public static void configureOrekit() {
        final File home    = new File(System.getProperty("user.home"));
        final File current = new File(System.getProperty("user.dir"));
        if (tryFilesystem(new File(home, ".orekit-data"))) {
            return;
        }
        if (tryFilesystem(new File(current, ".orekit-data"))) {
            return;
        }
        if (tryFilesystem(new File(home, "orekit-data"))) {
            return;
        }
        if (tryFilesystem(new File(current, "orekit-data"))) {
            return;
        }
        if (tryClasspath("/orekit-data")) {
            return;
        }
        if (tryClasspath("regular-data")) {
            return;
        }
        throw new RuntimeException("Orekit autoconfiguration failed.");
    }

    /** Try to use a directory from filesystem.
     * @param directory directory to try
     * @return true if directory was found (and configured)
     */
    private static boolean tryFilesystem(final File directory) {
        if (directory.exists() && directory.isDirectory()) {
            System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, directory.getAbsolutePath());
            System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "");
            return true;
        }
        return false;
    }

    /** Try to use a directory from classpath.
     * @param directory directory to try
     * @return true if directory was found (and configured)
     */
    private static boolean tryClasspath(final String directory) {
        final URL url = Autoconfiguration.class.getClassLoader().getResource(directory);
        if (url != null) {
            System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
            System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, directory);
            return true;
        }
        return false;
    }

}
