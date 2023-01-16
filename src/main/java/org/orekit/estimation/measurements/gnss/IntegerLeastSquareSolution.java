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

/** Class holding a solution to an Integer Least Square problem.
 * @author Luc Maisonobe
 * @since 10.0
 */
public class IntegerLeastSquareSolution {

    /** Solution array. */
    private final long[] solution;

    /** Squared distance to the corresponding float solution. */
    private final double d2;

    /** Simple constructor.
     * @param solution solution array
     * @param d2 squared distance to the corresponding float solution
     */
    public IntegerLeastSquareSolution(final long[] solution, final double d2) {
        this.solution = solution.clone();
        this.d2       = d2;
    }

    /** Get the solution array.
     * @return solution array
     */
    public long[] getSolution() {
        return solution.clone();
    }

    /** Get the squared distance to the corresponding float solution.
     * @return squared distance to the corresponding float solution
     */
    public double getSquaredDistance() {
        return d2;
    }

}
