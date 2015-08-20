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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.orekit.errors.OrekitInternalError;

/** Class used to tessellate an interest zone on an ellipsoid in either
 * {@link Tile tiles} or grids of {@link GeodeticPoint geodetic points}.
 * <p>
 * This class is typically used for Earth Observation missions, in order to
 * create tiles or grids that may be used as the basis of visibility event
 * detectors. Tiles are used when surface-related elements are needed, the
 * tiles created completely cover the zone of interest. Grids are used when
 * point-related elements are needed, the points created lie entirely within
 * the zone of interest.
 * </p>
 * <p>
 * One should note that as tessellation essentially creates a 2 dimensional
 * almost Cartesian map, it can never perfectly fulfill geometrical dimensions
 * because neither sphere nor ellipsoid are developable surfaces. This implies
 * that the tesselation will always be distorted, and distortion increases as
 * the size of the zone to be tessellated increases.
 * </p>
 * @author Luc Maisonobe
 */
public class EllipsoidTessellator {

    /** Number of segments tiles sides are split into for tiles fine positioning. */
    private final int quantization;

    /** Aiming used for orienting tiles. */
    private final TileAiming aiming;

    /** Underlying ellipsoid. */
    private final OneAxisEllipsoid ellipsoid;

    /** Simple constructor.
     * <p>
     * The {@code quantization} parameter is used internally to adjust points positioning.
     * For example when quantization is set to 4, a complete tile that has 4 corner points
     * separated by the tile lengths will really be computed on a grid containing 25 points
     * (5 rows of 5 points, as each side will be split in 4 segments, hence will have 5
     * points). This quantization allows rough adjustment to balance margins around the
     * zone of interest and improves geometric accuracy as the along and across directions
     * are readjusted at each points.
     * </p>
     * <p>
     * It is recommended to use at least 2 as the quantization parameter for tiling. The
     * rationale is that using only 1 for quantization would imply all points used are tiles
     * vertices, and hence would lead small zones to generate 4 tiles with a shared vertex
     * inside the zone and the 4 tiles covering the four quadrants at North-West, North-East,
     * South-East and South-West. A quantization value of at least 2 allows to shift the
     * tiles so the center point is an inside point rather than a tile vertex, hence allowing
     * a single tile to cover the small zone. A value even greater like 4 or 8 would allow even
     * finer positioning to balance the tiles margins around the zone.
     * </p>
     * @param ellipsoid underlying ellipsoid
     * @param aiming aiming used for orienting tiles
     * @param quantization number of segments tiles sides are split into for tiles fine positioning
     */
    public EllipsoidTessellator(final OneAxisEllipsoid ellipsoid, final TileAiming aiming,
                                final int quantization) {
        this.ellipsoid    = ellipsoid;
        this.aiming       = aiming;
        this.quantization = quantization;
    }

