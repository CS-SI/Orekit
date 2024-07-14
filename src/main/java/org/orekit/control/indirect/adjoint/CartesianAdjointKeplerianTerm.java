/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.adjoint;

/**
 * Class defining the Keplerian contributions in the adjoint equations for Cartesian coordinates.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @since 12.2
 */
public class CartesianAdjointKeplerianTerm implements CartesianAdjointEquationTerm {

    /** Central body gravitational constant. */
    private final double mu;

    /**
     * Constructor.
     * @param mu central body gravitational parameter.
     */
    public CartesianAdjointKeplerianTerm(final double mu) {
        this.mu = mu;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getVelocityAdjointContribution(double[] stateVariables, double[] adjointVariables) {
        return new double[0];
    }
}
