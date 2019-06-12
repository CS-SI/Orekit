/* Copyright 2002-2019 CS Systèmes d'Information
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
import java.util.stream.Stream;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolable;
import org.orekit.time.TimeStamped;

/** Position - Velocity - Acceleration linked to a date and a frame.
 */
public class AbsolutePVCoordinates extends TimeStampedPVCoordinates
    implements TimeStamped, TimeInterpolable<AbsolutePVCoordinates>,
               Serializable, PVCoordinatesProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150824L;

    /** Frame in which are defined the coordinates. */
    private final Frame frame;

    /** Build from position, velocity, acceleration.
     * @param frame the frame in which the coordinates are defined
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/sÂý)
     */
    public AbsolutePVCoordinates(final Frame frame, final AbsoluteDate date,
                                 final Vector3D position, final Vector3D velocity, final Vector3D acceleration) {
        super(date, position, velocity, acceleration);
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
        super(date, pva);
        this.frame = frame;
    }

    /** Build from frame and TimeStampedPVCoordinates.
     * @param frame the frame in which the coordinates are defined
     * @param pva TimeStampedPVCoordinates
     */
    public AbsolutePVCoordinates(final Frame frame, final TimeStampedPVCoordinates pva) {
        super(pva.getDate(), pva);
        this.frame = frame;
    }

    /** Multiplicative constructor
     * <p>Build a AbsolutePVCoordinates from another one and a scale factor.</p>
     * <p>The TimeStampedPVCoordinates built will be a * AbsPva</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param AbsPva base (unscaled) AbsolutePVCoordinates
     */
    public AbsolutePVCoordinates(final AbsoluteDate date,
                                 final double a, final AbsolutePVCoordinates AbsPva) {
        super(date, a, AbsPva);
        this.frame = AbsPva.frame;
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
        super(date, start, end);
        ensureIdenticalFrames(start, end);
        this.frame = start.frame;
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
        super(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates());
        ensureIdenticalFrames(absPv1, absPv2);
        this.frame = absPv1.getFrame();
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
        super(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates(),
                a3, absPv3.getPVCoordinates());
        ensureIdenticalFrames(absPv1, absPv2);
        ensureIdenticalFrames(absPv1, absPv3);
        this.frame = absPv1.getFrame();
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
        super(date, a1, absPv1.getPVCoordinates(), a2, absPv2.getPVCoordinates(),
                a3, absPv3.getPVCoordinates(), a4, absPv4.getPVCoordinates());
        ensureIdenticalFrames(absPv1, absPv2);
        ensureIdenticalFrames(absPv1, absPv3);
        ensureIdenticalFrames(absPv1, absPv4);
        this.frame = absPv1.getFrame();
    }

    /** Builds a AbsolutePVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param frame the frame in which the parameters are defined
     * @param date date of the built coordinates
     * @param p vector with time-derivatives embedded within the coordinates
     */
    public AbsolutePVCoordinates(final Frame frame, final AbsoluteDate date,
            final FieldVector3D<DerivativeStructure> p) {
        super(date, p);
        this.frame = frame;
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
        final TimeStampedPVCoordinates spv = super.shiftedBy(dt);
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
        return new PVCoordinatesProvider() {
            /** {@inheritDoc} */
            public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate d,  final Frame f) {
                final TimeStampedPVCoordinates shifted   = shiftedBy(d.durationFrom(getDate()));
                final Transform                transform = frame.getTransformTo(f, d);
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

    /** Get the TimeStampedPVCoordinates.
     * @return TimeStampedPVCoordinates
     */
    public TimeStampedPVCoordinates getPVCoordinates() {
        return this;
    }

    /** Get the TimeStampedPVCoordinates in a specified frame.
     * @param outputFrame frame in which the position/velocity coordinates shall be computed
     * @return TimeStampedPVCoordinates
     * @exception OrekitException if transformation between frames cannot be computed
     * @see #getPVCoordinates()
     */
    public TimeStampedPVCoordinates getPVCoordinates(final Frame outputFrame) {
        // If output frame requested is the same as definition frame,
        // PV coordinates are returned directly
        if (outputFrame == frame) {
            return getPVCoordinates();
        }

        // Else, PV coordinates are transformed to output frame
        final Transform t = frame.getTransformTo(outputFrame, getDate());
        return t.transformPVCoordinates(getPVCoordinates());
    }

    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate otherDate, final Frame outputFrame) {
        return shiftedBy(otherDate.durationFrom(getDate())).getPVCoordinates(outputFrame);
    }

    @Override
    public AbsolutePVCoordinates interpolate(final AbsoluteDate date, final Stream<AbsolutePVCoordinates> sample) {
        return interpolate(getFrame(), date, CartesianDerivativesFilter.USE_PVA, sample);
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
     * @param frame frame for the interpolted instance
     * @param date interpolation date
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitIllegalArgumentException if some elements in the sample do not
     * have the same defining frame as other
     */
    public static AbsolutePVCoordinates interpolate(final Frame frame, final AbsoluteDate date,
                                                    final CartesianDerivativesFilter filter,
                                                    final Stream<AbsolutePVCoordinates> sample) {


        // set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        switch (filter) {
            case USE_P :
                // populate sample with position data, ignoring velocity
                sample.forEach(pv -> {
                    final Vector3D position = pv.getPosition();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                position.toArray());
                });
                break;
            case USE_PV :
                // populate sample with position and velocity data
                sample.forEach(pv -> {
                    final Vector3D position = pv.getPosition();
                    final Vector3D velocity = pv.getVelocity();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                position.toArray(), velocity.toArray());
                });
                break;
            case USE_PVA :
                // populate sample with position, velocity and acceleration data
                sample.forEach(pv -> {
                    final Vector3D position     = pv.getPosition();
                    final Vector3D velocity     = pv.getVelocity();
                    final Vector3D acceleration = pv.getAcceleration();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                position.toArray(), velocity.toArray(), acceleration.toArray());
                });
                break;
            default :
                // this should never happen
                throw new OrekitInternalError(null);
        }

        // interpolate
        final double[][] p = interpolator.derivatives(0.0, 2);

        // build a new interpolated instance
        return new AbsolutePVCoordinates(frame, date, new Vector3D(p[0]), new Vector3D(p[1]), new Vector3D(p[2]));

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
        private static final long serialVersionUID = 20150916L;

        /** Double values. */
        private double[] d;

        /** Frame in which acoordinates are defined. */
        private final Frame frame;

        /** Simple constructor.
         * @param absPva instance to serialize
         */
        private DTO(final AbsolutePVCoordinates absPva) {

            // decompose date
            final double epoch  = FastMath.floor(absPva.getDate().durationFrom(AbsoluteDate.J2000_EPOCH));
            final double offset = absPva.getDate().durationFrom(AbsoluteDate.J2000_EPOCH.shiftedBy(epoch));

            this.d = new double[] {
                epoch, offset,
                absPva.getPosition().getX(),     absPva.getPosition().getY(),     absPva.getPosition().getZ(),
                absPva.getVelocity().getX(),     absPva.getVelocity().getY(),     absPva.getVelocity().getZ(),
                absPva.getAcceleration().getX(), absPva.getAcceleration().getY(), absPva.getAcceleration().getZ()
            };
            this.frame = absPva.frame;

        }

        /** Replace the deserialized data transfer object with a {@link AbsolutePVCoordinates}.
         * @return replacement {@link AbsolutePVCoordinates}
         */
        private Object readResolve() {
            return new AbsolutePVCoordinates(frame,
                                             AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                             new Vector3D(d[2], d[3], d[ 4]),
                                             new Vector3D(d[5], d[6], d[ 7]),
                                             new Vector3D(d[8], d[9], d[10]));
        }

    }

}



