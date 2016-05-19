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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * This class provides a default attitude provider.

 * <p>
 * The attitude pointing law is defined by an attitude provider and
 * the satellite axis vector chosen for pointing.
 * <p>
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class LofOffsetPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150529L;

    /** Rotation from local orbital frame. */
    private final AttitudeProvider attitudeLaw;

    /** Body shape. */
    private final BodyShape shape;

    /** Chosen satellite axis for pointing, given in satellite frame. */
    private final Vector3D satPointingVector;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @param attLaw Attitude law
     * @param satPointingVector satellite vector defining the pointing direction
     * @exception OrekitException if the frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public LofOffsetPointing(final Frame inertialFrame, final BodyShape shape,
                             final AttitudeProvider attLaw, final Vector3D satPointingVector)
        throws OrekitException {
        super(inertialFrame, shape.getBodyFrame());
        this.shape = shape;
        this.attitudeLaw = attLaw;
        this.satPointingVector = satPointingVector;
    }

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return attitudeLaw.getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                   final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // transform from specified reference frame to spacecraft frame
        final Transform refToSc =
                new Transform(date,
                              new Transform(date, pvProv.getPVCoordinates(date, frame).negate()),
                              new Transform(date, attitudeLaw.getAttitude(pvProv, date, frame).getOrientation()));

        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), date);

        // sample intersection points in current date neighborhood
        final Transform scToBody  = new Transform(date, refToSc.getInverse(), refToBody);
        final double h  = 0.1;
        final List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        sample.add(losIntersectionWithBody(scToBody.shiftedBy(-h)));
        sample.add(losIntersectionWithBody(scToBody));
        sample.add(losIntersectionWithBody(scToBody.shiftedBy(+h)));

        // use interpolation to compute properly the time-derivatives
        final TimeStampedPVCoordinates targetBody =
                TimeStampedPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_P, sample);

        // convert back to caller specified frame
        return refToBody.getInverse().transformPVCoordinates(targetBody);

    }

    /** Compute line of sight intersection with body.
     * @param scToBody transform from spacecraft frame to body frame
     * @return intersection point in body frame (only the position is set!)
     * @exception OrekitException if line of sight does not intersect body
     */
    private TimeStampedPVCoordinates losIntersectionWithBody(final Transform scToBody)
        throws OrekitException {

        // compute satellite pointing axis and position/velocity in body frame
        final Vector3D pointingBodyFrame = scToBody.transformVector(satPointingVector);
        final Vector3D pBodyFrame        = scToBody.transformPosition(Vector3D.ZERO);

        // Line from satellite following pointing direction
        // we use arbitrarily the Earth radius as a scaling factor, it could be anything else
        final Line pointingLine = new Line(pBodyFrame,
                                           pBodyFrame.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                          pointingBodyFrame),
                                           1.0e-10);

        // Intersection with body shape
        final GeodeticPoint gpIntersection =
            shape.getIntersectionPoint(pointingLine, pBodyFrame, shape.getBodyFrame(), scToBody.getDate());
        final Vector3D pIntersection =
            (gpIntersection == null) ? null : shape.transform(gpIntersection);

        // Check there is an intersection and it is not in the reverse pointing direction
        if ((pIntersection == null) ||
            (Vector3D.dotProduct(pIntersection.subtract(pBodyFrame), pointingBodyFrame) < 0)) {
            throw new OrekitException(OrekitMessages.ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND);
        }

        return new TimeStampedPVCoordinates(scToBody.getDate(),
                                            pIntersection, Vector3D.ZERO, Vector3D.ZERO);

    }

}
