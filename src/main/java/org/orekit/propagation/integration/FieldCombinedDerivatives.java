/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.integration;

import org.hipparchus.CalculusFieldElement;

/** Container for additional derivatives.
 * @see FieldAdditionalDerivativesProvider
 * @author Luc Maisonobe
 * @since 11.2
 * @param <T> type of the field elements
 */
public class FieldCombinedDerivatives<T extends CalculusFieldElement<T>> {

    /** Additional state derivatives. */
    private T[] additionalDerivatives;

    /** Primary state derivatives increments (may be null). */
    private final T[] mainStateDerivativesIncrements;

    /** Simple constructor.
     * @param additionalDerivatives additional state derivatives
     * @param mainStateDerivativesIncrements increments related to the main state parameters
     * (may be null if main state should not be incremented)
     */
    public FieldCombinedDerivatives(final T[] additionalDerivatives,
                                    final T[] mainStateDerivativesIncrements) {
        this.mainStateDerivativesIncrements = mainStateDerivativesIncrements == null ?
                                              null : mainStateDerivativesIncrements.clone();
        this.additionalDerivatives          = additionalDerivatives.clone();
    }

    /** Get the derivatives increments related to the main state.
     * @return primary state derivatives increments, or null if
     * main state should not be incremented
     */
    public T[] getMainStateDerivativesIncrements() {
        return mainStateDerivativesIncrements == null ?
               null : mainStateDerivativesIncrements.clone();
    }

    /** Get the derivatives related to the additional state.
     * @return additional state derivatives
     */
    public T[] getAdditionalDerivatives() {
        return additionalDerivatives.clone();
    }

}
