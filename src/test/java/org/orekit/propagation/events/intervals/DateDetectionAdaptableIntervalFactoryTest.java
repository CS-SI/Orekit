/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.events.intervals;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStamped;
import org.orekit.utils.PVCoordinates;

class DateDetectionAdaptableIntervalFactoryTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetSingleDateDetectionAdaptableInterval(final boolean isForward) {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory.getSingleDateDetectionAdaptableInterval();
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, isForward);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionConstantIntervalEmpty() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionConstantInterval();
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, true);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionIntervalEmpty() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionInterval();
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, false);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionIntervalOne() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionInterval(Mockito.mock(AbsoluteDate.class));
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, false);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionConstantInterval() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate otherDate = date.shiftedBy(3);
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionConstantInterval(date, otherDate);
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, false);
        Assertions.assertEquals(FastMath.abs(otherDate.durationFrom(date)) / 2, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionConstantIntervalSameDates() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionConstantInterval(date, date);
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, false);
        Assertions.assertNotEquals(0., actualMaxCheck);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetSingleDateDetectionFieldAdaptableInterval(final boolean isForward) {
        // GIVEN

        // WHEN
        final FieldAdaptableInterval<?> adaptableInterval = DateDetectionAdaptableIntervalFactory.getSingleDateDetectionFieldAdaptableInterval();
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(null, isForward);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionFieldConstantIntervalEmpty() {
        // GIVEN

        // WHEN
        final FieldAdaptableInterval<?> adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionFieldConstantInterval();
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(null, true);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetDatesDetectionFieldConstantIntervalOne() {
        // GIVEN

        // WHEN
        final FieldAdaptableInterval<?> adaptableInterval = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionFieldConstantInterval(Mockito.mock(FieldAbsoluteDate.class));
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(null, true);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionFieldConstantInterval() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldAbsoluteDate<Binary64> otherDate = date.shiftedBy(3);
        // WHEN
        final FieldAdaptableInterval<Binary64> adaptableInterval = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionFieldConstantInterval(date, otherDate);
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(null, false);
        Assertions.assertEquals(FastMath.abs(otherDate.durationFrom(date)).getReal() / 2, actualMaxCheck);
    }

    @Test
    void testGetDatesDetectionIntervalSameDates() {
        // GIVEN
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        final AdaptableInterval adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionInterval(date, date);
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(mockedState, false);
        Assertions.assertNotEquals(0., actualMaxCheck);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-100, 100, 1000})
    void testGetDatesDetectionInterval(final double initialStep) {
        // GIVEN
        final KeplerianPropagator propagator = getKeplerianPropagator();
        final DateDetector detector = new DateDetector();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final int size = 10;
        for (int i = 1; i < size + 1; i++) {
            detector.addEventDate(date.shiftedBy(initialStep * i));
        }
        final RecordAndContinue recordAndContinue = new RecordAndContinue();
        final AdaptableInterval interval = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionInterval(detector.getDates().stream().map(TimeStamped.class::cast).toArray(TimeStamped[]::new));
        // WHEN
        propagator.addEventDetector(detector.withMaxCheck(interval).withHandler(recordAndContinue));
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(initialStep * (size + 1)));
        // THEN
        Assertions.assertEquals(size, recordAndContinue.getEvents().size());
    }

    private static KeplerianPropagator getKeplerianPropagator() {
        return new KeplerianPropagator(getOrbit());
    }

    private static CartesianOrbit getOrbit() {
        return new CartesianOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, 1.);
    }

    @Test
    void testGetDatesDetectionFieldIntervalEmpty() {
        // GIVEN

        // WHEN
        final FieldAdaptableInterval<?> adaptableInterval = DateDetectionAdaptableIntervalFactory.getDatesDetectionFieldInterval();
        // THEN
        final double actualMaxCheck = adaptableInterval.currentInterval(null, true);
        Assertions.assertEquals(DateDetectionAdaptableIntervalFactory.DEFAULT_MAX_CHECK, actualMaxCheck);
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @ValueSource(doubles = {-100, 100, 1000})
    void testGetDatesDetectionFieldInterval(final double initialStep) {
        final FieldKeplerianPropagator<Binary64> propagator = getFieldKeplerianPropagator();
        final Binary64Field field = Binary64Field.getInstance();
        final FieldDateDetector<Binary64> fieldDateDetector = new FieldDateDetector<>(field);
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final int size = 10;
        for (int i = 1; i < size + 1; i++) {
            fieldDateDetector.addEventDate(date.shiftedBy(initialStep * i));
        }
        final FieldRecordAndContinue<Binary64> recordAndContinue = new FieldRecordAndContinue<>();
        final FieldAdaptableInterval<Binary64> interval = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionFieldInterval(fieldDateDetector.getDates().stream().map(FieldTimeStamped.class::cast).toArray(FieldTimeStamped[]::new));
        propagator.addEventDetector(fieldDateDetector.withMaxCheck(interval).withHandler(recordAndContinue));
        propagator.propagate(propagator.getInitialState().getDate().shiftedBy(initialStep * (size + 1)));
        Assertions.assertEquals(size, recordAndContinue.getEvents().size());
    }

    private static FieldKeplerianPropagator<Binary64> getFieldKeplerianPropagator() {
        return new FieldKeplerianPropagator<>(new FieldCartesianOrbit<>(Binary64Field.getInstance(), getOrbit()));
    }
}
