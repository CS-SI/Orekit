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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;

/** Container for the {@link ViennaOne} and {@link ViennaThree} coefficients a<sub>h</sub> and a<sub>w</sub>.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldViennaACoefficients<T extends CalculusFieldElement<T>> {

    /** Hydrostatic coefficient. */
    private final T ah;

    /** Wet coefficient. */
    private final T aw;

    /** Simple constructor.
     * @param ah hydrostatic coefficient
     * @param aw wet coefficient
     */
    public FieldViennaACoefficients(final T ah, final T aw) {
        this.ah = ah;
        this.aw = aw;
    }

    /** Get hydrostatic coefficient.
     * @return hydrostatic coefficient
     */
    public T getAh() {
        return ah;
    }

    /** Get wet coefficient.
     * @return wet coefficient
     */
    public T getAw() {
        return aw;
    }

}
