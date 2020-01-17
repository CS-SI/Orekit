/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.propagation.numerical;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.ApsideDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.AdditionalEquations;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

public class NumericalPropagatorTest {

    private double               mu;
    private AbsoluteDate         initDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;

    @Test
    public void testForceModelInitialized() {
        // setup
        // mutable holders
        SpacecraftState[] actualState = new SpacecraftState[1];
        AbsoluteDate[] actualDate = new AbsoluteDate[1];
        ForceModel force = new ForceModelAdapter(){
            @Override
            public void init(SpacecraftState initialState, AbsoluteDate target) {
                actualState[0] = initialState;
                actualDate[0] = target;
            }
        };

        // action
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(force);
        AbsoluteDate target = initDate.shiftedBy(60);
        propagator.propagate(target);

        // verify
        Assert.assertThat(actualDate[0], CoreMatchers.is(target));
        Assert.assertThat(actualState[0].getDate().durationFrom(initDate),
                CoreMatchers.is(0.0));
        Assert.assertThat(actualState[0].getPVCoordinates(),
                OrekitMatchers.pvIs(initialState.getPVCoordinates()));
    }

    @Test
    public void testEphemerisModeWithHandler() {
        // setup
        AbsoluteDate end = initDate.shiftedBy(90 * 60);

        // action
        final List<SpacecraftState> states = new ArrayList<>();
        propagator.setEphemerisMode(
                (interpolator, isLast) -> states.add(interpolator.getCurrentState()));
        propagator.propagate(end);
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        //verify
        Assert.assertTrue(states.size() > 10); // got some data
        for (SpacecraftState state : states) {
            PVCoordinates actual =
                    ephemeris.propagate(state.getDate()).getPVCoordinates();
            Assert.assertThat(actual, OrekitMatchers.pvIs(state.getPVCoordinates()));
        }
    }

    /** test for issue #238 */
    @Test
    public void testEventAtEndOfEphemeris() {
        // setup
        // choose duration that will round up when expressed as a double
        AbsoluteDate end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        propagator.setEphemerisMode();
        propagator.propagate(end);
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        CountingHandler handler = new CountingHandler();
        DateDetector detector = new DateDetector(10, 1e-9, end)
                .withHandler(handler);
        // propagation works fine w/o event detector, but breaks with it
        ephemeris.addEventDetector(detector);

        //action
        // fails when this throws an "out of range date for ephemerides"
        SpacecraftState actual = ephemeris.propagate(end);

        //verify
        Assert.assertEquals(actual.getDate().durationFrom(end), 0.0, 0.0);
        Assert.assertEquals(1, handler.eventCount);
    }

    /** test for issue #238 */
    @Test
    public void testEventAtBeginningOfEphemeris() {
        // setup
        // choose duration that will round up when expressed as a double
        AbsoluteDate end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        propagator.setEphemerisMode();
        propagator.propagate(end);
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        CountingHandler handler = new CountingHandler();
        // events directly on propagation start date are not triggered,
        // so move the event date slightly after
        AbsoluteDate eventDate = initDate.shiftedBy(FastMath.ulp(100.0) / 10.0);
        DateDetector detector = new DateDetector(10, 1e-9, eventDate)
                .withHandler(handler);
        // propagation works fine w/o event detector, but breaks with it
        ephemeris.addEventDetector(detector);

        // action + verify
        // propagate forward
        Assert.assertEquals(ephemeris.propagate(end).getDate().durationFrom(end), 0.0, 0.0);
        // propagate backward
        Assert.assertEquals(ephemeris.propagate(initDate).getDate().durationFrom(initDate), 0.0, 0.0);
        Assert.assertEquals(2, handler.eventCount);
    }

    /** Counts the number of events that have occurred. */
    private static class CountingHandler
            implements EventHandler<EventDetector> {

        /**
         * number of calls to {@link #eventOccurred(SpacecraftState,
         * EventDetector, boolean)}.
         */
        private int eventCount = 0;

        @Override
        public Action eventOccurred(SpacecraftState s,
                                    EventDetector detector,
                                    boolean increasing) {
            eventCount++;
            return Action.CONTINUE;
        }

        @Override
        public SpacecraftState resetState(EventDetector detector,
                                          SpacecraftState oldState) {
            return null;
        }
    }

    /**
     * check propagation succeeds when two events are within the tolerance of
     * each other.
     */
    @Test
    public void testCloseEventDates() {
        // setup
        DateDetector d1 = new DateDetector(10, 1, initDate.shiftedBy(15))
                .withHandler(new ContinueOnEvent<DateDetector>());
        DateDetector d2 = new DateDetector(10, 1, initDate.shiftedBy(15.5))
                .withHandler(new ContinueOnEvent<DateDetector>());
        propagator.addEventDetector(d1);
        propagator.addEventDetector(d2);

        //action
        AbsoluteDate end = initDate.shiftedBy(30);
        SpacecraftState actual = propagator.propagate(end);

        //verify
        Assert.assertEquals(actual.getDate().durationFrom(end), 0.0, 0.0);
    }

