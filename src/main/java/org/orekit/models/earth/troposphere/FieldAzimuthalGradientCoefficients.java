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

/** Container for the azimuthal gradient coefficients gn<sub>h</sub>, ge<sub>h</sub>, gn<sub>w</sub> and ge<sub>w</sub>.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 */
public class FieldAzimuthalGradientCoefficients<T extends CalculusFieldElement<T>> {

    /** North hydrostatic coefficient. */
    private final T gnh;

    /** East hydrostatic coefficient. */
    private final T geh;

    /** North wet coefficient. */
    private final T gnw;

    /** East wet coefficient. */
    private final T gew;

    /** Simple constructor.
     * @param gnh North hydrostatic coefficient
     * @param geh East hydrostatic coefficient
     * @param gnw North wet coefficient
     * @param gew East wet coefficient
     */
    public FieldAzimuthalGradientCoefficients(final T gnh, final T geh,
                                         final T gnw, final T gew) {
        this.gnh = gnh;
        this.geh = geh;
        this.gnw = gnw;
        this.gew = gew;
    }

    /** Get North hydrostatic coefficient.
     * @return North hydrostatic coefficient
     */
    public T getGnh() {
        return gnh;
    }

    /** Get East hydrostatic coefficient.
     * @return East hydrostatic coefficient
     */
    public T getGeh() {
        return geh;
    }

    /** Get North wet coefficient.
     * @return North wet coefficient
     */
    public T getGnw() {
        return gnw;
    }

   /** Get East wet coefficient.
     * @return East wet coefficient
     */
    public T getGew() {
        return gew;
    }

}
