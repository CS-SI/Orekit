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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Container associating a name with a stream or reader that can be opened <em>lazily</em>.
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
 *   <li>if some pre-reading of the first few bytes or characters are needed to decide how to
 *   load data (as in {@link org.orekit.files.ccsds.utils.lexical.LexicalAnalyzerSelector}),
 *   then the stream can be opened, buffered and rewound and a fake open method used
 *   to return the already open stream so a {@code try with resources} clause
 *   elsewhere works properly for closing the stream</li>
 * </ul>
 * <p>
 * Beware that the purpose of this class is only to delay this opening (or not open
 * the stream or reader at all), it is <em>not</em> intended to open the stream several
 * times and <em>not</em> intended to open both the binary stream and the characters reader.
 * Some implementations may fail if the {@link #getOpener() opener}'s
 * {@link Opener#openStreamOnce() openStreamOnce} or {@link Opener#openReaderOnce() openReaderOnce}
 * methods are called several times or are both called separately. This is particularly
 * true for network-based streams.
 * </p>
 * @see DataFilter
 * @author Luc Maisonobe
 * @since 9.2
 */
public class DataSource {

    /** Name of the data (file name, zip entry name...). */
    private final String name;

    /** Supplier for data stream. */
    private final Opener opener;

    /** Complete constructor.
     * @param name data name
     * @param streamOpener opener for the data stream
     */
    public DataSource(final String name, final StreamOpener streamOpener) {
        this.name   = name;
        this.opener = new BinaryBasedOpener(streamOpener);
    }

    /** Complete constructor.
     * @param name data name
     * @param readerOpener opener for characters reader
     */
    public DataSource(final String name, final ReaderOpener readerOpener) {
        this.name   = name;
        this.opener = new ReaderBasedOpener(readerOpener);
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

    /** Build an instance from URI only.
     * @param uri URI of the file
     * @since 11.0
     */
    public DataSource(final URI uri) {
        this(Paths.get(uri).toFile());
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
    public Opener getOpener() {
        return opener;
    }

    /** Interface for lazy-opening a binary stream one time. */
    public interface StreamOpener {
        /** Open the stream once.
         * <p>
         * Beware that this interface is only intended for <em>lazy</em> opening a
         * stream, i.e. to delay this opening (or not open the stream at all).
         * It is <em>not</em> intended to open the stream several times. Some
         * implementations may fail if an attempt to open a stream several
         * times is made. This is particularly true for network-based streams.
         * </p>
         * @return opened stream
         * @exception IOException if stream cannot be opened
         */
        InputStream openOnce() throws IOException;

    }

    /** Interface for lazy-opening a characters stream one time. */
    public interface ReaderOpener {
        /** Open the stream once.
         * <p>
         * Beware that this interface is only intended for <em>lazy</em> opening a
         * stream, i.e. to delay this opening (or not open the stream at all).
         * It is <em>not</em> intended to open the stream several times. Some
         * implementations may fail if an attempt to open a stream several
         * times is made. This is particularly true for network-based streams.
         * </p>
         * @return opened stream
         * @exception IOException if stream cannot be opened
         */
        Reader openOnce() throws IOException;

    }

    /** Interface for lazy-opening data streams one time. */
    public interface Opener {

        /** Check if the raw data is binary.
         * <p>
         * The raw data may be either binary or characters. In both cases,
         * either {@link #openStreamOnce()} or {@link #openReaderOnce()} may
         * be called, but one will be more efficient than the other as one
         * will supply data as is and the other one will convert raw data
         * before providing it. If conversion is needed, it will also be done
         * using {@link StandardCharsets#UTF_8 UTF8 encoding}, which may not
         * be suitable. This method helps the data consumer to either choose
         * the more efficient method or avoid wrong encoding conversion.
         * </p>
         * @return true if raw data is binary, false if raw data is characters
         */
        boolean rawDataIsBinary();

        /** Open a bytes stream once.
         * <p>
         * Beware that this interface is only intended for <em>lazy</em> opening a
         * stream, i.e. to delay this opening (or not open the stream at all).
         * It is <em>not</em> intended to open the stream several times and not
         * intended to open both the {@link #openStreamOnce() binary stream} and
         * the {@link #openReaderOnce() characters stream} separately (but opening
         * the reader may be implemented by opening the binary stream or vice-versa).
         * Implementations may fail if an attempt to open a stream several times is
         * made. This is particularly true for network-based streams.
         * </p>
         * @return opened stream or null if there are no data streams at all
         * @exception IOException if stream cannot be opened
         */
        InputStream openStreamOnce() throws IOException;

        /** Open a characters stream reader once.
         * <p>
         * Beware that this interface is only intended for <em>lazy</em> opening a
         * stream, i.e. to delay this opening (or not open the stream at all).
         * It is <em>not</em> intended to open the stream several times and not
         * intended to open both the {@link #openStreamOnce() binary stream} and
         * the {@link #openReaderOnce() characters stream} separately (but opening
         * the reader may be implemented by opening the binary stream or vice-versa).
         * Implementations may fail if an attempt to open a stream several times is
         * made. This is particularly true for network-based streams.
         * </p>
         * @return opened reader or null if there are no data streams at all
         * @exception IOException if stream cannot be opened
         */
        Reader openReaderOnce() throws IOException;

    }

    /** Opener based on a binary stream. */
    private static class BinaryBasedOpener implements Opener {

        /** Opener for the data stream. */
        private final StreamOpener streamOpener;

        /** Simple constructor.
         * @param streamOpener opener for the data stream
         */
        BinaryBasedOpener(final StreamOpener streamOpener) {
            this.streamOpener = streamOpener;
        }

        /** {@inheritDoc} */
        @Override
        public boolean rawDataIsBinary() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public InputStream openStreamOnce() throws IOException {
            return streamOpener.openOnce();
        }

        /** {@inheritDoc} */
        @Override
        public Reader openReaderOnce() throws IOException {
            // convert bytes to characters
            final InputStream is = openStreamOnce();
            return (is == null) ? null : new InputStreamReader(is, StandardCharsets.UTF_8);
        }

    }

    /** Opener based on a reader. */
    private static class ReaderBasedOpener implements Opener {

        /** Size of the characters buffer. */
        private static final int BUFFER_SIZE = 4096;

        /** Opener for characters reader. */
        private final ReaderOpener readerOpener;

        /** Simple constructor.
         * @param readerOpener opener for characters reader
         */
        ReaderBasedOpener(final ReaderOpener readerOpener) {
            this.readerOpener = readerOpener;
        }

        /** {@inheritDoc} */
        @Override
        public boolean rawDataIsBinary() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public InputStream openStreamOnce() throws IOException {

            // open the underlying reader
            final Reader reader = openReaderOnce();
            if (reader == null) {
                return null;
            }

            // set up a stream that convert characters to bytes
            return new InputStream() {

                private ByteBuffer buffer = null;

                /** {@inheritDoc} */
                @Override
                public int read() throws IOException {
                    if (buffer == null || !buffer.hasRemaining()) {
                        // we need to refill the array

                        // get characters from the reader
                        final CharBuffer cb = CharBuffer.allocate(BUFFER_SIZE);
                        final int read = reader.read(cb);
                        if (read < 0) {
                            // end of data
                            return read;
                        }

                        // convert the characters read into bytes
                        final int last = cb.position();
                        cb.rewind();
                        buffer = StandardCharsets.UTF_8.encode(cb.subSequence(0, last));

                    }

                    // return next byte
                    return buffer.get();

                }

            };
        }

        /** {@inheritDoc} */
        @Override
        public Reader openReaderOnce() throws IOException {
            return readerOpener.openOnce();
        }

    }

}

