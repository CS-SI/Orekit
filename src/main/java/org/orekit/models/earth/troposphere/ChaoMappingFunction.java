/* Copyright 2023 Thales Alenia Space
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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Chao mapping function for radio wavelengths.
 *
 * @see "C. C. Chao, A model for tropospheric calibration from delay surface and radiosonde ballon measurements, 1972"
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ChaoMappingFunction implements MappingFunction {

    /** First coefficient for hydrostatic (dry) component. */
    private static final double AD = 0.00143;

    /** Second coefficient for hydrostatic (dry) component. */
    private static final double BD = 0.0445;

    /** First coefficient for wet component. */
    private static final double AW = 0.00035;

    /** Second coefficient for wet component. */
    private static final double BW = 0.017;

    /** Builds a new instance.
     */
    public ChaoMappingFunction() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final GeodeticPoint point,
                                   final AbsoluteDate date) {
        final double sinE = FastMath.sin(elevation);
        final double tanE = FastMath.tan(elevation);
        return new double[] {
            1 / (sinE + AD / (tanE + BD)),
            1 / (sinE + AW / (tanE + BW))
        };
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final T elevation, final FieldGeodeticPoint<T> point,
                                                                  final FieldAbsoluteDate<T> date) {
        final T sinE = FastMath.sin(elevation);
        final T tanE = FastMath.tan(elevation);
        final T[] mapping = MathArrays.buildArray(date.getField(), 2);
        mapping[0] = sinE.add(tanE.add(BD).reciprocal().multiply(AD)).reciprocal();
        mapping[1] = sinE.add(tanE.add(BW).reciprocal().multiply(AW)).reciprocal();
        return mapping;
    }

}
