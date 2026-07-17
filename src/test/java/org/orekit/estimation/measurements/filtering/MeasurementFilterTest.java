/* Copyright 2022-2026 Romain Serra
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
package org.orekit.estimation.measurements.filtering;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Range;
import org.orekit.propagation.SpacecraftState;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MeasurementFilterTest {

    @Test
    void testFilter() {
        // GIVEN
        final MeasurementFilter<Range> measurementFilter = mock();
        final ObservedMeasurement<Range> observedMeasurement = mock();
        final SpacecraftState state = mock();
        doCallRealMethod().when(measurementFilter).filter(observedMeasurement, state);
        // WHEN
        measurementFilter.filter(observedMeasurement, state);
        // THEN
        verify(measurementFilter).filter(observedMeasurement, new SpacecraftState[] { state });
    }

    @Test
    void testFilterAll() {
        // GIVEN
        final MeasurementFilter<Range> measurementFilter = mock();
        final ObservedMeasurement<Range> observedMeasurement = mock();
        final SpacecraftState state = mock();
        final SpacecraftState[] states = new SpacecraftState[] { state };
        doCallRealMethod().when(measurementFilter).filterAll(List.of(observedMeasurement), states);
        // WHEN
        measurementFilter.filterAll(List.of(observedMeasurement), states);
        // THEN
        verify(measurementFilter).filter(observedMeasurement, states);
    }
}
