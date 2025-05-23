/* Copyright 2002-2025 CS GROUP
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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.LocalizedCoreFormats;
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
 * All {@link FiltersManager#addFilter(DataFilter) registered}
 * {@link DataFilter filters} are applied.
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
     */
    public ZipJarCrawler(final String resource) {
        this(ZipJarCrawler.class.getClassLoader(), resource);
    }

    /** Build a zip crawler for an archive file in classpath.
     * @param classLoader class loader to use to retrieve the resources
     * @param resource name of the zip file to browse
     */
    public ZipJarCrawler(final ClassLoader classLoader, final String resource) {
        try {
            this.file        = null;
            this.resource    = resource;
            this.classLoader = classLoader;
            this.url         = null;
            this.name        = classLoader.getResource(resource).toURI().toString();
        } catch (URISyntaxException use) {
            throw new OrekitException(use, LocalizedCoreFormats.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /** Build a zip crawler for an archive file on network.
     * @param url URL of the zip file on network
     */
    public ZipJarCrawler(final URL url) {
        try {
            this.file        = null;
            this.resource    = null;
            this.classLoader = null;
            this.url         = url;
            this.name        = url.toURI().toString();
        } catch (URISyntaxException use) {
            throw new OrekitException(use, LocalizedCoreFormats.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported,
                        final DataLoader visitor,
                        final DataProvidersManager manager) {

        try {

            // open the raw data stream
            try (InputStream in = openStream();
                 Archive archive = new Archive(in)) {
                return feed(name, supported, visitor, manager, archive);
            }

        } catch (IOException | ParseException e) {
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }

    }

    /**
     * Open a stream to the raw archive.
     *
     * @return an open stream.
     * @throws IOException if the stream could not be opened.
     */
    private InputStream openStream() throws IOException {
        if (file != null) {
            return new FileInputStream(file);
        } else if (resource != null) {
            return classLoader.getResourceAsStream(resource);
        } else {
            return url.openConnection().getInputStream();
        }
    }

    /** Feed a data file loader by browsing the entries in a zip/jar.
     * @param prefix prefix to use for name
     * @param supported pattern for file names supported by the visitor
     * @param visitor data file visitor to use
     * @param manager used for filtering data.
     * @param archive archive to read
     * @return true if something has been loaded
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private boolean feed(final String prefix,
                         final Pattern supported,
                         final DataLoader visitor,
                         final DataProvidersManager manager,
                         final Archive archive)
        throws IOException, ParseException {

        OrekitException delayedException = null;
        boolean loaded = false;

        // loop over all entries
        for (final Archive.EntryStream entry : archive) {

            try {

                if (visitor.stillAcceptsData() && !entry.isDirectory()) {

                    final String fullName = prefix + "!/" + entry.getName();

                    if (ZIP_ARCHIVE_PATTERN.matcher(entry.getName()).matches()) {

                        // recurse inside the archive entry
                        loaded = feed(fullName, supported, visitor, manager, new Archive(entry)) || loaded;

                    } else {

                        // remove leading directories
                        String entryName = entry.getName();
                        final int lastSlash = entryName.lastIndexOf('/');
                        if (lastSlash >= 0) {
                            entryName = entryName.substring(lastSlash + 1);
                        }

                        // apply all registered filters
                        DataSource data = new DataSource(entryName, () -> entry);
                        data = manager.getFiltersManager().applyRelevantFilters(data);

                        if (supported.matcher(data.getName()).matches()) {
                            // visit the current file
                            try (InputStream input = data.getOpener().openStreamOnce()) {
                                visitor.loadData(input, fullName);
                                loaded = true;
                            }
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
                        // this should never happen
                        throw new NoSuchElementException();
                    }
                    return next;
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

            @Override
            public int available() throws IOException {
                return zip.available();
            }

            @Override
            public int read(final byte[] b, final int off, final int len)
                    throws IOException {
                return zip.read(b, off, len);
            }

            @Override
            public long skip(final long n) throws IOException {
                return zip.skip(n);
            }

            @Override
            public boolean markSupported() {
                return zip.markSupported();
            }

            @Override
            public void mark(final int readlimit) {
                zip.mark(readlimit);
            }

            @Override
            public void reset() throws IOException {
                zip.reset();
            }

            @Override
            public int read(final byte[] b) throws IOException {
                return zip.read(b);
            }

        }

    }

}
