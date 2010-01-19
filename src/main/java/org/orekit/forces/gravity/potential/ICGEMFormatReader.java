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

import org.orekit.errors.OrekitException;

/** Reader for the ICGEM gravity field format.
 *
 * <p> This format is used to describe the gravity field of EIGEN models
 * published by the GFZ Potsdam since 2004. It is described in Franz
 * Barthelmes and Christoph F&ouml;rste paper:
 * <a href="http://op.gfz-potsdam.de/grace/results/grav/g005_ICGEM-Format.pdf">the
 * ICGEM-format</a>.
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
public class ICGEMFormatReader extends PotentialCoefficientsReader {

    /** Product type. */
    private static final String PRODUCT_TYPE            = "product_type";

    /** Gravity field product type. */
    private static final String GRAVITY_FIELD           = "gravity_field";

    /** Gravity constant marker. */
    private static final String GRAVITY_CONSTANT        = "earth_gravity_constant";

    /** Reference radius. */
    private static final String REFERENCE_RADIUS        = "radius";

    /** Max degree. */
    private static final String MAX_DEGREE              = "max_degree";

    /** Normalization indicator. */
    private static final String NORMALIZATION_INDICATOR = "norm";

    /** Indicator value for normalized coefficients. */
    private static final String NORMALIZED              = "fully_normalized";

    /** End of header marker. */
    private static final String END_OF_HEADER           = "end_of_head";

    /** Gravity field coefficient. */
    private static final String GFC                     = "gfc";

    /** Time stamped gravity field coefficient. */
    private static final String GFCT                    = "gfct";

    /** Gravity field coefficient first time derivative. */
    private static final String DOT                     = "dot";

    /** Format error message. */
    private static final String WRONG_FORMAT_MESSAGE    = "the reader is not adapted to the format ({0})";

    /** Line parsing error message. */
    private static final String LINE_PARSING_MESSAGE    = "unable to parse line {0} of file {1}:\n{2}";

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public ICGEMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        boolean inHeader = true;
        boolean okMu     = false;
        boolean okAe     = false;
        int lineNumber   = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            try {
                ++lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                final String[] tab = line.split("\\s+");
                if (inHeader) {
                    if ((tab.length == 2) && PRODUCT_TYPE.equals(tab[0])) {
                        if (!GRAVITY_FIELD.equals(tab[1])) {
                            throw OrekitException.createParseException(LINE_PARSING_MESSAGE,
                                                                       lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && GRAVITY_CONSTANT.equals(tab[0])) {
                        mu   = Double.parseDouble(tab[1].replace('D', 'E'));
                        okMu = true;
                    } else if ((tab.length == 2) && REFERENCE_RADIUS.equals(tab[0])) {
                        ae   = Double.parseDouble(tab[1].replace('D', 'E'));
                        okAe = true;
                    } else if ((tab.length == 2) && MAX_DEGREE.equals(tab[0])) {

                        final int maxDegree = Integer.parseInt(tab[1]);

                        // allocate arrays
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

                    } else if ((tab.length == 2) && NORMALIZATION_INDICATOR.equals(tab[0])) {
                        if (!NORMALIZED.equals(tab[1])) {
                            throw OrekitException.createParseException(LINE_PARSING_MESSAGE,
                                                                       lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && END_OF_HEADER.equals(tab[0])) {
                        inHeader = false;
                    }
                } else {
                    if (((tab.length == 7) && GFC.equals(tab[0])) ||
                        ((tab.length == 8) && GFCT.equals(tab[0]))) {
                        final int degree = Integer.parseInt(tab[1]);
                        final int order  = Integer.parseInt(tab[2]);
                        normalizedC[degree][order] = Double.parseDouble(tab[3].replace('D', 'E'));
                        normalizedS[degree][order] = Double.parseDouble(tab[4].replace('D', 'E'));
                    } else if ((tab.length == 7) && DOT.equals(tab[0])) {
                        // we ignore the time derivative records
                    } else {
                        throw OrekitException.createParseException(LINE_PARSING_MESSAGE,
                                                                   lineNumber, name, line);
                    }
                }
            } catch (NumberFormatException nfe) {
                throw OrekitException.createParseException(LINE_PARSING_MESSAGE,
                                                           lineNumber, name, line);
            }
        }

        boolean okCoeffs = true;
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

        if (!(okMu && okAe && okCoeffs)) {
            throw new OrekitException(WRONG_FORMAT_MESSAGE, name);
        }

        readCompleted = true;

    }

}
