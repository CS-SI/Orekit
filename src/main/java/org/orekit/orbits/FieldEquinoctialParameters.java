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
 * Data container for equinoctial orbital elements (Field version).
 * @param a semi-major axis
 * @param ex first component of the eccentricity vector
 * @param ey second component of the eccentricity vector
 * @param hx first component of the inclination vector
 * @param hy second component of the inclination vector
 * @param longitudeArgument argument of longitude corresponding to angle type
 * @param positionAngleType angle type
 * @author Romain Serra
 * @see PositionAngleType
 * @see EquinoctialParameters
 * @since 14.0
 */
public record FieldEquinoctialParameters<T extends CalculusFieldElement<T>>(T a, T ex, T ey, T hx, T hy, T longitudeArgument,
                                                                            PositionAngleType positionAngleType) {

    /**
     * Constructor from non-Field.
     * @param field field
     * @param elements equinoctial elements
     */
    public FieldEquinoctialParameters(final Field<T> field, final EquinoctialParameters elements) {
        this(field.getZero().newInstance(elements.a()), field.getZero().newInstance(elements.ex()),
                field.getZero().newInstance(elements.ey()), field.getZero().newInstance(elements.hx()),
                field.getZero().newInstance(elements.hy()), field.getZero().newInstance(elements.longitudeArgument()),
                elements.positionAngleType());
    }

    /**
     * Builds a new instance with the specified position angle type.
     * @param angleType angle type for the output
     * @return equinoctial elements with the specified position angle type
     */
    public FieldEquinoctialParameters<T> withPositionAngleType(final PositionAngleType angleType) {
        final T convertedAnomaly = FieldEquinoctialLongitudeArgumentUtility.convertL(positionAngleType, longitudeArgument,
                ex, ey, angleType);
        return new FieldEquinoctialParameters<>(a, ex, ey, hx, hy, convertedAnomaly, angleType);
    }

    /**
     * Convert Field elements to non-Field ones.
     * @return equinoctial elements
     */
    public EquinoctialParameters toEquinoctialElements() {
        return new EquinoctialParameters(a.getReal(), ex.getReal(), ey.getReal(), hx.getReal(), hy.getReal(),
                longitudeArgument.getReal(), positionAngleType);
    }
}
