/* Copyright 2002-2021 CS GROUP
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
import java.nio.file.Files;
import java.nio.file.Paths;

/** Container associating a name with a stream that can be opened <em>lazily</em>.
 * <p>
 * This association and the lazy-opening are useful in different cases:
 * <ul>
 *   <li>when {@link DirectoryCrawler crawling} a directory tree to select data
 *   to be loaded by a {@link DataLoader}, the files that are not meaningful for
 *   the loader can be ignored and not opened at all</li>
 *   <li>when {@link DataFilter data filtering} is used, the raw stream can
 *   be opened by the filter only if the upper level filtered stream is opened</li>
 *   <li>when opening a stream for loading the data it provides, the opening
 *   and closing actions can be grouped in Orekit internal code using a {@code try
 *   with resources} clause so closing is done properly even in case of exception</li>
 *   <li>if some pre-reading of the first few bytes are needed to decide how to
 *   load data (as in {@link org.orekit.files.ccsds.utils.lexical.LexicalAnalyzerSelector}),
 *   then the stream can be opened, buffered and rewound and a fake open method used
 *   to return the already open stream so a {@code try with resources} clause
 *   elsewhere works properly for closing the stream</li>
 * </ul>
 * </p>
 * <p>
 * Beware that the purpose of this class is only to delay this opening (or not open
 * the stream at all), it is <em>not</em> intended to open the stream several time.
 * Some implementations may fail if the {@link #getStreamOpener() stream opener}'s
 * {@link StreamOpener#openOnce() openStream} method is called several times.
 * This is particularly true in network-based streams.
 * </p>
 * <p>
 * This class is a simple container without any processing methods.
 * </p>
 * @see DataFilter
 * @author Luc Maisonobe
 * @since 9.2
 */
public class DataSource {

    /** Name of the data (file name, zip entry name...). */
    private final String name;

    /** Supplier for data stream. */
    private final StreamOpener streamOpener;

    /** Complete constructor.
     * @param name data name
     * @param streamOpener opener for the data stream
     */
    public DataSource(final String name, final StreamOpener streamOpener) {
        this.name         = name;
        this.streamOpener = streamOpener;
    }

    /** Build an instance from file name only.
     * @param fileName name of the file
     * @since 11.0
     */
    public DataSource(final String fileName) {
        this(fileName, () -> Files.newInputStream(Paths.get(fileName)));
    }

    /** Build an instance from a file on the local file system.
     * @param file file
     * @since 11.0
     */
    public DataSource(final File file) {
        this(file.getName(), () -> new FileInputStream(file));
    }

    /** Get the name of the data.
     * @return name of the data
     */
    public String getName() {
        return name;
    }

    /** Get the data stream opener.
     * @return data stream opener
     */
    public StreamOpener getStreamOpener() {
        return streamOpener;
    }

    /** Interface for lazy-opening a stream one time. */
    public interface StreamOpener {
        /** Open the stream once.
         * <p>
         * Beware that this interface is only intended for <em>lazy</em> opening a
         * stream, i.e. to delay this opening (or not open the stream at all).
         * It is <em>not</em> intended to open the stream several times. Some
         * implementations may fail if an attempt to open a stream several
         * times is made. This is particularly true in network-based streams.
         * </p>
         * @return opened stream
         * @exception IOException if stream cannot be opened
         */
        InputStream openOnce() throws IOException;

    }

}

