/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.forces.gravity.potential;

import org.orekit.time.AbsoluteDate;

/**
 * Interface used to provide spherical harmonics coefficients.
 * <p>
 * Two interfaces are provided to distinguish between normalized and un-normalized
 * coefficients: {@link NormalizedSphericalHarmonicsProvider} and {@link
 * UnnormalizedSphericalHarmonicsProvider}. To account for gravity pertubations all
 * providers are capable of providing the coefficients on specific dates, using the {@link
 * NormalizedSphericalHarmonicsProvider#onDate(AbsoluteDate) onDate(AbsoluteDate)}
 * methods.
 * <p>
 * Typical usage when evaluating the geopotential:
 * <pre><code>
 *     NormalizedSphericalHarmonicsProvider provider = ...;
 *     NormalizedShpericalHarmonics coeffs = provider.onDate(date);
 *     double c20 = coeffs.getNormalizedCnm(2, 0);
 * </code></pre>
 *
 * @author Luc Maisonobe
 * @see GravityFieldFactory
 * @since 6.0
 */
public interface SphericalHarmonicsProvider extends TideSystemProvider {

    /** Get the maximal supported degree.
     * @return maximal supported degree
     */
    int getMaxDegree();

    /** Get the maximal supported order.
     * @return maximal supported order
     */
    int getMaxOrder();

    /** Get the central body attraction coefficient.
     * @return mu (m³/s²)
     */
    double getMu();

    /** Get the value of the central body reference radius.
     * @return ae (m)
     */
    double getAe();

    /** Get the reference date for the harmonics.
     * @return reference date for the harmonics
     */
    AbsoluteDate getReferenceDate();

    /** Get the offset from {@link #getReferenceDate reference date} for the harmonics.
     * @param date current date
     * @return offset between current date and reference date if there is a reference
     * date, or 0.0 if there are no reference dates (i.e. if {@link #getReferenceDate}
     * returns null)
     */
    double getOffset(AbsoluteDate date);

}
