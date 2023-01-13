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
package org.orekit.gnss.metric.ntrip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NetworkRecordTest {

    private static String IGS        = "NET;IGS;IGS;B;N;https://igs.bkg.bund.de/root_ftp/NTRIP/neteams/neteamlist_igs-ip.htm;https://igs.bkg.bund.de:443/root_ftp/IGS/station/rnxskl/;http://register.rtcm-ntrip.org;none";
    private static String MISC       = "NET;MISC;BKG;B;N;http://igs.bkg.bund.de/root_ftp/NTRIP/neteams/neteamlist_igs-ip.htm;https://igs.bkg.bund.de:443/root_ftp/MISC/station/rnxskl/;http://register.rtcm-ntrip.org;none";

    @Test
    public void testIGS() {
        final NetworkRecord net = new NetworkRecord(IGS);
        Assertions.assertEquals(RecordType.NET,                                                         net.getRecordType());
        Assertions.assertEquals("IGS",                                                                  net.getNetworkIdentifier());
        Assertions.assertEquals("IGS",                                                                  net.getOperator());
        Assertions.assertEquals(Authentication.BASIC,                                                   net.getAuthentication());
        Assertions.assertEquals(false,                                                                  net.areFeesRequired());
        Assertions.assertEquals("https://igs.bkg.bund.de/root_ftp/NTRIP/neteams/neteamlist_igs-ip.htm", net.getNetworkInfoAddress());
        Assertions.assertEquals("https://igs.bkg.bund.de:443/root_ftp/IGS/station/rnxskl/",             net.getStreamInfoAddress());
        Assertions.assertEquals("http://register.rtcm-ntrip.org",                                       net.getRegistrationAddress());
        Assertions.assertEquals("none",                                                                 net.getMisc());
    }

    @Test
    public void testMISC() {
        final NetworkRecord net = new NetworkRecord(MISC);
        Assertions.assertEquals(RecordType.NET,                                                         net.getRecordType());
        Assertions.assertEquals("MISC",                                                                 net.getNetworkIdentifier());
        Assertions.assertEquals("BKG",                                                                  net.getOperator());
        Assertions.assertEquals(Authentication.BASIC,                                                   net.getAuthentication());
        Assertions.assertEquals(false,                                                                  net.areFeesRequired());
        Assertions.assertEquals("http://igs.bkg.bund.de/root_ftp/NTRIP/neteams/neteamlist_igs-ip.htm",  net.getNetworkInfoAddress());
        Assertions.assertEquals("https://igs.bkg.bund.de:443/root_ftp/MISC/station/rnxskl/",            net.getStreamInfoAddress());
        Assertions.assertEquals("http://register.rtcm-ntrip.org",                                       net.getRegistrationAddress());
        Assertions.assertEquals("none",                                                                 net.getMisc());
    }

    @Test
    public void testDigestAuthentication() {
        final NetworkRecord net = new NetworkRecord(IGS.replace(";B;", ";D;"));
        Assertions.assertEquals(Authentication.DIGEST, net.getAuthentication());
    }

    @Test
    public void testNoAuthentication() {
        final NetworkRecord net = new NetworkRecord(IGS.replace(";B;", ";N;"));
        Assertions.assertEquals(Authentication.NONE, net.getAuthentication());
    }

    @Test
    public void testRequiresFees() {
        final NetworkRecord net = new NetworkRecord(IGS.replace(";B;N;", ";B;Y;"));
        Assertions.assertTrue(net.areFeesRequired());
    }

}
