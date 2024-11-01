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

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianAnomalyUtility;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.GNSSOrbitalElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Common handling of {@link AbstractAnalyticalPropagator} methods for GNSS propagators.
 * <p>
 * This class allows to provide easily a subset of {@link AbstractAnalyticalPropagator} methods
 * for specific GNSS propagators.
 * </p>
 * @author Pascal Parraud
 */
public class GNSSPropagator extends AbstractAnalyticalPropagator {

    /** The GNSS propagation model used. */
    private final GNSSOrbitalElements orbitalElements;

    /** The spacecraft mass (kg). */
    private final double mass;

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
    GNSSPropagator(final GNSSOrbitalElements orbitalElements, final Frame eci,
                   final Frame ecef, final AttitudeProvider provider,
                   final double mass) {
        super(provider);
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
        final Orbit orbit = propagateOrbit(getStartDate());
        final Attitude attitude = provider.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        super.resetInitialState(new SpacecraftState(orbit, attitude, mass));
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
    public double getMU() {
        return orbitalElements.getMu();
    }

    /** Get the underlying GNSS propagation orbital elements.
     * @return the underlying GNSS orbital elements
     * @since 13.0
     */
    public GNSSOrbitalElements getOrbitalElements() {
        return orbitalElements;
    }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        // Create the harvester
        final GnssHarvester harvester = new GnssHarvester(this, stmName, initialStm, initialJacobianColumns);

        // Update the list of additional state provider
        addAdditionalStateProvider(harvester);
        // Return the configured harvester
        return harvester;
     }

    /** {@inheritDoc}
     * @since 13.0
     */
    @Override
    protected List<String> getJacobiansColumnsNames() {
        final List<String> columnsNames = new ArrayList<>();
        for (final ParameterDriver driver : orbitalElements.getParametersDrivers()) {
            if (driver.isSelected() && !columnsNames.contains(driver.getNamesSpanMap().getFirstSpan().getData())) {
                // As driver with same name should have same NamesSpanMap we only check if the first span is present,
                // if not we add all span names to columnsNames
                for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                    columnsNames.add(span.getData());
                }
            }
        }
        Collections.sort(columnsNames);
        return columnsNames;
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(final AbsoluteDate date) {
        // Gets the PVCoordinates in ECEF frame
        final PVCoordinates pvaInECEF = propagateInEcef(date);
        // Transforms the PVCoordinates to ECI frame
        final PVCoordinates pvaInECI = ecef.getTransformTo(eci, date).transformPVCoordinates(pvaInECEF);
        // Returns the Cartesian orbit
        return new CartesianOrbit(pvaInECI, eci, date, getMU());
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
        final UnivariateDerivative2 tk = new UnivariateDerivative2(getTk(date), 1.0, 0.0);
        // Mean anomaly
        final UnivariateDerivative2 mk = tk.multiply(orbitalElements.getMeanMotion()).add(orbitalElements.getM0());
        // Eccentric Anomaly
        final UnivariateDerivative2 e  = tk.newInstance(orbitalElements.getE());
        final UnivariateDerivative2 ek = FieldKeplerianAnomalyUtility.ellipticMeanToEccentric(e, mk);
        // True Anomaly
        final UnivariateDerivative2 vk =  FieldKeplerianAnomalyUtility.ellipticEccentricToTrue(e, ek);
        // Argument of Latitude
        final UnivariateDerivative2 phik    = vk.add(orbitalElements.getPa());
        final FieldSinCos<UnivariateDerivative2> cs2phi = FastMath.sinCos(phik.multiply(2));
        // Argument of Latitude Correction
        final UnivariateDerivative2 dphik = cs2phi.cos().multiply(orbitalElements.getCuc()).add(cs2phi.sin().multiply(orbitalElements.getCus()));
        // Radius Correction
        final UnivariateDerivative2 drk = cs2phi.cos().multiply(orbitalElements.getCrc()).add(cs2phi.sin().multiply(orbitalElements.getCrs()));
        // Inclination Correction
        final UnivariateDerivative2 dik = cs2phi.cos().multiply(orbitalElements.getCic()).add(cs2phi.sin().multiply(orbitalElements.getCis()));
        // Corrected Argument of Latitude
        final FieldSinCos<UnivariateDerivative2> csuk = FastMath.sinCos(phik.add(dphik));
        // Corrected Radius
        final UnivariateDerivative2 rk = ek.cos().multiply(-orbitalElements.getE()).add(1).multiply(orbitalElements.getSma()).add(drk);
        // Corrected Inclination
        final UnivariateDerivative2 ik  = tk.multiply(orbitalElements.getIDot()).add(orbitalElements.getI0()).add(dik);
        final UnivariateDerivative2 cik = ik.cos();
        // Positions in orbital plane
        final UnivariateDerivative2 xk = csuk.cos().multiply(rk);
        final UnivariateDerivative2 yk = csuk.sin().multiply(rk);
        // Corrected longitude of ascending node
        final double thetaDot = orbitalElements.getAngularVelocity();
        final FieldSinCos<UnivariateDerivative2> csomk =
            FastMath.sinCos(tk.multiply(orbitalElements.getOmegaDot() - thetaDot).
                            add(orbitalElements.getOmega0() - thetaDot * orbitalElements.getTime()));
        // returns the Earth-fixed coordinates
        final FieldVector3D<UnivariateDerivative2> positionWithDerivatives =
                        new FieldVector3D<>(xk.multiply(csomk.cos()).subtract(yk.multiply(csomk.sin()).multiply(cik)),
                                            xk.multiply(csomk.sin()).add(yk.multiply(csomk.cos()).multiply(cik)),
                                            yk.multiply(ik.sin()));
        return new PVCoordinates(new Vector3D(positionWithDerivatives.getX().getValue(),
                                              positionWithDerivatives.getY().getValue(),
                                              positionWithDerivatives.getZ().getValue()),
                                 new Vector3D(positionWithDerivatives.getX().getFirstDerivative(),
                                              positionWithDerivatives.getY().getFirstDerivative(),
                                              positionWithDerivatives.getZ().getFirstDerivative()),
                                 new Vector3D(positionWithDerivatives.getX().getSecondDerivative(),
                                              positionWithDerivatives.getY().getSecondDerivative(),
                                              positionWithDerivatives.getZ().getSecondDerivative()));
    }

    /**
     * Gets the duration from GNSS Reference epoch.
     * <p>This takes the GNSS week roll-over into account.</p>
     * @param date the considered date
     * @return the duration from GNSS orbit Reference epoch (s)
     */
    private double getTk(final AbsoluteDate date) {
        final double cycleDuration = orbitalElements.getCycleDuration();
        // Time from ephemeris reference epoch
        double tk = date.durationFrom(orbitalElements.getDate());
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

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return eci;
    }

    /** {@inheritDoc} */
    @Override
    protected double getMass(final AbsoluteDate date) {
        return mass;
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

    /** {@inheritDoc} */
    @Override
    protected void resetIntermediateState(final SpacecraftState state, final boolean forward) {
        throw new OrekitException(OrekitMessages.NON_RESETABLE_STATE);
    }

}
