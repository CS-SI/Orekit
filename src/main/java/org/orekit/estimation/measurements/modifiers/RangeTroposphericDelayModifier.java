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
package org.orekit.estimation.measurements.modifiers;

import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Range;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical range measurement with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Maxime Journot
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeTroposphericDelayModifier extends BaseRangeTroposphericDelayModifier implements EstimationModifier<Range> {

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range measurement method.
     */
    public RangeTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        super(model);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<Range> estimated) {

        final Range         measurement = estimated.getObservedMeasurement();
        final GroundStation station     = measurement.getStation();

        RangeModifierUtil.modifyWithoutDerivatives(estimated,  station, this::rangeErrorTroposphericModel);


    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<Range> estimated) {

        final Range           measurement = estimated.getObservedMeasurement();
        final GroundStation   station     = measurement.getStation();
        final SpacecraftState state       = estimated.getStates()[0];

        RangeModifierUtil.modify(estimated, getTropoModel(),
                                 new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                 station,
                                 this::rangeErrorTroposphericModel,
                                 this::rangeErrorTroposphericModel);


    }

}
