/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class ParameterDrivenDateIntervalDetectorTest {

    private Propagator propagator;

    @Test
    public void testNoShift() {
        final AbsoluteDate t0    = propagator.getInitialState().getDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("no-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent<>());

        Assert.assertEquals(10.0, detector.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        Assert.assertEquals("no-shift_START", detector.getStartDriver().getName());
        Assert.assertEquals("no-shift_STOP", detector.getStopDriver().getName());


        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(3600.0));

        Assert.assertEquals(2, logger.getLoggedEvents().size());
        Assert.assertEquals(0.0, logger.getLoggedEvents().get(0).getDate().durationFrom(start), 1.0e-10);
        Assert.assertEquals(0.0, logger.getLoggedEvents().get(1).getDate().durationFrom(stop),  1.0e-10);
    }

    @Test
    public void testSmallShift() {
        final AbsoluteDate t0    = propagator.getInitialState().getDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("small-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent<>());

        Assert.assertEquals(10.0, detector.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        Assert.assertEquals("small-shift_START", detector.getStartDriver().getName());
        Assert.assertEquals("small-shift_STOP", detector.getStopDriver().getName());

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        final double startShift = 5.5;
        detector.getStartDriver().setValue(startShift);
        final double stopShift  = -0.5;
        detector.getStopDriver().setValue(stopShift);
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(3600.0));

        Assert.assertEquals(2, logger.getLoggedEvents().size());
        Assert.assertEquals(startShift, logger.getLoggedEvents().get(0).getDate().durationFrom(start), 1.0e-10);
        Assert.assertEquals(stopShift,  logger.getLoggedEvents().get(1).getDate().durationFrom(stop),  1.0e-10);
    }

    @Test
    public void testLargeShift() {
        final AbsoluteDate t0    = propagator.getInitialState().getDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("large-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent<>());

        Assert.assertEquals(10.0, detector.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        Assert.assertEquals("large-shift_START", detector.getStartDriver().getName());
        Assert.assertEquals("large-shift_STOP", detector.getStopDriver().getName());

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        final double startShift =  500.5;
        detector.getStartDriver().setValue(startShift);
        final double stopShift  = -500.5;
        detector.getStopDriver().setValue(stopShift);
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(3600.0));

        Assert.assertEquals(0, logger.getLoggedEvents().size());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(506.0, 943.0, 7450);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(position,  velocity),
                                               FramesFactory.getEME2000(), date,
                                               Constants.EIGEN5C_EARTH_MU);

        propagator =
            new EcksteinHechlerPropagator(orbit,
                                          Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                          Constants.EIGEN5C_EARTH_MU,
                                          Constants.EIGEN5C_EARTH_C20,
                                          Constants.EIGEN5C_EARTH_C30,
                                          Constants.EIGEN5C_EARTH_C40,
                                          Constants.EIGEN5C_EARTH_C50,
                                          Constants.EIGEN5C_EARTH_C60);
    }

}

