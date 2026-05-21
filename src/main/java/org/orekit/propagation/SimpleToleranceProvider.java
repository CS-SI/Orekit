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
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;

import java.util.Arrays;


/**
 * Class implementing a tolerance provider with same relative and absolute tolerances for all dependent variables.
 *
 * @see ToleranceProvider
 * @since 14.0
 * @author Romain Serra
 */
public class SimpleToleranceProvider implements ToleranceProvider {

    /** Absolute tolerance for all. */
    private final double absoluteTolerance;

    /** Relative tolerance for all. */
    private final double relativeTolerance;

    /**
     * Constructor.
     * @param absoluteTolerance expected absolute error
     * @param relativeTolerance expected relative error
     */
    public SimpleToleranceProvider(final double absoluteTolerance, final double relativeTolerance) {
        this.absoluteTolerance = absoluteTolerance;
        this.relativeTolerance = relativeTolerance;
    }

    @Override
    public double[][] getTolerances(final Orbit referenceOrbit, final OrbitType propagationOrbitType,
                                    final PositionAngleType positionAngleType) {
        return getTolerances();
    }

    @Override
    public double[][] getTolerances(final Vector3D position, final Vector3D velocity) {
        return getTolerances();
    }

    /**
     * Retrieve constant absolute and respective tolerances.
     * @return tolerances
     */
    double[][] getTolerances() {
        final double[] absTol = new double[7];
        Arrays.fill(absTol, absoluteTolerance);
        final double[] relTol = new double[absTol.length];
        Arrays.fill(relTol, relativeTolerance);
        return new double[][] { absTol, relTol };
    }
}
