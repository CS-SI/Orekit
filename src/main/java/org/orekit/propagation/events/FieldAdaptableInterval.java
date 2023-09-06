/* Copyright 2023 Luc Maisonobe
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

package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;

/** This interface represents an event checking interval that depends on state.
*
* @see FieldEventDetector
* @author Luc Maisonobe
* @since 12.0
* @param <T> the type of the field elements
*/
@FunctionalInterface
public interface FieldAdaptableInterval<T extends CalculusFieldElement<T>> {

    /** Get the current value of maximal time interval between events handler checks.
     * @param state current state
     * @return current value of maximal time interval between events handler checks (only as a double)
     */
    double currentInterval(FieldSpacecraftState<T> state);

}
