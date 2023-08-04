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
import org.orekit.propagation.analytical.gnss.data.GLONASSAlmanac;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSOrbitalElements;
import org.orekit.utils.IERSConventions;

/**
 * This nested class aims at building a GLONASSAnalyticalPropagator.
 * <p>It implements the classical builder pattern.</p>
 * <p>
 * <b>Caution:</b> The Glonass analytical propagator can only be used with {@link GLONASSAlmanac}.
 * Using this propagator with a {@link GLONASSNavigationMessage} is prone to error.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class GLONASSAnalyticalPropagatorBuilder {

    //////////
    // Required parameter
    //////////

    /** The GLONASS orbital elements. */
    private final GLONASSOrbitalElements orbit;

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

    /** Data context. */
    private DataContext dataContext;

    /** Initializes the builder.
     * <p>The GLONASS orbital elements is the only requested parameter to build a GLONASSAnalyticalPropagator.</p>
     * <p>The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the
     *  {@link DataContext#getDefault() default data context}.<br>
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
     * {@code Builder(final GLONASSOrbitalElements gpsOrbElt, final DataContext dataContext)}</p>
     *
     * @param glonassOrbElt the GLONASS orbital elements to be used by the GLONASS analytical propagator.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     */
    @DefaultDataContext
    public GLONASSAnalyticalPropagatorBuilder(final GLONASSOrbitalElements glonassOrbElt) {
        this(glonassOrbElt, DataContext.getDefault());
    }

    /** Initializes the builder.
     * <p>The GLONASS orbital elements is the only requested parameter to build a GLONASSAnalyticalPropagator.</p>
     * <p>The attitude provider is set by default to be aligned with the EME2000 frame.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link Frames#getEME2000() EME2000 frame}.<br>
     * The ECEF frame is set by default to the
     *  {@link Frames#getITRF(IERSConventions, boolean) CIO/2010-based ITRF simple
     *  EOP}.
     * </p>
     *
     * @param glonassOrbElt the GLONASS orbital elements to be used by the GLONASS propagator.
     * @param dataContext the data context to use for frames and time scales.
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     * @see #ecef(Frame bodyFixed)
     * @since 10.1
     */
    public GLONASSAnalyticalPropagatorBuilder(final GLONASSOrbitalElements glonassOrbElt,
                                              final DataContext dataContext) {
        this.orbit            = glonassOrbElt;
        this.dataContext      = dataContext;
        this.mass             = Propagator.DEFAULT_MASS;
        final Frames frames   = dataContext.getFrames();
        this.eci              = frames.getEME2000();
        this.ecef             = frames.getITRF(IERSConventions.IERS_2010, true);
        this.attitudeProvider = FrameAlignedProvider.of(this.eci);
    }

    /** Sets the attitude provider.
     *
     * @param userProvider the attitude provider
     * @return the updated builder
     */
    public GLONASSAnalyticalPropagatorBuilder attitudeProvider(final AttitudeProvider userProvider) {
        this.attitudeProvider = userProvider;
        return this;
    }

    /** Sets the mass.
     *
     * @param userMass the mass (in kg)
     * @return the updated builder
     */
    public GLONASSAnalyticalPropagatorBuilder mass(final double userMass) {
        this.mass = userMass;
        return this;
    }

    /** Sets the Earth Centered Inertial frame used for propagation.
     *
     * @param inertial the ECI frame
     * @return the updated builder
     */
    public GLONASSAnalyticalPropagatorBuilder eci(final Frame inertial) {
        this.eci = inertial;
        return this;
    }

    /** Sets the Earth Centered Earth Fixed frame assimilated to the WGS84 ECEF.
     *
     * @param bodyFixed the ECEF frame
     * @return the updated builder
     */
    public GLONASSAnalyticalPropagatorBuilder ecef(final Frame bodyFixed) {
        this.ecef = bodyFixed;
        return this;
    }

    /** Sets the data context used by the propagator. Does not update the ECI or ECEF
     * frames which must be done separately using {@link #eci(Frame)} and {@link
     * #ecef(Frame)}.
     *
     * @param context used for propagation.
     * @return the updated builder.
     */
    public GLONASSAnalyticalPropagatorBuilder dataContext(final DataContext context) {
        this.dataContext = context;
        return this;
    }

    /** Finalizes the build.
     *
     * @return the built GLONASSPropagator
     */
    public GLONASSAnalyticalPropagator build() {
        return new GLONASSAnalyticalPropagator(orbit, eci, ecef, attitudeProvider, mass, dataContext);
    }

}
