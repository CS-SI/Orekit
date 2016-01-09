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
package org.orekit.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Reader for ocean tides files following the fes2004_Cnm-Snm.dat format.
 * @since 6.1
 * @author Luc Maisonobe
 */
public class FESCnmSnmReader extends OceanTidesReader {

    /** Default pattern for fields with unknown type (non-space characters). */
    private static final String  UNKNOWN_TYPE_PATTERN = "\\S+";

    /** Pattern for fields with integer type. */
    private static final String  INTEGER_TYPE_PATTERN = "[-+]?\\p{Digit}+";

    /** Pattern for fields with real type. */
    private static final String  REAL_TYPE_PATTERN = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

    /** Pattern for fields with Doodson number. */
    private static final String  DOODSON_TYPE_PATTERN = "\\p{Digit}{2,3}[.,]\\p{Digit}{3}";

    /** Scale of the Cnm, Snm parameters. */
    private final double scale;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param scale scale of the Cnm, Snm parameters
     */
    public FESCnmSnmReader(final String supportedNames, final double scale) {
        super(supportedNames);
        this.scale = scale;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name)
        throws OrekitException, IOException {

        // FES ocean tides models have the following form:
        //    Coefficients to compute variations in normalized Stokes coefficients (unit = 10^-12)
        //    Ocean tide model: FES2004 normalized model (fev. 2004) up to (100,100)
        //    (long period from FES2002 up to (50,50) + equilibrium Om1/Om2, atmospheric tide NOT included)
        //    Doodson Darw  l   m    DelC+     DelS+       DelC-     DelS-
        //     55.565 Om1   2   0   6.58128  -0.00000    -0.00000  -0.00000
        //     55.575 Om2   2   0  -0.06330   0.00000     0.00000   0.00000
        //     56.554 Sa    1   0  -0.00000  -0.00000    -0.00000  -0.00000
        //     56.554 Sa    2   0   0.56720   0.01099    -0.00000  -0.00000
        //     56.554 Sa    3   0   0.00908  -0.00050    -0.00000  -0.00000
        final String[] fieldsPatterns = new String[] {
            DOODSON_TYPE_PATTERN,
            UNKNOWN_TYPE_PATTERN,
            INTEGER_TYPE_PATTERN,
            INTEGER_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN,
            REAL_TYPE_PATTERN
        };
        final StringBuilder builder = new StringBuilder("^\\p{Space}*");
        for (int i = 0; i < fieldsPatterns.length; ++i) {
            builder.append("(");
            builder.append(fieldsPatterns[i]);
            builder.append(")");
            builder.append((i < fieldsPatterns.length - 1) ? "\\p{Space}+" : "\\p{Space}*$");
        }
        final Pattern regularLinePattern = Pattern.compile(builder.toString());

        // parse the file
        startParse(name);
        final BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        int lineNumber      = 0;
        boolean dataStarted = false;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            ++lineNumber;
            final Matcher regularMatcher = regularLinePattern.matcher(line);
            if (regularMatcher.matches()) {
                // we have found a regular data line

                // parse Doodson, degree and order fields
                final int doodson = Integer.parseInt(regularMatcher.group(1).replaceAll("[.,]", ""));
                final int n       = Integer.parseInt(regularMatcher.group(3));
                final int m       = Integer.parseInt(regularMatcher.group(4));

                if (canAdd(n, m)) {

                    // parse coefficients
                    final double cPlus  = scale * Double.parseDouble(regularMatcher.group(5));
                    final double sPlus  = scale * Double.parseDouble(regularMatcher.group(6));
                    final double cMinus = scale * Double.parseDouble(regularMatcher.group(7));
                    final double sMinus = scale * Double.parseDouble(regularMatcher.group(8));

                    // store parsed fields
                    addWaveCoefficients(doodson, n, m, cPlus,  sPlus, cMinus, sMinus, lineNumber, line);
                    dataStarted = true;

                }

            } else if (dataStarted) {
                // once the first data line has been encountered,
                // all remaining lines should also be data lines
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }
        }
        endParse();

    }

}
