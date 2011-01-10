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
package org.orekit.propagation.events;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
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
        OutOfOrderChecker detector = new OutOfOrderChecker(date.shiftedBy(5.25 * stepSize), stepSize);
        propagator.addEventDetector(detector);
        propagator.setMasterMode(stepSize, detector);
        propagator.propagate(date.shiftedBy(10 * stepSize));
        Assert.assertTrue(detector.outOfOrderCallDetected());

    }

    private static class OutOfOrderChecker extends DateDetector implements OrekitFixedStepHandler {

        private static final long serialVersionUID = 26319257020496654L;
        private AbsoluteDate triggerDate;
        private boolean outOfOrderCallDetected;
        private double stepSize;

        public OutOfOrderChecker(final AbsoluteDate target, final double stepSize) {
            super(target);
            triggerDate = null;
            outOfOrderCallDetected = false;
            this.stepSize = stepSize;
        }

        public int eventOccurred(SpacecraftState s, boolean increasing) {
            triggerDate = s.getDate();
            return CONTINUE;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            // step handling and event occurrences may be out of order up to one step
            if (triggerDate != null) {
                double dt = currentState.getDate().durationFrom(triggerDate);
                if (dt < 0) {
                    outOfOrderCallDetected = true;
                    Assert.assertTrue(FastMath.abs(dt) < stepSize);
                }
            }
        }

        public boolean outOfOrderCallDetected() {
            return outOfOrderCallDetected;
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        mu = Constants.EIGEN5C_EARTH_MU;
    }

}

