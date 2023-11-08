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

/** Baselass modifying theoretical range-rate measurements with tropospheric delay.
 * The effect of tropospheric correction on the range-rate is directly computed
 * through the computation of the tropospheric delay difference with respect to
 * time.
 *
 * In general, for GNSS, VLBI, ... there is hardly any frequency dependence in the delay.
 * For SLR techniques however, the frequency dependence is sensitive.
 *
 * @author Joris Olympio
 * @since 11.2
 */
public abstract class BaseRangeRateTroposphericDelayModifier {

    /** Tropospheric delay model. */
    private final DiscreteTroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range-rate measurement method.
     */
    protected BaseRangeRateTroposphericDelayModifier(final DiscreteTroposphericModel model) {
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
    public double rangeRateErrorTroposphericModel(final GroundStation station,
                                                  final SpacecraftState state) {
        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        // spacecraft position and elevation as seen from the ground station
        final Vector3D position = state.getPosition();

        // elevation
        final double elevation1 =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (elevation1 > 0) {
            // tropospheric delay in meters
            final double d1 = tropoModel.pathDelay(elevation1, station.getBaseFrame().getPoint(), tropoModel.getParameters(state.getDate()), state.getDate());

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPosition();

            // elevation
            final double elevation2 =
                            station.getBaseFrame().getTrackingCoordinates(position2, state2.getFrame(), state2.getDate()).
                            getElevation();

            // tropospheric delay dt after
            final double d2 = tropoModel.pathDelay(elevation2, station.getBaseFrame().getPoint(), tropoModel.getParameters(state2.getDate()), state2.getDate());

            return (d2 - d1) / dt;
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
    public <T extends CalculusFieldElement<T>> T rangeRateErrorTroposphericModel(final GroundStation station,
                                                                                 final FieldSpacecraftState<T> state,
                                                                                 final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // The effect of tropospheric correction on the range rate is
        // computed using finite differences.

        final double dt = 10; // s

        // spacecraft position and elevation as seen from the ground station
        final FieldVector3D<T> position     = state.getPosition();
        final T elevation1 =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate()).
                        getElevation();

        // only consider measures above the horizon
        if (elevation1.getReal() > 0) {
            // tropospheric delay in meters
            final T d1 = tropoModel.pathDelay(elevation1, station.getBaseFrame().getPoint(field), parameters, state.getDate());

            // propagate spacecraft state forward by dt
            final FieldSpacecraftState<T> state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final FieldVector3D<T> position2     = state2.getPosition();

            // elevation
            final T elevation2 =
                            station.getBaseFrame().getTrackingCoordinates(position2, state2.getFrame(), state2.getDate()).
                            getElevation();


            // tropospheric delay dt after
            final T d2 = tropoModel.pathDelay(elevation2, station.getBaseFrame().getPoint(field), parameters, state2.getDate());

            return d2.subtract(d1).divide(dt);
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
