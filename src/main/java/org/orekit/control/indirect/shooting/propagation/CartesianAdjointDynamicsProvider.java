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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;

/**
 * Abstract class for Cartesian adjoint derivatives provider.
 *
 * @author Romain Serra
 * @see AdjointDynamicsProvider
 * @since 12.2
 */
public abstract class CartesianAdjointDynamicsProvider implements AdjointDynamicsProvider {

    /** Adjoint name. */
    private final String adjointName;

    /** Adjoint dimension. */
    private final int dimension;

    /**
     * Constructor.
     * @param adjointName adjoint name
     * @param dimension adjoint dimension
     */
    protected CartesianAdjointDynamicsProvider(final String adjointName, final int dimension) {
        this.adjointName = adjointName;
        this.dimension = dimension;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return dimension;
    }

    /** {@inheritDoc} */
    @Override
    public String getAdjointName() {
        return adjointName;
    }

    /** {@inheritDoc} */
    @Override
    public abstract CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider();

    /** {@inheritDoc} */
    @Override
    public abstract <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(Field<T> field);
}
