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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;

/**
 * Interface for mapping functions used in the ionospheric delay computation.
 * <p>
 * The purpose of an ionospheric mapping function is to convert the
 * Vertical Total Electron Content (VTEC) to a Slant Total Electron Content (STEC)
 * using the following formula:
 * </p> <pre>
 * STEC = VTEC * m(e)
 * </pre> <p>
 * With m(e) the ionospheric mapping function and e the satellite elevation.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.2
 */
public interface IonosphericMappingFunction {

    /**
     * This method allows the computation of the ionospheric mapping factor.
     * @param elevation the elevation of the satellite, in radians.
     * @return the ionospheric mapping factor.
     */
    double mappingFactor(double elevation);

    /**
     * This method allows the computation of the ionospheric mapping factor.
     * @param elevation the elevation of the satellite, in radians.
     * @param <T> type of the elements
     * @return the ionospheric mapping factor.
     */
    <T extends CalculusFieldElement<T>> T mappingFactor(T elevation);

}
