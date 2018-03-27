/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.SphericalHarmonicsProvider;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;

/**
 * Interface used to provide un-normalized spherical harmonics coefficients.
 * @param <T> the type of the field elements
 */
public interface FieldUnnormalizedSphericalHarmonicsProvider <T extends RealFieldElement<T>> extends SphericalHarmonicsProvider {

    /** Un-normalized spherical harmonics coefficients evaluated at a specific instant.
     * @param <T> the type of the field elements
     */
    public interface FieldUnnormalizedSphericalHarmonics <T extends RealFieldElement<T>> extends FieldTimeStamped<T> {

        /** Get a spherical harmonic cosine coefficient.
         * @param n degree of the coefficient
         * @param i order of the coefficient
         * @return un-normalized coefficient Cnm
         * @exception OrekitException if the requested maximal degree or order exceeds the
         * available degree or order
         */
        double getUnnormalizedCnm(int n, int i) throws OrekitException;

        /** Get a spherical harmonic sine coefficient.
         * @param n degree of the coefficient
         * @param m order of the coefficient
         * @return un-normalized coefficient Snm
         * @exception OrekitException if the requested maximal degree or order exceeds the
         * available degree or order
         */
        double getUnnormalizedSnm(int n, int m) throws OrekitException;

    }

    /**
     * Get the un-normalized spherical harmonic coefficients at a specific instance in time.
     * @param <T>
     *
     * @param date of evaluation
     * @return un-normalized coefficients on {@code date}.
     * @throws OrekitException on error
     */
    FieldUnnormalizedSphericalHarmonics<T> onDate(FieldAbsoluteDate<T> date) throws OrekitException;
}
