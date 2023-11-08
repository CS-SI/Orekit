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
package org.orekit.data;

import org.hipparchus.CalculusFieldElement;

/** Class for luni-solar only terms.
 * @author Luc Maisonobe
 */
class LuniSolarTerm extends SeriesTerm {

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
    protected double argumentDerivative(final BodiesElements elements) {
        return cL * elements.getLDot() + cLPrime * elements.getLPrimeDot() + cF * elements.getFDot() +
               cD * elements.getDDot() + cOmega * elements.getOmegaDot();
    }

    /** {@inheritDoc} */
    protected <T extends CalculusFieldElement<T>> T argument(final FieldBodiesElements<T> elements) {
        return elements.getL().multiply(cL).
               add(elements.getLPrime().multiply(cLPrime)).
               add(elements.getF().multiply(cF)).
               add(elements.getD().multiply(cD)).
               add(elements.getOmega().multiply(cOmega));
    }

    /** {@inheritDoc} */
    protected <T extends CalculusFieldElement<T>> T argumentDerivative(final FieldBodiesElements<T> elements) {
        return elements.getLDot().multiply(cL).
               add(elements.getLPrimeDot().multiply(cLPrime)).
               add(elements.getFDot().multiply(cF)).
               add(elements.getDDot().multiply(cD)).
               add(elements.getOmegaDot().multiply(cOmega));
    }

}
