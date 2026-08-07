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

import org.hipparchus.exception.Localizable;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.VersionedITRF;
import org.orekit.gnss.IGSUtils;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;

import java.util.List;

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

    @Test
    public void testFinalsIGS() {

        final Orbex orbex = load("/sinex/orbex/finals-igs.obx");

        final TimeScale gps = TimeScalesFactory.getGPS();

        Assertions.assertEquals(0.09,                                          orbex.getVersion(), 1.0e-6);
        Assertions.assertEquals("IGS FINAL GNSS ORBIT COMBINATION",            orbex.getDescription().description());
        Assertions.assertEquals("IGS Analysis Center Coordinator",             orbex.getDescription().createdBy());
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 21, 12,  0, 0, gps), orbex.getCreationDate());
        Assertions.assertEquals("ORBIT",                                       orbex.getDescription().inputData());
        Assertions.assertEquals("Jim.Ray@noaa.gov",                            orbex.getDescription().contact());
        Assertions.assertEquals(PredefinedTimeSystem.GPS,                      orbex.getDescription().timeSystem());
        Assertions.assertEquals(new AbsoluteDate(2009, 4,  7,  0,  0, 0, gps), orbex.getFileEpochStartTime());
        Assertions.assertEquals(new AbsoluteDate(2009, 4,  7, 23, 45, 0, gps), orbex.getFileEpochEndTime());
        Assertions.assertEquals(900.0,                                         orbex.getDescription().epochInterval(), 1.0e-12);
        Assertions.assertEquals(ITRFVersion.ITRF_2005,                         ((VersionedITRF) orbex.getDescription().coordinateSystem()).getITRFVersion());
        Assertions.assertEquals("ECEF",                                        orbex.getDescription().frameType());
        Assertions.assertEquals("HLM",                                         orbex.getDescription().orbitType());
        Assertions.assertEquals(1,                                             orbex.getDescription().recordTypes().size());
        Assertions.assertEquals(EphemerisDataPredicate.PCS,                    orbex.getDescription().recordTypes().getFirst());
        Assertions.assertEquals("m",                                           orbex.getDescription().positionUnit().getName());
        Assertions.assertEquals("CENTER-OF-MASS",                              orbex.getDescription().orbitReference());
        Assertions.assertEquals("µs",                                          orbex.getDescription().clockCorrectionUnit().getName());

        Assertions.assertEquals(8, orbex.getData().size());
        Assertions.assertEquals("GPS BLOCK IIR-M", orbex.getData().get(new SatInSystem("G01")).description());
        Assertions.assertEquals("GPS BLOCK IIR-B", orbex.getData().get(new SatInSystem("G02")).description());
        Assertions.assertEquals("GPS BLOCK IIA",   orbex.getData().get(new SatInSystem("G03")).description());
        Assertions.assertEquals("GPS BLOCK IIA",   orbex.getData().get(new SatInSystem("G04")).description());
        Assertions.assertEquals("GLONASS-M",       orbex.getData().get(new SatInSystem("R21")).description());
        Assertions.assertEquals("GLONASS-M",       orbex.getData().get(new SatInSystem("R22")).description());
        Assertions.assertEquals("GLONASS-M",       orbex.getData().get(new SatInSystem("R23")).description());
        Assertions.assertEquals("GLONASS-M",       orbex.getData().get(new SatInSystem("R24")).description());

        final Data g04Data = orbex.getData().get(new SatInSystem("G04"));
        Assertions.assertEquals(2, g04Data.orbit().size());
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 7,  0,  0,  0.0, gps),
                                g04Data.orbit().get(0).getDate());
        TestUtils.validateVector3D(new Vector3D(-12801678.7490, 10532088.7120, 20686320.3260),
                                   g04Data.orbit().get(0).getPosition(),
                                   1.0e-4);
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 7, 23, 45, 0.0, gps),
                                g04Data.orbit().get(1).getDate());
        TestUtils.validateVector3D(new Vector3D(-11249295.1630, 11376440.6380, 21110255.5670),
                                   g04Data.orbit().get(1).getPosition(),
                                   1.0e-4);
        Assertions.assertEquals(2, g04Data.clock().size());
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 7,  0,  0,  0.0, gps),
                                g04Data.clock().get(0).getDate());
        Assertions.assertEquals(-41.6910930e-6, g04Data.clock().get(0).getBias(), 1.0e-13);
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 7, 23, 45, 0.0, gps),
                                g04Data.clock().get(1).getDate());
        Assertions.assertEquals(-42.9485550e-6, g04Data.clock().get(1).getBias(), 1.0e-13);
        Assertions.assertTrue(g04Data.attitude().isEmpty());
    }

    @Test
    public void testPosVelClkCrtAtt() {

        final Orbex orbex = load("/sinex/orbex/pos-vel-clk-crt-att.obx");

        final TimeScale gps = TimeScalesFactory.getGPS();

        Assertions.assertEquals(0.09,                                           orbex.getVersion(), 1.0e-6);
        Assertions.assertEquals("EXAMPLE GPS + LEO ORBIT",                      orbex.getDescription().description());
        Assertions.assertEquals("Dr. P. Caspian",                               orbex.getDescription().createdBy());
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 21, 12,  0, 0, gps),  orbex.getCreationDate());
        Assertions.assertEquals("d+p",                                          orbex.getDescription().inputData());
        Assertions.assertEquals("pc@igsac.narnia.gov",                          orbex.getDescription().contact());
        Assertions.assertEquals(PredefinedTimeSystem.GPS,                       orbex.getDescription().timeSystem());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29,  0,  0, 0, gps), orbex.getFileEpochStartTime());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29, 23, 45, 0, gps), orbex.getFileEpochEndTime());
        Assertions.assertTrue(Double.isNaN(orbex.getDescription().epochInterval()));
        Assertions.assertEquals(ITRFVersion.ITRF_2005,                          ((VersionedITRF) orbex.getDescription().coordinateSystem()).getITRFVersion());
        Assertions.assertEquals("ECEF",                                         orbex.getDescription().frameType());
        Assertions.assertEquals("FIT",                                          orbex.getDescription().orbitType());
        Assertions.assertEquals(5,                                              orbex.getDescription().recordTypes().size());
        Assertions.assertEquals(EphemerisDataPredicate.POS,                     orbex.getDescription().recordTypes().get(0));
        Assertions.assertEquals(EphemerisDataPredicate.VEL,                     orbex.getDescription().recordTypes().get(1));
        Assertions.assertEquals(EphemerisDataPredicate.CLK,                     orbex.getDescription().recordTypes().get(2));
        Assertions.assertEquals(EphemerisDataPredicate.CRT,                     orbex.getDescription().recordTypes().get(3));
        Assertions.assertEquals(EphemerisDataPredicate.ATT,                     orbex.getDescription().recordTypes().get(4));
        Assertions.assertEquals("m",                                            orbex.getDescription().positionUnit().getName());
        Assertions.assertEquals("CENTER-OF-MASS",                               orbex.getDescription().orbitReference());
        Assertions.assertEquals("m/s",                                          orbex.getDescription().velocityUnit().getName());
        Assertions.assertEquals("µs",                                           orbex.getDescription().clockCorrectionUnit().getName());

        Assertions.assertEquals(3, orbex.getData().size());
        Assertions.assertEquals("GPS BLOCK IIR-B", orbex.getData().get(new SatInSystem("G02")).description());
        Assertions.assertEquals("GPS BLOCK IIA",   orbex.getData().get(new SatInSystem("G03")).description());
        Assertions.assertEquals("CHAMP",           orbex.getData().get(new SatInSystem("L06")).description());

        final Data g02Data = orbex.getData().get(new SatInSystem("G02"));
        Assertions.assertEquals(2, g02Data.orbit().size());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29,  0,  0,  0.0, gps),
                                g02Data.orbit().getFirst().getDate());
        TestUtils.validateVector3D(new Vector3D(4049646.6140, 25594715.4960, -5815946.7980),
                                   g02Data.orbit().getFirst().getPosition(),
                                   1.0e-4);
        TestUtils.validateVector3D(new Vector3D(-353.5783, 821.0842, 2972.7179),
                                   g02Data.orbit().getFirst().getVelocity(),
                                   1.0e-4);
        Assertions.assertEquals(2, g02Data.clock().size());
        Assertions.assertEquals(new AbsoluteDate(2002, 12, 29,  0,  0,  0.0, gps),
                                g02Data.clock().getFirst().getDate());
        Assertions.assertEquals(-39.2268190e-6, g02Data.clock().getFirst().getBias(), 1.0e-13);

        final List<AngularCoordinates> att06 = orbex.getData().get(new SatInSystem("L06")).attitude();
        Assertions.assertEquals(4, att06.size());
        TestUtils.validateRotation(new Rotation( 0.9164178227001020,
                                                 0.3553674926002010,
                                                 0.1624720204001450,
                                                -0.0865746035002370,
                                                true),
                                   att06.get(0).getRotation().revert(),
                                   1.0e-15);
        TestUtils.validateRotation(new Rotation( 0.9264178234567890,
                                                 0.3653674934567890,
                                                 0.1724720345678901,
                                                -0.0965746045678901,
                                                true),
                                   att06.get(1).getRotation().revert(),
                                   1.0e-15);
        TestUtils.validateRotation(new Rotation( 0.9364178245678901,
                                                 0.3753674945678901,
                                                 0.1824720456789012,
                                                -0.1165746056789012,
                                                true),
                                   att06.get(2).getRotation().revert(),
                                   1.0e-15);
        TestUtils.validateRotation(new Rotation(-0.5066930256001020,
                                                -0.2289786888002010,
                                                 0.7772033941001450,
                                                -0.2945943349002370,
                                                true),
                                   att06.get(3).getRotation().revert(),
                                   1.0e-15);

    }

    @Test
    public void testUltraRapid() {

        final Orbex orbex = load("/sinex/orbex/ultra-rapid-igs.obx");

        final TimeScale gps = TimeScalesFactory.getGPS();

        Assertions.assertEquals(0.09,                                          orbex.getVersion(), 1.0e-6);
        Assertions.assertEquals("IGS ULTRA-RAPID ORBIT COMBINATION 15262_06",  orbex.getDescription().description());
        Assertions.assertEquals("IGS Analysis Center Coordinator",             orbex.getDescription().createdBy());
        Assertions.assertEquals(new AbsoluteDate(2009, 4,  7,  9,  0, 0, gps), orbex.getCreationDate());
        Assertions.assertEquals("ORBIT",                                       orbex.getDescription().inputData());
        Assertions.assertEquals("Jim.Ray@noaa.gov",                            orbex.getDescription().contact());
        Assertions.assertEquals(PredefinedTimeSystem.GPS,                      orbex.getDescription().timeSystem());
        Assertions.assertEquals(new AbsoluteDate(2009, 4,  6,  6,  0, 0, gps), orbex.getFileEpochStartTime());
        Assertions.assertEquals(new AbsoluteDate(2009, 4,  8,  5, 45, 0, gps), orbex.getFileEpochEndTime());
        Assertions.assertEquals(900.0,                                         orbex.getDescription().epochInterval(), 1.0e-12);
        Assertions.assertEquals(ITRFVersion.ITRF_2005,                         ((VersionedITRF) orbex.getDescription().coordinateSystem()).getITRFVersion());
        Assertions.assertEquals("ECEF",                                        orbex.getDescription().frameType());
        Assertions.assertEquals("HLM",                                         orbex.getDescription().orbitType());
        Assertions.assertEquals(1,                                             orbex.getDescription().recordTypes().size());
        Assertions.assertEquals(EphemerisDataPredicate.PCS,                    orbex.getDescription().recordTypes().getFirst());
        Assertions.assertEquals("m",                                           orbex.getDescription().positionUnit().getName());
        Assertions.assertEquals("CENTER-OF-MASS",                              orbex.getDescription().orbitReference());
        Assertions.assertEquals("µs",                                          orbex.getDescription().clockCorrectionUnit().getName());

        Assertions.assertEquals(7, orbex.getData().size());
        Assertions.assertEquals("BLOCK IIR-B", orbex.getData().get(new SatInSystem("G02")).description());
        Assertions.assertEquals("BLOCK IIA",   orbex.getData().get(new SatInSystem("G03")).description());
        Assertions.assertEquals("BLOCK IIA",   orbex.getData().get(new SatInSystem("G04")).description());
        Assertions.assertEquals("BLOCK IIR-M", orbex.getData().get(new SatInSystem("G29")).description());
        Assertions.assertEquals("BLOCK IIA",   orbex.getData().get(new SatInSystem("G30")).description());
        Assertions.assertEquals("BLOCK IIR-M", orbex.getData().get(new SatInSystem("G31")).description());
        Assertions.assertEquals("BLOCK IIA",   orbex.getData().get(new SatInSystem("G32")).description());

        final Data g30Data = orbex.getData().get(new SatInSystem("G30"));
        Assertions.assertEquals(2, g30Data.orbit().size());
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 6,  6,  0,  0.0, gps),
                                g30Data.orbit().get(0).getDate());
        TestUtils.validateVector3D(new Vector3D(-4952232.8610, 15715395.1680, -21178818.5590),
                                   g30Data.orbit().get(0).getPosition(),
                                   1.0e-4);
        Assertions.assertEquals(2, g30Data.clock().size());
        Assertions.assertEquals(new AbsoluteDate(2009, 4, 6,  6,  0,  0.0, gps),
                                g30Data.clock().get(0).getDate());
        Assertions.assertEquals(137.2970520e-6, g30Data.clock().get(0).getBias(), 1.0e-13);
    }

    @Test
    public void testMissingUnit() {
        doTryParseFail("/sinex/orbex/missing-unit.obx",
                       OrekitMessages.MISSING_ORBEX_UNIT, 1, 62, 0, "VEL");
    }

    @Test
    public void testUnknownRecordType() {
        doTryParseFail("/sinex/orbex/unknown-record-type.obx",
                       OrekitMessages.UNEXPECTED_DATA_AT_LINE_IN_FILE, 0, 17, -1, null);
    }

    @Test
    public void testWrongColumnsNumber1() {
        doTryParseFail("/sinex/orbex/wrong-columns-number-1.obx",
                       OrekitMessages.ORBEX_WRONG_COLUMNS, 0, 67, 3, "VEL");
    }

    @Test
    public void testWrongColumnsNumber2() {
        doTryParseFail("/sinex/orbex/wrong-columns-number-2.obx",
                       OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 0, 65, -1, null);
    }

    @Test
    public void testWrongCreationDate1() {
        doTryParseFail("/sinex/orbex/wrong-creation-date-1.obx",
                       OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 0, 6, -1, null);
    }

    @Test
    public void testWrongCreationDate2() {
        doTryParseFail("/sinex/orbex/wrong-creation-date-2.obx",
                       OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, 0, 6, -1, null);
    }

    @Test
    public void testDuplicatedId() {
        doTryParseFail("/sinex/orbex/duplicated-id.obx",
                       OrekitMessages.DUPLICATED_SATELLITE, 1, 28, 0, "G03");
    }

    @Test
    public void testIncompleteData() {
        doTryParseFail("/sinex/orbex/incomplete-data.obx",
                       OrekitMessages.INCOMPLETE_ORBEX_DATA, 3, 77, 0, "G03");
    }

    @Test
    public void testInvalidSatId() {
        doTryParseFail("/sinex/orbex/invalid-sat-id.obx",
                       OrekitMessages.INVALID_SATELLITE_ID, -1, -1, 0, "G99");
    }

    private void doTryParseFail(final String name, final Localizable specifier,
                                final int linePart, final int lineNumber,
                                final int fieldPart, final String field) {
        try {
            load(name);
            Assertions.fail("an exception should have been thrown");
        } catch (final OrekitException oe) {
            Assertions.assertEquals(specifier, oe.getSpecifier());
            if (linePart > 0) {
                Assertions.assertEquals(lineNumber, (Integer) oe.getParts()[linePart]);
            }
            if (fieldPart > 0) {
                Assertions.assertEquals(field, oe.getParts()[fieldPart]);
            }
        }
    }

    private Orbex load(final String name) {
        return new OrbexParser(IGSUtils::guessFrame,
                               PredefinedTimeSystem::parseTimeSystem,
                               TimeScalesFactory.getTimeScales()).
               parse(new DataSource(name, () -> OrbexParserTest.class.getResourceAsStream(name)));
    }

}
