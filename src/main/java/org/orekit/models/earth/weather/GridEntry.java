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
package org.orekit.models.earth.weather;

/** Grid entry in Global Pressure Temperature models.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
class GridEntry {

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

    /** Pressure model. */
    private final SeasonalModel pressure0;

    /** Temperature model. */
    private final SeasonalModel temperature0;

    /** Specific humidity model. */
    private final SeasonalModel qv0;

    /** Temperature gradient model. */
    private final SeasonalModel dT;

    /** ah coefficient model. */
    private final SeasonalModel ah;

    /** aw coefficient model. */
    private final SeasonalModel aw;

    /** Build an entry from its components.
     * @param latitude latitude (radian)
     * @param latKey latitude key (mas)
     * @param longitude longitude (radian)
     * @param lonKey longitude key (mas)
     * @param undulation undulation (m)
     * @param hS height correction
     * @param pressure0 pressure model
     * @param temperature0 temperature model
     * @param qv0 specific humidity model
     * @param dT temperature gradient model
     * @param ah ah coefficient model
     * @param aw aw coefficient model
     */
    GridEntry(final double latitude, final int latKey, final double longitude, final int lonKey,
              final double undulation, final double hS,
              final SeasonalModel pressure0, final SeasonalModel temperature0,
              final SeasonalModel qv0, final SeasonalModel dT,
              final SeasonalModel ah, final SeasonalModel aw) {

        this.latitude     = latitude;
        this.latKey       = latKey;
        this.longitude    = longitude;
        this.lonKey       = lonKey;
        this.undulation   = undulation;
        this.hS           = hS;
        this.pressure0    = pressure0;
        this.temperature0 = temperature0;
        this.qv0          = qv0;
        this.dT           = dT;
        this.ah           = ah;
        this.aw           = aw;
    }

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

    /** Get pressure model.
     * @return pressure model
     */
    SeasonalModel getPressure0() {
        return pressure0;
    }

    /** Get temperature model.
     * @return temperature model
     */
    SeasonalModel getTemperature0() {
        return temperature0;
    }

    /** Get specific humidity model.
     * @return specific humidity model
     */
    SeasonalModel getQv0() {
        return qv0;
    }

    /** Get temperature gradient model.
     * @return temperature gradient model
     */
    SeasonalModel getDt() {
        return dT;
    }

    /** Get ah coefficient model.
     * @return ah coefficient model
     */
    SeasonalModel getAh() {
        return ah;
    }

    /** Get aw coefficient model.
     * @return aw coefficient model
     */
    SeasonalModel getAw() {
        return aw;
    }

}
