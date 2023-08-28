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

import org.hipparchus.util.MathArrays;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;

/**
 * Narrow-Lane combination.
 * <p>
 * This combination create signal with a narrow wavelength.
 * The signal in this combination has a lower noise than each
 * separated separeted component.
 * </p>
 * <pre>
 *              f1 * m1 + f2 * m2
 *    mNL =  -----------------------
 *                   f1 + f2
 * </pre>
 * With:
 * <ul>
 * <li>mNL : Narrow-laning measurement.</li>
 * <li>f1  : Frequency of the first measurement.</li>
 * <li>pr1 : First measurement.</li>
 * <li>f2  : Frequency of the second measurement.</li>
 * <li>m1 : Second measurement.</li>
 * </ul>
 * <p>
 * Narrow-Lane combination is a dual frequency combination.
 * The two measurements shall have different frequencies but they must have the same {@link MeasurementType}.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class NarrowLaneCombination extends AbstractDualFrequencyCombination {

    /**
     * Package private constructor for the factory.
     * @param system satellite system for which the combination is applied
     */
    NarrowLaneCombination(final SatelliteSystem system) {
        super(CombinationType.NARROW_LANE, system);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedValue(final double obs1, final Frequency f1,
                                      final double obs2, final Frequency f2) {
        // Get the ration f/f0
        final double ratioF1 = f1.getRatio();
        final double ratioF2 = f2.getRatio();
        // Perform combination
        return MathArrays.linearCombination(ratioF1, obs1, ratioF2, obs2) / (ratioF1 + ratioF2);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedFrequency(final Frequency f1, final Frequency f2) {
        return f1.getMHzFrequency() + f2.getMHzFrequency();
    }

}
