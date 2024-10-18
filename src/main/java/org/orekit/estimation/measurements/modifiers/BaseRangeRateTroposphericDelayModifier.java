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
package org.orekit.estimation.measurements.modifiers;

import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

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
    private final TroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model  Tropospheric delay model appropriate for the current range-rate measurement method.
     * @since 12.1
     */
    protected BaseRangeRateTroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
    }

    /** Get the name of the effect modifying the measurement.
     * @return name of the effect modifying the measurement
     * @since 13.0
     */
    public String getEffectName() {
        return "troposphere";
    }

    /** Get the tropospheric delay model.
     * @return tropospheric delay model
     */
    protected TroposphericModel getTropoModel() {
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

        // tracking
        final TrackingCoordinates trackingCoordinates1 =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate());

        // only consider measures above the horizon
        if (trackingCoordinates1.getElevation() > 0) {
            // tropospheric delay in meters
            final double d1 = tropoModel.pathDelay(trackingCoordinates1,
                                                   station.getOffsetGeodeticPoint(state.getDate()),
                                                   station.getPressureTemperatureHumidity(state.getDate()),
                                                   tropoModel.getParameters(state.getDate()), state.getDate()).
                              getDelay();

            // propagate spacecraft state forward by dt
            final SpacecraftState state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final Vector3D position2 = state2.getPosition();

            // tracking
            final TrackingCoordinates trackingCoordinates2 =
                            station.getBaseFrame().getTrackingCoordinates(position2, state2.getFrame(), state2.getDate());

            // tropospheric delay dt after
            final double d2 = tropoModel.pathDelay(trackingCoordinates2,
                                                   station.getOffsetGeodeticPoint(state.getDate()),
                                                   station.getPressureTemperatureHumidity(state.getDate()),
                                                   tropoModel.getParameters(state2.getDate()), state2.getDate()).
                              getDelay();

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
        final FieldTrackingCoordinates<T> trackingCoordinates1 =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate());

        // only consider measures above the horizon
        if (trackingCoordinates1.getElevation().getReal() > 0) {
            // tropospheric delay in meters
            final T d1 = tropoModel.pathDelay(trackingCoordinates1,
                                              station.getOffsetGeodeticPoint(state.getDate()),
                                              station.getPressureTemperatureHumidity(state.getDate()),
                                              parameters, state.getDate()).
                         getDelay();

            // propagate spacecraft state forward by dt
            final FieldSpacecraftState<T> state2 = state.shiftedBy(dt);

            // spacecraft position and elevation as seen from the ground station
            final FieldVector3D<T> position2     = state2.getPosition();

            // elevation
            final FieldTrackingCoordinates<T> trackingCoordinates2 =
                            station.getBaseFrame().getTrackingCoordinates(position2, state2.getFrame(), state2.getDate());


            // tropospheric delay dt after
            final T d2 = tropoModel.pathDelay(trackingCoordinates2,
                                              station.getOffsetGeodeticPoint(state.getDate()),
                                              station.getPressureTemperatureHumidity(state.getDate()),
                                              parameters, state2.getDate()).
                         getDelay();

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
