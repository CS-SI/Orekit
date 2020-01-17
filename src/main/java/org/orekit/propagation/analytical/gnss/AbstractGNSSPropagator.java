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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Common handling of {@link AbstractAnalyticalPropagator} methods for GNSS propagators.
 * <p>
 * This abstract class allows to provide easily a subset of {@link AbstractAnalyticalPropagator} methods
 * for specific GNSS propagators.
 * </p>
 * @author Pascal Parraud
 */
public abstract class AbstractGNSSPropagator extends AbstractAnalyticalPropagator {

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

    /** The GNSS orbital elements used. */
    private final GNSSOrbitalElements gnssOrbit;

    /** Mean angular velocity of the Earth. */
    private final double av;

    /** Duration of the GNSS cycle in seconds. */
    private final double cycleDuration;

    /** The spacecraft mass (kg). */
    private final double mass;

    /** The Earth gravity coefficient used for GNSS propagation. */
    private final double mu;

    /** The ECI frame used for GNSS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GNSS propagation. */
    private final Frame ecef;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /** Build a new instance.
     * @param gnssOrbit the common GNSS orbital elements to be used by the Abstract GNSS propagator
     * @param attitudeProvider provider for attitude computation
     * @param eci the ECI frame used for GNSS propagation
     * @param ecef the ECEF frame used for GNSS propagation
     * @param mass the spacecraft mass (kg)
     * @param av mean angular velocity of the Earth (rad/s)
     * @param cycleDuration duration of the GNSS cycle in seconds
     * @param mu the Earth gravity coefficient used for GNSS propagation
     */
    protected AbstractGNSSPropagator(final GNSSOrbitalElements gnssOrbit, final AttitudeProvider attitudeProvider,
                                     final Frame eci, final Frame ecef, final double mass,
                                     final double av, final double cycleDuration, final double mu) {
        super(attitudeProvider);
        this.gnssOrbit     = gnssOrbit;
        this.av            = av;
        this.cycleDuration = cycleDuration;
        this.mass          = mass;
        this.mu            = mu;
        this.factory       = new DSFactory(1, 2);
        // Sets the Earth Centered Inertial frame
        this.eci  = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
        // Sets the start date as the date of the orbital elements
        setStartDate(gnssOrbit.getDate());
    }

