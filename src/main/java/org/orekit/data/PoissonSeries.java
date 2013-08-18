/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.apache.commons.math3.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Class representing a Poisson series for nutation or ephemeris computations.
 * <p>
 * A Poisson series is composed of a time polynomial part and a non-polynomial
 * part which consist in summation series. The {@link SeriesTerm series terms}
 * are harmonic functions (combination of sines and cosines) of polynomial
 * <em>arguments</em>. The polynomial arguments are combinations of luni-solar or
 * planetary {@link BodiesElements elements}.
 * </p>
 * <p>
 * The Poisson series files from IERS have various formats, with or without
 * polynomial part, with or without planetary components, with or without
 * period column, with terms of increasing degrees either in dedicated columns
 * or in successive sections of the file ... This class attempts to read all the
 * commonly found formats, by specifying the columns of interest.
 * </p>
 * <p>
 * The handling of increasing degrees terms (i.e. sin, cos, t sin, t cos, t^2 sin,
 * t^2 cos ...) is done as follows.
 * </p>
 * <ul>
 *   <li>user must specify pairs of columns to be extracted at each line,
 *       in increasing degree order</li>
 *   <li>negative columns indices correspond to inexistant values that will be
 *       replaced by 0.0)</li>
 *   <li>file may provide section headers to specify a degree, which is added
 *   to the current column degree</li>
 * </ul>
 * <p>
 * A file from an old convention, like table 5.1 in IERS conventions 1996, uses
 * separate columns for degree 0 and degree 1, and uses only sine for nutation in
 * longitude and cosine for nutation in obliquity. It reads as follows:
 * </p>
 * <pre>
 * ∆ψ = Σ (Ai+A'it) sin(ARGUMENT), ∆ε = Σ (Bi+B'it) cos(ARGUMENT)
 *
 *      MULTIPLIERS OF      PERIOD           LONGITUDE         OBLIQUITY
 *  l    l'   F    D   Om     days         Ai       A'i       Bi       B'i
 *
 *  0    0    0    0    1   -6798.4    -171996    -174.2    92025      8.9
 *  0    0    2   -2    2     182.6     -13187      -1.6     5736     -3.1
 *  0    0    2    0    2      13.7      -2274      -0.2      977     -0.5
 *  0    0    0    0    2   -3399.2       2062       0.2     -895      0.5
 * </pre>
 * <p>
 * In order to parse the nutation in longitude from the previous table, the
 * following settings should be used:
 * </p>
 * <ul>
 *   <li>totalColumns   = 10</li>
 *   <li>firstDelaunay  =  1</li>
 *   <li>firstPlanetary = -1 (there are no planetary columns in this table)</li>
 *   <li>sinCosColumns  =  7, -1, 8, -1 (we read only coefficients Ai and A'i here)</li>
 * </ul>
 * <p>
 * In order to parse the nutation in obliquity from the previous table, the
 * following settings should be used:
 * </p>
 * <ul>
 *   <li>totalColumns   = 10</li>
 *   <li>firstDelaunay  =  1</li>
 *   <li>firstPlanetary = -1 (there are no planetary columns in this table)</li>
 *   <li>sinCosColumns  =  -1, 9, -1, 10 (we read only coefficients Bi and B'i here)</li>
 * </ul>
 * <p>
 * A file from a recent convention, like table 5.3a in IERS conventions 2010, uses
 * only two columns for sin and cos, and separate degrees in successive sections with
 * dedicated headers. It reads as follows:
 * </p>
 * <pre>
 * ---------------------------------------------------------------------------------------------------
 *
 * (unit microarcsecond; cut-off: 0.1 microarcsecond)
 * (ARG being for various combination of the fundamental arguments of the nutation theory)
 *
 *   Sum_i[A_i * sin(ARG) + A"_i * cos(ARG)]
 *
 * + Sum_i[A'_i * sin(ARG) + A"'_i * cos(ARG)] * t           (see Chapter 5, Eq. (35))
 *
 * The Table below provides the values for A_i and A"_i (j=0) and then A'_i and A"'_i (j=1)
 *
 * The expressions for the fundamental arguments appearing in columns 4 to 8 (luni-solar part)
 * and in columns 9 to 17 (planetary part) are those of the IERS Conventions 2003
 *
 * ----------------------------------------------------------------------------------------------------------
 * j = 0  Number of terms = 1320
 * ----------------------------------------------------------------------------------------------------------
 *     i        A_i             A"_i     l    l'   F    D    Om  L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A
 * ----------------------------------------------------------------------------------------------------------
 *     1   -17206424.18        3338.60    0    0    0    0    1    0    0    0    0    0    0    0    0    0
 *     2    -1317091.22       -1369.60    0    0    2   -2    2    0    0    0    0    0    0    0    0    0
 *     3     -227641.81         279.60    0    0    2    0    2    0    0    0    0    0    0    0    0    0
 *     4      207455.40         -69.80    0    0    0    0    2    0    0    0    0    0    0    0    0    0
 *     5      147587.70        1181.70    0    1    0    0    0    0    0    0    0    0    0    0    0    0
 *
 * ...
 *
 *  1319          -0.10           0.00    0    0    0    0    0    1    0   -3    0    0    0    0    0   -2
 *  1320          -0.10           0.00    0    0    0    0    0    0    0    1    0    1   -2    0    0    0
 *
 * --------------------------------------------------------------------------------------------------------------
 * j = 1  Number of terms = 38
 * --------------------------------------------------------------------------------------------------------------
 *    i          A'_i            A"'_i    l    l'   F    D   Om L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A
 * --------------------------------------------------------------------------------------------------------------
 *  1321      -17418.82           2.89    0    0    0    0    1    0    0    0    0    0    0    0    0    0
 *  1322        -363.71          -1.50    0    1    0    0    0    0    0    0    0    0    0    0    0    0
 *  1323        -163.84           1.20    0    0    2   -2    2    0    0    0    0    0    0    0    0    0
 *  1324         122.74           0.20    0    1    2   -2    2    0    0    0    0    0    0    0    0    0
 * </pre>
 * <p>
 * In order to parse the nutation in longitude from the previous table, the
 * following settings should be used:
 * </p>
 * <ul>
 *   <li>totalColumns   = 17</li>
 *   <li>firstDelaunay  =  4</li>
 *   <li>firstPlanetary =  9</li>
 *   <li>sinCosColumns  =  2, 3 (we specify only degree 0, so when we read section j = 0
 *       we read degree 0, when we read section j = 1 we read degree 1 ...)</li>
 * </ul>
 * <p>
 * Our parsing algorithm involves adding the section degree from the "j = 0, 1, 2 ..." header
 * to the column degree. A side effect of this algorithm is that it is theoretically possible
 * to mix both formats and have for example degree two term appear as degree 2 column in section
 * j=0 and as degree 1 column in section j=1 and as degree 0 column in section j=2. This case
 * is not expected to be encountered in practice. The real files use either several columns
 * <em>or</em> several sections, but not both at the same time.
 * </p>
 *
 * @author Luc Maisonobe
 * @see SeriesTerm
 * @see PolynomialNutation
 */
public class PoissonSeries {

    /** Polynomial part. */
    private PolynomialNutation polynomial;

    /** Non-polynomial series. */
    private Map<Long, SeriesTerm> series;

    /** Build a Poisson series from an IERS table file.
     * @param stream stream containing the IERS table
     * @param name name of the resource file (for error messages only)
     * @param freeVariable name of the free variable in the polynomial part
     * @param unit default unit for polynomial, if not explicit within the file
     * (may be null if the table has no polynomial part)
     * @param factor multiplicative factor to use for non-polynomial coefficients
     * @param totalColumns total number of columns in the non-polynomial sections
     * @param firstDelaunay column of the first Delaunay multiplier (counting from 1)
     * @param firstPlanetary column of the first planetary multiplier (counting from 1)
     * @param sinCosColumns columns of the sine and cosine coefficients for successive
     * degrees (i.e. sin, cos, t sin, t cos, t^2 sin, t^2 cos ...)
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public PoissonSeries(final InputStream stream, final String name,
                         final char freeVariable, final PolynomialParser.Unit unit,
                         final double factor, final int totalColumns,
                         final int firstDelaunay, final int firstPlanetary,
                         final int ... sinCosColumns)
        throws OrekitException {

        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
        }

        try {

            final PolynomialParser polynomialParser;
            if (unit == null) {
                // we don't expect any polynomial, we directly the the zero polynomial
                polynomialParser = null;
                polynomial       = new PolynomialNutation(new double[0]);
            } else {
                // set up a parser that will later be used to fill in the polynomial
                polynomialParser = new PolynomialParser(freeVariable, unit);
                polynomial       = null;
            }

            // the degrees section header should read something like:
            // j = 0  Nb of terms = 1306
            // or something like:
            // j = 0  Number  of terms = 1037
            final Pattern degreeSectionHeaderPattern =
                Pattern.compile("^\\p{Space}*j\\p{Space}*=\\p{Space}*(\\p{Digit}+)" +
                                "[\\p{Alpha}\\p{Space}]+=\\p{Space}*(\\p{Digit}+)\\p{Space}*$");

            // regular lines are simply a space separated list of numbers
            final String number = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";
            final StringBuilder builder = new StringBuilder("^\\p{Space}*");
            for (int i = 0; i < totalColumns; ++i) {
                builder.append("(");
                builder.append(number);
                builder.append(")");
                builder.append((i < totalColumns - 1) ? "\\p{Space}+" : "\\p{Space}*$");
            }
            final Pattern regularLinePattern = Pattern.compile(builder.toString());

            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            ;
            int lineNumber    =  0;
            int expectedIndex = -1;
            int nTerms        = -1;
            int count         =  0;
            int degree        =  0;
            series            = new HashMap<Long, SeriesTerm>();

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {

                // replace unicode minus sign ('−') by regular hyphen ('-') for parsing
                // such unicode characters occur in tables that are copy-pasted from PDF files
                line = line.replace('\u2212', '-');
                ++lineNumber;

                final Matcher regularMatcher = regularLinePattern.matcher(line);
                if (regularMatcher.matches()) {
                    // we have found a regular data line

                    if (expectedIndex > 0) {
                        // we are in a file were terms are numbered, we check the index
                        if (Integer.parseInt(regularMatcher.group(1)) != expectedIndex) {
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, regularMatcher.group());
                        }
                    }

                    // get the Delaunay and planetary multipliers
                    final int cL      = Integer.parseInt(regularMatcher.group(firstDelaunay));
                    final int cLPrime = Integer.parseInt(regularMatcher.group(firstDelaunay + 1));
                    final int cF      = Integer.parseInt(regularMatcher.group(firstDelaunay + 2));
                    final int cD      = Integer.parseInt(regularMatcher.group(firstDelaunay + 3));
                    final int cOmega  = Integer.parseInt(regularMatcher.group(firstDelaunay + 4));
                    final int cMe = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary));
                    final int cVe = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 1));
                    final int cE  = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 2));
                    final int cMa = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 3));
                    final int cJu = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 4));
                    final int cSa = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 5));
                    final int cUr = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 6));
                    final int cNe = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 7));
                    final int cPa = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 8));
                    final long key = NutationCodec.encode(cL, cLPrime, cF, cD, cOmega,
                                                          cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);

                    // retrived the term, or build it if it's the first time it is encountered in the file
                    final SeriesTerm term;
                    if (series.containsKey(key)) {
                        // the term was already known, from another degree
                        term = series.get(key);
                    } else {
                        // the term is a new one
                        term = SeriesTerm.buildTerm(cL, cLPrime, cF, cD, cOmega,
                                                    cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
                    }

                    boolean nonZero = false;
                    for (int d = 0; d < sinCosColumns.length / 2; ++d) {
                        final double sinCoeff = parseCoefficient(regularMatcher, sinCosColumns[2 * d],     factor);
                        final double cosCoeff = parseCoefficient(regularMatcher, sinCosColumns[2 * d + 1], factor);
                        if (!Precision.equals(sinCoeff, 0.0, 1) || !Precision.equals(cosCoeff, 0.0, 1)) {
                            nonZero = true;
                            term.add(0, degree + d, sinCoeff, cosCoeff);
                            ++count;
                        }
                    }
                    if (nonZero) {
                        series.put(key, term);
                    }

                    if (expectedIndex > 0) {
                        // we are in a file were terms are numbered
                        // we must update the expected value for next term
                        ++expectedIndex;
                    }

                } else {

                    final Matcher headerMatcher = degreeSectionHeaderPattern.matcher(line);
                    if (headerMatcher.matches()) {

                        // we have found a degree section header
                        final int nextDegree = Integer.parseInt(headerMatcher.group(1));
                        if ((nextDegree != degree + 1) && (degree != 0 || nextDegree != 0)) {
                            throw new OrekitException(OrekitMessages.MISSING_SERIE_J_IN_FILE,
                                                      degree + 1, name, lineNumber);
                        }

                        if (nextDegree == 0) {
                            // in IERS files split in sections, all terms are numbered
                            // we can check the indices
                            expectedIndex = 1;
                        }

                        if (nextDegree > 0 && count != nTerms) {
                            // the previous degree does not have the expected number of terms
                            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
                        }

                        // remember the number of terms the upcoming sublist should have
                        nTerms =  Integer.parseInt(headerMatcher.group(2));
                        count  = 0;
                        degree = nextDegree;

                    } else if (polynomial == null) {
                        // look for the polynomial part
                        final double[] coefficients = polynomialParser.parse(line);
                        if (coefficients != null) {
                            polynomial = new PolynomialNutation(coefficients);
                        }
                    }

                }

            }

            if (polynomial == null || series.isEmpty()) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

            if (nTerms > 0 && count != nTerms) {
                // the last degree does not have the expected number of terms
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

    }

    /** Parse a scaled coefficient.
     * @param matcher line matcher holding the coefficient
     * @param group group number of the coefficient, or -1 if line does not contain coefficient
     * @param scale scaling factor to apply
     * @return scaled factor, or 0.0 if group is -1
     */
    private double parseCoefficient(final Matcher matcher, final int group, final double scale) {
        if (group < 0) {
            return 0.0;
        } else {
            return scale * Double.parseDouble(matcher.group(group));
        }
    }

    /** Evaluate the value of the series.
     * @param elements bodies elements for nutation
     * @return value of the series
     */
    public double value(final BodiesElements elements) {

        // polynomial part
        final double p = polynomial.value(elements.getTC());

        // non-polynomial part
        // compute sum accurately, using Møller-Knuth TwoSum algorithm without branching
        // the following statements must NOT be simplified, they rely on floating point
        // arithmetic properties (rounding and representable numbers)
        double npHigh = 0;
        double npLow  = 0;
        for (final SeriesTerm term : series.values()) {
            final double v       = term.value(elements)[0];
            final double sum     = npHigh + v;
            final double sPrime  = sum - v;
            final double tPrime  = sum - sPrime;
            final double deltaS  = npHigh  - sPrime;
            final double deltaT  = v - tPrime;
            npLow  += deltaS   + deltaT;
            npHigh  = sum;
        }

        // add the polynomial and the non-polynomial parts
        return p + (npHigh + npLow);

    }

    /** This interface represents a fast evaluator for Poisson series.
     * @see PoissonSeries#compile(PoissonSeries...)
     * @since 6.1
     */
    public interface CompiledSeries {

        /** Evaluate a set of Poisson series.
         * @param elements bodies elements for nutation
         * @return value of the series
         */
        double[] value(BodiesElements elements);

    }

    /** Join several nutation series, for fast simultaneous evaluation.
     * @param poissonSeries Poisson series to join
     * @return a single function that evaluates all series together
     * @since 6.1
     */
    public static CompiledSeries compile(final PoissonSeries ... poissonSeries) {

        // store all polynomials
        final PolynomialNutation[] polynomials =
                new PolynomialNutation[poissonSeries.length];
        for (int i = 0; i < polynomials.length; ++i) {
            polynomials[i] = poissonSeries[i].polynomial;
        }

        // gather all series terms
        final Map<Long, SeriesTerm> joinedMap = new HashMap<Long, SeriesTerm>();
        for (final PoissonSeries ps : poissonSeries) {
            for (long key : ps.series.keySet()) {
                if (!joinedMap.containsKey(key)) {

                    // retrieve all Delaunay and planetary multipliers from the key
                    final int[] m = NutationCodec.decode(key);

                    // prepare a new term, ready to handle the required dimension
                    final SeriesTerm term =
                            SeriesTerm.buildTerm(m[0], m[1], m[2], m[3], m[4],
                                                 m[5], m[6], m[7], m[8], m[9], m[10], m[11], m[12], m[13]);
                    term.add(poissonSeries.length - 1, -1, Double.NaN, Double.NaN);

                    // store it
                    joinedMap.put(key, term);

                }
            }
        }

        // join series by sharing terms, in order to speed up evaluation
        // which is dominated by the computation of sine/cosine in each term
        for (int i = 0; i < poissonSeries.length; ++i) {
            for (final Map.Entry<Long, SeriesTerm> entry : poissonSeries[i].series.entrySet()) {
                final SeriesTerm singleTerm = entry.getValue();
                final SeriesTerm joinedTerm = joinedMap.get(entry.getKey());
                for (int degree = 0; degree <= singleTerm.getDegree(0); ++degree) {
                    joinedTerm.add(i, degree,
                                   singleTerm.getSinCoeff(0, degree),
                                   singleTerm.getCosCoeff(0, degree));
                }
            }
        }

        // use a single array for faster access
        final SeriesTerm[] joinedTerms =
                joinedMap.values().toArray(new SeriesTerm[joinedMap.size()]);

        return new CompiledSeries() {

            /** {@inheritDoc} */
            @Override
            public double[] value(final BodiesElements elements) {

               // non-polynomial part
                // compute sum accurately, using Møller-Knuth TwoSum algorithm without branching
                // the following statements must NOT be simplified, they rely on floating point
                // arithmetic properties (rounding and representable numbers)
                final double[] npHigh = new double[polynomials.length];
                final double[] npLow  = new double[polynomials.length];
                for (final SeriesTerm term : joinedTerms) {
                    final double[] termValue = term.value(elements);
                    for (int i = 0; i < termValue.length; ++i) {
                        final double v       = termValue[i];
                        final double sum     = npHigh[i] + v;
                        final double sPrime  = sum - v;
                        final double tPrime  = sum - sPrime;
                        final double deltaS  = npHigh[i]  - sPrime;
                        final double deltaT  = v - tPrime;
                        npLow[i]  += deltaS   + deltaT;
                        npHigh[i]  = sum;
                    }
                }

                // add residual and polynomial part
                for (int i = 0; i < npHigh.length; ++i) {
                    npHigh[i] += npLow[i] + polynomials[i].value(elements.getTC());
                }
                return npHigh;

            }

        };

    }

}
