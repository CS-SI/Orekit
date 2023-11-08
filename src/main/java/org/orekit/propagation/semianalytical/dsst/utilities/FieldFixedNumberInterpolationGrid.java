/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;

/** Interpolation grid where a fixed number of points are
 * evenly spaced between the start and the end of the integration step.
 * <p>
 * The grid is adapted to the step considered,
 * meaning that for short steps, the grid will be dense,
 * while for long steps the points will be far away one from each other
 * </p>
 * @param <T> type of the field elements
 */
public class FieldFixedNumberInterpolationGrid<T extends CalculusFieldElement<T>> implements FieldInterpolationGrid<T> {

    /** Number of points in the grid per step. */
    private final int pointsPerStep;

    /** Field used by default. */
    private final Field<T> field;

    /** Constructor.
     * @param field field used by default
     * @param pointsPerStep number of points in the grid per step
     */
    public FieldFixedNumberInterpolationGrid(final Field<T> field, final int pointsPerStep) {
        this.field = field;
        this.pointsPerStep = pointsPerStep;
    }

    /** {@inheritDoc} */
    @Override
    public T[] getGridPoints(final T stepStart, final T stepEnd) {
        final T[] grid = MathArrays.buildArray(field, pointsPerStep);

        final T stepSize = (stepEnd.subtract(stepStart)).divide(pointsPerStep - 1);
        for (int i = 0; i < pointsPerStep; i++) {
            grid[i] = stepSize.multiply(i).add(stepStart);
        }

        return grid;
    }

}
