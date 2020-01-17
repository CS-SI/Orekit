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
 * This class aims at propagating a QZSS orbit from {@link QZSSOrbitalElements}.
 *
 * @see <a href="http://qzss.go.jp/en/technical/download/pdf/ps-is-qzss/is-qzss-pnt-003.pdf?t=1549268771755">
 *       QZSS Interface Specification</a>
 *
 * @author Bryan Cazabonne
 * @since 10.0
 *
 */
public class QZSSPropagator extends AbstractGNSSPropagator {

    // Constants
    /** WGS 84 value of the earth's rotation rate in rad/s. */
    private static final double QZSS_AV = 7.2921151467e-5;

    /** Duration of the QZSS cycle in seconds. */
    private static final double QZSS_CYCLE_DURATION = QZSSOrbitalElements.QZSS_WEEK_IN_SECONDS *
                                                      QZSSOrbitalElements.QZSS_WEEK_NB;

    // Fields
    /** The QZSS orbital elements used. */
    private final QZSSOrbitalElements qzssOrbit;

    /**
     * This nested class aims at building a QZSSPropagator.
     * <p>It implements the classical builder pattern.</p>
     *
     */
    public static class Builder {

        // Required parameter
        /** The QZSS orbital elements. */
        private final QZSSOrbitalElements orbit;

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
         * <p>The QZSS orbital elements is the only requested parameter to build a QZSSPropagator.</p>
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
         * @param qzssOrbElt the QZSS orbital elements to be used by the QZSSpropagator.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @see #Builder(QZSSOrbitalElements, Frames)
         */
        @DefaultDataContext
        public Builder(final QZSSOrbitalElements qzssOrbElt) {
            this(qzssOrbElt, DataContext.getDefault().getFrames());
        }

        /** Initializes the builder.
         * <p>The QZSS orbital elements is the only requested parameter to build a QZSSPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#getDefaultLaw(Frames)}.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The ECI frame is set by default to the
         *  {@link Frames#getEME2000()}  EME2000 frame}.<br>
         * The ECEF frame is set by default to the
         *  {@link Frames#getITRF(IERSConventions, boolean)} CIO/2010-based ITRF simple EOP}.
         * </p>
         *
         * @param qzssOrbElt the QZSS orbital elements to be used by the QZSSpropagator.
         * @param frames set of frames to use.
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         * @since 10.1
         */
        public Builder(final QZSSOrbitalElements qzssOrbElt,
                       final Frames frames) {
            this.orbit = qzssOrbElt;
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
         * @return the built QZSSPropagator
         */
        public QZSSPropagator build() {
            return new QZSSPropagator(this);
        }
    }

    /**
     * Private constructor.
     *
     * @param builder the builder
     */
    private QZSSPropagator(final Builder builder) {
        super(builder.orbit, builder.attitudeProvider,
              builder.eci, builder.ecef, builder.mass,
              QZSS_AV, QZSS_CYCLE_DURATION, QZSSOrbitalElements.QZSS_MU);
        // Stores the QZSS orbital elements
        this.qzssOrbit = builder.orbit;
    }

    /**
     * Gets the underlying QZSS orbital elements.
     *
     * @return the underlying QZSS orbital elements
     */
    public QZSSOrbitalElements getQZSSOrbitalElements() {
        return qzssOrbit;
    }

}
