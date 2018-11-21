/* Copyright 2002-2018 CS Systèmes d'Information
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
import java.util.ArrayList;
import java.util.Arrays;

import org.hipparchus.analysis.BivariateFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.util.MathArrays;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateTimeComponents;

/** Loads Vienna-1 tropospheric coefficients a given input stream.
 * A stream contains, for a given day and a given hour, the hydrostatic and wet zenith delays
 * and the ah and aw coefficients used for the computation of the mapping function.
 * The coefficients are given with a time interval of 6 hours, as well as on a global 2.5° x 2.0°
 * [longitude x latitude] grid.
 * <p>
 * A bilinear interpolation is performed the case of the user initialize the latitude and the
 * longitude with values that are not contained in the stream.
 * </p>
 * <p>
 * The coefficients are obtained from <a href="http://vmf.geo.tuwien.ac.at/trop_products/GRID/2.5x2/VMF1/">Vienna Mapping Functions Open Access Data</a>.
 * Find more on the files at the <a href="http://vmf.geo.tuwien.ac.at/readme.txt">VMF1 Model Documentation</a>.
 * <p>
 * The files have to be extracted to UTF-8 text files before being read by this loader.
 * <p>
 * After extraction, it is assumed they are named VMFG_YYYYMMDD.Hhh where YYYY is the 4-digits year, MM the month, DD the day
 * and hh the 2-digits hour.
 *
 * <p>
 * The format is always the same, with and example shown below.
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
 * @author Bryan Cazabonne
 */
public class ViennaOneCoefficientsLoader implements DataLoader {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "VMFG_\\\\*\\*\\.*H$";

    /** Regular expression for supported file name. */
    private String supportedNames;

    /** The hydrostatic and wet a coefficients loaded. */
    private double[] coefficientsA;

    /** The hydrostatic and wet zenith delays loaded. */
    private double[] zenithDelay;

    /** Geodetic site latitude, degrees.*/
    private double latitude;

    /** Geodetic site longitude, degrees.*/
    private double longitude;

    /** Constructor with supported names given by user.
     * @param supportedNames Supported names
     * @param latitude geodetic latitude of the station, between [-90°,90°]
     * @param longitude geodetic latitude of the station, between [0°,357.5°]
     */
    public ViennaOneCoefficientsLoader(final String supportedNames, final double latitude,
                                       final double longitude) {
        this.coefficientsA  = null;
        this.zenithDelay    = null;
        this.supportedNames = supportedNames;
        this.latitude       = latitude;
        this.longitude      = longitude;
    }

    /** Constructor with default supported names.
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude geodetic latitude of the station, in radians
     */
    public ViennaOneCoefficientsLoader(final double latitude, final double longitude) {
        this(DEFAULT_SUPPORTED_NAMES, latitude, longitude);
    }

    /** Returns the a coefficients array.
     * <ul>
     * <li>double[0] = m<sub>h</sub>(e) -&gt hydrostatic mapping function
     * <li>double[1] = m<sub>w</sub>(e) -&gt wet mapping function
     * </ul>
     * @return the a coefficients array
     */
    public double[] getA() {
        return coefficientsA.clone();
    }

    /** Returns the zenith delay array.
     * <ul>
     * <li>double[0] = D<sub>hz</sub> -&gt zenith hydrostatic delay
     * <li>double[1] = D<sub>wz</sub> -&gt zenith wet delay
     * </ul>
     * @return the zenith delay array
     */
    public double[] getZenithDelay() {
        return zenithDelay.clone();
    }

    /** Returns the supported names of the loader.
     * @return the supported names
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    /** Load the data using supported names .
     */
    public void loadViennaOneCoefficients() {
        DataProvidersManager.getInstance().feed(supportedNames, this);

        // Throw an exception if alphas or betas were not loaded properly
        if (coefficientsA == null || zenithDelay == null) {
            throw new OrekitException(OrekitMessages.VIENNA_ONE_ACOEF_OR_ZENITH_DELAY_NOT_LOADED, supportedNames);
        }
    }

