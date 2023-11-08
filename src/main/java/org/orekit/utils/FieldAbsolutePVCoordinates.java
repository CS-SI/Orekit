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


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FieldDerivative;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;

/** Field implementation of AbsolutePVCoordinates.
 * @see AbsolutePVCoordinates
 * @author Vincent Mouraux
 * @param <T> type of the field elements
 */
public class FieldAbsolutePVCoordinates<T extends CalculusFieldElement<T>> extends TimeStampedFieldPVCoordinates<T>
    implements FieldTimeStamped<T>, FieldPVCoordinatesProvider<T> {

    /** Frame in which are defined the coordinates. */
    private final Frame frame;

    /** Build from position, velocity, acceleration.
     * @param frame the frame in which the coordinates are defined
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/sÂý)
     */
    public FieldAbsolutePVCoordinates(final Frame frame, final FieldAbsoluteDate<T> date,
                                 final FieldVector3D<T> position, final FieldVector3D<T> velocity, final FieldVector3D<T> acceleration) {
        super(date, position, velocity, acceleration);
        this.frame = frame;
    }

    /** Build from position and velocity. Acceleration is set to zero.
     * @param frame the frame in which the coordinates are defined
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public FieldAbsolutePVCoordinates(final Frame frame, final FieldAbsoluteDate<T> date,
                                 final FieldVector3D<T> position,
                                 final FieldVector3D<T> velocity) {
        this(frame, date, position, velocity, FieldVector3D.getZero(date.getField()));
    }

    /** Build from frame, date and FieldPVA coordinates.
     * @param frame the frame in which the coordinates are defined
     * @param date date of the coordinates
     * @param pva TimeStampedPVCoordinates
     */
    public FieldAbsolutePVCoordinates(final Frame frame, final FieldAbsoluteDate<T> date, final FieldPVCoordinates<T> pva) {
        super(date, pva);
        this.frame = frame;
    }

    /** Build from frame and TimeStampedFieldPVCoordinates.
     * @param frame the frame in which the coordinates are defined
     * @param pva TimeStampedFieldPVCoordinates
     */
    public FieldAbsolutePVCoordinates(final Frame frame, final TimeStampedFieldPVCoordinates<T> pva) {
        super(pva.getDate(), pva);
        this.frame = frame;
    }

    /** Multiplicative constructor
     * <p>Build a FieldAbsolutePVCoordinates from another one and a scale factor.</p>
     * <p>The TimeStampedFieldPVCoordinates built will be a * AbsPva</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param AbsPva base (unscaled) FieldAbsolutePVCoordinates
     */
    public FieldAbsolutePVCoordinates(final FieldAbsoluteDate<T> date,
                                 final T a, final FieldAbsolutePVCoordinates<T> AbsPva) {
        super(date, a, AbsPva);
        this.frame = AbsPva.frame;
    }

    /** Subtractive constructor
     * <p>Build a relative FieldAbsolutePVCoordinates from a start and an end position.</p>
     * <p>The FieldAbsolutePVCoordinates built will be end - start.</p>
     * <p>In case start and end use two different pseudo-inertial frames,
     * the new FieldAbsolutePVCoordinates arbitrarily be defined in the start frame. </p>
     * @param date date of the built coordinates
     * @param start Starting FieldAbsolutePVCoordinates
     * @param end ending FieldAbsolutePVCoordinates
     */
    public FieldAbsolutePVCoordinates(final FieldAbsoluteDate<T> date,
                                 final FieldAbsolutePVCoordinates<T> start, final FieldAbsolutePVCoordinates<T> end) {
        super(date, start, end);
        ensureIdenticalFrames(start, end);
        this.frame = start.frame;
    }

    /** Linear constructor
     * <p>Build a FieldAbsolutePVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The FieldAbsolutePVCoordinates built will be a1 * u1 + a2 * u2</p>
     * <p>In case the FieldAbsolutePVCoordinates use different pseudo-inertial frames,
     * the new FieldAbsolutePVCoordinates arbitrarily be defined in the first frame. </p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param absPv1 first base (unscaled) FieldAbsolutePVCoordinates
     * @param a2 second scale factor
     * @param absPv2 second base (unscaled) FieldAbsolutePVCoordinates
     */
    public FieldAbsolutePVCoordinates(final FieldAbsoluteDate<T> date,
                                 final T a1, final FieldAbsolutePVCoordinates<T> absPv1,
                                 final T a2, final FieldAbsolutePVCoordinates<T> absPv2) {
        super(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates());
        ensureIdenticalFrames(absPv1, absPv2);
        this.frame = absPv1.getFrame();
    }

    /** Linear constructor
     * <p>Build a FieldAbsolutePVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The FieldAbsolutePVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * <p>In case the FieldAbsolutePVCoordinates use different pseudo-inertial frames,
     * the new FieldAbsolutePVCoordinates arbitrarily be defined in the first frame. </p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param absPv1 first base (unscaled) FieldAbsolutePVCoordinates
     * @param a2 second scale factor
     * @param absPv2 second base (unscaled) FieldAbsolutePVCoordinates
     * @param a3 third scale factor
     * @param absPv3 third base (unscaled) FieldAbsolutePVCoordinates
     */
    public FieldAbsolutePVCoordinates(final FieldAbsoluteDate<T> date,
                                 final T a1, final FieldAbsolutePVCoordinates<T> absPv1,
                                 final T a2, final FieldAbsolutePVCoordinates<T> absPv2,
                                 final T a3, final FieldAbsolutePVCoordinates<T> absPv3) {
        super(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates(),
                a3, absPv3.getPVCoordinates());
        ensureIdenticalFrames(absPv1, absPv2);
        ensureIdenticalFrames(absPv1, absPv3);
        this.frame = absPv1.getFrame();
    }

    /** Linear constructor
     * <p>Build a FieldAbsolutePVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The FieldAbsolutePVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * <p>In case the FieldAbsolutePVCoordinates use different pseudo-inertial frames,
     * the new AbsolutePVCoordinates arbitrarily be defined in the first frame. </p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param absPv1 first base (unscaled) FieldAbsolutePVCoordinates
     * @param a2 second scale factor
     * @param absPv2 second base (unscaled) FieldAbsolutePVCoordinates
     * @param a3 third scale factor
     * @param absPv3 third base (unscaled) FieldAbsolutePVCoordinates
     * @param a4 fourth scale factor
     * @param absPv4 fourth base (unscaled) FieldAbsolutePVCoordinates
     */
    public FieldAbsolutePVCoordinates(final FieldAbsoluteDate<T> date,
                                 final T a1, final FieldAbsolutePVCoordinates<T> absPv1,
                                 final T a2, final FieldAbsolutePVCoordinates<T> absPv2,
                                 final T a3, final FieldAbsolutePVCoordinates<T> absPv3,
                                 final T a4, final FieldAbsolutePVCoordinates<T> absPv4) {
        super(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates(),
                a3, absPv3.getPVCoordinates(), a4, absPv4.getPVCoordinates());
        ensureIdenticalFrames(absPv1, absPv2);
        ensureIdenticalFrames(absPv1, absPv3);
        ensureIdenticalFrames(absPv1, absPv4);
        this.frame = absPv1.getFrame();
    }

    /** Builds a FieldAbsolutePVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param frame the frame in which the parameters are defined
     * @param date date of the built coordinates
     * @param p vector with time-derivatives embedded within the coordinates
     * @param <U> type of the derivative
     */
    public <U extends FieldDerivative<T, U>> FieldAbsolutePVCoordinates(final Frame frame, final FieldAbsoluteDate<T> date,
                                                                        final FieldVector3D<U> p) {
        super(date, p);
        this.frame = frame;
    }

    /** Ensure that the frames from two FieldAbsolutePVCoordinates are identical.
     * @param absPv1 first FieldAbsolutePVCoordinates
     * @param absPv2 first FieldAbsolutePVCoordinates
     * @param <T> the type of the field elements
     * @throws OrekitIllegalArgumentException if frames are different
     */
    private static <T extends CalculusFieldElement<T>> void ensureIdenticalFrames(final FieldAbsolutePVCoordinates<T> absPv1, final FieldAbsolutePVCoordinates<T> absPv2)
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
    public FieldAbsolutePVCoordinates<T> shiftedBy(final T dt) {
        final TimeStampedFieldPVCoordinates<T> spv = super.shiftedBy(dt);
        return new FieldAbsolutePVCoordinates<>(frame, spv);
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
    public FieldAbsolutePVCoordinates<T> shiftedBy(final double dt) {
        final TimeStampedFieldPVCoordinates<T> spv = super.shiftedBy(dt);
        return new FieldAbsolutePVCoordinates<>(frame, spv);
    }

    /** Create a local provider using simply Taylor expansion through {@link #shiftedBy(double)}.
     * <p>
     * The time evolution is based on a simple Taylor expansion. It is <em>not</em> intended as a
     * replacement for proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @return provider based on Taylor expansion, for small time shifts around instance date
     */
    public FieldPVCoordinatesProvider<T> toTaylorProvider() {
        return new FieldPVCoordinatesProvider<T>() {
            /** {@inheritDoc} */
            public FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> d,  final Frame f) {
                final TimeStampedFieldPVCoordinates<T> shifted   = shiftedBy(d.durationFrom(getDate()));
                final FieldStaticTransform<T>          transform = frame.getStaticTransformTo(f, d);
                return transform.transformPosition(shifted.getPosition());
            }
            /** {@inheritDoc} */
            public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> d,  final Frame f) {
                final TimeStampedFieldPVCoordinates<T> shifted   = shiftedBy(d.durationFrom(getDate()));
                final FieldTransform<T>                transform = frame.getTransformTo(f, d);
                return transform.transformPVCoordinates(shifted);
            }
        };
    }

    /** Get the frame in which the coordinates are defined.
     * @return frame in which the coordinates are defined
     */
    public Frame getFrame() {
        return frame;
    }

    /** Get the TimeStampedFieldPVCoordinates.
     * @return TimeStampedFieldPVCoordinates
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates() {
        return this;
    }

    /** Get the position in a specified frame.
     * @param outputFrame frame in which the position coordinates shall be computed
     * @return position
     * @see #getPVCoordinates(Frame)
     * @since 12.0
     */
    public FieldVector3D<T> getPosition(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // Position vector is returned directly
        if (outputFrame == frame) {
            return getPosition();
        }

        // Else, position vector is transformed to output frame
        final FieldStaticTransform<T> t = frame.getStaticTransformTo(outputFrame, getDate());
        return t.transformPosition(getPosition());
    }

    /** Get the TimeStampedFieldPVCoordinates in a specified frame.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return TimeStampedFieldPVCoordinates
     * @exception OrekitException if transformation between frames cannot be computed
     * @see #getPVCoordinates()
     */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == frame) {
            return getPVCoordinates();
        }

        // Else, PV coordinates are transformed to output frame
        final FieldTransform<T> t = frame.getTransformTo(outputFrame, getDate());
        return t.transformPVCoordinates(getPVCoordinates());
    }

    @Override
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> otherDate, final Frame outputFrame) {
        return shiftedBy(otherDate.durationFrom(getDate())).getPVCoordinates(outputFrame);
    }

    /**
     * Converts to an AbsolutePVCoordinates instance.
     * @return AbsolutePVCoordinates with same properties
     */
    public AbsolutePVCoordinates toAbsolutePVCoordinates() {
        return new AbsolutePVCoordinates(frame, this.getDate()
            .toAbsoluteDate(), this.getPVCoordinates().toPVCoordinates());
    }
}
