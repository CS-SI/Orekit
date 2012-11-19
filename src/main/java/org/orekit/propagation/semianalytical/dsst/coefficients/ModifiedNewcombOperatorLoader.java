/* Copyright 2002-2012 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;

public class ModifiedNewcombOperatorLoader implements DataLoader {

    /** Supported file pattern. */
//    private static final String PATTERN = "^ModifidedNewcombOperator\\.txt$";

    /** maximum frequency index used in the tesseral perturbation. */
//    private int                 maxFreq;

    /** maximum resonant degree available. */
//    private int                 resonantDegree;

    /** maximum d'Alembert characteristic. */
//    private int                 alembertMax;

    /** maximum power of eccentricity used in the expansion of the Hansen coefficient Kernel. */
//    private int                 powerOfE;

    /** Simple constructor.
     * @param maxFreq maximum frequency index used in the tesseral perturbation
     * @param resonantDegree maximum resonant degree available
     * @param alembertMax maximum d'Alembert characteristic
     * @param powerOfE maximum power of eccentricity used in the expansion of the Hansen coefficient Kernel
     */
    public ModifiedNewcombOperatorLoader(final int maxFreq, final int resonantDegree,
                                         final int alembertMax, final int powerOfE) {
//        this.maxFreq = maxFreq;
//        this.resonantDegree = resonantDegree;
//        this.alembertMax = alembertMax;
//        this.powerOfE = powerOfE;

        // double pattern
//        final String doublePattern = "\\p{Blank}*(-?\\p{Digit}*\\.\\p{Digit}*)";

        // integer pattern
//        final String intPattern = "((?:\\p{Blank}|\\p{Digit})\\p{Digit})";
//        final Pattern regularPattern =
//                Pattern.compile(intPattern + intPattern + intPattern + intPattern + intPattern + doublePattern);

    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return false;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {
        // TODO
    }

}
