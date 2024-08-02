/* Copyright 2002-2024 CS GROUP
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
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
import org.orekit.utils.ParameterDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class ParameterDrivenDateIntervalDetectorTest {

    private Propagator propagator;

    @Test
    void testNoShift() {
        final AbsoluteDate t0    = propagator.getInitialState().getDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("no-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent());

        assertEquals(10.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        assertEquals("no-shift_START", detector.getStartDriver().getName());
        assertEquals("no-shift_STOP", detector.getStopDriver().getName());


        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(3600.0));

        assertEquals(2, logger.getLoggedEvents().size());
        assertEquals(0.0, logger.getLoggedEvents().get(0).getDate().durationFrom(start), 1.0e-10);
        assertEquals(0.0, logger.getLoggedEvents().get(1).getDate().durationFrom(stop),  1.0e-10);
    }

    @Test
    void testSmallShift() {
        final AbsoluteDate t0    = propagator.getInitialState().getDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("small-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent());

        assertEquals(10.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        assertEquals("small-shift_START", detector.getStartDriver().getName());
        assertEquals("small-shift_STOP", detector.getStopDriver().getName());

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        final double startShift = 5.5;
        detector.getStartDriver().setValue(startShift);
        final double stopShift  = -0.5;
        detector.getStopDriver().setValue(stopShift);
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(3600.0));

        assertEquals(2, logger.getLoggedEvents().size());
        assertEquals(startShift, logger.getLoggedEvents().get(0).getDate().durationFrom(start), 1.0e-10);
        assertEquals(stopShift,  logger.getLoggedEvents().get(1).getDate().durationFrom(stop),  1.0e-10);
    }

    @Test
    void testLargeShift() {
        final AbsoluteDate t0       = propagator.getInitialState().getDate();
        final AbsoluteDate start    = t0.shiftedBy( 120);
        final AbsoluteDate stop     = t0.shiftedBy(1120);
        final double       duration = stop.durationFrom(start);
        final AbsoluteDate median   = start.shiftedBy(0.5 * duration);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("large-shift", median, duration).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent());

        assertEquals(10.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        assertEquals(1.0e-12, detector.getThreshold(), 1.0e-15);
        assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        assertEquals("large-shift_START", detector.getStartDriver().getName());
        assertEquals("large-shift_STOP", detector.getStopDriver().getName());

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(detector));

        final double startShift =  500.5;
        detector.getStartDriver().setValue(startShift);
        final double stopShift  = -500.5;
        detector.getStopDriver().setValue(stopShift);
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(3600.0));

        assertEquals(0, logger.getLoggedEvents().size());

    }

    @Test
    void testSelection() {
        final AbsoluteDate t0       = propagator.getInitialState().getDate();
        final AbsoluteDate start    = t0.shiftedBy( 120);
        final AbsoluteDate stop     = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("large-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent());

        assertFalse(detector.getStartDriver().isSelected());
        assertFalse(detector.getStopDriver().isSelected());
        assertFalse(detector.getMedianDriver().isSelected());
        assertFalse(detector.getDurationDriver().isSelected());

        // check all 16 possible configurations, changing one selection at a time
        checkSelection(detector, detector.getDurationDriver(), true,  false, false, false, true,  false);
        checkSelection(detector, detector.getMedianDriver(),   true,  false, false, true,  true,  false);
        checkSelection(detector, detector.getDurationDriver(), false, false, false, true,  false, false);
        checkSelection(detector, detector.getStopDriver(),     true,  false, true,  true,  false, true);
        checkSelection(detector, detector.getDurationDriver(), true,  false, true,  true,  true,  true);
        checkSelection(detector, detector.getMedianDriver(),   false, false, true,  false, true,  true);
        checkSelection(detector, detector.getDurationDriver(), false, false, true,  false, false, false);
        checkSelection(detector, detector.getStartDriver(),    true,  true,  true,  false, false, false);
        checkSelection(detector, detector.getDurationDriver(), true,  true,  true,  false, true,  true);
        checkSelection(detector, detector.getMedianDriver(),   true,  true,  true,  true,  true,  true);
        checkSelection(detector, detector.getDurationDriver(), false, true,  true,  true,  false, true);
        checkSelection(detector, detector.getStopDriver(),     false, true,  false, true,  false, true);
        checkSelection(detector, detector.getDurationDriver(), true,  true,  false, true,  true,  true);
        checkSelection(detector, detector.getMedianDriver(),   false, true,  false, false, true,  true);
        checkSelection(detector, detector.getDurationDriver(), false, true,  false, false, false, false);
        checkSelection(detector, detector.getStartDriver(),    false, false, false, false, false, false);

    }

    @Test
    void testStartStopToMedianDuration() {
        final AbsoluteDate t0       = propagator.getInitialState().getDate();
        final AbsoluteDate start    = t0.shiftedBy( 120);
        final AbsoluteDate stop     = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("large-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent());
        assertEquals(   0.0, detector.getStartDriver().getValue(),    1.0e-15);
        assertEquals(   0.0, detector.getStopDriver().getValue(),     1.0e-15);
        assertEquals(   0.0, detector.getMedianDriver().getValue(),   1.0e-15);
        assertEquals(1000.0, detector.getDurationDriver().getValue(), 1.0e-15);
        detector.getStartDriver().setSelected(true);
        detector.getStartDriver().setValue(1.0);
        assertEquals(   1.0, detector.getStartDriver().getValue(),    1.0e-15);
        assertEquals(   0.0, detector.getStopDriver().getValue(),     1.0e-15);
        assertEquals(   0.5, detector.getMedianDriver().getValue(),   1.0e-15);
        assertEquals( 999.0, detector.getDurationDriver().getValue(), 1.0e-15);
        detector.getStopDriver().setSelected(true);
        detector.getStopDriver().setValue(10.0);
        assertEquals(   1.0, detector.getStartDriver().getValue(),    1.0e-15);
        assertEquals(  10.0, detector.getStopDriver().getValue(),     1.0e-15);
        assertEquals(   5.5, detector.getMedianDriver().getValue(),   1.0e-15);
        assertEquals(1009.0, detector.getDurationDriver().getValue(), 1.0e-15);
    }

    @Test
    void testMedianDurationToStartStop() {
        final AbsoluteDate t0       = propagator.getInitialState().getDate();
        final AbsoluteDate start    = t0.shiftedBy( 120);
        final AbsoluteDate stop     = t0.shiftedBy(1120);
        ParameterDrivenDateIntervalDetector detector = new ParameterDrivenDateIntervalDetector("large-shift", start, stop).
                                                       withMaxCheck(10.0).
                                                       withThreshold(1.0e-12).
                                                       withHandler(new ContinueOnEvent());
        assertEquals(   0.0, detector.getStartDriver().getValue(),    1.0e-15);
        assertEquals(   0.0, detector.getStopDriver().getValue(),     1.0e-15);
        assertEquals(   0.0, detector.getMedianDriver().getValue(),   1.0e-15);
        assertEquals(1000.0, detector.getDurationDriver().getValue(), 1.0e-15);
        detector.getMedianDriver().setSelected(true);
        detector.getMedianDriver().setValue(1.0);
        assertEquals(   1.0, detector.getStartDriver().getValue(),    1.0e-15);
        assertEquals(   1.0, detector.getStopDriver().getValue(),     1.0e-15);
        assertEquals(   1.0, detector.getMedianDriver().getValue(),   1.0e-15);
        assertEquals(1000.0, detector.getDurationDriver().getValue(), 1.0e-15);
        detector.getDurationDriver().setSelected(true);
        detector.getDurationDriver().setValue(900.0);
        assertEquals(  51.0, detector.getStartDriver().getValue(),    1.0e-15);
        assertEquals( -49.0, detector.getStopDriver().getValue(),     1.0e-15);
        assertEquals(   1.0, detector.getMedianDriver().getValue(),   1.0e-15);
        assertEquals( 900.0, detector.getDurationDriver().getValue(), 1.0e-15);
    }

    private void checkSelection(final ParameterDrivenDateIntervalDetector detector,
                                final ParameterDriver driver, final boolean selection, final boolean expectedStart,
                                final boolean expectedStop, final boolean expectedMedian,
                                final boolean expectedDuration, final boolean shouldFail) {
        try {
            driver.setSelected(selection);
            if (shouldFail) {
                fail("an exception should have been thrown");
            }
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INCONSISTENT_SELECTION, oe.getSpecifier());
        }
        assertEquals(selection,        driver.isSelected());
        assertEquals(expectedStart,    detector.getStartDriver().isSelected());
        assertEquals(expectedStop,     detector.getStopDriver().isSelected());
        assertEquals(expectedMedian,   detector.getMedianDriver().isSelected());
        assertEquals(expectedDuration, detector.getDurationDriver().isSelected());
    }

    @BeforeEach
    void setUp() {
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

