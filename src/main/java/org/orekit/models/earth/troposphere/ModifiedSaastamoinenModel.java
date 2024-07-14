/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.analysis.interpolation.LinearInterpolator;
import org.hipparchus.analysis.polynomials.PolynomialSplineFunction;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.HeightDependentPressureTemperatureHumidityConverter;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.Wang1988;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.InterpolationTableLoader;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** The modified Saastamoinen model. Estimates the path delay imposed to
 * electro-magnetic signals by the troposphere according to the formula:
 * <pre>
 * δ = 2.277e-3 / cos z * (P + (1255 / T + 0.05) * e - B * tan² z) + δR
 * </pre>
 * with the following input data provided to the model:
 * <ul>
 * <li>z: zenith angle</li>
 * <li>P: atmospheric pressure</li>
 * <li>T: temperature</li>
 * <li>e: partial pressure of water vapour</li>
 * <li>B, δR: correction terms</li>
 * </ul>
 * <p>
 * The model supports custom δR correction terms to be read from a
 * configuration file (saastamoinen-correction.txt) via the
 * {@link DataProvidersManager}.
 * </p>
 * @author Thomas Neidhart
 * @see "Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007"
 * @since 12.0
 */
public class ModifiedSaastamoinenModel implements TroposphericModel {

    /** Default file name for δR correction term table. */
    public static final String DELTA_R_FILE_NAME = "^saastamoinen-correction\\.txt$";

    /** Default lowest acceptable elevation angle [rad]. */
    public static final double DEFAULT_LOW_ELEVATION_THRESHOLD = 0.05;

    /** Provider for water pressure. */
    public static final Wang1988 WATER = new Wang1988();

    /** First pattern for δR correction term table. */
    private static final Pattern FIRST_DELTA_R_PATTERN = Pattern.compile("^\\^");

    /** Second pattern for δR correction term table. */
    private static final Pattern SECOND_DELTA_R_PATTERN = Pattern.compile("\\$$");

    /** Base delay coefficient. */
    private static final double L0 = 2.277e-5;

    /** Temperature numerator. */
    private static final double T_NUM = 1255;

    /** Wet offset. */
    private static final double WET_OFFSET = 0.05;

    /** X values for the B function. */
    private static final double[] X_VALUES_FOR_B = {
        0.0, 500.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 4000.0, 5000.0
    };

    /** Y values for the B function.
     * <p>
     * The values have been scaled up by a factor 100.0 due to conversion from hPa to Pa.
     * </p>
     */
    private static final double[] Y_VALUES_FOR_B = {
        115.6, 107.9, 100.6, 93.8, 87.4, 81.3, 75.7, 65.4, 56.3
    };

    /** Interpolation function for the B correction term. */
    private static final PolynomialSplineFunction B_FUNCTION = new LinearInterpolator().interpolate(X_VALUES_FOR_B, Y_VALUES_FOR_B);

    /** Interpolation function for the delta R correction term. */
    private final BilinearInterpolatingFunction deltaRFunction;

    /** Provider for atmospheric pressure, temperature and humidity at reference altitude. */
    private final PressureTemperatureHumidityProvider pth0Provider;

    /** Height dependent converter for pressure, temperature and humidity. */
    private final HeightDependentPressureTemperatureHumidityConverter converter;

    /** Lowest acceptable elevation angle [rad]. */
    private double lowElevationThreshold;

