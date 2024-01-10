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

/** Grid entry in Global Pressure Temperature models 3.
 * @author Luc Maisonobe
 * @since 12.1
 */
class Grid3Entry extends Grid2wEntry {

    /** Hydrostatic North gradient coefficient model. */
    private final SeasonalModel gnh;

    /** Wet North gradient coefficient model. */
    private final SeasonalModel gnw;

    /** Hydrostatic East gradient coefficient model. */
    private final SeasonalModel geh;

    /** Wet East gradient coefficient model. */
    private final SeasonalModel gew;

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
     * @param gnh hydrostatic North gradient coefficient model
     * @param gnw wet North gradient coefficient model
     * @param geh hydrostatic East gradient coefficient model
     * @param gew wet East gradient coefficient model
     */
    Grid3Entry(final double latitude, final int latKey, final double longitude, final int lonKey,
               final double undulation, final double hS,
               final SeasonalModel pressure0, final SeasonalModel temperature0,
               final SeasonalModel qv0, final SeasonalModel dT,
               final SeasonalModel ah, final SeasonalModel aw,
               final SeasonalModel lambda, final SeasonalModel tm,
               final SeasonalModel gnh, final SeasonalModel gnw,
               final SeasonalModel geh, final SeasonalModel gew) {

        super(latitude, latKey, longitude, lonKey, undulation, hS,
              pressure0, temperature0, qv0, dT, ah, aw,
              lambda, tm);
        this.gnh = gnh;
        this.gnw = gnw;
        this.geh = geh;
        this.gew = gew;
    }

    /** {@inheritDoc} */
    @Override
    public Grid3Entry buildWrappedEntry() {
        return new Grid3Entry(getLatitude(), getLatKey(),
                              getLongitude() + MathUtils.TWO_PI,
                              getLonKey() + GridEntry.DEG_TO_MAS * 360,
                              getUndulation(), getHs(),
                              getPressure0(), getTemperature0(),
                              getQv0(), getDt(),
                              getAh(), getAw(),
                              getLambda(), getTm(),
                              gnh, gnw, geh, gew);
    }

    /** Get hydrostatic North gradient coefficient model.
     * @return hydrostatic North gradient coefficient model
     */
    SeasonalModel getGnh() {
        return gnh;
    }

    /** Get wet North gradient coefficient model.
     * @return wet North gradient coefficient model
     */
    SeasonalModel getGnw() {
        return gnw;
    }

    /** Get hydrostatic East gradient coefficient model.
     * @return hydrostatic East gradient coefficient model
     */
    SeasonalModel getGeh() {
        return geh;
    }

    /** Get wet East gradient coefficient model.
     * @return wet East gradient coefficient model
     */
    SeasonalModel getGew() {
        return gew;
    }

}
