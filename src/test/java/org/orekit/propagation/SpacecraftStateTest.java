/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import junit.framework.Assert;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.optimization.OptimizationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class SpacecraftStateTest {

    @Test
    public void testInterpolationError()
        throws ParseException, OrekitException, OptimizationException {


        // polynomial models for interpolation error in position, velocity and attitude
        // these models grow as follows
        //   interpolation time (s)    position error (m)   velocity error (m/s)  attitude error (°)
        //           60                       20                    1                  0.001
        //          120                      100                    2                  0.002
        //          300                      600                    4                  0.005
        //          600                     2000                    6                  0.008
        //          900                     4000                    6                  0.010
        // the expected maximal residuals with respect to these models are about 1.6m, 1.4cm/s and 1.6e-4°
        PolynomialFunction pModel = new PolynomialFunction(new double[] {
            1.0956959601968066, -0.09688149720219452, 0.007737405158470629, -3.2724867778992773e-6
        });
        PolynomialFunction vModel = new PolynomialFunction(new double[] {
            -0.027225206270228786, 0.015684157425606932, -1.0261552212623246e-5, 5.20598059288169e-10
        });
        PolynomialFunction aModel = new PolynomialFunction(new double[] {
            -1.7504437924285588e-4, 2.1388130994378013e-5, -1.3397843704372905e-8, 2.96320042273176e-12
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
                               Math.toDegrees(Rotation.distance(shifted.getAttitude().getRotation(),
                                                                propagated.getAttitude().getRotation()));
            maxResidualP = Math.max(maxResidualP, residualP);
            maxResidualV = Math.max(maxResidualV, residualV);
            maxResidualA = Math.max(maxResidualA, residualA);
        }
        Assert.assertEquals(1.6,    maxResidualP, 4.0e-2);
        Assert.assertEquals(0.014,  maxResidualV, 4.0e-4);
        Assert.assertEquals(1.6e-4, maxResidualA, 4.0e-6);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDatesConsistency() throws OrekitException {
        new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit.shiftedBy(10.0)));
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
        throws ParseException, OrekitException, OptimizationException {

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
            maxDP = Math.max(maxDP, dPV.getPosition().getNorm());
            maxDV = Math.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = Math.max(maxDA, Math.toDegrees(alpha));
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
        double OMEGA = Math.toRadians(261);
        double lv = 0;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        orbit = new KeplerianOrbit(a, e, i, omega, OMEGA,
                                            lv, KeplerianOrbit.TRUE_ANOMALY, 
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
    private AttitudeLaw attitudeLaw;
    private Propagator propagator;

}
