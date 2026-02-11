/* Copyright 2002-2026 CS GROUP
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

import org.hipparchus.analysis.differentiation.Derivative;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeOffset;

/** Position - Velocity - Acceleration linked to a date and a frame.
 */
public class AbsolutePVCoordinates implements ShiftablePVCoordinatesHolder<AbsolutePVCoordinates>, PVCoordinatesProvider {

    /** Frame in which are defined the coordinates. */
    private final Frame frame;

    /** Position-velocity-acceleration vector. */
    private final TimeStampedPVCoordinates timeStampedPVCoordinates;

    /** Build from position, velocity, acceleration.
     * @param frame the frame in which the coordinates are defined
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/sÂý)
     */
    public AbsolutePVCoordinates(final Frame frame, final AbsoluteDate date,
                                 final Vector3D position, final Vector3D velocity, final Vector3D acceleration) {
        this.timeStampedPVCoordinates = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        this.frame = frame;
    }

    /** Build from position and velocity. Acceleration is set to zero.
     * @param frame the frame in which the coordinates are defined
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public AbsolutePVCoordinates(final Frame frame, final AbsoluteDate date,
                                 final Vector3D position,
                                 final Vector3D velocity) {
        this(frame, date, position, velocity, Vector3D.ZERO);
    }

    /** Build from frame, date and PVA coordinates.
     * @param frame the frame in which the coordinates are defined
     * @param date date of the coordinates
     * @param pva TimeStampedPVCoordinates
     */
    public AbsolutePVCoordinates(final Frame frame, final AbsoluteDate date, final PVCoordinates pva) {
        this(frame, new TimeStampedPVCoordinates(date, pva));
    }

    /** Build from frame and TimeStampedPVCoordinates.
     * @param frame the frame in which the coordinates are defined
     * @param pva TimeStampedPVCoordinates
     */
    public AbsolutePVCoordinates(final Frame frame, final TimeStampedPVCoordinates pva) {
        this.timeStampedPVCoordinates = pva;
        this.frame = frame;
    }

    /** Multiplicative constructor
     * <p>Build a AbsolutePVCoordinates from another one and a scale factor.</p>
     * <p>The TimeStampedPVCoordinates built will be a * AbsPva</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param absPva base (unscaled) AbsolutePVCoordinates
     */
    public AbsolutePVCoordinates(final AbsoluteDate date,
                                 final double a, final AbsolutePVCoordinates absPva) {
        this(absPva.getFrame(), new TimeStampedPVCoordinates(date, a, absPva.getPVCoordinates()));
    }

    /** Subtractive constructor
     * <p>Build a relative AbsolutePVCoordinates from a start and an end position.</p>
     * <p>The AbsolutePVCoordinates built will be end - start.</p>
     * <p>In case start and end use two different pseudo-inertial frames,
     * the new AbsolutePVCoordinates arbitrarily be defined in the start frame. </p>
     * @param date date of the built coordinates
     * @param start Starting AbsolutePVCoordinates
     * @param end ending AbsolutePVCoordinates
     */
    public AbsolutePVCoordinates(final AbsoluteDate date,
                                 final AbsolutePVCoordinates start, final AbsolutePVCoordinates end) {
        this(start.getFrame(), new TimeStampedPVCoordinates(date, start.getPVCoordinates(), end.getPVCoordinates()));
        ensureIdenticalFrames(start, end);
    }

    /** Linear constructor
     * <p>Build a AbsolutePVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The AbsolutePVCoordinates built will be a1 * u1 + a2 * u2</p>
     * <p>In case the AbsolutePVCoordinates use different pseudo-inertial frames,
     * the new AbsolutePVCoordinates arbitrarily be defined in the first frame. </p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param absPv1 first base (unscaled) AbsolutePVCoordinates
     * @param a2 second scale factor
     * @param absPv2 second base (unscaled) AbsolutePVCoordinates
     */
    public AbsolutePVCoordinates(final AbsoluteDate date,
                                 final double a1, final AbsolutePVCoordinates absPv1,
                                 final double a2, final AbsolutePVCoordinates absPv2) {
        this(absPv1.getFrame(), new TimeStampedPVCoordinates(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates()));
        ensureIdenticalFrames(absPv1, absPv2);
    }

    /** Linear constructor
     * <p>Build a AbsolutePVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The AbsolutePVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * <p>In case the AbsolutePVCoordinates use different pseudo-inertial frames,
     * the new AbsolutePVCoordinates arbitrarily be defined in the first frame. </p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param absPv1 first base (unscaled) AbsolutePVCoordinates
     * @param a2 second scale factor
     * @param absPv2 second base (unscaled) AbsolutePVCoordinates
     * @param a3 third scale factor
     * @param absPv3 third base (unscaled) AbsolutePVCoordinates
     */
    public AbsolutePVCoordinates(final AbsoluteDate date,
                                 final double a1, final AbsolutePVCoordinates absPv1,
                                 final double a2, final AbsolutePVCoordinates absPv2,
                                 final double a3, final AbsolutePVCoordinates absPv3) {
        this(absPv1.getFrame(), new TimeStampedPVCoordinates(date, a1, absPv1.getPVCoordinates(), a2,
                absPv2.getPVCoordinates(), a3, absPv3.getPVCoordinates()));
        ensureIdenticalFrames(absPv1, absPv2);
        ensureIdenticalFrames(absPv1, absPv3);
    }

