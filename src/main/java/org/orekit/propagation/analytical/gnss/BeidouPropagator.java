/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.orekit.data.DataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.propagation.Propagator;
import org.orekit.utils.IERSConventions;

/**
 * This class aims at propagating a Beidou orbit from {@link BeidouOrbitalElements}.
 *
 * @see <a href="http://www2.unb.ca/gge/Resources/beidou_icd_english_ver2.0.pdf">Beidou Interface Control Document</a>
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class BeidouPropagator extends AbstractGNSSPropagator {

    // Constants
    /** Value of the earth's rotation rate in rad/s. */
    private static final double BEIDOU_AV = 7.2921150e-5;

    /** Duration of the Beidou cycle in seconds. */
    private static final double BEIDOU_CYCLE_DURATION = BeidouOrbitalElements.BEIDOU_WEEK_IN_SECONDS *
                                                        BeidouOrbitalElements.BEIDOU_WEEK_NB;

    // Fields
    /** The Beidou orbital elements used. */
    private final BeidouOrbitalElements bdsOrbit;

    /**
     * This nested class aims at building a BeidouPropagator.
     * <p>It implements the classical builder pattern.</p>
     *
     */
    public static class Builder {

        // Required parameter
        /** The Beidou orbital elements. */
        private final BeidouOrbitalElements orbit;

        // Optional parameters
        /** The attitude provider. */
        private AttitudeProvider attitudeProvider;
        /** The mass. */
        private double mass = DEFAULT_MASS;
        /** The ECI frame. */
        private Frame eci  = null;
        /** The ECEF frame. */
        private Frame ecef = null;

        /** Initializes the builder.
         * <p>The Beidou orbital elements is the only requested parameter to build a BeidouPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW} in the
         *  default data context.<br>
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
         *
         * @param bdsOrbElt the Beidou orbital elements to be used by the Beidou propagator.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @see #Builder(BeidouOrbitalElements, Frames)
         */
        @DefaultDataContext
        public Builder(final BeidouOrbitalElements bdsOrbElt) {
            this(bdsOrbElt, DataContext.getDefault().getFrames());
        }

        /** Initializes the builder.
         * <p>The Beidou orbital elements is the only requested parameter to build a BeidouPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#getDefaultLaw(Frames)
         *  DEFAULT_LAW}.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The ECI frame is set by default to the
         *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
         * The ECEF frame is set by default to the
         *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP
         *  CIO/2010-based ITRF simple EOP}.
         * </p>
         *
         * @param bdsOrbElt the Beidou orbital elements to be used by the Beidou propagator.
         * @param frames set of frames to use building the propagator.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @since 10.1
         */
        public Builder(final BeidouOrbitalElements bdsOrbElt, final Frames frames) {
            this.orbit = bdsOrbElt;
            this.eci   = frames.getEME2000();
            this.ecef  = frames.getITRF(IERSConventions.IERS_2010, true);
            attitudeProvider = Propagator.getDefaultLaw(frames);
        }

        /** Sets the attitude provider.
         *
         * @param userProvider the attitude provider
         * @return the updated builder
         */
        public Builder attitudeProvider(final AttitudeProvider userProvider) {
            this.attitudeProvider = userProvider;
            return this;
        }

        /** Sets the mass.
         *
         * @param userMass the mass (in kg)
         * @return the updated builder
         */
        public Builder mass(final double userMass) {
            this.mass = userMass;
            return this;
        }

        /** Sets the Earth Centered Inertial frame used for propagation.
         *
         * @param inertial the ECI frame
         * @return the updated builder
         */
        public Builder eci(final Frame inertial) {
            this.eci = inertial;
            return this;
        }

        /** Sets the Earth Centered Earth Fixed frame.
         *
         * @param bodyFixed the ECEF frame
         * @return the updated builder
         */
        public Builder ecef(final Frame bodyFixed) {
            this.ecef = bodyFixed;
            return this;
        }

        /** Finalizes the build.
         *
         * @return the built BeidouPropagator
         */
        public BeidouPropagator build() {
            return new BeidouPropagator(this);
        }
    }

    /**
     * Private constructor.
     *
     * @param builder the builder
     */
    private BeidouPropagator(final Builder builder) {
        super(builder.orbit, builder.attitudeProvider,
              builder.eci, builder.ecef, builder.mass,
              BEIDOU_AV, BEIDOU_CYCLE_DURATION, BeidouOrbitalElements.BEIDOU_MU);
        // Stores the Beidou orbital elements
        this.bdsOrbit = builder.orbit;
    }

    /**
     * Get the underlying Beidou orbital elements.
     *
     * @return the underlying Beidou orbital elements
     */
    public BeidouOrbitalElements getBeidouOrbitalElements() {
        return bdsOrbit;
    }

}
