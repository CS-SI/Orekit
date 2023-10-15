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
package org.orekit.estimation.measurements.modifiers;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.models.earth.troposphere.DiscreteTroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

/** Base class modifying theoretical range measurements with tropospheric delay.
 * The effect of tropospheric correction on the range is directly computed
 * through the computation of the tropospheric delay.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Joris Olympio
 * @since 11.2
 */
public abstract class BaseRangeTroposphericDelayModifier {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range measurement method.
     */
    protected BaseRangeTroposphericDelayModifier(final DiscreteTroposphericModel model) {
        tropoModel = model;
    }

    /** Get the tropospheric delay model.
     * @return tropospheric delay model
     */
    protected DiscreteTroposphericModel getTropoModel() {
        return tropoModel;
    }

    /** Compute the measurement error due to Troposphere.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere
     */
    public double rangeErrorTroposphericModel(final GroundStation station,
                                              final SpacecraftState state) {

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPosition();
        final double elevation  =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (elevation > 0) {
            // tropospheric delay in meters
            final double delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(),
                                                      tropoModel.getParameters(), state.getDate());

            return delay;
        }

        return 0;
    }


    /** Compute the measurement error due to Troposphere.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere
     */
    public <T extends CalculusFieldElement<T>> T rangeErrorTroposphericModel(final GroundStation station,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // spacecraft position and elevation as seen from the ground station
        final FieldVector3D<T> position = state.getPosition();
        final T elevation =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (elevation .getReal() > 0) {
            // tropospheric delay in meters
            final T delay = tropoModel.pathDelay(elevation, station.getBaseFrame().getPoint(field),
                                                 parameters, state.getDate());

            return delay;
        }

        return zero;
    }

    /** Get the drivers for this modifier parameters.
     * @return drivers for this modifier parameters
     */
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

}
