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

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * This class aims at propagating a SBAS orbit from {@link SBASOrbitalElements}.
 *
 * @see "Tyler Reid, Todd Walker, Per Enge, L1/L5 SBAS MOPS Ephemeris Message to
 *       Support Multiple Orbit Classes, ION ITM, 2013"
 *
 * @author Bryan Cazabonne
 * @since 10.1
 *
 */
public class SBASPropagator extends AbstractAnalyticalPropagator {

    /** The SBAS orbital elements used. */
    private final SBASOrbitalElements sbasOrbit;

    /** The spacecraft mass (kg). */
    private final double mass;

    /** The Earth gravity coefficient used for SBAS propagation. */
    private final double mu;

    /** The ECI frame used for SBAS propagation. */
    private final Frame eci;

    /** The ECEF frame used for SBAS propagation. */
    private final Frame ecef;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /**
     * This nested class aims at building a SBASPropagator.
     * <p>It implements the classical builder pattern.</p>
     *
     */
    public static class Builder {

        /** The SBAS orbital elements. */
        private final SBASOrbitalElements orbit;

        /** The Earth gravity coefficient used for SBAS propagation. */
        private double mu;

        /** The attitude provider. */
        private AttitudeProvider attitudeProvider;

        /** The mass. */
        private double mass = DEFAULT_MASS;

        /** The ECI frame. */
        private Frame eci  = null;

        /** The ECEF frame. */
        private Frame ecef = null;

        /** Initializes the builder.
         * <p>The SBAS orbital elements is the only requested parameter to build a SBASPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW}.<br>
         * The Earth gravity coefficient is set by default to the
         *  {@link org.orekit.propagation.analytical.gnss.SBASOrbitalElements#SBAS_MU SBAS_MU}.<br>
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
        public Builder(final SBASOrbitalElements sbasOrbElt, final Frames frames) {
            this.orbit = sbasOrbElt;
            this.eci   = frames.getEME2000();
            this.ecef  = frames.getITRF(IERSConventions.IERS_2010, true);
            this.mu    = SBASOrbitalElements.SBAS_MU;
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

        /** Sets the Earth gravity coefficient.
        *
        * @param coefficient the Earth gravity coefficient
        * @return the updated builder
        */
        public Builder mu(final double coefficient) {
            this.mu = coefficient;
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
         * @return the built SBASPropagator
         */
        public SBASPropagator build() {
            return new SBASPropagator(this);
        }

    }

    /**
     * Private constructor.
     * @param builder the builder
     */
    private SBASPropagator(final Builder builder) {
        super(builder.attitudeProvider);
        // Stores the SBAS orbital elements
        this.sbasOrbit = builder.orbit;
        // Sets the start date as the date of the orbital elements
        setStartDate(sbasOrbit.getDate());
        // Sets the mu
        this.mu = builder.mu;
        // Sets the mass
        this.mass = builder.mass;
        // Sets the Earth Centered Inertial frame
        this.eci  = builder.eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = builder.ecef;

        this.factory = new DSFactory(1, 2);
    }

    /**
     * Gets the PVCoordinates of the GNSS SV in {@link #getECEF() ECEF frame}.
     *
     * <p>The algorithm uses automatic differentiation to compute velocity and
     * acceleration.</p>
     *
     * @param date the computation date
     * @return the GNSS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    public PVCoordinates propagateInEcef(final AbsoluteDate date) {
        // Duration from SBAS ephemeris Reference date
        final DerivativeStructure dt = factory.variable(0, getDT(date));
        // Satellite coordinates
        final DerivativeStructure x = dt.multiply(dt.multiply(0.5 * sbasOrbit.getXDotDot()).add(sbasOrbit.getXDot())).add(sbasOrbit.getX());
        final DerivativeStructure y = dt.multiply(dt.multiply(0.5 * sbasOrbit.getYDotDot()).add(sbasOrbit.getYDot())).add(sbasOrbit.getY());
        final DerivativeStructure z = dt.multiply(dt.multiply(0.5 * sbasOrbit.getZDotDot()).add(sbasOrbit.getZDot())).add(sbasOrbit.getZ());
        // Returns the Earth-fixed coordinates
        final FieldVector3D<DerivativeStructure> positionwithDerivatives =
                        new FieldVector3D<>(x, y, z);
        return new PVCoordinates(positionwithDerivatives);
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        // Gets the PVCoordinates in ECEF frame
        final PVCoordinates pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final PVCoordinates pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new CartesianOrbit(pvaInECI, eci, date, mu);
    }

    /**
     * Get the Earth gravity coefficient used for SBAS propagation.
     * @return the Earth gravity coefficient.
     */
    public double getMU() {
        return mu;
    }

    /**
     * Gets the Earth Centered Inertial frame used to propagate the orbit.
     *
     * @return the ECI frame
     */
    public Frame getECI() {
        return eci;
    }

    /**
     * Gets the Earth Centered Earth Fixed frame used to propagate GNSS orbits.
     *
     * @return the ECEF frame
     */
    public Frame getECEF() {
        return ecef;
    }

    /**
     * Get the underlying SBAS orbital elements.
     *
     * @return the underlying SBAS orbital elements
     */
    public SBASOrbitalElements getSBASOrbitalElements() {
        return sbasOrbit;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** Get the duration from SBAS Reference epoch.
     * @param date the considered date
     * @return the duration from SBAS orbit Reference epoch (s)
     */
    private double getDT(final AbsoluteDate date) {
        // Time from ephemeris reference epoch
        return date.durationFrom(sbasOrbit.getDate());
    }

}
