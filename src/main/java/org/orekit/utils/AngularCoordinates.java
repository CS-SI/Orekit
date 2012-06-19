/* Copyright 2002-2012 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for rotation/rotation rate pairs.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper attitude propagation but should be sufficient for either small
 * time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link PVCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class AngularCoordinates implements TimeShiftable<AngularCoordinates>, Serializable {

    /** Fixed orientation parallel with reference frame (identity rotation and zero rotation rate). */
    public static final AngularCoordinates IDENTITY =
            new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 3750363056414336775L;

    /** Rotation. */
    private final Rotation rotation;

    /** Rotation rate. */
    private final Vector3D rotationRate;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : Identity (0 0 0).</p>
     */
    public AngularCoordinates() {
        rotation     = Rotation.IDENTITY;
        rotationRate = Vector3D.ZERO;
    }

    /** Builds a rotation/rotation rate pair.
     * @param rotation rotation
     * @param rotationRate rotation rate (rad/s)
     */
    public AngularCoordinates(final Rotation rotation, final Vector3D rotationRate) {
        this.rotation     = rotation;
        this.rotationRate = rotationRate;
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

    /** Revert a rotation/rotation rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public AngularCoordinates revert() {
        return new AngularCoordinates(rotation.revert(), rotation.applyInverseTo(rotationRate.negate()));
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
    public AngularCoordinates shiftedBy(final double dt) {
        final double rate = rotationRate.getNorm();
        if (rate == 0.0) {
            // special case for fixed rotations
            return this;
        }

        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final Rotation evolution = new Rotation(rotationRate, -rate * dt);

        return new AngularCoordinates(evolution.applyTo(rotation), rotationRate);

    }

    /** Get the rotation.
     * @return the rotation.
     */
    public Rotation getRotation() {
        return rotation;
    }

    /** Get the rotation rate.
     * @return the rotation rate vector (rad/s).
     */
    public Vector3D getRotationRate() {
        return rotationRate;
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
     * Survey of Attitude Representations</a>. This change avoids the singularity at &pi;.
     * There is still a singularity at 2&pi;, which is handled by slightly offsetting all rotations
     * when this singularity is detected.
     * </p>
     * @param date interpolation date
     * @param useRate if true, use sample points rotation rate, otherwise ignore it and use
     * only rotation
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     */
    public static AngularCoordinates interpolate(final AbsoluteDate date, final boolean useRate,
                                                 final Collection<Pair<AbsoluteDate, AngularCoordinates>> sample) {

        // set up safety elements for 2PI singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        boolean  restart = true;
        Rotation offset  = sample.iterator().next().getValue().getRotation();
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            restart = false;
            Rotation reversedOffset = offset.revert();

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            if (useRate) {
                // populate sample with rotation and rotation rate data
                for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                    final double[][] rodrigues = getModifiedRodrigues(reversedOffset, threshold, datedAC.getValue());
                    if (rodrigues == null) {
                        // the rotation is too close to 2PI, we need to offset it
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                rodrigues[0], rodrigues[1]);
                }
            } else {
                // populate sample with rotation data only, ignoring rotation rate
                for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                    final double[][] rodrigues = getModifiedRodrigues(reversedOffset, threshold, datedAC.getValue());
                    if (rodrigues == null) {
                        // the rotation is too close to 2PI, we need to offset it
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                rodrigues[0]);
                }
            }

            if (!restart) {
                // interpolation succeeded with the current offset
                return createFromModifiedRodrigues(offset, interpolator.value(0), interpolator.derivative(0));
            }

            // interpolation failed, some intermediate rotation was too close to 2PI
            // we need to offset all rotations to avoid the singularity
            offset  = new Rotation(Vector3D.PLUS_I, epsilon).applyTo(offset);
            restart = true;

        }

        // this should never happen
        throw OrekitException.createInternalError(null);

    }

    /** Convert rotation and rate to modified Rodrigues vector and derivative.
     * <p>
     * The modified Rodrigues vector is tan(&theta;/4) u where &theta; and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param reversedOffset rotation to "subtract" from the created rotation
     * @param threshold threshold for rotations too close to 2&pi;
     * @param ac coordinates to convert
     * @return Rodrigues vector and derivative, or null if rotation is too close to 2&pi;
     */
    private static double[][] getModifiedRodrigues(final Rotation reversedOffset, final double threshold,
                                                   final AngularCoordinates ac) {

        final Rotation r = ac.getRotation().applyTo(reversedOffset);
        final double q0 = r.getQ0();
        if (q0 < threshold) {
            return null;
        }
        final double q1 = r.getQ1();
        final double q2 = r.getQ2();
        final double q3 = r.getQ3();
        final double x  = ac.getRotationRate().getX();
        final double y  = ac.getRotationRate().getY();
        final double z  = ac.getRotationRate().getZ();

        // derivatives of the quaternion
        final double q0Dot = -0.5 * MathArrays.linearCombination(q1, x, q2, y,  q3, z);
        final double q1Dot =  0.5 * MathArrays.linearCombination(q0, x, q2, z, -q3, y);
        final double q2Dot =  0.5 * MathArrays.linearCombination(q0, y, q3, x, -q1, z);
        final double q3Dot =  0.5 * MathArrays.linearCombination(q0, z, q1, y, -q2, x);

        final double inv = 1.0 / (1.0 + q0);
        return new double[][] {
            {
                inv * q1,
                inv * q2,
                inv * q3
            }, {
                inv * (q1Dot - inv * q1 * q0Dot),
                inv * (q2Dot - inv * q2 * q0Dot),
                inv * (q3Dot - inv * q3 * q0Dot)
            }
        };

    }

    /** Convert a modified Rodrigues vector and derivative to angular coordinates.
     * @param offset offset to "add" to the created rotation
     * @param r modified Rodrigues vector
     * @param rDot first derivative of modified Rodrigues vector
     * @return angular coordinates
     */
    private static AngularCoordinates createFromModifiedRodrigues(final Rotation offset,
                                                                  final double[] r, final double[] rDot) {

        // rotation
        final double r2    = r[0] * r[0] + r[1] * r[1] + r[2] * r[2];
        final double inv   = 1.0 / (1 + r2);
        final double ratio = inv * (1 - r2);
        final Rotation rotation =
                new Rotation(ratio, 2 * inv * r[0], 2 * inv * r[1], 2 * inv * r[2], false).applyTo(offset);

        // rotation rate
        final Vector3D p    = new Vector3D(r[0], r[1], r[2]);
        final Vector3D pDot = new Vector3D(rDot[0], rDot[1], rDot[2]);
        final Vector3D rate = new Vector3D( 4 * ratio * inv, pDot,
                                           -8 * inv * inv, p.crossProduct(pDot),
                                            8 * inv * inv * p.dotProduct(pDot), p);

        return new AngularCoordinates(rotation, rate);

    }

}
