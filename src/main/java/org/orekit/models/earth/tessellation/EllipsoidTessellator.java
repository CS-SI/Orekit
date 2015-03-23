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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.partitioning.BSPTree;
import org.apache.commons.math3.geometry.partitioning.Hyperplane;
import org.apache.commons.math3.geometry.partitioning.Region;
import org.apache.commons.math3.geometry.partitioning.RegionFactory;
import org.apache.commons.math3.geometry.partitioning.SubHyperplane;
import org.apache.commons.math3.geometry.spherical.oned.ArcsSet;
import org.apache.commons.math3.geometry.spherical.twod.Circle;
import org.apache.commons.math3.geometry.spherical.twod.S2Point;
import org.apache.commons.math3.geometry.spherical.twod.Sphere2D;
import org.apache.commons.math3.geometry.spherical.twod.SphericalPolygonsSet;
import org.apache.commons.math3.geometry.spherical.twod.SubCircle;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;
import org.orekit.bodies.Ellipse;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;

/** Class used to tessellate an interest zone on an ellipsoid in {@link Tile tiles}.
 * @author Luc Maisonobe
 */
public class EllipsoidTessellator {

    /** Split factor for tiles fine positioning. */
    private final int SPLITS = 4;

    /** Aiming used for orienting tiles. */
    private final TileAiming aiming;

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
     * @param aiming aiming used for orienting tiles
     * @param fullWidth full tiles width as a distance on surface, including overlap (in meters)
     * @param fullLength full tiles length as a distance on surface, including overlap (in meters)
     * @param widthOverlap overlap between adjacent tiles (in meters)
     * @param lengthOverlap overlap between adjacent tiles (in meters)
     */
    protected EllipsoidTessellator(final OneAxisEllipsoid ellipsoid, final TileAiming aiming,
                                   final double fullWidth, final double fullLength,
                                   final double widthOverlap, final double lengthOverlap) {
        this.ellipsoid     = ellipsoid;
        this.aiming        = aiming;
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

        final List<Tile>              tiles     = new ArrayList<Tile>();
        final RegionFactory<Sphere2D> factory   = new RegionFactory<Sphere2D>();
        SphericalPolygonsSet          remaining = zone;

        while (!remaining.isEmpty()) {

            // find a mesh covering at least one connected part of the zone
            final Mesh mesh = findMesh(remaining);

            // extract the tiles from the mesh
            Region<Sphere2D> meshCoverage = factory.getComplement(new SphericalPolygonsSet(zone.getTolerance()));
            final int minAcross = mesh.getMinAcrossIndex();
            final int maxAcross = mesh.getMaxAcrossIndex();
            for (int acrossIndex = firstIndex(minAcross, maxAcross); acrossIndex < maxAcross; acrossIndex += SPLITS) {
                final int minAlong = FastMath.min(mesh.getMinAlongIndex(acrossIndex),
                                                  mesh.getMinAlongIndex(acrossIndex + SPLITS));
                final int maxAlong = FastMath.max(mesh.getMaxAlongIndex(acrossIndex),
                                                  mesh.getMaxAlongIndex(acrossIndex + SPLITS));
                for (int alongIndex = firstIndex(minAlong, maxAlong); alongIndex < maxAlong; alongIndex += SPLITS) {

                    // get the base vertex nodes
                    final Mesh.Node node0 = getTileVertexNode(mesh, alongIndex,          acrossIndex);
                    final Mesh.Node node1 = getTileVertexNode(mesh, alongIndex + SPLITS, acrossIndex);
                    final Mesh.Node node2 = getTileVertexNode(mesh, alongIndex + SPLITS, acrossIndex + SPLITS);
                    final Mesh.Node node3 = getTileVertexNode(mesh, alongIndex,          acrossIndex + SPLITS);

                    // apply tile overlap
                    final Vector3D v0 = move(node0.getV(),
                                             new Vector3D(-lengthOverlap, node0.getAlong(),
                                                          -widthOverlap,  node0.getAcross()));
                    final Vector3D v1 = move(node1.getV(),
                                             new Vector3D(+lengthOverlap, node1.getAlong(),
                                                          -widthOverlap,  node1.getAcross()));
                    final Vector3D v2 = move(node2.getV(),
                                             new Vector3D(+lengthOverlap, node2.getAlong(),
                                                          +widthOverlap,  node2.getAcross()));
                    final Vector3D v3 = move(node3.getV(),
                                             new Vector3D(-lengthOverlap, node2.getAlong(),
                                                          +widthOverlap,  node2.getAcross()));

                    // create a quadrilateral region corresponding to the candidate tile
                    final GeodeticPoint gp0 = ellipsoid.transform(v0, ellipsoid.getBodyFrame(), null);
                    final GeodeticPoint gp1 = ellipsoid.transform(v1, ellipsoid.getBodyFrame(), null);
                    final GeodeticPoint gp2 = ellipsoid.transform(v2, ellipsoid.getBodyFrame(), null);
                    final GeodeticPoint gp3 = ellipsoid.transform(v3, ellipsoid.getBodyFrame(), null);
                    final SphericalPolygonsSet quadrilateral =
                            new SphericalPolygonsSet(zone.getTolerance(),
                                                     toS2Point(gp0), toS2Point(gp1), toS2Point(gp2), toS2Point(gp3));

                    if (!factory.intersection(remaining, quadrilateral).isEmpty()) {
                        // the tile does cover part of the remaining zone, it contributes to the tesselation
                        tiles.add(new Tile(gp0, gp1, gp2, gp3));
                        meshCoverage = factory.union(meshCoverage, quadrilateral);
                    }

                }
            }

            // remove the part of the zone covered by the mesh
            remaining = (SphericalPolygonsSet) factory.difference(remaining, meshCoverage);

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
        final GeodeticPoint start = toGeodetic(finder.getInsidePoint());
        final Mesh mesh = new Mesh(ellipsoid, zone, aiming, start);

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

                if (node.isInside()) {
                    // the node is inside the zone, the mesh must contain its 8 neighbors
                    addAllNeighborsIfNeeded(node, mesh, newNodes);
                }

            }

            // second expansion step: check if the loop of outside nodes
            // completely surrounds the zone, i.e. there are no peaks
            // pointing out of the loop between two nodes
            expanding = false;
            final List<Mesh.Node> boundary = mesh.getTaxicabBoundary();
            if (boundary.size() > 1) {
                Mesh.Node previous = boundary.get(boundary.size() - 1);
                for (final Mesh.Node node : boundary) {
                    if (meetInside(toS2Point(previous.getGP()), toS2Point(node.getGP()), zone)) {
                        // part of the mesh boundary is still inside the zone!
                        // the mesh must be expanded again
                        addAllNeighborsIfNeeded(previous, mesh, newNodes);
                        addAllNeighborsIfNeeded(node,     mesh, newNodes);
                        expanding = true;
                    }
                    previous = node;
                }
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
     * @param newNodes queue where new node must be put (may be null)
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
            node = mesh.addNode(move(base.getV(), direction.motion(base, splitLength, splitWidth)),
                                alongIndex, acrossIndex);

            if (newNodes != null) {
                // we have created a new node
                newNodes.add(node);
            }

        }

        // return the node, regardless of it being a new one or not
        return node;

    }

