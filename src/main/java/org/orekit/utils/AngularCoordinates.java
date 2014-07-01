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
                                      rotationRate.add(rotation.applyTo(offset.rotationRate)));
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
     * Survey of Attitude Representations</a>. This change avoids the singularity at &pi;.
     * There is still a singularity at 2&pi;, which is handled by slightly offsetting all rotations
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
                                                       pair.getSecond().getRotation(), pair.getSecond().getRotationRate()));
        }
        return TimeStampedAngularCoordinates.interpolate(date,
                                                         useRotationRates ? AngularDerivativesFilter.USE_RR : AngularDerivativesFilter.USE_R,
                                                         list);
    }

}