    /**
     * Create a new Saastamoinen model for the troposphere using the given environmental
     * conditions and table from the reference book.
     *
     * @param pth0Provider provider for atmospheric pressure, temperature and humidity at reference altitude
     * @see #ModifiedSaastamoinenModel(PressureTemperatureHumidityProvider, String, DataProvidersManager)
     */
    public ModifiedSaastamoinenModel(final PressureTemperatureHumidityProvider pth0Provider) {
        this(pth0Provider, defaultDeltaR());
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor uses the {@link DataContext#getDefault()
     * default data context} if {@code deltaRFileName != null}.
     *
     * @param pth0Provider provider for atmospheric pressure, temperature and humidity at reference altitude
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @see #ModifiedSaastamoinenModel(PressureTemperatureHumidityProvider, String, DataProvidersManager)
     */
    @DefaultDataContext
    public ModifiedSaastamoinenModel(final PressureTemperatureHumidityProvider pth0Provider,
                                     final String deltaRFileName) {
        this(pth0Provider, deltaRFileName,
             DataContext.getDefault().getDataProvidersManager());
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor allows the user to specify the source of
     * of the δR file.
     *
     * @param pth0Provider provider for atmospheric pressure, temperature and humidity at reference altitude
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @param dataProvidersManager provides access to auxiliary data.
     */
    public ModifiedSaastamoinenModel(final PressureTemperatureHumidityProvider pth0Provider,
                                     final String deltaRFileName,
                                     final DataProvidersManager dataProvidersManager) {
        this(pth0Provider,
             deltaRFileName == null ?
                     defaultDeltaR() :
                     loadDeltaR(deltaRFileName, dataProvidersManager));
    }

    /** Create a new Saastamoinen model.
     *
     * @param pth0Provider provider for atmospheric pressure, temperature and humidity at reference altitude
     * @param deltaR δR correction term function
     */
    private ModifiedSaastamoinenModel(final PressureTemperatureHumidityProvider pth0Provider,
                                      final BilinearInterpolatingFunction deltaR) {
        this.pth0Provider          = pth0Provider;
        this.converter             = new HeightDependentPressureTemperatureHumidityConverter(WATER);
        this.deltaRFunction        = deltaR;
        this.lowElevationThreshold = DEFAULT_LOW_ELEVATION_THRESHOLD;
    }

    /** Create a new Saastamoinen model using a standard atmosphere model.
     *
     * <ul>
     * <li>altitude: 0m</li>
     * <li>temperature: 18 degree Celsius</li>
     * <li>pressure: 1013.25 mbar</li>
     * <li>humidity: 50%</li>
     * <li>@link {@link Wang1988 Wang 1988} model to compute water vapor pressure</li>
     * </ul>
     *
     * @return a Saastamoinen model with standard environmental values
     */
    public static ModifiedSaastamoinenModel getStandardModel() {
        final double altitude    = 0;
        final double pressure    = TroposphericModelUtils.HECTO_PASCAL.toSI(1013.25);
        final double temperature = 273.15 + 18;
        final double humidity    = 0.5;
        final PressureTemperatureHumidity pth = new PressureTemperatureHumidity(altitude,
                                                                                pressure,
                                                                                temperature,
                                                                                WATER.waterVaporPressure(pressure,
                                                                                                         temperature,
                                                                                                         humidity),
                                                                                Double.NaN,
                                                                                Double.NaN);
        final PressureTemperatureHumidityProvider pth0Provider = new ConstantPressureTemperatureHumidityProvider(pth);
        return new ModifiedSaastamoinenModel(pth0Provider);
    }

    /** Get provider for atmospheric pressure, temperature and humidity at reference altitude.
     * @return provider for atmospheric pressure, temperature and humidity at reference altitude
     */
    public PressureTemperatureHumidityProvider getPth0Provider() {
        return pth0Provider;
    }

    /** {@inheritDoc}
     * <p>
     * The Saastamoinen model is not defined for altitudes below 0.0. for continuity
     * reasons, we use the value for h = 0 when altitude is negative.
     * </p>
     * <p>
     * There are also numerical issues for elevation angles close to zero. For continuity reasons,
     * elevations lower than a threshold will use the value obtained
     * for the threshold itself.
     * </p>
     * @see #getLowElevationThreshold()
     * @see #setLowElevationThreshold(double)
     */
    @Override
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates,
                                       final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        // limit the height to model range
        final double fixedHeight = FastMath.min(FastMath.max(point.getAltitude(), X_VALUES_FOR_B[0]),
                                                X_VALUES_FOR_B[X_VALUES_FOR_B.length - 1]);

        final PressureTemperatureHumidity pth = converter.convert(weather, fixedHeight);

        // interpolate the b correction term
        final double B = B_FUNCTION.value(fixedHeight);

        // calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI -
                                      FastMath.max(trackingCoordinates.getElevation(), lowElevationThreshold));

        // get correction factor
        final double deltaR = getDeltaR(fixedHeight, z);

        // calculate the path delay in m
        // beware since version 12.1 pressures are in Pa and not in hPa, hence the scaling has changed
        final double invCos = 1.0 / FastMath.cos(z);
        final double tan    = FastMath.tan(z);
        final double zh     = L0 * pth.getPressure();
        final double zw     = L0 * (T_NUM / pth.getTemperature() + WET_OFFSET) * pth.getWaterVaporPressure();
        final double sh     = zh * invCos;
        final double sw     = (zw - L0 * B * tan * tan) * invCos + deltaR;
        return new TroposphericDelay(zh, zw, sh, sw);

    }

