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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class EventDetectorTest {

    private double mu;

    @Test
    public void testBasicScheduling() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position,  velocity),
                                              FramesFactory.getEME2000(), date, mu);

        Propagator propagator = new KeplerianPropagator(orbit);
        double stepSize = 60.0;
        OutOfOrderChecker checker = new OutOfOrderChecker(stepSize);
        propagator.addEventDetector(new DateDetector(date.shiftedBy(5.25 * stepSize)).withHandler(checker));
        propagator.setMasterMode(stepSize, checker);
        propagator.propagate(date.shiftedBy(10 * stepSize));
        Assert.assertTrue(checker.outOfOrderCallDetected());

    }

    private static class OutOfOrderChecker implements EventHandler<DateDetector>, OrekitFixedStepHandler {

        private AbsoluteDate triggerDate;
        private boolean outOfOrderCallDetected;
        private double stepSize;

        public OutOfOrderChecker(final double stepSize) {
            triggerDate = null;
            outOfOrderCallDetected = false;
            this.stepSize = stepSize;
        }

        public Action eventOccurred(SpacecraftState s, DateDetector detector, boolean increasing) {
            triggerDate = s.getDate();
            return Action.CONTINUE;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            // step handling and event occurrences may be out of order up to one step
            // with variable steps, and two steps with fixed steps (due to the delay
            // induced by StepNormalizer)
            if (triggerDate != null) {
                double dt = currentState.getDate().durationFrom(triggerDate);
                if (dt < 0) {
                    outOfOrderCallDetected = true;
                    Assert.assertTrue(FastMath.abs(dt) < (2 * stepSize));
                }
            }
        }

        public boolean outOfOrderCallDetected() {
            return outOfOrderCallDetected;
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target) {
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target, double step) {
        }

    }

    @Test
    public void testIssue108Numerical() throws OrekitException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position,  velocity),
                                              FramesFactory.getEME2000(), date, mu);
        final double step = 60.0;
        final int    n    = 100;
        NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(step));
        propagator.resetInitialState(new SpacecraftState(orbit));
        GCallsCounter counter = new GCallsCounter(100000.0, 1.0e-6, 20, new StopOnEvent<GCallsCounter>());
        propagator.addEventDetector(counter);
        propagator.propagate(date.shiftedBy(n * step));
        Assert.assertEquals(n + 1, counter.getCount());
    }

    @Test
    public void testIssue108Analytical() throws OrekitException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CircularOrbit(new PVCoordinates(position,  velocity),
                                              FramesFactory.getEME2000(), date, mu);
        final double step = 60.0;
        final int    n    = 100;
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        GCallsCounter counter = new GCallsCounter(100000.0, 1.0e-6, 20, new StopOnEvent<GCallsCounter>());
        propagator.addEventDetector(counter);
        propagator.setMasterMode(step, new OrekitFixedStepHandler() {
            public void handleStep(SpacecraftState currentState, boolean isLast) {
            }
        });
        propagator.propagate(date.shiftedBy(n * step));
        Assert.assertEquals(n + 1, counter.getCount());
    }

    private static class GCallsCounter extends AbstractDetector<GCallsCounter> {

        private static final long serialVersionUID = 1L;
        private int count;

        public GCallsCounter(final double maxCheck, final double threshold,
                             final int maxIter, final EventHandler<? super GCallsCounter> handler) {
            super(maxCheck, threshold, maxIter, handler);
            count = 0;
        }

        protected GCallsCounter create(final double newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler<? super GCallsCounter> newHandler) {
            return new GCallsCounter(newMaxCheck, newThreshold, newMaxIter, newHandler);
        }

        public int getCount() {
            return count;
        }

        public double g(SpacecraftState s) {
            count++;
            return 1.0;
        }

    }

    @Test
    public void testNoisyGFunction() throws OrekitException {

        // initial conditions
        Frame eme2000 = FramesFactory.getEME2000();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate   = new AbsoluteDate(2011, 5, 11, utc);
        AbsoluteDate startDate     = new AbsoluteDate(2032, 10, 17, utc);
        AbsoluteDate interruptDate = new AbsoluteDate(2032, 10, 18, utc);
        AbsoluteDate targetDate    = new AbsoluteDate(2211, 5, 11, utc);
        KeplerianPropagator k1 =
                new KeplerianPropagator(new EquinoctialOrbit(new PVCoordinates(new Vector3D(4008462.4706055815, -3155502.5373837613, -5044275.9880020910),
                                                                               new Vector3D(-5012.9298276860990, 1920.3567095973078, -5172.7403501801580)),
                                                             eme2000, initialDate, Constants.WGS84_EARTH_MU));
        KeplerianPropagator k2 =
                new KeplerianPropagator(new EquinoctialOrbit(new PVCoordinates(new Vector3D(4008912.4039522274, -3155453.3125615157, -5044297.6484738905),
                                                                               new Vector3D(-5012.5883854112530, 1920.6332221785074, -5172.2177085540500)),
                                                             eme2000, initialDate, Constants.WGS84_EARTH_MU));
        k2.addEventDetector(new CloseApproachDetector(2015.243454166727, 0.0001, 100,
                                                      new ContinueOnEvent<CloseApproachDetector>(),
                                                      k1));
        k2.addEventDetector(new DateDetector(Constants.JULIAN_DAY, 1.0e-6, interruptDate));
        SpacecraftState s = k2.propagate(startDate, targetDate);
        Assert.assertEquals(0.0, interruptDate.durationFrom(s.getDate()), 1.1e-6);
    }

    private static class CloseApproachDetector extends AbstractDetector<CloseApproachDetector> {

        private static final long serialVersionUID = 1L;
        private final PVCoordinatesProvider provider;

        public CloseApproachDetector(double maxCheck, double threshold,
                                     final int maxIter, final EventHandler<? super CloseApproachDetector> handler,
                                     PVCoordinatesProvider provider) {
            super(maxCheck, threshold, maxIter, handler);
            this.provider = provider;
        }

        public double g(final SpacecraftState s) throws OrekitException {
            PVCoordinates pv1     = provider.getPVCoordinates(s.getDate(), s.getFrame());
            PVCoordinates pv2     = s.getPVCoordinates();
            Vector3D deltaP       = pv1.getPosition().subtract(pv2.getPosition());
            Vector3D deltaV       = pv1.getVelocity().subtract(pv2.getVelocity());
            double radialVelocity = Vector3D.dotProduct(deltaP.normalize(), deltaV);
            return radialVelocity;
        }

        protected CloseApproachDetector create(final double newMaxCheck, final double newThreshold,
                                               final int newMaxIter,
                                               final EventHandler<? super CloseApproachDetector> newHandler) {
            return new CloseApproachDetector(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                             provider);
        }

    }

    @Test
    public void testWrappedException() throws OrekitException {
        final Throwable dummyCause = new RuntimeException();
        try {
            // initial conditions
            Frame eme2000 = FramesFactory.getEME2000();
            TimeScale utc = TimeScalesFactory.getUTC();
            final AbsoluteDate initialDate   = new AbsoluteDate(2011, 5, 11, utc);
            final AbsoluteDate exceptionDate = initialDate.shiftedBy(3600.0);
            KeplerianPropagator k =
                    new KeplerianPropagator(new EquinoctialOrbit(new PVCoordinates(new Vector3D(4008462.4706055815, -3155502.5373837613, -5044275.9880020910),
                                                                                   new Vector3D(-5012.9298276860990, 1920.3567095973078, -5172.7403501801580)),
                                                                 eme2000, initialDate, Constants.WGS84_EARTH_MU));
            k.addEventDetector(new DateDetector(initialDate.shiftedBy(Constants.JULIAN_DAY)) {
                private static final long serialVersionUID = 1L;
                @Override
                public double g(final SpacecraftState s) throws OrekitException {
                    final double dt = s.getDate().durationFrom(exceptionDate);
                    if (FastMath.abs(dt) < 1.0) {
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
        EventDetector dummyDetector = new EventDetector() {
            private static final long serialVersionUID = 1L;

            @Override
            public double getThreshold() {
                return 1.0e-10;
            }

            @Override
            public int getMaxIterationCount() {
                return 100;
            }

            @Override
            public double getMaxCheckInterval() {
                return 60;
            }

            @Override
            public double g(SpacecraftState s) {
                return s.getDate().durationFrom(AbsoluteDate.J2000_EPOCH);
            }

            @Override
            public Action eventOccurred(SpacecraftState s, boolean increasing) {
                return Action.RESET_STATE;
            }
       };

       // by default, this method does nothing, so this should pass without exception
       dummyDetector.init(null, null);

       // by default, this method returns its argument
       SpacecraftState s = new SpacecraftState(new KeplerianOrbit(7e6, 0.01, 0.3, 0, 0, 0,
                                                                  PositionAngle.TRUE, FramesFactory.getEME2000(),
                                                                  AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU));
       Assert.assertSame(s, dummyDetector.resetState(s));

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu = Constants.EIGEN5C_EARTH_MU;
    }

}

