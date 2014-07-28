/* Copyright 2002-2014 CS Systèmes d'Information
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
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

    /** Fixed orientation parallel with reference frame (identity rotation, zero rotation rate and acceleration). */
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
    public AngularCoordinates(final Rotation rotation, final Vector3D rotationRate, final Vector3D rotationAcceleration) {
        this.rotation             = rotation;
        this.rotationRate         = rotationRate;
        this.rotationAcceleration = rotationAcceleration;
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
        final Rotation evolution = start.applyTo(end.revert());
        return new Vector3D(evolution.getAngle() / dt, evolution.getAxis());
    }

    /** Revert a rotation/rotation rate/ rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * @return a new triplet whose effect is the reverse of the effect
     * of the instance
     */
    public AngularCoordinates revert() {
        return new AngularCoordinates(rotation.revert(),
                                      rotation.applyInverseTo(rotationRate.negate()),
                                      rotation.applyInverseTo(rotationAcceleration.negate()));
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
        final Rotation rateContribution = (rate == 0.0) ? Rotation.IDENTITY : new Rotation(rotationRate, -rate * dt);

        // append rotation and rate contribution
        final AngularCoordinates linearPart =
                new AngularCoordinates(rateContribution.applyTo(rotation), rotationRate);

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
                new AngularCoordinates(new Rotation(rotationAcceleration, -0.5 * acc * dt * dt),
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
        return new AngularCoordinates(rotation.applyTo(offset.rotation),
                                      rotationRate.add(rotation.applyTo(offset.rotationRate)),
                                      rotationAcceleration.add(rotation.applyTo(offset.rotationAcceleration)));
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

    /** Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring rotation rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude
     * Interpolation</a>, changing the norm of the vector to match the modified Rodrigues
     * vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at π.
     * There is still a singularity at 2π, which is handled by slightly offsetting all rotations
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first time derivatives (rotation rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the rotations.
     * </p>
     * @param date interpolation date
     * @param useRotationRates if true, use sample points rotation rates,
     * otherwise ignore them and use only rotations
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     * @deprecated since 7.0 replaced with {@link TimeStampedAngularCoordinates#interpolate(AbsoluteDate, AngularDerivativesFilter, Collection)}
     */
    @Deprecated
    public static AngularCoordinates interpolate(final AbsoluteDate date, final boolean useRotationRates,
                                                 final Collection<Pair<AbsoluteDate, AngularCoordinates>> sample)
        throws OrekitException {
        final List<TimeStampedAngularCoordinates> list = new ArrayList<TimeStampedAngularCoordinates>(sample.size());
        for (final Pair<AbsoluteDate, AngularCoordinates> pair : sample) {
            list.add(new TimeStampedAngularCoordinates(pair.getFirst(),
                                                       pair.getSecond().getRotation(),
                                                       pair.getSecond().getRotationRate(),
                                                       pair.getSecond().getRotationAcceleration()));
        }
        return TimeStampedAngularCoordinates.interpolate(date,
                                                         useRotationRates ? AngularDerivativesFilter.USE_RR : AngularDerivativesFilter.USE_R,
                                                         list);
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
