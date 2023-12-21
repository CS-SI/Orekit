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
package org.orekit.models.earth.weather;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.troposphere.FieldViennaACoefficients;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.models.earth.troposphere.ViennaAProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/** The Global Pressure and Temperature 2 (GPT2) model.
 * This model is an empirical model that provides the temperature, the pressure and the water vapor pressure
 * of a site depending its latitude and  longitude. This model also {@link ViennaACoefficients provides}
 * the a<sub>h</sub> and a<sub>w</sub> coefficients for Vienna models.
 * <p>
 * The requisite coefficients for the computation of the weather parameters are provided by the
 * Department of Geodesy and Geoinformation of the Vienna University. They are based on an
 * external grid file like "gpt2_1.grd" (1° x 1°) or "gpt2_5.grd" (5° x 5°) available at:
 * <a href="http://vmf.geo.tuwien.ac.at/codes/"> link</a>
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
 * @see "K. Lagler, M. Schindelegger, J. Böhm, H. Krasna, T. Nilsson (2013),
 * GPT2: empirical slant delay model for radio space geodetic techniques. Geophys
 * Res Lett 40(6):1069–1073. doi:10.1002/grl.50288"
 *
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 12.1
 */
public class GlobalPressureTemperature2 implements ViennaAProvider, PressureTemperatureHumidityProvider {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Standard gravity constant [m/s²]. */
    private static final double G = Constants.G0_STANDARD_GRAVITY;

    /** Ideal gas constant for dry air [J/kg/K]. */
    private static final double R = 287.0;

    /** Conversion factor from degrees to mill arcseconds. */
    private static final int DEG_TO_MAS = 3600000;

    /** Loaded grid. */
    private final Grid grid;

    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Constructor with supported names and source of GPT2 auxiliary data given by user.
     *
     * @param source grid data source
     * @param utc UTC time scale.
     * @exception IOException if grid data cannot be read
     */
    public GlobalPressureTemperature2(final DataSource source, final TimeScale utc)
        throws IOException {
        this.utc = utc;

        // load the grid data
        try (InputStream         is  = source.getOpener().openStreamOnce();
             BufferedInputStream bis = new BufferedInputStream(is)) {
            final Parser parser = new Parser();
            parser.loadData(bis, source.getName());
            grid = parser.grid;
        }

    }

    /**
     * Constructor with supported names and source of GPT2 auxiliary data given by user.
     *
     * @param supportedNames supported names
     * @param dataProvidersManager provides access to auxiliary data.
     * @param utc UTC time scale.
     * @deprecated as of 12.1 used only by {@link GlobalPressureTemperature2Model}
     */
    @Deprecated
    protected GlobalPressureTemperature2(final String supportedNames,
                                         final DataProvidersManager dataProvidersManager,
                                         final TimeScale utc) {
        this.utc = utc;
        final Parser parser = new Parser();
        dataProvidersManager.feed(supportedNames, parser);
        grid = parser.grid;
    }

    /** {@inheritDoc} */
    @Override
    public ViennaACoefficients getA(final GeodeticPoint location, final AbsoluteDate date) {

        final CellInterpolator interpolator = grid.getInterpolator(location.getLatitude(), location.getLongitude());
        final int dayOfYear = date.getComponents(utc).getDate().getDayOfYear();

        // ah and aw coefficients
        return new ViennaACoefficients(interpolator.interpolate(e -> e.getAh().evaluate(dayOfYear)) * 0.001,
                                       interpolator.interpolate(e -> e.getAw().evaluate(dayOfYear)) * 0.001);

    }

