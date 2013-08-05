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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.exception.util.DummyLocalizable;
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
 *
 * @author Luc Maisonobe
 * @see SeriesTerm
 * @see PolynomialNutation
 */
public class PoissonSeries implements NutationFunction, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130728L;

    /** Coefficients of the polynomial part. */
    private double[] coefficients;

    /** Non-polynomial series. */
    private SeriesTerm[][] series;

    /** Build a Poisson series from an IERS table file.
     * @param stream stream containing the IERS table
     * @param name name of the resource file (for error messages only)
     * @param polyFactor multiplicative factor to use for polynomial coefficients
     * @param nonPolyFactor multiplicative factor to use for non-ploynomial coefficients
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public PoissonSeries(final InputStream stream, final String name,
                         final double polyFactor, final double nonPolyFactor)
        throws OrekitException {

        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
        }

        try {
            // the polynomial part should read something like:
            // - 16617. + 2004191898. t - 429782.9 t^2 - 198618.34 t^3 + 7.578 t^4 + 5.9285 t^5
            // or something like:
            // 0''.014506 + 4612''.15739966t + 1''.39667721t^2 - 0''.00009344t^3 + 0''.00001882t^4
            final Pattern termPattern =
                Pattern.compile("\\p{Space}*([-+]?)" +
                                "\\p{Space}*(\\p{Digit}+)(?:'')?(\\.\\p{Digit}*)" +
                                "(?:\\p{Space}*t(?:\\^\\p{Digit}+)?)?");

            // the series parts should read something like:
            // j = 0  Nb of terms = 1306
            //
            //  1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0
            //  2     -523908.04        -544.76    0    0    2   -2    2    0    0    0    0    0    0    0    0    0
            //  3      -90552.22         111.23    0    0    2    0    2    0    0    0    0    0    0    0    0    0
            //  4       82168.76         -27.64    0    0    0    0    2    0    0    0    0    0    0    0    0    0
            //
            // or something like:
            //
            // ----------------------------------------------------------------------------------------------------------
            // j = 0  Number  of terms = 1037
            // ----------------------------------------------------------------------------------------------------------
            //     i         B"_i            B_i      l    l'   F    D   Om  L_Me L_Ve  L_E L_Ma  L_J L_Sa  L_U L_Ne  p_A
            // ----------------------------------------------------------------------------------------------------------
            //     1        1537.70     9205233.10    0    0    0    0    1    0    0    0    0    0    0    0    0    0
            //     2        -458.70      573033.60    0    0    2   -2    2    0    0    0    0    0    0    0    0    0
            final Pattern seriesHeaderPattern =
                Pattern.compile("^\\p{Space}*j\\p{Space}*=\\p{Space}*(\\p{Digit}+)" +
                                ".*=\\p{Space}*(\\p{Digit}+)\\p{Space}*$");


            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            String line = reader.readLine();
            int lineNumber    = 1;
            int expectedIndex = 1;
            final List<SeriesTerm[]> array = new ArrayList<SeriesTerm[]>();

            while (line != null) {

                final int nTerms = parseSeriesHeader(seriesHeaderPattern.matcher(line),
                                                     array.size(), name, lineNumber);
                if (nTerms >= 0) {

                    // we have found a non-polynomial series

                    if (coefficients == null) {
                        // since non-polynomial part starts after polynomial part,
                        // getting here means there are no coefficients at all
                        coefficients = new double[0];
                    }

                    // skip sub-headers lines
                    line = reader.readLine();
                    ++lineNumber;
                    while (line != null) {
                        final String fields[] = line.trim().split(" +");
                        if (fields.length > 0 && Integer.toString(expectedIndex).equals(fields[0])) {
                            // we have found the first line we are interested in
                            break;
                        }
                        line = reader.readLine();
                        ++lineNumber;
                    }

                    // read the terms of the current series
                    final SeriesTerm[] serie = new SeriesTerm[nTerms];
                    for (int i = 0; i < nTerms; ++i) {
                        serie[i] =
                                parseSeriesTerm(line, nonPolyFactor, expectedIndex, name, lineNumber);
                        line = reader.readLine();
                        ++lineNumber;
                        ++expectedIndex;
                    }

                    // the series has been completed, store it
                    array.add(serie);

                } else {

                    if (coefficients == null) {
                        // look for the polynomial part
                        coefficients = parsePolynomial(termPattern.matcher(line), polyFactor);
                    }

                    // we are still in the header
                    line = reader.readLine();
                    ++lineNumber;

                }

            }

            if (coefficients == null || array.isEmpty()) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

            // store the non-polynomial part series
            series = (SeriesTerm[][]) array.toArray(new SeriesTerm[array.size()][]);

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

    }

    /** Parse a polynomial description line.
     * @param termMatcher matcher for the polynomial terms
     * @param polyFactor multiplicative factor to use for polynomial coefficients
     * @return parsed coefficients, or null if no coefficients found
     */
    private double[] parsePolynomial(final Matcher termMatcher, final double polyFactor) {

        // parse the polynomial one polynomial term after the other
        if (!termMatcher.lookingAt()) {
            return null;
        }

        // store the concatenated sign, integer and fractional parts of the monomial coefficient
        final List<String> coeffs = new ArrayList<String>();
        do {
            coeffs.add(termMatcher.group(1) + termMatcher.group(2) + termMatcher.group(3));
        } while (termMatcher.find());

        // parse the coefficients
        final double[] c = new double[coeffs.size()];
        for (int i = 0; i < c.length; ++i) {
            c[i] = polyFactor * Double.parseDouble(coeffs.get(i));
        }

        return c;

    }

    /** Parse a series header line.
     * @param headerMatcher matcher for the series header line
     * @param expected expected series index
     * @param name name of the resource file (for error messages only)
     * @param lineNumber line number (for error messages only)
     * @return the number of terms in the series (-1 if the line
     * cannot be parsed)
     * @exception OrekitException if the header does not match
     * the expected series number
     */
    private int parseSeriesHeader(final Matcher headerMatcher, final int expected,
                                  final String name, final int lineNumber)
        throws OrekitException {

        // is this a series header line ?
        if (!headerMatcher.matches()) {
            return -1;
        }

        // sanity check
        if (Integer.parseInt(headerMatcher.group(1)) != expected) {
            throw new OrekitException(OrekitMessages.MISSING_SERIE_J_IN_FILE,
                                      expected, name, lineNumber);
        }

        return Integer.parseInt(headerMatcher.group(2));

    }

    /** Parse a series term line.
     * @param line data line to parse
     * @param nonPolyFactor multiplicative factor to use for non-polynomial coefficients
     * @param expectedIndex expected index of the series term
     * @param name name of the resource file (for error messages only)
     * @param lineNumber line number (for error messages only)
     * @return a series term
     * @exception OrekitException if the line is null or cannot be parsed
     */
    private SeriesTerm parseSeriesTerm (final String line, final double nonPolyFactor,
                                        final int expectedIndex,
                                        final String name, final int lineNumber)
        throws OrekitException {

        // sanity check
        if (line == null) {
            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      name, lineNumber - 1);
        }

        // parse the Poisson series term
        final String[] fields = line.trim().split("\\p{Space}+");
        final int l = fields.length;
        if (l == 17 && Integer.parseInt(fields[0]) == expectedIndex) {
            return SeriesTerm.buildTerm(Double.parseDouble(fields[l - 16]) * nonPolyFactor,
                                        Double.parseDouble(fields[l - 15]) * nonPolyFactor,
                                        Integer.parseInt(fields[l - 14]), Integer.parseInt(fields[l - 13]),
                                        Integer.parseInt(fields[l - 12]), Integer.parseInt(fields[l - 11]),
                                        Integer.parseInt(fields[l - 10]), Integer.parseInt(fields[l -  9]),
                                        Integer.parseInt(fields[l -  8]), Integer.parseInt(fields[l -  7]),
                                        Integer.parseInt(fields[l -  6]), Integer.parseInt(fields[l -  5]),
                                        Integer.parseInt(fields[l -  4]), Integer.parseInt(fields[l -  3]),
                                        Integer.parseInt(fields[l -  2]), Integer.parseInt(fields[l -  1]));
        }

        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                  lineNumber, name, line);

    }

    /** {@inheritDoc} */
    public double value(final BodiesElements elements) {

        final double tc = elements.getTC();

        // polynomial part
        double p = 0;
        for (int i = coefficients.length - 1; i >= 0; --i) {
            p = p * tc + coefficients[i];
        }

        // non-polynomial part
        double np = 0;
        for (int i = series.length - 1; i >= 0; --i) {

            final SeriesTerm[] serie = series[i];

            // add the harmonic terms starting from the last (smallest) terms,
            // to avoid numerical problems
            double s = 0;
            for (int k = serie.length - 1; k >= 0; --k) {
                s += serie[k].value(elements);
            }

            np = np * tc + s;

        }

        // add the polynomial and the non-polynomial parts
        return p + np;

    }

}
