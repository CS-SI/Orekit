/* Copyright 2022-2025 Bryan Cazabonne
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
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

import java.util.Collections;
import java.util.List;

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

    /**
     * Performs initialization of J2-squared short period terms prior to propagation.
     * @param auxiliaryElements auxiliary elements
     * @param type type of the elements used (MEAN or OSCULATING)
     * @param parameters force model parameters
     * @return a list containing the initialized short period terms
     * @since 12.2
     */
    default List<ShortPeriodTerms> initializeShortPeriodTerms(final AuxiliaryElements auxiliaryElements,
                                                              final PropagationType type,
                                                              final double[] parameters) {
        return Collections.emptyList();
    }

    /**
     * Performs initialization of J2-squared short period terms prior to propagation.
     * @param auxiliaryElements auxiliary elements
     * @param type type of the orbital elements used (MEAN or OSCULATING)
     * @param parameters force model parameters
     * @param <T> type of the field elements
     * @return a list containing the initialized short period terms
     * @since 12.2
     */
    default <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>> initializeShortPeriodTerms(final FieldAuxiliaryElements<T> auxiliaryElements,
                                                                                                          final PropagationType type,
                                                                                                          final T[] parameters) {
        return Collections.emptyList();
    }

    /** Update the J2-squared short period terms.
     * <p>
     * The {@link ShortPeriodTerms short period terms} that will be updated
     * are the ones that were returned during the call to {@link
     * #initializeShortPeriodTerms(AuxiliaryElements, PropagationType, double[])}.
     * </p>
     * @param parameters force model parameters
     * @param meanStates mean states information: date, kinematics, attitude
     * @since 12.2
     */
    default void updateShortPeriodTerms(final double[] parameters, final SpacecraftState... meanStates) {
        // Does nothing by default
    }

    /** Update the J2-squared short period terms.
     * <p>
     * The {@link ShortPeriodTerms short period terms} that will be updated
     * are the ones that were returned during the call to {@link
     * #initializeShortPeriodTerms(AuxiliaryElements, PropagationType, double[])}.
     * </p>
     * @param parameters force model parameters
     * @param meanStates mean states information: date, kinematics, attitude
     * @param <T> type of the field elements
     * @since 12.2
     */
    @SuppressWarnings("unchecked")
    default <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(final T[] parameters, final FieldSpacecraftState<T>... meanStates) {
        // Does nothing by default
    }

}
