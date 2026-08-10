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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.generation.PositionBuilder;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeasurementNoiseTest {

    @Test
    void testModify() {
        // GIVEN
        final CorrelatedRandomVectorGenerator randomVectorGenerator = mock();
        final double[] noiseValue = new double[] {1., 2., 3.};
        when(randomVectorGenerator.nextVector()).thenReturn(noiseValue);
        final MeasurementNoise<Position> noise = new MeasurementNoise<>(randomVectorGenerator);
        final PositionBuilder builder = new PositionBuilder(1., 1., new ObservableSatellite(0));
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(date));
        // WHEN
        builder.init(date, date);
        builder.addModifier(noise);
        final double[] noisyPosition = builder.build(date, new SpacecraftState[] { state }).getEstimatedValue();
        // THEN
        final double[] position = state.getPosition().toArray();
        assertEquals(position[0] + noiseValue[0], noisyPosition[0]);
        assertEquals(position[1] + noiseValue[1], noisyPosition[1]);
        assertEquals(position[2] + noiseValue[2], noisyPosition[2]);
    }

    @Test
    void testGetEffectName() {
        assertEquals("noise", new MeasurementNoise<>(null).getEffectName());
    }
}
