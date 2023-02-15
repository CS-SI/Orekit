/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
package org.orekit.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/** Supported Ocean load Deformation coefficients (Love numbers k'<sub>i</sub>).
 * @see GravityFields
 * @since 6.1
 * @author Luc Maisonobe
 */
public enum OceanLoadDeformationCoefficients {

    /** Coefficients from IERS 1996 conventions.
     * <p>
     * Note that coefficients from conventions IERS 1996, 2003 and 2010 are all equal to each other.
     * </p>
     */
    IERS_1996 {

        /** {@inheritDoc} */
        @Override
        public double[] getCoefficients() {
            return new double[] {
                // IERS conventions 1996, chapter 6 page 48
                0.0, 0.0, -0.3075, -0.195, -0.132, -0.1032, -0.0892
            };
        }

    },

    /** Coefficients from IERS 2003 conventions.
     * <p>
     * Note that coefficients from conventions IERS 1996, 2003 and 2010 are all equal to each other.
     * </p>
     */
    IERS_2003 {

        /** {@inheritDoc} */
        @Override
        public double[] getCoefficients() {
            return new double[] {
                // IERS conventions 2003, section 6.4 page 67 equation 13
                0.0, 0.0, -0.3075, -0.195, -0.132, -0.1032, -0.0892
            };
        }

    },

    /** Coefficients from IERS 2010 conventions.
     * <p>
     * Note that coefficients from conventions IERS 1996, 2003 and 2010 are all equal to each other.
     * </p>
     */
    IERS_2010 {

        /** {@inheritDoc} */
        @Override
        public double[] getCoefficients() {
            return new double[] {
                // IERS conventions 2010, section 6.3.1 page 91
                0.0, 0.0, -0.3075, -0.195, -0.132, -0.1032, -0.0892
            };
        }

    },

    /** Coefficients computed by Pascal Gégout, CNRS / UMR5563 (GET).
     * <p>
     * These coefficients are available up to degree 250.
     * </p>
     */
    GEGOUT {

        /** Coefficients resources. */
        private static final String RESOURCE_NAME = "/assets/org/orekit/fic.love.kp.gegout";

        /** Pattern for fields with integer type. */
        private static final String  INTEGER_TYPE_PATTERN = "[-+]?\\p{Digit}+";

        /** Pattern for fields with real type. */
        private static final String  REAL_TYPE_PATTERN = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

        /** {@inheritDoc} */
        @Override
        public double[] getCoefficients() {

            int lineNumber = 0;
            String line = null;
            try (InputStream stream =
                            checkNull(OceanLoadDeformationCoefficients.class.getResourceAsStream(RESOURCE_NAME));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

                // regular lines are simply a degree index followed by the coefficient for this degree
                final StringBuilder builder = new StringBuilder("^\\p{Space}*");
                builder.append("(").append(INTEGER_TYPE_PATTERN).append(")");
                builder.append("\\p{Space}+");
                builder.append("(").append(REAL_TYPE_PATTERN).append(")");
                builder.append("\\p{Space}*$");
                final Pattern regularLinePattern = Pattern.compile(builder.toString());

                // setup the reader
                lineNumber = 0;
                int maxDegree = 0;
                final Map<Integer, Double> map = new HashMap<Integer, Double>();
                for (line = reader.readLine(); line != null; line = reader.readLine()) {
                    lineNumber++;
                    final Matcher regularMatcher = regularLinePattern.matcher(line);
                    if (regularMatcher.matches()) {
                        // we have found a regular data line
                        final int degree         = Integer.parseInt(regularMatcher.group(1));
                        final double coefficient = Double.parseDouble(regularMatcher.group(2));
                        map.put(degree, coefficient);
                        maxDegree = FastMath.max(maxDegree, degree);
                    }
                }

                final double[] coefficients = new double[maxDegree + 1];
                for (final Map.Entry<Integer, Double> entry : map.entrySet()) {
                    coefficients[entry.getKey()] = entry.getValue();
                }

                return coefficients;

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, RESOURCE_NAME, line);
            } catch (IOException ioe) {
                throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
            }

        }

        /**
         * Helper method to check for null resources. Throws an exception if {@code
         * stream} is null.
         *
         * @param stream loaded from the class resources.
         * @return {@code stream}.
         */
        private InputStream checkNull(final InputStream stream) {
            if (stream == null) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, RESOURCE_NAME);
            }
            return stream;
        }

    };

    /** Get the load deformation coefficients for ocean tides.
     * @return load deformation coefficients for ocean tides
     */
    public abstract double[] getCoefficients();

}
