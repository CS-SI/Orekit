/* Copyright 2002-2015 CS Systèmes d'Information
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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.UnsupportedAddressTypeException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.orekit.errors.OrekitException;


/** Helper class for loading data files from a zip/jar archive.

 * <p>
 * This class browses all entries in a zip/jar archive in filesystem or in classpath.
 * </p>
 * <p>
 * The organization of entries within the archive is unspecified. All entries are
 * checked in turn. If several entries of the archive are supported by the data
 * loader, all of them will be loaded.
 * </p>
 * <p>
 * Gzip-compressed files are supported.
 * </p>
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for
 * zip entries browsing.
 * </p>
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public class ZipJarCrawler implements DataProvider {

    /** Zip archive on the filesystem. */
    private final File file;

    /** Zip archive in the classpath. */
    private final String resource;

    /** Class loader to use. */
    private final ClassLoader classLoader;

    /** Zip archive on network. */
    private final URL url;

    /** Prefix name of the zip. */
    private final String name;

    /** Build a zip crawler for an archive file on filesystem.
     * @param file zip file to browse
     */
    public ZipJarCrawler(final File file) {
        this.file        = file;
        this.resource    = null;
        this.classLoader = null;
        this.url         = null;
        this.name        = file.getAbsolutePath();
    }

    /** Build a zip crawler for an archive file in classpath.
     * <p>
     * Calling this constructor has the same effect as calling
     * {@link #ZipJarCrawler(ClassLoader, String)} with
     * {@code ZipJarCrawler.class.getClassLoader()} as first
     * argument.
     * </p>
     * @param resource name of the zip file to browse
     * @exception OrekitException if resource name is malformed
     */
    public ZipJarCrawler(final String resource) throws OrekitException {
        this(ZipJarCrawler.class.getClassLoader(), resource);
    }

    /** Build a zip crawler for an archive file in classpath.
     * @param classLoader class loader to use to retrieve the resources
     * @param resource name of the zip file to browse
     * @exception OrekitException if resource name is malformed
     */
    public ZipJarCrawler(final ClassLoader classLoader, final String resource)
        throws OrekitException {
        try {
            this.file        = null;
            this.resource    = resource;
            this.classLoader = classLoader;
            this.url         = null;
            this.name        = classLoader.getResource(resource).toURI().toString();
        } catch (URISyntaxException use) {
            throw new OrekitException(use, LocalizedFormats.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /** Build a zip crawler for an archive file on network.
     * @param url URL of the zip file on network
     * @exception OrekitException if url syntax is malformed
     */
    public ZipJarCrawler(final URL url) throws OrekitException {
        try {
            this.file        = null;
            this.resource    = null;
            this.classLoader = null;
            this.url         = url;
            this.name        = url.toURI().toString();
        } catch (URISyntaxException use) {
            throw new OrekitException(use, LocalizedFormats.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported, final DataLoader visitor)
        throws OrekitException {

        try {

            // open the raw data stream
            Archive archive = null;;
            try {
                if (file != null) {
                    archive = new Archive(new FileInputStream(file));
                } else if (resource != null) {
                    archive = new Archive(classLoader.getResourceAsStream(resource));
                } else {
                    archive = new Archive(url.openConnection().getInputStream());
                }

                return feed(name, supported, visitor, archive);

            } finally {
                if (archive != null) {
                    archive.close();
                }
            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        } catch (ParseException pe) {
            throw new OrekitException(pe, new DummyLocalizable(pe.getMessage()));
        }

    }

    /** Feed a data file loader by browsing the entries in a zip/jar.
     * @param prefix prefix to use for name
     * @param supported pattern for file names supported by the visitor
     * @param visitor data file visitor to use
     * @param archive archive to read
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     * @return true if something has been loaded
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private boolean feed(final String prefix, final Pattern supported,
                         final DataLoader visitor, final Archive archive)
        throws OrekitException, IOException, ParseException {

        OrekitException delayedException = null;
        boolean loaded = false;

        // loop over all entries
        for (final Archive.EntryStream entry : archive) {

            try {

                if (visitor.stillAcceptsData() && !entry.isDirectory()) {

                    final String fullName = prefix + "!" + entry.getName();

                    if (ZIP_ARCHIVE_PATTERN.matcher(entry.getName()).matches()) {

                        // recurse inside the archive entry
                        loaded = feed(fullName, supported, visitor, new Archive(entry)) || loaded;

                    } else {

                        // remove leading directories
                        String entryName = entry.getName();
                        final int lastSlash = entryName.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            entryName = entryName.substring(lastSlash + 1);
                        }

                        // remove suffix from gzip entries
                        final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(entryName);
                        final String baseName = gzipMatcher.matches() ? gzipMatcher.group(1) : entryName;

                        if (supported.matcher(baseName).matches()) {

                            // visit the current entry
                            final InputStream stream =
                                gzipMatcher.matches() ? new GZIPInputStream(entry) : entry;
                            visitor.loadData(stream, fullName);
                            stream.close();
                            loaded = true;

                        }

                    }

                }

            } catch (OrekitException oe) {
                delayedException = oe;
            }

            entry.close();

        }

        if (!loaded && delayedException != null) {
            throw delayedException;
        }
        return loaded;

    }

    /** Local class wrapping a zip archive. */
    private static final class Archive implements Closeable, Iterable<Archive.EntryStream> {

        /** Zip stream. */
        private final ZipInputStream zip;

        /** Next entry. */
        private EntryStream next;

        /** Simple constructor.
         * @param rawStream raw stream
         * @exception IOException if first entry cannot be retrieved
         */
        Archive(final InputStream rawStream) throws IOException {
            zip = new ZipInputStream(rawStream);
            goToNext();
        }

        /** Go to next entry.
        * @exception IOException if next entry cannot be retrieved
         */
        private void goToNext() throws IOException {
            final ZipEntry ze = zip.getNextEntry();
            if (ze == null) {
                next = null;
            } else {
                next = new EntryStream(ze.getName(), ze.isDirectory());
            }
        }

        /** {@inheritDoc} */
        @Override
        public Iterator<Archive.EntryStream> iterator() {
            return new Iterator<EntryStream> () {

                /** {@inheritDoc} */
                @Override
                public boolean hasNext() {
                    return next != null;
                }

                /** {@inheritDoc} */
                @Override
                public EntryStream next() throws NoSuchElementException {
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    return next;
                }

                /** {@inheritDoc} */
                @Override
                public void remove() {
                    // this part is never called
                    throw new UnsupportedAddressTypeException();
                }

            };
        }

        /** {@inheritDoc} */
        @Override
        public void close() throws IOException {
            zip.close();
        }

        /** Archive entry. */
        public class EntryStream extends InputStream {

            /** Name of the entry. */
            private final String name;

            /** Directory indicator. */
            private boolean isDirectory;

            /** Indicator for already closed stream. */
            private boolean closed;

            /** Simple constructor.
             * @param name name of the entry
             * @param isDirectory if true, the entry is a directory
             */
            EntryStream(final String name, final boolean isDirectory) {
                this.name        = name;
                this.isDirectory = isDirectory;
                this.closed      = false;
            }

            /** Get the name of the entry.
             * @return name of the entry
             */
            public String getName() {
                return name;
            }

            /** Check if the entry is a directory.
             * @return true if the entry is a directory
             */
            public boolean isDirectory() {
                return isDirectory;
            }

            /** {@inheritDoc} */
            @Override
            public int read() throws IOException {
                // delegate read to global input stream
                return zip.read();
            }

            /** {@inheritDoc} */
            @Override
            public void close() throws IOException {
                if (!closed) {
                    zip.closeEntry();
                    goToNext();
                    closed = true;
                }
            }

        }

    }

}
