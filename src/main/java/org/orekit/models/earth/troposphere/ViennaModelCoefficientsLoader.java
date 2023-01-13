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
package org.orekit.models.earth.troposphere;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateTimeComponents;

/** Loads Vienna tropospheric coefficients a given input stream.
 * A stream contains, for a given day and a given hour, the hydrostatic and wet zenith delays
 * and the ah and aw coefficients used for the computation of the mapping function.
 * The coefficients are given with a time interval of 6 hours.
 * <p>
 * A bilinear interpolation is performed the case of the user initialize the latitude and the
 * longitude with values that are not contained in the stream.
 * </p>
 * <p>
 * The coefficients are obtained from <a href="http://vmf.geo.tuwien.ac.at/trop_products/GRID/">Vienna Mapping Functions Open Access Data</a>.
 * Find more on the files at the <a href="http://vmf.geo.tuwien.ac.at/readme.txt">VMF Model Documentation</a>.
 * <p>
 * The files have to be extracted to UTF-8 text files before being read by this loader.
 * <p>
 * After extraction, it is assumed they are named VMFG_YYYYMMDD.Hhh for {@link ViennaOneModel} and VMF3_YYYYMMDD.Hhh {@link ViennaThreeModel}.
 * Where YYYY is the 4-digits year, MM the month, DD the day and hh the 2-digits hour.
 *
 * <p>
 * The format is always the same, with and example shown below for VMF1 model.
 * <p>
 * Example:
 * </p>
 * <pre>
 * ! Version:            1.0
 * ! Source:             J. Boehm, TU Vienna (created: 2018-11-20)
 * ! Data_types:         VMF1 (lat lon ah aw zhd zwd)
 * ! Epoch:              2018 11 19 18 00  0.0
 * ! Scale_factor:       1.e+00
 * ! Range/resolution:   -90 90 0 360 2 2.5
 * ! Comment:            http://vmf.geo.tuwien.ac.at/trop_products/GRID/2.5x2/VMF1/VMF1_OP/
 *  90.0   0.0 0.00116059  0.00055318  2.3043  0.0096
 *  90.0   2.5 0.00116059  0.00055318  2.3043  0.0096
 *  90.0   5.0 0.00116059  0.00055318  2.3043  0.0096
 *  90.0   7.5 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  10.0 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  12.5 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  15.0 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  17.5 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  20.0 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  22.5 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  25.0 0.00116059  0.00055318  2.3043  0.0096
 *  90.0  27.5 0.00116059  0.00055318  2.3043  0.0096
 * </pre>
 *
 * <p>It is not safe for multiple threads to share a single instance of this class.
 *
 * @author Bryan Cazabonne
 */
