/* Copyright 2002-2019 CS Systèmes d'Information
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
import java.util.List;

import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/** The Global Pressure and Temperature 2 (GPT2) model.
 * This model is an empirical model that provides the temperature, the pressure and the water vapor pressure
 * of a site depending its latitude and  longitude. This model also provides the a<sub>h</sub>
 * and a<sub>w</sub> coefficients used for the {@link ViennaOneModel Vienna 1} model.
 * <p>
 * The requisite coefficients for the computation of the weather parameters are provided by the
 * Department of Geodesy and Geoinformation of the Vienna University. They are based on a 5° x 5°
 * external grid file "gpt2_5.grd" available at: <a href="http://vmf.geo.tuwien.ac.at/codes/"> link</a>
 * </p>
 * <p>
 * A bilinear interpolation is performed in order to obtained the correct values of the weather parameters.
 * </p>
 * <p>
 * The format is always the same, with and example shown below for the pressure and the temperature.
 * <p>
 * Example:
 * </p>
 * <pre>
 * %  lat    lon   p:a0    A1   B1   A2   B2  T:a0    A1   B1   A2   B2
 *   87.5    2.5 101421    21  409 -217 -122 259.2 -13.2 -6.1  2.6  0.3
 *   87.5    7.5 101416    21  411 -213 -120 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   12.5 101411    22  413 -209 -118 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   17.5 101407    23  415 -205 -116 259.4 -13.0 -6.1  2.6  0.3
 *   ...
 * </pre>
 *
 * @see K. Lagler, M. Schindelegger, J. Böhm, H. Krasna, T. Nilsson (2013),
 * GPT2: empirical slant delay model for radio space geodetic techniques. Geophys
 * Res Lett 40(6):1069–1073. doi:10.1002/grl.50288
 *
 * @author Bryan Cazabonne
 *
 */
public class GlobalPressureTemperature2Model implements DataLoader, WeatherModel {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "gpt2_5.grd";

    /** Standard gravity constant [m/s²]. */
    private static final double G = Constants.G0_STANDARD_GRAVITY;

    /** Ideal gas constant for dry air [J/kg/K]. */
    private static final double R = 287.0;

    /** Regular expression for supported file name. */
    private String supportedNames;

    /** The hydrostatic and wet a coefficients loaded. */
    private double[] coefficientsA;

    /** Geodetic site latitude, radians.*/
    private double latitude;

    /** Geodetic site longitude, radians.*/
    private double longitude;

    /** Temperature site, in kelvins. */
    private double temperature;

    /** Pressure site, in hPa. */
    private double pressure;

    /** water vapour pressure, in hPa. */
    private double e0;

    /** The height of the station in m. */
    private double height;

    /** Geoid used to compute the undulations. */
    private final Geoid geoid;

    /** Current date. */
    private AbsoluteDate date;

    /** Day of year. */
    private int dayOfYear;

    /** Constructor with supported names given by user.
     * @param supportedNames supported names
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude longitude geodetic latitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body
     */
    public GlobalPressureTemperature2Model(final String supportedNames, final double latitude,
                                           final double longitude, final Geoid geoid) {
        this.coefficientsA  = null;
        this.temperature    = Double.NaN;
        this.pressure       = Double.NaN;
        this.e0             = Double.NaN;
        this.supportedNames = supportedNames;
        this.geoid          = geoid;
        this.latitude       = latitude;

        // Normalize longitude between 0 and 2π
        this.longitude   = MathUtils.normalizeAngle(longitude, FastMath.PI);
    }

