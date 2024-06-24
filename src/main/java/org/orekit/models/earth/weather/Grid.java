/* Copyright 2002-2024 CS GROUP
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

import java.util.List;
import java.util.SortedSet;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Container for a complete grid.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
class Grid {

    /** Latitude sample. */
    private final SortedSet<Integer> latitudeSample;

    /** Longitude sample. */
    private final SortedSet<Integer> longitudeSample;

    /** Grid entries. */
    private final GridEntry[][] entries;

    /** Simple constructor.
     * @param latitudeSample latitude sample
     * @param longitudeSample longitude sample
     * @param loadedEntries loaded entries, organized as a simple list
     * @param name file name
     */
    Grid(final SortedSet<Integer> latitudeSample, final SortedSet<Integer> longitudeSample,
         final List<GridEntry> loadedEntries, final String name) {

        final int nA         = latitudeSample.size();
        final int nO         = longitudeSample.size() + 1; // we add one here for wrapping the grid
        this.entries         = new GridEntry[nA][nO];
        this.latitudeSample  = latitudeSample;
        this.longitudeSample = longitudeSample;

        // organize entries in the regular grid
        for (final GridEntry entry : loadedEntries) {
            final int latitudeIndex  = latitudeSample.headSet(entry.getLatKey() + 1).size() - 1;
            final int longitudeIndex = longitudeSample.headSet(entry.getLonKey() + 1).size() - 1;
            entries[latitudeIndex][longitudeIndex] = entry;
        }

        // finalize the grid
        for (final GridEntry[] row : entries) {

            // check for missing entries
            for (int longitudeIndex = 0; longitudeIndex < nO - 1; ++longitudeIndex) {
                if (row[longitudeIndex] == null) {
                    throw new OrekitException(OrekitMessages.IRREGULAR_OR_INCOMPLETE_GRID, name);
                }
            }

            // wrap the grid around the Earth in longitude
            row[nO - 1] = row[0].buildWrappedEntry();

        }

    }

    /** Get index of South entries in the grid.
     * @param latitude latitude to locate (radians)
     * @return index of South entries in the grid
     */
    private int getSouthIndex(final double latitude) {

        final int latKey = (int) FastMath.rint(FastMath.toDegrees(latitude) * GridEntry.DEG_TO_MAS);
        final int index  = latitudeSample.headSet(latKey + 1).size() - 1;

        // make sure we have at least one point remaining on North by clipping to size - 2
        return FastMath.min(index, latitudeSample.size() - 2);

    }

    /** Get index of West entries in the grid.
     * @param longitude longitude to locate (radians)
     * @return index of West entries in the grid
     */
    private int getWestIndex(final double longitude) {

        final int lonKey = (int) FastMath.rint(FastMath.toDegrees(longitude) * GridEntry.DEG_TO_MAS);
        final int index  = longitudeSample.headSet(lonKey + 1).size() - 1;

        // we don't do clipping in longitude because we have added a row to wrap around the Earth
        return index;

    }

    /** Get interpolator within a cell.
     * @param latitude latitude of point of interest
     * @param longitude longitude of point of interest
     * @return interpolator for the cell
     */
    CellInterpolator getInterpolator(final double latitude, final double longitude) {

        // keep longitude within grid range
        final double normalizedLongitude =
                        MathUtils.normalizeAngle(longitude,
                                                 entries[0][0].getLongitude() + FastMath.PI);

        // find neighboring grid entries
        final int southIndex = getSouthIndex(latitude);
        final int westIndex  = getWestIndex(normalizedLongitude);

        // build interpolator
        return new CellInterpolator(latitude, normalizedLongitude,
                                    entries[southIndex    ][westIndex    ],
                                    entries[southIndex    ][westIndex + 1],
                                    entries[southIndex + 1][westIndex    ],
                                    entries[southIndex + 1][westIndex + 1]);

    }

    /** Get interpolator within a cell.
     * @param <T> type of the field elements
     * @param latitude latitude of point of interest
     * @param longitude longitude of point of interest
     * @return interpolator for the cell
     */
    <T extends CalculusFieldElement<T>> FieldCellInterpolator<T> getInterpolator(final T latitude, final T longitude) {

        // keep longitude within grid range
        final T normalizedLongitude =
                        MathUtils.normalizeAngle(longitude,
                                                 longitude.newInstance(entries[0][0].getLongitude() + FastMath.PI));

        // find neighboring grid entries
        final int southIndex = getSouthIndex(latitude.getReal());
        final int westIndex  = getWestIndex(normalizedLongitude.getReal());

        // build interpolator
        return new FieldCellInterpolator<>(latitude, normalizedLongitude,
                                           entries[southIndex    ][westIndex    ],
                                           entries[southIndex    ][westIndex + 1],
                                           entries[southIndex + 1][westIndex    ],
                                           entries[southIndex + 1][westIndex + 1]);

    }

    /** Check if grid contains all specified models.
     * @param types models types
     * @return true if grid contain the model
     */
    boolean hasModels(final SeasonalModelType... types) {
        boolean hasAll = true;
        for (final SeasonalModelType type : types) {
            hasAll &= entries[0][0].getModel(type) != null;
        }
        return hasAll;
    }

}
