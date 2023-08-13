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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
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
 * The user has the possibility to use several mapping function models for the computations:
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

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.singletonList(totalZenithDelay);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        // Use an empirical model for tropospheric zenith hydro-static delay : Saastamoinen model
        final SaastamoinenModel saastamoinen = new SaastamoinenModel(t0, p0, 0.0);
        // Zenith delays. elevation = pi/2 because we compute the delay in the zenith direction
        final double zhd = saastamoinen.pathDelay(0.5 * FastMath.PI, point, parameters, date);
        final double ztd = parameters[0];
        // Mapping functions
        final double[] mf = model.mappingFactors(elevation, point, date);
        // Total delay
        return mf[0] * zhd + mf[1] * (ztd - zhd);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation, final FieldGeodeticPoint<T> point,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // Use an empirical model for tropospheric zenith hydro-static delay : Saastamoinen model
        final SaastamoinenModel saastamoinen = new SaastamoinenModel(t0, p0, 0.0);
        // Zenith delays. elevation = pi/2 because we compute the delay in the zenith direction
        final T zhd = saastamoinen.pathDelay(elevation.getPi().multiply(0.5), point, parameters, date);
        final T ztd = parameters[0];
        // Mapping functions
        final T[] mf = model.mappingFactors(elevation, point, date);
        // Total delay
        return mf[0].multiply(zhd).add(mf[1].multiply(ztd.subtract(zhd)));
    }

}
