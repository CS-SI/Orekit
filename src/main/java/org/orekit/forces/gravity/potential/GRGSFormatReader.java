/* Copyright 2002-2010 CS Communication & Systèmes
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
package org.orekit.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.errors.OrekitException;

/** Reader for the GRGS gravity field format.
 *
 * <p> This format was used to describe various gravity fields at GRGS (Toulouse).
 *
 * <p> The proper way to use this class is to call the
 *  {@link PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *
 * @see PotentialReaderFactory
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class GRGSFormatReader extends PotentialCoefficientsReader {

    /** Patterns for lines (the last pattern is repeated for all data lines). */
    private static final Pattern[] LINES;

    static {

        // sub-patterns
        final String real = "[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d";
        final String sep = ")\\s*(";

        // regular expression for header lines
        final String[] header = {
            "^\\s*FIELD - .*$",
            "^\\s+AE\\s+1/F\\s+GM\\s+OMEGA\\s*$",
            "^\\s*(" + real + sep + real + sep + real + sep + real + ")\\s*$",
            "^\\s*REFERENCE\\s+DATE\\s+:\\s+\\d.*$",
            "^\\s*MAXIMAL\\s+DEGREE\\s+:\\s+(\\d+)\\s.*$",
            "^\\s*L\\s+M\\s+DOT\\s+CBAR\\s+SBAR\\s+SIGMA C\\s+SIGMA S(\\s+LIB)?\\s*$"
        };

        // regular expression for data lines
        final String data = "^([ 0-9]{3})([ 0-9]{3})(   |DOT)\\s*(" +
                            real + sep + real + sep + real + sep + real +
                            ")(\\s+[0-9]+)?\\s*$";

        // compile the regular expressions
        LINES = new Pattern[header.length + 1];
        for (int i = 0; i < header.length; ++i) {
            LINES[i] = Pattern.compile(header[i]);
        }
        LINES[LINES.length - 1] = Pattern.compile(data);

    }

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public GRGSFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        //        FIELD - GRIM5, VERSION : C1, november 1999
        //        AE                  1/F                 GM                 OMEGA
        //0.63781364600000E+070.29825765000000E+030.39860044150000E+150.72921150000000E-04
        //REFERENCE DATE : 1997.00
        //MAXIMAL DEGREE : 120     Sigmas calibration factor : .5000E+01 (applied)
        //L  M DOT         CBAR                SBAR             SIGMA C      SIGMA S
        // 2  0DOT 0.13637590952454E-10 0.00000000000000E+00  .143968E-11  .000000E+00
        // 3  0DOT 0.28175700027753E-11 0.00000000000000E+00  .496704E-12  .000000E+00
        // 4  0DOT 0.12249148508277E-10 0.00000000000000E+00  .129977E-11  .000000E+00
        // 0  0     .99999999988600E+00  .00000000000000E+00  .153900E-09  .000000E+00
        // 2  0   -0.48416511550920E-03 0.00000000000000E+00  .204904E-10  .000000E+00

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        boolean okConstants = false;
        boolean okMaxDegree = false;
        boolean okCoeffs    = false;
        int lineNumber      = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {

            ++lineNumber;

            // match current header or data line
            final Matcher matcher = LINES[Math.min(LINES.length, lineNumber) - 1].matcher(line);
            if (!matcher.matches()) {
                throw OrekitException.createParseException("unable to parse line {0} of file {1}:\n{2}",
                                                           lineNumber, name, line);
            }

            if (lineNumber == 3) {
                // header line defining ae, 1/f, GM and Omega
                ae = Double.parseDouble(matcher.group(1).replace('D', 'E'));
                mu = Double.parseDouble(matcher.group(3).replace('D', 'E'));
                okConstants = true;
            } else if (lineNumber == 5) {
                // header line defining max degree
                final int maxDegree = Integer.parseInt(matcher.group(1));
                normalizedC = new double[maxDegree + 1][];
                normalizedS = new double[maxDegree + 1][];
                for (int k = 0; k < normalizedC.length; k++) {
                    normalizedC[k] = new double[k + 1];
                    normalizedS[k] = new double[k + 1];
                    if (!missingCoefficientsAllowed()) {
                        Arrays.fill(normalizedC[k], Double.NaN);
                        Arrays.fill(normalizedS[k], Double.NaN);
                    }
                }
                if (missingCoefficientsAllowed()) {
                    // set the default value for the only expected non-zero coefficient
                    normalizedC[0][0] = 1.0;
                }
                okMaxDegree = true;
            } else if (lineNumber > 6) {
                // data line
                if ("".equals(matcher.group(3).trim())) {
                    // non-dot data line
                    final int i = Integer.parseInt(matcher.group(1).trim());
                    final int j = Integer.parseInt(matcher.group(2).trim());
                    normalizedC[i][j] = Double.parseDouble(matcher.group(4).replace('D', 'E'));
                    normalizedS[i][j] = Double.parseDouble(matcher.group(5).replace('D', 'E'));
                    okCoeffs = true;
                }
            }

        }

        for (int k = 0; okCoeffs && k < normalizedC.length; k++) {
            final double[] cK = normalizedC[k];
            final double[] sK = normalizedS[k];
            for (int i = 0; okCoeffs && i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    okCoeffs = false;
                }
            }
            for (int i = 0; okCoeffs && i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    okCoeffs = false;
                }
            }
        }

        if (!(okConstants && okMaxDegree && okCoeffs)) {
            throw new OrekitException("the reader is not adapted to the format ({0})",
                                      name);
        }

        readCompleted = true;

    }

}
