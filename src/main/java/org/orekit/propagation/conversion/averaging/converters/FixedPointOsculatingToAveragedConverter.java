/* Copyright 2020-2025 Exotrail
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
package org.orekit.propagation.conversion.averaging.converters;

import org.orekit.propagation.conversion.averaging.AveragedOrbitalState;

/**
 * Abstract class for osculating-to-averaged converters based on a fixed-point algorithm.
 *
 * @author Romain Serra
 * @since 12.1
 * @see OsculatingToAveragedConverter
 * @param <T> type of averaged orbital state
 */
public abstract class FixedPointOsculatingToAveragedConverter<T extends AveragedOrbitalState>
        implements OsculatingToAveragedConverter<T> {

    /** Default convergence threshold. */
    public static final double DEFAULT_EPSILON = 1e-12;
    /** Default maximum number of iterations. */
    public static final int DEFAULT_MAX_ITERATIONS = 100;

    /** Convergence threshold. */
    private double epsilon;
    /** Maximum number of iterations. */
    private int maxIterations;

    /**
     * Protected constructor.
     * @param epsilon tolerance for convergence
     * @param maxIterations maximum number of iterations
     */
    protected FixedPointOsculatingToAveragedConverter(final double epsilon,
                                                      final int maxIterations) {
        this.epsilon = epsilon;
        this.maxIterations = maxIterations;
    }

    /**
     * Getter for the maximum number of iterations.
     * @return maximum number of iterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Getter for the convergence threshold.
     * @return convergence threshold
     */
    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Setter for epsilon.
     * @param epsilon convergence threshold.
     */
    public void setEpsilon(final double epsilon) {
        this.epsilon = epsilon;
    }

    /**
     * Setter for maximum number of iterations.
     * @param maxIterations maximum iterations
     */
    public void setMaxIterations(final int maxIterations) {
        this.maxIterations = maxIterations;
    }
}
