/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.geometry.enclosing.EnclosingBall;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.Region;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.Vertex;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.events.VisibilityTrigger;

/** Class representing a spacecraft sensor Field Of View with polygonal shape.
 * <p>Fields Of View are zones defined on the unit sphere centered on the
 * spacecraft. They can have any shape, they can be split in several
 * non-connected patches and can have holes.</p>
 * @author Luc Maisonobe
 * @since 10.1
 */
public class PolygonalFieldOfView extends AbstractFieldOfView {

    /** Spherical zone. */
    private final SphericalPolygonsSet zone;

    /** Spherical cap surrounding the zone. */
    private final EnclosingBall<Sphere2D, S2Point> cap;

    /** Build a new instance.
     * @param zone interior of the Field Of View, in spacecraft frame
     * @param margin angular margin to apply to the zone (if positive,
     * points outside of the raw FoV but close enough to the boundary are
     * considered visible; if negative, points inside of the raw FoV
     * but close enough to the boundary are considered not visible)
     */
    public PolygonalFieldOfView(final SphericalPolygonsSet zone, final double margin) {
        super(margin);
        this.zone = zone;
        this.cap  = zone.getEnclosingCap();
    }

    /** Build Field Of View with a regular polygon shape.
     * @param center center of the polygon (the center is in the inside part)
     * @param coneType type of defining cone
     * @param meridian point defining the reference meridian for one contact
     * point between defining cone and polygon (i.e. either a polygon edge
     * middle point or a polygon vertex)
     * @param radius defining cone angular radius
     * @param n number of sides of the polygon
     * @param margin angular margin to apply to the zone (if positive,
     * points outside of the raw FoV but close enough to the boundary are
     * considered visible; if negative, points inside of the raw FoV
     * but close enough to the boundary are considered not visible)
     * @since 10.1
     */
    public PolygonalFieldOfView(final Vector3D center, final DefiningConeType coneType,
                                final Vector3D meridian, final double radius,
                                final int n, final double margin) {

        super(margin);

        final double   verticesRadius = coneType.verticesRadius(radius, n);
        final Vector3D vertex         = coneType.createVertex(center, meridian, verticesRadius, n);
        this.zone                     = new SphericalPolygonsSet(center, vertex, verticesRadius,
                                                                 n, 1.0e-12 * verticesRadius);

        final Rotation r = new Rotation(center, MathUtils.TWO_PI / n, RotationConvention.VECTOR_OPERATOR);
        final S2Point[] support = new S2Point[n];
        support[0] = new S2Point(vertex);
        for (int i = 1; i < n; ++i) {
            support[i] = new S2Point(r.applyTo(support[i - 1].getVector()));
        }
        this.cap = new EnclosingBall<>(new S2Point(center), Vector3D.angle(center, vertex), support);

    }

    /** Get the interior zone.
     * @return the interior zone
     */
    public SphericalPolygonsSet getZone() {
        return zone;
    }

