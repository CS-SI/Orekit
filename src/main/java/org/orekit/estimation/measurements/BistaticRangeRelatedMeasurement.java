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
import java.util.Collections;
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
 * Class modeling a bistatic measurement using an emitting observer and a receiving observer.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted from the emitting observer</li>
 * <li>Reflected on the spacecraft</li>
 * <li>Received on the receiving observer</li>
 * </ul>
 * The date of the measurement corresponds to the reception of the reflected signal.
 * <p>
 * The motion of the observers and the spacecraft during the signal flight time are taken into account.
 * </p>
 *
 * @author Romain Serra
 * @since 14.0
 */
abstract class BistaticRangeRelatedMeasurement<T extends AbstractMeasurement<T>> extends AbstractMeasurement<T> {

    /**
     * Observer from which emission is made.
     */
    private final Observer emitter;

    /**
     * Observer that makes measurement value.
     */
    private final Observer receiver;

    /** Two-way signal model .*/
    private final TwoLegsSignalTravelTimer twoLegsSignalTimer;

    /**
     * Simple constructor.
     *
     * @param emitter     observer from which transmission is performed
     * @param receiver    observer from which measurement is performed
     * @param date        date of the measurement
     * @param value       observed value
     * @param sigma       theoretical standard deviation
     * @param baseWeight  base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite   satellite related to this measurement
     * @since 14.0
     */
    protected BistaticRangeRelatedMeasurement(final Observer emitter, final Observer receiver,
                                              final AbsoluteDate date,
                                              final double[] value, final double[] sigma, final double[] baseWeight,
                                              final SignalTravelTimeModel signalTravelTimeModel,
                                              final ObservableSatellite satellite) {
        super(date, true, value, sigma, baseWeight, signalTravelTimeModel, Collections.singletonList(satellite));

        // Add the parameters for the receiver
        addParametersDrivers(emitter.getParametersDrivers());
        addParametersDrivers(receiver.getParametersDrivers());

        // Set emitter
        this.emitter  = emitter;
        this.receiver = receiver;
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
     * @deprecated as of 14.0, replaced by {@link #getEmitter()}
     */
    @Deprecated
    public GroundStation getEmitterStation() {
        if (!(emitter instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) emitter;
    }

    /** Get the emitter object.
     * @return emitter object
     * @since 14.0
     */
    public Observer getEmitter() {
        return emitter;
    }

    /** Get the receiver ground station.
     * @return receiver ground station
     * @deprecated as of 14.0, replaced by {@link #getReceiver()}
     */
    @Deprecated
    public GroundStation getReceiverStation() {
        if (!(receiver instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) receiver;
    }

    /** Get the receiver object.
     * @return receiver object
     * @since 14.0
     */
    public Observer getReceiver() {
        return receiver;
    }

    /**
     * Method returning the full kinematic coordinates of signal participants at transmission dates.
     * @param state observable state
     * @return signal participants
     */
    protected TimeStampedPVCoordinates[] getParticipants(final SpacecraftState state) {
        // Compute actual reception date
        final AbsoluteDate receptionDate = getReceiver().getCorrectedReceptionDate(getDate());

        // Compute light time delays
        final PVCoordinatesProvider receiverPVProvider = getReceiver().getPVCoordinatesProvider();
        final Frame frame = state.getFrame();
        final TimeStampedPVCoordinates receiverPV = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final PVCoordinatesProvider satellitePVProvider = AbstractMeasurementObject.extractPVCoordinatesProvider(state,
                state.getPVCoordinates());
        final double[] delays = getTwoLegsSignalTimer().computeDelays(frame, receiverPV.getPosition(), receptionDate,
                satellitePVProvider, getEmitter().getPVCoordinatesProvider());

        // Form dates
        final AbsoluteDate transitDate = receptionDate.shiftedBy(-delays[1]);
        final AbsoluteDate emissionDate = transitDate.shiftedBy(-delays[0]);

        final double shift = transitDate.durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        return new TimeStampedPVCoordinates[] { emitter.getPVCoordinatesProvider().getPVCoordinates(emissionDate, frame),
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
        final FieldAbsoluteDate<Gradient> receptionDate = getReceiver().getCorrectedReceptionDateField(getDate(), nbParams, paramIndices);

        // Compute light time delays
        final FieldPVCoordinatesProvider<Gradient> receiverPVProvider = getReceiver().getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final TimeStampedFieldPVCoordinates<Gradient> receiverPV = receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final FieldPVCoordinatesProvider<Gradient> satellitePVProvider = AbstractMeasurementObject.extractFieldPVCoordinatesProvider(state, pva);
        final FieldPVCoordinatesProvider<Gradient> emitterPVProvider = getEmitter().getFieldPVCoordinatesProvider(nbParams, paramIndices);
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
