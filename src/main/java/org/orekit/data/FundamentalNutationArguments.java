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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/**
 * Class computing the fundamental arguments for nutation and tides.
 * <p>
 * The fundamental arguments are split in two sets:
 * </p>
 * <ul>
 *   <li>the Delaunay arguments for Moon and Sun effects</li>
 *   <li>the planetary arguments for other planets</li>
 * </ul>
 *
 * @author Luc Maisonobe
 * @see SeriesTerm
 * @see PoissonSeries
 * @see BodiesElements
 */
public class FundamentalNutationArguments implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130418L;

    /** Reference epoch.*/
    private final AbsoluteDate reference;

    // luni-solar Delaunay arguments

    /** Coefficients for mean anomaly of the Moon. */
    private final double[] lCoefficients;

    /** Coefficients for mean anomaly of the Sun. */
    private final double[] lPrimeCoefficients;

    /** Coefficients for L - &Omega; where L is the mean longitude of the Moon. */
    private final double[] fCoefficients;

    /** Coefficients for mean elongation of the Moon from the Sun. */
    private final double[] dCoefficients;

    /** Coefficients for mean longitude of the ascending node of the Moon. */
    private final double[] omegaCoefficients;

    // planetary nutation arguments

    /** Coefficients for mean Mercury longitude. */
    private final double[] lMeCoefficients;

    /** Coefficients for mean Venus longitude. */
    private final double[] lVeCoefficients;

    /** Coefficients for mean Earth longitude. */
    private final double[] lECoefficients;

    /** Coefficients for mean Mars longitude. */
    private final double[] lMaCoefficients;

    /** Coefficients for mean Jupiter longitude. */
    private final double[] lJCoefficients;

    /** Coefficients for mean Saturn longitude. */
    private final double[] lSaCoefficients;

    /** Coefficients for mean Uranus longitude. */
    private final double[] lUCoefficients;

    /** Coefficients for mean Neptune longitude. */
    private final double[] lNeCoefficients;

    /** Coefficients for general accumulated precession. */
    private final double[] paCoefficients;

    /** Build a model of fundamental arguments from an IERS table file.
     * @param stream stream containing the IERS table
     * @param name name of the resource file (for error messages only)
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public FundamentalNutationArguments(final InputStream stream, final String name)
        throws OrekitException {

        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
        }

        try {

            final DateParser dateParser             = new DateParser();
            final DefinitionParser definitionParser = new DefinitionParser();

            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            int lineNumber = 0;

            // look for the reference date and the 14 polynomials
            final Map<FundamentalName, double[]> polynomials = new HashMap<FundamentalName, double[]>(14);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                if (!dateParser.parse(line)) {
                    if (definitionParser.parseDefinition(line, lineNumber, name)) {
                        polynomials.put(definitionParser.getParsedName(),
                                        definitionParser.getParsedPolynomial());
                    }
                }
            }

            reference          = dateParser.getParsedDate(name);
            lCoefficients      = getCoefficients(FundamentalName.L,       polynomials, name);
            lPrimeCoefficients = getCoefficients(FundamentalName.L_PRIME, polynomials, name);
            fCoefficients      = getCoefficients(FundamentalName.F,       polynomials, name);
            dCoefficients      = getCoefficients(FundamentalName.D,       polynomials, name);
            omegaCoefficients  = getCoefficients(FundamentalName.OMEGA,   polynomials, name);
            if (polynomials.containsKey(FundamentalName.L_ME)) {
                // IERS conventions 2003 and later provide planetary nutation arguments
                lMeCoefficients = getCoefficients(FundamentalName.L_ME,    polynomials, name);
                lVeCoefficients = getCoefficients(FundamentalName.L_VE,    polynomials, name);
                lECoefficients  = getCoefficients(FundamentalName.L_E,     polynomials, name);
                lMaCoefficients = getCoefficients(FundamentalName.L_MA,    polynomials, name);
                lJCoefficients  = getCoefficients(FundamentalName.L_J,     polynomials, name);
                lSaCoefficients = getCoefficients(FundamentalName.L_SA,    polynomials, name);
                lUCoefficients  = getCoefficients(FundamentalName.L_U,     polynomials, name);
                lNeCoefficients = getCoefficients(FundamentalName.L_NE,    polynomials, name);
                paCoefficients  = getCoefficients(FundamentalName.PA,      polynomials, name);
            } else {
                // IERS conventions 1996 and earlier don't provide planetary nutation arguments
                final double[] zero = new double[] {
                    0.0
                };
                lMeCoefficients = zero;
                lVeCoefficients = zero;
                lECoefficients  = zero;
                lMaCoefficients = zero;
                lJCoefficients  = zero;
                lSaCoefficients = zero;
                lUCoefficients  = zero;
                lNeCoefficients = zero;
                paCoefficients  = zero;
            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }

    }

    /** Get the coefficients for a fundamental argument.
     * @param argument fundamental argument
     * @param polynomials map of the polynomials
     * @param fileName name of the file from which the coefficients have been read
     * @return polynomials coefficients (ordered from high degrees to low degrees)
     * @exception OrekitException if the argument is not found
     */
    private double[] getCoefficients(final FundamentalName argument,
                                     final Map<FundamentalName, double[]> polynomials,
                                     final String fileName)
        throws OrekitException {
        if (!polynomials.containsKey(argument)) {
            throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, fileName);
        }
        return polynomials.get(argument);
    }

    /** Evaluate a polynomial.
     * @param tc offset in Julian centuries
     * @param coefficients polynomial coefficients (ordered from high degrees to low degrees)
     * @return value of the polynomial
     */
    private double value(final double tc, final double[] coefficients) {
        double value = coefficients[0];
        for (int i = 1; i < coefficients.length; ++i) {
            value = coefficients[i] + tc * value;
        }
        return value;
    }

    /** Evaluate Delaunay arguments for the current date.
     * @param date current date
     * @return Delaunay arguments for the current date
     */
    public DelaunayArguments evaluateDelaunay(final AbsoluteDate date) {

        // offset in julian centuries
        final double tc =  date.durationFrom(reference) / Constants.JULIAN_CENTURY;

        return new DelaunayArguments(date, tc,
                                     value(tc, lCoefficients),      // mean anomaly of the Moon
                                     value(tc, lPrimeCoefficients), // mean anomaly of the Sun
                                     value(tc, fCoefficients),      // L - &Omega; where L is the mean longitude of the Moon
                                     value(tc, dCoefficients),      // mean elongation of the Moon from the Sun
                                     value(tc, omegaCoefficients)); // mean longitude of the ascending node of the Moon

    }

    /** Evaluate all fundamental arguments for the current date (Delaunay plus planetary).
     * @param date current date
     * @return all fundamental arguments for the current date (Delaunay plus planetary)
     */
    public BodiesElements evaluateAll(final AbsoluteDate date) {

        // offset in julian centuries
        final double tc =  date.durationFrom(reference) / Constants.JULIAN_CENTURY;

        return new BodiesElements(date, tc,
                                  value(tc, lCoefficients),      // mean anomaly of the Moon
                                  value(tc, lPrimeCoefficients), // mean anomaly of the Sun
                                  value(tc, fCoefficients),      // L - &Omega; where L is the mean longitude of the Moon
                                  value(tc, dCoefficients),      // mean elongation of the Moon from the Sun
                                  value(tc, omegaCoefficients),  // mean longitude of the ascending node of the Moon
                                  value(tc, lMeCoefficients),    // mean Mercury longitude
                                  value(tc, lVeCoefficients),    // mean Venus longitude
                                  value(tc, lECoefficients),     // mean Earth longitude
                                  value(tc, lMaCoefficients),    // mean Mars longitude
                                  value(tc, lJCoefficients),     // mean Jupiter longitude
                                  value(tc, lSaCoefficients),    // mean Saturn longitude
                                  value(tc, lUCoefficients),     // mean Uranus longitude
                                  value(tc, lNeCoefficients),    // mean Neptune longitude
                                  value(tc, paCoefficients));    // general accumulated precession in longitude

    }

    /** Enumerate for the fundamental names. */
    private enum FundamentalName {

        /** Constant for Mean anomaly of the Moon. */
        L() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "l";
            }
        },

        /** Constant for Mean anomaly of the Sun. */
        L_PRIME() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "l'";
            }
        },

        /** Constant for L - &Omega; where L is the mean longitude of the Moon. */
        F() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "F";
            }
        },

        /** Constant for mean elongation of the Moon from the Sun. */
        D() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "D";
            }
        },

        /** Constant for longitude of the ascending node of the Moon. */
        OMEGA() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "\u03a9";
            }
        },

        /** Constant for mean Mercury longitude. */
        L_ME() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LMe";
            }
        },

        /** Constant for mean Venus longitude. */
        L_VE() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LVe";
            }
        },

        /** Constant for mean Earth longitude. */
        L_E() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LE";
            }
        },

        /** Constant for mean Mars longitude. */
        L_MA() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LMa";
            }
        },

        /** Constant for mean Jupiter longitude. */
        L_J() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LJ";
            }
        },

        /** Constant for mean Saturn longitude. */
        L_SA() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LSa";
            }
        },

        /** Constant for mean Uranus longitude. */
        L_U() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LU";
            }
        },

        /** Constant for mean Neptune longitude. */
        L_NE() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "LNe";
            }
        },

        /** Constant for general accumulated precession in longitude. */
        PA() {
            /** {@inheritDoc} */
            public String getArgumentName() {
                return "pA";
            }
        };

        /** Get the fundamental name.
         * @return fundamental name
         */
        public abstract String getArgumentName();

    }

    /** Local parser for dates. */
    private static class DateParser {

        /** Regular expression pattern for date lines. */
        private final Pattern pattern;

        /** Last parsed date. */
        private AbsoluteDate parsedDate;

        /** Simple constructor.
         */
        public DateParser() {
            // the reference date should read something like:
            // reference = 2000-01-01T12:00:00 TT
            pattern = Pattern.compile("\\p{Space}*reference\\p{Space}*=\\p{Space}*" +
                                      "(\\p{Digit}{4}-\\p{Digit}{2}-\\p{Digit}{2}" +
                                      'T' +
                                      "\\p{Digit}{2}:\\p{Digit}{2}:\\p{Digit}{2}(?:\\.\\p{Digit}*)?)" +
                                      "\\p{Space}*TT");
        }

        /** Parse a reference date.
         * @param line line to parse
         * @return true if a date has been parsed
         */
        public boolean parse(final String line) {
            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                parsedDate = new AbsoluteDate(matcher.group(1), TimeScalesFactory.getTT());
                return true;
            } else {
                return false;
            }
        }

        /** Get last parsed date.
         * @param name name of the file parsed
         * @return last parsed date
         * @exception OrekitException if no date has been parsed
         */
        public AbsoluteDate getParsedDate(final String name) throws OrekitException {
            if (parsedDate == null) {
                // this should never happen with files embedded within Orekit
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }
            return parsedDate;
        }

    }

    /** Local parser for argument definition lines. */
    private static class DefinitionParser {

        /** Regular expression pattern for definitions. */
        private final Pattern pattern;

        /** Parser for monomials. */
        private MonomialParser monomialParser;

        /** Last parsed fundamental name. */
        private FundamentalName parsedName;

        /** Last parsed polynomial. */
        private double[] parsedPolynomial;

        /** Simple constructor. */
        public DefinitionParser() {

            // the luni-solar Delaunay arguments polynomial parts should read something like:
            // F5 ≡ Ω = 125.04455501° − 6962890.5431″t + 7.4722″t² + 0.007702″t³ − 0.00005939″t⁴
            // whereas the planetary arguments polynomial parts should read something like:
            // F14 ≡ pA  = 0.02438175 × t + 0.00000538691 × t²
            final String unicodeIdenticalTo = "\u2261";

            // pattern for the global line
            final StringBuilder builder = new StringBuilder();
            for (final FundamentalName fn : FundamentalName.values()) {
                if (builder.length() > 0) {
                    builder.append('|');
                }
                builder.append(fn.getArgumentName());
            }
            final String fundamentalName = "\\p{Space}*((?:" + builder.toString() + ")+)";
            pattern = Pattern.compile("\\p{Space}*F\\p{Digit}+\\p{Space}*" + unicodeIdenticalTo +
                                      fundamentalName + "\\p{Space}*=\\p{Space}*(.*)");

            monomialParser = new MonomialParser();

        }

        /** Parse a definition line.
         * @param line line to parse
         * @param lineNumber line number
         * @param fileName name of the file
         * @return true if a definition has been parsed
         * @exception OrekitException if polynomial cannot be parsed
         */
        public boolean parseDefinition(final String line, final int lineNumber, final String fileName)
            throws OrekitException {

            parsedName       = null;
            parsedPolynomial = null;

            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                for (FundamentalName fn : FundamentalName.values()) {
                    if (fn.getArgumentName().equals(matcher.group(1))) {
                        parsedName = fn;
                    }
                }

                // parse the polynomial
                final Map<Integer, Double> coefficients = new HashMap<Integer, Double>();
                int maxDegree = -1;
                monomialParser.initialize(matcher.group(2));
                while (monomialParser.parseNext()) {
                    maxDegree = FastMath.max(maxDegree, monomialParser.getParsedPower());
                    coefficients.put(monomialParser.getParsedPower(),
                                     monomialParser.getParsedCoefficient());
                }
                parsedPolynomial = new double[maxDegree + 1];
                for (Map.Entry<Integer, Double> entry : coefficients.entrySet()) {
                    parsedPolynomial[maxDegree - entry.getKey()] = entry.getValue();
                }

                return true;

            } else {
                return false;
            }

        }

        /** Get the last parsed fundamental name.
         * @return last parsed fundamental name
         */
        public FundamentalName getParsedName() {
            return parsedName;
        }

        /** Get the last parsed polynomial.
         * @return last parsed polynomial
         */
        public double[] getParsedPolynomial() {
            return parsedPolynomial.clone();
        }

    }

    /** Local parser for monomials. */
    private static class MonomialParser {

        /** Constant for degree unit. */
        private static final String UNICODE_DEGREE              = "\u00b0";

        /** Constant for white bullet used as degree unit. */
        private static final String UNICODE_WHITE_BULLET        = "\u25e6";

        /** Constant for arc-seconds unit. */
        private static final String UNICODE_DOUBLE_PRIME        = "\u2033";

        /** Constant for apostrophes used as arc-seconds unit. */
        private static final String UNICODE_DOUBLED_APOSTROPHE  = "''";

        /** Constant for quotation mark used as arc-seconds unit. */
        private static final String UNICODE_QUOTATION_MARK      = "\"";

        /** Constant for multiplication sign. */
        private static final String UNICODE_MULTIPLICATION_SIGN = "\u00d7";

        /** Constant for minus sign. */
        private static final String UNICODE_MINUS_SIGN          = "\u2212";

        /** Constant for power 2. */
        private static final String UNICODE_SUPERSCRIPT_2       = "\u00b2";

        /** Constant for power 3. */
        private static final String UNICODE_SUPERSCRIPT_3       = "\u00b3";

        /** Constant for power 4. */
        private static final String UNICODE_SUPERSCRIPT_4       = "\u2074";

        /** Constant for power 5. */
        private static final String UNICODE_SUPERSCRIPT_5       = "\u2075";

        // current IERS models don't use yet degree 5 or higher, so the previous
        // constants are sufficient for now. If IERS increase degrees, unicode has
        // special characters for up to degree 9 in the range 2076 to 2079

        /** Regular expression pattern for monomials. */
        private final Pattern pattern;

        /** Matcher for a definition. */
        private Matcher matcher;

        /** Start index for next search. */
        private int next;

        /** Last parsed coefficient. */
        private double parsedCoefficient;

        /** Last parsed power. */
        private int parsedPower;

        /** Simple constructor. */
        public MonomialParser() {

            // the luni-solar Delaunay arguments polynomial parts should read something like:
            // F5 ≡ Ω = 125.04455501° − 6962890.5431″t + 7.4722″t² + 0.007702″t³ − 0.00005939″t⁴
            // whereas the planetary arguments polynomial parts should read something like:
            // F14 ≡ pA  = 0.02438175 × t + 0.00000538691 × t²
            final String space           = "\\p{Space}*";
            final String sign            = "([-" + UNICODE_MINUS_SIGN + "+]?)";
            final String mantissa        = "(\\p{Digit}+\\.\\p{Digit}*|\\.\\p{Digit}+)";
            final String unit            = "(" +
                                           UNICODE_DEGREE + '|' +
                                           UNICODE_WHITE_BULLET + '|' +
                                           UNICODE_DOUBLE_PRIME + '|' +
                                           UNICODE_DOUBLED_APOSTROPHE + '|' +
                                           UNICODE_QUOTATION_MARK + ")?";
            final String power           = "(t(" +
                                           UNICODE_SUPERSCRIPT_2 + '|' +
                                           UNICODE_SUPERSCRIPT_3 + '|' +
                                           UNICODE_SUPERSCRIPT_4 + '|' +
                                           UNICODE_SUPERSCRIPT_5 + ")?)?";
            pattern = Pattern.compile(space + sign + space + mantissa + space + unit +
                                      space + UNICODE_MULTIPLICATION_SIGN + '?' + space + power);

        }

        /** Initialize parsing.
         * @param definition polynomial complete definition.
         */
        public void initialize(final String definition) {
            matcher = pattern.matcher(definition);
            next = 0;
        }

        /** Parse next monomial.
         * @return true if a monomial has been parsed
         */
        public boolean parseNext() {

            // advance matcher
            matcher.region(next, matcher.regionEnd());

            if (matcher.lookingAt()) {

                // parse coefficient, with proper sign and unit
                parsedCoefficient = Double.parseDouble(matcher.group(2));
                if ("-".equals(matcher.group(1)) || UNICODE_MINUS_SIGN.equals(matcher.group(1))) {
                    parsedCoefficient = -parsedCoefficient;
                }
                if (UNICODE_DEGREE.equals(matcher.group(3)) ||
                    UNICODE_WHITE_BULLET.equals(matcher.group(3))) {
                    parsedCoefficient = FastMath.toRadians(parsedCoefficient);
                } else if (UNICODE_DOUBLE_PRIME.equals(matcher.group(3)) ||
                           UNICODE_DOUBLED_APOSTROPHE.equals(matcher.group(3)) ||
                           UNICODE_QUOTATION_MARK.equals(matcher.group(3))) {
                    parsedCoefficient = FastMath.toRadians(parsedCoefficient / 3600.0);
                }

                if (matcher.group(4) == null) {
                    parsedPower = 0;
                } else if (matcher.group(5) == null) {
                    parsedPower = 1;
                } else if (UNICODE_SUPERSCRIPT_2.equals(matcher.group(5))) {
                    parsedPower = 2;
                } else if (UNICODE_SUPERSCRIPT_3.equals(matcher.group(5))) {
                    parsedPower = 3;
                } else if (UNICODE_SUPERSCRIPT_4.equals(matcher.group(5))) {
                    parsedPower = 4;
                } else {
                    // this should never happen for current IERS files (limited to degree 4)
                    parsedPower = 5;
                }

                next = matcher.end();
                return true;

            } else {

                parsedCoefficient = Double.NaN;
                parsedPower       = -1;
                return false;
            }

        }

        /** Get the last parsed coefficient.
         * @return last parsed coefficient converted to radians
         */
        public double getParsedCoefficient() {
            return parsedCoefficient;
        }

        /** Get the last parsed power.
         * @return last parsed power
         */
        public int getParsedPower() {
            return parsedPower;
        }

    }

}
