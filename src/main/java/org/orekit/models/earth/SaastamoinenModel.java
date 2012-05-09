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
package org.orekit.models.earth;

import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.util.FastMath;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.utils.Constants;

import org.orekit.utils.InterpolationTableLoader;

/** The modified Saastamoinen model. Estimates the path delay imposed to
 * electro-magnetic signals by the troposphere according to the formula:
 * <pre>
 * &delta; = 2.277e-3 / cos z * (P + (1255 / T + 0.05) * e - B * tan<sup>2</sup>
 * z) + &delta;R
 * </pre>
 * with the following input data provided to the model:
 * <ul>
 * <li>z: zenith angle</li>
 * <li>P: atmospheric pressure</li>
 * <li>T: temperature</li>
 * <li>e: partial pressure of water vapour</li>
 * <li>B, &delta;R: correction terms</li>
 * </ul>
 * <p>
 * The model supports custom &delta;R correction terms to be read from a
 * configuration file (saastamoinen-correction.txt) via the
 * {@link DataProvidersManager}.
 * </p>
 * @author Thomas Neidhart
 * @see Guochang Xu, GPS - Theory, Algorithms and Applications, Springer, 2007
 */
public class SaastamoinenModel implements TroposphericDelayModel {

    /** Serializable UID. */
    private static final long serialVersionUID = -5702086204232977550L;

    /** The temperature at the station [K]. */
    private double t0;

    /** The atmospheric pressure [mbar]. */
    private double p0;

    /** The humidity [percent]. */
    private double r0;

    /** Create a new Saastamoinen model for the troposphere using the given
     * environmental conditions.
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [percent] (50% -> 0.5)
     */
    public SaastamoinenModel(final double t0, final double p0, final double r0) {
        this.t0 = t0;
        this.p0 = p0;
        this.r0 = r0;
    }

    /** Create a new Saastamoinen model using a standard atmosphere model.
     * <p>
     * <ul>
     * <li>temperature: 18 degree Celsius
     * <li>pressure: 1013.25 mbar
     * <li>humidity: 50%
     * </ul>
     * </p>
     * @return a Saastamoinen model with standard environmental values
     */
    public static SaastamoinenModel getStandardModel() {
        return new SaastamoinenModel(273.16 + 18, 1013.25, 0.5);
    }

    /** {@inheritDoc} */
    public double calculatePathDelay(final double elevation, final double height) {
        // the corrected temperature using a temperature gradient of -6.5 K/km
        final double T = t0 - 6.5e-3 * height;
        // the corrected pressure
        final double P = p0 * FastMath.pow(1.0 - 2.26e-5 * height, 5.225);
        // the corrected humidity
        final double R = r0 * FastMath.exp(-6.396e-4 * height);

        // interpolate the b correction term
        final double B = Functions.INSTANCE.b.value(height / 1e3);
        // calculate e
        final double e = R * FastMath.exp(Functions.INSTANCE.e.value(T));

        // calculate the zenith angle from the elevation and convert to radians
        final double zInDegree = FastMath.abs(90.0 - elevation);
        final double z = FastMath.toRadians(zInDegree);

        // get correction factor
        final double deltaR = getDeltaR(height, zInDegree);

        // calculate the path delay in m
        final double tan = FastMath.tan(z);
        final double delta = 2.277e-3 / Math.cos(z) *
                             (P + (1255d / T + 5e-2) * e - B * tan * tan) + deltaR;

        return delta;
    }

    /** {@inheritDoc} */
    public double calculateSignalDelay(final double elevation, final double height) {
        return calculatePathDelay(elevation, height) / Constants.SPEED_OF_LIGHT;
    }

    /** Calculates the delta R correction term using linear interpolation.
     * @param height the height of the station in m
     * @param zenith the zenith angle of the satellite in degrees
     * @return the delta R correction term in m
     */
    private double getDeltaR(final double height, final double zenith) {
        // limit the height to a range of [0, 5000] m
        final double h = FastMath.min(Math.max(0, height), 5000);
        // limit the zenith angle to 90 degree
        // Note: the function is symmetric for negative zenith angles
        final double z = FastMath.min(Math.abs(zenith), 90);
        return Functions.INSTANCE.deltaR.value(h, z);
    }

