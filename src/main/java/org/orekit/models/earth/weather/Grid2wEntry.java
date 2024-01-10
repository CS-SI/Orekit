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

import org.hipparchus.util.MathUtils;

/** Grid entry in Global Pressure Temperature models 2 wet.
 * @author Luc Maisonobe
 * @since 12.1
 */
class Grid2wEntry extends Grid2Entry {

    /** Mean temperature weighted with water vapor pressure model. */
    private final SeasonalModel tm;

    /** Water vapor decrease factor model. */
    private final SeasonalModel lambda;

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
     * @param lambda water vapor decrease factor model
     * @param tm mean temperature weighted with water vapor pressure model
     */
    Grid2wEntry(final double latitude, final int latKey, final double longitude, final int lonKey,
                final double undulation, final double hS,
                final SeasonalModel pressure0, final SeasonalModel temperature0,
                final SeasonalModel qv0, final SeasonalModel dT,
                final SeasonalModel ah, final SeasonalModel aw,
                final SeasonalModel lambda, final SeasonalModel tm) {

        super(latitude, latKey, longitude, lonKey, undulation, hS,
              pressure0, temperature0, qv0, dT, ah, aw);
        this.tm     = tm;
        this.lambda = lambda;
    }

    /** {@inheritDoc} */
    @Override
    public Grid2wEntry buildWrappedEntry() {
        return new Grid2wEntry(getLatitude(), getLatKey(),
                               getLongitude() + MathUtils.TWO_PI,
                               getLonKey() + DEG_TO_MAS * 360,
                               getUndulation(), getHs(),
                               getPressure0(), getTemperature0(),
                               getQv0(), getDt(), getAh(), getAw(),
                               lambda, tm);
    }

    /** Get temperature weighted with water vapor pressure model.
     * @return temperature weighted with water vapor pressure model
     */
    SeasonalModel getTm() {
        return tm;
    }

    /** Get water vapor decrease factor model.
     * @return water vapor decrease factor model
     */
    SeasonalModel getLambda() {
        return lambda;
    }

}
