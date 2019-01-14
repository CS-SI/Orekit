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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

public class EstimatedTroposphericModel implements DiscreteTroposphericModel {

    /** Name of the parameters of this model: the zenith delay. */
    public static final String ZENITH_DELAY = " zenith delay";

    /** Serializable UID. */
    private static final long serialVersionUID = -2348714833140436533L;

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, 3);

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
                                                 dhz, SCALE, 0.0, Double.POSITIVE_INFINITY);
        dwzParameterDriver = new ParameterDriver("wet" + EstimatedTroposphericModel.ZENITH_DELAY,
                                                 dwz, SCALE, 0.0, Double.POSITIVE_INFINITY);
        this.model = model;
    }

    /** {@inheritDoc} */
    @Override
    public double[] mappingFactors(final double height, final double elevation,
                                   final AbsoluteDate date) {
        return model.mappingFactors(height, elevation, date);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T[] mappingFactors(final T height, final T elevation,
                                                              final FieldAbsoluteDate<T> date) {
        return model.mappingFactors(height, elevation, date);
    }

    /** {@inheritDoc} */
    @Override
    public double pathDelay(final double elevation, final double height,
                            final double[] parameters, final AbsoluteDate date) {
        // zenith delay
        final double[] zenithDelay = computeZenithDelay(height, parameters);
        // mapping function
        final double[] mappingFunction = mappingFactors(height, elevation, date);
        // Tropospheric path delay
        return zenithDelay[0] * mappingFunction[0] + zenithDelay[1] * mappingFunction[1];
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T pathDelay(final T elevation, final T height,
                                                       final T[] parameters, final FieldAbsoluteDate<T> date) {
        // Field
        final Field<T> field = date.getField();
        // zenith delay
        final T[] delays = computeZenithDelay(height, parameters, field);
        // mapping function
        final T[] mappingFunction = mappingFactors(height, elevation, date);
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
    public <T extends RealFieldElement<T>> T[] computeZenithDelay(final T height,
                                                                  final T[] parameters,
                                                                  final Field<T> field) {
        final T[] delay = MathArrays.buildArray(field, 2);
        delay[0] = parameters[0];
        delay[1] = parameters[1];
        return delay;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            dhzParameterDriver,
            dwzParameterDriver
        };
    }

}
