/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

class NtripClientTest {

    @Test
    void testProxy() {
        NtripClient client = new NtripClient("ntrip.example.org", NtripClient.DEFAULT_PORT);
        client.setProxy(Proxy.Type.SOCKS, "localhost", 1080);
        assertEquals(Proxy.Type.SOCKS, client.getProxy().type());
        assertEquals("localhost", ((InetSocketAddress) client.getProxy().address()).getHostName());
        assertEquals(1080, ((InetSocketAddress) client.getProxy().address()).getPort());
    }

    @Test
    void testUnknownProxy() {
        final String nonExistant = "socks.invalid";
        try {
            NtripClient client = new NtripClient("ntrip.example.org", NtripClient.DEFAULT_PORT);
            client.setProxy(Proxy.Type.SOCKS, nonExistant, 1080);
            fail("an exception should have been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNKNOWN_HOST, me.getSpecifier());
            assertEquals(nonExistant, me.getParts()[0]);
        }
    }

    @Test
    void testUnknownCaster() {
        final String nonExistant = "caster.invalid";
        try {
            NtripClient client = new NtripClient(nonExistant, NtripClient.DEFAULT_PORT);
            client.setTimeout(100);
            client.getSourceTable();
            fail("an exception should have been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.CANNOT_PARSE_SOURCETABLE, me.getSpecifier());
            assertEquals(nonExistant, me.getParts()[0]);
        }
    }

    @Test
    void testWrongContentType1() {
        try {
            DummyServer server = prepareServer("/gnss/ntrip/wrong-content-type.txt");
            server.run();
            NtripClient client = new NtripClient("localhost", server.getServerPort());
            client.setTimeout(500);
            client.getSourceTable();
            fail("an exception should have been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNEXPECTED_CONTENT_TYPE, me.getSpecifier());
            assertEquals("text/html", me.getParts()[0]);
        }
    }

    @Test
    void testWrongContentType2() {
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
            fail("an exception should have been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.UNEXPECTED_CONTENT_TYPE, me.getSpecifier());
            assertEquals("text/html", me.getParts()[0]);
        }
    }

    @Test
    void testOtherResponseCode() {
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
            fail("an exception should have been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.CONNECTION_ERROR, me.getSpecifier());
            assertEquals("localhost", me.getParts()[0]);
            assertEquals("Gone", me.getParts()[1]);
        }
    }

    @Test
    void testWrongRecordType() {
        try {
            DummyServer server = prepareServer("/gnss/ntrip/wrong-record-type.txt");
            server.run();
            NtripClient client = new NtripClient("localhost", server.getServerPort());
            client.setTimeout(500);
            client.getSourceTable();
            fail("an exception should have been thrown");
        } catch (OrekitException me) {
            assertEquals(OrekitMessages.SOURCETABLE_PARSE_ERROR, me.getSpecifier());
            assertEquals("localhost", me.getParts()[0]);
            assertEquals(7,           me.getParts()[1]);
            assertEquals("BCE;CLK01;BRDC_CoM_ITRF;RTCM 3.1;1057(60),1058(5),1059(5),1063(60),1064(5);0;GPS+GLO;MISC;DEU;50.09;8.66;0;1;RTNet;none;B;N;1400;BKG",       me.getParts()[2]);
        }
    }

    @Test
    void testLocalSourceTable() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(500);
        assertEquals("localhost", client.getHost());
        assertEquals(server.getServerPort(), client.getPort());
        SourceTable table = client.getSourceTable();
        assertEquals("st_filter,st_auth,st_match,st_strict,rtsp,plain_rtp", table.getNtripFlags());
        assertEquals( 2, table.getCasters().size());
        assertEquals( 2, table.getNetworks().size());
        assertEquals(42, table.getDataStreams().size());
        assertSame(table, client.getSourceTable());
    }

    @Test
    void testUnknownMessage() throws Exception {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/RTCM3EPH01.dat");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setReconnectParameters(0.001, 2.0, 2);
        final CountingObserver counter = new CountingObserver(m -> true);
        client.addObserver(1042, "RTCM3EPH01", counter);
        client.addObserver(1042, "RTCM3EPH01", new LoggingObserver());
        client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
        server.await(10, TimeUnit.SECONDS);
        // the 31st message causes the exception
        counter.awaitCount(30, 30 * 1000);
        // wait a bit for next message, the 31st
        // better condition would be to wait for StreamMonitor.exception to not be null
        Thread.sleep(1000);
        try {
            client.stopStreaming(100);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNKNOWN_ENCODED_MESSAGE_NUMBER, oe.getSpecifier());
            assertEquals("1046", oe.getParts()[0]);
        }
    }

    @Test
    void testMountPointAlreadyConnected() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/RTCM3EPH01.dat");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setReconnectParameters(0.001, 2.0, 2);
        client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
        try {
            client.startStreaming("RTCM3EPH01", Type.RTCM, false, false);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.MOUNPOINT_ALREADY_CONNECTED, oe.getSpecifier());
            assertEquals("RTCM3EPH01", oe.getParts()[0]);
        }
    }

    @Test
    void testGGA() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/zero-length-response.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setFix(2, 42, 13.456, FastMath.toRadians(43.5), FastMath.toRadians(-1.25), 317.5, 12.2);
        client.startStreaming("", Type.IGS_SSR, true, true);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ie) {
            // ignored
        }
        client.stopStreaming(100);
        assertEquals("$GPGGA,024213.456,4330.0000,N,0115.0000,W,1,04,1.0,317.5,M,12.2,M,,*7A",
                            server.getRequestProperty("Ntrip-GGA"));
    }

    @Test
    void testGGA2() {
        DummyServer server = prepareServer("/gnss/ntrip/sourcetable-products.igs-ip.net.txt",
                                           "/gnss/ntrip/zero-length-response.txt");
        server.run();
        NtripClient client = new NtripClient("localhost", server.getServerPort());
        client.setTimeout(100);
        client.setFix(2, 42, 13.456, FastMath.toRadians(-43.5), FastMath.toRadians(1.25), 317.5, 12.2);
        client.startStreaming("", Type.IGS_SSR, true, true);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ie) {
            // ignored
        }
        client.stopStreaming(100);
        assertEquals("$GPGGA,024213.456,4330.0000,S,0115.0000,E,1,04,1.0,317.5,M,12.2,M,,*75",
                            server.getRequestProperty("Ntrip-GGA"));
    }

    @Test
    void testNullGGA() {
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
            assertEquals(OrekitMessages.STREAM_REQUIRES_NMEA_FIX, oe.getSpecifier());
        }
    }

    @Test
    void testAuthenticationStream() {
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
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.FAILED_AUTHENTICATION, oe.getSpecifier());
            assertEquals("RTCM3EPH01", oe.getParts()[0]);
        }
    }

    @Test
    void testAuthenticationCaster() {
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
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.FAILED_AUTHENTICATION, oe.getSpecifier());
            assertEquals("caster", oe.getParts()[0]);
        }
    }

    @Test
    void testForbiddenRequest() {
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
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.CONNECTION_ERROR, oe.getSpecifier());
            assertEquals("localhost", oe.getParts()[0]);
            assertEquals("Forbidden", oe.getParts()[1]);
        }
    }

    @Test
    void testMissingFlags() {
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
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.MISSING_HEADER, oe.getSpecifier());
            assertEquals("localhost", oe.getParts()[0]);
            assertEquals("Ntrip-Flags", oe.getParts()[1]);
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
            fail(e.getLocalizedMessage());
        }
        return server;
    }

}
