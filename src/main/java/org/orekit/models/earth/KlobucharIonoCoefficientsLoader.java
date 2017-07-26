/* Copyright 2002-2017 CS Systèmes d'Information
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

package org.orekit.models.earth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

/** Loads Klobuchar-Style ionospheric coefficients a given input stream.
 * A stream contains the alphas and betas coefficient for a given day.
 * <p>
 * They are obtained from <a href="ftp://ftp.unibe.ch/aiub/CODE/">University of Bern Astronomical Institute ftp</a>.
 * Find more on the files at the <a href="http://www.aiub.unibe.ch/research/code___analysis_center/klobuchar_style_ionospheric_coefficients/index_eng.html">Astronomical Institute site</a>.
 * <p>
 * The files are UNIX-style compressed (.Z) files.
 * They have to be extracted to UTF-8 text files before being read by this loader.
 * <p>
 * After extraction, it is assumed they are named CGIMDDD0.YYN where DDD and YY substitute day of year and 2-digits year.
 * <p>
 * The format is always the same, with and example shown below. Only the last 2 lines contains the Klobuchar coefficients.
 * <p>
 * Example:
 * </p>
 * <pre>
 *      2              NAVIGATION DATA     GPS                 RINEX VERSION / TYPE
 * INXFIT V5.3         AIUB                06-JAN-17 09:12     PGM / RUN BY / DATE
 * CODE'S KLOBUCHAR-STYLE IONOSPHERE MODEL FOR DAY 001, 2017   COMMENT
 * Contact address: code(at)aiub.unibe.ch                      COMMENT
 * Data archive:    ftp.unibe.ch/aiub/CODE/                    COMMENT
 *                  www.aiub.unibe.ch/download/CODE/           COMMENT
 * WARNING: USE DATA AT SOUTHERN POLAR REGION WITH CARE        COMMENT
 *     1.2821D-08 -9.6222D-09 -3.5982D-07 -6.0901D-07          ION ALPHA
 *     1.0840D+05 -1.3197D+05 -2.6331D+05  4.0570D+05          ION BETA
 *                                                             END OF HEADER
 * </pre>
 * @author Maxime Journot
 */
public class KlobucharIonoCoefficientsLoader implements DataLoader {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "CGIM*0\\.*N$";

    /** Regular expression for supported file name. */
    private String supportedNames;

    /** The alpha coefficients loaded. */
    private double alpha[];

    /** The beta coefficients loaded. */
    private double beta[];

    /** Constructor with supported names given by user.
     * @param supportedNames Supported names
     */
    public KlobucharIonoCoefficientsLoader(final String supportedNames) {
        this.alpha = null;
        this.beta = null;
        this.supportedNames = supportedNames;
    }

    /** Constructor with default supported names. */
    public KlobucharIonoCoefficientsLoader() {
        this(DEFAULT_SUPPORTED_NAMES);
    }

    /** Returns the alpha coefficients array.
     * @return the alpha coefficients array
     */
    public double[] getAlpha() {
        return alpha.clone();
    }

    /** Returns the beta coefficients array.
     * @return the beta coefficients array
     */
    public double[] getBeta() {
        return beta.clone();
    }

    /** Returns the supported names of the loader.
     * @return the supported names
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /** Load the data using supported names .
     * @throws OrekitException */
    public void loadKlobucharIonosphericCoefficients() throws OrekitException {
        DataProvidersManager.getInstance().feed(supportedNames, this);

        // Throw an exception if alphas or betas were not loaded properly
        if (alpha == null || beta == null) {
            throw new OrekitException(OrekitMessages.KLOBUCHAR_ALPHA_BETA_NOT_LOADED, supportedNames);
        }
    }

    /** Load the data for a given day.
     * @param dateComponents day given but its DateComponents
     * @throws OrekitException if the coefficients could not be loaded
     */
    public void loadKlobucharIonosphericCoefficients(final DateComponents dateComponents) throws OrekitException {

        // The files are named CGIMDDD0.YYN where DDD and YY substitute day of year and 2-digits year
        final int    doy        = dateComponents.getDayOfYear();
        final String yearString = String.valueOf(dateComponents.getYear());

        this.supportedNames = String.format("CGIM%03d0.%2sN", doy, yearString.substring(yearString.length() - 2));

        try {
            this.loadKlobucharIonosphericCoefficients();
        } catch (OrekitException oe) {
            throw new OrekitException(oe,
                                      OrekitMessages.KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE,
                                      dateComponents.toString());
        }
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

    /** Load Klobuchar-Style ionospheric coefficients read from some file.
     * @param input data input stream
     * @param name name of the file (or zip entry)
     * @exception IOException if data can't be read
     * @exception ParseException if data can't be parsed
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // Open stream and parse data
        final BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        int lineNumber = 0;
        final String splitter = "\\s+";
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            ++lineNumber;
            line = line.trim();

            try {
                // Read alphas
                if (line.length() > 0 && line.endsWith("ALPHA")) {
                    final String[] alpha_line = line.split(splitter);
                    alpha = new double[4];
                    for (int j = 0; j < 4; j++) {
                        alpha[j] = Double.valueOf(alpha_line[j].replace("D", "E"));
                    }
                }

                // Read betas
                if (line.length() > 0 && line.endsWith("BETA")) {
                    final String[] beta_line = line.split(splitter);
                    beta = new double[4];
                    for (int j = 0; j < 4; j++) {
                        beta[j] = Double.valueOf(beta_line[j].replace("D", "E"));
                    }
                }
            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }
        }

        // Check that alphas and betas were found
        if (alpha == null || beta == null) {
            throw new OrekitException(OrekitMessages.NO_KLOBUCHAR_ALPHA_BETA_IN_FILE, name);
        }

        // Close the stream after reading
        input.close();
    }
}
