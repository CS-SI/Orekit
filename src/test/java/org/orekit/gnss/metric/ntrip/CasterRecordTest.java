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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasterRecordTest {

    private static String PRODUCTS   = "CAS;products.igs-ip.net;2101;PRODUCTS;BKG;0;DEU;50.12;8.69;0.0.0.0;0;http://products.igs-ip.net/home";
    private static String RTCM_NTRIP = "CAS;rtcm-ntrip.org;2101;NtripInfoCaster;BKG;0;DEU;50.12;8.69;0.0.0.0;0;http://www.rtcm-ntrip.org/home";

    @Test
    void testPRODUCTS() {
        final CasterRecord cas = new CasterRecord(PRODUCTS);
        assertEquals(RecordType.CAS,                    cas.getRecordType());
        assertEquals("products.igs-ip.net",             cas.getHostOrIPAddress());
        assertEquals(2101,                              cas.getPort());
        assertEquals("PRODUCTS",                        cas.getSourceIdentifier());
        assertEquals("BKG",                             cas.getOperator());
        assertFalse(cas.canReceiveNMEA());
        assertEquals("DEU",                             cas.getCountry());
        assertEquals(50.12,                             FastMath.toDegrees(cas.getLatitude()),  1.0e-15);
        assertEquals( 8.69,                             FastMath.toDegrees(cas.getLongitude()), 1.0e-15);
        assertEquals("0.0.0.0",                         cas.getFallbackHostOrIPAddress());
        assertEquals(0,                                 cas.getFallbackPort());
        assertEquals("http://products.igs-ip.net/home", cas.getMisc());
    }

    @Test
    void testRTCM_NTRIP() {
        final CasterRecord cas = new CasterRecord(RTCM_NTRIP);
        assertEquals(RecordType.CAS,                    cas.getRecordType());
        assertEquals("rtcm-ntrip.org",                  cas.getHostOrIPAddress());
        assertEquals(2101,                              cas.getPort());
        assertEquals("NtripInfoCaster",                 cas.getSourceIdentifier());
        assertEquals("BKG",                             cas.getOperator());
        assertFalse(cas.canReceiveNMEA());
        assertEquals("DEU",                             cas.getCountry());
        assertEquals(50.12,                             FastMath.toDegrees(cas.getLatitude()),  1.0e-15);
        assertEquals( 8.69,                             FastMath.toDegrees(cas.getLongitude()), 1.0e-15);
        assertEquals("0.0.0.0",                         cas.getFallbackHostOrIPAddress());
        assertEquals(0,                                 cas.getFallbackPort());
        assertEquals("http://www.rtcm-ntrip.org/home",  cas.getMisc());
    }

    @Test
    void testReceiveNMEA() {
        final CasterRecord cas = new CasterRecord(PRODUCTS.replace(";BKG;0;DEU;", ";BKG;1;DEU;"));
        assertTrue(cas.canReceiveNMEA());
    }

    @Test
    void testEmbeddedSemiColon() {
        final CasterRecord cas = new CasterRecord(PRODUCTS.replace("http://products.igs-ip.net/home", "aa\";\"bb\";\"cc"));
        assertEquals("aa;bb;cc", cas.getMisc());
    }

}
