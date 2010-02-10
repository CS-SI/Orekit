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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
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

    /** {@inheritDoc} */
    @Override
    public Attitude getAttitude(final Orbit orbit)
        throws OrekitException {
        return attitudeLaw.getAttitude(orbit);
    }

    /** {@inheritDoc} */
    protected Vector3D getTargetPoint(final Orbit orbit, final Frame frame)
        throws OrekitException {

        final AbsoluteDate date = orbit.getDate();
        final PVCoordinates pv = orbit.getPVCoordinates();

        // Compute satellite state at given date in orbit frame
        final Rotation satRot = attitudeLaw.getAttitude(orbit).getRotation();

        // Compute satellite pointing axis and position/velocity in body frame
        final Transform t = orbit.getFrame().getTransformTo(shape.getBodyFrame(), date);
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
            throw new OrekitException("attitude pointing law misses ground");
        }

        return shape.getBodyFrame().getTransformTo(frame, date).transformPosition(vIntersection);

    }

}
