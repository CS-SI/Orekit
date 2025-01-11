/* Copyright 2002-2024 Luc Maisonobe
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
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldAbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.FieldGnssOrbitalElements;
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
public class FieldGnssPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractAnalyticalPropagator<T> {

    /** The GNSS propagation model used. */
    private final FieldGnssOrbitalElements<T, ?> orbitalElements;

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
    FieldGnssPropagator(final FieldGnssOrbitalElements<T, ?> orbitalElements,
                        final Frame eci, final Frame ecef,
                        final AttitudeProvider provider, final T mass) {
        super(orbitalElements.getDate().getField(), provider);
        // Stores the GNSS orbital elements
        this.orbitalElements = orbitalElements;
        // Sets the start date as the date of the orbital elements
        setStartDate(orbitalElements.getDate());
        // Sets the mass
        this.mass = mass;
        // Sets the Earth Centered Inertial frame
        this.eci  = eci;
        // Sets the Earth Centered Earth Fixed frame
        this.ecef = ecef;
        // Sets initial state
        final T[] parameters = MathArrays.buildArray(orbitalElements.getDate().getField(), GNSSOrbitalElements.SIZE);
        parameters[GNSSOrbitalElements.TIME_INDEX]      = mass.newInstance(orbitalElements.getTime());
        parameters[GNSSOrbitalElements.I_DOT_INDEX]     = mass.newInstance(orbitalElements.getIDot());
        parameters[GNSSOrbitalElements.OMEGA_DOT_INDEX] = mass.newInstance(orbitalElements.getOmegaDot());
        parameters[GNSSOrbitalElements.CUC_INDEX]       = mass.newInstance(orbitalElements.getCuc());
        parameters[GNSSOrbitalElements.CUS_INDEX]       = mass.newInstance(orbitalElements.getCus());
        parameters[GNSSOrbitalElements.CRC_INDEX]       = mass.newInstance(orbitalElements.getCrc());
        parameters[GNSSOrbitalElements.CRS_INDEX]       = mass.newInstance(orbitalElements.getCrs());
        parameters[GNSSOrbitalElements.CIC_INDEX]       = mass.newInstance(orbitalElements.getCic());
        parameters[GNSSOrbitalElements.CIS_INDEX]       = mass.newInstance(orbitalElements.getCis());
        final FieldOrbit<T> orbit = propagateOrbit(getStartDate(), parameters);
        final FieldAttitude<T> attitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());

        // calling the method from base class because the one overridden below intentionally throws an exception
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
        return orbitalElements.getMu();
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
     * @param parameters propagation parameters
     * @return the GNSS SV PVCoordinates in {@link #getECEF() ECEF frame}
     */
    private FieldPVCoordinates<T> propagateInEcef(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // Duration from GNSS ephemeris Reference date
        final FieldUnivariateDerivative2<T> tk = new FieldUnivariateDerivative2<>(getTk(date),
                                                                                  date.getField().getOne(),
                                                                                  date.getField().getZero());
        // mean motion
        final T a          = orbitalElements.getSma();
        final T invA       = a.reciprocal();
        final T meanMotion = FastMath.sqrt(orbitalElements.getMu().multiply(invA)).multiply(invA);

        // Mean anomaly
        final FieldUnivariateDerivative2<T> mk = tk.multiply(meanMotion).add(orbitalElements.getM0());
        // Eccentric Anomaly
        final FieldUnivariateDerivative2<T> e  = tk.newInstance(orbitalElements.getE());
        final FieldUnivariateDerivative2<T> ek = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, mk);
        // True Anomaly
        final FieldUnivariateDerivative2<T> vk = FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, ek);
        // Argument of Latitude
        final FieldUnivariateDerivative2<T> phik    = vk.add(orbitalElements.getPa());
        final FieldSinCos<FieldUnivariateDerivative2<T>> cs2phi = FastMath.sinCos(phik.multiply(2));
        // Argument of Latitude Correction
        final FieldUnivariateDerivative2<T> dphik = cs2phi.cos().multiply(parameters[GNSSOrbitalElements.CUC_INDEX]).
                                                add(cs2phi.sin().multiply(parameters[GNSSOrbitalElements.CUS_INDEX]));
        // Radius Correction
        final FieldUnivariateDerivative2<T> drk = cs2phi.cos().multiply(parameters[GNSSOrbitalElements.CRC_INDEX]).
                                              add(cs2phi.sin().multiply(parameters[GNSSOrbitalElements.CRS_INDEX]));
        // Inclination Correction
        final FieldUnivariateDerivative2<T> dik = cs2phi.cos().multiply(parameters[GNSSOrbitalElements.CIC_INDEX]).
                                              add(cs2phi.sin().multiply(parameters[GNSSOrbitalElements.CIS_INDEX]));
        // Corrected Argument of Latitude
        final FieldSinCos<FieldUnivariateDerivative2<T>> csuk = FastMath.sinCos(phik.add(dphik));
        // Corrected Radius
        final FieldUnivariateDerivative2<T> rk = ek.cos().multiply(e.negate()).add(1).multiply(a).add(drk);
        // Corrected Inclination
        final FieldUnivariateDerivative2<T> ik  = tk.multiply(parameters[GNSSOrbitalElements.I_DOT_INDEX]).
                                                  add(orbitalElements.getI0()).add(dik);
        final FieldSinCos<FieldUnivariateDerivative2<T>> csik = FastMath.sinCos(ik);
        // Positions in orbital plane
        final FieldUnivariateDerivative2<T> xk = csuk.cos().multiply(rk);
        final FieldUnivariateDerivative2<T> yk = csuk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final FieldSinCos<FieldUnivariateDerivative2<T>> csomk =
            FastMath.sinCos(tk.multiply(parameters[GNSSOrbitalElements.OMEGA_DOT_INDEX].
                            subtract(orbitalElements.getAngularVelocity())).
                            add(orbitalElements.getOmega0()).
                            subtract(parameters[GNSSOrbitalElements.TIME_INDEX].multiply(orbitalElements.getAngularVelocity())));
        // returns the Earth-fixed coordinates
        final FieldVector3D<FieldUnivariateDerivative2<T>> positionWithDerivatives =
                        new FieldVector3D<>(xk.multiply(csomk.cos()).subtract(yk.multiply(csomk.sin()).multiply(csik.cos())),
                                            xk.multiply(csomk.sin()).add(yk.multiply(csomk.cos()).multiply(csik.cos())),
                                            yk.multiply(csik.sin()));
        return new FieldPVCoordinates<>(positionWithDerivatives);

    }

    /**
     * Gets the duration from GNSS Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     * @param date the considered date
     * @return the duration from GNSS orbit Reference epoch (s)
     */
    private T getTk(final FieldAbsoluteDate<T> date) {
        // Time from ephemeris reference epoch
        T tk = date.durationFrom(orbitalElements.getDate());
        // Adjusts the time to take roll over week into account
        while (tk.getReal() > 0.5 * orbitalElements.getCycleDuration()) {
            tk = tk.subtract(orbitalElements.getCycleDuration());
        }
        while (tk.getReal() < -0.5 * orbitalElements.getCycleDuration()) {
            tk = tk.add(orbitalElements.getCycleDuration());
        }
        // Returns the time from ephemeris reference epoch
        return tk;
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
