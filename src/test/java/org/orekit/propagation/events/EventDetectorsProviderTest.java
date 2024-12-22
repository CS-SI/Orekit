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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/**
 * Unit tests for {@link EventDetectorsProvider}.
 *
 * @since 12.0
 * @author Maxime Journot
 */
public class EventDetectorsProviderTest {

    /** Test default methods with different {@link ParameterDriver} list.
     * <p>
     * 1. Empty list<p>
     * 2. List without time-spanned drivers<p>
     * 3. List with time-spanned drivers
     */
    @Test
    public void testGetEventDetectors() {

        // Given
        // -----

        // Define drivers
        final List<ParameterDriver> drivers = new ArrayList<>();
        final EventDetectorsProvider provider = getProvider(drivers);

        // When: empty drivers list
        // ----

        List<EventDetector>                detectors      = provider.getEventDetectors().collect(Collectors.toList());
        List<FieldEventDetector<Binary64>> fieldDetectors = provider.getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());

        // Then
        Assertions.assertTrue(detectors.isEmpty());
        Assertions.assertTrue(fieldDetectors.isEmpty());


        // When: multiple drivers with no time-span
        // ----

        final ParameterDriver param1 = new ParameterDriver("param-1", 0., 1., -1., 1.);
        final ParameterDriver param2 = new ParameterDriver("param-2", 0., 1., -1., 1.);
        final ParameterDriver param3 = new ParameterDriver("param-3", 0., 1., -1., 1.);
        
        drivers.add(param1);
        drivers.add(param2);
        drivers.add(param3);
        
        final Binary64Field b64Field = Binary64Field.getInstance(); 

        detectors      = provider.getEventDetectors().collect(Collectors.toList());
        fieldDetectors = provider.getFieldEventDetectors(b64Field).collect(Collectors.toList());


        // Then: detectors' list is still empty
        Assertions.assertTrue(detectors.isEmpty());
        Assertions.assertTrue(fieldDetectors.isEmpty());
        
        // When: time-spanned drivers
        // ----

        // Add spans with different dates and steps between dates
        final AbsoluteDate t0 = AbsoluteDate.ARBITRARY_EPOCH;
        final double step2 = 3600.;
        final double step3 = 5000.;
        param2.addSpans(t0, t0.shiftedBy(Constants.JULIAN_DAY), step2);
        param3.addSpans(t0.shiftedBy(-2. * Constants.JULIAN_DAY), t0.shiftedBy(-Constants.JULIAN_DAY), step3);

        detectors      = provider.getEventDetectors().collect(Collectors.toList());
        fieldDetectors = provider.getFieldEventDetectors(Binary64Field.getInstance()).collect(Collectors.toList());

        // Then        
        
        // Detectors list not empty
        Assertions.assertFalse(detectors.isEmpty());
        Assertions.assertEquals(1, detectors.size());
        Assertions.assertInstanceOf(DateDetector.class, detectors.get(0));
        
        Assertions.assertFalse(fieldDetectors.isEmpty());
        Assertions.assertEquals(1, fieldDetectors.size());
        Assertions.assertInstanceOf(FieldDateDetector.class, fieldDetectors.get(0));

        // Check dates
        final int expectedDatesNb = 39;
        final DateDetector       dateDetector = (DateDetector) detectors.get(0);
        final List<TimeStamped>  dates        = dateDetector.getDates();
                
        Assertions.assertEquals(expectedDatesNb, dates.size());
        Assertions.assertEquals(0., dates.get(0).durationFrom(t0.shiftedBy(-2. * Constants.JULIAN_DAY + step3)), 0.);
        Assertions.assertEquals(0., dates.get(expectedDatesNb-1).durationFrom(t0.shiftedBy(Constants.JULIAN_DAY - step2)), 0.);

        // Field version
        final FieldDateDetector<Binary64> fieldDateDetector = (FieldDateDetector<Binary64>) fieldDetectors.get(0);
        List<FieldTimeStamped<Binary64>>  fieldDates        = fieldDateDetector.getDates();
        
        Assertions.assertEquals(expectedDatesNb, dates.size());
        Assertions.assertEquals(0.,
                                fieldDates.get(0).durationFrom(new FieldAbsoluteDate<>(b64Field,
                                                t0.shiftedBy(-2. * Constants.JULIAN_DAY + step3))).getReal(),
                                0.);
        Assertions.assertEquals(0.,
                                fieldDates.get(expectedDatesNb-1).durationFrom(new FieldAbsoluteDate<>(b64Field,
                                                t0.shiftedBy(Constants.JULIAN_DAY - step2))).getReal(),
                                0.);
    }

    @Test
    void testGetDateDetector() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final double shift = 10.;
        final AbsoluteDate nextDate = date.shiftedBy(shift);
        final EventDetectorsProvider provider = getProvider(new ArrayList<>());
        // WHEN
        final DateDetector detector = provider.getDateDetector(date, nextDate);
        // THEN
        Assertions.assertEquals(2, detector.getDates().size());
        Assertions.assertEquals(EventDetectorsProvider.DATATION_ACCURACY, detector.getThreshold());
        Assertions.assertInstanceOf(ResetDerivativesOnEvent.class, detector.getHandler());
        final AdaptableInterval interval = detector.getMaxCheckInterval();
        SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        for (int i = 0; i < 10; i++) {
            Mockito.when(mockedState.getDate()).thenReturn(date.shiftedBy(i));
            Assertions.assertTrue(interval.currentInterval(mockedState, true) >= shift / 2);
        }
    }

    @Test
    void testGetFieldDateDetector() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate nextDate = date.shiftedBy(1.);
        final EventDetectorsProvider provider = getProvider(new ArrayList<>());
        final Binary64Field field = Binary64Field.getInstance();
        // WHEN
        final FieldDateDetector<Binary64> detector = provider.getFieldDateDetector(field, date, nextDate);
        // THEN
        Assertions.assertEquals(2, detector.getDates().size());
        Assertions.assertEquals(EventDetectorsProvider.DATATION_ACCURACY, detector.getThreshold().getReal());
        Assertions.assertInstanceOf(FieldResetDerivativesOnEvent.class, detector.getHandler());
    }

    private static EventDetectorsProvider getProvider(final List<ParameterDriver> drivers) {
        return new EventDetectorsProvider() {

            @Override
            public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {

                return getFieldEventDetectors(field, drivers);
            }

            @Override
            public Stream<EventDetector> getEventDetectors() {
                return getEventDetectors(drivers);
            }
        };
    }
}
