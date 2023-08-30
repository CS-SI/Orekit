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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

public class FieldParameterDrivenDateIntervalDetectorTest {

    @Test
    public void testNoShift() {
        doTestNoShift(Binary64Field.getInstance());
    }

    @Test
    public void testSmallShift() {
        doTestSmallShift(Binary64Field.getInstance());
    }

    @Test
    public void testLargeShift() {
        doTestLargeShift(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestNoShift(Field<T> field) {
        final T zero = field.getZero();

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(506.0), zero.add(943.0), zero.add(7450));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                              FramesFactory.getEME2000(), date,
                                                              zero.add(Constants.EIGEN5C_EARTH_MU));
        FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<>(orbit,
                                                             Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                             zero.add(Constants.EIGEN5C_EARTH_MU),
                                                             Constants.EIGEN5C_EARTH_C20,
                                                             Constants.EIGEN5C_EARTH_C30,
                                                             Constants.EIGEN5C_EARTH_C40,
                                                             Constants.EIGEN5C_EARTH_C50,
                                                             Constants.EIGEN5C_EARTH_C60);

        final AbsoluteDate t0    = propagator.getInitialState().getDate().toAbsoluteDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        FieldParameterDrivenDateIntervalDetector<T> detector =
                        new FieldParameterDrivenDateIntervalDetector<>(field, "no-shift", start, stop).
                        withMaxCheck(10.0).
                        withThreshold(zero.newInstance(1.0e-12)).
                        withHandler(new FieldContinueOnEvent<>());

        Assertions.assertEquals(10.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-12, detector.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        Assertions.assertEquals("no-shift_START", detector.getStartDriver().getName());
        Assertions.assertEquals("no-shift_STOP", detector.getStopDriver().getName());

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(2, logger.getLoggedEvents().size());
        Assertions.assertEquals(0.0, logger.getLoggedEvents().get(0).getState().getDate().durationFrom(start).getReal(), 1.0e-10);
        Assertions.assertEquals(0.0, logger.getLoggedEvents().get(1).getState().getDate().durationFrom(stop).getReal(), 1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestSmallShift(Field<T> field) {
        final T zero = field.getZero();

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(506.0), zero.add(943.0), zero.add(7450));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                              FramesFactory.getEME2000(), date,
                                                              zero.add(Constants.EIGEN5C_EARTH_MU));
        FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<>(orbit,
                                                             Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                             zero.add(Constants.EIGEN5C_EARTH_MU),
                                                             Constants.EIGEN5C_EARTH_C20,
                                                             Constants.EIGEN5C_EARTH_C30,
                                                             Constants.EIGEN5C_EARTH_C40,
                                                             Constants.EIGEN5C_EARTH_C50,
                                                             Constants.EIGEN5C_EARTH_C60);

        final AbsoluteDate t0    = propagator.getInitialState().getDate().toAbsoluteDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        FieldParameterDrivenDateIntervalDetector<T> detector =
                        new FieldParameterDrivenDateIntervalDetector<>(field, "no-shift", start, stop).
                        withMaxCheck(10.0).
                        withThreshold(zero.newInstance(1.0e-12)).
                        withHandler(new FieldContinueOnEvent<>());

        Assertions.assertEquals(10.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-12, detector.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        Assertions.assertEquals("no-shift_START", detector.getStartDriver().getName());
        Assertions.assertEquals("no-shift_STOP", detector.getStopDriver().getName());

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));

        final double startShift = 5.5;
        detector.getStartDriver().setValue(startShift);
        final double stopShift  = -0.5;
        detector.getStopDriver().setValue(stopShift, null);
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(2, logger.getLoggedEvents().size());
        Assertions.assertEquals(startShift, logger.getLoggedEvents().get(0).getState().getDate().durationFrom(start).getReal(), 1.0e-10);
        Assertions.assertEquals(stopShift,  logger.getLoggedEvents().get(1).getState().getDate().durationFrom(stop).getReal(),  1.0e-10);

    }

    private <T extends CalculusFieldElement<T>> void doTestLargeShift(Field<T> field) {
        final T zero = field.getZero();

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(506.0), zero.add(943.0), zero.add(7450));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                              FramesFactory.getEME2000(), date,
                                                              zero.add(Constants.EIGEN5C_EARTH_MU));
        FieldEcksteinHechlerPropagator<T> propagator =
                        new FieldEcksteinHechlerPropagator<>(orbit,
                                                             Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                             zero.add(Constants.EIGEN5C_EARTH_MU),
                                                             Constants.EIGEN5C_EARTH_C20,
                                                             Constants.EIGEN5C_EARTH_C30,
                                                             Constants.EIGEN5C_EARTH_C40,
                                                             Constants.EIGEN5C_EARTH_C50,
                                                             Constants.EIGEN5C_EARTH_C60);

        final AbsoluteDate t0    = propagator.getInitialState().getDate().toAbsoluteDate();
        final AbsoluteDate start = t0.shiftedBy( 120);
        final AbsoluteDate stop  = t0.shiftedBy(1120);
        FieldParameterDrivenDateIntervalDetector<T> detector =
                        new FieldParameterDrivenDateIntervalDetector<>(field, "no-shift", start, stop).
                        withMaxCheck(10.0).
                        withThreshold(zero.newInstance(1.0e-12)).
                        withHandler(new FieldContinueOnEvent<>());

        Assertions.assertEquals(10.0, detector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-12, detector.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
        Assertions.assertEquals("no-shift_START", detector.getStartDriver().getName());
        Assertions.assertEquals("no-shift_STOP", detector.getStopDriver().getName());

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));

        final double startShift = 500.5;
        detector.getStartDriver().setValue(startShift);
        final double stopShift  = -500.5;
        detector.getStopDriver().setValue(stopShift);
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(0, logger.getLoggedEvents().size());

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

