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
package org.orekit.estimation.measurements;

import java.util.Map;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.model.TwoLeggedRangeRateModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a bistatic range rate measurement using
 *  an emitter ground station and a receiver ground station.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted from the emitter ground station</li>
 * <li>Reflected on the spacecraft</li>
 * <li>Received on the receiver ground station</li>
 * </ul>
 * The date of the measurement corresponds to the reception on ground of the reflected signal.
 * The quantity measured at the receiver is the bistatic radial velocity as the sum of the radial
 * velocities with respect to the two stations.
 * <p>
 * The motion of the stations and the spacecraft during the signal flight time are taken into account.
 * </p><p>
 * The Doppler measurement can be obtained by multiplying the velocity by (fe/c), where
 * fe is the emission frequency.
 * </p>
 *
 * @author Pascal Parraud
 * @since 11.2
 */
public class BistaticRangeRate extends BistaticRangeRelatedMeasurement<BistaticRangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "BistaticRangeRate";

    /** Range-rate measurement model. */
    private final TwoLeggedRangeRateModel twoLeggedRangeRateModel;

    /** Simple constructor.
     * @param emitter    emitter object
     * @param receiver   receiver object
     * @param date       date of the measurement
     * @param rangeRate  observed value, m/s
     * @param sigma      theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite  satellite related to this measurement
     */
    public BistaticRangeRate(final Observer emitter, final Observer receiver, final AbsoluteDate date,
                             final double rangeRate, final double sigma, final double baseWeight,
                             final ObservableSatellite satellite) {
        this(emitter, receiver, date, rangeRate, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(),
                satellite);
    }

    /** Simple constructor.
     * @param emitter               emitter object
     * @param receiver              receiver object
     * @param date                  date of the measurement
     * @param rangeRate             observed value, m/s
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal travel time model
     * @param satellite             satellite related to this measurement
     * @since 14.0
     */
    public BistaticRangeRate(final Observer emitter, final Observer receiver, final AbsoluteDate date,
                             final double rangeRate, final MeasurementQuality measurementQuality,
                             final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(emitter, receiver, date, new double[] {rangeRate}, measurementQuality, signalTravelTimeModel, satellite);

        // Add class member values
        this.twoLeggedRangeRateModel = new TwoLeggedRangeRateModel(getTwoLeggedSignalTimer());
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<BistaticRangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                                  final int evaluation,
                                                                                                  final SpacecraftState[] states,
                                                                                                  final boolean fillParticipants) {
        // Compute participants (position-velocities at signal transmissions)
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final PVCoordinatesProvider emitterPVProvider = getEmitter().getPVCoordinatesProvider();
        final PVCoordinatesProvider receiverPVProvider = getReceiver().getPVCoordinatesProvider();
        final double[] shifts = getShifts(state, emitterPVProvider, receiverPVProvider);

        // Extract dates
        final AbsoluteDate receptionDate = getDate().shiftedBy(shifts[0]);
        final AbsoluteDate transitDate = receptionDate.shiftedBy(shifts[1]);
        final AbsoluteDate emissionDate = transitDate.shiftedBy(shifts[2]);

        // Prepare the evaluation
        final double shift = transitDate.durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        final TimeStampedPVCoordinates[] participants = fillParticipants ? new TimeStampedPVCoordinates[] { emitterPVProvider.getPVCoordinates(emissionDate, frame),
                transitState.getPVCoordinates(), receiverPVProvider.getPVCoordinates(receptionDate, frame) } : new TimeStampedPVCoordinates[0];
        final EstimatedMeasurementBase<BistaticRangeRate> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState }, participants);

        // Compute range rate
        final PVCoordinatesProvider observablePVCoordinates = AbstractParticipant.extractPVCoordinatesProvider(state,
                state.getPVCoordinates());
        final TimeStampedPVCoordinates receiverPV = fillParticipants ? participants[2] : receiverPVProvider.getPVCoordinates(receptionDate, frame);
        final double rangeRate = twoLeggedRangeRateModel.value(frame, receiverPV, receptionDate,
                observablePVCoordinates, transitDate, emitterPVProvider, emissionDate);

        estimated.setEstimatedValue(rangeRate);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<BistaticRangeRate> theoreticalEvaluation(final int iteration,
                                                                            final int evaluation,
                                                                            final SpacecraftState[] states) {
        // Bistatic range-rate derivatives are computed with respect to spacecraft state in inertial frame
        // and station parameters
        // ----------------------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - measurements parameters (clock offset, station offsets, pole, prime meridian, sat clock offset...)
        // Compute time shifts w.r.t. recorded observation date
        final Gradient[] shifts = getFieldShifts(states);
        final GradientField field = shifts[0].getField();
        final FieldAbsoluteDate<Gradient> receptionDate = new FieldAbsoluteDate<>(field, getDate()).shiftedBy(shifts[0]);
        final FieldAbsoluteDate<Gradient> transitDate = receptionDate.shiftedBy(shifts[1]);
        final FieldAbsoluteDate<Gradient> emissionDate = transitDate.shiftedBy(shifts[2]);

        // Prepare the evaluation
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final double shift = transitDate.toAbsoluteDate().durationFrom(state);
        final SpacecraftState transitState = state.shiftedBy(shift);
        final int nbParams = field.getZero().getFreeParameters();
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final FieldPVCoordinatesProvider<Gradient> receiver = getReceiver().getFieldPVCoordinatesProvider(field.getOne().getFreeParameters(),
                getParameterIndices(states));
        final TimeStampedFieldPVCoordinates<Gradient> receiverPV = receiver.getPVCoordinates(receptionDate, frame);
        final EstimatedMeasurement<BistaticRangeRate> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {
                        getEmitter().getPVCoordinatesProvider().getPVCoordinates(emissionDate.toAbsoluteDate(), frame),
                        transitState.getPVCoordinates(),
                        receiverPV.toTimeStampedPVCoordinates()});

        // Compute range rate
        final FieldPVCoordinatesProvider<Gradient> emitter = getEmitter().getFieldPVCoordinatesProvider(nbParams,
                paramIndices);
        final FieldPVCoordinatesProvider<Gradient> observable = AbstractParticipant.extractFieldPVCoordinatesProvider(state,
                AbstractMeasurement.getCoordinates(state, 0, nbParams));
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                receiverPV.getPosition(), frame);
        final Gradient rangeRate = twoLeggedRangeRateModel.value(receptionCondition, receiverPV.getVelocity(), observable,
                transitDate, emitter, emissionDate);

        fillEstimation(rangeRate, getParameterIndices(states), estimated);
        return estimated;
    }

}
