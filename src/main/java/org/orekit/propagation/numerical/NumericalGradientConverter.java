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
package org.orekit.propagation.numerical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.utils.FieldDataDictionary;

/** Converter for states and parameters arrays.
 * @author Luc Maisonobe
 * @since 10.2
 */
class NumericalGradientConverter extends AbstractGradientConverter {


    /** Constructor.
     * @param state regular state
     * @param freeStateParameters number of free parameters, either 3 (position) or 6 (position-velocity)
     * @param provider provider to use if attitude needs to be recomputed
     * @param keepAdditionalData flag to keep additional data
     * @since 13.1.8
     */
    NumericalGradientConverter(final SpacecraftState state, final int freeStateParameters,
                               final AttitudeProvider provider, final boolean keepAdditionalData) {

        super(freeStateParameters);

        // initialize the list with the state having 0 force model parameters
        final AttitudeProvider passedAttitudeProvider = freeStateParameters > 3 ? provider : null;
        final FieldSpacecraftState<Gradient> basicGradientState = buildBasicGradientSpacecraftState(state,
                freeStateParameters, passedAttitudeProvider);
        initStates(keepAdditionalData ? basicGradientState.withAdditionalData(new FieldDataDictionary<>(basicGradientState.getMass().getField(), state.getAdditionalDataValues().toMap())) : basicGradientState);
    }

    /** Simple constructor with default values.
     * @param state regular state
     * @param freeStateParameters number of free parameters, either 3 (position) or 6 (position-velocity)
     * @param provider provider to use if attitude needs to be recomputed
     */
    NumericalGradientConverter(final SpacecraftState state, final int freeStateParameters,
                               final AttitudeProvider provider) {
        this(state, freeStateParameters, provider, false);
    }

}
