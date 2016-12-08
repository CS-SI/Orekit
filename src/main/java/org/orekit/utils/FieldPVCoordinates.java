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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.time.TimeShiftable;

/** Simple container for Position/Velocity pairs, using {@link RealFieldElement}.
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
public class FieldPVCoordinates<T extends RealFieldElement<T>>
    implements TimeShiftable<FieldPVCoordinates<T>> {

    /** The position. */
    private final FieldVector3D<T> position;

    /** The velocity. */
    private final FieldVector3D<T> velocity;

    /** The acceleration. */
    private final FieldVector3D<T> acceleration;

    /** Builds a PVCoordinates triplet with zero acceleration.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public FieldPVCoordinates(final FieldVector3D<T> position, final FieldVector3D<T> velocity) {
        this.position     = position;
        this.velocity     = velocity;
        final T zero      = position.getX().getField().getZero();
        this.acceleration = new FieldVector3D<T>(zero, zero, zero);
    }

    /** Builds a PVCoordinates triplet.
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

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a, final FieldPVCoordinates<T> pv) {
        position     = new FieldVector3D<T>(a, pv.position);
        velocity     = new FieldVector3D<T>(a, pv.velocity);
        acceleration = new FieldVector3D<T>(a, pv.acceleration);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a, final FieldPVCoordinates<T> pv) {
        position     = new FieldVector3D<T>(a, pv.position);
        velocity     = new FieldVector3D<T>(a, pv.velocity);
        acceleration = new FieldVector3D<T>(a, pv.acceleration);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a, final PVCoordinates pv) {
        position     = new FieldVector3D<T>(a, pv.getPosition());
        velocity     = new FieldVector3D<T>(a, pv.getVelocity());
        acceleration = new FieldVector3D<T>(a, pv.getAcceleration());
    }

    /** Subtractive constructor
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

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final double a1, final FieldPVCoordinates<T> pv1,
                              final double a2, final FieldPVCoordinates<T> pv2) {
        position     = new FieldVector3D<T>(a1, pv1.position, a2, pv2.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity, a2, pv2.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final FieldPVCoordinates<T> pv1,
                              final T a2, final FieldPVCoordinates<T> pv2) {
        position     = new FieldVector3D<T>(a1, pv1.position, a2, pv2.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity, a2, pv2.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public FieldPVCoordinates(final T a1, final PVCoordinates pv1,
                              final T a2, final PVCoordinates pv2) {
        position     = new FieldVector3D<T>(a1, pv1.getPosition(), a2, pv2.getPosition());
        velocity     = new FieldVector3D<T>(a1, pv1.getVelocity(), a2, pv2.getVelocity());
        acceleration = new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration());
    }

    /** Linear constructor
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
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /** Linear constructor
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
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
    }

    /** Linear constructor
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
        position     = new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition());
        velocity     = new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity());
        acceleration = new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration());
    }

    /** Linear constructor
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
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position,     a4, pv4.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Linear constructor
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
        position     = new FieldVector3D<T>(a1, pv1.position,     a2, pv2.position,     a3, pv3.position,     a4, pv4.position);
        velocity     = new FieldVector3D<T>(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new FieldVector3D<T>(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Linear constructor
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
        position     = new FieldVector3D<T>(a1, pv1.getPosition(),     a2, pv2.getPosition(),
                                            a3, pv3.getPosition(),     a4, pv4.getPosition());
        velocity     = new FieldVector3D<T>(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),
                                            a3, pv3.getVelocity(),     a4, pv4.getVelocity());
        acceleration = new FieldVector3D<T>(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(),
                                            a3, pv3.getAcceleration(), a4, pv4.getAcceleration());
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
    public static <T extends RealFieldElement<T>> FieldVector3D<T> estimateVelocity(final FieldVector3D<T> start,
                                                                                    final FieldVector3D<T> end,
                                                                                    final double dt) {
        final double scale = 1.0 / dt;
        return new FieldVector3D<T>(scale, end, -scale, start);
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
    public FieldPVCoordinates<T> shiftedBy(final double dt) {
        return new FieldPVCoordinates<T>(new FieldVector3D<T>(1, position, dt, velocity, 0.5 * dt * dt, acceleration),
                                         new FieldVector3D<T>(1, velocity, dt, acceleration),
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
    public FieldPVCoordinates<T> shiftedBy(final T dt) {
        final T one = dt.getField().getOne();
        return new FieldPVCoordinates<T>(new FieldVector3D<T>(one, position,
                                                              dt, velocity,
                                                              dt.multiply(dt).multiply(0.5), acceleration),
                                         new FieldVector3D<T>(one, velocity,
                                                              dt, acceleration),
                                         acceleration);
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
        return new FieldPVCoordinates<T>(position.negate(), velocity.negate(), acceleration.negate());
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
        final FieldVector3D<T> u       = new FieldVector3D<T>(inv, position);
        final FieldVector3D<T> v       = new FieldVector3D<T>(inv, velocity);
        final FieldVector3D<T> w       = new FieldVector3D<T>(inv, acceleration);
        final T   uv      = FieldVector3D.dotProduct(u, v);
        final T   v2      = FieldVector3D.dotProduct(v, v);
        final T   uw      = FieldVector3D.dotProduct(u, w);
        final FieldVector3D<T> uDot    = new FieldVector3D<T>(inv.getField().getOne(), v,
                                                              uv.multiply(-1), u);
        final FieldVector3D<T> uDotDot = new FieldVector3D<T>(inv.getField().getOne(), w,
                                                              uv.multiply(-2), v,
                                                              uv.multiply(uv).multiply(3).subtract(v2).subtract(uw), u);
        return new FieldPVCoordinates<T>(u, uDot, uDotDot);
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
        return new FieldPVCoordinates<T>(FieldVector3D.crossProduct(p1, p2),
                                 new FieldVector3D<T>(1, FieldVector3D.crossProduct(p1, v2),
                                                      1, FieldVector3D.crossProduct(v1, p2)),
                                 new FieldVector3D<T>(1, FieldVector3D.crossProduct(p1, a2),
                                                      2, FieldVector3D.crossProduct(v1, v2),
                                                      1, FieldVector3D.crossProduct(a1, p2)));
    }

    /** Convert to a constant position-velocity without derivatives.
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
        return new StringBuffer().append('{').append("P(").
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

}
