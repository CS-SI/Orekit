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
package org.orekit.propagation.analytical.gnss;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.utils.IERSConventions;

/**
 * This nested class aims at building a GNSSPropagator.
 * <p>It implements the classical builder pattern.</p>
 * @author Pascal Parraud
 * @since 11.0
 */
public class GNSSPropagatorBuilder {

    //////////
    // Required parameter
    //////////

    /** The GNSS orbital elements. */
    private final GNSSOrbitalElements orbit;

    ///////////
    // Optional parameters
    //////////

    /** The attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** The mass. */
    private double mass;

    /** The ECI frame. */
    private Frame eci;

    /** The ECEF frame. */
    private Frame ecef;

    /**
     * Initializes the builder.
     * <p>The GNSS orbital elements is the only requested parameter to build a GNSSPropagator.</p>
     * <p>The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
     *  CIO/2010-based ITRF simple EOP} in the default data context.
     * </p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     * Another data context can be set using
     * {@code Builder(final GNSSOrbitalElements gpsOrbElt, final Frames frames)}</p>
     *
     * @param gnssOrbElt the GNSS orbital elements to be used by the GNSSpropagator.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    @DefaultDataContext
    public GNSSPropagatorBuilder(final GNSSOrbitalElements gnssOrbElt) {
        this(gnssOrbElt, DataContext.getDefault().getFrames());
    }

    /** Initializes the builder.
     * <p>The GNSS orbital elements is the only requested parameter to build a GNSSPropagator.</p>
     * <p>The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link Frames#getEME2000() EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link Frames#getITRF(IERSConventions, boolean)} CIO/2010-based ITRF simple EOP}.
     * </p>
     *
     * @param gnssOrbElt the GNSS orbital elements to be used by the GNSSpropagator.
     * @param frames set of frames to use.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    public GNSSPropagatorBuilder(final GNSSOrbitalElements gnssOrbElt, final Frames frames) {
        this.orbit            = gnssOrbElt;
        this.mass             = Propagator.DEFAULT_MASS;
        this.eci              = frames.getEME2000();
        this.ecef             = frames.getITRF(IERSConventions.IERS_2010, true);
        this.attitudeProvider = FrameAlignedProvider.of(this.eci);
    }

    /** Sets the attitude provider.
     *
     * @param userProvider the attitude provider
     * @return the updated builder
     */
    public GNSSPropagatorBuilder attitudeProvider(final AttitudeProvider userProvider) {
        this.attitudeProvider = userProvider;
        return this;
    }

    /** Sets the mass.
     *
     * @param userMass the mass (in kg)
     * @return the updated builder
     */
    public GNSSPropagatorBuilder mass(final double userMass) {
        this.mass = userMass;
        return this;
    }

    /** Sets the Earth Centered Inertial frame used for propagation.
     *
     * @param inertial the ECI frame
     * @return the updated builder
     */
    public GNSSPropagatorBuilder eci(final Frame inertial) {
        this.eci = inertial;
        return this;
    }

    /** Sets the Earth Centered Earth Fixed frame assimilated to the WGS84 ECEF.
     *
     * @param bodyFixed the ECEF frame
     * @return the updated builder
     */
    public GNSSPropagatorBuilder ecef(final Frame bodyFixed) {
        this.ecef = bodyFixed;
        return this;
    }

    /** Finalizes the build.
     *
     * @return the built GNSSPropagator
     */
    public GNSSPropagator build() {
        return new GNSSPropagator(orbit, eci, ecef, attitudeProvider, mass);
    }

}
