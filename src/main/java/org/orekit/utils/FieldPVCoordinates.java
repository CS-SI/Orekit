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
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldBlendable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.FieldTimeShiftable;

/** Simple container for Position/Velocity pairs, using {@link CalculusFieldElement}.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper orbit propagation (it is not even Keplerian!) but should be sufficient
 * for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link FieldAngularCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 6.0
 * @see PVCoordinates
 */
public class FieldPVCoordinates<T extends CalculusFieldElement<T>>
    implements FieldTimeShiftable<FieldPVCoordinates<T>, T>, FieldBlendable<FieldPVCoordinates<T>, T> {

    /** The position. */
    private final FieldVector3D<T> position;

    /** The velocity. */
    private final FieldVector3D<T> velocity;

    /** The acceleration. */
    private final FieldVector3D<T> acceleration;

    /** Builds a FieldPVCoordinates triplet with zero acceleration.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public FieldPVCoordinates(final FieldVector3D<T> position, final FieldVector3D<T> velocity) {
        this.position     = position;
        this.velocity     = velocity;
        final T zero      = position.getX().getField().getZero();
        this.acceleration = new FieldVector3D<>(zero, zero, zero);
    }

    /** Builds a FieldPVCoordinates triplet.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public FieldPVCoordinates(final FieldVector3D<T> position, final FieldVector3D<T> velocity,
                              final FieldVector3D<T> acceleration) {
        this.position     = position;
        this.velocity     = velocity;
        this.acceleration = acceleration;
    }

    /** Builds a FieldPVCoordinates from a field and a regular PVCoordinates.
     * @param field field for the components
     * @param pv PVCoordinates triplet to convert
     */
    public FieldPVCoordinates(final Field<T> field, final PVCoordinates pv) {
        this.position     = new FieldVector3D<>(field, pv.getPosition());
        this.velocity     = new FieldVector3D<>(field, pv.getVelocity());
        this.acceleration = new FieldVector3D<>(field, pv.getAcceleration());
    }

    /** Multiplicative constructor.
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a, final FieldPVCoordinates<T> pv) {
        position     = new FieldVector3D<>(a, pv.position);
        velocity     = new FieldVector3D<>(a, pv.velocity);
        acceleration = new FieldVector3D<>(a, pv.acceleration);
    }

    /** Multiplicative constructor.
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a, final FieldPVCoordinates<T> pv) {
        position     = new FieldVector3D<>(a, pv.position);
        velocity     = new FieldVector3D<>(a, pv.velocity);
        acceleration = new FieldVector3D<>(a, pv.acceleration);
    }

    /** Multiplicative constructor.
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a, final PVCoordinates pv) {
        position     = new FieldVector3D<>(a, pv.getPosition());
        velocity     = new FieldVector3D<>(a, pv.getVelocity());
        acceleration = new FieldVector3D<>(a, pv.getAcceleration());
    }

    /** Subtractive constructor.
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public FieldPVCoordinates(final FieldPVCoordinates<T> start, final FieldPVCoordinates<T> end) {
        this.position     = end.position.subtract(start.position);
        this.velocity     = end.velocity.subtract(start.velocity);
        this.acceleration = end.acceleration.subtract(start.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                              final double a2, final FieldPVCoordinates<T> pv2) {
        position     = new FieldVector3D<>(a1, pv1.position, a2, pv2.position);
        velocity     = new FieldVector3D<>(a1, pv1.velocity, a2, pv2.velocity);
        acceleration = new FieldVector3D<>(a1, pv1.acceleration, a2, pv2.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2) {
        position     = new FieldVector3D<>(a1, pv1.position, a2, pv2.position);
        velocity     = new FieldVector3D<>(a1, pv1.velocity, a2, pv2.velocity);
        acceleration = new FieldVector3D<>(a1, pv1.acceleration, a2, pv2.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2) {
        position     = new FieldVector3D<>(a1, pv1.getPosition(), a2, pv2.getPosition());
        velocity     = new FieldVector3D<>(a1, pv1.getVelocity(), a2, pv2.getVelocity());
        acceleration = new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration());
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                              final double a2, final FieldPVCoordinates<T> pv2,
                              final double a3, final FieldPVCoordinates<T> pv3) {
        position     = new FieldVector3D<>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new FieldVector3D<>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new FieldVector3D<>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2,
                              final T a3, final FieldPVCoordinates<T> pv3) {
        position     = new FieldVector3D<>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new FieldVector3D<>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new FieldVector3D<>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2,
                              final T a3, final PVCoordinates pv3) {
        position     = new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition());
        velocity     = new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity());
        acceleration = new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration());
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                              final double a2, final FieldPVCoordinates<T> pv2,
                              final double a3, final FieldPVCoordinates<T> pv3,
                              final double a4, final FieldPVCoordinates<T> pv4) {
        position     = new FieldVector3D<>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position,     a4, pv4.position);
        velocity     = new FieldVector3D<>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new FieldVector3D<>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2,
                              final T a3, final FieldPVCoordinates<T> pv3,
                              final T a4, final FieldPVCoordinates<T> pv4) {
        position     = new FieldVector3D<>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position,     a4, pv4.position);
        velocity     = new FieldVector3D<>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new FieldVector3D<>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Linear constructor.
     * <p>Build a PVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2,
                              final T a3, final PVCoordinates pv3,
                              final T a4, final PVCoordinates pv4) {
        position     = new FieldVector3D<>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                           a3, pv3.getPosition(),     a4, pv4.getPosition());
        velocity     = new FieldVector3D<>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                           a3, pv3.getVelocity(),     a4, pv4.getVelocity());
        acceleration = new FieldVector3D<>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                           a3, pv3.getAcceleration(), a4, pv4.getAcceleration());
    }

    /** Builds a FieldPVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param p vector with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     * @since 9.2
     */
    public <U extends FieldDerivative<T, U>> FieldPVCoordinates(final FieldVector3D<U> p) {
        position = new FieldVector3D<>(p.getX().getValue(), p.getY().getValue(), p.getZ().getValue());
        if (p.getX().getOrder() >= 1) {
            velocity = new FieldVector3D<>(p.getX().getPartialDerivative(1),
                                           p.getY().getPartialDerivative(1),
                                           p.getZ().getPartialDerivative(1));
            if (p.getX().getOrder() >= 2) {
                acceleration = new FieldVector3D<>(p.getX().getPartialDerivative(2),
                                                   p.getY().getPartialDerivative(2),
                                                   p.getZ().getPartialDerivative(2));
            } else {
                acceleration = FieldVector3D.getZero(position.getX().getField());
            }
        } else {
            final FieldVector3D<T> zero = FieldVector3D.getZero(position.getX().getField());
            velocity     = zero;
            acceleration = zero;
        }
    }

    /** Get fixed position/velocity at origin (both p, v and a are zero vectors).
     * @param field field for the components
     * @param <T> the type of the field elements
     * @return a new fixed position/velocity at origin
     */
    public static <T extends CalculusFieldElement<T>> FieldPVCoordinates<T> getZero(final Field<T> field) {
        return new FieldPVCoordinates<>(field, PVCoordinates.ZERO);
    }

    /** Transform the instance to a {@link FieldVector3D}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The {@link FieldDerivativeStructure} coordinates correspond to time-derivatives up
     * to the user-specified order.
     * </p>
     * @param order derivation order for the vector components (must be either 0, 1 or 2)
     * @return vector with time-derivatives embedded within the coordinates
          * @since 9.2
     */
    public FieldVector3D<FieldDerivativeStructure<T>> toDerivativeStructureVector(final int order) {

        final FDSFactory<T> factory;
        final FieldDerivativeStructure<T> x;
        final FieldDerivativeStructure<T> y;
        final FieldDerivativeStructure<T> z;
        switch (order) {
            case 0 :
                factory = new FDSFactory<>(getPosition().getX().getField(), 1, order);
                x = factory.build(position.getX());
                y = factory.build(position.getY());
                z = factory.build(position.getZ());
                break;
            case 1 :
                factory = new FDSFactory<>(getPosition().getX().getField(), 1, order);
                x = factory.build(position.getX(), velocity.getX());
                y = factory.build(position.getY(), velocity.getY());
                z = factory.build(position.getZ(), velocity.getZ());
                break;
            case 2 :
                factory = new FDSFactory<>(getPosition().getX().getField(), 1, order);
                x = factory.build(position.getX(), velocity.getX(), acceleration.getX());
                y = factory.build(position.getY(), velocity.getY(), acceleration.getY());
                z = factory.build(position.getZ(), velocity.getZ(), acceleration.getZ());
                break;
            default :
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

        return new FieldVector3D<>(x, y, z);

    }

    /** Transform the instance to a {@link FieldVector3D}&lt;{@link FieldUnivariateDerivative1}&gt;.
     * <p>
     * The {@link FieldUnivariateDerivative1} coordinates correspond to time-derivatives up
     * to the order 1.
     * </p>
     * @return vector with time-derivatives embedded within the coordinates
     * @see #toUnivariateDerivative2Vector()
     * @since 10.2
     */
    public FieldVector3D<FieldUnivariateDerivative1<T>> toUnivariateDerivative1Vector() {

        final FieldUnivariateDerivative1<T> x = new FieldUnivariateDerivative1<>(position.getX(), velocity.getX());
        final FieldUnivariateDerivative1<T> y = new FieldUnivariateDerivative1<>(position.getY(), velocity.getY());
        final FieldUnivariateDerivative1<T> z = new FieldUnivariateDerivative1<>(position.getZ(), velocity.getZ());

        return new FieldVector3D<>(x, y, z);
    }

    /** Transform the instance to a {@link FieldVector3D}&lt;{@link FieldUnivariateDerivative2}&gt;.
     * <p>
     * The {@link FieldUnivariateDerivative2} coordinates correspond to time-derivatives up
     * to the order 2.
     * </p>
     * @return vector with time-derivatives embedded within the coordinates
     * @see #toUnivariateDerivative1Vector()
     * @since 10.2
     */
    public FieldVector3D<FieldUnivariateDerivative2<T>> toUnivariateDerivative2Vector() {

        final FieldUnivariateDerivative2<T> x = new FieldUnivariateDerivative2<>(position.getX(), velocity.getX(), acceleration.getX());
        final FieldUnivariateDerivative2<T> y = new FieldUnivariateDerivative2<>(position.getY(), velocity.getY(), acceleration.getY());
        final FieldUnivariateDerivative2<T> z = new FieldUnivariateDerivative2<>(position.getZ(), velocity.getZ(), acceleration.getZ());

        return new FieldVector3D<>(x, y, z);
    }

    /** Transform the instance to a {@link FieldPVCoordinates}&lt;{@link FieldDerivativeStructure}&gt;.
     * <p>
     * The {@link FieldDerivativeStructure} coordinates correspond to time-derivatives up
     * to the user-specified order. As both the instance components {@link #getPosition() position},
     * {@link #getVelocity() velocity} and {@link #getAcceleration() acceleration} and the
     * {@link FieldDerivativeStructure#getPartialDerivative(int...) derivatives} of the components
     * holds time-derivatives, there are several ways to retrieve these derivatives. If for example
     * the {@code order} is set to 2, then both {@code pv.getPosition().getX().getPartialDerivative(2)},
     * {@code pv.getVelocity().getX().getPartialDerivative(1)} and
     * {@code pv.getAcceleration().getX().getValue()} return the exact same value.
     * </p>
     * <p>
     * If derivation order is 1, the first derivative of acceleration will be computed as a
     * Keplerian-only jerk. If derivation order is 2, the second derivative of velocity (which
     * is also the first derivative of acceleration) will be computed as a Keplerian-only jerk,
     * and the second derivative of acceleration will be computed as a Keplerian-only jounce.
     * </p>
     * @param order derivation order for the vector components (must be either 0, 1 or 2)
     * @return pv coordinates with time-derivatives embedded within the coordinates
          * @since 9.2
     */
    public FieldPVCoordinates<FieldDerivativeStructure<T>> toDerivativeStructurePV(final int order) {

        final FDSFactory<T> factory;
        final FieldDerivativeStructure<T> x0;
        final FieldDerivativeStructure<T> y0;
        final FieldDerivativeStructure<T> z0;
        final FieldDerivativeStructure<T> x1;
        final FieldDerivativeStructure<T> y1;
        final FieldDerivativeStructure<T> z1;
        final FieldDerivativeStructure<T> x2;
        final FieldDerivativeStructure<T> y2;
        final FieldDerivativeStructure<T> z2;
        switch (order) {
            case 0 :
                factory = new FDSFactory<>(getPosition().getX().getField(), 1, order);
                x0 = factory.build(position.getX());
                y0 = factory.build(position.getY());
                z0 = factory.build(position.getZ());
                x1 = factory.build(velocity.getX());
                y1 = factory.build(velocity.getY());
                z1 = factory.build(velocity.getZ());
                x2 = factory.build(acceleration.getX());
                y2 = factory.build(acceleration.getY());
                z2 = factory.build(acceleration.getZ());
                break;
            case 1 : {
                factory = new FDSFactory<>(getPosition().getX().getField(), 1, order);
                final T                r2            = position.getNormSq();
                final T                r             = r2.sqrt();
                final T                pvOr2         = FieldVector3D.dotProduct(position, velocity).divide(r2);
                final T                a             = acceleration.getNorm();
                final T                aOr           = a.divide(r);
                final FieldVector3D<T> keplerianJerk = new FieldVector3D<>(pvOr2.multiply(-3), acceleration,
                                                                           aOr.negate(), velocity);
                x0 = factory.build(position.getX(),     velocity.getX());
                y0 = factory.build(position.getY(),     velocity.getY());
                z0 = factory.build(position.getZ(),     velocity.getZ());
                x1 = factory.build(velocity.getX(),     acceleration.getX());
                y1 = factory.build(velocity.getY(),     acceleration.getY());
                z1 = factory.build(velocity.getZ(),     acceleration.getZ());
                x2 = factory.build(acceleration.getX(), keplerianJerk.getX());
                y2 = factory.build(acceleration.getY(), keplerianJerk.getY());
                z2 = factory.build(acceleration.getZ(), keplerianJerk.getZ());
                break;
            }
            case 2 : {
                factory = new FDSFactory<>(getPosition().getX().getField(), 1, order);
                final T                r2              = position.getNormSq();
                final T                r               = r2.sqrt();
                final T                pvOr2           = FieldVector3D.dotProduct(position, velocity).divide(r2);
                final T                a               = acceleration.getNorm();
                final T                aOr             = a.divide(r);
                final FieldVector3D<T> keplerianJerk   = new FieldVector3D<>(pvOr2.multiply(-3), acceleration,
                                                                             aOr.negate(), velocity);
                final T                v2              = velocity.getNormSq();
                final T                pa              = FieldVector3D.dotProduct(position, acceleration);
                final T                aj              = FieldVector3D.dotProduct(acceleration, keplerianJerk);
                final FieldVector3D<T> keplerianJounce = new FieldVector3D<>(v2.add(pa).multiply(-3).divide(r2).add(pvOr2.multiply(pvOr2).multiply(15)).subtract(aOr), acceleration,
                                                                             aOr.multiply(4).multiply(pvOr2).subtract(aj.divide(a.multiply(r))), velocity);
                x0 = factory.build(position.getX(),     velocity.getX(),      acceleration.getX());
                y0 = factory.build(position.getY(),     velocity.getY(),      acceleration.getY());
                z0 = factory.build(position.getZ(),     velocity.getZ(),      acceleration.getZ());
                x1 = factory.build(velocity.getX(),     acceleration.getX(),  keplerianJerk.getX());
                y1 = factory.build(velocity.getY(),     acceleration.getY(),  keplerianJerk.getY());
                z1 = factory.build(velocity.getZ(),     acceleration.getZ(),  keplerianJerk.getZ());
                x2 = factory.build(acceleration.getX(), keplerianJerk.getX(), keplerianJounce.getX());
                y2 = factory.build(acceleration.getY(), keplerianJerk.getY(), keplerianJounce.getY());
                z2 = factory.build(acceleration.getZ(), keplerianJerk.getZ(), keplerianJounce.getZ());
                break;
            }
            default :
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

        return new FieldPVCoordinates<>(new FieldVector3D<>(x0, y0, z0),
                                        new FieldVector3D<>(x1, y1, z1),
                                        new FieldVector3D<>(x2, y2, z2));

    }


    /** Transform the instance to a {@link FieldPVCoordinates}&lt;{@link FieldUnivariateDerivative1}&gt;.
     * <p>
     * The {@link FieldUnivariateDerivative1} coordinates correspond to time-derivatives up
     * to the order 1.
     * The first derivative of acceleration will be computed as a Keplerian-only jerk.
     * </p>
     * @return pv coordinates with time-derivatives embedded within the coordinates
     * @since 10.2
     */
    public FieldPVCoordinates<FieldUnivariateDerivative1<T>> toUnivariateDerivative1PV() {

        final T   r2            = position.getNormSq();
        final T   r             = FastMath.sqrt(r2);
        final T   pvOr2         = FieldVector3D.dotProduct(position, velocity).divide(r2);
        final T   a             = acceleration.getNorm();
        final T   aOr           = a.divide(r);
        final FieldVector3D<T> keplerianJerk   = new FieldVector3D<>(pvOr2.multiply(-3), acceleration,
                                                                     aOr.negate(), velocity);

        final FieldUnivariateDerivative1<T> x0 = new FieldUnivariateDerivative1<>(position.getX(),     velocity.getX());
        final FieldUnivariateDerivative1<T> y0 = new FieldUnivariateDerivative1<>(position.getY(),     velocity.getY());
        final FieldUnivariateDerivative1<T> z0 = new FieldUnivariateDerivative1<>(position.getZ(),     velocity.getZ());
        final FieldUnivariateDerivative1<T> x1 = new FieldUnivariateDerivative1<>(velocity.getX(),     acceleration.getX());
        final FieldUnivariateDerivative1<T> y1 = new FieldUnivariateDerivative1<>(velocity.getY(),     acceleration.getY());
        final FieldUnivariateDerivative1<T> z1 = new FieldUnivariateDerivative1<>(velocity.getZ(),     acceleration.getZ());
        final FieldUnivariateDerivative1<T> x2 = new FieldUnivariateDerivative1<>(acceleration.getX(), keplerianJerk.getX());
        final FieldUnivariateDerivative1<T> y2 = new FieldUnivariateDerivative1<>(acceleration.getY(), keplerianJerk.getY());
        final FieldUnivariateDerivative1<T> z2 = new FieldUnivariateDerivative1<>(acceleration.getZ(), keplerianJerk.getZ());

        return new FieldPVCoordinates<>(new FieldVector3D<>(x0, y0, z0),
                                        new FieldVector3D<>(x1, y1, z1),
                                        new FieldVector3D<>(x2, y2, z2));

    }

    /** Transform the instance to a {@link FieldPVCoordinates}&lt;{@link FieldUnivariateDerivative2}&gt;.
     * <p>
     * The {@link FieldUnivariateDerivative2} coordinates correspond to time-derivatives up
     * to the order 2.
     * As derivation order is 2, the second derivative of velocity (which
     * is also the first derivative of acceleration) will be computed as a Keplerian-only jerk,
     * and the second derivative of acceleration will be computed as a Keplerian-only jounce.
     * </p>
     * @return pv coordinates with time-derivatives embedded within the coordinates
     * @since 10.2
     */
    public FieldPVCoordinates<FieldUnivariateDerivative2<T>> toUnivariateDerivative2PV() {

        final T                r2              = position.getNormSq();
        final T                r               = r2.sqrt();
        final T                pvOr2           = FieldVector3D.dotProduct(position, velocity).divide(r2);
        final T                a               = acceleration.getNorm();
        final T                aOr             = a.divide(r);
        final FieldVector3D<T> keplerianJerk   = new FieldVector3D<>(pvOr2.multiply(-3), acceleration,
                                                                     aOr.negate(), velocity);
        final T                v2              = velocity.getNormSq();
        final T                pa              = FieldVector3D.dotProduct(position, acceleration);
        final T                aj              = FieldVector3D.dotProduct(acceleration, keplerianJerk);
        final FieldVector3D<T> keplerianJounce = new FieldVector3D<>(v2.add(pa).multiply(-3).divide(r2).add(pvOr2.multiply(pvOr2).multiply(15)).subtract(aOr), acceleration,
                                                                     aOr.multiply(4).multiply(pvOr2).subtract(aj.divide(a.multiply(r))), velocity);

        final FieldUnivariateDerivative2<T> x0 = new FieldUnivariateDerivative2<>(position.getX(),     velocity.getX(),      acceleration.getX());
        final FieldUnivariateDerivative2<T> y0 = new FieldUnivariateDerivative2<>(position.getY(),     velocity.getY(),      acceleration.getY());
        final FieldUnivariateDerivative2<T> z0 = new FieldUnivariateDerivative2<>(position.getZ(),     velocity.getZ(),      acceleration.getZ());
        final FieldUnivariateDerivative2<T> x1 = new FieldUnivariateDerivative2<>(velocity.getX(),     acceleration.getX(),  keplerianJerk.getX());
        final FieldUnivariateDerivative2<T> y1 = new FieldUnivariateDerivative2<>(velocity.getY(),     acceleration.getY(),  keplerianJerk.getY());
        final FieldUnivariateDerivative2<T> z1 = new FieldUnivariateDerivative2<>(velocity.getZ(),     acceleration.getZ(),  keplerianJerk.getZ());
        final FieldUnivariateDerivative2<T> x2 = new FieldUnivariateDerivative2<>(acceleration.getX(), keplerianJerk.getX(), keplerianJounce.getX());
        final FieldUnivariateDerivative2<T> y2 = new FieldUnivariateDerivative2<>(acceleration.getY(), keplerianJerk.getY(), keplerianJounce.getY());
        final FieldUnivariateDerivative2<T> z2 = new FieldUnivariateDerivative2<>(acceleration.getZ(), keplerianJerk.getZ(), keplerianJounce.getZ());

        return new FieldPVCoordinates<>(new FieldVector3D<>(x0, y0, z0),
                                        new FieldVector3D<>(x1, y1, z1),
                                        new FieldVector3D<>(x2, y2, z2));

    }

    /** Estimate velocity between two positions.
     * <p>Estimation is based on a simple fixed velocity translation
     * during the time interval between the two positions.</p>
     * @param start start position
     * @param end end position
     * @param dt time elapsed between the dates of the two positions
     * @param <T> the type of the field elements
     * @return velocity allowing to go from start to end positions
     */
    public static <T extends CalculusFieldElement<T>> FieldVector3D<T> estimateVelocity(final FieldVector3D<T> start,
                                                                                    final FieldVector3D<T> end,
                                                                                    final double dt) {
        final double scale = 1.0 / dt;
        return new FieldVector3D<>(scale, end, -scale, start);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple quadratic model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public FieldPVCoordinates<T> shiftedBy(final double dt) {
        return new FieldPVCoordinates<>(new FieldVector3D<>(1, position, dt, velocity, 0.5 * dt * dt, acceleration),
                                        new FieldVector3D<>(1, velocity, dt, acceleration),
                                        acceleration);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple quadratic model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    @Override
    public FieldPVCoordinates<T> shiftedBy(final T dt) {
        final T one = dt.getField().getOne();
        return new FieldPVCoordinates<>(positionShiftedBy(dt),
                                        new FieldVector3D<>(one, velocity, dt, acceleration),
                                        acceleration);
    }

    /**
     * Get a time-shifted position. Same as {@link #shiftedBy(CalculusFieldElement)} except
     * that only the sifted position is returned.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple Taylor expansion. It is <em>not</em> intended as a replacement
     * for proper orbit propagation (it is not even Keplerian!) but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     *
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is
     * immutable)
     * @since 11.2
     */
    public FieldVector3D<T> positionShiftedBy(final T dt) {
        final T one = dt.getField().getOne();
        return new FieldVector3D<>(one, position, dt, velocity, dt.multiply(dt).multiply(0.5), acceleration);
    }

    /** Gets the position.
     * @return the position vector (m).
     */
    public FieldVector3D<T> getPosition() {
        return position;
    }

    /** Gets the velocity.
     * @return the velocity vector (m/s).
     */
    public FieldVector3D<T> getVelocity() {
        return velocity;
    }

    /** Gets the acceleration.
     * @return the acceleration vector (m/s²).
     */
    public FieldVector3D<T> getAcceleration() {
        return acceleration;
    }

    /** Gets the momentum.
     * <p>This vector is the p &otimes; v where p is position, v is velocity
     * and &otimes; is cross product. To get the real physical angular momentum
     * you need to multiply this vector by the mass.</p>
     * <p>The returned vector is recomputed each time this method is called, it
     * is not cached.</p>
     * @return a new instance of the momentum vector (m²/s).
     */
    public FieldVector3D<T> getMomentum() {
        return FieldVector3D.crossProduct(position, velocity);
    }

    /**
     * Get the angular velocity (spin) of this point as seen from the origin.
     *
     * <p> The angular velocity vector is parallel to the {@link #getMomentum()
     * angular * momentum} and is computed by ω = p &times; v / ||p||²
     *
     * @return the angular velocity vector
     * @see <a href="http://en.wikipedia.org/wiki/Angular_velocity">Angular Velocity on
     *      Wikipedia</a>
     */
    public FieldVector3D<T> getAngularVelocity() {
        return this.getMomentum().scalarMultiply(
                this.getPosition().getNormSq().reciprocal());
    }

    /** Get the opposite of the instance.
     * @return a new position-velocity which is opposite to the instance
     */
    public FieldPVCoordinates<T> negate() {
        return new FieldPVCoordinates<>(position.negate(), velocity.negate(), acceleration.negate());
    }

    /** Normalize the position part of the instance.
     * <p>
     * The computed coordinates first component (position) will be a
     * normalized vector, the second component (velocity) will be the
     * derivative of the first component (hence it will generally not
     * be normalized), and the third component (acceleration) will be the
     * derivative of the second component (hence it will generally not
     * be normalized).
     * </p>
     * @return a new instance, with first component normalized and
     * remaining component computed to have consistent derivatives
     */
    public FieldPVCoordinates<T> normalize() {
        final T   inv     = position.getNorm().reciprocal();
        final FieldVector3D<T> u       = new FieldVector3D<>(inv, position);
        final FieldVector3D<T> v       = new FieldVector3D<>(inv, velocity);
        final FieldVector3D<T> w       = new FieldVector3D<>(inv, acceleration);
        final T   uv      = FieldVector3D.dotProduct(u, v);
        final T   v2      = FieldVector3D.dotProduct(v, v);
        final T   uw      = FieldVector3D.dotProduct(u, w);
        final FieldVector3D<T> uDot    = new FieldVector3D<>(inv.getField().getOne(), v,
                                                             uv.multiply(-1), u);
        final FieldVector3D<T> uDotDot = new FieldVector3D<>(inv.getField().getOne(), w,
                                                             uv.multiply(-2), v,
                                                             uv.multiply(uv).multiply(3).subtract(v2).subtract(uw), u);
        return new FieldPVCoordinates<>(u, uDot, uDotDot);
    }

    /** Compute the cross-product of two instances.
     * @param pv2 second instances
     * @return the cross product v1 ^ v2 as a new instance
     */
    public FieldPVCoordinates<T> crossProduct(final FieldPVCoordinates<T> pv2) {
        final FieldVector3D<T> p1 = position;
        final FieldVector3D<T> v1 = velocity;
        final FieldVector3D<T> a1 = acceleration;
        final FieldVector3D<T> p2 = pv2.position;
        final FieldVector3D<T> v2 = pv2.velocity;
        final FieldVector3D<T> a2 = pv2.acceleration;
        return new FieldPVCoordinates<>(FieldVector3D.crossProduct(p1, p2),
                                        new FieldVector3D<>(1, FieldVector3D.crossProduct(p1, v2),
                                                            1, FieldVector3D.crossProduct(v1, p2)),
                                        new FieldVector3D<>(1, FieldVector3D.crossProduct(p1, a2),
                                                            2, FieldVector3D.crossProduct(v1, v2),
                                                            1, FieldVector3D.crossProduct(a1, p2)));
    }

    /** Convert to a constant position-velocity.
     * @return a constant position-velocity
     */
    public PVCoordinates toPVCoordinates() {
        return new PVCoordinates(position.toVector3D(), velocity.toVector3D(), acceleration.toVector3D());
    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuilder().append('{').append("P(").
                                  append(position.getX().getReal()).append(comma).
                                  append(position.getY().getReal()).append(comma).
                                  append(position.getZ().getReal()).append("), V(").
                                  append(velocity.getX().getReal()).append(comma).
                                  append(velocity.getY().getReal()).append(comma).
                                  append(velocity.getZ().getReal()).append("), A(").
                                  append(acceleration.getX().getReal()).append(comma).
                                  append(acceleration.getY().getReal()).append(comma).
                                  append(acceleration.getZ().getReal()).append(")}").toString();
    }

    /** {@inheritDoc} */
    @Override
    public FieldPVCoordinates<T> blendArithmeticallyWith(final FieldPVCoordinates<T> other,
                                                         final T blendingValue)
            throws MathIllegalArgumentException {
        final FieldVector3D<T> blendedPosition     = position.blendArithmeticallyWith(other.getPosition(), blendingValue);
        final FieldVector3D<T> blendedVelocity     = velocity.blendArithmeticallyWith(other.getVelocity(), blendingValue);
        final FieldVector3D<T> blendedAcceleration = acceleration.blendArithmeticallyWith(other.getAcceleration(), blendingValue);

        return new FieldPVCoordinates<>(blendedPosition, blendedVelocity, blendedAcceleration);
    }
}
