/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.forces.maneuvers;


import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class ConstantThrustManeuverTest {

    // Body mu
    private double mu;

    private CircularOrbit dummyOrbit(AbsoluteDate date) {
        return new CircularOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                                 FramesFactory.getEME2000(), date, mu);
    }

    @Test
    public void testPositiveDuration() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                             new TimeComponents(23, 30, 00.000),
                                             TimeScalesFactory.getUTC());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, 10.0, 400.0, 300.0, Vector3D.PLUS_K);
        EventDetector[] switches = maneuver.getEventsDetectors();

        Orbit o1 = dummyOrbit(date.shiftedBy(- 1.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = dummyOrbit(date.shiftedBy(  1.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = dummyOrbit(date.shiftedBy(  9.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = dummyOrbit(date.shiftedBy( 11.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    @Test
    public void testNegativeDuration() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                             new TimeComponents(23, 30, 00.000),
                                             TimeScalesFactory.getUTC());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, -10.0, 400.0, 300.0, Vector3D.PLUS_K);
        EventDetector[] switches = maneuver.getEventsDetectors();

        Orbit o1 = dummyOrbit(date.shiftedBy(-11.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = dummyOrbit(date.shiftedBy( -9.0));
        Assert.assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = dummyOrbit(date.shiftedBy( -1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = dummyOrbit(date.shiftedBy(  1.0));
        Assert.assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    @Test
    public void testRoughBehaviour() throws OrekitException {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuver.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuver.getISP(),    1.0e-10);

        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        final NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
        propagator.setAttitudeProvider(law);
        propagator.addForceModel(maneuver);
        final SpacecraftState finalorb = propagator.propagate(fireDate.shiftedBy(3800));

        final double massTolerance =
                FastMath.abs(maneuver.getFlowRate()) * maneuver.getEventsDetectors()[0].getThreshold();
        Assert.assertEquals(2007.8824544261233, finalorb.getMass(), massTolerance);
        Assert.assertEquals(2.6872, FastMath.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), FastMath.PI)), 1e-4);
        Assert.assertEquals(28970, finalorb.getA()/1000, 1);

    }

    @Test
    public void testForwardAndBackward() throws OrekitException {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = FastMath.toRadians(7);
        final double omega = FastMath.toRadians(180);
        final double OMEGA = FastMath.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = FastMath.toRadians(-7.4978);
        final double alpha = FastMath.toRadians(351);
        final AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                       new TimeComponents(23, 30, 00.000),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                               FramesFactory.getEME2000(), initDate, mu);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new DateComponents(2004, 01, 02),
                                                       new TimeComponents(04, 15, 34.080),
                                                       TimeScalesFactory.getUTC());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);
        Assert.assertEquals(f,   maneuver.getThrust(), 1.0e-10);
        Assert.assertEquals(isp, maneuver.getISP(),    1.0e-10);

        double[][] tol = NumericalPropagator.tolerances(1.0, orbit, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator1 =
            new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator1.setInitialStepSize(60);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator1);
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(law);
        propagator1.addForceModel(maneuver);
        final SpacecraftState finalState = propagator1.propagate(fireDate.shiftedBy(3800));
        AdaptiveStepsizeIntegrator integrator2 =
                        new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator2.setInitialStepSize(60);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator2);
        propagator2.setInitialState(finalState);
        propagator2.setAttitudeProvider(law);
        propagator2.addForceModel(maneuver);
        final SpacecraftState recoveredState = propagator2.propagate(orbit.getDate());
        final Vector3D refPosition = initialState.getPVCoordinates().getPosition();
        final Vector3D recoveredPosition = recoveredState.getPVCoordinates().getPosition();
        Assert.assertEquals(0.0, Vector3D.distance(refPosition, recoveredPosition), 30.0);
        Assert.assertEquals(initialState.getMass(), recoveredState.getMass(), 1.5e-10);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");

        // Body mu
        mu = 3.9860047e14;

    }

}
