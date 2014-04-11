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
import java.util.Collection;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
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

    /** Multiplicative constructor
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

    /** Subtractive constructor
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

    /** Linear constructor
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
    public PVCoordinates(final double a1, final PVCoordinates pv1,
                         final double a2, final PVCoordinates pv2,
                         final double a3, final PVCoordinates pv3) {
        position     = new Vector3D(a1, pv1.position,     a2, pv2.position,     a3, pv3.position);
        velocity     = new Vector3D(a1, pv1.velocity,     a2, pv2.velocity,     a3, pv3.velocity);
        acceleration = new Vector3D(a1, pv1.acceleration, a2, pv2.acceleration, a3, pv3.acceleration);
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
     * a simple quadratic model. It is <em>not</em> intended as a replacement for
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

    /** Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param useVelocities if true, use sample points velocities,
     * otherwise ignore them and use only positions
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @deprecated as of 7.1, replaced with {@link #interpolate(AbsoluteDate, PVASampleFilter, Collection)}
     */
    @Deprecated
    public static PVCoordinates interpolate(final AbsoluteDate date, final boolean useVelocities,
                                            final Collection<Pair<AbsoluteDate, PVCoordinates>> sample) {
        return interpolate(date,
                           useVelocities ? PVASampleFilter.SAMPLE_PV : PVASampleFilter.SAMPLE_P,
                           sample);
    }

    /** Interpolate position-velocity-acceleration.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities) or second time
     * derivatives (accelerations) from sample can be ignored, the interpolated
     * instance always includes interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives to extract from sample
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     */
    public static PVCoordinates interpolate(final AbsoluteDate date, final PVASampleFilter filter,
                                            final Collection<Pair<AbsoluteDate, PVCoordinates>> sample) {

        // set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        switch (filter) {
        case SAMPLE_PVA :
            // populate sample with position, velocity and acceleration data
            for (final Pair<AbsoluteDate, PVCoordinates> datedPV : sample) {
                final Vector3D position     = datedPV.getValue().getPosition();
                final Vector3D velocity     = datedPV.getValue().getVelocity();
                final Vector3D acceleration = datedPV.getValue().getAcceleration();
                interpolator.addSamplePoint(datedPV.getKey().getDate().durationFrom(date),
                                            new double[] {
                                                position.getX(), position.getY(), position.getZ()
                                            }, new double[] {
                                                velocity.getX(), velocity.getY(), velocity.getZ()
                                            }, new double[] {
                                                acceleration.getX(), acceleration.getY(), acceleration.getZ()
                                            });
            }
            break;
        case SAMPLE_PV :
            // populate sample with position and velocity data
            for (final Pair<AbsoluteDate, PVCoordinates> datedPV : sample) {
                final Vector3D position = datedPV.getValue().getPosition();
                final Vector3D velocity = datedPV.getValue().getVelocity();
                interpolator.addSamplePoint(datedPV.getKey().getDate().durationFrom(date),
                                            new double[] {
                                                position.getX(), position.getY(), position.getZ()
                                            }, new double[] {
                                                velocity.getX(), velocity.getY(), velocity.getZ()
                                            });
            }
            break;
        case SAMPLE_P :
            // populate sample with position data, ignoring velocity
            for (final Pair<AbsoluteDate, PVCoordinates> datedPV : sample) {
                final Vector3D position = datedPV.getValue().getPosition();
                interpolator.addSamplePoint(datedPV.getKey().getDate().durationFrom(date),
                                            new double[] {
                                                position.getX(), position.getY(), position.getZ()
                                            });
            }
            break;
        default :
            // this should never happen
            throw OrekitException.createInternalError(null);
        }

        // interpolate
        final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
        final DerivativeStructure[] p  = interpolator.value(zero);

        // build a new interpolated instance
        return new PVCoordinates(new Vector3D(p[0].getValue(),
                                              p[1].getValue(),
                                              p[2].getValue()),
                                 new Vector3D(p[0].getPartialDerivative(1),
                                              p[1].getPartialDerivative(1),
                                              p[2].getPartialDerivative(1)),
                                 new Vector3D(p[0].getPartialDerivative(2),
                                              p[1].getPartialDerivative(2),
                                              p[2].getPartialDerivative(2)));

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
     * @return a new instance of the momentum vector (m<sup>2</sup>/s).
     */
    public Vector3D getMomentum() {
        return Vector3D.crossProduct(position, velocity);
    }

    /**
     * Get the angular velocity (spin) of this point as seen from the origin.
     * <p/>
     * The angular velocity vector is parallel to the {@link #getMomentum() angular
     * momentum} and is computed by &omega; = p &times; v / ||p||<sup>2</sup>
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

}
