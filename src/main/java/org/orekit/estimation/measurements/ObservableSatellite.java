/* Copyright 2002-2026 CS GROUP
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
package org.orekit.estimation.measurements;

import org.orekit.utils.ParameterDriversProvider;

/** Class modeling a satellite that can be observed.
 *
 * @author Luc Maisonobe
 * @since 9.3
 */
public class ObservableSatellite extends MeasurementObject implements ParameterDriversProvider  {

    /** Prefix for satellite names. */
    private static final String SAT_PREFIX = "sat-";

    /** Index of the propagator related to this satellite. */
    private final int propagatorIndex;

    /** Simple constructor.
     * <p>
     * This constructor builds a default name based on the propagator index.
     * </p>
     * @param propagatorIndex index of the propagator related to this satellite
     */
    public ObservableSatellite(final int propagatorIndex) {
        this(propagatorIndex, null);
    }

    /** Simple constructor.
     * @param propagatorIndex index of the propagator related to this satellite
     * @param name satellite name (if null, a default name built from index will be used)
     * @since 13.0
     */
    public ObservableSatellite(final int propagatorIndex, final String name) {
        super( name == null ? SAT_PREFIX + propagatorIndex : name );
        this.propagatorIndex = propagatorIndex;
    }

    /** Get the index of the propagator related to this satellite.
     * @return index of the propagator related to this satellite
     */
    public int getPropagatorIndex() {
        return propagatorIndex;
    }

    /** {@inheritDoc}
     * @since 12.0
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ObservableSatellite) {
            return propagatorIndex == ((ObservableSatellite) other).propagatorIndex;
        } else {
            return false;

        }
    }

    /** {@inheritDoc}
     * @since 12.0
     */
    @Override
    public int hashCode() {
        return propagatorIndex;
    }

}
