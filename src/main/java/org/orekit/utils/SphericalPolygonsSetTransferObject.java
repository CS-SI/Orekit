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
package org.orekit.utils;

import java.io.Serializable;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.partitioning.BSPTree;
import org.hipparchus.geometry.partitioning.BSPTreeVisitor;
import org.hipparchus.geometry.spherical.twod.Circle;
import org.hipparchus.geometry.spherical.twod.Sphere2D;
import org.hipparchus.geometry.spherical.twod.SphericalPolygonsSet;

/** Transfer object for serializing {@link SphericalPolygonsSet} instances.
 * <p>
 * This object is intended to be used when {@link SphericalPolygonsSet} instances
 * needs to be serialized. Instead of serializing the zone, an instance of
 * this class is created from the zone and serialized. Then upon de-serialization,
 * the zone can be rebuilt, typically from a {@code readResolve} method.
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class SphericalPolygonsSetTransferObject implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20150112L;

    /** Tolerance for the zone. */
    private final double tolerance;

    /** Zone cut hyperplanes. */
    private final double[] cuts;

    /** Leaf nodes indices. */
    private final int[] leafs;

    /** Zone interior/exterior indicators. */
    private final boolean[] flags;

    /** Internal nodes counter. */
    private transient int internalNodes;

    /** Leaf nodes counter. */
    private transient int leafNodes;

    /** Node index. */
    private transient int nodeIndex;

    /** Index in cut hyperplanes array. */
    private transient int cutIndex;

    /** Index in interior/exterior flags array. */
    private transient int flagIndex;

    /** Simple constructor.
     * @param zone zone to serialize
     */
    public SphericalPolygonsSetTransferObject(final SphericalPolygonsSet zone) {

        this.tolerance = zone.getTolerance();

        // count the nodes
        internalNodes = 0;
        leafNodes     = 0;
        zone.getTree(false).visit(new BSPTreeVisitor<Sphere2D>() {

            /** {@inheritDoc} */
            public Order visitOrder(final BSPTree<Sphere2D> node) {
                return Order.SUB_MINUS_PLUS;
            }

            /** {@inheritDoc} */
            public void visitInternalNode(final BSPTree<Sphere2D> node) {
                ++internalNodes;
            }

            /** {@inheritDoc} */
            public void visitLeafNode(final BSPTree<Sphere2D> node) {
                ++leafNodes;
            }

        });

        // allocate the arrays for flattened tree
        cuts      = new double[3 * internalNodes];
        leafs     = new int[leafNodes];
        flags     = new boolean[leafNodes];
        nodeIndex = 0;
        cutIndex  = 0;
        flagIndex = 0;
        zone.getTree(false).visit(new BSPTreeVisitor<Sphere2D>() {

            /** {@inheritDoc} */
            public Order visitOrder(final BSPTree<Sphere2D> node) {
                return Order.SUB_MINUS_PLUS;
            }

            /** {@inheritDoc} */
            public void visitInternalNode(final BSPTree<Sphere2D> node) {
                final Vector3D cutPole = ((Circle) node.getCut().getHyperplane()).getPole();
                cuts[cutIndex++] = cutPole.getX();
                cuts[cutIndex++] = cutPole.getY();
                cuts[cutIndex++] = cutPole.getZ();
                nodeIndex++;
            }

            /** {@inheritDoc} */
            public void visitLeafNode(final BSPTree<Sphere2D> node) {
                leafs[flagIndex]   = nodeIndex++;
                flags[flagIndex++] = (Boolean) node.getAttribute();
            }

        });

    }

    /** Rebuild the zone from saved data.
     * @return rebuilt zone
     */
    public SphericalPolygonsSet rebuildZone() {

        // rebuild the tree from the flattened arrays
        BSPTree<Sphere2D> node = new BSPTree<Sphere2D>();
        final int nbNodes = cuts.length / 3 + leafs.length;
        cutIndex  = 0;
        flagIndex = 0;
        for (nodeIndex = 0; nodeIndex < nbNodes; ++nodeIndex) {
            if (leafs[flagIndex] == nodeIndex) {
                // this is a leaf node
                node.setAttribute(Boolean.valueOf(flags[flagIndex++]));
                while (node.getParent() != null) {
                    final BSPTree<Sphere2D> parent = node.getParent();
                    if (node == parent.getMinus()) {
                        node = parent.getPlus();
                        break;
                    } else {
                        node = parent;
                    }
                }
            } else {
                // this is an internal node
                final double x = cuts[cutIndex++];
                final double y = cuts[cutIndex++];
                final double z = cuts[cutIndex++];
                node.insertCut(new Circle(new Vector3D(x, y, z), tolerance));
                node = node.getMinus();
            }
        }

        return new SphericalPolygonsSet(node, tolerance);

    }

}
