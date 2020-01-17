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

import java.lang.reflect.Array;

import org.hamcrest.MatcherAssert;
import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
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
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.integration.FieldAbstractIntegratedPropagator;
import org.orekit.propagation.integration.FieldAdditionalEquations;
import org.orekit.propagation.sampling.FieldOrekitStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public class FieldNumericalPropagatorTest {

    private double               mu;

    @Test(expected=OrekitException.class)
    public void testNotInitialised1() {
        doTestNotInitialised1(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestNotInitialised1(Field<T> field) {
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);

        final FieldAbstractIntegratedPropagator<T> notInitialised =
            new FieldNumericalPropagator<>(field, new ClassicalRungeKuttaFieldIntegrator<>(field, field.getZero().add(10.0)));
        notInitialised.propagate(initDate);
    }

    @Test(expected=OrekitException.class)
    public void testNotInitialised2() {
        doTestNotInitialised2(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestNotInitialised2(Field<T> field) {
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldAbstractIntegratedPropagator<T> notInitialised =
            new FieldNumericalPropagator<>(field, new ClassicalRungeKuttaFieldIntegrator<>(field, field.getZero().add((10.0))));
        notInitialised.propagate(initDate, initDate.shiftedBy(3600));
    }

    @Test
    public void testEventAtEndOfEphemeris() {
        doTestEventAtEndOfEphemeris(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>, D extends FieldEventDetector<T>> void doTestEventAtEndOfEphemeris(Field<T> field) {

        T zero = field.getZero();
        FieldNumericalPropagator<T>  propagator = createPropagator(field);
        FieldAbsoluteDate<T> initDate = propagator.getInitialState().getDate();

        // choose duration that will round up when expressed as a double
        FieldAbsoluteDate<T> end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        propagator.setEphemerisMode();
        propagator.propagate(end);
        FieldBoundedPropagator<T> ephemeris = propagator.getGeneratedEphemeris();
        CountingHandler<D, T> handler = new CountingHandler<D, T>();
        FieldDateDetector<T> detector = new FieldDateDetector<>(zero.add(10), zero.add(1e-9), toArray(end)).withHandler(handler);
        // propagation works fine w/o event detector, but breaks with it
        ephemeris.addEventDetector(detector);

        //action
        // fails when this throws an "out of range date for ephemerides"
        FieldSpacecraftState<T> actual = ephemeris.propagate(end);

        //verify
        Assert.assertEquals(actual.getDate().durationFrom(end).getReal(), 0.0, 0.0);
        Assert.assertEquals(1, handler.eventCount);
    }

    @Test
    public void testEventAtBeginningOfEphemeris() {
        doTestEventAtBeginningOfEphemeris(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>, D extends FieldEventDetector<T>> void doTestEventAtBeginningOfEphemeris(Field<T> field)
        {

        T zero = field.getZero();
        FieldNumericalPropagator<T>  propagator = createPropagator(field);
        FieldAbsoluteDate<T> initDate = propagator.getInitialState().getDate();

        // setup
        // choose duration that will round up when expressed as a double
        FieldAbsoluteDate<T> end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        propagator.setEphemerisMode();
        propagator.propagate(end);
        FieldBoundedPropagator<T> ephemeris = propagator.getGeneratedEphemeris();
        CountingHandler<D, T> handler = new CountingHandler<D, T>();
        // events directly on propagation start date are not triggered,
        // so move the event date slightly after
        FieldAbsoluteDate<T> eventDate = initDate.shiftedBy(FastMath.ulp(100.0) / 10.0);
        FieldDateDetector<T> detector = new FieldDateDetector<>(zero.add(10), zero.add(1e-9), toArray(eventDate))
                .withHandler(handler);
        // propagation works fine w/o event detector, but breaks with it
        ephemeris.addEventDetector(detector);

        // action + verify
        // propagate forward
        Assert.assertEquals(ephemeris.propagate(end).getDate().durationFrom(end).getReal(), 0.0, 0.0);
        // propagate backward
        Assert.assertEquals(ephemeris.propagate(initDate).getDate().durationFrom(initDate).getReal(), 0.0, 0.0);
        Assert.assertEquals(2, handler.eventCount);
    }

    public class CountingHandler <D extends FieldEventDetector<T>, T extends RealFieldElement<T>>
            implements FieldEventHandler<FieldEventDetector<T>, T> {

        /**
         * number of calls to {@link #eventOccurred(FieldSpacecraftState<T>,
         * FieldEventDetector<T>, boolean)}.
         */
        private int eventCount = 0;

        @Override
        public Action eventOccurred(FieldSpacecraftState<T> s,
                                    FieldEventDetector<T> detector,
                                    boolean increasing) {
            eventCount++;
            return Action.CONTINUE;
        }

        @Override
        public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector,
                                          FieldSpacecraftState<T> oldState) {
            return null;
        }


    }

    @Test
    public void testCloseEventDates() {
        doTestCloseEventDates(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestCloseEventDates(Field<T> field) {

        T zero = field.getZero();
        FieldNumericalPropagator<T>  propagator = createPropagator(field);
        FieldAbsoluteDate<T> initDate = propagator.getInitialState().getDate();

        // setup
        FieldDateDetector<T> d1 = new FieldDateDetector<>(zero.add(10), zero.add(1), toArray(initDate.shiftedBy(15)))
                .withHandler(new FieldContinueOnEvent<FieldDateDetector<T>, T>());
        FieldDateDetector<T> d2 = new FieldDateDetector<>(zero.add(10), zero.add(1), toArray(initDate.shiftedBy(15.5)))
                .withHandler(new FieldContinueOnEvent<FieldDateDetector<T>, T>());
        propagator.addEventDetector(d1);
        propagator.addEventDetector(d2);

        //action
        FieldAbsoluteDate<T> end = initDate.shiftedBy(30);
        FieldSpacecraftState<T> actual = propagator.propagate(end);

        //verify
        Assert.assertEquals(actual.getDate().durationFrom(end).getReal(), 0.0, 0.0);
    }

    @Test
    public void testEphemerisDates() {
        doTestEphemerisDates(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestEphemerisDates(Field<T> field) {

        T zero = field.getZero();


        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, "2015-07-01", tai);
        FieldAbsoluteDate<T> startDate = new FieldAbsoluteDate<>(field, "2015-07-03", tai).shiftedBy(-0.1);
        FieldAbsoluteDate<T> endDate = new FieldAbsoluteDate<>(field, "2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS), zero, zero, zero, zero, zero,
                                                                 PositionAngle.TRUE, eci, initialDate, zero.add(mu));
        OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = NumericalPropagator.tolerances(1e-3, orbit.toOrbit(), type);
        FieldNumericalPropagator<T> prop = new FieldNumericalPropagator<>(field,
                new DormandPrince853FieldIntegrator<>(field, 0.1, 500, tol[0], tol[1]));
        prop.setOrbitType(type);
        prop.resetInitialState(new FieldSpacecraftState<>(new FieldCartesianOrbit<>(orbit)));

        //action
        prop.setEphemerisMode();
        prop.propagate(startDate, endDate);
        FieldBoundedPropagator<T> ephemeris = prop.getGeneratedEphemeris();

        //verify
        TimeStampedFieldPVCoordinates<T> actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedFieldPVCoordinates<T> expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition().toVector3D(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition().toVector3D(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity().toVector3D(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity().toVector3D(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate).getReal(),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate).getReal(),
                OrekitMatchers.closeTo(0, 0));
        //test date
        FieldAbsoluteDate<T> date = endDate.shiftedBy(-0.11);
        Assert.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date).getReal(), 0, 0);
    }

    @Test
    public void testEphemerisDatesBackward() {
        doTestEphemerisDatesBackward(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestEphemerisDatesBackward(Field<T> field) {

        T zero = field.getZero();
          //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, "2015-07-05", tai);
        FieldAbsoluteDate<T> startDate = new FieldAbsoluteDate<>(field, "2015-07-03", tai).shiftedBy(-0.1);
        FieldAbsoluteDate<T> endDate = new FieldAbsoluteDate<>(field, "2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<>(zero.add(600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS), zero, zero, zero, zero, zero,
                                                                 PositionAngle.TRUE, eci, initialDate, zero.add(mu));
        OrbitType type = OrbitType.CARTESIAN;
        double[][] tol = NumericalPropagator.tolerances(1e-3, orbit.toOrbit(), type);
        FieldNumericalPropagator<T> prop = new FieldNumericalPropagator<>(field,
                new DormandPrince853FieldIntegrator<>(field, 0.1, 500, tol[0], tol[1]));
        prop.setOrbitType(type);
        prop.resetInitialState(new FieldSpacecraftState<>(new FieldCartesianOrbit<>(orbit)));

        //action
        prop.setEphemerisMode();
        prop.propagate(endDate, startDate);
        FieldBoundedPropagator<T> ephemeris = prop.getGeneratedEphemeris();

        //verify
        TimeStampedFieldPVCoordinates<T> actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedFieldPVCoordinates<T> expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition().toVector3D(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition().toVector3D(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity().toVector3D(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity().toVector3D(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate).getReal(),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate).getReal(),
                OrekitMatchers.closeTo(0, 0));
        //test date
        FieldAbsoluteDate<T> date = endDate.shiftedBy(-0.11);
        Assert.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date).getReal(), 0, 0);
    }

    @Test
    public void testNoExtrapolation() {
        doTestNoExtrapolation(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestNoExtrapolation(Field<T> field) {

        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);





        // Propagate of the initial at the initial date
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate);
        // Initial orbit definition
        final FieldVector3D<T> initialPosition = initialState.getPVCoordinates().getPosition();
        final FieldVector3D<T> initialVelocity = initialState.getPVCoordinates().getVelocity();

        // Final orbit definition
        final FieldVector3D<T> finalPosition   = finalState.getPVCoordinates().getPosition();
        final FieldVector3D<T> finalVelocity   = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assert.assertEquals(initialPosition.getX().getReal(), finalPosition.getX().getReal(), 1.0e-10);
        Assert.assertEquals(initialPosition.getY().getReal(), finalPosition.getY().getReal(), 1.0e-10);
        Assert.assertEquals(initialPosition.getZ().getReal(), finalPosition.getZ().getReal(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getX().getReal(), finalVelocity.getX().getReal(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getY().getReal(), finalVelocity.getY().getReal(), 1.0e-10);
        Assert.assertEquals(initialVelocity.getZ().getReal(), finalVelocity.getZ().getReal(), 1.0e-10);

    }

    @Test
    public void testKepler() {
        doTestKepler(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestKepler(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(-60), initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu().divide(initialState.getA())).getReal() / initialState.getA().getReal();
        Assert.assertEquals(initialState.getA().getReal(),    finalState.getA().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(),    finalState.getEquinoctialEx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(),    finalState.getEquinoctialEy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHx().getReal(),    finalState.getHx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHy().getReal(),    finalState.getHy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getLM().getReal() + n * dt, finalState.getLM().getReal(), 2.0e-9);

    }

    @Test
    public void testCartesian() {
        doTestCartesian(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestCartesian(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        // Propagation of the initial at t + dt
        final T dt = zero.add(3200);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        final FieldPVCoordinates<T> finalState =
            propagator.propagate(initDate.shiftedBy(dt)).getPVCoordinates();
        final FieldVector3D<T> pFin = finalState.getPosition();
        final FieldVector3D<T> vFin = finalState.getVelocity();

        // Check results
        final FieldPVCoordinates<T> reference = initialState.shiftedBy(dt).getPVCoordinates();
        final FieldVector3D<T> pRef = reference.getPosition();
        final FieldVector3D<T> vRef = reference.getVelocity();
        Assert.assertEquals(0, pRef.subtract(pFin).getNorm().getReal(), 2e-4);
        Assert.assertEquals(0, vRef.subtract(vFin).getNorm().getReal(), 7e-8);

        try {
            propagator.getGeneratedEphemeris();
            Assert.fail("an exception should have been thrown");
        } catch (IllegalStateException ise) {
            // expected
        }
    }

    @Test
    public void testPropagationTypesElliptical() {
        doTestPropagationTypesElliptical(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestPropagationTypesElliptical(Field<T> field) {

        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getNormalizedProvider(5, 5));
        propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final FieldPVCoordinates<T> pv = initialState.getPVCoordinates();
        final T dP = zero.add(0.001);
        final T dV = pv.getPosition().getNormSq().multiply(pv.getVelocity().getNorm()).reciprocal().multiply(dP.multiply(initialState.getMu()));

        final FieldPVCoordinates<T> pvcM = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngle.MEAN, propagator);
        final FieldPVCoordinates<T> pviM = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngle.MEAN, propagator);
        final FieldPVCoordinates<T> pveM = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngle.MEAN, propagator);
        final FieldPVCoordinates<T> pvkM = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngle.MEAN, propagator);

        final FieldPVCoordinates<T> pvcE = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngle.ECCENTRIC, propagator);
        final FieldPVCoordinates<T> pviE = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngle.ECCENTRIC, propagator);
        final FieldPVCoordinates<T> pveE = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, propagator);
        final FieldPVCoordinates<T> pvkE = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngle.ECCENTRIC, propagator);

        final FieldPVCoordinates<T> pvcT = propagateInType(initialState, dP, OrbitType.CARTESIAN,   PositionAngle.TRUE, propagator);
        final FieldPVCoordinates<T> pviT = propagateInType(initialState, dP, OrbitType.CIRCULAR,    PositionAngle.TRUE, propagator);
        final FieldPVCoordinates<T> pveT = propagateInType(initialState, dP, OrbitType.EQUINOCTIAL, PositionAngle.TRUE, propagator);
        final FieldPVCoordinates<T> pvkT = propagateInType(initialState, dP, OrbitType.KEPLERIAN,   PositionAngle.TRUE, propagator);
        Assert.assertEquals(0, pvcM.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 3.0);
        Assert.assertEquals(0, pvcM.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 2.0);
        Assert.assertEquals(0, pviM.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.6);
        Assert.assertEquals(0, pviM.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.4);
        Assert.assertEquals(0, pvkM.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.5);
        Assert.assertEquals(0, pvkM.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.3);
        Assert.assertEquals(0, pveM.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.2);
        Assert.assertEquals(0, pveM.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.2);

        Assert.assertEquals(0, pvcE.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 3.0);
        Assert.assertEquals(0, pvcE.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 2.0);

        Assert.assertEquals(0, pviE.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.03);
        Assert.assertEquals(0, pviE.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.04);
        Assert.assertEquals(0, pvkE.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.4);
        Assert.assertEquals(0, pvkE.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.3);
       Assert.assertEquals(0, pveE.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.2);
        Assert.assertEquals(0, pveE.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.07);

        Assert.assertEquals(0, pvcT.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 3.0);
        Assert.assertEquals(0, pvcT.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 2.0);
        Assert.assertEquals(0, pviT.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.3);
        Assert.assertEquals(0, pviT.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.2);
        Assert.assertEquals(0, pvkT.getPosition().subtract(pveT.getPosition()).getNorm().getReal() / dP.getReal(), 0.4);
        Assert.assertEquals(0, pvkT.getVelocity().subtract(pveT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.2);

    }

    @Test
    public void testPropagationTypesHyperbolic() {
        doTestPropagationTypesHyperbolic(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestPropagationTypesHyperbolic(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        FieldSpacecraftState<T> state =
            new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(zero.add(-10000000.0), zero.add(2.5), zero.add(0.3), zero, zero, zero,
                                                                 PositionAngle.TRUE,
                                                                 FramesFactory.getEME2000(), initDate,
                                                                 zero.add(mu)));

        ForceModel gravityField =
            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                  GravityFieldFactory.getNormalizedProvider(5, 5));
        propagator.addForceModel(gravityField);

        // Propagation of the initial at t + dt
        final FieldPVCoordinates<T> pv = state.getPVCoordinates();
        final T dP = zero.add(0.001);
        final T dV = dP.multiply(state.getMu()).divide(
                          pv.getPosition().getNormSq().multiply(pv.getVelocity().getNorm()));

        final FieldPVCoordinates<T> pvcM = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngle.MEAN, propagator);
        final FieldPVCoordinates<T> pvkM = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngle.MEAN, propagator);

        final FieldPVCoordinates<T> pvcE = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, propagator);
        final FieldPVCoordinates<T> pvkE = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, propagator);

        final FieldPVCoordinates<T> pvcT = propagateInType(state, dP, OrbitType.CARTESIAN, PositionAngle.TRUE, propagator);
        final FieldPVCoordinates<T> pvkT = propagateInType(state, dP, OrbitType.KEPLERIAN, PositionAngle.TRUE, propagator);

        Assert.assertEquals(0, pvcM.getPosition().subtract(pvkT.getPosition()).getNorm().getReal() / dP.getReal(), 0.3);
        Assert.assertEquals(0, pvcM.getVelocity().subtract(pvkT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.4);
        Assert.assertEquals(0, pvkM.getPosition().subtract(pvkT.getPosition()).getNorm().getReal() / dP.getReal(), 0.2);
        Assert.assertEquals(0, pvkM.getVelocity().subtract(pvkT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.3);
        Assert.assertEquals(0, pvcE.getPosition().subtract(pvkT.getPosition()).getNorm().getReal() / dP.getReal(), 0.3);
        Assert.assertEquals(0, pvcE.getVelocity().subtract(pvkT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.4);
        Assert.assertEquals(0, pvkE.getPosition().subtract(pvkT.getPosition()).getNorm().getReal() / dP.getReal(), 0.009);
        Assert.assertEquals(0, pvkE.getVelocity().subtract(pvkT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.006);
        Assert.assertEquals(0, pvcT.getPosition().subtract(pvkT.getPosition()).getNorm().getReal() / dP.getReal(), 0.3);
        Assert.assertEquals(0, pvcT.getVelocity().subtract(pvkT.getVelocity()).getNorm().getReal() / dV.getReal(), 0.4);

    }

    private <T extends RealFieldElement<T>> FieldPVCoordinates<T> propagateInType(FieldSpacecraftState<T> state, T dP,
                                          OrbitType type , PositionAngle angle, FieldNumericalPropagator<T> propagator)
        {
        T zero = dP.getField().getZero();
        final T dt = zero.add(3200);
        final double minStep = 0.001;
        final double maxStep = 1000;
        double[][] tol = NumericalPropagator.tolerances(dP.getReal(), state.getOrbit().toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                new DormandPrince853FieldIntegrator<>(zero.getField(), minStep, maxStep, tol[0], tol[1]);
        FieldNumericalPropagator<T> newPropagator = new FieldNumericalPropagator<>(zero.getField(), integrator);
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
        doTestException(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestException(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        propagator.setMasterMode(new FieldOrekitStepHandler<T>() {
            private int countDown = 3;
            private FieldAbsoluteDate<T> previousCall = null;
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator,
                                   boolean isLast) {
                if (previousCall != null) {
                    System.out.println(interpolator.getCurrentState().getDate().compareTo(previousCall) < 0);
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
        doTestStopEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestStopEvent(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        final FieldAbsoluteDate<T> stopDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.STOP);
        propagator.addEventDetector(new FieldDateDetector<>(stopDate).withHandler(checking));
        Assert.assertEquals(1, propagator.getEventsDetectors().size());
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate).getReal(), 1.0e-10);
        propagator.clearEventsDetectors();
        Assert.assertEquals(0, propagator.getEventsDetectors().size());

    }

    @Test
    public void testResetStateEvent() {
        doTestResetStateEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestResetStateEvent(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);
        final FieldAbsoluteDate<T> resetDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.RESET_STATE) {
            public FieldSpacecraftState<T> resetState(FieldDateDetector<T> detector, FieldSpacecraftState<T> oldState) {
                return new FieldSpacecraftState<>(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass().subtract(200.0));
            }
        };
        propagator.addEventDetector(new FieldDateDetector<>(resetDate).withHandler(checking));
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(initialState.getMass().getReal() - 200, finalState.getMass().getReal(), 1.0e-10);
    }

    @Test
    public void testResetDerivativesEvent() {
        doTestResetDerivativesEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestResetDerivativesEvent(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);
        final FieldAbsoluteDate<T> resetDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.RESET_DERIVATIVES);
        propagator.addEventDetector(new FieldDateDetector<>(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        Assert.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate).getReal(), 1.0e-10);
        propagator.setResetAtEnd(true);
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        Assert.assertEquals(dt, propagator.getInitialState().getDate().durationFrom(initDate).getReal(), 1.0e-10);
        checking.assertEvent(true);
        final double n = FastMath.sqrt(initialState.getMu().getReal() / initialState.getA().getReal()) / initialState.getA().getReal();
        Assert.assertEquals(initialState.getA().getReal(),    finalState.getA().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(),    finalState.getEquinoctialEx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(),    finalState.getEquinoctialEy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHx().getReal(),    finalState.getHx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHy().getReal(),    finalState.getHy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getLM().getReal() + n * dt, finalState.getLM().getReal(), 6.0e-10);
    }

    @Test
    public void testContinueEvent() {
        doTestContinueEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestContinueEvent(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);




        final FieldAbsoluteDate<T> resetDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.CONTINUE);
        propagator.addEventDetector(new FieldDateDetector<>(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        Assert.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate).getReal(), 1.0e-10);
        propagator.setResetAtEnd(false);
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        Assert.assertEquals(0.0, propagator.getInitialState().getDate().durationFrom(initDate).getReal(), 1.0e-10);
        checking.assertEvent(true);
        final double n = FastMath.sqrt(initialState.getMu().getReal() / initialState.getA().getReal()) / initialState.getA().getReal();
        Assert.assertEquals(initialState.getA().getReal(),    finalState.getA().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(),    finalState.getEquinoctialEx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(),    finalState.getEquinoctialEy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHx().getReal(),    finalState.getHx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHy().getReal(),    finalState.getHy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getLM().getReal() + n * dt, finalState.getLM().getReal(), 6.0e-10);
    }

    @Test
    public void testAdditionalStateEvent() {
        doTestAdditionalStateEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestAdditionalStateEvent(Field<T> field) {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), type);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(initialState);

        propagator.addAdditionalEquations(new FieldAdditionalEquations<T>() {

            public String getName() {
                return "linear";
            }

            public T[] computeDerivatives(FieldSpacecraftState<T> s, T[] pDot) {
                pDot[0] = zero.add(1.0);
                return MathArrays.buildArray(field, 7);
            }
        });
        try {
            propagator.addAdditionalEquations(new FieldAdditionalEquations<T>() {

                public String getName() {
                    return "linear";
                }

                public T[] computeDerivatives(FieldSpacecraftState<T> s, T[] pDot) {
                    pDot[0] = zero.add(1.0);
                    return MathArrays.buildArray(field, 7);
                }
            });
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
        try {
            propagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
               public String getName() {
                    return "linear";
                }

                public T[] getAdditionalState(FieldSpacecraftState<T> state) {
                    return null;
                }
            });
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE);
        }
        propagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
            public String getName() {
                return "constant";
            }

            public T[] getAdditionalState(FieldSpacecraftState<T> state) {
                T[] ret = MathArrays.buildArray(field, 1);
                ret[0] = zero.add(1.0);
                return ret;
            }
        });
        Assert.assertTrue(propagator.isAdditionalStateManaged("linear"));
        Assert.assertTrue(propagator.isAdditionalStateManaged("constant"));
        Assert.assertFalse(propagator.isAdditionalStateManaged("non-managed"));
        Assert.assertEquals(2, propagator.getManagedAdditionalStates().length);
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("linear", zero.add(1.5)));

        CheckingHandler<AdditionalStateLinearDetector<T>, T> checking =
                new CheckingHandler<AdditionalStateLinearDetector<T>, T>(Action.STOP);
        propagator.addEventDetector(new AdditionalStateLinearDetector<T>(zero.add(10.0), zero.add(1.0e-8)).withHandler(checking));

        final double dt = 3200;
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        Assert.assertEquals(3.0, finalState.getAdditionalState("linear")[0].getReal(), 1.0e-8);
        Assert.assertEquals(1.5, finalState.getDate().durationFrom(initDate).getReal(), 1.0e-8);

    }

    private static class AdditionalStateLinearDetector<T extends RealFieldElement<T>>
        extends FieldAbstractDetector<AdditionalStateLinearDetector<T>, T> {

        public AdditionalStateLinearDetector(T maxCheck, T threshold) {
            this(maxCheck, threshold, DEFAULT_MAX_ITER, new FieldStopOnEvent<AdditionalStateLinearDetector<T>, T>());
        }

        private AdditionalStateLinearDetector(T maxCheck, T threshold, int maxIter,
                                              FieldEventHandler<? super AdditionalStateLinearDetector<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
        }

        protected AdditionalStateLinearDetector<T> create(final T newMaxCheck, final T newThreshold,
                                                       final int newMaxIter,
                                                       final FieldEventHandler<? super AdditionalStateLinearDetector<T>, T> newHandler) {
            return new AdditionalStateLinearDetector<T>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        public T g(FieldSpacecraftState<T> s) {
            return s.getAdditionalState("linear")[0].subtract(3.0);
        }

    }


    @Test
    public void testResetAdditionalStateEvent() {
        doTestResetAdditionalStateEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestResetAdditionalStateEvent(final Field<T> field) {
        FieldNumericalPropagator<T> propagator = createPropagator(field);


        propagator.addAdditionalEquations(new FieldAdditionalEquations<T>() {

            public String getName() {
                return "linear";
            }

            public T[] computeDerivatives(FieldSpacecraftState<T> s, T[] pDot) {
                pDot[0] = field.getOne();
                return null;
            }
        });
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("linear",
                                                                                   field.getZero().add(1.5)));

        CheckingHandler<AdditionalStateLinearDetector<T>, T> checking =
            new CheckingHandler<AdditionalStateLinearDetector<T>, T>(Action.RESET_STATE) {
            public FieldSpacecraftState<T> resetState(AdditionalStateLinearDetector<T> detector, FieldSpacecraftState<T> oldState)
                {
                return oldState.addAdditionalState("linear", oldState.getAdditionalState("linear")[0].multiply(2));
            }
        };

        propagator.addEventDetector(new AdditionalStateLinearDetector<T>(field.getZero().add(10.0),
                                                                         field.getZero().add(1.0e-8)).withHandler(checking));

        final double dt = 3200;
        checking.assertEvent(false);
        final FieldAbsoluteDate<T> initDate = propagator.getInitialState().getDate();
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate.shiftedBy(dt));
       // checking.assertEvent(true);
        Assert.assertEquals(dt + 4.5, finalState.getAdditionalState("linear")[0].getReal(), 1.0e-8);
        Assert.assertEquals(dt, finalState.getDate().durationFrom(initDate).getReal(), 1.0e-8);

    }

    @Test
    public void testEventDetectionBug() {
        doTestEventDetectionBug(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestEventDetectionBug(final Field<T> field) {

        T zero = field.getZero();
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<>(field, 2005, 1, 1, 0, 0, 0.0, utc);
        T duration = zero.add(100000.0);
        FieldAbsoluteDate<T> endDate = new FieldAbsoluteDate<>(initialDate, duration);

        // Initialization of the frame EME2000
        Frame EME2000 = FramesFactory.getEME2000();


        // Initial orbit
        double a = 35786000. + 6378137.0;
        double e = 0.70;
        double rApogee = a*(1+e);
        double vApogee = FastMath.sqrt(mu*(1-e)/(a*(1+e)));
        FieldOrbit<T> geo = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(rApogee), zero, zero),
                                                                               new FieldVector3D<>(zero, zero.add(vApogee), zero)),
                                                       EME2000, initialDate, zero.add(mu));


        duration = geo.getKeplerianPeriod();
        endDate = new FieldAbsoluteDate<>(initialDate, duration);

        // Numerical Integration
        final double minStep  = 0.001;
        final double maxStep  = 1000;
        final double initStep = 60;
        final OrbitType type = OrbitType.EQUINOCTIAL;
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};

        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, minStep, maxStep, absTolerance, relTolerance);
        integrator.setInitialStepSize(zero.add(initStep));

        // Numerical propagator based on the integrator
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        T mass = field.getZero().add(1000.0);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(geo, mass);
        propagator.setInitialState(initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);


        // Set the events Detectors
        FieldApsideDetector<T> event1 = new FieldApsideDetector<>(geo);
        propagator.addEventDetector(event1);

        // Set the propagation mode
        propagator.setSlaveMode();

        // Propagate
        FieldSpacecraftState<T> finalState = propagator.propagate(endDate);

        // we should stop long before endDate
        Assert.assertTrue(endDate.durationFrom(finalState.getDate()).getReal() > 40000.0);
    }

    @Test
    public void testEphemerisGenerationIssue14() {
        doTestEphemerisGenerationIssue14(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestEphemerisGenerationIssue14(Field<T> field)
        {

        // Propagation of the initial at t + dt
        FieldNumericalPropagator<T> propagator = createPropagator(field);
        final double dt = 3200;
        final FieldAbsoluteDate<T> initDate = propagator.getInitialState().getDate();

        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setEphemerisMode();
        propagator.propagate(initDate.shiftedBy(dt));
        final FieldBoundedPropagator<T> ephemeris1 = propagator.getGeneratedEphemeris();
        Assert.assertEquals(initDate, ephemeris1.getMinDate());
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        propagator.getPVCoordinates(initDate.shiftedBy( 2 * dt), FramesFactory.getEME2000());
        propagator.getPVCoordinates(initDate.shiftedBy(-2 * dt), FramesFactory.getEME2000());

        // the new propagations should not have changed ephemeris1
        Assert.assertEquals(initDate, ephemeris1.getMinDate());
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

        final FieldBoundedPropagator<T> ephemeris2 = propagator.getGeneratedEphemeris();
        Assert.assertEquals(initDate.shiftedBy(-2 * dt), ephemeris2.getMinDate());
        Assert.assertEquals(initDate.shiftedBy( 2 * dt), ephemeris2.getMaxDate());

        // generating ephemeris2 should not have changed ephemeris1
        Assert.assertEquals(initDate, ephemeris1.getMinDate());
        Assert.assertEquals(initDate.shiftedBy(dt), ephemeris1.getMaxDate());

    }

    @Test
    public void testEphemerisAdditionalState() {
        doTestEphemerisAdditionalState(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestEphemerisAdditionalState(final Field<T> field)
        {

        // Propagation of the initial at t + dt
        final double dt = -3200;
        final double rate = 2.0;
        FieldNumericalPropagator<T> propagator = createPropagator(field);
        FieldAbsoluteDate<T> initDate = propagator.getInitialState().getDate();

        propagator.addAdditionalStateProvider(new FieldAdditionalStateProvider<T>() {
            public String getName() {
                return "squaredA";
            }
            public T[] getAdditionalState(FieldSpacecraftState<T> state) {
                T[] a = MathArrays.buildArray(field, 1);
                a[0] = state.getA().multiply(state.getA());
                return a;
            }
        });
        propagator.addAdditionalEquations(new FieldAdditionalEquations<T>() {
            public String getName() {
                return "extra";
            }
            public T[] computeDerivatives(FieldSpacecraftState<T> s, T[] pDot) {
                pDot[0] = field.getZero().add(rate);
                return null;
            }
        });
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("extra", field.getZero().add(1.5)));

        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setEphemerisMode();
        propagator.propagate(initDate.shiftedBy(dt));
        final FieldBoundedPropagator<T> ephemeris1 = propagator.getGeneratedEphemeris();
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
        FieldSpacecraftState<T> s = ephemeris1.propagate(initDate.shiftedBy(shift));
        Assert.assertEquals(2, s.getAdditionalStates().size());
        Assert.assertTrue(s.hasAdditionalState("squaredA"));
        Assert.assertTrue(s.hasAdditionalState("extra"));
        Assert.assertEquals(s.getA().multiply(s.getA()).getReal(), s.getAdditionalState("squaredA")[0].getReal(), 1.0e-10);
        Assert.assertEquals(1.5 + shift * rate, s.getAdditionalState("extra")[0].getReal(), 1.0e-10);

        try {
            ephemeris1.resetInitialState(s);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NON_RESETABLE_STATE, oe.getSpecifier());
        }

    }

    @Test
    public void testIssue157() {
        doTestIssue157(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestIssue157(final Field<T> field) {
        try {
            FieldOrbit<T> orbit = new FieldKeplerianOrbit<>(field.getZero().add(13378000),
                                                            field.getZero().add(0.05),
                                                            field.getZero().add(0),
                                                            field.getZero().add(0),
                                                            field.getZero().add(FastMath.PI),
                                                            field.getZero().add(0),
                                                            PositionAngle.MEAN,
                                                            FramesFactory.getTOD(false),
                                                            new FieldAbsoluteDate<>(field, 2003, 5, 6, TimeScalesFactory.getUTC()),
                                                            field.getZero().add(Constants.EIGEN5C_EARTH_MU));
            FieldNumericalPropagator.tolerances(field.getZero().add(1.0), orbit, OrbitType.KEPLERIAN);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, oe.getSpecifier());
        }
    }

    @Test
    public void testShiftKeplerianEllipticTrueWithoutDerivatives() {
        doTestShiftKeplerianEllipticTrueWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianEllipticTrueWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.KEPLERIAN, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftKeplerianEllipticTrueWithDerivatives() {
        doTestShiftKeplerianEllipticTrueWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianEllipticTrueWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftKeplerianEllipticEccentricWithoutDerivatives() {
        doTestShiftKeplerianEllipticEccentricWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianEllipticEccentricWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftKeplerianEllipticEcentricWithDerivatives() {
        doTestShiftKeplerianEllipticEcentricWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianEllipticEcentricWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftKeplerianEllipticMeanWithoutDerivatives() {
        doTestShiftKeplerianEllipticMeanWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianEllipticMeanWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.KEPLERIAN, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftKeplerianEllipticMeanWithDerivatives() {
        doTestShiftKeplerianEllipticMeanWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianEllipticMeanWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.KEPLERIAN, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftKeplerianHyperbolicTrueWithoutDerivatives() {
        doTestShiftKeplerianHyperbolicTrueWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianHyperbolicTrueWithoutDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.KEPLERIAN, PositionAngle.TRUE, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftKeplerianHyperbolicTrueWithDerivatives() {
        doTestShiftKeplerianHyperbolicTrueWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianHyperbolicTrueWithDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.KEPLERIAN, PositionAngle.TRUE, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftKeplerianHyperbolicEccentricWithoutDerivatives() {
        doTestShiftKeplerianHyperbolicEccentricWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianHyperbolicEccentricWithoutDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftKeplerianHyperbolicEcentricWithDerivatives() {
        doTestShiftKeplerianHyperbolicEcentricWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianHyperbolicEcentricWithDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftKeplerianHyperbolicMeanWithoutDerivatives() {
        doTestShiftKeplerianHyperbolicMeanWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianHyperbolicMeanWithoutDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.KEPLERIAN, PositionAngle.MEAN, false,
                    0.484, 1.94, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftKeplerianHyperbolicMeanWithDerivatives() {
        doTestShiftKeplerianHyperbolicMeanWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftKeplerianHyperbolicMeanWithDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.KEPLERIAN, PositionAngle.MEAN, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCartesianEllipticTrueWithoutDerivatives() {
        doTestShiftCartesianEllipticTrueWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianEllipticTrueWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CARTESIAN, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCartesianEllipticTrueWithDerivatives() {
        doTestShiftCartesianEllipticTrueWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianEllipticTrueWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CARTESIAN, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCartesianEllipticEccentricWithoutDerivatives() {
        doTestShiftCartesianEllipticEccentricWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianEllipticEccentricWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCartesianEllipticEcentricWithDerivatives() {
        doTestShiftCartesianEllipticEcentricWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianEllipticEcentricWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCartesianEllipticMeanWithoutDerivatives() {
        doTestShiftCartesianEllipticMeanWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianEllipticMeanWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CARTESIAN, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCartesianEllipticMeanWithDerivatives() {
        doTestShiftCartesianEllipticMeanWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianEllipticMeanWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CARTESIAN, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCartesianHyperbolicTrueWithoutDerivatives() {
        doTestShiftCartesianHyperbolicTrueWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianHyperbolicTrueWithoutDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.CARTESIAN, PositionAngle.TRUE, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftCartesianHyperbolicTrueWithDerivatives() {
        doTestShiftCartesianHyperbolicTrueWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianHyperbolicTrueWithDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.CARTESIAN, PositionAngle.TRUE, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCartesianHyperbolicEccentricWithoutDerivatives() {
        doTestShiftCartesianHyperbolicEccentricWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianHyperbolicEccentricWithoutDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftCartesianHyperbolicEcentricWithDerivatives() {
        doTestShiftCartesianHyperbolicEcentricWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianHyperbolicEcentricWithDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.CARTESIAN, PositionAngle.ECCENTRIC, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCartesianHyperbolicMeanWithoutDerivatives() {
        doTestShiftCartesianHyperbolicMeanWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianHyperbolicMeanWithoutDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.CARTESIAN, PositionAngle.MEAN, false,
                    0.48, 1.93, 12.1, 48.3, 108.5);
    }

    @Test
    public void testShiftCartesianHyperbolicMeanWithDerivatives() {
        doTestShiftCartesianHyperbolicMeanWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCartesianHyperbolicMeanWithDerivatives(final Field<T> field) {
        doTestShift(createHyperbolicOrbit(field), OrbitType.CARTESIAN, PositionAngle.MEAN, true,
                    1.38e-4, 1.10e-3, 1.72e-2, 1.37e-1, 4.62e-1);
    }

    @Test
    public void testShiftCircularTrueWithoutDerivatives() {
        doTestShiftCircularTrueWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCircularTrueWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CIRCULAR, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCircularTrueWithDerivatives() {
        doTestShiftCircularTrueWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCircularTrueWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CIRCULAR, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCircularEccentricWithoutDerivatives() {
        doTestShiftCircularEccentricWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCircularEccentricWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCircularEcentricWithDerivatives() {
        doTestShiftCircularEcentricWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCircularEcentricWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftCircularMeanWithoutDerivatives() {
        doTestShiftCircularMeanWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCircularMeanWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CIRCULAR, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftCircularMeanWithDerivatives() {
        doTestShiftCircularMeanWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftCircularMeanWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.CIRCULAR, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftEquinoctialTrueWithoutDerivatives() {
        doTestShiftEquinoctialTrueWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftEquinoctialTrueWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.EQUINOCTIAL, PositionAngle.TRUE, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftEquinoctialTrueWithDerivatives() {
        doTestShiftEquinoctialTrueWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftEquinoctialTrueWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.EQUINOCTIAL, PositionAngle.TRUE, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftEquinoctialEccentricWithoutDerivatives() {
        doTestShiftEquinoctialEccentricWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftEquinoctialEccentricWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftEquinoctialEcentricWithDerivatives() {
        doTtestShiftEquinoctialEcentricWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTtestShiftEquinoctialEcentricWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    @Test
    public void testShiftEquinoctialMeanWithoutDerivatives() {
        doTestShiftEquinoctialMeanWithoutDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftEquinoctialMeanWithoutDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.EQUINOCTIAL, PositionAngle.MEAN, false,
                    18.1, 72.0, 437.3, 1601.1, 3141.8);
    }

    @Test
    public void testShiftEquinoctialMeanWithDerivatives() {
        doTestShiftEquinoctialMeanWithDerivatives(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestShiftEquinoctialMeanWithDerivatives(final Field<T> field) {
        doTestShift(createEllipticOrbit(field), OrbitType.EQUINOCTIAL, PositionAngle.MEAN, true,
                    1.14, 9.1, 140.3, 1066.7, 3306.9);
    }

    private static <T extends RealFieldElement<T>> void doTestShift(final FieldCartesianOrbit<T> orbit, final OrbitType orbitType,
                                                                    final PositionAngle angleType, final boolean withDerivatives,
                                                                    final double error60s, final double error120s,
                                                                    final double error300s, final double error600s,
                                                                    final double error900s)
        {

        T zero = orbit.getDate().getField().getZero();

        Utils.setDataRoot("regular-data:atmosphere:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        final FieldNumericalPropagator<T> np = createPropagator(new FieldSpacecraftState<>(orbit), orbitType, angleType);

        // the reference date for shifts is set at 60s, so the propagator can provide derivatives if needed
        // (derivatives are not available in the initial orbit)
        final FieldAbsoluteDate<T> reference = orbit.getDate().shiftedBy(60.0);
        final ShiftChecker<T> checker   = new ShiftChecker<>(withDerivatives, orbitType, angleType,
                                                             error60s,
                                                             error120s, error300s,
                                                             error600s, error900s);
        @SuppressWarnings("unchecked")
        FieldTimeStamped<T>[] dates = (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 6);
        dates[0] = reference;
        dates[1] = reference.shiftedBy( 60.0);
        dates[2] = reference.shiftedBy(120.0);
        dates[3] = reference.shiftedBy(300.0);
        dates[4] = reference.shiftedBy(600.0);
        dates[5] = reference.shiftedBy(900.0);
        np.addEventDetector(new FieldDateDetector<T>(zero.add(30.0), zero.add(1.0e-9), (FieldTimeStamped<T>[]) dates).
                            withHandler(checker));
        np.propagate(reference.shiftedBy(1000.0));
    }

    private static class ShiftChecker<T extends RealFieldElement<T>> implements FieldEventHandler<FieldDateDetector<T>, T> {

        private final boolean           withDerivatives;
        private final OrbitType         orbitType;
        private final PositionAngle     angleType;
        private final double            error60s;
        private final double            error120s;
        private final double            error300s;
        private final double            error600s;
        private final double            error900s;
        private FieldSpacecraftState<T> referenceState;

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
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldDateDetector<T> detector,
                                    final boolean increasing)
            {
            if (referenceState == null) {
                // first event, we retrieve the reference state for later use
                if (withDerivatives) {
                    referenceState = s;
                } else {
                    // remove derivatives, to check accuracy of the shiftedBy method decreases without them
                    final T[] stateVector = MathArrays.buildArray(s.getDate().getField(), 6);
                    final FieldOrbit<T> o = s.getOrbit();
                    orbitType.mapOrbitToArray(o, angleType, stateVector, null);
                    final FieldOrbit<T> fixedOrbit = orbitType.mapArrayToOrbit(stateVector, null, angleType,
                                                                               o.getDate(), o.getMu(), o.getFrame());
                    referenceState = new FieldSpacecraftState<>(fixedOrbit, s.getAttitude(), s.getMass());
                }
            } else {
                // recurring event, we compare with the shifted reference state
                final T dt = s.getDate().durationFrom(referenceState.getDate());
                final FieldSpacecraftState<T> shifted = referenceState.shiftedBy(dt);
                final T error = FieldVector3D.distance(shifted.getPVCoordinates().getPosition(),
                                                       s.getPVCoordinates().getPosition());
                switch ((int) FastMath.rint(dt.getReal())) {
                    case 60 :
                        Assert.assertEquals(error60s,  error.getReal(), 0.01 * error60s);
                        break;
                    case 120 :
                        Assert.assertEquals(error120s, error.getReal(), 0.01 * error120s);
                        break;
                    case 300 :
                        Assert.assertEquals(error300s, error.getReal(), 0.01 * error300s);
                        break;
                    case 600 :
                        Assert.assertEquals(error600s, error.getReal(), 0.01 * error600s);
                        break;
                    case 900 :
                        Assert.assertEquals(error900s, error.getReal(), 0.01 * error900s);
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

    private static <T extends RealFieldElement<T>> FieldNumericalPropagator<T> createPropagator(FieldSpacecraftState<T> spacecraftState,
                                                                                                OrbitType orbitType,
                                                                                                PositionAngle angleType)
        {

        final Field<T> field                          = spacecraftState.getDate().getField();
        final T       zero                            = field.getZero();
        final double  minStep                         = 0.001;
        final double  maxStep                         = 120.0;
        final T       positionTolerance               = zero.add(0.1);
        final int     degree                          = 20;
        final int     order                           = 20;
        final double  spacecraftArea                  = 1.0;
        final double  spacecraftDragCoefficient       = 2.0;
        final double  spacecraftReflectionCoefficient = 2.0;

        // propagator main configuration
        final double[][] tol           = FieldNumericalPropagator.tolerances(positionTolerance, spacecraftState.getOrbit(), orbitType);
        final FieldODEIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, minStep, maxStep, tol[0], tol[1]);
        final FieldNumericalPropagator<T> np   = new FieldNumericalPropagator<>(field, integrator);
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

    private static <T extends RealFieldElement<T>> FieldCartesianOrbit<T> createEllipticOrbit(Field<T> field) {
        T zero = field.getZero();
        final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final FieldVector3D<T>     position     = new FieldVector3D<>(zero.add(6896874.444705),
                                                                      zero.add(1956581.072644),
                                                                      zero.add(-147476.245054));
        final FieldVector3D<T>     velocity     = new FieldVector3D<>(zero.add(166.816407662),
                                                                      zero.add(-1106.783301861),
                                                                      zero.add(-7372.745712770));
        final TimeStampedFieldPVCoordinates<T> pv = new TimeStampedFieldPVCoordinates<>(date, position, velocity, FieldVector3D.getZero(field));
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        return new FieldCartesianOrbit<>(pv, frame, zero.add(mu));
    }

    private static <T extends RealFieldElement<T>> FieldCartesianOrbit<T> createHyperbolicOrbit(Field<T> field) {
        T zero = field.getZero();
        final FieldAbsoluteDate<T> date         = new FieldAbsoluteDate<>(field, "2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final FieldVector3D<T>     position     = new FieldVector3D<>(zero.add(224267911.905821),
                                                                      zero.add(290251613.109399),
                                                                      zero.add(45534292.777492));
        final FieldVector3D<T>     velocity     = new FieldVector3D<>(zero.add(-1494.068165293),
                                                                      zero.add(1124.771027677),
                                                                      zero.add(526.915286134));
        final TimeStampedFieldPVCoordinates<T> pv = new TimeStampedFieldPVCoordinates<>(date, position, velocity, FieldVector3D.getZero(field));
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        return new FieldCartesianOrbit<>(pv, frame, zero.add(mu));
    }

    private class CheckingHandler<D extends FieldEventDetector<T>, T extends RealFieldElement<T>> implements FieldEventHandler<D, T> {

        private final Action actionOnEvent;
        private boolean gotHere;

        public CheckingHandler(final Action actionOnEvent) {
            this.actionOnEvent = actionOnEvent;
            this.gotHere       = false;
        }

        public void assertEvent(boolean expected) {
            Assert.assertEquals(expected, gotHere);
        }

        public Action eventOccurred(FieldSpacecraftState<T> s, D detector, boolean increasing) {
            gotHere = true;
            return actionOnEvent;
        }

    }

    private <T extends RealFieldElement<T>>  FieldNumericalPropagator<T> createPropagator(Field<T> field)
        {
        T zero = field.getZero();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6),
                                                              zero.add(1.0e6),
                                                              zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0),
                                                              zero.add(8000.0),
                                                              zero.add(1000.0));
        FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), initDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        OrbitType type = OrbitType.EQUINOCTIAL;
        double[][] tolerance = FieldNumericalPropagator.tolerances(zero.add(0.001), orbit, type);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);

        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);


        propagator.setInitialState(initialState);
        return propagator;
    }

    private <T extends RealFieldElement<T>> FieldTimeStamped<T>[] toArray(final FieldAbsoluteDate<T> date) {
        @SuppressWarnings("unchecked")
        final FieldTimeStamped<T>[] array = (FieldTimeStamped<T>[]) Array.newInstance(FieldTimeStamped.class, 1);
        array[0] = date;
        return array;
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
    }

}

