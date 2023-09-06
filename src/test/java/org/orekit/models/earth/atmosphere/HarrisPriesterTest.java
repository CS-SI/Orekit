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
package org.orekit.models.earth.atmosphere;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinatesProvider;

public class HarrisPriesterTest {

    // Sun
    private PVCoordinatesProvider sun;

    // Earth
    private OneAxisEllipsoid earth;

    // Earth rotating frame
    private Frame earthFrame;

    // Time Scale
    private TimeScale utc;

    // Date
    private AbsoluteDate date;

    @Test
    public void testStandard() {

        final HarrisPriester hp = new HarrisPriester(sun, earth);

        // Position at 500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 500000.);
        final Vector3D pos = earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho = hp.getDensity(date, pos, earthFrame);

        Assertions.assertEquals(3.9237E-13, rho, 1.0e-17);

    }

    @Test
    public void testParameterN() {

        final HarrisPriester hp = new HarrisPriester(sun, earth);

        // Position at 500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 500000.);
        final Vector3D pos = earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho4 = hp.getDensity(date, pos, earthFrame);


        final HarrisPriester hp2 = new HarrisPriester(sun, earth, 2);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho2 = hp2.getDensity(date, pos, earthFrame);

        final HarrisPriester hp6 = new HarrisPriester(sun, earth, 6);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho6 = hp6.getDensity(date, pos, earthFrame);

        final double c2Psi2 = 0.02163787;

        Assertions.assertEquals(c2Psi2, (rho6-rho2)/(rho4-rho2) - 1., 1.e-8);

    }

    @Test
    public void testMaxAlt() {

        final HarrisPriester hp = new HarrisPriester(sun, earth);

        // Position at 1500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 1500000.);
        final Vector3D pos = earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho = hp.getDensity(date, pos, earthFrame);

        Assertions.assertEquals(0.0, rho, 0.0);
    }

    @Test
    public void testUserTab() {

        final double[][] userTab = {
            {100000.,   4.974e+02,  4.974e+02},
            {110000.,   7.800e+01,  7.800e+01},
            {120000.,   2.490e+01,  2.400e+01},
            {130000.,   8.377e+00,  8.710e+00},
            {140000.,   3.899e+00,  4.059e+00},
            {150000.,   2.122e+00,  2.215e+00},
            {160000.,   1.263e+00,  1.344e+00},
            {170000.,   8.008e-01,  8.758e-01},
            {180000.,   5.283e-01,  6.010e-01},
            {190000.,   3.618e-01,  4.297e-01},
            {200000.,   2.557e-01,  3.162e-01},
            {210000.,   1.839e-01,  2.396e-01},
            {220000.,   1.341e-01,  1.853e-01},
            {230000.,   9.949e-02,  1.455e-01},
            {240000.,   7.488e-02,  1.157e-01},
            {250000.,   5.709e-02,  9.308e-02},
            {260000.,   4.403e-02,  7.555e-02},
            {270000.,   3.430e-02,  6.182e-02},
            {280000.,   2.697e-02,  5.095e-02},
            {290000.,   2.139e-02,  4.226e-02},
            {300000.,   1.708e-02,  3.526e-02},
            {320000.,   1.099e-02,  2.511e-02},
            {340000.,   7.214e-03,  1.819e-02},
            {360000.,   4.824e-03,  1.337e-02},
            {380000.,   3.274e-03,  9.955e-03},
            {400000.,   2.249e-03,  7.492e-03},
            {420000.,   1.558e-03,  5.684e-03},
            {440000.,   1.091e-03,  4.355e-03},
            {460000.,   7.701e-04,  3.362e-03},
            {480000.,   5.474e-04,  2.612e-03},
            {500000.,   3.916e-04,  2.042e-03},
            {520000.,   2.819e-04,  1.605e-03},
            {540000.,   2.042e-04,  1.267e-03},
            {560000.,   1.488e-04,  1.005e-03},
            {580000.,   1.092e-04,  7.997e-04},
            {600000.,   8.070e-05,  6.390e-04},
            {620000.,   6.012e-05,  5.123e-04},
            {640000.,   4.519e-05,  4.121e-04},
            {660000.,   3.430e-05,  3.325e-04},
            {680000.,   2.632e-05,  2.691e-04},
            {700000.,   2.043e-05,  2.185e-04},
            {720000.,   1.607e-05,  1.779e-04},
            {740000.,   1.281e-05,  1.452e-04},
            {760000.,   1.036e-05,  1.190e-04},
            {780000.,   8.496e-06,  9.776e-05},
            {800000.,   7.069e-06,  8.059e-05},
            {850000.,   4.800e-06,  5.500e-05},
            {900000.,   3.300e-06,  3.700e-05},
            {950000.,   2.450e-06,  2.400e-05},
            {1000000.,  1.900e-06,  1.700e-05},
            {1100000.,  1.180e-06,  8.700e-06},
            {1200000.,  7.500e-07,  4.800e-06},
            {1300000.,  5.300e-07,  3.200e-06},
            {1400000.,  4.100e-07,  2.000e-06},
            {1500000.,  2.900e-07,  1.350e-06},
            {1600000.,  2.000e-07,  9.500e-07},
            {1700000.,  1.600e-07,  7.700e-07},
            {1800000.,  1.200e-07,  6.300e-07},
            {1900000.,  9.600e-08,  5.200e-07},
            {2000000.,  7.300e-08,  4.400e-07}
        };

        // Position at 1500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 1500000.);
        final Vector3D pos = earth.transform(point);

        final HarrisPriester hp = new HarrisPriester(sun, earth, userTab);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho = hp.getDensity(date, pos, earthFrame);

        Assertions.assertEquals(2.9049E-7, rho, 1.0e-11);

        final HarrisPriester hp6 = new HarrisPriester(sun, earth, userTab, 6);
        final double rho6 = hp6.getDensity(date, pos, earthFrame);

        final HarrisPriester hp2 = new HarrisPriester(sun, earth, userTab, 2);
        final double rho2 = hp2.getDensity(date, pos, earthFrame);

        final double c2Psi2 = 0.02163787;

        Assertions.assertEquals(c2Psi2, (rho6-rho2)/(rho-rho2) - 1., 1.0e-8);

    }

