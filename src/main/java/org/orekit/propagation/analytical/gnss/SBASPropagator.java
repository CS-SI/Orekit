/* Copyright 2002-2023 CS GROUP
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
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.AbstractAnalyticalPropagator;
import org.orekit.propagation.analytical.gnss.data.SBASOrbitalElements;
import org.orekit.time.AbsoluteDate;
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

    /**
     * Private constructor.
     * @param sbasOrbit Glonass orbital elements
     * @param eci Earth Centered Inertial frame
     * @param ecef Earth Centered Earth Fixed frame
     * @param provider Attitude provider
     * @param mass Satellite mass (kg)
     * @param mu Earth's gravity coefficient used for SBAS propagation
     */
    SBASPropagator(final SBASOrbitalElements sbasOrbit, final Frame eci,
                   final Frame ecef, final AttitudeProvider provider,
                   final double mass, final double mu) {
        super(provider);
        // Stores the SBAS orbital elements
        this.sbasOrbit = sbasOrbit;
        // Sets the start date as the date of the orbital elements
        setStartDate(sbasOrbit.getDate());
        // Sets the mu
        this.mu = mu;
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
        final UnivariateDerivative2 dt = new UnivariateDerivative2(getDT(date), 1.0, 0.0);
        // Satellite coordinates
        final UnivariateDerivative2 x = dt.multiply(dt.multiply(0.5 * sbasOrbit.getXDotDot()).add(sbasOrbit.getXDot())).add(sbasOrbit.getX());
        final UnivariateDerivative2 y = dt.multiply(dt.multiply(0.5 * sbasOrbit.getYDotDot()).add(sbasOrbit.getYDot())).add(sbasOrbit.getY());
        final UnivariateDerivative2 z = dt.multiply(dt.multiply(0.5 * sbasOrbit.getZDotDot()).add(sbasOrbit.getZDot())).add(sbasOrbit.getZ());
        // Returns the Earth-fixed coordinates
        final FieldVector3D<UnivariateDerivative2> positionwithDerivatives =
                        new FieldVector3D<>(x, y, z);
        return new PVCoordinates(new Vector3D(positionwithDerivatives.getX().getValue(),
                                              positionwithDerivatives.getY().getValue(),
                                              positionwithDerivatives.getZ().getValue()),
                                 new Vector3D(positionwithDerivatives.getX().getFirstDerivative(),
                                              positionwithDerivatives.getY().getFirstDerivative(),
                                              positionwithDerivatives.getZ().getFirstDerivative()),
                                 new Vector3D(positionwithDerivatives.getX().getSecondDerivative(),
                                              positionwithDerivatives.getY().getSecondDerivative(),
                                              positionwithDerivatives.getZ().getSecondDerivative()));
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