    @Test
    public void testEphemerisDates() {
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-01", tai);
        AbsoluteDate startDate = new AbsoluteDate("2015-07-03", tai).shiftedBy(-0.1);
        AbsoluteDate endDate = new AbsoluteDate("2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, eci, initialDate, mu);
        OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = NumericalPropagator.tolerances(1e-3, orbit, type);
        NumericalPropagator prop = new NumericalPropagator(
                new DormandPrince853Integrator(0.1, 500, tol[0], tol[1]));
        prop.setOrbitType(type);
        prop.resetInitialState(new SpacecraftState(new CartesianOrbit(orbit)));

        //action
        prop.setEphemerisMode();
        prop.propagate(startDate, endDate);
        BoundedPropagator ephemeris = prop.getGeneratedEphemeris();

        //verify
        TimeStampedPVCoordinates actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedPVCoordinates expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate),
                OrekitMatchers.closeTo(0, 0));
        //test date
        AbsoluteDate date = endDate.shiftedBy(-0.11);
        Assert.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date), 0, 0);
    }

    @Test
    public void testEphemerisDatesBackward() {
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-05", tai);
        AbsoluteDate startDate = new AbsoluteDate("2015-07-03", tai).shiftedBy(-0.1);
        AbsoluteDate endDate = new AbsoluteDate("2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, eci, initialDate, mu);
        OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = NumericalPropagator.tolerances(1e-3, orbit, type);
        NumericalPropagator prop = new NumericalPropagator(
                new DormandPrince853Integrator(0.1, 500, tol[0], tol[1]));
        prop.setOrbitType(type);
        prop.resetInitialState(new SpacecraftState(new CartesianOrbit(orbit)));

        //action
        prop.setEphemerisMode();
        prop.propagate(endDate, startDate);
        BoundedPropagator ephemeris = prop.getGeneratedEphemeris();

        //verify
        TimeStampedPVCoordinates actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedPVCoordinates expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate),
                OrekitMatchers.closeTo(0, 0));
        //test date
        AbsoluteDate date = endDate.shiftedBy(-0.11);
        Assert.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date), 0, 0);
    }

    @Test
    public void testNoExtrapolation() {

        // Propagate of the initial at the initial date
        final SpacecraftState finalState = propagator.propagate(initDate);

        // Initial orbit definition
        final Vector3D initialPosition = initialState.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = initialState.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition   = finalState.getPVCoordinates().getPosition();
        final Vector3D finalVelocity   = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assert.assertEquals(initialPosition.getX(), finalPosition.getX(), 1.0e-10);
        Assert.assertEquals(initialPosition.getY(), finalPosition.getY(), 1.0e-10);
        Assert.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 1.0e-10);

    }

    @Test(expected=OrekitException.class)
    public void testNotInitialised1() {
        final AbstractIntegratedPropagator notInitialised =
            new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
        notInitialised.propagate(AbsoluteDate.J2000_EPOCH);
    }

    @Test(expected=OrekitException.class)
    public void testNotInitialised2() {
        final AbstractIntegratedPropagator notInitialised =
            new NumericalPropagator(new ClassicalRungeKuttaIntegrator(10.0));
        notInitialised.propagate(AbsoluteDate.J2000_EPOCH, AbsoluteDate.J2000_EPOCH.shiftedBy(3600));
    }

    @Test
    public void testKepler() {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final SpacecraftState finalState =
            propagator.propagate(initDate.shiftedBy(-60), initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 2.0e-9);

    }

    @Test
    public void testCartesian() {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        propagator.setOrbitType(OrbitType.CARTESIAN);
        final PVCoordinates finalState =
            propagator.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        final Vector3D pFin = finalState.getPosition();
        final Vector3D vFin = finalState.getVelocity();

        // Check results
        final PVCoordinates reference = initialState.shiftedBy(dt).getPVCoordinates();
        final Vector3D pRef = reference.getPosition();
        final Vector3D vRef = reference.getVelocity();
        Assert.assertEquals(0, pRef.subtract(pFin).getNorm(), 2e-4);
        Assert.assertEquals(0, vRef.subtract(vFin).getNorm(), 7e-8);

        try {
            propagator.getGeneratedEphemeris();
            Assert.fail("an exception should have been thrown");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    @Test
    public void testPropagationTypesElliptical() throws ParseException, IOException {
     // setup
        AbsoluteDate         initDate  = new AbsoluteDate();
        SpacecraftState     initialState;
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;

        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit, type);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getNormalizedProvider(5, 5));
        propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final PVCoordinates pv = initialState.getPVCoordinates();
        final double dP = 0.001;
        final double dV = initialState.getMu() * dP /
                          (pv.getPosition().getNormSq() * pv.getVelocity().getNorm());

        final PVCoordinates pvcM = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngle.MEAN);
        final PVCoordinates pviM = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngle.MEAN);
        final PVCoordinates pveM = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngle.MEAN);
        final PVCoordinates pvkM = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngle.MEAN);

        final PVCoordinates pvcE = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngle.ECCENTRIC);
        final PVCoordinates pviE = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngle.ECCENTRIC);
        final PVCoordinates pveE = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC);
        final PVCoordinates pvkE = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngle.ECCENTRIC);

        final PVCoordinates pvcT = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngle.TRUE);
        final PVCoordinates pviT = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngle.TRUE);
        final PVCoordinates pveT = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        final PVCoordinates pvkT = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngle.TRUE);

        Assert.assertEquals(0, pvcM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 3.0);
        Assert.assertEquals(0, pvcM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2.0);
        Assert.assertEquals(0, pviM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.6);
        Assert.assertEquals(0, pviM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.4);
        Assert.assertEquals(0, pvkM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.5);
        Assert.assertEquals(0, pvkM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.3);
        Assert.assertEquals(0, pveM.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.2);
        Assert.assertEquals(0, pveM.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.2);

        Assert.assertEquals(0, pvcE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 3.0);
        Assert.assertEquals(0, pvcE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2.0);
        Assert.assertEquals(0, pviE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.03);
        Assert.assertEquals(0, pviE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.04);
        Assert.assertEquals(0, pvkE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.4);
        Assert.assertEquals(0, pvkE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.3);
        Assert.assertEquals(0, pveE.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.2);
        Assert.assertEquals(0, pveE.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.07);

        Assert.assertEquals(0, pvcT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 3.0);
        Assert.assertEquals(0, pvcT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 2.0);
        Assert.assertEquals(0, pviT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pviT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.2);
        Assert.assertEquals(0, pvkT.getPosition().subtract(pveT.getPosition()).getNorm() / dP, 0.4);
        Assert.assertEquals(0, pvkT.getVelocity().subtract(pveT.getVelocity()).getNorm() / dV, 0.2);

    }

    @Test
    public void testPropagationTypesHyperbolic() throws ParseException, IOException {

        SpacecraftState state =
            new SpacecraftState(new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                   PositionAngle.TRUE,
                                                   FramesFactory.getEME2000(), initDate,
                                                   mu));

        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getNormalizedProvider(5, 5));
        propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final PVCoordinates pv = state.getPVCoordinates();
        final double dP = 0.001;
        final double dV = state.getMu() * dP /
                          (pv.getPosition().getNormSq() * pv.getVelocity().getNorm());

        final PVCoordinates pvcM = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngle.MEAN);
        final PVCoordinates pvkM = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngle.MEAN);

        final PVCoordinates pvcE = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngle.ECCENTRIC);
        final PVCoordinates pvkE = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC);

        final PVCoordinates pvcT = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngle.TRUE);
        final PVCoordinates pvkT = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngle.TRUE);

        Assert.assertEquals(0, pvcM.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvcM.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.4);
        Assert.assertEquals(0, pvkM.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.2);
        Assert.assertEquals(0, pvkM.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.3);

        Assert.assertEquals(0, pvcE.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvcE.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.4);
        Assert.assertEquals(0, pvkE.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.009);
        Assert.assertEquals(0, pvkE.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.006);

        Assert.assertEquals(0, pvcT.getPosition().subtract(pvkT.getPosition()).getNorm() / dP, 0.3);
        Assert.assertEquals(0, pvcT.getVelocity().subtract(pvkT.getVelocity()).getNorm() / dV, 0.4);

    }

    private PVCoordinates propagateInType(SpacecraftState state, double dP,
                                          OrbitType type, PositionAngle angle)
        {

        final double dt = 3200;
        final double minStep = 0.001;
        final double maxStep = 1000;

        double[][] tol = NumericalPropagator.tolerances(dP, state.getOrbit(), type);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        NumericalPropagator newPropagator = new NumericalPropagator(integrator);
        newPropagator.setOrbitType(type);
        newPropagator.setPositionAngleType(angle);
        newPropagator.setInitialState(state);
        for (ForceModel force: propagator.getAllForceModels()) {
            newPropagator.addForceModel(force);
        }
        return newPropagator.propagate(state.getDate().shiftedBy(dt)).getPVCoordinates();

    }

    @Test(expected=OrekitException.class)
    public void testException() {
        propagator.setMasterMode(new OrekitStepHandler() {
            private int countDown = 3;
            private AbsoluteDate previousCall = null;
            public void init(SpacecraftState s0, AbsoluteDate t) {
            }
            public void handleStep(OrekitStepInterpolator interpolator,
                                   boolean isLast) {
                if (previousCall != null) {
                    Assert.assertTrue(interpolator.getCurrentState().getDate().compareTo(previousCall) < 0);
                }
                if (--countDown == 0) {
                    throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE, "dummy error");
                }
            }
        });
        propagator.propagate(initDate.shiftedBy(-3600));
    }

    @Test
    public void testStopEvent() {
        final AbsoluteDate stopDate = initDate.shiftedBy(1000);
        CheckingHandler<DateDetector> checking = new CheckingHandler<DateDetector>(Action.STOP);
        propagator.addEventDetector(new DateDetector(stopDate).withHandler(checking));
        Assert.assertEquals(1, propagator.getEventsDetectors().size());
        checking.assertEvent(false);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
        propagator.clearEventsDetectors();
        Assert.assertEquals(0, propagator.getEventsDetectors().size());

    }

    @Test
    public void testResetStateEvent() {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        CheckingHandler<DateDetector> checking = new CheckingHandler<DateDetector>(Action.RESET_STATE) {
            public SpacecraftState resetState(DateDetector detector, SpacecraftState oldState) {
                return new SpacecraftState(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass() - 200.0);
            }
        };
        propagator.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        checking.assertEvent(false);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(initialState.getMass() - 200, finalState.getMass(), 1.0e-10);
    }

    @Test
    public void testResetDerivativesEvent() {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        CheckingHandler<DateDetector> checking = new CheckingHandler<DateDetector>(Action.RESET_DERIVATIVES);
        propagator.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        Assert.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        propagator.setResetAtEnd(true);
        final SpacecraftState finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        Assert.assertEquals(dt, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        checking.assertEvent(true);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    @Test
    public void testContinueEvent() {
        final AbsoluteDate resetDate = initDate.shiftedBy(1000);
        CheckingHandler<DateDetector> checking = new CheckingHandler<DateDetector>(Action.CONTINUE);
        propagator.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        Assert.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        propagator.setResetAtEnd(false);
        final SpacecraftState finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        Assert.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate), 1.0e-10);
        checking.assertEvent(true);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA()) / initialState.getA();
        Assert.assertEquals(initialState.getA(),    finalState.getA(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx(),    finalState.getEquinoctialEx(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy(),    finalState.getEquinoctialEy(),    1.0e-10);
        Assert.assertEquals(initialState.getHx(),    finalState.getHx(),    1.0e-10);
        Assert.assertEquals(initialState.getHy(),    finalState.getHy(),    1.0e-10);
        Assert.assertEquals(initialState.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    @Test
    public void testAdditionalStateEvent() {
        propagator.addAdditionalEquations(new AdditionalEquations() {

            public String getName() {
                return "linear";
            }

            public double[] computeDerivatives(SpacecraftState s, double[] pDot) {
                pDot[0] = 1.0;
                return new double[7];
            }
        });
        try {
            propagator.addAdditionalEquations(new AdditionalEquations() {

                public String getName() {
                    return "linear";
                }

                public double[] computeDerivatives(SpacecraftState s, double[] pDot) {
                    pDot[0] = 1.0;
                    return new double[7];
                }
            });
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
        try {
            propagator.addAdditionalStateProvider(new AdditionalStateProvider() {
               public String getName() {
                    return "linear";
                }

                public double[] getAdditionalState(SpacecraftState state) {
                    return null;
                }
            });
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
        propagator.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() {
                return "constant";
            }

            public double[] getAdditionalState(SpacecraftState state) {
                return new double[] { 1.0 };
            }
        });
        Assert.assertTrue(propagator.isAdditionalStateManaged("linear"));
        Assert.assertTrue(propagator.isAdditionalStateManaged("constant"));
        Assert.assertFalse(propagator.isAdditionalStateManaged("non-managed"));
        Assert.assertEquals(2, propagator.getManagedAdditionalStates().length);
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("linear", 1.5));

        CheckingHandler<AdditionalStateLinearDetector> checking =
                new CheckingHandler<AdditionalStateLinearDetector>(Action.STOP);
        propagator.addEventDetector(new AdditionalStateLinearDetector(10.0, 1.0e-8).withHandler(checking));

        final double dt = 3200;
        checking.assertEvent(false);
        final SpacecraftState finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        Assert.assertEquals(3.0, finalState.getAdditionalState("linear")[0], 1.0e-8);
        Assert.assertEquals(1.5, finalState.getDate().durationFrom(initDate), 1.0e-8);

    }

    private static class AdditionalStateLinearDetector extends AbstractDetector<AdditionalStateLinearDetector> {

        public AdditionalStateLinearDetector(double maxCheck, double threshold) {
            this(maxCheck, threshold, DEFAULT_MAX_ITER, new StopOnEvent<AdditionalStateLinearDetector>());
        }

        private AdditionalStateLinearDetector(double maxCheck, double threshold, int maxIter,
                                              EventHandler<? super AdditionalStateLinearDetector> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        protected AdditionalStateLinearDetector create(final double newMaxCheck, final double newThreshold,
                                                       final int newMaxIter,
                                                       final EventHandler<? super AdditionalStateLinearDetector> newHandler) {
            return new AdditionalStateLinearDetector(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        public double g(SpacecraftState s) {
            return s.getAdditionalState("linear")[0] - 3.0;
        }

    }

    @Test
    public void testResetAdditionalStateEvent() {
        propagator.addAdditionalEquations(new AdditionalEquations() {

            public String getName() {
                return "linear";
            }

            public double[] computeDerivatives(SpacecraftState s, double[] pDot) {
                pDot[0] = 1.0;
                return null;
            }
        });
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("linear", 1.5));

        CheckingHandler<AdditionalStateLinearDetector> checking =
            new CheckingHandler<AdditionalStateLinearDetector>(Action.RESET_STATE) {
            public SpacecraftState resetState(AdditionalStateLinearDetector detector, SpacecraftState oldState)
                {
                return oldState.addAdditionalState("linear", oldState.getAdditionalState("linear")[0] * 2);
            }
        };

        propagator.addEventDetector(new AdditionalStateLinearDetector(10.0, 1.0e-8).withHandler(checking));

        final double dt = 3200;
        checking.assertEvent(false);
        final SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        Assert.assertEquals(dt + 4.5, finalState.getAdditionalState("linear")[0], 1.0e-8);
        Assert.assertEquals(dt, finalState.getDate().durationFrom(initDate), 1.0e-8);

    }

    @Test
    public void testEventDetectionBug() throws IOException, ParseException {

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2005, 1, 1, 0, 0, 0.0, utc);
        double duration = 100000.;
        AbsoluteDate endDate = new AbsoluteDate(initialDate, duration);

        // Initialization of the frame EME2000
        Frame EME2000 = FramesFactory.getEME2000();


        // Initial orbit
        double a = 35786000. + 6378137.0;
        double e = 0.70;
        double rApogee = a*(1+e);
        double vApogee = FastMath.sqrt(mu*(1-e)/(a*(1+e)));
        Orbit geo = new CartesianOrbit(new PVCoordinates(new Vector3D(rApogee, 0., 0.),
                                                         new Vector3D(0., vApogee, 0.)), EME2000,
                                                         initialDate, mu);


        duration = geo.getKeplerianPeriod();
        endDate = new AbsoluteDate(initialDate, duration);

        // Numerical Integration
        final double minStep  = 0.001;
        final double maxStep  = 1000;
        final double initStep = 60;
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};

        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(minStep, maxStep, absTolerance, relTolerance);
        integrator.setInitialStepSize(initStep);

        // Numerical propagator based on the integrator
        propagator = new NumericalPropagator(integrator);
        double mass = 1000.;
        SpacecraftState initialState = new SpacecraftState(geo, mass);
        propagator.setInitialState(initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);


        // Set the events Detectors
        ApsideDetector event1 = new ApsideDetector(geo);
        propagator.addEventDetector(event1);

        // Set the propagation mode
        propagator.setSlaveMode();

        // Propagate
        SpacecraftState finalState = propagator.propagate(endDate);

        // we should stop long before endDate
        Assert.assertTrue(endDate.durationFrom(finalState.getDate()) > 40000.0);
    }

    @Test
    public void testEphemerisGenerationIssue14() throws IOException {

        // Propagation of the initial at t + dt
        final double dt = 3200;
        propagator.getInitialState();

        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setEphemerisMode();
        propagator.propagate(initDate.shiftedBy(dt));
        final BoundedPropagator ephemeris1 = propagator.getGeneratedEphemeris();
        Assert.assertEquals(initDate, ephemeris1.getMinDate());
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        propagator.getPVCoordinates(initDate.shiftedBy( 2 * dt), FramesFactory.getEME2000());
        propagator.getPVCoordinates(initDate.shiftedBy(-2 * dt), FramesFactory.getEME2000());

        // the new propagations should not have changed ephemeris1
        Assert.assertEquals(initDate, ephemeris1.getMinDate());
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        final BoundedPropagator ephemeris2 = propagator.getGeneratedEphemeris();
        Assert.assertEquals(initDate.shiftedBy(-2 * dt), ephemeris2.getMinDate());
        Assert.assertEquals(initDate.shiftedBy( 2 * dt), ephemeris2.getMaxDate());

        // generating ephemeris2 should not have changed ephemeris1
        Assert.assertEquals(initDate, ephemeris1.getMinDate());
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

    }

    @Test
    public void testEphemerisAdditionalState() throws IOException {

        // Propagation of the initial at t + dt
        final double dt = -3200;
        final double rate = 2.0;

        propagator.addAdditionalStateProvider(new AdditionalStateProvider() {
            public String getName() {
                return "squaredA";
            }
            public double[] getAdditionalState(SpacecraftState state) {
                return new double[] { state.getA() * state.getA() };
            }
        });
        propagator.addAdditionalEquations(new AdditionalEquations() {
            public String getName() {
                return "extra";
            }
            public double[] computeDerivatives(SpacecraftState s, double[] pDot) {
                pDot[0] = rate;
                return null;
            }
        });
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("extra", 1.5));

        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setEphemerisMode();
        propagator.propagate(initDate.shiftedBy(dt));
        final BoundedPropagator ephemeris1 = propagator.getGeneratedEphemeris();
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMinDate());
        Assert.assertEquals(initDate, ephemeris1.getMaxDate());
        try {
            ephemeris1.propagate(ephemeris1.getMinDate().shiftedBy(-10.0));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException pe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, pe.getSpecifier());
        }
        try {
            ephemeris1.propagate(ephemeris1.getMaxDate().shiftedBy(+10.0));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException pe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_EPHEMERIDES_DATE, pe.getSpecifier());
        }

        double shift = -60;
        SpacecraftState s = ephemeris1.propagate(initDate.shiftedBy(shift));
        Assert.assertEquals(2, s.getAdditionalStates().size());
        Assert.assertTrue(s.hasAdditionalState("squaredA"));
        Assert.assertTrue(s.hasAdditionalState("extra"));
        Assert.assertEquals(s.getA() * s.getA(), s.getAdditionalState("squaredA")[0], 1.0e-10);
        Assert.assertEquals(1.5 + shift * rate, s.getAdditionalState("extra")[0], 1.0e-10);

        try {
            ephemeris1.resetInitialState(s);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }

    }

    @Test
    public void testIssue157() {
        try {
            Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngle.MEAN,
                                             FramesFactory.getTOD(false),
                                             new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                             Constants.EIGEN5C_EARTH_MU);
            NumericalPropagator.tolerances(1.0, orbit, OrbitType.KEPLERIAN);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException pe) {
            Assert.assertEquals(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, pe.getSpecifier());
        }
    }

    private static class CheckingHandler<T extends EventDetector> implements EventHandler<T> {

        private final Action actionOnEvent;
        private boolean gotHere;

        public CheckingHandler(final Action actionOnEvent) {
            this.actionOnEvent = actionOnEvent;
            this.gotHere       = false;
        }

        public void assertEvent(boolean expected) {
            Assert.assertEquals(expected, gotHere);
        }

        public Action eventOccurred(SpacecraftState s, T detector, boolean increasing) {
            gotHere = true;
            return actionOnEvent;
        }

    }

    @Test
    public void testParallelismIssue258() throws InterruptedException, ExecutionException, FileNotFoundException {

        Utils.setDataRoot("regular-data:atmosphere:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final double mu = GravityFieldFactory.getNormalizedProvider(2, 2).getMu();

        // Geostationary transfer orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.72831215; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascension of ascending node
        final double lM = 0; // mean anomaly
        final Frame inertialFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate initialDate = new AbsoluteDate(2003, 1, 1, 00, 00, 00.000, utc);
        final Orbit initialOrbit = new CartesianOrbit( new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                                          inertialFrame, initialDate, mu));
        final SpacecraftState initialState = new SpacecraftState(initialOrbit, 1000);

        // initialize the testing points
        final List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        final NumericalPropagator propagator = createPropagator(initialState, OrbitType.CARTESIAN, PositionAngle.TRUE);
        final double samplingStep = 10000.0;
        propagator.setMasterMode(samplingStep, (state, isLast) -> states.add(state));
        propagator.propagate(initialDate.shiftedBy(5 * samplingStep));

        // compute reference errors, using serial computation in a for loop
        final double[][] referenceErrors = new double[states.size() - 1][];
        for (int startIndex = 0; startIndex < states.size() - 1; ++startIndex) {
            referenceErrors[startIndex] = recomputeFollowing(startIndex, states);
        }

        final Consumer<SpacecraftState> checker = point -> {
            try {
                final int startIndex = states.indexOf(point);
                double[] errors = recomputeFollowing(startIndex, states);
                for (int k = 0; k < errors.length; ++k) {
                    Assert.assertEquals(startIndex + " to " + (startIndex + k + 1),
                                        referenceErrors[startIndex][k], errors[k],
                                        1.0e-9);
                }
            } catch (OrekitException oe) {
                Assert.fail(oe.getLocalizedMessage());
            }
        };

        // serial propagation using Stream
        states.stream().forEach(checker);

        // parallel propagation using parallelStream
        states.parallelStream().forEach(checker);

    }

    @Test
    public void testShiftKeplerianEllipticTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftKeplerianEllipticTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftKeplerianEllipticEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftKeplerianEllipticEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftKeplerianEllipticMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftKeplerianEllipticMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.KEPLERIAN, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftKeplerianHyperbolicTrueWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngle.TRUE, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftKeplerianHyperbolicTrueWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftKeplerianHyperbolicEccentricWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftKeplerianHyperbolicEcentricWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftKeplerianHyperbolicMeanWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngle.MEAN, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftKeplerianHyperbolicMeanWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.KEPLERIAN, PositionAngle.MEAN, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCartesianEllipticTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCartesianEllipticTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCartesianEllipticEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCartesianEllipticEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCartesianEllipticMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCartesianEllipticMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CARTESIAN, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCartesianHyperbolicTrueWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngle.TRUE, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftCartesianHyperbolicTrueWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngle.TRUE, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCartesianHyperbolicEccentricWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftCartesianHyperbolicEcentricWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCartesianHyperbolicMeanWithoutDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngle.MEAN, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftCartesianHyperbolicMeanWithDerivatives() {
        doTestShift(createHyperbolicOrbit(), OrbitType.CARTESIAN, PositionAngle.MEAN, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCircularTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCircularTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCircularEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCircularEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCircularMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCircularMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.CIRCULAR, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftEquinoctialTrueWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftEquinoctialTrueWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftEquinoctialEccentricWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftEquinoctialEcentricWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftEquinoctialMeanWithoutDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftEquinoctialMeanWithDerivatives() {
        doTestShift(createEllipticOrbit(), OrbitType.EQUINOCTIAL, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    private static void doTestShift(final CartesianOrbit orbit, final OrbitType orbitType,
                                    final PositionAngle angleType, final boolean withDerivatives,
                                    final double error60s, final double error120s,
                                    final double error300s, final double error600s,
                                    final double error900s)
        {

        Utils.setDataRoot("regular-data:atmosphere:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final NumericalPropagator np = createPropagator(new SpacecraftState(orbit), orbitType, angleType);

        // the reference date for shifts is set at 60s, so the propagator can provide derivatives if needed
        // (derivatives are not available in the initial orbit)
        final AbsoluteDate reference = orbit.getDate().shiftedBy(60.0);
        final ShiftChecker checker   = new ShiftChecker(withDerivatives, orbitType, angleType,
                                                        error60s,
                                                        error120s, error300s,
                                                        error600s, error900s);
        np.addEventDetector(new DateDetector(30.0, 1.0e-9, reference,
                                             reference.shiftedBy( 60.0),
                                             reference.shiftedBy(120.0),
                                             reference.shiftedBy(300.0),
                                             reference.shiftedBy(600.0),
                                             reference.shiftedBy(900.0)).
                            withHandler(checker));
        np.propagate(reference.shiftedBy(1000.0));
    }

    private static class ShiftChecker implements EventHandler<DateDetector> {

        private final boolean       withDerivatives;
        private final OrbitType     orbitType;
        private final PositionAngle angleType;
        private final double        error60s;
        private final double        error120s;
        private final double        error300s;
        private final double        error600s;
        private final double        error900s;
        private SpacecraftState     referenceState;

        ShiftChecker(final boolean withDerivatives, final OrbitType orbitType,
                     final PositionAngle angleType, final double error60s,
                     final double error120s, final double error300s,
                     final double error600s, final double error900s) {
            this.withDerivatives = withDerivatives;
            this.orbitType       = orbitType;
            this.angleType       = angleType;
            this.error60s        = error60s;
            this.error120s       = error120s;
            this.error300s       = error300s;
            this.error600s       = error600s;
            this.error900s       = error900s;
            this.referenceState  = null;
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final DateDetector detector,
                                    final boolean increasing)
            {
            if (referenceState == null) {
                // first event, we retrieve the reference state for later use
                if (withDerivatives) {
                    referenceState = s;
                } else {
                    // remove derivatives, to check accuracy of the shiftedBy method decreases without them
                    final double[] stateVector = new double[6];
                    final Orbit o = s.getOrbit();
                    orbitType.mapOrbitToArray(o, angleType, stateVector, null);
                    final Orbit fixedOrbit = orbitType.mapArrayToOrbit(stateVector, null, angleType,
                                                                       o.getDate(), o.getMu(), o.getFrame());
                    referenceState = new SpacecraftState(fixedOrbit, s.getAttitude(), s.getMass());
                }
            } else {
                // recurring event, we compare with the shifted reference state
                final double dt = s.getDate().durationFrom(referenceState.getDate());
                final SpacecraftState shifted = referenceState.shiftedBy(dt);
                final double error = Vector3D.distance(shifted.getPVCoordinates().getPosition(),
                                                       s.getPVCoordinates().getPosition());
                switch ((int) FastMath.rint(dt)) {
                    case 60 :
                        Assert.assertEquals(error60s,  error, 0.01 * error60s);
                        break;
                    case 120 :
                        Assert.assertEquals(error120s, error, 0.01 * error120s);
                        break;
                    case 300 :
                        Assert.assertEquals(error300s, error, 0.01 * error300s);
                        break;
                    case 600 :
                        Assert.assertEquals(error600s, error, 0.01 * error600s);
                        break;
                    case 900 :
                        Assert.assertEquals(error900s, error, 0.01 * error900s);
                        break;
                    default :
                        // this should never happen
                        Assert.fail("no error set for dt = " + dt);
                        break;
                }
            }
            return Action.CONTINUE;
        }

    }

    /** Test de-activation of event detection and step handling.
     *  When propagating out of start and target date in propagate(startDate, targetDate)
     *  <p>See issue 449 in Orekit forge and 
     *  {@link org.orekit.propagation.Propagator#propagate(AbsoluteDate, AbsoluteDate)}.
     *  </p>
     */
    @Test
    public void testEventAndStepHandlerDeactivationIssue449() {

        // Setup
        RecordAndContinue<DateDetector> recordAndContinue = new RecordAndContinue<>();
        DateDetector dateDetector = new DateDetector(1, 1E-1,
                                                     initDate.shiftedBy(10.),
                                                     initDate.shiftedBy(15.),
                                                     initDate.shiftedBy(20.))
                        .withHandler(recordAndContinue);

        propagator.addEventDetector(dateDetector);
        
        final AbsoluteDate startDate = initDate.shiftedBy(30.);
        final AbsoluteDate finalDate = initDate.shiftedBy(40.);
        
        final DateRecorderHandler dateRecorderHandler = new DateRecorderHandler(startDate, finalDate);
        propagator.setMasterMode(1.0, dateRecorderHandler);

        // Action
        propagator.propagate(startDate, finalDate);

        // Verify
        // No event is detected
        Assert.assertEquals(0, recordAndContinue.getEvents().size());
        
        // Handler is deactivated (no dates recorded between start and stop date)
        Assert.assertEquals(0, dateRecorderHandler.handledDatesOutOfInterval.size());
    }
    
    /** Record the dates treated by the handler.
     *  If they are out of an interval defined by a start and final date.
     */
    private static class DateRecorderHandler implements OrekitFixedStepHandler {

        /** Start date of the propagation. */
        private final AbsoluteDate startDate;
        
        /** Final date of the propagation. */
        private final AbsoluteDate finalDate;
        
        /** List of handled date. Recorded only if they are out of the propagation interval. */
        public final List<AbsoluteDate> handledDatesOutOfInterval;
        
        DateRecorderHandler(final AbsoluteDate startDate, final AbsoluteDate finalDate) {
          this.startDate = startDate;
          this.finalDate = finalDate;
          this.handledDatesOutOfInterval = new ArrayList<>();
        }

        @Override
        public void handleStep(SpacecraftState currentState, boolean isLast)
                {
          final AbsoluteDate date = currentState.getDate();
          if (date.compareTo(startDate) < 0 || date.compareTo(finalDate) > 0) {
            handledDatesOutOfInterval.add(currentState.getDate());
          }
        }
      }
    
    /**
     * Assume we have 5 epochs, we will propagate from the input epoch to all the following epochs.
     *   If we have [0, 1, 2, 3, 4], and input is 2, then we will do 2->3, 2->4.
     * @param startIndex index of start state
     * @param states all states
     * @return position error for recomputed following points
     */
    private static double[] recomputeFollowing(final int startIndex, List<SpacecraftState> allPoints)
        {
        SpacecraftState startState = allPoints.get(startIndex);
        NumericalPropagator innerPropagator = createPropagator(startState, OrbitType.CARTESIAN, PositionAngle.TRUE);
        double[] errors = new double[allPoints.size() - startIndex - 1];
        for (int endIndex = startIndex + 1; endIndex < allPoints.size(); ++endIndex) {
            final TimeStampedPVCoordinates reference  = allPoints.get(endIndex).getPVCoordinates();
            final TimeStampedPVCoordinates recomputed = innerPropagator.propagate(reference.getDate()).getPVCoordinates();
            errors[endIndex - startIndex - 1] = Vector3D.distance(recomputed.getPosition(), reference.getPosition());
        }
        return errors;
    }

    private synchronized static NumericalPropagator createPropagator(SpacecraftState spacecraftState,
                                                                     OrbitType orbitType, PositionAngle angleType)
        {

        final double minStep                         = 0.001;
        final double maxStep                         = 120.0;
        final double positionTolerance               = 0.1;
        final int    degree                          = 20;
        final int    order                           = 20;
        final double spacecraftArea                  = 1.0;
        final double spacecraftDragCoefficient       = 2.0;
        final double spacecraftReflectionCoefficient = 2.0;

        // propagator main configuration
        final double[][] tol           = NumericalPropagator.tolerances(positionTolerance, spacecraftState.getOrbit(), orbitType);
        final ODEIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        final NumericalPropagator np   = new NumericalPropagator(integrator);
        np.setOrbitType(orbitType);
        np.setPositionAngleType(angleType);
        np.setInitialState(spacecraftState);

        // Earth gravity field
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final NormalizedSphericalHarmonicsProvider harmonicsGravityProvider = GravityFieldFactory.getNormalizedProvider(degree, order);
        np.addForceModel(new HolmesFeatherstoneAttractionModel(earth.getBodyFrame(), harmonicsGravityProvider));

        // Sun and Moon attraction
        np.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        np.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

        // atmospheric drag
        MarshallSolarActivityFutureEstimation msafe =
                        new MarshallSolarActivityFutureEstimation("Jan2000F10-edited-data\\.txt",
                                                                  MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        DataContext.getDefault().getDataProvidersManager().feed(msafe.getSupportedNames(), msafe);
        DTM2000 atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), earth);
        np.addForceModel(new DragForce(atmosphere, new IsotropicDrag(spacecraftArea, spacecraftDragCoefficient)));

        // solar radiation pressure
        np.addForceModel(new SolarRadiationPressure(CelestialBodyFactory.getSun(),
                                                    earth.getEquatorialRadius(),
                                                    new IsotropicRadiationSingleCoefficient(spacecraftArea, spacecraftReflectionCoefficient)));

        return np;

    }

    private CartesianOrbit createEllipticOrbit() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        return new CartesianOrbit(pv, frame, mu);
    }

    private CartesianOrbit createHyperbolicOrbit() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        return new CartesianOrbit(pv, frame, mu);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit, OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        propagator = null;
    }

    /**
     * Adapter class for {@link ForceModel} so that sub classes only have to implement the
     * methods they want.
     */
    private static class ForceModelAdapter implements ForceModel {

        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        @Override
        public boolean isSupported(String name) {
            return false;
        }

        @Override
        public void addContribution(SpacecraftState s, TimeDerivativesEquations adder) {
        }

        @Override
        public <T extends RealFieldElement<T>> void
        addContribution(FieldSpacecraftState<T> s,
                        FieldTimeDerivativesEquations<T> adder) {
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
            {
            return Vector3D.ZERO;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                             final T[] parameters)
            {
            return FieldVector3D.getZero(s.getDate().getField());
        }

        @Override
        public Stream<EventDetector> getEventsDetectors() {
            return Stream.empty();
        }

        @Override
        public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
            return Stream.empty();
        }

        @Override
        public ParameterDriver[] getParametersDrivers() {
            return new ParameterDriver[0];
        }

        @Override
        public ParameterDriver getParameterDriver(String name)
            {
            final ParameterDriver[] drivers =  getParametersDrivers();
            final String[] names = new String[drivers.length];
            for (int i = 0; i < names.length; ++i) {
                names[i] = drivers[i].getName();
            }
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME,
                                      name, names);
        }

    }

}

