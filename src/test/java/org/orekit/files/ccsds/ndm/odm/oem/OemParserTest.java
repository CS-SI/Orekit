/* Copyright 2002-2021 CS GROUP
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
package org.orekit.files.ccsds.ndm.odm.oem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.Array2DRowRealMatrix;
import org.hipparchus.util.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.frames.FactoryManagedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class OemParserTest {

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testIssue788() {

        // Read the file
        final String ex = "/ccsds/odm/oem/test.oem";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().buildOemParser();
        final Oem file = parser.parseMessage(source);

        // Verify
        Assert.assertEquals(file.getDataContext().getCelestialBodies().getEarth().getGM(), file.getSegments().get(0).getMu(), Double.MIN_VALUE);
        Assert.assertEquals(3.986004328969392E14, file.getSegments().get(0).getMu(), Double.MIN_VALUE);

    }

    @Test
    public void testParseOEM1() throws IOException {
        //
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem file = parser.parseMessage(source);
        Assert.assertEquals(3, file.getSegments().size());
        Assert.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assert.assertEquals("1996-062A", file.getSegments().get(0).getMetadata().getObjectID());
        Assert.assertEquals("MARS BARYCENTER", file.getSegments().get(0).getMetadata().getCenter().getName());
        Assert.assertEquals(1996, file.getSegments().get(0).getMetadata().getLaunchYear());
        Assert.assertEquals(62, file.getSegments().get(0).getMetadata().getLaunchNumber());
        Assert.assertEquals("A", file.getSegments().get(0).getMetadata().getLaunchPiece());
        Assert.assertNull(file.getSegments().get(0).getMetadata().getCenter().getBody());
        Assert.assertNull(file.getSegments().get(0).getMetadata().getCenter().getBody());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 00, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getUseableStopTime());
        Assert.assertEquals(InterpolationMethod.HERMITE, file.getSegments().get(0).getMetadata().getInterpolationMethod());
        Assert.assertEquals(7, file.getSegments().get(0).getMetadata().getInterpolationDegree());
        ArrayList<String> ephemeridesDataLinesComment = new ArrayList<String>();
        ephemeridesDataLinesComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 1996NOV 04. It is");
        ephemeridesDataLinesComment.add("to be used for DSN scheduling purposes only.");
        Assert.assertEquals(ephemeridesDataLinesComment, file.getSegments().get(0).getData().getComments());
        CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates
                                                  (new Vector3D(2789.619 * 1000, -280.045 * 1000, -1746.755 * 1000),
                                                   new Vector3D(4.73372 * 1000, -2.49586 * 1000, -1.04195 * 1000)),
                                                   FramesFactory.getEME2000(),
                                                   new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                                                   CelestialBodyFactory.getEarth().getGM());
        Assert.assertArrayEquals(orbit.getPVCoordinates().getPosition().toArray(), file.getSegments().get(0).getData().getEphemeridesDataLines().get(0).getPosition().toArray(), 1e-10);
        Assert.assertArrayEquals(orbit.getPVCoordinates().getVelocity().toArray(), file.getSegments().get(0).getData().getEphemeridesDataLines().get(0).getVelocity().toArray(), 1e-10);
        Assert.assertEquals(Vector3D.ZERO, file.getSegments().get(1).getData().getEphemeridesDataLines().get(1).getAcceleration());
        final Array2DRowRealMatrix covMatrix = new Array2DRowRealMatrix(6, 6);
        final double[] column1 = {
             3.3313494e-04,  4.6189273e-04, -3.0700078e-04, -3.3493650e-07, -2.2118325e-07, -3.0413460e-07
        };
        final double[] column2 = {
             4.6189273e-04,  6.7824216e-04, -4.2212341e-04, -4.6860842e-07, -2.8641868e-07, -4.9894969e-07
        };
        final double[] column3 = {
            -3.0700078e-04, -4.2212341e-04, 3.2319319e-04,  2.4849495e-07, 1.7980986e-07,  3.5403109e-07
        };
        final double[] column4 = {
            -3.3493650e-07, -4.6860842e-07, 2.4849495e-07,  4.29602280e-10, 2.6088992e-10,  1.86926319e-10
        };
        final double[] column5 = {
            -2.2118325e-07, -2.8641868e-07, 1.7980986e-07,  2.6088992e-10, 1.7675147e-10,  1.0088625e-10
        };
        final double[] column6 = {
            -3.0413460e-07, -4.9894969e-07, 3.5403109e-07,  1.8692631e-10, 1.0088625e-10,  6.2244443e-10
        };
        covMatrix.setColumn(0, column1);
        covMatrix.setColumn(1, column2);
        covMatrix.setColumn(2, column3);
        covMatrix.setColumn(3, column4);
        covMatrix.setColumn(4, column5);
        covMatrix.setColumn(5, column6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(covMatrix.getEntry(i, j) * 1.0e6,
                                    file.getSegments().get(2).getData().getCovarianceMatrices().get(0).getCovarianceMatrix().getEntry(i, j),
                                    1e-10);
            }
        }
        Assert.assertEquals(new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            file.getSegments().get(2).getCovarianceMatrices().get(0).getEpoch());
        Assert.assertEquals(LOFType.QSW,
                            file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asOrbitRelativeFrame().getLofType());
        Assert.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asFrame());
        Assert.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asCelestialBodyFrame());
        Assert.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asSpacecraftBodyFrame());
        Assert.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(1).getReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals(FramesFactory.getEME2000(),
                            file.getSegments().get(2).getCovarianceMatrices().get(1).getReferenceFrame().asFrame());
    }

    @Test
    public void testParseOEM2() throws URISyntaxException {

        final String ex = "/ccsds/odm/oem/OEMExample2.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final AbsoluteDate missionReferenceDate = new AbsoluteDate("1996-12-17T00:00:00.000", TimeScalesFactory.getUTC());
        OemParser parser = new ParserBuilder().
                           withConventions(IERSConventions.IERS_2010).
                           withSimpleEOP(true).
                           withDataContext(DataContext.getDefault()).
                           withMissionReferenceDate(missionReferenceDate).
                           withMu(CelestialBodyFactory.getMars().getGM()).
                           withDefaultInterpolationDegree(1).
                           buildOemParser();

        final Oem file = parser.parseMessage(source);
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("comment 1");
        metadataComment.add("comment 2");
        Assert.assertEquals(metadataComment, file.getSegments().get(0).getMetadata().getComments());
        Assert.assertEquals("TOD/2010 simple EOP",
                            file.getSegments().get(0).getMetadata().getReferenceFrame().asFrame().getName());
        Assert.assertEquals("TOD",
                            file.getSegments().get(0).getMetadata().getReferenceFrame().getName());
        Assert.assertEquals("EME2000", file.getSegments().get(1).getMetadata().getReferenceFrame().getName());
        List<OemSegment> blocks = file.getSegments();
        Assert.assertEquals(2, blocks.size());
        Assert.assertEquals(129600.331,
                            blocks.get(0).getMetadata().getFrameEpoch().durationFrom(missionReferenceDate),
                            1.0e-15);
        Assert.assertEquals(129600.331,
                            blocks.get(0).getMetadata().getStartTime().durationFrom(missionReferenceDate),
                            1.0e-15);
        Assert.assertEquals(941347.267,
                            blocks.get(1).getMetadata().getStartTime().durationFrom(missionReferenceDate),
                            1.0e-15);

    }

    @Test
    public void testParseOEM3KVN() throws IOException {

        final String ex = "/ccsds/odm/oem/OEMExample3.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem file = parser.parse(source); // using the generic API here
        Assert.assertEquals("Copy of OEMExample.txt with changes so that interpolation will work.",
                            file.getHeader().getComments().get(0));
        Assert.assertEquals(new AbsoluteDate("1996-11-04T17:22:31", TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assert.assertEquals("NASA/JPL", file.getHeader().getOriginator());
        Assert.assertEquals("OEM 201113719185", file.getHeader().getMessageId());
        Assert.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assert.assertEquals("1996-062A", file.getSegments().get(0).getMetadata().getObjectID());

        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals(true, file.getSatellites().containsKey("1996-062A"));
        Assert.assertEquals(false, file.getSatellites().containsKey("MARS GLOBAL SURVEYOR"));
        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals("1996-062A", file.getSatellites().values().iterator().next().getId());
        Assert.assertEquals(
                new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                file.getSegments().get(0).getMetadata().getStartTime());

        final OemSatelliteEphemeris satellite = file.getSatellites().get("1996-062A");
        Assert.assertEquals("1996-062A", satellite.getId());
        final OemSegment segment = (OemSegment) satellite.getSegments().get(0);
        Assert.assertEquals(CelestialBodyFactory.getMars().getGM(), segment.getMu(), 1.0);
        Assert.assertEquals("EME2000", segment.getMetadata().getReferenceFrame().getName());
        Assert.assertEquals(segment.getMetadata().getCenter().getName(), "MARS BARYCENTER");
        Assert.assertNull(segment.getMetadata().getCenter().getBody());
        // Frame not creatable since it's center can't be created.
        try {
            segment.getFrame();
            Assert.fail("Expected Exception");
        } catch (OrekitException e){
            Assert.assertEquals(e.getSpecifier(), OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY);
        }
        Assert.assertEquals("UTC", segment.getMetadata().getTimeSystem().name());
        Assert.assertEquals(InterpolationMethod.HERMITE, segment.getMetadata().getInterpolationMethod());
        Assert.assertEquals(2, segment.getMetadata().getInterpolationDegree());
        Assert.assertEquals(3, segment.getInterpolationSamples());
        Assert.assertEquals(segment.getAvailableDerivatives(), CartesianDerivativesFilter.USE_PV);
        // propagator can't be created since frame can't be created
        try {
            satellite.getPropagator();
            Assert.fail("Expected Exception");
        } catch (OrekitException e){
            Assert.assertEquals(e.getSpecifier(),
                    OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY);
        }

        List<OemSegment> segments = file.getSegments();
        Assert.assertEquals(3, segments.size());
        Assert.assertEquals(3, segments.get(2).getData().getCoordinates().size());
        final TimeStampedPVCoordinates pv20 = segments.get(2).getData().getCoordinates().get(0);
        Assert.assertEquals(
                            new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            pv20.getDate());
        Assert.assertEquals(-2432166.0,   pv20.getPosition().getX(), 1.0e-10);
        Assert.assertEquals(  -63042.0,   pv20.getPosition().getY(), 1.0e-10);
        Assert.assertEquals( 1742754.0,   pv20.getPosition().getZ(), 1.0e-10);
        Assert.assertEquals(    7337.02,  pv20.getVelocity().getX(), 1.0e-10);
        Assert.assertEquals(   -3495.867, pv20.getVelocity().getY(), 1.0e-10);
        Assert.assertEquals(   -1041.945, pv20.getVelocity().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv21 = segments.get(2).getData().getCoordinates().get(1);
        Assert.assertEquals(new AbsoluteDate("1996-12-28T21:59:02.267", TimeScalesFactory.getUTC()),
                            pv21.getDate());
        Assert.assertEquals(-2445234.0,   pv21.getPosition().getX(), 1.0e-10);
        Assert.assertEquals( -878141.0,   pv21.getPosition().getY(), 1.0e-10);
        Assert.assertEquals( 1873073.0,   pv21.getPosition().getZ(), 1.0e-10);
        Assert.assertEquals(    1860.43,  pv21.getVelocity().getX(), 1.0e-10);
        Assert.assertEquals(   -3421.256, pv21.getVelocity().getY(), 1.0e-10);
        Assert.assertEquals(    -996.366, pv21.getVelocity().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv22 = segments.get(2).getData().getCoordinates().get(2);
        Assert.assertEquals(new AbsoluteDate("1996-12-28T22:00:02.267", TimeScalesFactory.getUTC()),
                            pv22.getDate());
        Assert.assertEquals(-2458079.0,   pv22.getPosition().getX(), 1.0e-10);
        Assert.assertEquals( -683858.0,   pv22.getPosition().getY(), 1.0e-10);
        Assert.assertEquals( 2007684.0,   pv22.getPosition().getZ(), 1.0e-10);
        Assert.assertEquals(    6367.86,  pv22.getVelocity().getX(), 1.0e-10);
        Assert.assertEquals(   -3339.563, pv22.getVelocity().getY(), 1.0e-10);
        Assert.assertEquals(    -946.654, pv22.getVelocity().getZ(), 1.0e-10);

        Assert.assertEquals(2, segments.get(2).getCovarianceMatrices().size());
        final CartesianCovariance c20 = segments.get(2).getCovarianceMatrices().get(0);
        Assert.assertEquals(new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            c20.getEpoch());
        Assert.assertEquals(OrbitRelativeFrame.RTN, c20.getReferenceFrame().asOrbitRelativeFrame());
        Assert.assertEquals( 333.13494,       c20.getCovarianceMatrix().getEntry(0, 0), 1.0e-5);
        Assert.assertEquals( 461.89273,       c20.getCovarianceMatrix().getEntry(1, 0), 1.0e-5);
        Assert.assertEquals( 678.24216,       c20.getCovarianceMatrix().getEntry(1, 1), 1.0e-5);
        Assert.assertEquals(-307.00078,       c20.getCovarianceMatrix().getEntry(2, 0), 1.0e-5);
        Assert.assertEquals(-422.12341,       c20.getCovarianceMatrix().getEntry(2, 1), 1.0e-5);
        Assert.assertEquals( 323.19319,       c20.getCovarianceMatrix().getEntry(2, 2), 1.0e-5);
        Assert.assertEquals(  -0.33493650,    c20.getCovarianceMatrix().getEntry(3, 0), 1.0e-8);
        Assert.assertEquals(  -0.46860842,    c20.getCovarianceMatrix().getEntry(3, 1), 1.0e-8);
        Assert.assertEquals(   0.24849495,    c20.getCovarianceMatrix().getEntry(3, 2), 1.0e-8);
        Assert.assertEquals(   0.00042960228, c20.getCovarianceMatrix().getEntry(3, 3), 1.0e-11);
        Assert.assertEquals(  -0.22118325,    c20.getCovarianceMatrix().getEntry(4, 0), 1.0e-8);
        Assert.assertEquals(  -0.28641868,    c20.getCovarianceMatrix().getEntry(4, 1), 1.0e-8);
        Assert.assertEquals(   0.17980986,    c20.getCovarianceMatrix().getEntry(4, 2), 1.0e-8);
        Assert.assertEquals(   0.00026088992, c20.getCovarianceMatrix().getEntry(4, 3), 1.0e-11);
        Assert.assertEquals(   0.00017675147, c20.getCovarianceMatrix().getEntry(4, 4), 1.0e-11);
        Assert.assertEquals(  -0.30413460,    c20.getCovarianceMatrix().getEntry(5, 0), 1.0e-8);
        Assert.assertEquals(  -0.49894969,    c20.getCovarianceMatrix().getEntry(5, 1), 1.0e-8);
        Assert.assertEquals(   0.35403109,    c20.getCovarianceMatrix().getEntry(5, 2), 1.0e-8);
        Assert.assertEquals(   0.00018692631, c20.getCovarianceMatrix().getEntry(5, 3), 1.0e-11);
        Assert.assertEquals(   0.00010088625, c20.getCovarianceMatrix().getEntry(5, 4), 1.0e-11);
        Assert.assertEquals(   0.00062244443, c20.getCovarianceMatrix().getEntry(5, 5), 1.0e-11);
        for (int i = 0; i < c20.getCovarianceMatrix().getRowDimension(); ++i) {
            for (int j = i + 1; j < c20.getCovarianceMatrix().getColumnDimension(); ++j) {
                Assert.assertEquals(c20.getCovarianceMatrix().getEntry(j, i),
                                    c20.getCovarianceMatrix().getEntry(i, j),
                                    1.0e-10);
            }
        }

        final CartesianCovariance c21 = segments.get(2).getCovarianceMatrices().get(1);
        Assert.assertEquals(new AbsoluteDate("1996-12-29T21:00:00", TimeScalesFactory.getUTC()),
                            c21.getEpoch());
        Assert.assertEquals(CelestialBodyFrame.EME2000, c21.getReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals( 344.24505,       c21.getCovarianceMatrix().getEntry(0, 0), 1.0e-5);
        Assert.assertEquals( 450.78162,       c21.getCovarianceMatrix().getEntry(1, 0), 1.0e-5);
        Assert.assertEquals( 689.35327,       c21.getCovarianceMatrix().getEntry(1, 1), 1.0e-5);
        for (int i = 0; i < c21.getCovarianceMatrix().getRowDimension(); ++i) {
            for (int j = i + 1; j < c21.getCovarianceMatrix().getColumnDimension(); ++j) {
                Assert.assertEquals(c21.getCovarianceMatrix().getEntry(j, i),
                                    c21.getCovarianceMatrix().getEntry(i, j),
                                    1.0e-10);
            }
        }

    }

    @Test
    public void testParseOEM3XML() throws IOException {

        final String ex = "/ccsds/odm/oem/OEMExample3.xml";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem file = parser.parseMessage(source);
        Assert.assertEquals("OEM 201113719185", file.getHeader().getMessageId());
        Assert.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assert.assertEquals("2000-028A", file.getSegments().get(0).getMetadata().getObjectID());

        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals(true, file.getSatellites().containsKey("2000-028A"));
        Assert.assertEquals(false, file.getSatellites().containsKey("MARS GLOBAL SURVEYOR"));
        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals("2000-028A", file.getSatellites().values().iterator().next().getId());
        Assert.assertEquals(
                new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                file.getSegments().get(0).getMetadata().getStartTime());

        final OemSatelliteEphemeris satellite = file.getSatellites().get("2000-028A");
        Assert.assertEquals("2000-028A", satellite.getId());
        final OemSegment segment = (OemSegment) satellite.getSegments().get(0);
        Assert.assertEquals(CelestialBodyFactory.getMars().getGM(), segment.getMu(), 1.0);
        Assert.assertEquals("J2000", segment.getMetadata().getReferenceFrame().getName());
        Assert.assertEquals(segment.getMetadata().getCenter().getName(), "MARS BARYCENTER");
        Assert.assertNull(segment.getMetadata().getCenter().getBody());
        // Frame not creatable since it's center can't be created.
        try {
            segment.getFrame();
            Assert.fail("Expected Exception");
        } catch (OrekitException e){
            Assert.assertEquals(e.getSpecifier(), OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY);
        }
        Assert.assertEquals("UTC", segment.getMetadata().getTimeSystem().name());
        Assert.assertEquals(InterpolationMethod.HERMITE, segment.getMetadata().getInterpolationMethod());
        Assert.assertEquals(7, segment.getMetadata().getInterpolationDegree());
        Assert.assertEquals(segment.getAvailableDerivatives(), CartesianDerivativesFilter.USE_PVA);

        List<OemSegment> segments = file.getSegments();
        Assert.assertEquals(1, segments.size());
        Assert.assertEquals("Produced by M.R. Sombedody, MSOO NAV/JPL, 1996 OCT 11. It is", segments.get(0).getData().getComments().get(0));
        Assert.assertEquals("to be used for DSN scheduling purposes only.", segments.get(0).getData().getComments().get(1));
        Assert.assertEquals(4, segments.get(0).getData().getCoordinates().size());
        final TimeStampedPVCoordinates pv00 = segments.get(0).getData().getCoordinates().get(0);
        Assert.assertEquals(new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                            pv00.getDate());
        Assert.assertEquals( 2789600.0, pv00.getPosition().getX(),     1.0e-10);
        Assert.assertEquals( -280000.0, pv00.getPosition().getY(),     1.0e-10);
        Assert.assertEquals(-1746800.0, pv00.getPosition().getZ(),     1.0e-10);
        Assert.assertEquals(    4730.0, pv00.getVelocity().getX(),     1.0e-10);
        Assert.assertEquals(   -2500.0, pv00.getVelocity().getY(),     1.0e-10);
        Assert.assertEquals(   -1040.0, pv00.getVelocity().getZ(),     1.0e-10);
        Assert.assertEquals(       8.0, pv00.getAcceleration().getX(), 1.0e-10);
        Assert.assertEquals(       1.0, pv00.getAcceleration().getY(), 1.0e-10);
        Assert.assertEquals(    -159.0, pv00.getAcceleration().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv01 = segments.get(0).getData().getCoordinates().get(1);
        Assert.assertEquals(new AbsoluteDate("1996-12-18T12:01:00.331", TimeScalesFactory.getUTC()),
                            pv01.getDate());
        Assert.assertEquals( 2783400.0, pv01.getPosition().getX(),     1.0e-10);
        Assert.assertEquals( -308100.0, pv01.getPosition().getY(),     1.0e-10);
        Assert.assertEquals(-1877100.0, pv01.getPosition().getZ(),     1.0e-10);
        Assert.assertEquals(    5190.0, pv01.getVelocity().getX(),     1.0e-10);
        Assert.assertEquals(   -2420.0, pv01.getVelocity().getY(),     1.0e-10);
        Assert.assertEquals(   -2000.0, pv01.getVelocity().getZ(),     1.0e-10);
        Assert.assertEquals(       8.0, pv01.getAcceleration().getX(), 1.0e-10);
        Assert.assertEquals(       1.0, pv01.getAcceleration().getY(), 1.0e-10);
        Assert.assertEquals(       1.0, pv01.getAcceleration().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv02 = segments.get(0).getData().getCoordinates().get(2);
        Assert.assertEquals(new AbsoluteDate("1996-12-18T12:02:00.331", TimeScalesFactory.getUTC()),
                            pv02.getDate());
        Assert.assertEquals( 2776000.0, pv02.getPosition().getX(),     1.0e-10);
        Assert.assertEquals( -336900.0, pv02.getPosition().getY(),     1.0e-10);
        Assert.assertEquals(-2008700.0, pv02.getPosition().getZ(),     1.0e-10);
        Assert.assertEquals(    5640.0, pv02.getVelocity().getX(),     1.0e-10);
        Assert.assertEquals(   -2340.0, pv02.getVelocity().getY(),     1.0e-10);
        Assert.assertEquals(   -1950.0, pv02.getVelocity().getZ(),     1.0e-10);
        Assert.assertEquals(       8.0, pv02.getAcceleration().getX(), 1.0e-10);
        Assert.assertEquals(       1.0, pv02.getAcceleration().getY(), 1.0e-10);
        Assert.assertEquals(     159.0, pv02.getAcceleration().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv03 = segments.get(0).getData().getCoordinates().get(3);
        Assert.assertEquals(new AbsoluteDate("1996-12-28T21:28:00.331", TimeScalesFactory.getUTC()),
                            pv03.getDate());
        Assert.assertEquals(-3881000.0, pv03.getPosition().getX(),     1.0e-10);
        Assert.assertEquals(  564000.0, pv03.getPosition().getY(),     1.0e-10);
        Assert.assertEquals( -682800.0, pv03.getPosition().getZ(),     1.0e-10);
        Assert.assertEquals(   -3290.0, pv03.getVelocity().getX(),     1.0e-10);
        Assert.assertEquals(   -3670.0, pv03.getVelocity().getY(),     1.0e-10);
        Assert.assertEquals(    1640.0, pv03.getVelocity().getZ(),     1.0e-10);
        Assert.assertEquals(      -3.0, pv03.getAcceleration().getX(), 1.0e-10);
        Assert.assertEquals(       0.0, pv03.getAcceleration().getY(), 1.0e-10);
        Assert.assertEquals(       0.0, pv03.getAcceleration().getZ(), 1.0e-10);

        Assert.assertEquals(1, segments.get(0).getCovarianceMatrices().size());
        final CartesianCovariance c20 = segments.get(0).getCovarianceMatrices().get(0);
        Assert.assertEquals(new AbsoluteDate("1996-12-28T22:28:00.331", TimeScalesFactory.getUTC()),
                            c20.getEpoch());
        Assert.assertEquals(CelestialBodyFrame.ITRF1997, c20.getReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals( 316000.0, c20.getCovarianceMatrix().getEntry(0, 0), 1.0e-10);
        Assert.assertEquals( 722000.0, c20.getCovarianceMatrix().getEntry(1, 0), 1.0e-10);
        Assert.assertEquals( 518000.0, c20.getCovarianceMatrix().getEntry(1, 1), 1.0e-10);
        Assert.assertEquals( 202000.0, c20.getCovarianceMatrix().getEntry(2, 0), 1.0e-10);
        Assert.assertEquals( 715000.0, c20.getCovarianceMatrix().getEntry(2, 1), 1.0e-10);
        Assert.assertEquals(   2000.0, c20.getCovarianceMatrix().getEntry(2, 2), 1.0e-10);
        Assert.assertEquals( 912000.0, c20.getCovarianceMatrix().getEntry(3, 0), 1.0e-10);
        Assert.assertEquals( 306000.0, c20.getCovarianceMatrix().getEntry(3, 1), 1.0e-10);
        Assert.assertEquals( 276000.0, c20.getCovarianceMatrix().getEntry(3, 2), 1.0e-10);
        Assert.assertEquals( 797000.0, c20.getCovarianceMatrix().getEntry(3, 3), 1.0e-10);
        Assert.assertEquals( 562000.0, c20.getCovarianceMatrix().getEntry(4, 0), 1.0e-10);
        Assert.assertEquals( 899000.0, c20.getCovarianceMatrix().getEntry(4, 1), 1.0e-10);
        Assert.assertEquals(  22000.0, c20.getCovarianceMatrix().getEntry(4, 2), 1.0e-10);
        Assert.assertEquals(  79000.0, c20.getCovarianceMatrix().getEntry(4, 3), 1.0e-10);
        Assert.assertEquals( 415000.0, c20.getCovarianceMatrix().getEntry(4, 4), 1.0e-10);
        Assert.assertEquals( 245000.0, c20.getCovarianceMatrix().getEntry(5, 0), 1.0e-10);
        Assert.assertEquals( 965000.0, c20.getCovarianceMatrix().getEntry(5, 1), 1.0e-10);
        Assert.assertEquals( 950000.0, c20.getCovarianceMatrix().getEntry(5, 2), 1.0e-10);
        Assert.assertEquals( 435000.0, c20.getCovarianceMatrix().getEntry(5, 3), 1.0e-10);
        Assert.assertEquals( 621000.0, c20.getCovarianceMatrix().getEntry(5, 4), 1.0e-10);
        Assert.assertEquals( 991000.0, c20.getCovarianceMatrix().getEntry(5, 5), 1.0e-10);
        for (int i = 0; i < c20.getCovarianceMatrix().getRowDimension(); ++i) {
            for (int j = i + 1; j < c20.getCovarianceMatrix().getColumnDimension(); ++j) {
                Assert.assertEquals(c20.getCovarianceMatrix().getEntry(j, i),
                                    c20.getCovarianceMatrix().getEntry(i, j),
                                    1.0e-10);
            }
        }

    }

    @Test
    public void testParseOemMissingOptionalData() throws IOException {

        final String ex = "/ccsds/odm/oem/OEMExample6.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem file = parser.parseMessage(source);
        Assert.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assert.assertEquals("1996-062A", file.getSegments().get(0).getMetadata().getObjectID());

        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals(true, file.getSatellites().containsKey("1996-062A"));
        Assert.assertEquals(false, file.getSatellites().containsKey("MARS GLOBAL SURVEYOR"));
        Assert.assertEquals(1, file.getSatellites().size());
        Assert.assertEquals("1996-062A", file.getSatellites().values().iterator().next().getId());
        Assert.assertEquals(
                new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                file.getSegments().get(0).getMetadata().getStartTime());

        OemSatelliteEphemeris satellite = file.getSatellites().get("1996-062A");
        Assert.assertEquals("1996-062A", satellite.getId());
        OemSegment segment = satellite.getSegments().get(0);
        Assert.assertEquals(CelestialBodyFactory.getMars().getGM(), segment.getMu(), 1.0);
        FactoryManagedFrame eme2000 = FramesFactory.getEME2000();
        Frame actualFrame = segment.getFrame();
        AbsoluteDate actualStart = satellite.getStart();
        Transform actualTransform = eme2000.getTransformTo(actualFrame, actualStart);
        CelestialBody mars = CelestialBodyFactory.getMars();
        TimeStampedPVCoordinates marsPV = mars.getPVCoordinates(actualStart, eme2000);
        TimeStampedPVCoordinates marsPV_in_marscentered_frame = mars.getPVCoordinates(actualStart, actualFrame);
        MatcherAssert.assertThat(marsPV_in_marscentered_frame,
                                 OrekitMatchers.pvCloseTo(PVCoordinates.ZERO, 1e-3));
        Assert.assertEquals(actualTransform.getTranslation(), marsPV.getPosition().negate());
        Assert.assertEquals(actualTransform.getVelocity(), marsPV.getVelocity().negate());
        Assert.assertEquals(actualTransform.getAcceleration(), marsPV.getAcceleration().negate());
        Assert.assertEquals(
                Rotation.distance(actualTransform.getRotation(), Rotation.IDENTITY),
                0.0, 0.0);
        Assert.assertEquals(actualTransform.getRotationRate(), Vector3D.ZERO);
        Assert.assertEquals(actualTransform.getRotationAcceleration(), Vector3D.ZERO);
        Assert.assertEquals("Mars/EME2000", actualFrame.getName());
        Assert.assertEquals(CelestialBodyFrame.EME2000, segment.getMetadata().getReferenceFrame().asCelestialBodyFrame());
        Assert.assertEquals("UTC", segment.getMetadata().getTimeSystem().name());
        Assert.assertEquals(segment.getAvailableDerivatives(),
                CartesianDerivativesFilter.USE_PV);
        Assert.assertEquals(satellite.getSegments().get(0).getMetadata().getStartTime(), actualStart);
        Assert.assertEquals(satellite.getSegments().get(2).getMetadata().getStopTime(), satellite.getStop());

        final BoundedPropagator propagator = satellite.getPropagator();
        Assert.assertEquals(propagator.getMinDate(), satellite.getStart());
        Assert.assertEquals(propagator.getMinDate(), satellite.getSegments().get(0).getStart());
        Assert.assertEquals(propagator.getMaxDate(), satellite.getStop());
        Assert.assertEquals(propagator.getMaxDate(), satellite.getSegments().get(2).getStop());

        final List<TimeStampedPVCoordinates> dataLines = new ArrayList<>();
        for (OemSegment block : file.getSegments()) {
            for (TimeStampedPVCoordinates dataLine : block.getData().getEphemeridesDataLines()) {
                if (dataLine.getDate().compareTo(satellite.getStart()) >= 0) {
                    dataLines.add(dataLine);
                }
            }
        }

        final int ulps = 12;
        for (TimeStampedPVCoordinates coord : dataLines) {
            MatcherAssert.assertThat(propagator.getPVCoordinates(coord.getDate(), actualFrame),
                                     OrekitMatchers.pvCloseTo(coord, ulps));
            MatcherAssert.assertThat(propagator.propagate(coord.getDate()).getPVCoordinates(),
                                     OrekitMatchers.pvCloseTo(coord, ulps));
        }

    }

    @Test
    public void testWrongODMType() {
        try {
            final String ex = "/ccsds/odm/oem/OEMExample1.txt";
            final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assert.assertEquals("OPMExample1.txt", oe.getParts()[0]);
        }
    }

    @Test
    public void testEphemerisNumberFormatErrorType() {
        final String ex = "/ccsds/odm/oem/OEM-ephemeris-number-format-error.txt";
        try {
            final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(44, oe.getParts()[0]);
            Assert.assertEquals(ex, oe.getParts()[1]);
            Assert.assertEquals("1996-12-28T21:59:02.267 -2445.234 -878.141 this-is-not-a-number 1.86043 -3.421256 -0.996366", oe.getParts()[2]);
        }
    }

    @Test
    public void testCovarianceNumberFormatErrorType() {
        final String ex = "/ccsds/odm/oem/OEM-covariance-number-format-error.txt";
        try {
            final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(52, oe.getParts()[0]);
            Assert.assertEquals(ex, oe.getParts()[1]);
            Assert.assertEquals("4.6189273e-04 this-is-not-a-number", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/odm/oem/OEMExample1.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        final DataSource source =  new DataSource(wrongName, () -> getClass().getResourceAsStream(wrongName));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentTimeSystems() {
        try {
            final String ex = "/ccsds/odm/oem/OEM-inconsistent-time-systems.txt";
            final DataSource source =  new DataSource(ex, () -> getClass().getResourceAsStream(ex));
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals("UTC", oe.getParts()[0]);
            Assert.assertEquals("TCG", oe.getParts()[1]);
        }
    }

    @Test
    public void testLowerCaseValue() {
        //setup
        String file = "/ccsds/odm/oem/oemLowerCaseValue.oem";
        final DataSource source =  new DataSource(file, () -> getClass().getResourceAsStream(file));

        //action
        final Oem actual = new ParserBuilder().
                               withMu(CelestialBodyFactory.getMars().getGM()).
                               buildOemParser().
                               parseMessage(source);

        //verify
        Assert.assertEquals(
                CelestialBodyFactory.getEarth(),
                actual.getSegments().get(0).getMetadata().getCenter().getBody());
    }

    @Test
    public void testWrongKeyword()
        throws URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = "/ccsds/odm/oem/OEM-wrong-keyword.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(19, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
        }
    }

    @Test
    public void testKeywordWithinEphemeris()
        throws URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = "/ccsds/odm/oem/OEM-keyword-within-ephemeris.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(24, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("USER_DEFINED_TEST_KEY"));
        }
    }

    @Test
    public void testKeywordWithinCovariance()
        throws URISyntaxException {
        // simple test for OMM file, contains p/v entries and other mandatory
        // data.
        final String name = "/ccsds/odm/oem/OEM-keyword-within-covariance.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(91, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("USER_DEFINED_TEST_KEY"));
        }
    }

    @Test
    public void testTooLargeCovarianceDimension()
        throws URISyntaxException {
        final String name = "/ccsds/odm/oem/OEM-too-large-covariance-dimension.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(91, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("1.0e-12"));
        }
    }

    @Test
    public void testTooSmallCovarianceDimension()
        throws URISyntaxException {
        final String name = "/ccsds/odm/oem/OEM-too-small-covariance-dimension.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("EPOCH", oe.getParts()[0]);
            Assert.assertEquals(89, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).endsWith("OEM-too-small-covariance-dimension.txt"));
        }
    }

    @Test
    public void testTooManyCovarianceColumns()
        throws URISyntaxException {
        final String name = "/ccsds/odm/oem/OEM-too-many-covariance-columns.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(51, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("3.3313494e-04"));
        }
    }

    @Test
    public void testTooFewCovarianceColumns()
        throws URISyntaxException {
        final String name = "/ccsds/odm/oem/OEM-too-few-covariance-columns.txt";
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        try {
            new ParserBuilder().
            withMu(CelestialBodyFactory.getMars().getGM()).
            buildOemParser().
            parseMessage(source);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(55, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertTrue(((String) oe.getParts()[2]).startsWith("-2.2118325e-07"));
        }
    }

    /**
     * Check if the parser enters the correct interpolation degree
     * (the parsed one or the default if there is none)
     */
    @Test
    public void testDefaultInterpolationDegree()
        throws URISyntaxException {

        final String name = "/ccsds/odm/oem/OEMExample8.txt";

        final ParserBuilder builder = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM());

        final DataSource source1 =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Oem file1 = builder.buildOemParser().parseMessage(source1);
        Assert.assertEquals(1, file1.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assert.assertEquals(7, file1.getSegments().get(1).getMetadata().getInterpolationDegree());

        final DataSource source2 =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Oem file2 = builder.withDefaultInterpolationDegree(5).buildOemParser().parseMessage(source2);
        Assert.assertEquals(5, file2.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assert.assertEquals(7, file2.getSegments().get(1).getMetadata().getInterpolationDegree());
    }

    /**
     * Check the parser can parse several ITRF frames. Test case for #361.
     */
    @Test
    public void testITRFFrames() {
        // setup
        Charset utf8 = StandardCharsets.UTF_8;
        IERSConventions conventions = IERSConventions.IERS_2010;
        boolean simpleEop = true;
        OemParser parser  = new ParserBuilder().
                            withMu(CelestialBodyFactory.getMars().getGM()).
                            buildOemParser();

        // frames to check
        List<Pair<String, Frame>> frames = new ArrayList<>();
        frames.add(new Pair<>("ITRF-1993", FramesFactory.getITRF(ITRFVersion.ITRF_1993, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF-1997", FramesFactory.getITRF(ITRFVersion.ITRF_1997, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2000",  FramesFactory.getITRF(ITRFVersion.ITRF_2000, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2005",  FramesFactory.getITRF(ITRFVersion.ITRF_2005, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2008",  FramesFactory.getITRF(ITRFVersion.ITRF_2008, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2014",  FramesFactory.getITRF(ITRFVersion.ITRF_2014, conventions, simpleEop)));

        for (Pair<String, Frame> frame : frames) {
            final String frameName = frame.getFirst();

            InputStream pre    = OemParserTest.class.getResourceAsStream("/ccsds/odm/oem/OEMExample7.txt.pre");
            InputStream middle = new ByteArrayInputStream(("REF_FRAME = " + frameName).getBytes(utf8));
            InputStream post   = OemParserTest.class.getResourceAsStream("/ccsds/odm/oem/OEMExample7.txt.post");
            DataSource   source = new DataSource("<patched>", () -> new SequenceInputStream(pre, new SequenceInputStream(middle, post)));

            // action
            Oem actual = parser.parseMessage(source);

            // verify
            OemSegment segment = actual.getSegments().get(0);
            Assert.assertEquals(frameName.replace("-", ""),
                                segment.getMetadata().getReferenceFrame().getName());
            // check expected frame
            Frame actualFrame = segment.getFrame();
            Frame expectedFrame = frame.getSecond();
            Assert.assertEquals(expectedFrame, actualFrame);
            Assert.assertEquals(expectedFrame.getTransformProvider(),
                                actualFrame.getTransformProvider());
        }
    }

}
