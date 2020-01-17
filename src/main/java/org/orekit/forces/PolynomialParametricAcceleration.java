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
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** This class implements a {@link AbstractParametricAcceleration parametric acceleration}
 * with polynomial signed amplitude.
 * @since 9.0
 * @author Luc Maisonobe
 */
public class PolynomialParametricAcceleration extends AbstractParametricAcceleration {

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
    private final ParameterDriver[] drivers;

    /** Reference date for computing polynomials. */
    private AbsoluteDate referenceDate;

    /** Simple constructor.
     * <p>
     * The signed amplitude of the acceleration is ∑pₙ(t-t₀)ⁿ, where
     * pₙ is parameter {@code n}, t is current date and t₀ is reference date.
     * The value t-t₀ is in seconds.
     * </p>
     * <p>
     * The {@code degree + 1} parameters for this model are the polynomial
     * coefficients in increasing degree order. Their reference values (used
     * also as the initial values) are all set to 0. User can change them before
     * starting the propagation (or orbit determination) by calling {@link
     * #getParametersDrivers()} and {@link ParameterDriver#setValue(double)}.
     * </p>
     * @param direction acceleration direction in defining frame
     * @param isInertial if true, direction is defined in the same inertial
     * frame used for propagation (i.e. {@link SpacecraftState#getFrame()}),
     * otherwise direction is defined in spacecraft frame (i.e. using the
     * propagation {@link
     * org.orekit.propagation.Propagator#setAttitudeProvider(AttitudeProvider)
     * attitude law})
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing polynomials, if null
     * the reference date will be automatically set at propagation start
     * @param degree polynomial degree (i.e. a value of 0 corresponds to a constant acceleration)
     */
    public PolynomialParametricAcceleration(final Vector3D direction, final boolean isInertial,
                                            final String prefix, final AbsoluteDate referenceDate,
                                            final int degree) {
        this(direction, isInertial, null, prefix, referenceDate, degree);
    }

    /** Simple constructor.
     * <p>
     * The signed amplitude of the acceleration is ∑pₙ(t-t₀)ⁿ, where
     * pₙ is parameter {@code n}, t is current date and t₀ is reference date.
     * The value t-t₀ is in seconds.
     * </p>
     * <p>
     * The {@code degree + 1} parameters for this model are the polynomial
     * coefficients in increasing degree order. Their reference values (used
     * also as the initial values) are all set to 0. User can change them before
     * starting the propagation (or orbit determination) by calling {@link
     * #getParametersDrivers()} and {@link ParameterDriver#setValue(double)}.
     * </p>
     * @param direction acceleration direction in overridden spacecraft frame
     * @param attitudeOverride provider for attitude used to compute acceleration
     * direction
     * @param prefix prefix to use for parameter drivers
     * @param referenceDate reference date for computing polynomials, if null
     * the reference date will be automatically set at propagation start
     * @param degree polynomial degree (i.e. a value of 0 corresponds to a constant acceleration)
     */
    public PolynomialParametricAcceleration(final Vector3D direction, final AttitudeProvider attitudeOverride,
                                            final String prefix, final AbsoluteDate referenceDate,
                                            final int degree) {
        this(direction, false, attitudeOverride, prefix, referenceDate, degree);
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
     * @param degree polynomial degree (i.e. a value of 0 corresponds to a constant acceleration)
     */
    private PolynomialParametricAcceleration(final Vector3D direction, final boolean isInertial,
                                             final AttitudeProvider attitudeOverride,
                                             final String prefix, final AbsoluteDate referenceDate,
                                             final int degree) {
        super(direction, isInertial, attitudeOverride);
        this.referenceDate = referenceDate;
        this.drivers       = new ParameterDriver[degree + 1];
        try {
            for (int i = 0; i < drivers.length; ++i) {
                drivers[i] = new ParameterDriver(prefix + "[" + i + "]", 0.0, ACCELERATION_SCALE,
                                                 Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        } catch (OrekitException oe) {
            // this should never happen as scale is hard-coded
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

    /** {@inheritDoc} */
    @Override
    protected double signedAmplitude(final SpacecraftState state, final double[] parameters) {
        final double dt = state.getDate().durationFrom(referenceDate);
        double amplitude = 0;
        for (int i = parameters.length - 1; i >= 0; --i) {
            amplitude += amplitude * dt + parameters[i];
        }
        return amplitude;
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends RealFieldElement<T>> T signedAmplitude(final FieldSpacecraftState<T> state, final T[] parameters) {
        final T dt = state.getDate().durationFrom(referenceDate);
        T amplitude = dt.getField().getZero();
        for (int i = parameters.length - 1; i >= 0; --i) {
            amplitude = amplitude.add(amplitude.multiply(dt).add(parameters[i]));
        }
        return amplitude;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return drivers.clone();
    }

}
