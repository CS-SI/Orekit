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

import org.hipparchus.util.FastMath;

/** Interpolation grid where points obey a maximum time gap.
 * <p>
 * The grid is adapted to the step considered,
 * meaning that for short steps, the grid will have numerous points.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 7.1
 */
public class MaxGapInterpolationGrid implements InterpolationGrid {

    /** Maximum time gap. */
    private final double maxGap;

    /** Constructor.
     * @param maxGap maximum time gap between interpolation points
     */
    public MaxGapInterpolationGrid(final double maxGap) {
        this.maxGap = maxGap;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getGridPoints(final double stepStart, final double stepEnd) {
        final int pointsPerStep = FastMath.max(2, (int) FastMath.ceil(FastMath.abs(stepEnd - stepStart) / maxGap));
        final double[] grid = new double[pointsPerStep];

        final double stepSize = (stepEnd - stepStart) / (pointsPerStep - 1);
        for (int i = 0; i < pointsPerStep; i++) {
            grid[i] = stepSize * i + stepStart;
        }

        return grid;
    }
}