    /** Get a node to be used as a tile vertex, creating it if needed.
     * @param mesh complete mesh containing nodes
     * @param alongIndex index of the tile vertex node
     * @param acrossIndex index of the tile vertex node
     * @return tile vertex node
     * @exception OrekitException if tile direction cannot be computed
     */
    private Mesh.Node getTileVertexNode(final Mesh mesh, final int alongIndex, final int acrossIndex)
        throws OrekitException {

        Mesh.Node node = mesh.getClosestExistingNode(alongIndex, acrossIndex);

        while (node.getAlongIndex() < alongIndex) {
            // the node is before desired index in the along direction
            // we need to create intermediates nodes up to the desired index
            node = addNeighborIfNeeded(node, Direction.PLUS_ALONG, mesh, null);
        }

        while (node.getAlongIndex() > alongIndex) {
            // the node is after desired index in the along direction
            // we need to create intermediates nodes up to the desired index
            node = addNeighborIfNeeded(node, Direction.MINUS_ALONG, mesh, null);
        }

        while (node.getAcrossIndex() < acrossIndex) {
            // the node is before desired index in the across direction
            // we need to create intermediates nodes up to the desired index
            node = addNeighborIfNeeded(node, Direction.PLUS_ACROSS, mesh, null);
        }

        while (node.getAcrossIndex() > acrossIndex) {
            // the node is after desired index in the across direction
            // we need to create intermediates nodes up to the desired index
            node = addNeighborIfNeeded(node, Direction.MINUS_ACROSS, mesh, null);
        }

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

        // safety check for too small motion
        if (motion.getNorm() < Precision.EPSILON * start.getNorm()) {
            return start;
        }

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
        final double alpha1  = circle.toSubSpace(s1).getAlpha();
        final double alpha2  = MathUtils.normalizeAngle(circle.toSubSpace(s2).getAlpha(),
                                                        alpha1 + FastMath.PI);
        final SubCircle sub  = new SubCircle(circle,
                                             new ArcsSet(alpha1, alpha2, zone.getTolerance()));
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

}
