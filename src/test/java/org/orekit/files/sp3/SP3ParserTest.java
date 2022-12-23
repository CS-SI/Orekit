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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.data.UnixCompressFilter;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sp3.SP3.SP3Coordinate;
import org.orekit.files.sp3.SP3.SP3Ephemeris;
import org.orekit.files.sp3.SP3.SP3OrbitType;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.gnss.TimeSystem;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class SP3ParserTest {

    @Test
    public void testParseSP3a1() throws IOException, URISyntaxException {
        // simple test for version sp3-a, only contains position entries
        final String    ex     = "/sp3/example-a-1.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());
        Assertions.assertSame(Predefined.ITRF_CIO_CONV_2010_ACCURATE_EOP,
                          ((FactoryManagedFrame) file.getSatellites().get("1").getFrame()).getFactoryKey());

        Assertions.assertEquals(25, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("1").getCoordinates();
        Assertions.assertEquals(3, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assertions.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        checkPVEntry(new PVCoordinates(new Vector3D(16258524.75, -3529015.75, -20611427.049),
                                       Vector3D.ZERO),
                     coord);
        Assertions.assertEquals(-0.0000625406, coord.getClockCorrection(), 1.0e-15);
        Assertions.assertEquals("NGS", file.getAgency());
        Assertions.assertEquals("ITR92", file.getCoordinateSystem());
        Assertions.assertEquals("d", file.getDataUsed());
        Assertions.assertEquals(0.0, file.getDayFraction(), 1.0e-15);
        Assertions.assertEquals("1994-12-16T23:59:50.000", file.getEpoch().toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals(49703, file.getJulianDay());
        Assertions.assertEquals(3, file.getNumberOfEpochs());
        Assertions.assertEquals(900.0, file.getEpochInterval(), 1.0e-15);
        Assertions.assertEquals(779, file.getGpsWeek());
        Assertions.assertEquals(518400.0, file.getSecondsOfWeek(), 1.0e-10);
        Assertions.assertEquals(25, file.getSatellites().size());
        Assertions.assertEquals(SP3.SP3FileType.UNDEFINED, file.getType());
        Assertions.assertNull(file.getSatellites().get(null));
    }

    @Test
    public void testParseSP3a2() throws IOException {
        // simple test for version sp3-a, contains p/v entries
        final String    ex     = "/sp3/example-a-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(25, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("1").getCoordinates();
        Assertions.assertEquals(3, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assertions.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        // V 1  -6560.373522  25605.954994  -9460.427179     -0.024236
        checkPVEntry(new PVCoordinates(new Vector3D(16258524.75, -3529015.75, -20611427.049),
                                       new Vector3D(-656.0373, 2560.5954, -946.0427)),
                     coord);
        Assertions.assertEquals(-0.0000625406, coord.getClockCorrection(), 1.0e-15);
        Assertions.assertEquals(-0.0000024236, coord.getClockRateChange(), 1.0e-15);
    }

    @Test
    public void testParseSP3c1() throws IOException {
        // simple test for version sp3-c, contains p entries
        final String    ex     = "/sp3/example-c-1.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.HLM, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(26, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getCoordinates();
        Assertions.assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       Vector3D.ZERO),
                     coord);
        Assertions.assertEquals(0.0001891633, coord.getClockCorrection(), 1.0e-15);
    }

    @Test
    public void testParseSP3c2() throws IOException {
        // simple test for version sp3-c, contains p/v entries and correlations
        final String    ex     = "/sp3/example-c-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.HLM, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(26, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getCoordinates();
        Assertions.assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // VG01  20298.880364 -18462.044804   1381.387685     -4.534317 14 14 14 191
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       new Vector3D(2029.8880364, -1846.2044804, 138.1387685)),
                     coord);
        Assertions.assertEquals(0.0001891633,  coord.getClockCorrection(), 1.0e-15);
        Assertions.assertEquals(-0.0004534317, coord.getClockRateChange(), 1.0e-15);
    }

    @Test
    public void testParseSP3d1() throws IOException {
        // simple test for version sp3-d, contains p entries
        final String    ex     = "/sp3/example-d-1.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.BCT, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(140, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("S37").getCoordinates();
        Assertions.assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2013  4  3  0  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2013, 4, 3, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PS37 -34534.904566  24164.610955     29.812840      0.299420
        checkPVEntry(new PVCoordinates(new Vector3D(-34534904.566, 24164610.955, 29812.840),
                                       Vector3D.ZERO),
                     coord);
        Assertions.assertEquals(0.00000029942, coord.getClockCorrection(), 1.0e-15);
    }

    @Test
    public void testParseSP3d2() throws IOException {
        // simple test for version sp3-c, contains p/v entries and correlations
        final String    ex     = "/sp3/example-d-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.HLM, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(26, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getCoordinates();
        Assertions.assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // VG01  20298.880364 -18462.044804   1381.387685     -4.534317 14 14 14 191
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       new Vector3D(2029.8880364, -1846.2044804, 138.1387685)),
                     coord);
        Assertions.assertEquals(0.0001891633,  coord.getClockCorrection(), 1.0e-15);
        Assertions.assertEquals(-0.0004534317, coord.getClockRateChange(), 1.0e-15);
    }

    @Test
    public void testSP3GFZ() throws IOException {
        // simple test for version sp3-c, contains more than 85 satellites
        final String    ex     = "/sp3/gbm19500_truncated.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        Assertions.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(87, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("R23").getCoordinates();
        Assertions.assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        Assertions.assertEquals(new AbsoluteDate(2017, 5, 21, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        // PR23  24552.470459   -242.899447   6925.437998     86.875825
        checkPVEntry(new PVCoordinates(new Vector3D(24552470.459, -242899.447, 6925437.998),
                                       Vector3D.ZERO),
                     coord);
        Assertions.assertEquals(0.000086875825, coord.getClockCorrection(), 1.0e-15);
    }

    @Test
    public void testSP3Propagator() throws Exception {
        // setup
        final String    ex     = "/sp3/example-a-2.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final Frame     frame  = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);

        // action
        final SP3 file = parser.parse(source);

        // verify
        TimeScale gps = TimeScalesFactory.getGPS();
        SP3Ephemeris ephemeris = file.getSatellites().get("1");
        BoundedPropagator propagator = ephemeris.getPropagator();
        Assertions.assertEquals(propagator.getMinDate(), new AbsoluteDate(1994, 12, 17, gps));
        Assertions.assertEquals(propagator.getMaxDate(), new AbsoluteDate(1994, 12, 17, 23, 45, 0, gps));
        SP3Coordinate expected = ephemeris.getCoordinates().get(0);
        checkPVEntry(
                propagator.getPVCoordinates(propagator.getMinDate(), frame),
                expected);
        expected = ephemeris.getCoordinates().get(1);
        checkPVEntry(propagator.getPVCoordinates(expected.getDate(), frame), expected);
        expected = ephemeris.getCoordinates().get(2);
        checkPVEntry(
                propagator.getPVCoordinates(propagator.getMaxDate(), frame),
                expected);

        ephemeris = file.getSatellites().get("31");
        propagator = ephemeris.getPropagator();
        Assertions.assertEquals(propagator.getMinDate(), new AbsoluteDate(1994, 12, 17, gps));
        Assertions.assertEquals(propagator.getMaxDate(), new AbsoluteDate(1994, 12, 17, 23, 45, 0, gps));
        expected = ephemeris.getCoordinates().get(0);
        checkPVEntry(
                propagator.propagate(propagator.getMinDate()).getPVCoordinates(frame),
                expected);
        expected = ephemeris.getCoordinates().get(1);
        checkPVEntry(propagator.propagate(expected.getDate()).getPVCoordinates(frame), expected);
        expected = ephemeris.getCoordinates().get(2);
        checkPVEntry(
                propagator.propagate(propagator.getMaxDate()).getPVCoordinates(frame),
                expected);
    }

    @Test
    public void testSP3Compressed() throws IOException {
        final String ex = "/sp3/gbm18432.sp3.Z";

        final SP3Parser parser = new SP3Parser();
        final DataSource compressed = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3 file = parser.parse(new UnixCompressFilter().filter(compressed));

        Assertions.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assertions.assertEquals("FIT",file.getOrbitTypeKey());
        Assertions.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assertions.assertEquals(71, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("R13").getCoordinates();
        Assertions.assertEquals(288, coords.size());

        final SP3Coordinate coord = coords.get(228);


        Assertions.assertEquals(new AbsoluteDate(2015, 5, 5, 19, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PR13  25330.290321   -411.728000   2953.331527   -482.447619
        checkPVEntry(new PVCoordinates(new Vector3D(25330290.321, -411728.000, 2953331.527),
                                       Vector3D.ZERO),
                     coord);
        Assertions.assertEquals(-0.000482447619,  coord.getClockCorrection(), 1.0e-15);
    }

    private void checkPVEntry(final PVCoordinates expected, final PVCoordinates actual) {
        final Vector3D expectedPos = expected.getPosition();
        final Vector3D expectedVel = expected.getVelocity();

        final Vector3D actualPos = actual.getPosition();
        final Vector3D actualVel = actual.getVelocity();

        // sp3 files can have mm accuracy
        final double eps = 1e-3;

        Assertions.assertEquals(expectedPos.getX(), actualPos.getX(), eps);
        Assertions.assertEquals(expectedPos.getY(), actualPos.getY(), eps);
        Assertions.assertEquals(expectedPos.getZ(), actualPos.getZ(), eps);

        Assertions.assertEquals(expectedVel.getX(), actualVel.getX(), eps);
        Assertions.assertEquals(expectedVel.getY(), actualVel.getY(), eps);
        Assertions.assertEquals(expectedVel.getZ(), actualVel.getZ(), eps);

        Assertions.assertEquals(Vector3D.ZERO, actual.getAcceleration());
    }

    @Test
    public void testTruncatedLine() throws IOException {
        try {
            final String    ex     = "/sp3/truncated-line.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final Frame     frame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
            parser.parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(27, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testMissingEOF() throws IOException {
        try {
            final String    ex     = "/sp3/missing-eof.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final Frame     frame  = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
            parser.parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.SP3_UNEXPECTED_END_OF_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(24, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testWrongLineIdentifier() throws IOException {
        try {
            final String    ex     = "/sp3/wrong-line-identifier.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final Frame     frame  = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
            parser.parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                oe.getSpecifier());
            Assertions.assertEquals(13, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testBHN() throws IOException {
        final Frame       frame        = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final SP3Parser   parser       = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
        final String      ex           = "/sp3/esaBHN.sp3.Z";
        final DataSource   compressed   = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
        final SP3     file         = parser.parse(uncompressed);
        Assertions.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assertions.assertEquals("BHN",file.getOrbitTypeKey());
    }

    @Test
    public void testPRO() throws IOException {
        final Frame       frame        = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final SP3Parser   parser       = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
        final String      ex           = "/sp3/esaPRO.sp3.Z";
        final DataSource   compressed   = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
        final SP3     file         = parser.parse(uncompressed);
        Assertions.assertEquals(SP3OrbitType.EXT, file.getOrbitType());
        Assertions.assertEquals("PRO",file.getOrbitTypeKey());
    }

    @Test
    public void testUnknownType() throws IOException {
        final Frame       frame        = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final SP3Parser   parser       = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
        final String      ex           = "/sp3/unknownType.sp3.Z";
        final DataSource   compressed   = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final DataSource   uncompressed = new UnixCompressFilter().filter(compressed);
        final SP3     file         = parser.parse(uncompressed);
        Assertions.assertEquals(SP3OrbitType.OTHER, file.getOrbitType());
        Assertions.assertEquals("UKN",file.getOrbitTypeKey());
    }

    @Test
    public void testUnsupportedVersion() throws IOException {
        try {
            final String    ex     = "/sp3/unsupported-version.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final Frame     frame  = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
            parser.parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.SP3_UNSUPPORTED_VERSION,
                                oe.getSpecifier());
            Assertions.assertEquals('z', ((Character) oe.getParts()[0]).charValue());
        }

    }

    @Test
    public void testWrongNumberOfEpochs() throws IOException {
        try {
            final String    ex     = "/sp3/wrong-number-of-epochs.sp3";
            final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            final Frame     frame  = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
            final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
            parser.parse(source);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.SP3_NUMBER_OF_EPOCH_MISMATCH,
                                oe.getSpecifier());
            Assertions.assertEquals(  2, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertEquals(192, ((Integer) oe.getParts()[2]).intValue());
        }

    }

    @Test
    public void testIssue803() {

        // Test issue 803 (see https://gitlab.orekit.org/orekit/orekit/-/issues/803)
        final String    ex     = "/sp3/truncated-nsgf.orb.lageos2.160305.v35.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Coordinates
        final List<SP3Coordinate> coords = file.getSatellites().get("L52").getCoordinates();
        Assertions.assertEquals(1, coords.size());
        final SP3Coordinate coord = coords.get(0);

        // Verify
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());

        // PL52   2228.470946   7268.265924   9581.471543
        // VL52 -44856.945000  24321.151000  -7116.222800
        checkPVEntry(new PVCoordinates(new Vector3D(2228470.946, 7268265.924, 9581471.543),
                                       new Vector3D(-4485.6945000, 2432.1151000, -711.6222800)),
                     coord);
        Assertions.assertEquals(999999.999999, coord.getClockCorrection(), 1.0e-6);
        Assertions.assertEquals(999999.999999, coord.getClockRateChange(), 1.0e-6);

    }

    @Test
    public void testIssue827() {

        // Test issue 827 (see https://gitlab.orekit.org/orekit/orekit/-/issues/827)
        final String    ex     = "/sp3/truncated-nsgf.orb.lageos2.160305.v35.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Coordinates
        final List<SP3Coordinate> coords = file.getSatellites().get("L52").getCoordinates();
        Assertions.assertEquals(1, coords.size());
        final SP3Coordinate coord = coords.get(0);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());

        // 2016  2 28 0 0 0.00000000
        Assertions.assertEquals(new AbsoluteDate(2016, 2, 28, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord.getDate());


        // PL52   2228.470946   7268.265924   9581.471543
        // VL52 -44856.945000  24321.151000  -7116.222800
        checkPVEntry(new PVCoordinates(new Vector3D(2228470.946, 7268265.924, 9581471.543),
                                       new Vector3D(-4485.6945000, 2432.1151000, -711.6222800)),
                     coord);
        Assertions.assertEquals(999999.999999, coord.getClockCorrection(), 1.0e-6);
        Assertions.assertEquals(999999.999999, coord.getClockRateChange(), 1.0e-6);

    }

    @Test
    public void testIssue828() {

        // Test issue 828 (see https://gitlab.orekit.org/orekit/orekit/-/issues/828)
        final String    ex     = "/sp3/example-d-3.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.IRNSS, file.getType());

    }

    @Test
    public void testIssue828Bis() {

        // Test issue 828 (see https://gitlab.orekit.org/orekit/orekit/-/issues/828)
        final String    ex     = "/sp3/example-d-4.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.SBAS, file.getType());

    }

    @Test
    public void testIssue895HeaderComment() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-header-comment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());

    }

    @Test
    public void testIssue895ClockRecord() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-clock-record.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());
        Assertions.assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getCoordinates();
        Assertions.assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2021 12 26  0  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2021, 12, 26, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord.getDate());

        // PL51   5029.867893   1304.362160 -11075.527276 999999.999999
        // VL51 -17720.521773 -55720.482742 -14441.695083 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(5029867.893, 1304362.160, -11075527.276),
                                       new Vector3D(-1772.0521773, -5572.0482742, -1444.1695083)),
                     coord);

    }

    @Test
    public void testIssue895RolloverMinutes() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-minutes-increment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());
        Assertions.assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getCoordinates();
        Assertions.assertEquals(91, coords.size());

        final SP3Coordinate coord30 = coords.get(30);

        // 2016  7  6 16 60  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2016, 7, 6, 17, 0, 0,
                TimeScalesFactory.getUTC()), coord30.getDate());

        // PL51  11948.228978   2986.113872   -538.901114 999999.999999
        // VL51   4605.419303 -27972.588048 -53316.820671 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(11948228.978,   2986113.872,   -538901.114),
                                       new Vector3D(460.5419303, -2797.2588048, -5331.6820671)),
                     coord30);

        final SP3Coordinate coord31 = coords.get(31);

        // 2016  7  6 17  2  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2016, 7, 6, 17, 2, 0,
                TimeScalesFactory.getUTC()), coord31.getDate());

        // PL51  11982.652569   2645.786926  -1177.549463 999999.999999
        // VL51   1128.248622 -28724.293303 -53097.358387 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(11982652.569,   2645786.926,  -1177549.463),
                                       new Vector3D(112.8248622, -2872.4293303, -5309.7358387)),
                     coord31);

        final SP3Coordinate coord60 = coords.get(60);

        // 2016  7  6 17 60  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2016, 7, 6, 18, 0, 0,
                TimeScalesFactory.getUTC()), coord60.getDate());

        // PL51  -1693.056569  -4123.276630 -11431.599723 999999.999999
        // VL51 -59412.268951   4066.817074   7604.890337 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(-1693056.569,  -4123276.630, -11431599.723),
                                       new Vector3D(-5941.2268951,   406.6817074,   760.4890337)),
                     coord60);

    }

    @Test
    public void testIssue895RolloverHours() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-hours-increment.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());
        Assertions.assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getCoordinates();
        Assertions.assertEquals(61, coords.size());

        final SP3Coordinate coord30 = coords.get(30);

        // 2016  7  7 24  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2016, 7, 7, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord30.getDate());

        //PL51   2989.229334  -8494.421415   8385.068555
        //VL51 -19617.027447 -43444.824985 -36706.159070
        checkPVEntry(new PVCoordinates(new Vector3D(2989229.334,  -8494421.415,   8385068.555),
                                       new Vector3D(-1961.7027447, -4344.4824985, -3670.6159070)),
                     coord30);

        final SP3Coordinate coord31 = coords.get(31);

        // 2016  7  7  0  2  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2016, 7, 7, 0, 2, 0,
                TimeScalesFactory.getUTC()), coord31.getDate());

        // PL51   2744.983592  -9000.639164   7931.904779
        // VL51 -21072.925764 -40899.633288 -38801.567078
        checkPVEntry(new PVCoordinates(new Vector3D(2744983.592,  -9000639.164,   7931904.779),
                                       new Vector3D(-2107.2925764, -4089.9633288, -3880.1567078)),
                     coord31);

    }

    @Test
    public void testIssue895SecondDigits() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-second-digits.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());
        Assertions.assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getCoordinates();
        Assertions.assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2016  7  3  0  0  0.1234
        Assertions.assertEquals(new AbsoluteDate(2016, 7, 3, 0, 0, 0.1234,
                TimeScalesFactory.getUTC()), coord.getDate());

    }

    @Test
    public void testIssue895NoEOF() {

        // Test issue 895
        final String    ex     = "/sp3/issue895-no-eof.sp3";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final SP3   file   = new SP3Parser().parse(source);

        // Verify
        Assertions.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assertions.assertEquals(SP3.SP3FileType.LEO, file.getType());
        Assertions.assertEquals(1, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("L51").getCoordinates();
        Assertions.assertEquals(1, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2021 12 26  0  0  0.00000000
        Assertions.assertEquals(new AbsoluteDate(2021, 12, 26, 0, 0, 0,
                TimeScalesFactory.getUTC()), coord.getDate());

        // PL51   5029.867893   1304.362160 -11075.527276 999999.999999
        // VL51 -17720.521773 -55720.482742 -14441.695083 999999.999999
        checkPVEntry(new PVCoordinates(new Vector3D(5029867.893, 1304362.160, -11075527.276),
                                       new Vector3D(-1772.0521773, -5572.0482742, -1444.1695083)),
                     coord);

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
}
