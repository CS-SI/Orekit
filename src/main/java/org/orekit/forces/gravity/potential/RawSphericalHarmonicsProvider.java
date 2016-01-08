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

import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Interface used to provide raw spherical harmonics coefficients.
 * <p>
 * This interface is intended to be used only as the workhorse for
 * either {@link NormalizedSphericalHarmonicsProvider} or
 * {@link SphericalHarmonicsProvider} implementations.
 * </p>
 * @see GravityFieldFactory
 * @author Luc Maisonobe
 * @since 6.0
 */
public interface RawSphericalHarmonicsProvider extends SphericalHarmonicsProvider {

    /**
     * The raw spherical harmonics at a particular instant.
     *
     * @see RawSphericalHarmonicsProvider#onDate(AbsoluteDate)
     */
    interface RawSphericalHarmonics extends TimeStamped {

        /** Get a spherical harmonic cosine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return raw coefficient Cnm
         * @exception OrekitException if the requested maximal degree or order exceeds the
         * available degree or order
         */
        double getRawCnm(int n, int m)
            throws OrekitException;

        /** Get a spherical harmonic sine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return raw coefficient Snm
         * @exception OrekitException if the requested maximal degree or order exceeds the
         * available degree or order
         */
        double getRawSnm(int n, int m)
            throws OrekitException;

    }

    /**
     * Get the raw spherical harmonic coefficients on a specific date.
     * @param date to evaluate the spherical harmonics
     * @return the raw spherical harmonics on {@code date}.
     * @throws OrekitException on error
     */
    RawSphericalHarmonics onDate(AbsoluteDate date) throws OrekitException;

}
