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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

import java.util.Map;

/** Grid entry in Global Pressure Temperature models, evaluated at some date.
 * @param <T> type oif the field elements
 * @author Luc Maisonobe
 * @since 13.0
 */
class FieldEvaluatedGridEntry<T extends CalculusFieldElement<T>> {

    /** Standard gravity constant [m/s²]. */
    private static final double G = Constants.G0_STANDARD_GRAVITY;

    /** Molar mass of dry air in kg/mol. */
    private static final double DMTR = 28.965e-3;

    /** Universal gas constant in J/K/mol. */
    private static final double RG = 8.3143;

    /** Underlying fixed grid entry. */
    private final GridEntry entry;

    /** Corrected height. */
    private final T correctedHeight;

    /** Virtual temperature factor. */
    private final T factor;

    /** Evaluated seasonal models. */
    private final Map<SeasonalModelType, T> evaluatedModels;

    /** Build an entry from its components.
     * @param entry underlying fixed entry
     * @param altitude altitude
     * @param evaluatedModels evaluated models
     */
    FieldEvaluatedGridEntry(final GridEntry entry, final T altitude,
                            final Map<SeasonalModelType, T> evaluatedModels) {
        this.entry           = entry;
        this.evaluatedModels = evaluatedModels;
        this.correctedHeight = altitude.subtract(entry.getUndulation() + entry.getHs());
        final T t0 = getEvaluatedModel(SeasonalModelType.TEMPERATURE);
        final T qv = getEvaluatedModel(SeasonalModelType.QV).multiply(0.001);
        final T tv = t0.multiply(qv.multiply(0.6077).add(1));
        this.factor          = correctedHeight.negate().multiply(G * DMTR / RG).divide(tv);
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
    public T getEvaluatedModel(final SeasonalModelType type) {
        return evaluatedModels.get(type);
    }

    /** Get temperature.
     * @return temperature
     */
    public T getTemperature() {
        final T t0   = getEvaluatedModel(SeasonalModelType.TEMPERATURE);
        final T dtdh = getEvaluatedModel(SeasonalModelType.DT).multiply(0.001);
        return t0.add(dtdh.multiply(correctedHeight));
    }

    /** Get pressure.
     * @return pressure
     */
    public T getPressure() {
        final T p0 = getEvaluatedModel(SeasonalModelType.PRESSURE).multiply(0.01);
        return FastMath.exp(factor).multiply(p0);
    }

    /** Get water vapor pressure.
     * <p>
     * This applies only to GPT2w and GPT3 as GPT2 does not have water vapor decrease factor λ
     * </p>
     * @return water vapor pressure
     */
    public T getWaterVaporPressure() {
        final T p0     = getEvaluatedModel(SeasonalModelType.PRESSURE).multiply(0.01);
        final T qv     = getEvaluatedModel(SeasonalModelType.QV).multiply(0.001);
        final T e0     = p0.multiply(qv).divide(qv.multiply(0.378).add(0.622));
        final T lambda = getEvaluatedModel(SeasonalModelType.LAMBDA);
        return FastMath.exp(factor.multiply(lambda.add(1))).multiply(e0);
    }

}
