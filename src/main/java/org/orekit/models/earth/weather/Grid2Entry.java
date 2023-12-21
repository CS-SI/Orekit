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

import org.hipparchus.util.MathUtils;

/** Grid entry in Global Pressure Temperature models 2.
 * @author Luc Maisonobe
 * @since 12.1
 */
class Grid2Entry extends GridEntry {

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
    Grid2Entry(final double latitude, final int latKey, final double longitude, final int lonKey,
               final double undulation, final double hS,
               final SeasonalModel pressure0, final SeasonalModel temperature0,
               final SeasonalModel qv0, final SeasonalModel dT,
               final SeasonalModel ah, final SeasonalModel aw) {

        super(latitude, latKey, longitude, lonKey, undulation, hS);
        this.pressure0    = pressure0;
        this.temperature0 = temperature0;
        this.qv0          = qv0;
        this.dT           = dT;
        this.ah           = ah;
        this.aw           = aw;
    }

    /** {@inheritDoc} */
    @Override
    public Grid2Entry buildWrappedEntry() {
        return new Grid2Entry(getLatitude(), getLatKey(),
                              getLongitude() + MathUtils.TWO_PI,
                              getLonKey() + DEG_TO_MAS * 360,
                              getUndulation(), getHs(),
                              pressure0, temperature0, qv0, dT, ah, aw);
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
