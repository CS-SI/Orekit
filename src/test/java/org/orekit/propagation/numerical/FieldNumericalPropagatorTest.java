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
package org.orekit.propagation.numerical;

import org.hamcrest.MatcherAssert;
import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldAdditionalStateProvider;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldApsideDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler.Action;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.integration.FieldAbstractIntegratedPropagator;
import org.orekit.propagation.integration.FieldAdditionalEquations;
import org.orekit.propagation.sampling.FieldOrekitStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


public class FieldNumericalPropagatorTest {

    private double               mu;

    @Test(expected=OrekitException.class)
    public void testErr1() throws OrekitException{
        doTestNotInitialised1(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestNotInitialised1(Field<T> field) throws OrekitException {
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);

        final FieldAbstractIntegratedPropagator<T> notInitialised =
            new FieldNumericalPropagator<T>(field, new ClassicalRungeKuttaFieldIntegrator<T>(field, field.getZero().add(10.0)));
        notInitialised.propagate(initDate);
    }

    @Test(expected=OrekitException.class)
    public void testErr2() throws OrekitException{
        doTestNotInitialised2(Decimal64Field.getInstance());
    };

    private <T extends RealFieldElement<T>>  void doTestNotInitialised2(Field<T> field) throws OrekitException {
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldAbstractIntegratedPropagator<T> notInitialised =
            new FieldNumericalPropagator<T>(field, new ClassicalRungeKuttaFieldIntegrator<T>(field, field.getZero().add((10.0))));
        notInitialised.propagate(initDate, initDate.shiftedBy(3600));
    }

    @Test
    public void testEventAtEndOfEphemeris() throws OrekitException {
        doTestEventAtEndOfEphemeris(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>, D extends FieldEventDetector<T>> void doTestEventAtEndOfEphemeris(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        FieldAbsoluteDate<T>         initDate  = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

        // choose duration that will round up when expressed as a double
        FieldAbsoluteDate<T> end = initDate.shiftedBy(100)
                .shiftedBy(3 * FastMath.ulp(100.0) / 4);
        propagator.setEphemerisMode();
        propagator.propagate(end);
        FieldBoundedPropagator<T> ephemeris = propagator.getGeneratedEphemeris();
        CountingHandler<D, T> handler = new CountingHandler<D, T>();
        FieldDateDetector<T> detector = new FieldDateDetector<T>(zero.add(10), zero.add(1e-9), end).withHandler(handler);
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
    public void testEventAtBeginningOfEphemeris() throws OrekitException {
        doTestEventAtBeginningOfEphemeris(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>, D extends FieldEventDetector<T>> void doTestEventAtBeginningOfEphemeris(Field<T> field)
        throws OrekitException {

        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

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
        FieldDateDetector<T> detector = new FieldDateDetector<T>(zero.add(10), zero.add(1e-9), eventDate)
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
    public void testCloseEventDates() throws OrekitException {
        doTestCloseEventDates(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestCloseEventDates(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        // setup
        FieldAbsoluteDate<T>         initDate  = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

        // setup
        FieldDateDetector<T> d1 = new FieldDateDetector<T>(zero.add(10), zero.add(1), initDate.shiftedBy(15))
                .withHandler(new FieldContinueOnEvent<FieldDateDetector<T>, T>());
        FieldDateDetector<T> d2 = new FieldDateDetector<T>(zero.add(10), zero.add(1), initDate.shiftedBy(15.5))
                .withHandler(new FieldContinueOnEvent<FieldDateDetector<T>,T>());
        propagator.addEventDetector(d1);
        propagator.addEventDetector(d2);

        //action
        FieldAbsoluteDate<T> end = initDate.shiftedBy(30);
        FieldSpacecraftState<T> actual = propagator.propagate(end);

        //verify
        Assert.assertEquals(actual.getDate().durationFrom(end).getReal(), 0.0, 0.0);
    }

    @Test
    public void testEphemerisDates() throws OrekitException {
        doTestEphemerisDates(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestEphemerisDates(Field<T> field) throws OrekitException {

        T zero = field.getZero();


        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<T>(field, "2015-07-01", tai);
        FieldAbsoluteDate<T> startDate = new FieldAbsoluteDate<T>(field, "2015-07-03", tai).shiftedBy(-0.1);
        FieldAbsoluteDate<T> endDate = new FieldAbsoluteDate<T>(field, "2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(
                zero.add(600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS), zero, zero, zero, zero, zero,
                PositionAngle.TRUE, eci, initialDate, mu);
        double[][] tol = NumericalPropagator.tolerances(1, orbit.toOrbit(), OrbitType.CARTESIAN);
        FieldPropagator<T> prop = new FieldNumericalPropagator<T>(field,
                new DormandPrince853FieldIntegrator<T>(field, 0.1, 500, tol[0], tol[1]));
        prop.resetInitialState(new FieldSpacecraftState<T>(new FieldCartesianOrbit<T>(orbit)));

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
    public void testEphemerisDatesBackward() throws OrekitException {
        doTestEphemerisDatesBackward(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestEphemerisDatesBackward(Field<T> field) throws OrekitException {

        T zero = field.getZero();
          //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<T>(field,"2015-07-05", tai);
        FieldAbsoluteDate<T> startDate = new FieldAbsoluteDate<T>(field,"2015-07-03", tai).shiftedBy(-0.1);
        FieldAbsoluteDate<T> endDate = new FieldAbsoluteDate<T>(field,"2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        FieldKeplerianOrbit<T> orbit = new FieldKeplerianOrbit<T>(
                zero.add(600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS), zero, zero, zero, zero, zero,
                PositionAngle.TRUE, eci, initialDate, mu);
        double[][] tol = NumericalPropagator.tolerances(1, orbit.toOrbit(), OrbitType.CARTESIAN);
        FieldPropagator<T> prop = new FieldNumericalPropagator<T>(field,
                new DormandPrince853FieldIntegrator<T>(field, 0.1, 500, tol[0], tol[1]));
        prop.resetInitialState(new FieldSpacecraftState<T>(new FieldCartesianOrbit<T>(orbit)));

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
    public void testNoExtrapolation() throws OrekitException {
        doTestNoExtrapolation(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestNoExtrapolation(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
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
    public void testKepler() throws OrekitException {
        doTestKepler(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestKepler(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

        // Propagation of the initial at t + dt
        final double dt = 3200;
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(-60), initDate.shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA().getReal()) / initialState.getA().getReal();
        Assert.assertEquals(initialState.getA().getReal(),    finalState.getA().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(),    finalState.getEquinoctialEx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(),    finalState.getEquinoctialEy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHx().getReal(),    finalState.getHx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHy().getReal(),    finalState.getHy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getLM().getReal() + n * dt, finalState.getLM().getReal(), 2.0e-9);

    }

    @Test
    public void testCartesian() throws OrekitException {
        doTestCartesian(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestCartesian(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
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
    public void testPropagationTypesElliptical() throws OrekitException {
        doTestPropagationTypesElliptical(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestPropagationTypesElliptical(Field<T> field) throws OrekitException {

        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
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
    public void testPropagationTypesHyperbolic() throws OrekitException {
        doTestPropagationTypesHyperbolic(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestPropagationTypesHyperbolic(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

        FieldSpacecraftState<T> state =
            new FieldSpacecraftState<T>(new FieldKeplerianOrbit<T>(zero.add(-10000000.0), zero.add(2.5), zero.add(0.3), zero, zero, zero,
                                                   PositionAngle.TRUE,
                                                   FramesFactory.getEME2000(), initDate,
                                                   mu));

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
        throws OrekitException {
        T zero = dP.getField().getZero();
        final T dt = zero.add(3200);
        final double minStep = 0.001;
        final double maxStep = 1000;
        OrbitType type_OT;
        switch (type) {
            case CARTESIAN : type_OT = OrbitType.CARTESIAN;
                break;
            case KEPLERIAN : type_OT = OrbitType.KEPLERIAN;
                break;
            case CIRCULAR : type_OT = OrbitType.CIRCULAR;
                break;
            case EQUINOCTIAL: type_OT = OrbitType.EQUINOCTIAL;
                break;
                default: type_OT = null;
        }
        double[][] tol = NumericalPropagator.tolerances(dP.getReal(), state.getOrbit().toOrbit(), type_OT);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                new DormandPrince853FieldIntegrator<T>(zero.getField(), minStep, maxStep, tol[0], tol[1]);
        FieldNumericalPropagator<T> newPropagator = new FieldNumericalPropagator<T>(zero.getField(), integrator);
        newPropagator.setOrbitType(type);
        newPropagator.setPositionAngleType(angle);
        newPropagator.setInitialState(state);
        for (ForceModel force: propagator.getForceModels()) {
            newPropagator.addForceModel(force);
        }
        return newPropagator.propagate(state.getDate().shiftedBy(dt)).getPVCoordinates();

    }

    @Test(expected=OrekitException.class)
    public void testException() throws OrekitException {
        doTestException(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestException(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

        propagator.setMasterMode(new FieldOrekitStepHandler<T>() {
            private int countDown = 3;
            private FieldAbsoluteDate<T> previousCall = null;
            public void init(FieldSpacecraftState<T> s0, FieldAbsoluteDate<T> t) {
            }
            public void handleStep(FieldOrekitStepInterpolator<T> interpolator,
                                   boolean isLast) throws OrekitException {
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
    public void testStopEvent() throws OrekitException {
        doTestStopEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestStopEvent(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);

        final FieldAbsoluteDate<T> stopDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.STOP);
        propagator.addEventDetector(new FieldDateDetector<T>(stopDate).withHandler(checking));
        Assert.assertEquals(1, propagator.getEventsDetectors().size());
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate).getReal(), 1.0e-10);
        propagator.clearEventsDetectors();
        Assert.assertEquals(0, propagator.getEventsDetectors().size());

    }

    @Test
    public void testResetStateEvent() throws OrekitException {
        doTestResetStateEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestResetStateEvent(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        final FieldAbsoluteDate<T> resetDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.RESET_STATE) {
            public FieldSpacecraftState<T> resetState(FieldDateDetector<T> detector, FieldSpacecraftState<T> oldState) {
                return new FieldSpacecraftState<T>(oldState.getOrbit(), oldState.getAttitude(), oldState.getMass().subtract(200.0));
            }
        };
        propagator.addEventDetector(new FieldDateDetector<T>(resetDate).withHandler(checking));
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState = propagator.propagate(initDate.shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(initialState.getMass().getReal() - 200, finalState.getMass().getReal(), 1.0e-10);
    }

    @Test
    public void testResetDerivativesEvent() throws OrekitException {
        doTestResetDerivativesEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestResetDerivativesEvent(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        final FieldAbsoluteDate<T> resetDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>, T> checking = new CheckingHandler<FieldDateDetector<T>, T>(Action.RESET_DERIVATIVES);
        propagator.addEventDetector(new FieldDateDetector<T>(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA().getReal()) / initialState.getA().getReal();
        Assert.assertEquals(initialState.getA().getReal(),    finalState.getA().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(),    finalState.getEquinoctialEx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(),    finalState.getEquinoctialEy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHx().getReal(),    finalState.getHx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHy().getReal(),    finalState.getHy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getLM().getReal() + n * dt, finalState.getLM().getReal(), 6.0e-10);
    }

    @Test
    public void testContinueEvent() throws OrekitException {
        doTestContinueEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestContinueEvent(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);




        final FieldAbsoluteDate<T> resetDate = initDate.shiftedBy(1000);
        CheckingHandler<FieldDateDetector<T>,T> checking = new CheckingHandler<FieldDateDetector<T>,T>(Action.CONTINUE);
        propagator.addEventDetector(new FieldDateDetector<T>(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        final FieldSpacecraftState<T> finalState =
            propagator.propagate(initDate.shiftedBy(dt));
        checking.assertEvent(true);
        final double n = FastMath.sqrt(initialState.getMu() / initialState.getA().getReal()) / initialState.getA().getReal();
        Assert.assertEquals(initialState.getA().getReal(),    finalState.getA().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEx().getReal(),    finalState.getEquinoctialEx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getEquinoctialEy().getReal(),    finalState.getEquinoctialEy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHx().getReal(),    finalState.getHx().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getHy().getReal(),    finalState.getHy().getReal(),    1.0e-10);
        Assert.assertEquals(initialState.getLM().getReal() + n * dt, finalState.getLM().getReal(), 6.0e-10);
    }

    @Test
    public void testAdditionalStateEvent() throws OrekitException {
        doTestAdditionalStateEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestAdditionalStateEvent(Field<T> field) throws OrekitException {
        T zero = field.getZero();
        // setup
        final FieldAbsoluteDate<T>   initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        FieldSpacecraftState<T>      initialState;
        FieldNumericalPropagator<T>  propagator;
        final FieldVector3D<T> position = new FieldVector3D<T>(zero.add(7.0e6), zero.add(1.0e6), zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<T>(zero.add(-500.0), zero.add(8000.0), zero.add(1000.0));

        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new FieldSpacecraftState<T>(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit.toOrbit(), OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T>integrator =
                new DormandPrince853FieldIntegrator<T>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        propagator = new FieldNumericalPropagator<T>(field, integrator);
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
        propagator.setInitialState(propagator.getInitialState().addAdditionalState("linear",zero.add(1.5)));

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

        public T g(FieldSpacecraftState<T> s) throws OrekitException {
            return s.getAdditionalState("linear")[0].subtract(3.0);
        }

    }


    @Test
    public void testResetAdditionalStateEvent() throws OrekitException {
        doTestResetAdditionalStateEvent(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestResetAdditionalStateEvent(final Field<T> field) throws OrekitException {
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
                throws OrekitException {
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
    public void testEventDetectionBug() throws OrekitException {
        doTestEventDetectionBug(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestEventDetectionBug(final Field<T> field) throws OrekitException {

        T zero = field.getZero();
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> initialDate = new FieldAbsoluteDate<T>(field, 2005, 1, 1, 0, 0, 0.0, utc);
        T duration = zero.add(100000.0);
        FieldAbsoluteDate<T> endDate = new FieldAbsoluteDate<T>(initialDate,duration);

        // Initialization of the frame EME2000
        Frame EME2000 = FramesFactory.getEME2000();


        // Initial orbit
        double a = 35786000. + 6378137.0;
        double e = 0.70;
        double rApogee = a*(1+e);
        double vApogee = FastMath.sqrt(mu*(1-e)/(a*(1+e)));
        FieldOrbit<T> geo = new FieldCartesianOrbit<T>(new FieldPVCoordinates<T>(new FieldVector3D<T>(zero.add(rApogee), zero, zero),
                                                                                 new FieldVector3D<T>(zero, zero.add(vApogee), zero)),
                                                       EME2000, initialDate, mu);


        duration = geo.getKeplerianPeriod();
        endDate = new FieldAbsoluteDate<T>(initialDate, duration);

        // Numerical Integration
        final double minStep  = 0.001;
        final double maxStep  = 1000;
        final double initStep = 60;
        final double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001};
        final double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7};

        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, minStep, maxStep, absTolerance, relTolerance);
        integrator.setInitialStepSize(zero.add(initStep));

        // Numerical propagator based on the integrator
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        T mass = field.getZero().add(1000.0);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(geo, mass);
        propagator.setInitialState(initialState);
        propagator.setOrbitType(OrbitType.CARTESIAN);


        // Set the events Detectors
        FieldApsideDetector<T> event1 = new FieldApsideDetector<T>(geo);
        propagator.addEventDetector(event1);

        // Set the propagation mode
        propagator.setSlaveMode();

        // Propagate
        FieldSpacecraftState<T> finalState = propagator.propagate(endDate);

        // we should stop long before endDate
        Assert.assertTrue(endDate.durationFrom(finalState.getDate()).getReal() > 40000.0);
    }

    @Test
    public void testEphemerisGenerationIssue14() throws OrekitException {
        doTestEphemerisGenerationIssue14(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestEphemerisGenerationIssue14(Field<T> field)
        throws OrekitException {

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
    public void testEphemerisAdditionalState() throws OrekitException {
        doTestEphemerisAdditionalState(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>>  void doTestEphemerisAdditionalState(final Field<T> field)
        throws OrekitException {

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
    public void testIssue157() throws OrekitException {
        doTestIssue157(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestIssue157(final Field<T> field) throws OrekitException {
        try {
            FieldOrbit<T> orbit = new FieldKeplerianOrbit<T>(field.getZero().add(13378000),
                                                             field.getZero().add(0.05),
                                                             field.getZero().add(0),
                                                             field.getZero().add(0),
                                                             field.getZero().add(FastMath.PI),
                                                             field.getZero().add(0),
                                                             PositionAngle.MEAN,
                                                             FramesFactory.getTOD(false),
                                                             new FieldAbsoluteDate<T>(field, 2003, 5, 6, TimeScalesFactory.getUTC()),
                                                             Constants.EIGEN5C_EARTH_MU);
            FieldNumericalPropagator.tolerances(field.getZero().add(1.0), orbit, OrbitType.KEPLERIAN);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, oe.getSpecifier());
        }
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
        throws OrekitException {
        T zero = field.getZero();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(7.0e6),
                                                              zero.add(1.0e6),
                                                              zero.add(4.0e6));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(-500.0),
                                                              zero.add(8000.0),
                                                              zero.add(1000.0));
        FieldAbsoluteDate<T> initDate = FieldAbsoluteDate.getJ2000Epoch(field);
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[][] tolerance = FieldNumericalPropagator.tolerances(zero.add(0.001), orbit, OrbitType.EQUINOCTIAL);
        AdaptiveStepsizeFieldIntegrator<T> integrator =
                new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);

        integrator.setInitialStepSize(zero.add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);


        propagator.setInitialState(initialState);
        return propagator;
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
    }

}

