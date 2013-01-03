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
package org.orekit.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Reader for the ICGEM gravity field format.
 *
 * <p>This format is used to describe the gravity field of EIGEN models
 * published by the GFZ Potsdam since 2004. It is described in Franz
 * Barthelmes and Christoph F&ouml;rste paper: "the ICGEM-format".
 * The 2006-02-28 version of this paper can be found <a
 * href="http://op.gfz-potsdam.de/grace/results/grav/g005_ICGEM-Format.pdf">here</a>
 * and the 2011-06-07 version of this paper can be found <a
 * href="http://icgem.gfz-potsdam.de/ICGEM/documents/ICGEM-Format-2011.pdf">here</a>.
 * These versions differ in time-dependent coefficients, which are linear-only prior
 * to 2011 (up to eigen-5 model) and have also harmonic effects after that date
 * (starting with eigen-6 model). Both versions are supported
 * by the class (for now, they simply ignore the time-dependent parts).</p>
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *
 * @see GravityFieldFactory
 * @author Luc Maisonobe
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

    /** Gravity field coefficient trend. */
    private static final String TRND                    = "trnd";

    /** Gravity field coefficient sine amplitude. */
    private static final String ASIN                    = "asin";

    /** Gravity field coefficient cosine amplitude. */
    private static final String ACOS                    = "acos";

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
                            throw OrekitException.createParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                       lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && GRAVITY_CONSTANT.equals(tab[0])) {
                        mu   = Double.parseDouble(tab[1].replace('D', 'E'));
                        okMu = true;
                    } else if ((tab.length == 2) && REFERENCE_RADIUS.equals(tab[0])) {
                        ae   = Double.parseDouble(tab[1].replace('D', 'E'));
                        okAe = true;
                    } else if ((tab.length == 2) && MAX_DEGREE.equals(tab[0])) {

                        final int maxDegree = FastMath.min(maxReadDegree, Integer.parseInt(tab[1]));

                        // allocate arrays
                        normalizedC = new double[maxDegree + 1][];
                        normalizedS = new double[maxDegree + 1][];
                        for (int k = 0; k < normalizedC.length; k++) {
                            final int maxOrder = FastMath.min(maxReadOrder, k);
                            normalizedC[k] = new double[maxOrder + 1];
                            normalizedS[k] = new double[maxOrder + 1];
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
                            throw OrekitException.createParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
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
                        if (degree <= maxReadDegree && order <= maxReadOrder) {
                            normalizedC[degree][order] = Double.parseDouble(tab[3].replace('D', 'E'));
                            normalizedS[degree][order] = Double.parseDouble(tab[4].replace('D', 'E'));
                        }
                    } else if (((tab.length == 7) && DOT.equals(tab[0])) ||
                               ((tab.length == 7) && TRND.equals(tab[0])) ||
                               ((tab.length == 8) && ASIN.equals(tab[0])) ||
                               ((tab.length == 8) && ACOS.equals(tab[0]))) {
                        // we ignore the time derivative records
                    } else {
                        throw OrekitException.createParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                   lineNumber, name, line);
                    }
                }
            } catch (NumberFormatException nfe) {
                throw OrekitException.createParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
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
            String loaderName = getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new OrekitException(OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                                      name, loaderName);
        }

        readCompleted = true;

    }

}
