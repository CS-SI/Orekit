/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.List;

/** Common handling of {@link FieldAbstractAnalyticalPropagator} methods for GNSS propagators.
 * <p>
 * This class allows to provide easily a subset of {@link FieldAbstractAnalyticalPropagator} methods
 * for specific GNSS propagators.
 * </p>
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public class FieldGnssPropagator<T extends CalculusFieldElement<T>>
    extends FieldAbstractAnalyticalPropagator<T> {

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
    private final GNSSOrbitalElements orbitalElements;

    /** The spacecraft mass (kg). */
    private final T mass;

    /** The ECI frame used for GNSS propagation. */
    private final Frame eci;

    /** The ECEF frame used for GNSS propagation. */
    private final Frame ecef;

    /**
     * Build a new instance.
     * @param orbitalElements GNSS orbital elements
     * @param eci Earth Centered Inertial frame
     * @param ecef Earth Centered Earth Fixed frame
     * @param provider Attitude provider
     * @param mass Satellite mass (kg)
     */
    FieldGnssPropagator(final GNSSOrbitalElements orbitalElements, final Frame eci,
                        final Frame ecef, final AttitudeProvider provider,
                        final T mass) {
        super(mass.getField(), provider);
        // Stores the GNSS propagation model
        this.orbitalElements = orbitalElements;
        // Sets the start date as the date of the orbital elements
        setStartDate(new FieldAbsoluteDate<>(mass.getField(), orbitalElements.getDate()));
        // Sets the mass
        this.mass = mass;
        // Sets the Earth Centered Inertial frame
        this.eci  = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
        // Sets initial state
        final FieldOrbit<T> orbit = propagateOrbit(getStartDate(), getParameters(mass.getField()));
        final FieldAttitude<T> attitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        super.resetInitialState(new FieldSpacecraftState<>(orbit, attitude, mass));
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return orbitalElements.getParametersDrivers();
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

    /**
     * Gets the Earth gravity coefficient used for GNSS propagation.
     *
     * @return the Earth gravity coefficient.
     */
    public T getMU() {
        return mass.newInstance(orbitalElements.getMu());
    }

    /** {@inheritDoc} */
    @Override
    protected FieldOrbit<T> propagateOrbit(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // Gets the PVCoordinates in ECEF frame
        final FieldPVCoordinates<T> pvaInECEF = propagateInEcef(date, parameters);
        // Transforms the PVCoordinates to ECI frame
        final FieldPVCoordinates<T> pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new FieldCartesianOrbit<>(pvaInECI, eci, date, getMU());
    }

    /**
     * Gets the PVCoordinates of the GNSS SV in {@link #getECEF() ECEF frame}.
     *
     * <p>The algorithm uses automatic differentiation to compute velocity and
     * acceleration.</p>
     *
     * @param date the computation date
     * @param parameters model parameters
     * @return the GNSS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    private FieldPVCoordinates<T> propagateInEcef(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // Duration from GNSS ephemeris Reference date
        final FieldUnivariateDerivative2<T> tk = new FieldUnivariateDerivative2<>(getTk(date),
                                                                                  date.getField().getOne(),
                                                                                  date.getField().getZero());
        // mean motion
        final FieldUnivariateDerivative2<T> invA =
            FastMath.abs(new FieldUnivariateDerivative2<>(parameters[GNSSOrbitalElements.SMA_INDEX],
                                                          date.getField().getOne(),
                                                          date.getField().getZero())).
                reciprocal();
        final FieldUnivariateDerivative2<T> meanMotion =
            FastMath.sqrt(invA.multiply(orbitalElements.getMu())).multiply(invA);

        // Mean anomaly
        final FieldUnivariateDerivative2<T> mk = tk.multiply(meanMotion).add(parameters[GNSSOrbitalElements.M0_INDEX]);
        // Eccentric Anomaly
        final FieldUnivariateDerivative2<T> ek = getEccentricAnomaly(parameters[GNSSOrbitalElements.E_INDEX], mk);
        // True Anomaly
        final FieldUnivariateDerivative2<T> vk =  getTrueAnomaly(parameters[GNSSOrbitalElements.E_INDEX], ek);
        // Argument of Latitude
        final FieldUnivariateDerivative2<T> phik    = vk.add(parameters[GNSSOrbitalElements.PA_INDEX]);
        final FieldUnivariateDerivative2<T> twoPhik = phik.multiply(2);
        final FieldUnivariateDerivative2<T> c2phi   = twoPhik.cos();
        final FieldUnivariateDerivative2<T> s2phi   = twoPhik.sin();
        // Argument of Latitude Correction
        final FieldUnivariateDerivative2<T> dphik = c2phi.multiply(parameters[GNSSOrbitalElements.CUC_INDEX]).
                                                add(s2phi.multiply(parameters[GNSSOrbitalElements.CUS_INDEX]));
        // Radius Correction
        final FieldUnivariateDerivative2<T> drk = c2phi.multiply(parameters[GNSSOrbitalElements.CRC_INDEX]).
                                              add(s2phi.multiply(parameters[GNSSOrbitalElements.CRS_INDEX]));
        // Inclination Correction
        final FieldUnivariateDerivative2<T> dik = c2phi.multiply(parameters[GNSSOrbitalElements.CIC_INDEX]).
                                              add(s2phi.multiply(parameters[GNSSOrbitalElements.CIS_INDEX]));
        // Corrected Argument of Latitude
        final FieldUnivariateDerivative2<T> uk = phik.add(dphik);
        // Corrected Radius
        final FieldUnivariateDerivative2<T> rk = ek.cos().multiply(parameters[GNSSOrbitalElements.E_INDEX].negate()).add(1).
                                                 multiply(parameters[GNSSOrbitalElements.SMA_INDEX]).add(drk);
        // Corrected Inclination
        final FieldUnivariateDerivative2<T> ik  = tk.multiply(parameters[GNSSOrbitalElements.IO_DOT_INDEX]).
                                                  add(parameters[GNSSOrbitalElements.I0_INDEX]).add(dik);
        final FieldUnivariateDerivative2<T> cik = ik.cos();
        // Positions in orbital plane
        final FieldUnivariateDerivative2<T> xk = uk.cos().multiply(rk);
        final FieldUnivariateDerivative2<T> yk = uk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final FieldUnivariateDerivative2<T> omk = tk.multiply(parameters[GNSSOrbitalElements.OMEGA_DOT_INDEX].
                                                              subtract(orbitalElements.getAngularVelocity())).
                                                  add(parameters[GNSSOrbitalElements.OM0_INDEX].
                                                                 subtract(parameters[GNSSOrbitalElements.TIME_INDEX].
                                                                          multiply(orbitalElements.getAngularVelocity())));
        final FieldUnivariateDerivative2<T> comk = omk.cos();
        final FieldUnivariateDerivative2<T> somk = omk.sin();
        // returns the Earth-fixed coordinates
        final FieldVector3D<FieldUnivariateDerivative2<T>> positionWithDerivatives =
                        new FieldVector3D<>(xk.multiply(comk).subtract(yk.multiply(somk).multiply(cik)),
                                            xk.multiply(somk).add(yk.multiply(comk).multiply(cik)),
                                            yk.multiply(ik.sin()));
        return new FieldPVCoordinates<>(new FieldVector3D<>(positionWithDerivatives.getX().getValue(),
                                                            positionWithDerivatives.getY().getValue(),
                                                            positionWithDerivatives.getZ().getValue()),
                                        new FieldVector3D<>(positionWithDerivatives.getX().getFirstDerivative(),
                                                            positionWithDerivatives.getY().getFirstDerivative(),
                                                            positionWithDerivatives.getZ().getFirstDerivative()),
                                        new FieldVector3D<>(positionWithDerivatives.getX().getSecondDerivative(),
                                                            positionWithDerivatives.getY().getSecondDerivative(),
                                                            positionWithDerivatives.getZ().getSecondDerivative()));
    }

    /**
     * Gets the duration from GNSS Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     * @param date the considered date
     * @return the duration from GNSS orbit Reference epoch (s)
     */
    private T getTk(final FieldAbsoluteDate<T> date) {
        final double cycleDuration = orbitalElements.getCycleDuration();
        // Time from ephemeris reference epoch
        T tk = date.durationFrom(orbitalElements.getDate());
        // Adjusts the time to take roll over week into account
        while (tk.getReal() > 0.5 * cycleDuration) {
            tk = tk.subtract(cycleDuration);
        }
        while (tk.getReal() < -0.5 * cycleDuration) {
            tk = tk.add(cycleDuration);
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
     * @param e the eccentricity
     * @param mk the mean anomaly (rad)
     * @return the eccentric anomaly (rad)
     */
    private FieldUnivariateDerivative2<T> getEccentricAnomaly(final T e, final FieldUnivariateDerivative2<T> mk) {

        // reduce M to [-PI PI] interval
        final FieldUnivariateDerivative2<T> reducedM =
            new FieldUnivariateDerivative2<>(MathUtils.normalizeAngle(mk.getValue(), mk.getValue().getField().getZero()),
                                             mk.getFirstDerivative(),
                                             mk.getSecondDerivative());

        // compute start value according to A. W. Odell and R. H. Gooding S12 starter
        FieldUnivariateDerivative2<T> ek;
        if (FastMath.abs(reducedM.getValue().getReal()) < 1.0 / 6.0) {
            if (FastMath.abs(reducedM.getValue().getReal()) < Precision.SAFE_MIN) {
                // this is an Orekit change to the S12 starter.
                // If reducedM is 0.0, the derivative of cbrt is infinite which induces NaN appearing later in
                // the computation. As in this case E and M are almost equal, we initialize ek with reducedM
                ek = reducedM;
            } else {
                // this is the standard S12 starter
                ek = reducedM.add(reducedM.multiply(6).cbrt().subtract(reducedM).multiply(e));
            }
        } else {
            if (reducedM.getValue().getReal() < 0) {
                final FieldUnivariateDerivative2<T> w = reducedM.add(FastMath.PI);
                ek = reducedM.add(w.multiply(-A).divide(w.subtract(B)).subtract(FastMath.PI).subtract(reducedM).multiply(e));
            } else {
                final FieldUnivariateDerivative2<T> minusW = reducedM.subtract(FastMath.PI);
                ek = reducedM.add(minusW.multiply(A).divide(minusW.add(B)).add(FastMath.PI).subtract(reducedM).multiply(e));
            }
        }

        final T e1 = e.negate().add(1);
        final boolean noCancellationRisk = (e1.getReal() + ek.getValue().getReal() * ek.getValue().getReal() / 6) >= 0.1;

        // perform two iterations, each consisting of one Halley step and one Newton-Raphson step
        for (int j = 0; j < 2; ++j) {
            final FieldUnivariateDerivative2<T> f;
            FieldUnivariateDerivative2<T> fd;
            final FieldUnivariateDerivative2<T> fdd  = ek.sin().multiply(e);
            final FieldUnivariateDerivative2<T> fddd = ek.cos().multiply(e);
            if (noCancellationRisk) {
                f  = ek.subtract(fdd).subtract(reducedM);
                fd = fddd.subtract(1).negate();
            } else {
                f  = eMeSinE(e, ek).subtract(reducedM);
                final FieldUnivariateDerivative2<T> s = ek.multiply(0.5).sin();
                fd = s.multiply(s).multiply(e.multiply(2)).add(e1);
            }
            final FieldUnivariateDerivative2<T> dee = f.multiply(fd).divide(f.multiply(0.5).multiply(fdd).subtract(fd.multiply(fd)));

            // update eccentric anomaly, using expressions that limit underflow problems
            final FieldUnivariateDerivative2<T> w = fd.add(dee.multiply(0.5).multiply(fdd.add(dee.multiply(fdd).divide(3))));
            fd = fd.add(dee.multiply(fdd.add(dee.multiply(0.5).multiply(fdd))));
            ek = ek.subtract(f.subtract(dee.multiply(fd.subtract(w))).divide(fd));
        }

        // expand the result back to original range
        ek = ek.add(mk.getValue().subtract(reducedM.getValue()));

        // Returns the eccentric anomaly
        return ek;
    }

    /**
     * Accurate computation of E - e sin(E).
     *
     * @param e the eccentricity
     * @param E eccentric anomaly
     * @return E - e sin(E)
     */
    private FieldUnivariateDerivative2<T> eMeSinE(final T e, final FieldUnivariateDerivative2<T> E) {
        FieldUnivariateDerivative2<T> x = E.sin().multiply(e.negate().add(1));
        final FieldUnivariateDerivative2<T> mE2 = E.negate().multiply(E);
        FieldUnivariateDerivative2<T> term = E;
        FieldUnivariateDerivative2<T> d    = E.getField().getZero();
        // the inequality test below IS intentional and should NOT be replaced by a check with a small tolerance
        for (FieldUnivariateDerivative2<T> x0 = d.add(Double.NaN);
             !Double.valueOf(x.getValue().getReal()).equals(x0.getValue().getReal());) {
            d = d.add(2);
            term = term.multiply(mE2.divide(d.multiply(d.add(1))));
            x0 = x;
            x = x.subtract(term);
        }
        return x;
    }

    /** Gets true anomaly from eccentric anomaly.
     *
     * @param e the eccentricity
     * @param ek the eccentric anomaly (rad)
     * @return the true anomaly (rad)
     */
    private FieldUnivariateDerivative2<T> getTrueAnomaly(final T e, final FieldUnivariateDerivative2<T> ek) {
        final FieldUnivariateDerivative2<T> svk = ek.sin().multiply(FastMath.sqrt(e.square().negate().add(1)));
        final FieldUnivariateDerivative2<T> cvk = ek.cos().subtract(e);
        return svk.atan2(cvk);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    @Override
    protected T getMass(final FieldAbsoluteDate<T> date) {
        return mass;
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final FieldSpacecraftState<T> state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

}
