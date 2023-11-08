/* Copyright 2002-2023 CS GROUP
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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.util.FastMath;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;

/**
 * Geometry-free combination.
 * <p>
 * This combination removes the geometry part of the measurement.
 * It can be used to estimate the ionospheric electron content or to detect
 * cycle slips in the carrier phase, as well.
 * </p>
 * <pre>
 *    mGF =  m2 - m1
 * </pre>
 * With:
 * <ul>
 * <li>mGF: Geometry-free measurement.</li>
 * <li>m1 : First measurement.</li>
 * <li>m2 : Second measurement.</li>
 * </ul>
 * <p>
 * Geometry-Free combination is a dual frequency combination.
 * The two measurements shall have different frequencies but they must have the same {@link MeasurementType}.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class GeometryFreeCombination extends AbstractDualFrequencyCombination {

    /**
     * Package private constructor for the factory.
     * @param system satellite system for which the combination is applied
     */
    GeometryFreeCombination(final SatelliteSystem system) {
        super(CombinationType.GEOMETRY_FREE, system);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedValue(final double obs1, final Frequency f1,
                                      final double obs2, final Frequency f2) {
        // Combined observed value does not depend on frequency for the Geometry-Free combination
        return FastMath.abs(obs2 - obs1);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedFrequency(final Frequency f1, final Frequency f2) {
        // There is not combined frequency for the Geometry-Free combination
        return Double.NaN;
    }

}
