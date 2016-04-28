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

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.BSPTree;
import org.hipparchus.geometry.partitioning.BSPTreeVisitor;
import org.hipparchus.geometry.partitioning.Region.Location;
import org.hipparchus.geometry.spherical.twod.Edge;
import org.hipparchus.geometry.spherical.twod.S2Point;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;
import org.hipparchus.geometry.spherical.twod.Vertex;

/** BSP Tree visitor aimed at finding a point strictly inside a spherical zone.
 * <p>
 * This class is heavily based on the class PropertiesComputer from the
 * Hipparchus library, also distributed under the terms of
 * the Apache Software License V2.
 * </p>
 * @author Luc Maisonobe
 */
class InsideFinder implements BSPTreeVisitor<Sphere2D> {

    /** Zone of interest. */
    private final SphericalPolygonsSet zone;

    /** Inside point. */
    private S2Point insidePointSecondChoice;

    /** Inside point. */
    private S2Point insidePointFirstChoice;

    /** Simple constructor.
     * @param zone zone of interest
     */
    InsideFinder(final SphericalPolygonsSet zone) {
        this.zone                    = zone;
        this.insidePointFirstChoice  = null;
        this.insidePointSecondChoice = null;
    }

    /** {@inheritDoc} */
    @Override
    public Order visitOrder(final BSPTree<Sphere2D> node) {
        return Order.MINUS_PLUS_SUB;
    }

    /** {@inheritDoc} */
    @Override
    public void visitInternalNode(final BSPTree<Sphere2D> node) {
    }

    /** {@inheritDoc} */
    @Override
    public void visitLeafNode(final BSPTree<Sphere2D> node) {

        // we have already found a good point
        if (insidePointFirstChoice != null) {
            return;
        }

        if ((Boolean) node.getAttribute()) {

            // transform this inside leaf cell into a simple convex polygon
            final SphericalPolygonsSet convex =
                    new SphericalPolygonsSet(node.pruneAroundConvexCell(Boolean.TRUE,
                                                                        Boolean.FALSE,
                                                                        null),
                                                                        zone.getTolerance());

            // extract the start of the single loop boundary of the convex cell
            final List<Vertex> boundary = convex.getBoundaryLoops();
            final Vertex start = boundary.get(0);
            int n = 0;
            Vector3D sumB = Vector3D.ZERO;
            for (Edge e = start.getOutgoing(); n == 0 || e.getStart() != start; e = e.getEnd().getOutgoing()) {
                sumB = new Vector3D(1, sumB, e.getLength(), e.getCircle().getPole());
                n++;
            }

            final S2Point candidate = new S2Point(sumB);

            // check the candidate point is really considered inside
            // it may appear outside if the current leaf cell is very thin
            // and checkPoint selects another (very close) tree leaf node
            if (zone.checkPoint(candidate) == Location.INSIDE) {
                insidePointFirstChoice = candidate;
            } else {
                insidePointSecondChoice = candidate;
            }

        }

    }

    /** Get the inside point.
     * @return inside point, or null if the region is empty
     */
    public S2Point getInsidePoint() {
        return insidePointFirstChoice != null ? insidePointFirstChoice : insidePointSecondChoice;
    }

}
