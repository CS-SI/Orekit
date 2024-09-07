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
package org.orekit.control.indirect.shooting.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.CartesianAdjointEquationTerm;
import org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;

/**
 * Class for Cartesian adjoint derivatives provider (both standard and Field).
 *
 * @author Romain Serra
 * @since 12.2
 */
public class CartesianAdjointDynamicsProvider implements AdjointDynamicsProvider {

    /** Cartesian cost function. */
    private final CartesianCost cartesianCost;

    /** Cartesian adjoint terms. */
    private final CartesianAdjointEquationTerm[] equationTerms;

    /**
     * Constructor.
     * @param cartesianCost Cartesian cost
     * @param equationTerms adjoint equation terms
     */
    public CartesianAdjointDynamicsProvider(final CartesianCost cartesianCost,
                                            final CartesianAdjointEquationTerm... equationTerms) {
        this.cartesianCost = cartesianCost;
        this.equationTerms = equationTerms;
    }

    /** {@inheritDoc} */
    @Override
    public String getAdjointName() {
        return cartesianCost.getAdjointName();
    }

    /** {@inheritDoc} */
    @Override
    public CartesianAdjointDerivativesProvider buildAdditionalDerivativesProvider() {
        return new CartesianAdjointDerivativesProvider(cartesianCost, equationTerms);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldCartesianAdjointDerivativesProvider<T> buildFieldAdditionalDerivativesProvider(final Field<T> field) {
        return new FieldCartesianAdjointDerivativesProvider<>(cartesianCost, equationTerms);
    }
}
