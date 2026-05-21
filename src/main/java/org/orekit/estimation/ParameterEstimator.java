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
package org.orekit.estimation;

import java.util.Arrays;

import org.orekit.propagation.conversion.PropagatorBuilder;
import org.orekit.utils.ParameterDriversList;

/**
 * Interface for parameter estimation.
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 14.0
 */
public interface ParameterEstimator {

    /**
     * Getter for the propagator builders.
     * @return builders
     */
    PropagatorBuilder[] getPropagatorBuilders();

    /** Get the orbital parameters supported by this estimator.
     * <p>
     * If there are more than one propagator builder, then the names
     * of the drivers have an index marker in square brackets appended
     * to them in order to distinguish the various orbits. So for example
     * with one builder generating Keplerian orbits the names would be
     * simply "a", "e", "i"... but if there are several builders the
     * names would be "a[0]", "e[0]", "i[0]"..."a[1]", "e[1]", "i[1]"...
     * </p>
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return orbital parameters supported by this estimator
     */
    default ParameterDriversList getOrbitalParametersDrivers(final boolean estimatedOnly) {
        final ParameterDriversList estimated = new ParameterDriversList();
        final PropagatorBuilder[] propagatorBuilders = getPropagatorBuilders();
        for (int i = 0; i < propagatorBuilders.length; ++i) {
            final String suffix = propagatorBuilders.length > 1 ? "[" + i + "]" : null;
            propagatorBuilders[i].getOrbitalParametersDrivers().getDrivers().stream()
                    .filter(delegatingDriver -> delegatingDriver.isSelected() || !estimatedOnly)
                    .forEach(delegatingDriver -> {
                        if (suffix != null && !delegatingDriver.getName().endsWith(suffix)) {
                            // we add suffix only conditionally because the method may already have been called
                            // and suffixes may have already been appended
                            delegatingDriver.setName(delegatingDriver.getName() + suffix);
                        }
                        estimated.add(delegatingDriver);
                    });
        }
        return estimated;
    }

    /** Get the propagation parameters supported by this estimator.
     * @param estimatedOnly if true, only estimated parameters are returned
     * @return propagation parameters supported by this estimator
     */
    default ParameterDriversList getPropagationParametersDrivers(final boolean estimatedOnly) {
        final ParameterDriversList estimated = new ParameterDriversList();
        Arrays.stream(getPropagatorBuilders())
                .flatMap(propagatorBuilder -> propagatorBuilder.getPropagationParametersDrivers().getDrivers().stream())
                .filter(delegatingDriver -> delegatingDriver.isSelected() || !estimatedOnly)
                .flatMap(delegatingDriver -> delegatingDriver.getRawDrivers().stream())
                .forEach(estimated::add);
        return estimated;
    }
}
