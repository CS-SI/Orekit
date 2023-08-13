/* Copyright 2002-2023 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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

import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.BistaticRange;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical bistatic range measurement with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Maxime Journot
 * @author Joris Olympio
 * @author Mark Rutten
 * @since 11.2
 */
public class BistaticRangeTroposphericDelayModifier extends BaseRangeTroposphericDelayModifier implements EstimationModifier<BistaticRange> {

    /** Constructor.
     *
     * @param model Tropospheric delay model appropriate for the current range measurement method.
     */
    public BistaticRangeTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        super(model);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<BistaticRange> estimated) {
        final BistaticRange measurement = estimated.getObservedMeasurement();
        final GroundStation emitter     = measurement.getEmitterStation();
        final GroundStation receiver    = measurement.getReceiverStation();

        BistaticModifierUtil.modify(estimated, emitter, receiver, this::rangeErrorTroposphericModel);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<BistaticRange> estimated) {
        final BistaticRange measurement = estimated.getObservedMeasurement();
        final GroundStation emitter     = measurement.getEmitterStation();
        final GroundStation receiver    = measurement.getReceiverStation();
        final SpacecraftState state     = estimated.getStates()[0];

        BistaticModifierUtil.modify(estimated, getTropoModel(),
                                    new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                    emitter, receiver,
                                    this::rangeErrorTroposphericModel,
                                    this::rangeErrorTroposphericModel);

    }

}
