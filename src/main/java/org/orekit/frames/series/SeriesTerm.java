/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.frames.series;

import java.io.Serializable;

/** Base class for nutation series terms.
 * @author Luc Maisonobe
 * @see Development
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public abstract class SeriesTerm implements Serializable {

    /** Coefficient for the sine of the argument. */
    private final double sinCoeff;

    /** Coefficient for the cosine of the argument. */
    private final double cosCoeff;

    /** Simple constructor for the base class.
     * @param sinCoeff coefficient for the sine of the argument
     * @param cosCoeff coefficient for the cosine of the argument
     */
    protected SeriesTerm(final double sinCoeff, final double cosCoeff) {
        this.sinCoeff = sinCoeff;
        this.cosCoeff = cosCoeff;
    }

    /** Compute the value of the term for the current date.
     * @param elements luni-solar and planetary elements for the current date
     * @return current value of the term
     */
    public double value(final BodiesElements elements) {
        final double a = argument(elements);
        return sinCoeff * Math.sin(a) + cosCoeff * Math.cos(a);
    }

    /** Compute the argument for the current date.
     * @param elements luni-solar and planetary elements for the current date
     * @return current value of the argument
     */
    protected abstract double argument(final BodiesElements elements);

    /** Factory method for building the appropriate object.
     * <p>The method checks the null coefficients and build an instance
     * of an appropriate type to avoid too many unnecessary multiplications
     * by zero coefficients.</p>
     * @param sinCoeff coefficient for the sine of the argument
     * @param cosCoeff coefficient for the cosine of the argument
     * @param cL coefficient for mean anomaly of the Moon
     * @param cLPrime coefficient for mean anomaly of the Sun
     * @param cF coefficient for L - &Omega; where L is the mean longitude of the Moon
     * @param cD coefficient for mean elongation of the Moon from the Sun
     * @param cOmega coefficient for mean longitude of the ascending node of the Moon
     * @param cMe coefficient for mean Mercury longitude
     * @param cVe coefficient for mean Venus longitude
     * @param cE coefficient for mean Earth longitude
     * @param cMa coefficient for mean Mars longitude
     * @param cJu coefficient for mean Jupiter longitude
     * @param cSa coefficient for mean Saturn longitude
     * @param cUr coefficient for mean Uranus longitude
     * @param cNe coefficient for mean Neptune longitude
     * @param cPa coefficient for general accumulated precession in longitude
     * @return a nutation serie term instance well suited for the set of coefficients
     */
    public static SeriesTerm buildTerm(final double sinCoeff, final double cosCoeff,
                                       final int cL, final int cLPrime, final int cF,
                                       final int cD, final int cOmega,
                                       final int cMe, final int cVe, final int cE,
                                       final int cMa, final int cJu, final int cSa,
                                       final int cUr, final int cNe, final int cPa) {
        if (cL == 0 && cLPrime == 0 && cF == 0 && cD == 0 && cOmega == 0) {
            return new PlanetaryTerm(sinCoeff, cosCoeff,
                                     cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
        } else if (cMe == 0 && cVe == 0 && cE == 0 && cMa == 0 && cJu == 0 &&
                   cSa == 0 && cUr == 0 && cNe == 0 && cPa == 0) {
            return new LuniSolarTerm(sinCoeff, cosCoeff,
                                     cL, cLPrime, cF, cD, cOmega);
        } else if (cLPrime == 0 && cUr == 0 && cNe == 0 && cPa == 0) {
            return new NoFarPlanetsTerm(sinCoeff, cosCoeff,
                                        cL, cF, cD, cOmega,
                                        cMe, cVe, cE, cMa, cJu, cSa);
        } else {
            return new GeneralTerm(sinCoeff, cosCoeff,
                                   cL, cLPrime, cF, cD, cOmega,
                                   cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
        }

    }

}
