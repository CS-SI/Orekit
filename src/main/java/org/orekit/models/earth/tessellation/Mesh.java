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
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.partitioning.Region.Location;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.util.FastMath;
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

    /** Aiming used for orienting tiles. */
    private final TileAiming aiming;

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
     * @param start location of the first node.
     * @exception OrekitException if along direction of first tile cannot be computed
     */
    public Mesh(final OneAxisEllipsoid ellipsoid, final SphericalPolygonsSet zone,
                final TileAiming aiming, final Vector3D start)
        throws OrekitException {
        this.ellipsoid      = ellipsoid;
        this.zone           = zone;
        this.aiming         = aiming;
        this.nodes          = new HashMap<Long, Node>();
        this.minAlongIndex  = 0;
        this.maxAlongIndex  = 0;
        this.minAcrossIndex = 0;
        this.maxAcrossIndex = 0;
        store(new Node(start, 0, 0));
    }

    /** Get the minimum along tile index.
     * @return minimum along tile index
     */
    public int getMinAlongIndex() {
        return minAlongIndex;
    }

    /** Get the maximum along tile index.
     * @return maximum along tile index
     */
    public int getMaxAlongIndex() {
        return maxAlongIndex;
    }

    /** Get the minimum along tile index for a specific across index.
     * @param acrossIndex across index to use
     * @return minimum along tile index for a specific across index
     * or {@link #getMaxAlongIndex() getMaxAlongIndex() + 1} if there
     * are no nodes with the specified acrossIndex.
     */
    public int getMinAlongIndex(final int acrossIndex) {
        int min = maxAlongIndex + 1;
        for (final Map.Entry<Long, Node> entry : nodes.entrySet()) {
            final Node node = entry.getValue();
            if (node.getAcrossIndex() == acrossIndex) {
                min = FastMath.min(min, node.getAlongIndex());
            }
        }
        return min;
    }

    /** Get the maximum along tile index for a specific across index.
     * @param acrossIndex across index to use
     * @return maximum along tile index for a specific across index
     * or {@link #getMinAlongIndex() getMinAlongIndex() - 1} if there
     * are no nodes with the specified acrossIndex.
     */
    public int getMaxAlongIndex(final int acrossIndex) {
        int max = minAlongIndex - 1;
        for (final Map.Entry<Long, Node> entry : nodes.entrySet()) {
            final Node node = entry.getValue();
            if (node.getAcrossIndex() == acrossIndex) {
                max = FastMath.max(max, node.getAlongIndex());
            }
        }
        return max;
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
    public class Node {

        /** Node position in Cartesian coordinates. */
        private final Vector3D v;

        /** Node position in geodetic coordinates. */
        private final GeodeticPoint gp;

        /** Along tile direction. */
        private final Vector3D along;

        /** Across tile direction. */
        private final Vector3D across;

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
        private Node(final Vector3D v, final int alongIndex, final int acrossIndex)
            throws OrekitException {
            this.v            = v;
            this.gp           = ellipsoid.transform(v, ellipsoid.getBodyFrame(), null);
            this.along        = aiming.alongTileDirection(v, gp);
            this.across       = Vector3D.crossProduct(v, along).normalize();
            this.insideZone   = zone.checkPoint(new S2Point(gp.getLongitude(), 0.5 * FastMath.PI - gp.getLatitude())) != Location.OUTSIDE;
            this.alongIndex   = alongIndex;
            this.acrossIndex  = acrossIndex;
        }

        /** Get the node position in Cartesian coordinates.
         * @return node position in Cartesian coordinates
         */
        public Vector3D getV() {
            return v;
        }

        /** Get the node position in geodetic coordinates.
         * @return vode position in geodetic coordinates
         */
        public GeodeticPoint getGP() {
            return gp;
        }

        /** Get the along tile direction.
         * @return along tile direction
         */
        public Vector3D getAlong() {
            return along;
        }

        /** Get the across tile direction.
         * @return across tile direction
         */
        public Vector3D getAcross() {
            return across;
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

    }

}
