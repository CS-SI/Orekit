/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.propagation.SpacecraftState;


/** Scheduling predicate taking care of two satellites seeing each other.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class InterSatVisibility implements SchedulingPredicate {

    /** Earth body. */
    private final OneAxisEllipsoid earth;

    /** Index of satellite 1 propagator. */
    private final int satellite1Index;

    /** Index of satellite 2 propagator. */
    private final int satellite2Index;

    /** Simple constructor.
     * @param earth Earth body
     * @param satellite1Index index of satellite 1 propagator
     * @param satellite2Index index of satellite 2 propagator
     */
    public InterSatVisibility(final OneAxisEllipsoid earth,
                              final int satellite1Index, final int satellite2Index) {
        this.earth           = earth;
        this.satellite1Index = satellite1Index;
        this.satellite2Index = satellite2Index;
    }

    /** {@inheritDoc} */
    @Override
    public boolean feasibleMeasurement(final SpacecraftState... states) {

        final SpacecraftState state1 = states[satellite1Index];
        final SpacecraftState state2 = states[satellite2Index];
        final Vector3D        p1     = state1.getPVCoordinates().getPosition();
        final Vector3D        p2     = state2.getPVCoordinates().getPosition();
        final Line            line   = new Line(p1, p2, 1.0);

        final Vector3D pI = earth.getCartesianIntersectionPoint(line, p1, state1.getFrame(), state1.getDate());
        if (pI == null) {
            // the line containing both satellites does not even encounter Earth
            // inter-satellites visibility is certain
            return true;
        }

        // inter-satellites visibility is blocked only if pI is between p1 and p2, so if both
        // p1 and p2 as seen from pI are in the same direction, the satellites see each other
        return Vector3D.dotProduct(p1.subtract(pI), p2.subtract(pI)) > 0.0;

    }

}
