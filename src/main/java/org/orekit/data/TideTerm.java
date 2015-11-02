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

/** Class for tide terms.
 * <p>
 * BEWARE! For consistency with all the other Poisson series terms,
 * the elements in γ, l, l', F, D and Ω are ADDED together to compute
 * the argument of the term. In classical tides series, the computed
 * argument is cGamma * γ - (cL * l + cLPrime * l' + cF * F + cD * D
 * + cOmega * Ω). So at parsing time, the signs of cL, cLPrime, cF,
 * cD and cOmega must already have been reversed so the addition
 * performed here will work. This is done automatically when the
 * parser has been configured with a call to {@link
 * PoissonSeriesParser#withDoodson(int, int)} as the relationship
 * between the Doodson arguments and the traditional Delaunay
 * arguments ensures the proper sign is known.
 * </p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 */
class TideTerm<T extends RealFieldElement<T>> extends SeriesTerm<T> {

    /** Coefficient for γ = GMST + π tide parameter. */
    private final int cGamma;

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

    /** Build a tide term for nutation series.
     * @param cGamma coefficient for γ = GMST + π tide parameter
     * @param cL coefficient for mean anomaly of the Moon
     * @param cLPrime coefficient for mean anomaly of the Sun
     * @param cF coefficient for L - Ω where L is the mean longitude of the Moon
     * @param cD coefficient for mean elongation of the Moon from the Sun
     * @param cOmega coefficient for mean longitude of the ascending node of the Moon
     */
    TideTerm(final int cGamma,
                    final int cL, final int cLPrime, final int cF, final int cD, final int cOmega) {
        this.cGamma  = cGamma;
        this.cL      = cL;
        this.cLPrime = cLPrime;
        this.cF      = cF;
        this.cD      = cD;
        this.cOmega  = cOmega;
    }

    /** {@inheritDoc} */
    protected double argument(final BodiesElements elements) {
        return cGamma * elements.getGamma() +
               cL * elements.getL() + cLPrime * elements.getLPrime() + cF * elements.getF() +
               cD * elements.getD() + cOmega * elements.getOmega();
    }

    /** {@inheritDoc} */
    protected T argument(final FieldBodiesElements<T> elements) {
        return elements.getGamma().multiply(cGamma).
                add(elements.getL().multiply(cL)).
                add(elements.getLPrime().multiply(cLPrime)).
                add(elements.getF().multiply(cF)).
                add(elements.getD().multiply(cD)).
                add(elements.getOmega().multiply(cOmega));

    }

}
