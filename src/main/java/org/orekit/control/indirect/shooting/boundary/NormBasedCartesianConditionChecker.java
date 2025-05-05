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

import org.hipparchus.util.FastMath;
import org.orekit.control.indirect.shooting.AbstractFixedBoundaryCartesianSingleShooting;
import org.orekit.utils.PVCoordinates;

/**
 * Class defining convergence criterion on the norm of relative position and velocity vectors, with absolute tolerances.
 *
 * @author Romain Serra
 * @since 12.2
 * @see AbstractFixedBoundaryCartesianSingleShooting
 */
public class NormBasedCartesianConditionChecker implements CartesianBoundaryConditionChecker {

    /** Maximum iteration count. */
    private final int maximumIterationCount;

    /** Absolute tolerance when checking relative position norm. */
    private final double absoluteToleranceDistance;

    /** Absolute tolerance when checking relative velocity norm. */
    private final double absoluteToleranceSpeed;

    /**
     * Constructor.
     * @param maximumIterationCount maximum iteration count
     * @param absoluteToleranceDistance absolute tolerance on distance
     * @param absoluteToleranceSpeed absolute tolerance on speed
     */
    public NormBasedCartesianConditionChecker(final int maximumIterationCount,
                                              final double absoluteToleranceDistance,
                                              final double absoluteToleranceSpeed) {
        this.maximumIterationCount = maximumIterationCount;
        this.absoluteToleranceDistance = FastMath.abs(absoluteToleranceDistance);
        this.absoluteToleranceSpeed = FastMath.abs(absoluteToleranceSpeed);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaximumIterationCount() {
        return maximumIterationCount;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConverged(final PVCoordinates targetPV, final PVCoordinates actualPV) {
        return targetPV.getPosition().subtract(actualPV.getPosition()).getNorm() < absoluteToleranceDistance &&
                targetPV.getVelocity().subtract(actualPV.getVelocity()).getNorm() < absoluteToleranceSpeed;
    }

}
