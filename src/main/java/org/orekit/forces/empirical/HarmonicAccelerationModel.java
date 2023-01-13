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
package org.orekit.forces.empirical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Harmonic acceleration model.
 * @since 10.3
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 */
public class HarmonicAccelerationModel implements AccelerationModel {

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
    private final List<ParameterDriver> drivers;

    /** Reference date for computing phase. */
    private AbsoluteDate referenceDate;

    /** Angular frequency ω = 2kπ/T. */
    private final double omega;

    /** Simple constructor.
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing polynomials, if null
     * the reference date will be automatically set at propagation start
     * @param fundamentalPeriod fundamental period (typically set to initial orbit
     * {@link org.orekit.orbits.Orbit#getKeplerianPeriod() Keplerian period})
     * @param harmonicMultiplier multiplier to compute harmonic period from
     * fundamental period)
     */
    public HarmonicAccelerationModel(final String prefix, final AbsoluteDate referenceDate,
                                     final double fundamentalPeriod, final int harmonicMultiplier) {
        this.referenceDate = referenceDate;
        this.omega         = harmonicMultiplier * MathUtils.TWO_PI / fundamentalPeriod;
        this.drivers       = new ArrayList<>(2);
        drivers.add(new ParameterDriver(prefix + " γ",
                                        0.0, AMPLITUDE_SCALE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        drivers.add(new ParameterDriver(prefix + " φ",
                                        0.0, PHASE_SCALE, -MathUtils.TWO_PI, MathUtils.TWO_PI));
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        if (referenceDate == null) {
            referenceDate = initialState.getDate();
        }
    }

    /** {@inheritDoc} */
    @Override
    public double signedAmplitude(final SpacecraftState state,
                                  final double[] parameters) {
        final double dt = state.getDate().durationFrom(referenceDate);
        return parameters[0] * FastMath.sin(dt * omega + parameters[1]);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T signedAmplitude(final FieldSpacecraftState<T> state,
                                                             final T[] parameters) {
        final T dt = state.getDate().durationFrom(referenceDate);
        return parameters[0].multiply(dt.multiply(omega).add(parameters[1]).sin());
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(drivers);
    }

}
