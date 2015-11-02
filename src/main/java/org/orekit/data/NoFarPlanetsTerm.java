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

/** Class for terms that do not depend on far planets and some other elements.
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 */
class NoFarPlanetsTerm<T extends RealFieldElement<T>> extends SeriesTerm<T> {

    /** Coefficient for mean anomaly of the Moon. */
    private final int cL;

    /** Coefficient for L - Ω where L is the mean longitude of the Moon. */
    private final int cF;

    /** Coefficient for mean elongation of the Moon from the Sun. */
    private final int cD;

    /** Coefficient for mean longitude of the ascending node of the Moon. */
    private final int cOmega;

    /** Coefficient for mean Mercury longitude. */
    private final int cMe;

    /** Coefficient for mean Venus longitude. */
    private final int cVe;

    /** Coefficient for mean Earth longitude. */
    private final int cE;

    /** Coefficient for mean Mars longitude. */
    private final int cMa;

    /** Coefficient for mean Jupiter longitude. */
    private final int cJu;

    /** Coefficient for mean Saturn longitude. */
    private final int cSa;

    /** Build a planetary term for nutation series.
     * @param cL coefficient for mean anomaly of the Moon
     * @param cF coefficient for L - Ω where L is the mean longitude of the Moon
     * @param cD coefficient for mean elongation of the Moon from the Sun
     * @param cOmega coefficient for mean longitude of the ascending node of the Moon
     * @param cMe coefficient for mean Mercury longitude
     * @param cVe coefficient for mean Venus longitude
     * @param cE coefficient for mean Earth longitude
     * @param cMa coefficient for mean Mars longitude
     * @param cJu coefficient for mean Jupiter longitude
     * @param cSa coefficient for mean Saturn longitude
     */
    NoFarPlanetsTerm(final int cL, final int cF, final int cD, final int cOmega,
                            final int cMe, final int cVe, final int cE, final int cMa,
                            final int cJu, final int cSa) {
        this.cL     = cL;
        this.cF     = cF;
        this.cD     = cD;
        this.cOmega = cOmega;
        this.cMe    = cMe;
        this.cVe    = cVe;
        this.cE     = cE;
        this.cMa    = cMa;
        this.cJu    = cJu;
        this.cSa    = cSa;
    }

    /** {@inheritDoc} */
    protected double argument(final BodiesElements elements) {
        return cL * elements.getL() + cF * elements.getF() +
               cD * elements.getD() + cOmega * elements.getOmega() +
               cMe * elements.getLMe() + cVe * elements.getLVe() + cE  * elements.getLE() +
               cMa * elements.getLMa() + cJu * elements.getLJu() + cSa * elements.getLSa();

    }

    /** {@inheritDoc} */
    protected T argument(final FieldBodiesElements<T> elements) {
        return elements.getL().multiply(cL).
                add(elements.getF().multiply(cF)).
                add(elements.getD().multiply(cD)).
                add(elements.getOmega().multiply(cOmega)).
                add(elements.getLMe().multiply(cMe)).
                add(elements.getLVe().multiply(cVe)).
                add(elements.getLE().multiply(cE)).
                add(elements.getLMa().multiply(cMa)).
                add(elements.getLJu().multiply(cJu)).
                add(elements.getLSa().multiply(cSa));

    }

}
