/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.models.earth.tessellation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.partitioning.Region.Location;
import org.apache.commons.math3.geometry.partitioning.RegionFactory;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.Sphere2D;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.Ellipse;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;

/** Base class able to tessellate an interest zone on an ellipsoid in {@link Tile tiles}.
 * @author Luc Maisonobe
 */
public abstract class EllipsoidTessellator {

    /** Underlying ellipsoid. */
    private final OneAxisEllipsoid ellipsoid;

    /** Tile width (distance on ellipsoid surface). */
    private final double width;

    /** Tile length (distance on ellipsoid surface). */
    private final double length;

    /** Simple constructor.
     * @param ellipsoid underlying ellipsoid
     * @param width tiles width as a distance on surface (in meters)
     * @param length tiles length as a distance on surface (in meters)
     */
    protected EllipsoidTessellator(final OneAxisEllipsoid ellipsoid,
                                   final double width, final double length) {
        this.ellipsoid = ellipsoid;
        this.width     = width;
        this.length    = length;
    }

    /** Get the underlying ellipsoid.
     * @return underlying ellipsoid
     */
    public OneAxisEllipsoid getEllipsoid() {
        return ellipsoid;
    }

    /** Tessellate a zone of interest into tiles.
     * @param zone zone of interest to tessellate
     * @return a list of tiles covering the zone of interest
     * @exception OrekitException if the zone cannot be tessellated
     */
    public List<Tile> tessellate(final SphericalPolygonsSet zone)
        throws OrekitException {

        // start mesh inside the zone
        final InsideFinder finder = new InsideFinder(zone.getTolerance());
        zone.getTree(false).visit(finder);
        final Vector3D start = finder.getInsidePoint();
        final Mesh mesh = new Mesh(zone, start);

        // mesh expansion loop
        final Queue<Mesh.Node> activeNodes = new LinkedList<Mesh.Node>();
        activeNodes.add(mesh.getNode(0, 0));
        while (!activeNodes.isEmpty()) {

            // retrieve an active node
            final Mesh.Node node = activeNodes.remove();

            if (node.insideZone) {
                // the node is inside the zone, the mesh *must* contain all its neighbors
                if (mesh.getNode(node.alongIndex - 1, node.acrossIndex) == null) {
                    // add a previous node in the along direction
                    activeNodes.add(mesh.addNode(move(node.v, node.along, -0.5 * length),
                                                 node.alongIndex - 1, node.acrossIndex));
                }
                if (mesh.getNode(node.alongIndex + 1, node.acrossIndex) == null) {
                    // add a next node in the along direction
                    activeNodes.add(mesh.addNode(move(node.v, node.along, +0.5 * length),
                                                 node.alongIndex + 1, node.acrossIndex));
                }
                if (mesh.getNode(node.alongIndex, node.acrossIndex - 1) == null) {
                    // add a previous node in the across direction
                    activeNodes.add(mesh.addNode(move(node.v, node.across, -0.5 * width),
                                                 node.alongIndex, node.acrossIndex - 1));
                }
                if (mesh.getNode(node.alongIndex, node.acrossIndex + 1) == null) {
                    // add a next node in the across direction
                    activeNodes.add(mesh.addNode(move(node.v, node.across, +0.5 * width),
                                                 node.alongIndex, node.acrossIndex + 1));
                }
            }

        }

        // TODO: implement tessellation
        throw OrekitException.createInternalError(null);

    }

    /** Convert a point on the unit 2-sphere to geodetic coordinates.
     * @param point point on the unit 2-sphere
     * @return geodetic point
     */
    protected GeodeticPoint toGeodetic(final S2Point point) {
        return new GeodeticPoint(0.5 * FastMath.PI - point.getPhi(), point.getTheta(), 0.0);
    }

    /** Convert a point on the ellipsoid to the unit 2-sphere.
     * @param point point on the ellipsoid
     * @return point on the unit 2-sphere
     */
    protected S2Point toS2Point(final GeodeticPoint point) {
        return new S2Point(point.getLongitude(), 0.5 * FastMath.PI - point.getLatitude());
    }

    /** Find the along tile direction for tessellation at specified point.
     * @param point point on the ellipsoid
     * @return normalized along tile direction
     * @exception OrekitException if direction cannot be estimated
     */
    protected abstract Vector3D alongTileDirection(GeodeticPoint point) throws OrekitException;

