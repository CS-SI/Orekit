/* Copyright 2022-2025 Thales Alenia Space
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

import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

import java.util.Map;

/** Grid entry in Global Pressure Temperature models, evaluated at some date.
 * @author Luc Maisonobe
 * @since 13.0
 */
class EvaluatedGridEntry {

    /** Standard gravity constant [m/s²]. */
    private static final double G = Constants.G0_STANDARD_GRAVITY;

    /** Molar mass of dry air in kg/mol. */
    private static final double DMTR = 28.965e-3;

    /** Universal gas constant in J/K/mol. */
    private static final double RG = 8.3143;

    /** Underlying fixed grid entry. */
    private final GridEntry entry;

    /** Corrected height. */
    private final double correctedHeight;

    /** Virtual temperature factor. */
    private final double factor;

    /** Evaluated seasonal models. */
    private final Map<SeasonalModelType, Double> evaluatedModels;

    /** Build an entry from its components.
     * @param entry underlying fixed entry
     * @param altitude altitude
     * @param evaluatedModels evaluated models
     */
    EvaluatedGridEntry(final GridEntry entry, final double altitude,
                       final Map<SeasonalModelType, Double> evaluatedModels) {
        this.entry           = entry;
        this.evaluatedModels = evaluatedModels;
        this.correctedHeight = altitude - entry.getUndulation() - entry.getHs();
        final double t0 = getEvaluatedModel(SeasonalModelType.TEMPERATURE);
        final double qv = getEvaluatedModel(SeasonalModelType.QV) * 0.001;
        final double tv = t0 * (1 + 0.6077 * qv);
        this.factor     = -correctedHeight * G * DMTR / (RG * tv);
    }

    /** Get underlying fixed entry.
     * @return underlying fixed entry
     */
    public GridEntry getEntry() {
        return entry;
    }

    /** Get evaluated model.
     * @param type model type
     * @return evaluated model type
     */
    public double getEvaluatedModel(final SeasonalModelType type) {
        return evaluatedModels.get(type);
    }

    /** Get temperature.
     * @return temperature
     */
    public double getTemperature() {
        final double t0   = getEvaluatedModel(SeasonalModelType.TEMPERATURE);
        final double dtdh = getEvaluatedModel(SeasonalModelType.DT) * 0.001;
        return t0 + dtdh * correctedHeight;
    }

    /** Get pressure.
     * @return pressure
     */
    public double getPressure() {
        final double p0 = getEvaluatedModel(SeasonalModelType.PRESSURE) * 0.01;
        return p0 * FastMath.exp(factor);
    }

    /** Get water vapor pressure.
     * <p>
     * This applies only to GPT2w and GPT3 as GPT2 does not have water vapor decrease factor λ
     * </p>
     * @return water vapor pressure
     */
    public double getWaterVaporPressure() {
        final double p0     = getEvaluatedModel(SeasonalModelType.PRESSURE) * 0.01;
        final double qv     = getEvaluatedModel(SeasonalModelType.QV) * 0.001;
        final double e0     = qv * p0 / (0.622 + 0.378 * qv);
        final double lambda = getEvaluatedModel(SeasonalModelType.LAMBDA);
        return e0 * FastMath.exp(factor * (1 + lambda));
    }

}
