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
package org.orekit.gnss.metric.ntrip;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class NtripClientTest {

    private String     proxyHost;
    private int        proxyPort;
    private String     proxyUser;
    private char[]     proxyPassword;
    private String     bkgUser;
    private char[]     bkgPassword;

    @Test
    public void testUnknownProxy() {
        final String nonExistant = "socks.invalid";
        try {
            NtripClient client = new NtripClient("ntrip.example.org", NtripClient.DEFAULT_PORT);
            client.setProxy(Proxy.Type.SOCKS, nonExistant, 1080);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_HOST, me.getSpecifier());
            Assert.assertEquals(nonExistant, me.getParts()[0]);
        }
    }
    
    @Test
    public void testUnknownCaster() {
        final String nonExistant = "caster.invalid";
        try {
            NtripClient client = new NtripClient(nonExistant, NtripClient.DEFAULT_PORT);
            client.setTimeout(100);
            client.getSourceTable();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.CANNOT_PARSE_SOURCETABLE, me.getSpecifier());
            Assert.assertEquals(nonExistant, me.getParts()[0]);
        }
    }

    @Test
    public void testWrongContentType() {
        try {
            DummyServer server = prepareServer("/gnss/ntrip//wrong-content-type.txt");
            server.run();
            NtripClient client = new NtripClient("localhost", server.getServerPort());
            client.setTimeout(500);
            client.getSourceTable();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNEXPECTED_CONTENT_TYPE, me.getSpecifier());
            Assert.assertEquals("text/html", me.getParts()[0]);
        }
    }

    @Test
    public void testLocalSourceTable() {
        DummyServer server = prepareServer("/gnss/ntrip//sourcetable-products.igs-ip.net.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(500);
        Assert.assertEquals("localhost", client.getHost());
        Assert.assertEquals(server.getServerPort(), client.getPort());
        SourceTable table = client.getSourceTable();
        Assert.assertEquals("st_filter,st_auth,st_match,st_strict,rtsp,plain_rtp", table.getNtripFlags());
        Assert.assertEquals( 2, table.getCasters().size());
        Assert.assertEquals( 2, table.getNetworks().size());
        Assert.assertEquals(42, table.getDataStreams().size());
    }

    @Test
    public void testUnknownMessage() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/RTCM3EPH01.dat");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setReconnectParameters(0.001, 2.0, 2);
        CountingObserver observer = new CountingObserver(m -> true);
        client.addObserver(0, "RTCM3EPH01", observer);
        client.startStreaming("RTCM3EPH01", Type.RTCM, false, false, null);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ie) {
            // ignored
        }
        try {
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // ignored
        }
    }

    @Test
    public void testGGA() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/zero-length-response.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setFix(2, 42, 13.456, Math.toRadians(43.5), Math.toRadians(-1.25), 317.5, 12.2);
        client.startStreaming("", Type.IGS_SSR, true, true);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ie) {
            // ignored
        }
        client.stopStreaming(100);
        Assert.assertEquals("$GPGGA,024213.456,4330.0000,N,0115.0000,W,1,04,1.0,317.5,M,12.2,M,,*7A",
                            server.getRequestProperty("Ntrip-GGA"));
    }

    @Before
    public void setUp() {
        try {
                // loading properties
                final Properties properties = new Properties();
                try (InputStream is = this.getClass().getResourceAsStream("/gnss/ntrip/orekit-test-auth.properties")) {
                    properties.load(is);
                }

                // determine proxy type
                final String proxyTypeName = properties.getProperty("orekit.test.proxy.type");
                if (proxyTypeName != null) {
                    proxyHost     = properties.getProperty("orekit.test.proxy.host");
                    proxyPort     = Integer.parseInt(properties.getProperty("orekit.test.proxy.port"));
                    proxyUser     = properties.getProperty("orekit.test.proxy.user");
                    proxyPassword = properties.getProperty("orekit.test.proxy.password").toCharArray();
                    bkgUser       = properties.getProperty("orekit.test.bkg.user");
                    bkgPassword   = properties.getProperty("orekit.test.bkg.password").toCharArray();
                }

                // configure authenticator
                Authenticator.setDefault(new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        if (getRequestingHost().equalsIgnoreCase(proxyHost) &&
                            getRequestingPort() == proxyPort) {
                            // authenticate to proxy
                            return new PasswordAuthentication(proxyUser, proxyPassword);
                        } else {
                            // authenticate to bkg
                            return new PasswordAuthentication(bkgUser, bkgPassword);
                        }
                    }
                });

        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }
    }

    private DummyServer prepareServer(String... names) {
        DummyServer server = null;
        try {
            final String[] fileNames = new String[names.length];
            for (int i = 0; i < names.length; ++i) {
                fileNames[i] = Paths.get(getClass().getResource(names[i]).toURI()).toString();
            }
            server = new DummyServer(fileNames);
        } catch (URISyntaxException | IOException e) {
            Assert.fail(e.getLocalizedMessage());
        }
        return server;
    }


    @After
    public void tearDown() {
        proxyHost     = null;
        proxyPort     = -1;
        proxyUser     = null;
        proxyPassword = null;
        bkgUser       = null;
        bkgPassword   = null;
    }

}
