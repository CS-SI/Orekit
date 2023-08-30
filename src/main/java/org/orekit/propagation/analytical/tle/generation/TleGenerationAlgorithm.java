/* Copyright 2023 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.tle.generation;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.TLE;

/**
 * This interface provides a way to generate a TLE from a spacecraft state.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public interface TleGenerationAlgorithm {

    /**
     * Generate a TLE from a given spacecraft state and a template TLE.
     * <p>
     * The template TLE is only used to get identifiers like satellite
     * number, launch year, etc.
     * In other words, the keplerian elements contained in the generate
     * TLE a based on the provided state and not the template TLE.
     * </p>
     * @param state spacecraft state
     * @param templateTLE template TLE
     * @return a TLE corresponding to the given state
     */
    TLE generate(SpacecraftState state, TLE templateTLE);

    /**
     * Generate a TLE from a given spacecraft state and a template TLE.
     * <p>
     * The template TLE is only used to get identifiers like satellite
     * number, launch year, etc.
     * In other words, the keplerian elements contained in the generate
     * TLE a based on the provided state and not the template TLE.
     * </p>
     * @param <T> type of the elements
     * @param state spacecraft state
     * @param templateTLE template TLE
     * @return a TLE corresponding to the given state
     */
    <T extends CalculusFieldElement<T>> FieldTLE<T> generate(FieldSpacecraftState<T> state,
                                                             FieldTLE<T> templateTLE);

}
