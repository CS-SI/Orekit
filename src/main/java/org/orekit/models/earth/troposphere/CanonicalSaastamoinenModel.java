/* Copyright 2002-2024 Thales Alenia Space
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
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.HeightDependentPressureTemperatureHumidityConverter;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

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
public class CanonicalSaastamoinenModel implements TroposphericModel {

    /** Default lowest acceptable elevation angle [rad]. */
    public static final double DEFAULT_LOW_ELEVATION_THRESHOLD = 0.05;

    /** Base delay coefficient. */
    private static final double L0 = 2.2768e-5;

    /** Temperature numerator. */
    private static final double T_NUM = 1255;

    /** Wet offset. */
    private static final double WET_OFFSET = 0.05;

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

    static {
        B_FUNCTION = new LinearInterpolator().interpolate(X_VALUES_FOR_B, Y_VALUES_FOR_B);
    }

    /** Lowest acceptable elevation angle [rad]. */
    private double lowElevationThreshold;

    /**
     * Create a new Saastamoinen model for the troposphere using the given environmental
     * conditions and table from the reference book.
     *
     * @see HeightDependentPressureTemperatureHumidityConverter
     */
    public CanonicalSaastamoinenModel() {
        this.lowElevationThreshold = DEFAULT_LOW_ELEVATION_THRESHOLD;
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
    public TroposphericDelay pathDelay(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                       final PressureTemperatureHumidity weather,
                                       final double[] parameters, final AbsoluteDate date) {

        // there are no data in the model for negative altitudes and altitude bigger than 6000 m
        // limit the height to a range of [0, 5000] m
        final double fixedHeight = FastMath.min(FastMath.max(point.getAltitude(), X_VALUES_FOR_B[0]),
                                                X_VALUES_FOR_B[X_VALUES_FOR_B.length - 1]);

        // interpolate the b correction term
        final double B = B_FUNCTION.value(fixedHeight);

        // calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - FastMath.max(trackingCoordinates.getElevation(),
                                                                       lowElevationThreshold));

        // calculate the path delay
        final double invCos = 1.0 / FastMath.cos(z);
        final double tan    = FastMath.tan(z);
        final double zh     = L0 * weather.getPressure();
        final double zw     = L0 * (T_NUM / weather.getTemperature() + WET_OFFSET) * weather.getWaterVaporPressure();
        final double sh     = zh * invCos;
        final double sw     = (zw - L0 * B * tan * tan) * invCos;
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

        final Field<T> field = date.getField();
        final T zero = field.getZero();

        // there are no data in the model for negative altitudes and altitude bigger than 5000 m
        // limit the height to a range of [0, 5000] m
        final T fixedHeight = FastMath.min(FastMath.max(point.getAltitude(), X_VALUES_FOR_B[0]),
                                           X_VALUES_FOR_B[X_VALUES_FOR_B.length - 1]);

        // interpolate the b correction term
        final T B = B_FUNCTION.value(fixedHeight);

        // calculate the zenith angle from the elevation
        final T z = FastMath.abs(zero.getPi().multiply(0.5).
                                 subtract(FastMath.max(trackingCoordinates.getElevation(), lowElevationThreshold)));

        // calculate the path delay in m
        final T invCos = FastMath.cos(z).reciprocal();
        final T tan    = FastMath.tan(z);
        final T zh     = weather.getPressure().multiply(L0);
        final T zw     = weather.getTemperature().reciprocal().multiply(T_NUM).add(WET_OFFSET).
                         multiply(weather.getWaterVaporPressure()).multiply(L0);
        final T sh     = zh.multiply(invCos);
        final T sw     = zw.subtract(B.multiply(tan).multiply(tan).multiply(L0)).multiply(invCos);
        return new FieldTroposphericDelay<>(zh, zw, sh, sw);

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
     */
    public double getLowElevationThreshold() {
        return lowElevationThreshold;
    }

    /** Set the low elevation threshold value for path delay computation.
     * @param lowElevationThreshold The new value for the threshold [rad]
     * @see #pathDelay(TrackingCoordinates, GeodeticPoint, PressureTemperatureHumidity, double[], AbsoluteDate)
     * @see #pathDelay(FieldTrackingCoordinates, FieldGeodeticPoint, FieldPressureTemperatureHumidity, CalculusFieldElement[], FieldAbsoluteDate)
     */
    public void setLowElevationThreshold(final double lowElevationThreshold) {
        this.lowElevationThreshold = lowElevationThreshold;
    }

}

