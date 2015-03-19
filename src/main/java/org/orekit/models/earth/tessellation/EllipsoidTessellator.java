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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.partitioning.BSPTree;
import org.apache.commons.math3.geometry.partitioning.Hyperplane;
import org.apache.commons.math3.geometry.partitioning.SubHyperplane;
import org.apache.commons.math3.geometry.partitioning.Region.Location;
import org.apache.commons.math3.geometry.spherical.oned.ArcsSet;
import org.apache.commons.math3.geometry.spherical.twod.Circle;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.Sphere2D;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.geometry.spherical.twod.SubCircle;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.Ellipse;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;

/** Base class able to tessellate an interest zone on an ellipsoid in {@link Tile tiles}.
 * @author Luc Maisonobe
 */
public abstract class EllipsoidTessellator {

    /** Split factor for tiles fine positioning. */
    private final int SPLITS = 4;

    /** Underlying ellipsoid. */
    private final OneAxisEllipsoid ellipsoid;

    /** Tile half width (distance on ellipsoid surface). */
    private final double splitWidth;

    /** Tile half length (distance on ellipsoid surface). */
    private final double splitLength;

    /** Overlap between adjacent tiles (distance on ellipsoid surface). */
    private final double widthOverlap;

    /** Overlap between adjacent tiles (distance on ellipsoid surface). */
    private final double lengthOverlap;

    /** Simple constructor.
     * @param ellipsoid underlying ellipsoid
     * @param fullWidth full tiles width as a distance on surface, including overlap (in meters)
     * @param fullLength full tiles length as a distance on surface, including overlap (in meters)
     * @param widthOverlap overlap between adjacent tiles (in meters)
     * @param lengthOverlap overlap between adjacent tiles (in meters)
     */
    protected EllipsoidTessellator(final OneAxisEllipsoid ellipsoid,
                                   final double fullWidth, final double fullLength,
                                   final double widthOverlap, final double lengthOverlap) {
        this.ellipsoid     = ellipsoid;
        this.splitWidth    = (fullWidth  - widthOverlap)  / SPLITS;
        this.splitLength   = (fullLength - lengthOverlap) / SPLITS;
        this.widthOverlap  = widthOverlap;
        this.lengthOverlap = lengthOverlap;
    }

    /** Tessellate a zone of interest into tiles.
     * @param zone zone of interest to tessellate
     * @return a list of tiles covering the zone of interest
     * @exception OrekitException if the zone cannot be tessellated
     */
    public List<Tile> tessellate(final SphericalPolygonsSet zone)
        throws OrekitException {

        final List<Tile> tiles = new ArrayList<Tile>();
        SphericalPolygonsSet remaining = zone;

        while (!remaining.isEmpty()) {

            // find a mesh covering at least one connected part of the zone
            final Mesh mesh = findMesh(remaining);

            for (int acrossIndex = firstIndex(mesh.minAcrossIndex, mesh.maxAcrossIndex);
                 acrossIndex < mesh.maxAcrossIndex + SPLITS;
                 acrossIndex += SPLITS) {
                // TODO
            }
            final List<Mesh.Node> boundary = mesh.getTaxicabBoundary();

            // TODO: implement tessellation
            throw OrekitException.createInternalError(null);

        }

        return tiles;

    }

