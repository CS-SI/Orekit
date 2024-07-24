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

import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;

/**
 * Abstract class defining common things for Cartesian adjoint dynamics between standard and Field versions.
 * @author Romain Serra
 * @see AdditionalDerivativesProvider
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @since 12.2
 */
public class AbstractCartesianAdjointDerivativesProvider {

    /** Name of the additional variables. */
    private final String name;

    /** Cost function. */
    private final CartesianCost cost;

    /**
     * Constructor.
     * @param cost cost function
     */
    public AbstractCartesianAdjointDerivativesProvider(final CartesianCost cost) {
        this.name = cost.getAdjointName();
        this.cost = cost;
    }

    /**
     * Getter for the cost.
     * @return cost
     */
    public CartesianCost getCost() {
        return cost;
    }

    /** Getter for the name.
     * @return name */
    public String getName() {
        return name;
    }

    /** Getter for the dimension.
     * @return dimension
     */
    public int getDimension() {
        return cost.getAdjointDimension();
    }
}
