/* Copyright 2022 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.CalculusFieldElement;

/**
 * Semi-analytical J2-squared model.
 * <p>
 * This interface is implemented by models providing J2-squared
 * second-order terms in equinoctial elements. These terms are
 * used in the computation of the closed-form J2-squared perturbation
 * in semi-analytical satellite theory.
 * </p>
 * @see ZeisModel
 * @author Bryan Cazabonne
 * @since 12.0
 */
public interface J2SquaredModel {

    /**
     * Compute the J2-squared second-order terms in equinoctial elements.
     * @param context model context
     * @return the J2-squared second-order terms in equinoctial elements.
     *         Order must follow: [A, K, H, Q, P, M]
     */
    double[] computeMeanEquinoctialSecondOrderTerms(DSSTJ2SquaredClosedFormContext context);

    /**
     * Compute the J2-squared second-order terms in equinoctial elements.
     * @param context model context
     * @param <T> type of the elements
     * @return the J2-squared second-order terms in equinoctial elements.
     *         Order must follow: [A, K, H, Q, P, M]
     */
    <T extends CalculusFieldElement<T>> T[] computeMeanEquinoctialSecondOrderTerms(FieldDSSTJ2SquaredClosedFormContext<T> context);

}
