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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.utils.IERSConventions;

/**
 * This class aims at propagating a IRNSS orbit from {@link IRNSSOrbitalElements}.
 *
 * @see "Indian Regiona Navigation Satellite System, Signal In Space ICD
 *       for standard positioning service, version 1.1"
 *
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class IRNSSPropagator extends AbstractGNSSPropagator {

    // Constants
    /** WGS 84 value of the earth's rotation rate in rad/s. */
    private static final double IRNSS_AV = 7.2921151467e-5;

    /** Duration of the IRNSS cycle in seconds. */
    private static final double IRNSS_CYCLE_DURATION = IRNSSOrbitalElements.IRNSS_WEEK_IN_SECONDS *
                                                       IRNSSOrbitalElements.IRNSS_WEEK_NB;

    // Fields
    /** The IRNSS orbital elements used. */
    private final IRNSSOrbitalElements irnssOrbit;

    /**
     * This nested class aims at building a IRNSSPropagator.
     * <p>It implements the classical builder pattern.</p>
     *
     */
    public static class Builder {

        // Required parameter
        /** The IRNSS orbital elements. */
        private final IRNSSOrbitalElements orbit;

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
         * <p>The IRNSS orbital elements is the only requested parameter to build a IRNSSPropagator.</p>
         * <p>The attitude provider is set by default to be aligned with the J2000 frame.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The ECI frame is set by default to the
         *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
         * The ECEF frame is set by default to the
         *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
         * </p>
         *
         * @param irnssOrbElt the IRNSS orbital elements to be used by the IRNSSpropagator.
         * @param frames      set of reference frames to use to initialize {@link
         *                    #ecef(Frame)}, {@link #eci(Frame)}, and {@link
         *                    #attitudeProvider(AttitudeProvider)}.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         */
        public Builder(final IRNSSOrbitalElements irnssOrbElt, final Frames frames) {
            this.orbit = irnssOrbElt;
            this.eci   = frames.getEME2000();
            this.ecef  = frames.getITRF(IERSConventions.IERS_2010, true);
            this.attitudeProvider = new InertialProvider(eci);
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

        /** Sets the Earth Centered Earth Fixed frame assimilated to the WGS84 ECEF.
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
         * @return the built IRNSSPropagator
         */
        public IRNSSPropagator build() {
            return new IRNSSPropagator(this);
        }
    }

    /**
     * Private constructor.
     *
     * @param builder the builder
     */
    private IRNSSPropagator(final Builder builder) {
        super(builder.orbit, builder.attitudeProvider,
              builder.eci, builder.ecef, builder.mass,
              IRNSS_AV, IRNSS_CYCLE_DURATION, IRNSSOrbitalElements.IRNSS_MU);
        // Stores the IRNSS orbital elements
        this.irnssOrbit = builder.orbit;
    }

    /**
     * Gets the underlying IRNSS orbital elements.
     *
     * @return the underlying IRNSS orbital elements
     */
    public IRNSSOrbitalElements getIRNSSOrbitalElements() {
        return irnssOrbit;
    }

}
