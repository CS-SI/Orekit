/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import org.hipparchus.RealFieldElement;
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

/** Simple container for rotation / rotation rate pairs, using {@link
 * RealFieldElement}.
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
public class FieldAngularCoordinates<T extends RealFieldElement<T>> {


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
             new FieldVector3D<T>(rotation.getQ0().getField().getZero(),
                                  rotation.getQ0().getField().getZero(),
                                  rotation.getQ0().getField().getZero()));
    }

    /** Builds a rotation / rotation rate / rotation acceleration triplet.
     * @param rotation i.e. the orientation of the vehicle
     * @param rotationRate rotation rate rate Ω, i.e. the spin vector (rad/s)
     * @param rotationAcceleration angular acceleration vector dΩ/dt (rad²/s²)
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
     * @exception OrekitException if the vectors are inconsistent for the
     * rotation to be found (null, aligned, ...)
     */
    public FieldAngularCoordinates (final FieldPVCoordinates<T> u1, final FieldPVCoordinates<T> u2,
                              final FieldPVCoordinates<T> v1, final FieldPVCoordinates<T> v2,
                              final double tolerance)
        throws OrekitException {

        try {
            // find the initial fixed rotation
            rotation = new FieldRotation<T>(u1.getPosition(), u2.getPosition(),
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
            final FieldVector3D<T> c1        = new FieldVector3D<T>(1, ru1DotDot, -2, oDotv1, -1, oov1, -1, v1.getAcceleration());
            final FieldVector3D<T> ru2DotDot = rotation.applyTo(u2.getAcceleration());
            final FieldVector3D<T> oDotv2    = FieldVector3D.crossProduct(rotationRate, v2.getVelocity());
            final FieldVector3D<T> oov2      = FieldVector3D.crossProduct(rotationRate, rotationRate.crossProduct( v2.getPosition()));
            final FieldVector3D<T> c2        = new FieldVector3D<T>(1, ru2DotDot, -2, oDotv2, -1, oov2, -1, v2.getAcceleration());
            rotationAcceleration     = inverseCrossProducts(v1.getPosition(), c1, v2.getPosition(), c2, tolerance);

        } catch (MathIllegalArgumentException miae) {
            throw new OrekitException(miae);
        }

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
     * @return vector Ω such that: Ω ⨯ v₁ = c₁ and Ω ⨯ v₂ = c₂
     * @exception MathIllegalArgumentException if vectors are inconsistent and
     * no solution can be found
     */
    private FieldVector3D<T> inverseCrossProducts(final FieldVector3D<T> v1, final FieldVector3D<T> c1,
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
            final FieldDecompositionSolver<T> solver = new FieldQRDecomposition<T>(m).getSolver();
            final FieldVector<T> v = solver.solve(rhs);
            omega = new FieldVector3D<T>(v.getEntry(0), v.getEntry(1), v.getEntry(2));

        } catch (MathIllegalArgumentException miae) {
            if (miae.getSpecifier() == LocalizedCoreFormats.SINGULAR_MATRIX) {

                // handle some special cases for which we can compute a solution
                final T c12 = c1.getNormSq();
                final T c1n = c12.sqrt();
                final T c22 = c2.getNormSq();
                final T c2n = c22.sqrt();
                if (c1n.getReal() <= threshold.getReal() && c2n.getReal() <= threshold.getReal()) {
                    // simple special case, velocities are cancelled
                    return new FieldVector3D<T>(v12.getField().getZero(), v12.getField().getZero(), v12.getField().getZero());
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
                    omega = new FieldVector3D<T>(v12.pow(-1), v1.crossProduct(c1));
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

    /** Estimate rotation rate between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @param <T> the type of the field elements
     * @return rotation rate allowing to go from start to end orientations
     */
    public static <T extends RealFieldElement<T>>
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
    public static <T extends RealFieldElement<T>>
        FieldVector3D<T> estimateRate(final FieldRotation<T> start,
                                      final FieldRotation<T> end,
                                      final T dt) {
        final FieldRotation<T> evolution = start.compose(end.revert(), RotationConvention.VECTOR_OPERATOR);
        return new FieldVector3D<T>(evolution.getAngle().divide(dt),
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
        return new FieldAngularCoordinates<T>(rotation.revert(),
                                              rotation.applyInverseTo(rotationRate.negate()),
                                              rotation.applyInverseTo(rotationAcceleration.negate()));
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
                                                  new FieldRotation<T>(one, zero, zero, zero, false) :
                                                  new FieldRotation<T>(rotationRate,
                                                                       rate.multiply(dt),
                                                                       RotationConvention.FRAME_TRANSFORM);

        // append rotation and rate contribution
        final FieldAngularCoordinates<T> linearPart =
                new FieldAngularCoordinates<T>(rateContribution.compose(rotation, RotationConvention.VECTOR_OPERATOR),
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
                new FieldAngularCoordinates<T>(new FieldRotation<T>(rotationAcceleration,
                                                                    acc.multiply(dt.multiply(0.5).multiply(dt)),
                                                                    RotationConvention.FRAME_TRANSFORM),
                                               new FieldVector3D<T>(dt, rotationAcceleration),
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
     * @return the rotation acceleration vector dΩ/dt (rad²/s²).
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
        return new FieldAngularCoordinates<T>(rotation.compose(offset.rotation, RotationConvention.VECTOR_OPERATOR),
                                              rotationRate.add(rOmega),
                                              new FieldVector3D<T>( 1.0, rotationAcceleration,
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

}
