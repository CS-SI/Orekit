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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Parser for {@link PoissonSeries Poisson series} files.
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
 *   <li>negative columns indices correspond to inexistent values that will be
 *       replaced by 0.0)</li>
 *   <li>file may provide section headers to specify a degree, which is added
 *       to the current column degree</li>
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
 *   <li>totalColumns   = 10 (see {@link #PoissonSeriesParser(int)})</li>
 *   <li>firstDelaunay  =  1 (see {@link #withFirstDelaunay(int)})</li>
 *   <li>no calls to {@link #withFirstPlanetary(int)} as there are no planetary columns in this table</li>
 *   <li>sinCosColumns  =  7, -1 for degree 0 for Ai (see {@link #withSinCos(int, int, double, int, double)})</li>
 *   <li>sinCosColumns  =  8, -1 for degree 1 for A'i (see {@link #withSinCos(int, int, double, int, double)})</li>
 * </ul>
 * <p>
 * In order to parse the nutation in obliquity from the previous table, the
 * following settings should be used:
 * </p>
 * <ul>
 *   <li>totalColumns   = 10 (see {@link #PoissonSeriesParser(int)})</li>
 *   <li>firstDelaunay  =  1 (see {@link #withFirstDelaunay(int)})</li>
 *   <li>no calls to {@link #withFirstPlanetary(int)} as there are no planetary columns in this table</li>
 *   <li>sinCosColumns  =  -1, 9 for degree 0 for Bi (see {@link #withSinCos(int, int, double, int, double)})</li>
 *   <li>sinCosColumns  =  -1, 10 for degree 1 for B'i (see {@link #withSinCos(int, int, double, int, double)})</li>
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
 *   <li>totalColumns   = 17 (see {@link #PoissonSeriesParser(int)})</li>
 *   <li>firstDelaunay  =  4 (see {@link #withFirstDelaunay(int)})</li>
 *   <li>firstPlanetary =  9 (see {@link #withFirstPlanetary(int)})</li>
 *   <li>sinCosColumns  =  2,3 (we specify only degree 0, so when we read
 *       section j = 0 we read degree 0, when we read section j = 1 we read
 *       degree 1, see {@link #withSinCos(int, int, double, int, double)} ...)</li>
 * </ul>
 * <p>
 * A file from a recent convention, like table 6.5a in IERS conventions 2010, contains
 * both Doodson arguments (τ, s, h, p, N', ps), Doodson numbers and Delaunay parameters.
 * In this case, the coefficients for the Delaunay parameters must be <em>subtracted</em>
 * from the τ = GMST + π tide parameter, so the signs in the files must be reversed
 * in order to match the Doodson arguments and Doodson numbers. This is done automatically
 * (and consistency is checked) only when the {@link #withDoodson(int, int)} method is
 * called at parser configuration time. Some other files use the γ = GMST + π tide parameter
 * rather than Doodson τ argument and the coefficients for the Delaunay parameters must be
 * <em>added</em> to the γ parameter, so no sign reversal is performed. In order to avoid
 * ambiguity as the two cases are incompatible with each other, trying to add a configuration
 * for τ by calling {@link #withDoodson(int, int)} and to also add a configuration for γ by
 * calling {@link #withGamma(int)} triggers an exception.
 * </p>
 * <p>The table 6.5a file also contains a column for the waves names (the Darwin's symbol)
 * which may be empty, so it must be identified explicitly by calling {@link
 * #withOptionalColumn(int)}. The 6.5a table reads as follows:
 * </p>
 * <pre>
 * The in-phase (ip) amplitudes (A₁ δkfR Hf) and the out-of-phase (op) amplitudes (A₁ δkfI Hf)
 * of the corrections for frequency dependence of k₂₁⁽⁰⁾, taking the nominal value k₂₁ for the
 * diurnal tides as (0.29830 − i 0.00144). Units: 10⁻¹² . The entries for δkfR and δkfI are in
 * units of 10⁻⁵. Multipliers of the Doodson arguments identifying the tidal terms are given,
 * as also those of the Delaunay variables characterizing the nutations produced by these
 * terms.
 *
 * Name   deg/hr    Doodson  τ  s  h  p  N' ps   l  l' F  D  Ω  δkfR  δkfI     Amp.    Amp.
 *                    No.                                       /10−5 /10−5    (ip)    (op)
 *   2Q₁ 12.85429   125,755  1 -3  0  2   0  0   2  0  2  0  2    -29     3    -0.1     0.0
 *    σ₁ 12.92714   127,555  1 -3  2  0   0  0   0  0  2  2  2    -30     3    -0.1     0.0
 *       13.39645   135,645  1 -2  0  1  -1  0   1  0  2  0  1    -45     5    -0.1     0.0
 *    Q₁ 13.39866   135,655  1 -2  0  1   0  0   1  0  2  0  2    -46     5    -0.7     0.1
 *    ρ₁ 13.47151   137,455  1 -2  2 -1   0  0  -1  0  2  2  2    -49     5    -0.1     0.0
 * </pre>
 * <ul>
 *   <li>totalColumns   = 18 (see {@link #PoissonSeriesParser(int)})</li>
 *   <li>optionalColumn =  1 (see {@link #withOptionalColumn(int)})</li>
 *   <li>firstDoodson, Doodson number = 4, 3 (see {@link #withDoodson(int, int)})</li>
 *   <li>firstDelaunay  =  10 (see {@link #withFirstDelaunay(int)})</li>
 *   <li>sinCosColumns  =  17, 18, see {@link #withSinCos(int, int, double, int, double)} ...)</li>
 * </ul>
 * <p>
 * Our parsing algorithm involves adding the section degree from the "j = 0, 1, 2 ..." header
 * to the column degree. A side effect of this algorithm is that it is theoretically possible
 * to mix both formats and have for example degree two term appear as degree 2 column in section
 * j=0 and as degree 1 column in section j=1 and as degree 0 column in section j=2. This case
 * is not expected to be encountered in practice. The real files use either several columns
 * <em>or</em> several sections, but not both at the same time.
 * </p>
 * @param <T> the type of the field elements
 *
 * @author Luc Maisonobe
 * @see SeriesTerm
 * @see PolynomialNutation
 * @since 6.1
 */
public class PoissonSeriesParser<T extends RealFieldElement<T>> {

    /** Default pattern for fields with unknown type (non-space characters). */
    private static final String  UNKNOWN_TYPE_PATTERN = "\\S+";

    /** Pattern for optional fields (either nothing or non-space characters). */
    private static final String  OPTIONAL_FIELD_PATTERN = "\\S*";

    /** Pattern for fields with integer type. */
    private static final String  INTEGER_TYPE_PATTERN = "[-+]?\\p{Digit}+";

    /** Pattern for fields with real type. */
    private static final String  REAL_TYPE_PATTERN = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

    /** Pattern for fields with Doodson number. */
    private static final String  DOODSON_TYPE_PATTERN = "\\p{Digit}{2,3}[.,]\\p{Digit}{3}";

    /** Parser for the polynomial part. */
    private final PolynomialParser polynomialParser;

    /** Fields patterns. */
    private final String[] fieldsPatterns;

    /** Optional column (counting from 1). */
    private final int optional;

    /** Column of the γ = GMST + π tide multiplier (counting from 1). */
    private final int gamma;

    /** Column of the first Doodson multiplier (counting from 1). */
    private final int firstDoodson;

    /** Column of the Doodson number (counting from 1). */
    private final int doodson;

    /** Column of the first Delaunay multiplier (counting from 1). */
    private final int firstDelaunay;

    /** Column of the first planetary multiplier (counting from 1). */
    private final int firstPlanetary;

    /** columns of the sine and cosine coefficients for successive degrees.
     * <p>
     * The ordering is: sin, cos, t sin, t cos, t^2 sin, t^2 cos ...
     * </p>
     */
    private final int[] sinCosColumns;

    /** Multiplicative factors to use for various columns. */
    private final double[] sinCosFactors;

    /** Build a parser for a Poisson series from an IERS table file.
     * @param polynomialParser polynomial parser to use
     * @param fieldsPatterns patterns for fields
     * @param optional optional column
     * @param gamma column of the GMST tide multiplier
     * @param firstDoodson column of the first Doodson multiplier
     * @param doodson column of the Doodson number
     * @param firstDelaunay column of the first Delaunay multiplier
     * @param firstPlanetary column of the first planetary multiplier
     * @param sinCosColumns columns of the sine and cosine coefficients
     * @param factors multiplicative factors to use for various columns
     */
    private PoissonSeriesParser(final PolynomialParser polynomialParser,
                                final String[] fieldsPatterns, final int optional,
                                final int gamma, final int firstDoodson,
                                final int doodson, final int firstDelaunay,
                                final int firstPlanetary, final int[] sinCosColumns,
                                final double[] factors) {
        this.polynomialParser = polynomialParser;
        this.fieldsPatterns   = fieldsPatterns;
        this.optional         = optional;
        this.gamma            = gamma;
        this.firstDoodson     = firstDoodson;
        this.doodson          = doodson;
        this.firstDelaunay    = firstDelaunay;
        this.firstPlanetary   = firstPlanetary;
        this.sinCosColumns    = sinCosColumns;
        this.sinCosFactors    = factors;
    }

    /** Build a parser for a Poisson series from an IERS table file.
     * @param totalColumns total number of columns in the non-polynomial sections
     */
    public PoissonSeriesParser(final int totalColumns) {
        this(null, createInitialFieldsPattern(totalColumns), -1,
             -1, -1, -1, -1, -1, new int[0], new double[0]);
    }

    /** Create an array with only non-space fields patterns.
     * @param totalColumns total number of columns
     * @return a new fields pattern array
     */
    private static String[] createInitialFieldsPattern(final int totalColumns) {
        final String[] patterns = new String[totalColumns];
        setPatterns(patterns, 1, totalColumns, UNKNOWN_TYPE_PATTERN);
        return patterns;
    }

    /** Set fields patterns.
     * @param array fields pattern array to modify
     * @param first first column to set (counting from 1), do nothing if non-positive
     * @param count number of colums to set
     * @param pattern pattern to use
     */
    private static void setPatterns(final String[] array, final int first, final int count,
                                    final String pattern) {
        if (first > 0) {
            Arrays.fill(array, first - 1, first - 1 + count, pattern);
        }
    }

    /** Set up polynomial part parsing.
     * @param freeVariable name of the free variable in the polynomial part
     * @param unit default unit for polynomial, if not explicit within the file
     * @return a new parser, with polynomial parser updated
     */
    public PoissonSeriesParser<T> withPolynomialPart(final char freeVariable, final PolynomialParser.Unit unit) {
        return new PoissonSeriesParser<T>(new PolynomialParser(freeVariable, unit), fieldsPatterns, optional,
                                          gamma, firstDoodson, doodson, firstDelaunay,
                                          firstPlanetary, sinCosColumns, sinCosFactors);
    }

    /** Set up optional column.
     * <p>
     * Optional columns typically appears in tides-related files, as some waves have
     * specific names (χ₁, M₂, ...) and other waves don't have names and hence are
     * replaced by spaces in the corresponding file line.
     * </p>
     * <p>
     * At most one column may be optional.
     * </p>
     * @param column column of the GMST tide multiplier (counting from 1)
     * @return a new parser, with updated columns settings
     */
    public PoissonSeriesParser<T> withOptionalColumn(final int column) {

        // update the fields pattern to expect 1 optional field at the right index
        final String[] newFieldsPatterns = fieldsPatterns.clone();
        setPatterns(newFieldsPatterns, optional, 1, UNKNOWN_TYPE_PATTERN);
        setPatterns(newFieldsPatterns, column,   1, OPTIONAL_FIELD_PATTERN);

        return new PoissonSeriesParser<T>(polynomialParser, newFieldsPatterns, column,
                                          gamma, firstDoodson, doodson, firstDelaunay,
                                          firstPlanetary, sinCosColumns, sinCosFactors);

    }

    /** Set up column of GMST tide multiplier.
     * @param column column of the GMST tide multiplier (counting from 1)
     * @return a new parser, with updated columns settings
     * @exception OrekitException if τ has been configured by a previous call
     * to {@link #withDoodson(int, int)}
     * @see #withDoodson(int, int)
     */
    public PoissonSeriesParser<T> withGamma(final int column) throws OrekitException {

        // check we don't try to have both τ and γ configured at the same time
        if (firstDoodson > 0 && column > 0) {
            throw new OrekitException(OrekitMessages.CANNOT_PARSE_BOTH_TAU_AND_GAMMA);
        }

        // update the fields pattern to expect 1 integer at the right index
        final String[] newFieldsPatterns = fieldsPatterns.clone();
        setPatterns(newFieldsPatterns, gamma,  1, UNKNOWN_TYPE_PATTERN);
        setPatterns(newFieldsPatterns, column, 1, INTEGER_TYPE_PATTERN);

        return new PoissonSeriesParser<T>(polynomialParser, newFieldsPatterns, optional,
                                          column, firstDoodson, doodson, firstDelaunay,
                                          firstPlanetary, sinCosColumns, sinCosFactors);

    }

    /** Set up columns for Doodson multiplers and Doodson number.
     * @param firstMultiplierColumn column of the first Doodson multiplier which
     * corresponds to τ (counting from 1)
     * @param numberColumn column of the Doodson number (counting from 1)
     * @return a new parser, with updated columns settings
     * @exception OrekitException if γ has been configured by a previous call
     * to {@link #withGamma(int)}
     * @see #withGamma(int)
     * @see #withFirstDelaunay(int)
     */
    public PoissonSeriesParser<T> withDoodson(final int firstMultiplierColumn, final int numberColumn)
        throws OrekitException {

        // check we don't try to have both τ and γ configured at the same time
        if (gamma > 0 && firstMultiplierColumn > 0) {
            throw new OrekitException(OrekitMessages.CANNOT_PARSE_BOTH_TAU_AND_GAMMA);
        }

        final String[] newFieldsPatterns = fieldsPatterns.clone();

        // update the fields pattern to expect 6 integers at the right indices
        setPatterns(newFieldsPatterns, firstDoodson,          6, UNKNOWN_TYPE_PATTERN);
        setPatterns(newFieldsPatterns, firstMultiplierColumn, 6, INTEGER_TYPE_PATTERN);

        // update the fields pattern to expect 1 Doodson number at the right index
        setPatterns(newFieldsPatterns, doodson,      1, UNKNOWN_TYPE_PATTERN);
        setPatterns(newFieldsPatterns, numberColumn, 1, DOODSON_TYPE_PATTERN);

        return new PoissonSeriesParser<T>(polynomialParser, newFieldsPatterns, optional,
                                          gamma, firstMultiplierColumn, numberColumn, firstDelaunay,
                                          firstPlanetary, sinCosColumns, sinCosFactors);

    }

    /** Set up first column of Delaunay multiplier.
     * @param firstColumn column of the first Delaunay multiplier (counting from 1)
     * @return a new parser, with updated columns settings
     */
    public PoissonSeriesParser<T> withFirstDelaunay(final int firstColumn) {

        // update the fields pattern to expect 5 integers at the right indices
        final String[] newFieldsPatterns = fieldsPatterns.clone();
        setPatterns(newFieldsPatterns, firstDelaunay, 5, UNKNOWN_TYPE_PATTERN);
        setPatterns(newFieldsPatterns, firstColumn,   5, INTEGER_TYPE_PATTERN);

        return new PoissonSeriesParser<T>(polynomialParser, newFieldsPatterns, optional,
                                          gamma, firstDoodson, doodson, firstColumn,
                                          firstPlanetary, sinCosColumns, sinCosFactors);

    }

    /** Set up first column of planetary multiplier.
     * @param firstColumn column of the first planetary multiplier (counting from 1)
     * @return a new parser, with updated columns settings
     */
    public PoissonSeriesParser<T> withFirstPlanetary(final int firstColumn) {

        // update the fields pattern to expect 9 integers at the right indices
        final String[] newFieldsPatterns = fieldsPatterns.clone();
        setPatterns(newFieldsPatterns, firstPlanetary, 9, UNKNOWN_TYPE_PATTERN);
        setPatterns(newFieldsPatterns, firstColumn,    9, INTEGER_TYPE_PATTERN);

        return new PoissonSeriesParser<T>(polynomialParser, newFieldsPatterns, optional,
                                          gamma, firstDoodson, doodson, firstDelaunay,
                                          firstColumn, sinCosColumns, sinCosFactors);

    }

    /** Set up columns of the sine and cosine coefficients.
     * @param degree degree to set up
     * @param sinColumn column of the sine coefficient for t<sup>degree</sup> counting from 1
     * (may be -1 if there are no sine coefficients)
     * @param sinFactor multiplicative factor for the sine coefficient
     * @param cosColumn column of the cosine coefficient for t<sup>degree</sup> counting from 1
     * (may be -1 if there are no cosine coefficients)
     * @param cosFactor multiplicative factor for the cosine coefficient
     * @return a new parser, with updated columns settings
     */
    public PoissonSeriesParser<T> withSinCos(final int degree,
                                             final int sinColumn, final double sinFactor,
                                             final int cosColumn, final double cosFactor) {

        // update the sin/cos columns array
        final int      maxDegree        = FastMath.max(degree, sinCosColumns.length / 2 - 1);
        final int[]    newSinCosColumns = new int[2 * (maxDegree + 1)];
        Arrays.fill(newSinCosColumns, -1);
        System.arraycopy(sinCosColumns, 0, newSinCosColumns, 0, sinCosColumns.length);
        newSinCosColumns[2 * degree]     = sinColumn;
        newSinCosColumns[2 * degree + 1] = cosColumn;

        final double[] newSinCosFactors = new double[2 * (maxDegree + 1)];
        Arrays.fill(newSinCosFactors, Double.NaN);
        System.arraycopy(sinCosFactors, 0, newSinCosFactors, 0, sinCosFactors.length);
        newSinCosFactors[2 * degree]     = sinFactor;
        newSinCosFactors[2 * degree + 1] = cosFactor;

        // update the fields pattern to expect real numbers at the right indices
        final String[] newFieldsPatterns = fieldsPatterns.clone();
        if (2 * degree < sinCosColumns.length) {
            setPatterns(newFieldsPatterns, sinCosColumns[2 * degree], 1, UNKNOWN_TYPE_PATTERN);
        }
        setPatterns(newFieldsPatterns, sinColumn, 1, REAL_TYPE_PATTERN);
        if (2 * degree  + 1 < sinCosColumns.length) {
            setPatterns(newFieldsPatterns, sinCosColumns[2 * degree + 1], 1, UNKNOWN_TYPE_PATTERN);
        }
        setPatterns(newFieldsPatterns, cosColumn, 1, REAL_TYPE_PATTERN);

        return new PoissonSeriesParser<T>(polynomialParser, newFieldsPatterns, optional,
                                          gamma, firstDoodson, doodson, firstDelaunay,
                                          firstPlanetary, newSinCosColumns, newSinCosFactors);

    }

    /** Parse a stream.
     * @param stream stream containing the IERS table
     * @param name name of the resource file (for error messages only)
     * @return parsed Poisson series
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public PoissonSeries<T> parse(final InputStream stream, final String name) throws OrekitException {

        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
        }

        // the degrees section header should read something like:
        // j = 0  Nb of terms = 1306
        // or something like:
        // j = 0  Number  of terms = 1037
        final Pattern degreeSectionHeaderPattern =
            Pattern.compile("^\\p{Space}*j\\p{Space}*=\\p{Space}*(\\p{Digit}+)" +
                            "[\\p{Alpha}\\p{Space}]+=\\p{Space}*(\\p{Digit}+)\\p{Space}*$");

        // regular lines are simply a space separated list of numbers
        final StringBuilder builder = new StringBuilder("^\\p{Space}*");
        for (int i = 0; i < fieldsPatterns.length; ++i) {
            builder.append("(");
            builder.append(fieldsPatterns[i]);
            builder.append(")");
            builder.append((i < fieldsPatterns.length - 1) ? "\\p{Space}+" : "\\p{Space}*$");
        }
        final Pattern regularLinePattern = Pattern.compile(builder.toString());

        try {

            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            int lineNumber    =  0;
            int expectedIndex = -1;
            int nTerms        = -1;
            int count         =  0;
            int degree        =  0;

            // prepare the container for the parsed data
            PolynomialNutation<T> polynomial;
            if (polynomialParser == null) {
                // we don't expect any polynomial, we directly the the zero polynomial
                polynomial = new PolynomialNutation<T>(new double[0]);
            } else {
                // the dedicated parser will fill in the polynomial later
                polynomial = null;
            }
            final Map<Long, SeriesTerm<T>> series = new HashMap<Long, SeriesTerm<T>>();

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

                    // get the Doodson multipliers as well as the Doodson number
                    final int cTau     = (firstDoodson < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstDoodson));
                    final int cS       = (firstDoodson < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstDoodson + 1));
                    final int cH       = (firstDoodson < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstDoodson + 2));
                    final int cP       = (firstDoodson < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstDoodson + 3));
                    final int cNprime  = (firstDoodson < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstDoodson + 4));
                    final int cPs      = (firstDoodson < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstDoodson + 5));
                    final int nDoodson = (doodson      < 0) ? 0 : Integer.parseInt(regularMatcher.group(doodson).replaceAll("[.,]", ""));

                    // get the tide multipler
                    int cGamma   = (gamma < 0) ? 0 : Integer.parseInt(regularMatcher.group(gamma));

                    // get the Delaunay multipliers
                    int cL       = Integer.parseInt(regularMatcher.group(firstDelaunay));
                    int cLPrime  = Integer.parseInt(regularMatcher.group(firstDelaunay + 1));
                    int cF       = Integer.parseInt(regularMatcher.group(firstDelaunay + 2));
                    int cD       = Integer.parseInt(regularMatcher.group(firstDelaunay + 3));
                    int cOmega   = Integer.parseInt(regularMatcher.group(firstDelaunay + 4));

                    // get the planetary multipliers
                    final int cMe      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary));
                    final int cVe      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 1));
                    final int cE       = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 2));
                    final int cMa      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 3));
                    final int cJu      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 4));
                    final int cSa      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 5));
                    final int cUr      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 6));
                    final int cNe      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 7));
                    final int cPa      = (firstPlanetary < 0) ? 0 : Integer.parseInt(regularMatcher.group(firstPlanetary + 8));

                    if (nDoodson > 0) {

                        // set up the traditional parameters corresponding to the Doodson arguments
                        cGamma  = cTau;
                        cL      = -cL;
                        cLPrime = -cLPrime;
                        cF      = -cF;
                        cD      = -cD;
                        cOmega  = -cOmega;

                        // check Doodson number, Doodson multiplers and Delaunay multipliers consistency
                        if (nDoodson != doodsonToDoodsonNumber(cTau, cS, cH, cP, cNprime, cPs) ||
                            nDoodson != delaunayToDoodsonNumber(cGamma, cL, cLPrime, cF, cD, cOmega)) {
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, regularMatcher.group());
                        }

                    }

                    final long key = NutationCodec.encode(cGamma, cL, cLPrime, cF, cD, cOmega,
                                                          cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);

                    // retrieved the term, or build it if it's the first time it is encountered in the file
                    final SeriesTerm<T> term;
                    if (series.containsKey(key)) {
                        // the term was already known, from another degree
                        term = series.get(key);
                    } else {
                        // the term is a new one
                        term = SeriesTerm.buildTerm(cGamma, cL, cLPrime, cF, cD, cOmega,
                                                    cMe, cVe, cE, cMa, cJu, cSa, cUr, cNe, cPa);
                    }

                    boolean nonZero = false;
                    for (int d = 0; d < sinCosColumns.length / 2; ++d) {
                        final double sinCoeff =
                                parseCoefficient(regularMatcher, sinCosColumns[2 * d],     sinCosFactors[2 * d]);
                        final double cosCoeff =
                                parseCoefficient(regularMatcher, sinCosColumns[2 * d + 1], sinCosFactors[2 * d + 1]);
                        if (!Precision.equals(sinCoeff, 0.0, 0) || !Precision.equals(cosCoeff, 0.0, 0)) {
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
                            polynomial = new PolynomialNutation<T>(coefficients);
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

            // build the series
            return new PoissonSeries<T>(polynomial, series);

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

    /** Compute Doodson number from Delaunay multipliers.
     * @param cGamma coefficient for γ = GMST + π tide parameter
     * @param cL coefficient for mean anomaly of the Moon
     * @param cLPrime coefficient for mean anomaly of the Sun
     * @param cF coefficient for L - Ω where L is the mean longitude of the Moon
     * @param cD coefficient for mean elongation of the Moon from the Sun
     * @param cOmega coefficient for mean longitude of the ascending node of the Moon
     * @return computed Doodson number
     */
    private int delaunayToDoodsonNumber(final int cGamma,
                                        final int cL, final int cLPrime, final int cF,
                                        final int cD, final int cOmega) {

        // reconstruct Doodson multipliers from gamma and Delaunay multipliers
        final int cTau    = cGamma;
        final int cS      = cGamma + (cL + cF + cD);
        final int cH      = cLPrime - cD;
        final int cP      = -cL;
        final int cNprime = cF - cOmega;
        final int cPs     = -cLPrime;

        return doodsonToDoodsonNumber(cTau, cS, cH, cP, cNprime, cPs);

    }

    /** Compute Doodson number from Doodson multipliers.
     * @param cTau coefficient for mean lunar time
     * @param cS coefficient for mean longitude of the Moon
     * @param cH coefficient for mean longitude of the Sun
     * @param cP coefficient for longitude of Moon mean perigee
     * @param cNprime negative of the longitude of the Moon's mean ascending node on the ecliptic
     * @param cPs coefficient for longitude of Sun mean perigee
     * @return computed Doodson number
     */
    private int doodsonToDoodsonNumber(final int cTau,
                                       final int cS, final int cH, final int cP,
                                       final int cNprime, final int cPs) {

        return ((((cTau * 10 + (cS + 5)) * 10 + (cH + 5)) * 10 + (cP + 5)) * 10 + (cNprime + 5)) * 10 + (cPs + 5);

    }

}
