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
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsDriversProvider;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElementsFactory;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.conversion.AbstractAnalyticalPropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

/**
 * Builder for {@link GNSSPropagator}.
 * @param <O> type of the orbital elements
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @since 11.0
 */
public class GNSSPropagatorBuilder<O extends GNSSOrbitalElements<O>>
    extends AbstractAnalyticalPropagatorBuilder<GNSSPropagator<O>, O, GNSSOrbitalElementsFactory<O>> {

    /** The body-fixed frame. */
    private final Frame bodyFixed;

    /** Initializes the builder.
     * <p>The attitude provider is set by default to be aligned with the provided inertial frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.
     * </p>
     *
     * @param factory factory for initial orbit
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     */
    public GNSSPropagatorBuilder(final GNSSOrbitalElementsFactory<O> factory,
                                 final Frame inertial, final Frame bodyFixed) {
        super(factory, false, FrameAlignedProvider.of(inertial), Propagator.DEFAULT_MASS);
        this.bodyFixed = bodyFixed;

        // add non-Keplerian propagation parameters (iDot, cic, cis,…)
        addPropagationParameters(factory.createFromDrivers().getParametersDrivers());

    }

    /**  {@inheritDoc} */
    @Override
    public GNSSPropagator<O> buildPropagator(final double[] normalizedParameters) {

        // set parameters
        setParameters(normalizedParameters);

        // Keplerian elements
        final GNSSOrbitalElementsFactory<O> factory = getOrbitalParameterFactory();
        final O elements = factory.createFromDrivers();

        // non-Keplerian elements
        final ParameterDriversList pDrivers = getPropagationParametersDrivers();
        elements.setTime(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.TIME).getValue());
        elements.setADot(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.A_DOT).getValue());
        elements.setDeltaN0(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.DELTA_N0).getValue());
        elements.setDeltaN0Dot(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.DELTA_N0_DOT).getValue());
        elements.setIDot(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.INCLINATION_RATE).getValue());
        elements.setOmegaDot(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.LONGITUDE_RATE).getValue());
        elements.setCuc(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.LATITUDE_COSINE).getValue());
        elements.setCus(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.LATITUDE_SINE).getValue());
        elements.setCrc(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.RADIUS_COSINE).getValue());
        elements.setCrs(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.RADIUS_SINE).getValue());
        elements.setCic(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.INCLINATION_COSINE).getValue());
        elements.setCis(pDrivers.findByName(GNSSOrbitalElementsDriversProvider.INCLINATION_SINE).getValue());

        return new GNSSPropagator<>(elements, factory.getFrame(),
                                    bodyFixed, getAttitudeProvider(), getMass());

    }

}
