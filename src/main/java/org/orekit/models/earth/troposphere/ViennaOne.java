/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

/** The Vienna 1 tropospheric delay model for radio techniques.
 * The Vienna model data are given with a time interval of 6 hours
 * as well as on a global 2.5° * 2.0° grid.
 * This version considered the height correction for the hydrostatic part
 * developed by Niell, 1996.
 *
 * @see "Boehm, J., Werl, B., and Schuh, H., (2006),
 *       Troposhere mapping functions for GPS and very long baseline
 *       interferometry from European Centre for Medium-Range Weather
 *       Forecasts operational analysis data, J. Geophy. Res., Vol. 111,
 *       B02406, doi:10.1029/2005JB003629"
 * @since 12.1
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 */
public class ViennaOne extends AbstractVienna {

    /** Build a new instance.
     * @param aProvider provider for a<sub>h</sub> and a<sub>w</sub> coefficients
     * @param gProvider provider for {@link AzimuthalGradientCoefficients} and {@link FieldAzimuthalGradientCoefficients}
     * @param zenithDelayProvider provider for zenith delays
     * @param utc                 UTC time scale
     */
    public ViennaOne(final ViennaAProvider aProvider,
                     final AzimuthalGradientProvider gProvider,
                     final TroposphericModel zenithDelayProvider,
                     final TimeScale utc) {
        super(aProvider, gProvider, zenithDelayProvider, utc);
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final TrackingCoordinates trackingCoordinates,
                                   final GeodeticPoint point,
                                   final PressureTemperatureHumidity weather,
                                   final AbsoluteDate date) {

        // a coefficients
        final ViennaACoefficients a = getAProvider().getA(point, date);

        // Day of year computation
        final double dofyear = getDayOfYear(date);

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
        final double coef = psi + ((dofyear - t0) / 365) * MathUtils.TWO_PI;
        final double ch = c0h + ((FastMath.cos(coef) + 1) * (c11h / 2) + c10h) * (1 - FastMath.cos(latitude));

        // General constants | Wet part
        final double bw = 0.00146;
        final double cw = 0.04391;

        final double[] function = new double[2];
        function[0] = TroposphericModelUtils.mappingFunction(a.getAh(), bh, ch,
                                                             trackingCoordinates.getElevation());
        function[1] = TroposphericModelUtils.mappingFunction(a.getAw(), bw, cw,
                                                             trackingCoordinates.getElevation());

        // Apply height correction
        final double correction = TroposphericModelUtils.computeHeightCorrection(trackingCoordinates.getElevation(),
                                                                                 point.getAltitude());
        function[0] = function[0] + correction;

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                  final FieldGeodeticPoint<T> point,
                                                                  final FieldPressureTemperatureHumidity<T> weather,
                                                                  final FieldAbsoluteDate<T> date) {

        final Field<T> field = date.getField();
        final T zero = field.getZero();

        // a coefficients
        final FieldViennaACoefficients<T> a = getAProvider().getA(point, date);

        // Day of year computation
        final T dofyear = getDayOfYear(date);

        // General constants | Hydrostatic part
        final T bh  = zero.newInstance(0.0029);
        final T c0h = zero.newInstance(0.062);
        final T c10h;
        final T c11h;
        final T psi;

        // Latitude and longitude of the station
        final T latitude = point.getLatitude();

        // sin(latitude) > 0 -> northern hemisphere
        if (FastMath.sin(latitude.getReal()) > 0) {
            c10h = zero.newInstance(0.001);
            c11h = zero.newInstance(0.005);
            psi  = zero;
        } else {
            c10h = zero.newInstance(0.002);
            c11h = zero.newInstance(0.007);
            psi  = zero.getPi();
        }

        // Compute hydrostatique coefficient c
        // Temporal factor
        double t0 = 28;
        if (latitude.getReal() < 0) {
            // southern hemisphere: t0 = 28 + an integer half of year
            t0 += 183;
        }
        final T coef = psi.add(dofyear.subtract(t0).divide(365).multiply(MathUtils.TWO_PI));
        final T ch = c11h.divide(2.0).multiply(FastMath.cos(coef).add(1.0)).add(c10h).multiply(FastMath.cos(latitude).negate().add(1.)).add(c0h);

        // General constants | Wet part
        final T bw = zero.newInstance(0.00146);
        final T cw = zero.newInstance(0.04391);

        final T[] function = MathArrays.buildArray(field, 2);
        function[0] = TroposphericModelUtils.mappingFunction(a.getAh(), bh, ch,
                                                             trackingCoordinates.getElevation());
        function[1] = TroposphericModelUtils.mappingFunction(a.getAw(), bw, cw,
                                                             trackingCoordinates.getElevation());

        // Apply height correction
        final T correction = TroposphericModelUtils.computeHeightCorrection(trackingCoordinates.getElevation(),
                                                                            point.getAltitude(),
                                                                            field);
        function[0] = function[0].add(correction);

        return function;
    }

}
