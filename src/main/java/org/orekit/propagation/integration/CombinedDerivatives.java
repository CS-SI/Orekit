/* Copyright 2002-2023 CS GROUP
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

/** Container for additional derivatives.
 * @see AdditionalDerivativesProvider
 * @author Luc Maisonobe
 * @since 11.2
 */
public class CombinedDerivatives {

    /** Additional state derivatives. */
    private double[] additionalDerivatives;

    /** Primary state derivatives increments (may be null). */
    private final double[] mainStateDerivativesIncrements;

    /** Simple constructor.
     * @param additionalDerivatives additional state derivatives
     * @param mainStateDerivativesIncrements increments related to the main state parameters
     * (may be null if main state should not be incremented)
     */
    public CombinedDerivatives(final double[] additionalDerivatives,
                               final double[] mainStateDerivativesIncrements) {
        this.mainStateDerivativesIncrements = mainStateDerivativesIncrements == null ?
                                              null : mainStateDerivativesIncrements.clone();
        this.additionalDerivatives          = additionalDerivatives.clone();
    }

    /** Get the derivatives increments related to the main state.
     * @return primary state derivatives increments, or null if
     * main state should not be incremented
     */
    public double[] getMainStateDerivativesIncrements() {
        return mainStateDerivativesIncrements == null ?
               null : mainStateDerivativesIncrements.clone();
    }

    /** Get the derivatives related to the additional state.
     * @return additional state derivatives
     */
    public double[] getAdditionalDerivatives() {
        return additionalDerivatives.clone();
    }

}
