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
package org.orekit.estimation.measurements.gnss;

/** Interface defining ambiguity acceptance tests.
 * @see AmbiguitySolver
 * @author Luc Maisonobe
 * @since 10.0
 */
public interface AmbiguityAcceptance {

    /** Get the number of candidate solutions to search for.
     * @return number of candidate solutions to search for
     */
    int numberOfCandidates();

    /** Check if one of the candidate solutions can be accepted.
     * @param candidates candidate solutions of the Integer Least Squares problem,
     * in increasing squared distance order (the array contains at least
     * {@link #numberOfCandidates()} candidates)
     * @return the candidate solution to accept (normally the one at index 0), or
     * null if we should still use the float solution
     */
    IntegerLeastSquareSolution accept(IntegerLeastSquareSolution[] candidates);

}
