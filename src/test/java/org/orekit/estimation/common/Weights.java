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

package org.orekit.estimation.common;

/** Container for base weights.
 * @author Luc Maisonobe
 */
class Weights {

    /** Base weight for range measurements. */
    private final double rangeBaseWeight;

    /** Base weight for range rate measurements. */
    private final double rangeRateBaseWeight;

    /** Base weight for azimuth-elevation measurements. */
    private final double[] azElBaseWeight;

    /** Base weight for PV measurements. */
    private final double pvBaseWeight;

    /** Simple constructor.
     * @param rangeBaseWeight base weight for range measurements
     * @param rangeRateBaseWeight base weight for range rate measurements
     * @param azElBaseWeight base weight for azimuth-elevation measurements
     * @param pvBaseWeight base weight for PV measurements
     */
    Weights(final double rangeBaseWeight,
            final double rangeRateBaseWeight,
            final double[] azElBaseWeight,
            final double pvBaseWeight) {
        this.rangeBaseWeight     = rangeBaseWeight;
        this.rangeRateBaseWeight = rangeRateBaseWeight;
        this.azElBaseWeight      = azElBaseWeight.clone();
        this.pvBaseWeight        = pvBaseWeight;
    }

    /** Get base weight for range measurements.
     * @return base weight for range measurements
     */
    public double getRangeBaseWeight() {
        return rangeBaseWeight;
    }

    /** Get base weight for range rate measurements.
     * @return base weight for range rate measurements
     */
    public double getRangeRateBaseWeight() {
        return rangeRateBaseWeight;
    }

    /** Get base weight for azimuth-elevation measurements.
     * @return base weight for azimuth-elevation measurements
     */
    public double[] getAzElBaseWeight() {
        return azElBaseWeight.clone();
    }

    /** Get base weight for PV measurements.
     * @return base weight for PV measurements
     */
    public double getPVBaseWeight() {
        return pvBaseWeight;
    }

}
