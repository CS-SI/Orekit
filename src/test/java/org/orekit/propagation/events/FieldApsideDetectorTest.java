/* Copyright 2002-2025 CS GROUP
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
import org.hipparchus.complex.Complex;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.events.FieldEventsLogger.FieldLoggedEvent;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.propagation.events.intervals.ApsideDetectionAdaptableIntervalFactory;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;

class FieldApsideDetectorTest {

    @Test
    void testSimple() {
        doTestSimple(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestSimple(Field<T> field) {

        final FieldPropagator<T> propagator = createPropagator(field);
        FieldEventDetector<T> detector = new FieldApsideDetector<>(propagator.getInitialState().getOrbit()).
                                         withMaxCheck(600.0).
                                         withThreshold(field.getZero().newInstance(1.0e-12)).
                                         withHandler(new FieldContinueOnEvent<T>());

        Assertions.assertEquals(600.0, detector.getMaxCheckInterval().currentInterval(null, true), 1.0e-15);
        Assertions.assertEquals(1.0e-12, detector.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));

        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));

        Assertions.assertEquals(30, logger.getLoggedEvents().size());
        for (FieldLoggedEvent<T> e : logger.getLoggedEvents()) {
            FieldKeplerianOrbit<T> o = (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(e.getState().getOrbit());
            double expected = e.isIncreasing() ? 0.0 : FastMath.PI;
            Assertions.assertEquals(expected, MathUtils.normalizeAngle(o.getMeanAnomaly().getReal(), expected), 4.0e-14);
        }

    }

    @Test
    void testFixedMaxCheck() {
        doTestMaxcheck(Binary64Field.getInstance(), FieldAdaptableInterval.of(20.), 4818);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testConstructor() {
        // GIVEN
        final double period = 10.;
        final Complex threshold = Complex.ONE;
        final FieldOrbit<Complex> mockedFieldOrbit = Mockito.mock(FieldOrbit.class);
        Mockito.when(mockedFieldOrbit.getKeplerianPeriod()).thenReturn(new Complex(period));
        // WHEN
        final FieldApsideDetector<Complex> fieldApsideDetector = new FieldApsideDetector<>(threshold, mockedFieldOrbit);
        // THEN
        final Orbit mockedOrbit = Mockito.mock(Orbit.class);
        Mockito.when(mockedOrbit.getKeplerianPeriod()).thenReturn(period);
        final ApsideDetector apsideDetector = new ApsideDetector(threshold.getReal(), mockedOrbit);
        Assertions.assertEquals(apsideDetector.getThreshold(), fieldApsideDetector.getThreshold().getReal());
    }

    @Test
    void testAnomalyAwareMaxCheck() {
        final AdaptableInterval adaptableInterval = ApsideDetectionAdaptableIntervalFactory
                .getApsideDetectionAdaptableInterval();
        doTestMaxcheck(Binary64Field.getInstance(), (state, isForward) -> adaptableInterval.currentInterval(state.toSpacecraftState(), true),
                706);
    }

    private <T extends CalculusFieldElement<T>> void doTestMaxcheck(final Field<T> field,
                                                                    final FieldAdaptableInterval<T> maxCheck,
                                                                    int expectedCalls) {
        final FieldPropagator<T> propagator = createPropagator(field);
        CountingApsideDetector<T> detector = new CountingApsideDetector<>(propagator, maxCheck);
        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(detector));
        propagator.propagate(propagator.getInitialState().getOrbit().getDate().shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(30, logger.getLoggedEvents().size());
        Assertions.assertEquals(expectedCalls, detector.count);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    private <T extends CalculusFieldElement<T>> FieldPropagator<T> createPropagator(final Field<T> field) {
        final T zero = field.getZero();

        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<T> position = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.56), zero.add(-25767.257));
        final FieldVector3D<T> velocity = new FieldVector3D<>(zero.add(506.0), zero.add(943.0), zero.add(7450));
        final FieldAbsoluteDate<T> date = new FieldAbsoluteDate<>(field, 2003, 9, 16, utc);
        final FieldOrbit<T> orbit = new FieldCartesianOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                              FramesFactory.getEME2000(), date,
                                                              zero.add(Constants.EIGEN5C_EARTH_MU));
        return new FieldEcksteinHechlerPropagator<>(orbit,
                                                    Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                    zero.add(Constants.EIGEN5C_EARTH_MU),
                                                    Constants.EIGEN5C_EARTH_C20,
                                                    Constants.EIGEN5C_EARTH_C30,
                                                    Constants.EIGEN5C_EARTH_C40,
                                                    Constants.EIGEN5C_EARTH_C50,
                                                    Constants.EIGEN5C_EARTH_C60);
    }

    private class CountingApsideDetector<T extends CalculusFieldElement<T>> implements FieldDetectorModifier<T> {

        private final FieldEventDetector<T> detector;
        private int count;
        
        public CountingApsideDetector(final FieldPropagator<T> propagator, final FieldAdaptableInterval<T> maxCheck) {
            this.detector = new FieldApsideDetector<>(propagator.getInitialState().getOrbit()).
                    withMaxCheck(maxCheck).
                    withThreshold(propagator.getInitialState().getDate().getField().getZero().newInstance(1.0e-12)).
                    withHandler(new FieldContinueOnEvent<>());
        }

        @Override
        public FieldEventDetector<T> getDetector() {
            return detector;
        }

        public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
            FieldDetectorModifier.super.init(s0, t);
            count = 0;
        }

        public T g(final FieldSpacecraftState<T> s) {
            ++count;
            return FieldDetectorModifier.super.g(s);
        }

    }

}

