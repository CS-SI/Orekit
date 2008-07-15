/* Copyright 2002-2008 CS Communication & Systèmes
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
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;



/**
 * This class handles target pointing attitude law.

 * <p>
 * This class represents the attitude law where the satellite z axis is
 * pointing to a ground point target.</p>
 * <p>
 * The target position and velocity are defined in a body frame specified by the user.
 * It is important to make sure this frame is consistent.
 * </p>
 * <p>
 * The object <code>TargetPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class TargetPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = 2018902366782191143L;

    /** Target in body frame. */
    private final PVCoordinates target;


    /** Creates a new instance from body frame and target expressed in position/velocity coordinates.
     * @param bodyFrame body frame.
     * @param target target expressed in position/velocity coordinates in body frame
     */
    public TargetPointing(final Frame bodyFrame, final PVCoordinates target) {
        super(bodyFrame);
        this.target = target;
    }

    /** Creates a new instance from body shape and target expressed in geodetic coordinates.
     * @param targetGeo target defined as a geodetic point in body shape frame
     * @param shape body shape
     */
    public TargetPointing(final GeodeticPoint targetGeo, final BodyShape shape) {
        super(shape.getBodyFrame());
        // Transform target from geodetic coordinates to position-velocity coordinates
        target = new PVCoordinates(shape.transform(targetGeo), Vector3D.ZERO);
    }

    /** Get target expressed in body frame at given date.
     * @param date computation date.
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame frame in which satellite position-velocity is given.
     * @return target position/velocity in body frame.
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    protected PVCoordinates getTargetInBodyFrame(final AbsoluteDate date,
                                                 final PVCoordinates pv, final Frame frame) {
        // Returns attribute target
        return target;
    }

}
