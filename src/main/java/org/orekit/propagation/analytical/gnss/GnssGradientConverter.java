/* Copyright 2022-2026 Luc Maisonobe
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
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.utils.ParameterDriver;

import java.util.List;

/** Converter for GNSS propagator.
 * @param <O> type of the orbital elements (non-field version)
 * @param <P> type of the orbital elements (field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
class GnssGradientConverter<O extends GNSSOrbitalElements<O>,
                            P extends FieldGnssOrbitalElements<Gradient, O, P>>
    extends AbstractAnalyticalGradientConverter {

    /** Fixed dimension of the state. */
    public static final int FREE_STATE_PARAMETERS = 6;

    /** Orbit propagator. */
    private final GNSSPropagator<O> propagator;

    /** Simple constructor.
     * @param propagator orbit propagator used to access initial orbit
     */
    GnssGradientConverter(final GNSSPropagator<O> propagator) {
        super(propagator, FREE_STATE_PARAMETERS);
        this.propagator = propagator;
    }

    /** {@inheritDoc} */
    @Override
    public FieldGnssPropagator<Gradient> getPropagator() {

        // prepare initial state with proper derivatives
        final FieldSpacecraftState<Gradient> state = getState(this);

        // build propagator handling gradient
        final FieldKeplerianOrbit<Gradient> orbit =
            (FieldKeplerianOrbit<Gradient>) OrbitType.KEPLERIAN.convertType(state.getOrbit());
        return new FieldGnssPropagator<>(state,
                                         propagator.getOrbitalElements().toField(orbit),
                                         propagator.getECEF(), propagator.getAttitudeProvider(),
                                         state.getMass());

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return propagator.getParametersDrivers();
    }

}
