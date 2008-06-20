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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;


/**
 * Class representing a development for nutation or GST computations.
 * <p>
 * Developments are composed of a time polynomial part and a non-polynomial
 * part which consist in summation series. The {@link SeriesTerm series terms}
 * are harmonic functions (combination of sines and cosines) of general
 * <em>arguments</em>. The arguments are combination of luni-solar or
 * planetary {@link BodiesElements elements}.
 * </p>
 *
 * @author Luc Maisonobe
 * @see SeriesTerm
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class Development implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -3016824169123970737L;

    /** Error message for non IERS files. */
    private static final String NOT_IERS_FILE =
        "file {0} is not an IERS data file";

    /** Coefficients of the polynomial part. */
    private double[] coefficients;

    /** Non-polynomial series. */
    private SeriesTerm[][] series;

    /** Build a development from an IERS table file.
     * @param stream stream containing the IERS table
     * @param factor multiplicative factor to use for coefficients
     * @param name name of the resource file (for error messages only)
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public Development(final InputStream stream, final double factor, final String name)
        throws OrekitException {

        if (stream == null) {
            throw new OrekitException("unable to find nutation model file {0}",
                                      new Object[] {
                                          name
                                      });
        }

        try {
            // the polynomial part should read something like:
            // -16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5
            // or something like:
            // 0''.014506 + 4612''.15739966t + 1''.39667721t^2 - 0''.00009344t^3 + 0''.00001882t^4
            final Pattern termPattern =
                Pattern.compile("\\p{Space}*([-+]?)" +
                                "\\p{Space}*(\\p{Digit}+)(?:'')?(\\.\\p{Digit}+)" +
                                "(?:\\p{Space}*t(?:\\^\\p{Digit}+)?)?");

            // the series parts should read something like:
            // j = 0  Nb of terms = 1306
            //
            //  1    -6844318.44        1328.67    0    0    0    0    1    0    0    0    0    0    0    0    0    0
            //  2     -523908.04        -544.76    0    0    2   -2    2    0    0    0    0    0    0    0    0    0
            //  3      -90552.22         111.23    0    0    2    0    2    0    0    0    0    0    0    0    0    0
            //  4       82168.76         -27.64    0    0    0    0    2    0    0    0    0    0    0    0    0    0
            final Pattern seriesHeaderPattern =
                Pattern.compile("^\\p{Space}*j\\p{Space}*=\\p{Space}*(\\p{Digit}+)" +
                                ".*=\\p{Space}*(\\p{Digit}+)\\p{Space}*$");


            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = reader.readLine();
            int lineNumber = 1;

            // look for the polynomial part
            while (line != null) {
                if (parsePolynomial(termPattern.matcher(line), factor)) {
                    // we have parsed the polynomial part
                    line = null;
                } else {
                    // we are still in the header
                    line = reader.readLine();
                    ++lineNumber;
                }
            }
            if (coefficients == null) {
                throw new OrekitException(NOT_IERS_FILE,
                                          new Object[] {
                                              name
                                          });
            }

            line = reader.readLine();
            ++lineNumber;

            // look for the non-polynomial part
            final List<SeriesTerm[]> array = new ArrayList<SeriesTerm[]>();
            while (line != null) {
                final int nTerms = parseSeriesHeader(seriesHeaderPattern.matcher(line),
                                                     array.size(), name, lineNumber);
                if (nTerms >= 0) {
                    // we have found a non-polynomial series

                    // skip blank lines
                    line = reader.readLine();
                    ++lineNumber;
                    while ((line != null) && (line.trim().length() == 0)) {
                        line = reader.readLine();
                        ++lineNumber;
                    }

                    // read the terms of the current serie
                    final SeriesTerm[] serie = new SeriesTerm[nTerms];
                    for (int i = 0; i < nTerms; ++i) {
                        serie[i] = parseSeriesTerm(line, factor, name, lineNumber);
                        line = reader.readLine();
                        ++lineNumber;
                    }

                    // the serie has been completed, store it
                    array.add(serie);

                } else {
                    // we are still in the intermediate lines
                    line = reader.readLine();
                    ++lineNumber;
                }
            }

            if (array.isEmpty()) {
                throw new OrekitException(NOT_IERS_FILE,
                                          new Object[] {
                                              name
                                          });
            }

            // store the non-polynomial part series
            series = (SeriesTerm[][]) array.toArray(new SeriesTerm[array.size()][]);

        } catch (NumberFormatException nfe) {
            throw new OrekitException(nfe.getMessage(), nfe);
        } catch (IOException ioe) {
            throw new OrekitException(ioe.getMessage(), ioe);
        }

    }

    /** Parse a polynomial description line.
     * @param termMatcher matcher for the polynomial terms
     * @param factor multiplicative factor to use for coefficients
     * @return true if the line was parsed successfully
     */
    private boolean parsePolynomial(final Matcher termMatcher, final double factor) {

        // parse the polynomial one polynomial term after the other
        if (!termMatcher.lookingAt()) {
            return false;
        }

        // store the concatenated sign, integer and fractional parts of the monomial coefficient
        final List<String> coeffs = new ArrayList<String>();
        do {
            coeffs.add(termMatcher.group(1) + termMatcher.group(2) + termMatcher.group(3));
        } while (termMatcher.find());

        // parse the coefficients
        coefficients = new double[coeffs.size()];
        for (int i = 0; i < coefficients.length; ++i) {
            coefficients[i] = factor * Double.parseDouble(coeffs.get(i));
        }

        return true;

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
            throw new OrekitException("missing serie j = {0} in nutation " +
                                      "model file {1} (line {2})",
                                      new Object[] {
                                          Integer.valueOf(expected),
                                          name,
                                          Integer.valueOf(lineNumber)
                                      });
        }

        return Integer.parseInt(headerMatcher.group(2));

    }

    /** Parse a series term line.
     * @param line data line to parse
     * @param factor multiplicative factor to use for coefficients
     * @param name name of the resource file (for error messages only)
     * @param lineNumber line number (for error messages only)
     * @return a series term
     * @exception OrekitException if the line is null or cannot be parsed
     */
    private SeriesTerm parseSeriesTerm (final String line, final double factor,
                                        final String name, final int lineNumber)
        throws OrekitException {

        // sanity check
        if (line == null) {
            throw new OrekitException("unexpected end of nutation model file {0} (after line {1})",
                                      new Object[] {
                                          name, Integer.valueOf(lineNumber - 1)
                                      });
        }

        // parse the nutation serie term
        final String[] fields = line.split("\\p{Space}+");
        final int l = fields.length;
        if ((l == 17) || ((l == 18) && (fields[0].length() == 0))) {
            return SeriesTerm.buildTerm(Double.parseDouble(fields[l - 16]) * factor,
                                        Double.parseDouble(fields[l - 15]) * factor,
                                        Integer.parseInt(fields[l - 14]), Integer.parseInt(fields[l - 13]),
                                        Integer.parseInt(fields[l - 12]), Integer.parseInt(fields[l - 11]),
                                        Integer.parseInt(fields[l - 10]), Integer.parseInt(fields[l -  9]),
                                        Integer.parseInt(fields[l -  8]), Integer.parseInt(fields[l -  7]),
                                        Integer.parseInt(fields[l -  6]), Integer.parseInt(fields[l -  5]),
                                        Integer.parseInt(fields[l -  4]), Integer.parseInt(fields[l -  3]),
                                        Integer.parseInt(fields[l -  2]), Integer.parseInt(fields[l -  1]));
        }

        throw new OrekitException("unable to parse line {0} of nutation model file {1}:\n{2}",
                                  new Object[] {
                                      Integer.toString(lineNumber), name, line
                                  });

    }

    /** Compute the value of the development for the current date.
     * @param t current date
     * @param elements luni-solar and planetary elements for the current date
     * @return current value of the development
     */
    public double value(final double t, final BodiesElements elements) {

        // polynomial part
        double p = 0;
        for (int i = coefficients.length - 1; i >= 0; --i) {
            p = p * t + coefficients[i];
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

            np = np * t + s;

        }

        // add the polynomial and the non-polynomial parts
        return p + np;

    }

}
