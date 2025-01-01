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
package org.orekit.control.indirect.adjoint.cost;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;

/**
 * Abstract class for cost with Cartesian coordinates.
 *
 * @param <T> field type
 * @author Romain Serra
 * @see CartesianCost
 * @since 13.0
 */
public abstract class FieldAbstractCartesianCost<T extends CalculusFieldElement<T>> implements FieldCartesianCost<T> {

    /** Name of adjoint vector. */
    private final String name;

    /** Mass flow rate factor (always positive). */
    private final T massFlowRateFactor;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     */
    protected FieldAbstractCartesianCost(final String name, final T massFlowRateFactor) {
        this.name = name;
        this.massFlowRateFactor = FastMath.abs(massFlowRateFactor);
    }

    /**
     * Getter for adjoint vector name.
     * @return name
     */
    @Override
    public String getAdjointName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public T getMassFlowRateFactor() {
        return massFlowRateFactor;
    }

    /**
     * Computes the Euclidean norm of the adjoint velocity vector.
     * @param adjointVariables adjoint vector
     * @return norm of adjoint velocity
     */
    protected T getFieldAdjointVelocityNorm(final T[] adjointVariables) {
        return FastMath.sqrt(adjointVariables[3].square().add(adjointVariables[4].square()).add(adjointVariables[5].square()));
    }
}
