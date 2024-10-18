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

import java.util.function.Function;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.interpolation.FieldBilinearInterpolatingFunction;
import org.hipparchus.util.MathArrays;

/** Interpolator within a grid cell.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldCellInterpolator<T extends CalculusFieldElement<T>> {

    /** Latitude of point of interest. */
    private final T latitude;

    /** Longitude of point of interest. */
    private final T longitude;

    /** South-West grid entry. */
    private final FieldEvaluatedGridEntry<T> southWest;

    /** South-East grid entry. */
    private final FieldEvaluatedGridEntry<T> southEast;

    /** North-West grid entry. */
    private final FieldEvaluatedGridEntry<T> northWest;

    /** North-East grid entry. */
    private final FieldEvaluatedGridEntry<T> northEast;

    /** Simple constructor.
     * @param latitude latitude of point of interest
     * @param longitude longitude of point of interest
     * @param southWest South-West grid entry
     * @param southEast South-East grid entry
     * @param northWest North-West grid entry
     * @param northEast North-East grid entry
     */
    FieldCellInterpolator(final T latitude, final T longitude,
                          final FieldEvaluatedGridEntry<T> southWest, final FieldEvaluatedGridEntry<T> southEast,
                          final FieldEvaluatedGridEntry<T> northWest, final FieldEvaluatedGridEntry<T> northEast) {
        this.latitude  = latitude;
        this.longitude = longitude;
        this.southWest = southWest;
        this.southEast = southEast;
        this.northWest = northWest;
        this.northEast = northEast;
    }

    /** Interpolate a grid function.
     * @param gridGetter getter for the grid function
     * @return interpolated function
     * @since 13.0
     */
    T fieldInterpolate(final Function<FieldEvaluatedGridEntry<T>, T> gridGetter) {

        final Field<T> field = latitude.getField();
        final T        zero  = field.getZero();

        // cell surrounding the point
        final T[] xVal = MathArrays.buildArray(field, 2);
        xVal[0] = zero.newInstance(southWest.getEntry().getLongitude());
        xVal[1] = zero.newInstance(northEast.getEntry().getLongitude());
        final T[] yVal = MathArrays.buildArray(field, 2);
        yVal[0] = zero.newInstance(southWest.getEntry().getLatitude());
        yVal[1] = zero.newInstance(northEast.getEntry().getLatitude());

        // evaluate grid points at specified day
        final T[][] fval = MathArrays.buildArray(field, 2, 2);
        fval[0][0] = gridGetter.apply(southWest);
        fval[0][1] = gridGetter.apply(northWest);
        fval[1][0] = gridGetter.apply(southEast);
        fval[1][1] = gridGetter.apply(northEast);

        // perform interpolation in the grid
        return new FieldBilinearInterpolatingFunction<>(xVal, yVal, fval).value(longitude, latitude);

    }

}
