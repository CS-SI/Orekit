/* Copyright 2002-2026 CS GROUP
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
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundObserver;
import org.orekit.estimation.measurements.Observer;
import org.orekit.estimation.measurements.TDOA;
import org.orekit.models.earth.troposphere.TroposphericModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.FieldAbsoluteDate;
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
     * @since 12.1
     */
    public TDOATroposphericDelayModifier(final TroposphericModel model) {
        tropoModel = model;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "troposphere";
    }

    /** Compute the measurement error due to Troposphere on a single downlink.
     * @param observer object that observes signal
     * @param state    estimated spacecraft state
     * @return the measurement error due to Troposphere (s)
     */
    private double timeErrorTroposphericModel(final Observer observer, final SpacecraftState state) {

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {

            // tracking
            final TrackingCoordinates trackingCoordinates = groundObserver.getTrackingCoordinates(state);

            // only consider measurements above the horizon
            if (trackingCoordinates.getElevation() > 0) {
                // Delay in meters
                final double delay = tropoModel.pathDelay(trackingCoordinates, groundObserver.getOffsetGeodeticPoint(state.getDate()),
                                tropoModel.getParameters(state.getDate()), state.getDate()).
                        getDelay();
                // return delay in seconds
                return delay / Constants.SPEED_OF_LIGHT;
            }

            return 0;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }

    /** Compute the measurement error due to Troposphere on a single downlink.
     * @param <T>        type of the element
     * @param observer   object that observes signal
     * @param state      estimated spacecraft state
     * @param parameters tropospheric model parameters
     * @return the measurement error due to Troposphere (s)
     */
    private <T extends CalculusFieldElement<T>> T timeErrorTroposphericModel(final Observer observer,
                                                                             final FieldSpacecraftState<T> state,
                                                                             final T[] parameters) {

        // Currently not calculating tropospheric delays for this type of observer
        if (observer instanceof GroundObserver groundObserver) {

            // Field
            final FieldAbsoluteDate<T> date = state.getDate();
            final Field<T> field = date.getField();
            final T zero = field.getZero();

            // tracking
            final FieldTrackingCoordinates<T> trackingCoordinates = groundObserver.getTrackingCoordinates(state);

            // only consider measurements above the horizon
            if (trackingCoordinates.getElevation().getReal() > 0) {
                // delay in meters
                final T delay = tropoModel.pathDelay(trackingCoordinates, groundObserver.getOffsetGeodeticPoint(date),
                                parameters, date).
                        getDelay();
                // return delay in seconds
                return delay.divide(Constants.SPEED_OF_LIGHT);
            }

            return zero;
        } else {
            throw new OrekitException(OrekitMessages.WRONG_OBSERVER_TYPE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return tropoModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<TDOA> estimated) {

        final TDOA     measurement    = estimated.getObservedMeasurement();
        final Observer primeObserver  = measurement.getPrimeObserver();
        final Observer secondObserver = measurement.getSecondObserver();

        TDOAModifierUtil.modifyWithoutDerivatives(estimated,  primeObserver, secondObserver,
                                                  this::timeErrorTroposphericModel, this);

    }

    /** {@inheritDoc} */
    @Override
    public void modify(final EstimatedMeasurement<TDOA> estimated) {

        final TDOA            measurement    = estimated.getObservedMeasurement();
        final Observer        primeObserver  = measurement.getPrimeObserver();
        final Observer        secondObserver = measurement.getSecondObserver();
        final SpacecraftState state          = estimated.getStates()[0];

        TDOAModifierUtil.modify(estimated, tropoModel,
                                new ModifierGradientConverter(state, 6, new FrameAlignedProvider(state.getFrame())),
                                primeObserver, secondObserver,
                                this::timeErrorTroposphericModel,
                                this::timeErrorTroposphericModel,
                                this);

    }

}
