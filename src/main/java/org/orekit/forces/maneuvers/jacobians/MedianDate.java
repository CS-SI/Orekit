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
package org.orekit.forces.maneuvers.jacobians;

import org.orekit.propagation.AdditionalStateProvider;
import org.orekit.propagation.SpacecraftState;

/** Generator for one column of a Jacobian matrix for special case of maneuver median date.
 * <p>
 * Typical use cases for this are estimation of maneuver median date during
 * either orbit determination or maneuver optimization.
 * </p>
 * @author Luc Maisonobe
 * @since 11.1
 * @see Duration
 * @see TriggerDate
 */
public class MedianDate implements AdditionalStateProvider {

    /** Name of the parameter corresponding to the start date. */
    private final String startName;

    /** Name of the parameter corresponding to the stop date. */
    private final String stopName;

    /** Name of the parameter corresponding to the column. */
    private final String columnName;

    /** Simple constructor.
     * @param startName name of the parameter corresponding to the start date
     * @param stopName name of the parameter corresponding to the stop date
     * @param columnName name of the parameter corresponding to the column
     */
    public MedianDate(final String startName, final String stopName, final String columnName) {
        this.startName  = startName;
        this.stopName   = stopName;
        this.columnName = columnName;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return columnName;
    }

    /** {@inheritDoc}
     * <p>
     * The column state can be computed only if the start and stop dates columns are available.
     * </p>
     */
    @Override
    public boolean yields(final SpacecraftState state) {
        return !(state.hasAdditionalState(startName) && state.hasAdditionalState(stopName));
    }

    /** {@inheritDoc} */
    @Override
    public double[] getAdditionalState(final SpacecraftState state) {

        // compute partial derivatives with respect to start and stop dates
        final double[] dYdT0 = state.getAdditionalState(startName);
        final double[] dYdT1 = state.getAdditionalState(stopName);

        // combine derivatives to get partials with respect to median date
        final double[] dYdTm = new double[dYdT0.length];
        for (int i = 0; i < dYdTm.length; ++i) {
            dYdTm[i] = dYdT0[i] + dYdT1[i];
        }
        return dYdTm;

    }

}