public class ViennaModelCoefficientsLoader extends AbstractSelfFeedingLoader
        implements DataLoader {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "VMF*_\\\\*\\*\\.*H$";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** The hydrostatic and wet a coefficients loaded. */
    private double[] coefficientsA;

    /** The hydrostatic and wet zenith delays loaded. */
    private double[] zenithDelay;

    /** Geodetic site latitude, radians.*/
    private double latitude;

    /** Geodetic site longitude, radians.*/
    private double longitude;

    /** Vienna tropospheric model type.*/
    private ViennaModelType type;

    /** Constructor with supported names given by user. This constructor uses the
     * {@link DataContext#getDefault() default data context}.
     *
     * @param supportedNames Supported names
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude geodetic latitude of the station, in radians
     * @param type the type of Vienna tropospheric model (one or three)
     * @see #ViennaModelCoefficientsLoader(String, double, double, ViennaModelType, DataProvidersManager)
     */
    @DefaultDataContext
    public ViennaModelCoefficientsLoader(final String supportedNames, final double latitude,
                                         final double longitude, final ViennaModelType type) {
        this(supportedNames, latitude, longitude, type, DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * Constructor with supported names and source of mapping function files given by the
     * user.
     *
     * @param supportedNames Supported names
     * @param latitude       geodetic latitude of the station, in radians
     * @param longitude      geodetic latitude of the station, in radians
     * @param type           the type of Vienna tropospheric model (one or three)
     * @param dataProvidersManager provides access to auxiliary files.
     * @since 10.1
     */
    public ViennaModelCoefficientsLoader(final String supportedNames,
                                         final double latitude,
                                         final double longitude,
                                         final ViennaModelType type,
                                         final DataProvidersManager dataProvidersManager) {
        super(supportedNames, dataProvidersManager);
        this.coefficientsA  = null;
        this.zenithDelay    = null;
        this.type           = type;
        this.latitude       = latitude;

        // Normalize longitude between 0 and 2π
        this.longitude = MathUtils.normalizeAngle(longitude, FastMath.PI);
    }

    /** Constructor with default supported names. This constructor uses the
     * {@link DataContext#getDefault() default data context}.
     *
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude geodetic latitude of the station, in radians
     * @param type the type of Vienna tropospheric model (one or three)
     * @see #ViennaModelCoefficientsLoader(String, double, double, ViennaModelType, DataProvidersManager)
     */
    @DefaultDataContext
    public ViennaModelCoefficientsLoader(final double latitude, final double longitude,
                                         final ViennaModelType type) {
        this(DEFAULT_SUPPORTED_NAMES, latitude, longitude, type);
    }

    /** Returns the a coefficients array.
     * <ul>
     * <li>double[0] = a<sub>h</sub>
     * <li>double[1] = a<sub>w</sub>
     * </ul>
     * @return the a coefficients array
     */
    public double[] getA() {
        return coefficientsA.clone();
    }

    /** Returns the zenith delay array.
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @return the zenith delay array
     */
    public double[] getZenithDelay() {
        return zenithDelay.clone();
    }

    @Override
    public String getSupportedNames() {
        return super.getSupportedNames();
    }

    /** Load the data using supported names .
     */
    public void loadViennaCoefficients() {
        feed(this);

        // Throw an exception if ah, ah, zh or zw were not loaded properly
        if (coefficientsA == null || zenithDelay == null) {
            throw new OrekitException(OrekitMessages.VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_LOADED,
                    getSupportedNames());
        }
    }

    /** Load the data for a given day.
     * @param dateTimeComponents date and time component.
     */
    public void loadViennaCoefficients(final DateTimeComponents dateTimeComponents) {

        // The files are named VMFG_YYYYMMDD.Hhh for Vienna-1 model and VMF3_YYYYMMDD.Hhh for Vienna-3 model.
        // Where YYYY is the 4-digits year, MM the month, DD the day of the month and hh the 2-digits hour.
        // Coefficients are only available for hh = 00 or 06 or 12 or 18.
        final int    year        = dateTimeComponents.getDate().getYear();
        final int    month       = dateTimeComponents.getDate().getMonth();
        final int    day         = dateTimeComponents.getDate().getDay();
        final int    hour        = dateTimeComponents.getTime().getHour();

        // Correct month format is with 2-digits.
        final String monthString;
        if (month < 10) {
            monthString = "0" + month;
        } else {
            monthString = String.valueOf(month);
        }

        // Correct day format is with 2-digits.
        final String dayString;
        if (day < 10) {
            dayString = "0" + day;
        } else {
            dayString = String.valueOf(day);
        }

        // Correct hour format is with 2-digits.
        final String hourString;
        if (hour < 10) {
            hourString = "0" + hour;
        } else {
            hourString = String.valueOf(hour);
        }

        // Name of the file is different between VMF1 and VMF3.
        // For VMF1 it starts with "VMFG" whereas with VMF3 it starts with "VMF3"
        switch (type) {
            case VIENNA_ONE:
                setSupportedNames(String.format("VMFG_%04d%2s%2s.H%2s", year, monthString, dayString, hourString));
                break;
            case VIENNA_THREE:
                setSupportedNames(String.format("VMF3_%04d%2s%2s.H%2s", year, monthString, dayString, hourString));
                break;
            default:
                break;
        }

        try {
            this.loadViennaCoefficients();
        } catch (OrekitException oe) {
            throw new OrekitException(oe,
                                      OrekitMessages.VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE,
                                      dateTimeComponents.toString());
        }
    }

    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    @Override
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException {

        int lineNumber = 0;
        String line = null;

        // Initialize Lists
        final ArrayList<Double> latitudes  = new ArrayList<>();
        final ArrayList<Double> longitudes = new ArrayList<>();
        final ArrayList<Double> ah         = new ArrayList<>();
        final ArrayList<Double> aw         = new ArrayList<>();
        final ArrayList<Double> zhd        = new ArrayList<>();
        final ArrayList<Double> zwd        = new ArrayList<>();

        // Open stream and parse data
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();

                // Fill latitudes and longitudes lists
                if (line.length() > 0 && line.startsWith("! Range/resolution:")) {
                    final String[] range_line = SEPARATOR.split(line);

                    // Latitudes list
                    for (double lat = Double.parseDouble(range_line[2]); lat <= Double.parseDouble(range_line[3]); lat = lat + Double.parseDouble(range_line[6])) {
                        latitudes.add(FastMath.toRadians(lat));
                    }

                    // Longitude list
                    for (double lon = Double.parseDouble(range_line[4]); lon <= Double.parseDouble(range_line[5]); lon = lon + Double.parseDouble(range_line[7])) {
                        longitudes.add(FastMath.toRadians(lon));
                        // For VFM1 files, header specify that longitudes end at 360°
                        // In reality they end at 357.5°. That is why we stop the loop when the longitude
                        // reaches 357.5°.
                        if (type == ViennaModelType.VIENNA_ONE && lon >= 357.5) {
                            break;
                        }
                    }
                }

                // Fill ah, aw, zhd and zwd lists
                if (line.length() > 0 && !line.startsWith("!")) {
                    final String[] values_line = SEPARATOR.split(line);
                    ah.add(Double.parseDouble(values_line[2]));
                    aw.add(Double.parseDouble(values_line[3]));
                    zhd.add(Double.parseDouble(values_line[4]));
                    zwd.add(Double.parseDouble(values_line[5]));
                }
            }

        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

        // Check that ah, aw, zh and zw were found (only one check is enough)
        if (ah.isEmpty()) {
            throw new OrekitException(OrekitMessages.NO_VIENNA_ACOEF_OR_ZENITH_DELAY_IN_FILE, name);
        }

        final int dimLat = latitudes.size();
        final int dimLon = longitudes.size();

        // Change Lists to Arrays
        final double[] xVal = new double[dimLat];
        for (int i = 0; i < dimLat; i++) {
            xVal[i] = latitudes.get(i);
        }

        final double[] yVal = new double[dimLon];
        for (int j = 0; j < dimLon; j++) {
            yVal[j] = longitudes.get(j);
        }

        final double[][] fvalAH = new double[dimLat][dimLon];
        final double[][] fvalAW = new double[dimLat][dimLon];
        final double[][] fvalZH = new double[dimLat][dimLon];
        final double[][] fvalZW = new double[dimLat][dimLon];

        int index = dimLon * dimLat;
        for (int x = 0; x < dimLat; x++) {
            for (int y = dimLon - 1; y >= 0; y--) {
                index = index - 1;
                fvalAH[x][y] = ah.get(index);
                fvalAW[x][y] = aw.get(index);
                fvalZH[x][y] = zhd.get(index);
                fvalZW[x][y] = zwd.get(index);
            }
        }

        // Build Bilinear Interpolation Functions
        final BilinearInterpolatingFunction functionAH = new BilinearInterpolatingFunction(xVal, yVal, fvalAH);
        final BilinearInterpolatingFunction functionAW = new BilinearInterpolatingFunction(xVal, yVal, fvalAW);
        final BilinearInterpolatingFunction functionZH = new BilinearInterpolatingFunction(xVal, yVal, fvalZH);
        final BilinearInterpolatingFunction functionZW = new BilinearInterpolatingFunction(xVal, yVal, fvalZW);

        coefficientsA = new double[2];
        zenithDelay   = new double[2];

        // Get the values for the given latitude and longitude
        coefficientsA[0] = functionAH.value(latitude, longitude);
        coefficientsA[1] = functionAW.value(latitude, longitude);
        zenithDelay[0]   = functionZH.value(latitude, longitude);
        zenithDelay[1]   = functionZW.value(latitude, longitude);

    }

}
