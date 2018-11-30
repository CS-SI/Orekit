/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.ParameterDriver;

/** The Vienna1 tropospheric delay model for radio techniques.
 * With this implementation, the hydrostatic and wet zenith delays as well
 * as the ah and aw coefficients are estimated as {@link org.orekit.utils.ParameterDriver ParameterDriver}.
 *
 * This version considered the height correction for the hydrostatic part
 * developed by Niell, 1996.
 *
 * @see Boehm, J., Werl, B., and Schuh, H., (2006),
 *      "Troposhere mapping functions for GPS and very long baseline
 *      interferometry from European Centre for Medium-Range Weather
 *      Forecasts operational analysis data," J. Geophy. Res., Vol. 111,
 *      B02406, doi:10.1029/2005JB003629
 *
 * @author Bryan Cazabonne
 */
public class EstimatedViennaOneModel implements DiscreteTroposphericModel {

    /** Name of one of the parameters of this model: the hydrostatic zenith delay. */
    public static final String HYDROSTATIC_ZENITH_DELAY = "hydrostatic zenith delay";

    /** Name of one of the parameters of this model: the slope hydrostatic zenith delay. */
    public static final String SLOPE_HYDROSTATIC_ZENITH_DELAY = "slope hydrostatic zenith delay";

    /** Name of one of the parameters of this model: the wet zenith delay. */
    public static final String WET_ZENITH_DELAY = "wet zenith delay";

    /** Name of one of the parameters of this model: the slope wet zenith delay. */
    public static final String SLOPE_WET_ZENITH_DELAY = "slope wet zenith delay";

    /** Name of the parameters of this model: the mapping function coefficient a<sub>h</sub>. */
    public static final String AH_COEFFICIENT = "mapping function coefficient ah";

    /** Name of the parameters of this model: the mapping function slope coefficient a<sub>h</sub>. */
    public static final String AH_SLOPE_COEFFICIENT = "mapping function slope coefficient ah";

    /** Name of the parameters of this model: the mapping function coefficient a<sub>w</sub>. */
    public static final String AW_COEFFICIENT = "mapping function coefficient aw";

    /** Name of the parameters of this model: the mapping function slope coefficient a<sub>w</sub>. */
    public static final String AW_SLOPE_COEFFICIENT = "mapping function slope coefficient aw";

    /** Serializable UID. */
    private static final long serialVersionUID = 3421856575466386588L;

    /** Geodetic site latitude, radians.*/
    private final double latitude;

    /** Driver for hydrostatic tropospheric delay parameter. */
    private final ParameterDriver dhzParameterDriver;

    /** Driver for slope hydrostatic tropospheric delay parameter. */
    private final ParameterDriver dhzSlopeParameterDriver;

    /** Driver for wet tropospheric delay parameter. */
    private final ParameterDriver dwzParameterDriver;

    /** Driver for slope wet tropospheric delay parameter. */
    private final ParameterDriver dwzSlopeParameterDriver;

    /** Driver for hydrostatic coefficient a<sub>h</sub>. */
    private final ParameterDriver ahParameterDriver;

    /** Driver for slope hydrostatic coefficient a<sub>h</sub>. */
    private final ParameterDriver ahSlopeParameterDriver;

    /** Driver for wet coefficient a<sub>w</sub>. */
    private final ParameterDriver awParameterDriver;

    /** Driver for slope wet coefficient a<sub>h</sub>. */
    private final ParameterDriver awSlopeParameterDriver;

