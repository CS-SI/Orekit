/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;

/** The Vienna1 tropospheric delay model for radio techniques.
 * The Vienna model data are given with a time interval of 6 hours
 * as well as on a global 2.5° * 2.0° grid.
 *
 * This version considered the height correction for the hydrostatic part
 * developed by Niell, 1996.
 *
 * @see "Boehm, J., Werl, B., and Schuh, H., (2006),
 *       Troposhere mapping functions for GPS and very long baseline
 *       interferometry from European Centre for Medium-Range Weather
 *       Forecasts operational analysis data, J. Geophy. Res., Vol. 111,
 *       B02406, doi:10.1029/2005JB003629"
 *
 * @author Bryan Cazabonne
 */
public class ViennaOneModel implements DiscreteTroposphericModel {

    /** The a coefficient for the computation of the wet and hydrostatic mapping functions.*/
    private final double[] coefficientsA;

    /** Values of hydrostatic and wet delays as provided by the Vienna model. */
    private final double[] zenithDelay;

    /** Geodetic site latitude, radians.*/
    private final double latitude;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param coefficientA The a coefficients for the computation of the wet and hydrostatic mapping functions.
     * @param zenithDelay Values of hydrostatic and wet delays
     * @param latitude geodetic latitude of the station, in radians
     * @see #ViennaOneModel(double[], double[], double, TimeScale)
     */
    @DefaultDataContext
    public ViennaOneModel(final double[] coefficientA, final double[] zenithDelay,
                          final double latitude) {
        this(coefficientA, zenithDelay, latitude,
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Build a new instance.
     *
     * @param coefficientA The a coefficients for the computation of the wet and
     *                     hydrostatic mapping functions.
     * @param zenithDelay  Values of hydrostatic and wet delays
     * @param latitude     geodetic latitude of the station, in radians
     * @param utc          UTC time scale.
     * @since 10.1
     */
    public ViennaOneModel(final double[] coefficientA,
                          final double[] zenithDelay,
                          final double latitude,
                          final TimeScale utc) {
        this.coefficientsA = coefficientA.clone();
        this.zenithDelay   = zenithDelay.clone();
        this.latitude      = latitude;
        this.utc = utc;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final double height,
                            final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final double[] delays = computeZenithDelay(height, parameters, date);
        // mapping function
        final double[] mappingFunction = mappingFactors(elevation, height, parameters, date);
        // Tropospheric path delay
        return delays[0] * mappingFunction[0] + delays[1] * mappingFunction[1];
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T pathDelay(final T elevation, final T height,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // zenith delay
        final T[] delays = computeZenithDelay(height, parameters, date);
        // mapping function
        final T[] mappingFunction = mappingFactors(elevation, height, parameters, date);
        // Tropospheric path delay
        return delays[0].multiply(mappingFunction[0]).add(delays[1].multiply(mappingFunction[1]));
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeZenithDelay(final double height, final double[] parameters, final AbsoluteDate date) {
        return zenithDelay.clone();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] computeZenithDelay(final T height, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {
        final Field<T> field = height.getField();
        final T zero = field.getZero();
        final T[] delays = MathArrays.buildArray(field, 2);
        delays[0] = zero.add(zenithDelay[0]);
        delays[1] = zero.add(zenithDelay[1]);
        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final double height,
                                   final double[] parameters, final AbsoluteDate date) {
        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // General constants | Hydrostatic part
        final double bh  = 0.0029;
        final double c0h = 0.062;
        final double c10h;
        final double c11h;
        final double psi;

        // sin(latitude) > 0 -> northern hemisphere
        if (FastMath.sin(latitude) > 0) {
            c10h = 0.001;
            c11h = 0.005;
            psi  = 0;
        } else {
            c10h = 0.002;
            c11h = 0.007;
            psi  = FastMath.PI;
        }

        // Temporal factor
        double t0 = 28;
        if (latitude < 0) {
            // southern hemisphere: t0 = 28 + an integer half of year
            t0 += 183;
        }
        // Compute hydrostatique coefficient c
        final double coef = ((dofyear - t0) / 365) * 2 * FastMath.PI + psi;
        final double ch = c0h + ((FastMath.cos(coef) + 1) * (c11h / 2) + c10h) * (1 - FastMath.cos(latitude));

        // General constants | Wet part
        final double bw = 0.00146;
        final double cw = 0.04391;

        final double[] function = new double[2];
        function[0] = computeFunction(coefficientsA[0], bh, ch, elevation);
        function[1] = computeFunction(coefficientsA[1], bw, cw, elevation);

        // Apply height correction
        final double correction = computeHeightCorrection(elevation, height);
        function[0] = function[0] + correction;

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] mappingFactors(final T elevation, final T height,
                                                              final T[] parameters, final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T zero = field.getZero();

        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // General constants | Hydrostatic part
        final T bh  = zero.add(0.0029);
        final T c0h = zero.add(0.062);
        final T c10h;
        final T c11h;
        final T psi;

        // sin(latitude) > 0 -> northern hemisphere
        if (FastMath.sin(latitude) > 0) {
            c10h = zero.add(0.001);
            c11h = zero.add(0.005);
            psi  = zero;
        } else {
            c10h = zero.add(0.002);
            c11h = zero.add(0.007);
            psi  = zero.add(FastMath.PI);
        }

        // Compute hydrostatique coefficient c
        // Temporal factor
        double t0 = 28;
        if (latitude < 0) {
            // southern hemisphere: t0 = 28 + an integer half of year
            t0 += 183;
        }
        final T coef = psi.add(((dofyear - t0) / 365) * 2 * FastMath.PI);
        final T ch = c11h.divide(2.0).multiply(FastMath.cos(coef).add(1.0)).add(c10h).multiply(1 - FastMath.cos(latitude)).add(c0h);

        // General constants | Wet part
        final T bw = zero.add(0.00146);
        final T cw = zero.add(0.04391);

        final T[] function = MathArrays.buildArray(field, 2);
        function[0] = computeFunction(zero.add(coefficientsA[0]), bh, ch, elevation);
        function[1] = computeFunction(zero.add(coefficientsA[1]), bw, cw, elevation);

        // Apply height correction
        final T correction = computeHeightCorrection(elevation, height, field);
        function[0] = function[0].add(correction);

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Compute the mapping function related to the coefficient values and the elevation.
     * @param a a coefficient
     * @param b b coefficient
     * @param c c coefficient
     * @param elevation the elevation of the satellite, in radians.
     * @return the value of the function at a given elevation
     */
    private double computeFunction(final double a, final double b, final double c, final double elevation) {
        final double sinE = FastMath.sin(elevation);
        // Numerator
        final double numMP = 1 + a / (1 + b / (1 + c));
        // Denominator
        final double denMP = sinE + a / (sinE + b / (sinE + c));

        final double felevation = numMP / denMP;

        return felevation;
    }

    /** Compute the mapping function related to the coefficient values and the elevation.
     * @param <T> type of the elements
     * @param a a coefficient
     * @param b b coefficient
     * @param c c coefficient
     * @param elevation the elevation of the satellite, in radians.
     * @return the value of the function at a given elevation
     */
    private <T extends RealFieldElement<T>> T computeFunction(final T a, final T b, final T c, final T elevation) {
        final T sinE = FastMath.sin(elevation);
        // Numerator
        final T numMP = a.divide(b.divide(c.add(1.0)).add(1.0)).add(1.0);
        // Denominator
        final T denMP = a.divide(b.divide(c.add(sinE)).add(sinE)).add(sinE);

        final T felevation = numMP.divide(denMP);

        return felevation;
    }

    /** This method computes the height correction for the hydrostatic
     *  component of the mapping function.
     *  The formulas are given by Neill's paper, 1996:
     *<p>
     *      Niell A. E. (1996)
     *      "Global mapping functions for the atmosphere delay of radio wavelengths,”
     *      J. Geophys. Res., 101(B2), pp.  3227–3246, doi:  10.1029/95JB03048.
     *</p>
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @return the height correction, in m
     */
    private double computeHeightCorrection(final double elevation, final double height) {
        final double fixedHeight = FastMath.max(0.0, height);
        final double sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final double function = computeFunction(2.53e-5, 5.49e-3, 1.14e-3, elevation);
        // Ref: Eq. 6
        final double dmdh = (1 / sinE) - function;
        // Ref: Eq. 7
        final double correction = dmdh * (fixedHeight / 1000);
        return correction;
    }

    /** This method computes the height correction for the hydrostatic
     *  component of the mapping function.
     *  The formulas are given by Neill's paper, 1996:
     *<p>
     *      Niell A. E. (1996)
     *      "Global mapping functions for the atmosphere delay of radio wavelengths,”
     *      J. Geophys. Res., 101(B2), pp.  3227–3246, doi:  10.1029/95JB03048.
     *</p>
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite, in radians.
     * @param height the height of the station in m above sea level.
     * @param field field to which the elements belong
     * @return the height correction, in m
     */
    private <T extends RealFieldElement<T>> T computeHeightCorrection(final T elevation, final T height, final Field<T> field) {
        final T zero = field.getZero();
        final T fixedHeight = FastMath.max(zero, height);
        final T sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final T function = computeFunction(zero.add(2.53e-5), zero.add(5.49e-3), zero.add(1.14e-3), elevation);
        // Ref: Eq. 6
        final T dmdh = sinE.reciprocal().subtract(function);
        // Ref: Eq. 7
        final T correction = dmdh.multiply(fixedHeight.divide(1000.0));
        return correction;
    }

}
