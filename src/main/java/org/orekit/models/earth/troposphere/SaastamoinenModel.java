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

/** The modified Saastamoinen model. Estimates the path delay imposed to
 * electro-magnetic signals by the troposphere according to the formula:
 * <pre>
 * δ = 2.277e-3 / cos z * (P + (1255 / T + 0.05) * e - B * tan²
 * z) + δR
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
 */
public class SaastamoinenModel implements DiscreteTroposphericModel {

    /** Default file name for δR correction term table. */
    public static final String DELTA_R_FILE_NAME = "^saastamoinen-correction\\.txt$";

    /** Default lowest acceptable elevation angle [rad]. */
    public static final double DEFAULT_LOW_ELEVATION_THRESHOLD = 0.05;

    /** First pattern for δR correction term table. */
    private static final Pattern FIRST_DELTA_R_PATTERN = Pattern.compile("^\\^");

    /** Second pattern for δR correction term table. */
    private static final Pattern SECOND_DELTA_R_PATTERN = Pattern.compile("\\$$");

    /** X values for the B function. */
    private static final double[] X_VALUES_FOR_B = {
        0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0
    };

    /** E values for the B function. */
    private static final double[] Y_VALUES_FOR_B = {
        1.156, 1.079, 1.006, 0.938, 0.874, 0.813, 0.757, 0.654, 0.563
    };

    /** Coefficients for the partial pressure of water vapor polynomial. */
    private static final double[] E_COEFFICIENTS = {
        -37.2465, 0.213166, -0.000256908
    };

    /** Interpolation function for the B correction term. */
    private final PolynomialSplineFunction bFunction;

    /** Polynomial function for the e term. */
    private final PolynomialFunction eFunction;

    /** Interpolation function for the delta R correction term. */
    private final BilinearInterpolatingFunction deltaRFunction;

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
     * @see #SaastamoinenModel(double, double, double, String, DataProvidersManager)
     * @since 10.1
     */
    public SaastamoinenModel(final double t0, final double p0, final double r0) {
        this(t0, p0, r0, defaultDeltaR());
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor uses the {@link DataContext#getDefault()
     * default data context} if {@code deltaRFileName != null}.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @since 7.1
     * @see #SaastamoinenModel(double, double, double, String, DataProvidersManager)
     */
    @DefaultDataContext
    public SaastamoinenModel(final double t0, final double p0, final double r0,
                             final String deltaRFileName) {
        this(t0, p0, r0, deltaRFileName,
                DataContext.getDefault().getDataProvidersManager());
    }

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions. This constructor allows the user to specify the source of
     * of the δR file.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @param dataProvidersManager provides access to auxiliary data.
     * @since 10.1
     */
    public SaastamoinenModel(final double t0,
                             final double p0,
                             final double r0,
                             final String deltaRFileName,
                             final DataProvidersManager dataProvidersManager) {
        this(t0, p0, r0,
             deltaRFileName == null ?
                     defaultDeltaR() :
                     loadDeltaR(deltaRFileName, dataProvidersManager));
    }

    /** Create a new Saastamoinen model.
     *
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -> 0.5)
     * @param deltaR δR correction term function
     * @since 7.1
     */
    private SaastamoinenModel(final double t0, final double p0, final double r0,
                              final BilinearInterpolatingFunction deltaR) {
        checkParameterRangeInclusive("humidity", r0, 0.0, 1.0);
        this.t0             = t0;
        this.p0             = p0;
        this.r0             = r0;
        this.bFunction      = new LinearInterpolator().interpolate(X_VALUES_FOR_B, Y_VALUES_FOR_B);
        this.eFunction      = new PolynomialFunction(E_COEFFICIENTS);
        this.deltaRFunction = deltaR;
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
    public static SaastamoinenModel getStandardModel() {
        return new SaastamoinenModel(273.16 + 18, 1013.25, 0.5);
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

        // get correction factor
        final double deltaR = getDeltaR(fixedHeight, z);

        // calculate the path delay in m
        final double tan = FastMath.tan(z);
        final double delta = 2.277e-3 / FastMath.cos(z) *
                             (P + (1255d / T + 5e-2) * e - B * tan * tan) + deltaR;

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

        // get correction factor
        final T deltaR = getDeltaR(fixedHeight, z, field);

        // calculate the path delay in m
        final T tan = FastMath.tan(z);
        final T delta = FastMath.cos(z).divide(2.277e-3).reciprocal().
                        multiply(P.add(T.divide(1255d).reciprocal().add(5e-2).multiply(e)).subtract(B.multiply(tan).multiply(tan))).add(deltaR);

        return delta;
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
     * @see #pathDelay(double, GeodeticPoint, double[], AbsoluteDate)
     * @see #pathDelay(CalculusFieldElement, FieldGeodeticPoint, CalculusFieldElement[], FieldAbsoluteDate)
     * @since 10.2
     */
    public double getLowElevationThreshold() {
        return lowElevationThreshold;
    }

    /** Set the low elevation threshold value for path delay computation.
     * @param lowElevationThreshold The new value for the threshold [rad]
     * @see #pathDelay(double, GeodeticPoint, double[], AbsoluteDate)
     * @see #pathDelay(CalculusFieldElement, FieldGeodeticPoint, CalculusFieldElement[], FieldAbsoluteDate)
     * @since 10.2
     */
    public void setLowElevationThreshold(final double lowElevationThreshold) {
        this.lowElevationThreshold = lowElevationThreshold;
    }
}

