/* Copyright 2002-2026 CS GROUP
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

import java.util.stream.IntStream;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.MathArrays;

/**
 * Base class for DSST interpolation grid methods.
 * @author Luc Maisonobe
 * @author Nicolas Bernard
 * @author Bryan Cazabonne
 * @since 14.0
 */
abstract class AbstractInterpolationGrid implements InterpolationGrid {

    /** Constructor. */
    protected AbstractInterpolationGrid() {
        // nothing
    }

    /** {@inheritDoc} */
    @Override
    public double[] getGridPoints(final double stepStart, final double stepEnd) {
        final int pointsPerStep = getPointsPerStep(stepStart, stepEnd);
        final double stepSize = (stepEnd - stepStart) / (pointsPerStep - 1);
        return IntStream.range(0, pointsPerStep).mapToDouble(i -> stepSize * i + stepStart).toArray();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getGridPoints(final T stepStart, final T stepEnd) {
        final int pointsPerStep = getPointsPerStep(stepStart, stepEnd);
        final T[] grid = MathArrays.buildArray(stepStart.getField(), pointsPerStep);
        final T stepSize = stepEnd.subtract(stepStart).divide(pointsPerStep - 1.);
        IntStream.range(0, pointsPerStep).forEach(i -> grid[i] = stepSize.multiply(i).add(stepStart));
        return grid;
    }

    /**
     * Get the number of points in the grid per step.
     * @param stepStart start of the step
     * @param stepEnd end of the step
     * @return the number of points in the grid per step
     */
    protected abstract int getPointsPerStep(double stepStart, double stepEnd);

    /**
     * Get the number of points in the grid per step.
     * @param stepStart start of the step
     * @param stepEnd end of the step
     * @param <T> type of field elements
     * @return the number of points in the grid per step
     */
    protected <T extends CalculusFieldElement<T>> int getPointsPerStep(final T stepStart, final T stepEnd) {
        return getPointsPerStep(stepStart.getReal(), stepEnd.getReal());
    };

}
