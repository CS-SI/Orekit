/* Contributed in the public domain.
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
package org.orekit.forces.gravity.potential;

import java.util.List;

import org.orekit.time.AbsoluteDate;

/**
 * Defines methods for obtaining gravity fields.
 *
 * @author Evan Ward
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @author Luc Maisonobe
 * @see GravityFieldFactory
 * @since 10.1
 */
public interface GravityFields {

    /** Get a constant gravity field normalized coefficients provider
     * frozen at a given epoch.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @param freezingDate freezing epoch
     * @return a gravity field coefficients provider containing already loaded data
     * @since 12.0
     * @see #getNormalizedProvider(int, int)
     */
    NormalizedSphericalHarmonicsProvider getConstantNormalizedProvider(int degree, int order,
                                                                       AbsoluteDate freezingDate);

    /** Get a gravity field normalized coefficients provider.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getConstantNormalizedProvider(int, int, AbsoluteDate)
     */
    NormalizedSphericalHarmonicsProvider getNormalizedProvider(int degree,
                                                               int order);

    /** Get a constant gravity field unnormalized coefficients provider
     * frozen at a given epoch.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @param freezingDate freezing epoch
     * @return a gravity field coefficients provider containing already loaded data
     * @since 12.0
     * @see #getUnnormalizedProvider(int, int)
     */
    UnnormalizedSphericalHarmonicsProvider getConstantUnnormalizedProvider(int degree, int order,
                                                                           AbsoluteDate freezingDate);

    /** Get a gravity field unnormalized coefficients provider.
     *
     * @param degree maximal degree
     * @param order maximal order
     * @return a gravity field coefficients provider containing already loaded data
     * @since 6.0
     * @see #getConstantUnnormalizedProvider(int, int, AbsoluteDate)
     */
    UnnormalizedSphericalHarmonicsProvider getUnnormalizedProvider(int degree,
                                                                   int order);

    /** Get the ocean tides waves.
     *
     * <p><span style="color:red">
     * WARNING: as of 2013-11-17, there seem to be an inconsistency when loading
     * one or the other file, for wave Sa (Doodson number 56.554) and P1 (Doodson
     * number 163.555). The sign of the coefficients are different. We think the
     * problem lies in the input files from IERS and not in the conversion (which
     * works for all other waves), but cannot be sure. For this reason, ocean
     * tides are still considered experimental at this date.
     * </span></p>
     * @param degree maximal degree
     * @param order maximal order
     * @return list of tides waves containing already loaded data
     * @since 6.1
     */
    List<OceanTidesWave> getOceanTidesWaves(int degree, int order);
}
