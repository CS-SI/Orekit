/* Copyright 2022-2026 Romain Serra
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.measurements;

import java.util.Arrays;
import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.signal.SignalTravelTimeModel;
import org.orekit.estimation.measurements.signal.TwoLegsSignalTravelTimer;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Class modeling a bistatic measurement using an emitter ground station and a receiver ground station.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted from the emitter ground station</li>
 * <li>Reflected on the spacecraft</li>
 * <li>Received on the receiver ground station</li>
 * </ul>
 * The date of the measurement corresponds to the reception on ground of the reflected signal.
 * <p>
 * The motion of the stations and the spacecraft during the signal flight time are taken into account.
 * </p>
 *
 * @author Romain Serra
 * @since 14.0
 */
public abstract class BistaticRangeRelatedMeasurement<T extends GroundReceiverMeasurement<T>> extends GroundReceiverMeasurement<T> {

    /**
     * Ground station from which transmission is made.
     */
    private final GroundStation emitter;

    /** Two-way signal model .*/
    private final TwoLegsSignalTravelTimer twoLegsSignalTimer;

    /**
     * Simple constructor.
     *
     * @param emitter     ground station from which transmission is performed
     * @param receiver    ground station from which measurement is performed
     * @param twoWay      flag on two-way type
     * @param date        date of the measurement
     * @param value       observed value
     * @param sigma       theoretical standard deviation
     * @param baseWeight  base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite   satellite related to this measurement
     * @since 14.0
     */
    protected BistaticRangeRelatedMeasurement(final GroundStation emitter, final GroundStation receiver,
                                              final boolean twoWay, final AbsoluteDate date,
                                              final double[] value, final double[] sigma, final double[] baseWeight,
                                              final SignalTravelTimeModel signalTravelTimeModel,
                                              final ObservableSatellite satellite) {
        super(receiver, twoWay, date, value, sigma, baseWeight, signalTravelTimeModel, satellite);

        // Add the parameters for the receiver
        addParametersDrivers(emitter.getParametersDrivers());

        // Set emitter
        this.emitter  = emitter;
        this.twoLegsSignalTimer = new TwoLegsSignalTravelTimer(signalTravelTimeModel);
    }

    /**
     * Getter for the two legs timer.
     * @return model
     */
    public TwoLegsSignalTravelTimer getTwoLegsSignalTimer() {
        return twoLegsSignalTimer;
    }

    /** Get the emitter ground station.
     * @return emitter ground station
     */
    public GroundStation getEmitterStation() {
        return emitter;
    }

    /**
     * Method returning the full kinematic coordinates of signal participants at transmission dates.
     * @param state observable state
     * @return signal participants
     */
    protected TimeStampedPVCoordinates[] getParticipants(final SpacecraftState state) {
        // Compute actual reception date
        final AbsoluteDate receptionDate = getCorrectedReceptionDate();

        // Compute light time delays
        final PVCoordinatesProvider receiverPVProvider = getReceiverStation().getPVCoordinatesProvider();
        final Frame frame = state.getFrame();
        final TimeStampedPVCoordinates receiverPV = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final PVCoordinatesProvider satellitePVProvider = MeasurementObject.extractPVCoordinatesProvider(state,
                state.getPVCoordinates());
        final double[] delays = getTwoLegsSignalTimer().computeDelays(frame, receiverPV.getPosition(), receptionDate,
                satellitePVProvider, getEmitterStation().getPVCoordinatesProvider());

        // Form dates
        final AbsoluteDate transitDate = receptionDate.shiftedBy(-delays[1]);
        final AbsoluteDate emissionDate = transitDate.shiftedBy(-delays[0]);

        final double shift = transitDate.durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        return new TimeStampedPVCoordinates[] { emitter.getPVCoordinates(emissionDate, frame),
                transitState.getPVCoordinates(), receiverPV };
    }

    /**
     * Method computing consecutive Field time shifts of participants, starting from observation date.
     * @param states observables
     * @return time shifts
     */
    protected Gradient[] getFieldShifts(final SpacecraftState[] states) {
        // Derivatives are computed with respect to spacecraft state in inertial frame and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(state, 0, nbParams);

        // Compute actual reception date
        final FieldAbsoluteDate<Gradient> receptionDate = getCorrectedReceptionDateField(nbParams, paramIndices);

        // Compute light time delays
        final FieldPVCoordinatesProvider<Gradient> receiverPVProvider = getReceiverStation().getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final TimeStampedFieldPVCoordinates<Gradient> receiverPV = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final FieldPVCoordinatesProvider<Gradient> satellitePVProvider = MeasurementObject.extractFieldPVCoordinatesProvider(state, pva);
        final FieldPVCoordinatesProvider<Gradient> emitterPVProvider = getEmitterStation().getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final Gradient[] delays = getTwoLegsSignalTimer().computeDelays(frame, receiverPV.getPosition(), receptionDate,
                satellitePVProvider, emitterPVProvider);

        return new Gradient[] { receptionDate.durationFrom(getDate()), delays[1].negate(), delays[0].negate() };
    }

    /**
     * Fill estimated measurements with value and derivatives.
     * @param quantity estimated quantity
     * @param paramIndices indices mapping parameter names to derivative indices
     * @param estimated theoretical measurement class
     */
    protected void fillEstimation(final Gradient quantity, final Map<String, Integer> paramIndices,
                                  final EstimatedMeasurement<T> estimated) {
        estimated.setEstimatedValue(quantity.getValue());

        // First order derivatives with respect to state
        final double[] derivatives = quantity.getGradient();
        estimated.setStateDerivatives(0, Arrays.copyOfRange(derivatives, 0, 6));

        // Set first order derivatives with respect to parameters
        for (final ParameterDriver driver : getParametersDrivers()) {
            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = paramIndices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }
    }

}
