/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;



/**
 * This class handles nadir pointing attitude law.

 * <p>
 * This class represents the attitude law where the satellite z axis is
 * pointing to the vertical of the ground point under satellite.</p>
 * <p>
 * The object <code>NadirPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class NadirPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 9077899256315179822L;

    /** Body shape.  */
    private final BodyShape shape;

    /** Creates new instance.
     * @param shape Body shape
     */
    public NadirPointing(final BodyShape shape) {
        // Call constructor of superclass
        super(shape.getBodyFrame());
        this.shape = shape;
    }

    /** {@inheritDoc} */
    protected PVCoordinates getTargetInBodyFrame(final AbsoluteDate date,
                                                 final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // Get target in body frame
        final PVCoordinates groundPoint = getObservedGroundPoint(date, pv, frame);

        // Transform to given frame
        final Transform t = frame.getTransformTo(getBodyFrame(), date);

        // Target in body frame.
        return t.transformPVCoordinates(groundPoint);

    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getObservedGroundPoint(final AbsoluteDate date,
                                                final PVCoordinates pv,
                                                final Frame frame)
        throws OrekitException {

        // nadir point position in same frame as initial pv
        final Vector3D p      = pv.getPosition();
        final Vector3D v      = pv.getVelocity();
        final Vector3D nadirP = getNadir(date, p, frame);

        // velocity of nadir due to satellite self motion, computed using a four
        // points finite differences algorithm because we cannot compute shape normal
        // curvature along the track for any shape and the nadir motion depends on it
        final double h           = 0.05;
        final double s2          = 1.0 / (12 * h);
        final double s1          = 8 * s2;
        final Vector3D nadirP2h  = getNadir(new AbsoluteDate(date,  2 * h), new Vector3D(1, p,  2 * h, v), frame);
        final Vector3D nadirM2h  = getNadir(new AbsoluteDate(date, -2 * h), new Vector3D(1, p, -2 * h, v), frame);
        final Vector3D nadirP1h  = getNadir(new AbsoluteDate(date,      h), new Vector3D(1, p,      h, v), frame);
        final Vector3D nadirM1h  = getNadir(new AbsoluteDate(date,     -h), new Vector3D(1, p,     -h, v), frame);
        final Vector3D nadirV    = new Vector3D(-s2, nadirP2h, s2, nadirM2h, s1, nadirP1h, -s1, nadirM1h);

        return new PVCoordinates(nadirP, nadirV);

    }

    /** Get nadir in a specified frame.
     * @param date date when system state shall be computed
     * @param position satellite position in given frame
     * @param frame the frame in which pv is defined and in which nadir is requested
     * @return nadir in specified frame
     * @exception OrekitException if some conversion fails
     */
    private Vector3D getNadir(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws OrekitException {

        final Transform t = frame.getTransformTo(getBodyFrame(), date);
        final Vector3D satInBodyFrame = t.transformPosition(position);

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, getBodyFrame(), date);

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in specified frame
        return t.getInverse().transformPosition(shape.transform(gpNadir));

    }

}
