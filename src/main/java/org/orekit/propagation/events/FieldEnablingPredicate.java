/* Copyright 2022-2026 Luc Maisonobe
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

import java.util.Arrays;

import org.hipparchus.CalculusFieldElement;
import org.orekit.propagation.FieldSpacecraftState;

/** This interface represents an event enabling predicate function.
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.0
 * @see EnablingPredicate
 */
@FunctionalInterface
public interface FieldEnablingPredicate<T extends CalculusFieldElement<T>> {

    /** Compute an event enabling function of state.
     * @param state current state
     * @param detector underlying detector
     * @param g value of the underlying detector for the current state
     * @return true if the event is enabled (i.e. it can be
     * triggered), false if it should be ignored
     */
    boolean eventIsEnabled(FieldSpacecraftState<T> state, FieldEventDetector<T> detector, T g);

    /**
     * Method returning true if and only if the predicate does not depend on dependent variables,
     * other than the Cartesian coordinates (or equivalent), mass and attitude (excepts for its rates).
     * It should thus return false if the STM or other secondary variables are needed to evaluate the predicate.
     * This information is used for performance in propagation. The default implementation returns false.
     * @return flag
     * @since 14.0
     */
    default boolean dependsOnMainVariablesOnly() {
        return false;
    }

    /**
     * Method combining predicated based on the OR logic operator.
     * @param enablingPredicates predicates
     * @param <T> field type
     * @return combined predicate
     * @since 13.1
     */
    @SafeVarargs
    static <T extends CalculusFieldElement<T>> FieldEnablingPredicate<T> orCombine(final FieldEnablingPredicate<T>... enablingPredicates) {
        return new FieldEnablingPredicate<T>() {
            @Override
            public boolean eventIsEnabled(final FieldSpacecraftState<T> state, final FieldEventDetector<T> detector, final T g) {
                return Arrays.stream(enablingPredicates).anyMatch(p -> p.eventIsEnabled(state, detector, g));
            }

            @Override
            public boolean dependsOnMainVariablesOnly() {
                return Arrays.stream(enablingPredicates).allMatch(FieldEnablingPredicate::dependsOnMainVariablesOnly);
            }
        };
    }

    /**
     * Method combining predicated based on the AND logic operator.
     * @param enablingPredicates predicates
     * @param <T> field type
     * @return combined predicate
     * @since 13.1
     */
    @SafeVarargs
    static <T extends CalculusFieldElement<T>> FieldEnablingPredicate<T> andCombine(final FieldEnablingPredicate<T>... enablingPredicates) {
        return new FieldEnablingPredicate<T>() {
            @Override
            public boolean eventIsEnabled(final FieldSpacecraftState<T> state, final FieldEventDetector<T> detector, final T g) {
                return Arrays.stream(enablingPredicates).allMatch(p -> p.eventIsEnabled(state, detector, g));
            }

            @Override
            public boolean dependsOnMainVariablesOnly() {
                return Arrays.stream(enablingPredicates).allMatch(FieldEnablingPredicate::dependsOnMainVariablesOnly);
            }
        };
    }
}
