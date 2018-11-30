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
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** An estimated tropospheric delay model.
 * <p>
 * With this implementation, the hydrostatic δ<sub>h</sub> and wet δ<sub>w</sub> zenith delays are
 * estimated.
 * </p> <p>
 * The mapping functions m<sub>h</sub>(e) and m<sub>w</sub>(e) are
 * computed thanks to a {@link #model} initialized by the user.
 * The user has the possiblility to use several mapping function models for the computations:
 * the {@link GlobalMappingFunctionModel Global Mapping Function},
 * the {@link NiellMappingFunctionModel Niell Mapping Function}
 * or the {@link MendesPavlisModel Mendes & Pavlis} model for the optical wavelenghts.
 * </p>
 * @author Bryan Cazabonne
 * */
public class EstimatedTroposphericModel implements DiscreteTroposphericModel {

    /** Name of one of the parameters of this model: the hydrostatic zenith delay. */
    public static final String HYDROSTATIC_ZENITH_DELAY = "hydrostatic zenith delay";

    /** Name of one of the parameters of this model: the slope hydrostatic zenith delay. */
    public static final String SLOPE_HYDROSTATIC_ZENITH_DELAY = "slope hydrostatic zenith delay";

    /** Name of one of the parameters of this model: the wet zenith delay. */
    public static final String WET_ZENITH_DELAY = "wet zenith delay";

    /** Name of one of the parameters of this model: the slope wet zenith delay. */
    public static final String SLOPE_WET_ZENITH_DELAY = "slope wet zenith delay";

    /** Serializable UID. */
    private static final long serialVersionUID = -2348714833140436533L;

    /** Mapping Function model. */
    private final MappingFunction model;

    /** Driver for hydrostatic tropospheric delay parameter. */
    private final ParameterDriver dhzParameterDriver;

    /** Driver for slope hydrostatic tropospheric delay parameter. */
    private final ParameterDriver dhzSlopeParameterDriver;

    /** Driver for wet tropospheric delay parameter. */
    private final ParameterDriver dwzParameterDriver;

    /** Driver for wet tropospheric delay parameter. */
    private final ParameterDriver dwzSlopeParameterDriver;

    /** Build a new instance.
     * @param model mapping function model.
     * @param dhz initial value for the hydrostatic zenith delay
     * @param dwz initial value for the wet zenith delay
     */
    public EstimatedTroposphericModel(final MappingFunction model,
                                      final double dhz,
                                      final double dwz) {
        dhzParameterDriver      = new ParameterDriver(EstimatedTroposphericModel.HYDROSTATIC_ZENITH_DELAY,
                                                 dhz, FastMath.scalb(1.0, -2), 0.0, Double.POSITIVE_INFINITY);

        dhzSlopeParameterDriver = new ParameterDriver(EstimatedTroposphericModel.SLOPE_HYDROSTATIC_ZENITH_DELAY,
                                                 0.0, FastMath.scalb(1.0, -20), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        dwzParameterDriver      = new ParameterDriver(EstimatedTroposphericModel.WET_ZENITH_DELAY,
                                                 dwz, FastMath.scalb(1.0, -5), 0.0, Double.POSITIVE_INFINITY);

        dwzSlopeParameterDriver = new ParameterDriver(EstimatedTroposphericModel.SLOPE_WET_ZENITH_DELAY,
                                                 0.0, FastMath.scalb(1.0, -20), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double elevation, final double height,
                                   final double[] parameters, final AbsoluteDate date) {
        return model.mappingFactors(elevation, height, parameters, date);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] mappingFactors(final T elevation, final T height,
                                                              final T[] parameters, final FieldAbsoluteDate<T> date) {
        return model.mappingFactors(elevation, height, parameters, date);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final double height,
                            final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final double[] zenithDelay = computeZenithDelay(height, parameters, date);
        // mapping function
        final double[] mappingFunction = mappingFactors(elevation, height, parameters, date);
        // Tropospheric path delay
        return zenithDelay[0] * mappingFunction[0] + zenithDelay[1] * mappingFunction[1];
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
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> list = new ArrayList<>(4);
        list.add(0, dhzParameterDriver);
        list.add(1, dhzSlopeParameterDriver);
        list.add(2, dwzParameterDriver);
        list.add(3, dwzSlopeParameterDriver);
        return Collections.unmodifiableList(list);
    }

}
