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

import java.io.Serializable;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeShiftable;

/** Simple container for rotation/rotation rate/rotation acceleration triplets.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * an approximate solution of the fixed acceleration motion. It is <em>not</em>
 * intended as a replacement for proper attitude propagation but should be
 * sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link PVCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class AngularCoordinates implements TimeShiftable<AngularCoordinates>, Serializable {

    /** Fixed orientation parallel with reference frame
     * (identity rotation, zero rotation rate and acceleration).
     */
    public static final AngularCoordinates IDENTITY =
            new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 20140414L;

    /** Rotation. */
    private final Rotation rotation;

    /** Rotation rate. */
    private final Vector3D rotationRate;

    /** Rotation acceleration. */
    private final Vector3D rotationAcceleration;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : Identity, Ω = (0 0 0), dΩ/dt = (0 0 0).</p>
     */
    public AngularCoordinates() {
        this(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);
    }

    /** Builds a rotation/rotation rate pair.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     */
    public AngularCoordinates(final Rotation rotation, final Vector3D rotationRate) {
        this(rotation, rotationRate, Vector3D.ZERO);
    }

    /** Builds a rotation/rotation rate/rotation acceleration triplet.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad²/s²)
     */
    public AngularCoordinates(final Rotation rotation,
                              final Vector3D rotationRate, final Vector3D rotationAcceleration) {
        this.rotation             = rotation;
        this.rotationRate         = rotationRate;
        this.rotationAcceleration = rotationAcceleration;
    }

    /** Build the rotation that transforms a pair of pv coordinates into another one.

     * <p><em>WARNING</em>! This method requires much more stringent assumptions on
     * its parameters than the similar {@link Rotation#Rotation(Vector3D, Vector3D,
     * Vector3D, Vector3D) constructor} from the {@link Rotation Rotation} class.
     * As far as the Rotation constructor is concerned, the {@code v₂} vector from
     * the second pair can be slightly misaligned. The Rotation constructor will
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
    public AngularCoordinates(final PVCoordinates u1, final PVCoordinates u2,
                              final PVCoordinates v1, final PVCoordinates v2,
                              final double tolerance)
        throws OrekitException {

        try {
            // find the initial fixed rotation
            rotation = new Rotation(u1.getPosition(), u2.getPosition(),
                                    v1.getPosition(), v2.getPosition());

            // find rotation rate Ω such that
            //  Ω ⨯ v₁ = r(dot(u₁)) - dot(v₁)
            //  Ω ⨯ v₂ = r(dot(u₂)) - dot(v₂)
            final Vector3D ru1Dot = rotation.applyTo(u1.getVelocity());
            final Vector3D ru2Dot = rotation.applyTo(u2.getVelocity());
            rotationRate = inverseCrossProducts(v1.getPosition(), ru1Dot.subtract(v1.getVelocity()),
                                                v2.getPosition(), ru2Dot.subtract(v2.getVelocity()),
                                                tolerance);

            // find rotation acceleration dot(Ω) such that
            // dot(Ω) ⨯ v₁ = r(dotdot(u₁)) - 2 Ω ⨯ dot(v₁) - Ω ⨯  (Ω ⨯ v₁) - dotdot(v₁)
            // dot(Ω) ⨯ v₂ = r(dotdot(u₂)) - 2 Ω ⨯ dot(v₂) - Ω ⨯  (Ω ⨯ v₂) - dotdot(v₂)
            final Vector3D ru1DotDot = rotation.applyTo(u1.getAcceleration());
            final Vector3D oDotv1    = Vector3D.crossProduct(rotationRate, v1.getVelocity());
            final Vector3D oov1      = Vector3D.crossProduct(rotationRate, Vector3D.crossProduct(rotationRate, v1.getPosition()));
            final Vector3D c1        = new Vector3D(1, ru1DotDot, -2, oDotv1, -1, oov1, -1, v1.getAcceleration());
            final Vector3D ru2DotDot = rotation.applyTo(u2.getAcceleration());
            final Vector3D oDotv2    = Vector3D.crossProduct(rotationRate, v2.getVelocity());
            final Vector3D oov2      = Vector3D.crossProduct(rotationRate, Vector3D.crossProduct(rotationRate, v2.getPosition()));
            final Vector3D c2        = new Vector3D(1, ru2DotDot, -2, oDotv2, -1, oov2, -1, v2.getAcceleration());
            rotationAcceleration     = inverseCrossProducts(v1.getPosition(), c1, v2.getPosition(), c2, tolerance);

        } catch (MathRuntimeException mrte) {
            throw new OrekitException(mrte);
        }

    }

    /** Build one of the rotations that transform one pv coordinates into another one.

     * <p>Except for a possible scale factor, if the instance were
     * applied to the vector u it will produce the vector v. There is an
     * infinite number of such rotations, this constructor choose the
     * one with the smallest associated angle (i.e. the one whose axis
     * is orthogonal to the (u, v) plane). If u and v are collinear, an
     * arbitrary rotation axis is chosen.</p>

     * @param u origin vector
     * @param v desired image of u by the rotation
     * @exception OrekitException if the vectors components cannot be converted to
     * {@link DerivativeStructure} with proper order
     */
    public AngularCoordinates(final PVCoordinates u, final PVCoordinates v) throws OrekitException {
        this(new FieldRotation<DerivativeStructure>(u.toDerivativeStructureVector(2),
                                                    v.toDerivativeStructureVector(2)));
    }

    /** Builds a AngularCoordinates from  a {@link FieldRotation}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The rotation components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param r rotation with time-derivatives embedded within the coordinates
     */
    public AngularCoordinates(final FieldRotation<DerivativeStructure> r) {

        final double q0       = r.getQ0().getReal();
        final double q1       = r.getQ1().getReal();
        final double q2       = r.getQ2().getReal();
        final double q3       = r.getQ3().getReal();

        rotation     = new Rotation(q0, q1, q2, q3, false);
        if (r.getQ0().getOrder() >= 1) {
            final double q0Dot    = r.getQ0().getPartialDerivative(1);
            final double q1Dot    = r.getQ1().getPartialDerivative(1);
            final double q2Dot    = r.getQ2().getPartialDerivative(1);
            final double q3Dot    = r.getQ3().getPartialDerivative(1);
            rotationRate =
                    new Vector3D(2 * MathArrays.linearCombination(-q1, q0Dot,  q0, q1Dot,  q3, q2Dot, -q2, q3Dot),
                                 2 * MathArrays.linearCombination(-q2, q0Dot, -q3, q1Dot,  q0, q2Dot,  q1, q3Dot),
                                 2 * MathArrays.linearCombination(-q3, q0Dot,  q2, q1Dot, -q1, q2Dot,  q0, q3Dot));
            if (r.getQ0().getOrder() >= 2) {
                final double q0DotDot = r.getQ0().getPartialDerivative(2);
                final double q1DotDot = r.getQ1().getPartialDerivative(2);
                final double q2DotDot = r.getQ2().getPartialDerivative(2);
                final double q3DotDot = r.getQ3().getPartialDerivative(2);
                rotationAcceleration =
                        new Vector3D(2 * MathArrays.linearCombination(-q1, q0DotDot,  q0, q1DotDot,  q3, q2DotDot, -q2, q3DotDot),
                                     2 * MathArrays.linearCombination(-q2, q0DotDot, -q3, q1DotDot,  q0, q2DotDot,  q1, q3DotDot),
                                     2 * MathArrays.linearCombination(-q3, q0DotDot,  q2, q1DotDot, -q1, q2DotDot,  q0, q3DotDot));
            } else {
                rotationAcceleration = Vector3D.ZERO;
            }
        } else {
            rotationRate         = Vector3D.ZERO;
            rotationAcceleration = Vector3D.ZERO;
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
    private static Vector3D inverseCrossProducts(final Vector3D v1, final Vector3D c1,
                                                 final Vector3D v2, final Vector3D c2,
                                                 final double tolerance)
        throws MathIllegalArgumentException {

        final double v12 = v1.getNormSq();
        final double v1n = FastMath.sqrt(v12);
        final double v22 = v2.getNormSq();
        final double v2n = FastMath.sqrt(v22);
        final double threshold = tolerance * FastMath.max(v1n, v2n);

        Vector3D omega;

        try {
            // create the over-determined linear system representing the two cross products
            final RealMatrix m = MatrixUtils.createRealMatrix(6, 3);
            m.setEntry(0, 1,  v1.getZ());
            m.setEntry(0, 2, -v1.getY());
            m.setEntry(1, 0, -v1.getZ());
            m.setEntry(1, 2,  v1.getX());
            m.setEntry(2, 0,  v1.getY());
            m.setEntry(2, 1, -v1.getX());
            m.setEntry(3, 1,  v2.getZ());
            m.setEntry(3, 2, -v2.getY());
            m.setEntry(4, 0, -v2.getZ());
            m.setEntry(4, 2,  v2.getX());
            m.setEntry(5, 0,  v2.getY());
            m.setEntry(5, 1, -v2.getX());

            final RealVector rhs = MatrixUtils.createRealVector(new double[] {
                c1.getX(), c1.getY(), c1.getZ(),
                c2.getX(), c2.getY(), c2.getZ()
            });

            // find the best solution we can
            final DecompositionSolver solver = new QRDecomposition(m, threshold).getSolver();
            final RealVector v = solver.solve(rhs);
            omega = new Vector3D(v.getEntry(0), v.getEntry(1), v.getEntry(2));

        } catch (MathIllegalArgumentException miae) {
            if (miae.getSpecifier() == LocalizedCoreFormats.SINGULAR_MATRIX) {

                // handle some special cases for which we can compute a solution
                final double c12 = c1.getNormSq();
                final double c1n = FastMath.sqrt(c12);
                final double c22 = c2.getNormSq();
                final double c2n = FastMath.sqrt(c22);

                if (c1n <= threshold && c2n <= threshold) {
                    // simple special case, velocities are cancelled
                    return Vector3D.ZERO;
                } else if (v1n <= threshold && c1n >= threshold) {
                    // this is inconsistent, if v₁ is zero, c₁ must be 0 too
                    throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE, c1n, 0, true);
                } else if (v2n <= threshold && c2n >= threshold) {
                    // this is inconsistent, if v₂ is zero, c₂ must be 0 too
                    throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE, c2n, 0, true);
                } else if (Vector3D.crossProduct(v1, v2).getNorm() <= threshold && v12 > threshold) {
                    // simple special case, v₂ is redundant with v₁, we just ignore it
                    // use the simplest Ω: orthogonal to both v₁ and c₁
                    omega = new Vector3D(1.0 / v12, Vector3D.crossProduct(v1, c1));
                } else {
                    throw miae;
                }
            } else {
                throw miae;
            }

        }

        // check results
        final double d1 = Vector3D.distance(Vector3D.crossProduct(omega, v1), c1);
        if (d1 > threshold) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE, d1, 0, true);
        }

        final double d2 = Vector3D.distance(Vector3D.crossProduct(omega, v2), c2);
        if (d2 > threshold) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_LARGE, d2, 0, true);
        }

        return omega;

    }

    /** Transform the instance to a {@link FieldRotation}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The {@link DerivativeStructure} coordinates correspond to time-derivatives up
     * to the user-specified order.
     * </p>
     * @param order derivation order for the vector components
     * @return rotation with time-derivatives embedded within the coordinates
     * @exception OrekitException if the user specified order is too large
     */
    public FieldRotation<DerivativeStructure> toDerivativeStructureRotation(final int order)
        throws OrekitException {

        // quaternion components
        final double q0 = rotation.getQ0();
        final double q1 = rotation.getQ1();
        final double q2 = rotation.getQ2();
        final double q3 = rotation.getQ3();

        // first time-derivatives of the quaternion
        final double oX    = rotationRate.getX();
        final double oY    = rotationRate.getY();
        final double oZ    = rotationRate.getZ();
        final double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        final double q1Dot = 0.5 * MathArrays.linearCombination( q0, oX, -q3, oY,  q2, oZ);
        final double q2Dot = 0.5 * MathArrays.linearCombination( q3, oX,  q0, oY, -q1, oZ);
        final double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX,  q1, oY,  q0, oZ);

        // second time-derivatives of the quaternion
        final double oXDot = rotationAcceleration.getX();
        final double oYDot = rotationAcceleration.getY();
        final double oZDot = rotationAcceleration.getZ();
        final double q0DotDot = -0.5 * MathArrays.linearCombination(new double[] {
            q1, q2,  q3, q1Dot, q2Dot,  q3Dot
        }, new double[] {
            oXDot, oYDot, oZDot, oX, oY, oZ
        });
        final double q1DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q2, -q3, q0Dot, q2Dot, -q3Dot
        }, new double[] {
            oXDot, oZDot, oYDot, oX, oZ, oY
        });
        final double q2DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q3, -q1, q0Dot, q3Dot, -q1Dot
        }, new double[] {
            oYDot, oXDot, oZDot, oY, oX, oZ
        });
        final double q3DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q1, -q2, q0Dot, q1Dot, -q2Dot
        }, new double[] {
            oZDot, oYDot, oXDot, oZ, oY, oX
        });

        final DerivativeStructure q0DS;
        final DerivativeStructure q1DS;
        final DerivativeStructure q2DS;
        final DerivativeStructure q3DS;
        switch(order) {
            case 0 :
                q0DS = new DerivativeStructure(1, 0, q0);
                q1DS = new DerivativeStructure(1, 0, q1);
                q2DS = new DerivativeStructure(1, 0, q2);
                q3DS = new DerivativeStructure(1, 0, q3);
                break;
            case 1 :
                q0DS = new DerivativeStructure(1, 1, q0, q0Dot);
                q1DS = new DerivativeStructure(1, 1, q1, q1Dot);
                q2DS = new DerivativeStructure(1, 1, q2, q2Dot);
                q3DS = new DerivativeStructure(1, 1, q3, q3Dot);
                break;
            case 2 :
                q0DS = new DerivativeStructure(1, 2, q0, q0Dot, q0DotDot);
                q1DS = new DerivativeStructure(1, 2, q1, q1Dot, q1DotDot);
                q2DS = new DerivativeStructure(1, 2, q2, q2Dot, q2DotDot);
                q3DS = new DerivativeStructure(1, 2, q3, q3Dot, q3DotDot);
                break;
            default :
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

        return new FieldRotation<DerivativeStructure>(q0DS, q1DS, q2DS, q3DS, false);

    }

    /** Estimate rotation rate between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return rotation rate allowing to go from start to end orientations
     */
    public static Vector3D estimateRate(final Rotation start, final Rotation end, final double dt) {
        final Rotation evolution = start.compose(end.revert(), RotationConvention.VECTOR_OPERATOR);
        return new Vector3D(evolution.getAngle() / dt, evolution.getAxis(RotationConvention.VECTOR_OPERATOR));
    }

    /** Revert a rotation/rotation rate/ rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * @return a new triplet whose effect is the reverse of the effect
     * of the instance
     */
    public AngularCoordinates revert() {
        return new AngularCoordinates(rotation.revert(),
                                      rotation.applyInverseTo(rotationRate).negate(),
                                      rotation.applyInverseTo(rotationAcceleration).negate());
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * an approximate solution of the fixed acceleration motion. It is <em>not</em>
     * intended as a replacement for proper attitude propagation but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public AngularCoordinates shiftedBy(final double dt) {

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
        final double rate = rotationRate.getNorm();
        final Rotation rateContribution = (rate == 0.0) ?
                                          Rotation.IDENTITY :
                                          new Rotation(rotationRate, rate * dt, RotationConvention.FRAME_TRANSFORM);

        // append rotation and rate contribution
        final AngularCoordinates linearPart =
                new AngularCoordinates(rateContribution.compose(rotation, RotationConvention.VECTOR_OPERATOR), rotationRate);

        final double acc  = rotationAcceleration.getNorm();
        if (acc == 0.0) {
            // no acceleration, the linear part is sufficient
            return linearPart;
        }

        // compute the quadratic contribution, ignoring initial rotation and rotation rate
        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final AngularCoordinates quadraticContribution =
                new AngularCoordinates(new Rotation(rotationAcceleration,
                                                    0.5 * acc * dt * dt,
                                                    RotationConvention.FRAME_TRANSFORM),
                                       new Vector3D(dt, rotationAcceleration),
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
    public Rotation getRotation() {
        return rotation;
    }

    /** Get the rotation rate.
     * @return the rotation rate vector Ω (rad/s).
     */
    public Vector3D getRotationRate() {
        return rotationRate;
    }

    /** Get the rotation acceleration.
     * @return the rotation acceleration vector dΩ/dt (rad²/s²).
     */
    public Vector3D getRotationAcceleration() {
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
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    public AngularCoordinates addOffset(final AngularCoordinates offset) {
        final Vector3D rOmega    = rotation.applyTo(offset.rotationRate);
        final Vector3D rOmegaDot = rotation.applyTo(offset.rotationAcceleration);
        return new AngularCoordinates(rotation.compose(offset.rotation, RotationConvention.VECTOR_OPERATOR),
                                      rotationRate.add(rOmega),
                                      new Vector3D( 1.0, rotationAcceleration,
                                                    1.0, rOmegaDot,
                                                   -1.0, Vector3D.crossProduct(rotationRate, rOmega)));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    public AngularCoordinates subtractOffset(final AngularCoordinates offset) {
        return addOffset(offset.revert());
    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of u by the rotation
     */
    public PVCoordinates applyTo(final PVCoordinates pv) {

        final Vector3D transformedP = rotation.applyTo(pv.getPosition());
        final Vector3D crossP       = Vector3D.crossProduct(rotationRate, transformedP);
        final Vector3D transformedV = rotation.applyTo(pv.getVelocity()).subtract(crossP);
        final Vector3D crossV       = Vector3D.crossProduct(rotationRate, transformedV);
        final Vector3D crossCrossP  = Vector3D.crossProduct(rotationRate, crossP);
        final Vector3D crossDotP    = Vector3D.crossProduct(rotationAcceleration, transformedP);
        final Vector3D transformedA = new Vector3D( 1, rotation.applyTo(pv.getAcceleration()),
                                                   -2, crossV,
                                                   -1, crossCrossP,
                                                   -1, crossDotP);

        return new PVCoordinates(transformedP, transformedV, transformedA);

    }

    /** Apply the rotation to a pv coordinates.
     * @param pv vector to apply the rotation to
     * @return a new pv coordinates which is the image of u by the rotation
     */
    public TimeStampedPVCoordinates applyTo(final TimeStampedPVCoordinates pv) {

        final Vector3D transformedP = getRotation().applyTo(pv.getPosition());
        final Vector3D crossP       = Vector3D.crossProduct(getRotationRate(), transformedP);
        final Vector3D transformedV = getRotation().applyTo(pv.getVelocity()).subtract(crossP);
        final Vector3D crossV       = Vector3D.crossProduct(getRotationRate(), transformedV);
        final Vector3D crossCrossP  = Vector3D.crossProduct(getRotationRate(), crossP);
        final Vector3D crossDotP    = Vector3D.crossProduct(getRotationAcceleration(), transformedP);
        final Vector3D transformedA = new Vector3D( 1, getRotation().applyTo(pv.getAcceleration()),
                                                   -2, crossV,
                                                   -1, crossCrossP,
                                                   -1, crossDotP);

        return new TimeStampedPVCoordinates(pv.getDate(), transformedP, transformedV, transformedA);

    }

    /** Convert rotation, rate and acceleration to modified Rodrigues vector and derivatives.
     * <p>
     * The modified Rodrigues vector is tan(θ/4) u where θ and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param sign multiplicative sign for quaternion components
     * @return modified Rodrigues vector and derivatives (vector on row 0, first derivative
     * on row 1, second derivative on row 2)
     * @see #createFromModifiedRodrigues(double[][])
     */
    public double[][] getModifiedRodrigues(final double sign) {

        final double q0    = sign * getRotation().getQ0();
        final double q1    = sign * getRotation().getQ1();
        final double q2    = sign * getRotation().getQ2();
        final double q3    = sign * getRotation().getQ3();
        final double oX    = getRotationRate().getX();
        final double oY    = getRotationRate().getY();
        final double oZ    = getRotationRate().getZ();
        final double oXDot = getRotationAcceleration().getX();
        final double oYDot = getRotationAcceleration().getY();
        final double oZDot = getRotationAcceleration().getZ();

        // first time-derivatives of the quaternion
        final double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        final double q1Dot = 0.5 * MathArrays.linearCombination( q0, oX, -q3, oY,  q2, oZ);
        final double q2Dot = 0.5 * MathArrays.linearCombination( q3, oX,  q0, oY, -q1, oZ);
        final double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX,  q1, oY,  q0, oZ);

        // second time-derivatives of the quaternion
        final double q0DotDot = -0.5 * MathArrays.linearCombination(new double[] {
            q1, q2,  q3, q1Dot, q2Dot,  q3Dot
        }, new double[] {
            oXDot, oYDot, oZDot, oX, oY, oZ
        });
        final double q1DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q2, -q3, q0Dot, q2Dot, -q3Dot
        }, new double[] {
            oXDot, oZDot, oYDot, oX, oZ, oY
        });
        final double q2DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q3, -q1, q0Dot, q3Dot, -q1Dot
        }, new double[] {
            oYDot, oXDot, oZDot, oY, oX, oZ
        });
        final double q3DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q1, -q2, q0Dot, q1Dot, -q2Dot
        }, new double[] {
            oZDot, oYDot, oXDot, oZ, oY, oX
        });

        // the modified Rodrigues is tan(θ/4) u where θ and u are the rotation angle and axis respectively
        // this can be rewritten using quaternion components:
        //      r (q₁ / (1+q₀), q₂ / (1+q₀), q₃ / (1+q₀))
        // applying the derivation chain rule to previous expression gives rDot and rDotDot
        final double inv          = 1.0 / (1.0 + q0);
        final double mTwoInvQ0Dot = -2 * inv * q0Dot;

        final double r1       = inv * q1;
        final double r2       = inv * q2;
        final double r3       = inv * q3;

        final double mInvR1   = -inv * r1;
        final double mInvR2   = -inv * r2;
        final double mInvR3   = -inv * r3;

        final double r1Dot    = MathArrays.linearCombination(inv, q1Dot, mInvR1, q0Dot);
        final double r2Dot    = MathArrays.linearCombination(inv, q2Dot, mInvR2, q0Dot);
        final double r3Dot    = MathArrays.linearCombination(inv, q3Dot, mInvR3, q0Dot);

        final double r1DotDot = MathArrays.linearCombination(inv, q1DotDot, mTwoInvQ0Dot, r1Dot, mInvR1, q0DotDot);
        final double r2DotDot = MathArrays.linearCombination(inv, q2DotDot, mTwoInvQ0Dot, r2Dot, mInvR2, q0DotDot);
        final double r3DotDot = MathArrays.linearCombination(inv, q3DotDot, mTwoInvQ0Dot, r3Dot, mInvR3, q0DotDot);

        return new double[][] {
            {
                r1,       r2,       r3
            }, {
                r1Dot,    r2Dot,    r3Dot
            }, {
                r1DotDot, r2DotDot, r3DotDot
            }
        };

    }

    /** Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * @param r modified Rodrigues vector (with first and second times derivatives)
     * @return angular coordinates
     * @see #getModifiedRodrigues(double)
     */
    public static AngularCoordinates createFromModifiedRodrigues(final double[][] r) {

        // rotation
        final double rSquared = r[0][0] * r[0][0] + r[0][1] * r[0][1] + r[0][2] * r[0][2];
        final double oPQ0     = 2 / (1 + rSquared);
        final double q0       = oPQ0 - 1;
        final double q1       = oPQ0 * r[0][0];
        final double q2       = oPQ0 * r[0][1];
        final double q3       = oPQ0 * r[0][2];

        // rotation rate
        final double oPQ02    = oPQ0 * oPQ0;
        final double q0Dot    = -oPQ02 * MathArrays.linearCombination(r[0][0], r[1][0], r[0][1], r[1][1],  r[0][2], r[1][2]);
        final double q1Dot    = oPQ0 * r[1][0] + r[0][0] * q0Dot;
        final double q2Dot    = oPQ0 * r[1][1] + r[0][1] * q0Dot;
        final double q3Dot    = oPQ0 * r[1][2] + r[0][2] * q0Dot;
        final double oX       = 2 * MathArrays.linearCombination(-q1, q0Dot,  q0, q1Dot,  q3, q2Dot, -q2, q3Dot);
        final double oY       = 2 * MathArrays.linearCombination(-q2, q0Dot, -q3, q1Dot,  q0, q2Dot,  q1, q3Dot);
        final double oZ       = 2 * MathArrays.linearCombination(-q3, q0Dot,  q2, q1Dot, -q1, q2Dot,  q0, q3Dot);

        // rotation acceleration
        final double q0DotDot = (1 - q0) / oPQ0 * q0Dot * q0Dot -
                                oPQ02 * MathArrays.linearCombination(r[0][0], r[2][0], r[0][1], r[2][1], r[0][2], r[2][2]) -
                                (q1Dot * q1Dot + q2Dot * q2Dot + q3Dot * q3Dot);
        final double q1DotDot = MathArrays.linearCombination(oPQ0, r[2][0], 2 * r[1][0], q0Dot, r[0][0], q0DotDot);
        final double q2DotDot = MathArrays.linearCombination(oPQ0, r[2][1], 2 * r[1][1], q0Dot, r[0][1], q0DotDot);
        final double q3DotDot = MathArrays.linearCombination(oPQ0, r[2][2], 2 * r[1][2], q0Dot, r[0][2], q0DotDot);
        final double oXDot    = 2 * MathArrays.linearCombination(-q1, q0DotDot,  q0, q1DotDot,  q3, q2DotDot, -q2, q3DotDot);
        final double oYDot    = 2 * MathArrays.linearCombination(-q2, q0DotDot, -q3, q1DotDot,  q0, q2DotDot,  q1, q3DotDot);
        final double oZDot    = 2 * MathArrays.linearCombination(-q3, q0DotDot,  q2, q1DotDot, -q1, q2DotDot,  q0, q3DotDot);

        return new AngularCoordinates(new Rotation(q0, q1, q2, q3, false),
                                      new Vector3D(oX, oY, oZ),
                                      new Vector3D(oXDot, oYDot, oZDot));

    }

}
