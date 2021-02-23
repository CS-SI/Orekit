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
package org.orekit.files.ccsds;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.CcsdsFrame;
import org.orekit.files.ccsds.definitions.CcsdsModifiedFrame;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.ITRFVersion;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class CCSDSFrameTest {

    @Test
    public void testLOFFramesNotRegularFrames() {
        for (final CcsdsFrame frame : CcsdsFrame.values()) {
            if (frame.isLof()) {
                try {
                    frame.getFrame(IERSConventions.IERS_2010, true);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException oe) {
                    Assert.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
                }
            } else {
                Assert.assertNull(frame.getLofType());
                Assert.assertNotNull(frame.getFrame(IERSConventions.IERS_2010, true));
            }
        }
    }

    /**
     * Check mapping frames to CCSDS frames.
     */
    @Test
    public void testMap() {
        // action + verify
        // check all non-LOF frames created by OEMParser
        for (CcsdsFrame ccsdsFrame : CcsdsFrame.values()) {
            if (!ccsdsFrame.isLof()) {
                Frame frame = ccsdsFrame.getFrame(IERSConventions.IERS_2010, true);
                CcsdsFrame actual = CcsdsFrame.map(frame);
                if (ccsdsFrame == CcsdsFrame.J2000) {
                    // CCSDS allows both J2000 and EME2000 names
                    // Orekit chose to use EME2000 when guessing name from frame instance
                    MatcherAssert.assertThat(actual, CoreMatchers.is(CcsdsFrame.EME2000));
                } else  if (ccsdsFrame == CcsdsFrame.TDR) {
                    // CCSDS allows both GTOD (in ADM section A3) and
                    // TDR (in ODM table 5-3 and section A2) names
                    // Orekit chose to use GTOD when guessing name from frame instance
                    MatcherAssert.assertThat(actual, CoreMatchers.is(CcsdsFrame.GTOD));
                } else {
                    MatcherAssert.assertThat(actual, CoreMatchers.is(ccsdsFrame));
                }
            }
        }

        // check common Orekit frames from FramesFactory
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getGCRF()),
                                 CoreMatchers.is(CcsdsFrame.GCRF));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getEME2000()),
                                 CoreMatchers.is(CcsdsFrame.EME2000));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getITRFEquinox(IERSConventions.IERS_2010, true)),
                                 CoreMatchers.is(CcsdsFrame.GRC));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getICRF()),
                                 CoreMatchers.is(CcsdsFrame.ICRF));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                 CoreMatchers.is(CcsdsFrame.ITRF2014));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getGTOD(true)),
                                 CoreMatchers.is(CcsdsFrame.GTOD));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getTEME()),
                                 CoreMatchers.is(CcsdsFrame.TEME));
        MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getTOD(true)),
                                 CoreMatchers.is(CcsdsFrame.TOD));

        // check that guessed name loses the IERS conventions and simpleEOP flag
        for (ITRFVersion version : ITRFVersion.values()) {
            final String name = version.getName().replaceAll("-", "");
            for (final IERSConventions conventions : IERSConventions.values()) {
                MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getITRF(version, conventions, true)).name(),
                                         CoreMatchers.is(name));
                MatcherAssert.assertThat(CcsdsFrame.map(FramesFactory.getITRF(version, conventions, false)).name(),
                                         CoreMatchers.is(name));
            }
        }

        // check other names in Annex A
        MatcherAssert.assertThat(
                CcsdsFrame.map(CelestialBodyFactory.getMars().getInertiallyOrientedFrame()),
                CoreMatchers.is(CcsdsFrame.MCI));
        MatcherAssert.assertThat(CcsdsFrame.map(CelestialBodyFactory.getSolarSystemBarycenter().
                                 getInertiallyOrientedFrame()),
                                 CoreMatchers.is(CcsdsFrame.ICRF));
        // check some special CCSDS frames
        CcsdsModifiedFrame frame = new CcsdsModifiedFrame(FramesFactory.getEME2000(),
                                                          CcsdsFrame.EME2000,
                                                          CelestialBodyFactory.getMars(), "MARS");
        MatcherAssert.assertThat(CcsdsFrame.map(frame), CoreMatchers.is(CcsdsFrame.EME2000));
        Vector3D v = frame.getTransformProvider().getTransform(AbsoluteDate.J2000_EPOCH).getTranslation();
        FieldVector3D<Decimal64> v64 = frame.getTransformProvider().getTransform(FieldAbsoluteDate.getJ2000Epoch(Decimal64Field.getInstance())).getTranslation();
        Assert.assertEquals(0.0, FieldVector3D.distance(v64, v).getReal(), 1.0e-10);

        // check unknown frame
        try {
            Frame topo = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                   Constants.WGS84_EARTH_FLATTENING,
                                                                   FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                              new GeodeticPoint(1.2, 2.3, 45.6),
                            "dummy");
            CcsdsFrame.map(topo);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_INVALID_FRAME, oe.getSpecifier());
            Assert.assertEquals("dummy", oe.getParts()[0]);
        }

        // check a fake ICRF
        Frame fakeICRF = new Frame(FramesFactory.getGCRF(), Transform.IDENTITY,
                                   CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER + "/inertial");
        MatcherAssert.assertThat(CcsdsFrame.map(fakeICRF), CoreMatchers.is(CcsdsFrame.ICRF));
    }

    /**
     * Check guessing names.
     */
    @Test
    public void testGuessFrame() {

        Frame itrf89 = FramesFactory.getITRF(ITRFVersion.ITRF_89, IERSConventions.IERS_1996, true);
        Assert.assertEquals("ITRF89", CcsdsFrame.guessFrame(itrf89));

        Frame topo = new TopocentricFrame(new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                               Constants.WGS84_EARTH_FLATTENING,
                                                               FramesFactory.getITRF(IERSConventions.IERS_2010, true)),
                                          new GeodeticPoint(1.2, 2.3, 45.6),
                        "dummy");
        Assert.assertEquals("dummy", CcsdsFrame.guessFrame(topo));
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
