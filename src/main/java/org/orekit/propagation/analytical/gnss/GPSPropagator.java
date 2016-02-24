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
 * This class aims at computing GPS spacecraft position from a {@link GPSOrbitalElements GPS orbit}.
 * <p>The algorithm is defined at Table 20-IV from IS-GPS-200 document.</p>
 *
 * @author CS
 */
public class GPSPropagator extends AbstractAnalyticalPropagator {

    // Constants
    /** WGS 84 value of the earth's rotation rate in rad/s */
    private static final double GPS_AV = 7.2921151467e-5;

    /** Duration of the GPS cycle in seconds */
    private static final double GPS_CYCLE_DURATION = GPSOrbitalElements.GPS_WEEK_IN_SECONDS * GPSOrbitalElements.GPS_WEEK_NB;

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
    /** The GPS orbit used */
    private final GPSOrbitalElements gpsOrbit;

    /** The frame used for GPS orbit propagation. */
    private final Frame ecef;

    /** The spacecraft mass (kg). */
    private final double mass;


    /**
     * Constructor.
     *
     * @param orb the GPS orbit to use for propagation
     * @param attitudeProvider provider for attitude computation
     * @param mass spacecraft mass (kg)
     * @exception OrekitException if some specific error occurs
     */
    public GPSPropagator(final GPSOrbitalElements orb, final AttitudeProvider attitudeProvider,
                         final double mass) throws OrekitException {
        super(attitudeProvider);
        this.mass = mass;
        this.ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        // Stores the GPS orbit
        this.gpsOrbit = orb;
    }

    /**
     * Constructor with default mass.
     *
     * @param orb the GPS orbit to use for propagation
     * @param attitudeProvider provider for attitude computation
     * @exception OrekitException if some specific error occurs
     */
    public GPSPropagator(final GPSOrbitalElements orb, final AttitudeProvider attitudeProvider) throws OrekitException {
        this(orb, attitudeProvider, DEFAULT_MASS);
    }

    /**
     * Constructor with default attitude law.
     *
     * @param orb the GPS orbit to use for propagation
     * @param mass spacecraft mass (kg)
     * @exception OrekitException if some specific error occurs
     */
    public GPSPropagator(final GPSOrbitalElements orb, final double mass) throws OrekitException {
        this(orb, DEFAULT_LAW, mass);
    }

    /**
     * Constructor with default mass and default attitude law.
     *
     * @param orb the GPS orbit to use for propagation
     * @exception OrekitException if some specific error occurs
     */
    public GPSPropagator(final GPSOrbitalElements orb) throws OrekitException {
        this(orb, DEFAULT_LAW, DEFAULT_MASS);
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return ecef;
    }

    /**
     * Gets the underlying GPS orbit.
     *
     * @return the underlying GPS orbit
     */
    public GPSOrbitalElements getGPSOrbit() {
        return gpsOrbit;
    }

    /**
     * Gets the position of the GPS SV at the requested date from the GPS orbit.
     *
     * @param date the computation date
     * @return the position in GPS reference frame (WGS84 ECEF)
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date) {
        // Computes the position at the date
        final Vector3D pos = progate(date);
        return new PVCoordinates(pos, Vector3D.NaN, Vector3D.NaN);
    }

    /**
     * Gets the position of the GPS SV in ECEF.
     *
     * <p>The algorithm is defined at Table 20-IV from IS-GPS-200 document.</p>
     *
     * @param date the computation date
     * @return the GPS SV position in ECEF
     */
    private Vector3D progate(final AbsoluteDate date) {
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
        if (tk > 0.5 * GPS_CYCLE_DURATION) {
            tk -= GPS_CYCLE_DURATION;
        }
        if (tk < -0.5 * GPS_CYCLE_DURATION) {
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
            double f;
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
     * <p>This algorithm has been copied from the OREKIT library (KeplerianOrbit class).</p>
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

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
            throws PropagationException {
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
    protected Orbit propagateOrbit(final AbsoluteDate date)
            throws PropagationException {
        return new CartesianOrbit(getPVCoordinates(date), ecef, date, GPSOrbitalElements.GPS_MU);
    }
}
