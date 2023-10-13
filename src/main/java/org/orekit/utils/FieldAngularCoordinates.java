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
package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivative;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative1;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.linear.FieldDecompositionSolver;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.FieldQRDecomposition;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.FieldTimeShiftable;

/** Simple container for rotation / rotation rate pairs, using {@link
 * CalculusFieldElement}.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple quadratic model. It is <em>not</em> intended as a replacement for
 * proper attitude propagation but should be sufficient for either small
 * time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link FieldPVCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 6.0
 * @see AngularCoordinates
 */
public class FieldAngularCoordinates<T extends CalculusFieldElement<T>>
        implements FieldTimeShiftable<FieldAngularCoordinates<T>, T> {

    /** rotation. */
    private final FieldRotation<T> rotation;

    /** rotation rate. */
    private final FieldVector3D<T> rotationRate;

    /** rotation acceleration. */
    private final FieldVector3D<T> rotationAcceleration;

    /** Builds a rotation/rotation rate pair.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     */
    public FieldAngularCoordinates(final FieldRotation<T> rotation,
                                   final FieldVector3D<T> rotationRate) {
        this(rotation, rotationRate,
             new FieldVector3D<>(rotation.getQ0().getField().getZero(),
                                 rotation.getQ0().getField().getZero(),
                                 rotation.getQ0().getField().getZero()));
    }

    /** Builds a rotation / rotation rate / rotation acceleration triplet.
     * @param rotation i.e. the orientation of the vehicle
     * @param rotationRate rotation rate Ω, i.e. the spin vector (rad/s)
     * @param rotationAcceleration angular acceleration vector dΩ/dt (rad/s²)
     */
    public FieldAngularCoordinates(final FieldRotation<T> rotation,
                                   final FieldVector3D<T> rotationRate,
                                   final FieldVector3D<T> rotationAcceleration) {
        this.rotation             = rotation;
        this.rotationRate         = rotationRate;
        this.rotationAcceleration = rotationAcceleration;
    }

    /** Build the rotation that transforms a pair of pv coordinates into another one.

     * <p><em>WARNING</em>! This method requires much more stringent assumptions on
     * its parameters than the similar {@link FieldRotation#FieldRotation(FieldVector3D, FieldVector3D,
     * FieldVector3D, FieldVector3D) constructor} from the {@link FieldRotation FieldRotation} class.
     * As far as the FieldRotation constructor is concerned, the {@code v₂} vector from
     * the second pair can be slightly misaligned. The FieldRotation constructor will
     * compensate for this misalignment and create a rotation that ensure {@code
     * v₁ = r(u₁)} and {@code v₂ ∈ plane (r(u₁), r(u₂))}. <em>THIS IS NOT
     * TRUE ANYMORE IN THIS CLASS</em>! As derivatives are involved and must be
     * preserved, this constructor works <em>only</em> if the two pairs are fully
     * consistent, i.e. if a rotation exists that fulfill all the requirements: {@code
     * v₁ = r(u₁)}, {@code v₂ = r(u₂)}, {@code dv₁/dt = dr(u₁)/dt}, {@code dv₂/dt
     * = dr(u₂)/dt}, {@code d²v₁/dt² = d²r(u₁)/dt²}, {@code d²v₂/dt² = d²r(u₂)/dt²}.</p>
     * @param u1 first vector of the origin pair
     * @param u2 second vector of the origin pair
     * @param v1 desired image of u1 by the rotation
     * @param v2 desired image of u2 by the rotation
     * @param tolerance relative tolerance factor used to check singularities
     */
    public FieldAngularCoordinates(final FieldPVCoordinates<T> u1, final FieldPVCoordinates<T> u2,
                                   final FieldPVCoordinates<T> v1, final FieldPVCoordinates<T> v2,
                                   final double tolerance) {

        try {
            // find the initial fixed rotation
            rotation = new FieldRotation<>(u1.getPosition(), u2.getPosition(),
                                           v1.getPosition(), v2.getPosition());

            // find rotation rate Ω such that
            //  Ω ⨯ v₁ = r(dot(u₁)) - dot(v₁)
            //  Ω ⨯ v₂ = r(dot(u₂)) - dot(v₂)
            final FieldVector3D<T> ru1Dot = rotation.applyTo(u1.getVelocity());
            final FieldVector3D<T> ru2Dot = rotation.applyTo(u2.getVelocity());


            rotationRate = inverseCrossProducts(v1.getPosition(), ru1Dot.subtract(v1.getVelocity()),
                                                v2.getPosition(), ru2Dot.subtract(v2.getVelocity()),
                                                tolerance);


            // find rotation acceleration dot(Ω) such that
            // dot(Ω) ⨯ v₁ = r(dotdot(u₁)) - 2 Ω ⨯ dot(v₁) - Ω ⨯  (Ω ⨯ v₁) - dotdot(v₁)
            // dot(Ω) ⨯ v₂ = r(dotdot(u₂)) - 2 Ω ⨯ dot(v₂) - Ω ⨯  (Ω ⨯ v₂) - dotdot(v₂)
            final FieldVector3D<T> ru1DotDot = rotation.applyTo(u1.getAcceleration());
            final FieldVector3D<T> oDotv1    = FieldVector3D.crossProduct(rotationRate, v1.getVelocity());
            final FieldVector3D<T> oov1      = FieldVector3D.crossProduct(rotationRate, rotationRate.crossProduct(v1.getPosition()));
            final FieldVector3D<T> c1        = new FieldVector3D<>(1, ru1DotDot, -2, oDotv1, -1, oov1, -1, v1.getAcceleration());
            final FieldVector3D<T> ru2DotDot = rotation.applyTo(u2.getAcceleration());
            final FieldVector3D<T> oDotv2    = FieldVector3D.crossProduct(rotationRate, v2.getVelocity());
            final FieldVector3D<T> oov2      = FieldVector3D.crossProduct(rotationRate, rotationRate.crossProduct( v2.getPosition()));
            final FieldVector3D<T> c2        = new FieldVector3D<>(1, ru2DotDot, -2, oDotv2, -1, oov2, -1, v2.getAcceleration());
            rotationAcceleration     = inverseCrossProducts(v1.getPosition(), c1, v2.getPosition(), c2, tolerance);

        } catch (MathIllegalArgumentException miae) {
            throw new OrekitException(miae);
        }

    }

    /** Builds a FieldAngularCoordinates from a field and a regular AngularCoordinates.
     * @param field field for the components
     * @param ang AngularCoordinates to convert
     */
    public FieldAngularCoordinates(final Field<T> field, final AngularCoordinates ang) {
        this.rotation             = new FieldRotation<>(field, ang.getRotation());
        this.rotationRate         = new FieldVector3D<>(field, ang.getRotationRate());
        this.rotationAcceleration = new FieldVector3D<>(field, ang.getRotationAcceleration());
    }

    /** Builds a FieldAngularCoordinates from  a {@link FieldRotation}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The rotation components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param r rotation with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     * @since 9.2
     */
    public <U extends FieldDerivative<T, U>> FieldAngularCoordinates(final FieldRotation<U> r) {

        final T q0       = r.getQ0().getValue();
        final T q1       = r.getQ1().getValue();
        final T q2       = r.getQ2().getValue();
        final T q3       = r.getQ3().getValue();

        rotation     = new FieldRotation<>(q0, q1, q2, q3, false);
        if (r.getQ0().getOrder() >= 1) {
            final T q0Dot    = r.getQ0().getPartialDerivative(1);
            final T q1Dot    = r.getQ1().getPartialDerivative(1);
            final T q2Dot    = r.getQ2().getPartialDerivative(1);
            final T q3Dot    = r.getQ3().getPartialDerivative(1);
            rotationRate =
                    new FieldVector3D<>(q0.linearCombination(q1.negate(), q0Dot, q0,          q1Dot,
                                                             q3,          q2Dot, q2.negate(), q3Dot).multiply(2),
                                        q0.linearCombination(q2.negate(), q0Dot, q3.negate(), q1Dot,
                                                             q0,          q2Dot, q1,          q3Dot).multiply(2),
                                        q0.linearCombination(q3.negate(), q0Dot, q2,          q1Dot,
                                                             q1.negate(), q2Dot, q0,          q3Dot).multiply(2));
            if (r.getQ0().getOrder() >= 2) {
                final T q0DotDot = r.getQ0().getPartialDerivative(2);
                final T q1DotDot = r.getQ1().getPartialDerivative(2);
                final T q2DotDot = r.getQ2().getPartialDerivative(2);
                final T q3DotDot = r.getQ3().getPartialDerivative(2);
                rotationAcceleration =
                        new FieldVector3D<>(q0.linearCombination(q1.negate(), q0DotDot, q0,          q1DotDot,
                                                                 q3,          q2DotDot, q2.negate(), q3DotDot).multiply(2),
                                            q0.linearCombination(q2.negate(), q0DotDot, q3.negate(), q1DotDot,
                                                                 q0,          q2DotDot, q1,          q3DotDot).multiply(2),
                                            q0.linearCombination(q3.negate(), q0DotDot, q2,          q1DotDot,
                                                                 q1.negate(), q2DotDot, q0,          q3DotDot).multiply(2));
            } else {
                rotationAcceleration = FieldVector3D.getZero(q0.getField());
            }
        } else {
            rotationRate         = FieldVector3D.getZero(q0.getField());
            rotationAcceleration = FieldVector3D.getZero(q0.getField());
        }

    }

    /** Fixed orientation parallel with reference frame
     * (identity rotation, zero rotation rate and acceleration).
     * @param field field for the components
     * @param <T> the type of the field elements
     * @return a new fixed orientation parallel with reference frame
     */
    public static <T extends CalculusFieldElement<T>> FieldAngularCoordinates<T> getIdentity(final Field<T> field) {
        return new FieldAngularCoordinates<>(field, AngularCoordinates.IDENTITY);
    }

    /** Find a vector from two known cross products.
     * <p>
     * We want to find Ω such that: Ω ⨯ v₁ = c₁ and Ω ⨯ v₂ = c₂
     * </p>
     * <p>
     * The first equation (Ω ⨯ v₁ = c₁) will always be fulfilled exactly,
     * and the second one will be fulfilled if possible.
     * </p>
     * @param v1 vector forming the first known cross product
     * @param c1 know vector for cross product Ω ⨯ v₁
     * @param v2 vector forming the second known cross product
     * @param c2 know vector for cross product Ω ⨯ v₂
     * @param tolerance relative tolerance factor used to check singularities
     * @param <T> the type of the field elements
     * @return vector Ω such that: Ω ⨯ v₁ = c₁ and Ω ⨯ v₂ = c₂
     * @exception MathIllegalArgumentException if vectors are inconsistent and
     * no solution can be found
     */
    private static <T extends CalculusFieldElement<T>> FieldVector3D<T> inverseCrossProducts(final FieldVector3D<T> v1, final FieldVector3D<T> c1,
                                                                                         final FieldVector3D<T> v2, final FieldVector3D<T> c2,
                                                                                         final double tolerance)
        throws MathIllegalArgumentException {

        final T v12 = v1.getNormSq();
        final T v1n = v12.sqrt();
        final T v22 = v2.getNormSq();
        final T v2n = v22.sqrt();
        final T threshold;
        if (v1n.getReal() >= v2n.getReal()) {
            threshold = v1n.multiply(tolerance);
        }
        else {
            threshold = v2n.multiply(tolerance);
        }
        FieldVector3D<T> omega = null;

        try {
            // create the over-determined linear system representing the two cross products
            final FieldMatrix<T> m = MatrixUtils.createFieldMatrix(v12.getField(), 6, 3);
            m.setEntry(0, 1, v1.getZ());
            m.setEntry(0, 2, v1.getY().negate());
            m.setEntry(1, 0, v1.getZ().negate());
            m.setEntry(1, 2, v1.getX());
            m.setEntry(2, 0, v1.getY());
            m.setEntry(2, 1, v1.getX().negate());
            m.setEntry(3, 1, v2.getZ());
            m.setEntry(3, 2, v2.getY().negate());
            m.setEntry(4, 0, v2.getZ().negate());
            m.setEntry(4, 2, v2.getX());
            m.setEntry(5, 0, v2.getY());
            m.setEntry(5, 1, v2.getX().negate());

            final T[] kk = MathArrays.buildArray(v2n.getField(), 6);
            kk[0] = c1.getX();
            kk[1] = c1.getY();
            kk[2] = c1.getZ();
            kk[3] = c2.getX();
            kk[4] = c2.getY();
            kk[5] = c2.getZ();
            final FieldVector<T> rhs = MatrixUtils.createFieldVector(kk);

            // find the best solution we can
            final FieldDecompositionSolver<T> solver = new FieldQRDecomposition<>(m).getSolver();
            final FieldVector<T> v = solver.solve(rhs);
            omega = new FieldVector3D<>(v.getEntry(0), v.getEntry(1), v.getEntry(2));

        } catch (MathIllegalArgumentException miae) {
            if (miae.getSpecifier() == LocalizedCoreFormats.SINGULAR_MATRIX) {

                // handle some special cases for which we can compute a solution
                final T c12 = c1.getNormSq();
                final T c1n = c12.sqrt();
                final T c22 = c2.getNormSq();
                final T c2n = c22.sqrt();
                if (c1n.getReal() <= threshold.getReal() && c2n.getReal() <= threshold.getReal()) {
                    // simple special case, velocities are cancelled
                    return new FieldVector3D<>(v12.getField().getZero(), v12.getField().getZero(), v12.getField().getZero());
                } else if (v1n.getReal() <= threshold.getReal() && c1n.getReal() >= threshold.getReal()) {
                    // this is inconsistent, if v₁ is zero, c₁ must be 0 too
                    throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE,
                                                           c1n.getReal(), 0, true);
                } else if (v2n.getReal() <= threshold.getReal() && c2n.getReal() >= threshold.getReal()) {
                    // this is inconsistent, if v₂ is zero, c₂ must be 0 too
                    throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE,
                                                           c2n.getReal(), 0, true);
                } else if (v1.crossProduct(v1).getNorm().getReal() <= threshold.getReal() && v12.getReal() > threshold.getReal()) {
                    // simple special case, v₂ is redundant with v₁, we just ignore it
                    // use the simplest Ω: orthogonal to both v₁ and c₁
                    omega = new FieldVector3D<>(v12.reciprocal(), v1.crossProduct(c1));
                } else {
                    throw miae;
                }
            } else {
                throw miae;
            }
        }
        // check results
        final T d1 = FieldVector3D.distance(omega.crossProduct(v1), c1);
        if (d1.getReal() > threshold.getReal()) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE, 0, true);
        }

        final T d2 = FieldVector3D.distance(omega.crossProduct(v2), c2);
        if (d2.getReal() > threshold.getReal()) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE, 0, true);
        }

        return omega;

    }

    /** Transform the instance to a {@link FieldRotation}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The {@link FieldDerivativeStructure} coordinates correspond to time-derivatives up
     * to the user-specified order.
     * </p>
     * @param order derivation order for the vector components
     * @return rotation with time-derivatives embedded within the coordinates
          * @since 9.2
     */
    public FieldRotation<FieldDerivativeStructure<T>> toDerivativeStructureRotation(final int order) {

        // quaternion components
        final T q0 = rotation.getQ0();
        final T q1 = rotation.getQ1();
        final T q2 = rotation.getQ2();
        final T q3 = rotation.getQ3();

        // first time-derivatives of the quaternion
        final T oX    = rotationRate.getX();
        final T oY    = rotationRate.getY();
        final T oZ    = rotationRate.getZ();
        final T q0Dot = q0.linearCombination(q1.negate(), oX, q2.negate(), oY, q3.negate(), oZ).multiply(0.5);
        final T q1Dot = q0.linearCombination(q0,          oX, q3.negate(), oY, q2,          oZ).multiply(0.5);
        final T q2Dot = q0.linearCombination(q3,          oX, q0,          oY, q1.negate(), oZ).multiply(0.5);
        final T q3Dot = q0.linearCombination(q2.negate(), oX, q1,          oY, q0,          oZ).multiply(0.5);

        // second time-derivatives of the quaternion
        final T oXDot = rotationAcceleration.getX();
        final T oYDot = rotationAcceleration.getY();
        final T oZDot = rotationAcceleration.getZ();
        final T q0DotDot = q0.linearCombination(array6(q1, q2,  q3, q1Dot, q2Dot,  q3Dot),
                                                array6(oXDot, oYDot, oZDot, oX, oY, oZ)).multiply(-0.5);
        final T q1DotDot = q0.linearCombination(array6(q0, q2, q3.negate(), q0Dot, q2Dot, q3Dot.negate()),
                                                array6(oXDot, oZDot, oYDot, oX, oZ, oY)).multiply(0.5);
        final T q2DotDot = q0.linearCombination(array6(q0, q3, q1.negate(), q0Dot, q3Dot, q1Dot.negate()),
                                                array6(oYDot, oXDot, oZDot, oY, oX, oZ)).multiply(0.5);
        final T q3DotDot = q0.linearCombination(array6(q0, q1, q2.negate(), q0Dot, q1Dot, q2Dot.negate()),
                                                array6(oZDot, oYDot, oXDot, oZ, oY, oX)).multiply(0.5);

        final FDSFactory<T> factory;
        final FieldDerivativeStructure<T> q0DS;
        final FieldDerivativeStructure<T> q1DS;
        final FieldDerivativeStructure<T> q2DS;
        final FieldDerivativeStructure<T> q3DS;
        switch (order) {
            case 0 :
                factory = new FDSFactory<>(q0.getField(), 1, order);
                q0DS = factory.build(q0);
                q1DS = factory.build(q1);
                q2DS = factory.build(q2);
                q3DS = factory.build(q3);
                break;
            case 1 :
                factory = new FDSFactory<>(q0.getField(), 1, order);
                q0DS = factory.build(q0, q0Dot);
                q1DS = factory.build(q1, q1Dot);
                q2DS = factory.build(q2, q2Dot);
                q3DS = factory.build(q3, q3Dot);
                break;
            case 2 :
                factory = new FDSFactory<>(q0.getField(), 1, order);
                q0DS = factory.build(q0, q0Dot, q0DotDot);
                q1DS = factory.build(q1, q1Dot, q1DotDot);
                q2DS = factory.build(q2, q2Dot, q2DotDot);
                q3DS = factory.build(q3, q3Dot, q3DotDot);
                break;
            default :
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

        return new FieldRotation<>(q0DS, q1DS, q2DS, q3DS, false);

    }

    /** Transform the instance to a {@link FieldRotation}&lt;{@link UnivariateDerivative1}&gt;.
     * <p>
     * The {@link UnivariateDerivative1} coordinates correspond to time-derivatives up
     * to the order 1.
     * </p>
     * @return rotation with time-derivatives embedded within the coordinates
     */
    public FieldRotation<FieldUnivariateDerivative1<T>> toUnivariateDerivative1Rotation() {

        // quaternion components
        final T q0 = rotation.getQ0();
        final T q1 = rotation.getQ1();
        final T q2 = rotation.getQ2();
        final T q3 = rotation.getQ3();

        // first time-derivatives of the quaternion
        final T oX    = rotationRate.getX();
        final T oY    = rotationRate.getY();
        final T oZ    = rotationRate.getZ();
        final T q0Dot = q0.linearCombination(q1.negate(), oX, q2.negate(), oY, q3.negate(), oZ).multiply(0.5);
        final T q1Dot = q0.linearCombination(q0,          oX, q3.negate(), oY, q2,          oZ).multiply(0.5);
        final T q2Dot = q0.linearCombination(q3,          oX, q0,          oY, q1.negate(), oZ).multiply(0.5);
        final T q3Dot = q0.linearCombination(q2.negate(), oX, q1,          oY, q0,          oZ).multiply(0.5);

        final FieldUnivariateDerivative1<T> q0UD = new FieldUnivariateDerivative1<>(q0, q0Dot);
        final FieldUnivariateDerivative1<T> q1UD = new FieldUnivariateDerivative1<>(q1, q1Dot);
        final FieldUnivariateDerivative1<T> q2UD = new FieldUnivariateDerivative1<>(q2, q2Dot);
        final FieldUnivariateDerivative1<T> q3UD = new FieldUnivariateDerivative1<>(q3, q3Dot);

        return new FieldRotation<>(q0UD, q1UD, q2UD, q3UD, false);

    }

    /** Transform the instance to a {@link FieldRotation}&lt;{@link UnivariateDerivative2}&gt;.
     * <p>
     * The {@link UnivariateDerivative2} coordinates correspond to time-derivatives up
     * to the order 2.
     * </p>
     * @return rotation with time-derivatives embedded within the coordinates
     */
    public FieldRotation<FieldUnivariateDerivative2<T>> toUnivariateDerivative2Rotation() {

        // quaternion components
        final T q0 = rotation.getQ0();
        final T q1 = rotation.getQ1();
        final T q2 = rotation.getQ2();
        final T q3 = rotation.getQ3();

        // first time-derivatives of the quaternion
        final T oX    = rotationRate.getX();
        final T oY    = rotationRate.getY();
        final T oZ    = rotationRate.getZ();
        final T q0Dot = q0.linearCombination(q1.negate(), oX, q2.negate(), oY, q3.negate(), oZ).multiply(0.5);
        final T q1Dot = q0.linearCombination(q0,          oX, q3.negate(), oY, q2,          oZ).multiply(0.5);
        final T q2Dot = q0.linearCombination(q3,          oX, q0,          oY, q1.negate(), oZ).multiply(0.5);
        final T q3Dot = q0.linearCombination(q2.negate(), oX, q1,          oY, q0,          oZ).multiply(0.5);

        // second time-derivatives of the quaternion
        final T oXDot = rotationAcceleration.getX();
        final T oYDot = rotationAcceleration.getY();
        final T oZDot = rotationAcceleration.getZ();
        final T q0DotDot = q0.linearCombination(array6(q1, q2,  q3, q1Dot, q2Dot,  q3Dot),
                                                array6(oXDot, oYDot, oZDot, oX, oY, oZ)).multiply(-0.5);
        final T q1DotDot = q0.linearCombination(array6(q0, q2, q3.negate(), q0Dot, q2Dot, q3Dot.negate()),
                                                array6(oXDot, oZDot, oYDot, oX, oZ, oY)).multiply(0.5);
        final T q2DotDot = q0.linearCombination(array6(q0, q3, q1.negate(), q0Dot, q3Dot, q1Dot.negate()),
                                                array6(oYDot, oXDot, oZDot, oY, oX, oZ)).multiply(0.5);
        final T q3DotDot = q0.linearCombination(array6(q0, q1, q2.negate(), q0Dot, q1Dot, q2Dot.negate()),
                                                array6(oZDot, oYDot, oXDot, oZ, oY, oX)).multiply(0.5);

        final FieldUnivariateDerivative2<T> q0UD = new FieldUnivariateDerivative2<>(q0, q0Dot, q0DotDot);
        final FieldUnivariateDerivative2<T> q1UD = new FieldUnivariateDerivative2<>(q1, q1Dot, q1DotDot);
        final FieldUnivariateDerivative2<T> q2UD = new FieldUnivariateDerivative2<>(q2, q2Dot, q2DotDot);
        final FieldUnivariateDerivative2<T> q3UD = new FieldUnivariateDerivative2<>(q3, q3Dot, q3DotDot);

        return new FieldRotation<>(q0UD, q1UD, q2UD, q3UD, false);

    }

    /** Build an arry of 6 elements.
     * @param e1 first element
     * @param e2 second element
     * @param e3 third element
     * @param e4 fourth element
     * @param e5 fifth element
     * @param e6 sixth element
     * @return a new array
     * @since 9.2
     */
    private T[] array6(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final T[] array = MathArrays.buildArray(e1.getField(), 6);
        array[0] = e1;
        array[1] = e2;
        array[2] = e3;
        array[3] = e4;
        array[4] = e5;
        array[5] = e6;
        return array;
    }

    /** Estimate rotation rate between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @param <T> the type of the field elements
     * @return rotation rate allowing to go from start to end orientations
     */
    public static <T extends CalculusFieldElement<T>>
        FieldVector3D<T> estimateRate(final FieldRotation<T> start,
                                      final FieldRotation<T> end,
                                      final double dt) {
        return estimateRate(start, end, start.getQ0().getField().getZero().add(dt));
    }

    /** Estimate rotation rate between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @param <T> the type of the field elements
     * @return rotation rate allowing to go from start to end orientations
     */
    public static <T extends CalculusFieldElement<T>>
        FieldVector3D<T> estimateRate(final FieldRotation<T> start,
                                      final FieldRotation<T> end,
                                      final T dt) {
        final FieldRotation<T> evolution = start.compose(end.revert(), RotationConvention.VECTOR_OPERATOR);
        return new FieldVector3D<>(evolution.getAngle().divide(dt),
                                   evolution.getAxis(RotationConvention.VECTOR_OPERATOR));
    }

    /**
     * Revert a rotation / rotation rate / rotation acceleration triplet.
     *
     * <p> Build a triplet which reverse the effect of another triplet.
     *
     * @return a new triplet whose effect is the reverse of the effect
     * of the instance
     */
    public FieldAngularCoordinates<T> revert() {
        return new FieldAngularCoordinates<>(rotation.revert(),
                                             rotation.applyInverseTo(rotationRate.negate()),
                                             rotation.applyInverseTo(rotationAcceleration.negate()));
    }

    /** Get a time-shifted rotation. Same as {@link #shiftedBy(double)} except
     * only the shifted rotation is computed.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * an approximate solution of the fixed acceleration motion. It is <em>not</em>
     * intended as a replacement for proper attitude propagation but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     * @see  #shiftedBy(CalculusFieldElement)
     * @since 11.2
     */
    public FieldRotation<T> rotationShiftedBy(final T dt) {

        // the shiftedBy method is based on a local approximation.
        // It considers separately the contribution of the constant
        // rotation, the linear contribution or the rate and the
        // quadratic contribution of the acceleration. The rate
        // and acceleration contributions are small rotations as long
        // as the time shift is small, which is the crux of the algorithm.
        // Small rotations are almost commutative, so we append these small
        // contributions one after the other, as if they really occurred
        // successively, despite this is not what really happens.

        // compute the linear contribution first, ignoring acceleration
        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final T rate = rotationRate.getNorm();
        final FieldRotation<T> rateContribution = (rate.getReal() == 0.0) ?
                FieldRotation.getIdentity(dt.getField()) :
                new FieldRotation<>(rotationRate, rate.multiply(dt), RotationConvention.FRAME_TRANSFORM);

        // append rotation and rate contribution
        final FieldRotation<T> linearPart =
                rateContribution.compose(rotation, RotationConvention.VECTOR_OPERATOR);

        final T acc  = rotationAcceleration.getNorm();
        if (acc.getReal() == 0.0) {
            // no acceleration, the linear part is sufficient
            return linearPart;
        }

        // compute the quadratic contribution, ignoring initial rotation and rotation rate
        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final FieldRotation<T> quadraticContribution =
                new FieldRotation<>(rotationAcceleration,
                        acc.multiply(dt).multiply(dt).multiply(0.5),
                        RotationConvention.FRAME_TRANSFORM);

        // the quadratic contribution is a small rotation:
        // its initial angle and angular rate are both zero.
        // small rotations are almost commutative, so we append the small
        // quadratic part after the linear part as a simple offset
        return quadraticContribution
                .compose(linearPart, RotationConvention.VECTOR_OPERATOR);

    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple quadratic model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public FieldAngularCoordinates<T> shiftedBy(final double dt) {
        return shiftedBy(rotation.getQ0().getField().getZero().add(dt));
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple quadratic model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public FieldAngularCoordinates<T> shiftedBy(final T dt) {

        // the shiftedBy method is based on a local approximation.
        // It considers separately the contribution of the constant
        // rotation, the linear contribution or the rate and the
        // quadratic contribution of the acceleration. The rate
        // and acceleration contributions are small rotations as long
        // as the time shift is small, which is the crux of the algorithm.
        // Small rotations are almost commutative, so we append these small
        // contributions one after the other, as if they really occurred
        // successively, despite this is not what really happens.

        // compute the linear contribution first, ignoring acceleration
        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final T rate = rotationRate.getNorm();
        final T zero = rate.getField().getZero();
        final T one  = rate.getField().getOne();
        final FieldRotation<T> rateContribution = (rate.getReal() == 0.0) ?
                                                  new FieldRotation<>(one, zero, zero, zero, false) :
                                                  new FieldRotation<>(rotationRate,
                                                                      rate.multiply(dt),
                                                                      RotationConvention.FRAME_TRANSFORM);

        // append rotation and rate contribution
        final FieldAngularCoordinates<T> linearPart =
                new FieldAngularCoordinates<>(rateContribution.compose(rotation, RotationConvention.VECTOR_OPERATOR),
                                              rotationRate);

        final T acc  = rotationAcceleration.getNorm();
        if (acc.getReal() == 0.0) {
            // no acceleration, the linear part is sufficient
            return linearPart;
        }

        // compute the quadratic contribution, ignoring initial rotation and rotation rate
        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final FieldAngularCoordinates<T> quadraticContribution =
                new FieldAngularCoordinates<>(new FieldRotation<>(rotationAcceleration,
                                                                  acc.multiply(dt.multiply(0.5).multiply(dt)),
                                                                  RotationConvention.FRAME_TRANSFORM),
                                              new FieldVector3D<>(dt, rotationAcceleration),
                                              rotationAcceleration);

        // the quadratic contribution is a small rotation:
        // its initial angle and angular rate are both zero.
        // small rotations are almost commutative, so we append the small
        // quadratic part after the linear part as a simple offset
        return quadraticContribution.addOffset(linearPart);

    }

    /** Get the rotation.
     * @return the rotation.
     */
    public FieldRotation<T> getRotation() {
        return rotation;
    }

    /** Get the rotation rate.
     * @return the rotation rate vector (rad/s).
     */
    public FieldVector3D<T> getRotationRate() {
        return rotationRate;
    }

    /** Get the rotation acceleration.
     * @return the rotation acceleration vector dΩ/dt (rad/s²).
     */
    public FieldVector3D<T> getRotationAcceleration() {
        return rotationAcceleration;
    }

    /** Add an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.addOffset(b)} and {@code
     * b.addOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(FieldAngularCoordinates) addOffset} and
     * {@link #subtractOffset(FieldAngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(FieldAngularCoordinates)
     */
    public FieldAngularCoordinates<T> addOffset(final FieldAngularCoordinates<T> offset) {
        final FieldVector3D<T> rOmega    = rotation.applyTo(offset.rotationRate);
        final FieldVector3D<T> rOmegaDot = rotation.applyTo(offset.rotationAcceleration);
        return new FieldAngularCoordinates<>(rotation.compose(offset.rotation, RotationConvention.VECTOR_OPERATOR),
                                             rotationRate.add(rOmega),
                                             new FieldVector3D<>( 1.0, rotationAcceleration,
                                                                  1.0, rOmegaDot,
                                                                 -1.0, FieldVector3D.crossProduct(rotationRate, rOmega)));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset Rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(FieldAngularCoordinates) addOffset} and
     * {@link #subtractOffset(FieldAngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(FieldAngularCoordinates)
     */
    public FieldAngularCoordinates<T> subtractOffset(final FieldAngularCoordinates<T> offset) {
        return addOffset(offset.revert());
    }

    /** Convert to a regular angular coordinates.
     * @return a regular angular coordinates
     */
    public AngularCoordinates toAngularCoordinates() {
        return new AngularCoordinates(rotation.toRotation(), rotationRate.toVector3D(),
                                      rotationAcceleration.toVector3D());
    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of pv by the rotation
     */
    public FieldPVCoordinates<T> applyTo(final PVCoordinates pv) {

        final FieldVector3D<T> transformedP = rotation.applyTo(pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(rotationRate, transformedP);
        final FieldVector3D<T> transformedV = rotation.applyTo(pv.getVelocity()).subtract(crossP);
        final FieldVector3D<T> crossV       = FieldVector3D.crossProduct(rotationRate, transformedV);
        final FieldVector3D<T> crossCrossP  = FieldVector3D.crossProduct(rotationRate, crossP);
        final FieldVector3D<T> crossDotP    = FieldVector3D.crossProduct(rotationAcceleration, transformedP);
        final FieldVector3D<T> transformedA = new FieldVector3D<>( 1, rotation.applyTo(pv.getAcceleration()),
                                                                  -2, crossV,
                                                                  -1, crossCrossP,
                                                                  -1, crossDotP);

        return new FieldPVCoordinates<>(transformedP, transformedV, transformedA);

    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of pv by the rotation
     */
    public TimeStampedFieldPVCoordinates<T> applyTo(final TimeStampedPVCoordinates pv) {

        final FieldVector3D<T> transformedP = rotation.applyTo(pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(rotationRate, transformedP);
        final FieldVector3D<T> transformedV = rotation.applyTo(pv.getVelocity()).subtract(crossP);
        final FieldVector3D<T> crossV       = FieldVector3D.crossProduct(rotationRate, transformedV);
        final FieldVector3D<T> crossCrossP  = FieldVector3D.crossProduct(rotationRate, crossP);
        final FieldVector3D<T> crossDotP    = FieldVector3D.crossProduct(rotationAcceleration, transformedP);
        final FieldVector3D<T> transformedA = new FieldVector3D<>( 1, rotation.applyTo(pv.getAcceleration()),
                                                                  -2, crossV,
                                                                  -1, crossCrossP,
                                                                  -1, crossDotP);

        return new TimeStampedFieldPVCoordinates<>(pv.getDate(), transformedP, transformedV, transformedA);

    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of pv by the rotation
     * @since 9.0
     */
    public FieldPVCoordinates<T> applyTo(final FieldPVCoordinates<T> pv) {

        final FieldVector3D<T> transformedP = rotation.applyTo(pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(rotationRate, transformedP);
        final FieldVector3D<T> transformedV = rotation.applyTo(pv.getVelocity()).subtract(crossP);
        final FieldVector3D<T> crossV       = FieldVector3D.crossProduct(rotationRate, transformedV);
        final FieldVector3D<T> crossCrossP  = FieldVector3D.crossProduct(rotationRate, crossP);
        final FieldVector3D<T> crossDotP    = FieldVector3D.crossProduct(rotationAcceleration, transformedP);
        final FieldVector3D<T> transformedA = new FieldVector3D<>( 1, rotation.applyTo(pv.getAcceleration()),
                                                                  -2, crossV,
                                                                  -1, crossCrossP,
                                                                  -1, crossDotP);

        return new FieldPVCoordinates<>(transformedP, transformedV, transformedA);

    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of pv by the rotation
     * @since 9.0
     */
    public TimeStampedFieldPVCoordinates<T> applyTo(final TimeStampedFieldPVCoordinates<T> pv) {

        final FieldVector3D<T> transformedP = rotation.applyTo(pv.getPosition());
        final FieldVector3D<T> crossP       = FieldVector3D.crossProduct(rotationRate, transformedP);
        final FieldVector3D<T> transformedV = rotation.applyTo(pv.getVelocity()).subtract(crossP);
        final FieldVector3D<T> crossV       = FieldVector3D.crossProduct(rotationRate, transformedV);
        final FieldVector3D<T> crossCrossP  = FieldVector3D.crossProduct(rotationRate, crossP);
        final FieldVector3D<T> crossDotP    = FieldVector3D.crossProduct(rotationAcceleration, transformedP);
        final FieldVector3D<T> transformedA = new FieldVector3D<>( 1, rotation.applyTo(pv.getAcceleration()),
                                                                  -2, crossV,
                                                                  -1, crossCrossP,
                                                                  -1, crossDotP);

        return new TimeStampedFieldPVCoordinates<>(pv.getDate(), transformedP, transformedV, transformedA);

    }

    /** Convert rotation, rate and acceleration to modified Rodrigues vector and derivatives.
     * <p>
     * The modified Rodrigues vector is tan(θ/4) u where θ and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param sign multiplicative sign for quaternion components
     * @return modified Rodrigues vector and derivatives (vector on row 0, first derivative
     * on row 1, second derivative on row 2)
     * @see #createFromModifiedRodrigues(CalculusFieldElement[][])
     * @since 9.0
     */
    public T[][] getModifiedRodrigues(final double sign) {

        final T q0    = getRotation().getQ0().multiply(sign);
        final T q1    = getRotation().getQ1().multiply(sign);
        final T q2    = getRotation().getQ2().multiply(sign);
        final T q3    = getRotation().getQ3().multiply(sign);
        final T oX    = getRotationRate().getX();
        final T oY    = getRotationRate().getY();
        final T oZ    = getRotationRate().getZ();
        final T oXDot = getRotationAcceleration().getX();
        final T oYDot = getRotationAcceleration().getY();
        final T oZDot = getRotationAcceleration().getZ();

        // first time-derivatives of the quaternion
        final T q0Dot = q0.linearCombination(q1.negate(), oX, q2.negate(), oY, q3.negate(), oZ).multiply(0.5);
        final T q1Dot = q0.linearCombination( q0, oX, q3.negate(), oY,  q2, oZ).multiply(0.5);
        final T q2Dot = q0.linearCombination( q3, oX,  q0, oY, q1.negate(), oZ).multiply(0.5);
        final T q3Dot = q0.linearCombination(q2.negate(), oX,  q1, oY,  q0, oZ).multiply(0.5);

        // second time-derivatives of the quaternion
        final T q0DotDot = linearCombination(q1, oXDot, q2, oYDot, q3, oZDot,
                                             q1Dot, oX, q2Dot, oY, q3Dot, oZ).
                           multiply(-0.5);
        final T q1DotDot = linearCombination(q0, oXDot, q2, oZDot, q3.negate(), oYDot,
                                             q0Dot, oX, q2Dot, oZ, q3Dot.negate(), oY).
                           multiply(0.5);
        final T q2DotDot = linearCombination(q0, oYDot, q3, oXDot, q1.negate(), oZDot,
                                             q0Dot, oY, q3Dot, oX, q1Dot.negate(), oZ).
                           multiply(0.5);
        final T q3DotDot = linearCombination(q0, oZDot, q1, oYDot, q2.negate(), oXDot,
                                             q0Dot, oZ, q1Dot, oY, q2Dot.negate(), oX).
                           multiply(0.5);

        // the modified Rodrigues is tan(θ/4) u where θ and u are the rotation angle and axis respectively
        // this can be rewritten using quaternion components:
        //      r (q₁ / (1+q₀), q₂ / (1+q₀), q₃ / (1+q₀))
        // applying the derivation chain rule to previous expression gives rDot and rDotDot
        final T inv          = q0.add(1).reciprocal();
        final T mTwoInvQ0Dot = inv.multiply(q0Dot).multiply(-2);

        final T r1       = inv.multiply(q1);
        final T r2       = inv.multiply(q2);
        final T r3       = inv.multiply(q3);

        final T mInvR1   = inv.multiply(r1).negate();
        final T mInvR2   = inv.multiply(r2).negate();
        final T mInvR3   = inv.multiply(r3).negate();

        final T r1Dot    = q0.linearCombination(inv, q1Dot, mInvR1, q0Dot);
        final T r2Dot    = q0.linearCombination(inv, q2Dot, mInvR2, q0Dot);
        final T r3Dot    = q0.linearCombination(inv, q3Dot, mInvR3, q0Dot);

        final T r1DotDot = q0.linearCombination(inv, q1DotDot, mTwoInvQ0Dot, r1Dot, mInvR1, q0DotDot);
        final T r2DotDot = q0.linearCombination(inv, q2DotDot, mTwoInvQ0Dot, r2Dot, mInvR2, q0DotDot);
        final T r3DotDot = q0.linearCombination(inv, q3DotDot, mTwoInvQ0Dot, r3Dot, mInvR3, q0DotDot);

        final T[][] rodrigues = MathArrays.buildArray(q0.getField(), 3, 3);
        rodrigues[0][0] = r1;
        rodrigues[0][1] = r2;
        rodrigues[0][2] = r3;
        rodrigues[1][0] = r1Dot;
        rodrigues[1][1] = r2Dot;
        rodrigues[1][2] = r3Dot;
        rodrigues[2][0] = r1DotDot;
        rodrigues[2][1] = r2DotDot;
        rodrigues[2][2] = r3DotDot;
        return rodrigues;

    }

    /**
     * Compute a linear combination.
     * @param a1 first factor of the first term
     * @param b1 second factor of the first term
     * @param a2 first factor of the second term
     * @param b2 second factor of the second term
     * @param a3 first factor of the third term
     * @param b3 second factor of the third term
     * @param a4 first factor of the fourth term
     * @param b4 second factor of the fourth term
     * @param a5 first factor of the fifth term
     * @param b5 second factor of the fifth term
     * @param a6 first factor of the sixth term
     * @param b6 second factor of the sicth term
     * @return a<sub>1</sub>&times;b<sub>1</sub> + a<sub>2</sub>&times;b<sub>2</sub> +
     * a<sub>3</sub>&times;b<sub>3</sub> + a<sub>4</sub>&times;b<sub>4</sub> +
     * a<sub>5</sub>&times;b<sub>5</sub> + a<sub>6</sub>&times;b<sub>6</sub>
     */
    private T linearCombination(final T a1, final T b1, final T a2, final T b2, final T a3, final T b3,
                                final T a4, final T b4, final T a5, final T b5, final T a6, final T b6) {

        final T[] a = MathArrays.buildArray(a1.getField(), 6);
        a[0] = a1;
        a[1] = a2;
        a[2] = a3;
        a[3] = a4;
        a[4] = a5;
        a[5] = a6;

        final T[] b = MathArrays.buildArray(b1.getField(), 6);
        b[0] = b1;
        b[1] = b2;
        b[2] = b3;
        b[3] = b4;
        b[4] = b5;
        b[5] = b6;

        return a1.linearCombination(a, b);

    }

    /** Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * @param r modified Rodrigues vector (with first and second times derivatives)
     * @param <T> the type of the field elements
     * @return angular coordinates
     * @see #getModifiedRodrigues(double)
     * @since 9.0
     */
    public static <T extends CalculusFieldElement<T>>  FieldAngularCoordinates<T> createFromModifiedRodrigues(final T[][] r) {

        // rotation
        final T rSquared = r[0][0].multiply(r[0][0]).add(r[0][1].multiply(r[0][1])).add(r[0][2].multiply(r[0][2]));
        final T oPQ0     = rSquared.add(1).reciprocal().multiply(2);
        final T q0       = oPQ0.subtract(1);
        final T q1       = oPQ0.multiply(r[0][0]);
        final T q2       = oPQ0.multiply(r[0][1]);
        final T q3       = oPQ0.multiply(r[0][2]);

        // rotation rate
        final T oPQ02    = oPQ0.multiply(oPQ0);
        final T q0Dot    = oPQ02.multiply(q0.linearCombination(r[0][0], r[1][0], r[0][1], r[1][1],  r[0][2], r[1][2])).negate();
        final T q1Dot    = oPQ0.multiply(r[1][0]).add(r[0][0].multiply(q0Dot));
        final T q2Dot    = oPQ0.multiply(r[1][1]).add(r[0][1].multiply(q0Dot));
        final T q3Dot    = oPQ0.multiply(r[1][2]).add(r[0][2].multiply(q0Dot));
        final T oX       = q0.linearCombination(q1.negate(), q0Dot,  q0, q1Dot,  q3, q2Dot, q2.negate(), q3Dot).multiply(2);
        final T oY       = q0.linearCombination(q2.negate(), q0Dot, q3.negate(), q1Dot,  q0, q2Dot,  q1, q3Dot).multiply(2);
        final T oZ       = q0.linearCombination(q3.negate(), q0Dot,  q2, q1Dot, q1.negate(), q2Dot,  q0, q3Dot).multiply(2);

        // rotation acceleration
        final T q0DotDot = q0.subtract(1).negate().divide(oPQ0).multiply(q0Dot).multiply(q0Dot).
                           subtract(oPQ02.multiply(q0.linearCombination(r[0][0], r[2][0], r[0][1], r[2][1], r[0][2], r[2][2]))).
                           subtract(q1Dot.multiply(q1Dot).add(q2Dot.multiply(q2Dot)).add(q3Dot.multiply(q3Dot)));
        final T q1DotDot = q0.linearCombination(oPQ0, r[2][0], r[1][0].add(r[1][0]), q0Dot, r[0][0], q0DotDot);
        final T q2DotDot = q0.linearCombination(oPQ0, r[2][1], r[1][1].add(r[1][1]), q0Dot, r[0][1], q0DotDot);
        final T q3DotDot = q0.linearCombination(oPQ0, r[2][2], r[1][2].add(r[1][2]), q0Dot, r[0][2], q0DotDot);
        final T oXDot    = q0.linearCombination(q1.negate(), q0DotDot,  q0, q1DotDot,  q3, q2DotDot, q2.negate(), q3DotDot).multiply(2);
        final T oYDot    = q0.linearCombination(q2.negate(), q0DotDot, q3.negate(), q1DotDot,  q0, q2DotDot,  q1, q3DotDot).multiply(2);
        final T oZDot    = q0.linearCombination(q3.negate(), q0DotDot,  q2, q1DotDot, q1.negate(), q2DotDot,  q0, q3DotDot).multiply(2);

        return new FieldAngularCoordinates<>(new FieldRotation<>(q0, q1, q2, q3, false),
                                             new FieldVector3D<>(oX, oY, oZ),
                                             new FieldVector3D<>(oXDot, oYDot, oZDot));

    }

}
