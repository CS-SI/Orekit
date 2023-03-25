/* Copyright 2023 Luc Maisonobe
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
package org.orekit.utils;

import java.io.Serializable;

import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.time.TimeShiftable;

/** Modified Rodrigues vector with derivatives.
 * <p>
 * The modified Rodrigues vector is an alternative representation of {@link AngularCoordinates}.
 * It is equal to tan(θ/4) u  where θ and u are the rotation angle and axis respectively.
 * </p>
 * <p>
 * Modified Rodrigues vectors are well suited for interpolation and shifts.
 * </p>
 * @author luc
 * @since 12.0
 */
public class ModifiedRodrigues implements TimeShiftable<ModifiedRodrigues>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20230325L;

    /** Vector value. */
    private final Vector3D r0;

    /** Vector first derivative. */
    private final Vector3D r1;

    /** Vector second derivative. */
    private final Vector3D r2;

    /** Convert angular coordinates to modified Rodrigues vector.
     * @param sign multiplicative sign for quaternion components
     * @param ac coordinates to convert
     */
    public ModifiedRodrigues(final double sign, final AngularCoordinates ac) {

        final FieldRotation<UnivariateDerivative2> ud2R = ac.getUnivariateDerivative2Rotation();

        final double q0    = sign * ud2R.getQ0().getValue();
        final double q1    = sign * ud2R.getQ1().getValue();
        final double q2    = sign * ud2R.getQ2().getValue();
        final double q3    = sign * ud2R.getQ3().getValue();

        // first time-derivatives of the quaternion
        final double q0Dot = ud2R.getQ0().getFirstDerivative();
        final double q1Dot = ud2R.getQ1().getFirstDerivative();
        final double q2Dot = ud2R.getQ2().getFirstDerivative();
        final double q3Dot = ud2R.getQ3().getFirstDerivative();

        // second time-derivatives of the quaternion
        final double q0DotDot = ud2R.getQ0().getSecondDerivative();
        final double q1DotDot = ud2R.getQ1().getSecondDerivative();
        final double q2DotDot = ud2R.getQ2().getSecondDerivative();
        final double q3DotDot = ud2R.getQ3().getSecondDerivative();

        // the modified Rodrigues is tan(θ/4) u where θ and u are the rotation angle and axis respectively
        // this can be rewritten using quaternion components:
        //      r (q₁ / (1+q₀), q₂ / (1+q₀), q₃ / (1+q₀))
        // applying the derivation chain rule to previous expression gives rDot and rDotDot
        final double inv          = 1.0 / (1.0 + q0);
        final double mTwoInvQ0Dot = -2 * inv * q0Dot;

        final double rX       = inv * q1;
        final double rY       = inv * q2;
        final double rZ       = inv * q3;

        final double mInvR1   = -inv * rX;
        final double mInvR2   = -inv * rY;
        final double mInvR3   = -inv * rZ;

        final double rXDot    = MathArrays.linearCombination(inv, q1Dot, mInvR1, q0Dot);
        final double rYDot    = MathArrays.linearCombination(inv, q2Dot, mInvR2, q0Dot);
        final double rZDot    = MathArrays.linearCombination(inv, q3Dot, mInvR3, q0Dot);

        final double rXDotDot = MathArrays.linearCombination(inv, q1DotDot, mTwoInvQ0Dot, rXDot, mInvR1, q0DotDot);
        final double rYDotDot = MathArrays.linearCombination(inv, q2DotDot, mTwoInvQ0Dot, rYDot, mInvR2, q0DotDot);
        final double rZDotDot = MathArrays.linearCombination(inv, q3DotDot, mTwoInvQ0Dot, rZDot, mInvR3, q0DotDot);

        r0 = new Vector3D(rX,       rY,       rZ);
        r1 = new Vector3D(rXDot,    rYDot,    rZDot);
        r2 = new Vector3D(rXDotDot, rYDotDot, rZDotDot);

    }

    /** Build from value and derivatives.
     * @param r0 modified Rodrigues vector value
     * @param r1 modified Rodrigues vector first derivative
     * @param r2 modified Rodrigues vector second derivative
     */
    public ModifiedRodrigues(final Vector3D r0, final Vector3D r1, final Vector3D r2) {
        this.r0 = r0;
        this.r1 = r1;
        this.r2 = r2;
    }

    /** Get the modified Rodrigues vector value.
     * @return modified Rodrigues vector value
     */
    public Vector3D getValue() {
        return r0;
    }

    /** Get the modified Rodrigues vector first derivative.
     * @return modified Rodrigues vector first derivative
     */
    public Vector3D getFirstDerivative() {
        return r1;
    }

    /** Get the modified Rodrigues vector second derivative.
     * @return modified Rodrigues vector second derivative
     */
    public Vector3D getSecondDerivative() {
        return r2;
    }

    /** {@inheritDoc} */
    @Override
    public ModifiedRodrigues shiftedBy(final double dt) {
        final double halfDt = 0.5 * dt;
        return new ModifiedRodrigues(new Vector3D(r0.getX() + dt * (r1.getX() + halfDt * r2.getX()),
                                                  r0.getY() + dt * (r1.getY() + halfDt * r2.getY()),
                                                  r0.getZ() + dt * (r1.getZ() + halfDt * r2.getZ())),
                                     new Vector3D(r1.getX() + dt * r2.getX(),
                                                  r1.getY() + dt * r2.getY(),
                                                  r1.getZ() + dt * r2.getZ()),
                                     r2);
    }

    /** Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * @return angular coordinates
     */
    public AngularCoordinates toAngularCoordinates() {

        // rotation
        final double rSquared = r0.getNormSq();
        final double oPQ0     = 2 / (1 + rSquared);
        final double q0       = oPQ0 - 1;
        final double q1       = oPQ0 * r0.getX();
        final double q2       = oPQ0 * r0.getY();
        final double q3       = oPQ0 * r0.getZ();

        // rotation rate
        final double oPQ02    = oPQ0 * oPQ0;
        final double q0Dot    = -oPQ02 * MathArrays.linearCombination(r0.getX(), r1.getX(), r0.getY(), r1.getY(),  r0.getZ(), r1.getZ());
        final double q1Dot    = oPQ0 * r1.getX() + r0.getX() * q0Dot;
        final double q2Dot    = oPQ0 * r1.getY() + r0.getY() * q0Dot;
        final double q3Dot    = oPQ0 * r1.getZ() + r0.getZ() * q0Dot;

        // rotation acceleration
        final double q0DotDot = (1 - q0) / oPQ0 * q0Dot * q0Dot -
                        oPQ02 * MathArrays.linearCombination(r0.getX(), r2.getX(), r0.getY(), r2.getY(), r0.getZ(), r2.getZ()) -
                        (q1Dot * q1Dot + q2Dot * q2Dot + q3Dot * q3Dot);
        final double q1DotDot = MathArrays.linearCombination(oPQ0, r2.getX(), 2 * r1.getX(), q0Dot, r0.getX(), q0DotDot);
        final double q2DotDot = MathArrays.linearCombination(oPQ0, r2.getY(), 2 * r1.getY(), q0Dot, r0.getY(), q0DotDot);
        final double q3DotDot = MathArrays.linearCombination(oPQ0, r2.getZ(), 2 * r1.getZ(), q0Dot, r0.getZ(), q0DotDot);

        return new AngularCoordinates(new FieldRotation<>(new UnivariateDerivative2(q0, q0Dot, q0DotDot),
                                                          new UnivariateDerivative2(q1, q1Dot, q1DotDot),
                                                          new UnivariateDerivative2(q2, q2Dot, q2DotDot),
                                                          new UnivariateDerivative2(q3, q3Dot, q3DotDot),
                                                          false));

    }

}
