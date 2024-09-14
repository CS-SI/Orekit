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

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;

/** Grid entry in Global Pressure Temperature models.
 * @author Luc Maisonobe
 * @since 12.1
 */
class GridEntry {

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

    /** Seasonal models. */
    private final Map<SeasonalModelType, SeasonalModel> models;

    /** Build an entry from its components.
     * @param latitude latitude (radian)
     * @param latKey latitude key (mas)
     * @param longitude longitude (radian)
     * @param lonKey longitude key (mas)
     * @param undulation undulation (m)
     * @param hS height correction
     * @param models seasonal models
     */
    GridEntry(final double latitude, final int latKey, final double longitude, final int lonKey,
              final double undulation, final double hS, final Map<SeasonalModelType, SeasonalModel> models) {

        this.latitude     = latitude;
        this.latKey       = latKey;
        this.longitude    = longitude;
        this.lonKey       = lonKey;
        this.undulation   = undulation;
        this.hS           = hS;
        this.models       = models;
    }

    /** Build a new entry 360° to the East of instance.
     * @return new wrapping entry (always same type as instance)
     */
    public GridEntry buildWrappedEntry() {
        return new GridEntry(latitude, latKey,
                             longitude + MathUtils.TWO_PI,
                             lonKey + DEG_TO_MAS * 360,
                             undulation, hS,
                             models);
    }

    /** Get latitude (radian).
     * @return latitude (radian)
     */
    public double getLatitude() {
        return latitude;
    }

    /** Get latitude key (mas).
     * @return latitude key (mas)
     */
    public int getLatKey() {
        return latKey;
    }

    /** Get longitude (radian).
     * @return longitude (radian)
     */
    public double getLongitude() {
        return longitude;
    }

    /** Get longitude key (mas).
     * @return longitude key (mas)
     */
    public int getLonKey() {
        return lonKey;
    }

    /** Get undulation.
     * @return undulation
     */
    public double getUndulation() {
        return undulation;
    }

    /** Get height correction.
     * @return height correction
     */
    public double getHs() {
        return hS;
    }

    /** Check if an entry has a model.
     * @param type model type
     * @return true if the entry has the model
     * @since 13.0
     */
    public boolean hasModel(final SeasonalModelType type) {
        return models.containsKey(type);
    }

    /** Evaluate the entry at one date.
     * @param sc1 sine and cosine of yearly harmonic term
     * @param sc2 sine and cosine of bi-yearly harmonic term
     * @param altitude altitude
     * @return evaluated entry
     */
    public EvaluatedGridEntry evaluate(final SinCos sc1, final SinCos sc2, final double altitude) {

        // evaluate all models
        final Map<SeasonalModelType, Double> evaluatedModels = new HashMap<>(models.size());
        for (final Map.Entry<SeasonalModelType, SeasonalModel> entry : models.entrySet()) {
            evaluatedModels.put(entry.getKey(), entry.getValue().evaluate(sc1, sc2));
        }

        // build the evaluated grid entry
        return new EvaluatedGridEntry(this, altitude, evaluatedModels);

    }

    /** Evaluate the entry at one date.
     * @param <T> type of the field elements
     * @param sc1 sine and cosine of yearly harmonic term
     * @param sc2 sine and cosine of bi-yearly harmonic term
     * @param altitude altitude
     * @return evaluated entry
     */
    public <T extends CalculusFieldElement<T>> FieldEvaluatedGridEntry<T> evaluate(final FieldSinCos<T> sc1,
                                                                                   final FieldSinCos<T> sc2,
                                                                                   final T altitude) {

        // evaluate all models
        final Map<SeasonalModelType, T> evaluatedModels = new HashMap<>(models.size());
        for (final Map.Entry<SeasonalModelType, SeasonalModel> entry : models.entrySet()) {
            evaluatedModels.put(entry.getKey(), entry.getValue().evaluate(sc1, sc2));
        }

        // build the evaluated grid entry
        return new FieldEvaluatedGridEntry<>(this, altitude, evaluatedModels);

    }

}
