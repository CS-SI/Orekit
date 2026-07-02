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
package org.orekit.estimation.measurements;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstimationsProviderTest {

    @ParameterizedTest
    @EnumSource(EstimatedMeasurementBase.Status.class)
    @SuppressWarnings("unchecked")
    void testGetRejectionIndicesNone(final EstimatedMeasurementBase.Status status) {
        // GIVEN
        final EstimationsProvider provider = mock();
        when(provider.getRejectedIndices()).thenCallRealMethod();
        when(provider.getNumber()).thenReturn(1);
        final EstimatedMeasurement mockedMeasurement = mockMeasurement(status);
        when(provider.getEstimatedMeasurement(0)).thenReturn(mockedMeasurement);
        // WHEN
        final int[] indices = provider.getRejectedIndices();
        // THEN
        switch (status) {
            case REJECTED:
                assertEquals(0, indices[0]);
                break;
            case PROCESSED:
                assertEquals(0, indices.length);
                break;
        }
    }

    private EstimatedMeasurement<?> mockMeasurement(final EstimatedMeasurementBase.Status status) {
        final EstimatedMeasurement<?> measurement = mock(EstimatedMeasurement.class);
        when(measurement.getStatus()).thenReturn(status);
        return measurement;
    }
}
