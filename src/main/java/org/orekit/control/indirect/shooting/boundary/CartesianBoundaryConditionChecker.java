/* Copyright 2022-2025 Romain Serra
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
package org.orekit.control.indirect.shooting.boundary;

import org.orekit.control.indirect.shooting.AbstractFixedBoundaryCartesianSingleShooting;
import org.orekit.utils.PVCoordinates;

/**
 * Interface defining convergence criterion when the terminal condition is on a Cartesian state.
 *
 * @author Romain Serra
 * @since 12.2
 * @see AbstractFixedBoundaryCartesianSingleShooting
 */
public interface CartesianBoundaryConditionChecker {


    /**
     * Returns the maximum number of iterations.
     * @return maximum iterations
     */
    int getMaximumIterationCount();

    /**
     * Asserts convergence.
     * @param targetPV target position-velocity
     * @param actualPV actual position-velocity
     * @return convergence flag
     */
    boolean isConverged(PVCoordinates targetPV, PVCoordinates actualPV);

}
