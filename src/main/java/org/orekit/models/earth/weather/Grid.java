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
package org.orekit.models.earth.weather;

import java.util.List;
import java.util.function.ToDoubleFunction;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

/** Container for a complete grid.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
class Grid {

    /** Latitude indexer. */
    private final Indexer latitudeIndexer;

    /** Longitude indexer. */
    private final Indexer longitudeIndexer;

    /** Grid entries. */
    private final GridEntry[][] entries;

    /** Simple constructor.
     * @param loadedEntries loaded entries, organized as a simple list
     * @param name file name
     */
    Grid(final List<GridEntry> loadedEntries, final String name) {

        // set up indexers
        latitudeIndexer  = new Indexer(loadedEntries, GridEntry::getLatitude, name);
        longitudeIndexer = new Indexer(loadedEntries, GridEntry::getLongitude, name);

        // organize entries in the regular grid (with one extra column for wrapping in longitude)
        entries = new GridEntry[latitudeIndexer.n][longitudeIndexer.n + 1];
        for (final GridEntry entry : loadedEntries) {
            final int ia = latitudeIndexer.closeIndex(entry.getLatitude());
            final int io = longitudeIndexer.closeIndex(entry.getLongitude());
            entries[ia][io] = entry;
        }

        // wrap the grid around the Earth in longitude
        for (int ia = 0; ia < latitudeIndexer.n; ia++) {
            if (entries[ia][0] != null) {
                entries[ia][longitudeIndexer.n] = entries[ia][0].buildWrappedEntry();
            }
        }

        // check all regularly spaced coordinates are present in the loaded entries
        for (final GridEntry[] row : entries) {
            for (final GridEntry entry : row) {
                if (entry == null) {
                    throw new OrekitException(OrekitMessages.IRREGULAR_OR_INCOMPLETE_GRID, name);
                }
            }
        }

    }

    /** Get index of South entries in the grid.
     * @param latitude latitude to locate (radians)
     * @return index of South entries in the grid
     */
    private int getSouthIndex(final double latitude) {
        // make sure we have at least one point remaining on North by clipping to size - 2
        return FastMath.min(latitudeIndexer.lowIndex(latitude), latitudeIndexer.n - 2);
    }

    /** Get index of West entries in the grid.
     * @param longitude longitude to locate (radians)
     * @return index of West entries in the grid
     */
    private int getWestIndex(final double longitude) {
        // we don't do clipping in longitude because we have added a column to wrap around the Earth
        return longitudeIndexer.lowIndex(longitude);
    }

    /** Get interpolator within a cell.
     * @param latitude latitude of point of interest
     * @param longitude longitude of point of interest
     * @param altitude altitude of point of interest
     * @param deltaRef duration since reference date
     * @return interpolator for the cell
     */
    CellInterpolator getInterpolator(final double latitude, final double longitude,
                                     final double altitude, final double deltaRef) {

        // keep longitude within grid range
        final double normalizedLongitude =
                        MathUtils.normalizeAngle(longitude,
                                                 entries[0][0].getLongitude() + FastMath.PI);

        // find neighboring grid entries
        final int southIndex = getSouthIndex(latitude);
        final int westIndex  = getWestIndex(normalizedLongitude);

        final double coef = (deltaRef / Constants.JULIAN_YEAR) * 2 * FastMath.PI;
        final SinCos sc1  = FastMath.sinCos(coef);
        final SinCos sc2  = FastMath.sinCos(2.0 * coef);

        // build interpolator
        return new CellInterpolator(latitude, normalizedLongitude,
                                    entries[southIndex    ][westIndex    ].evaluate(sc1, sc2, altitude),
                                    entries[southIndex    ][westIndex + 1].evaluate(sc1, sc2, altitude),
                                    entries[southIndex + 1][westIndex    ].evaluate(sc1, sc2, altitude),
                                    entries[southIndex + 1][westIndex + 1].evaluate(sc1, sc2, altitude));

    }

    /** Get interpolator within a cell.
     * @param <T> type of the field elements
     * @param latitude latitude of point of interest
     * @param longitude longitude of point of interest
     * @param altitude altitude of point of interest
     * @param deltaRef duration since reference date
     * @return interpolator for the cell
     */
    <T extends CalculusFieldElement<T>> FieldCellInterpolator<T> getInterpolator(final T latitude, final T longitude,
                                                                                 final T altitude, final T deltaRef) {

        // keep longitude within grid range
        final T normalizedLongitude =
                        MathUtils.normalizeAngle(longitude,
                                                 longitude.newInstance(entries[0][0].getLongitude() + FastMath.PI));

        // find neighboring grid entries
        final int southIndex = getSouthIndex(latitude.getReal());
        final int westIndex  = getWestIndex(normalizedLongitude.getReal());

        final T              coef = deltaRef.multiply(2 * FastMath.PI / Constants.JULIAN_YEAR);
        final FieldSinCos<T> sc1  = FastMath.sinCos(coef);
        final FieldSinCos<T> sc2  = FastMath.sinCos(coef.multiply(2));

         // build interpolator
        return new FieldCellInterpolator<>(latitude, normalizedLongitude,
                                           entries[southIndex    ][westIndex    ].evaluate(sc1, sc2, altitude),
                                           entries[southIndex    ][westIndex + 1].evaluate(sc1, sc2, altitude),
                                           entries[southIndex + 1][westIndex    ].evaluate(sc1, sc2, altitude),
                                           entries[southIndex + 1][westIndex + 1].evaluate(sc1, sc2, altitude));

    }

    /** Check if grid contains all specified models.
     * @param types models types
     * @return true if grid contain the model
     */
    boolean hasModels(final SeasonalModelType... types) {
        boolean hasAll = true;
        for (final SeasonalModelType type : types) {
            hasAll &= entries[0][0].hasModel(type);
        }
        return hasAll;
    }

    /** Indexer for latitude/longitude.
     * @since 14.0
     */
    private static class Indexer {

        /** Minimum value. */
        private final double min;

        /** Step between values. */
        private final double step;

        /** Number of sampling points. */
        private final int n;

        /** Build an indexer.
         * @param entries   all loaded entries
         * @param extractor extractor for the coordinate we are looking for
         * @param name      file name
         */
        Indexer(final List<GridEntry> entries, final ToDoubleFunction<GridEntry> extractor, final String name) {

            final double tolerance = Unit.parse("mas").toSI(1.0);

            // look for minimum and maximum grid row/column
            double inf = Double.POSITIVE_INFINITY;
            double sup = Double.NEGATIVE_INFINITY;
            for (final GridEntry entry : entries) {
                final double coordinate = extractor.applyAsDouble(entry);
                inf = FastMath.min(inf, coordinate);
                sup = FastMath.max(sup, coordinate);
            }

            // look for first step
            double firstStep = Double.POSITIVE_INFINITY;
            for (final GridEntry entry : entries) {
                final double delta = extractor.applyAsDouble(entry) - inf;
                if (delta > tolerance) {
                    // this entry does not belong to the minimum grid row/column
                    firstStep = FastMath.min(firstStep, delta);
                }
            }

            // store grid characteristics
            this.min  = inf;
            this.step = firstStep;
            this.n    = 1 + (int) FastMath.rint((sup - inf) / firstStep);

            // check regularity
            for (final GridEntry entry : entries) {
                final double coordinate = extractor.applyAsDouble(entry);
                final double rebuilt = min + closeIndex(coordinate) * step;
                if (FastMath.abs(coordinate - rebuilt) > tolerance) {
                    throw new OrekitException(OrekitMessages.IRREGULAR_OR_INCOMPLETE_GRID, name);
                }
            }

        }

        /** Find index corresponding to coordinate.
         * @param coordinate coordinate along axis
         * @return index of grid point at or just below coordinate
         */
        public int lowIndex(final double coordinate) {
            return (int) FastMath.floor((coordinate - min) / step);
        }

        /** Find index corresponding to coordinate.
         * @param coordinate coordinate along axis
         * @return index of grid point closest to coordinate
         */
        public int closeIndex(final double coordinate) {
            return (int) FastMath.rint((coordinate - min) / step);
        }

    }

}
