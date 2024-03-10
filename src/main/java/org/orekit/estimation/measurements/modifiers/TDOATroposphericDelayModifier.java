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
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TrackingCoordinates;

/** Class modifying theoretical TDOA measurements with tropospheric delay.
 * <p>
 * The effect of tropospheric correction on the TDOA is a time delay computed
 * directly from the difference in tropospheric delays for each downlink.
 * </p><p>
 * Tropospheric delay is not frequency dependent for signals up to 15 GHz.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOATroposphericDelayModifier implements EstimationModifier<TDOA> {

    /** Tropospheric delay model. */
    private final TroposphericModel tropoModel;

    /** Constructor.
     *
     * @param model tropospheric model appropriate for the current TDOA measurement method.
     * @deprecated as of 12.1, replaced by {@link #TDOATroposphericDelayModifier(TroposphericModel)}
     */
    @Deprecated
    public TDOATroposphericDelayModifier(final org.orekit.models.earth.troposphere.DiscreteTroposphericModel model) {
        this(new org.orekit.models.earth.troposphere.TroposphericModelAdapter(model));
    }

    /** Constructor.
     *
     * @param model tropospheric model appropriate for the current TDOA measurement method.
     * @since 12.1
     */
    public TDOATroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
    }

    /** Compute the measurement error due to Troposphere on a single downlink.
     * @param station station
     * @param state spacecraft state
     * @return the measurement error due to Troposphere (s)
     */
    private double timeErrorTroposphericModel(final GroundStation station, final SpacecraftState state) {
        final Vector3D position = state.getPosition();

        // tracking
        final TrackingCoordinates trackingCoordinates =
                        station.getBaseFrame().getTrackingCoordinates(position, state.getFrame(), state.getDate());

        // only consider measurements above the horizon
        if (trackingCoordinates.getElevation() > 0) {
            // Delay in meters
            final double delay = tropoModel.pathDelay(trackingCoordinates,
                                                      station.getOffsetGeodeticPoint(state.getDate()),
                                                      station.getPressureTemperatureHumidity(state.getDate()),
                                                      tropoModel.getParameters(state.getDate()), state.getDate()).
                                 getDelay();
            // return delay in seconds
            return delay / Constants.SPEED_OF_LIGHT;
        }

        return 0;
    }

    /** Compute the measurement error due to Troposphere on a single downlink.
     * @param <T> type of the element
     * @param station station
     * @param state spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere (s)
     */
    private <T extends CalculusFieldElement<T>> T timeErrorTroposphericModel(final GroundStation station,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {
        // Field
        final Field<T> field = state.getDate().getField();
        final T zero         = field.getZero();

        // tracking
        final FieldVector3D<T> pos = state.getPosition();
        final FieldTrackingCoordinates<T> trackingCoordinates =
                        station.getBaseFrame().getTrackingCoordinates(pos, state.getFrame(), state.getDate());

        // only consider measurements above the horizon
        if (trackingCoordinates.getElevation().getReal() > 0) {
            // delay in meters
            final T delay = tropoModel.pathDelay(trackingCoordinates,
                                                 station.getOffsetGeodeticPoint(state.getDate()),
                                                 station.getPressureTemperatureHumidity(state.getDate()),
                                                 parameters, state.getDate()).
                            getDelay();
            // return delay in seconds
            return delay.divide(Constants.SPEED_OF_LIGHT);
        }

        return zero;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<TDOA> estimated) {

        final TDOA            measurement   = estimated.getObservedMeasurement();
        final GroundStation   primeStation  = measurement.getPrimeStation();
        final GroundStation   secondStation = measurement.getSecondStation();

        TDOAModifierUtil.modifyWithoutDerivatives(estimated,  primeStation, secondStation,
                                                  this::timeErrorTroposphericModel, this);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<TDOA> estimated) {

        final TDOA            measurement   = estimated.getObservedMeasurement();
        final GroundStation   primeStation  = measurement.getPrimeStation();
        final GroundStation   secondStation = measurement.getSecondStation();
        final SpacecraftState state         = estimated.getStates()[0];

        TDOAModifierUtil.modify(estimated, tropoModel,
                                new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                primeStation, secondStation,
                                this::timeErrorTroposphericModel,
                                this::timeErrorTroposphericModel,
                                this);

    }

}
