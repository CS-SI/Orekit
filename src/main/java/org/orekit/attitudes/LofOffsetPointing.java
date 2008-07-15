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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Line;
import org.orekit.utils.PVCoordinates;


/**
 * This class provides a default attitude law.

 * <p>
 * The attitude pointing law is defined by an attitude law and
 * the satellite axis vector chosen for pointing.
 * <p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class LofOffsetPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -713570668596014285L;

    /** Rotation from local orbital frame. */
    private final AttitudeLaw attitudeLaw;

    /** Body shape. */
    private final BodyShape shape;

    /** Chosen satellite axis for pointing, given in satellite frame. */
    private final Vector3D satPointingVector;

    /** Creates new instance.
     * @param shape Body shape
     * @param attLaw Attitude law
     * @param satPointingVector satellite vector defining the pointing direction
     */
    public LofOffsetPointing(final BodyShape shape, final AttitudeLaw attLaw,
                             final Vector3D satPointingVector) {
        super(shape.getBodyFrame());
        this.shape = shape;
        this.attitudeLaw = attLaw;
        this.satPointingVector = satPointingVector;
    }

    /** Compute the system state at given date in given frame.
     * @param date date when system state shall be computed
     * @param pv satellite position/velocity in given frame
     * @param frame the frame in which pv is defined
     * @return satellite attitude state at date
     * @throws OrekitException if some specific error occurs
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p> */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame)
        throws OrekitException {
        return attitudeLaw.getState(date, pv, frame);
    }

    /** Get target expressed in body frame at given date.
     * @param date computation date.
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame Frame in which satellite position-velocity is given.
     * @return target position/velocity in body frame
     * @throws OrekitException if some specific error occurs
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    protected PVCoordinates getTargetInBodyFrame(final AbsoluteDate date,
                                                 final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // Compute satellite state at given date in given frame
        final Rotation satRot = getState(date, pv, frame).getRotation();

        // Compute satellite pointing axis and position/velocity in body frame
        final Transform t = frame.getTransformTo(shape.getBodyFrame(), date);
        final Vector3D pointingBodyFrame =
            t.transformVector(satRot.applyInverseTo(satPointingVector));
        final Vector3D pBodyFrame = t.transformPosition(pv.getPosition());

        // Line from satellite following pointing direction
        final Line pointingLine = new Line(pBodyFrame, pointingBodyFrame);

        // Intersection with body shape
        final GeodeticPoint gpIntersection =
            shape.getIntersectionPoint(pointingLine, pBodyFrame, shape.getBodyFrame(), date);
        final Vector3D vIntersection =
            (gpIntersection == null) ? null : shape.transform(gpIntersection);

        // Check there is an intersection and it is not in the reverse pointing direction
        if ((vIntersection == null) ||
            (Vector3D.dotProduct(vIntersection.subtract(pBodyFrame), pointingBodyFrame) < 0)) {
            throw new OrekitException("attitude pointing law misses ground", new Object[0]);
        }

        return new PVCoordinates(vIntersection, Vector3D.ZERO);

    }



}
