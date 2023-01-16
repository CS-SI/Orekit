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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CasterRecordTest {

    private static String PRODUCTS   = "CAS;products.igs-ip.net;2101;PRODUCTS;BKG;0;DEU;50.12;8.69;0.0.0.0;0;http://products.igs-ip.net/home";
    private static String RTCM_NTRIP = "CAS;rtcm-ntrip.org;2101;NtripInfoCaster;BKG;0;DEU;50.12;8.69;0.0.0.0;0;http://www.rtcm-ntrip.org/home";

    @Test
    public void testPRODUCTS() {
        final CasterRecord cas = new CasterRecord(PRODUCTS);
        Assertions.assertEquals(RecordType.CAS,                    cas.getRecordType());
        Assertions.assertEquals("products.igs-ip.net",             cas.getHostOrIPAddress());
        Assertions.assertEquals(2101,                              cas.getPort());
        Assertions.assertEquals("PRODUCTS",                        cas.getSourceIdentifier());
        Assertions.assertEquals("BKG",                             cas.getOperator());
        Assertions.assertEquals(false,                             cas.canReceiveNMEA());
        Assertions.assertEquals("DEU",                             cas.getCountry());
        Assertions.assertEquals(50.12,                             FastMath.toDegrees(cas.getLatitude()),  1.0e-15);
        Assertions.assertEquals( 8.69,                             FastMath.toDegrees(cas.getLongitude()), 1.0e-15);
        Assertions.assertEquals("0.0.0.0",                         cas.getFallbackHostOrIPAddress());
        Assertions.assertEquals(0,                                 cas.getFallbackPort());
        Assertions.assertEquals("http://products.igs-ip.net/home", cas.getMisc());
    }

    @Test
    public void testRTCM_NTRIP() {
        final CasterRecord cas = new CasterRecord(RTCM_NTRIP);
        Assertions.assertEquals(RecordType.CAS,                    cas.getRecordType());
        Assertions.assertEquals("rtcm-ntrip.org",                  cas.getHostOrIPAddress());
        Assertions.assertEquals(2101,                              cas.getPort());
        Assertions.assertEquals("NtripInfoCaster",                 cas.getSourceIdentifier());
        Assertions.assertEquals("BKG",                             cas.getOperator());
        Assertions.assertEquals(false,                             cas.canReceiveNMEA());
        Assertions.assertEquals("DEU",                             cas.getCountry());
        Assertions.assertEquals(50.12,                             FastMath.toDegrees(cas.getLatitude()),  1.0e-15);
        Assertions.assertEquals( 8.69,                             FastMath.toDegrees(cas.getLongitude()), 1.0e-15);
        Assertions.assertEquals("0.0.0.0",                         cas.getFallbackHostOrIPAddress());
        Assertions.assertEquals(0,                                 cas.getFallbackPort());
        Assertions.assertEquals("http://www.rtcm-ntrip.org/home",  cas.getMisc());
    }

    @Test
    public void testReceiveNMEA() {
        final CasterRecord cas = new CasterRecord(PRODUCTS.replace(";BKG;0;DEU;", ";BKG;1;DEU;"));
        Assertions.assertTrue(cas.canReceiveNMEA());
    }

    @Test
    public void testEmbeddedSemiColon() {
        final CasterRecord cas = new CasterRecord(PRODUCTS.replace("http://products.igs-ip.net/home", "aa\";\"bb\";\"cc"));
        Assertions.assertEquals("aa;bb;cc", cas.getMisc());
    }

}
