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
package org.orekit.forces;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** This class implements a {@link AbstractParametricAcceleration parametric acceleration}
 * with harmonic signed amplitude.
 * @since 9.0
 * @author Luc Maisonobe
 */
public class HarmonicParametricAcceleration extends AbstractParametricAcceleration {

    /** Amplitude scaling factor.
     * <p>
     * 2⁻²⁰ is the order of magnitude of third body perturbing acceleration.
     * </p>
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double AMPLITUDE_SCALE = FastMath.scalb(1.0, -20);

    /** Phase scaling factor.
     * <p>
     * 2⁻²³ is the order of magnitude of an angle corresponding to one meter along
     * track for a Low Earth Orbiting satellite.
     * </p>
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double PHASE_SCALE = FastMath.scalb(1.0, -23);

    /** Drivers for the parameters. */
    private final ParameterDriver[] drivers;

    /** Reference date for computing phase. */
    private AbsoluteDate referenceDate;

    /** Angular frequency ω = 2kπ/T. */
    private final double omega;

    /** Simple constructor.
     * <p>
     * The signed amplitude of the acceleration is γ sin[2kπ(t-t₀)/T + φ], where
     * γ is parameter {@code 0} and represents the full amplitude, t is current
     * date, t₀ is reference date, {@code T} is fundamental period, {@code k} is
     * harmonic multiplier, and φ is parameter {@code 1} and represents phase at t₀.
     * The value t-t₀ is in seconds.
     * </p>
     * <p>
     * The fundamental period {@code T} is often set to the Keplerian period of the
     * orbit and the harmonic multiplier {@code k} is often set to 1 or 2. The model
     * has two parameters, one for the full amplitude and one for the phase at reference
     * date.
     * </p>
     * <p>
     * The two parameters for this model are the full amplitude (parameter 0) and the
     * phase at reference date (parameter 1). Their reference values (used also as the
     * initial values) are both set to 0. User can change them before starting the
     * propagation (or orbit determination) by calling {@link #getParametersDrivers()}
     * and {@link ParameterDriver#setValue(double)}.
     * </p>
     * @param direction acceleration direction in defining frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing phase, if null
     * the reference date will be automatically set at propagation start
     * @param fundamentalPeriod fundamental period (typically set to initial orbit
     * {@link org.orekit.orbits.Orbit#getKeplerianPeriod() Keplerian period})
     * @param harmonicMultiplier multiplier to compute harmonic period from
     * fundamental period)
     */
    public HarmonicParametricAcceleration(final Vector3D direction, final boolean isInertial,
                                          final String prefix, final AbsoluteDate referenceDate,
                                          final double fundamentalPeriod, final int harmonicMultiplier) {
        this(direction, isInertial, null, prefix, referenceDate,
             fundamentalPeriod, harmonicMultiplier);
    }

    /** Simple constructor.
     * <p>
     * The signed amplitude of the acceleration is γ sin[2kπ(t-t₀)/T + φ], where
     * γ is parameter {@code 0} and represents the full amplitude, t is current
     * date, t₀ is reference date, {@code T} is fundamental period, {@code k} is
     * harmonic multiplier, and φ is parameter {@code 1} and represents phase at t₀.
     * The value t-t₀ is in seconds.
     * </p>
     * <p>
     * The fundamental period {@code T} is often set to the Keplerian period of the
     * orbit and the harmonic multiplier {@code k} is often set to 1 or 2. The model
     * has two parameters, one for the full amplitude and one for the phase at reference
     * date.
     * </p>
     * <p>
     * The two parameters for this model are the full amplitude (parameter 0) and the
     * phase at reference date (parameter 1). Their reference values (used also as the
     * initial values) are both set to 0. User can change them before starting the
     * propagation (or orbit determination) by calling {@link #getParametersDrivers()}
     * and {@link ParameterDriver#setValue(double)}.
     * </p>
     * @param direction acceleration direction in overridden spacecraft frame
     * @param attitudeOverride provider for attitude used to compute acceleration
     * direction
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing phase, if null
     * the reference date will be automatically set at propagation start
     * @param fundamentalPeriod fundamental period (typically set to initial orbit
     * {@link org.orekit.orbits.Orbit#getKeplerianPeriod() Keplerian period})
     * @param harmonicMultiplier multiplier to compute harmonic period from
     * fundamental period)
     */
    public HarmonicParametricAcceleration(final Vector3D direction, final AttitudeProvider attitudeOverride,
                                          final String prefix, final AbsoluteDate referenceDate,
                                          final double fundamentalPeriod, final int harmonicMultiplier) {
        this(direction, false, attitudeOverride, prefix, referenceDate,
             fundamentalPeriod, harmonicMultiplier);
    }

    /** Simple constructor.
     * @param direction acceleration direction in overridden spacecraft frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param attitudeOverride provider for attitude used to compute acceleration
     * direction
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing polynomials, if null
     * the reference date will be automatically set at propagation start
     * @param fundamentalPeriod fundamental period (typically set to initial orbit
     * {@link org.orekit.orbits.Orbit#getKeplerianPeriod() Keplerian period})
     * @param harmonicMultiplier multiplier to compute harmonic period from
     * fundamental period)
     */
    private HarmonicParametricAcceleration(final Vector3D direction, final boolean isInertial,
                                           final AttitudeProvider attitudeOverride,
                                           final String prefix, final AbsoluteDate referenceDate,
                                           final double fundamentalPeriod, final int harmonicMultiplier) {
        super(direction, isInertial, attitudeOverride);
        this.referenceDate = referenceDate;
        this.omega         = harmonicMultiplier * MathUtils.TWO_PI / fundamentalPeriod;
        try {
            drivers = new ParameterDriver[] {
                new ParameterDriver(prefix + " γ",
                                    0.0, AMPLITUDE_SCALE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                new ParameterDriver(prefix + " φ",
                                    0.0, PHASE_SCALE, -MathUtils.TWO_PI, MathUtils.TWO_PI),
            };
        } catch (OrekitException oe) {
            // this should never happen as scales are hard-coded
            throw new OrekitInternalError(oe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return isInertial();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        if (referenceDate == null) {
            referenceDate = initialState.getDate();
        }
    }

    /** {@inheritDoc}.
     * The signed amplitude of the acceleration is γ sin[2kπ(t-t₀)/T + φ], where
     * γ is parameter {@code 0} and represents the full amplitude, t is current
     * date, t₀ is reference date, {@code T} is fundamental period, {@code k} is
     * harmonic multiplier, and φ is parameter {@code 1} and represents phase at t₀.
     * The value t-t₀ is in seconds.
     */
    @Override
    protected double signedAmplitude(final SpacecraftState state, final double[] parameters) {
        final double dt = state.getDate().durationFrom(referenceDate);
        return parameters[0] * FastMath.sin(dt * omega + parameters[1]);
    }

    /** {@inheritDoc}
     * The signed amplitude of the acceleration is γ sin[2kπ(t-t₀)/T + φ], where
     * γ is parameter {@code 0} and represents the full amplitude, t is current
     * date, t₀ is reference date, {@code T} is fundamental period, {@code k} is
     * harmonic multiplier, and φ is parameter {@code 1} and represents phase at t₀.
     * The value t-t₀ is in seconds.
     */
    @Override
    protected <T extends RealFieldElement<T>> T signedAmplitude(final FieldSpacecraftState<T> state, final T[] parameters) {
        final T dt = state.getDate().durationFrom(referenceDate);
        return parameters[0].multiply(dt.multiply(omega).add(parameters[1]).sin());
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return drivers.clone();
    }

}
