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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * Base class for ground pointing attitude providers.
 *
 * <p>This class is a basic model for different kind of ground pointing
 * attitude providers, such as : body center pointing, nadir pointing,
 * target pointing, etc...
 * </p>
 * <p>
 * The object <code>GroundPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     AttitudeProvider
 * @author V&eacute;ronique Pommier-Maurussane
 */
public abstract class GroundPointing implements AttitudeProvider {

    /** J axis. */
    private static final PVCoordinates PLUS_J =
            new PVCoordinates(Vector3D.PLUS_J, Vector3D.ZERO, Vector3D.ZERO);

    /** K axis. */
    private static final PVCoordinates PLUS_K =
            new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);

    /** Inertial frame. */
    private final Frame inertialFrame;

    /** Body frame. */
    private final Frame bodyFrame;

    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param bodyFrame the frame that rotates with the body
     * @since 7.1
     */
    protected GroundPointing(final Frame inertialFrame, final Frame bodyFrame) {
        if (!inertialFrame.isPseudoInertial()) {
            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                      inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;
        this.bodyFrame     = bodyFrame;
    }

    /** Get the body frame.
     * @return body frame
     */
    public Frame getBodyFrame() {
        return bodyFrame;
    }

    /** Compute the target point position/velocity in specified frame.
     * @param pvProv provider for PV coordinates
     * @param date date at which target point is requested
     * @param frame frame in which observed ground point should be provided
     * @return observed ground point position (element 0) and velocity (at index 1)
     * in specified frame
     */
    protected abstract TimeStampedPVCoordinates getTargetPV(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame);

    /** Compute the target point position/velocity in specified frame.
     * @param pvProv provider for PV coordinates
     * @param date date at which target point is requested
     * @param frame frame in which observed ground point should be provided
     * @param <T> type of the field elements
     * @return observed ground point position (element 0) and velocity (at index 1)
     * in specified frame
     * @since 9.0
     */
    protected abstract <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(FieldPVCoordinatesProvider<T> pvProv,
                                                                                                        FieldAbsoluteDate<T> date,
                                                                                                        Frame frame);

    /** Compute the target point position in specified frame.
     * @param pvProv provider for PV coordinates
     * @param date date at which target point is requested
     * @param frame frame in which observed ground point should be provided
     * @return observed ground point position in specified frame
     * @since 12.0
     */
    protected Vector3D getTargetPosition(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        return getTargetPV(pvProv, date, frame).getPosition();
    }

    /** Compute the target point position in specified frame.
     * @param pvProv provider for PV coordinates
     * @param date date at which target point is requested
     * @param frame frame in which observed ground point should be provided
     * @param <T> type of the field elements
     * @return observed ground point position in specified frame
     * @since 12.0
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetPosition(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                     final FieldAbsoluteDate<T> date,
                                                                                     final Frame frame) {
        return getTargetPV(pvProv, date, frame).getPosition();
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame) {

        // satellite-target relative vector
        final PVCoordinates pva  = pvProv.getPVCoordinates(date, inertialFrame);
        final TimeStampedPVCoordinates delta =
                new TimeStampedPVCoordinates(date, pva, getTargetPV(pvProv, date, inertialFrame));

        // spacecraft and target should be away from each other to define a pointing direction
        if (delta.getPosition().getNorm() == 0.0) {
            throw new OrekitException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET);
        }

        // attitude definition:
        // line of sight    -> +z satellite axis,
        // orbital velocity -> (z, +x) half plane
        final Vector3D p  = pva.getPosition();
        final Vector3D v  = pva.getVelocity();
        final Vector3D a  = pva.getAcceleration();
        final double   r2 = p.getNormSq();
        final double   r  = FastMath.sqrt(r2);
        final Vector3D keplerianJerk = new Vector3D(-3 * Vector3D.dotProduct(p, v) / r2, a, -a.getNorm() / r, v);
        final PVCoordinates velocity = new PVCoordinates(v, a, keplerianJerk);

        final PVCoordinates los    = delta.normalize();
        final PVCoordinates normal = PVCoordinates.crossProduct(delta, velocity).normalize();

        AngularCoordinates ac = new AngularCoordinates(los, normal, PLUS_K, PLUS_J, 1.0e-9);

        if (frame != inertialFrame) {
            // prepend transform from specified frame to inertial frame
            ac = ac.addOffset(frame.getTransformTo(inertialFrame, date).getAngular());
        }

        // build the attitude
        return new Attitude(date, frame, ac);

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>>FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv,
                                                                           final FieldAbsoluteDate<T> date,
                                                                           final Frame frame) {

        // satellite-target relative vector
        final FieldPVCoordinates<T> pva  = pvProv.getPVCoordinates(date, inertialFrame);
        final TimeStampedFieldPVCoordinates<T> delta =
                new TimeStampedFieldPVCoordinates<>(date, pva, getTargetPV(pvProv, date, inertialFrame));

        // spacecraft and target should be away from each other to define a pointing direction
        if (delta.getPosition().getNorm().getReal() == 0.0) {
            throw new OrekitException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET);
        }

        // attitude definition:
        // line of sight    -> +z satellite axis,
        // orbital velocity -> (z, +x) half plane
        final FieldVector3D<T> p  = pva.getPosition();
        final FieldVector3D<T> v  = pva.getVelocity();
        final FieldVector3D<T> a  = pva.getAcceleration();
        final T   r2 = p.getNormSq();
        final T   r  = r2.sqrt();
        final FieldVector3D<T> keplerianJerk = new FieldVector3D<>(FieldVector3D.dotProduct(p, v).multiply(-3).divide(r2), a, a.getNorm().divide(r).multiply(-1), v);
        final FieldPVCoordinates<T> velocity = new FieldPVCoordinates<>(v, a, keplerianJerk);


        final FieldPVCoordinates<T> los    = delta.normalize();

        final FieldPVCoordinates<T> normal = (delta.crossProduct(velocity)).normalize();

        final FieldVector3D<T> zero  = FieldVector3D.getZero(r.getField());
        final FieldVector3D<T> plusK = FieldVector3D.getPlusK(r.getField());
        final FieldVector3D<T> plusJ = FieldVector3D.getPlusJ(r.getField());
        FieldAngularCoordinates<T> ac =
                        new FieldAngularCoordinates<>(los, normal,
                                                      new FieldPVCoordinates<>(plusK, zero, zero),
                                                      new FieldPVCoordinates<>(plusJ, zero, zero),
                                                      1.0e-6);

        if (frame != inertialFrame) {
            // prepend transform from specified frame to inertial frame
            ac = ac.addOffset(new FieldAngularCoordinates<>(r.getField(),
                                                            frame.getTransformTo(inertialFrame, date.toAbsoluteDate()).getAngular()));
        }

        // build the attitude
        return new FieldAttitude<>(date, frame, ac);

    }

    /** {@inheritDoc} */
    @Override
    public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                        final Frame frame) {

        // satellite-target relative vector
        final PVCoordinates pva  = pvProv.getPVCoordinates(date, inertialFrame);
        final Vector3D targetPosition = getTargetPosition(pvProv, date, inertialFrame);
        final Vector3D deltaPosition = targetPosition.subtract(pva.getPosition());

        // spacecraft and target should be away from each other to define a pointing direction
        if (deltaPosition.getNorm() == 0.0) {
            throw new OrekitException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET);
        }

        final Vector3D los    = deltaPosition.normalize();
        final Vector3D normal = Vector3D.crossProduct(los, pva.getVelocity()).normalize();

        final Rotation rotation = new Rotation(los, normal, PLUS_K.getPosition(), PLUS_J.getPosition());

        if (frame != inertialFrame) {
            // prepend transform from specified frame to inertial frame
            return rotation.compose(frame.getStaticTransformTo(inertialFrame, date).getRotation(),
                    RotationConvention.VECTOR_OPERATOR);
        }

        return rotation;

    }


    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                    final FieldAbsoluteDate<T> date,
                                                                                    final Frame frame) {
        // satellite-target relative vector
        final FieldPVCoordinates<T> pva  = pvProv.getPVCoordinates(date, inertialFrame);
        final FieldVector3D<T> targetPosition = getTargetPosition(pvProv, date, inertialFrame);
        final FieldVector3D<T> deltaPosition = targetPosition.subtract(pva.getPosition());

        // spacecraft and target should be away from each other to define a pointing direction
        if (deltaPosition.getNorm().getReal() == 0.0) {
            throw new OrekitException(OrekitMessages.SATELLITE_COLLIDED_WITH_TARGET);
        }

        final FieldVector3D<T> los    = deltaPosition.normalize();
        final FieldVector3D<T> normal = FieldVector3D.crossProduct(los, pva.getVelocity()).normalize();

        final Field<T> field = date.getField();
        final FieldRotation<T> rotation = new FieldRotation<>(los, normal,
                new FieldVector3D<>(field, PLUS_K.getPosition()), new FieldVector3D<>(field, PLUS_J.getPosition()));

        if (frame != inertialFrame) {
            // prepend transform from specified frame to inertial frame
            return rotation.compose(frame.getStaticTransformTo(inertialFrame, date).getRotation(),
                    RotationConvention.VECTOR_OPERATOR);
        }

        return rotation;

    }

}
