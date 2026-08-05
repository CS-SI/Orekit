/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.VersionedITRF;
import org.orekit.gnss.IGSUtils;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

public class OrbexParserTest {

    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("gnss");
    }

    @Test
    public void testSimple() {

        final Orbex orbex = load("/sinex/orbex/simple.obx");

        final TimeScale gps = TimeScalesFactory.getGPS();

        Assertions.assertEquals(0.09,                        orbex.getVersion(), 1.0e-6);
        Assertions.assertEquals("EXAMPLE LEO ORBIT",         orbex.getDescription().description());
        Assertions.assertEquals("Dr. P. Caspian, Narnia AC", orbex.getDescription().createdBy());
        Assertions.assertEquals(new AbsoluteDate(2010, 2, 8, 12, 0, 0, gps), orbex.getCreationDate());
        Assertions.assertEquals("p",                         orbex.getDescription().inputData());
        Assertions.assertEquals("pc@igsac.narnia.gov",       orbex.getDescription().contact());
        Assertions.assertEquals(PredefinedTimeSystem.GPS,    orbex.getDescription().timeSystem());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29, 0, 0, 0, gps), orbex.getFileEpochStartTime());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29, 0, 0, 2, gps), orbex.getFileEpochEndTime());
        Assertions.assertTrue(Double.isNaN(orbex.getDescription().epochInterval()));
        Assertions.assertEquals(ITRFVersion.ITRF_2000,       ((VersionedITRF) orbex.getDescription().coordinateSystem()).getITRFVersion());
        Assertions.assertEquals("ECEF",                      orbex.getDescription().frameType());
        Assertions.assertEquals("FIT",                       orbex.getDescription().orbitType());
        Assertions.assertEquals(1,                           orbex.getDescription().recordTypes().size());
        Assertions.assertEquals(EphemerisDataPredicate.POS,  orbex.getDescription().recordTypes().getFirst());
        Assertions.assertEquals("m",                         orbex.getDescription().positionUnit().getName());
        Assertions.assertEquals("CENTER-OF-MASS",            orbex.getDescription().orbitReference());

        Assertions.assertEquals(1, orbex.getData().size());
        final Data data = orbex.getData().get(new SatInSystem("L06"));
        Assertions.assertEquals("CHAMP", data.description());
        Assertions.assertEquals(3, data.orbit().size());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29, 0, 0, 0, gps),
                                data.orbit().get(0).getDate());
        TestUtils.validateVector3D(new Vector3D(1781848.9098, 5968846.1797, -2704551.4098),
                                   data.orbit().get(0).getPosition(),
                                   1.0e-4);
        TestUtils.validateVector3D(Vector3D.ZERO, data.orbit().get(0).getVelocity(), 1.0e-15);
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29, 0, 0, 1.000000000001, gps),
                                data.orbit().get(1).getDate());
        TestUtils.validateVector3D(new Vector3D(1727998.7897, 5780000.6581, -3119210.3412),
                                   data.orbit().get(1).getPosition(),
                                   1.0e-4);
        TestUtils.validateVector3D(Vector3D.ZERO, data.orbit().get(1).getVelocity(), 1.0e-15);
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29, 0, 0, 2.000000000003, gps),
                                data.orbit().get(2).getDate());
        TestUtils.validateVector3D(new Vector3D(1664504.1705, 5565312.9920, -3519546.7577),
                                   data.orbit().get(2).getPosition(),
                                   1.0e-4);
        TestUtils.validateVector3D(Vector3D.ZERO, data.orbit().get(2).getVelocity(), 1.0e-15);

    }

    private Orbex load(final String name) {
        return new OrbexParser(IGSUtils::guessFrame,
                               PredefinedTimeSystem::parseTimeSystem,
                               TimeScalesFactory.getTimeScales()).
               parse(new DataSource(name, () -> OrbexParserTest.class.getResourceAsStream(name)));
    }

}
