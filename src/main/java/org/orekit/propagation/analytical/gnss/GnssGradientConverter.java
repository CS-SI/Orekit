/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.utils.ParameterDriver;

import java.util.List;

/** Converter for GNSS propagator.
 * @author Luc Maisonobe
 * @since 13.0
 */
class GnssGradientConverter extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Orbit propagator. */
    private final GNSSPropagator propagator;

    /** Simple constructor.
     * @param propagator orbit propagator used to access initial orbit
     */
    GnssGradientConverter(final GNSSPropagator propagator) {
        super(propagator, propagator.getOrbitalElements().getMu(), FREE_STATE_PARAMETERS);
        // Initialize fields
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldGnssPropagator<Gradient> getPropagator(final FieldSpacecraftState<Gradient> state,
                                                       final Gradient[] parameters) {

        // Zero
        final Gradient zero = state.getA().getField().getZero();

       // Return the "Field" propagator
        return new FieldGnssPropagator<>(propagator.getOrbitalElements(),
                                         propagator.getECI(), propagator.getECEF(),
                                         propagator.getAttitudeProvider(),
                                         zero.newInstance(propagator.getMass(state.getDate().toAbsoluteDate())));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return propagator.getOrbitalElements().getParametersDrivers();
    }

}
