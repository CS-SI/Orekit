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
package org.orekit.forces.gravity.potential;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.orekit.errors.OrekitException;

/** Reader for the SHM gravity potential format.
 *
 * <p> This format is used to describe the gravity field of EIGEN models,
 * edited by the GFZ Potsdam.
 * It is described in <a href="http://www.gfz-potsdam.de/grace/results/">
 * Potsdam university website </a>
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

    /** Format compatibility flag. */
    private boolean fileIsOK;

    /** The input to check and read. */
    private InputStream in;

    /** Simple constructor (the first method to call then is <code>isFileOK</code>.
     *  It is done automatically by the factory).
     */
    protected SHMFormatReader() {
        in = null;
        fileIsOK = false;
    }

    /** Check the file to determine if its format is understood by the reader or not.
     * @param input the input to check
     * @return true if it is readable, false if not.
     * @exception IOException when the {@link InputStream} cannot be buffered.
     */
    public boolean isFileOK(final InputStream input) throws IOException {

        this.in = input;
        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        // tests variables
        boolean iKnow = false;
        boolean earth = false;
        int lineIndex = 0;
        int c = 1;
        // read the first lines to detect the format
        for (String line = r.readLine(); iKnow != true; line = r.readLine()) {
            // check the first line
            if (line == null) {
                iKnow = true;
            } else {
                if (c == 1) {
                    if (!("FIRST ".equals(line.substring(0, 6)) &&
                          "SHM    ".equals(line.substring(49, 56)))) {
                        iKnow = true;
                    }
                }
                // check for the earth line
                if ((line.length() >= 6) && earth == false && c > 1 && c < 25) {
                    if ("EARTH ".equals(line.substring(0, 6))) {
                        earth = true;
                    }
                }
                if (c >= 25 && earth == false) {
                    iKnow = true;
                }
                // check there is at least two coef line
                if ((line.length() >= 6) && earth == true && c > 2 && c < 27) {
                    if (GRCOEF.equals(line.substring(0, 6))) {
                        lineIndex++;
                    }
                    if (GRCOF2.equals(line.substring(0, 6))) {
                        lineIndex++;
                    }

                }
                if (c >= 27 && lineIndex < 2) {
                    iKnow = true;
                }
                // if everything is OK, accept
                if (earth == true && lineIndex >= 2) {
                    iKnow = true;
                    fileIsOK = true;
                }
                c++;
            }
        }
        return fileIsOK;
    }

    /** Computes the coefficients by reading the selected (and tested) file.
     * @exception OrekitException when the file has not been initialized or checked.
     * @exception IOException when the file is corrupted.
     */
    public void read() throws OrekitException, IOException {
        if (in == null) {
            throw new OrekitException("the reader has not been tested", new Object[0]);
        }
        if (fileIsOK == false) {
            throw new OrekitException("the reader is not adapted to the format",
                                      new Object[0]);
        }

        final BufferedReader r = new BufferedReader(new InputStreamReader(in));
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            if (line.length() >= 6) {
                final String[] tab = line.split("\\s+");

                // read the earth values
                if ("EARTH".equals(tab[0])) {
                    mu = Double.parseDouble(tab[1].replace('D', 'E'));
                    ae = Double.parseDouble(tab[2].replace('D', 'E'));
                }

                // initialize the arrays
                if ("SHM".equals(tab[0])) {
                    final int i = Integer.parseInt(tab[1]);
                    normalizedC = new double[i + 1][];
                    normalizedS = new double[i + 1][];
                    for (int k = 0; k < normalizedC.length; k++) {
                        normalizedC[k] = new double[k + 1];
                        normalizedS[k] = new double[k + 1];
                    }
                }

                // fill the arrays
                if (GRCOEF.equals(line.substring(0, 6))) {
                    final int i = Integer.parseInt(tab[1]);
                    final int j = Integer.parseInt(tab[2]);
                    normalizedC[i][j] = Double.parseDouble(tab[3].replace('D', 'E'));
                    normalizedS[i][j] = Double.parseDouble(tab[4].replace('D', 'E'));
                }
                if (GRCOF2.equals(tab[0])) {
                    final int i = Integer.parseInt(tab[1]);
                    final int j = Integer.parseInt(tab[2]);
                    normalizedC[i][j] = Double.parseDouble(tab[3].replace('D', 'E'));
                    normalizedS[i][j] = Double.parseDouble(tab[4].replace('D', 'E'));
                }

            }
        }
    }

}
