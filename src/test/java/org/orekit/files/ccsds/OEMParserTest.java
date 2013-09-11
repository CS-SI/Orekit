/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.files.ccsds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.general.OrbitFile;
import org.orekit.files.general.OrbitFile.TimeSystem;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class OEMParserTest {

    @Before
    public void setUp()
        throws Exception {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseOEM1() 
            throws OrekitException, IOException {
        // 
        final String ex = "/ccsds/OEMExample.txt";
        final InputStream inEntry = getClass().getResourceAsStream(ex);
        final OEMParser parser = new OEMParser().withMu(CelestialBodyFactory.getEarth().getGM());
        final OEMFile file = parser.parse(inEntry);
        Assert.assertEquals(TimeSystem.UTC, file.getTimeSystem());
        Assert.assertEquals("MARS GLOBAL SURVEYOR", file.getEphemeridesBlocks().get(0).getMetaData().getObjectName());
        Assert.assertEquals("1996-062A", file.getEphemeridesBlocks().get(0).getMetaData().getObjectID());
        Assert.assertEquals("MARS BARYCENTER", file.getEphemeridesBlocks().get(0).getMetaData().getCenterName());
        Assert.assertEquals(1996, file.getEphemeridesBlocks().get(0).getMetaData().getLaunchYear());
        Assert.assertEquals(62, file.getEphemeridesBlocks().get(0).getMetaData().getLaunchNumber());
        Assert.assertEquals("A", file.getEphemeridesBlocks().get(0).getMetaData().getLaunchPiece());
        Assert.assertFalse(file.getEphemeridesBlocks().get(0).getMetaData().getHasCreatableBody());
        Assert.assertNull(file.getEphemeridesBlocks().get(0).getMetaData().getCenterBody());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 00, 0.331, TimeScalesFactory.getUTC()),
                            file.getEphemeridesBlocks().get(0).getStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 28, 0.331, TimeScalesFactory.getUTC()),
                            file.getEphemeridesBlocks().get(0).getStopTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 18, 12, 10, 0.331, TimeScalesFactory.getUTC()),
                            file.getEphemeridesBlocks().get(0).getUseableStartTime());
        Assert.assertEquals(new AbsoluteDate(1996, 12, 28, 21, 23, 0.331, TimeScalesFactory.getUTC()),
                            file.getEphemeridesBlocks().get(0).getUseableStopTime());
        Assert.assertEquals("HERMITE", file.getEphemeridesBlocks().get(0).getInterpolationMethod());
        Assert.assertEquals(7, file.getEphemeridesBlocks().get(0).getInterpolationDegree());
        ArrayList<String> ephemeridesDataLinesComment = new ArrayList<String>();
        ephemeridesDataLinesComment.add("This file was produced by M.R. Somebody, MSOO NAV/JPL, 1996NOV 04. It is");
        ephemeridesDataLinesComment.add("to be used for DSN scheduling purposes only.");
        Assert.assertEquals(ephemeridesDataLinesComment, file.getEphemeridesBlocks().get(0).getEphemeridesDataLinesComment());
        CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates
                                                  (new Vector3D(2789.619 * 1000, -280.045 * 1000, -1746.755 * 1000),
                                                   new Vector3D(4.73372 * 1000, -2.49586 * 1000, -1.04195 * 1000)), 
                                                   FramesFactory.getEME2000(), 
                                                   new AbsoluteDate("1996-12-18T12:00:00.331",TimeScalesFactory.getUTC()), 
                                                   CelestialBodyFactory.getEarth().getGM());
        Assert.assertArrayEquals(orbit.getPVCoordinates().getPosition().toArray(), file.getEphemeridesBlocks().get(0).getEphemeridesDataLines().get(0).getOrbit().getPVCoordinates().getPosition().toArray(),1e-10);
        Assert.assertArrayEquals(orbit.getPVCoordinates().getVelocity().toArray(), file.getEphemeridesBlocks().get(0).getEphemeridesDataLines().get(0).getOrbit().getPVCoordinates().getVelocity().toArray(),1e-10);
        Assert.assertArrayEquals((new Vector3D(1, 1, 1)).toArray(), file.getEphemeridesBlocks().get(1).getEphemeridesDataLines().get(0).getAcceleration().toArray(), 1e-10);        
        Array2DRowRealMatrix covMatrix = new Array2DRowRealMatrix(6, 6);
        double[] column1 = {
            3.331349476038534e-04, 4.618927349220216e-04,
            -3.070007847730449e-04, -3.349365033922630e-07,
            -2.211832501084875e-07, -3.041346050686871e-07
        };
        double[] column2 = {
            4.618927349220216e-04, 6.782421679971363e-04,
            -4.221234189514228e-04, -4.686084221046758e-07,
            -2.864186892102733e-07, -4.989496988610662e-07
        };
        double[] column3 = {
            -3.070007847730449e-04, -4.221234189514228e-04,
            3.231931992380369e-04, 2.484949578400095e-07,
            1.798098699846038e-07, 3.540310904497689e-07
        };
        double[] column4 = {
            -3.349365033922630e-07, -4.686084221046758e-07,
            2.484949578400095e-07, 4.296022805587290e-10,
            2.608899201686016e-10, 1.869263192954590e-10
        };
        double[] column5 = {
            -2.211832501084875e-07, -2.864186892102733e-07,
            1.798098699846038e-07, 2.608899201686016e-10,
            1.767514756338532e-10, 1.008862586240695e-10
        };
        double[] column6 = {
            -3.041346050686871e-07, -4.989496988610662e-07,
            3.540310904497689e-07, 1.869263192954590e-10,
            1.008862586240695e-10, 6.224444338635500e-10
        };
        covMatrix.setColumn(0, column1);
        covMatrix.setColumn(1, column2);
        covMatrix.setColumn(2, column3);
        covMatrix.setColumn(3, column4);
        covMatrix.setColumn(4, column5);
        covMatrix.setColumn(5, column6);
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                Assert.assertEquals(covMatrix.getEntry(i, j),
                                    file.getEphemeridesBlocks().get(2).getCovarianceMatrices().get(0).getMatrix().getEntry(i, j),
                                    1e-10);
            }
        }
        Assert.assertEquals(new AbsoluteDate("1996-12-28T21:29:07.267", TimeScalesFactory.getUTC()),
                            file.getEphemeridesBlocks().get(2).getCovarianceMatrices().get(0).getEpoch());   
        Assert.assertEquals(FramesFactory.getEME2000(),
                            file.getEphemeridesBlocks().get(2).getCovarianceMatrices().get(1).getFrame());   
    }

    @Test
    public void testParseOEM2() 
            throws OrekitException, URISyntaxException {

        final String name = getClass().getResource("/ccsds/OEMExample2.txt").toURI().getPath();
        OEMParser parser = new OEMParser().withConventions(IERSConventions.IERS_2010).withMu(CelestialBodyFactory.getMars().getGM());

        final OEMFile file = parser.parse(name);
        ArrayList<String> headerComment = new ArrayList<String>();
        headerComment.add("comment");
        Assert.assertEquals(headerComment, file.getHeaderComment());
        ArrayList<String> metadataComment = new ArrayList<String>();
        metadataComment.add("comment 1");
        metadataComment.add("comment 2");
        Assert.assertEquals(metadataComment, file.getEphemeridesBlocks().get(0).getMetaData().getComment());        
    }

    @Test
    public void testWrongODMType() {
        try {
            new OEMParser().parse(getClass().getResourceAsStream("/ccsds/OPMExample.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_UNEXPECTED_KEYWORD, oe.getSpecifier());
            Assert.assertEquals(1, oe.getParts()[0]);
            Assert.assertEquals("CCSDS_OPM_VERS = 2.0", oe.getParts()[1]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        final String realName = getClass().getResource("/ccsds/OEMExample.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new OEMParser().parse(wrongName);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentTimeSystems() {
        try {
            new OEMParser().withMu(CelestialBodyFactory.getMars().getGM()).parse(getClass().getResourceAsStream("/ccsds/OEM-inconsistent-time-systems.txt"));
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_OEM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals(OrbitFile.TimeSystem.UTC, oe.getParts()[0]);
            Assert.assertEquals(OrbitFile.TimeSystem.TCG, oe.getParts()[1]);
        }
    }

}
