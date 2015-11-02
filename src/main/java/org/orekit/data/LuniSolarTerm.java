/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.data;

import org.apache.commons.math3.RealFieldElement;

/** Class for luni-solar only terms.
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 */
class LuniSolarTerm<T extends RealFieldElement<T>> extends SeriesTerm<T> {

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for mean anomaly of the Sun. */
    private final int cLPrime;

    /** Coefficient for L - Ω where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /** Build a luni-solar term for nutation series.
     * @param cL coefficient for mean anomaly of the Moon
     * @param cLPrime coefficient for mean anomaly of the Sun
     * @param cF coefficient for L - Ω where L is the mean longitude of the Moon
     * @param cD coefficient for mean elongation of the Moon from the Sun
     * @param cOmega coefficient for mean longitude of the ascending node of the Moon
     */
    LuniSolarTerm(final int cL, final int cLPrime, final int cF, final int cD, final int cOmega) {
        this.cL      = cL;
        this.cLPrime = cLPrime;
        this.cF      = cF;
        this.cD      = cD;
        this.cOmega  = cOmega;
    }

    /** {@inheritDoc} */
    protected double argument(final BodiesElements elements) {
        return cL * elements.getL() + cLPrime * elements.getLPrime() + cF * elements.getF() +
               cD * elements.getD() + cOmega * elements.getOmega();
    }

    /** {@inheritDoc} */
    protected T argument(final FieldBodiesElements<T> elements) {
        return elements.getL().multiply(cL).
                add(elements.getLPrime().multiply(cLPrime)).
                add(elements.getF().multiply(cF)).
                add(elements.getD().multiply(cD)).
                add(elements.getOmega().multiply(cOmega));

    }

}
