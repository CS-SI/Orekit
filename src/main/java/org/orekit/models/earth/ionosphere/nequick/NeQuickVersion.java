/* Copyright 2002-2024 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.ionosphere.nequick;

import org.orekit.data.DataSource;

/** Enumerate for NeQuick model version.
 * @author Luc Maisonobe
 * @since 13.0
 */
public enum NeQuickVersion {

    /** Galileo-specific version of NeQuick 2. */
    NEQUICK_2_GALILEO(36, 36, "/assets/org/orekit/nequick/modipNeQG_wrapped.asc", true),

    /** Original NeQuick 2 as recommended by ITU-R P.531. */
    NEQUICK_2_ITU(180, 180, "/assets/org/orekit/nequick/modip.asc", false);

    /** Number of cells of ModipGrid grid in longitude (without wrapping). */
    private final int nbCellsLon;

    /** Number of cells of ModipGrid grid in latitude (without wrapping). */
    private final int nbCellsLat;

    /** Name of grid resource file. */
    private final String resourceName;

    /** Indicator for already included ModipGrid grid wrapping in resource file. */
    private final boolean wrappingAlreadyIncluded;

    /** Simple constructor.
     * @param nbCellsLon number of cells of ModipGrid grid in longitude (without wrapping)
     * @param nbCellsLat number of cells of ModipGrid grid in latitude (without wrapping)
     * @param resourceName name of grid resource file
     * @param wrappingAlreadyIncluded indicator for already included ModipGrid grid wrapping in resource file
     */
    NeQuickVersion(final int nbCellsLon, final int nbCellsLat,
                   final String resourceName, final boolean wrappingAlreadyIncluded) {
        this.nbCellsLon              = nbCellsLon;
        this.nbCellsLat              = nbCellsLat;
        this.resourceName            = resourceName;
        this.wrappingAlreadyIncluded = wrappingAlreadyIncluded;
    }

    /** Get number of cells of ModipGrid grid in longitude (without wrapping).
     * @return number of cells of ModipGrid grid in longitude (without wrapping)
     */
    public int getnbCellsLon() {
        return nbCellsLon;
    }

    /** Get number of cells of ModipGrid grid in latitude (without wrapping).
     * @return number of cells of ModipGrid grid in latitude (without wrapping)
     */
    public int getnbCellsLat() {
        return nbCellsLat;
    }

    /** Get grid source.
     * <p>
     * A new data source is created each time this method is called,
     * in practice it should be called only once, as per the
     * initialization on demand holder idiom used to load the grid
     * </p>
     * @return grid source
     */
    public DataSource getSource() {
        return new DataSource(resourceName,
                              () -> NeQuickVersion.class.getResourceAsStream(resourceName));
    }

    /** Check if wrapping is already included in resource file.
     * @return true if wrapping is already included in resource file
     */
    public boolean isWrappingAlreadyIncluded() {
        return wrappingAlreadyIncluded;
    }

}
