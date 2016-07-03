/* Copyright 2002-2016 CS Systèmes d'Information
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
import java.util.List;
import java.util.Map;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.bodies.Ellipse;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;

/** Class creating a mesh for spherical zones tessellation.
 * @author Luc Maisonobe
 */
class Mesh {

    /** Underlying ellipsoid. */
    private final OneAxisEllipsoid ellipsoid;

    /** Zone of interest to tessellate. */
    private final SphericalPolygonsSet zone;

    /** Zone covered by the mesh. */
    private SphericalPolygonsSet coverage;

    /** Aiming used for orienting tiles. */
    private final TileAiming aiming;

    /** Distance between nodes in the along direction. */
    private final double alongGap;

    /** Distance between nodes in the across direction. */
    private final double acrossGap;

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
     * @param ellipsoid underlying ellipsoid
     * @param zone zone of interest to tessellate
     * @param aiming aiming used for orienting tiles
     * @param alongGap distance between nodes in the along direction
     * @param acrossGap distance between nodes in the across direction
     * @param start location of the first node.
     * @exception OrekitException if along direction of first tile cannot be computed
     */
    Mesh(final OneAxisEllipsoid ellipsoid, final SphericalPolygonsSet zone,
                final TileAiming aiming, final double alongGap, final double acrossGap,
                final S2Point start)
        throws OrekitException {
        this.ellipsoid      = ellipsoid;
        this.zone           = zone;
        this.coverage       = null;
        this.aiming         = aiming;
        this.alongGap       = alongGap;
        this.acrossGap      = acrossGap;
        this.nodes          = new HashMap<Long, Node>();
        this.minAlongIndex  = 0;
        this.maxAlongIndex  = 0;
        this.minAcrossIndex = 0;
        this.maxAcrossIndex = 0;

        // create an enabled first node at origin
        final Node origin = new Node(start, 0, 0);
        origin.setEnabled();

        // force the first node to be considered inside
        // It may appear outside if the zone is very thin and
        // BSPTree.checkPoint selects a very close but wrong
        // tree leaf tree for the point. Even in this case,
        // we want the mesh to be properly defined and surround
        // the area
        origin.forceInside();

        store(origin);

    }

    /** Get the minimum along tile index for enabled nodes.
     * @return minimum along tile index for enabled nodes
     */
    public int getMinAlongIndex() {
        return minAlongIndex;
    }

    /** Get the maximum along tile index for enabled nodes.
     * @return maximum along tile index for enabled nodes
     */
    public int getMaxAlongIndex() {
        return maxAlongIndex;
    }

    /** Get the minimum along tile index for enabled nodes for a specific across index.
     * @param acrossIndex across index to use
     * @return minimum along tile index for enabled nodes for a specific across index
     * or {@link #getMaxAlongIndex() getMaxAlongIndex() + 1} if there
     * are no nodes with the specified acrossIndex.
     */
    public int getMinAlongIndex(final int acrossIndex) {
        for (int alongIndex = minAlongIndex; alongIndex <= maxAlongIndex; ++alongIndex) {
            final Node node = getNode(alongIndex, acrossIndex);
            if (node != null && node.isEnabled()) {
                return alongIndex;
            }
        }
        return maxAlongIndex + 1;
    }

    /** Get the maximum along tile index for enabled nodes for a specific across index.
     * @param acrossIndex across index to use
     * @return maximum along tile index for enabled nodes for a specific across index
     * or {@link #getMinAlongIndex() getMinAlongIndex() - 1} if there
     * are no nodes with the specified acrossIndex.
     */
    public int getMaxAlongIndex(final int acrossIndex) {
        for (int alongIndex = maxAlongIndex; alongIndex >= minAlongIndex; --alongIndex) {
            final Node node = getNode(alongIndex, acrossIndex);
            if (node != null && node.isEnabled()) {
                return alongIndex;
            }
        }
        return minAlongIndex - 1;
    }

    /** Get the minimum across tile index.
     * @return minimum across tile index
     */
    public int getMinAcrossIndex() {
        return minAcrossIndex;
    }

    /** Get the maximum across tile index.
     * @return maximum across tile index
     */
    public int getMaxAcrossIndex() {
        return maxAcrossIndex;
    }

