/* Copyright 2022-2026 Romain Serra
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

package org.orekit.propagation.events.intervals;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;

/** This class defines an interval based on the osculating orbital period if available.
*
* @see FieldAdaptableInterval
* @see PeriodBasedAdaptableInterval
* @author Romain Serra
* @since 14.0
*/
public class FieldPeriodBasedAdaptableInterval<T extends CalculusFieldElement<T>> implements FieldAdaptableInterval<T> {

    /** Multiplying factor on orbital period to define interval. */
    private final double factor;

    /** Minimum value and default one for interval size when not period is defined. */
    private final double defaultMaxCheck;

    /**
     * Constructor.
     * @param factor multiplying factor on the orbital, Keplerian period
     * @param defaultMaxCheck default value in case the period is not defined, also used as minimum
     */
    public FieldPeriodBasedAdaptableInterval(final double factor, final double defaultMaxCheck) {
        this.factor = factor;
        this.defaultMaxCheck = defaultMaxCheck;
    }

    @Override
    public double currentInterval(final FieldSpacecraftState<T> state, final boolean isForward) {
        double maxCheck = defaultMaxCheck;
        if (state.isOrbitDefined()) {
            final FieldOrbit<T> orbit = state.getOrbit();
            if (orbit.isElliptical()) {
                maxCheck = FastMath.min(maxCheck, factor * orbit.getKeplerianPeriod().getReal());
            }
        }
        return maxCheck;
    }
}