    /** {@inheritDoc} */
    @Override
    public double offsetFromBoundary(final Vector3D lineOfSight, final double angularRadius,
                                     final VisibilityTrigger trigger) {

        final S2Point los             = new S2Point(lineOfSight);
        final double  margin          = getMargin();
        final double  correctedRadius = trigger.radiusCorrection(angularRadius);
        final double  deadBand        = margin + angularRadius;

        // for faster computation, we start using only the surrounding cap, to filter out
        // far away points (which correspond to most of the points if the Field Of View is small)
        final double crudeDistance = cap.getCenter().distance(los) - cap.getRadius();
        if (crudeDistance > deadBand + 0.01) {
            // we know we are strictly outside of the zone,
            // use the crude distance to compute the (positive) return value
            return crudeDistance + correctedRadius - margin;
        }

        // we are close, we need to compute carefully the exact offset;
        // we project the point to the closest zone boundary
        return zone.projectToBoundary(los).getOffset() + correctedRadius - margin;

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D projectToBoundary(final Vector3D lineOfSight) {
        return ((S2Point) zone.projectToBoundary(new S2Point(lineOfSight)).getProjected()).getVector();
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

        final List<List<GeodeticPoint>> footprint = new ArrayList<>();

        final List<Vertex> boundary = zone.getBoundaryLoops();
        for (final Vertex loopStart : boundary) {
            int count = 0;
            final List<GeodeticPoint> loop  = new ArrayList<>();
            boolean intersectionsFound      = false;
            for (Edge edge = loopStart.getOutgoing();
                 count == 0 || edge.getStart() != loopStart;
                 edge = edge.getEnd().getOutgoing()) {
                ++count;
                final int    n     = (int) FastMath.ceil(edge.getLength() / angularStep);
                final double delta =  edge.getLength() / n;
                for (int i = 0; i < n; ++i) {
                    final Vector3D awaySC      = new Vector3D(r, edge.getPointAt(i * delta));
                    final Vector3D awayBody    = fovToBody.transformPosition(awaySC);
                    final Line     lineOfSight = new Line(position, awayBody, 1.0e-3);
                    GeodeticPoint  gp          = body.getIntersectionPoint(lineOfSight, position,
                                                                           bodyFrame, null);
                    if (gp != null &&
                        Vector3D.dotProduct(awayBody.subtract(position),
                                            body.transform(gp).subtract(position)) < 0) {
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

                    // add the point in front of the list
                    // (to ensure the loop will be in trigonometric orientation)
                    loop.add(0, gp);

                }
            }

            if (intersectionsFound) {
                // at least some of the points did intersect the body,
                // this loop contributes to the footprint
                footprint.add(loop);
            }

        }

        if (footprint.isEmpty()) {
            // none of the Field Of View loops cross the body
            // either the body is outside of Field Of View, or it is fully contained
            // we check the center
            final Vector3D bodyCenter = fovToBody.toStaticTransform().getInverse().transformPosition(Vector3D.ZERO);
            if (zone.checkPoint(new S2Point(bodyCenter)) != Region.Location.OUTSIDE) {
                // the body is fully contained in the Field Of View
                // we use the full limb as the footprint
                final Vector3D x        = bodyCenter.orthogonal();
                final Vector3D y        = Vector3D.crossProduct(bodyCenter, x).normalize();
                final double   sinEta   = body.getEquatorialRadius() / r;
                final double   sinEta2  = sinEta * sinEta;
                final double   cosAlpha = (FastMath.cos(angularStep) + sinEta2 - 1) / sinEta2;
                final int      n        = (int) FastMath.ceil(MathUtils.TWO_PI / FastMath.acos(cosAlpha));
                final double   delta    = MathUtils.TWO_PI / n;
                final List<GeodeticPoint> loop = new ArrayList<>(n);
                for (int i = 0; i < n; ++i) {
                    final SinCos sc = FastMath.sinCos(i * delta);
                    final Vector3D outside = new Vector3D(r * sc.cos(), x,
                                                          r * sc.sin(), y);
                    loop.add(body.transform(body.pointOnLimb(position, outside), bodyFrame, null));
                }
                footprint.add(loop);
            }
        }

        return footprint;

    }

    /** Enumerate for cone/polygon relative position.
     * @since 10.1
     */
    public enum DefiningConeType {

        /** Constant for cones inside polygons and touching it at polygon edges middle points. */
        INSIDE_CONE_TOUCHING_POLYGON_AT_EDGES_MIDDLE() {

            /** {@inheritDoc}*/
            @Override
            protected double verticesRadius(final double radius, final int n) {
                // convert the inside (edges middle points) radius to outside (vertices) radius
                return FastMath.atan(FastMath.tan(radius) / FastMath.cos(FastMath.PI / n));
            }

            /** {@inheritDoc}*/
            @Override
            protected Vector3D createVertex(final Vector3D center, final Vector3D meridian,
                                            final double verticesRadius, final int n) {
                // convert the edge middle meridian to a vertex
                final SinCos scA = FastMath.sinCos(FastMath.PI / n);
                final SinCos scR = FastMath.sinCos(verticesRadius);
                final Vector3D z = center.normalize();
                final Vector3D y = Vector3D.crossProduct(center, meridian).normalize();
                final Vector3D x = Vector3D.crossProduct(y, z);
                return new Vector3D(scR.sin() * scA.cos(), x, scR.sin() * scA.sin(), y, scR.cos(), z);
            }

        },

        /** Constant for cones outside polygons and touching it at polygon vertices. */
        OUTSIDE_CONE_TOUCHING_POLYGON_AT_VERTICES() {

            /** {@inheritDoc}*/
            @Override
            protected double verticesRadius(final double radius, final int n) {
                return radius;
            }

            /** {@inheritDoc}*/
            @Override
            protected Vector3D createVertex(final Vector3D center, final Vector3D meridian,
                                            final double verticesRadius, final int n) {
                // convert the vertex meridian to a vertex
                final SinCos scR = FastMath.sinCos(verticesRadius);
                final Vector3D z = center.normalize();
                final Vector3D y = Vector3D.crossProduct(center, meridian).normalize();
                final Vector3D x = Vector3D.crossProduct(y, z);
                return new Vector3D(scR.sin(), x, scR.cos(), z);
            }

        };

        /** Compute radius of cone going through vertices.
         * @param radius defining cone angular radius
         * @param n number of sides of the polygon
         * @return radius of cone going through vertices
         */
        protected abstract double verticesRadius(double radius, int n);

        /** Create a vertex.
         * @param center center of the polygon (the center is in the inside part)
         * @param meridian point defining the reference meridian for one contact
         * point between defining cone and polygon (i.e. either a polygon edge
         * middle point or a polygon vertex)
         * @param verticesRadius defining radius of cone passing through vertices
         * @param n number of sides of the polygon
         * @return created vertex
         */
        protected abstract Vector3D createVertex(Vector3D center, Vector3D meridian,
                                                 double verticesRadius, int n);

    }

}
