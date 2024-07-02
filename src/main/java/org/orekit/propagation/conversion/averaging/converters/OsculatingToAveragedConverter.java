/* Copyright 2020-2024 Exotrail
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
package org.orekit.propagation.conversion.averaging.converters;

import org.orekit.orbits.Orbit;
import org.orekit.propagation.conversion.averaging.AveragedOrbitalState;

/**
 * Interface for osculating-to-averaged converters.
 *
 * @author Romain Serra
 * @since 12.1
 * @see AveragedOrbitalState
 * @param <T> type of averaged orbital state
 */
public interface OsculatingToAveragedConverter<T extends AveragedOrbitalState> {

    /**
     * Convert osculating orbit to averaged orbital state according to underlying theory.
     * @param osculatingOrbit osculating orbit
     * @return averaged orbital state
     */
    T convertToAveraged(Orbit osculatingOrbit);

}
