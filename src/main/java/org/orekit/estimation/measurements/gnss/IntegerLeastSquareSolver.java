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

import org.hipparchus.linear.RealMatrix;

/** Interface for algorithms solving integer least square problems.
 * @see IntegerLeastSquareSolution
 * @author Luc Maisonobe
 * @since 10.0
 */
public interface IntegerLeastSquareSolver {

    /** Find the best solutions to an Integer Least Square problem.
     * @param nbSol number of solutions to search for
     * @param floatAmbiguities float estimates of ambiguities
     * @param indirection indirection array to extract ambiguity covariances from global covariance matrix
     * @param covariance global covariance matrix (includes ambiguities among other parameters)
     * @return at most {@code nbSol} solutions a to the Integer Least Square problem, in increasing
     * squared distance order
     */
    IntegerLeastSquareSolution[] solveILS(int nbSol, double[] floatAmbiguities, int[] indirection,
                                          RealMatrix covariance);

}
