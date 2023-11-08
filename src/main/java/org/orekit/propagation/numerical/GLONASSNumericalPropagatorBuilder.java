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
package org.orekit.propagation.numerical;

import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.gnss.data.GLONASSAlmanac;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSOrbitalElements;

/**
 * This nested class aims at building a GLONASSNumericalPropagator.
 * <p>It implements the classical builder pattern.</p>
 * <p>
 * <b>Caution:</b> The Glonass numerical propagator can only be used with {@link GLONASSNavigationMessage}.
 * Using this propagator with a {@link GLONASSAlmanac} is prone to error.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class GLONASSNumericalPropagatorBuilder {

    //////////
    // Required parameter
    //////////

    /** The GLONASS orbital elements. */
    private final GLONASSOrbitalElements orbit;

    /** The 4th order Runge-Kutta integrator. */
    private final ClassicalRungeKuttaIntegrator integrator;

    /** Flag for availability of projections of acceleration transmitted within the navigation message. */
    private final boolean isAccAvailable;

    ///////////
    // Optional parameters
    //////////

    /** The attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** The mass. */
    private double mass;

    /** The ECI frame. */
    private Frame eci;

    /** Data context for the propagator. */
    private DataContext dataContext;

    /**
     * Initializes the builder.
     * <p>The attitude provider is set by default to EME2000 aligned in the
     *  default data context.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The data context is by default to the
     *  {@link DataContext#getDefault() default data context}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * </p>
     *
     * @param integrator 4th order Runge-Kutta as recommended by GLONASS ICD
     * @param glonassOrbElt the GLONASS orbital elements to be used by the GLONASSNumericalPropagator.
     * @param isAccAvailable flag for availability of the projections of accelerations transmitted within
     *        the navigation message
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     */
    @DefaultDataContext
    public GLONASSNumericalPropagatorBuilder(final ClassicalRungeKuttaIntegrator integrator,
                                             final GLONASSOrbitalElements glonassOrbElt,
                                             final boolean isAccAvailable) {
        this(integrator, glonassOrbElt, isAccAvailable, DataContext.getDefault());
    }


    /**
     * Initializes the builder.
     * <p>The attitude provider is set by default to EME2000 aligned in the
     *  provided data context.<br>
     * The mass is set by default to the
     *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
     * The ECI frame is set by default to the
     *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame} in the default data
     *  context.<br>
     * </p>
     *
     * @param integrator 4th order Runge-Kutta as recommended by GLONASS ICD
     * @param glonassOrbElt the GLONASS orbital elements to be used by the GLONASSNumericalPropagator.
     * @param isAccAvailable flag for availability of the projections of accelerations transmitted within
     *        the navigation message
     * @param context data context
     * @see #attitudeProvider(AttitudeProvider provider)
     * @see #mass(double mass)
     * @see #eci(Frame inertial)
     */
    public GLONASSNumericalPropagatorBuilder(final ClassicalRungeKuttaIntegrator integrator,
                                             final GLONASSOrbitalElements glonassOrbElt,
                                             final boolean isAccAvailable,
                                             final DataContext context) {
        this.isAccAvailable   = isAccAvailable;
        this.integrator       = integrator;
        this.orbit            = glonassOrbElt;
        this.mass             = Propagator.DEFAULT_MASS;
        this.dataContext      = context;
        this.eci              = dataContext.getFrames().getEME2000();
        this.attitudeProvider = FrameAlignedProvider.of(this.eci);
    }

    /**
     * Sets the attitude provider.
     *
     * @param userProvider the attitude provider
     * @return the updated builder
     */
    public GLONASSNumericalPropagatorBuilder attitudeProvider(final AttitudeProvider userProvider) {
        this.attitudeProvider = userProvider;
        return this;
    }

    /**
     * Sets the mass.
     *
     * @param userMass the mass (in kg)
     * @return the updated builder
     */
    public GLONASSNumericalPropagatorBuilder mass(final double userMass) {
        this.mass = userMass;
        return this;
    }

    /**
     * Sets the Earth Centered Inertial frame used for propagation.
     *
     * @param inertial the ECI frame
     * @return the updated builder
     */
    public GLONASSNumericalPropagatorBuilder eci(final Frame inertial) {
        this.eci = inertial;
        return this;
    }

    /**
     * Finalizes the build.
     *
     * @return the built Glonass numerical propagator
     */
    public GLONASSNumericalPropagator build() {
        return new GLONASSNumericalPropagator(integrator, orbit, eci, attitudeProvider,
                                              mass, dataContext, isAccAvailable);
    }

}
