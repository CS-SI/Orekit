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

import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AngularRadioRefractionModifierTest {

    @Test
    void testModifyWithoutDerivatives() {
        // GIVEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final GroundStation station = mock();
        final AngularAzEl angularAzEl = new AngularAzEl(station, AbsoluteDate.ARBITRARY_EPOCH, new double[2], new double[2], new double[2], new ObservableSatellite(0));
        final EstimatedMeasurementBase<AngularAzEl> estimated = new EstimatedMeasurementBase<>(angularAzEl, 0, 0,
                new SpacecraftState[] {state}, new TimeStampedPVCoordinates[]{state.getPVCoordinates()});
        final double elevation = 1.;
        estimated.setEstimatedValue(2.0, elevation);
        final AtmosphericRefractionModel refractionModel = mock();
        final AngularRadioRefractionModifier modifier = new AngularRadioRefractionModifier(refractionModel);
        final double expectedRefraction = 0.1;
        when(refractionModel.getRefraction(any(Double.class))).thenReturn(expectedRefraction);
        // WHEN
        modifier.modifyWithoutDerivatives(estimated);
        // THEN
        assertEquals(expectedRefraction, estimated.getEstimatedValue()[1] - elevation, 1e-15);
    }
}
