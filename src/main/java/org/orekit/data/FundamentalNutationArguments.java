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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;

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
    private static final long serialVersionUID = 20131209L;

    /** IERS conventions to use. */
    private final IERSConventions conventions;

    /** Time scale for GMST computation. */
    private final TimeScale timeScale;

    /** Function computing Greenwich Mean Sidereal Time. */
    private final transient TimeFunction<DerivativeStructure> gmstFunction;

    // luni-solar Delaunay arguments

    /** Coefficients for mean anomaly of the Moon. */
    private final double[] lCoefficients;

    /** Coefficients for mean anomaly of the Sun. */
    private final double[] lPrimeCoefficients;

    /** Coefficients for L - Ω where L is the mean longitude of the Moon. */
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
     * @param conventions IERS conventions to use
     * @param timeScale time scale for GMST computation
     * (may be null if tide parameter γ = GMST + π is not needed)
     * @param stream stream containing the IERS table
     * @param name name of the resource file (for error messages only)
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    public FundamentalNutationArguments(final IERSConventions conventions,
                                        final TimeScale timeScale,
                                        final InputStream stream, final String name)
        throws OrekitException {
        this(conventions, timeScale, parseCoefficients(stream, name));
    }

    /** Build a model of fundamental arguments from an IERS table file.
     * @param conventions IERS conventions to use
     * @param timeScale time scale for GMST computation
     * (may be null if tide parameter γ = GMST + π is not needed)
     * @param coefficients list of coefficients arrays (all 14 arrays must be provided,
     * the 5 Delaunay first and the 9 planetary afterwards)
     * @exception OrekitException if GMST function cannot be retrieved
     * @since 6.1
     */
    public FundamentalNutationArguments(final IERSConventions conventions, final TimeScale timeScale,
                                        final List<double[]> coefficients)
        throws OrekitException {
        this.conventions        = conventions;
        this.timeScale          = timeScale;
        this.gmstFunction       = (timeScale == null) ? null : conventions.getGMSTFunction(timeScale);
        this.lCoefficients      = coefficients.get( 0);
        this.lPrimeCoefficients = coefficients.get( 1);
        this.fCoefficients      = coefficients.get( 2);
        this.dCoefficients      = coefficients.get( 3);
        this.omegaCoefficients  = coefficients.get( 4);
        this.lMeCoefficients    = coefficients.get( 5);
        this.lVeCoefficients    = coefficients.get( 6);
        this.lECoefficients     = coefficients.get( 7);
        this.lMaCoefficients    = coefficients.get( 8);
        this.lJCoefficients     = coefficients.get( 9);
        this.lSaCoefficients    = coefficients.get(10);
        this.lUCoefficients     = coefficients.get(11);
        this.lNeCoefficients    = coefficients.get(12);
        this.paCoefficients     = coefficients.get(13);
    }

    /** Parse coefficients.
     * @param stream stream containing the IERS table
     * @param name name of the resource file (for error messages only)
     * @return list of coefficients arrays
     * @exception OrekitException if stream is null or the table cannot be parsed
     */
    private static List<double[]> parseCoefficients(final InputStream stream, final String name)
        throws OrekitException {

        if (stream == null) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, name);
        }

        try {

            final DefinitionParser definitionParser = new DefinitionParser();

            // setup the reader
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            int lineNumber = 0;

            // look for the reference date and the 14 polynomials
            final int n = FundamentalName.values().length;
            final Map<FundamentalName, double[]> polynomials = new HashMap<FundamentalName, double[]>(n);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                if (definitionParser.parseDefinition(line, lineNumber, name)) {
                    polynomials.put(definitionParser.getParsedName(),
                                    definitionParser.getParsedPolynomial());
                }
            }

            final List<double[]> coefficients = new ArrayList<double[]>(n);
            coefficients.add(getCoefficients(FundamentalName.L,       polynomials, name));
            coefficients.add(getCoefficients(FundamentalName.L_PRIME, polynomials, name));
            coefficients.add(getCoefficients(FundamentalName.F,       polynomials, name));
            coefficients.add(getCoefficients(FundamentalName.D,       polynomials, name));
            coefficients.add(getCoefficients(FundamentalName.OMEGA,   polynomials, name));
            if (polynomials.containsKey(FundamentalName.L_ME)) {
                // IERS conventions 2003 and later provide planetary nutation arguments
                coefficients.add(getCoefficients(FundamentalName.L_ME,    polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_VE,    polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_E,     polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_MA,    polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_J,     polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_SA,    polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_U,     polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.L_NE,    polynomials, name));
                coefficients.add(getCoefficients(FundamentalName.PA,      polynomials, name));
            } else {
                // IERS conventions 1996 and earlier don't provide planetary nutation arguments
                final double[] zero = new double[] {
                    0.0
                };
                while (coefficients.size() < n) {
                    coefficients.add(zero);
                }
            }

            return coefficients;

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
    private static double[] getCoefficients(final FundamentalName argument,
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
     * @param coefficients polynomial coefficients (ordered from low degrees to high degrees)
     * @return value of the polynomial
     */
    private double value(final double tc, final double[] coefficients) {
        double value = 0;
        for (int i = coefficients.length - 1; i >= 0; --i) {
            value = coefficients[i] + tc * value;
        }
        return value;
    }

    /** Evaluate all fundamental arguments for the current date (Delaunay plus planetary).
     * @param date current date
     * @return all fundamental arguments for the current date (Delaunay plus planetary)
     */
    public BodiesElements evaluateAll(final AbsoluteDate date) {

        final double tc = conventions.evaluateTC(date);
        final double gamma = gmstFunction == null ?
                             Double.NaN : gmstFunction.value(date).getValue() + FastMath.PI;

        return new BodiesElements(date, tc, gamma,
                                  value(tc, lCoefficients),      // mean anomaly of the Moon
                                  value(tc, lPrimeCoefficients), // mean anomaly of the Sun
                                  value(tc, fCoefficients),      // L - Ω where L is the mean longitude of the Moon
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

    /** Evaluate a polynomial.
     * @param tc offset in Julian centuries
     * @param coefficients polynomial coefficients (ordered from low degrees to high degrees)
     * @return value of the polynomial
     */
    private DerivativeStructure value(final DerivativeStructure tc, final double[] coefficients) {
        DerivativeStructure value = tc.getField().getZero();
        for (int i = coefficients.length - 1; i >= 0; --i) {
            value = value.multiply(tc).add(coefficients[i]);
        }
        return value;
    }

    /** Evaluate all fundamental arguments for the current date (Delaunay plus planetary),
     * including the first time derivative.
     * @param date current date
     * @return all fundamental arguments for the current date (Delaunay plus planetary),
     * including the first time derivative
     */
    public FieldBodiesElements<DerivativeStructure> evaluateDerivative(final AbsoluteDate date) {

        final DerivativeStructure tc = conventions.dsEvaluateTC(date);

        return new FieldBodiesElements<DerivativeStructure>(date, tc,
                                                            gmstFunction.value(date).add(FastMath.PI),
                                                            value(tc, lCoefficients),      // mean anomaly of the Moon
                                                            value(tc, lPrimeCoefficients), // mean anomaly of the Sun
                                                            value(tc, fCoefficients),      // L - Ω where L is the mean longitude of the Moon
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

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(conventions, timeScale,
                                      Arrays.asList(lCoefficients, lPrimeCoefficients, fCoefficients,
                                                    dCoefficients, omegaCoefficients,
                                                    lMeCoefficients, lVeCoefficients, lECoefficients,
                                                    lMaCoefficients, lJCoefficients, lSaCoefficients,
                                                    lUCoefficients, lNeCoefficients, paCoefficients));
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131209L;

        /** IERS conventions to use. */
        private final IERSConventions conventions;

        /** Time scale for GMST computation. */
        private final TimeScale timeScale;

        /** All coefficients. */
        private final List<double[]> coefficients;

        /** Simple constructor.
         * @param conventions IERS conventions to use
         * @param timeScale time scale for GMST computation
         * @param coefficients all coefficients
         */
        DataTransferObject(final IERSConventions conventions, final TimeScale timeScale,
                                  final List<double[]> coefficients) {
            this.conventions  = conventions;
            this.timeScale    = timeScale;
            this.coefficients = coefficients;
        }

        /** Replace the deserialized data transfer object with a {@link TIRFProvider}.
         * @return replacement {@link TIRFProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new FundamentalNutationArguments(conventions, timeScale, coefficients);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

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

        /** Constant for L - Ω where L is the mean longitude of the Moon. */
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

    /** Local parser for argument definition lines. */
    private static class DefinitionParser {

        /** Regular expression pattern for definitions. */
        private final Pattern pattern;

        /** Parser for polynomials. */
        private PolynomialParser polynomialParser;

        /** Last parsed fundamental name. */
        private FundamentalName parsedName;

        /** Last parsed polynomial. */
        private double[] parsedPolynomial;

        /** Simple constructor. */
        DefinitionParser() {

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

            polynomialParser = new PolynomialParser('t', PolynomialParser.Unit.NO_UNITS);

        }

        /** Parse a definition line.
         * @param line line to parse
         * @param lineNumber line number
         * @param fileName name of the file
         * @return true if a definition has been parsed
         */
        public boolean parseDefinition(final String line, final int lineNumber, final String fileName) {

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
                parsedPolynomial = polynomialParser.parse(matcher.group(2));

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

}
