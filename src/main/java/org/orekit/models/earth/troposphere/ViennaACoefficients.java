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
package org.orekit.models.earth.troposphere;

/** Container for the {@link ViennaOne} and {@link ViennaThree} coefficients a<sub>h</sub> and a<sub>w</sub>.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ViennaACoefficients {

    /** Hydrostatic coefficient. */
    private final double ah;

    /** Wet coefficient. */
    private final double aw;

    /** Simple constructor.
     * @param ah hydrostatic coefficient
     * @param aw wet coefficient
     */
    public ViennaACoefficients(final double ah, final double aw) {
        this.ah = ah;
        this.aw = aw;
    }

    /** Get hydrostatic coefficient.
     * @return hydrostatic coefficient
     */
    public double getAh() {
        return ah;
    }

    /** Get wet coefficient.
     * @return wet coefficient
     */
    public double getAw() {
        return aw;
    }

}
