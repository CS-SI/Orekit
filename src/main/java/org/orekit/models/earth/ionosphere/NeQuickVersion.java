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
package org.orekit.models.earth.ionosphere;

/** Enumerate for NeQuick model version.
 * @author Luc Maisonobe
 * @since 13.0
 */
public enum NeQuickVersion {

    /** Galileo-specific version of NeQuick 2. */
    NEQUICK_2_GALILEO(36, 36, "modipNeQG_wrapped.asc", true),

    /** Original NeQuick 2 as recommended by ITU-R P.531. */
    NEQUICK_2_ITU(180, 180, "modip.asc", false);

    /** Number of cells of MODIP grid in longitude (without wrapping). */
    private final int cellsLon;

    /** Number of cells of MODIP grid in latitude (without wrapping). */
    private final int cellsLat;

    /** Name of grid resource file. */
    private final String resourceName;

    /** Indicator for already included MODIP grid wrapping in resource file. */
    private final boolean wrappingAlreadyIncluded;

    /** Simple constructor.
     * @param cellsLon number of cells of MODIP grid in longitude (without wrapping)
     * @param cellsLat number of cells of MODIP grid in latitude (without wrapping)
     * @param resourceName name of grid resource file
     * @param wrappingAlreadyIncluded indicator for already included MODIP grid wrapping in resource file
     */
    NeQuickVersion(final int cellsLon, final int cellsLat,
                   final String resourceName, final boolean wrappingAlreadyIncluded) {
        this.cellsLon                = cellsLon;
        this.cellsLat                = cellsLat;
        this.resourceName            = resourceName;
        this.wrappingAlreadyIncluded = wrappingAlreadyIncluded;
    }

    /** Get number of cells of MODIP grid in longitude (without wrapping).
     * @return number of cells of MODIP grid in longitude (without wrapping)
     */
    int getcellsLon() {
        return cellsLon;
    }

    /** Get number of cells of MODIP grid in latitude (without wrapping).
     * @return number of cells of MODIP grid in latitude (without wrapping)
     */
    int getcellsLat() {
        return cellsLat;
    }

    /** Get name of grid resource file.
     * @return name of grid resource file
     */
    String getResourceName() {
        return resourceName;
    }

    /** Check if wrapping is already included in resource file.
     * @return true if wrapping is already included in resource file
     */
    boolean isWrappingAlreadyIncluded() {
        return wrappingAlreadyIncluded;
    }

}