    /** {@inheritDoc}
     * <p>
     * The Saastamoinen model is not defined for altitudes below 0.0. for continuity
     * reasons, we use the value for h = 0 when altitude is negative.
     * </p>
     * <p>
     * There are also numerical issues for elevation angles close to zero. For continuity reasons,
     * elevations lower than a threshold will use the value obtained
     * for the threshold itself.
     * </p>
     * @see #getLowElevationThreshold()
     * @see #setLowElevationThreshold(double)
     */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTroposphericDelay<T> pathDelay(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                                   final FieldGeodeticPoint<T> point,
                                                                                   final FieldPressureTemperatureHumidity<T> weather,
                                                                                   final T[] parameters, final FieldAbsoluteDate<T> date) {

        // limit the height to model range
        final T fixedHeight = FastMath.min(FastMath.max(point.getAltitude(), X_VALUES_FOR_B[0]),
                                           X_VALUES_FOR_B[X_VALUES_FOR_B.length - 1]);

        final FieldPressureTemperatureHumidity<T> pth = converter.convert(weather, fixedHeight);

        final Field<T> field = date.getField();
        final T zero = field.getZero();

        // interpolate the b correction term
        final T B = B_FUNCTION.value(fixedHeight);

        // calculate the zenith angle from the elevation
        final T z = FastMath.abs(FastMath.max(trackingCoordinates.getElevation(),
                                              zero.newInstance(lowElevationThreshold)).negate().
                                 add(zero.getPi().multiply(0.5)));

        // get correction factor
        final T deltaR = getDeltaR(fixedHeight, z, field);

        // calculate the path delay in m
        // beware since version 12.1 pressures are in Pa and not in hPa, hence the scaling has changed
        final T invCos = FastMath.cos(z).reciprocal();
        final T tan    = FastMath.tan(z);
        final T zh     = pth.getPressure().multiply(L0);
        final T zw     = pth.getTemperature().reciprocal().multiply(T_NUM).add(WET_OFFSET).
                         multiply(pth.getWaterVaporPressure()).multiply(L0);
        final T sh     = zh.multiply(invCos);
        final T sw     = zw.subtract(B.multiply(tan).multiply(tan).multiply(L0)).multiply(invCos).add(deltaR);
        return new FieldTroposphericDelay<>(zh, zw, sh, sw);

    }

    /** Calculates the delta R correction term using linear interpolation.
     * @param height the height of the station in m
     * @param zenith the zenith angle of the satellite
     * @return the delta R correction term in m
     */
    private double getDeltaR(final double height, final double zenith) {
        // limit the height to a range of [0, 5000] m
        final double h = FastMath.min(FastMath.max(0, height), 5000);
        // limit the zenith angle to 90 degree
        // Note: the function is symmetric for negative zenith angles
        final double z = FastMath.min(Math.abs(zenith), 0.5 * FastMath.PI);
        return deltaRFunction.value(h, z);
    }

    /** Calculates the delta R correction term using linear interpolation.
     * @param <T> type of the elements
     * @param height the height of the station in m
     * @param zenith the zenith angle of the satellite
     * @param field field used by default
     * @return the delta R correction term in m
     */
    private  <T extends CalculusFieldElement<T>> T getDeltaR(final T height, final T zenith,
                                                         final Field<T> field) {
        final T zero = field.getZero();
        // limit the height to a range of [0, 5000] m
        final T h = FastMath.min(FastMath.max(zero, height), zero.add(5000));
        // limit the zenith angle to 90 degree
        // Note: the function is symmetric for negative zenith angles
        final T z = FastMath.min(zenith.abs(), zero.getPi().multiply(0.5));
        return deltaRFunction.value(h, z);
    }

    /** Load δR function.
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table
     * @param dataProvidersManager provides access to auxiliary data.
     * @return δR function
     */
    private static BilinearInterpolatingFunction loadDeltaR(
            final String deltaRFileName,
            final DataProvidersManager dataProvidersManager) {

        // read the δR interpolation function from the config file
        final InterpolationTableLoader loader = new InterpolationTableLoader();
        dataProvidersManager.feed(deltaRFileName, loader);
        if (!loader.stillAcceptsData()) {
            final double[] elevations = loader.getOrdinateGrid();
            for (int i = 0; i < elevations.length; ++i) {
                elevations[i] = FastMath.toRadians(elevations[i]);
            }
            return new BilinearInterpolatingFunction(loader.getAbscissaGrid(), elevations,
                                                     loader.getValuesSamples());
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE,
                                  SECOND_DELTA_R_PATTERN.
                                  matcher(FIRST_DELTA_R_PATTERN.matcher(deltaRFileName).replaceAll("")).
                                  replaceAll(""));
    }