    /** Get the number of nodes.
     * @return number of nodes
     */
    public int getNumberOfNodes() {
        return nodes.size();
    }

    /** Get the distance between nodes in the along direction.
     * @return distance between nodes in the along direction
     */
    public double getAlongGap() {
        return alongGap;
    }

    /** Get the distance between nodes in the across direction.
     * @return distance between nodes in the across direction
     */
    public double getAcrossGap() {
        return acrossGap;
    }

    /** Retrieve a node from its indices.
     * @param alongIndex index in the along direction
     * @param acrossIndex index in the across direction
     * @return node or null if no nodes are available at these indices
     * @see #addNode(int, int)
     */
    public Node getNode(final int alongIndex, final int acrossIndex) {
        return nodes.get(key(alongIndex, acrossIndex));
    }

    /** Add a node.
     * <p>
     * This method is similar to {@link #getNode(int, int) getNode} except
     * it creates the node if it doesn't alreay exists. All created nodes
     * have a status set to {@code disabled}.
     * </p>
     * @param alongIndex index in the along direction
     * @param acrossIndex index in the across direction
     * @return node at specified indices, guaranteed to be non-null
     * @exception OrekitException if tile direction cannot be computed
     * @see #getNode(int, int)
     */
    public Node addNode(final int alongIndex, final int acrossIndex)
        throws OrekitException {

        // create intermediate (disabled) nodes, up to specified indices
        Node node = getExistingAncestor(alongIndex, acrossIndex);
        while (node.getAlongIndex() != alongIndex || node.getAcrossIndex() != acrossIndex) {
            final Direction direction;
            if (node.getAlongIndex() < alongIndex) {
                direction = Direction.PLUS_ALONG;
            } else if (node.getAlongIndex() > alongIndex) {
                direction = Direction.MINUS_ALONG;
            } else if (node.getAcrossIndex() < acrossIndex) {
                direction = Direction.PLUS_ACROSS;
            } else {
                direction = Direction.MINUS_ACROSS;
            }
            final S2Point s2p = node.move(direction.motion(node, alongGap, acrossGap));
            node = new Node(s2p, direction.neighborAlongIndex(node), direction.neighborAcrossIndex(node));
            store(node);
        }

        return node;

    }

    /** Find the closest existing ancestor of a node.
     * <p>
     * The path from origin to any node is first in the along direction,
     * and then in the across direction. Using always the same path pattern
     * ensures consistent distortion of the mesh.
     * </p>
     * @param alongIndex index in the along direction
     * @param acrossIndex index in the across direction
     * @return an existing node in the path from origin to specified indices
     */
    private Node getExistingAncestor(final int alongIndex, final int acrossIndex) {

        // start from the desired node indices
        int l = alongIndex;
        int c = acrossIndex;
        Node node = getNode(l, c);

        // rewind the path backward, up to origin,
        // that is first in the across direction, then in the along direction
        // the loop WILL end as there is at least one node at (0, 0)
        while (node == null) {
            if (c != 0) {
                c += c > 0 ? -1 : +1;
            } else {
                l += l > 0 ? -1 : +1;
            }
            node = getNode(l, c);
        }

        // we have found an existing ancestor
        return node;

    }

