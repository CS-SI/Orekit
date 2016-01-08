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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.RealFieldElement;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for FieldRotation<T>/FieldRotation<T> rate pairs, using {@link RealFieldElement}.
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
public class FieldAngularCoordinates<T extends RealFieldElement<T>>
     implements TimeShiftable<FieldAngularCoordinates<T>>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140414L;

    /** FieldRotation<T>. */
    private final FieldRotation<T> rotation;

    /** FieldRotation<T> rate. */
    private final FieldVector3D<T> rotationRate;

    /** FieldRotation<T> acceleration. */
    private final FieldVector3D<T> rotationAcceleration;

    /** Builds a rotation/rotation rate pair.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     */
    public FieldAngularCoordinates(final FieldRotation<T> rotation, final FieldVector3D<T> rotationRate) {
        this(rotation, rotationRate,
             new FieldVector3D<T>(rotation.getQ0().getField().getZero(),
                                  rotation.getQ0().getField().getZero(),
                                  rotation.getQ0().getField().getZero()));
    }

    /** Builds a FieldRotation<T>/FieldRotation<T> rate/FieldRotation<T> acceleration triplet.
     * @param rotation FieldRotation<T>
     * @param rotationRate FieldRotation<T> rate Ω (rad/s)
     * @param rotationAcceleration FieldRotation<T> acceleration dΩ/dt (rad²/s²)
     */
    public FieldAngularCoordinates(final FieldRotation<T> rotation, final FieldVector3D<T> rotationRate,
                                   final FieldVector3D<T> rotationAcceleration) {
        this.rotation             = rotation;
        this.rotationRate         = rotationRate;
        this.rotationAcceleration = rotationAcceleration;
    }

    /** Estimate FieldRotation<T> rate between two orientations.
     * <p>Estimation is based on a simple fixed rate FieldRotation<T>
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @param <T> the type of the field elements
     * @return FieldRotation<T> rate allowing to go from start to end orientations
     */
    public static <T extends RealFieldElement<T>>
        FieldVector3D<T> estimateRate(final FieldRotation<T> start,
                                      final FieldRotation<T> end,
                                      final double dt) {
        final FieldRotation<T> evolution = start.compose(end.revert(), RotationConvention.VECTOR_OPERATOR);
        return new FieldVector3D<T>(evolution.getAngle().divide(dt),
                                    evolution.getAxis(RotationConvention.VECTOR_OPERATOR));
    }

    /** Revert a FieldRotation<T>/FieldRotation<T>/FieldRotation<T> acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
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
                                                                    acc.multiply(0.5 * dt * dt),
                                                                    RotationConvention.FRAME_TRANSFORM),
                                               new FieldVector3D<T>(dt, rotationAcceleration),
                                               rotationAcceleration);

        // the quadratic contribution is a small rotation:
        // its initial angle and angular rate are both zero.
        // small rotations are almost commutative, so we append the small
        // quadratic part after the linear part as a simple offset
        return quadraticContribution.addOffset(linearPart);

    }

    /** Get the FieldRotation<T>.
     * @return the FieldRotation<T>.
     */
    public FieldRotation<T> getRotation() {
        return rotation;
    }

    /** Get the FieldRotation<T> rate.
     * @return the FieldRotation<T> rate vector (rad/s).
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
     * We consider here that the offset FieldRotation<T> is applied first and the
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

    /** Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring FieldRotation<T> rate remains the exact derivative of FieldRotation<T>.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude
     * Interpolation</a>, changing the norm of the vector to match the modified Rodrigues
     * vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at π.
     * There is still a singularity at 2π, which is handled by slightly offsetting all FieldRotation<T>s
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first time derivatives (FieldRotation<T> rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the FieldRotation<T>s.
     * </p>
     * @param date interpolation date
     * @param useRotationRates if true, use sample points FieldRotation<T> rates,
     * otherwise ignore them and use only FieldRotation<T>s
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     * @deprecated since 7.0 replaced with {@link TimeStampedFieldAngularCoordinates#interpolate(AbsoluteDate,
     * AngularDerivativesFilter, Collection)}
     */
    @Deprecated
    public static <T extends RealFieldElement<T>>
        FieldAngularCoordinates<T> interpolate(final AbsoluteDate date,
                                               final boolean useRotationRates,
                                               final Collection<Pair<AbsoluteDate,
                                               FieldAngularCoordinates<T>>> sample)
        throws OrekitException {
        final List<TimeStampedFieldAngularCoordinates<T>> list = new ArrayList<TimeStampedFieldAngularCoordinates<T>>(sample.size());
        for (final Pair<AbsoluteDate, FieldAngularCoordinates<T>> pair : sample) {
            list.add(new TimeStampedFieldAngularCoordinates<T>(pair.getFirst(),
                                                               pair.getSecond().getRotation(),
                                                               pair.getSecond().getRotationRate(),
                                                               pair.getSecond().getRotationAcceleration()));
        }
        return TimeStampedFieldAngularCoordinates.interpolate(date,
                                                              useRotationRates ? AngularDerivativesFilter.USE_RR : AngularDerivativesFilter.USE_R,
                                                              list);
    }

}
