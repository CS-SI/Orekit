/* Copyright 2002-2024 CS GROUP
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
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

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
 * The tropospheric zenith delay δ<sub>h</sub> is computed empirically with a
 * {@link DiscreteTroposphericModel tropospheric model}
 * while the tropospheric total zenith delay δ<sub>t</sub> is estimated as a {@link ParameterDriver},
 * hence the wet part is the difference between the two.
 * @deprecated as of 12.1, replaced by {@link EstimatedModel}
 */
@Deprecated
public class EstimatedTroposphericModel extends EstimatedModel implements DiscreteTroposphericModel {

    /** Name of the parameter of this model: the total zenith delay. */
    public static final String TOTAL_ZENITH_DELAY = "total zenith delay";

    /** Build a new instance using the given environmental conditions.
     * <p>
     * This constructor uses a {@link ModifiedSaastamoinenModel} for the hydrostatic contribution.
     * </p>
     * @param t0 the temperature at the station [K]
     * @param p0 the atmospheric pressure at the station [mbar]
     * @param model mapping function model (NMF or GMF).
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     */
    @DefaultDataContext
    public EstimatedTroposphericModel(final double t0, final double p0,
                                      final MappingFunction model, final double totalDelay) {
        super(0.0, t0, p0, new TroposphereMappingFunctionAdapter(model), totalDelay);
    }

    /** Build a new instance using the given environmental conditions.
     * @param hydrostatic model for hydrostatic component
     * @param model mapping function model (NMF or GMF).
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     * @since 12.1
     */
    public EstimatedTroposphericModel(final DiscreteTroposphericModel hydrostatic,
                                      final MappingFunction model,
                                      final double totalDelay) {
        super(new TroposphericModelAdapter(hydrostatic),
              new TroposphereMappingFunctionAdapter(model),
              totalDelay);
    }

    /** Build a new instance using a standard atmosphere model.
     * <ul>
     * <li>temperature: 18 degree Celsius
     * <li>pressure: 1013.25 mbar
     * </ul>
     * @param model mapping function model (NMF or GMF).
     * @param totalDelay initial value for the tropospheric zenith total delay [m]
     */
    @DefaultDataContext
    public EstimatedTroposphericModel(final MappingFunction model, final double totalDelay) {
        this(273.15 + 18.0, 1013.25, model, totalDelay);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public double pathDelay(final double elevation, final GeodeticPoint point,
                            final double[] parameters, final AbsoluteDate date) {
        return pathDelay(new TrackingCoordinates(0.0, elevation, 0.0), point,
                         TroposphericModelUtils.STANDARD_ATMOSPHERE, parameters, date).getDelay();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public <T extends CalculusFieldElement<T>> T pathDelay(final T elevation,
                                                           final FieldGeodeticPoint<T> point,
                                                           final T[] parameters,
                                                           final FieldAbsoluteDate<T> date) {
        return pathDelay(new FieldTrackingCoordinates<>(date.getField().getZero(), elevation, date.getField().getZero()),
                         point,
                         new FieldPressureTemperatureHumidity<>(date.getField(), TroposphericModelUtils.STANDARD_ATMOSPHERE),
                         parameters, date).getDelay();
    }

}
