/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class SpacecraftStateTest {

    @Test
    public void testShiftError()
        throws ParseException, OrekitException {


        // polynomial models for interpolation error in position, velocity and attitude
        // these models grow as follows
        //   interpolation time (s)    position error (m)   velocity error (m/s)  attitude error (°)
        //           60                       20                    1                  0.00007
        //          120                      100                    2                  0.00025
        //          300                      600                    4                  0.00125
        //          600                     2000                    6                  0.0028
        //          900                     4000                    6                  0.0075
        // the expected maximal residuals with respect to these models are about 4m, 4cm/s and 7.0e-5°
        PolynomialFunction pModel = new PolynomialFunction(new double[] {
            1.0861222899572454, -0.09715781161843842, 0.007738813180913936, -3.273915351103342E-6
        });
        PolynomialFunction vModel = new PolynomialFunction(new double[] {
            -0.02580749589147073, 0.015669287539738435, -1.0221727893509467E-5, 4.903886053117456E-10
        });
        PolynomialFunction aModel = new PolynomialFunction(new double[] {
            2.367656161750781E-5, -9.04040437097894E-7, 2.7648633804186084E-8, -3.862811467792131E-11, -3.465934294894873E-15, 2.7789684889607137E-17
        });

        AbsoluteDate centerDate = orbit.getDate().shiftedBy(100.0);
        SpacecraftState centerState = propagator.propagate(centerDate);
        double maxResidualP = 0;
        double maxResidualV = 0;
        double maxResidualA = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            SpacecraftState shifted = centerState.shiftedBy(dt);
            SpacecraftState propagated = propagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), shifted.getPVCoordinates());
            double residualP = pModel.value(dt) - dpv.getPosition().getNorm();
            double residualV = vModel.value(dt) - dpv.getVelocity().getNorm();
            double residualA = aModel.value(dt) -
                               FastMath.toDegrees(Rotation.distance(shifted.getAttitude().getRotation(),
                                                                propagated.getAttitude().getRotation()));
            maxResidualP = FastMath.max(maxResidualP, FastMath.abs(residualP));
            maxResidualV = FastMath.max(maxResidualV, FastMath.abs(residualV));
            maxResidualA = FastMath.max(maxResidualA, FastMath.abs(residualA));
        }
        Assert.assertEquals(4.0,    maxResidualP, 0.2);
        Assert.assertEquals(0.04,   maxResidualV, 0.01);
        Assert.assertEquals(7.0e-5, maxResidualA, 0.3e-5);
    }

    @Test
    public void testInterpolation()
        throws ParseException, OrekitException {
        checkInterpolationError( 2, 5162.2580, 1.47722511, 169847849.38e-9, 0.0);
        checkInterpolationError( 3,  650.5940, 0.62788726,    189888.18e-9, 0.0);
        checkInterpolationError( 4,  259.3868, 0.11878960,       232.33e-9, 0.0);
        checkInterpolationError( 5,   29.5445, 0.02278694,         0.48e-9, 0.0);
        checkInterpolationError( 6,    6.7633, 0.00336356,         0.09e-9, 0.0);
        checkInterpolationError( 9,    0.0082, 0.00000577,         1.49e-9, 0.0);
        checkInterpolationError(10,    0.0011, 0.00000058,         5.61e-9, 0.0);
    }

    private void checkInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                         double expectedErrorA, double expectedErrorM)
        throws PropagationException {
        AbsoluteDate centerDate = orbit.getDate().shiftedBy(100.0);
        SpacecraftState centerState = propagator.propagate(centerDate);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (int i = 0; i < n; ++i) {
            sample.add(propagator.propagate(centerDate.shiftedBy(i * 900.0 / (n - 1))));
        }
        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            SpacecraftState interpolated = centerState.interpolate(centerDate.shiftedBy(dt), sample);
            SpacecraftState propagated = propagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm());
            maxErrorA = FastMath.max(maxErrorA, FastMath.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation(),
                                                                                                  propagated.getAttitude().getRotation())));
            maxErrorM = FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass() - propagated.getMass()));
        }
        Assert.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assert.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assert.assertEquals(expectedErrorA, maxErrorA, 4.0e-10);
        Assert.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDatesConsistency() throws OrekitException {
        new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit.shiftedBy(10.0),
                                                           orbit.getDate().shiftedBy(10.0), orbit.getFrame()));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFramesConsistency() throws OrekitException {
        new SpacecraftState(orbit,
                            new Attitude(orbit.getDate(),
                                         FramesFactory.getGCRF(),
                                         Rotation.IDENTITY, Vector3D.ZERO));
    }

    @Test
    public void testTransform()
        throws ParseException, OrekitException {

        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransform().getInverse();
            PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates());
            Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates().getPosition());
            maxDP = FastMath.max(maxDP, dPV.getPosition().getNorm());
            maxDV = FastMath.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = FastMath.max(maxDA, FastMath.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);

    }

    @Before
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data");
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);
        attitudeLaw = new BodyCenterPointing(FramesFactory.getITRF2005());
        propagator =
            new EcksteinHechlerPropagator(orbit, attitudeLaw, mass,
                                          ae, mu, c20, c30, c40, c50, c60);

        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        mass  = Double.NaN;
        orbit = null;
        attitudeLaw = null;
        propagator = null;
    }

    private double mass;
    private Orbit orbit;
    private AttitudeProvider attitudeLaw;
    private Propagator propagator;

}
