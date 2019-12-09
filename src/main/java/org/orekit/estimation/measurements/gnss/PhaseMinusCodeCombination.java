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

import org.orekit.gnss.SatelliteSystem;

/**
 * Phase minus Code combination.
 * <p>
 * This combination is a single frequency combination of
 * measurements that can be used for cycle-slip detection.
 * </p>
 * <pre>
 *    m<sub>f</sub> =  Φ<sub>f</sub> - R<sub>f</sub>
 * </pre>
 * With:
 * <ul>
 * <li>m<sub>f</sub> : Phase minus Code measurement.</li>
 * <li>Φ<sub>f</sub> : Phase measurement.</li>
 * <li>R<sub>f</sub> : Code measurement.</li>
 * <li>f             : Frequency.</li>
 * </ul>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class PhaseMinusCodeCombination extends AbstractSingleFrequencyCombination {

    /**
     * Package private constructor for the factory.
     * @param system satellite system for wich the combination is applied
     */
    PhaseMinusCodeCombination(final SatelliteSystem system) {
        super(CombinationType.PHASE_MINUS_CODE, system);
    }

    /** {@inheritDoc} */
    @Override
    protected double getCombinedValue(final double phase, final double pseudoRange, final double f) {
        // Combination does not depend on the frequency
        return phase - pseudoRange;
    }

}
