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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.hipparchus.exception.DummyLocalizable;
import org.orekit.errors.OrekitException;


/** Provider for data files directly fetched from network.

 * <p>
 * This class handles a list of URLs pointing to data files or zip/jar on
 * the net. Since the net is not a tree structure the list elements
 * cannot be top elements recursively browsed as in {@link
 * DirectoryCrawler}, they must be data files or zip/jar archives.
 * </p>
 * <p>
 * The files fetched from network can be locally cached on disk. This prevents
 * too frequent network access if the URLs are remote ones (for example
 * original internet URLs).
 * </p>
 * <p>
 * If the URL points to a remote server (typically on the web) on the other side
 * of a proxy server, you need to configure the networking layer of your
 * application to use the proxy. For a typical authenticating proxy as used in
 * many corporate environments, this can be done as follows using for example
 * the AuthenticatorDialog graphical authenticator class that can be found
 * in the tests directories:
 * <pre>
 *   System.setProperty("http.proxyHost",     "proxy.your.domain.com");
 *   System.setProperty("http.proxyPort",     "8080");
 *   System.setProperty("http.nonProxyHosts", "localhost|*.your.domain.com");
 *   Authenticator.setDefault(new AuthenticatorDialog());
 * </pre>
 *
 * <p>
 * Gzip-compressed files are supported.
 * </p>
 * <p>
 * Zip archives entries are supported recursively.
 * </p>
 * <p>
 * This is a simple application of the <code>visitor</code> design pattern for
 * list browsing.
 * </p>
 * @see DataProvidersManager
 * @author Luc Maisonobe
 */
public class NetworkCrawler implements DataProvider {

    /** URLs list. */
    private final List<URL> urls;

    /** Connection timeout (milliseconds). */
    private int timeout;

    /** Build a data classpath crawler.
     * <p>The default timeout is set to 10 seconds.</p>
     * @param urls list of data file URLs
     */
    public NetworkCrawler(final URL... urls) {

        this.urls = new ArrayList<URL>();
        for (final URL url : urls) {
            this.urls.add(url);
        }

        timeout = 10000;

    }

    /** Set the timeout for connection.
     * @param timeout connection timeout in milliseconds
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /** {@inheritDoc} */
    public boolean feed(final Pattern supported, final DataLoader visitor)
        throws OrekitException {

        try {
            OrekitException delayedException = null;
            boolean loaded = false;
            for (URL url : urls) {
                try {

                    if (visitor.stillAcceptsData()) {
                        final String name     = url.toURI().toString();
                        final String fileName = new File(url.getPath()).getName();
                        if (ZIP_ARCHIVE_PATTERN.matcher(fileName).matches()) {

                            // browse inside the zip/jar file
                            new ZipJarCrawler(url).feed(supported, visitor);
                            loaded = true;

                        } else {

                            // remove suffix from gzip files
                            final Matcher gzipMatcher = GZIP_FILE_PATTERN.matcher(fileName);
                            final String baseName = gzipMatcher.matches() ? gzipMatcher.group(1) : fileName;

                            if (supported.matcher(baseName).matches()) {

                                final InputStream stream = getStream(url);

                                // visit the current file
                                if (gzipMatcher.matches()) {
                                    visitor.loadData(new GZIPInputStream(stream), name);
                                } else {
                                    visitor.loadData(stream, name);
                                }

                                stream.close();
                                loaded = true;

                            }

                        }
                    }

                } catch (OrekitException oe) {
                    // maybe the next path component will be able to provide data
                    // wait until all components have been tried
                    delayedException = oe;
                }
            }

            if (!loaded && delayedException != null) {
                throw delayedException;
            }

            return loaded;

        } catch (URISyntaxException | IOException | ParseException e) {
            throw new OrekitException(e, new DummyLocalizable(e.getMessage()));
        }

    }

    /** Get the stream to read from the remote URL.
     * @param url url to read from
     * @return stream to read the content of the URL
     * @throws IOException if the URL cannot be opened for reading
     */
    private InputStream getStream(final URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeout);
        return connection.getInputStream();
    }

}