    /** {@inheritDoc} */
    @Override
    public PressureTemperatureHumidity getWeatherParamerers(final GeodeticPoint location, final AbsoluteDate date) {

        final CellInterpolator interpolator = grid.getInterpolator(location.getLatitude(), location.getLongitude());
        final int dayOfYear = date.getComponents(utc).getDate().getDayOfYear();

        // Corrected height (can be negative)
        final double undu            = interpolator.interpolate(e -> e.getUndulation());
        final double correctedheight = location.getAltitude() - undu - interpolator.interpolate(e -> e.getHs());

        // Temperature gradient [K/m]
        final double dTdH = interpolator.interpolate(e -> e.getDt().evaluate(dayOfYear)) * 0.001;

        // Specific humidity
        final double qv = interpolator.interpolate(e -> e.getQv0().evaluate(dayOfYear)) * 0.001;

        // For the computation of the temperature and the pressure, we use
        // the standard ICAO atmosphere formulas.

        // Temperature [K]
        final double t0 = interpolator.interpolate(e -> e.getTemperature0().evaluate(dayOfYear));
        final double temperature = t0 + dTdH * correctedheight;

        // Pressure [hPa]
        final double p0       = interpolator.interpolate(e -> e.getPressure0().evaluate(dayOfYear));
        final double exponent = G / (dTdH * R);
        final double pressure = p0 * FastMath.pow(1 - (dTdH / t0) * correctedheight, exponent) * 0.01;

        // Water vapor pressure [hPa]
        final double e0 = qv * pressure / (0.622 + 0.378 * qv);

        return new PressureTemperatureHumidity(location.getAltitude(),
                                               TroposphericModelUtils.HECTO_PASCAL.toSI(pressure),
                                               temperature,
                                               TroposphericModelUtils.HECTO_PASCAL.toSI(e0));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldViennaACoefficients<T> getA(final FieldGeodeticPoint<T> location,
                                                                                final FieldAbsoluteDate<T> date) {

        final FieldCellInterpolator<T> interpolator = grid.getInterpolator(location.getLatitude(), location.getLongitude());
        final int dayOfYear = date.getComponents(utc).getDate().getDayOfYear();

        // ah and aw coefficients
        return new FieldViennaACoefficients<>(interpolator.interpolate(e -> e.getAh().evaluate(dayOfYear)).multiply(0.001),
                                              interpolator.interpolate(e -> e.getAw().evaluate(dayOfYear)).multiply(0.001));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T> getWeatherParamerers(final FieldGeodeticPoint<T> location,
                                                                                                        final FieldAbsoluteDate<T> date) {

        final FieldCellInterpolator<T> interpolator = grid.getInterpolator(location.getLatitude(), location.getLongitude());
        final int dayOfYear = date.getComponents(utc).getDate().getDayOfYear();

        // Corrected height (can be negative)
        final T undu            = interpolator.interpolate(e -> e.getUndulation());
        final T correctedheight = location.getAltitude().subtract(undu).subtract(interpolator.interpolate(e -> e.getHs()));

        // Temperature gradient [K/m]
        final T dTdH = interpolator.interpolate(e -> e.getDt().evaluate(dayOfYear)).multiply(0.001);

        // Specific humidity
        final T qv = interpolator.interpolate(e -> e.getQv0().evaluate(dayOfYear)).multiply(0.001);

        // For the computation of the temperature and the pressure, we use
        // the standard ICAO atmosphere formulas.

        // Temperature [K]
        final T t0 = interpolator.interpolate(e -> e.getTemperature0().evaluate(dayOfYear));
        final T temperature = correctedheight.multiply(dTdH).add(t0);

        // Pressure [hPa]
        final T p0       = interpolator.interpolate(e -> e.getPressure0().evaluate(dayOfYear));
        final T exponent = dTdH.multiply(R).reciprocal().multiply(G);
        final T pressure = FastMath.pow(correctedheight.multiply(dTdH.negate().divide(t0)).add(1), exponent).multiply(p0.multiply(0.001));

        // Water vapor pressure [hPa]
        final T e0 = pressure.multiply(qv.divide(qv.multiply(0.378).add(0.622 )));

        return new FieldPressureTemperatureHumidity<>(location.getAltitude(),
                                                      TroposphericModelUtils.HECTO_PASCAL.toSI(pressure),
                                                      temperature,
                                                      TroposphericModelUtils.HECTO_PASCAL.toSI(e0));

    }

    /** Parser for GPT2 grid files. */
    private static class Parser implements DataLoader {

        /** Grid entries. */
        private Grid grid;

        @Override
        public boolean stillAcceptsData() {
            return grid == null;
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException {

            final SortedSet<Integer> latSample = new TreeSet<>();
            final SortedSet<Integer> lonSample = new TreeSet<>();
            final List<GridEntry>    entries   = new ArrayList<>();

            // Open stream and parse data
            int   lineNumber = 0;
            String line      = null;
            try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                 BufferedReader    br = new BufferedReader(isr)) {

                for (line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    line = line.trim();

                    // read grid data
                    if (line.length() > 0 && !line.startsWith("%")) {
                        final String[] fields = SEPARATOR.split(line);
                        final double latDegree = Double.parseDouble(fields[0]);
                        final double lonDegree = Double.parseDouble(fields[1]);
                        final GridEntry entry = new GridEntry(FastMath.toRadians(latDegree),
                                                              (int) FastMath.rint(latDegree * DEG_TO_MAS),
                                                              FastMath.toRadians(lonDegree),
                                                              (int) FastMath.rint(lonDegree * DEG_TO_MAS),
                                                              Double.parseDouble(fields[22]),
                                                              Double.parseDouble(fields[23]),
                                                              createModel(fields,  2),
                                                              createModel(fields,  7),
                                                              createModel(fields, 12),
                                                              createModel(fields, 17),
                                                              createModel(fields, 24),
                                                              createModel(fields, 29));
                        latSample.add(entry.getLatKey());
                        lonSample.add(entry.getLonKey());
                        entries.add(entry);
                    }

                }
            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            // organize entries in a grid that wraps arouns Earth in longitude
            grid = new Grid(latSample, lonSample, entries, name);

        }

        /** Create a seasonal model.
         * @param fields parsed fields
         * @param first index of the constant field
         * @return created model
         */
        private SeasonalModel createModel(final String[] fields, final int first) {
            return new SeasonalModel(Double.parseDouble(fields[first    ]),
                                     Double.parseDouble(fields[first + 1]),
                                     Double.parseDouble(fields[first + 2]),
                                     Double.parseDouble(fields[first + 3]),
                                     Double.parseDouble(fields[first + 4]));
        }

    }

}
