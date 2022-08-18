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
package org.orekit.frames;

import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class FrameToolsTest {

    /**
     * Configure access to Orekit data folder for simple unit tests.
     */
    @BeforeClass
    public static void configureOrekitDataAccess() {
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

        final Frame pivotFrame = FramesFactory.getGCRF();
        final Orbit orbit = getOrbit();

        final FrameFacade RTN = new FrameFacade(null, null, OrbitRelativeFrame.QSW, null, "RTN");
        final FrameFacade TNW = new FrameFacade(null, null, OrbitRelativeFrame.TNW, null, "RTN");

        // When
        final Transform lofInToLofOut = FrameTools.getTransform(RTN, TNW, pivotFrame, orbit.getDate(), orbit);

        // Then
        final double[][] expectedRotationMatrix = new double[][]{
                {0, 1, 0},
                {-1, 0, 0},
                {0, 0, 1}};

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(expectedRotationMatrix[i][j], lofInToLofOut.getRotation().getMatrix()[i][j], threshold);
            }
        }

    }

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
                    Transform t = FrameTools.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
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
                    Transform t = FrameTools.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
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
                    Transform t = FrameTools.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
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
                    Transform t = FrameTools.getTransform(from, to, pivotFrame, orbit.getDate(), orbit);
                    Assert.assertNotNull(t);
                } catch (IllegalArgumentException iae) {
                    Assert.assertTrue(iae.getMessage().contains("is not implemented"));
                } catch (Exception e) {
                    Assert.fail(e.getCause().getMessage());
                }
            }
        }
    }

    @Test
    public void testConvertCovFrame() {

        final Orbit pv = getOrbit();

        final Frame pivotFrame = FramesFactory.getGCRF();

        double sig_sma = 1000;
        double sig_ecc = 1000 / pv.getA();
        double sig_inc = FastMath.toRadians(0.01);
        double sig_pom = FastMath.toRadians(0.01);
        double sig_gom = FastMath.toRadians(0.01);
        double sig_anm = FastMath.toRadians(0.1);

        sig_sma *= sig_sma;
        sig_ecc *= sig_ecc;
        sig_inc *= sig_inc;
        sig_pom *= sig_pom;
        sig_gom *= sig_gom;
        sig_anm *= sig_anm;

        final RealMatrix covMatrix = new DiagonalMatrix(new double[]{sig_sma, sig_ecc, sig_inc,
                sig_pom, sig_gom, sig_anm});

        CelestialBodyFrame in = CelestialBodyFrame.EME2000;
        FrameFacade from = new FrameFacade(in.getFrame(IERSConventions.IERS_2010, false, DataContext.getDefault()),
                in, null, null, in.name());

        OrbitRelativeFrame out = OrbitRelativeFrame.TNW;
        FrameFacade to = new FrameFacade(null, null, out, null, out.name());

        final Transform t = FrameTools.getTransform(from, to, pivotFrame, pv.getDate(), pv);

        final RealMatrix converted = FrameTools.convertCovFrame(covMatrix, t);

        // Reference data from CelestLab :
        // https://sourceforge.isae.fr/svn/dcas-soft-espace/support/softs/CelestLab/trunk/help/en_US/scilab_en_US_help/jacobian%20matrices.html cas-3
        // Attention : CCSDS covariance matrix are supposed to be expressed in cartesian.
        //             However,the Celestlab example uses keplerian data.
        //             So here we consider that the covariance is cartesian.
        //             The celestlab variable dpv_dkep must be overloaded so that it can be an identity matrix.

        double[][] expected = {{2.543E-08, 0., 5.027E-09, 0., 0., 0.},
                {0., 1000000., 0., 0., 0., 0.},
                {5.027E-09, 0., 2.543E-08, 0., 0., 0.},
                {0., 0., 0., 0.0000015, 0., 0.0000015},
                {0., 0., 0., 0., 3.046E-08, 0.},
                {0., 0., 0., 0.0000015, 0., 0.0000015}};

        // Both matrices are identical if the absolute error on each term doesn't exceed 1E-7 USI
        validateMatrix(expected, converted.getData(), 4.e-8);
    }

    private void validateMatrix(double[][] data, double[][] expected, double threshold) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                Assert.assertEquals(expected[i][j], data[i][j], threshold);
            }
        }
    }
}