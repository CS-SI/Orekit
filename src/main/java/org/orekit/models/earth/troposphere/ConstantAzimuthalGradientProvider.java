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
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Constant provider for {@link AzimuthalGradientCoefficients} and {@link FieldAzimuthalGradientCoefficients}.
 * @since 12.1
 * @author Luc Maisonobe
 */
public class ConstantAzimuthalGradientProvider implements AzimuthalGradientProvider {

    /** Constant parameters. */
    private final AzimuthalGradientCoefficients a;

    /** Simple constructor.
     * @param a constant parameters (may be null if no gradients are available)
     */
    public ConstantAzimuthalGradientProvider(final AzimuthalGradientCoefficients a) {
        this.a = a;
    }

    /** {@inheritDoc} */
    @Override
    public AzimuthalGradientCoefficients getGradientCoefficients(final GeodeticPoint location,
                                                                 final AbsoluteDate date) {
        return a;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldAzimuthalGradientCoefficients<T> getGradientCoefficients(final FieldGeodeticPoint<T> location,
                                                                                                             final FieldAbsoluteDate<T> date) {
        final T zero = date.getField().getZero();
        return a == null ?
                    null :
                    new FieldAzimuthalGradientCoefficients<>(zero.newInstance(a.getGnh()),
                                                             zero.newInstance(a.getGeh()),
                                                             zero.newInstance(a.getGnw()),
                                                             zero.newInstance(a.getGew()));
    }

}