    /** Get the duration from GNSS Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     * @param date the considered date
     * @return the duration from GNSS orbit Reference epoch (s)
     */
    private double getTk(final AbsoluteDate date) {
        // Time from ephemeris reference epoch
        double tk = date.durationFrom(gnssOrbit.getDate());
        // Adjusts the time to take roll over week into account
        while (tk > 0.5 * cycleDuration) {
            tk -= cycleDuration;
        }
        while (tk < -0.5 * cycleDuration) {
            tk += cycleDuration;
        }
        // Returns the time from ephemeris reference epoch
        return tk;
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
        // Duration from GNSS ephemeris Reference date
        final DerivativeStructure tk = factory.variable(0, getTk(date));
        // Mean anomaly
        final DerivativeStructure mk = tk.multiply(gnssOrbit.getMeanMotion()).add(gnssOrbit.getM0());
        // Eccentric Anomaly
        final DerivativeStructure ek = getEccentricAnomaly(mk);
        // True Anomaly
        final DerivativeStructure vk =  getTrueAnomaly(ek);
        // Argument of Latitude
        final DerivativeStructure phik    = vk.add(gnssOrbit.getPa());
        final DerivativeStructure twoPhik = phik.multiply(2);
        final DerivativeStructure c2phi   = twoPhik.cos();
        final DerivativeStructure s2phi   = twoPhik.sin();
        // Argument of Latitude Correction
        final DerivativeStructure dphik = c2phi.multiply(gnssOrbit.getCuc()).add(s2phi.multiply(gnssOrbit.getCus()));
        // Radius Correction
        final DerivativeStructure drk = c2phi.multiply(gnssOrbit.getCrc()).add(s2phi.multiply(gnssOrbit.getCrs()));
        // Inclination Correction
        final DerivativeStructure dik = c2phi.multiply(gnssOrbit.getCic()).add(s2phi.multiply(gnssOrbit.getCis()));
        // Corrected Argument of Latitude
        final DerivativeStructure uk = phik.add(dphik);
        // Corrected Radius
        final DerivativeStructure rk = ek.cos().multiply(-gnssOrbit.getE()).add(1).multiply(gnssOrbit.getSma()).add(drk);
        // Corrected Inclination
        final DerivativeStructure ik  = tk.multiply(gnssOrbit.getIDot()).add(gnssOrbit.getI0()).add(dik);
        final DerivativeStructure cik = ik.cos();
        // Positions in orbital plane
        final DerivativeStructure xk = uk.cos().multiply(rk);
        final DerivativeStructure yk = uk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final DerivativeStructure omk = tk.multiply(gnssOrbit.getOmegaDot() - av).
                                        add(gnssOrbit.getOmega0() - av * gnssOrbit.getTime());
        final DerivativeStructure comk = omk.cos();
        final DerivativeStructure somk = omk.sin();
        // returns the Earth-fixed coordinates
        final FieldVector3D<DerivativeStructure> positionwithDerivatives =
                        new FieldVector3D<>(xk.multiply(comk).subtract(yk.multiply(somk).multiply(cik)),
                                            xk.multiply(somk).add(yk.multiply(comk).multiply(cik)),
                                            yk.multiply(ik.sin()));
        return new PVCoordinates(positionwithDerivatives);
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
        final DerivativeStructure reducedM = mk.getFactory().build(mlDerivatives);

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        DerivativeStructure ek;
        if (FastMath.abs(reducedM.getValue()) < 1.0 / 6.0) {
            if (FastMath.abs(reducedM.getValue()) < Precision.SAFE_MIN) {
                // this is an Orekit change to the S12 starter.
                // If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN appearing later in
                // the computation. As in this case E and M are almost equal, we initialize ek with reducedM
                ek = reducedM;
            } else {
                // this is the standard S12 starter
                ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(gnssOrbit.getE()));
            }
        } else {
            if (reducedM.getValue() < 0) {
                final DerivativeStructure w = reducedM.add(FastMath.PI);
                ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM).multiply(gnssOrbit.getE()));
            } else {
                final DerivativeStructure minusW = reducedM.subtract(FastMath.PI);
                ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM).multiply(gnssOrbit.getE()));
            }
        }

        final double e1 = 1 - gnssOrbit.getE();
        final boolean noCancellationRisk = (e1 + ek.getValue() * ek.getValue() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final DerivativeStructure f;
            DerivativeStructure fd;
            final DerivativeStructure fdd  = ek.sin().multiply(gnssOrbit.getE());
            final DerivativeStructure fddd = ek.cos().multiply(gnssOrbit.getE());
            if (noCancellationRisk) {
                f  = ek.subtract(fdd).subtract(reducedM);
                fd = fddd.subtract(1).negate();
            } else {
                f  = eMeSinE(ek).subtract(reducedM);
                final DerivativeStructure s = ek.multiply(0.5).sin();
                fd = s.multiply(s).multiply(2 * gnssOrbit.getE()).add(e1);
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
        DerivativeStructure x = E.sin().multiply(1 - gnssOrbit.getE());
        final DerivativeStructure mE2 = E.negate().multiply(E);
        DerivativeStructure term = E;
        DerivativeStructure d    = E.getField().getZero();
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (DerivativeStructure x0 = d.add(Double.NaN); !Double.valueOf(x.getValue()).equals(Double.valueOf(x0.getValue()));) {
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
        final DerivativeStructure svk = ek.sin().multiply(FastMath.sqrt(1. - gnssOrbit.getE() * gnssOrbit.getE()));
        final DerivativeStructure cvk = ek.cos().subtract(gnssOrbit.getE());
        return svk.atan2(cvk);
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
     * Get the Earth gravity coefficient used for GNSS propagation.
     * @return the Earth gravity coefficient.
     */
    public double getMU() {
        return mu;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
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
     * Gets the Earth Centered Earth Fixed frame used to propagate GNSS orbits according to the
     * Interface Control Document.
     *
     * @return the ECEF frame
     */
    public Frame getECEF() {
        return ecef;
    }

}
