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
package org.orekit.propagation.events;

import java.lang.reflect.Array;
import java.util.Locale;
import java.util.function.Function;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;

public class FieldEventDetectorTest {

    private double mu;

    @Test
    public void testEventHandlerInit() {
        doTestEventHandlerInit(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestEventHandlerInit(Field<T> field) {

        final T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668),
                                                              zero.add(3492467.56),
                                                              zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.848),
                                                              zero.add(942.781),
                                                              zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                             FramesFactory.getEME2000(), date, zero.add(mu));
        // mutable boolean
        final boolean[] eventOccurred = new boolean[1];
        FieldEventHandler<T> handler = new FieldEventHandler<T>() {
            private boolean initCalled;
            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s,
                                        FieldEventDetector<T> detector,
                                        boolean increasing) {
                if (!initCalled) {
                    throw new RuntimeException("init() not called before eventOccurred()");
                }
                eventOccurred[0] = true;
                return Action.STOP;
            }

            @Override
            public void init(final FieldSpacecraftState<T> initialState,
                             final FieldAbsoluteDate<T> target,
                             final FieldEventDetector<T> detector) {
                initCalled = true;
            }
        };

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        T stepSize = zero.add(60.0);
        @SuppressWarnings("unchecked")
        final FieldDateDetector<T> detector = new FieldDateDetector<>(field, date.shiftedBy(stepSize.multiply(5.25))).withHandler(handler);
        propagator.addEventDetector(detector);
        propagator.propagate(date.shiftedBy(stepSize.multiply(10)));
        Assertions.assertTrue(eventOccurred[0]);

    }

    @Test
    public void testBasicScheduling() {
        doTestBasicScheduling(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestBasicScheduling(Field<T> field) {

        final T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668),
                                                              zero.add(3492467.56),
                                                              zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.848),
                                                              zero.add(942.781),
                                                              zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                             FramesFactory.getEME2000(), date, zero.add(mu));

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        T stepSize = zero.add(60.0);
        OutOfOrderChecker<T> checker = new OutOfOrderChecker<>(stepSize);
        @SuppressWarnings("unchecked")
        FieldDateDetector<T> detector = new FieldDateDetector<>(field, date.shiftedBy(stepSize.multiply(5.25))).withHandler(checker);
        propagator.addEventDetector(detector);
        propagator.setStepHandler(stepSize, checker);
        propagator.propagate(date.shiftedBy(stepSize.multiply(10)));
        Assertions.assertTrue(checker.outOfOrderCallDetected());

    }

    private static class OutOfOrderChecker<T extends CalculusFieldElement<T>>
        implements FieldEventHandler<T>, FieldOrekitFixedStepHandler<T> {

        private FieldAbsoluteDate<T> triggerDate;
        private boolean outOfOrderCallDetected;
        private T stepSize;

        public OutOfOrderChecker(final T stepSize) {
            triggerDate = null;
            outOfOrderCallDetected = false;
            this.stepSize = stepSize;
        }

        public Action eventOccurred(FieldSpacecraftState<T> s, FieldEventDetector<T> detector, boolean increasing) {
            triggerDate = s.getDate();
            return Action.CONTINUE;
        }

        public void handleStep(FieldSpacecraftState<T> currentState) {
            // step handling and event occurrences may be out of order up to one step
            // with variable steps, and two steps with fixed steps (due to the delay
            // induced by StepNormalizer)
            if (triggerDate != null) {
                double dt = currentState.getDate().durationFrom(triggerDate).getReal();
                if (dt < 0) {
                    outOfOrderCallDetected = true;
                    Assertions.assertTrue(FastMath.abs(dt) < (2 * stepSize.getReal()));
                }
            }
        }

        public boolean outOfOrderCallDetected() {
            return outOfOrderCallDetected;
        }

    }

    @Test
    public void testIssue108Numerical() {
        doTestIssue108Numerical(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue108Numerical(Field<T> field) {
        final T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668),
                                                              zero.add(3492467.56),
                                                              zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.848),
                                                              zero.add(942.781),
                                                              zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                             FramesFactory.getEME2000(), date, zero.add(mu));
        final T step = zero.add(60.0);
        final int    n    = 100;
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, new ClassicalRungeKuttaFieldIntegrator<>(field, step));
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.resetInitialState(new FieldSpacecraftState<>(orbit));
        GCallsCounter<T> counter = new GCallsCounter<>(s -> 100000.0, zero.add(1.0e-6), 20,
                                                       new FieldStopOnEvent<T>());
        propagator.addEventDetector(counter);
        propagator.propagate(date.shiftedBy(step.multiply(n)));
        Assertions.assertEquals(n + 1, counter.getCount());
    }

    @Test
    public void testIssue108Analytical() {
        doTestIssue108Analytical(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue108Analytical(Field<T> field) {
        final T zero = field.getZero();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668),
                                                              zero.add(3492467.56),
                                                              zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(505.848),
                                                              zero.add(942.781),
                                                              zero.add(7435.922));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCircularOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                        FramesFactory.getEME2000(), date, zero.add(mu));
        final T step = zero.add(60.0);
        final int    n    = 100;
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        GCallsCounter<T> counter = new GCallsCounter<>(s -> 100000.0, zero.add(1.0e-6), 20,
                                                       new FieldStopOnEvent<T>());
        propagator.addEventDetector(counter);
        propagator.setStepHandler(step, currentState -> {});
        propagator.propagate(date.shiftedBy(step.multiply(n)));
        // analytical propagator can take one big step, further reducing calls to g()
        Assertions.assertEquals(2, counter.getCount());
    }

    private static class GCallsCounter<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<GCallsCounter<T>, T> {

        private int count;

        public GCallsCounter(final FieldAdaptableInterval<T> maxCheck, final T threshold,
                             final int maxIter, final FieldEventHandler<T> handler) {
            super(maxCheck, threshold, maxIter, handler);
            count = 0;
        }

        protected GCallsCounter<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                          final int newMaxIter,
                                          final FieldEventHandler<T> newHandler) {
            return new GCallsCounter<>(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        public int getCount() {
            return count;
        }

        public T g(FieldSpacecraftState<T> s) {
            count++;
            return s.getMass().getField().getZero().add(1.0);
        }

    }

    @Test
    public void testNoisyGFunction() {
        doTestNoisyGFunction(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoisyGFunction(Field<T> field) {

        final T zero = field.getZero();

        // initial conditions
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> initialDate   = new FieldAbsoluteDate<>(field, 2011, 5, 11, utc);
        FieldAbsoluteDate<T> startDate     = new FieldAbsoluteDate<>(field, 2032, 10, 17, utc);
        @SuppressWarnings("unchecked")
        FieldAbsoluteDate<T>[] interruptDates =
                        ( FieldAbsoluteDate<T>[]) Array.newInstance(FieldAbsoluteDate.class, 1);
        interruptDates[0] = new FieldAbsoluteDate<>(field, 2032, 10, 18, utc);
        FieldAbsoluteDate<T> targetDate    = new FieldAbsoluteDate<>(field, 2211, 5, 11, utc);
        FieldKeplerianPropagator<T> k1 =
                new FieldKeplerianPropagator<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(4008462.4706055815),
                                                                                                                        zero.add(-3155502.5373837613),
                                                                                                                        zero.add(-5044275.9880020910)),
                                                                                                    new FieldVector3D<>(zero.add(-5012.9298276860990),
                                                                                                                        zero.add(1920.3567095973078),
                                                                                                                        zero.add(-5172.7403501801580))),
                                                                           eme2000, initialDate, zero.add(Constants.WGS84_EARTH_MU)));
        FieldKeplerianPropagator<T> k2 =
                new FieldKeplerianPropagator<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(4008912.4039522274),
                                                                                                                        zero.add(-3155453.3125615157),
                                                                                                                        zero.add(-5044297.6484738905)),
                                                                                                    new FieldVector3D<>(zero.add(-5012.5883854112530),
                                                                                                                        zero.add(1920.6332221785074),
                                                                                                                        zero.add(-5172.2177085540500))),
                                                             eme2000, initialDate, zero.add(Constants.WGS84_EARTH_MU)));
        k2.addEventDetector(new FieldCloseApproachDetector<>(s -> 2015.243454166727, zero.add(0.0001), 100,
                                                             new FieldContinueOnEvent<T>(),
                                                             k1));
        k2.addEventDetector(new FieldDateDetector<>(field, interruptDates).
                            withMaxCheck(s -> Constants.JULIAN_DAY).
                            withThreshold(field.getZero().newInstance(1.0e-6)));
        FieldSpacecraftState<T> s = k2.propagate(startDate, targetDate);
        Assertions.assertEquals(0.0, interruptDates[0].durationFrom(s.getDate()).getReal(), 1.1e-6);
    }

    private static class FieldCloseApproachDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldCloseApproachDetector<T>, T> {

        private final FieldPVCoordinatesProvider<T> provider;

        public FieldCloseApproachDetector(FieldAdaptableInterval<T> maxCheck, T threshold,
                                          final int maxIter, final FieldEventHandler<T> handler,
                                          FieldPVCoordinatesProvider<T> provider) {
            super(maxCheck, threshold, maxIter, handler);
            this.provider = provider;
        }

        public T g(final FieldSpacecraftState<T> s) {
            FieldPVCoordinates<T> pv1     = provider.getPVCoordinates(s.getDate(), s.getFrame());
            FieldPVCoordinates<T> pv2     = s.getPVCoordinates();
            FieldVector3D<T> deltaP       = pv1.getPosition().subtract(pv2.getPosition());
            FieldVector3D<T> deltaV       = pv1.getVelocity().subtract(pv2.getVelocity());
            T radialVelocity = FieldVector3D.dotProduct(deltaP.normalize(), deltaV);
            return radialVelocity;
        }

        protected FieldCloseApproachDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                                       final int newMaxIter,
                                                       final FieldEventHandler<T> newHandler) {
            return new FieldCloseApproachDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                                    provider);
        }

    }

    @Test
    public void testWrappedException() {
        doTestWrappedException(Binary64Field.getInstance());
    }

    @SuppressWarnings("unchecked")
    private <T extends CalculusFieldElement<T>> void doTestWrappedException(Field<T> field) {
        final T zero = field.getZero();
        final Throwable dummyCause = new RuntimeException();
        try {
            // initial conditions
            Frame eme2000 = FramesFactory.getEME2000();
            TimeScale utc = TimeScalesFactory.getUTC();
            final FieldAbsoluteDate<T> initialDate   = new FieldAbsoluteDate<>(field, 2011, 5, 11, utc);
            final FieldAbsoluteDate<T> exceptionDate = initialDate.shiftedBy(3600.0);
            FieldKeplerianPropagator<T> k =
                            new FieldKeplerianPropagator<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(4008462.4706055815),
                                                                                                                                    zero.add(-3155502.5373837613),
                                                                                                                                    zero.add(-5044275.9880020910)),
                                                                                                                new FieldVector3D<>(zero.add(-5012.9298276860990),
                                                                                                                                    zero.add(1920.3567095973078),
                                                                                                                                    zero.add(-5172.7403501801580))),
                                            eme2000, initialDate, zero.add(Constants.WGS84_EARTH_MU)));
            k.addEventDetector(new FieldDateDetector<T>(field, initialDate.shiftedBy(Constants.JULIAN_DAY)) {
                @Override
                public T g(final FieldSpacecraftState<T> s) {
                    final T dt = s.getDate().durationFrom(exceptionDate);
                    if (dt.abs().getReal() < 1.0) {
                        throw new OrekitException(dummyCause, LocalizedCoreFormats.SIMPLE_MESSAGE, "dummy");
                    }
                    return dt;
                }
            });
            k.propagate(initialDate.shiftedBy(Constants.JULIAN_YEAR));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertSame(OrekitException.class, oe.getClass());
            Assertions.assertSame(dummyCause, oe.getCause().getCause());
            String expected = "failed to find root between 2011-05-11T00:00:00.000Z " +
                    "(g=-3.6E3) and 2012-05-10T06:00:00.000Z (g=3.1554E7)\n" +
                    "Last iteration at 2011-05-11T01:00:00.000Z (g=-3.6E3)";
            MatcherAssert.assertThat(oe.getMessage(Locale.US),
                    CoreMatchers.containsString(expected));
        }
    }

    @Test
    public void testDefaultMethods() {
        doTestDefaultMethods(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestDefaultMethods(final Field<T> field) {
        FieldEventDetector<T> dummyDetector = new FieldEventDetector<T>() {

            @Override
            public T getThreshold() {
                return field.getZero().add(1.0e-10);
            }

            @Override
            public int getMaxIterationCount() {
                return 100;
            }

            @Override
            public FieldAdaptableInterval<T> getMaxCheckInterval() {
                return s -> 60;
            }

            @Override
            public T g(FieldSpacecraftState<T> s) {
                return s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH);
            }

            @Override
            public FieldEventHandler<T> getHandler() {
                return (state, detector, increasing) ->  Action.RESET_STATE;
            }
       };

       // by default, this method does nothing, so this should pass without exception
       dummyDetector.init(null, null);

       // by default, this method returns its argument
       FieldSpacecraftState<T> s = new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(field.getZero().add(7e6),
                                                                                        field.getZero().add(0.01),
                                                                                        field.getZero().add(0.3),
                                                                                        field.getZero().add(0),
                                                                                        field.getZero().add(0),
                                                                                        field.getZero().add(0), PositionAngleType.TRUE,
                                                                                        FramesFactory.getEME2000(),
                                                                                        FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                        field.getZero().add(Constants.EIGEN5C_EARTH_MU)));
       Assertions.assertSame(s, dummyDetector.getHandler().resetState(dummyDetector, s));

    }

    @Test
    public void testForwardAnalytical() {
        doTestScheduling(Binary64Field.getInstance(), 0.0, 1.0, 21, this::buildAnalytical);
    }

    @Test
    public void testBackwardAnalytical() {
        doTestScheduling(Binary64Field.getInstance(), 1.0, 0.0, 21, this::buildAnalytical);
    }

    @Test
    public void testForwardNumerical() {
        doTestScheduling(Binary64Field.getInstance(), 0.0, 1.0, 23, this::buildNumerical);
    }

    @Test
    public void testBackwardNumerical() {
        doTestScheduling(Binary64Field.getInstance(), 1.0, 0.0, 23, this::buildNumerical);
    }

    private <T extends CalculusFieldElement<T>> FieldPropagator<T> buildAnalytical(final FieldOrbit<T> orbit) {
        return  new FieldKeplerianPropagator<>(orbit);
    }

    private <T extends CalculusFieldElement<T>> FieldPropagator<T> buildNumerical(final FieldOrbit<T> orbit) {
        Field<T>            field      = orbit.getDate().getField();
        OrbitType           type       = OrbitType.CARTESIAN;
        double[][]          tol        = FieldNumericalPropagator.tolerances(field.getZero().newInstance(0.0001),
                                                                             orbit, type);
        FieldODEIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, 0.0001, 10.0, tol[0], tol[1]);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(type);
        propagator.setInitialState(new FieldSpacecraftState<>(orbit));
        return propagator;
    }

    private <T extends CalculusFieldElement<T>> void doTestScheduling(final Field<T> field,
                                                                      final double start, final double stop, final int expectedCalls,
                                                                      final Function<FieldOrbit<T>, FieldPropagator<T>> propagatorBuilder) {

        // initial conditions
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        final FieldAbsoluteDate<T> initialDate   = new FieldAbsoluteDate<>(field, 2011, 5, 11, utc);
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(field.getZero().newInstance(4008462.4706055815),
                                                                                                             field.getZero().newInstance(-3155502.5373837613),
                                                                                                             field.getZero().newInstance(-5044275.9880020910)),
                                                                                         new FieldVector3D<>(field.getZero().newInstance(-5012.9298276860990),
                                                                                                             field.getZero().newInstance(1920.3567095973078),
                                                                                                             field.getZero().newInstance(-5172.7403501801580))),
                                                 eme2000, initialDate, field.getZero().newInstance(Constants.WGS84_EARTH_MU));
        FieldPropagator<T> propagator = propagatorBuilder.apply(orbit.shiftedBy(start));

        // checker that will be used in both step handler and events handlers
        // to check they are called in consistent order
        final ScheduleChecker<T> checker = new ScheduleChecker<>(initialDate.shiftedBy(start),
                                                                 initialDate.shiftedBy(stop));
        propagator.setStepHandler((interpolator) -> {
            checker.callDate(interpolator.getCurrentState().getDate());
        });

        for (int i = 0; i < 10; ++i) {
            @SuppressWarnings("unchecked")
            FieldDateDetector<T> detector = new FieldDateDetector<>(field, initialDate.shiftedBy(0.0625 * (i + 1))).
                                            withHandler((state, d, increasing) -> {
                                                checker.callDate(state.getDate());
                                                return Action.CONTINUE;
                                            });
            propagator.addEventDetector(detector);
        }

        propagator.propagate(initialDate.shiftedBy(start), initialDate.shiftedBy(stop));

        Assertions.assertEquals(expectedCalls, checker.calls);

    }

    /** Checker for method calls scheduling. */
    private static class ScheduleChecker<T extends CalculusFieldElement<T>> {

        private final FieldAbsoluteDate<T> start;
        private final FieldAbsoluteDate<T> stop;
        private FieldAbsoluteDate<T>       last;
        private int                        calls;

        ScheduleChecker(final FieldAbsoluteDate<T> start, final FieldAbsoluteDate<T> stop) {
            this.start = start;
            this.stop  = stop;
            this.last  = null;
            this.calls = 0;
        }

        void callDate(final FieldAbsoluteDate<T> date) {
            if (last != null) {
                // check scheduling is always consistent with integration direction
                if (start.isBefore(stop)) {
                    // forward direction
                    Assertions.assertTrue(date.isAfterOrEqualTo(start));
                    Assertions.assertTrue(date.isBeforeOrEqualTo(stop));
                    Assertions.assertTrue(date.isAfterOrEqualTo(last));
               } else {
                    // backward direction
                   Assertions.assertTrue(date.isBeforeOrEqualTo(start));
                   Assertions.assertTrue(date.isAfterOrEqualTo(stop));
                   Assertions.assertTrue(date.isBeforeOrEqualTo(last));
                }
            }
            last = date;
            ++calls;
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu = Constants.EIGEN5C_EARTH_MU;
    }

}

