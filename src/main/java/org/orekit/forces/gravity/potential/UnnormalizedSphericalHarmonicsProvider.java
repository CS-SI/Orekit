/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.orekit.errors.OrekitException;

/** Interface used to provide un-normalized spherical harmonics coefficients.
 * @see GravityFieldFactory
 * @author Luc Maisonobe
 * @since 6.0
 */
public interface UnnormalizedSphericalHarmonicsProvider extends SphericalHarmonicsProvider {

    /** Get a spherical harmonic cosine coefficient.
     * @param dateOffset date offset since reference date (s)
     * @param n degree of the coefficient
     * @param m order of the coefficient
     * @return un-normalized coefficient Cnm
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    double getUnnormalizedCnm(double dateOffset, int n, int m)
        throws OrekitException;

    /** Get a spherical harmonic sine coefficient.
     * @param dateOffset date offset since reference date (s)
     * @param n degree of the coefficient
     * @param m order of the coefficient
     * @return un-normalized coefficient Snm
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order
     */
    double getUnnormalizedSnm(double dateOffset, int n, int m)
        throws OrekitException;

}