    /** Compute a mesh completely surrounding at least one connected part of a zone.
     * @param zone zone to mesh
     * @return a mesh covering at least one connected part of the zone
     * @exception OrekitException if tile direction cannot be computed
     */
    private Mesh findMesh(final SphericalPolygonsSet zone) throws OrekitException {

        // start mesh inside the zone
        final InsideFinder finder = new InsideFinder(zone.getTolerance());
        zone.getTree(false).visit(finder);
        final Vector3D start = finder.getInsidePoint();
        final Mesh mesh = new Mesh(zone, start);

        // mesh expansion loop
        boolean expanding = true;
        final Queue<Mesh.Node> newNodes = new LinkedList<Mesh.Node>();
        newNodes.add(mesh.getNode(0, 0));
        while (expanding) {

            // first expansion step: set up the mesh so that all its
            // inside nodes are completely surrounded by at least
            // one layer of outside nodes
            while (!newNodes.isEmpty()) {

                // retrieve an active node
                final Mesh.Node node = newNodes.remove();

                if (node.insideZone) {
                    // the node is inside the zone, the mesh must contain its 8 neighbors
                    addAllNeighborsIfNeeded(node, mesh, newNodes);
                }

            }

            // second expansion step: check if the loop of outside nodes
            // completely surrounds the zone, i.e. there are no peaks
            // pointing out of the loop between two nodes
            expanding = false;
            final List<Mesh.Node> boundary = mesh.getTaxicabBoundary();
            Mesh.Node previous = boundary.get(boundary.size() - 1);
            for (final Mesh.Node node : boundary) {
                if (meetInside(toS2Point(previous.gp), toS2Point(node.gp), zone)) {
                    // part of the mesh boundary is still inside the zone!
                    // the mesh must be expanded again
                    addAllNeighborsIfNeeded(previous, mesh, newNodes);
                    addAllNeighborsIfNeeded(node,     mesh, newNodes);
                    expanding = true;
                }
                previous = node;
            }

        }

        return mesh;

    }

    /** Ensure all 8 neighbors of a node are in the mesh.
     * @param base base node
     * @param mesh complete mesh containing nodes
     * @param newNodes queue where new node must be put
     * @exception OrekitException if tile direction cannot be computed
     */
    private void addAllNeighborsIfNeeded(final Mesh.Node base, final Mesh mesh,
                                         final Queue<Mesh.Node> newNodes)
        throws OrekitException {
        final Mesh.Node alongMinus = addNeighborIfNeeded(base, Direction.MINUS_ALONG, mesh, newNodes); // (-1,  0)
        final Mesh.Node alongPlus  = addNeighborIfNeeded(base, Direction.PLUS_ALONG,  mesh, newNodes); // (+1,  0)
        addNeighborIfNeeded(alongMinus, Direction.MINUS_ACROSS, mesh, newNodes);                       // (-1, -1)
        addNeighborIfNeeded(base,       Direction.MINUS_ACROSS, mesh, newNodes);                       // ( 0, -1)
        addNeighborIfNeeded(alongPlus,  Direction.MINUS_ACROSS, mesh, newNodes);                       // (+1, -1)
        addNeighborIfNeeded(alongMinus, Direction.PLUS_ACROSS,  mesh, newNodes);                       // (-1, +1)
        addNeighborIfNeeded(base,       Direction.PLUS_ACROSS,  mesh, newNodes);                       // ( 0, +1)
        addNeighborIfNeeded(alongPlus,  Direction.PLUS_ACROSS,  mesh, newNodes);                       // (+1, +1)
    }