    /** Contains several functions used by the Saastamoinen model to calculate
     * the path delay. The functions are static and thus accessed via a static
     * instance of this class. The &delta;R correction terms can be optionally
     * loaded from a configuration file, otherwise default values are used.
     */
    private static class Functions {

        /** The singleton instance containing the functions. */
        private static final Functions INSTANCE = new Functions();

        /** Interpolation function for the B correction term. */
        private final UnivariateFunction b;

        /** Polynomial function for the e term. */
        private final PolynomialFunction e;

        /** Interpolation function for the delta R correction term. */
        private final BivariateFunction deltaR;

        /** Initialize the functions. */
        private Functions() {
            final double xValForB[] = {0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0};
            final double yValForB[] = {1.156, 1.079, 1.006, 0.938, 0.874, 0.813, 0.757, 0.654, 0.563};

            b = new LinearInterpolator().interpolate(xValForB, yValForB);

            // a function to estimate the partial pressure of water vapour
            e = new PolynomialFunction(new double[] {-37.2465, 0.213166, -0.000256908});

            // read the delta R interpolation function from the config file
            final InterpolationTableLoader loader = new InterpolationTableLoader();
            BivariateFunction func = null;
            try {
                DataProvidersManager.getInstance().feed("^saastamoinen-correction\\.txt$", loader);
                if (!loader.stillAcceptsData()) {
                    func = loader.getInterpolationFunction();
                }
            } catch (OrekitException ex) {
                // config file could not be loaded, use the default values instead
            }

            if (func != null) {
                deltaR = func;
            } else {
                // use default values if the file could not be read

                // the correction table in the referenced book only contains values for an angle of 60 - 80
                // degree, thus for 0 degree, the correction term is assumed to be 0, for degrees > 80 it
                // is assumed to be the same value as for 80.

                // the height in m
                final double xValForR[] = {0, 500, 1000, 1500, 2000, 3000, 4000, 5000};
                // the zenith angle in degrees
                final double yValForR[] = {0.0, 60.0, 66.0, 70.0, 73.0, 75.0, 76.0, 77.0,
                                           78.0, 78.50, 79.0, 79.50, 79.75, 80.0, 90.0};

                final double[][] fval = new double[][] {
                    {0.000, 0.003, 0.006, 0.012, 0.020, 0.031, 0.039, 0.050, 0.065,
                     0.075, 0.087, 0.102, 0.111, 0.121, 0.121},
                    {0.000, 0.003, 0.006, 0.011, 0.018, 0.028, 0.035, 0.045, 0.059,
                     0.068, 0.079, 0.093, 0.101, 0.110, 0.110},
                    {0.000, 0.002, 0.005, 0.010, 0.017, 0.025, 0.032, 0.041, 0.054,
                     0.062, 0.072, 0.085, 0.092, 0.100, 0.100},
                    {0.000, 0.002, 0.005, 0.009, 0.015, 0.023, 0.029, 0.037, 0.049,
                     0.056, 0.065, 0.077, 0.083, 0.091, 0.091},
                    {0.000, 0.002, 0.004, 0.008, 0.013, 0.021, 0.026, 0.033, 0.044,
                     0.051, 0.059, 0.070, 0.076, 0.083, 0.083},
                    {0.000, 0.002, 0.003, 0.006, 0.011, 0.017, 0.021, 0.027, 0.036,
                     0.042, 0.049, 0.058, 0.063, 0.068, 0.068},
                    {0.000, 0.001, 0.003, 0.005, 0.009, 0.014, 0.017, 0.022, 0.030,
                     0.034, 0.040, 0.047, 0.052, 0.056, 0.056},
                    {0.000, 0.001, 0.002, 0.004, 0.007, 0.011, 0.014, 0.018, 0.024,
                     0.028, 0.033, 0.039, 0.043, 0.047, 0.047} };

                // the actual delta R is interpolated using a a bi-cubic spline interpolator
                deltaR = new BicubicSplineInterpolator().interpolate(xValForR, yValForR, fval);
            }
        }
    }
}
