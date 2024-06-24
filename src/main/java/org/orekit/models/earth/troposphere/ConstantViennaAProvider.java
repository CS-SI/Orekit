/* Copyright 2002-2024 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

/** Provider for constant Vienna A coefficients.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ConstantViennaAProvider implements ViennaAProvider {

    /** Constant parameters. */
    private final ViennaACoefficients a;

    /** Simple constructor.
     * @param a constant parameters
     */
    public ConstantViennaAProvider(final ViennaACoefficients a) {
        this.a = a;
    }

    /** {@inheritDoc} */
    @Override
    public ViennaACoefficients getA(final GeodeticPoint location, final AbsoluteDate date) {
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldViennaACoefficients<T> getA(final FieldGeodeticPoint<T> location,
                                                                                final FieldAbsoluteDate<T> date) {
        return new FieldViennaACoefficients<T>(date.getField().getZero().newInstance(a.getAh()),
                                               date.getField().getZero().newInstance(a.getAw()));
    }

}