    /** Constructor with default supported names.
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude geodetic latitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body
     */
    public GlobalPressureTemperature2Model(final double latitude, final double longitude, final Geoid geoid) {
        this(DEFAULT_SUPPORTED_NAMES, latitude, longitude, geoid);
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

    /** Returns the temperature at the station [K].
     * @return the temperature at the station [K]
     */
    public double getTemperature() {
        return temperature;
    }

    /** Returns the pressure at the station [hPa].
     * @return the pressure at the station [hPa]
     */
    public double getPressure() {
        return pressure;
    }

    /** Returns the water vapor pressure at the station [hPa].
     * @return the water vapor pressure at the station [hPa]
     */
    public double getWaterVaporPressure() {
        return e0;
    }

    /** Returns the supported names of the loader.
     * @return the supported names
     */
    public String getSupportedNames() {
        return supportedNames;
    }

    @Override
    public void weatherParameters(final double stationHeight, final AbsoluteDate currentDate) {
        this.date      = currentDate;
        this.dayOfYear = currentDate.getComponents(TimeScalesFactory.getUTC()).getDate().getDayOfYear();
        this.height    = stationHeight;
        DataProvidersManager.getInstance().feed(supportedNames, this);
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
        // Latitudes [rad]
        final List<Double> latitudes    = new ArrayList<>();
        // Longitudes [rad]
        final List<Double> longitudes   = new ArrayList<>();
        // Orthometric grid height [m]
        final List<Double> hS           = new ArrayList<>();
        // Hydrostatic coefficient a
        final List<Double> ah           = new ArrayList<>();
        // Wet coefficient a
        final List<Double> aw           = new ArrayList<>();
        // Temperature [K]
        final List<Double> temperature0 = new ArrayList<>();
        // Temperature gradient [K/m]
        final List<Double> dT           = new ArrayList<>();
        // Pressure [Pa]
        final List<Double> pressure0    = new ArrayList<>();
        // Specific humidity [kg/kg]
        final List<Double> qv0          = new ArrayList<>();

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            ++lineNumber;
            line = line.trim();

            try {

                // Fill ah, aw, temp, pres, humidity lists
                if (line.length() > 0 && !line.startsWith("%")) {
                    final String[] values_line = line.split(splitter);
                    hS.add(Double.valueOf(values_line[23]));

                    fillArray(pressure0,    values_line, 2, 3, 4, 5, 6);
                    fillArray(temperature0, values_line, 7, 8, 9, 10, 11);
                    fillArray(qv0,          values_line, 12, 13, 14, 15, 16);
                    fillArray(dT,           values_line, 17, 18, 19, 20, 21);
                    fillArray(ah,           values_line, 24, 25, 26, 27, 28);
                    fillArray(aw,           values_line, 29, 30, 31, 32, 33);
                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }
        }

        // Close the stream after reading
        input.close();

        // Latitudes list
        for (double lat = -87.5; lat <= 87.5; lat = lat + 5.0) {
            latitudes.add(FastMath.toRadians(lat));
        }

        // Longitude list
        for (double lon = 2.5; lon <= 357.5; lon = lon + 5.0) {
            longitudes.add(FastMath.toRadians(lon));
        }

        final int dimLat = latitudes.size();
        final int dimLon = longitudes.size();

        // Change List to double[]
        final double[] xVal = new double[dimLat];
        for (int i = 0; i < dimLat; i++) {
            xVal[i] = latitudes.get(i);
        }

        // Change List to double[]
        final double[] yVal = new double[dimLon];
        for (int j = 0; j < dimLon; j++) {
            yVal[j] = longitudes.get(j);
        }

        final double[][] fvalPressure0    = new double[dimLat][dimLon];
        final double[][] fvalTemperature0 = new double[dimLat][dimLon];
        final double[][] fvalqv0          = new double[dimLat][dimLon];
        final double[][] fvaldT           = new double[dimLat][dimLon];
        final double[][] fvalHS           = new double[dimLat][dimLon];
        final double[][] fvalAH           = new double[dimLat][dimLon];
        final double[][] fvalAW           = new double[dimLat][dimLon];

        int index = dimLon * dimLat;
        for (int x = 0; x < dimLat; x++) {
            for (int y = dimLon - 1; y >= 0; y--) {
                index = index - 1;
                fvalPressure0[x][y]    = pressure0.get(index);
                fvalTemperature0[x][y] = temperature0.get(index);
                fvalqv0[x][y]          = qv0.get(index);
                fvaldT[x][y]           = dT.get(index);
                fvalHS[x][y]           = hS.get(index);
                fvalAH[x][y]           = ah.get(index);
                fvalAW[x][y]           = aw.get(index);
            }
        }

        // Build Bilinear Interpolation Functions
        final BilinearInterpolatingFunction functionPressure0    = new BilinearInterpolatingFunction(xVal, yVal, fvalPressure0);
        final BilinearInterpolatingFunction functionTemperature0 = new BilinearInterpolatingFunction(xVal, yVal, fvalTemperature0);
        final BilinearInterpolatingFunction functionqv0          = new BilinearInterpolatingFunction(xVal, yVal, fvalqv0);
        final BilinearInterpolatingFunction functiondT           = new BilinearInterpolatingFunction(xVal, yVal, fvaldT);
        final BilinearInterpolatingFunction functionHS           = new BilinearInterpolatingFunction(xVal, yVal, fvalHS);
        final BilinearInterpolatingFunction functionAH           = new BilinearInterpolatingFunction(xVal, yVal, fvalAH);
        final BilinearInterpolatingFunction functionAW           = new BilinearInterpolatingFunction(xVal, yVal, fvalAW);

        // ah and aw coefficients
        coefficientsA = new double[2];
        coefficientsA[0] = functionAH.value(latitude, longitude) * 0.001;
        coefficientsA[1] = functionAW.value(latitude, longitude) * 0.001;

        // Corrected height (can be negative)
        final double undu = geoid.getUndulation(latitude, longitude, date);
        final double correctedheight = height - undu - functionHS.value(latitude, longitude);

        // Temperature gradient [K/m]
        final double dTdH = functiondT.value(latitude, longitude) * 0.001;

        // Specific humidity
        final double qv = functionqv0.value(latitude, longitude) * 0.001;

        // For the computation of the temperature and the pressure, we use
        // the standard ICAO atmosphere formulas.

        // Temperature [K]
        final double t0 = functionTemperature0.value(latitude, longitude);
        this.temperature = t0 + dTdH * correctedheight;

        // Pressure [hPa]
        final double p0 = functionPressure0.value(latitude, longitude);
        final double exponent = G / (dTdH * R);
        this.pressure = p0 * FastMath.pow(1 - (dTdH / t0) * correctedheight, exponent) * 0.01;

        // Water vapor pressure [hPa]
        this.e0 = qv * pressure / (0.622 + 0.378 * qv);
    }

    /** Fill the arrays containing the weather coefficients.
     * @param array array to fill
     * @param values string array containing the values
     * @param indexA0 index of coefficient A0 for this parameter
     * @param indexA1 index of coefficient A1 for this parameter
     * @param indexB1 index of coefficient B1 for this parameter
     * @param indexA2 index of coefficient A2 for this parameter
     * @param indexB2 index of coefficient B2 for this parameter
     */
    private void fillArray(final List<Double> array, final String[] values,
                           final int indexA0, final int indexA1, final int indexB1,
                           final int indexA2, final int indexB2) {

        // Parse coefficients
        final double a0 = Double.parseDouble(values[indexA0]);
        final double a1 = Double.parseDouble(values[indexA1]);
        final double b1 = Double.parseDouble(values[indexB1]);
        final double a2 = Double.parseDouble(values[indexA2]);
        final double b2 = Double.parseDouble(values[indexB2]);

        // Temporal factors
        final double coef = (dayOfYear / 365.25) * 2 * FastMath.PI;
        final SinCos sc1  = FastMath.sinCos(coef);
        final SinCos sc2  = FastMath.sinCos(2.0 * coef);

        final double value = a0 + a1 * sc1.cos() + b1 * sc1.sin() + a2 * sc2.cos() + b2 * sc2.sin();
        array.add(value);
    }
}
