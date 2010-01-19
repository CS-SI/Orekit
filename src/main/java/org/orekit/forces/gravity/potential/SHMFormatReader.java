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

/** Reader for the SHM gravity field format.
 *
 * <p> This format was used to describe the gravity field of EIGEN models
 * published by the GFZ Potsdam up to 2003. It was then replaced by
 * {@link ICGEMFormatReader ICGEM format}. The SHM format is described in
 * <a href="http://www.gfz-potsdam.de/grace/results/"> Potsdam university
 * website</a>.
 *
 * <p> The proper way to use this class is to call the
 *  {@link PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *
 * @see PotentialReaderFactory
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SHMFormatReader extends PotentialCoefficientsReader {

    /** First field labels. */
    private static final String GRCOEF = "GRCOEF";

    /** Second field labels. */
    private static final String GRCOF2 = "GRCOF2";

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public SHMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        boolean okEarth  = false;
        boolean okSHM    = false;
        boolean okCoeffs = false;
        String line = r.readLine();
        if ("FIRST ".equals(line.substring(0, 6)) &&
            "SHM    ".equals(line.substring(49, 56))) {
            for (line = r.readLine(); line != null; line = r.readLine()) {
                if (line.length() >= 6) {
                    final String[] tab = line.split("\\s+");

                    // read the earth values
                    if ("EARTH".equals(tab[0])) {
                        mu = Double.parseDouble(tab[1].replace('D', 'E'));
                        ae = Double.parseDouble(tab[2].replace('D', 'E'));
                        okEarth = true;
                    }

                    // initialize the arrays
                    if ("SHM".equals(tab[0])) {
                        final int i = Integer.parseInt(tab[1]);
                        normalizedC = new double[i + 1][];
                        normalizedS = new double[i + 1][];
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
                        okSHM = true;
                    }

                    // fill the arrays
                    if (GRCOEF.equals(line.substring(0, 6))) {
                        final int i = Integer.parseInt(tab[1]);
                        final int j = Integer.parseInt(tab[2]);
                        normalizedC[i][j] = Double.parseDouble(tab[3].replace('D', 'E'));
                        normalizedS[i][j] = Double.parseDouble(tab[4].replace('D', 'E'));
                        okCoeffs = true;
                    }
                    if (GRCOF2.equals(tab[0])) {
                        final int i = Integer.parseInt(tab[1]);
                        final int j = Integer.parseInt(tab[2]);
                        normalizedC[i][j] = Double.parseDouble(tab[3].replace('D', 'E'));
                        normalizedS[i][j] = Double.parseDouble(tab[4].replace('D', 'E'));
                        okCoeffs = true;
                    }

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

        if (!(okEarth && okSHM && okCoeffs)) {
            throw new OrekitException("the reader is not adapted to the format ({0})",
                                      name);
        }

        readCompleted = true;

    }

}
