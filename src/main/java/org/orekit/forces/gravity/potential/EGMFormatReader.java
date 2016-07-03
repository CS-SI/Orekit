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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

/**This reader is adapted to the EGM Format.
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected gravity field file.</p>
 *
 * @see GravityFieldFactory
 * @author Fabien Maussion
 */
public class EGMFormatReader extends PotentialCoefficientsReader {

    /** Flag for using WGS84 values for equatorial radius and central attraction coefficient. */
    private final boolean useWgs84Coefficients;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public EGMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        this(supportedNames, missingCoefficientsAllowed, false);
    }

    /**
     * Simple constructor that allows overriding 'standard' EGM96 ae and mu with
     * WGS84 variants.
     *
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     * @param useWgs84Coefficients if true, the WGS84 values will be used for equatorial radius
     * and central attraction coefficient
     */
    public EGMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed,
                           final boolean useWgs84Coefficients) {
        super(supportedNames, missingCoefficientsAllowed);
        this.useWgs84Coefficients = useWgs84Coefficients;
    }


    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // reset the indicator before loading any data
        setReadComplete(false);

        // both EGM96 and EGM2008 use the same values for ae and mu
        // if a new EGM model changes them, we should have some selection logic
        // based on file name (a better way would be to have the data in the
        // file...)
        if (this.useWgs84Coefficients) {
            setAe(Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
            setMu(Constants.WGS84_EARTH_MU);
        } else {
            setAe(Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
            setMu(Constants.EGM96_EARTH_MU);
        }

        final String lowerCaseName = name.toLowerCase(Locale.US);
        if (lowerCaseName.contains("2008") || lowerCaseName.contains("zerotide")) {
            setTideSystem(TideSystem.ZERO_TIDE);
        } else {
            setTideSystem(TideSystem.TIDE_FREE);
        }

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        final List<List<Double>> c = new ArrayList<List<Double>>();
        final List<List<Double>> s = new ArrayList<List<Double>>();
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
                if (i <= getMaxParseDegree() && j <= getMaxParseOrder()) {
                    for (int k = 0; k <= i; ++k) {
                        extendListOfLists(c, k, FastMath.min(k, getMaxParseOrder()),
                                          missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                        extendListOfLists(s, k, FastMath.min(k, getMaxParseOrder()),
                                          missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                    }
                    parseCoefficient(tab[2], c, i, j, "C", name);
                    parseCoefficient(tab[3], s, i, j, "S", name);
                }

            }
        }

        if (missingCoefficientsAllowed() && getMaxParseDegree() > 0 && getMaxParseOrder() > 0) {
            // ensure at least the (0, 0) element is properly set
            extendListOfLists(c, 0, 0, 0.0);
            extendListOfLists(s, 0, 0, 0.0);
            if (Precision.equals(c.get(0).get(0), 0.0, 0)) {
                c.get(0).set(0, 1.0);
            }
        }

        if ((!okFields) || (c.size() < 1)) {
            String loaderName = getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new OrekitException(OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                                      name, loaderName);
        }

        setRawCoefficients(true, toArray(c), toArray(s), name);
        setReadComplete(true);

    }

    /** Get a provider for read spherical harmonics coefficients.
     * <p>
     * EGM fields don't include time-dependent parts, so this method returns
     * directly a constant provider.
     * </p>
     * @param wantNormalized if true, the provider will provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order or if no gravity field has read yet
     * @since 6.0
     */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order)
        throws OrekitException {
        return getConstantProvider(wantNormalized, degree, order);
    }

}
