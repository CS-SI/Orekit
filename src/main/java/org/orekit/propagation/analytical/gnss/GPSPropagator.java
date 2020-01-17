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
 * This class aims at propagating a GPS orbit from {@link GPSOrbitalElements}.
 *
 * @see <a href="http://www.gps.gov/technical/icwg/IS-GPS-200H.pdf">GPS Interface Specification</a>
 * @author Pascal Parraud
 * @since 8.0
 */
public class GPSPropagator extends AbstractGNSSPropagator {

    // Constants
    /** WGS 84 value of the earth's rotation rate in rad/s. */
    private static final double GPS_AV = 7.2921151467e-5;

    /** Duration of the GPS cycle in seconds. */
    private static final double GPS_CYCLE_DURATION = GPSOrbitalElements.GPS_WEEK_IN_SECONDS *
                                                     GPSOrbitalElements.GPS_WEEK_NB;

    // Fields
    /** The GPS orbital elements used. */
    private final GPSOrbitalElements gpsOrbit;

    /**
     * This nested class aims at building a GPSPropagator.
     * <p>It implements the classical builder pattern.</p>
     *
     */
    public static class Builder {

        // Required parameter
        /** The GPS orbital elements. */
        private final GPSOrbitalElements orbit;

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
         * <p>The GPS orbital elements is the only requested parameter to build a GPSPropagator.</p>
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
         * @param gpsOrbElt the GPS orbital elements to be used by the GPSpropagator.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @see #Builder(GPSOrbitalElements, Frames)
         */
        @DefaultDataContext
        public Builder(final GPSOrbitalElements gpsOrbElt) {
            this(gpsOrbElt, DataContext.getDefault().getFrames());
        }

        /** Initializes the builder.
         * <p>The GPS orbital elements is the only requested parameter to build a GPSPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#getDefaultLaw(Frames)}  DEFAULT_LAW}.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The ECI frame is set by default to the
         *  {@link Frames#getEME2000() EME2000 frame}.<br>
         * The ECEF frame is set by default to the
         *  {@link Frames#getITRF(IERSConventions, boolean)} CIO/2010-based ITRF simple EOP}.
         * </p>
         *
         * @param gpsOrbElt the GPS orbital elements to be used by the GPSpropagator.
         * @param frames set of frames to use.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @since 10.1
         */
        public Builder(final GPSOrbitalElements gpsOrbElt, final Frames frames) {
            this.orbit = gpsOrbElt;
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
         * @return the built GPSPropagator
         */
        public GPSPropagator build() {
            return new GPSPropagator(this);
        }
    }

    /**
     * Private constructor.
     *
     * @param builder the builder
     */
    private GPSPropagator(final Builder builder) {
        super(builder.orbit, builder.attitudeProvider,
              builder.eci, builder.ecef, builder.mass,
              GPS_AV, GPS_CYCLE_DURATION, GPSOrbitalElements.GPS_MU);
        // Stores the GPS orbital elements
        this.gpsOrbit = builder.orbit;
    }

    /**
     * Gets the underlying GPS orbital elements.
     *
     * @return the underlying GPS orbital elements
     */
    public GPSOrbitalElements getGPSOrbitalElements() {
        return gpsOrbit;
    }

}
