/* Copyright 2022-2026 Romain Serra
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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

/**
 * Data container for Keplerian orbital elements (Field version).
 * @param a semi-major axis
 * @param e eccentricity
 * @param i inclination
 * @param pa position angle
 * @param raan right ascension of ascending node
 * @param anomaly anomaly corresponding to angle type
 * @param positionAngleType angle type
 * @author Romain Serra
 * @see KeplerianParameters
 * @see PositionAngleType
 * @since 14.0
 */
public record FieldKeplerianParameters<T extends CalculusFieldElement<T>>(T a, T e, T i, T pa, T raan, T anomaly,
                                                                          PositionAngleType positionAngleType) {

    /**
     * Constructor from non-Field.
     * @param field field
     * @param elements Keplerian elements
     */
    public FieldKeplerianParameters(final Field<T> field, final KeplerianParameters elements) {
        this(field.getZero().newInstance(elements.a()), field.getZero().newInstance(elements.e()),
                field.getZero().newInstance(elements.i()), field.getZero().newInstance(elements.pa()),
                field.getZero().newInstance(elements.raan()), field.getZero().newInstance(elements.anomaly()),
                elements.positionAngleType());
    }

    /**
     * Builds a new instance with the specified position angle type.
     * @param angleType angle type for the output
     * @return Keplerian elements with the specified position angle type
     */
    public FieldKeplerianParameters<T> withPositionAngleType(final PositionAngleType angleType) {
        final T convertedAnomaly = FieldKeplerianAnomalyUtility.convertAnomaly(positionAngleType, anomaly, e, angleType);
        return new FieldKeplerianParameters<>(a, e, i, pa, raan, convertedAnomaly, angleType);
    }

    /**
     * Convert Field elements to non-Field ones.
     * @return Keplerian elements
     */
    public KeplerianParameters toKeplerianElements() {
        return new KeplerianParameters(a.getReal(), e.getReal(), i.getReal(), pa.getReal(), raan.getReal(),
                anomaly.getReal(), positionAngleType);
    }
}