    @Test
    public void testOutOfRange() {
        Assertions.assertThrows(OrekitException.class, () -> {
            final HarrisPriester hp = new HarrisPriester(sun, earth);

            // Position at 50 km height
            final GeodeticPoint point = new GeodeticPoint(0, 0, 50000.);
            final Vector3D pos = earth.transform(point);

            // COMPUTE DENSITY KG/M3 RHO
            hp.getDensity(date, pos, earthFrame);
        });
    }

    @Test
    public void testVelocityDerivative() {
        final Frame eme2000 = FramesFactory.getEME2000();
        final HarrisPriester hp = new HarrisPriester(sun, earth);
        final Vector3D pos = earth.getBodyFrame().getStaticTransformTo(eme2000, date).
                             transformPosition(earth.transform(new GeodeticPoint(-1.7, 4.2, 987654.321)));
        double dP = 100.0;
        double dVxdX = gradientComponent(hp, pos, Vector3D.PLUS_I, dP, eme2000, v -> v.getX());
        double dVxdY = gradientComponent(hp, pos, Vector3D.PLUS_J, dP, eme2000, v -> v.getX());
        double dVxdZ = gradientComponent(hp, pos, Vector3D.PLUS_K, dP, eme2000, v -> v.getX());
        double dVydX = gradientComponent(hp, pos, Vector3D.PLUS_I, dP, eme2000, v -> v.getY());
        double dVydY = gradientComponent(hp, pos, Vector3D.PLUS_J, dP, eme2000, v -> v.getY());
        double dVydZ = gradientComponent(hp, pos, Vector3D.PLUS_K, dP, eme2000, v -> v.getY());
        double dVzdX = gradientComponent(hp, pos, Vector3D.PLUS_I, dP, eme2000, v -> v.getZ());
        double dVzdY = gradientComponent(hp, pos, Vector3D.PLUS_J, dP, eme2000, v -> v.getZ());
        double dVzdZ = gradientComponent(hp, pos, Vector3D.PLUS_K, dP, eme2000, v -> v.getZ());

        DSFactory factory = new DSFactory(3, 1);
        FieldVector3D<DerivativeStructure> dsPos = new FieldVector3D<>(factory.variable(0, pos.getX()),
                                                                       factory.variable(1, pos.getY()),
                                                                       factory.variable(2, pos.getZ()));
        FieldVector3D<DerivativeStructure> dsVel = hp.getVelocity(new FieldAbsoluteDate<>(factory.getDerivativeField(),
                                                                                          date),
                                                                  dsPos, eme2000);
        Assertions.assertEquals(dVxdX, dsVel.getX().getPartialDerivative(1, 0, 0), 1.0e-16);
        Assertions.assertEquals(dVxdY, dsVel.getX().getPartialDerivative(0, 1, 0), 1.0e-16);
        Assertions.assertEquals(dVxdZ, dsVel.getX().getPartialDerivative(0, 0, 1), 1.0e-16);
        Assertions.assertEquals(dVydX, dsVel.getY().getPartialDerivative(1, 0, 0), 1.0e-16);
        Assertions.assertEquals(dVydY, dsVel.getY().getPartialDerivative(0, 1, 0), 1.0e-16);
        Assertions.assertEquals(dVydZ, dsVel.getY().getPartialDerivative(0, 0, 1), 1.0e-16);
        Assertions.assertEquals(dVzdX, dsVel.getZ().getPartialDerivative(1, 0, 0), 1.0e-16);
        Assertions.assertEquals(dVzdY, dsVel.getZ().getPartialDerivative(0, 1, 0), 1.0e-16);
        Assertions.assertEquals(dVzdZ, dsVel.getZ().getPartialDerivative(0, 0, 1), 1.0e-16);

    }

    private double gradientComponent(final Atmosphere atm, final Vector3D position, final Vector3D direction,
                                     final double dP, final Frame frame, final ComponentGetter getter) {
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(5, dP);
        UnivariateFunction f = delta -> {
            try {
                return getter.get(atm.getVelocity(date, new Vector3D(1,  position, delta, direction), frame));
            } catch (OrekitException oe) {
                return Double.NaN;
            }
        };
        return differentiator.differentiate(f).value(new DSFactory(1, 1).variable(0, 0.0)).getPartialDerivative(1);
    }

    private interface ComponentGetter {
        double get(Vector3D v);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        sun = CelestialBodyFactory.getSun();

        earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, earthFrame);

        // Equinoxe 21 mars 2003 à 1h00m
        utc  = TimeScalesFactory.getUTC();
        date = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), utc);
    }

    @AfterEach
    public void tearDown() {
        utc = null;
    }

}
