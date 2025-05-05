/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.gnss;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SatInSystemTest {

    @Test
    public void testRegularConstructor() {
        SatInSystem sis = new SatInSystem(SatelliteSystem.GALILEO, 12);
        Assertions.assertEquals(SatelliteSystem.GALILEO, sis.getSystem());
        Assertions.assertEquals(12, sis.getPRN());
    }

    @Test
    public void testAnyPRN() {
        SatInSystem sis = new SatInSystem(SatelliteSystem.QZSS, SatInSystem.ANY_PRN);
        Assertions.assertEquals(SatelliteSystem.QZSS, sis.getSystem());
        Assertions.assertEquals(SatInSystem.ANY_PRN, sis.getPRN());
    }

    @Test
    public void testBeidou() {
        Assertions.assertEquals(SatelliteSystem.BEIDOU, new SatInSystem("C 7").getSystem());
        Assertions.assertEquals(7, new SatInSystem("C 7").getPRN());
    }

    @Test
    public void testGalileo() {
        Assertions.assertEquals(SatelliteSystem.GALILEO, new SatInSystem("E11").getSystem());
        Assertions.assertEquals(11, new SatInSystem("E11").getPRN());
    }

    @Test
    public void testGlonass() {
        Assertions.assertEquals(SatelliteSystem.GLONASS, new SatInSystem("R05").getSystem());
        Assertions.assertEquals(5, new SatInSystem("R05").getPRN());
    }

    @Test
    public void testGps() {
        Assertions.assertEquals(SatelliteSystem.GPS, new SatInSystem("G01").getSystem());
        Assertions.assertEquals(1, new SatInSystem("G01").getPRN());
    }

    @Test
    public void testNavIC() {
        Assertions.assertEquals(SatelliteSystem.NAVIC, new SatInSystem("I14").getSystem());
        Assertions.assertEquals(14, new SatInSystem("I14").getPRN());
    }

    @Test
    public void testQzss() {
        Assertions.assertEquals(SatelliteSystem.QZSS, new SatInSystem("J03").getSystem());
        Assertions.assertEquals(195, new SatInSystem("J03").getPRN());
    }

    @Test
    public void testSbas() {
        Assertions.assertEquals(SatelliteSystem.SBAS, new SatInSystem("S09").getSystem());
        Assertions.assertEquals(109, new SatInSystem("S09").getPRN());
    }

    @Test
    public void testUserDefined() {
        Assertions.assertEquals(SatelliteSystem.USER_DEFINED_K, new SatInSystem("K01").getSystem());
        Assertions.assertEquals(1, new SatInSystem("K01").getPRN());
    }

    @Test
    public void testParseAny() {
        Assertions.assertEquals(SatelliteSystem.GALILEO, new SatInSystem("E  ").getSystem());
        Assertions.assertEquals(SatInSystem.ANY_PRN,     new SatInSystem("E  ").getPRN());
        Assertions.assertEquals(SatelliteSystem.BEIDOU,  new SatInSystem("C").getSystem());
        Assertions.assertEquals(SatInSystem.ANY_PRN,     new SatInSystem("C").getPRN());
    }

    @Test
    public void testHashcode() {
        Assertions.assertEquals(78, new SatInSystem("E11").hashCode());
    }

    @Test
    public void testEquals() {
        Assertions.assertEquals(new SatInSystem(SatelliteSystem.GALILEO, 11), new SatInSystem("E11"));
        Assertions.assertNotEquals(new SatInSystem(SatelliteSystem.GALILEO, 12), new SatInSystem("E11"));
        Assertions.assertNotEquals(new SatInSystem(SatelliteSystem.GPS, 11), new SatInSystem("E11"));
        Assertions.assertNotEquals(new SatInSystem(SatelliteSystem.GALILEO, 11), "E11");
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("E01", new SatInSystem(SatelliteSystem.GALILEO, 1).toString());
        Assertions.assertEquals("E  ", new SatInSystem(SatelliteSystem.GALILEO, SatInSystem.ANY_PRN).toString());
        Assertions.assertEquals("S10", new SatInSystem(SatelliteSystem.SBAS, 110).toString());
        Assertions.assertEquals("J07", new SatInSystem(SatelliteSystem.QZSS, 199).toString());
    }

}
