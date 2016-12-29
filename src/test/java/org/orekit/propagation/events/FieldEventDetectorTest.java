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
package org.orekit.propagation.events;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCircularOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler.Action;
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
    public void testBasicScheduling() throws OrekitException {
        doTestBasicScheduling(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestBasicScheduling(Field<T> field) throws OrekitException {

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
                                                             FramesFactory.getEME2000(), date, mu);

        FieldPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        T stepSize = zero.add(60.0);
        OutOfOrderChecker<T> checker = new OutOfOrderChecker<>(stepSize);
        propagator.addEventDetector(new FieldDateDetector<>(date.shiftedBy(stepSize.multiply(5.25))).withHandler(checker));
        propagator.setMasterMode(stepSize, checker);
        propagator.propagate(date.shiftedBy(stepSize.multiply(10)));
        Assert.assertTrue(checker.outOfOrderCallDetected());

    }

    private static class OutOfOrderChecker<T extends RealFieldElement<T>>
        implements FieldEventHandler<FieldDateDetector<T>, T>, FieldOrekitFixedStepHandler<T> {

        private FieldAbsoluteDate<T> triggerDate;
        private boolean outOfOrderCallDetected;
        private T stepSize;

        public OutOfOrderChecker(final T stepSize) {
            triggerDate = null;
            outOfOrderCallDetected = false;
            this.stepSize = stepSize;
        }

        public Action eventOccurred(FieldSpacecraftState<T> s, FieldDateDetector<T> detector, boolean increasing) {
            triggerDate = s.getDate();
            return Action.CONTINUE;
        }

        public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast) {
            // step handling and event occurrences may be out of order up to one step
            // with variable steps, and two steps with fixed steps (due to the delay
            // induced by StepNormalizer)
            if (triggerDate != null) {
                double dt = currentState.getDate().durationFrom(triggerDate).getReal();
                if (dt < 0) {
                    outOfOrderCallDetected = true;
                    Assert.assertTrue(FastMath.abs(dt) < (2 * stepSize.getReal()));
                }
            }
        }

        public boolean outOfOrderCallDetected() {
            return outOfOrderCallDetected;
        }

    }

    @Test
    public void testIssue108Numerical() throws OrekitException {
        doTestIssue108Numerical(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestIssue108Numerical(Field<T> field) throws OrekitException {
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
                                                             FramesFactory.getEME2000(), date, mu);
        final T step = zero.add(60.0);
        final int    n    = 100;
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, new ClassicalRungeKuttaFieldIntegrator<T>(field, step));
        propagator.resetInitialState(new FieldSpacecraftState<>(orbit));
        GCallsCounter<T> counter = new GCallsCounter<>(zero.add(100000.0), zero.add(1.0e-6), 20,
                                                       new FieldStopOnEvent<GCallsCounter<T>, T>());
        propagator.addEventDetector(counter);
        propagator.propagate(date.shiftedBy(step.multiply(n)));
        Assert.assertEquals(n + 1, counter.getCount());
    }

    @Test
    public void testIssue108Analytical() throws OrekitException {
        doTestIssue108Analytical(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestIssue108Analytical(Field<T> field) throws OrekitException {
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
                        FramesFactory.getEME2000(), date, mu);
        final T step = zero.add(60.0);
        final int    n    = 100;
        FieldKeplerianPropagator<T> propagator = new FieldKeplerianPropagator<>(orbit);
        GCallsCounter<T> counter = new GCallsCounter<>(zero.add(100000.0), zero.add(1.0e-6), 20,
                                                       new FieldStopOnEvent<GCallsCounter<T>, T>());
        propagator.addEventDetector(counter);
        propagator.setMasterMode(step, new FieldOrekitFixedStepHandler<T>() {
            public void handleStep(FieldSpacecraftState<T> currentState, boolean isLast) {
            }
        });
        propagator.propagate(date.shiftedBy(step.multiply(n)));
        Assert.assertEquals(n + 1, counter.getCount());
    }

    private static class GCallsCounter<T extends RealFieldElement<T>> extends FieldAbstractDetector<GCallsCounter<T>, T> {

        private int count;

        public GCallsCounter(final T maxCheck, final T threshold,
                             final int maxIter, final FieldEventHandler<? super GCallsCounter<T>, T> handler) {
            super(maxCheck, threshold, maxIter, handler);
            count = 0;
        }

        protected GCallsCounter<T> create(final T newMaxCheck, final T newThreshold,
                                          final int newMaxIter,
                                          final FieldEventHandler<? super GCallsCounter<T>, T> newHandler) {
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
    public void testNoisyGFunction() throws OrekitException {
        doTestNoisyGFunction(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestNoisyGFunction(Field<T> field) throws OrekitException {

        final T zero = field.getZero();

        // initial conditions
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        FieldAbsoluteDate<T> initialDate   = new FieldAbsoluteDate<>(field, 2011, 5, 11, utc);
        FieldAbsoluteDate<T> startDate     = new FieldAbsoluteDate<>(field, 2032, 10, 17, utc);
        FieldAbsoluteDate<T> interruptDate = new FieldAbsoluteDate<>(field, 2032, 10, 18, utc);
        FieldAbsoluteDate<T> targetDate    = new FieldAbsoluteDate<>(field, 2211, 5, 11, utc);
        FieldKeplerianPropagator<T> k1 =
                new FieldKeplerianPropagator<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(4008462.4706055815),
                                                                                                                        zero.add(-3155502.5373837613),
                                                                                                                        zero.add(-5044275.9880020910)),
                                                                                                    new FieldVector3D<>(zero.add(-5012.9298276860990),
                                                                                                                        zero.add(1920.3567095973078),
                                                                                                                        zero.add(-5172.7403501801580))),
                                                                           eme2000, initialDate, Constants.WGS84_EARTH_MU));
        FieldKeplerianPropagator<T> k2 =
                new FieldKeplerianPropagator<>(new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(new FieldVector3D<>(zero.add(4008912.4039522274),
                                                                                                                        zero.add(-3155453.3125615157),
                                                                                                                        zero.add(-5044297.6484738905)),
                                                                                                    new FieldVector3D<>(zero.add(-5012.5883854112530),
                                                                                                                        zero.add(1920.6332221785074),
                                                                                                                        zero.add(-5172.2177085540500))),
                                                             eme2000, initialDate, Constants.WGS84_EARTH_MU));
        k2.addEventDetector(new FieldCloseApproachDetector<>(zero.add(2015.243454166727), zero.add(0.0001), 100,
                                                             new FieldContinueOnEvent<FieldCloseApproachDetector<T>, T>(),
                                                             k1));
        k2.addEventDetector(new FieldDateDetector<>(zero.add(Constants.JULIAN_DAY), zero.add(1.0e-6), interruptDate));
        FieldSpacecraftState<T> s = k2.propagate(startDate, targetDate);
        Assert.assertEquals(0.0, interruptDate.durationFrom(s.getDate()).getReal(), 1.1e-6);
    }

    private static class FieldCloseApproachDetector<T extends RealFieldElement<T>> extends FieldAbstractDetector<FieldCloseApproachDetector<T>, T> {

        private final FieldPVCoordinatesProvider<T> provider;

        public FieldCloseApproachDetector(T maxCheck, T threshold,
                                          final int maxIter, final FieldEventHandler<? super FieldCloseApproachDetector<T>, T> handler,
                                          FieldPVCoordinatesProvider<T> provider) {
            super(maxCheck, threshold, maxIter, handler);
            this.provider = provider;
        }

        public T g(final FieldSpacecraftState<T> s) throws OrekitException {
            FieldPVCoordinates<T> pv1     = provider.getPVCoordinates(s.getDate(), s.getFrame());
            FieldPVCoordinates<T> pv2     = s.getPVCoordinates();
            FieldVector3D<T> deltaP       = pv1.getPosition().subtract(pv2.getPosition());
            FieldVector3D<T> deltaV       = pv1.getVelocity().subtract(pv2.getVelocity());
            T radialVelocity = FieldVector3D.dotProduct(deltaP.normalize(), deltaV);
            return radialVelocity;
        }

        protected FieldCloseApproachDetector<T> create(final T newMaxCheck, final T newThreshold,
                                                       final int newMaxIter,
                                                       final FieldEventHandler<? super FieldCloseApproachDetector<T>, T> newHandler) {
            return new FieldCloseApproachDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                                    provider);
        }

    }

    @Test
    public void testWrappedException() throws OrekitException {
        doTestWrappedException(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestWrappedException(Field<T> field) throws OrekitException {
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
                                            eme2000, initialDate, Constants.WGS84_EARTH_MU));
            k.addEventDetector(new FieldDateDetector<T>(initialDate.shiftedBy(Constants.JULIAN_DAY)) {
                @Override
                public T g(final FieldSpacecraftState<T> s) throws OrekitException {
                    final T dt = s.getDate().durationFrom(exceptionDate);
                    if (dt.abs().getReal() < 1.0) {
                        throw new OrekitException(dummyCause, LocalizedCoreFormats.SIMPLE_MESSAGE, "dummy");
                    }
                    return dt;
                }
            });
            k.propagate(initialDate.shiftedBy(Constants.JULIAN_YEAR));
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertSame(dummyCause, oe.getCause());
        }
    }

    @Test
    public void testDefaultMethods() throws OrekitException {
        doTestDefaultMethods(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestDefaultMethods(final Field<T> field) throws OrekitException {
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
            public T getMaxCheckInterval() {
                return field.getZero().add(60);
            }

            @Override
            public T g(FieldSpacecraftState<T> s) {
                return s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH);
            }

            @Override
            public Action eventOccurred(FieldSpacecraftState<T> s, boolean increasing) {
                return Action.RESET_STATE;
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
                                                                                        field.getZero().add(0), PositionAngle.TRUE,
                                                                                        FramesFactory.getEME2000(),
                                                                                        FieldAbsoluteDate.getJ2000Epoch(field),
                                                                                        Constants.EIGEN5C_EARTH_MU));
       Assert.assertSame(s, dummyDetector.resetState(s));

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu = Constants.EIGEN5C_EARTH_MU;
    }

}

