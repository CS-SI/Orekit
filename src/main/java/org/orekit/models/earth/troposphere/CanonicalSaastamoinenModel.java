/* Copyright 2023 Thales Alenia Space
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

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.interpolation.LinearInterpolator;
import org.hipparchus.analysis.polynomials.PolynomialSplineFunction;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.HeightDependentPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.NbsNrcSteamTable;
import org.orekit.models.earth.weather.water.WaterVaporPressureProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** The canonical Saastamoinen model.
 * <p>
 * Estimates the path delay imposed to
 * electro-magnetic signals by the troposphere according to the formula:
 * \[
 * \delta = \frac{0.002277}{\cos z (1 - 0.00266\cos 2\varphi - 0.00028 h})}
 * \left[P+(\frac{1255}{T}+0.005)e - B(h) \tan^2 z\right]
 * \]
 * with the following input data provided to the model:
 * <ul>
 * <li>z: zenith angle</li>
 * <li>P: atmospheric pressure</li>
 * <li>T: temperature</li>
 * <li>e: partial pressure of water vapor</li>
 * </ul>
 * @author Luc Maisonobe
 * @see "J Saastamoinen, Atmospheric Correction for the Troposphere and Stratosphere in Radio
 * Ranging of Satellites"
 * @since 12.1
 */
public class CanonicalSaastamoinenModel implements DiscreteTroposphericModel {

    /** Default lowest acceptable elevation angle [rad]. */
    public static final double DEFAULT_LOW_ELEVATION_THRESHOLD = 0.05;

    /** X values for the B function (table 1 in reference paper). */
    private static final double[] X_VALUES_FOR_B = {
        0.0, 200.0, 400.0, 600.0, 800.0, 1000.0, 1500.0, 2000.0, 2500.0, 3000.0, 4000.0, 5000.0, 6000.0
    };

    /** Y values for the B function (table 1 in reference paper).
     * <p>
     * The values have been scaled up by a factor 100.0 due to conversion from hPa to Pa.
     * </p>
     */
    private static final double[] Y_VALUES_FOR_B = {
        116.0, 113.0, 110.0, 107.0, 104.0, 101.0, 94.0, 88.0, 82.0, 76.0, 66.0, 57.0, 49.0
    };

    /** Interpolation function for the B correction term. */
    private static final PolynomialSplineFunction B_FUNCTION;

    /** Provider for pressure, temperature and humidity. */
    private final PressureTemperatureHumidityProvider pthProvider;

    static {
        B_FUNCTION = new LinearInterpolator().interpolate(X_VALUES_FOR_B, Y_VALUES_FOR_B);
    }

    /** Lowest acceptable elevation angle [rad]. */
    private double lowElevationThreshold;

    /**
     * Create a new Saastamoinen model for the troposphere using the given environmental
     * conditions and table from the reference book.
     *
     * @param pthProvider provider for pressure, temperature and humidity
     * @see HeightDependentPressureTemperatureHumidityProvider
     */
    public CanonicalSaastamoinenModel(final PressureTemperatureHumidityProvider pthProvider) {
        this.pthProvider           = pthProvider;
        this.lowElevationThreshold = DEFAULT_LOW_ELEVATION_THRESHOLD;
    }

    /** Create a new Saastamoinen model using a standard atmosphere model.
     *
     * <ul>
     * <li>temperature: 18 degree Celsius
     * <li>pressure: 1013.25 mbar
     * <li>humidity: 50%
     * </ul>
     *
     * @return a Saastamoinen model with standard environmental values
     */
    public static CanonicalSaastamoinenModel getStandardModel() {

        // build standard meteorological data
        final double pressure           = TropoUnit.HECTO_PASCAL.toSI(1013.25);
        final double temperature        = 273.15 + 18;
        final double relativeHumidity   = 0.5;
        final WaterVaporPressureProvider waterPressureProvider = new NbsNrcSteamTable();
        final double waterVaporPressure = waterPressureProvider.waterVaporPressure(pressure,
                                                                                   temperature,
                                                                                   relativeHumidity);
        final PressureTemperatureHumidity pth = new PressureTemperatureHumidity(pressure,
                                                                                temperature,
                                                                                waterVaporPressure);
        final PressureTemperatureHumidityProvider pth0Provider =
                        new ConstantPressureTemperatureHumidityProvider(pth);
        final PressureTemperatureHumidityProvider pthProvider =
                        new HeightDependentPressureTemperatureHumidityProvider(0.0, 5000.0, 0.0, pth0Provider, waterPressureProvider);
        return new CanonicalSaastamoinenModel(pthProvider);

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
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {

        final PressureTemperatureHumidity pth = pthProvider.getWeatherParamerers(point, date);

        // there are no data in the model for negative altitudes and altitude bigger than 6000 m
        // limit the height to a range of [0, 5000] m
        final double fixedHeight = FastMath.min(FastMath.max(point.getAltitude(), X_VALUES_FOR_B[0]),
                                                X_VALUES_FOR_B[X_VALUES_FOR_B.length - 1]);

        // interpolate the b correction term
        final double B = B_FUNCTION.value(fixedHeight);

        // calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - FastMath.max(elevation, lowElevationThreshold));

        // calculate the path delay in m
        final double tan = FastMath.tan(z);
        final double delta = 2.277e-5 / FastMath.cos(z) *
                             (pth.getPressure() +
                              (1255.0 / pth.getTemperature() + 0.05) * pth.getWaterVaporPressure() - B * tan * tan);

        return delta;
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
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {

        final Field<T> field = date.getField();
        final T zero = field.getZero();

        final FieldPressureTemperatureHumidity<T> pth = pthProvider.getWeatherParamerers(point, date);

        // there are no data in the model for negative altitudes and altitude bigger than 5000 m
        // limit the height to a range of [0, 5000] m
        final T fixedHeight = FastMath.min(FastMath.max(point.getAltitude(), X_VALUES_FOR_B[0]),
                                           X_VALUES_FOR_B[X_VALUES_FOR_B.length - 1]);

        // interpolate the b correction term
        final T B = B_FUNCTION.value(fixedHeight);

        // calculate the zenith angle from the elevation
        final T z = FastMath.abs(zero.getPi().multiply(0.5).
                                 subtract(FastMath.max(elevation, lowElevationThreshold)));

        // calculate the path delay in m
        final T tan = FastMath.tan(z);
        final T delta = FastMath.cos(z).divide(2.277e-5).reciprocal().
                        multiply(pth.getPressure().
                                 add(pth.getTemperature().reciprocal().multiply(1255.0).add(0.05).
                                     multiply(pth.getWaterVaporPressure())).
                                 subtract(B.multiply(tan).multiply(tan)));

        return delta;

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Get the low elevation threshold value for path delay computation.
     * @return low elevation threshold, in rad.
     * @see #pathDelay(double, GeodeticPoint, double[], AbsoluteDate)
     * @see #pathDelay(CalculusFieldElement, FieldGeodeticPoint, CalculusFieldElement[], FieldAbsoluteDate)
     */
    public double getLowElevationThreshold() {
        return lowElevationThreshold;
    }

    /** Set the low elevation threshold value for path delay computation.
     * @param lowElevationThreshold The new value for the threshold [rad]
     * @see #pathDelay(double, GeodeticPoint, double[], AbsoluteDate)
     * @see #pathDelay(CalculusFieldElement, FieldGeodeticPoint, CalculusFieldElement[], FieldAbsoluteDate)
     */
    public void setLowElevationThreshold(final double lowElevationThreshold) {
        this.lowElevationThreshold = lowElevationThreshold;
    }

}

