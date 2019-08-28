/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.geometry.fov;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;

/** Class representing a spacecraft sensor Field Of View with circular shape.
 * <p>The field of view is defined by an axis and an half-aperture angle.</p>
 * @author Luc Maisonobe
 * @since 10.1
 */
public class CircularFieldOfView extends AbstractFieldOfView {

    /** Direction of the FOV center. */
    private final Vector3D center;

    /** FOV half aperture angle. */
    private final double halfAperture;

    /** Scaled X axis defining FoV boundary. */
    private final Vector3D scaledX;

    /** Scaled Y axis defining FoV boundary. */
    private final Vector3D scaledY;

    /** Scaled Z axis defining FoV boundary. */
    private final Vector3D scaledZ;

    /** Perimeter of the Field Of View. */
    private final double perimeter;

    /** Build a new instance.
     * @param center direction of the FOV center, in spacecraft frame
     * @param halfAperture FOV half aperture angle
     * @param margin angular margin to apply to the zone (if positive,
     * the Field Of View will consider points slightly outside of the
     * zone are still visible)
     */
    public CircularFieldOfView(final Vector3D center, final double halfAperture,
                               final double margin) {

        super(margin);
        this.center       = center;
        this.halfAperture = halfAperture;

        // precompute utility vectors for walking around the FoV
        final Vector3D zAxis = center.normalize();
        final Vector3D xAxis = center.orthogonal();
        final Vector3D yAxis = Vector3D.crossProduct(zAxis, xAxis);
        final SinCos   sc    = FastMath.sinCos(halfAperture);
        scaledX   = new Vector3D(sc.sin(), xAxis);
        scaledY   = new Vector3D(sc.sin(), yAxis);
        scaledZ   = new Vector3D(sc.cos(), zAxis);
        perimeter = MathUtils.TWO_PI * sc.sin();

    }

    /** Get the direction of the FOV center, in spacecraft frame.
     * @return direction of the FOV center, in spacecraft frame
     */
    public Vector3D getCenter() {
        return center;
    }

    /** get the FOV half aperture angle.
     * @return FOV half aperture angle
     */
    public double getHalfAperture() {
        return halfAperture;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromBoundary(final Vector3D lineOfSight) {
        return Vector3D.angle(center, lineOfSight) - halfAperture - getMargin();
    }

    /** {@inheritDoc} */
    @Override
    public List<List<GeodeticPoint>> getFootprint(final Transform fovToBody,
                                                  final OneAxisEllipsoid body,
                                                  final double angularStep) {

        final Frame     bodyFrame = body.getBodyFrame();
        final Vector3D  position  = fovToBody.transformPosition(Vector3D.ZERO);
        final double    r         = position.getNorm();
        if (body.isInside(position)) {
            throw new OrekitException(OrekitMessages.POINT_INSIDE_ELLIPSOID);
        }

        // prepare loop around FoV
        boolean                         intersectionsFound = false;
        final int                       nbPoints           = (int) FastMath.ceil(perimeter / angularStep);
        final List<GeodeticPoint>       loop               = new ArrayList<>(nbPoints);

        // loop in inverse trigonometric order, so footprint is in trigonometric order
        final double step = MathUtils.TWO_PI / nbPoints;
        for (int i = 0; i < nbPoints; ++i) {
            final Vector3D direction   = directionAt(-i * step);
            final Vector3D awaySC      = new Vector3D(r, direction);
            final Vector3D awayBody    = fovToBody.transformPosition(awaySC);
            final Line     lineOfSight = new Line(position, awayBody, 1.0e-3);
            GeodeticPoint  gp          = body.getIntersectionPoint(lineOfSight, position, bodyFrame, null);
            if (gp != null &&
                Vector3D.dotProduct(awayBody.subtract(position), body.transform(gp).subtract(position)) < 0) {
                // the intersection is in fact on the half-line pointing
                // towards the back side, it is a spurious intersection
                gp = null;
            }

            if (gp != null) {
                // the line of sight does intersect the body
                intersectionsFound = true;
            } else {
                // the line of sight does not intersect body
                // we use a point on the limb
                gp = body.transform(body.pointOnLimb(position, awayBody), bodyFrame, null);
            }

            // add the point
            loop.add(gp);

        }

        final List<List<GeodeticPoint>> footprint = new ArrayList<>();
        if (intersectionsFound) {
            // at least some of the points did intersect the body, there is a footprint
            footprint.add(loop);
        } else {
            // the Field Of View loop does not cross the body
            // either the body is outside of Field Of View, or it is fully contained
            // we check the center
            final Vector3D bodyCenter = fovToBody.getInverse().transformPosition(Vector3D.ZERO);
            if (Vector3D.angle(center, bodyCenter) < halfAperture) {
                // the body is fully contained in the Field Of View
                // the previous loop did compute the full limb as the footprint
                footprint.add(loop);
            }
        }

        return footprint;

    }

    /** Get boundary direction at angle.
     * @param angle phase angle of the boundary direction
     * @return boundary direction at phase angle
     */
    private Vector3D directionAt(final double angle) {
        final SinCos sc = FastMath.sinCos(angle);
        return new Vector3D(sc.cos(), scaledX, sc.sin(), scaledY, 1.0, scaledZ);
    }

}
