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

import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;

/** Interface for providing data files to {@link DataLoader file loaders}.
 * <p>
 * This interface defines a generic way to explore some collection holding
 * data files and load some of them. The collection may be a list of resources
 * in the classpath, a directories tree in filesystem, a zip or jar archive,
 * a database, a connexion to a remote server ...
 * </p>
 * <p>
 * The proper way to use this interface is to configure one or more
 * implementations and register them in the {@link DataProvidersManager data
 * providers manager singleton}, or to let this manager use its default
 * configuration. Once registered, they will be used automatically whenever
 * some data needs to be loaded. This allow high level applications developers
 * to customize Orekit data loading mechanism and get a tighter intergation of
 * the library within their application.
 * </p>
 * @see DataLoader
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public interface DataProvider {

    /** Pattern for name of gzip files. */
    Pattern GZIP_FILE_PATTERN = Pattern.compile("(.*)\\.gz$");

    /** Pattern for name of zip/jar archives. */
    Pattern ZIP_ARCHIVE_PATTERN = Pattern.compile("(.*)(?:(?:\\.zip)|(?:\\.jar))$");

    /** Feed a data file loader by browsing the data collection.
     * <p>
     * The method crawls all files referenced in the instance (for example
     * all files in a directories tree) and for each file supported by the
     * file loader it asks the file loader to load it.
     * </p>
     * <p>
     * If the method completes without exception, then the data loader
     * is considered to have been fed successfully and the top level
     * {@link DataProvidersManager data providers manager} will return
     * immediately without attempting to use the next configured providers.
     * </p>
     * <p>
     * If the method completes abruptly with an exception, then the top level
     * {@link DataProvidersManager data providers manager} will try to use
     * the next configured providers, in case another one can feed the
     * {@link DataLoader data loader}.
     * </p>
     * @param supported pattern for file names supported by the visitor
     * @param visitor data file visitor to use
     * @return true if some data has been loaded
     * @exception OrekitException if the data loader cannot be fed
     * (read error ...)
     */
    boolean feed(Pattern supported, DataLoader visitor)
        throws OrekitException;

}
