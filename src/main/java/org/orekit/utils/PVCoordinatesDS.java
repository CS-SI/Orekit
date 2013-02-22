/* Copyright 2002-2013 CS Systèmes d'Information
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
import org.apache.commons.math3.util.Pair;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for Position/Velocity pairs, using {@link DerivativeStructure}.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper orbit propagation (it is not even Keplerian!) but should be sufficient
 * for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link AngularCoordinatesDS}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 6.0
 * @see PVCoordinates
 */
public class PVCoordinatesDS implements TimeShiftable<PVCoordinatesDS>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130222L;

    /** The position. */
    private final Vector3DDS position;

    /** The velocity. */
    private final Vector3DDS velocity;

    /** Builds a PVCoordinates pair.
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public PVCoordinatesDS(final Vector3DDS position, final Vector3DDS velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinatesDS(final double a, final PVCoordinatesDS pv) {
        position = new Vector3DDS(a, pv.position);
        velocity = new Vector3DDS(a, pv.velocity);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinatesDS(final DerivativeStructure a, final PVCoordinatesDS pv) {
        position = new Vector3DDS(a, pv.position);
        velocity = new Vector3DDS(a, pv.velocity);
    }

    /** Multiplicative constructor
     * <p>Build a PVCoordinates from another one and a scale factor.</p>
     * <p>The PVCoordinates built will be a * pv</p>
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public PVCoordinatesDS(final DerivativeStructure a, final PVCoordinates pv) {
        position = new Vector3DDS(a, pv.getPosition());
        velocity = new Vector3DDS(a, pv.getVelocity());
    }

    /** Subtractive constructor
     * <p>Build a relative PVCoordinates from a start and an end position.</p>
     * <p>The PVCoordinates built will be end - start.</p>
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public PVCoordinatesDS(final PVCoordinatesDS start, final PVCoordinatesDS end) {
        this.position = end.position.subtract(start.position);
        this.velocity = end.velocity.subtract(start.velocity);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public PVCoordinatesDS(final double a1, final PVCoordinatesDS pv1,
                           final double a2, final PVCoordinatesDS pv2) {
        position = new Vector3DDS(a1, pv1.position, a2, pv2.position);
        velocity = new Vector3DDS(a1, pv1.velocity, a2, pv2.velocity);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public PVCoordinatesDS(final DerivativeStructure a1, final PVCoordinatesDS pv1,
                           final DerivativeStructure a2, final PVCoordinatesDS pv2) {
        position = new Vector3DDS(a1, pv1.position, a2, pv2.position);
        velocity = new Vector3DDS(a1, pv1.velocity, a2, pv2.velocity);
    }

    /** Linear constructor
     * <p>Build a PVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The PVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public PVCoordinatesDS(final DerivativeStructure a1, final PVCoordinates pv1,
                           final DerivativeStructure a2, final PVCoordinates pv2) {
        position = new Vector3DDS(a1, pv1.getPosition(), a2, pv2.getPosition());
        velocity = new Vector3DDS(a1, pv1.getVelocity(), a2, pv2.getVelocity());
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
    public PVCoordinatesDS(final double a1, final PVCoordinatesDS pv1,
                           final double a2, final PVCoordinatesDS pv2,
                           final double a3, final PVCoordinatesDS pv3) {
        position = new Vector3DDS(a1, pv1.position, a2, pv2.position, a3, pv3.position);
        velocity = new Vector3DDS(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity);
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
    public PVCoordinatesDS(final DerivativeStructure a1, final PVCoordinatesDS pv1,
                           final DerivativeStructure a2, final PVCoordinatesDS pv2,
                           final DerivativeStructure a3, final PVCoordinatesDS pv3) {
        position = new Vector3DDS(a1, pv1.position, a2, pv2.position, a3, pv3.position);
        velocity = new Vector3DDS(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity);
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
    public PVCoordinatesDS(final DerivativeStructure a1, final PVCoordinates pv1,
                           final DerivativeStructure a2, final PVCoordinates pv2,
                           final DerivativeStructure a3, final PVCoordinates pv3) {
        position = new Vector3DDS(a1, pv1.getPosition(), a2, pv2.getPosition(), a3, pv3.getPosition());
        velocity = new Vector3DDS(a1, pv1.getVelocity(), a2, pv2.getVelocity(), a3, pv3.getVelocity());
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
    public PVCoordinatesDS(final double a1, final PVCoordinatesDS pv1,
                           final double a2, final PVCoordinatesDS pv2,
                           final double a3, final PVCoordinatesDS pv3,
                           final double a4, final PVCoordinatesDS pv4) {
        position = new Vector3DDS(a1, pv1.position, a2, pv2.position, a3, pv3.position, a4, pv4.position);
        velocity = new Vector3DDS(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity, a4, pv4.velocity);
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
    public PVCoordinatesDS(final DerivativeStructure a1, final PVCoordinatesDS pv1,
                           final DerivativeStructure a2, final PVCoordinatesDS pv2,
                           final DerivativeStructure a3, final PVCoordinatesDS pv3,
                           final DerivativeStructure a4, final PVCoordinatesDS pv4) {
        position = new Vector3DDS(a1, pv1.position, a2, pv2.position, a3, pv3.position, a4, pv4.position);
        velocity = new Vector3DDS(a1, pv1.velocity, a2, pv2.velocity, a3, pv3.velocity, a4, pv4.velocity);
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
    public PVCoordinatesDS(final DerivativeStructure a1, final PVCoordinates pv1,
                           final DerivativeStructure a2, final PVCoordinates pv2,
                           final DerivativeStructure a3, final PVCoordinates pv3,
                           final DerivativeStructure a4, final PVCoordinates pv4) {
        position = new Vector3DDS(a1, pv1.getPosition(), a2, pv2.getPosition(),
                                  a3, pv3.getPosition(), a4, pv4.getPosition());
        velocity = new Vector3DDS(a1, pv1.getVelocity(), a2, pv2.getVelocity(),
                                  a3, pv3.getVelocity(), a4, pv4.getVelocity());
    }

    /** Estimate velocity between two positions.
     * <p>Estimation is based on a simple fixed velocity translation
     * during the time interval between the two positions.</p>
     * @param start start position
     * @param end end position
     * @param dt time elapsed between the dates of the two positions
     * @return velocity allowing to go from start to end positions
     */
    public static Vector3DDS estimateVelocity(final Vector3DDS start, final Vector3DDS end,
                                              final double dt) {
        final double scale = 1.0 / dt;
        return new Vector3DDS(scale, end, -scale, start);
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public PVCoordinatesDS shiftedBy(final double dt) {
        return new PVCoordinatesDS(new Vector3DDS(1, position, dt, velocity), velocity);
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
     */
    public static PVCoordinatesDS interpolate(final AbsoluteDate date, final boolean useVelocities,
                                              final Collection<Pair<AbsoluteDate, PVCoordinatesDS>> sample) {

        // get derivative structure properties
        final DerivativeStructure prototype = sample.iterator().next().getValue().getPosition().getX();
        final int parameters = prototype.getFreeParameters();
        final int order      = prototype.getOrder();

        // set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        if (useVelocities) {
            // populate sample with position and velocity data
            for (final Pair<AbsoluteDate, PVCoordinatesDS> datedPV : sample) {
                interpolator.addSamplePoint(datedPV.getKey().getDate().durationFrom(date),
                                            unfold(datedPV.getValue().getPosition()),
                                            unfold(datedPV.getValue().getVelocity()));
            }
        } else {
            // populate sample with position data, ignoring velocity
            for (final Pair<AbsoluteDate, PVCoordinatesDS> datedPV : sample) {
                interpolator.addSamplePoint(datedPV.getKey().getDate().durationFrom(date),
                                            unfold(datedPV.getValue().getPosition()));
            }
        }

        // interpolate
        final DerivativeStructure zero = new DerivativeStructure(1, 1, 0, 0.0);
        final DerivativeStructure[] p  = interpolator.value(zero);

        // build a new interpolated instance
        return new PVCoordinatesDS(fold(parameters, order, p, 0),
                                   fold(parameters, order, p, 1));

    }

    /** Unfold coordinates derivatives arrays.
     * @param v vector to unfold
     * @return all derivatives for all coordinates
     */
    private static double[] unfold(final Vector3DDS v) {
        final double[] vx = v.getX().getAllDerivatives();
        final double[] vy = v.getY().getAllDerivatives();
        final double[] vz = v.getZ().getAllDerivatives();
        final int size = vx.length;
        final double[] flat = new double[3 * size];
        System.arraycopy(vx, 0, flat, 0, size);
        System.arraycopy(vy, 0, flat, size, size);
        System.arraycopy(vz, 0, flat, 2 * size, size);
        return flat;
    }

    /** Fold coordinates derivatives arrays.
     * @param parameters number of free parameters
     * @param order derivation order
     * @param a array to fold
     * @param timeOrder time-derivation orders to fold
     * @return vector for specified time-derivative
     */
    private static Vector3DDS fold(final int parameters, final int order,
                                   final DerivativeStructure[] a,
                                   final int timeOrder) {
        final int size = a.length / 3;
        final double[] vx = new double[size];
        final double[] vy = new double[size];
        final double[] vz = new double[size];
        for (int i = 0; i < size; ++i) {
            vx[i] = a[i].getPartialDerivative(timeOrder);
            vy[i] = a[i + size].getPartialDerivative(timeOrder);
            vz[i] = a[i + 2 * size].getPartialDerivative(timeOrder);
        }
        return new Vector3DDS(new DerivativeStructure(parameters, order, vx),
                              new DerivativeStructure(parameters, order, vy),
                              new DerivativeStructure(parameters, order, vz));
    }

    /** Gets the position.
     * @return the position vector (m).
     */
    public Vector3DDS getPosition() {
        return position;
    }

    /** Gets the velocity.
     * @return the velocity vector (m/s).
     */
    public Vector3DDS getVelocity() {
        return velocity;
    }

    /** Gets the momentum.
     * <p>This vector is the p &otimes; v where p is position, v is velocity
     * and &otimes; is cross product. To get the real physical angular momentum
     * you need to multiply this vector by the mass.</p>
     * <p>The returned vector is recomputed each time this method is called, it
     * is not cached.</p>
     * @return a new instance of the momentum vector (m<sup>2</sup>/s).
     */
    public Vector3DDS getMomentum() {
        return Vector3DDS.crossProduct(position, velocity);
    }

    /** Get the opposite of the instance.
     * @return a new position-velocity which is opposite to the instance
     */
    public PVCoordinatesDS negate() {
        return new PVCoordinatesDS(position.negate(), velocity.negate());
    }

    /** Convert to a constant position-velocity without derivatives.
     * @return a constant position-velocity
     */
    public PVCoordinates toPVCoordinates() {
        return new PVCoordinates(position.toVector3D(), velocity.toVector3D());
    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append("P(").
                                  append(position.getX().getValue()).append(comma).
                                  append(position.getY().getValue()).append(comma).
                                  append(position.getZ().getValue()).append("), V(").
                                  append(velocity.getX().getValue()).append(comma).
                                  append(velocity.getY().getValue()).append(comma).
                                  append(velocity.getZ().getValue()).append(")}").toString();
    }

}