    /** Linear constructor
     * <p>Build a AbsolutePVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The AbsolutePVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * <p>In case the AbsolutePVCoordinates use different pseudo-inertial frames,
     * the new AbsolutePVCoordinates arbitrarily be defined in the first frame. </p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param absPv1 first base (unscaled) AbsolutePVCoordinates
     * @param a2 second scale factor
     * @param absPv2 second base (unscaled) AbsolutePVCoordinates
     * @param a3 third scale factor
     * @param absPv3 third base (unscaled) AbsolutePVCoordinates
     * @param a4 fourth scale factor
     * @param absPv4 fourth base (unscaled) AbsolutePVCoordinates
     */
    public AbsolutePVCoordinates(final AbsoluteDate date,
                                 final double a1, final AbsolutePVCoordinates absPv1,
                                 final double a2, final AbsolutePVCoordinates absPv2,
                                 final double a3, final AbsolutePVCoordinates absPv3,
                                 final double a4, final AbsolutePVCoordinates absPv4) {
        this(absPv1.getFrame(), new TimeStampedPVCoordinates(date, a1, absPv1.getPVCoordinates(), a2,
                absPv2.getPVCoordinates(), a3, absPv3.getPVCoordinates(), a4, absPv4.getPVCoordinates()));
        ensureIdenticalFrames(absPv1, absPv2);
        ensureIdenticalFrames(absPv1, absPv3);
        ensureIdenticalFrames(absPv1, absPv4);
    }

    /** Builds a AbsolutePVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link Derivative}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param frame the frame in which the parameters are defined
     * @param date date of the built coordinates
     * @param p vector with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     */
    public <U extends Derivative<U>> AbsolutePVCoordinates(final Frame frame, final AbsoluteDate date,
                                                           final FieldVector3D<U> p) {
        this(frame, new TimeStampedPVCoordinates(date, p));
    }

    /** Ensure that the frames from two AbsolutePVCoordinates are identical.
     * @param absPv1 first AbsolutePVCoordinates
     * @param absPv2 first AbsolutePVCoordinates
     * @throws OrekitIllegalArgumentException if frames are different
     */
    private static void ensureIdenticalFrames(final AbsolutePVCoordinates absPv1, final AbsolutePVCoordinates absPv2)
        throws OrekitIllegalArgumentException {
        if (!absPv1.frame.equals(absPv2.frame)) {
            throw new OrekitIllegalArgumentException(OrekitMessages.INCOMPATIBLE_FRAMES,
                                                     absPv1.frame.getName(), absPv2.frame.getName());
        }
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
    public AbsolutePVCoordinates shiftedBy(final double dt) {
        final TimeStampedPVCoordinates spv = timeStampedPVCoordinates.shiftedBy(dt);
        return new AbsolutePVCoordinates(frame, spv);
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
     * @since 13.0
     */
    @Override
    public AbsolutePVCoordinates shiftedBy(final TimeOffset dt) {
        final TimeStampedPVCoordinates spv = timeStampedPVCoordinates.shiftedBy(dt);
        return new AbsolutePVCoordinates(frame, spv);
    }

    /** Create a local provider using simply Taylor expansion through {@link #shiftedBy(double)}.
     * <p>
     * The time evolution is based on a simple Taylor expansion. It is <em>not</em> intended as a
     * replacement for proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @return provider based on Taylor expansion, for small time shifts around instance date
     */
    public PVCoordinatesProvider toTaylorProvider() {
        return this;
    }

    /** Get the frame in which the coordinates are defined.
     * @return frame in which the coordinates are defined
     */
    public Frame getFrame() {
        return frame;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return getPVCoordinates().getDate();
    }

    /** Get the TimeStampedPVCoordinates.
     * @return TimeStampedPVCoordinates
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        return timeStampedPVCoordinates;
    }

    /**
     * Getter for the acceleration vector.
     * @return acceleration
     */
    public Vector3D getAcceleration() {
        return timeStampedPVCoordinates.getAcceleration();
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate otherDate, final Frame outputFrame) {
        final double duration = otherDate.durationFrom(getDate());
        final Vector3D position = getPosition().add((getVelocity().add(getAcceleration().scalarMultiply(duration / 2))).scalarMultiply(duration));

        if (outputFrame == frame) {
            return position;
        }
        return frame.getStaticTransformTo(outputFrame, otherDate).transformPosition(position);
    }

    /** {@inheritDoc} */
    @Override
    @DefaultDataContext
    public String toString() {
        return timeStampedPVCoordinates.toString();
    }
}



