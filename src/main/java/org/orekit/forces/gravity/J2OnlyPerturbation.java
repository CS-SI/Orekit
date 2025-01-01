/* Copyright 2022-2025 Romain Serra
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
package org.orekit.forces.gravity;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;

/** J2-only force model.
 * This class models the oblateness part alone of the central body's potential (degree 2 and order 0),
 * whilst avoiding the computational overhead of generic NxM spherical harmonics.
 *
 * <p>
 * This J2 coefficient has same magnitude and opposite sign than the so-called unnormalized C20 coefficient.
 * </p>
 *
 * <p>
 * This class should not be used in combination of {@link HolmesFeatherstoneAttractionModel},
 * otherwise the J2 term would be taken into account twice.
 * </p>
 *
 * @author Romain Serra
 */
public class J2OnlyPerturbation implements ForceModel {

    /** Central body's gravitational constant. */
    private final double mu;

    /** Central body's equatorial radius. */
    private final double rEq;

    /** Central body's J2 coefficient as a function of time. */
    private final TimeScalarFunction j2OverTime;

    /** Frame where J2 applies. */
    private final Frame frame;

    /** Constructor with {@link TimeScalarFunction}.
     * It is the user's responsibility to make sure the Field and double versions are consistent with each other.
     * @param mu central body's gravitational constant
     * @param rEq central body's equatorial radius
     * @param j2OverTime J2 coefficient as a function of time.
     * @param frame frame where J2 applies
     */
    public J2OnlyPerturbation(final double mu, final double rEq, final TimeScalarFunction j2OverTime,
                              final Frame frame) {
        this.mu = mu;
        this.rEq = rEq;
        this.j2OverTime = j2OverTime;
        this.frame = frame;
    }

    /** Constructor with constant J2.
     * @param mu central body gravitational constant
     * @param rEq central body's equatorial radius
     * @param constantJ2 constant J2 coefficient
     * @param frame frame where J2 applies
     */
    public J2OnlyPerturbation(final double mu, final double rEq, final double constantJ2, final Frame frame) {
        this.mu = mu;
        this.rEq = rEq;
        this.frame = frame;
        this.j2OverTime = new TimeScalarFunction() {
            @Override
            public double value(final AbsoluteDate date) {
                return constantJ2;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                return date.getField().getZero().newInstance(constantJ2);
            }
        };
    }

