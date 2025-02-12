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
 * Interface for osculating to mean orbit converters.
 * <p>An osculating-to-mean converter consists of:
 * <ul>
 * <li>an algorithm performing the conversion,</li>
 * <li>a theory giving the meaning of the mean orbit.</li>
 * </ul>
 *
 * @author Pascal Parraud
 * @since 13.0
 */
public interface OsculatingToMeanConverter {

    /**
     * Gets the theory defining the mean orbit.
     * @return the mean theory
     */
    MeanTheory getMeanTheory();

    /**
     * Converts an osculating orbit into a mean orbit.
     * @param osculating osculating orbit
     * @return mean orbit
     */
    Orbit convertToMean(Orbit osculating);

    /**
     * Converts an osculating orbit into a mean orbit.
     * @param <T> type of the filed elements
     * @param osculating osculating orbit
     * @return mean orbit
     */
    <T extends CalculusFieldElement<T>> FieldOrbit<T> convertToMean(FieldOrbit<T> osculating);

}
