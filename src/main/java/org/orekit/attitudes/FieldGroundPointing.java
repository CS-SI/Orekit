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
package org.orekit.attitudes;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


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
public abstract class FieldGroundPointing<T extends RealFieldElement<T>> implements FieldAttitudeProvider<T> {

    /** Inertial frame. */
    private final Frame inertialFrame;

    /** Body frame. */
    private final Frame bodyFrame;


    /** Default constructor.
     * Build a new instance with arbitrary default elements.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param bodyFrame the frame that rotates with the body
     * @exception OrekitException if the first frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    protected FieldGroundPointing(final Frame inertialFrame, final Frame bodyFrame)
        throws OrekitException {
        if (!inertialFrame.isPseudoInertial()) {

            throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                      inertialFrame.getName());
        }
        this.inertialFrame = inertialFrame;
        this.bodyFrame     = bodyFrame;
    }


    /** J axis.
     * @param field field used by default
     * @return J axis in FieldPVCoordinates
     * */
    private FieldPVCoordinates<T> PLUS_J(final Field<T> field) {
        return new FieldPVCoordinates<T>(new FieldVector3D<T>(field.getZero(), field.getOne(), field.getZero()),
                                      new FieldVector3D<T>(field.getZero(), field.getZero(), field.getZero()),
                                      new FieldVector3D<T>(field.getZero(), field.getZero(), field.getZero()));
    }

    /** K axis.
     * @param field field used by default
     * @return K axis in FieldPVCoordinates
     * */
    private FieldPVCoordinates<T> PLUS_K (final Field<T> field) {
        return new FieldPVCoordinates<T>(new FieldVector3D<T>(field.getZero(), field.getZero(), field.getOne()),
                            new FieldVector3D<T>(field.getZero(), field.getZero(), field.getZero()),
                            new FieldVector3D<T>(field.getZero(), field.getZero(), field.getZero()));
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
     * @throws OrekitException if some specific error occurs,
     * such as no target reached
     */
    protected abstract TimeStampedFieldPVCoordinates<T> getTargetPV(FieldPVCoordinatesProvider<T> pvProv,
                                                                    FieldAbsoluteDate<T> date, Frame frame)
        throws OrekitException;

    /** {@inheritDoc} */
    public FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv, final FieldAbsoluteDate<T> date,
                                final Frame frame)
        throws OrekitException {

        // satellite-target relative vector
        final FieldPVCoordinates<T> pva  = pvProv.getPVCoordinates(date, inertialFrame);
        final TimeStampedFieldPVCoordinates<T> delta =
                new TimeStampedFieldPVCoordinates<T> (date, pva, getTargetPV(pvProv, date, inertialFrame));

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
        final FieldVector3D<T> keplerianJerk = new FieldVector3D<T>(FieldVector3D.dotProduct(p, v).multiply(-3).divide(r2), a, a.getNorm().divide(r).multiply(-1), v);
        final FieldPVCoordinates<T> velocity = new FieldPVCoordinates<T>(v, a, keplerianJerk);


        final FieldPVCoordinates<T> los    = delta.normalize();

        final FieldPVCoordinates<T> normal = (delta.crossProduct(velocity)).normalize();

        final FieldAngularCoordinates<T> ac = new FieldAngularCoordinates<T>(los, normal, PLUS_K(r.getField()), PLUS_J(r.getField()), 1.0e-6);

        if (frame != inertialFrame) {
            // prepend transform from specified frame to inertial frame
           //TODO ac = ac.addOffset(frame.getTransformTo(inertialFrame, date.toAbsoluteDate()).getAngular());
        }

        // build the attitude
        return new FieldAttitude<T>(date, frame, ac);

    }


}
