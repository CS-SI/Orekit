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

import org.hipparchus.CalculusFieldElement;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Provider for {@link ViennaOne} and {@link ViennaThree} coefficients a<sub>h</sub> and a<sub>w</sub>.
 * @since 12.1
 * @author Luc Maisonobe
 */
public interface ViennaAProvider {

    /** Get coefficients array for VMF mapping function.
     * <ul>
     * <li>double[0] = a<sub>h</sub>
     * <li>double[1] = a<sub>w</sub>
     * </ul>
     * @param location location at which parameters are requested
     * @param date date at which parameters are requested
     * @return the coefficients array for VMF mapping function
     */
    ViennaACoefficients getA(GeodeticPoint location, AbsoluteDate date);

    /** Get coefficients array for VMF mapping function.
     * <ul>
     * <li>double[0] = a<sub>h</sub>
     * <li>double[1] = a<sub>w</sub>
     * </ul>
     * @param <T> type of the field elements
     * @param location location at which parameters are requested
     * @param date date at which parameters are requested
     * @return the coefficients array for VMF mapping function
     */
    <T extends CalculusFieldElement<T>> FieldViennaACoefficients<T> getA(FieldGeodeticPoint<T> location,
                                                                         FieldAbsoluteDate<T> date);

}
