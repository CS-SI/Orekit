/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * This class aims at propagating a GPS orbit from {@link GPSOrbitalElements}.
 *
 * @see <a href="http://www.gps.gov/technical/icwg/IS-GPS-200H.pdf">GPS Interface Specification</a>
 * @author Pascal Parraud
 * @since 8.0
 */
public class GPSPropagator extends AbstractAnalyticalPropagator {

    // Constants
    /** WGS 84 value of the earth's rotation rate in rad/s. */
    private static final double GPS_AV = 7.2921151467e-5;

    /** Duration of the GPS cycle in seconds. */
    private static final double GPS_CYCLE_DURATION = GPSOrbitalElements.GPS_WEEK_IN_SECONDS *
                                                     GPSOrbitalElements.GPS_WEEK_NB;

    // Data used to solve Kepler's equation
    /** First coefficient to compute Kepler equation solver starter. */
    private static final double A;

    /** Second coefficient to compute Kepler equation solver starter. */
    private static final double B;

    static {
        final double k1 = 3 * FastMath.PI + 2;
        final double k2 = FastMath.PI - 1;
        final double k3 = 6 * FastMath.PI - 1;
        A  = 3 * k2 * k2 / k1;
        B  = k3 * k3 / (6 * k1);
    }

    // Fields
    /** The GPS orbital elements used. */
    private final GPSOrbitalElements gpsOrbit;

    /** The spacecraft mass (kg). */
    private final double mass;

    /** The ECI frame used for GPS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GPS propagation. */
    private final Frame ecef;

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
        private AttitudeProvider attitudeProvider = DEFAULT_LAW;
        /** The mass. */
        private double mass = DEFAULT_MASS;
        /** The ECI frame. */
        private Frame eci  = null;
        /** The ECEF frame. */
        private Frame ecef = null;

        /** Initializes the builder.
         * <p>The GPS orbital elements is the only requested parameter to build a GPSPropagator.</p>
         * <p>The attitude provider is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW}.<br/>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br/>
         * The ECI frame is set by default to the
         *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br/>
         * The ECEF frame is set by default to the
         *  {@link org.orekit.frames.Predefined#ITRF_CIO_CONV_2010_SIMPLE_EOP CIO/2010-based ITRF simple EOP}.
         * </p>
         *
         * @param gpsOrbElt the GPS orbital elements to be used by the GPSpropagator.
         * @throws OrekitException if data embedded in the library cannot be read
         * @see #attitudeProvider(AttitudeProvider provider)
         * @see #mass(double mass)
         * @see #eci(Frame inertial)
         * @see #ecef(Frame bodyFixed)
         */
        public Builder(final GPSOrbitalElements gpsOrbElt) throws OrekitException {
            this.orbit = gpsOrbElt;
            this.eci   = FramesFactory.getEME2000();
            this.ecef  = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
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
        super(builder.attitudeProvider);
        // Stores the GPS orbital elements
        this.gpsOrbit = builder.orbit;
        // Sets the start date as the date of the orbital elements
        setStartDate(gpsOrbit.getDate());
        // Sets the mass
        this.mass = builder.mass;
        // Sets the Earth Centered Inertial frame
        this.eci  = builder.eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = builder.ecef;
    }

    /**
     * Gets the PVCoordinates of the GPS SV at the requested date in
     * the {@link #getECEF() ECEF frame} from the GPS orbital elements.
     *
     * @param date the computation date
     * @return the PVCoordinates in ECEF frame
     * @see #getECEF()
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date) {
        // Computes the position at the date
        final Vector3D pos = progate(date);
        // TODO: computes the velocity ...
        final Vector3D vel = Vector3D.NaN;
        final Vector3D acc = Vector3D.NaN;
        // Gets the transform
        return new PVCoordinates(pos, vel, acc);
    }

    /**
     * Gets the position of the GPS SV in WGS84 ECEF.
     *
     * <p>The algorithm is defined at Table 20-IV from IS-GPS-200 document.</p>
     *
     * @param date the computation date
     * @return the GPS SV position in WGS84 ECEF
     */
    public Vector3D progate(final AbsoluteDate date) {
        // Duration from GPS ephemeris Reference date
        final double tk = getTk(date);
        // Mean anomaly
        final double mk = gpsOrbit.getM0() + gpsOrbit.getMeanMotion() * tk;
        // Eccentric Anomaly
        final double ek = getEccentricAnomaly(mk);
        // True Anomaly
        final double vk =  getTrueAnomaly(ek);
        // Argument of Latitude
        final double phik = vk + gpsOrbit.getPa();
        final double c2phi = FastMath.cos(2. * phik);
        final double s2phi = FastMath.sin(2. * phik);
        // Argument of Latitude Correction
        final double dphik = gpsOrbit.getCuc() * c2phi + gpsOrbit.getCus() * s2phi;
        // Radius Correction
        final double drk = gpsOrbit.getCrc() * c2phi + gpsOrbit.getCrs() * s2phi;
        // Inclination Correction
        final double dik = gpsOrbit.getCic() * c2phi + gpsOrbit.getCis() * s2phi;
        // Corrected Argument of Latitude
        final double uk = phik + dphik;
        // Corrected Radius
        final double rk = gpsOrbit.getSma() * (1. - gpsOrbit.getE() * FastMath.cos(ek)) + drk;
        // Corrected Inclination
        final double ik = gpsOrbit.getI0() + gpsOrbit.getIDot() * tk + dik;
        final double cik = FastMath.cos(ik);
        // Positions in orbital plane
        final double xk = rk * FastMath.cos(uk);
        final double yk = rk * FastMath.sin(uk);
        // Corrected longitude of ascending node
        final double omk = gpsOrbit.getOmega0() + (gpsOrbit.getOmegaDot() - GPS_AV) * tk - GPS_AV * gpsOrbit.getTime();
        final double comk = FastMath.cos(omk);
        final double somk = FastMath.sin(omk);
        // returns the Earth-fixed coordinates
        return new Vector3D(xk * comk - yk * somk * cik,
                            xk * somk + yk * comk * cik,
                            yk * FastMath.sin(ik));
    }