    /** Tessellate a zone of interest into tiles.
     * <p>
     * The created tiles will completely cover the zone of interest.
     * </p>
     * <p>
     * The distance between a vertex at a tile corner and the vertex at the same corner
     * in the next vertex are computed by subtracting the overlap width (resp. overlap length)
     * from the full width (resp. full length). If for example the full width is specified to
     * be 55 km and the overlap in width is specified to be +5 km, successive tiles would span
     * as follows:
     * </p>
     * <ul>
     *   <li>tile 1 covering from   0 km to  55 km</li>
     *   <li>tile 2 covering from  50 km to 105 km</li>
     *   <li>tile 3 covering from 100 km to 155 km</li>
     *   <li>...</li>
     * </ul>
     * <p>
     * In order to achieve the same 50 km step but using a 5 km gap instead of an overlap, one would
     * need to specify the full width to be 45 km and the overlap to be -5 km. With these settings,
     * successive tiles would span as follows:
     * </p>
     * <ul>
     *   <li>tile 1 covering from   0 km to  45 km</li>
     *   <li>tile 2 covering from  50 km to  95 km</li>
     *   <li>tile 3 covering from 100 km to 155 km</li>
     *   <li>...</li>
     * </ul>
     * @param zone zone of interest to tessellate
     * @param fullWidth full tiles width as a distance on surface, including overlap (in meters)
     * @param fullLength full tiles length as a distance on surface, including overlap (in meters)
     * @param widthOverlap overlap between adjacent tiles (in meters), if negative the tiles
     * will have a gap between each other instead of an overlap
     * @param lengthOverlap overlap between adjacent tiles (in meters), if negative the tiles
     * will have a gap between each other instead of an overlap
     * @param truncateLastWidth if true, the first tiles strip will be started as close as
     * possible to the zone of interest, and the last tiles strip will have its width reduced
     * to also remain close to the zone of interest; if false all tiles strip will have the
     * same {@code fullWidth} and they will be balanced around zone of interest
     * @param truncateLastLength if true, the first tile in each strip will be started as close as
     * possible to the zone of interest, and the last tile in each strip will have its length reduced
     * to also remain close to the zone of interest; if false all tiles in each strip will have the
     * same {@code fullLength} and they will be balanced around zone of interest
     * @return a list of lists of tiles covering the zone of interest,
     * each sub-list corresponding to a part not connected to the other
     * parts (for example for islands)
     * @exception OrekitException if the zone cannot be tessellated
     */
    public List<List<Tile>> tessellate(final SphericalPolygonsSet zone,
                                       final double fullWidth, final double fullLength,
                                       final double widthOverlap, final double lengthOverlap,
                                       final boolean truncateLastWidth, final boolean truncateLastLength)
        throws OrekitException {

        final double                  splitWidth  = (fullWidth  - widthOverlap)  / quantization;
        final double                  splitLength = (fullLength - lengthOverlap) / quantization;
        final Map<Mesh, List<Tile>>   map         = new IdentityHashMap<Mesh, List<Tile>>();
        final RegionFactory<Sphere2D> factory     = new RegionFactory<Sphere2D>();
        SphericalPolygonsSet          remaining   = (SphericalPolygonsSet) zone.copySelf();
        S2Point                       inside      = getInsidePoint(remaining);

        while (inside != null) {

            // find a mesh covering at least one connected part of the zone
            final List<Mesh.Node> mergingSeeds = new ArrayList<Mesh.Node>();
            Mesh mesh = new Mesh(ellipsoid, zone, aiming, splitLength, splitWidth, inside);
            mergingSeeds.add(mesh.getNode(0, 0));
            List<Tile> tiles = null;
            while (!mergingSeeds.isEmpty()) {

                // expand the mesh around the seed
                neighborExpandMesh(mesh, mergingSeeds, zone);

                // extract the tiles from the mesh
                // this further expands the mesh so tiles dimensions are multiples of quantization,
                // hence it must be performed here before checking meshes independence
                tiles = extractTiles(mesh, zone, lengthOverlap, widthOverlap, truncateLastWidth, truncateLastLength);

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
            inside    = getInsidePoint(remaining);

            map.put(mesh, tiles);

        }

        // concatenate the lists from the independent meshes
        final List<List<Tile>> tilesLists = new ArrayList<List<Tile>>(map.size());
        for (final Map.Entry<Mesh, List<Tile>> entry : map.entrySet()) {
            tilesLists.add(entry.getValue());
        }

        return tilesLists;

    }

    /** Sample a zone of interest into a grid sample of {@link GeodeticPoint geodetic points}.
     * <p>
     * The created points will be entirely within the zone of interest.
     * </p>
     * @param zone zone of interest to sample
     * @param width grid sample cells width as a distance on surface (in meters)
     * @param length grid sample cells length as a distance on surface (in meters)
     * @return a list of lists of points sampling the zone of interest,
     * each sub-list corresponding to a part not connected to the other
     * parts (for example for islands)
     * @exception OrekitException if the zone cannot be sampled
     */
    public List<List<GeodeticPoint>> sample(final SphericalPolygonsSet zone,
                                            final double width, final double length)
        throws OrekitException {

        final double                         splitWidth  = width  / quantization;
        final double                         splitLength = length / quantization;
        final Map<Mesh, List<GeodeticPoint>> map         = new IdentityHashMap<Mesh, List<GeodeticPoint>>();
        final RegionFactory<Sphere2D>        factory     = new RegionFactory<Sphere2D>();
        SphericalPolygonsSet                 remaining   = (SphericalPolygonsSet) zone.copySelf();
        S2Point                              inside      = getInsidePoint(remaining);

        while (inside != null) {

            // find a mesh covering at least one connected part of the zone
            final List<Mesh.Node> mergingSeeds = new ArrayList<Mesh.Node>();
            Mesh mesh = new Mesh(ellipsoid, zone, aiming, splitLength, splitWidth, inside);
            mergingSeeds.add(mesh.getNode(0, 0));
            List<GeodeticPoint> sample = null;
            while (!mergingSeeds.isEmpty()) {

                // expand the mesh around the seed
                neighborExpandMesh(mesh, mergingSeeds, zone);

                // extract the sample from the mesh
                // this further expands the mesh so sample cells dimensions are multiples of quantization,
                // hence it must be performed here before checking meshes independence
                sample = extractSample(mesh, zone);

                // check the mesh is independent from existing meshes
                mergingSeeds.clear();
                for (final Map.Entry<Mesh, List<GeodeticPoint>> entry : map.entrySet()) {
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
            inside    = getInsidePoint(remaining);

            map.put(mesh, sample);

        }

        // concatenate the lists from the independent meshes
        final List<List<GeodeticPoint>> sampleLists = new ArrayList<List<GeodeticPoint>>(map.size());
        for (final Map.Entry<Mesh, List<GeodeticPoint>> entry : map.entrySet()) {
            sampleLists.add(entry.getValue());
        }

        return sampleLists;

    }

    /** Get an inside point from a zone of interest.
     * @param zone zone to mesh
     * @return a point inside the zone or null if zone is empty or too thin
     * @exception OrekitException if tile direction cannot be computed
     */
    private S2Point getInsidePoint(final SphericalPolygonsSet zone)
        throws OrekitException {

        final InsideFinder finder = new InsideFinder(zone);
        zone.getTree(false).visit(finder);
        return finder.getInsidePoint();

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
            final List<Mesh.Node> boundary = mesh.getTaxicabBoundary(false);
            if (boundary.size() > 1) {
                Mesh.Node previous = boundary.get(boundary.size() - 1);
                for (final Mesh.Node node : boundary) {
                    if (meetInside(previous.getS2P(), node.getS2P(), zone)) {
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
     * @param lengthOverlap overlap between adjacent tiles
     * @param widthOverlap overlap between adjacent tiles
     * @param truncateLastWidth true if we can reduce last tile width
     * @param truncateLastLength true if we can reduce last tile length
     * @return extracted tiles
     * @exception OrekitException if tile direction cannot be computed
     */
    private List<Tile> extractTiles(final Mesh mesh, final SphericalPolygonsSet zone,
                                    final double lengthOverlap, final double widthOverlap,
                                    final boolean truncateLastWidth, final boolean truncateLastLength)
        throws OrekitException {

        final List<Tile>      tiles = new ArrayList<Tile>();
        final List<RangePair> rangePairs = new ArrayList<RangePair>();

        final int minAcross = mesh.getMinAcrossIndex();
        final int maxAcross = mesh.getMaxAcrossIndex();
        for (Range acrossPair : nodesIndices(minAcross, maxAcross, truncateLastWidth)) {

            int minAlong = mesh.getMaxAlongIndex() + 1;
            int maxAlong = mesh.getMinAlongIndex() - 1;
            for (int c = acrossPair.lower; c <= acrossPair.upper; ++c) {
                minAlong = FastMath.min(minAlong, mesh.getMinAlongIndex(c));
                maxAlong = FastMath.max(maxAlong, mesh.getMaxAlongIndex(c));
            }

            for (Range alongPair : nodesIndices(minAlong, maxAlong, truncateLastLength)) {

                // get the base vertex nodes
                final Mesh.Node node0 = mesh.addNode(alongPair.lower, acrossPair.lower);
                final Mesh.Node node1 = mesh.addNode(alongPair.upper, acrossPair.lower);
                final Mesh.Node node2 = mesh.addNode(alongPair.upper, acrossPair.upper);
                final Mesh.Node node3 = mesh.addNode(alongPair.lower, acrossPair.upper);

                // apply tile overlap
                final S2Point s2p0 = node0.move(new Vector3D(-0.5 * lengthOverlap, node0.getAlong(),
                                                             -0.5 * widthOverlap,  node0.getAcross()));
                final S2Point s2p1 = node1.move(new Vector3D(+0.5 * lengthOverlap, node1.getAlong(),
                                                             -0.5 * widthOverlap,  node1.getAcross()));
                final S2Point s2p2 = node2.move(new Vector3D(+0.5 * lengthOverlap, node2.getAlong(),
                                                             +0.5 * widthOverlap,  node2.getAcross()));
                final S2Point s2p3 = node3.move(new Vector3D(-0.5 * lengthOverlap, node2.getAlong(),
                                                             +0.5 * widthOverlap,  node2.getAcross()));

                // create a quadrilateral region corresponding to the candidate tile
                final SphericalPolygonsSet quadrilateral =
                        new SphericalPolygonsSet(zone.getTolerance(), s2p0, s2p1, s2p2, s2p3);

                if (!new RegionFactory<Sphere2D>().intersection(zone.copySelf(), quadrilateral).isEmpty()) {

                    // the tile does cover part of the zone, it contributes to the tessellation
                    tiles.add(new Tile(toGeodetic(s2p0), toGeodetic(s2p1), toGeodetic(s2p2), toGeodetic(s2p3)));
                    rangePairs.add(new RangePair(acrossPair, alongPair));

                }

            }
        }

        // ensure the taxicab boundary follows the built tile sides
        // this is done outside of the previous loop because in order
        // to avoid one tile changing the min/max indices of the
        // neighboring tile as they share some nodes that will be enabled here
        for (final RangePair rangePair : rangePairs) {
            for (int c = rangePair.across.lower; c < rangePair.across.upper; ++c) {
                mesh.addNode(rangePair.along.lower, c + 1).setEnabled();
                mesh.addNode(rangePair.along.upper, c).setEnabled();
            }
            for (int l = rangePair.along.lower; l < rangePair.along.upper; ++l) {
                mesh.addNode(l,     rangePair.across.lower).setEnabled();
                mesh.addNode(l + 1, rangePair.across.upper).setEnabled();
            }
        }

        return tiles;

    }

    /** Extract a sample of points from a mesh.
     * @param mesh mesh from which grid should be extracted
     * @param zone zone covered by the mesh
     * @return extracted grid
     * @exception OrekitException if tile direction cannot be computed
     */
    private List<GeodeticPoint> extractSample(final Mesh mesh, final SphericalPolygonsSet zone)
        throws OrekitException {

        // find how to select sample points taking quantization into account
        // to have the largest possible number of points while still
        // being inside the zone of interest
        int selectedAcrossModulus = -1;
        int selectedAlongModulus  = -1;
        int selectedCount         = -1;
        for (int acrossModulus = 0; acrossModulus < quantization; ++acrossModulus) {
            for (int alongModulus = 0; alongModulus < quantization; ++alongModulus) {

                // count how many points would be selected for the current modulus
                int count = 0;
                for (int across = mesh.getMinAcrossIndex() + acrossModulus;
                        across <= mesh.getMaxAcrossIndex();
                        across += quantization) {
                    for (int along = mesh.getMinAlongIndex() + alongModulus;
                            along <= mesh.getMaxAlongIndex();
                            along += quantization) {
                        final Mesh.Node  node = mesh.getNode(along, across);
                        if (node != null && node.isInside()) {
                            ++count;
                        }
                    }
                }

                if (count > selectedCount) {
                    // current modulus are better than the selected ones
                    selectedAcrossModulus = acrossModulus;
                    selectedAlongModulus  = alongModulus;
                    selectedCount         = count;
                }
            }
        }

        // extract the sample points
        final List<GeodeticPoint> sample = new ArrayList<GeodeticPoint>(selectedCount);
        for (int across = mesh.getMinAcrossIndex() + selectedAcrossModulus;
                across <= mesh.getMaxAcrossIndex();
                across += quantization) {
            for (int along = mesh.getMinAlongIndex() + selectedAlongModulus;
                    along <= mesh.getMaxAlongIndex();
                    along += quantization) {
                final Mesh.Node  node = mesh.getNode(along, across);
                if (node != null && node.isInside()) {
                    sample.add(toGeodetic(node.getS2P()));
                }
            }
        }

        return sample;

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

            while (estimateAlongMotion(node, insideNode.getV()) > +mesh1.getAlongGap()) {
                // the node is before desired index in the along direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex() + 1, node.getAcrossIndex());
            }

            while (estimateAlongMotion(node, insideNode.getV()) < -mesh1.getAlongGap()) {
                // the node is after desired index in the along direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex() - 1, node.getAcrossIndex());
            }

            while (estimateAcrossMotion(node, insideNode.getV()) > +mesh1.getAcrossGap()) {
                // the node is before desired index in the across direction
                // we need to create intermediates nodes up to the desired index
                node = larger.addNode(node.getAlongIndex(), node.getAcrossIndex() + 1);
            }

            while (estimateAcrossMotion(node, insideNode.getV()) < -mesh1.getAcrossGap()) {
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
     * @exception OrekitException if tile direction cannot be computed
     */
    private void addNode(final int alongIndex, final int acrossIndex,
                         final Mesh mesh, final Collection<Mesh.Node> newNodes)
        throws OrekitException {

        final Mesh.Node node = mesh.addNode(alongIndex, acrossIndex);

        if (!node.isEnabled()) {
            // enable the node
            node.setEnabled();
            newNodes.add(node);
        }

    }

    /** Convert a point on the unit 2-sphere to geodetic coordinates.
     * @param point point on the unit 2-sphere
     * @return geodetic point (arbitrarily set at altitude 0)
     */
    protected GeodeticPoint toGeodetic(final S2Point point) {
        return new GeodeticPoint(0.5 * FastMath.PI - point.getPhi(), point.getTheta(), 0.0);
    }

    /** Build a simple zone (connected zone without holes).
     * <p>
     * In order to build more complex zones (not connected or with
     * holes), the user should directly call Apache Commons
     * Math {@link SphericalPolygonsSet} constructors and
     * {@link RegionFactory region factory} if set operations
     * are needed (union, intersection, difference ...).
     * </p>
     * <p>
     * Take care that the vertices boundary points must be given <em>counterclockwise</em>.
     * Using the wrong order defines the complementary of the real zone,
     * and will often result in tessellation failure as the zone is too
     * wide.
     * </p>
     * @param tolerance angular separation below which points are considered
     * equal (typically 1.0e-10)
     * @param points vertices of the boundary, in <em>counterclockwise</em>
     * order, each point being a two-elements arrays with latitude at index 0
     * and longitude at index 1
     * @return a zone defined on the unit 2-sphere
     */
    public static SphericalPolygonsSet buildSimpleZone(final double tolerance,
                                                       final double[] ... points) {
        final S2Point[] vertices = new S2Point[points.length];
        for (int i = 0; i < points.length; ++i) {
            vertices[i] = new S2Point(points[i][1], 0.5 * FastMath.PI - points[i][0]);
        }
        return new SphericalPolygonsSet(tolerance, vertices);
    }

    /** Build a simple zone (connected zone without holes).
     * <p>
     * In order to build more complex zones (not connected or with
     * holes), the user should directly call Apache Commons
     * Math {@link SphericalPolygonsSet} constructors and
     * {@link RegionFactory region factory} if set operations
     * are needed (union, intersection, difference ...).
     * </p>
     * <p>
     * Take care that the vertices boundary points must be given <em>counterclockwise</em>.
     * Using the wrong order defines the complementary of the real zone,
     * and will often result in tessellation failure as the zone is too
     * wide.
     * </p>
     * @param tolerance angular separation below which points are considered
     * equal (typically 1.0e-10)
     * @param points vertices of the boundary, in <em>counterclockwise</em>
     * order
     * @return a zone defined on the unit 2-sphere
     */
    public static SphericalPolygonsSet buildSimpleZone(final double tolerance,
                                                       final GeodeticPoint ... points) {
        final S2Point[] vertices = new S2Point[points.length];
        for (int i = 0; i < points.length; ++i) {
            vertices[i] = new S2Point(points[i].getLongitude(),
                                      0.5 * FastMath.PI - points[i].getLatitude());
        }
        return new SphericalPolygonsSet(tolerance, vertices);
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
                    throw new OrekitInternalError(null);
            }
        }
    }

    /** Get an iterator over mesh nodes indices.
     * @param minIndex minimum node index
     * @param maxIndex maximum node index
     * @param truncateLast true if we can reduce last tile
     * @return iterator over mesh nodes indices
     */
    private Iterable<Range> nodesIndices(final int minIndex, final int maxIndex, final boolean truncateLast) {

        final int first;
        if (truncateLast) {

            // truncate last tile rather than balance tiles around the zone of interest
            first = minIndex;

        } else {

            // balance tiles around the zone of interest rather than truncate last tile

            // number of tiles needed to cover the full indices range
            final int range      = maxIndex - minIndex;
            final int nbTiles    = (range + quantization - 1) / quantization;

            // extra nodes that must be added to complete the tiles
            final int extraNodes = nbTiles * quantization  - range;

            // balance the extra nodes before min index and after maxIndex
            final int extraBefore = (extraNodes + 1) / 2;

            first = minIndex - extraBefore;

        }

        return new Iterable<Range>() {

            /** {@inheritDoc} */
            @Override
            public Iterator<Range> iterator() {
                return new Iterator<Range>() {

                    private int nextLower = first;

                    /** {@inheritDoc} */
                    @Override
                    public boolean hasNext() {
                        return nextLower < maxIndex;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public Range next() {

                        if (nextLower >= maxIndex) {
                            throw new NoSuchElementException();
                        }
                        final int lower = nextLower;

                        nextLower += quantization;
                        if (truncateLast && nextLower > maxIndex && lower < maxIndex) {
                            // truncate last tile
                            nextLower = maxIndex;
                        }

                        return new Range(lower, nextLower);

                    }

                    /** {@inheritDoc} */
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }
        };

    }

    /** Local class for a range of indices to be used for building a tile. */
    private static class Range {

        /** Lower index. */
        private final int lower;

        /** Upper index. */
        private final int upper;

        /** Simple constructor.
         * @param lower lower index
         * @param upper upper index
         */
        Range(final int lower, final int upper) {
            this.lower = lower;
            this.upper = upper;
        }

    }

    /** Local class for a pair of ranges of indices to be used for building a tile. */
    private static class RangePair {

        /** Across range. */
        private final Range across;

        /** Along range. */
        private final Range along;

        /** Simple constructor.
         * @param across across range
         * @param along along range
         */
        RangePair(final Range across, final Range along) {
            this.across = across;
            this.along  = along;
        }

    }

}
