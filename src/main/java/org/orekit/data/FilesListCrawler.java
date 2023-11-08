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


/** Provider for data files in an explicit list.
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for
 * list browsing.
 * </p>
 * @see DataProvidersManager
 * @since 10.1
 * @author Luc Maisonobe
 */
public class FilesListCrawler extends AbstractListCrawler<File> {

    /** Build a data classpath crawler.
     * <p>The default timeout is set to 10 seconds.</p>
     * @param inputs list of input files
     */
    public FilesListCrawler(final File... inputs) {
        super(inputs);
    }

    /** {@inheritDoc} */
    @Override
    protected String getCompleteName(final File input) {
        return input.getPath();
    }

    /** {@inheritDoc} */
    @Override
    protected String getBaseName(final File input) {
        return input.getName();
    }

    /** {@inheritDoc} */
    @Override
    protected ZipJarCrawler getZipJarCrawler(final File input) {
        return new ZipJarCrawler(input);
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream getStream(final File input) throws IOException {
        return new FileInputStream(input);
    }

}
