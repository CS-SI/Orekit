/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;

import java.util.SortedSet;

import org.hipparchus.linear.RealMatrix;

/** Interface for algorithms solving integer least square problems.
 * @see IntegerLeastSquareSolution
 * @author Luc Maisonobe
 * @since 10.0
 */
public interface IntegerLeastSquareSolver {

    /** Find the best solutions to an Integer Least Square problem.
     * @param floatAmbiguities float estimates of ambiguities
     * @param indirection indirection array to extract ambiguity covariances from global covariance matrix
     * @param covariance global covariance matrix (includes ambiguities among other parameters)
     * @param nbSol number of solutions to search for
     * @param chi search bound (size of the search ellipsoid)
     * @return at most {@code nbSol} solutions a to the Integer Least Square problem
     * with (â - a)ᵀ Q⁻¹ (â - a) ≤ χ², where â is the vector of float ambiguities
     * and Q is the covariance matrices
     */
    SortedSet<IntegerLeastSquareSolution> solveILS(double[] floatAmbiguities, int[] indirection, RealMatrix covariance,
                                                   int nbSol, double chi);

}
