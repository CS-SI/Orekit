/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.sp3;

import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.VersionedITRF;
import org.orekit.gnss.IGSUtils;
import org.orekit.gnss.TimeSystem;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.AggregatedClockModel;
import org.orekit.time.ClockModel;
import org.orekit.time.ClockOffset;
import org.orekit.time.SampledClockModel;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeSpanMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class SP3ParserTest {

    @Test
    void testParseSP3a1() {
        // simple test for version sp3-a, only contains position entries
        final String    ex     = "/sp3/example-a-1.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals('a', file.getHeader().getVersion());
        assertEquals(SP3OrbitType.FIT, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());
        assertEquals(ITRFVersion.ITRF_1992,
                                ((VersionedITRF) file.getSatellites().get("1").getFrame()).getITRFVersion());

        assertEquals(25, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("1").getSegments().get(0).getCoordinates();
        assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        checkPVEntry(new PVCoordinates(new Vector3D(16258524.75, -3529015.75, -20611427.049),
                                       Vector3D.ZERO),
                     coord);
        assertEquals(-0.0000625406, coord.getClockCorrection(), 1.0e-15);
        assertEquals("NGS", file.getHeader().getAgency());
        assertEquals("ITR92", file.getHeader().getCoordinateSystem());
        assertEquals(1, file.getHeader().getDataUsed().size());
        assertEquals(DataUsed.TWO_RECEIVER_TWO_SATELLITE_CARRIER_PHASE, file.getHeader().getDataUsed().get(0));
        assertEquals(0.0, file.getHeader().getDayFraction(), 1.0e-15);
        assertEquals("1994-12-16T23:59:50.000", file.getHeader().getEpoch().toString(TimeScalesFactory.getUTC()));
        assertEquals(49703, file.getHeader().getModifiedJulianDay());
        assertEquals(3, file.getHeader().getNumberOfEpochs());
        assertEquals(900.0, file.getHeader().getEpochInterval(), 1.0e-15);
        assertEquals(779, file.getHeader().getGpsWeek());
        assertEquals(518400.0, file.getHeader().getSecondsOfWeek(), 1.0e-10);
        assertEquals(25, file.getSatellites().size());
        assertEquals(SP3FileType.UNDEFINED, file.getHeader().getType());
        assertNull(file.getSatellites().get(null));
    }

    @Test
    void testClockModel() {
        // simple test for version sp3-a, only contains position entries
        final String     ex         = "/sp3/gbm18432.sp3.Z";
        final DataSource compressed = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource source     = new UnixCompressFilter().filter(compressed);
        final SP3Parser  parser     = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 6, IGSUtils::guessFrame);
        final SP3        sp3        = parser.parse(source);
        final TimeScale  ts         = sp3.getHeader().getTimeSystem().getTimeScale(TimeScalesFactory.getTimeScales());
        final AggregatedClockModel clockModel = sp3.getEphemeris("C02").extractClockModel();

        assertEquals(3, clockModel.getModels().getSpansNumber());
        assertNull(clockModel.getModels().getFirstSpan().getData());
        assertNull(clockModel.getModels().getLastSpan().getData());
        final ClockModel middle = clockModel.getModels().getFirstSpan().next().getData();
        assertEquals(0.0,
                                new AbsoluteDate(2015, 5, 5, 0, 0, 0.0, ts).durationFrom(middle.getValidityStart()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2015, 5, 5, 23, 55, 0.0, ts).durationFrom(middle.getValidityEnd()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2015, 5, 5, 0, 0, 0.0, ts).durationFrom(clockModel.getValidityStart()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2015, 5, 5, 23, 55, 0.0, ts).durationFrom(clockModel.getValidityEnd()),
                                1.0e-15);

        // points exactly on files entries
        assertEquals(-9.16573060e-4,
                                clockModel.getOffset(new AbsoluteDate(2015, 5, 5, 0, 10, 0.0, ts)).getOffset(),
                                1.0e-16);
        assertEquals(-9.16566535e-4,
                                clockModel.getOffset(new AbsoluteDate(2015, 5, 5, 0, 15, 0.0, ts)).getOffset(),
                                1.0e-16);

        // intermediate point
        final ClockOffset co = clockModel.getOffset(new AbsoluteDate(2015, 5, 5, 0, 12, 5.25, ts));
        assertEquals(-9.16570332e-04, co.getOffset(),       1.0e-12);
        assertEquals( 2.17288913e-11, co.getRate(),         1.0e-19);
        assertEquals(-4.31319472e-16, co.getAcceleration(), 1.0e-24);

    }

    @Test
    void testParseSP3a2() {
        // simple test for version sp3-a, contains p/v entries
        final String    ex     = "/sp3/example-a-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals('a', file.getHeader().getVersion());
        assertEquals(SP3OrbitType.FIT, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());
        assertEquals(CartesianDerivativesFilter.USE_PV, file.getHeader().getFilter());

        assertEquals(25, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("1").getSegments().get(0).getCoordinates();
        assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        // V 1  -6560.373522  25605.954994  -9460.427179     -0.024236
        checkPVEntry(new PVCoordinates(new Vector3D(16258524.75, -3529015.75, -20611427.049),
                                       new Vector3D(-656.0373, 2560.5954, -946.0427)),
                     coord);
        assertEquals(-0.0000625406, coord.getClockCorrection(), 1.0e-15);
        assertEquals(-0.0000000000024236, coord.getClockRateChange(), 1.0e-15);
    }

    @Test
    void testParseSP3c1() {
        // simple test for version sp3-c, contains p entries
        final String    ex     = "/sp3/example-c-1.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals('c', file.getHeader().getVersion());
        assertEquals(SP3OrbitType.HLM, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());
        assertEquals(CartesianDerivativesFilter.USE_P, file.getHeader().getFilter());

        assertEquals(26, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       Vector3D.ZERO),
                     coord);
        assertEquals(0.0001891633, coord.getClockCorrection(), 1.0e-15);
    }

    @Test
    void testParseSP3c2() {
        // simple test for version sp3-c, contains p/v entries and correlations
        final String    ex     = "/sp3/example-c-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals('c', file.getHeader().getVersion());
        assertEquals(SP3OrbitType.HLM, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());

        assertEquals(26, file.getSatelliteCount());

        assertEquals(2, file.getSatellites().get("G01").getSegments().size());
        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                                                 TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // VG01  20298.880364 -18462.044804   1381.387685     -4.534317 14 14 14 191
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       new Vector3D(2029.8880364, -1846.2044804, 138.1387685)),
                     coord);
        assertEquals(0.0001891633,  coord.getClockCorrection(), 1.0e-15);
        assertEquals(-0.0000000004534317, coord.getClockRateChange(), 1.0e-15);
    }

    @Test
    void testParseSP3cWithoutAgency() {
        // simple test for version sp3-c, contains p/v entries and correlations
        final String    ex     = "/sp3/missing-agency.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals('c', file.getHeader().getVersion());
        assertEquals("", file.getHeader().getAgency());
    }

    @Test
    void testParseSP3d1() {
        // simple test for version sp3-d, contains p entries
        final String    ex     = "/sp3/example-d-1.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals('d', file.getHeader().getVersion());
        assertEquals(SP3OrbitType.BCT, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());

        assertEquals(5, file.getHeader().getComments().size());
        assertEquals("Note: This is a simulated file, meant to illustrate what an SP3-d header",     file.getHeader().getComments().get(0));
        assertEquals("might look like with more than 85 satellites. Source for GPS and SBAS satel-", file.getHeader().getComments().get(1));
        assertEquals("lite positions: BRDM0930.13N. G=GPS,R=GLONASS,E=Galileo,C=BeiDou,J=QZSS,",     file.getHeader().getComments().get(2));
        assertEquals("I=IRNSS,S=SBAS. For definitions of SBAS satellites, refer to the website:",    file.getHeader().getComments().get(3));
        assertEquals("http://igs.org/mgex/status-SBAS",                                              file.getHeader().getComments().get(4));

        assertEquals(140, file.getSatelliteCount());

        assertEquals(2, file.getSatellites().get("S37").getSegments().size());
        assertEquals(1, file.getSatellites().get("S37").getSegments().get(0).getCoordinates().size());
        assertEquals(1, file.getSatellites().get("S37").getSegments().get(1).getCoordinates().size());

        final SP3Coordinate coord = file.getSatellites().get("S37").getSegments().get(0).getCoordinates().get(0);

        // 2013  4  3  0  0  0.00000000
        assertEquals(new AbsoluteDate(2013, 4, 3, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PS37 -34534.904566  24164.610955     29.812840      0.299420
        checkPVEntry(new PVCoordinates(new Vector3D(-34534904.566, 24164610.955, 29812.840),
                                       Vector3D.ZERO),
                     coord);
        assertEquals(0.00000029942, coord.getClockCorrection(), 1.0e-15);
    }

    @Test
    void testParseSP3d2() {
        // simple test for version sp3-c, contains p/v entries and correlations
        final String      ex    = "/sp3/example-d-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3        file   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 1, IGSUtils::guessFrame).
                                  parse(source);

        assertEquals('d', file.getHeader().getVersion());
        assertEquals(SP3OrbitType.HLM, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());

        assertEquals(26, file.getSatelliteCount());

        assertEquals(2, file.getSatellites().get("G01").getSegments().size());
        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // VG01  20298.880364 -18462.044804   1381.387685     -4.534317 14 14 14 191
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       new Vector3D(2029.8880364, -1846.2044804, 138.1387685)),
                     coord);
        assertEquals(0.0001891633,        coord.getClockCorrection(),         1.0e-10);
        assertEquals(55.512e-3,           coord.getPositionAccuracy().getX(), 1.0e-6);
        assertEquals(55.512e-3,           coord.getPositionAccuracy().getY(), 1.0e-6);
        assertEquals(55.512e-3,           coord.getPositionAccuracy().getZ(), 1.0e-6);
        assertEquals(223.1138e-12,        coord.getClockAccuracy(),           1.0e-16);
        assertEquals(-0.0000000004534317, coord.getClockRateChange(),         1.0e-16);
        assertEquals(22.737e-7,           coord.getVelocityAccuracy().getX(), 1.0e-10);
        assertEquals(22.737e-7,           coord.getVelocityAccuracy().getY(), 1.0e-10);
        assertEquals(22.737e-7,           coord.getVelocityAccuracy().getZ(), 1.0e-10);
        assertEquals(111.75277e-16,       coord.getClockRateAccuracy(),       1.0e-21);
        assertFalse(coord.hasClockEvent());
        assertFalse(coord.hasClockPrediction());
        assertFalse(coord.hasOrbitManeuverEvent());
        assertFalse(coord.hasOrbitPrediction());

        final List<SP3Coordinate> coords2 = file.getSatellites().get("G01").getSegments().get(1).getCoordinates();
        assertFalse(coords2.get(0).hasClockEvent());
        assertTrue(coords2.get(0).hasClockPrediction());
        assertFalse(coords2.get(0).hasOrbitManeuverEvent());
        assertTrue(coords2.get(0).hasOrbitPrediction());

        final BoundedPropagator propagator = file.getEphemeris("G01").getPropagator();
        final SpacecraftState s = propagator.propagate(coord.getDate());
        final Frame frame = file.getSatellites().get("G01").getFrame();
        assertEquals(0.0,
                                Vector3D.distance(coord.getPosition(), s.getPVCoordinates(frame).getPosition()),
                                4.2e-8);
        assertEquals(0.0,
                                Vector3D.distance(coord.getVelocity(), s.getPVCoordinates(frame).getVelocity()),
                                3.9e-12);
        assertEquals(coord.getClockCorrection(),
                                s.getAdditionalState(SP3Utils.CLOCK_ADDITIONAL_STATE)[0],
                                1.0e-18);

    }

    @Test
    void testSP3GFZ() {
        // simple test for version sp3-c, contains more than 85 satellites
        final String    ex     = "/sp3/gbm19500_truncated.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        assertEquals(SP3OrbitType.FIT, file.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());

        assertEquals(87, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("R23").getSegments().get(0).getCoordinates();
        assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        assertEquals(new AbsoluteDate(2017, 5, 21, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // PR23  24552.470459   -242.899447   6925.437998     86.875825
        checkPVEntry(new PVCoordinates(new Vector3D(24552470.459, -242899.447, 6925437.998),
                                       Vector3D.ZERO),
                     coord);
        assertEquals(0.000086875825, coord.getClockCorrection(), 1.0e-15);
    }

    @Test
    void testSP3Propagator() {
        // setup
        final String     ex         = "/sp3/gbm18432.sp3.Z";
        final DataSource compressed = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource source     = new UnixCompressFilter().filter(compressed);
        final SP3Parser  parser     = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 2, IGSUtils::guessFrame);

        // action
        final SP3 file = parser.parse(source);

        // verify
        TimeScale gps = TimeScalesFactory.getGPS();
        assertNull(file.getSatellites().get("XYZ"));
        SP3Ephemeris ephemeris = file.getSatellites().get("C03");
        Frame frame = ephemeris.getFrame();
        BoundedPropagator propagator = ephemeris.getPropagator();
        assertEquals(propagator.getMinDate(), new AbsoluteDate(2015, 5, 5, gps));
        assertEquals(propagator.getMaxDate(), new AbsoluteDate(2015, 5, 5, 23, 55, 0, gps));
        SP3Coordinate expected = ephemeris.getSegments().get(0).getCoordinates().get(0);
        SpacecraftState s = propagator.propagate(propagator.getMinDate());
        assertEquals(0.0,
                                Vector3D.distance(s.getPVCoordinates(frame).getPosition(),
                                                  expected.getPosition()),
                                3.0e-8);
        assertEquals(expected.getClockCorrection(),
                                s.getAdditionalState(SP3Utils.CLOCK_ADDITIONAL_STATE)[0],
                                1.0e-15);
        expected = ephemeris.getSegments().get(0).getCoordinates().get(1);
        s = propagator.propagate(expected.getDate());
        assertEquals(0.0,
                                Vector3D.distance(s.getPVCoordinates(frame).getPosition(),
                                                  expected.getPosition()),
                                3.0e-8);
        assertEquals(expected.getClockCorrection(),
                                s.getAdditionalState(SP3Utils.CLOCK_ADDITIONAL_STATE)[0],
                                1.0e-15);
        expected = ephemeris.getSegments().get(0).getCoordinates().get(ephemeris.getSegments().get(0).getCoordinates().size() - 1);
        s = propagator.propagate(propagator.getMaxDate());
        assertEquals(0.0,
                                Vector3D.distance(s.getPVCoordinates(frame).getPosition(),
                                                  expected.getPosition()),
                                3.0e-8);
        assertEquals(expected.getClockCorrection(),
                                s.getAdditionalState(SP3Utils.CLOCK_ADDITIONAL_STATE)[0],
                                1.0e-15);
        SP3Coordinate previous = ephemeris.getSegments().get(0).getCoordinates().get(ephemeris.getSegments().get(0).getCoordinates().size() - 2);
        final double deltaClock = expected.getClockCorrection() - previous.getClockCorrection();
        final double deltaT     = expected.getDate().durationFrom(previous.getDate());
        assertEquals(deltaClock / deltaT,
                                s.getAdditionalStateDerivative(SP3Utils.CLOCK_ADDITIONAL_STATE)[0],
                                1.0e-26);

        ephemeris = file.getSatellites().get("E19");
        propagator = ephemeris.getPropagator(new FrameAlignedProvider(ephemeris.getFrame()));
        assertEquals(propagator.getMinDate(), new AbsoluteDate(2015, 5, 5, gps));
        assertEquals(propagator.getMaxDate(), new AbsoluteDate(2015, 5, 5, 23, 55, 0, gps));
        expected = ephemeris.getSegments().get(0).getCoordinates().get(0);
        assertEquals(0.0,
                                Vector3D.distance(propagator.propagate(propagator.getMinDate()).getPVCoordinates(frame).getPosition(),
                                                  expected.getPosition()),
                                3.0e-8);
        expected = ephemeris.getSegments().get(0).getCoordinates().get(1);
        assertEquals(0.0,
                                Vector3D.distance(propagator.propagate(expected.getDate()).getPVCoordinates(frame).getPosition(),
                                                  expected.getPosition()),
                                3.0e-8);
        expected = ephemeris.getSegments().get(0).getCoordinates().get(ephemeris.getSegments().get(0).getCoordinates().size() - 1);
        assertEquals(0.0,
                                Vector3D.distance(propagator.propagate(propagator.getMaxDate()).getPVCoordinates(frame).getPosition(),
                                                  expected.getPosition()),
                                3.1e-8);
    }

    @Test
    void testSP3Compressed() {
        final String ex = "/sp3/gbm18432.sp3.Z";

        final SP3Parser parser = new SP3Parser();
        final DataSource compressed = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3 file = parser.parse(new UnixCompressFilter().filter(compressed));

        assertEquals(SP3OrbitType.FIT, file.getHeader().getOrbitType());
        assertEquals("FIT",file.getHeader().getOrbitTypeKey());
        assertEquals(TimeSystem.GPS, file.getHeader().getTimeSystem());

        assertEquals(71, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("R13").getSegments().get(0).getCoordinates();
        assertEquals(288, coords.size());

        final SP3Coordinate coord = coords.get(228);


        assertEquals(new AbsoluteDate(2015, 5, 5, 19, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PR13  25330.290321   -411.728000   2953.331527   -482.447619
        checkPVEntry(new PVCoordinates(new Vector3D(25330290.321, -411728.000, 2953331.527),
                                       Vector3D.ZERO),
                     coord);
        assertEquals(-0.000482447619,  coord.getClockCorrection(), 1.0e-15);
    }

    private void checkPVEntry(final PVCoordinates expected, final PVCoordinates actual) {
        final Vector3D expectedPos = expected.getPosition();
        final Vector3D expectedVel = expected.getVelocity();

        final Vector3D actualPos = actual.getPosition();
        final Vector3D actualVel = actual.getVelocity();

        // sp3 files can have mm accuracy
        final double eps = 1e-3;

        assertEquals(expectedPos.getX(), actualPos.getX(), eps);
        assertEquals(expectedPos.getY(), actualPos.getY(), eps);
        assertEquals(expectedPos.getZ(), actualPos.getZ(), eps);

        assertEquals(expectedVel.getX(), actualVel.getX(), eps);
        assertEquals(expectedVel.getY(), actualVel.getY(), eps);
        assertEquals(expectedVel.getZ(), actualVel.getZ(), eps);

        assertEquals(Vector3D.ZERO, actual.getAcceleration());
    }

    @Test
    void testTruncatedLine() {
        try {
            final String    ex     = "/sp3/truncated-line.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
            parser.parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            assertEquals(27, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    void testMissingEOF() {
        final String    ex     = "/sp3/missing-eof.sp3";
        try {
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
            parser.parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_NUMBER_OF_EPOCH_MISMATCH, oe.getSpecifier());
            assertEquals(  1, ((Integer) oe.getParts()[0]).intValue());
            assertEquals( ex, oe.getParts()[1]);
            assertEquals(192, ((Integer) oe.getParts()[2]).intValue());
        }

    }

    @Test
    void testMissingStandardDeviation() {
        final String    ex     = "/sp3/missing-standard-deviation.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
        final SP3 sp3 = parser.parse(source);
        assertEquals(32, sp3.getSatelliteCount());
        List<SP3Coordinate> coordinates06 = sp3.getEphemeris("G06").getSegments().get(0).getCoordinates();
        assertEquals(21, coordinates06.size());
        for (int i = 0; i < 21; ++i) {
            final Vector3D positionAccuracy = coordinates06.get(i).getPositionAccuracy();
            if (i == 7 || i == 8) {
                // some standard deviations are missing
                assertNull(positionAccuracy);
            } else {
                // other are present
                assertTrue(positionAccuracy.getNorm() < 0.0122);
                assertTrue(positionAccuracy.getNorm() > 0.0045);
            }
        }
    }

    @Test
    void testWrongLineIdentifier() {
        try {
            final String    ex     = "/sp3/wrong-line-identifier.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
            parser.parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            assertEquals(13, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    void testBHN() {
        final SP3Parser   parser       = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
        final String      ex           = "/sp3/esaBHN.sp3.Z";
        final DataSource   compressed   = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
        final SP3     file         = parser.parse(uncompressed);
        assertEquals(SP3OrbitType.FIT, file.getHeader().getOrbitType());
        assertEquals("BHN", file.getHeader().getOrbitTypeKey());
    }

    @Test
    void testPRO() {
        final SP3Parser   parser       = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
        final String      ex           = "/sp3/esaPRO.sp3.Z";
        final DataSource   compressed   = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
        final SP3     file         = parser.parse(uncompressed);
        assertEquals(SP3OrbitType.EXT, file.getHeader().getOrbitType());
        assertEquals("PRO", file.getHeader().getOrbitTypeKey());
    }

    @Test
    void testUnknownType() {
        final SP3Parser   parser       = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
        final String      ex           = "/sp3/unknownType.sp3.Z";
        final DataSource   compressed   = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
        final SP3     file         = parser.parse(uncompressed);
        assertEquals(SP3OrbitType.OTHER, file.getHeader().getOrbitType());
        assertEquals("UKN", file.getHeader().getOrbitTypeKey());
    }

    @Test
    void testUnsupportedVersion() {
        try {
            final String    ex     = "/sp3/unsupported-version.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
            parser.parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_UNSUPPORTED_VERSION,
                                oe.getSpecifier());
            assertEquals('z', ((Character) oe.getParts()[0]).charValue());
        }

    }

    @Test
    void testWrongNumberOfEpochs() {
        try {
            final String    ex     = "/sp3/wrong-number-of-epochs.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
            parser.parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_NUMBER_OF_EPOCH_MISMATCH,
                                oe.getSpecifier());
            assertEquals(  2, ((Integer) oe.getParts()[0]).intValue());
            assertEquals(192, ((Integer) oe.getParts()[2]).intValue());
        }

    }

    @Test
    void testInconsistentSamplingDates() {
        try {
            final String    ex     = "/sp3/inconsistent-sampling-dates.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, IGSUtils::guessFrame);
            parser.parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INCONSISTENT_SAMPLING_DATE, oe.getSpecifier());
            assertEquals(new AbsoluteDate(1994, 12, 17, 23, 45, 0.0, TimeScalesFactory.getGPS()), oe.getParts()[0]);
            assertEquals(new AbsoluteDate(1994, 12, 17, 23, 46, 0.0, TimeScalesFactory.getGPS()), oe.getParts()[1]);
        }

    }

    @Test
    void testIssue803() {

        // Test issue 803 (see https://gitlab.orekit.org/orekit/orekit/-/issues/803)
        final String    ex     = "/sp3/truncated-nsgf.orb.lageos2.160305.v35.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Coordinates
        final List<SP3Coordinate> coords = file.getSatellites().get("L52").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());
        final SP3Coordinate coord = coords.get(0);

        // Verify
        assertEquals(SP3FileType.LEO, file.getHeader().getType());

        // PL52   2228.470946   7268.265924   9581.471543
        // VL52 -44856.945000  24321.151000  -7116.222800
        checkPVEntry(new PVCoordinates(new Vector3D(2228470.946, 7268265.924, 9581471.543),
                                       new Vector3D(-4485.6945000, 2432.1151000, -711.6222800)),
                     coord);
        assertEquals(0.999999999999,   coord.getClockCorrection(), 1.0e-12);
        assertEquals(9.99999999999e-5, coord.getClockRateChange(), 1.0e-16);

    }

    @Test
    void testIssue827() {

        // Test issue 827 (see https://gitlab.orekit.org/orekit/orekit/-/issues/827)
        final String    ex     = "/sp3/truncated-nsgf.orb.lageos2.160305.v35.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Coordinates
        final List<SP3Coordinate> coords = file.getSatellites().get("L52").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());
        final SP3Coordinate coord = coords.get(0);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());

        // 2016  2 28 0 0 0.00000000
        assertEquals(new AbsoluteDate(2016, 2, 28, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord.getDate());


        // PL52   2228.470946   7268.265924   9581.471543
        // VL52 -44856.945000  24321.151000  -7116.222800
        checkPVEntry(new PVCoordinates(new Vector3D(2228470.946, 7268265.924, 9581471.543),
                                       new Vector3D(-4485.6945000, 2432.1151000, -711.6222800)),
                     coord);
        assertEquals(0.999999999999,   coord.getClockCorrection(), 1.0e-12);
        assertEquals(9.99999999999e-5, coord.getClockRateChange(), 1.0e-16);

    }

    @Test
    void testIssue828() {

        // Test issue 828 (see https://gitlab.orekit.org/orekit/orekit/-/issues/828)
        final String    ex     = "/sp3/example-d-3.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.IRNSS, file.getHeader().getType());

    }

    @Test
    void testIssue828Bis() {

        // Test issue 828 (see https://gitlab.orekit.org/orekit/orekit/-/issues/828)
        final String    ex     = "/sp3/example-d-4.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.SBAS, file.getHeader().getType());

    }

    @Test
    void testIssue1314() {

        // Test issue 828 (see https://gitlab.orekit.org/orekit/orekit/-/issues/1314)
        final String    ex     = "/sp3/example-d-5.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.GALILEO, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.SBAS, file.getHeader().getType());

    }

    @Test
    void testIssue895HeaderComment() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-header-comment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());

    }

    @Test
    void testIssue895ClockRecord() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-clock-record.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());
        assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2021 12 26  0  0  0.00000000
        assertEquals(new AbsoluteDate(2021, 12, 26, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord.getDate());

        // PL51   5029.867893   1304.362160 -11075.527276 999999.999999
        // VL51 -17720.521773 -55720.482742 -14441.695083 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(5029867.893, 1304362.160, -11075527.276),
                                       new Vector3D(-1772.0521773, -5572.0482742, -1444.1695083)),
                     coord);

    }

    @Test
    void testIssue895RolloverMinutes() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-minutes-increment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());
        assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getSegments().get(0).getCoordinates();
        assertEquals(91, coords.size());

        final SP3Coordinate coord30 = coords.get(30);

        // 2016  7  6 16 60  0.00000000
        assertEquals(new AbsoluteDate(2016, 7, 6, 17, 0, 0,
                TimeScalesFactory.getUTC()), coord30.getDate());

        // PL51  11948.228978   2986.113872   -538.901114 999999.999999
        // VL51   4605.419303 -27972.588048 -53316.820671 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(11948228.978,   2986113.872,   -538901.114),
                                       new Vector3D(460.5419303, -2797.2588048, -5331.6820671)),
                     coord30);

        final SP3Coordinate coord31 = coords.get(31);

        // 2016  7  6 17  2  0.00000000
        assertEquals(new AbsoluteDate(2016, 7, 6, 17, 2, 0,
                TimeScalesFactory.getUTC()), coord31.getDate());

        // PL51  11982.652569   2645.786926  -1177.549463 999999.999999
        // VL51   1128.248622 -28724.293303 -53097.358387 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(11982652.569,   2645786.926,  -1177549.463),
                                       new Vector3D(112.8248622, -2872.4293303, -5309.7358387)),
                     coord31);

        final SP3Coordinate coord60 = coords.get(60);

        // 2016  7  6 17 60  0.00000000
        assertEquals(new AbsoluteDate(2016, 7, 6, 18, 0, 0,
                TimeScalesFactory.getUTC()), coord60.getDate());

        // PL51  -1693.056569  -4123.276630 -11431.599723 999999.999999
        // VL51 -59412.268951   4066.817074   7604.890337 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(-1693056.569,  -4123276.630, -11431599.723),
                                       new Vector3D(-5941.2268951,   406.6817074,   760.4890337)),
                     coord60);

    }

    @Test
    void testIssue895RolloverHours() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-hours-increment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());
        assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getSegments().get(0).getCoordinates();
        assertEquals(61, coords.size());

        final SP3Coordinate coord30 = coords.get(30);

        // 2016  7  7 24  0  0.00000000
        assertEquals(new AbsoluteDate(2016, 7, 7, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord30.getDate());

        //PL51   2989.229334  -8494.421415   8385.068555
        //VL51 -19617.027447 -43444.824985 -36706.159070
        checkPVEntry(new PVCoordinates(new Vector3D(2989229.334,  -8494421.415,   8385068.555),
                                       new Vector3D(-1961.7027447, -4344.4824985, -3670.6159070)),
                     coord30);

        final SP3Coordinate coord31 = coords.get(31);

        // 2016  7  7  0  2  0.00000000
        assertEquals(new AbsoluteDate(2016, 7, 7, 0, 2, 0,
                TimeScalesFactory.getUTC()), coord31.getDate());

        // PL51   2744.983592  -9000.639164   7931.904779
        // VL51 -21072.925764 -40899.633288 -38801.567078
        checkPVEntry(new PVCoordinates(new Vector3D(2744983.592,  -9000639.164,   7931904.779),
                                       new Vector3D(-2107.2925764, -4089.9633288, -3880.1567078)),
                     coord31);

    }

    @Test
    void testIssue895SecondDigits() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-second-digits.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());
        assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2016  7  3  0  0  0.1234
        assertEquals(new AbsoluteDate(2016, 7, 3, 0, 0, 0.1234,
                TimeScalesFactory.getUTC()), coord.getDate());

    }

    @Test
    void testIssue895NoEOF() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-no-eof.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());
        assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getSegments().get(0).getCoordinates();
        assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2021 12 26  0  0  0.00000000
        assertEquals(new AbsoluteDate(2021, 12, 26, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord.getDate());

        // PL51   5029.867893   1304.362160 -11075.527276 999999.999999
        // VL51 -17720.521773 -55720.482742 -14441.695083 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(5029867.893, 1304362.160, -11075527.276),
                                       new Vector3D(-1772.0521773, -5572.0482742, -1444.1695083)),
                     coord);

    }

    @Test
    void testExceededSatCount() {
        final String    ex     = "/sp3/exceeded-sat-count.sp3";
        try {
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new SP3Parser().parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_TOO_MANY_SATELLITES_FOR_VERSION, oe.getSpecifier());
            assertEquals('c', ((Character) oe.getParts()[0]).charValue());
            assertEquals( 99, ((Integer) oe.getParts()[1]).intValue());
            assertEquals(140, ((Integer) oe.getParts()[2]).intValue());
            assertEquals(ex, oe.getParts()[3]);
        }
    }

    @Test
    void testIssue1014() {

        // Test issue 1014
        final String    ex     = "/sp3/issue1014-days-increment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        assertEquals(TimeSystem.UTC, file.getHeader().getTimeSystem());
        assertEquals(SP3FileType.LEO, file.getHeader().getType());
        assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getSegments().get(0).getCoordinates();
        assertEquals(62, coords.size());

        final SP3Coordinate coord = coords.get(61);

        // following date is completely wrong (2022 january 0th instead of 2021 december 31st)
        // 2022  1  0  1  2  0.00000000
        assertEquals(new AbsoluteDate(2021, 12, 31, 1, 2, 0,
                              TimeScalesFactory.getUTC()), coord.getDate());

        // PL51  -5691.093473  -9029.216710  -5975.427658
        // VL51 -39188.598237  -5856.539265  45893.223756
        checkPVEntry(new PVCoordinates(new Vector3D(-5691093.473,  -9029216.710,  -5975427.658),
                                       new Vector3D(-3918.8598237,  -585.6539265,  4589.3223756)),
                     coord);

    }

    @Test
    void testWrongPosVelBaseA() {
        doTestWrongHeaderEntry("/sp3/wrong-pos-vel-base-a.sp3", "pos/vel accuracy base");
    }

    @Test
    void testWrongPosVelBaseD() {
        doTestWrongHeaderEntry("/sp3/wrong-pos-vel-base-d.sp3", "pos/vel accuracy base");
    }

    @Test
    void testWrongClockBaseA() {
        doTestWrongHeaderEntry("/sp3/wrong-clock-base-a.sp3", "clock accuracy base");
    }

    @Test
    void testWrongClockBaseD() {
        doTestWrongHeaderEntry("/sp3/wrong-clock-base-d.sp3", "clock accuracy base");
    }

    @Test
    void testWrongTooManyCommentsA() {
        doTestWrongHeaderEntry("/sp3/too-many-comments-a.sp3", "comments");
    }

    @Test
    void testWrongTooLongCommentA() {
        doTestWrongHeaderEntry("/sp3/too-long-comment-a.sp3", "comments");
    }

    @Test
    void testWrongTooLongCommentD() {
        doTestWrongHeaderEntry("/sp3/too-long-comment-d.sp3", "comments");
    }

    private void doTestWrongHeaderEntry(final String ex, final String entry) {
        try {
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new SP3Parser().parse(source);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INVALID_HEADER_ENTRY, oe.getSpecifier());
            assertEquals(entry, oe.getParts()[0]);
            assertEquals(ex, oe.getParts()[2]);
        }
    }

    @Test
    void testSpliceWrongType() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_type.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_FILE_METADATA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongTimeSystem() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_time_system.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_FILE_METADATA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongSatelliteCount() {
        final SP3 spliced = splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_satellite_count.sp3");
        assertEquals(86, spliced.getSatelliteCount());
    }

    @Test
    void testSpliceWrongOrbitType() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_orbit_type.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_FILE_METADATA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongCoordinateSystem() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_coordinate_system.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_FILE_METADATA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongDataUsed() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_data_used.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oe) {
            assertEquals(OrekitMessages.SP3_INVALID_HEADER_ENTRY, oe.getSpecifier());
            assertEquals("data used", oe.getParts()[0]);
            assertEquals("v", oe.getParts()[1]);
        }
    }

    @Test
    void testSpliceWrongAgency() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_agency.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_FILE_METADATA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongSatelliteList() {
        final SP3 spliced = splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_satellite_list.sp3");
        assertEquals(86, spliced.getSatelliteCount());
    }

    @Test
    void testSpliceWrongDerivatives() {
        try {
            splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_wrong_derivatives.sp3");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_SATELLITE_MEDATADA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongFrame() {
        try {
            final String     name1 = "/sp3/gbm19500_truncated.sp3";
            final DataSource source1 = new DataSource(name1, () -> getClass().getResourceAsStream(name1));
            final SP3        file1   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 7,
                                                     s -> FramesFactory.getITRF(IERSConventions.IERS_2010, false)).
                                       parse(source1);
            final String     name2 = "/sp3/gbm19500_after_no_drop.sp3";
            final DataSource source2 = new DataSource(name2, () -> getClass().getResourceAsStream(name2));
            final SP3        file2   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 7,
                                                     s -> FramesFactory.getITRF(IERSConventions.IERS_1996, false)).
                                       parse(source2);
            SP3.splice(Arrays.asList(file1, file2));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_SATELLITE_MEDATADA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongInterpolationSamples() {
        try {
            final String     name1 = "/sp3/gbm19500_truncated.sp3";
            final DataSource source1 = new DataSource(name1, () -> getClass().getResourceAsStream(name1));
            final SP3        file1   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 7,
                                                     s -> FramesFactory.getITRF(IERSConventions.IERS_2010, false)).
                                       parse(source1);
            final String     name2 = "/sp3/gbm19500_after_no_drop.sp3";
            final DataSource source2 = new DataSource(name2, () -> getClass().getResourceAsStream(name2));
            final SP3        file2   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 4,
                                                     s -> FramesFactory.getITRF(IERSConventions.IERS_2010, false)).
                                       parse(source2);
            SP3.splice(Arrays.asList(file1, file2));
             fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_SATELLITE_MEDATADA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceWrongMu() {
        try {
            final String     name1 = "/sp3/gbm19500_truncated.sp3";
            final DataSource source1 = new DataSource(name1, () -> getClass().getResourceAsStream(name1));
            final SP3        file1   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 7,
                                                     s -> FramesFactory.getITRF(IERSConventions.IERS_2010, false)).
                                       parse(source1);
            final String     name2 = "/sp3/gbm19500_after_no_drop.sp3";
            final DataSource source2 = new DataSource(name2, () -> getClass().getResourceAsStream(name2));
            final SP3        file2   = new SP3Parser(1.00001 * Constants.EIGEN5C_EARTH_MU, 7,
                                                     s -> FramesFactory.getITRF(IERSConventions.IERS_2010, false)).
                                       parse(source2);
            SP3.splice(Arrays.asList(file1, file2));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.SP3_INCOMPATIBLE_SATELLITE_MEDATADA, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceNewSegment() {
        SP3 sp3 = splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_large_gap.sp3");
        assertEquals(2, sp3.getEphemeris("C01").getSegments().size());
        final TimeScale ts = sp3.getHeader().getTimeSystem().getTimeScale(TimeScalesFactory.getTimeScales());

        final AggregatedClockModel clockModel = sp3.getEphemeris("C01").extractClockModel();
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 0, 0.0, ts).durationFrom(clockModel.getValidityStart()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 25, 0.0, ts).durationFrom(clockModel.getValidityEnd()),
                                1.0e-15);

        // there are 5 spans after splicing:
        // one null    from      -∞             to 2017-05-21T00:00:00
        // one regular from 2017-05-21T00:00:00 to 2017-05-21T00:05:00
        // one null    from 2017-05-21T00:05:00 to 2017-05-21T00:20:00
        // one regular from 2017-05-21T00:20:00 to 2017-05-21T00:25:00
        // one null    from 2017-05-21T00:25:00 to        +∞
        assertEquals(5, clockModel.getModels().getSpansNumber());
        TimeSpanMap.Span<ClockModel> span = clockModel.getModels().getFirstSpan();
        assertNull(span.getData());
        span = span.next();
        assertInstanceOf(SampledClockModel.class, span.getData());
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 0, 0.0, ts).durationFrom(span.getData().getValidityStart()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 5, 0.0, ts).durationFrom(span.getData().getValidityEnd()),
                                1.0e-15);
        span = span.next();
        assertNull(span.getData());
        span = span.next();
        assertInstanceOf(SampledClockModel.class, span.getData());
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 20, 0.0, ts).durationFrom(span.getData().getValidityStart()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 25, 0.0, ts).durationFrom(span.getData().getValidityEnd()),
                                1.0e-15);
        span = span.next();
        assertNull(span.getData());

        assertEquals(2.4314257e-5, clockModel.getOffset(new AbsoluteDate(2017, 5, 21, 0,  4, 59.5, ts)).getOffset(), 1.0e-12);
        assertEquals(2.4301550e-5, clockModel.getOffset(new AbsoluteDate(2017, 5, 21, 0, 20,  0.5, ts)).getOffset(), 1.0e-12);
        try {
            clockModel.getOffset(new AbsoluteDate(2017, 5, 21, 0, 10, 0.0, ts));
            fail("an exceptions should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_DATA_GENERATED, oe.getSpecifier());
        }

    }

    @Test
    void testSpliceDrop() {

        final SP3 spliced = splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_after_drop.sp3");
        final TimeScale ts = spliced.getHeader().getTimeSystem().getTimeScale(TimeScalesFactory.getTimeScales());

        assertEquals(SP3OrbitType.FIT, spliced.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, spliced.getHeader().getTimeSystem());

        assertEquals(87, spliced.getSatelliteCount());

        final List<SP3Coordinate> coords = spliced.getSatellites().get("R23").getSegments().get(0).getCoordinates();
        assertEquals(3, coords.size());

        final SP3Coordinate coord = coords.get(0);

        assertEquals(new AbsoluteDate(2017, 5, 21, 0, 0, 0, TimeScalesFactory.getGPS()),
                                coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // PR23  24552.470459   -242.899447   6925.437998     86.875825
        checkPVEntry(new PVCoordinates(new Vector3D(24552470.459, -242899.447, 6925437.998), Vector3D.ZERO),
                     coord);
        assertEquals(0.000086875825, coord.getClockCorrection(), 1.0e-15);

        assertEquals(new AbsoluteDate(2017, 5, 21, 0, 10, 0, TimeScalesFactory.getGPS()),
                                coords.get(coords.size() - 1).getDate());

        assertEquals(1.25,  spliced.getHeader().getPosVelBase(), 1.0e-15);
        assertEquals(1.025, spliced.getHeader().getClockBase(),  1.0e-15);

        // since the gap between the two files is equal to the epoch interval
        // the segments are kept merged
        final AggregatedClockModel clockModel = spliced.getEphemeris("R23").extractClockModel();
        assertEquals(3, clockModel.getModels().getSpansNumber());
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 0, 0.0, ts).durationFrom(clockModel.getValidityStart()),
                                1.0e-15);
        assertEquals(0.0,
                                new AbsoluteDate(2017, 5, 21, 0, 10, 0.0, ts).durationFrom(clockModel.getValidityEnd()),
                                1.0e-15);
        try {
            clockModel.getOffset(clockModel.getValidityStart().shiftedBy(-1));
            fail("an exceptions should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_DATA_GENERATED, oe.getSpecifier());
        }
        try {
            clockModel.getOffset(clockModel.getValidityEnd().shiftedBy(1));
            fail("an exceptions should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.NO_DATA_GENERATED, oe.getSpecifier());
        }
    }

    @Test
    void testSpliceNoDrop() {

        final SP3 spliced = splice("/sp3/gbm19500_truncated.sp3", "/sp3/gbm19500_after_no_drop.sp3");

        assertEquals(SP3OrbitType.FIT, spliced.getHeader().getOrbitType());
        assertEquals(TimeSystem.GPS, spliced.getHeader().getTimeSystem());

        assertEquals(87, spliced.getSatelliteCount());

        final List<SP3Coordinate> coords = spliced.getSatellites().get("R23").getSegments().get(0).getCoordinates();
        assertEquals(4, coords.size());

        final SP3Coordinate coord = coords.get(0);

        assertEquals(new AbsoluteDate(2017, 5, 21, 0, 0, 0, TimeScalesFactory.getGPS()),
                                coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // PR23  24552.470459   -242.899447   6925.437998     86.875825
        checkPVEntry(new PVCoordinates(new Vector3D(24552470.459, -242899.447, 6925437.998), Vector3D.ZERO),
                     coord);
        assertEquals(0.000086875825, coord.getClockCorrection(), 1.0e-15);

        assertEquals(new AbsoluteDate(2017, 5, 21, 0, 15, 0, TimeScalesFactory.getGPS()),
                                coords.get(coords.size() - 1).getDate());

        assertEquals("R23", spliced.getEphemeris("R23").getId());
        try {
            spliced.getEphemeris(88);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INVALID_SATELLITE_ID, oe.getSpecifier());
            assertEquals(88, ((Integer) oe.getParts()[0]).intValue());
        }
        try {
            spliced.getEphemeris("Z00");
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INVALID_SATELLITE_ID, oe.getSpecifier());
            assertEquals("Z00", oe.getParts()[0]);
        }

    }

    private SP3 splice(final String name1, final String name2) {
        final DataSource source1 = new DataSource(name1, () -> getClass().getResourceAsStream(name1));
        final SP3        file1   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 2, IGSUtils::guessFrame).parse(source1);
        final DataSource source2 = new DataSource(name2, () -> getClass().getResourceAsStream(name2));
        final SP3        file2   = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 2, IGSUtils::guessFrame).parse(source2);
        return SP3.splice(Arrays.asList(file1, file2));
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
