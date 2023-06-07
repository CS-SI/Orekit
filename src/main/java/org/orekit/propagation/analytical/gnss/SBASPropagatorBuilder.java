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
import org.orekit.propagation.analytical.gnss.data.GNSSConstants;
import org.orekit.propagation.analytical.gnss.data.SBASOrbitalElements;
import org.orekit.utils.IERSConventions;

/**
 * This nested class aims at building a SBASPropagator.
 * <p>It implements the classical builder pattern.</p>
 * @since 11.0
 * @author Bryan Cazabonne
 */
public class SBASPropagatorBuilder {

    /** The SBAS orbital elements. */
    private final SBASOrbitalElements orbit;

    /** The Earth gravity coefficient used for SBAS propagation. */
    private double mu;

    /** The attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** The mass. */
    private double mass;

    /** The ECI frame. */
    private Frame eci;

    /** The ECEF frame. */
    private Frame ecef;

    /** Initializes the builder.
     * <p>The SBAS orbital elements is the only requested parameter to build a SBASPropagator.</p>
     * <p>The attitude provider is set by default be aligned with the EME2000 frame.<br>
     * The Earth gravity coefficient is set by default to the
     *  {@link org.orekit.propagation.analytical.gnss.data.GNSSConstants#SBAS_MU SBAS_MU}.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
     * </p>
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.</p>
     *
     * @param sbasOrbElt the SBAS orbital elements to be used by the SBAS propagator.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mu(double coefficient)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     * @since 12.0
     */
    @DefaultDataContext
    public SBASPropagatorBuilder(final SBASOrbitalElements sbasOrbElt) {
        this(sbasOrbElt, DataContext.getDefault().getFrames());
    }

    /** Initializes the builder.
     * <p>The SBAS orbital elements is the only requested parameter to build a SBASPropagator.</p>
     * <p>The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The Earth gravity coefficient is set by default to the
     *  {@link org.orekit.propagation.analytical.gnss.data.GNSSConstants#SBAS_MU SBAS_MU}.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
     * </p>
     *
     * @param sbasOrbElt the SBAS orbital elements to be used by the SBAS propagator.
     * @param frames     set of reference frames to use to initialize {@link
     *                   #ecef(Frame)}, {@link #eci(Frame)}, and {@link
     *                   #attitudeProvider(AttitudeProvider)}.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mu(double coefficient)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    public SBASPropagatorBuilder(final SBASOrbitalElements sbasOrbElt, final Frames frames) {
        this.orbit            = sbasOrbElt;
        this.mass             = Propagator.DEFAULT_MASS;
        this.eci              = frames.getEME2000();
        this.ecef             = frames.getITRF(IERSConventions.IERS_2010, true);
        this.mu               = GNSSConstants.SBAS_MU;
        this.attitudeProvider = new FrameAlignedProvider(eci);
    }

    /** Sets the attitude provider.
     *
     * @param userProvider the attitude provider
     * @return the updated builder
     */
    public SBASPropagatorBuilder attitudeProvider(final AttitudeProvider userProvider) {
        this.attitudeProvider = userProvider;
        return this;
    }

    /** Sets the Earth gravity coefficient.
    *
    * @param coefficient the Earth gravity coefficient
    * @return the updated builder
    */
    public SBASPropagatorBuilder mu(final double coefficient) {
        this.mu = coefficient;
        return this;
    }

    /** Sets the mass.
     *
     * @param userMass the mass (in kg)
     * @return the updated builder
     */
    public SBASPropagatorBuilder mass(final double userMass) {
        this.mass = userMass;
        return this;
    }

    /** Sets the Earth Centered Inertial frame used for propagation.
     *
     * @param inertial the ECI frame
     * @return the updated builder
     */
    public SBASPropagatorBuilder eci(final Frame inertial) {
        this.eci = inertial;
        return this;
    }

    /** Sets the Earth Centered Earth Fixed frame assimilated to the WGS84 ECEF.
     *
     * @param bodyFixed the ECEF frame
     * @return the updated builder
     */
    public SBASPropagatorBuilder ecef(final Frame bodyFixed) {
        this.ecef = bodyFixed;
        return this;
    }

    /** Finalizes the build.
     *
     * @return the built SBASPropagator
     */
    public SBASPropagator build() {
        return new SBASPropagator(orbit, eci, ecef, attitudeProvider, mass, mu);
    }

}
