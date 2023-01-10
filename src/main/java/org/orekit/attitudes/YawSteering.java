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
package org.orekit.attitudes;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * This class handles yaw steering law.

 * <p>
 * Yaw steering is mainly used for low Earth orbiting satellites with no
 * missions-related constraints on yaw angle. It sets the yaw angle in
 * such a way the solar arrays have maximal lighting without changing the
 * roll and pitch.
 * </p>
 * <p>
 * The motion in yaw is smooth when the Sun is far from the orbital plane,
 * but gets more and more <i>square like</i> as the Sun gets closer to the
 * orbital plane. The degenerate extreme case with the Sun in the orbital
 * plane leads to a yaw angle switching between two steady states, with
 * instantaneaous π radians rotations at each switch, two times per orbit.
 * This degenerate case is clearly not operationally sound so another pointing
 * mode is chosen when Sun comes closer than some predefined threshold to the
 * orbital plane.
 * </p>
 * <p>
 * This class can handle (for now) only a theoretically perfect yaw steering
 * (i.e. the yaw angle is exactly the optimal angle). Smoothed yaw steering with a
 * few sine waves approaching the optimal angle will be added in the future if
 * needed.
 * </p>
 * <p>
 * This attitude is implemented as a wrapper on top of an underlying ground
 * pointing law that defines the roll and pitch angles.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * @see    GroundPointing
 * @author Luc Maisonobe
 */
public class YawSteering extends GroundPointing implements AttitudeProviderModifier {

    /** Pointing axis. */
    private static final PVCoordinates PLUS_Z =
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Underlying ground pointing attitude provider.  */
    private final GroundPointing groundPointingLaw;

    /** Sun motion model. */
    private final PVCoordinatesProvider sun;

    /** Normal to the plane where the Sun must remain. */
    private final PVCoordinates phasingNormal;

    /** Creates a new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param groundPointingLaw ground pointing attitude provider without yaw compensation
     * @param sun sun motion model
     * @param phasingAxis satellite axis that must be roughly in Sun direction
     * (if solar arrays rotation axis is Y, then this axis should be either +X or -X)
     * @since 7.1
     */
    public YawSteering(final Frame inertialFrame,
                       final GroundPointing groundPointingLaw,
                       final PVCoordinatesProvider sun,
                       final Vector3D phasingAxis) {
        super(inertialFrame, groundPointingLaw.getBodyFrame());
        this.groundPointingLaw = groundPointingLaw;
        this.sun = sun;
        this.phasingNormal = new PVCoordinates(Vector3D.crossProduct(Vector3D.PLUS_K, phasingAxis).normalize(),
                                               Vector3D.ZERO,
                                               Vector3D.ZERO);
    }

    /** Get the underlying (ground pointing) attitude provider.
     * @return underlying attitude provider, which in this case is a {@link GroundPointing} instance
     */
    public AttitudeProvider getUnderlyingAttitudeProvider() {
        return groundPointingLaw;
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                final AbsoluteDate date, final Frame frame) {
        return groundPointingLaw.getTargetPV(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                        final FieldAbsoluteDate<T> date,
                                                                                        final Frame frame) {
        return groundPointingLaw.getTargetPV(pvProv, date, frame);
    }

    /** Compute the base system state at given date, without compensation.
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @return satellite base attitude state, i.e without compensation.
     */
    public Attitude getBaseState(final PVCoordinatesProvider pvProv,
                                 final AbsoluteDate date, final Frame frame) {
        return groundPointingLaw.getAttitude(pvProv, date, frame);
    }

    /** Compute the base system state at given date, without compensation.
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @param <T> type of the field elements
     * @return satellite base attitude state, i.e without compensation.
     * @since 9.0
     */
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getBaseState(final FieldPVCoordinatesProvider<T> pvProv,
                                                                         final FieldAbsoluteDate<T> date, final Frame frame) {
        return groundPointingLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame) {

        // attitude from base attitude provider
        final Attitude base = getBaseState(pvProv, date, frame);

        // Compensation rotation definition :
        //  . Z satellite axis is unchanged
        //  . phasing axis shall be aligned to sun direction
        final PVCoordinates sunDirection = new PVCoordinates(pvProv.getPVCoordinates(date, frame),
                                                             sun.getPVCoordinates(date, frame));
        final PVCoordinates sunNormal =
                PVCoordinates.crossProduct(PLUS_Z, base.getOrientation().applyTo(sunDirection));
        final TimeStampedAngularCoordinates compensation =
                new TimeStampedAngularCoordinates(date,
                                                  PLUS_Z, sunNormal.normalize(),
                                                  PLUS_Z, phasingNormal,
                                                  1.0e-9);

        // add compensation
        return new Attitude(frame, compensation.addOffset(base.getOrientation()));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                        final FieldAbsoluteDate<T> date, final Frame frame) {

        final Field<T>              field = date.getField();
        final FieldVector3D<T>      zero  = FieldVector3D.getZero(field);
        final FieldPVCoordinates<T> plusZ = new FieldPVCoordinates<>(FieldVector3D.getPlusK(field), zero, zero);

        // attitude from base attitude provider
        final FieldAttitude<T> base = getBaseState(pvProv, date, frame);

        // Compensation rotation definition :
        //  . Z satellite axis is unchanged
        //  . phasing axis shall be aligned to sun direction
        final FieldPVCoordinates<T> sunDirection =
                        new FieldPVCoordinates<>(pvProv.getPVCoordinates(date, frame),
                                                 new FieldPVCoordinates<>(field,
                                                                          sun.getPVCoordinates(date.toAbsoluteDate(), frame)));
        final FieldPVCoordinates<T> sunNormal =
                plusZ.crossProduct(base.getOrientation().applyTo(sunDirection));
        final TimeStampedFieldAngularCoordinates<T> compensation =
                new TimeStampedFieldAngularCoordinates<>(date,
                                                         plusZ, sunNormal.normalize(),
                                                         plusZ, new FieldPVCoordinates<>(field, phasingNormal),
                                                         1.0e-9);

        // add compensation
        return new FieldAttitude<>(frame, compensation.addOffset(base.getOrientation()));

    }

}
