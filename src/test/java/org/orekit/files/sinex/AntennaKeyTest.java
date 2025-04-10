/* Copyright 2002-2025 Thales Alenia Space
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
package org.orekit.files.sinex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AntennaKeyTest {

    private AntennaKey key1;
    private AntennaKey key2;

    @Test
    public void testGetters() {
        Assertions.assertEquals("TRM59800.00",                key1.getName());
        Assertions.assertEquals("SCIS",                       key1.getRadomeCode());
        Assertions.assertEquals("51123",                      key1.getSerialNumber());
        Assertions.assertEquals("AOAD/M_T",                   key2.getName());
        Assertions.assertEquals(AntennaKey.OTHER_RADOME_CODE, key2.getRadomeCode());
        Assertions.assertEquals(AntennaKey.ANY_SERIAL_NUMBER, key2.getSerialNumber());
    }

    @Test
    public void testEquals() {
        Assertions.assertTrue(key1.equals(key1));
        Assertions.assertTrue(key2.equals(key2));
        Assertions.assertFalse(key1.equals(key2));
        Assertions.assertFalse(key1.equals(key1.getName()));
    }

    @Test
    public void testMatchingCandidates() {

        Assertions.assertEquals(4, key1.matchingCandidates().size());
        Assertions.assertSame(key1, key1.matchingCandidates().get(0));
        Assertions.assertNotEquals(key1, key1.matchingCandidates().get(1));
        Assertions.assertNotEquals(key1, key1.matchingCandidates().get(2));
        Assertions.assertNotEquals(key1, key1.matchingCandidates().get(3));
        Assertions.assertEquals(new AntennaKey(key1.getName(), key1.getRadomeCode(), AntennaKey.ANY_SERIAL_NUMBER),
                                key1.matchingCandidates().get(1));
        Assertions.assertEquals(new AntennaKey(key1.getName(), AntennaKey.OTHER_RADOME_CODE, key1.getSerialNumber()),
                                key1.matchingCandidates().get(2));
        Assertions.assertEquals(new AntennaKey(key1.getName(), AntennaKey.OTHER_RADOME_CODE, AntennaKey.ANY_SERIAL_NUMBER),
                                key1.matchingCandidates().get(3));

        Assertions.assertEquals(4, key2.matchingCandidates().size());
        Assertions.assertSame(key2, key2.matchingCandidates().get(0));
        Assertions.assertEquals(key2, key2.matchingCandidates().get(1));
        Assertions.assertEquals(key2, key2.matchingCandidates().get(2));
        Assertions.assertEquals(key2, key2.matchingCandidates().get(3));
        Assertions.assertEquals(new AntennaKey(key2.getName(), key2.getRadomeCode(), AntennaKey.ANY_SERIAL_NUMBER),
                                key2.matchingCandidates().get(1));
        Assertions.assertEquals(new AntennaKey(key2.getName(), AntennaKey.OTHER_RADOME_CODE, key2.getSerialNumber()),
                                key2.matchingCandidates().get(2));
        Assertions.assertEquals(new AntennaKey(key2.getName(), AntennaKey.OTHER_RADOME_CODE, AntennaKey.ANY_SERIAL_NUMBER),
                                key2.matchingCandidates().get(3));

    }

    @Test
    public void testHashcode() {
        Assertions.assertEquals(1567968453, key1.hashCode());
        Assertions.assertEquals( 659789169, key2.hashCode());
    }

    @BeforeEach
    public void setUp() {
        key1 = new AntennaKey("TRM59800.00", "SCIS", "51123");
        key2 = new AntennaKey("AOAD/M_T", AntennaKey.OTHER_RADOME_CODE, AntennaKey.ANY_SERIAL_NUMBER);
    }

}
