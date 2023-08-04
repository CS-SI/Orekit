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

/** This interface represents an event enabling predicate function.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.0
 */
public interface FieldEnablingPredicate<T extends CalculusFieldElement<T>> {

    /** Compute an event enabling function of state.
     * @param state current state
     * @param detector underlying detector
     * @param g value of the underlying detector for the current state
     * @return true if the event is enabled (i.e. it can be
     * triggered), false if it should be ignored
     */
    boolean eventIsEnabled(FieldSpacecraftState<T> state, FieldEventDetector<T> detector, T g);

}
