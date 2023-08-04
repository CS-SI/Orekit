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
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** Polynomial acceleration model.
 * @since 10.3
 * @author Luc Maisonobe
 * @author Bryan Cazabonne
 */
public class PolynomialAccelerationModel implements AccelerationModel {

    /** Acceleration scaling factor.
     * <p>
     * 2⁻²⁰ is the order of magnitude of third body perturbing acceleration.
     * </p>
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double ACCELERATION_SCALE = FastMath.scalb(1.0, -20);

    /** Drivers for the polynomial coefficients. */
    private final List<ParameterDriver> drivers;

    /** Reference date for computing polynomials. */
    private AbsoluteDate referenceDate;

    /** Simple constructor.
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing polynomials, if null
     * the reference date will be automatically set at propagation start
     * @param degree polynomial degree (i.e. a value of 0 corresponds to a constant acceleration)
     */
    public PolynomialAccelerationModel(final String prefix,
                                       final AbsoluteDate referenceDate,
                                       final int degree) {
        // Reference date
        this.referenceDate = referenceDate;
        // Parameter drivers
        drivers = new ArrayList<>();
        for (int i = 0; i < degree + 1; ++i) {
            drivers.add(new ParameterDriver(prefix + "[" + i + "]", 0.0, ACCELERATION_SCALE,
                                            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        }
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
        double amplitude = 0;
        for (int i = parameters.length - 1; i >= 0; --i) {
            amplitude += amplitude * dt + parameters[i];
        }
        return amplitude;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T signedAmplitude(final FieldSpacecraftState<T> state,
                                                             final T[] parameters) {
        final T dt = state.getDate().durationFrom(referenceDate);
        T amplitude = dt.getField().getZero();
        for (int i = parameters.length - 1; i >= 0; --i) {
            amplitude = amplitude.add(amplitude.multiply(dt).add(parameters[i]));
        }
        return amplitude;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(drivers);
    }

}
