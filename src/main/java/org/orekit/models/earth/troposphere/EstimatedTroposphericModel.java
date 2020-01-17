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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** An estimated tropospheric model. The tropospheric delay is computed according to the formula:
 * <p>
 * δ = δ<sub>h</sub> * m<sub>h</sub> + (δ<sub>t</sub> - δ<sub>h</sub>) * m<sub>w</sub>
 * <p>
 * With:
 * <ul>
 * <li>δ<sub>h</sub>: Tropospheric zenith hydro-static delay.</li>
 * <li>δ<sub>t</sub>: Tropospheric total zenith delay.</li>
 * <li>m<sub>h</sub>: Hydro-static mapping function.</li>
 * <li>m<sub>w</sub>: Wet mapping function.</li>
 * </ul>
 * <p>
 * The mapping functions m<sub>h</sub>(e) and m<sub>w</sub>(e) are
 * computed thanks to a {@link #model} initialized by the user.
 * The user has the possiblility to use several mapping function models for the computations:
 * the {@link GlobalMappingFunctionModel Global Mapping Function}, or
 * the {@link NiellMappingFunctionModel Niell Mapping Function}
 * </p> <p>
 * The tropospheric zenith delay δ<sub>h</sub> is computed empirically with a {@link SaastamoinenModel}
 * while the tropospheric total zenith delay δ<sub>t</sub> is estimated as a {@link ParameterDriver}
 */
public class EstimatedTroposphericModel implements DiscreteTroposphericModel {

    /** Name of the parameter of this model: the total zenith delay. */
    public static final String TOTAL_ZENITH_DELAY = "total zenith delay";

    /** Mapping Function model. */
    private final MappingFunction model;

    /** Driver for the tropospheric zenith total delay.*/
    private final ParameterDriver totalZenithDelay;

    /** The temperature at the station [K]. */
    private double t0;

    /** The atmospheric pressure [mbar]. */
    private double p0;

    /** Build a new instance using the given environmental conditions.
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param model mapping function model (NMF or GMF).
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     */
    public EstimatedTroposphericModel(final double t0, final double p0,
                                      final MappingFunction model, final double totalDelay) {

        totalZenithDelay = new ParameterDriver(EstimatedTroposphericModel.TOTAL_ZENITH_DELAY,
                                               totalDelay, FastMath.scalb(1.0, 0), 0.0, Double.POSITIVE_INFINITY);

        this.t0    = t0;
        this.p0    = p0;
        this.model = model;
    }

    /** Build a new instance using a standard atmosphere model.
     * <ul>
     * <li>temperature: 18 degree Celsius
     * <li>pressure: 1013.25 mbar
     * </ul>
     * @param model mapping function model (NMF or GMF).
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     */
    public EstimatedTroposphericModel(final MappingFunction model, final double totalDelay) {
        this(273.15 + 18.0, 1013.25, model, totalDelay);
    }

    @Override
    public double[] mappingFactors(final double elevation, final double height,
                                   final double[] parameters, final AbsoluteDate date) {
        return model.mappingFactors(elevation, height, parameters, date);
    }

    @Override
    public <T extends RealFieldElement<T>> T[] mappingFactors(final T elevation, final T height,
                                                              final T[] parameters, final FieldAbsoluteDate<T> date) {
        return model.mappingFactors(elevation, height, parameters, date);
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(totalZenithDelay);
    }

    @Override
    public double pathDelay(final double elevation, final double height,
                            final double[] parameters, final AbsoluteDate date) {

        // Mapping functions
        final double[] mf = mappingFactors(elevation, height, parameters, date);
        // Zenith delays
        final double[] delays = computeZenithDelay(height, parameters, date);
        // Total delay
        return mf[0] * delays[0] + mf[1] * (delays[1] - delays[0]);
    }

    @Override
    public <T extends RealFieldElement<T>> T pathDelay(final T elevation, final T height,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {

        // Mapping functions
        final T[] mf = mappingFactors(elevation, height, parameters, date);
        // Zenith delays
        final T[] delays = computeZenithDelay(height, parameters, date);
        // Total delay
        return mf[0].multiply(delays[0]).add(mf[1].multiply(delays[1].subtract(delays[0])));
    }

    /** This method allows the computation of the zenith hydrostatic and zenith total delays.
     *  The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>tz</sub> → zenith total delay
     * </ul>
     * <p>
     * The user have to be careful because the others tropospheric models in Orekit
     * compute the zenith wet delay instead of the total zenith delay.
     * </p>
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public double[] computeZenithDelay(final double height, final double[] parameters,
                                       final AbsoluteDate date) {

        // Use an empirical model for tropospheric zenith hydro-static delay : Saastamoinen model
        final SaastamoinenModel saastamoinen = new SaastamoinenModel(t0, p0, 0.0);

        // elevation = pi/2 because we compute the delay in the zenith direction
        final double zhd = saastamoinen.pathDelay(0.5 * FastMath.PI, height, parameters, date);
        final double ztd = parameters[0];

        return new double[] {
            zhd,
            ztd
        };
    }

    /** This method allows the computation of the zenith hydrostatic and zenith total delays.
     *  The resulting element is an array having the following form:
     * <ul>
     * <li>double[0] = D<sub>hz</sub> → zenith hydrostatic delay
     * <li>double[1] = D<sub>tz</sub> → zenith total delay
     * </ul>
     * <p>
     * The user have to be careful because the others tropospheric models in Orekit
     * compute the zenith wet delay instead of the total zenith delay.
     * </p>
     * @param <T> type of the elements
     * @param height the height of the station in m above sea level.
     * @param parameters tropospheric model parameters.
     * @param date current date
     * @return a two components array containing the zenith hydrostatic and wet delays.
     */
    public <T extends RealFieldElement<T>> T[] computeZenithDelay(final T height, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {

        // Field
        final Field<T> field = date.getField();
        final T zero         = field.getZero();

        // Use an empirical model for tropospheric zenith hydro-static delay : Saastamoinen model
        final SaastamoinenModel saastamoinen = new SaastamoinenModel(t0, p0, 0.0);

        // elevation = pi/2 because we compute the delay in the zenith direction
        final T zhd = saastamoinen.pathDelay(zero.add(0.5 * FastMath.PI), height, parameters, date);
        final T ztd = parameters[0];

        final T[] delays = MathArrays.buildArray(field, 2);
        delays[0] = zhd;
        delays[1] = ztd;

        return delays;
    }

}