    /** Add a neighbor node to a mesh if not already present.
     * @param base base node
     * @param direction direction of the neighbor
     * @param mesh complete mesh containing nodes
     * @param newNodes queue where new node must be put
     * @return neighbor node (which was either already present, or is created)
     * @exception OrekitException if tile direction cannot be computed
     */
    private Mesh.Node addNeighborIfNeeded(final Mesh.Node base, final Direction direction,
                                          final Mesh mesh, final Queue<Mesh.Node> newNodes)
        throws OrekitException {

        final int alongIndex  = direction.neighborAlongIndex(base);
        final int acrossIndex = direction.neighborAcrossIndex(base);
        Mesh.Node node        = mesh.getNode(alongIndex, acrossIndex);

        if (node == null) {

            // create a new node
            node = mesh.addNode(move(base.v, direction.motion(base, splitLength, splitWidth)),
                                alongIndex, acrossIndex);

            // we have created a new node
            newNodes.add(node);

        }

        // return the node, regardless of it being a new one or not
        return node;

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
     * @param point point on the ellipsoid (Cartesian coordinates)
     * @param gp point on the ellipsoid (geodetic coordinates)
     * @return normalized along tile direction
     * @exception OrekitException if direction cannot be estimated
     */
    protected abstract Vector3D alongTileDirection(Vector3D point, GeodeticPoint gp) throws OrekitException;

    /** Move to a nearby point.
     * <p>
     * The motion will be approximated, assuming the body surface has constant
     * curvature along the motion direction. The approximation will be accurate
     * for short distances, and error will increase as distance increases.
     * </p>
     * @param start start point
     * @param motion straight motion, which must be curved back to surface
     * @return arrival point, approximately at specified distance from start
     */
    private Vector3D move(final Vector3D start, final Vector3D motion) {

        // find elliptic plane section
        final Vector3D normal      = Vector3D.crossProduct(start, motion);
        final Ellipse planeSection = ellipsoid.getPlaneSection(start, normal);

        // find the center of curvature (point on the evolute) below start point
        final Vector2D omega2D = planeSection.getCenterOfCurvature(planeSection.toPlane(start));
        final Vector3D omega3D = planeSection.toSpace(omega2D);

        // compute arrival point, assuming constant radius of curvature
        final Vector3D delta = start.subtract(omega3D);
        final double   theta = motion.getNorm() / delta.getNorm();
        return new Vector3D(1, omega3D,
                            FastMath.cos(theta), delta,
                            FastMath.sin(theta) / theta, motion);

    }

    /** Check if an arc meets the inside of a zone.
     * @param s1 first point
     * @param s2 second point
     * @param zone zone to check arc against
     * @return true if the arc meets the inside of the zone
     */
    private boolean meetInside(final S2Point s1, final S2Point s2,
                               final SphericalPolygonsSet zone) {
        final Circle  circle = new Circle(s1, s2, zone.getTolerance());
        final SubCircle sub  = new SubCircle(circle,
                                             new ArcsSet(circle.toSubSpace(s1).getAlpha(),
                                                         circle.toSubSpace(s2).getAlpha(),
                                                         zone.getTolerance()));
        return recurseMeetInside(zone.getTree(false), sub);

    }

    /** Check if an arc meets the inside of a zone.
     * <p>
     * This method is heavily based on the Characterization class from
     * Apache Commons Math library, also distributed under the terms
     * of the Apache Software License V2.
     * </p>
     * @param node spherical zone node
     * @param sub arc to characterize
     * @return true if the arc meets the inside of the zone
     */
    private boolean recurseMeetInside(final BSPTree<Sphere2D> node, final SubHyperplane<Sphere2D> sub) {

        if (node.getCut() == null) {
            // we have reached a leaf node
            if (sub.isEmpty()) {
                return false;
            } else {
                return (Boolean) node.getAttribute();
            }
        } else {
            final Hyperplane<Sphere2D> hyperplane = node.getCut().getHyperplane();
            switch (sub.side(hyperplane)) {
            case PLUS:
                return recurseMeetInside(node.getPlus(),  sub);
            case MINUS:
                return recurseMeetInside(node.getMinus(), sub);
            case BOTH:
                final SubHyperplane.SplitSubHyperplane<Sphere2D> split = sub.split(hyperplane);
                if (recurseMeetInside(node.getPlus(), split.getPlus())) {
                    return true;
                } else {
                    return recurseMeetInside(node.getMinus(), split.getMinus());
                }
            default:
                // this should not happen
                throw OrekitException.createInternalError(null);
            }
        }
    }

    /** Compute the index of the first vertex of the first tile.
     * @param minIndex minimum node index
     * @param maxIndex maximum node index
     * @return index of the first vertex of the first tile
     */
    private int firstIndex(final int minIndex, final int maxIndex) {

        // number of tiles needed to cover the full indices range
        final int range      = maxIndex - minIndex;
        final int nbTiles    = (range + SPLITS - 1) / SPLITS;

        // extra nodes that must be added to complete the tiles
        final int extraNodes = nbTiles * SPLITS  - range;

        // balance the extra nodes before min index and after maxIndex
        final int extraBefore = (extraNodes + 1) / 2;

        // node index of the first vertex of the first tile
        return minIndex - extraBefore;

    }

    /** Local class holding a mesh aligned along tiles axes. */
    private class Mesh {

        /** Zone of interest to tessellate. */
        private final SphericalPolygonsSet zone;

        /** Map containing nodes. */
        private final Map<Long, Node> nodes;

        /** Minimum along tile index. */
        private int minAlongIndex;

        /** Maximum along tile index. */
        private int maxAlongIndex;

        /** Minimum across tile index. */
        private int minAcrossIndex;

        /** Maximum across tile index. */
        private int maxAcrossIndex;

        /** Simple constructor.
         * @param zone zone of interest to tessellate
         * @param start location of the first node.
         * @exception OrekitException if along direction of first tile cannot be computed
         */
        public Mesh(final SphericalPolygonsSet zone, final Vector3D start)
            throws OrekitException {
            this.zone           = zone;
            this.nodes          = new HashMap<Long, Node>();
            this.minAlongIndex  = 0;
            this.maxAlongIndex  = 0;
            this.minAcrossIndex = 0;
            this.maxAcrossIndex = 0;
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

        /** Find the existing node closest to a location.
         * @param alongIndex index in the along direction
         * @param acrossIndex index in the across direction
         * @return node or null if no node is available at these indices
         */
        public Node getClosestExistingNode(final int alongIndex, final int acrossIndex) {

            // we know at least the first node (at 0, 0) exists, we use its
            // taxicab distance to the desired location as a search limit
            final int maxD = FastMath.max(FastMath.abs(alongIndex), FastMath.abs(acrossIndex));

            // search for an existing point in increasing taxicab distances
            for (int d = 0; d < maxD; ++d) {
                for (int deltaAcross = 0; deltaAcross <= d; ++deltaAcross) {
                    final int deltaAlong = d - deltaAcross;
                    Node node = getNode(alongIndex - deltaAlong, acrossIndex - deltaAcross);
                    if (node != null) {
                        return node;
                    }
                    if (deltaAcross != 0) {
                        node = getNode(alongIndex - deltaAlong, acrossIndex + deltaAcross);
                        if (node != null) {
                            return node;
                        }
                    }
                    if (deltaAlong != 0) {
                        node = getNode(alongIndex + deltaAlong, acrossIndex - deltaAcross);
                        if (node != null) {
                            return node;
                        }
                        if (deltaAcross != 0) {
                            node = getNode(alongIndex + deltaAlong, acrossIndex + deltaAcross);
                            if (node != null) {
                                return node;
                            }
                        }
                    }
                }
            }

            // at least the first node always exists
            return getNode(0, 0);

        }

        /** Get the oriented list of nodes at mesh boundary, in taxicab geometry.
         * @return list of nodes
         */
        public List<Node> getTaxicabBoundary() {

            final List<Node> boundary = new ArrayList<Node>();

            // search for the lower left corner
            Node lowerLeft = null;
            for (int i = minAlongIndex; lowerLeft == null && i <= maxAlongIndex; ++i) {
                for (int j = minAcrossIndex; lowerLeft == null && j <= maxAcrossIndex; ++j) {
                    lowerLeft = getNode(i, j);
                }
            }

            // loop counterclockwise around the mesh
            Direction direction = Direction.PLUS_ALONG;
            Node node = lowerLeft;
            do {
                boundary.add(node);
                Node neighbor = null;
                while (neighbor == null) {
                    neighbor = getNode(direction.neighborAlongIndex(node),
                                       direction.neighborAcrossIndex(node));
                    if (neighbor == null) {
                        direction = direction.next();
                    }
                }
                node = neighbor;
            } while (node != lowerLeft);

            return boundary;

        }

        /** Store a node.
         * @param node to add
         */
        private void store(final Node node) {
            minAlongIndex  = FastMath.min(minAlongIndex,  node.alongIndex);
            maxAlongIndex  = FastMath.max(maxAlongIndex,  node.alongIndex);
            minAcrossIndex = FastMath.min(minAcrossIndex, node.acrossIndex);
            maxAcrossIndex = FastMath.max(maxAcrossIndex, node.acrossIndex);
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
                this.along       = alongTileDirection(v, gp);
                this.insideZone  = zone.checkPoint(toS2Point(gp)) != Location.OUTSIDE;
                this.alongIndex  = alongIndex;
                this.acrossIndex = acrossIndex;
            }

        }

    }

    /** Neighboring directions. */
    private enum Direction {

        /** Along tile in the plus direction. */
        PLUS_ALONG() {

            /** {@inheritDoc} */
            @Override
            public Direction next() {
                return PLUS_ACROSS;
            }

            /** {@inheritDoc} */
            @Override
            public int neighborAlongIndex(final Mesh.Node base) {
                return base.alongIndex + 1;
            }

            /** {@inheritDoc} */
            @Override
            public Vector3D motion(final Mesh.Node base,
                                   final double alongDistance, final double acrossDistance) {
                return new Vector3D(alongDistance, base.along);
            }

        },

        /** Along tile in the minus direction. */
        MINUS_ALONG() {

            /** {@inheritDoc} */
            @Override
            public Direction next() {
                return MINUS_ACROSS;
            }

            /** {@inheritDoc} */
            @Override
            public int neighborAlongIndex(final Mesh.Node base) {
                return base.alongIndex - 1;
            }

            /** {@inheritDoc} */
            @Override
            public Vector3D motion(final Mesh.Node base,
                                   final double alongDistance, final double acrossDistance) {
                return new Vector3D(-alongDistance, base.along);
            }

        },

        /** Across tile in the plus direction. */
        PLUS_ACROSS() {

            /** {@inheritDoc} */
            @Override
            public Direction next() {
                return MINUS_ALONG;
            }

            /** {@inheritDoc} */
            @Override
            public int neighborAcrossIndex(final Mesh.Node base) {
                return base.acrossIndex + 1;
            }

            /** {@inheritDoc} */
            @Override
            public Vector3D motion(final Mesh.Node base,
                                   final double alongDistance, final double acrossDistance) {
                return new Vector3D(acrossDistance,
                                    Vector3D.crossProduct(base.v, base.along).normalize());
            }

        },

        /** Across tile in the minus direction. */
        MINUS_ACROSS() {

            /** {@inheritDoc} */
            @Override
            public Direction next() {
                return PLUS_ALONG;
            }

            /** {@inheritDoc} */
            @Override
            public int neighborAcrossIndex(final Mesh.Node base) {
                return base.acrossIndex - 1;
            }

            /** {@inheritDoc} */
            @Override
            public Vector3D motion(final Mesh.Node base,
                                   final double alongDistance, final double acrossDistance) {
                return new Vector3D(-acrossDistance,
                                    Vector3D.crossProduct(base.v, base.along).normalize());
            }

        };

        /** Get the next direction in counterclockwise order.
         * @return next direction
         */
        public abstract Direction next();

        /** Get the along index of neighbor.
         * @param base base node
         * @return along index of neighbor node
         */
        public int neighborAlongIndex(final Mesh.Node base) {
            return base.alongIndex;
        }

        /** Get the across index of neighbor.
         * @param base base node
         * @return across index of neighbor node
         */
        public int neighborAcrossIndex(final Mesh.Node base) {
            return base.acrossIndex;
        }

        /** Get the motion towards neighbor.
         * @param base base node
         * @param alongDistance distance for along tile motions
         * @param acrossDistance distance for across tile motions
         * @return motion towards neighbor
         */
        public abstract Vector3D motion(Mesh.Node base,
                                        double alongDistance, double acrossDistance);

    }

}
