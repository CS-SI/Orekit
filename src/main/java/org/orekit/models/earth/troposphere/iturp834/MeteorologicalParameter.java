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
package org.orekit.models.earth.troposphere.iturp834;

/** Meteorological parameter.
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
enum MeteorologicalParameter {

    /** Air total pressure at the Earth surface. */
    AIR_TOTAL_PRESSURE("pres"),

    /** Water vapour partial pressure at the Earth surface. */
    WATER_VAPOUR_PARTIAL_PRESSURE("vapr"),

    /** Mean temperature of the water vapour column above the surface. */
    MEAN_TEMPERATURE("tmpm"),

    /** Vapour pressure decrease factor. */
    VAPOUR_PRESSURE_DECREASE_FACTOR("lamd"),

    /** Lapse rate of mean temperature of water vapour from Earth surface. */
    LAPSE_RATE_MEAN_TEMPERATURE("alfm");

    /** Prefix of data file. */
    private final String prefix;

    /** Simple constructor.
     * @param prefix prefix of data file
     */
    MeteorologicalParameter(final String prefix) {
        this.prefix = prefix;
    }

    /** Get name of average value file.
     * @return name of average value file
     */
    public String averageValue() {
        return prefix + "_gd_a1.dat";
    }

    /** Get name of seasonal fluctuation.
     * @return name of seasonal fluctuation file
     */
    public String seasonalFluctuation() {
        return prefix + "_gd_a2.dat";
    }

    /** Get name of day of minimum value file.
     * @return name of day of minimum value file
     */
    public String dayMinimum() {
        return prefix + "_gd_a3.dat";
    }

}
