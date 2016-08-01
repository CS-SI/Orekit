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

import java.util.Arrays;

import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitInternalError;

/** Base class for nutation series terms.
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @see PoissonSeries
 */
abstract class SeriesTerm<T extends RealFieldElement<T>> {

    /** Coefficients for the sine part. */
    private double[][] sinCoeff;

    /** Coefficients for the cosine part. */
    private double[][] cosCoeff;

    /** Simple constructor for the base class.
     */
    protected SeriesTerm() {
        this.sinCoeff = new double[0][0];
        this.cosCoeff = new double[0][0];
    }

    /** Get the degree of the function component.
     * @param index index of the function component (must be less than dimension)
     * @return degree of the function component
     */
    public int getDegree(final int index) {
        return  sinCoeff[index].length - 1;
    }

    /** Add a pair of values to existing term coefficients.
     * <p>
     * Despite it would seem logical to simply set coefficients
     * rather than add to them, this does not work for some IERS
     * files. As an example in table 5.3a in IERS conventions 2003,
     * the coefficients for luni-solar term for 2F+Ω with period
     * 13.633 days appears twice with different coefficients, as
     * well as term for 2(F+D+Ω)+l with period 5.643 days, term for
     * 2(F+D+Ω)-l with period 9.557 days, term for 2(Ω-l') with
     * period -173.318 days, term for 2D-l with period 31.812 days ...
     * 35 different duplicated terms have been identified in the
     * tables 5.3a and 5.3b in IERS conventions 2003.
     * The coefficients read in lines duplicating a term must be
     * added together.
     * </p>
     * @param index index of the components (will automatically
     * increase dimension if needed)
     * @param degree degree of the coefficients, may be negative if
     * the term does not contribute to component (will automatically
     * increase {@link #getDegree() degree} of the component if needed)
     * @param sinID coefficient for the sine part, at index and degree
     * @param cosID coefficient for the cosine part, at index and degree
     */
    public void add(final int index, final int degree,
                    final double sinID, final double cosID) {
        sinCoeff = extendArray(index, degree, sinCoeff);
        cosCoeff = extendArray(index, degree, cosCoeff);
        if (degree >= 0) {
            sinCoeff[index][degree] += sinID;
            cosCoeff[index][degree] += cosID;
        }
    }

    /** Get a coefficient for the sine part.
     * @param index index of the function component (must be less than dimension)
     * @param degree degree of the coefficients
     * (must be less than {@link #getDegree() degree} for the component)
     * @return coefficient for the sine part, at index and degree
     */
    public double getSinCoeff(final int index, final int degree) {
        return sinCoeff[index][degree];
    }

    /** Get a coefficient for the cosine part.
     * @param index index of the function component (must be less than dimension)
     * @param degree degree of the coefficients
     * (must be less than {@link #getDegree() degree} for the component)
     * @return coefficient for the cosine part, at index and degree
     */
    public double getCosCoeff(final int index, final int degree) {
        return cosCoeff[index][degree];
    }

    /** Evaluate the value of the series term.
     * @param elements bodies elements for nutation
     * @return value of the series term
     */
    public double[] value(final BodiesElements elements) {

        // preliminary computation
        final double tc  = elements.getTC();
        final double a   = argument(elements);
        final double sin = FastMath.sin(a);
        final double cos = FastMath.cos(a);

        // compute each function
        final double[] values = new double[sinCoeff.length];
        for (int i = 0; i < values.length; ++i) {
            double s = 0;
            double c = 0;
            for (int j = sinCoeff[i].length - 1; j >= 0; --j) {
                s = s * tc + sinCoeff[i][j];
                c = c * tc + cosCoeff[i][j];
            }
            values[i] = s * sin + c * cos;
        }

        return values;

    }

    /** Compute the argument for the current date.
     * @param elements luni-solar and planetary elements for the current date
     * @return current value of the argument
     */
    protected abstract double argument(BodiesElements elements);

