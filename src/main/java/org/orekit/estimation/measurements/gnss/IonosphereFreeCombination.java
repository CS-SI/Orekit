/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.hipparchus.util.MathArrays;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;

/**
 * Ionosphere-free combination.
 * <p>
 * This combination removes the first order (up to 99.9%)
 * ionospheric effect.
 * </p>
 * <pre>
 *             f1² * m1 - f2² * m2
 *    mIF =  -----------------------
 *                  f1² - f2²
 * </pre>
 * With:
 * <ul>
 * <li>mIF: Ionosphere-free measurement.</li>
 * <li>f1 : Frequency of the first measurement.</li>
 * <li>m1 : First measurement.</li>
 * <li>f2 : Frequency of the second measurement.</li>
 * <li>m1 : Second measurement.</li>
 * </ul>
 * <p>
 * Ionosphere-free combination is a dual frequency combination.
 * The two measurements shall have different frequencies but they must have the same {@link MeasurementType}.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class IonosphereFreeCombination extends AbstractDualFrequencyCombination {

    /**
     * Package private constructor for the factory.
     * @param system satellite system for wich the combination is applied
     */
    IonosphereFreeCombination(final SatelliteSystem system) {
        super(CombinationType.IONO_FREE, system);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedValue(final double obs1, final double f1,
                                      final double obs2, final double f2) {
        final double f1Sq = f1 * f1;
        final double f2Sq = f2 * f2;
        return MathArrays.linearCombination(f1Sq, obs1, -f2Sq, obs2) / (f1Sq - f2Sq);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedFrequency(final double f1, final double f2) {
        final double f1Sq = f1 * f1;
        final double f2Sq = f2 * f2;
        return FastMath.abs(f1Sq - f2Sq) / Frequency.F0;
    }

}
