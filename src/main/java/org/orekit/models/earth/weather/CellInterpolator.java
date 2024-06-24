/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.weather;

import java.util.function.ToDoubleFunction;

import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;

/** Interpolator within a grid cell.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class CellInterpolator {

    /** Latitude of point of interest. */
    private final double latitude;

    /** Longitude of point of interest. */
    private final double longitude;

    /** South-West grid entry. */
    private final GridEntry southWest;

    /** South-East grid entry. */
    private final GridEntry southEast;

    /** North-West grid entry. */
    private final GridEntry northWest;

    /** North-East grid entry. */
    private final GridEntry northEast;

    /** Simple constructor.
     * @param latitude latitude of point of interest
     * @param longitude longitude of point of interest
     * @param southWest South-West grid entry
     * @param southEast South-East grid entry
     * @param northWest North-West grid entry
     * @param northEast North-East grid entry
     */
    CellInterpolator(final double latitude, final double longitude,
                     final GridEntry southWest, final GridEntry southEast,
                     final GridEntry northWest, final GridEntry northEast) {
        this.latitude  = latitude;
        this.longitude = longitude;
        this.southWest = southWest;
        this.southEast = southEast;
        this.northWest = northWest;
        this.northEast = northEast;
    }

    /** Interpolate a grid function.
     * @param gridGetter getter for the grid function
     * @return interpolated function"
     */
    double interpolate(final ToDoubleFunction<GridEntry> gridGetter) {

        // cell surrounding the point
        final double[] xVal = new double[] {
            southWest.getLongitude(), southEast.getLongitude()
        };
        final double[] yVal = new double[] {
            southWest.getLatitude(), northWest.getLatitude()
        };

        // evaluate grid points at specified day
        final double[][] fval = new double[][] {
            {
                gridGetter.applyAsDouble(southWest),
                gridGetter.applyAsDouble(northWest)
            }, {
                gridGetter.applyAsDouble(southEast),
                gridGetter.applyAsDouble(northEast)
            }
        };

        // perform interpolation in the grid
        return new BilinearInterpolatingFunction(xVal, yVal, fval).value(longitude, latitude);

    }

}