    /** Constructor with spherical harmonics provider.
     * @param harmonicsProvider spherical harmonics provider of unnormalized coefficients
     * @param frame frame where J2 applies
     */
    public J2OnlyPerturbation(final UnnormalizedSphericalHarmonicsProvider harmonicsProvider, final Frame frame) {
        this.mu = harmonicsProvider.getMu();
        this.rEq = harmonicsProvider.getAe();
        this.frame = frame;
        this.j2OverTime = new TimeScalarFunction() {
            @Override
            public double value(final AbsoluteDate date) {
                return -harmonicsProvider.getUnnormalizedC20(date);
            }

            @Override
            public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                return date.getField().getZero().newInstance(value(date.toAbsoluteDate()));
            }
        };
    }

    /** Getter for mu.
     * @return mu
     */
    public double getMu() {
        return mu;
    }

    /** Getter for equatorial radius.
     * @return equatorial radius
     */
    public double getrEq() {
        return rEq;
    }

    /** Getter for frame.
     * @return frame
     */
    public Frame getFrame() {
        return frame;
    }

    /** Return J2 at requested date.
     * @param date epoch at which J2 coefficient should be retrieved
     * @return J2 coefficient
     */
    public double getJ2(final AbsoluteDate date) {
        return j2OverTime.value(date);
    }

    /** Return J2 at requested date (Field version).
     * @param <T> field
     * @param date epoch at which J2 coefficient should be retrieved
     * @return J2 coefficient
     */
    public <T extends CalculusFieldElement<T>> T getJ2(final FieldAbsoluteDate<T> date) {
        return j2OverTime.value(date);
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState state, final double[] parameters) {
        final AbsoluteDate date = state.getDate();
        final StaticTransform fromPropagationToJ2Frame = state.getFrame().getStaticTransformTo(frame, date);
        final Vector3D positionInJ2Frame = fromPropagationToJ2Frame.transformPosition(state.getPosition());
        final double j2 = j2OverTime.value(date);
        final Vector3D accelerationInJ2Frame = computeAccelerationInJ2Frame(positionInJ2Frame, mu, rEq, j2);
        final StaticTransform fromJ2FrameToPropagationOne = fromPropagationToJ2Frame.getStaticInverse();
        return fromJ2FrameToPropagationOne.transformVector(accelerationInJ2Frame);
    }

    /**
     * Compute acceleration in J2 frame.
     * @param positionInJ2Frame position in J2 frame@
     * @param mu gravitational parameter
     * @param rEq equatorial radius
     * @param j2 J2 coefficient
     * @return acceleration in J2 frame
     */
    public static Vector3D computeAccelerationInJ2Frame(final Vector3D positionInJ2Frame, final double mu,
                                                        final double rEq, final double j2) {
        final double squaredRadius = positionInJ2Frame.getNormSq();
        final double squaredZ = positionInJ2Frame.getZ() * positionInJ2Frame.getZ();
        final double ratioTimesFive = 5. * squaredZ / squaredRadius;
        final double ratioTimesFiveMinusOne = ratioTimesFive - 1.;
        final double componentX = positionInJ2Frame.getX() * ratioTimesFiveMinusOne;
        final double componentY = positionInJ2Frame.getY() * ratioTimesFiveMinusOne;
        final double componentZ = positionInJ2Frame.getZ() * (ratioTimesFive - 3);
        final double squaredRadiiRatio = rEq * rEq / squaredRadius;
        final double cubedRadius = squaredRadius * FastMath.sqrt(squaredRadius);
        final double factor = 3 * j2 * mu * squaredRadiiRatio / (2 * cubedRadius);
        return new Vector3D(componentX, componentY, componentZ).scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        final FieldAbsoluteDate<T> date = state.getDate();
        final FieldStaticTransform<T> fromPropagationToJ2Frame = state.getFrame().getStaticTransformTo(frame, date);
        final FieldVector3D<T> positionInJ2Frame = fromPropagationToJ2Frame.transformPosition(state.getPosition());
        final FieldVector3D<T> accelerationInJ2Frame = computeAccelerationInJ2Frame(positionInJ2Frame, mu, rEq,
                j2OverTime.value(date));
        final FieldStaticTransform<T> fromJ2FrameToPropagation = fromPropagationToJ2Frame.getStaticInverse();
        return fromJ2FrameToPropagation.transformVector(accelerationInJ2Frame);
    }

    /**
     * Compute acceleration in J2 frame. Field version.
     * @param positionInJ2Frame position in J2 frame@
     * @param mu gravitational parameter
     * @param rEq equatorial radius
     * @param j2 J2 coefficient
     * @param <T> field type
     * @return acceleration in J2 frame
     */
    public static <T extends CalculusFieldElement<T>> FieldVector3D<T> computeAccelerationInJ2Frame(final FieldVector3D<T> positionInJ2Frame,
                                                                                                    final double mu, final double rEq, final T j2) {
        final T squaredRadius = positionInJ2Frame.getNormSq();
        final T squaredZ = positionInJ2Frame.getZ().square();
        final T ratioTimesFive = squaredZ.multiply(5.).divide(squaredRadius);
        final T ratioTimesFiveMinusOne = ratioTimesFive.subtract(1.);
        final T componentX = positionInJ2Frame.getX().multiply(ratioTimesFiveMinusOne);
        final T componentY = positionInJ2Frame.getY().multiply(ratioTimesFiveMinusOne);
        final T componentZ = positionInJ2Frame.getZ().multiply(ratioTimesFive.subtract(3.));
        final T squaredRadiiRatio = squaredRadius.reciprocal().multiply(rEq * rEq);
        final T cubedRadius = squaredRadius.multiply(FastMath.sqrt(squaredRadius));
        final T factor = j2.multiply(mu).multiply(3.).multiply(squaredRadiiRatio).divide(cubedRadius.multiply(2));
        return new FieldVector3D<>(componentX, componentY, componentZ).scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }
}
