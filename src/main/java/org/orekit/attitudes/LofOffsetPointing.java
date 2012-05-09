/* Copyright 2002-2012 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


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
    private static final long serialVersionUID = -713570668596014285L;

    /** Rotation from local orbital frame. */
    private final AttitudeProvider attitudeLaw;

    /** Body shape. */
    private final BodyShape shape;

    /** Chosen satellite axis for pointing, given in satellite frame. */
    private final Vector3D satPointingVector;

    /** Creates new instance.
     * @param shape Body shape
     * @param attLaw Attitude law
     * @param satPointingVector satellite vector defining the pointing direction
     */
    public LofOffsetPointing(final BodyShape shape, final AttitudeProvider attLaw,
                             final Vector3D satPointingVector) {
        super(shape.getBodyFrame());
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
    protected Vector3D getTargetPoint(final PVCoordinatesProvider pvProv,
                                      final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        final PVCoordinates pv = pvProv.getPVCoordinates(date, frame);

        // Compute satellite state at given date in orbit frame
        final Rotation satRot = attitudeLaw.getAttitude(pvProv, date, frame).getRotation();

        // Compute satellite pointing axis and position/velocity in body frame
        final Transform t = frame.getTransformTo(shape.getBodyFrame(), date);
        final Vector3D pointingBodyFrame =
            t.transformVector(satRot.applyInverseTo(satPointingVector));
        final Vector3D pBodyFrame = t.transformPosition(pv.getPosition());

        // Line from satellite following pointing direction
        // we use arbitrarily the Earth radius as a scaling factor, it could be anything else
        final Line pointingLine = new Line(pBodyFrame,
                                           pBodyFrame.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, pointingBodyFrame));

        // Intersection with body shape
        final GeodeticPoint gpIntersection =
            shape.getIntersectionPoint(pointingLine, pBodyFrame, shape.getBodyFrame(), date);
        final Vector3D vIntersection =
            (gpIntersection == null) ? null : shape.transform(gpIntersection);

        // Check there is an intersection and it is not in the reverse pointing direction
        if ((vIntersection == null) ||
            (Vector3D.dotProduct(vIntersection.subtract(pBodyFrame), pointingBodyFrame) < 0)) {
            throw new OrekitException(OrekitMessages.ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND);
        }

        return shape.getBodyFrame().getTransformTo(frame, date).transformPosition(vIntersection);

    }

}
