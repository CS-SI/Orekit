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
 * the {@link NiellMappingFunctionModel Niell Mapping Function} for the radio wavelengths
 * or the {@link MendesPavlisModel Mendes & Pavlis} model for the optical wavelengths.
 * </p>
 * @author Bryan Cazabonne
 * */
public class EstimatedTroposphericModel implements DiscreteTroposphericModel {

    /** Name of one of the parameters of this model: the hydrostatic zenith delay. */
    public static final String HYDROSTATIC_ZENITH_DELAY = "hydrostatic zenith delay";

    /** Name of one of the parameters of this model: the wet zenith delay. */
    public static final String WET_ZENITH_DELAY = "wet zenith delay";

    /** Serializable UID. */
    private static final long serialVersionUID = -2348714833140436533L;

    /** Mapping Function model. */
    private final MappingFunction model;

    /** Driver for hydrostatic tropospheric delay parameter. */
    private final ParameterDriver initDateDHZParameterDriver;

    /** Driver for hydrostatic tropospheric delay parameter. */
    private final ParameterDriver endDateDHZParameterDriver;

    /** Driver for wet tropospheric delay parameter. */
    private final ParameterDriver initDateDWZParameterDriver;

    /** Driver for wet tropospheric delay parameter. */
    private final ParameterDriver endDateDWZParameterDriver;

    /** Build a new instance.
     * <p>
     * By definition, init date and end date parameters have the same name.
     * It is recommended to change the name of the parameters by adding a prefix with the reference date.
     * </p>
     * @param model mapping function model.
     * @param hydroDelayInitDate initial value for the hydrostatic zenith delay (first date)
     * @param hydroDelayEndDate initial value for the slope hydrostatic zenith delay (end date)
     * @param wetDelayInitDate initial value for the wet zenith delay (first date)
     * @param wetDelayEndDate initial value for the slope wet zenith delay (end date)
     */
    public EstimatedTroposphericModel(final MappingFunction model,
                                      final double hydroDelayInitDate,
                                      final double hydroDelayEndDate,
                                      final double wetDelayInitDate,
                                      final double wetDelayEndDate) {

        initDateDHZParameterDriver = new ParameterDriver(EstimatedTroposphericModel.HYDROSTATIC_ZENITH_DELAY,
                                                 hydroDelayInitDate, FastMath.scalb(1.0, -2), 0.0, 10.0);

        endDateDHZParameterDriver  = new ParameterDriver(EstimatedTroposphericModel.HYDROSTATIC_ZENITH_DELAY,
                                                 hydroDelayEndDate, FastMath.scalb(1.0, -2), 0.0, 10.0);

        initDateDWZParameterDriver = new ParameterDriver(EstimatedTroposphericModel.WET_ZENITH_DELAY,
                                                 wetDelayInitDate, FastMath.scalb(1.0, -3), 0.0, 1.0);

        endDateDWZParameterDriver  = new ParameterDriver(EstimatedTroposphericModel.WET_ZENITH_DELAY,
                                                 wetDelayEndDate, FastMath.scalb(1.0, -3), 0.0, 1.0);

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
        // Time intervals
        final double dt1 = endDateDHZParameterDriver.getReferenceDate().durationFrom(date);
        final double dt0 = date.durationFrom(initDateDHZParameterDriver.getReferenceDate());
        final double dt  = dt1 + dt0;

        // Zenith delay
        final double[] delays = new double[2];

        if (FastMath.abs(dt) < 0.001) {
            // Constant model
            delays[0] = parameters[0];
            delays[1] = parameters[2];
        } else {
            // Linear model
            delays[0] = (parameters[0] * dt1 + parameters[1] * dt0) / dt;
            delays[1] = (parameters[2] * dt1 + parameters[3] * dt0) / dt;
        }

        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] computeZenithDelay(final T height, final T[] parameters,
                                                                  final FieldAbsoluteDate<T> date) {
        // Field
        final Field<T> field = date.getField();

        // Time intervals
        final T dt1 = date.durationFrom(endDateDHZParameterDriver.getReferenceDate()).negate();
        final T dt0 = date.durationFrom(initDateDHZParameterDriver.getReferenceDate());
        final T dt  = dt1.add(dt0);

        // Zenith delay
        final T[] delays = MathArrays.buildArray(field, 2);

        if (FastMath.abs(dt).getReal() < 0.001) {
            // Constant model
            delays[0] = parameters[0];
            delays[1] = parameters[2];
        } else {
            // Linear model
            delays[0] = (parameters[0].multiply(dt1).add(parameters[1].multiply(dt0))).divide(dt);
            delays[1] = (parameters[2].multiply(dt1).add(parameters[3].multiply(dt0))).divide(dt);
        }

        return delays;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> list = new ArrayList<ParameterDriver>(4);
        list.add(0, initDateDHZParameterDriver);
        list.add(1, endDateDHZParameterDriver);
        list.add(2, initDateDWZParameterDriver);
        list.add(3, endDateDWZParameterDriver);
        return Collections.unmodifiableList(list);
    }

}
