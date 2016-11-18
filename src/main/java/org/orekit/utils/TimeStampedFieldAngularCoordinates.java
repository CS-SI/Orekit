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

import java.util.Collection;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link FieldAngularCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedFieldAngularCoordinates<T extends RealFieldElement<T>>
    extends FieldAngularCoordinates<T> {

    /** The date. */
    private final FieldAbsoluteDate<T> date;

    /** Build the rotation that transforms a pair of pv coordinates into another pair.

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

     * @param date coordinates date
     * @param u1 first vector of the origin pair
     * @param u2 second vector of the origin pair
     * @param v1 desired image of u1 by the rotation
     * @param v2 desired image of u2 by the rotation
     * @param tolerance relative tolerance factor used to check singularities
     * @exception OrekitException if the vectors components cannot be converted to
     * {@link DerivativeStructure} with proper order
     */
    public TimeStampedFieldAngularCoordinates (final AbsoluteDate date,
                                               final FieldPVCoordinates<T> u1, final FieldPVCoordinates<T> u2,
                                               final FieldPVCoordinates<T> v1, final FieldPVCoordinates<T> v2,
                                               final double tolerance)
        throws OrekitException {
        this(new FieldAbsoluteDate<>(u1.getPosition().getX().getField(), date),
             u1, u2, v1, v2, tolerance);
    }

    /** Build the rotation that transforms a pair of pv coordinates into another pair.

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

     * @param date coordinates date
     * @param u1 first vector of the origin pair
     * @param u2 second vector of the origin pair
     * @param v1 desired image of u1 by the rotation
     * @param v2 desired image of u2 by the rotation
     * @param tolerance relative tolerance factor used to check singularities
     * @exception OrekitException if the vectors components cannot be converted to
     * {@link DerivativeStructure} with proper order
     */
    public TimeStampedFieldAngularCoordinates (final FieldAbsoluteDate<T> date,
                                               final FieldPVCoordinates<T> u1, final FieldPVCoordinates<T> u2,
                                               final FieldPVCoordinates<T> v1, final FieldPVCoordinates<T> v2,
                                               final double tolerance)
        throws OrekitException {
        super(u1, u2, v1, v2, tolerance);
        this.date = date;
    }

    /** Builds a rotation/rotation rate pair.
     * @param date coordinates date
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad²/s²)
     */
    public TimeStampedFieldAngularCoordinates(final AbsoluteDate date,
                                              final FieldRotation<T> rotation,
                                              final FieldVector3D<T> rotationRate,
                                              final FieldVector3D<T> rotationAcceleration) {
        this(new FieldAbsoluteDate<>(rotation.getQ0().getField(), date),
             rotation, rotationRate, rotationAcceleration);
    }

    /** Builds a rotation/rotation rate pair.
     * @param date coordinates date
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad²/s²)
     */
    public TimeStampedFieldAngularCoordinates(final FieldAbsoluteDate<T> date,
                                              final FieldRotation<T> rotation,
                                              final FieldVector3D<T> rotationRate,
                                              final FieldVector3D<T> rotationAcceleration) {
        super(rotation, rotationRate, rotationAcceleration);
        this.date = date;
    }

    /** Revert a rotation/rotation rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public TimeStampedFieldAngularCoordinates<T> revert() {
        return new TimeStampedFieldAngularCoordinates<T>(date,
                                                         getRotation().revert(),
                                                         getRotation().applyInverseTo(getRotationRate().negate()),
                                                         getRotation().applyInverseTo(getRotationAcceleration().negate()));
    }

    /** Get the date.
     * @return date
     */
    public FieldAbsoluteDate<T> getDate() {
        return date;
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedFieldAngularCoordinates<T> shiftedBy(final double dt) {
        return shiftedBy(getDate().getField().getZero().add(dt));
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedFieldAngularCoordinates<T> shiftedBy(final T dt) {
        final FieldAngularCoordinates<T> sac = super.shiftedBy(dt);
        return new TimeStampedFieldAngularCoordinates<T>(date.shiftedBy(dt),
                                                         sac.getRotation(), sac.getRotationRate(), sac.getRotationAcceleration());

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
    public TimeStampedFieldAngularCoordinates<T> addOffset(final FieldAngularCoordinates<T> offset) {
        final FieldVector3D<T> rOmega    = getRotation().applyTo(offset.getRotationRate());
        final FieldVector3D<T> rOmegaDot = getRotation().applyTo(offset.getRotationAcceleration());
        return new TimeStampedFieldAngularCoordinates<T>(date,
                                                         getRotation().compose(offset.getRotation(), RotationConvention.VECTOR_OPERATOR),
                                                         getRotationRate().add(rOmega),
                                                         new FieldVector3D<T>( 1.0, getRotationAcceleration(),
                                                                               1.0, rOmegaDot,
                                                                              -1.0, FieldVector3D.crossProduct(getRotationRate(), rOmega)));
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
    public TimeStampedFieldAngularCoordinates<T> subtractOffset(final FieldAngularCoordinates<T> offset) {
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
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public static <T extends RealFieldElement<T>>
        TimeStampedFieldAngularCoordinates<T> interpolate(final AbsoluteDate date,
                                                          final AngularDerivativesFilter filter,
                                                          final Collection<TimeStampedFieldAngularCoordinates<T>> sample)
        throws OrekitException {
        return interpolate(new FieldAbsoluteDate<>(sample.iterator().next().getRotation().getQ0().getField(), date),
                           filter, sample);
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
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public static <T extends RealFieldElement<T>>
        TimeStampedFieldAngularCoordinates<T> interpolate(final FieldAbsoluteDate<T> date,
                                                          final AngularDerivativesFilter filter,
                                                          final Collection<TimeStampedFieldAngularCoordinates<T>> sample)
        throws OrekitException {

        // get field properties
        final Field<T> field = sample.iterator().next().getRotation().getQ0().getField();
        final T zero = field.getZero();
        final T one  = field.getOne();

        // set up safety elements for 2π singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear model canceling mean rotation rate
        final FieldVector3D<T> meanRate;
        if (filter != AngularDerivativesFilter.USE_R) {
            FieldVector3D<T> sum = new FieldVector3D<T>(zero, zero, zero);
            for (final TimeStampedFieldAngularCoordinates<T> datedAC : sample) {
                sum = sum.add(datedAC.getRotationRate());
            }
            meanRate = new FieldVector3D<T>(1.0 / sample.size(), sum);
        } else {
            if (sample.size() < 2) {
                throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION,
                                          sample.size());
            }
            FieldVector3D<T> sum = new FieldVector3D<T>(zero, zero, zero);
            TimeStampedFieldAngularCoordinates<T> previous = null;
            for (final TimeStampedFieldAngularCoordinates<T> datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getRotation(), datedAC.getRotation(),
                                               datedAC.date.durationFrom(previous.getDate())));
                }
                previous = datedAC;
            }
            meanRate = new FieldVector3D<T>(1.0 / (sample.size() - 1), sum);
        }
        TimeStampedFieldAngularCoordinates<T> offset =
                new TimeStampedFieldAngularCoordinates<T>(date, new FieldRotation<T>(one, zero, zero, zero, false),
                                                          meanRate, new FieldVector3D<T>(zero, zero, zero));

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

            // add sample points
            final double[] previous = new double[] {
                1.0, 0.0, 0.0, 0.0
            };

            for (final TimeStampedFieldAngularCoordinates<T> ac : sample) {

                // remove linear offset from the current coordinates
                final T dt = zero.add(ac.date.durationFrom(date));
                final TimeStampedFieldAngularCoordinates<T> fixed = ac.subtractOffset(offset.shiftedBy(dt));

                final T[][] rodrigues = getModifiedRodrigues(fixed, previous, threshold);
                if (rodrigues == null) {
                    // the sample point is close to a modified Rodrigues vector singularity
                    // we need to change the linear offset model to avoid this
                    restart = true;
                    break;
                }
                switch (filter) {
                    case USE_RRA:
                        // populate sample with rotation, rotation rate and acceleration data
                        interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1], rodrigues[2]);
                        break;
                    case USE_RR:
                        // populate sample with rotation and rotation rate data
                        interpolator.addSamplePoint(dt, rodrigues[0], rodrigues[1]);
                        break;
                    case USE_R:
                        // populate sample with rotation data only
                        interpolator.addSamplePoint(dt, rodrigues[0]);
                        break;
                    default :
                        // this should never happen
                        throw new OrekitInternalError(null);
                }
            }

            if (restart) {
                // interpolation failed, some intermediate rotation was too close to 2π
                // we need to offset all rotations to avoid the singularity
                offset = offset.addOffset(new FieldAngularCoordinates<T>(new FieldRotation<T>(new FieldVector3D<T>(one, zero, zero),
                                                                                              zero.add(epsilon),
                                                                                              RotationConvention.VECTOR_OPERATOR),
                                                                         new FieldVector3D<T>(zero, zero, zero),
                                                                         new FieldVector3D<T>(zero, zero, zero)));
            } else {
                // interpolation succeeded with the current offset
                final T[][] p = interpolator.derivatives(field.getZero(), 2);
                return createFromModifiedRodrigues(p, offset);
            }

        }

        // this should never happen
        throw new OrekitInternalError(null);

    }

    /** Create a 6 elements array.
     * @param field field to which coordinates belong
     * @param a0 first element
     * @param a1 second element
     * @param a2 third element
     * @param a3 fourth element
     * @param a4 fifth element
     * @param a5 sixth element
     * @param <T> the type of the field elements
     * @return array containing a0, a1, a2, a3, a4, a5
     */
    private static <T extends RealFieldElement<T>> T[] array6(final Field<T> field,
                                                              final T a0, final T a1, final T a2,
                                                              final T a3, final T a4, final T a5) {
        final T[] array = MathArrays.buildArray(field, 6);
        array[0] = a0;
        array[1] = a1;
        array[2] = a2;
        array[3] = a3;
        array[4] = a4;
        array[5] = a5;
        return array;
    }

    /** Create a 3x3 matrix.
     * @param field field to which coordinates belong
     * @param a00 first element, first row
     * @param a01 second element, first row
     * @param a02 third element, first row
     * @param a10 first element, second row
     * @param a11 second element, second row
     * @param a12 third element, second row
     * @param a20 first element, third row
     * @param a21 second element, third row
     * @param a22 third element, third row
     * @param <T> the type of the field elements
     * @return array containing a0, a1, a2
     */
    private static <T extends RealFieldElement<T>> T[][] matrix33(final Field<T> field,
                                                                  final T a00, final T a01, final T a02,
                                                                  final T a10, final T a11, final T a12,
                                                                  final T a20, final T a21, final T a22) {
        final T[][] matrix = MathArrays.buildArray(field, 3, 3);
        matrix[0][0] = a00;
        matrix[0][1] = a01;
        matrix[0][2] = a02;
        matrix[1][0] = a10;
        matrix[1][1] = a11;
        matrix[1][2] = a12;
        matrix[2][0] = a20;
        matrix[2][1] = a21;
        matrix[2][2] = a22;
        return matrix;
    }

    /** Convert rotation, rate and acceleration to modified Rodrigues vector and derivatives.
     * <p>
     * The modified Rodrigues vector is tan(θ/4) u where θ and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param fixed coordinates to convert, with offset already fixed
     * @param previous previous quaternion used
     * @param threshold threshold for rotations too close to 2π
     * @param <T> the type of the field elements
     * @return modified Rodrigues vector and derivative, or null if rotation is too close to 2π
     */
    private static <T extends RealFieldElement<T>> T[][] getModifiedRodrigues(final TimeStampedFieldAngularCoordinates<T> fixed,
                                                                              final double[] previous, final double threshold) {

        // make sure all interpolated points will be on the same branch
        T q0 = fixed.getRotation().getQ0();
        T q1 = fixed.getRotation().getQ1();
        T q2 = fixed.getRotation().getQ2();
        T q3 = fixed.getRotation().getQ3();
        if (MathArrays.linearCombination(q0.getReal(), previous[0],
                                         q1.getReal(), previous[1],
                                         q2.getReal(), previous[2],
                                         q3.getReal(), previous[3]) < 0) {
            q0 = q0.negate();
            q1 = q1.negate();
            q2 = q2.negate();
            q3 = q3.negate();
        }
        previous[0] = q0.getReal();
        previous[1] = q1.getReal();
        previous[2] = q2.getReal();
        previous[3] = q3.getReal();

        // check modified Rodrigues vector singularity
        if (q0.getReal() < threshold) {
            // this is an intermediate point that happens to be 2PI away from reference
            // we need to change the linear offset model to avoid this point
            return null;
        }

        final Field<T> field = q0.getField();

        final T oX    = fixed.getRotationRate().getX();
        final T oY    = fixed.getRotationRate().getY();
        final T oZ    = fixed.getRotationRate().getZ();
        final T oXDot = fixed.getRotationAcceleration().getX();
        final T oYDot = fixed.getRotationAcceleration().getY();
        final T oZDot = fixed.getRotationAcceleration().getZ();

        // first time-derivatives of the quaternion
        final T q0Dot = q0.linearCombination(q1.negate(), oX, q2.negate(), oY, q3.negate(), oZ).multiply(0.5);
        final T q1Dot = q1.linearCombination(q0,          oX, q3.negate(), oY, q2,          oZ).multiply(0.5);
        final T q2Dot = q2.linearCombination(q3,          oX, q0,          oY, q1.negate(), oZ).multiply(0.5);
        final T q3Dot = q3.linearCombination(q2.negate(), oX, q1,          oY, q0,          oZ).multiply(0.5);

        // second time-derivatives of the quaternion
        final T q0DotDot = q0.linearCombination(array6(field, q1, q2,  q3, q1Dot, q2Dot,  q3Dot),
                                                array6(field, oXDot, oYDot, oZDot, oX, oY, oZ)).multiply(-0.5);
        final T q1DotDot = q1.linearCombination(array6(field, q0, q2, q3.negate(), q0Dot, q2Dot, q3Dot.negate()),
                                                array6(field, oXDot, oZDot, oYDot, oX, oZ, oY)).multiply(0.5);
        final T q2DotDot = q2.linearCombination(array6(field, q0, q3, q1.negate(), q0Dot, q3Dot, q1Dot.negate()),
                                                array6(field, oYDot, oXDot, oZDot, oY, oX, oZ)).multiply(0.5);
        final T q3DotDot = q3.linearCombination(array6(field, q0, q1, q2.negate(), q0Dot, q1Dot, q2Dot.negate()),
                                                array6(field, oZDot, oYDot, oXDot, oZ, oY, oX)).multiply(0.5);

        // the modified Rodrigues is tan(θ/4) u where θ and u are the rotation angle and axis respectively
        // this can be rewritten using quaternion components:
        //      r (q₁ / (1+q₀), q₂ / (1+q₀), q₃ / (1+q₀))
        // applying the derivation chain rule to previous expression gives rDot and rDotDot
        final T inv          = q0.add(1.0).reciprocal();
        final T mTwoInvQ0Dot = inv.multiply(q0Dot).multiply(-2);

        final T r1       = inv.multiply(q1);
        final T r2       = inv.multiply(q2);
        final T r3       = inv.multiply(q3);

        final T mInvR1   = inv.multiply(r1).negate();
        final T mInvR2   = inv.multiply(r2).negate();
        final T mInvR3   = inv.multiply(r3).negate();

        final T r1Dot    = r1.linearCombination(inv, q1Dot, mInvR1, q0Dot);
        final T r2Dot    = r2.linearCombination(inv, q2Dot, mInvR2, q0Dot);
        final T r3Dot    = r3.linearCombination(inv, q3Dot, mInvR3, q0Dot);

        final T r1DotDot = r1.linearCombination(inv, q1DotDot, mTwoInvQ0Dot, r1Dot, mInvR1, q0DotDot);
        final T r2DotDot = r2.linearCombination(inv, q2DotDot, mTwoInvQ0Dot, r2Dot, mInvR2, q0DotDot);
        final T r3DotDot = r3.linearCombination(inv, q3DotDot, mTwoInvQ0Dot, r3Dot, mInvR3, q0DotDot);

        return matrix33(field,
                        r1,       r2,       r3,
                        r1Dot,    r2Dot,    r3Dot,
                        r1DotDot, r2DotDot, r3DotDot);

    }

    /** Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * @param r modified Rodrigues vector (with first derivatives)
     * @param offset linear offset model to add (its date must be consistent with the modified Rodrigues vector)
     * @param <T> the type of the field elements
     * @return angular coordinates
     */
    private static <T extends RealFieldElement<T>>
        TimeStampedFieldAngularCoordinates<T> createFromModifiedRodrigues(final T[][] r,
                                                                          final TimeStampedFieldAngularCoordinates<T> offset) {

        // rotation
        final T rSquared = r[0][0].multiply(r[0][0]).add(r[0][1].multiply(r[0][1])).add(r[0][2].multiply(r[0][2]));
        final T oPQ0     = rSquared.add(1).reciprocal().multiply(2);
        final T q0       = oPQ0.subtract(1);
        final T q1       = oPQ0.multiply(r[0][0]);
        final T q2       = oPQ0.multiply(r[0][1]);
        final T q3       = oPQ0.multiply(r[0][2]);

        // rotation rate
        final T oPQ02    = oPQ0.multiply(oPQ0);
        final T q0Dot    = oPQ02.negate().multiply(q0.linearCombination(r[0][0], r[1][0], r[0][1], r[1][1],  r[0][2], r[1][2]));
        final T q1Dot    = oPQ0.multiply(r[1][0]).add(r[0][0].multiply(q0Dot));
        final T q2Dot    = oPQ0.multiply(r[1][1]).add(r[0][1].multiply(q0Dot));
        final T q3Dot    = oPQ0.multiply(r[1][2]).add(r[0][2].multiply(q0Dot));
        final T oX       = q1.linearCombination(q1.negate(), q0Dot, q0,          q1Dot, q3,          q2Dot, q2.negate(), q3Dot).multiply(2);
        final T oY       = q2.linearCombination(q2.negate(), q0Dot, q3.negate(), q1Dot, q0,          q2Dot, q1,          q3Dot).multiply(2);
        final T oZ       = q3.linearCombination(q3.negate(), q0Dot, q2,          q1Dot, q1.negate(), q2Dot, q0,          q3Dot).multiply(2);

        // rotation acceleration
        final T q0DotDot = q0.getField().getOne().subtract(q0).divide(oPQ0).multiply(q0Dot).multiply(q0Dot).
                           subtract(oPQ02.multiply(q0.linearCombination(r[0][0], r[2][0], r[0][1], r[2][1], r[0][2], r[2][2]))).
                           subtract(q1Dot.multiply(q1Dot).add(q2Dot.multiply(q2Dot)).add(q3Dot.multiply(q3Dot)));
        final T q1DotDot = q1.linearCombination(oPQ0, r[2][0], r[1][0].multiply(2), q0Dot, r[0][0], q0DotDot);
        final T q2DotDot = q2.linearCombination(oPQ0, r[2][1], r[1][1].multiply(2), q0Dot, r[0][1], q0DotDot);
        final T q3DotDot = q3.linearCombination(oPQ0, r[2][2], r[1][2].multiply(2), q0Dot, r[0][2], q0DotDot);
        final T oXDot    = q1.linearCombination(q1.negate(), q0DotDot, q0,          q1DotDot, q3,          q2DotDot, q2.negate(), q3DotDot).multiply(2);
        final T oYDot    = q2.linearCombination(q2.negate(), q0DotDot, q3.negate(), q1DotDot, q0,          q2DotDot, q1,          q3DotDot).multiply(2);
        final T oZDot    = q3.linearCombination(q3.negate(), q0DotDot, q2,          q1DotDot, q1.negate(), q2DotDot, q0,          q3DotDot).multiply(2);

        return new TimeStampedFieldAngularCoordinates<T>(offset.getDate(),
                                                         new FieldRotation<T>(q0, q1, q2, q3, false),
                                                         new FieldVector3D<T>(oX, oY, oZ),
                                                         new FieldVector3D<T>(oXDot, oYDot, oZDot)).addOffset(offset);

    }

}
