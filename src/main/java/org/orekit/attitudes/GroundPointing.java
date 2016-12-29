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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
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

    /** Serializable UID. */
    private static final long serialVersionUID = 20150529L;

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
     * @exception OrekitException if the first frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    protected GroundPointing(final Frame inertialFrame, final Frame bodyFrame)
        throws OrekitException {
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
     * @throws OrekitException if some specific error occurs,
     * such as no target reached
     */
    protected abstract TimeStampedPVCoordinates getTargetPV(PVCoordinatesProvider pvProv,
                                                            AbsoluteDate date, Frame frame)
        throws OrekitException;

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date,
                                final Frame frame)
        throws OrekitException {

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

}
