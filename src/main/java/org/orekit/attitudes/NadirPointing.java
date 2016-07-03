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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * This class handles nadir pointing attitude provider.

 * <p>
 * This class represents the attitude provider where the satellite z axis is
 * pointing to the vertical of the ground point under satellite.</p>
 * <p>
 * The object <code>NadirPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class NadirPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150529L;

    /** Body shape.  */
    private final BodyShape shape;

    /** Creates new instance.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param shape Body shape
     * @exception OrekitException if the frame specified is not a pseudo-inertial frame
     * @since 7.1
     */
    public NadirPointing(final Frame inertialFrame, final BodyShape shape)
        throws OrekitException {
        // Call constructor of superclass
        super(inertialFrame, shape.getBodyFrame());
        this.shape = shape;
    }

    /** {@inheritDoc} */
    protected TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                   final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // transform from specified reference frame to body frame
        final Transform refToBody = frame.getTransformTo(shape.getBodyFrame(), date);

        // sample intersection points in current date neighborhood
        final double h  = 0.01;
        final List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(-2 * h), frame), refToBody.shiftedBy(-2 * h)));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(-h),     frame), refToBody.shiftedBy(-h)));
        sample.add(nadirRef(pvProv.getPVCoordinates(date,                   frame), refToBody));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(+h),     frame), refToBody.shiftedBy(+h)));
        sample.add(nadirRef(pvProv.getPVCoordinates(date.shiftedBy(+2 * h), frame), refToBody.shiftedBy(+2 * h)));

        // use interpolation to compute properly the time-derivatives
        return TimeStampedPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_P, sample);

    }

    /** Compute ground point in nadir direction, in reference frame.
     * @param scRef spacecraft coordinates in reference frame
     * @param refToBody transform from reference frame to body frame
     * @return intersection point in body frame (only the position is set!)
     * @exception OrekitException if line of sight does not intersect body
     */
    private TimeStampedPVCoordinates nadirRef(final TimeStampedPVCoordinates scRef, final Transform refToBody)
        throws OrekitException {

        final Vector3D satInBodyFrame = refToBody.transformPosition(scRef.getPosition());

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, getBodyFrame(), scRef.getDate());

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in body frame
        final Vector3D pNadirBody = shape.transform(gpNadir);

        // nadir point position in reference frame
        final Vector3D pNadirRef = refToBody.getInverse().transformPosition(pNadirBody);

        return new TimeStampedPVCoordinates(scRef.getDate(), pNadirRef, Vector3D.ZERO, Vector3D.ZERO);

    }

}
