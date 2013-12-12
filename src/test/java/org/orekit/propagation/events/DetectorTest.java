/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class DetectorTest {

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

        public SpacecraftState resetState(DateDetector detector, SpacecraftState oldState) {
            return oldState;
        }

        public void init(SpacecraftState s0, AbsoluteDate t) {
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
        GCallsCounter counter = new GCallsCounter(100000.0, 1.0e-6, 20, new StopOnEvent<DetectorTest.GCallsCounter>());
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
        GCallsCounter counter = new GCallsCounter(100000.0, 1.0e-6, 20, new StopOnEvent<DetectorTest.GCallsCounter>());
        propagator.addEventDetector(counter);
        propagator.setMasterMode(step, new OrekitFixedStepHandler() {
            public void init(SpacecraftState s0, AbsoluteDate t) {
            }
            public void handleStep(SpacecraftState currentState, boolean isLast) {
            }
        });
        propagator.propagate(date.shiftedBy(n * step));
        Assert.assertEquals(n + 1, counter.getCount());
    }

    private static class GCallsCounter extends AbstractReconfigurableDetector<GCallsCounter> {

        private static final long serialVersionUID = 1L;
        private int count;

        public GCallsCounter(final double maxCheck, final double threshold,
                             final int maxIter, final EventHandler<GCallsCounter> handler) {
            super(maxCheck, threshold, maxIter, handler);
            count = 0;
        }

        protected GCallsCounter create(final double newMaxCheck, final double newThreshold,
                                       final int newMaxIter, final EventHandler<GCallsCounter> newHandler) {
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

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu = Constants.EIGEN5C_EARTH_MU;
    }

}

