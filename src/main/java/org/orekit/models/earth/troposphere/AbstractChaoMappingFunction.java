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
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

/** Chao mapping function for radio wavelengths.
 *
 * @see "C. C. Chao, A model for tropospheric calibration from delay surface and radiosonde ballon measurements, 1972"
 *
 * @author Luc Maisonobe
 * @since 12.1
 */
public class AbstractChaoMappingFunction implements TroposphereMappingFunction {

    /** First coefficient for hydrostatic (dry) component. */
    private final double ad;

    /** Second coefficient for hydrostatic (dry) component. */
    private final double bd;

    /** First coefficient for wet component. */
    private final double aw;

    /** Second coefficient for wet component. */
    private final double bw;

    /** Builds a new instance.
     * @param ad first coefficient for hydrostatic (dry) component
     * @param bd second coefficient for hydrostatic (dry) component
     * @param aw first coefficient for wet component
     * @param bw second coefficient for wet component
     */
    protected AbstractChaoMappingFunction(final double ad, final double bd, final double aw, final double bw) {
        this.ad = ad;
        this.bd = bd;
        this.aw = aw;
        this.bw = bw;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                   final AbsoluteDate date) {
        final double sinE = FastMath.sin(trackingCoordinates.getElevation());
        final double tanE = FastMath.tan(trackingCoordinates.getElevation());
        return new double[] {
            1 / (sinE + ad / (tanE + bd)),
            1 / (sinE + aw / (tanE + bw))
        };
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                  final FieldGeodeticPoint<T> point,
                                                                  final FieldAbsoluteDate<T> date) {
        final T sinE = FastMath.sin(trackingCoordinates.getElevation());
        final T tanE = FastMath.tan(trackingCoordinates.getElevation());
        final T[] mapping = MathArrays.buildArray(date.getField(), 2);
        mapping[0] = sinE.add(tanE.add(bd).reciprocal().multiply(ad)).reciprocal();
        mapping[1] = sinE.add(tanE.add(bw).reciprocal().multiply(aw)).reciprocal();
        return mapping;
    }

}
