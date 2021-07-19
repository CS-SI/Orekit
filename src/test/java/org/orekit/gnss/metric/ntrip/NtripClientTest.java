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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class NtripClientTest {

    @Test
    public void testProxy() {
        NtripClient client = new NtripClient("ntrip.example.org", NtripClient.DEFAULT_PORT);
        client.setProxy(Proxy.Type.SOCKS, "localhost", 1080);
        Assert.assertEquals(Proxy.Type.SOCKS, client.getProxy().type());
        Assert.assertEquals("localhost", ((InetSocketAddress) client.getProxy().address()).getHostName());
        Assert.assertEquals(1080, ((InetSocketAddress) client.getProxy().address()).getPort());
    }

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
    public void testWrongContentType1() {
        try {
            DummyServer server = prepareServer("/gnss/ntrip/wrong-content-type.txt");
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
    public void testWrongContentType2() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/wrong-content-type.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(500);
        client.setReconnectParameters(0.001, 2.0, 2);
        try {
            client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.UNEXPECTED_CONTENT_TYPE, me.getSpecifier());
            Assert.assertEquals("text/html", me.getParts()[0]);
        }
    }

    @Test
    public void testOtherResponseCode() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/gone.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(500);
        client.setReconnectParameters(0.001, 2.0, 2);
        try {
            client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.CONNECTION_ERROR, me.getSpecifier());
            Assert.assertEquals("localhost", me.getParts()[0]);
            Assert.assertEquals("Gone", me.getParts()[1]);
        }
    }

    @Test
    public void testWrongRecordType() {
        try {
            DummyServer server = prepareServer("/gnss/ntrip/wrong-record-type.txt");
            server.run();
            NtripClient client = new NtripClient("localhost", server.getServerPort());
            client.setTimeout(500);
            client.getSourceTable();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException me) {
            Assert.assertEquals(OrekitMessages.SOURCETABLE_PARSE_ERROR, me.getSpecifier());
            Assert.assertEquals("localhost", me.getParts()[0]);
            Assert.assertEquals(7,           me.getParts()[1]);
            Assert.assertEquals("BCE;CLK01;BRDC_CoM_ITRF;RTCM 3.1;1057(60),1058(5),1059(5),1063(60),1064(5);0;GPS+GLO;MISC;DEU;50.09;8.66;0;1;RTNet;none;B;N;1400;BKG",       me.getParts()[2]);
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
        Assert.assertSame(table, client.getSourceTable());
    }

    @Test
    public void testUnknownMessage() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/RTCM3EPH01.dat");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setReconnectParameters(0.001, 2.0, 2);
        client.addObserver(1042, "RTCM3EPH01", new CountingObserver(m -> true));
        client.addObserver(1042, "RTCM3EPH01", new LoggingObserver());
        client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            // ignored
        }
        try {
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER, oe.getSpecifier());
            Assert.assertEquals("1046", oe.getParts()[0]);
        }
    }

    @Test
    public void testMountPointAlreadyConnected() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/RTCM3EPH01.dat");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setReconnectParameters(0.001, 2.0, 2);
        client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
        try {
            client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MOUNPOINT_ALREADY_CONNECTED, oe.getSpecifier());
            Assert.assertEquals("RTCM3EPH01", oe.getParts()[0]);
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

    @Test
    public void testGGA2() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/zero-length-response.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setFix(2, 42, 13.456, Math.toRadians(-43.5), Math.toRadians(1.25), 317.5, 12.2);
        client.startStreaming("", Type.IGS_SSR, true, true);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ie) {
            // ignored
        }
        client.stopStreaming(100);
        Assert.assertEquals("$GPGGA,024213.456,4330.0000,S,0115.0000,E,1,04,1.0,317.5,M,12.2,M,,*75",
                            server.getRequestProperty("Ntrip-GGA"));
    }

    @Test
    public void testNullGGA() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/zero-length-response.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        try {
            client.startStreaming("", Type.IGS_SSR, true, true);
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.STREAM_REQUIRES_NMEA_FIX, oe.getSpecifier());
        }
    }

    @Test
    public void testAuthenticationStream() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/requires-basic-authentication.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        try {
            client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.FAILED_AUTHENTICATION, oe.getSpecifier());
            Assert.assertEquals("RTCM3EPH01", oe.getParts()[0]);
        }
    }

    @Test
    public void testAuthenticationCaster() {
        DummyServer server = prepareServer("/gnss/ntrip/requires-basic-authentication.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        try {
            client.getSourceTable();
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.FAILED_AUTHENTICATION, oe.getSpecifier());
            Assert.assertEquals("caster", oe.getParts()[0]);
        }
    }

    @Test
    public void testForbiddenRequest() {
        DummyServer server = prepareServer("/gnss/ntrip/forbidden-request.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        try {
            client.getSourceTable();
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CONNECTION_ERROR, oe.getSpecifier());
            Assert.assertEquals("localhost", oe.getParts()[0]);
            Assert.assertEquals("Forbidden", oe.getParts()[1]);
        }
    }

    @Test
    public void testMissingFlags() {
        DummyServer server = prepareServer("/gnss/ntrip/missing-flags.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        try {
            client.getSourceTable();
            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                // ignored
            }
            client.stopStreaming(100);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.MISSING_HEADER, oe.getSpecifier());
            Assert.assertEquals("localhost", oe.getParts()[0]);
            Assert.assertEquals("Ntrip-Flags", oe.getParts()[1]);
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

}
