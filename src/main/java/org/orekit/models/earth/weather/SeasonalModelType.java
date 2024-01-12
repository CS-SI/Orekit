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

/** Type of seasonal model used in Global Pressure Temperature models.
 * @see "Landskron, D. & Böhm, J. J Geod (2018)
 *      VMF3/GPT3: refined discrete and empirical troposphere mapping functions
 *      92: 349. https://doi.org/10.1007/s00190-017-1066-2"
 * @author Luc Maisonobe
 * @since 12.1
 */
enum SeasonalModelType {

    /** Pressure model. */
    PRESSURE("p"),

    /** Temperature model. */
    TEMPERATURE("T"),

    /** Specific humidity model. */
    QV("Q"),

    /** Temperature gradient model. */
    DT("dT"),

    /** ah coefficient model. */
    AH("h", "a_h"),

    /** aw coefficient model. */
    AW("w", "a_w"),

    /** Water vapor decrease factor model. */
    LAMBDA("lambda"),

    /** Mean temperature weighted with water vapor pressure model. */
    TM("Tm"),

    /** Hydrostatic North gradient coefficient model. */
    GN_H("Gn_h"),

    /** Hydrostatic East gradient coefficient model. */
    GE_H("Ge_h"),

    /** Wet North gradient coefficient model. */
    GN_W("Gn_w"),

    /** Wet East gradient coefficient model. */
    GE_W("Ge_w");

    /** Global suffix that applies to all labels. */
    private static final String SUFFIX = ":a0";

    /** Parsing map. */
    private static final Map<String, SeasonalModelType> LABELS_MAP = new HashMap<>();
    static {
        for (final SeasonalModelType type : values()) {
            for (final String label : type.labels) {
                LABELS_MAP.put(label + SUFFIX, type);
            }
        }
    }

    /** Labels in grid files headers. */
    private final String[] labels;

    /** Simple constructor.
     * @param labels labels in grid files headers
     */
    SeasonalModelType(final String... labels) {
        this.labels = labels.clone();
    }

    /** Parse a field to get the type.
     * @param field field to parse
     * @param lineNumber line number
     * @param name file name
     * @return the type corresponding to the field
     * @exception IllegalArgumentException if the field does not correspond to a type
     */
    public static SeasonalModelType parseType(final String field, final int lineNumber, final String name) {
        return LABELS_MAP.get(field);
    }

}
