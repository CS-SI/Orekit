/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractGradientConverter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Converter for states and parameters arrays.
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 * @since 10.2
 */
class DSSTGradientConverter extends AbstractGradientConverter {

    /** Fixed dimension of the state. */
    private static final int FREE_STATE_PARAMETERS = 6;

    /** Simple constructor.
     * @param state regular state
     * @param provider provider to use if attitude needs to be recomputed
     */
    DSSTGradientConverter(final SpacecraftState state, final AttitudeProvider provider) {

        super(FREE_STATE_PARAMETERS);

        // equinoctial parameters always has derivatives
        final Gradient sma  = Gradient.variable(FREE_STATE_PARAMETERS, 0, state.getA());
        final Gradient ex   = Gradient.variable(FREE_STATE_PARAMETERS, 1, state.getEquinoctialEx());
        final Gradient ey   = Gradient.variable(FREE_STATE_PARAMETERS, 2, state.getEquinoctialEy());
        final Gradient hx   = Gradient.variable(FREE_STATE_PARAMETERS, 3, state.getHx());
        final Gradient hy   = Gradient.variable(FREE_STATE_PARAMETERS, 4, state.getHy());
        final Gradient l    = Gradient.variable(FREE_STATE_PARAMETERS, 5, state.getLM());

        final Gradient gMu = Gradient.constant(FREE_STATE_PARAMETERS, state.getMu());

        // date
        final AbsoluteDate date = state.getDate();
        final FieldAbsoluteDate<Gradient> dateField = new FieldAbsoluteDate<>(sma.getField(), date);

        // mass never has derivatives
        final Gradient gM = Gradient.constant(FREE_STATE_PARAMETERS, state.getMass());

        final FieldOrbit<Gradient> gOrbit =
                        new FieldEquinoctialOrbit<>(sma, ex, ey, hx, hy, l,
                                                    PositionAngleType.MEAN,
                                                    state.getFrame(),
                                                    dateField,
                                                    gMu);

        final FieldAttitude<Gradient> gAttitude;
        // compute attitude partial derivatives
        gAttitude = provider.getAttitude(gOrbit, gOrbit.getDate(), gOrbit.getFrame());

        // initialize the list with the state having 0 force model parameters
        initStates(new FieldSpacecraftState<>(gOrbit, gAttitude, gM));

    }

}
