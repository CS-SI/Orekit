/* Copyright 2002-2021 CS GROUP
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
package org.orekit.propagation.analytical;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Converter for Keplerian propagator.
 *
 * @author Nicolas Fialton
 */
public class KeplerianGradientConverter extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Keplerian propagator. */
    private final FieldKeplerianPropagator<Gradient> gPropagator;

    /**
     * Simple constructor.
     * <p>
     * This method uses the {@link DataContext#getDefault() default data
     * context}.
     * @param state
     * @param propagator
     */
    @DefaultDataContext
    KeplerianGradientConverter(final SpacecraftState state, final KeplerianPropagator propagator) {
        super(FREE_STATE_PARAMETERS);

        // Convert to keplerian orbit
        final KeplerianOrbit kep = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit());

        // keplerian elements always has derivatives
        final Gradient gSma     = Gradient.variable(FREE_STATE_PARAMETERS, 0, kep.getA());
        final Gradient gE       = Gradient.variable(FREE_STATE_PARAMETERS, 1, kep.getE());
        final Gradient gI       = Gradient.variable(FREE_STATE_PARAMETERS, 2, kep.getI());
        final Gradient gRaan    = Gradient.variable(FREE_STATE_PARAMETERS, 3, kep.getRightAscensionOfAscendingNode());
        final Gradient gPa      = Gradient.variable(FREE_STATE_PARAMETERS, 4, kep.getPerigeeArgument());
        final Gradient gAnomaly = Gradient.variable(FREE_STATE_PARAMETERS, 5, kep.getTrueAnomaly());

        // date
        final AbsoluteDate date = state.getDate();
        final FieldAbsoluteDate<Gradient> dateField = new FieldAbsoluteDate<>(gSma.getField(), date);

        // mu never has derivatives
        final Gradient gMu = Gradient.constant(FREE_STATE_PARAMETERS, state.getMu());

        // mass never has derivatives
        final Gradient gM = Gradient.constant(FREE_STATE_PARAMETERS, state.getMass());

        // orbit
        final FieldOrbit<Gradient> gOrbit = new FieldKeplerianOrbit<>(gSma, gE, gI, gRaan, gPa, gAnomaly,
                                                                      PositionAngle.TRUE,
                                                                      state.getFrame(),
                                                                      dateField,
                                                                      gMu);


        // Keplerian gradient propagator
        gPropagator = new FieldKeplerianPropagator<>(gOrbit, propagator.getAttitudeProvider(), gMu, gM);
    }

    /** {@inheritDoc} */
    @DefaultDataContext
    public FieldKeplerianPropagator<Gradient> getPropagator() {

        return gPropagator;
    }

}
