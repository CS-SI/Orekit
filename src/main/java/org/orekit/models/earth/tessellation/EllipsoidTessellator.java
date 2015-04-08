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
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.partitioning.BSPTree;
import org.apache.commons.math3.geometry.partitioning.Hyperplane;
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
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;

/** Class used to tessellate an interest zone on an ellipsoid in {@link Tile tiles}.
 * <p>
 * This class is typically used for Earth Observation missions, in order to
 * create tiles that may be used as the basis of visibility event detectors.
 * </p>
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
     * @param widthOverlap overlap between adjacent tiles (in meters), if negative the tiles
     * will have a gap between each other instead of an overlap
     * @param lengthOverlap overlap between adjacent tiles (in meters), if negative the tiles
     * will have a gap between each other instead of an overlap
     */
    public EllipsoidTessellator(final OneAxisEllipsoid ellipsoid, final TileAiming aiming,
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
     * @return a list of lists of tiles covering the zone of interest,
     * each sub-list corresponding to a part not connected to the other
     * parts (for example for islands)
     * @exception OrekitException if the zone cannot be tessellated
     */
    public List<List<Tile>> tessellate(final SphericalPolygonsSet zone)
        throws OrekitException {

        final Map<Mesh, List<Tile>>   map       = new IdentityHashMap<Mesh, List<Tile>>();
        final RegionFactory<Sphere2D> factory   = new RegionFactory<Sphere2D>();
        SphericalPolygonsSet          remaining = zone;

        while (!remaining.isEmpty()) {

            // find a mesh covering at least one connected part of the zone
            final List<Mesh.Node> mergingSeeds = new ArrayList<Mesh.Node>();
            Mesh mesh = createMesh(remaining);
            mergingSeeds.add(mesh.getNode(0, 0));
            List<Tile> tiles = null;
            while (!mergingSeeds.isEmpty()) {

                // expand the mesh around the seed
                neighborExpandMesh(mesh, mergingSeeds, zone);

                // extract the tiles from the mesh
                // this further expands the mesh so tiles dimensions are multiples of SPLITS,
                // hence it must be performed here before checking meshes independence
                tiles = extractTiles(mesh, zone);

                // check the mesh is independent from existing meshes
                mergingSeeds.clear();
                for (final Map.Entry<Mesh, List<Tile>> entry : map.entrySet()) {
                    if (!factory.intersection(mesh.getCoverage(), entry.getKey().getCoverage()).isEmpty()) {
                        // the meshes are not independent, they intersect each other!

                        // merge the two meshes together
                        mesh = mergeMeshes(mesh, entry.getKey(), mergingSeeds);
                        map.remove(entry.getKey());
                        break;

                    }
                }

            }

            // remove the part of the zone covered by the mesh
            remaining = (SphericalPolygonsSet) factory.difference(remaining, mesh.getCoverage());

            map.put(mesh, tiles);

        }

        // concatenate the lists from the independent meshes
        final List<List<Tile>> tilesLists = new ArrayList<List<Tile>>(map.size());
        for (final Map.Entry<Mesh, List<Tile>> entry : map.entrySet()) {
            tilesLists.add(entry.getValue());
        }

        return tilesLists;

    }

    /** Compute a mesh completely surrounding at least one connected part of a zone.
     * @param zone zone to mesh
     * @return a mesh covering at least one connected part of the zone
     * @exception OrekitException if tile direction cannot be computed
     */
    private Mesh createMesh(final SphericalPolygonsSet zone) throws OrekitException {

        // start mesh inside the zone
        final InsideFinder finder = new InsideFinder(zone.getTolerance());
        zone.getTree(false).visit(finder);
        final GeodeticPoint start = toGeodetic(finder.getInsidePoint());
        return new Mesh(ellipsoid, zone, aiming, splitLength, splitWidth, start);

    }

    /** Expand a mesh so it surrounds at least one connected part of a zone.
     * <p>
     * This part of mesh expansion is neighbors based. It includes the seed
     * node neighbors, and their neighbors, and the neighbors of their
     * neighbors until the path-connected sub-parts of the zone these nodes
     * belong to are completely surrounded by the mesh taxicab boundary.
     * </p>
     * @param mesh mesh to expand
     * @param seeds seed nodes (already in the mesh) from which to start expansion
     * @param zone zone to mesh
     * @exception OrekitException if tile direction cannot be computed
     */
    private void neighborExpandMesh(final Mesh mesh, final Collection<Mesh.Node> seeds,
                                    final SphericalPolygonsSet zone)
        throws OrekitException {

        // mesh expansion loop
        boolean expanding = true;
        final Queue<Mesh.Node> newNodes = new LinkedList<Mesh.Node>();
        newNodes.addAll(seeds);
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
            final List<Mesh.Node> boundary = mesh.getTaxicabBoundary(true);
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

    }

    /** Extract tiles from a mesh.
     * @param mesh mesh from which tiles should be extracted
     * @param zone zone covered by the mesh
     * @return extracted tiles
     * @exception OrekitException if tile direction cannot be computed
     */
    private List<Tile> extractTiles(final Mesh mesh, final SphericalPolygonsSet zone)
        throws OrekitException {

        final List<Tile> tiles = new ArrayList<Tile>();

        final int minAcross = mesh.getMinAcrossIndex();
        final int maxAcross = mesh.getMaxAcrossIndex();
        for (int acrossIndex = firstIndex(minAcross, maxAcross); acrossIndex < maxAcross; acrossIndex += SPLITS) {
            final int minAlong = FastMath.min(mesh.getMinAlongIndex(acrossIndex),
                                              mesh.getMinAlongIndex(acrossIndex + SPLITS));
            final int maxAlong = FastMath.max(mesh.getMaxAlongIndex(acrossIndex),
                                              mesh.getMaxAlongIndex(acrossIndex + SPLITS));
            for (int alongIndex = firstIndex(minAlong, maxAlong); alongIndex < maxAlong; alongIndex += SPLITS) {

                // get the base vertex nodes
                final Mesh.Node node0 = mesh.addNode(alongIndex,          acrossIndex);
                final Mesh.Node node1 = mesh.addNode(alongIndex + SPLITS, acrossIndex);
                final Mesh.Node node2 = mesh.addNode(alongIndex + SPLITS, acrossIndex + SPLITS);
                final Mesh.Node node3 = mesh.addNode(alongIndex,          acrossIndex + SPLITS);

                // apply tile overlap
                final GeodeticPoint gp0 = node0.move(new Vector3D(-lengthOverlap, node0.getAlong(),
                                                                  -widthOverlap,  node0.getAcross()));
                final GeodeticPoint gp1 = node1.move(new Vector3D(+lengthOverlap, node1.getAlong(),
                                                                  -widthOverlap,  node1.getAcross()));
                final GeodeticPoint gp2 = node2.move(new Vector3D(+lengthOverlap, node2.getAlong(),
                                                                  +widthOverlap,  node2.getAcross()));
                final GeodeticPoint gp3 = node3.move(new Vector3D(-lengthOverlap, node2.getAlong(),
                                                                  +widthOverlap,  node2.getAcross()));

                // create a quadrilateral region corresponding to the candidate tile
                final SphericalPolygonsSet quadrilateral =
                        new SphericalPolygonsSet(zone.getTolerance(),
                                                 toS2Point(gp0), toS2Point(gp1), toS2Point(gp2), toS2Point(gp3));

                if (!new RegionFactory<Sphere2D>().intersection(zone, quadrilateral).isEmpty()) {

                    // the tile does cover part of the zone, it contributes to the tessellation
                    tiles.add(new Tile(gp0, gp1, gp2, gp3));

                    // ensure the taxicab boundary follows the built tile sides
                    for (int k = 0; k < SPLITS; ++k) {
                        mesh.addNode(alongIndex + k,      acrossIndex).setEnabled(true);
                        mesh.addNode(alongIndex + k + 1,  acrossIndex + SPLITS).setEnabled(true);
                        mesh.addNode(alongIndex,          acrossIndex + k + 1).setEnabled(true);
                        mesh.addNode(alongIndex + SPLITS, acrossIndex + k).setEnabled(true);
                    }

                }

            }
        }

        return tiles;

    }

    /** Merge two meshes together.
     * @param mesh1 first mesh
     * @param mesh2 second mesh
     * @param mergingSeeds collection were to put the nodes created during the merge
     * @return merged mesh (really one of the instances)
     * @exception OrekitException if tile direction cannot be computed
     */
    private Mesh mergeMeshes(final Mesh mesh1, final Mesh mesh2,
                             final Collection<Mesh.Node> mergingSeeds)
        throws OrekitException {

        // select the way merge will be performed
        final Mesh larger;
        final Mesh smaller;
        if (mesh1.getNumberOfNodes() >= mesh2.getNumberOfNodes()) {
            // the larger new mesh should absorb the smaller existing mesh
            larger  = mesh1;
            smaller = mesh2;
        } else {
            // the larger existing mesh should absorb the smaller new mesh
            larger  = mesh2;
            smaller = mesh1;
        }

        // prepare seed nodes for next iteration
        for (final Mesh.Node insideNode : smaller.getInsideNodes()) {

            // beware we cannot reuse the node itself as the two meshes are not aligned!
            // we have to create new nodes around the previous location
            Mesh.Node node = larger.getClosestExistingNode(insideNode.getV());

            while (estimateAlongMotion(node, insideNode.getV()) > +splitLength) {
                // the node is before desired index in the along direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex() + 1, node.getAcrossIndex());
            }

            while (estimateAlongMotion(node, insideNode.getV()) < -splitLength) {
                // the node is after desired index in the along direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex() - 1, node.getAcrossIndex());
            }

            while (estimateAcrossMotion(node, insideNode.getV()) > +splitWidth) {
                // the node is before desired index in the across direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex(), node.getAcrossIndex() + 1);
            }

            while (estimateAcrossMotion(node, insideNode.getV()) < -splitWidth) {
                // the node is after desired index in the across direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex(), node.getAcrossIndex() - 1);
            }

            // now we are close to the inside node,
            // make sure the four surrounding nodes are available
            final int otherAlong  = (estimateAlongMotion(node, insideNode.getV()) < 0.0) ?
                                    node.getAlongIndex()  - 1 : node.getAlongIndex() + 1;
            final int otherAcross = (estimateAcrossMotion(node, insideNode.getV()) < 0.0) ?
                                    node.getAcrossIndex()  - 1 : node.getAcrossIndex() + 1;
            addNode(node.getAlongIndex(), node.getAcrossIndex(), larger, mergingSeeds);
            addNode(node.getAlongIndex(), otherAcross,           larger, mergingSeeds);
            addNode(otherAlong,           node.getAcrossIndex(), larger, mergingSeeds);
            addNode(otherAlong,           otherAcross,           larger, mergingSeeds);

        }

        return larger;

    }

    /** Ensure all 8 neighbors of a node are in the mesh.
     * @param base base node
     * @param mesh complete mesh containing nodes
     * @param newNodes queue where new node must be put
     * @exception OrekitException if tile direction cannot be computed
     */
    private void addAllNeighborsIfNeeded(final Mesh.Node base, final Mesh mesh,
                                         final Collection<Mesh.Node> newNodes)
        throws OrekitException {
        addNode(base.getAlongIndex() - 1, base.getAcrossIndex() - 1, mesh, newNodes);
        addNode(base.getAlongIndex() - 1, base.getAcrossIndex(),     mesh, newNodes);
        addNode(base.getAlongIndex() - 1, base.getAcrossIndex() + 1, mesh, newNodes);
        addNode(base.getAlongIndex(),     base.getAcrossIndex() - 1, mesh, newNodes);
        addNode(base.getAlongIndex(),     base.getAcrossIndex() + 1, mesh, newNodes);
        addNode(base.getAlongIndex() + 1, base.getAcrossIndex() - 1, mesh, newNodes);
        addNode(base.getAlongIndex() + 1, base.getAcrossIndex(),     mesh, newNodes);
        addNode(base.getAlongIndex() + 1, base.getAcrossIndex() + 1, mesh, newNodes);
    }

    /** Add a node to a mesh if not already present.
     * @param alongIndex index in the along direction
     * @param acrossIndex index in the across direction
     * @param mesh complete mesh containing nodes
     * @param newNodes queue where new node must be put
     * @return neighbor node (which was either already present, or is created)
     * @exception OrekitException if tile direction cannot be computed
     */
    private Mesh.Node addNode(final int alongIndex, final int acrossIndex,
                              final Mesh mesh, final Collection<Mesh.Node> newNodes)
        throws OrekitException {

        final Mesh.Node node = mesh.addNode(alongIndex, acrossIndex);

        if (!node.isEnabled()) {
            // enable the node
            node.setEnabled(true);
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

    /** Estimate an approximate motion in the along direction.
     * @param start node at start of motion
     * @param end desired point at end of motion
     * @return approximate motion in the along direction
     */
    private double estimateAlongMotion(final Mesh.Node start, final Vector3D end) {
        return Vector3D.dotProduct(start.getAlong(), end.subtract(start.getV()));
    }

    /** Estimate an approximate motion in the across direction.
     * @param start node at start of motion
     * @param end desired point at end of motion
     * @return approximate motion in the across direction
     */
    private double estimateAcrossMotion(final Mesh.Node start, final Vector3D end) {
        return Vector3D.dotProduct(start.getAcross(), end.subtract(start.getV()));
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
