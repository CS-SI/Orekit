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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.orekit.errors.OrekitException;

/**This reader is adapted to the EGM Format.
 *
 * <p> The proper way to use this class is to call the {@link PotentialReaderFactory}
 *  which will determine which reader to use with the selected potential
 *  coefficients file <p>
 *
 * @see org.orekit.forces.gravity.potential.PotentialReaderFactory
 * @author Fabien Maussion
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class EGMFormatReader extends PotentialCoefficientsReader {

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public EGMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
        ae = 6378136.3;
        mu = 398600.4415e9;
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        final List<double[]> cl = new ArrayList<double[]>();
        final List<double[]> sl = new ArrayList<double[]>();
        boolean okFields = true;
        for (String line = r.readLine(); okFields && line != null; line = r.readLine()) {
            if (line.length() >= 15) {

                // get the fields defining the current the potential terms
                final String[] tab = line.trim().split("\\s+");
                if (tab.length != 6) {
                    okFields = false;
                }

                final int i = Integer.parseInt(tab[0]);
                final int j = Integer.parseInt(tab[1]);
                final double c = Double.parseDouble(tab[2]);
                final double s = Double.parseDouble(tab[3]);

                // extend the cl array if needed
                final int ck = cl.size();
                for (int k = ck; k <= i; ++k) {
                    final double[] d = new double[k + 1];
                    if (!missingCoefficientsAllowed()) {
                        Arrays.fill(d, Double.NaN);
                    } else {
                        if (k == 0) {
                            d[0] = 1.0;
                        }
                    }
                    cl.add(new double[k + 1]);
                }
                final double[] cli = cl.get(i);

                // extend the sl array if needed
                final int sk = sl.size();
                for (int k = sk; k <= i; ++k) {
                    final double[] d = new double[k + 1];
                    if (!missingCoefficientsAllowed()) {
                        Arrays.fill(d, Double.NaN);
                    }
                    sl.add(new double[k + 1]);
                }
                final double[] sli = sl.get(i);

                // store the terms
                cli[j] = c;
                sli[j] = s;

            }
        }

        boolean okCoeffs = true;
        for (int k = 0; okCoeffs && k < cl.size(); k++) {
            final double[] cK = cl.get(k);
            for (int i = 0; okCoeffs && i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    okCoeffs = false;
                }
            }
            final double[] sK = sl.get(k);
            for (int i = 0; okCoeffs && i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    okCoeffs = false;
                }
            }
        }

        if ((!okFields) || (cl.size() < 1) || (!okCoeffs)) {
            throw new OrekitException("the reader is not adapted to the format ({0})",
                                      name);
        }

        // convert to simple triangular arrays
        normalizedC = cl.toArray(new double[cl.size()][]);
        normalizedS = sl.toArray(new double[sl.size()][]);
        readCompleted = true;

    }

}