    /** Evaluate the value of the series term.
     * @param elements bodies elements for nutation
     * @return value of the series term
     */
    public T[] value(final FieldBodiesElements<T> elements) {

        // preliminary computation
        final T tc  = elements.getTC();
        final T a   = argument(elements);
        final T sin = a.sin();
        final T cos = a.cos();

        // compute each function
        final T[] values = MathArrays.buildArray(tc.getField(), sinCoeff.length);
        for (int i = 0; i < values.length; ++i) {
            T s = tc.getField().getZero();
            T c = tc.getField().getZero();
            for (int j = sinCoeff[i].length - 1; j >= 0; --j) {
                s = s.multiply(tc).add(sinCoeff[i][j]);
                c = c.multiply(tc).add(cosCoeff[i][j]);
            }
            values[i] = s.multiply(sin).add(c.multiply(cos));
        }

        return values;

    }

    /** Compute the argument for the current date.
     * @param elements luni-solar and planetary elements for the current date
     * @return current value of the argument
     */
    protected abstract T argument(FieldBodiesElements<T> elements);

    /** Factory method for building the appropriate object.
     * <p>The method checks the null coefficients and build an instance
     * of an appropriate type to avoid too many unnecessary multiplications
     * by zero coefficients.</p>
     * @param <S> the type of the field elements
     * @param cGamma coefficient for γ = GMST + π tide parameter
     * @param cL coefficient for mean anomaly of the Moon
     * @param cLPrime coefficient for mean anomaly of the Sun
     * @param cF coefficient for L - Ω where L is the mean longitude of the Moon
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
    public static <S extends RealFieldElement<S>> SeriesTerm<S> buildTerm(final int cGamma,
                                                                          final int cL, final int cLPrime, final int cF,
                                                                          final int cD, final int cOmega,
                                                                          final int cMe, final int cVe, final int cE,
                                                                          final int cMa, final int cJu, final int cSa,
                                                                          final int cUr, final int cNe, final int cPa) {
        if (cGamma == 0 && cL == 0 && cLPrime == 0 && cF == 0 && cD == 0 && cOmega == 0) {
            return new PlanetaryTerm<S>(cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
        } else if (cGamma == 0 &&
                   cMe == 0 && cVe == 0 && cE == 0 && cMa == 0 && cJu == 0 &&
                   cSa == 0 && cUr == 0 && cNe == 0 && cPa == 0) {
            return new LuniSolarTerm<S>(cL, cLPrime, cF, cD, cOmega);
        } else if (cGamma != 0 &&
                   cMe == 0 && cVe == 0 && cE == 0 && cMa == 0 && cJu == 0 &&
                   cSa == 0 && cUr == 0 && cNe == 0 && cPa == 0) {
            return new TideTerm<S>(cGamma, cL, cLPrime, cF, cD, cOmega);
        } else if (cGamma == 0 && cLPrime == 0 && cUr == 0 && cNe == 0 && cPa == 0) {
            return new NoFarPlanetsTerm<S>(cL, cF, cD, cOmega,
                                           cMe, cVe, cE, cMa, cJu, cSa);
        } else if (cGamma == 0) {
            return new GeneralTerm<S>(cL, cLPrime, cF, cD, cOmega,
                                      cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
        } else {
            throw new OrekitInternalError(null);
        }

    }

    /** Extend an array to old at least index and degree.
     * @param index index of the function
     * @param degree degree of the coefficients
     * @param array to extend
     * @return extended array
     */
    private static double[][] extendArray(final int index, final int degree,
                                          final double[][] array) {

        // extend the number of rows if needed
        final double[][] extended;
        if (array.length > index) {
            extended = array;
        } else {
            final int rows =  index + 1;
            extended = new double[rows][];
            System.arraycopy(array, 0, extended, 0, array.length);
            Arrays.fill(extended, array.length, index + 1, new double[0]);
        }

        // extend the number of elements in the row if needed
        extended[index] = extendArray(degree, extended[index]);

        return extended;

    }

    /** Extend an array to old at least index and degree.
     * @param degree degree of the coefficients
     * @param array to extend
     * @return extended array
     */
    private static double[] extendArray(final int degree, final double[] array) {
        // extend the number of elements if needed
        if (array.length > degree) {
            return array;
        } else {
            final double[] extended = new double[degree + 1];
            System.arraycopy(array, 0, extended, 0, array.length);
            return extended;
        }
    }

}
