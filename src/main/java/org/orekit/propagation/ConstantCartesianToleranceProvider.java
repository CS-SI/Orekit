/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.util.Arrays;


/**
 * Class implementing a Cartesian tolerance provider with values independent on input position-velocity vector.
 *
 * @see CartesianToleranceProvider
 * @since 14.0
 * @author Romain Serra
 */
public class ConstantCartesianToleranceProvider implements CartesianToleranceProvider {

    /** Absolute tolerance for position vector. */
    private final double dP;

    /** Absolute tolerance for velocity vector. */
    private final double dV;

    /** Absolute tolerance for mass. */
    private final double dM;

    /** Relative tolerance for all. */
    private final double relativeTolerance;

    /**
     * Constructor.
     * @param dP expected absolute error in position
     * @param dV expected absolute error in velocity
     * @param dM expected absolute error in mass
     * @param relativeTolerance expected relative error
     */
    public ConstantCartesianToleranceProvider(final double dP, final double dV, final double dM,
                                              final double relativeTolerance) {
        this.dP = dP;
        this.dV = dV;
        this.dM = dM;
        this.relativeTolerance = relativeTolerance;
    }

    @Override
    public double[][] getTolerances(final Vector3D position, final Vector3D velocity) {
        final double[] absTol = new double[7];
        final double[] relTol = absTol.clone();
        Arrays.fill(absTol, 0, 3, dP);
        Arrays.fill(absTol, 3, 6, dV);
        absTol[6] = dM;
        Arrays.fill(relTol, 0, 7, relativeTolerance);
        return new double[][] { absTol, relTol };
    }
}