    /** Get the nodes that lie inside the interest zone.
     * @return nodes that lie inside the interest zone
     */
    public List<Node> getInsideNodes() {
        final List<Node> insideNodes = new ArrayList<Node>();
        for (final Map.Entry<Long, Node> entry : nodes.entrySet()) {
            if (entry.getValue().isInside()) {
                insideNodes.add(entry.getValue());
            }
        }
        return insideNodes;
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

    /** Find the existing node closest to a location.
     * @param location reference location in Cartesian coordinates
     * @return node or null if no node is available at these indices
     */
    public Node getClosestExistingNode(final Vector3D location) {
        Node selected = null;
        double min = Double.POSITIVE_INFINITY;
        for (final Map.Entry<Long, Node> entry : nodes.entrySet()) {
            final double distance = Vector3D.distance(location, entry.getValue().getV());
            if (distance < min) {
                selected = entry.getValue();
                min      = distance;
            }
        }
        return selected;
    }

    /** Get the oriented list of <em>enabled</em> nodes at mesh boundary, in taxicab geometry.
     * @param simplified if true, don't include intermediate points along almost straight lines
     * @return list of nodes
     */
    public List<Node> getTaxicabBoundary(final boolean simplified) {

        final List<Node> boundary = new ArrayList<Node>();
        if (nodes.size() < 2) {
            boundary.add(getNode(0, 0));
        } else {

            // search for the lower left corner
            Node lowerLeft = null;
            for (int i = minAlongIndex; lowerLeft == null && i <= maxAlongIndex; ++i) {
                for (int j = minAcrossIndex; lowerLeft == null && j <= maxAcrossIndex; ++j) {
                    lowerLeft = getNode(i, j);
                    if (lowerLeft != null && !lowerLeft.isEnabled()) {
                        lowerLeft = null;
                    }
                }
            }

            // loop counterclockwise around the mesh
            Direction direction = Direction.MINUS_ACROSS;
            Node node = lowerLeft;
            do {
                boundary.add(node);
                Node neighbor = null;
                do {
                    direction = direction.next();
                    neighbor = getNode(direction.neighborAlongIndex(node),
                                       direction.neighborAcrossIndex(node));
                } while (neighbor == null || !neighbor.isEnabled());
                direction = direction.next().next();
                node = neighbor;
            } while (node != lowerLeft);
        }

        // filter out infinitely thin parts corresponding to spikes
        // joining outliers points to the main mesh
        boolean changed = true;
        while (changed && boundary.size() > 1) {
            changed = false;
            final int n = boundary.size();
            for (int i = 0; i < n; ++i) {
                final int previousIndex = (i + n - 1) % n;
                final int nextIndex     = (i + 1)     % n;
                if (boundary.get(previousIndex) == boundary.get(nextIndex)) {
                    // the current point is an infinitely thin spike, remove it
                    boundary.remove(FastMath.max(i, nextIndex));
                    boundary.remove(FastMath.min(i, nextIndex));
                    changed = true;
                    break;
                }
            }
        }

        if (simplified) {
            for (int i = 0; i < boundary.size(); ++i) {
                final int  n        = boundary.size();
                final Node previous = boundary.get((i + n - 1) % n);
                final int  pl       = previous.getAlongIndex();
                final int  pc       = previous.getAcrossIndex();
                final Node current  = boundary.get(i);
                final int  cl       = current.getAlongIndex();
                final int  cc       = current.getAcrossIndex();
                final Node next     = boundary.get((i + 1)     % n);
                final int  nl       = next.getAlongIndex();
                final int  nc       = next.getAcrossIndex();
                if ((pl == cl && cl == nl) || (pc == cc && cc == nc)) {
                    // the current point is a spurious intermediate in a straight line, remove it
                    boundary.remove(i--);
                }
            }
        }

        return boundary;

    }

    /** Get the zone covered by the mesh.
     * @return mesh coverage
     */
    public SphericalPolygonsSet getCoverage() {

        if (coverage == null) {

            // lazy build of mesh coverage
            final List<Mesh.Node> boundary = getTaxicabBoundary(true);
            final S2Point[] vertices = new S2Point[boundary.size()];
            for (int i = 0; i < vertices.length; ++i) {
                vertices[i] = boundary.get(i).getS2P();
            }
            coverage = new SphericalPolygonsSet(zone.getTolerance(), vertices);
        }

        // as caller may modify the BSP tree, we must provide a copy of our safe instance
        return (SphericalPolygonsSet) coverage.copySelf();

    }

    /** Store a node.
     * @param node to add
     */
    private void store(final Node node) {

        // the new node invalidates current estimation of the coverage
        coverage = null;

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
    public class Node {

        /** Node position in spherical coordinates. */
        private final S2Point s2p;

        /** Node position in Cartesian coordinates. */
        private final Vector3D v;

        /** Normalized along tile direction. */
        private final Vector3D along;

        /** Normalized across tile direction. */
        private final Vector3D across;

        /** Indicator for node location with respect to interest zone. */
        private boolean insideZone;

        /** Index in the along direction. */
        private final int alongIndex;

        /** Index in the across direction. */
        private final int acrossIndex;

        /** Indicator for construction nodes used only as intermediate points (disabled) vs. real nodes (enabled). */
        private boolean enabled;

        /** Create a node.
         * @param s2p position in spherical coordinates (my be null)
         * @param alongIndex index in the along direction
         * @param acrossIndex index in the across direction
         * @exception OrekitException if tile direction cannot be computed
         */
        private Node(final S2Point s2p, final int alongIndex, final int acrossIndex)
            throws OrekitException {
            final GeodeticPoint gp = new GeodeticPoint(0.5 * FastMath.PI - s2p.getPhi(), s2p.getTheta(), 0.0);
            this.v           = ellipsoid.transform(gp);
            this.s2p         = s2p;
            this.along       = aiming.alongTileDirection(v, gp);
            this.across      = Vector3D.crossProduct(v, along).normalize();
            this.insideZone  = zone.checkPoint(s2p) != Location.OUTSIDE;
            this.alongIndex  = alongIndex;
            this.acrossIndex = acrossIndex;
            this.enabled     = false;
        }

        /** Set the enabled property.
         */
        public void setEnabled() {

            // store status
            this.enabled = true;

            // update min/max indices
            minAlongIndex  = FastMath.min(minAlongIndex,  alongIndex);
            maxAlongIndex  = FastMath.max(maxAlongIndex,  alongIndex);
            minAcrossIndex = FastMath.min(minAcrossIndex, acrossIndex);
            maxAcrossIndex = FastMath.max(maxAcrossIndex, acrossIndex);

        }

        /** Check if a node is enabled.
         * @return true if the node is enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /** Get the node position in spherical coordinates.
         * @return vode position in spherical coordinates
         */
        public S2Point getS2P() {
            return s2p;
        }

        /** Get the node position in Cartesian coordinates.
         * @return vode position in Cartesian coordinates
         */
        public Vector3D getV() {
            return v;
        }

        /** Get the normalized along tile direction.
         * @return normalized along tile direction
         */
        public Vector3D getAlong() {
            return along;
        }

        /** Get the normalized across tile direction.
         * @return normalized across tile direction
         */
        public Vector3D getAcross() {
            return across;
        }

        /** Force the node location to be considered inside interest zone.
         */
        private void forceInside() {
            insideZone = true;
        }

        /** Check if the node location is inside interest zone.
         * @return true if the node location is inside interest zone
         */
        public boolean isInside() {
            return insideZone;
        }

        /** Get the index in the along direction.
         * @return index in the along direction
         */
        public int getAlongIndex() {
            return alongIndex;
        }

        /** Get the index in the across direction.
         * @return index in the across direction
         */
        public int getAcrossIndex() {
            return acrossIndex;
        }

        /** Move to a nearby point along surface.
         * <p>
         * The motion will be approximated, assuming the body surface has constant
         * curvature along the motion direction. The approximation will be accurate
         * for short distances, and error will increase as distance increases.
         * </p>
         * @param motion straight motion, which must be curved back to surface
         * @return arrival point, approximately at specified distance from node
         * @exception OrekitException if points cannot be converted to geodetic coordinates
         */
        public S2Point move(final Vector3D motion)
            throws OrekitException {

            // safety check for too small motion
            if (motion.getNorm() < Precision.EPSILON * v.getNorm()) {
                return s2p;
            }

            // find elliptic plane section
            final Vector3D normal      = Vector3D.crossProduct(v, motion);
            final Ellipse planeSection = ellipsoid.getPlaneSection(v, normal);

            // find the center of curvature (point on the evolute) below start point
            final Vector2D omega2D = planeSection.getCenterOfCurvature(planeSection.toPlane(v));
            final Vector3D omega3D = planeSection.toSpace(omega2D);

            // compute approximated arrival point, assuming constant radius of curvature
            final Vector3D delta = v.subtract(omega3D);
            final double   theta = motion.getNorm() / delta.getNorm();
            final Vector3D approximated = new Vector3D(1, omega3D,
                                                       FastMath.cos(theta), delta,
                                                       FastMath.sin(theta) / theta, motion);

            // convert to spherical coordinates
            final GeodeticPoint approximatedGP = ellipsoid.transform(approximated, ellipsoid.getBodyFrame(), null);
            return new S2Point(approximatedGP.getLongitude(), 0.5 * FastMath.PI - approximatedGP.getLatitude());

        }

    }

}
