/* Copyright 2002-2011 CS Communication & Systèmes
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.math.exception.util.DummyLocalizable;
import org.apache.commons.math.exception.util.LocalizedFormats;
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
 * @version $Revision$ $Date$
 */
public class ZipJarCrawler implements DataProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = -6907774815393561820L;

    /** Zip archive on the filesystem. */
    private final File file;

    /** Zip archive in the classpath. */
    private final String resource;

    /** Zip archive on network. */
    private final URL url;

    /** Prefix name of the zip. */
    private final String name;

    /** Build a zip crawler for an archive file on filesystem.
     * @param file zip file to browse
     */
    public ZipJarCrawler(final File file) {
        this.file     = file;
        this.resource = null;
        this.url      = null;
        this.name     = file.getAbsolutePath();
    }

    /** Build a zip crawler for an archive file in classpath.
     * @param resource name of the zip file to browse
     * @exception OrekitException if resource name is malformed
     */
    public ZipJarCrawler(final String resource) throws OrekitException {
        try {
            this.file     = null;
            this.resource = resource;
            this.url      = null;
            this.name     = ZipJarCrawler.class.getClassLoader().getResource(resource).toURI().toString();
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
            this.file     = null;
            this.resource = null;
            this.url      = url;
            this.name     = url.toURI().toString();
        } catch (URISyntaxException use) {
            throw new OrekitException(use, LocalizedFormats.SIMPLE_MESSAGE, use.getMessage());
        }
    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported, final DataLoader visitor)
        throws OrekitException {

        try {

            // open the raw data stream
            final InputStream rawStream;
            if (file != null) {
                rawStream = new FileInputStream(file);
            } else if (resource != null) {
                rawStream = ZipJarCrawler.class.getClassLoader().getResourceAsStream(resource);
            } else {
                rawStream = url.openConnection().getInputStream();
            }

            // add the zip format analysis layer and browse the archive
            final ZipInputStream zip = new ZipInputStream(rawStream);
            final boolean loaded = feed(name, supported, visitor, zip);
            zip.close();

            return loaded;

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
     * @param zip zip/jar input stream
     * @exception OrekitException if some data is missing, duplicated
     * or can't be read
     * @return true if something has been loaded
     * @exception IOException if data cannot be read
     * @exception ParseException if data cannot be read
     */
    private boolean feed(final String prefix, final Pattern supported,
                         final DataLoader visitor, final ZipInputStream zip)
        throws OrekitException, IOException, ParseException {

        OrekitException delayedException = null;
        boolean loaded = false;

        // loop over all entries
        ZipEntry entry = zip.getNextEntry();
        while (entry != null) {

            try {

                if (visitor.stillAcceptsData() && !entry.isDirectory()) {

                    final String fullName = prefix + "!" + entry.getName();

                    if (ZIP_ARCHIVE_PATTERN.matcher(entry.getName()).matches()) {

                        // recurse inside the archive entry
                        loaded = feed(fullName, supported, visitor, new ZipInputStream(zip)) || loaded;

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
                                gzipMatcher.matches() ? new GZIPInputStream(zip) : zip;
                            visitor.loadData(stream, fullName);
                            loaded = true;

                        }

                    }

                }

            } catch (OrekitException oe) {
                delayedException = oe;
            }

            // prepare next entry processing
            zip.closeEntry();
            entry = zip.getNextEntry();

        }

        if (!loaded && delayedException != null) {
            throw delayedException;
        }
        return loaded;

    }

}