    /** Create the default δR function.
     * @return δR function
     */
    private static BilinearInterpolatingFunction defaultDeltaR() {

        // the correction table in the referenced book only contains values for an angle of 60 - 80
        // degree, thus for 0 degree, the correction term is assumed to be 0, for degrees > 80 it
        // is assumed to be the same value as for 80.

        // the height in m
        final double[] xValForR = {
            0, 500, 1000, 1500, 2000, 3000, 4000, 5000
        };

        // the zenith angle
        final double[] yValForR = {
            FastMath.toRadians( 0.00), FastMath.toRadians(60.00), FastMath.toRadians(66.00), FastMath.toRadians(70.00),
            FastMath.toRadians(73.00), FastMath.toRadians(75.00), FastMath.toRadians(76.00), FastMath.toRadians(77.00),
            FastMath.toRadians(78.00), FastMath.toRadians(78.50), FastMath.toRadians(79.00), FastMath.toRadians(79.50),
            FastMath.toRadians(79.75), FastMath.toRadians(80.00), FastMath.toRadians(90.00)
        };

        final double[][] fval = new double[][] {
            {
                0.000, 0.003, 0.006, 0.012, 0.020, 0.031, 0.039, 0.050, 0.065, 0.075, 0.087, 0.102, 0.111, 0.121, 0.121
            }, {
                0.000, 0.003, 0.006, 0.011, 0.018, 0.028, 0.035, 0.045, 0.059, 0.068, 0.079, 0.093, 0.101, 0.110, 0.110
            }, {
                0.000, 0.002, 0.005, 0.010, 0.017, 0.025, 0.032, 0.041, 0.054, 0.062, 0.072, 0.085, 0.092, 0.100, 0.100
            }, {
                0.000, 0.002, 0.005, 0.009, 0.015, 0.023, 0.029, 0.037, 0.049, 0.056, 0.065, 0.077, 0.083, 0.091, 0.091
            }, {
                0.000, 0.002, 0.004, 0.008, 0.013, 0.021, 0.026, 0.033, 0.044, 0.051, 0.059, 0.070, 0.076, 0.083, 0.083
            }, {
                0.000, 0.002, 0.003, 0.006, 0.011, 0.017, 0.021, 0.027, 0.036, 0.042, 0.049, 0.058, 0.063, 0.068, 0.068
            }, {
                0.000, 0.001, 0.003, 0.005, 0.009, 0.014, 0.017, 0.022, 0.030, 0.034, 0.040, 0.047, 0.052, 0.056, 0.056
            }, {
                0.000, 0.001, 0.002, 0.004, 0.007, 0.011, 0.014, 0.018, 0.024, 0.028, 0.033, 0.039, 0.043, 0.047, 0.047
            }
        };

        // the actual delta R is interpolated using a a bilinear interpolator
        return new BilinearInterpolatingFunction(xValForR, yValForR, fval);

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Get the low elevation threshold value for path delay computation.
     * @return low elevation threshold, in rad.
     * @see #pathDelay(TrackingCoordinates, GeodeticPoint, PressureTemperatureHumidity, double[], AbsoluteDate)
     * @see #pathDelay(FieldTrackingCoordinates, FieldGeodeticPoint, FieldPressureTemperatureHumidity, CalculusFieldElement[], FieldAbsoluteDate)
     * @since 10.2
     */
    public double getLowElevationThreshold() {
        return lowElevationThreshold;
    }

    /** Set the low elevation threshold value for path delay computation.
     * @param lowElevationThreshold The new value for the threshold [rad]
     * @see #pathDelay(TrackingCoordinates, GeodeticPoint, PressureTemperatureHumidity, double[], AbsoluteDate)
     * @see #pathDelay(FieldTrackingCoordinates, FieldGeodeticPoint, FieldPressureTemperatureHumidity, CalculusFieldElement[], FieldAbsoluteDate)
     * @since 10.2
     */
    public void setLowElevationThreshold(final double lowElevationThreshold) {
        this.lowElevationThreshold = lowElevationThreshold;
    }
}

