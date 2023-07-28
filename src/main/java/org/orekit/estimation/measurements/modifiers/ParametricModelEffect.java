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
package org.orekit.estimation.measurements.modifiers;

import org.orekit.estimation.measurements.GroundStation;
import org.orekit.propagation.SpacecraftState;

/** Functional interface for parameteric models.
 * @author Luc Maisonobe
 * @since 11.2
 */
@FunctionalInterface
interface ParametricModelEffect {

    /** Evaluate the parametric model effect.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to parametric model
     */
    double evaluate(GroundStation station, SpacecraftState state);

}
