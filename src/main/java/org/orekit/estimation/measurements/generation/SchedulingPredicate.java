/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.generation;

import org.orekit.propagation.SpacecraftState;


/** Interface for checking measurements feasibility, used by {@link PredicateScheduler}.
 * @author Luc Maisonobe
 * @since 9.3
 */
public interface SchedulingPredicate {

    /** Create a new scheduling predicate that is the logical AND of the given scheduling predicates.
     * @param predicates scheduling predicates to combine
     * @return a new scheduling predicate that is the logical AND of the given scheduling predicates
     */
    static SchedulingPredicate andCombine(final SchedulingPredicate... predicates) {
        return new SchedulingPredicate() {
            /** {@inheritDoc} */
            @Override
            public boolean feasibleMeasurement(final SpacecraftState... states) {
                for (SchedulingPredicate predicate : predicates) {
                    if (!predicate.feasibleMeasurement(states)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /** Create a new scheduling predicate that is the logical OR of the given scheduling predicates.
     * @param predicates scheduling predicates to combine
     * @return a new scheduling predicate that is the logical OR of the given scheduling predicates
     */
    static SchedulingPredicate orCombine(final SchedulingPredicate... predicates) {
        return new SchedulingPredicate() {
            /** {@inheritDoc} */
            @Override
            public boolean feasibleMeasurement(final SpacecraftState... states) {
                for (SchedulingPredicate predicate : predicates) {
                    if (predicate.feasibleMeasurement(states)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /** Create a new scheduling predicate that is the logical NOT of the given scheduling predicate.
     * @param predicate scheduling predicate to reverse
     * @return a new scheduling predicate that is the logical NOT of the given scheduling predicate
     */
    static SchedulingPredicate notCombine(final SchedulingPredicate predicate) {
        return new SchedulingPredicate() {
            /** {@inheritDoc} */
            @Override
            public boolean feasibleMeasurement(final SpacecraftState... states) {
                return !predicate.feasibleMeasurement(states);
            }
        };
    }

    /** Check if a measurement is feasible for the specified states.
     * @param states spacecraft states
     * @return true if a measurement is feasible for the specified states
     */
    boolean feasibleMeasurement(SpacecraftState... states);

}
