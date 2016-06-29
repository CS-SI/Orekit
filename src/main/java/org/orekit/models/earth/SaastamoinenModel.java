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

import java.io.Serializable;
import java.util.Arrays;

import org.hipparchus.analysis.BivariateFunction;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.interpolation.LinearInterpolator;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.InterpolationTableLoader;

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
public class SaastamoinenModel implements TroposphericModel {

    /** Default file name for δR correction term table. */
    public static final String DELTA_R_FILE_NAME = "^saastamoinen-correction\\.txt$";

    /** Serializable UID. */
    private static final long serialVersionUID = 20160126L;

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
    private final transient UnivariateFunction bFunction;

    /** Polynomial function for the e term. */
    private final transient PolynomialFunction eFunction;

    /** Interpolation function for the delta R correction term. */
    private final transient BilinearInterpolatingFunction deltaRFunction;

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
     * @param r0 the humidity at the station [fraction] (50% -&gt; 0.5)
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table (typically {@link #DELTA_R_FILE_NAME}), if null
     * default values from the reference book are used
     * @exception OrekitException if δR correction term table cannot be loaded
     * @since 7.1
     */
    public SaastamoinenModel(final double t0, final double p0, final double r0,
                             final String deltaRFileName)
        throws OrekitException {
        this(t0, p0, r0,
             deltaRFileName == null ? defaultDeltaR() : loadDeltaR(deltaRFileName));
    }

    /** Create a new Saastamoinen model.
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param r0 the humidity at the station [fraction] (50% -> 0.5)
     * @param deltaR δR correction term function
     * @since 7.1
     */
    private SaastamoinenModel(final double t0, final double p0, final double r0,
                              final BilinearInterpolatingFunction deltaR) {
        this.t0             = t0;
        this.p0             = p0;
        this.r0             = r0;
        this.bFunction      = new LinearInterpolator().interpolate(X_VALUES_FOR_B, Y_VALUES_FOR_B);
        this.eFunction      = new PolynomialFunction(E_COEFFICIENTS);
        this.deltaRFunction = deltaR;
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
     * @exception OrekitException if δR correction term table cannot be loaded
     */
    public static SaastamoinenModel getStandardModel()
        throws OrekitException {
        return new SaastamoinenModel(273.16 + 18, 1013.25, 0.5, (String) null);
    }

    /** {@inheritDoc} */
    public double pathDelay(final double elevation, final double height) {
        // the corrected temperature using a temperature gradient of -6.5 K/km
        final double T = t0 - 6.5e-3 * height;
        // the corrected pressure
        final double P = p0 * FastMath.pow(1.0 - 2.26e-5 * height, 5.225);
        // the corrected humidity
        final double R = r0 * FastMath.exp(-6.396e-4 * height);

        // interpolate the b correction term
        final double B = bFunction.value(height / 1e3);
        // calculate e
        final double e = R * FastMath.exp(eFunction.value(T));

        // calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - elevation);

        // get correction factor
        final double deltaR = getDeltaR(height, z);

        // calculate the path delay in m
        final double tan = FastMath.tan(z);
        final double delta = 2.277e-3 / FastMath.cos(z) *
                             (P + (1255d / T + 5e-2) * e - B * tan * tan) + deltaR;

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

    /** Load δR function.
     * @param deltaRFileName regular expression for filename containing δR
     * correction term table
     * @return δR function
     * @exception OrekitException if table cannot be loaded
     */
    private static BilinearInterpolatingFunction loadDeltaR(final String deltaRFileName)
        throws OrekitException {

        // read the δR interpolation function from the config file
        final InterpolationTableLoader loader = new InterpolationTableLoader();
        DataProvidersManager.getInstance().feed(deltaRFileName, loader);
        if (!loader.stillAcceptsData()) {
            final double[] elevations = loader.getOrdinateGrid();
            for (int i = 0; i < elevations.length; ++i) {
                elevations[i] = FastMath.toRadians(elevations[i]);
            }
            return new BilinearInterpolatingFunction(loader.getAbscissaGrid(), elevations,
                                                     loader.getValuesSamples());
        }
        throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE,
                                  deltaRFileName.replaceAll("^\\^", "").replaceAll("\\$$", ""));
    }

    /** Create the default δR function.
     * @return δR function
     */
    private static BilinearInterpolatingFunction defaultDeltaR() {

        // the correction table in the referenced book only contains values for an angle of 60 - 80
        // degree, thus for 0 degree, the correction term is assumed to be 0, for degrees > 80 it
        // is assumed to be the same value as for 80.

        // the height in m
        final double xValForR[] = {
            0, 500, 1000, 1500, 2000, 3000, 4000, 5000
        };

        // the zenith angle
        final double yValForR[] = {
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

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(this);
    }

    /** Specialization of the data transfer object for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20160126L;

        /** The temperature at the station [K]. */
        private double t0;

        /** The atmospheric pressure [mbar]. */
        private double p0;

        /** The humidity [percent]. */
        private double r0;

        /** Samples x-coordinates. */
        private final double[] xval;

        /** Samples y-coordinates. */
        private final double[] yval;

        /** Set of cubic splines patching the whole data grid. */
        private final double[][] fval;

        /** Simple constructor.
         * @param model model to serialize
         */
        DataTransferObject(final SaastamoinenModel model) {
            this.t0 = model.t0;
            this.p0 = model.p0;
            this.r0 = model.r0;
            this.xval = model.deltaRFunction.xval.clone();
            this.yval = model.deltaRFunction.yval.clone();
            this.fval = model.deltaRFunction.fval.clone();
        }

        /** Replace the deserialized data transfer object with a {@link SaastamoinenModel}.
         * @return replacement {@link SaastamoinenModel}
         */
        private Object readResolve() {
            return new SaastamoinenModel(t0, p0, r0,
                                         new BilinearInterpolatingFunction(xval, yval, fval));
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

