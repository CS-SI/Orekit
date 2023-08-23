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

package org.orekit.models.earth.ionosphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.regex.Pattern;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

/** Loads Klobuchar-Style ionospheric coefficients a given input stream.
 * A stream contains the alphas and betas coefficient for a given day.
 * <p>
 * They are obtained from <a href="ftp://ftp.aiub.unibe.ch/CODE/">University of Bern Astronomical Institute ftp</a>.
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
 *
 * <p>It is not safe for multiple threads to share a single instance of this class.
 *
 * @author Maxime Journot
 */
public class KlobucharIonoCoefficientsLoader extends AbstractSelfFeedingLoader
        implements DataLoader {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "CGIM*0\\.*N$";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** The alpha coefficients loaded. */
    private double[] alpha;

    /** The beta coefficients loaded. */
    private double[] beta;

    /**
     * Constructor with supported names given by user. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param supportedNames regular expression that matches the names of the RINEX files
     *                       with Klobuchar coefficients.
     * @see #KlobucharIonoCoefficientsLoader(String, DataProvidersManager)
     */
    @DefaultDataContext
    public KlobucharIonoCoefficientsLoader(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * Constructor that uses user defined supported names and data context.
     *
     * @param supportedNames       regular expression that matches the names of the RINEX
     *                             files with Klobuchar coefficients.
     * @param dataProvidersManager provides access to auxiliary data files.
     */
    public KlobucharIonoCoefficientsLoader(final String supportedNames,
                                           final DataProvidersManager dataProvidersManager) {
        super(supportedNames, dataProvidersManager);
        this.alpha = null;
        this.beta = null;
    }

    /**
     * Constructor with default supported names. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @see #KlobucharIonoCoefficientsLoader(String, DataProvidersManager)
     * @see #KlobucharIonoCoefficientsLoader(String)
     */
    @DefaultDataContext
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

    @Override
    public String getSupportedNames() {
        return super.getSupportedNames();
    }

    /** Load the data using supported names .
     */
    public void loadKlobucharIonosphericCoefficients() {
        feed(this);

        // Throw an exception if alphas or betas were not loaded properly
        if (alpha == null || beta == null) {
            throw new OrekitException(OrekitMessages.KLOBUCHAR_ALPHA_BETA_NOT_LOADED,
                    getSupportedNames());
        }
    }

    /** Load the data for a given day.
     * @param dateComponents day given but its DateComponents
     */
    public void loadKlobucharIonosphericCoefficients(final DateComponents dateComponents) {

        // The files are named CGIMDDD0.YYN where DDD and YY substitute day of year and 2-digits year
        final int    doy        = dateComponents.getDayOfYear();
        final String yearString = String.valueOf(dateComponents.getYear());

        this.setSupportedNames(String.format("CGIM%03d0.%2sN", doy, yearString.substring(yearString.length() - 2)));

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
     */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException {

        int lineNumber = 0;
        String line = null;
        // Open stream and parse data
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();

                // Read alphas
                if (line.length() > 0 && line.endsWith("ALPHA")) {
                    final String[] alpha_line = SEPARATOR.split(line);
                    alpha = new double[4];
                    for (int j = 0; j < 4; j++) {
                        alpha[j] = Double.parseDouble(alpha_line[j].replace("D", "E"));
                    }
                }

                // Read betas
                if (line.length() > 0 && line.endsWith("BETA")) {
                    final String[] beta_line = SEPARATOR.split(line);
                    beta = new double[4];
                    for (int j = 0; j < 4; j++) {
                        beta[j] = Double.parseDouble(beta_line[j].replace("D", "E"));
                    }
                }
            }

        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

        // Check that alphas and betas were found
        if (alpha == null || beta == null) {
            throw new OrekitException(OrekitMessages.NO_KLOBUCHAR_ALPHA_BETA_IN_FILE, name);
        }

    }
}
