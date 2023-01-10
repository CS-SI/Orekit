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

import org.orekit.gnss.SatelliteSystem;

/**
 * GRoup And Phase Ionospheric Calibration (GRAPHIC) combination.
 * <p>
 * This combination is a ionosphere-free single frequency
 * combination of measurements.
 * </p>
 * <pre>
 *    mf =  0.5 * (Φf + Rf)
 * </pre>
 * With:
 * <ul>
 * <li>mf : GRAPHIC measurement.</li>
 * <li>Φf : Phase measurement.</li>
 * <li>Rf : Code measurement.</li>
 * <li>f  : Frequency.</li>
 * </ul>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class GRAPHICCombination extends AbstractSingleFrequencyCombination {

    /**
     * Package private constructor for the factory.
     * @param system satellite system for which the combination is applied
     */
    GRAPHICCombination(final SatelliteSystem system) {
        super(CombinationType.GRAPHIC, system);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedValue(final double phase, final double pseudoRange) {
        // Combination does not depend on the frequency
        return 0.5 * (phase + pseudoRange);
    }

}
