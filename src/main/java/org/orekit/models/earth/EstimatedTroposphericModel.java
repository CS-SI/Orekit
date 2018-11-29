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

    /** Name of the parameters of this model: the zenith delay. */
    public static final String ZENITH_DELAY = " zenith delay";

    /** Serializable UID. */
    private static final long serialVersionUID = -2348714833140436533L;

    /** Mapping Function model. */
    private final MappingFunction model;

    /** Driver for hydrostatic tropospheric delay parameter. */
    private final ParameterDriver dhzParameterDriver;

    /** Driver for wet tropospheric delay parameter. */
    private final ParameterDriver dwzParameterDriver;

    /** Build a new instance.
     * @param model mapping function model.
     * @param dhz initial value for the hydrostatic zenith delay
     * @param dwz initial value for the wet zenith delay
     */
    public EstimatedTroposphericModel(final MappingFunction model,
                                      final double dhz,
                                      final double dwz) {
        dhzParameterDriver = new ParameterDriver("hydrostatic" + EstimatedTroposphericModel.ZENITH_DELAY,
                                                 dhz, FastMath.scalb(1.0, -2), 0.0, Double.POSITIVE_INFINITY);
        dwzParameterDriver = new ParameterDriver("wet" + EstimatedTroposphericModel.ZENITH_DELAY,
                                                 dwz, FastMath.scalb(1.0, -5), 0.0, Double.POSITIVE_INFINITY);
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
        final double[] zenithDelay = computeZenithDelay(height, parameters);
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
        final T[] delays = computeZenithDelay(height, parameters);
        // mapping function
        final T[] mappingFunction = mappingFactors(elevation, height, parameters, date);
        // Tropospheric path delay
        return delays[0].multiply(mappingFunction[0]).add(delays[1].multiply(mappingFunction[1]));
    }

    /** {@inheritDoc} */
    @Override
    public double[] computeZenithDelay(final double height, final double[] parameters) {
        return new double[] {
            parameters[0],
            parameters[1]
        };
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] computeZenithDelay(final T height, final T[] parameters) {
        final Field<T> field = height.getField();
        final T[] delay = MathArrays.buildArray(field, 2);
        delay[0] = parameters[0];
        delay[1] = parameters[1];
        return delay;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> list = new ArrayList<>(2);
        list.add(dhzParameterDriver);
        list.add(dwzParameterDriver);
        return Collections.unmodifiableList(list);
    }

}