    /**
     * Get the duration from GPS Reference epoch.
     * <p>This takes the GPS week roll-over into account.</p>
     *
     * @param date the considered date
     * @return the duration from GPS orbit Reference epoch (s)
     */
    private double getTk(final AbsoluteDate date) {
        // Time from ephemeris reference epoch
        double tk = date.durationFrom(gpsOrbit.getDate());
        // Adjusts the time to take roll over week into account
        while (tk > 0.5 * GPS_CYCLE_DURATION) {
            tk -= GPS_CYCLE_DURATION;
        }
        while (tk < -0.5 * GPS_CYCLE_DURATION) {
            tk += GPS_CYCLE_DURATION;
        }
        // Returns the time from ephemeris reference epoch
        return tk;
    }

    /**
     * Gets eccentric anomaly from mean anomaly.
     * <p>The algorithm used to solve the Kepler equation has been published in:
     * "Procedures for  solving Kepler's Equation", A. W. Odell and R. H. Gooding,
     * Celestial Mechanics 38 (1986) 307-334</p>
     * <p>It has been copied from the OREKIT library (KeplerianOrbit class).</p>
     *
     * @param mk the mean anomaly (rad)
     * @return the eccentric anomaly (rad)
     */
    private double getEccentricAnomaly(final double mk) {
        // reduce M to [-PI PI] interval
        final double reducedM = MathUtils.normalizeAngle(mk, 0.0);
        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        double ek;
        if (FastMath.abs(reducedM) < 1.0 / 6.0) {
            ek = reducedM + gpsOrbit.getE() * (FastMath.cbrt(6 * reducedM) - reducedM);
        } else {
            if (reducedM < 0) {
                final double w = FastMath.PI + reducedM;
                ek = reducedM + gpsOrbit.getE() * (A * w / (B - w) - FastMath.PI - reducedM);
            } else {
                final double w = FastMath.PI - reducedM;
                ek = reducedM + gpsOrbit.getE() * (FastMath.PI - A * w / (B - w) - reducedM);
            }
        }

        final double e1 = 1 - gpsOrbit.getE();
        final boolean noCancellationRisk = (e1 + ek * ek / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final double f;
            double fd;
            final double fdd  = gpsOrbit.getE() * FastMath.sin(ek);
            final double fddd = gpsOrbit.getE() * FastMath.cos(ek);
            if (noCancellationRisk) {
                f  = (ek - fdd) - reducedM;
                fd = 1 - fddd;
            } else {
                f  = eMeSinE(ek) - reducedM;
                final double s = FastMath.sin(0.5 * ek);
                fd = e1 + 2 * gpsOrbit.getE() * s * s;
            }
            final double dee = f * fd / (0.5 * f * fdd - fd * fd);

            // update eccentric anomaly, using expressions that limit underflow problems
            final double w = fd + 0.5 * dee * (fdd + dee * fddd / 3);
            fd += dee * (fdd + 0.5 * dee * fddd);
            ek -= (f - dee * (fd - w)) / fd;
        }

        // expand the result back to original range
        ek += mk - reducedM;

        // Returns the eccentric anomaly
        return ek;
    }

    /**
     * Accurate computation of E - e sin(E).
     *
     * @param E eccentric anomaly
     * @return E - e sin(E)
     */
    private double eMeSinE(final double E) {
        double x = (1 - gpsOrbit.getE()) * FastMath.sin(E);
        final double mE2 = -E * E;
        double term = E;
        double d    = 0;
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (double x0 = Double.NaN; x != x0;) {
            d += 2;
            term *= mE2 / (d * (d + 1));
            x0 = x;
            x = x - term;
        }
        return x;
    }

    /** Gets true anomaly from eccentric anomaly.
     *
     * @param ek the eccentric anomaly (rad)
     * @return the true anomaly (rad)
     */
    private double getTrueAnomaly(final double ek) {
        final double svk = FastMath.sin(ek) * FastMath.sqrt(1. - gpsOrbit.getE() * gpsOrbit.getE());
        final double cvk = FastMath.cos(ek) - gpsOrbit.getE();
        return FastMath.atan2(svk, cvk);
    }

    /**
     * Get the Earth gravity coefficient used for GPS propagation.
     * @return the Earth gravity coefficient.
     */
    public static double getMU() {
        return GPSOrbitalElements.GPS_MU;
    }

    /**
     * Gets the underlying GPS orbital elements.
     *
     * @return the underlying GPS orbital elements
     */
    public GPSOrbitalElements getGPSOrbitalElements() {
        return gpsOrbit;
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
     * Gets the Earth Centered Earth Fixed frame used to propagate GPS orbits according to the
     * <a href="http://www.gps.gov/technical/icwg/IS-GPS-200H.pdf">GPS Interface Specification</a>.
     * <p>This frame is assimilated to the WGS84 ECEF.</p>
     *
     * @return the ECEF frame
     */
    public Frame getECEF() {
        return ecef;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        throw new PropagationException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward)
            throws PropagationException {
        throw new PropagationException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) throws PropagationException {
        try {
            // Gets the PVCoordinates in ECEF frame
            final PVCoordinates pvaInECEF = getPVCoordinates(date);
            // Transforms the PVCoordinates to ECI frame
            final PVCoordinates pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
            // Returns the Cartesian orbit
            return new CartesianOrbit(pvaInECI, eci, date, GPSOrbitalElements.GPS_MU);
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }
}
