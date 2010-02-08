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
import org.orekit.orbits.Orbit;
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
    @Override
    public PVCoordinates getObservedGroundPoint(final Orbit orbit, final Frame frame)
        throws OrekitException {

        // nadir point position in same frame as initial pv
        final Vector3D nadirP = getNadir(orbit, frame);

        // velocity of nadir due to satellite self motion, computed using a four
        // points finite differences algorithm because we cannot compute shape normal
        // curvature along the track for any shape and the nadir motion depends on it
        final double h           = 0.05;
        final double s2          = 1.0 / (12 * h);
        final double s1          = 8 * s2;
        final Vector3D nadirP2h  = getNadir(orbit.shiftedBy( 2 * h), frame);
        final Vector3D nadirM2h  = getNadir(orbit.shiftedBy(-2 * h), frame);
        final Vector3D nadirP1h  = getNadir(orbit.shiftedBy(     h), frame);
        final Vector3D nadirM1h  = getNadir(orbit.shiftedBy(    -h), frame);
        final Vector3D nadirV    = new Vector3D(-s2, nadirP2h, s2, nadirM2h, s1, nadirP1h, -s1, nadirM1h);

        return new PVCoordinates(nadirP, nadirV);

    }

    /** Get nadir in a specified frame.
     * @param orbit orbit state
     * @param frame frame in which observed ground point should be provided
     * @return nadir in specified frame
     * @exception OrekitException if some conversion fails
     */
    private Vector3D getNadir(final Orbit orbit, final Frame frame)
        throws OrekitException {

        final AbsoluteDate date = orbit.getDate();
        final Vector3D satInBodyFrame = orbit.getPVCoordinates(getBodyFrame()).getPosition();

        // satellite position in geodetic coordinates
        final GeodeticPoint gpSat = shape.transform(satInBodyFrame, getBodyFrame(), date);

        // nadir position in geodetic coordinates
        final GeodeticPoint gpNadir = new GeodeticPoint(gpSat.getLatitude(), gpSat.getLongitude(), 0.0);

        // nadir point position in specified frame
        return getBodyFrame().getTransformTo(frame, date).transformPosition(shape.transform(gpNadir));

    }

}
