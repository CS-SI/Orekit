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

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
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
         *  {@link org.orekit.propagation.Propagator#DEFAULT_LAW DEFAULT_LAW}.<br>
         * The mass is set by default to the
         *  {@link org.orekit.propagation.Propagator#DEFAULT_MASS DEFAULT_MASS}.<br>
         * The ECI frame is set by default to the
         *  {@link org.orekit.frames.Predefined#EME2000 EME2000 frame}.<br>
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
     * Gets the PVCoordinates of the GPS SV in {@link #getECEF() ECEF frame}.
     *
     * <p>The algorithm is defined at Table 20-IV from IS-GPS-200 document,
     * with automatic differentiation added to compute velocity and
     * acceleration.</p>
     *
     * @param date the computation date
     * @return the GPS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    public PVCoordinates propagateInEcef(final AbsoluteDate date) {
        // Duration from GPS ephemeris Reference date
        final DerivativeStructure tk = new DerivativeStructure(1, 2, 0, getTk(date));
        // Mean anomaly
        final DerivativeStructure mk = tk.multiply(gpsOrbit.getMeanMotion()).add(gpsOrbit.getM0());
        // Eccentric Anomaly
        final DerivativeStructure ek = getEccentricAnomaly(mk);
        // True Anomaly
        final DerivativeStructure vk =  getTrueAnomaly(ek);
        // Argument of Latitude
        final DerivativeStructure phik    = vk.add(gpsOrbit.getPa());
        final DerivativeStructure twoPhik = phik.multiply(2);
        final DerivativeStructure c2phi   = twoPhik.cos();
        final DerivativeStructure s2phi   = twoPhik.sin();
        // Argument of Latitude Correction
        final DerivativeStructure dphik = c2phi.multiply(gpsOrbit.getCuc()).add(s2phi.multiply(gpsOrbit.getCus()));
        // Radius Correction
        final DerivativeStructure drk = c2phi.multiply(gpsOrbit.getCrc()).add(s2phi.multiply(gpsOrbit.getCrs()));
        // Inclination Correction
        final DerivativeStructure dik = c2phi.multiply(gpsOrbit.getCic()).add(s2phi.multiply(gpsOrbit.getCis()));
        // Corrected Argument of Latitude
        final DerivativeStructure uk = phik.add(dphik);
        // Corrected Radius
        final DerivativeStructure rk = ek.cos().multiply(-gpsOrbit.getE()).add(1).multiply(gpsOrbit.getSma()).add(drk);
        // Corrected Inclination
        final DerivativeStructure ik  = tk.multiply(gpsOrbit.getIDot()).add(gpsOrbit.getI0()).add(dik);
        final DerivativeStructure cik = ik.cos();
        // Positions in orbital plane
        final DerivativeStructure xk = uk.cos().multiply(rk);
        final DerivativeStructure yk = uk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final DerivativeStructure omk = tk.multiply(gpsOrbit.getOmegaDot() - GPS_AV).
                                        add(gpsOrbit.getOmega0() - GPS_AV * gpsOrbit.getTime());
        final DerivativeStructure comk = omk.cos();
        final DerivativeStructure somk = omk.sin();
        // returns the Earth-fixed coordinates
        final FieldVector3D<DerivativeStructure> positionwithDerivatives =
                        new FieldVector3D<DerivativeStructure>(xk.multiply(comk).subtract(yk.multiply(somk).multiply(cik)),
                                                               xk.multiply(somk).add(yk.multiply(comk).multiply(cik)),
                                                               yk.multiply(ik.sin()));
        return new PVCoordinates(positionwithDerivatives);
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
    private DerivativeStructure getEccentricAnomaly(final DerivativeStructure mk) {

        // reduce M to [-PI PI] interval
        final double[] mlDerivatives = mk.getAllDerivatives();
        mlDerivatives[0] = MathUtils.normalizeAngle(mlDerivatives[0], 0.0);
        final DerivativeStructure reducedM = new DerivativeStructure(mk.getFreeParameters(),
                                                                     mk.getOrder(),
                                                                     mlDerivatives);

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        DerivativeStructure ek;
        if (FastMath.abs(reducedM.getValue()) < 1.0 / 6.0) {
            ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(gpsOrbit.getE()));
        } else {
            if (reducedM.getValue() < 0) {
                final DerivativeStructure w = reducedM.add(FastMath.PI);
                ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM).multiply(gpsOrbit.getE()));
            } else {
                final DerivativeStructure minusW = reducedM.subtract(FastMath.PI);
                ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM).multiply(gpsOrbit.getE()));
            }
        }

        final double e1 = 1 - gpsOrbit.getE();
        final boolean noCancellationRisk = (e1 + ek.getValue() * ek.getValue() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final DerivativeStructure f;
            DerivativeStructure fd;
            final DerivativeStructure fdd  = ek.sin().multiply(gpsOrbit.getE());
            final DerivativeStructure fddd = ek.cos().multiply(gpsOrbit.getE());
            if (noCancellationRisk) {
                f  = ek.subtract(fdd).subtract(reducedM);
                fd = fddd.subtract(1).negate();
            } else {
                f  = eMeSinE(ek).subtract(reducedM);
                final DerivativeStructure s = ek.multiply(0.5).sin();
                fd = s.multiply(s).multiply(2 * gpsOrbit.getE()).add(e1);
            }
            final DerivativeStructure dee = f.multiply(fd).divide(f.multiply(0.5).multiply(fdd).subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow problems
            final DerivativeStructure w = fd.add(dee.multiply(0.5).multiply(fdd.add(dee.multiply(fdd).divide(3))));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
            ek = ek.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));
        }

        // expand the result back to original range
        ek = ek.add(mk.getValue() - reducedM.getValue());

        // Returns the eccentric anomaly
        return ek;
    }

    /**
     * Accurate computation of E - e sin(E).
     *
     * @param E eccentric anomaly
     * @return E - e sin(E)
     */
    private DerivativeStructure eMeSinE(final DerivativeStructure E) {
        DerivativeStructure x = E.sin().multiply(1 - gpsOrbit.getE());
        final DerivativeStructure mE2 = E.negate().multiply(E);
        DerivativeStructure term = E;
        DerivativeStructure d    = E.getField().getZero();
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (DerivativeStructure x0 = d.add(Double.NaN); x.getValue() != x0.getValue();) {
            d = d.add(2);
            term = term.multiply(mE2.divide(d.multiply(d.add(1))));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /** Gets true anomaly from eccentric anomaly.
     *
     * @param ek the eccentric anomaly (rad)
     * @return the true anomaly (rad)
     */
    private DerivativeStructure getTrueAnomaly(final DerivativeStructure ek) {
        final DerivativeStructure svk = ek.sin().multiply(FastMath.sqrt(1. - gpsOrbit.getE() * gpsOrbit.getE()));
        final DerivativeStructure cvk = ek.cos().subtract(gpsOrbit.getE());
        return svk.atan2(cvk);
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
    public void resetInitialState(final SpacecraftState state) throws OrekitException {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward)
        throws OrekitException {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    protected Orbit propagateOrbit(final AbsoluteDate date) throws OrekitException {
        // Gets the PVCoordinates in ECEF frame
        final PVCoordinates pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final PVCoordinates pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new CartesianOrbit(pvaInECI, eci, date, GPSOrbitalElements.GPS_MU);
    }
}
