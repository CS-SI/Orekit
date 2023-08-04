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
import org.hipparchus.analysis.differentiation.FieldDerivative;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link FieldAngularCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedFieldAngularCoordinates<T extends CalculusFieldElement<T>>
    extends FieldAngularCoordinates<T> implements FieldTimeStamped<T> {

    /** The date. */
    private final FieldAbsoluteDate<T> date;

    /** Build the rotation that transforms a pair of pv coordinates into another pair.

     * <p><em>WARNING</em>! This method requires much more stringent assumptions on
     * its parameters than the similar {@link org.hipparchus.geometry.euclidean.threed.Rotation#Rotation(
     * org.hipparchus.geometry.euclidean.threed.Vector3D, org.hipparchus.geometry.euclidean.threed.Vector3D,
     * org.hipparchus.geometry.euclidean.threed.Vector3D, org.hipparchus.geometry.euclidean.threed.Vector3D)
     * constructor} from the {@link org.hipparchus.geometry.euclidean.threed.Rotation Rotation} class.
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
     */
    public TimeStampedFieldAngularCoordinates (final AbsoluteDate date,
                                               final FieldPVCoordinates<T> u1, final FieldPVCoordinates<T> u2,
                                               final FieldPVCoordinates<T> v1, final FieldPVCoordinates<T> v2,
                                               final double tolerance) {
        this(new FieldAbsoluteDate<>(u1.getPosition().getX().getField(), date),
             u1, u2, v1, v2, tolerance);
    }

    /** Build the rotation that transforms a pair of pv coordinates into another pair.

     * <p><em>WARNING</em>! This method requires much more stringent assumptions on
     * its parameters than the similar {@link org.hipparchus.geometry.euclidean.threed.Rotation#Rotation(
     * org.hipparchus.geometry.euclidean.threed.Vector3D, org.hipparchus.geometry.euclidean.threed.Vector3D,
     * org.hipparchus.geometry.euclidean.threed.Vector3D, org.hipparchus.geometry.euclidean.threed.Vector3D)
     * constructor} from the {@link org.hipparchus.geometry.euclidean.threed.Rotation Rotation} class.
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
     */
    public TimeStampedFieldAngularCoordinates (final FieldAbsoluteDate<T> date,
                                               final FieldPVCoordinates<T> u1, final FieldPVCoordinates<T> u2,
                                               final FieldPVCoordinates<T> v1, final FieldPVCoordinates<T> v2,
                                               final double tolerance) {
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

    /** Builds an instance for a regular {@link TimeStampedAngularCoordinates}.
     * @param field fields to which the elements belong
     * @param ac coordinates to convert
     * @since 9.0
     */
    public TimeStampedFieldAngularCoordinates(final Field<T> field,
                                              final TimeStampedAngularCoordinates ac) {
        this(new FieldAbsoluteDate<>(field, ac.getDate()),
             new FieldRotation<>(field, ac.getRotation()),
             new FieldVector3D<>(field, ac.getRotationRate()),
             new FieldVector3D<>(field, ac.getRotationAcceleration()));
    }

    /** Builds a TimeStampedFieldAngularCoordinates from  a {@link FieldRotation}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The rotation components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param date coordinates date
     * @param r rotation with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     * @since 9.2
     */
    public <U extends FieldDerivative<T, U>> TimeStampedFieldAngularCoordinates(final FieldAbsoluteDate<T> date,
                                                                                final FieldRotation<U> r) {
        super(r);
        this.date = date;
    }

    /** Revert a rotation/rotation rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public TimeStampedFieldAngularCoordinates<T> revert() {
        return new TimeStampedFieldAngularCoordinates<>(date,
                                                        getRotation().revert(),
                                                        getRotation().applyInverseTo(getRotationRate().negate()),
                                                        getRotation().applyInverseTo(getRotationAcceleration().negate()));
    }

    /** {@inheritDoc} */
    @Override
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
        return new TimeStampedFieldAngularCoordinates<>(date.shiftedBy(dt),
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
        return new TimeStampedFieldAngularCoordinates<>(date,
                                                        getRotation().compose(offset.getRotation(), RotationConvention.VECTOR_OPERATOR),
                                                        getRotationRate().add(rOmega),
                                                        new FieldVector3D<>( 1.0, getRotationAcceleration(),
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

}
