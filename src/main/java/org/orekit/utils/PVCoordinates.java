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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.TimeShiftable;

/** Simple container for Position/Velocity/Acceleration triplets.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple quadratic model. It is <em>not</em> intended as a replacement for
 * proper orbit propagation (it is not even Keplerian!) but should be sufficient
 * for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link AngularCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
public class PVCoordinates implements TimeShiftable<PVCoordinates>, Serializable {

    /** Fixed position/velocity at origin (both p, v and a are zero vectors). */
    public static final PVCoordinates ZERO = new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 20140407L;

    /** The position. */
    private final Vector3D position;

    /** The velocity. */
    private final Vector3D velocity;

    /** The acceleration. */
    private final Vector3D acceleration;

    /** Simple constructor.
     * <p> Set the Coordinates to default : (0 0 0), (0 0 0), (0 0 0).</p>
     */
    public PVCoordinates() {
        position     = Vector3D.ZERO;
        velocity     = Vector3D.ZERO;
        acceleration = Vector3D.ZERO;
    }

    /** Builds a PVCoordinates triplet with zero acceleration.
     * <p>Acceleration is set to zero</p>
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public PVCoordinates(final Vector3D position, final Vector3D velocity) {
        this.position     = position;
        this.velocity     = velocity;
        this.acceleration = Vector3D.ZERO;
    }

    /** Builds a PVCoordinates triplet.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public PVCoordinates(final Vector3D position, final Vector3D velocity, final Vector3D acceleration) {
        this.position     = position;
        this.velocity     = velocity;
        this.acceleration = acceleration;
    }

    /** Multiplicative constructor.
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinates(final double a, final PVCoordinates pv) {
        position     = new Vector3D(a, pv.position);
        velocity     = new Vector3D(a, pv.velocity);
        acceleration = new Vector3D(a, pv.acceleration);
    }

    /** Subtractive constructor.
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public PVCoordinates(final PVCoordinates start, final PVCoordinates end) {
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
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2) {
        position     = new Vector3D(a1, pv1.position,     a2, pv2.position);
        velocity     = new Vector3D(a1, pv1.velocity,     a2, pv2.velocity);
        acceleration = new Vector3D(a1, pv1.acceleration, a2, pv2.acceleration);
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
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2,
                         final double a3, final PVCoordinates pv3) {
        position     = new Vector3D(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new Vector3D(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new Vector3D(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
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
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2,
                         final double a3, final PVCoordinates pv3,
                         final double a4, final PVCoordinates pv4) {
        position     = new Vector3D(a1, pv1.position,     a2, pv2.position,
                                    a3, pv3.position,     a4, pv4.position);
        velocity     = new Vector3D(a1, pv1.velocity,     a2, pv2.velocity,
                                    a3, pv3.velocity,     a4, pv4.velocity);
        acceleration = new Vector3D(a1, pv1.acceleration, a2, pv2.acceleration,
                                    a3, pv3.acceleration, a4, pv4.acceleration);
    }

    /** Builds a PVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param p vector with time-derivatives embedded within the coordinates
     */
    public PVCoordinates(final FieldVector3D<DerivativeStructure> p) {
        position = new Vector3D(p.getX().getReal(), p.getY().getReal(), p.getZ().getReal());
        if (p.getX().getOrder() >= 1) {
            velocity = new Vector3D(p.getX().getPartialDerivative(1),
                                    p.getY().getPartialDerivative(1),
                                    p.getZ().getPartialDerivative(1));
            if (p.getX().getOrder() >= 2) {
                acceleration = new Vector3D(p.getX().getPartialDerivative(2),
                                            p.getY().getPartialDerivative(2),
                                            p.getZ().getPartialDerivative(2));
            } else {
                acceleration = Vector3D.ZERO;
            }
        } else {
            velocity     = Vector3D.ZERO;
            acceleration = Vector3D.ZERO;
        }
    }

    /** Transform the instance to a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The {@link DerivativeStructure} coordinates correspond to time-derivatives up
     * to the user-specified order.
     * </p>
     * @param order derivation order for the vector components (must be either 0, 1 or 2)
     * @return vector with time-derivatives embedded within the coordinates
     * @exception OrekitException if the user specified order is too large
     */
    public FieldVector3D<DerivativeStructure> toDerivativeStructureVector(final int order)
        throws OrekitException {

        final DerivativeStructure x;
        final DerivativeStructure y;
        final DerivativeStructure z;
        switch(order) {
            case 0 :
                x = new DerivativeStructure(1, 0, position.getX());
                y = new DerivativeStructure(1, 0, position.getY());
                z = new DerivativeStructure(1, 0, position.getZ());
                break;
            case 1 :
                x = new DerivativeStructure(1, 1, position.getX(), velocity.getX());
                y = new DerivativeStructure(1, 1, position.getY(), velocity.getY());
                z = new DerivativeStructure(1, 1, position.getZ(), velocity.getZ());
                break;
            case 2 :
                x = new DerivativeStructure(1, 2, position.getX(), velocity.getX(), acceleration.getX());
                y = new DerivativeStructure(1, 2, position.getY(), velocity.getY(), acceleration.getY());
                z = new DerivativeStructure(1, 2, position.getZ(), velocity.getZ(), acceleration.getZ());
                break;
            default :
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, order);
        }

        return new FieldVector3D<DerivativeStructure>(x, y, z);

    }

    /** Estimate velocity between two positions.
     * <p>Estimation is based on a simple fixed velocity translation
     * during the time interval between the two positions.</p>
     * @param start start position
     * @param end end position
     * @param dt time elapsed between the dates of the two positions
     * @return velocity allowing to go from start to end positions
     */
    public static Vector3D estimateVelocity(final Vector3D start, final Vector3D end, final double dt) {
        final double scale = 1.0 / dt;
        return new Vector3D(scale, end, -scale, start);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple Taylor expansion. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public PVCoordinates shiftedBy(final double dt) {
        return new PVCoordinates(new Vector3D(1, position, dt, velocity, 0.5 * dt * dt, acceleration),
                                 new Vector3D(1, velocity, dt, acceleration),
                                 acceleration);
    }

    /** Gets the position.
     * @return the position vector (m).
     */
    public Vector3D getPosition() {
        return position;
    }

    /** Gets the velocity.
     * @return the velocity vector (m/s).
     */
    public Vector3D getVelocity() {
        return velocity;
    }

    /** Gets the acceleration.
     * @return the acceleration vector (m/s²).
     */
    public Vector3D getAcceleration() {
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
    public Vector3D getMomentum() {
        return Vector3D.crossProduct(position, velocity);
    }

    /**
     * Get the angular velocity (spin) of this point as seen from the origin.
     *
     * <p> The angular velocity vector is parallel to the {@link #getMomentum()
     * angular momentum} and is computed by ω = p &times; v / ||p||²
     *
     * @return the angular velocity vector
     * @see <a href="http://en.wikipedia.org/wiki/Angular_velocity">Angular Velocity on
     *      Wikipedia</a>
     */
    public Vector3D getAngularVelocity() {
        return this.getMomentum().scalarMultiply(1.0 / this.getPosition().getNormSq());
    }

    /** Get the opposite of the instance.
     * @return a new position-velocity which is opposite to the instance
     */
    public PVCoordinates negate() {
        return new PVCoordinates(position.negate(), velocity.negate(), acceleration.negate());
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
    public PVCoordinates normalize() {
        final double   inv     = 1.0 / position.getNorm();
        final Vector3D u       = new Vector3D(inv, position);
        final Vector3D v       = new Vector3D(inv, velocity);
        final Vector3D w       = new Vector3D(inv, acceleration);
        final double   uv      = Vector3D.dotProduct(u, v);
        final double   v2      = Vector3D.dotProduct(v, v);
        final double   uw      = Vector3D.dotProduct(u, w);
        final Vector3D uDot    = new Vector3D(1, v, -uv, u);
        final Vector3D uDotDot = new Vector3D(1, w, -2 * uv, v, 3 * uv * uv - v2 - uw, u);
        return new PVCoordinates(u, uDot, uDotDot);
    }

    /** Compute the cross-product of two instances.
     * @param pv1 first instances
     * @param pv2 second instances
     * @return the cross product v1 ^ v2 as a new instance
     */
    public static PVCoordinates crossProduct(final PVCoordinates pv1, final PVCoordinates pv2) {
        final Vector3D p1 = pv1.position;
        final Vector3D v1 = pv1.velocity;
        final Vector3D a1 = pv1.acceleration;
        final Vector3D p2 = pv2.position;
        final Vector3D v2 = pv2.velocity;
        final Vector3D a2 = pv2.acceleration;
        return new PVCoordinates(Vector3D.crossProduct(p1, p2),
                                 new Vector3D(1, Vector3D.crossProduct(p1, v2),
                                              1, Vector3D.crossProduct(v1, p2)),
                                 new Vector3D(1, Vector3D.crossProduct(p1, a2),
                                              2, Vector3D.crossProduct(v1, v2),
                                              1, Vector3D.crossProduct(a1, p2)));
    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
                append(position.getX()).append(comma).
                append(position.getY()).append(comma).
                append(position.getZ()).append("), V(").
                append(velocity.getX()).append(comma).
                append(velocity.getY()).append(comma).
                append(velocity.getZ()).append("), A(").
                append(acceleration.getX()).append(comma).
                append(acceleration.getY()).append(comma).
                append(acceleration.getZ()).append(")}").toString();
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140723L;

        /** Double values. */
        private double[] d;

        /** Simple constructor.
         * @param pv instance to serialize
         */
        private DTO(final PVCoordinates pv) {
            this.d = new double[] {
                pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ(),
            };
        }

        /** Replace the deserialized data transfer object with a {@link PVCoordinates}.
         * @return replacement {@link PVCoordinates}
         */
        private Object readResolve() {
            return new PVCoordinates(new Vector3D(d[0], d[1], d[2]),
                                     new Vector3D(d[3], d[4], d[5]),
                                     new Vector3D(d[6], d[7], d[8]));
        }

    }

}
