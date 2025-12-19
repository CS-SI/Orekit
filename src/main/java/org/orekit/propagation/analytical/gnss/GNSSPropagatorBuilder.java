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
package org.orekit.propagation.analytical.gnss;

import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.conversion.AbstractAnalyticalPropagatorBuilder;

import java.util.stream.Collectors;

/**
 * Builder for {@link GNSSPropagator}.
 * @param <O> type of the orbital elements
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @since 11.0
 */
public class GNSSPropagatorBuilder<O extends GNSSOrbitalElements<O>>
    extends AbstractAnalyticalPropagatorBuilder<GNSSPropagator<O>, O, GNSSOrbitalElementsFactory<O>> {

    /** Initializes the builder.
     * <p>
     * The attitude provider is set by default to be aligned with the provided inertial frame.
     * This can be changed after construction by calling
     * {@link #setAttitudeProvider(org.orekit.attitudes.AttitudeProvider) setAttitudeProvider}
     * </p>
     * <p>
     * The mass is set to the {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.
     * This can be changed after construction by calling {@link #setMass(double) setMass}
     * </p>
     * @param factory factory for initial orbit
     */
    public GNSSPropagatorBuilder(final GNSSOrbitalElementsFactory<O> factory) {
        super(factory, false, FrameAlignedProvider.of(factory.getInertial()), Propagator.DEFAULT_MASS);

        // add non-Keplerian propagation parameters (iDot, cic, cis,…)
        addPropagationParameters(factory.
                                 getNonKeplerianParametersDrivers().
                                 getDrivers().
                                 stream().
                                 map(d -> d.getRawDrivers().get(0)).
                                 collect(Collectors.toList()));

    }

    /**  {@inheritDoc} */
    @Override
    public GNSSPropagator<O> buildPropagator(final double[] normalizedParameters) {

        // set parameters
        setParameters(normalizedParameters);

        return new GNSSPropagator<>(getOrbitalParameterFactory().createFromDrivers(),
                                    getOrbitalParameterFactory().getInertial(),
                                    getOrbitalParameterFactory().getBodyFixed(),
                                    getAttitudeProvider(), getMass());

    }

    /** {@inheritDoc} */
    @Override
    public GNSSPropagator<O> buildPropagator() {
        return buildPropagator(getSelectedNormalizedParameters());
    }

}
