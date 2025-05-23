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
package org.orekit.propagation.events;

import org.orekit.propagation.SpacecraftState;

import java.util.Arrays;

/** This interface represents an event enabling predicate function.
 * @author Luc Maisonobe
 * @since 7.1
 */
public interface EnablingPredicate {

    /** Compute an event enabling function of state.
     * @param state current state
     * @param detector underlying detector
     * @param g value of the underlying detector for the current state
     * @return true if the event is enabled (i.e. it can be
     * triggered), false if it should be ignored
     */
    boolean eventIsEnabled(SpacecraftState state, EventDetector detector, double g);

    /**
     * Method combining predicated based on the OR logic operator.
     * @param enablingPredicates predicates
     * @return combined predicate
     * @since 13.1
     */
    static EnablingPredicate orCombine(EnablingPredicate... enablingPredicates) {
        return (state, detector, g) -> Arrays.stream(enablingPredicates).anyMatch(p -> p.eventIsEnabled(state, detector, g));
    }

    /**
     * Method combining predicated based on the AND logic operator.
     * @param enablingPredicates predicates
     * @return combined predicate
     * @since 13.1
     */
    static EnablingPredicate andCombine(EnablingPredicate... enablingPredicates) {
        return (state, detector, g) -> Arrays.stream(enablingPredicates).allMatch(p -> p.eventIsEnabled(state, detector, g));
    }
}
