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
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.OrbitParamsType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalGradientConverter;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.analytical.gnss.data.NonKeplerianDriversFactory;
import org.orekit.utils.ParameterDriver;

import java.util.List;

/** Converter for GNSS propagator.
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
class GnssGradientConverter<O extends GNSSOrbitalElements<O>>
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

    /** {@inheritDoc}
     * <p>
     * The free variables of the returned gradients are the six <em>Cartesian</em> coordinates
     * of the initial state, extended with one slot per selected non-Keplerian driver. The
     * state transition matrix built from this propagator is therefore a genuine dY/dY₀, with
     * the same representation for its rows and its columns, consistent with
     * {@link org.orekit.propagation.analytical.AbstractAnalyticalMatricesHarvester#getOrbitParamsType()}
     * returning {@link OrbitParamsType#CARTESIAN}. The change of representation towards the
     * propagator builder parameters is a separate quantity, provided by
     * {@link org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory#jacobianWrtParameters}.
     * </p>
     */
    @Override
    public FieldGnssPropagator<Gradient, O> getPropagator() {

        // Cartesian state whose coordinates are the free variables of the gradients
        final FieldSpacecraftState<Gradient> gState = getState(this);
        final int nbParams = gState.getMass().getFreeParameters();

        // Keplerian view of that very state; the conversion carries the derivatives along,
        // so the free variables remain the Cartesian coordinates
        final FieldKeplerianOrbit<Gradient> gOrbit =
            (FieldKeplerianOrbit<Gradient>) OrbitParamsType.KEPLERIAN.convertType(gState.getOrbit());

        // prepare non-Keplerian elements with proper derivatives
        final Gradient[] parameters = propagator.getDriversFactory().toGradients(nbParams);

        // convert elements to support gradient
        final FieldGnssOrbitalElements<Gradient, O> nonKeplerian =
            propagator.getOrbitalElements().toField(gOrbit, parameters, d  -> Gradient.constant(nbParams, d));
        final FieldGnssOrbitalElements<Gradient, O> gElements =
            FieldGnssPropagator.buildOrbitalElements(gState, nonKeplerian,
                                                     new NonKeplerianDriversFactory(),
                                                     propagator.getECEF(),
                                                     propagator.getAttitudeProvider(),
                                                     gState.getMass());

        // build propagator handling gradient
        final FieldGnssPropagator<Gradient, O> gPropagator =
            new FieldGnssPropagator<>(gElements, gState.getFrame(),
                                      propagator.getECEF(), propagator.getAttitudeProvider(),
                                      gState.getMass());
        final List<ParameterDriver> gDrivers = gPropagator.getParametersDrivers();
        for (final ParameterDriver driver : getParametersDrivers()) {
            if (driver.isSelected()) {
                gDrivers.
                    stream().
                    filter(gDriver -> driver.getName().equals(gDriver.getName())).
                    findFirst().
                    ifPresent(gDriver -> gDriver.setSelected(true));
            }
        }

        return gPropagator;

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return propagator.getParametersDrivers();
    }

}