    /** Load the data for a given day.
     * @param dateTimeComponents date and time component.
     * @throws OrekitException if the coefficients could not be loaded
     */
    public void loadViennaOneCoefficients(final DateTimeComponents dateTimeComponents) {

        // The files are named VMFG_YYYYMMDD.Hhh where YYYY is the 4-digits year, MM the month, DD the day of the month
        // and hh the 2-digits hour.
        // Coefficients are only available for hh = 00 or 06 or 12 or 18.
        final int    year        = dateTimeComponents.getDate().getYear();
        final int    month       = dateTimeComponents.getDate().getMonth();
        final int    day         = dateTimeComponents.getDate().getDay();
        final int    hour        = dateTimeComponents.getTime().getHour();

        // Correct month format is with 2-digits.
        final String monthString;
        if (month < 10) {
            monthString = "0" + String.valueOf(month);
        } else {
            monthString = String.valueOf(month);
        }

        // Correct day format is with 2-digits.
        final String dayString;
        if (day < 10) {
            dayString = "0" + String.valueOf(day);
        } else {
            dayString = String.valueOf(day);
        }

        // Correct hour format is with 2-digits.
        final String hourString;
        if (hour < 10) {
            hourString = "0" + String.valueOf(hour);
        } else {
            hourString = String.valueOf(hour);
        }

        this.supportedNames = String.format("VMFG_%04d%2s%2s.H%2s", year, monthString, dayString, hourString);

        try {
            this.loadViennaOneCoefficients();
        } catch (OrekitException oe) {
            throw new OrekitException(oe,
                                      OrekitMessages.VIENNA_ONE_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE,
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

        // Open stream and parse data
        final BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        int lineNumber = 0;
        final String splitter = "\\s+";

        // Initialize Lists
        final ArrayList<Double> latitudes  = new ArrayList<>();
        final ArrayList<Double> longitudes = new ArrayList<>();
        final ArrayList<Double> ah         = new ArrayList<>();
        final ArrayList<Double> aw         = new ArrayList<>();
        final ArrayList<Double> zhd        = new ArrayList<>();
        final ArrayList<Double> zwd        = new ArrayList<>();

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            ++lineNumber;
            line = line.trim();

            try {
                // Fill latitudes and longitudes lists
                if (line.length() > 0 && line.startsWith("! Range/resolution:")) {
                    final String[] range_line = line.split(splitter);

                    // Latitudes list
                    for (double lat = Double.valueOf(range_line[2]); lat <= Double.valueOf(range_line[3]); lat = lat + Double.valueOf(range_line[6])) {
                        latitudes.add(lat);
                    }

                    // Longitude list -> Stop at 357.5° not at 360°
                    for (double lon = Double.valueOf(range_line[4]); lon < Double.valueOf(range_line[5]); lon = lon + Double.valueOf(range_line[7])) {
                        longitudes.add(lon);
                    }
                }

                // Fill ah, aw, zhd and zwd lists
                if (line.length() > 0 && !line.startsWith("!")) {
                    final String[] values_line = line.split(splitter);
                    ah.add(Double.valueOf(values_line[2]));
                    aw.add(Double.valueOf(values_line[3]));
                    zhd.add(Double.valueOf(values_line[4]));
                    zwd.add(Double.valueOf(values_line[5]));
                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

        }

        // Close the stream after reading
        input.close();

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

        // Check that ah, aw, zh and zw were found
        if (coefficientsA == null || zenithDelay == null) {
            throw new OrekitException(OrekitMessages.NO_VIENNA_ONE_ACOEF_OR_ZENITH_DELAY_IN_FILE, name);
        }

    }

    /**
     * Function that implements a standard bilinear interpolation.
     * The interpolation as found
     * in the Wikipedia reference <a href =
     * "http://en.wikipedia.org/wiki/Bilinear_interpolation">BiLinear
     * Interpolation</a>. This is a stand-in until Apache Math has a
     * bilinear interpolator
     */
    private static class BilinearInterpolatingFunction implements BivariateFunction {

        /**
         * The minimum number of points that are needed to compute the
         * function.
         */
        private static final int MIN_NUM_POINTS = 2;

        /** Samples x-coordinates. */
        private final double[] xval;

        /** Samples y-coordinates. */
        private final double[] yval;

        /** Set of cubic splines patching the whole data grid. */
        private final double[][] fval;

        /**
         * @param x Sample values of the x-coordinate, in increasing order.
         * @param y Sample values of the y-coordinate, in increasing order.
         * @param f Values of the function on every grid point. the expected
         *        number of elements.
         * @throws MathIllegalArgumentException if the length of x and y don't
         *         match the row, column height of f, or if any of the arguments
         *         are null, or if any of the arrays has zero length, or if
         *         {@code x} or {@code y} are not strictly increasing.
         */
        BilinearInterpolatingFunction(final double[] x, final double[] y, final double[][] f)
                        throws MathIllegalArgumentException {

            if (x == null || y == null || f == null || f[0] == null) {
                throw new IllegalArgumentException("All arguments must be non-null");
            }

            final int xLen = x.length;
            final int yLen = y.length;

            if (xLen == 0 || yLen == 0 || f.length == 0 || f[0].length == 0) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.NO_DATA);
            }

            if (xLen < MIN_NUM_POINTS || yLen < MIN_NUM_POINTS || f.length < MIN_NUM_POINTS ||
                            f[0].length < MIN_NUM_POINTS) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.INSUFFICIENT_DATA);
            }

            if (xLen != f.length) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                       xLen, f.length);
            }

            if (yLen != f[0].length) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                                       yLen, f[0].length);
            }

            MathArrays.checkOrder(x);
            MathArrays.checkOrder(y);

            xval = x.clone();
            yval = y.clone();
            fval = f.clone();
        }

        @Override
        public double value(final double x, final double y) {
            final int offset = 1;
            final int count = offset + 1;
            final int i = searchIndex(x, xval, offset, count);
            final int j = searchIndex(y, yval, offset, count);

            final double x1 = xval[i];
            final double x2 = xval[i + 1];
            final double y1 = yval[j];
            final double y2 = yval[j + 1];
            final double fQ11 = fval[i][j];
            final double fQ21 = fval[i + 1][j];
            final double fQ12 = fval[i][j + 1];
            final double fQ22 = fval[i + 1][j + 1];

            final double f = (fQ11 * (x2 - x)  * (y2 - y) +
                            fQ21 * (x  - x1) * (y2 - y) +
                            fQ12 * (x2 - x)  * (y  - y1) +
                            fQ22 * (x  - x1) * (y  - y1)) /
                            ((x2 - x1) * (y2 - y1));

            return f;
        }

        /**
         * @param c Coordinate.
         * @param val Coordinate samples.
         * @param offset how far back from found value to offset for
         *        querying
         * @param count total number of elements forward from beginning that
         *        will be queried
         * @return the index in {@code val} corresponding to the interval
         *         containing {@code c}.
         * @throws MathIllegalArgumentException if {@code c} is out of the range
         *         defined by the boundary values of {@code val}.
         */
        private int searchIndex(final double c, final double[] val, final int offset, final int count)
            throws MathIllegalArgumentException {
            int r = Arrays.binarySearch(val, c);

            if (r == -1 || r == -val.length - 1) {
                throw new MathIllegalArgumentException(LocalizedCoreFormats.OUT_OF_RANGE_SIMPLE,
                                                       c, val[0], val[val.length - 1]);
            }

            if (r < 0) {
                // "c" in within an interpolation sub-interval, which
                // returns
                // negative
                // need to remove the negative sign for consistency
                r = -r - offset - 1;
            } else {
                r -= offset;
            }

            if (r < 0) {
                r = 0;
            }

            if ((r + count) >= val.length) {
                // "c" is the last sample of the range: Return the index
                // of the sample at the lower end of the last sub-interval.
                r = val.length - count;
            }

            return r;
        }

    }

}
