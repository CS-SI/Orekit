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
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

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
 * All {@link FiltersManager#addFilter(DataFilter) registered}
 * {@link DataFilter filters} are applied.
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
public class NetworkCrawler extends AbstractListCrawler<URL> {

    /** Connection timeout (milliseconds). */
    private int timeout;

    /** Build a data classpath crawler.
     * <p>The default timeout is set to 10 seconds.</p>
     * @param inputs list of input file URLs
     */
    public NetworkCrawler(final URL... inputs) {
        super(inputs);
        timeout = 10000;
    }

    /** Set the timeout for connection.
     * @param timeout connection timeout in milliseconds
     */
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    /** {@inheritDoc} */
    @Override
    protected String getCompleteName(final URL input) {
        try {
            return input.toURI().toString();
        } catch (URISyntaxException ue) {
            throw new OrekitException(ue, new DummyLocalizable(ue.getMessage()));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected String getBaseName(final URL input) {
        return new File(input.getPath()).getName();
    }

    /** {@inheritDoc} */
    @Override
    protected ZipJarCrawler getZipJarCrawler(final URL input) {
        return new ZipJarCrawler(input);
    }

    /** {@inheritDoc} */
    @Override
    protected InputStream getStream(final URL input) throws IOException {
        final URLConnection connection = input.openConnection();
        connection.setConnectTimeout(timeout);
        return connection.getInputStream();
    }

}