    /** Build a new instance.
     * @param dhz initial value for the hydrostatic zenith delay
     * @param dwz initial value for the wet zenith delay
     * @param ah initial value for coefficient a<sub>h</sub>
     * @param aw initial value for coefficient a<sub>w</sub>
     * @param latitude geodetic latitude of the station
     */
    public EstimatedViennaOneModel(final double dhz, final double dwz,
                                   final double ah, final double aw,
                                   final double latitude) {
        dhzParameterDriver = new ParameterDriver(EstimatedViennaOneModel.HYDROSTATIC_ZENITH_DELAY,
                                                 dhz, FastMath.scalb(1.0, -2), 0.0, Double.POSITIVE_INFINITY);

        dhzSlopeParameterDriver = new ParameterDriver(EstimatedViennaOneModel.SLOPE_HYDROSTATIC_ZENITH_DELAY,
                                                 0.0, FastMath.scalb(1.0, -20), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        dwzParameterDriver = new ParameterDriver(EstimatedViennaOneModel.WET_ZENITH_DELAY,
                                                 dwz, FastMath.scalb(1.0, -5), 0.0, Double.POSITIVE_INFINITY);

        dwzSlopeParameterDriver = new ParameterDriver(EstimatedViennaOneModel.SLOPE_WET_ZENITH_DELAY,
                                                 0.0, FastMath.scalb(1.0, -20), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        ahParameterDriver = new ParameterDriver(EstimatedViennaOneModel.AH_COEFFICIENT,
                                                 ah, FastMath.scalb(1.0, -12), 0.0, Double.POSITIVE_INFINITY);

        ahSlopeParameterDriver = new ParameterDriver(EstimatedViennaOneModel.AH_SLOPE_COEFFICIENT,
                                                 0.0, FastMath.scalb(1.0, -20), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        awParameterDriver = new ParameterDriver(EstimatedViennaOneModel.AW_COEFFICIENT,
                                                 aw, FastMath.scalb(1.0, -14), 0.0, Double.POSITIVE_INFINITY);

        awSlopeParameterDriver = new ParameterDriver(EstimatedViennaOneModel.AW_SLOPE_COEFFICIENT,
                                                 0.0, FastMath.scalb(1.0, -20), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.latitude  = latitude;
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
        final double[] delays = new double[2];
        final double dt = date.durationFrom(getParametersDrivers().get(0).getReferenceDate());
        delays[0] = parameters[1] * dt + parameters[0];
        delays[1] = parameters[3] * dt + parameters[2];
        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] computeZenithDelay(final T height, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {
        final Field<T> field = date.getField();
        final T[] delays = MathArrays.buildArray(field, 2);
        final T dt = date.durationFrom(getParametersDrivers().get(0).getReferenceDate());
        delays[0] = parameters[1].multiply(dt).add(parameters[0]);
        delays[1] = parameters[3].multiply(dt).add(parameters[2]);
        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final double height,
                                   final double[] parameters, final AbsoluteDate date) {
        // Day of year computation
        final DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getUTC());
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

        // Compute hydrostatique coefficient c
        final double coef = ((dofyear - 28) / 365) * 2 * FastMath.PI + psi;
        final double ch = c0h + ((FastMath.cos(coef) + 1) * (c11h / 2) + c10h) * (1 - FastMath.cos(latitude));

        // General constants | Wet part
        final double bw = 0.00146;
        final double cw = 0.04391;

        // ah and aw coefficients : linear model
        final double dt = date.durationFrom(getParametersDrivers().get(4).getReferenceDate());
        final double ah = parameters[5] * dt + parameters[4];
        final double aw = parameters[7] * dt + parameters[6];

        final double[] function = new double[2];
        function[0] = computeFunction(ah, bh, ch, elevation);
        function[1] = computeFunction(aw, bw, cw, elevation);

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
        final DateTimeComponents dtc = date.getComponents(TimeScalesFactory.getUTC());
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
        final T coef = psi.add(((dofyear - 28) / 365) * 2 * FastMath.PI);
        final T ch = c11h.divide(2.0).multiply(FastMath.cos(coef).add(1.0)).add(c10h).multiply(1 - FastMath.cos(latitude)).add(c0h);

        // General constants | Wet part
        final T bw = zero.add(0.00146);
        final T cw = zero.add(0.04391);

        // ah and aw coefficients : linear model
        final T dt = date.durationFrom(getParametersDrivers().get(4).getReferenceDate());
        final T ah = parameters[5].multiply(dt).add(parameters[4]);
        final T aw = parameters[7].multiply(dt).add(parameters[6]);

        final T[] function = MathArrays.buildArray(field, 2);
        function[0] = computeFunction(ah, bh, ch, elevation);
        function[1] = computeFunction(aw, bw, cw, elevation);

        // Apply height correction
        final T correction = computeHeightCorrection(elevation, height, field);
        function[0] = function[0].add(correction);

        return function;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> list = new ArrayList<>(8);
        list.add(0, dhzParameterDriver);
        list.add(1, dhzSlopeParameterDriver);
        list.add(2, dwzParameterDriver);
        list.add(3, dwzSlopeParameterDriver);
        list.add(4, ahParameterDriver);
        list.add(5, ahSlopeParameterDriver);
        list.add(6, awParameterDriver);
        list.add(7, awSlopeParameterDriver);
        return Collections.unmodifiableList(list);
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
        // Denominateur
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
        // Denominateur
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
        final double sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final double function = computeFunction(2.53e-5, 5.49e-3, 1.14e-3, elevation);
        // Ref: Eq. 6
        final double dmdh = (1 / sinE) - function;
        // Ref: Eq. 7
        final double correction = dmdh * (height / 1000);
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
        final T sinE = FastMath.sin(elevation);
        // Ref: Eq. 4
        final T function = computeFunction(zero.add(2.53e-5), zero.add(5.49e-3), zero.add(1.14e-3), elevation);
        // Ref: Eq. 6
        final T dmdh = sinE.reciprocal().subtract(function);
        // Ref: Eq. 7
        final T correction = dmdh.multiply(height.divide(1000.0));
        return correction;
    }

}
