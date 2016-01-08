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
package org.orekit.propagation.semianalytical.dsst.utilities;

/** Interpolation grid where a fixed number of points are
 * evenly spaced between the start and the end of the integration step.
 * <p>
 * The grid is adapted to the step considered,
 * meaning that for short steps, the grid will be dense,
 * while for long steps the points will be far away one from each other
 * </p>
 *
 * @author Nicolas Bernard
 */
public class FixedNumberInterpolationGrid implements InterpolationGrid {

    /** Number of points in the grid per step. */
    private final int pointsPerStep;

    /** Constructor.
     * @param pointsPerStep number of points in the grid per step
     */
    public FixedNumberInterpolationGrid(final int pointsPerStep) {
        this.pointsPerStep = pointsPerStep;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getGridPoints(final double stepStart, final double stepEnd) {
        final double[] grid = new double[pointsPerStep];

        final double stepSize = (stepEnd - stepStart) / (pointsPerStep - 1);
        for (int i = 0; i < pointsPerStep; i++) {
            grid[i] = stepSize * i + stepStart;
        }

        return grid;
    }
}
