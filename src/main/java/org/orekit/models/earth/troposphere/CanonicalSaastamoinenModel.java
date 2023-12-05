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
import java.util.regex.Pattern;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.analysis.interpolation.LinearInterpolator;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.analysis.polynomials.PolynomialSplineFunction;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.InterpolationTableLoader;
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
        0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0, 6.0
    };

    /** E values for the B function (table 1 in reference paper). */
    private static final double[] Y_VALUES_FOR_B = {
        1.16, 1.13, 1.10, 1.07, 1.04, 1.01, 0.94, 0.88, 0.82, 0.76, 0.66, 0.57, 0.49
    };

    /** Interpolation function for the B correction term. */
    private final PolynomialSplineFunction bFunction;

    /** The temperature at the station [K]. */
    private double t0;

    /** The atmospheric pressure [mbar]. */
    private double p0;

    /** The humidity [percent]. */
    private double r0;

    /** Lowest acceptable elevation angle [rad]. */
    private double lowElevationThreshold;

    /**
     * Create a new Saastamoinen model for the troposphere using the given environmental
     * conditions and table from the reference book.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     */
    public CanonicalSaastamoinenModel(final double t0, final double p0, final double r0) {
        checkParameterRangeInclusive("humidity", r0, 0.0, 1.0);
        this.t0             = t0;
        this.p0             = p0;
        this.r0             = r0;
        this.bFunction      = new LinearInterpolator().interpolate(X_VALUES_FOR_B, Y_VALUES_FOR_B);
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
        return new CanonicalSaastamoinenModel(273.16 + 18, 1013.25, 0.5);
    }

    /** Check if the given parameter is within an acceptable range.
     * The bounds are inclusive: an exception is raised when either of those conditions are met:
     * <ul>
     *     <li>The parameter is strictly greater than upperBound</li>
     *     <li>The parameter is strictly lower than lowerBound</li>
     * </ul>
     * <p>
     * In either of these cases, an OrekitException is raised.
     * </p>
     * @param parameterName name of the parameter
     * @param parameter value of the parameter
     * @param lowerBound lower bound of the acceptable range (inclusive)
     * @param upperBound upper bound of the acceptable range (inclusive)
     */
    private void checkParameterRangeInclusive(final String parameterName, final double parameter,
                                              final double lowerBound, final double upperBound) {
        if (parameter < lowerBound || parameter > upperBound) {
            throw new OrekitException(OrekitMessages.INVALID_PARAMETER_RANGE, parameterName,
                                      parameter, lowerBound, upperBound);
        }
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

        // there are no data in the model for negative altitudes and altitude bigger than 5000 m
        // limit the height to a range of [0, 5000] m
        final double fixedHeight = FastMath.min(FastMath.max(0, point.getAltitude()), 5000);

        // the corrected temperature using a temperature gradient of -6.5 K/km
        final double T = t0 - 6.5e-3 * fixedHeight;
        // the corrected pressure
        final double P = p0 * FastMath.pow(1.0 - 2.26e-5 * fixedHeight, 5.225);
        // the corrected humidity
        final double R = r0 * FastMath.exp(-6.396e-4 * fixedHeight);

        // interpolate the b correction term
        final double B = bFunction.value(fixedHeight / 1e3);
        // calculate e
        final double e = R * FastMath.exp(eFunction.value(T));

        // calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - FastMath.max(elevation, lowElevationThreshold));

        // calculate the path delay in m
        final double tan = FastMath.tan(z);
        final double delta = 2.277e-3 / FastMath.cos(z) *
                             (P + (1255d / T + 5e-2) * e - B * tan * tan);

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
        // there are no data in the model for negative altitudes and altitude bigger than 5000 m
        // limit the height to a range of [0, 5000] m
        final T fixedHeight = FastMath.min(FastMath.max(zero, point.getAltitude()), zero.add(5000));

        // the corrected temperature using a temperature gradient of -6.5 K/km
        final T T = fixedHeight.multiply(6.5e-3).negate().add(t0);
        // the corrected pressure
        final T P = fixedHeight.multiply(2.26e-5).negate().add(1.0).pow(5.225).multiply(p0);
        // the corrected humidity
        final T R = FastMath.exp(fixedHeight.multiply(-6.396e-4)).multiply(r0);

        // interpolate the b correction term
        final T B = bFunction.value(fixedHeight.divide(1e3));
        // calculate e
        final T e = R.multiply(FastMath.exp(eFunction.value(T)));

        // calculate the zenith angle from the elevation
        final T z = FastMath.abs(FastMath.max(elevation, zero.add(lowElevationThreshold)).negate().add(zero.getPi().multiply(0.5)));

        // calculate the path delay in m
        final T tan = FastMath.tan(z);
        final T delta = FastMath.cos(z).divide(2.277e-3).reciprocal().
                        multiply(P.add(T.divide(1255d).reciprocal().add(5e-2).multiply(e)).subtract(B.multiply(tan).multiply(tan)));

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

