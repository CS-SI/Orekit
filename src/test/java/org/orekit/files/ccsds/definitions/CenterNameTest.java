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
package org.orekit.files.ccsds.definitions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.L2Frame;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


class CenterNameTest {

    @Test
    void testSupportedCenters() {
        for (final String name : Arrays.asList("SOLAR_SYSTEM_BARYCENTER", "SUN", "MERCURY", "VENUS",
                                               "EARTH_MOON", "EARTH", "MOON", "MARS", "JUPITER",
                                               "SATURN", "URANUS", "NEPTUNE", "PLUTO")) {
            assertNotNull(CenterName.valueOf(name).getCelestialBody());
        }
    }

    @Test
    void testUnupportedCenters() {
        for (final String name : Arrays.asList("CERES", "SEDNA", "ERIS", "PLANET-9")) {
            try {
                CenterName.valueOf(name);
                fail("an exception should have been thrown");
            } catch (IllegalArgumentException iae) {
                // expected
            }
        }
    }

    @Test
    void testGuess() {
        assertEquals("SATURN",
                            CenterName.guessCenter(CelestialBodyFactory.getSaturn().getBodyOrientedFrame()));
        assertEquals("MERCURY",
                            CenterName.guessCenter(CelestialBodyFactory.getMercury().getInertiallyOrientedFrame()));
        assertEquals("PLANET-X",
                            CenterName.guessCenter(new ModifiedFrame(FramesFactory.getEME2000(), CelestialBodyFrame.EME2000,
                                                                     CelestialBodyFactory.getMars(), "PLANET-X")));
        assertEquals("SOLAR SYSTEM BARYCENTER",
                            CenterName.guessCenter(FramesFactory.getICRF()));
        assertEquals("EARTH",
                            CenterName.guessCenter(Frame.getRoot()));
        assertEquals("EARTH",
                            CenterName.guessCenter(FramesFactory.getTOD(true)));
        assertEquals("UNKNOWN",
                            CenterName.guessCenter(new L2Frame(CelestialBodyFactory.getSun(), CelestialBodyFactory.getEarth())));
    }

    @Test
    void testMap() {
        assertEquals(CenterName.SATURN,
                            CenterName.map(CelestialBodyFactory.getSaturn().getBodyOrientedFrame()));
        assertNull(CenterName.map(new L2Frame(CelestialBodyFactory.getSun(), CelestialBodyFactory.getEarth())));
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
