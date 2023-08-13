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
package org.orekit.models.earth.troposphere;

import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
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
public class ViennaOneModel implements DiscreteTroposphericModel, MappingFunction {

    /** The a coefficient for the computation of the wet and hydrostatic mapping functions.*/
    private final double[] coefficientsA;

    /** Values of hydrostatic and wet delays as provided by the Vienna model. */
    private final double[] zenithDelay;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Build a new instance.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param coefficientA The a coefficients for the computation of the wet and hydrostatic mapping functions.
     * @param zenithDelay Values of hydrostatic and wet delays
     * @see #ViennaOneModel(double[], double[], TimeScale)
     */
    @DefaultDataContext
    public ViennaOneModel(final double[] coefficientA, final double[] zenithDelay) {
        this(coefficientA, zenithDelay,
             DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Build a new instance.
     *
     * @param coefficientA The a coefficients for the computation of the wet and
     *                     hydrostatic mapping functions.
     * @param zenithDelay  Values of hydrostatic and wet delays
     * @param utc          UTC time scale.
     * @since 10.1
     */
    public ViennaOneModel(final double[] coefficientA,
                          final double[] zenithDelay,
                          final TimeScale utc) {
        this.coefficientsA = coefficientA.clone();
        this.zenithDelay   = zenithDelay.clone();
        this.utc           = utc;
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final double[] delays = computeZenithDelay(point, parameters, date);
        // mapping function
        final double[] mappingFunction = mappingFactors(elevation, point, date);
        // Tropospheric path delay
        return delays[0] * mappingFunction[0] + delays[1] * mappingFunction[1];
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // zenith delay
        final T[] delays = computeZenithDelay(point, parameters, date);
        // mapping function
        final T[] mappingFunction = mappingFactors(elevation, point, date);
        // Tropospheric path delay
        return delays[0].multiply(mappingFunction[0]).add(delays[1].multiply(mappingFunction[1]));
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>T[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public double[] computeZenithDelay(final GeodeticPoint point, final double[] parameters, final AbsoluteDate date) {
        return zenithDelay.clone();
    }

    /** This method allows the  computation of the zenith hydrostatic and
     * zenith wet delay. The resulting element is an array having the following form:
     * <ul>
     * <li>T[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>T[1] = D<sub>wz</sub> → zenith wet delay
     * </ul>
     * @param <T> type of the elements
     * @param point station location
     * @param parameters tropospheric model parameters
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public <T extends CalculusFieldElement<T>> T[] computeZenithDelay(final FieldGeodeticPoint<T> point, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T zero = field.getZero();
        final T[] delays = MathArrays.buildArray(field, 2);
        delays[0] = zero.add(zenithDelay[0]);
        delays[1] = zero.add(zenithDelay[1]);
        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final GeodeticPoint point,
                                   final AbsoluteDate date) {
        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(utc);
        final int dofyear = dtc.getDate().getDayOfYear();

        // General constants | Hydrostatic part
        final double bh  = 0.0029;
        final double c0h = 0.062;
        final double c10h;
        final double c11h;
        final double psi;

        // Latitude of the station
        final double latitude = point.getLatitude();

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
        function[0] = TroposphericModelUtils.mappingFunction(coefficientsA[0], bh, ch, elevation);
        function[1] = TroposphericModelUtils.mappingFunction(coefficientsA[1], bw, cw, elevation);

        // Apply height correction
        final double correction = TroposphericModelUtils.computeHeightCorrection(elevation, point.getAltitude());
        function[0] = function[0] + correction;

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final T elevation, final FieldGeodeticPoint<T> point,
                                                              final FieldAbsoluteDate<T> date) {
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

        // Latitude and longitude of the station
        final T latitude = point.getLatitude();

        // sin(latitude) > 0 -> northern hemisphere
        if (FastMath.sin(latitude.getReal()) > 0) {
            c10h = zero.add(0.001);
            c11h = zero.add(0.005);
            psi  = zero;
        } else {
            c10h = zero.add(0.002);
            c11h = zero.add(0.007);
            psi  = zero.getPi();
        }

        // Compute hydrostatique coefficient c
        // Temporal factor
        double t0 = 28;
        if (latitude.getReal() < 0) {
            // southern hemisphere: t0 = 28 + an integer half of year
            t0 += 183;
        }
        final T coef = psi.add(zero.getPi().multiply(2.0).multiply((dofyear - t0) / 365));
        final T ch = c11h.divide(2.0).multiply(FastMath.cos(coef).add(1.0)).add(c10h).multiply(FastMath.cos(latitude).negate().add(1.)).add(c0h);

        // General constants | Wet part
        final T bw = zero.add(0.00146);
        final T cw = zero.add(0.04391);

        final T[] function = MathArrays.buildArray(field, 2);
        function[0] = TroposphericModelUtils.mappingFunction(zero.add(coefficientsA[0]), bh, ch, elevation);
        function[1] = TroposphericModelUtils.mappingFunction(zero.add(coefficientsA[1]), bw, cw, elevation);

        // Apply height correction
        final T correction = TroposphericModelUtils.computeHeightCorrection(elevation, point.getAltitude(), field);
        function[0] = function[0].add(correction);

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

}
