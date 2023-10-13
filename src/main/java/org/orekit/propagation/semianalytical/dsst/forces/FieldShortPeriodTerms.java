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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.Map;
import java.util.Set;

import org.hipparchus.CalculusFieldElement;
import org.orekit.orbits.FieldOrbit;
import org.orekit.time.FieldAbsoluteDate;

/** Additive short period terms contributing to the mean to osculating orbit mapping.
 * <p>
 * Each instance contains a set of several terms that are computed together.
 * </p>
 * @see DSSTForceModel
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public interface FieldShortPeriodTerms <T extends CalculusFieldElement<T>> {

    /** Evaluate the contributions of the short period terms.
     * @param meanOrbit mean orbit to which the short period contribution applies
     * @return short period terms contributions
     */
    T[] value(FieldOrbit<T> meanOrbit);

    /** Get the prefix for short period coefficients keys.
     * <p>
     * This prefix is used to identify the coefficients of the
     * current force model from the coefficients pertaining to
     * other force models. All the keys in the map returned by
     * {@link #getCoefficients(FieldAbsoluteDate, Set)}
     * start with this prefix, which must be unique among all
     * providers.
     * </p>
     * @return the prefix for short periodic coefficients keys
     * @see #getCoefficients(FieldAbsoluteDate, Set)
     */
    String getCoefficientsKeyPrefix();

    /** Computes the coefficients involved in the contributions.
     * <p>
     * This method is intended mainly for validation purposes. Its output
     * is highly dependent on the implementation details in each force model
     * and may change from version to version. It is <em>not</em> recommended
     * to use it for any operational purposes.
     * </p>
     * @param date current date
     * @param selected set of coefficients that should be put in the map
     * (empty set means all coefficients are selected)
     * @return the selected coefficients of the short periodic variations,
     * in a map where all keys start with {@link #getCoefficientsKeyPrefix()}
     */
    Map<String, T[]> getCoefficients(FieldAbsoluteDate<T> date, Set<String> selected);

}
