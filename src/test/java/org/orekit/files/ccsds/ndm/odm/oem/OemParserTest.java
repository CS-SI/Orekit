/* Copyright 2002-2023 CS GROUP
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
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
        Assertions.assertEquals(file.getDataContext().getCelestialBodies().getEarth().getGM(), file.getSegments().get(0).getMu(), Double.MIN_VALUE);
        Assertions.assertEquals(3.986004328969392E14, file.getSegments().get(0).getMu(), Double.MIN_VALUE);

    }

    @Test
    public void testParseOEM1() throws IOException {
        //
        final String ex = "/ccsds/odm/oem/OEMExample1.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem file = parser.parseMessage(source);
        Assertions.assertEquals("public, test-data", file.getHeader().getClassification());
        Assertions.assertEquals(3, file.getSegments().size());
        Assertions.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assertions.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assertions.assertEquals("1996-062A", file.getSegments().get(0).getMetadata().getObjectID());
        Assertions.assertEquals("MARS BARYCENTER", file.getSegments().get(0).getMetadata().getCenter().getName());
        Assertions.assertEquals(1996, file.getSegments().get(0).getMetadata().getLaunchYear());
        Assertions.assertEquals(62, file.getSegments().get(0).getMetadata().getLaunchNumber());
        Assertions.assertEquals("A", file.getSegments().get(0).getMetadata().getLaunchPiece());
        Assertions.assertNull(file.getSegments().get(0).getMetadata().getCenter().getBody());
        Assertions.assertNull(file.getSegments().get(0).getMetadata().getCenter().getBody());
        Assertions.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 00, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getStartTime());
        Assertions.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getStopTime());
        Assertions.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getUseableStartTime());
        Assertions.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.331, TimeScalesFactory.getUTC()),
                            file.getSegments().get(0).getMetadata().getUseableStopTime());
        Assertions.assertEquals(InterpolationMethod.HERMITE, file.getSegments().get(0).getMetadata().getInterpolationMethod());
        Assertions.assertEquals(7, file.getSegments().get(0).getMetadata().getInterpolationDegree());
        ArrayList<String> ephemeridesDataLinesComment = new ArrayList<String>();
        ephemeridesDataLinesComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 1996NOV 04. It is");
        ephemeridesDataLinesComment.add("to be used for DSN scheduling purposes only.");
        Assertions.assertEquals(ephemeridesDataLinesComment, file.getSegments().get(0).getData().getComments());
        CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates
                                                  (new Vector3D(2789.619 * 1000, -280.045 * 1000, -1746.755 * 1000),
                                                   new Vector3D(4.73372 * 1000, -2.49586 * 1000, -1.04195 * 1000)),
                                                   FramesFactory.getEME2000(),
                                                   new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                                                   CelestialBodyFactory.getEarth().getGM());
        Assertions.assertArrayEquals(orbit.getPosition().toArray(),
                                     file.getSegments().get(0).getData().getEphemeridesDataLines().get(0).getPosition().toArray(), 1e-10);
        Assertions.assertArrayEquals(orbit.getPVCoordinates().getVelocity().toArray(), file.getSegments().get(0).getData().getEphemeridesDataLines().get(0).getVelocity().toArray(), 1e-10);
        Assertions.assertEquals(Vector3D.ZERO, file.getSegments().get(1).getData().getEphemeridesDataLines().get(1).getAcceleration());
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
                Assertions.assertEquals(covMatrix.getEntry(i, j) * 1.0e6,
                                    file.getSegments().get(2).getData().getCovarianceMatrices().get(0).getCovarianceMatrix().getEntry(i, j),
                                    1e-10);
            }
        }
        Assertions.assertEquals(new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            file.getSegments().get(2).getCovarianceMatrices().get(0).getEpoch());
        Assertions.assertEquals(LOFType.QSW,
                            file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asOrbitRelativeFrame().getLofType());
        Assertions.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asFrame());
        Assertions.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(0).getReferenceFrame().asSpacecraftBodyFrame());
        Assertions.assertNull(file.getSegments().get(2).getCovarianceMatrices().get(1).getReferenceFrame().asOrbitRelativeFrame());
        Assertions.assertEquals(FramesFactory.getEME2000(),
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
        Assertions.assertEquals(headerComment, file.getHeader().getComments());
        final List<String> metadataComment = new ArrayList<String>();
        metadataComment.add("comment 1");
        metadataComment.add("comment 2");
        Assertions.assertEquals(metadataComment, file.getSegments().get(0).getMetadata().getComments());
        Assertions.assertEquals("TOD/2010 simple EOP",
                            file.getSegments().get(0).getMetadata().getReferenceFrame().asFrame().getName());
        Assertions.assertEquals("TOD",
                            file.getSegments().get(0).getMetadata().getReferenceFrame().getName());
        Assertions.assertEquals("EME2000", file.getSegments().get(1).getMetadata().getReferenceFrame().getName());
        List<OemSegment> blocks = file.getSegments();
        Assertions.assertEquals(2, blocks.size());
        Assertions.assertEquals(129600.331,
                            blocks.get(0).getMetadata().getFrameEpoch().durationFrom(missionReferenceDate),
                            1.0e-15);
        Assertions.assertEquals(129600.331,
                            blocks.get(0).getMetadata().getStartTime().durationFrom(missionReferenceDate),
                            1.0e-15);
        Assertions.assertEquals(941347.267,
                            blocks.get(1).getMetadata().getStartTime().durationFrom(missionReferenceDate),
                            1.0e-15);

    }

    @Test
    public void testParseOEM3KVN() throws IOException {

        final String ex = "/ccsds/odm/oem/OEMExample3.txt";
        final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));
        final OemParser parser  = new ParserBuilder().withMu(CelestialBodyFactory.getMars().getGM()).buildOemParser();
        final Oem file = parser.parse(source); // using the generic API here
        Assertions.assertEquals("Copy of OEMExample.txt with changes so that interpolation will work.",
                            file.getHeader().getComments().get(0));
        Assertions.assertEquals(new AbsoluteDate("1996-11-04T17:22:31", TimeScalesFactory.getUTC()),
                            file.getHeader().getCreationDate());
        Assertions.assertEquals("NASA/JPL", file.getHeader().getOriginator());
        Assertions.assertEquals("OEM 201113719185", file.getHeader().getMessageId());
        Assertions.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assertions.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assertions.assertEquals("1996-062A", file.getSegments().get(0).getMetadata().getObjectID());

        Assertions.assertEquals(1, file.getSatellites().size());
        Assertions.assertEquals(true, file.getSatellites().containsKey("1996-062A"));
        Assertions.assertEquals(false, file.getSatellites().containsKey("MARS GLOBAL SURVEYOR"));
        Assertions.assertEquals(1, file.getSatellites().size());
        Assertions.assertEquals("1996-062A", file.getSatellites().values().iterator().next().getId());
        Assertions.assertEquals(
                new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                file.getSegments().get(0).getMetadata().getStartTime());

        final OemSatelliteEphemeris satellite = file.getSatellites().get("1996-062A");
        Assertions.assertEquals("1996-062A", satellite.getId());
        final OemSegment segment = (OemSegment) satellite.getSegments().get(0);
        Assertions.assertEquals(CelestialBodyFactory.getMars().getGM(), segment.getMu(), 1.0);
        Assertions.assertEquals("EME2000", segment.getMetadata().getReferenceFrame().getName());
        Assertions.assertEquals(segment.getMetadata().getCenter().getName(), "MARS BARYCENTER");
        Assertions.assertNull(segment.getMetadata().getCenter().getBody());
        // Frame not creatable since it's center can't be created.
        try {
            segment.getFrame();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e){
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY);
        }
        Assertions.assertEquals("UTC", segment.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(InterpolationMethod.HERMITE, segment.getMetadata().getInterpolationMethod());
        Assertions.assertEquals(2, segment.getMetadata().getInterpolationDegree());
        Assertions.assertEquals(3, segment.getInterpolationSamples());
        Assertions.assertEquals(segment.getAvailableDerivatives(), CartesianDerivativesFilter.USE_PV);

        List<OemSegment> segments = file.getSegments();
        Assertions.assertEquals(3, segments.size());
        Assertions.assertEquals(3, segments.get(2).getData().getCoordinates().size());
        final TimeStampedPVCoordinates pv20 = segments.get(2).getData().getCoordinates().get(0);
        Assertions.assertEquals(
                            new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            pv20.getDate());
        Assertions.assertEquals(-2432166.0,   pv20.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals(  -63042.0,   pv20.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals( 1742754.0,   pv20.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals(    7337.02,  pv20.getVelocity().getX(), 1.0e-10);
        Assertions.assertEquals(   -3495.867, pv20.getVelocity().getY(), 1.0e-10);
        Assertions.assertEquals(   -1041.945, pv20.getVelocity().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv21 = segments.get(2).getData().getCoordinates().get(1);
        Assertions.assertEquals(new AbsoluteDate("1996-12-28T21:59:02.267", TimeScalesFactory.getUTC()),
                            pv21.getDate());
        Assertions.assertEquals(-2445234.0,   pv21.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals( -878141.0,   pv21.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals( 1873073.0,   pv21.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals(    1860.43,  pv21.getVelocity().getX(), 1.0e-10);
        Assertions.assertEquals(   -3421.256, pv21.getVelocity().getY(), 1.0e-10);
        Assertions.assertEquals(    -996.366, pv21.getVelocity().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv22 = segments.get(2).getData().getCoordinates().get(2);
        Assertions.assertEquals(new AbsoluteDate("1996-12-28T22:00:02.267", TimeScalesFactory.getUTC()),
                            pv22.getDate());
        Assertions.assertEquals(-2458079.0,   pv22.getPosition().getX(), 1.0e-10);
        Assertions.assertEquals( -683858.0,   pv22.getPosition().getY(), 1.0e-10);
        Assertions.assertEquals( 2007684.0,   pv22.getPosition().getZ(), 1.0e-10);
        Assertions.assertEquals(    6367.86,  pv22.getVelocity().getX(), 1.0e-10);
        Assertions.assertEquals(   -3339.563, pv22.getVelocity().getY(), 1.0e-10);
        Assertions.assertEquals(    -946.654, pv22.getVelocity().getZ(), 1.0e-10);

        Assertions.assertEquals(2, segments.get(2).getCovarianceMatrices().size());
        final CartesianCovariance c20 = segments.get(2).getCovarianceMatrices().get(0);
        Assertions.assertEquals(new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            c20.getEpoch());
        Assertions.assertEquals(OrbitRelativeFrame.RTN, c20.getReferenceFrame().asOrbitRelativeFrame());
        Assertions.assertEquals( 333.13494,       c20.getCovarianceMatrix().getEntry(0, 0), 1.0e-5);
        Assertions.assertEquals( 461.89273,       c20.getCovarianceMatrix().getEntry(1, 0), 1.0e-5);
        Assertions.assertEquals( 678.24216,       c20.getCovarianceMatrix().getEntry(1, 1), 1.0e-5);
        Assertions.assertEquals(-307.00078,       c20.getCovarianceMatrix().getEntry(2, 0), 1.0e-5);
        Assertions.assertEquals(-422.12341,       c20.getCovarianceMatrix().getEntry(2, 1), 1.0e-5);
        Assertions.assertEquals( 323.19319,       c20.getCovarianceMatrix().getEntry(2, 2), 1.0e-5);
        Assertions.assertEquals(  -0.33493650,    c20.getCovarianceMatrix().getEntry(3, 0), 1.0e-8);
        Assertions.assertEquals(  -0.46860842,    c20.getCovarianceMatrix().getEntry(3, 1), 1.0e-8);
        Assertions.assertEquals(   0.24849495,    c20.getCovarianceMatrix().getEntry(3, 2), 1.0e-8);
        Assertions.assertEquals(   0.00042960228, c20.getCovarianceMatrix().getEntry(3, 3), 1.0e-11);
        Assertions.assertEquals(  -0.22118325,    c20.getCovarianceMatrix().getEntry(4, 0), 1.0e-8);
        Assertions.assertEquals(  -0.28641868,    c20.getCovarianceMatrix().getEntry(4, 1), 1.0e-8);
        Assertions.assertEquals(   0.17980986,    c20.getCovarianceMatrix().getEntry(4, 2), 1.0e-8);
        Assertions.assertEquals(   0.00026088992, c20.getCovarianceMatrix().getEntry(4, 3), 1.0e-11);
        Assertions.assertEquals(   0.00017675147, c20.getCovarianceMatrix().getEntry(4, 4), 1.0e-11);
        Assertions.assertEquals(  -0.30413460,    c20.getCovarianceMatrix().getEntry(5, 0), 1.0e-8);
        Assertions.assertEquals(  -0.49894969,    c20.getCovarianceMatrix().getEntry(5, 1), 1.0e-8);
        Assertions.assertEquals(   0.35403109,    c20.getCovarianceMatrix().getEntry(5, 2), 1.0e-8);
        Assertions.assertEquals(   0.00018692631, c20.getCovarianceMatrix().getEntry(5, 3), 1.0e-11);
        Assertions.assertEquals(   0.00010088625, c20.getCovarianceMatrix().getEntry(5, 4), 1.0e-11);
        Assertions.assertEquals(   0.00062244443, c20.getCovarianceMatrix().getEntry(5, 5), 1.0e-11);
        for (int i = 0; i < c20.getCovarianceMatrix().getRowDimension(); ++i) {
            for (int j = i + 1; j < c20.getCovarianceMatrix().getColumnDimension(); ++j) {
                Assertions.assertEquals(c20.getCovarianceMatrix().getEntry(j, i),
                                    c20.getCovarianceMatrix().getEntry(i, j),
                                    1.0e-10);
            }
        }

        final CartesianCovariance c21 = segments.get(2).getCovarianceMatrices().get(1);
        Assertions.assertEquals(new AbsoluteDate("1996-12-29T21:00:00", TimeScalesFactory.getUTC()),
                            c21.getEpoch());
        Assertions.assertEquals(CelestialBodyFrame.EME2000, c21.getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals( 344.24505,       c21.getCovarianceMatrix().getEntry(0, 0), 1.0e-5);
        Assertions.assertEquals( 450.78162,       c21.getCovarianceMatrix().getEntry(1, 0), 1.0e-5);
        Assertions.assertEquals( 689.35327,       c21.getCovarianceMatrix().getEntry(1, 1), 1.0e-5);
        for (int i = 0; i < c21.getCovarianceMatrix().getRowDimension(); ++i) {
            for (int j = i + 1; j < c21.getCovarianceMatrix().getColumnDimension(); ++j) {
                Assertions.assertEquals(c21.getCovarianceMatrix().getEntry(j, i),
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
        Assertions.assertEquals("OEM 201113719185", file.getHeader().getMessageId());
        Assertions.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assertions.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assertions.assertEquals("2000-028A", file.getSegments().get(0).getMetadata().getObjectID());

        Assertions.assertEquals(1, file.getSatellites().size());
        Assertions.assertEquals(true, file.getSatellites().containsKey("2000-028A"));
        Assertions.assertEquals(false, file.getSatellites().containsKey("MARS GLOBAL SURVEYOR"));
        Assertions.assertEquals(1, file.getSatellites().size());
        Assertions.assertEquals("2000-028A", file.getSatellites().values().iterator().next().getId());
        Assertions.assertEquals(
                new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                file.getSegments().get(0).getMetadata().getStartTime());

        final OemSatelliteEphemeris satellite = file.getSatellites().get("2000-028A");
        Assertions.assertEquals("2000-028A", satellite.getId());
        final OemSegment segment = (OemSegment) satellite.getSegments().get(0);
        Assertions.assertEquals(CelestialBodyFactory.getMars().getGM(), segment.getMu(), 1.0);
        Assertions.assertEquals("J2000", segment.getMetadata().getReferenceFrame().getName());
        Assertions.assertEquals(segment.getMetadata().getCenter().getName(), "MARS BARYCENTER");
        Assertions.assertNull(segment.getMetadata().getCenter().getBody());
        // Frame not creatable since it's center can't be created.
        try {
            segment.getFrame();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e){
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY);
        }
        Assertions.assertEquals("UTC", segment.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(InterpolationMethod.HERMITE, segment.getMetadata().getInterpolationMethod());
        Assertions.assertEquals(7, segment.getMetadata().getInterpolationDegree());
        Assertions.assertEquals(segment.getAvailableDerivatives(), CartesianDerivativesFilter.USE_PVA);

        List<OemSegment> segments = file.getSegments();
        Assertions.assertEquals(1, segments.size());
        Assertions.assertEquals("Produced by M.R. Sombedody, MSOO NAV/JPL, 1996 OCT 11. It is", segments.get(0).getData().getComments().get(0));
        Assertions.assertEquals("to be used for DSN scheduling purposes only.", segments.get(0).getData().getComments().get(1));
        Assertions.assertEquals(4, segments.get(0).getData().getCoordinates().size());
        final TimeStampedPVCoordinates pv00 = segments.get(0).getData().getCoordinates().get(0);
        Assertions.assertEquals(new AbsoluteDate("1996-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                            pv00.getDate());
        Assertions.assertEquals( 2789600.0, pv00.getPosition().getX(),     1.0e-10);
        Assertions.assertEquals( -280000.0, pv00.getPosition().getY(),     1.0e-10);
        Assertions.assertEquals(-1746800.0, pv00.getPosition().getZ(),     1.0e-10);
        Assertions.assertEquals(    4730.0, pv00.getVelocity().getX(),     1.0e-10);
        Assertions.assertEquals(   -2500.0, pv00.getVelocity().getY(),     1.0e-10);
        Assertions.assertEquals(   -1040.0, pv00.getVelocity().getZ(),     1.0e-10);
        Assertions.assertEquals(       8.0, pv00.getAcceleration().getX(), 1.0e-10);
        Assertions.assertEquals(       1.0, pv00.getAcceleration().getY(), 1.0e-10);
        Assertions.assertEquals(    -159.0, pv00.getAcceleration().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv01 = segments.get(0).getData().getCoordinates().get(1);
        Assertions.assertEquals(new AbsoluteDate("1996-12-18T12:01:00.331", TimeScalesFactory.getUTC()),
                            pv01.getDate());
        Assertions.assertEquals( 2783400.0, pv01.getPosition().getX(),     1.0e-10);
        Assertions.assertEquals( -308100.0, pv01.getPosition().getY(),     1.0e-10);
        Assertions.assertEquals(-1877100.0, pv01.getPosition().getZ(),     1.0e-10);
        Assertions.assertEquals(    5190.0, pv01.getVelocity().getX(),     1.0e-10);
        Assertions.assertEquals(   -2420.0, pv01.getVelocity().getY(),     1.0e-10);
        Assertions.assertEquals(   -2000.0, pv01.getVelocity().getZ(),     1.0e-10);
        Assertions.assertEquals(       8.0, pv01.getAcceleration().getX(), 1.0e-10);
        Assertions.assertEquals(       1.0, pv01.getAcceleration().getY(), 1.0e-10);
        Assertions.assertEquals(       1.0, pv01.getAcceleration().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv02 = segments.get(0).getData().getCoordinates().get(2);
        Assertions.assertEquals(new AbsoluteDate("1996-12-18T12:02:00.331", TimeScalesFactory.getUTC()),
                            pv02.getDate());
        Assertions.assertEquals( 2776000.0, pv02.getPosition().getX(),     1.0e-10);
        Assertions.assertEquals( -336900.0, pv02.getPosition().getY(),     1.0e-10);
        Assertions.assertEquals(-2008700.0, pv02.getPosition().getZ(),     1.0e-10);
        Assertions.assertEquals(    5640.0, pv02.getVelocity().getX(),     1.0e-10);
        Assertions.assertEquals(   -2340.0, pv02.getVelocity().getY(),     1.0e-10);
        Assertions.assertEquals(   -1950.0, pv02.getVelocity().getZ(),     1.0e-10);
        Assertions.assertEquals(       8.0, pv02.getAcceleration().getX(), 1.0e-10);
        Assertions.assertEquals(       1.0, pv02.getAcceleration().getY(), 1.0e-10);
        Assertions.assertEquals(     159.0, pv02.getAcceleration().getZ(), 1.0e-10);
        final TimeStampedPVCoordinates pv03 = segments.get(0).getData().getCoordinates().get(3);
        Assertions.assertEquals(new AbsoluteDate("1996-12-28T21:28:00.331", TimeScalesFactory.getUTC()),
                            pv03.getDate());
        Assertions.assertEquals(-3881000.0, pv03.getPosition().getX(),     1.0e-10);
        Assertions.assertEquals(  564000.0, pv03.getPosition().getY(),     1.0e-10);
        Assertions.assertEquals( -682800.0, pv03.getPosition().getZ(),     1.0e-10);
        Assertions.assertEquals(   -3290.0, pv03.getVelocity().getX(),     1.0e-10);
        Assertions.assertEquals(   -3670.0, pv03.getVelocity().getY(),     1.0e-10);
        Assertions.assertEquals(    1640.0, pv03.getVelocity().getZ(),     1.0e-10);
        Assertions.assertEquals(      -3.0, pv03.getAcceleration().getX(), 1.0e-10);
        Assertions.assertEquals(       0.0, pv03.getAcceleration().getY(), 1.0e-10);
        Assertions.assertEquals(       0.0, pv03.getAcceleration().getZ(), 1.0e-10);

        Assertions.assertEquals(1, segments.get(0).getCovarianceMatrices().size());
        final CartesianCovariance c20 = segments.get(0).getCovarianceMatrices().get(0);
        Assertions.assertEquals(new AbsoluteDate("1996-12-28T22:28:00.331", TimeScalesFactory.getUTC()),
                            c20.getEpoch());
        Assertions.assertEquals(CelestialBodyFrame.ITRF1997, c20.getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals( 316000.0, c20.getCovarianceMatrix().getEntry(0, 0), 1.0e-10);
        Assertions.assertEquals( 722000.0, c20.getCovarianceMatrix().getEntry(1, 0), 1.0e-10);
        Assertions.assertEquals( 518000.0, c20.getCovarianceMatrix().getEntry(1, 1), 1.0e-10);
        Assertions.assertEquals( 202000.0, c20.getCovarianceMatrix().getEntry(2, 0), 1.0e-10);
        Assertions.assertEquals( 715000.0, c20.getCovarianceMatrix().getEntry(2, 1), 1.0e-10);
        Assertions.assertEquals(   2000.0, c20.getCovarianceMatrix().getEntry(2, 2), 1.0e-10);
        Assertions.assertEquals( 912000.0, c20.getCovarianceMatrix().getEntry(3, 0), 1.0e-10);
        Assertions.assertEquals( 306000.0, c20.getCovarianceMatrix().getEntry(3, 1), 1.0e-10);
        Assertions.assertEquals( 276000.0, c20.getCovarianceMatrix().getEntry(3, 2), 1.0e-10);
        Assertions.assertEquals( 797000.0, c20.getCovarianceMatrix().getEntry(3, 3), 1.0e-10);
        Assertions.assertEquals( 562000.0, c20.getCovarianceMatrix().getEntry(4, 0), 1.0e-10);
        Assertions.assertEquals( 899000.0, c20.getCovarianceMatrix().getEntry(4, 1), 1.0e-10);
        Assertions.assertEquals(  22000.0, c20.getCovarianceMatrix().getEntry(4, 2), 1.0e-10);
        Assertions.assertEquals(  79000.0, c20.getCovarianceMatrix().getEntry(4, 3), 1.0e-10);
        Assertions.assertEquals( 415000.0, c20.getCovarianceMatrix().getEntry(4, 4), 1.0e-10);
        Assertions.assertEquals( 245000.0, c20.getCovarianceMatrix().getEntry(5, 0), 1.0e-10);
        Assertions.assertEquals( 965000.0, c20.getCovarianceMatrix().getEntry(5, 1), 1.0e-10);
        Assertions.assertEquals( 950000.0, c20.getCovarianceMatrix().getEntry(5, 2), 1.0e-10);
        Assertions.assertEquals( 435000.0, c20.getCovarianceMatrix().getEntry(5, 3), 1.0e-10);
        Assertions.assertEquals( 621000.0, c20.getCovarianceMatrix().getEntry(5, 4), 1.0e-10);
        Assertions.assertEquals( 991000.0, c20.getCovarianceMatrix().getEntry(5, 5), 1.0e-10);
        for (int i = 0; i < c20.getCovarianceMatrix().getRowDimension(); ++i) {
            for (int j = i + 1; j < c20.getCovarianceMatrix().getColumnDimension(); ++j) {
                Assertions.assertEquals(c20.getCovarianceMatrix().getEntry(j, i),
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
        Assertions.assertEquals("UTC", file.getSegments().get(0).getMetadata().getTimeSystem().name());
        Assertions.assertEquals("MARS GLOBAL SURVEYOR", file.getSegments().get(0).getMetadata().getObjectName());
        Assertions.assertEquals("1996-062A", file.getSegments().get(0).getMetadata().getObjectID());

        Assertions.assertEquals(1, file.getSatellites().size());
        Assertions.assertEquals(true, file.getSatellites().containsKey("1996-062A"));
        Assertions.assertEquals(false, file.getSatellites().containsKey("MARS GLOBAL SURVEYOR"));
        Assertions.assertEquals(1, file.getSatellites().size());
        Assertions.assertEquals("1996-062A", file.getSatellites().values().iterator().next().getId());
        Assertions.assertEquals(
                new AbsoluteDate("2002-12-18T12:00:00.331", TimeScalesFactory.getUTC()),
                file.getSegments().get(0).getMetadata().getStartTime());

        OemSatelliteEphemeris satellite = file.getSatellites().get("1996-062A");
        Assertions.assertEquals("1996-062A", satellite.getId());
        OemSegment segment = satellite.getSegments().get(0);
        Assertions.assertEquals(CelestialBodyFactory.getMars().getGM(), segment.getMu(), 1.0);
        FactoryManagedFrame eme2000 = FramesFactory.getEME2000();
        Frame actualFrame = segment.getFrame();
        AbsoluteDate actualStart = satellite.getStart();
        Transform actualTransform = eme2000.getTransformTo(actualFrame, actualStart);
        CelestialBody mars = CelestialBodyFactory.getMars();
        TimeStampedPVCoordinates marsPV = mars.getPVCoordinates(actualStart, eme2000);
        TimeStampedPVCoordinates marsPV_in_marscentered_frame = mars.getPVCoordinates(actualStart, actualFrame);
        MatcherAssert.assertThat(marsPV_in_marscentered_frame,
                                 OrekitMatchers.pvCloseTo(PVCoordinates.ZERO, 1e-3));
        Assertions.assertEquals(actualTransform.getTranslation(), marsPV.getPosition().negate());
        Assertions.assertEquals(actualTransform.getVelocity(), marsPV.getVelocity().negate());
        Assertions.assertEquals(actualTransform.getAcceleration(), marsPV.getAcceleration().negate());
        Assertions.assertEquals(
                Rotation.distance(actualTransform.getRotation(), Rotation.IDENTITY),
                0.0, 0.0);
        Assertions.assertEquals(actualTransform.getRotationRate(), Vector3D.ZERO);
        Assertions.assertEquals(actualTransform.getRotationAcceleration(), Vector3D.ZERO);
        Assertions.assertEquals("Mars/EME2000", actualFrame.getName());
        Assertions.assertEquals(CelestialBodyFrame.EME2000, segment.getMetadata().getReferenceFrame().asCelestialBodyFrame());
        Assertions.assertEquals("UTC", segment.getMetadata().getTimeSystem().name());
        Assertions.assertEquals(segment.getAvailableDerivatives(),
                CartesianDerivativesFilter.USE_PV);
        Assertions.assertEquals(satellite.getSegments().get(0).getMetadata().getStartTime(), actualStart);
        Assertions.assertEquals(satellite.getSegments().get(2).getMetadata().getStopTime(), satellite.getStop());

        final BoundedPropagator propagator = satellite.getPropagator();
        Assertions.assertEquals(propagator.getMinDate(), satellite.getStart());
        Assertions.assertEquals(propagator.getMinDate(), satellite.getSegments().get(0).getStart());
        Assertions.assertEquals(propagator.getMaxDate(), satellite.getStop());
        Assertions.assertEquals(propagator.getMaxDate(), satellite.getSegments().get(2).getStop());

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
            Assertions.assertEquals(OrekitMessages.UNSUPPORTED_FILE_FORMAT, oe.getSpecifier());
            Assertions.assertEquals("OPMExample1.txt", oe.getParts()[0]);
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
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(44, oe.getParts()[0]);
            Assertions.assertEquals(ex, oe.getParts()[1]);
            Assertions.assertEquals("1996-12-28T21:59:02.267 -2445.234 -878.141 this-is-not-a-number 1.86043 -3.421256 -0.996366", oe.getParts()[2]);
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
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(52, oe.getParts()[0]);
            Assertions.assertEquals(ex, oe.getParts()[1]);
            Assertions.assertEquals("4.6189273e-04 this-is-not-a-number", oe.getParts()[2]);
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assertions.assertEquals(wrongName, oe.getParts()[0]);
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
            Assertions.assertEquals(OrekitMessages.CCSDS_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assertions.assertEquals("UTC", oe.getParts()[0]);
            Assertions.assertEquals("TCG", oe.getParts()[1]);
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
        Assertions.assertEquals(
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(19, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("WRONG_KEYWORD"));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(24, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("USER_DEFINED_TEST_KEY"));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assertions.assertEquals(91, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("USER_DEFINED_TEST_KEY"));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(91, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("1.0e-12"));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals("EPOCH", oe.getParts()[0]);
            Assertions.assertEquals(89, ((Integer) oe.getParts()[1]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).endsWith("OEM-too-small-covariance-dimension.txt"));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(51, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("3.3313494e-04"));
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
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(55, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[2]).startsWith("-2.2118325e-07"));
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
        Assertions.assertEquals(1, file1.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assertions.assertEquals(7, file1.getSegments().get(1).getMetadata().getInterpolationDegree());

        final DataSource source2 =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Oem file2 = builder.withDefaultInterpolationDegree(5).buildOemParser().parseMessage(source2);
        Assertions.assertEquals(5, file2.getSegments().get(0).getMetadata().getInterpolationDegree());
        Assertions.assertEquals(7, file2.getSegments().get(1).getMetadata().getInterpolationDegree());
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
        frames.add(new Pair<>("ITRF-93",  FramesFactory.getITRF(ITRFVersion.ITRF_1993, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF-97",  FramesFactory.getITRF(ITRFVersion.ITRF_1997, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2000", FramesFactory.getITRF(ITRFVersion.ITRF_2000, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2005", FramesFactory.getITRF(ITRFVersion.ITRF_2005, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2008", FramesFactory.getITRF(ITRFVersion.ITRF_2008, conventions, simpleEop)));
        frames.add(new Pair<>("ITRF2014", FramesFactory.getITRF(ITRFVersion.ITRF_2014, conventions, simpleEop)));

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
            switch (frameName) {
                case "ITRF-93" :
                    Assertions.assertEquals("ITRF1993", segment.getMetadata().getReferenceFrame().getName());
                    break;
                case "ITRF-97" :
                    Assertions.assertEquals("ITRF1997", segment.getMetadata().getReferenceFrame().getName());
                    break;
                default :
                    Assertions.assertEquals(frameName, segment.getMetadata().getReferenceFrame().getName());
                    break;
            }
            // check expected frame
            Frame actualFrame = segment.getFrame();
            Frame expectedFrame = frame.getSecond();
            Assertions.assertEquals(expectedFrame, actualFrame);
            Assertions.assertEquals(expectedFrame.getTransformProvider(),
                                actualFrame.getTransformProvider());
        }
    }

    @Test
    public void testEmptyComments() {
        final String name = "/ccsds/odm/oem/ISS.resampled.truncated.txt";
        final ParserBuilder builder = new ParserBuilder();
        final DataSource source =  new DataSource(name, () -> getClass().getResourceAsStream(name));
        final Oem iss = builder.buildOemParser().parseMessage(source);
        Assertions.assertEquals("1998-067-A", iss.getSegments().get(0).getMetadata().getObjectID());
        Assertions.assertEquals(23, iss.getSegments().get(0).getData().getComments().size());
        Assertions.assertEquals("", iss.getSegments().get(0).getData().getComments().get(13));
        Assertions.assertEquals("", iss.getSegments().get(0).getData().getComments().get(20));
        Assertions.assertEquals(25, iss.getSegments().get(0).getData().getCoordinates().size());
    }

}
