/* Copyright 2002-2026 CS GROUP
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
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.propagation.conversion.AbstractAnalyticalPropagatorBuilder;

/**
 * Builder for {@link GNSSPropagator}.
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @since 11.0
 */
public class GNSSPropagatorBuilder extends AbstractAnalyticalPropagatorBuilder<GNSSPropagator> {

    /** The GNSS propagation model orbital elements. */
    private final GNSSOrbitalElements<?> orbitalElements;

    /** The body-fixed frame. */
    private final Frame bodyFixed;

    /** Initializes the builder.
     * <p>The GNSS orbital elements and frames are the only requested parameters to build a GNSSPropagator.</p>
     * <p>The attitude provider is set by default to be aligned with the provided inertial frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.
     * </p>
     *
     * @param orbitalElements orbital elements
     * @param inertial inertial frame, use to provide the propagated orbit
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     */
    public GNSSPropagatorBuilder(final GNSSOrbitalElements<?> orbitalElements,
                                 final Frame inertial, final Frame bodyFixed) {
        super(new GNSSPropagator(orbitalElements, inertial, bodyFixed,
                                 FrameAlignedProvider.of(inertial),
                                 Propagator.DEFAULT_MASS).
              getInitialState().
              getOrbit(),
              PositionAngleType.TRUE,
              1.0,
              false,
              FrameAlignedProvider.of(inertial),
              Propagator.DEFAULT_MASS);
        this.orbitalElements = orbitalElements;
        this.bodyFixed       = bodyFixed;
        addSupportedParameters(orbitalElements.getParametersDrivers());
    }

    /**  {@inheritDoc} */
    @Override
    public GNSSPropagator buildPropagator(final double[] normalizedParameters) {
        setParameters(normalizedParameters);
        return new GNSSPropagator(orbitalElements, getFrame(), bodyFixed, getAttitudeProvider(), getMass());
    }

}
