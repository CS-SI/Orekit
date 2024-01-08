/* Copyright 2023 Thales Alenia Space
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

/** Base grid entry in Global Pressure Temperature models.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
abstract class GridEntry {

    /** Conversion factor from degrees to mill arcseconds. */
    public static final int DEG_TO_MAS = 3600000;

    /** Latitude (radian). */
    private final double latitude;

    /** Latitude key (mas). */
    private final int latKey;

    /** Longitude (radian). */
    private final double longitude;

    /** Longitude key (mas). */
    private final int lonKey;

    /** Undulation. */
    private final double undulation;

    /** Height correction. */
    private final double hS;

    /** Build an entry from its components.
     * @param latitude latitude (radian)
     * @param latKey latitude key (mas)
     * @param longitude longitude (radian)
     * @param lonKey longitude key (mas)
     * @param undulation undulation (m)
     * @param hS height correction
     */
    GridEntry(final double latitude, final int latKey, final double longitude, final int lonKey,
              final double undulation, final double hS) {

        this.latitude     = latitude;
        this.latKey       = latKey;
        this.longitude    = longitude;
        this.lonKey       = lonKey;
        this.undulation   = undulation;
        this.hS           = hS;
    }

    /** Build a new entry 360° to the East of instance.
     * @return new wrapping entry (always same type as instance)
     */
    public abstract GridEntry buildWrappedEntry();

    /** Get latitude (radian).
     * @return latitude (radian)
     */
    double getLatitude() {
        return latitude;
    }

    /** Get latitude key (mas).
     * @return latitude key (mas)
     */
    int getLatKey() {
        return latKey;
    }

    /** Get longitude (radian).
     * @return longitude (radian)
     */
    double getLongitude() {
        return longitude;
    }

    /** Get longitude key (mas).
     * @return longitude key (mas)
     */
    int getLonKey() {
        return lonKey;
    }

    /** Get undulation.
     * @return undulation
     */
    double getUndulation() {
        return undulation;
    }

    /** Get height correction.
     * @return height correction
     */
    double getHs() {
        return hS;
    }

}
