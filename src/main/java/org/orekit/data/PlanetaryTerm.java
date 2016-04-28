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
package org.orekit.data;

import org.hipparchus.RealFieldElement;

/** Class for planetary only terms.
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 */
class PlanetaryTerm<T extends RealFieldElement<T>> extends SeriesTerm<T> {

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

    /** Coefficient for mean Uranus longitude. */
    private final int cUr;

    /** Coefficient for mean Neptune longitude. */
    private final int cNe;

    /** Coefficient for general accumulated precession in longitude. */
    private final int cPa;

    /** Build a planetary term for nutation series.
     * @param cMe coefficient for mean Mercury longitude
     * @param cVe coefficient for mean Venus longitude
     * @param cE coefficient for mean Earth longitude
     * @param cMa coefficient for mean Mars longitude
     * @param cJu coefficient for mean Jupiter longitude
     * @param cSa coefficient for mean Saturn longitude
     * @param cUr coefficient for mean Uranus longitude
     * @param cNe coefficient for mean Neptune longitude
     * @param cPa coefficient for general accumulated precession in longitude
      */
    PlanetaryTerm(final int cMe, final int cVe, final int cE, final int cMa, final int cJu,
                         final int cSa, final int cUr, final int cNe, final int cPa) {
        this.cMe = cMe;
        this.cVe = cVe;
        this.cE  = cE;
        this.cMa = cMa;
        this.cJu = cJu;
        this.cSa = cSa;
        this.cUr = cUr;
        this.cNe = cNe;
        this.cPa = cPa;
    }

    /** {@inheritDoc} */
    protected double argument(final BodiesElements elements) {
        return cMe * elements.getLMe() + cVe * elements.getLVe() + cE  * elements.getLE() +
               cMa * elements.getLMa() + cJu * elements.getLJu() +
               cSa * elements.getLSa() + cUr * elements.getLUr() +
               cNe * elements.getLNe() + cPa * elements.getPa();
    }

    /** {@inheritDoc} */
    protected T argument(final FieldBodiesElements<T> elements) {
        return elements.getLMe().multiply(cMe).
                add(elements.getLVe().multiply(cVe)).
                add(elements.getLE().multiply(cE)).
                add(elements.getLMa().multiply(cMa)).
                add(elements.getLJu().multiply(cJu)).
                add(elements.getLSa().multiply(cSa)).
                add(elements.getLUr().multiply(cUr)).
                add(elements.getLNe().multiply(cNe)).
                add(elements.getPa().multiply(cPa));

    }

}
