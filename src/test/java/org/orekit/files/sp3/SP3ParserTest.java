/* Copyright 2002-2012 Space Applications Services
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.files.sp3.SP3File.SP3Coordinate;
import org.orekit.files.sp3.SP3File.SP3Ephemeris;
import org.orekit.files.sp3.SP3File.SP3OrbitType;
import org.orekit.files.sp3.SP3File.TimeSystem;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class SP3ParserTest {

    @Test
    public void testParseSP3a1() throws OrekitException, IOException, URISyntaxException {
        // simple test for version sp3-a, only contains position entries
        final String ex = "/sp3/sp3_a_example1.txt";

        final SP3Parser parser = new SP3Parser();
        final String fileName = getClass().getResource(ex).toURI().getPath();
        final SP3File file = (SP3File) parser.parse(fileName);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(25, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("1").getCoordinates();
        Assert.assertEquals(3, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        checkPVEntry(new PVCoordinates(new Vector3D(16258524.75, -3529015.75, -20611427.049),
                                       new Vector3D(0, 0, 0)),
                     coord);
        Assert.assertEquals("NGS", file.getAgency());
        Assert.assertEquals("ITR92", file.getCoordinateSystem());
        Assert.assertEquals("d", file.getDataUsed());
        Assert.assertEquals(0.0, file.getDayFraction(), 1.0e-15);
        Assert.assertEquals("1994-12-16T23:59:50.000", file.getEpoch().toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals(49703, file.getJulianDay());
        Assert.assertEquals(96, file.getNumberOfEpochs());
        Assert.assertEquals(900.0, file.getEpochInterval(), 1.0e-15);
        Assert.assertEquals(779, file.getGpsWeek());
        Assert.assertEquals(518400.0, file.getSecondsOfWeek(), 1.0e-10);
        Assert.assertEquals(25, file.getSatellites().size());
        Assert.assertEquals(SP3File.SP3FileType.UNDEFINED, file.getType());
        Assert.assertNull(file.getSatellites().get(null));
    }

    @Test
    public void testParseSP3a2() throws OrekitException, IOException {
        // simple test for version sp3-a, contains p/v entries
        final String ex = "/sp3/sp3_a_example2.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.FIT, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(25, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("1").getCoordinates();
        Assert.assertEquals(3, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 1994 12 17 0 0 0.00000000
        Assert.assertEquals(new AbsoluteDate(1994, 12, 17, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // P 1 16258.524750 -3529.015750 -20611.427050 -62.540600
        // V 1  -6560.373522  25605.954994  -9460.427179     -0.024236
        checkPVEntry(new PVCoordinates(new Vector3D(16258524.75, -3529015.75, -20611427.049),
                                       new Vector3D(-656.0373, 2560.5954, -946.0427)),
                     coord);
    }

    @Test
    public void testParseSP3c1() throws OrekitException, IOException {
        // simple test for version sp3-c, contains p/v entries
        final String ex = "/sp3/sp3_c_example1.txt";

        final SP3Parser parser = new SP3Parser();
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final SP3File file = parser.parse(inEntry);

        Assert.assertEquals(SP3OrbitType.HLM, file.getOrbitType());
        Assert.assertEquals(TimeSystem.GPS, file.getTimeSystem());

        Assert.assertEquals(26, file.getSatelliteCount());

        final List<SP3Coordinate> coords = file.getSatellites().get("G01").getCoordinates();
        Assert.assertEquals(2, coords.size());

        final SP3Coordinate coord = coords.get(0);

        // 2001  8  8  0  0  0.00000000
        Assert.assertEquals(new AbsoluteDate(2001, 8, 8, 0, 0, 0,
                TimeScalesFactory.getGPS()), coord.getDate());

        // PG01 -11044.805800 -10475.672350  21929.418200    189.163300 18 18 18 219
        checkPVEntry(new PVCoordinates(new Vector3D(-11044805.8, -10475672.35, 21929418.2),
                                       new Vector3D(0, 0, 0)),
                     coord);
    }

    @Test
    public void testSP3Propagator() throws Exception {
        // setup
        final String ex = "/sp3/sp3_a_example2.txt";
        final Frame frame = FramesFactory.getITRF(IERSConventions.IERS_2003, true);
        final SP3Parser parser = new SP3Parser(Constants.EIGEN5C_EARTH_MU, 3, s -> frame);
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        TimeScale gps = TimeScalesFactory.getGPS();

        // action
        final SP3File file = parser.parse(inEntry);

        // verify
        SP3Ephemeris ephemeris = file.getSatellites().get("1");
        BoundedPropagator propagator = ephemeris.getPropagator();
        Assert.assertEquals(propagator.getMinDate(), new AbsoluteDate(1994, 12, 17, gps));
        Assert.assertEquals(propagator.getMaxDate(), new AbsoluteDate(1994, 12, 17, 23, 45, 0, gps));
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
        Assert.assertEquals(propagator.getMinDate(), new AbsoluteDate(1994, 12, 17, gps));
        Assert.assertEquals(propagator.getMaxDate(), new AbsoluteDate(1994, 12, 17, 23, 45, 0, gps));
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

    private void checkPVEntry(final PVCoordinates expected, final PVCoordinates actual) {
        final Vector3D expectedPos = expected.getPosition();
        final Vector3D expectedVel = expected.getVelocity();

        final Vector3D actualPos = actual.getPosition();
        final Vector3D actualVel = actual.getVelocity();

        // sp3 files can have mm accuracy
        final double eps = 1e-3;

        Assert.assertEquals(expectedPos.getX(), actualPos.getX(), eps);
        Assert.assertEquals(expectedPos.getY(), actualPos.getY(), eps);
        Assert.assertEquals(expectedPos.getZ(), actualPos.getZ(), eps);

        Assert.assertEquals(expectedVel.getX(), actualVel.getX(), eps);
        Assert.assertEquals(expectedVel.getY(), actualVel.getY(), eps);
        Assert.assertEquals(expectedVel.getZ(), actualVel.getZ(), eps);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }
}
