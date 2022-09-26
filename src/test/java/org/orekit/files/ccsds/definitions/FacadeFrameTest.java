/* Copyright 2002-2022 CS GROUP
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

import java.util.Arrays;

import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.StateCovarianceMatrixProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class FacadeFrameTest {

    /**
     * Configure access to Orekit data folder for simple unit tests.
     */
    @BeforeClass
    public static void configureOrekitDataAccess() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testMapCelestial() {
        for (CelestialBodyFrame cbf : CelestialBodyFrame.values()) {
            FrameFacade ff = FrameFacade.parse(cbf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assert.assertSame(cbf, ff.asCelestialBodyFrame());
            Assert.assertNull(ff.asOrbitRelativeFrame());
            Assert.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapLOF() {
        for (OrbitRelativeFrame orf : OrbitRelativeFrame.values()) {
            FrameFacade ff = FrameFacade.parse(orf.name(),
                                               IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                               true, true, true);
            Assert.assertNull(ff.asCelestialBodyFrame());
            Assert.assertSame(orf, ff.asOrbitRelativeFrame());
            Assert.assertNull(ff.asSpacecraftBodyFrame());
        }
    }

    @Test
    public void testMapSpacecraft() {
        for (SpacecraftBodyFrame.BaseEquipment be : SpacecraftBodyFrame.BaseEquipment.values()) {
            for (String label : Arrays.asList("1", "2", "A", "B")) {
                SpacecraftBodyFrame sbf = new SpacecraftBodyFrame(be, label);
                FrameFacade ff = FrameFacade.parse(sbf.toString(),
                                                   IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                                   true, true, true);
            Assert.assertNull(ff.asCelestialBodyFrame());
            Assert.assertNull(ff.asOrbitRelativeFrame());
            Assert.assertEquals(be,    ff.asSpacecraftBodyFrame().getBaseEquipment());
            Assert.assertEquals(label, ff.asSpacecraftBodyFrame().getLabel());
            }
        }
    }

    @Test
    public void testUnknownFrame() {
        final String name = "unknown";
        FrameFacade ff = FrameFacade.parse(name,
                                           IERSConventions.IERS_2010, true, DataContext.getDefault(),
                                           true, true, true);
        Assert.assertNull(ff.asFrame());
        Assert.assertNull(ff.asCelestialBodyFrame());
        Assert.assertNull(ff.asOrbitRelativeFrame());
        Assert.assertNull(ff.asSpacecraftBodyFrame());
        Assert.assertEquals(name, ff.getName());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Provides an orbit for the tests.
     *
     * @return a Keplerian orbit defined in EME2000.
     */
    private Orbit getOrbit() {

        Orbit kOrb = new KeplerianOrbit(7.E6, 0.001, FastMath.toRadians(45.), 0., 0., 0.,
                PositionAngle.TRUE, FramesFactory.getEME2000(),
                new AbsoluteDate(2000, 1, 1, TimeScalesFactory.getUTC()),
                Constants.GRIM5C1_EARTH_MU);

        return OrbitType.CARTESIAN.convertType(kOrb);
    }

    /**
     * Test that the getTransform method returns expected rotation matrix with theta = 90° when asked the transform
     * between RTN and TNW local orbital frame.
     */
    @Test
    public void testGetTransformLofToLofAtPeriapsis() {
        // Given
        final double threshold = 1e-15;
        final double[][] expectedRotationMatrix = new double[][]{
                {0, 1, 0},
                {-1, 0, 0},
                {0, 0, 1}};

        final Frame pivotFrame = FramesFactory.getGCRF();
        final Orbit orbit      = getOrbit();

        final FrameFacade RTN = new FrameFacade(null, null, OrbitRelativeFrame.QSW, null, "RTN");
        final FrameFacade TNW = new FrameFacade(null, null, OrbitRelativeFrame.TNW, null, "RTN");

        // When
        final Transform lofInToLofOut = FrameFacade.getTransform(RTN, TNW, pivotFrame, orbit.getDate(), orbit);

        // Then
        validateMatrix(lofInToLofOut.getRotation().getMatrix(), expectedRotationMatrix, threshold);

    }

    /**
     * Test getTransform with every combination of frames. It shouldn't throw an exception.
     */
    @Test
    public void testGetTransform() {

        final Orbit orbit = getOrbit();

        final Frame pivotFrame = FramesFactory.getGCRF();

        // From celestial to celestial
        for (CelestialBodyFrame in : CelestialBodyFrame.values()) {
            FrameFacade from = new FrameFacade(in.getFrame(IERSConventions.IERS_2010, false, DataContext.getDefault()),
                    in, null, null, in.name());
            for (CelestialBodyFrame out : CelestialBodyFrame.values()) {
                FrameFacade to = new FrameFacade(
                        out.getFrame(IERSConventions.IERS_2010, false, DataContext.getDefault()),
                        out, null, null, out.name());
                try {
                    Transform t = FrameFacade.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
                    Assert.assertNotNull(t);
                } catch (IllegalArgumentException iae) {
                    Assert.assertTrue(iae.getMessage().contains("is not implemented"));
                } catch (Exception e) {
                    Assert.fail(e.getCause().getMessage());
                }
            }
        }
        // From celestial to LOF
        for (CelestialBodyFrame in : CelestialBodyFrame.values()) {
            FrameFacade from = new FrameFacade(in.getFrame(IERSConventions.IERS_2010, false, DataContext.getDefault()),
                    in, null, null, in.name());
            for (OrbitRelativeFrame out : OrbitRelativeFrame.values()) {
                FrameFacade to = new FrameFacade(null, null, out, null, out.name());
                try {
                    Transform t = FrameFacade.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
                    Assert.assertNotNull(t);
                } catch (IllegalArgumentException iae) {
                    Assert.assertTrue(iae.getMessage().contains("is not implemented"));
                } catch (Exception e) {
                    Assert.fail(e.getCause().getMessage());
                }
            }
        }
        // From LOF to celestial
        for (OrbitRelativeFrame in : OrbitRelativeFrame.values()) {
            FrameFacade from = new FrameFacade(null, null, in, null, in.name());
            for (CelestialBodyFrame out : CelestialBodyFrame.values()) {
                FrameFacade to = new FrameFacade(
                        out.getFrame(IERSConventions.IERS_2010, false, DataContext.getDefault()),
                        out, null, null, out.name());
                try {
                    Transform t = FrameFacade.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
                    Assert.assertNotNull(t);
                } catch (IllegalArgumentException iae) {
                    Assert.assertTrue(iae.getMessage().contains("is not implemented"));
                } catch (Exception e) {
                    Assert.fail(e.getCause().getMessage());
                }
            }
        }
        // From LOF to LOF
        for (OrbitRelativeFrame in : OrbitRelativeFrame.values()) {
            FrameFacade from = new FrameFacade(null, null, in, null, in.name());
            for (OrbitRelativeFrame out : OrbitRelativeFrame.values()) {
                FrameFacade to = new FrameFacade(null, null, out, null, out.name());
                try {
                    Transform t = FrameFacade.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
                    Assert.assertNotNull(t);
                } catch (IllegalArgumentException iae) {
                    Assert.assertTrue(iae.getMessage().contains("is not implemented"));
                } catch (Exception e) {
                    Assert.fail(e.getCause().getMessage());
                }
            }
        }
    }

    /**
     * Assert that data double array is equals to expected.
     *
     * @param data      input data to assert
     * @param expected  expected data
     * @param threshold threshold for precision
     */
    private void validateMatrix(double[][] data, double[][] expected, double threshold) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                Assert.assertEquals(expected[i][j], data[i][j], threshold);
            }
        }
    }

}
