/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.conversion.osc2mean;

import org.hipparchus.CalculusFieldElement;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;

/**
 * Interface for theories that convert osculating into mean orbit.
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public interface MeanTheory {

    /** Gets the name of the theory used for osculating to mean conversion.
     * @return the actual theory
     */
    String getTheoryName();

    /** Gets reference radius of the central body (m).
     * @return reference radius of the central body
     */
    double getReferenceRadius();

    /** Pre-treatment of the osculating orbit to be converted.
     * <p>By default, no pre-treatment is applied to the osculating orbit.</p>
     * @param osculating the osculating orbit to be treated
     * @return preprocessed osculating orbit
     */
    default Orbit preprocessing(Orbit osculating) {
        return osculating;
    }

    /** Rough initialization of the mean orbit.
     * <p>By default, the mean orbit is initialized with the osculating orbit.</p>
     * @param osculating the osculating orbit
     * @return initial mean orbit
     */
    default Orbit initialize(Orbit osculating) {
        return osculating;
    }

    /** Gets osculating orbit from mean orbit.
     * @param mean mean orbit
     * @return osculating orbit
     */
    Orbit meanToOsculating(Orbit mean);

    /** Post-treatment of the converted mean orbit.
     * <p>By default, the mean orbit returned is of the same
     * type as the osculating orbit to be converted.</p>
     * @param osculating the osculating orbit to be converted
     * @param mean the converted mean orbit
     * @return postprocessed mean orbit
     */
    default Orbit postprocessing(Orbit osculating, Orbit mean) {
        return osculating.getType().convertType(mean);
    }

    /** Pre-treatment of the osculating orbit to be converted.
     * <p>By default, no pre-treatment is applied to the osculating orbit.</p>
     * @param <T> type of the field elements
     * @param osculating the osculating orbit to be treated
     * @return preprocessed osculating orbit
     */
    default <T extends CalculusFieldElement<T>> FieldOrbit<T> preprocessing(FieldOrbit<T> osculating) {
        return osculating;
    }

    /** Rough initialization of the mean orbit.
     * <p>By default, the mean orbit is initialized with the osculating orbit.</p>
     * @param <T> type of the field elements
     * @param osculating the osculating orbit
     * @return initial mean orbit
     */
    default <T extends CalculusFieldElement<T>> FieldOrbit<T> initialize(FieldOrbit<T> osculating) {
        return osculating;
    }

    /** Gets osculating orbit from mean orbit.
     * @param <T> type of the field elements
     * @param mean mean orbit
     * @return osculating orbit
     */
    <T extends CalculusFieldElement<T>> FieldOrbit<T> meanToOsculating(FieldOrbit<T> mean);

    /** Post-treatment of the converted mean orbit.
     * <p>By default, the mean orbit returned is of the same
     * type as the osculating orbit to be converted.</p>
     * @param <T> type of the field elements
     * @param osculating the osculating orbit to be converted
     * @param mean the converted mean orbit
     * @return postprocessed mean orbit
     */
    default <T extends CalculusFieldElement<T>> FieldOrbit<T> postprocessing(FieldOrbit<T> osculating,
                                                                             FieldOrbit<T> mean) {
        return osculating.getType().convertType(mean);
    }

}