    /** Move to a nearby point.
     * <p>
     * The motion will be approximated, assuming the body surface has constant
     * curvature along the motion direction. The approximation will be accurate
     * for short distances, and error will increase as distance increases.
     * </p>
     * @param start start point
     * @param direction normalized motion direction
     * @param motion motion along surface (can be negative for moving in the reverse direction)
     * @return arrival point, approximately at specified distance from start
     */
    private Vector3D move(final Vector3D start, final Vector3D direction, final double motion) {

        // find elliptic plane section
        final Ellipse planeSection = getEllipsoid().getPlaneSection(start, Vector3D.crossProduct(start, direction));

        // find the center of curvature (point on the evolute) below start point
        final Vector3D omega = planeSection.toSpace(planeSection.getCenterOfCurvature(planeSection.toPlane(start)));

        // compute arrival point, assuming constant radius of curvature
        final Vector3D delta = start.subtract(omega);
        final double   r     = delta.getNorm();
        final double   theta = motion / r;
        return new Vector3D(1, omega, FastMath.cos(theta), delta, r * FastMath.sin(theta), direction);

    }

    /** Check if a candidate tile encounters a zone.
     * @param v0 first vertex
     * @param v1 second vertex
     * @param v2 third vertex
     * @param v3 fourth vertex
     * @param zone zone to check tile against
     * @return true if tile encounters the zone
     */
    private boolean encounters(final GeodeticPoint v0, final GeodeticPoint v1,
                               final GeodeticPoint v2, final GeodeticPoint v3,
                               final SphericalPolygonsSet zone) {
        final SphericalPolygonsSet tileZone =
                new SphericalPolygonsSet(zone.getTolerance(),
                                         toS2Point(v0), toS2Point(v1), toS2Point(v2), toS2Point(v3));
        return !new RegionFactory<Sphere2D>().intersection(tileZone, zone).isEmpty();
    }

    /** Local class holding a mesh aligned along tiles axes. */
    private class Mesh {

        /** Zone of interest to tessellate. */
        private final SphericalPolygonsSet zone;

        /** Map containing nodes. */
        private final Map<Long, Node> nodes;

        /** Simple constructor.
         * @param zone zone of interest to tessellate
         * @param start location of the first node.
         * @exception OrekitException if along direction of first tile cannot be computed
         */
        public Mesh(final SphericalPolygonsSet zone, final Vector3D start)
            throws OrekitException {
            this.zone  = zone;
            this.nodes = new HashMap<Long, Node>();
            store(new Node(start, 0, 0));
        }

        /** Retrieve a node from its indices.
         * @param alongIndex index in the along direction
         * @param acrossIndex index in the across direction
         * @return node or null if no node is available at these indices
         */
        public Node getNode(final int alongIndex, final int acrossIndex) {
            return nodes.get(key(alongIndex, acrossIndex));
        }

        /** Add a node.
         * @param v node location
         * @param alongIndex index in the along direction
         * @param acrossIndex index in the across direction
         * @return added node
         * @exception OrekitException if tile direction cannot be computed
         */
        public Node addNode(final Vector3D v, final int alongIndex, final int acrossIndex)
            throws OrekitException {
            final Node node = new Node(v, alongIndex, acrossIndex);
            store(node);
            return node;
        }

        /** Store a node.
         * @param node to add
         */
        private void store(final Node node) {
            nodes.put(key(node.alongIndex, node.acrossIndex), node);
        }

        /** Convert along and across indices to map key.
         * @param alongIndex index in the along direction
         * @param acrossIndex index in the across direction
         * @return key map key
         */
        private long key(final int alongIndex, final int acrossIndex) {
            return ((long) alongIndex) << 32 | (((long) acrossIndex) & 0xFFFFl);
        }

        /** Container for mesh nodes. */
        private class Node {

            /** Node position in Cartesian coordinates. */
            private final Vector3D v;

            /** Node position in geodetic coordinates. */
            private final GeodeticPoint gp;

            /** Along tile direction. */
            private Vector3D along;

            /** Across tile direction. */
            private Vector3D across;

            /** Indicator for node location with respect to interest zone. */
            private final boolean insideZone;

            /** Index in the along direction. */
            private final int alongIndex;

            /** Index in the across direction. */
            private final int acrossIndex;

            /** Create a node.
             * @param v position in Cartesian coordinates
             * @param alongIndex index in the along direction
             * @param acrossIndex index in the across direction
             * @exception OrekitException if tile direction cannot be computed
             */
            public Node(final Vector3D v, final int alongIndex, final int acrossIndex)
                throws OrekitException {
                this.v           = v;
                this.gp          = ellipsoid.transform(v, ellipsoid.getBodyFrame(), null);
                this.along       = alongTileDirection(gp);
                this.across      = Vector3D.crossProduct(v, along).normalize();
                this.insideZone  = zone.checkPoint(toS2Point(gp)) != Location.OUTSIDE;
                this.alongIndex  = alongIndex;
                this.acrossIndex = acrossIndex;
            }

        }

    }

}
