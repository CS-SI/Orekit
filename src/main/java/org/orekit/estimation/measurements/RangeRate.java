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

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.orekit.estimation.measurements.model.OneLeggedRangeRateModel;
import org.orekit.estimation.measurements.model.TwoLeggedRangeRateModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldAdjustableEmitterSignalTimer;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.signal.TwoLeggedSignalTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling one-way or two-way range rate measurement between two vehicles.
 * One-way range rate (or Doppler) measurements generally apply to specific satellites
 * (e.g. GNSS, DORIS), where a signal is transmitted from a satellite to a sensor.
 * Two-way range rate measurements are applicable to any system. The signal is
 * transmitted to the (non-spinning) satellite and returned by a transponder
 * (or reflected back) to the same measuring sensor.
 * The Doppler measurement can be obtained by multiplying the velocity by (fe/c), where
 * fe is the emission frequency.
 *
 * @author Thierry Ceolin
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRate extends AbstractRangeRelatedMeasurement<RangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "RangeRate";

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoWay if true, this is a two-way measurement
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public RangeRate(final Observer observer, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final boolean twoWay, final ObservableSatellite satellite) {
        this(observer, date, rangeRate, new MeasurementQuality(sigma, baseWeight), twoWay, new SignalTravelTimeModel(),
                satellite);
    }

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param twoWay if true, this is a two-way measurement
     * @param signalTravelTimeModel signal travel model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public RangeRate(final Observer observer, final AbsoluteDate date,
                     final double rangeRate, final MeasurementQuality measurementQuality,
                     final boolean twoWay, final SignalTravelTimeModel signalTravelTimeModel,
                     final ObservableSatellite satellite) {
        super(observer, date, rangeRate, measurementQuality, twoWay, signalTravelTimeModel, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<RangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                          final int evaluation,
                                                                                          final SpacecraftState[] states,
                                                                                          final boolean fillParticipants) {
        // compute reception date
        final double clockOffset = getObserver().getOffsetValue(getDate());
        final AbsoluteDate receptionDate = getDate().shiftedBy(-clockOffset);

        if (isTwoWay()) {
            return twoWayTheoreticalEvaluationWithoutDerivatives(iteration, evaluation, receptionDate, states[0], fillParticipants);
        } else {
            return oneWayTheoreticalEvaluationWithoutDerivatives(iteration, evaluation, receptionDate, states[0], fillParticipants);
        }

    }

    /** Evaluate measurement in two-way without derivatives.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param receptionDate signal final reception date
     * @param state state
     * @param fillParticipants flag to compute and store participants dynamical states at measurement date and along signal path if applicable
     * @return theoretical value
     * @since 14.0
     */
    private EstimatedMeasurementBase<RangeRate> twoWayTheoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                              final int evaluation,
                                                                                              final AbsoluteDate receptionDate,
                                                                                              final SpacecraftState state,
                                                                                              final boolean fillParticipants) {
        EstimatedMeasurementBase<RangeRate> estimated = initializeTwoWayTheoreticalEvaluation(this, iteration, evaluation,
                receptionDate, state, true);

        // Compute range rate
        final TwoLeggedSignalTimer twoLeggedSignalTimer = new TwoLeggedSignalTimer(getSignalTravelTimeModel().getWarmedUpModel());
        final TimeStampedPVCoordinates[] participantsPV = estimated.getParticipants();
        final AbsoluteDate transitDate = participantsPV[1].getDate();
        final AbsoluteDate emissionDate = participantsPV[0].getDate();
        final TwoLeggedRangeRateModel rangeRateModel = new TwoLeggedRangeRateModel(twoLeggedSignalTimer);
        final PVCoordinatesProvider satellitePVProvider = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final double rangeRate = rangeRateModel.value(state.getFrame(), participantsPV[2], receptionDate,
                satellitePVProvider, transitDate, getObserver().getPVCoordinatesProvider(), emissionDate) / 2.;

        if (!fillParticipants) {
            estimated = new EstimatedMeasurementBase<>(estimated.getObservedMeasurement(),
                    iteration, evaluation, estimated.getStates(), new TimeStampedPVCoordinates[0]);
        }
        estimated.setEstimatedValue(rangeRate);
        return estimated;
    }

    /** Evaluate measurement in one-way without derivatives.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param receptionDate signal reception date
     * @param state state
     * @param fillParticipants flag to compute and store participants dynamical states at measurement date and along signal path if applicable
     * @return theoretical value
     * @since 14.0
     */
    private EstimatedMeasurementBase<RangeRate> oneWayTheoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                              final int evaluation,
                                                                                              final AbsoluteDate receptionDate,
                                                                                              final SpacecraftState state,
                                                                                              final boolean fillParticipants) {
        EstimatedMeasurementBase<RangeRate> estimated = initializeOneWayTheoreticalEvaluation(this, iteration, evaluation,
                receptionDate, state, true);

        // physical range rate value
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(getSignalTravelTimeModel().getWarmedUpModel());
        final AbsoluteDate emissionDate = estimated.getParticipants()[0].getDate();
        final PVCoordinates receiverPV = estimated.getParticipants()[1];
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(receptionDate,
                receiverPV.getPosition(), state.getFrame());
        double rangeRate = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(),
                AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates()), emissionDate);

        // clock drifts, taken in account only in case of one way
        final ObservableSatellite satellite    = getSatellites().getFirst();
        final double dtsDot = satellite.getOffsetRate(emissionDate);
        final double dtgDot = getObserver().getOffsetRate(receptionDate);
        final double clockDriftBias = (dtgDot - dtsDot) * Constants.SPEED_OF_LIGHT;
        rangeRate += clockDriftBias;

        if (!fillParticipants) {
            estimated = new EstimatedMeasurementBase<>(estimated.getObservedMeasurement(), iteration, evaluation,
                    estimated.getStates(), new TimeStampedPVCoordinates[0]);
        }
        estimated.setEstimatedValue(rangeRate);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<RangeRate> twoWayTheoreticalEvaluation(final int iteration, final int evaluation,
                                                                          final FieldPVCoordinatesProvider<Gradient> satellitePVProvider,
                                                                          final SpacecraftState state,
                                                                          final Map<String, Integer> indices,
                                                                          final int nbParams) {
        // Compute light time delays
        final Frame frame = state.getFrame();
        final FieldPVCoordinatesProvider<Gradient> observerPVProvider = getObserver().getFieldPVCoordinatesProvider(nbParams, indices);
        final FieldAbsoluteDate<Gradient> receptionDate = getCorrectedReceptionDateField(nbParams, indices);
        final TimeStampedFieldPVCoordinates<Gradient> receiverPV = observerPVProvider.getPVCoordinates(receptionDate, frame);
        final TwoLeggedSignalTimer twoLeggedSignalTimer = new TwoLeggedSignalTimer(getSignalTravelTimeModel());
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                receiverPV.getPosition(), frame);
        final Gradient[] delays = twoLeggedSignalTimer.computeDelays(receptionCondition,
                satellitePVProvider, observerPVProvider);

        // Prepare the evaluation
        final FieldAbsoluteDate<Gradient> transitDate = receptionDate.shiftedBy(delays[1].negate());
        final SpacecraftState transitState = state.shiftedBy(transitDate.toAbsoluteDate().durationFrom(state));
        final FieldAbsoluteDate<Gradient> emissionDate = transitDate.shiftedBy(delays[0].negate());
        final EstimatedMeasurement<RangeRate> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {
                        getObserver().getPVCoordinatesProvider().getPVCoordinates(emissionDate.toAbsoluteDate(), frame),
                        transitState.getPVCoordinates(),
                        receiverPV.toTimeStampedPVCoordinates()});

        // Compute range rate
        final TwoLeggedRangeRateModel rangeRateModel = new TwoLeggedRangeRateModel(twoLeggedSignalTimer);
        final Gradient rangeRate = rangeRateModel.value(receptionCondition, receiverPV.getVelocity(),
                satellitePVProvider, transitDate, observerPVProvider, emissionDate).half();
        fillEstimation(rangeRate, indices, estimated);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation,
                                                                          final FieldPVCoordinatesProvider<Gradient> satellitePVProvider,
                                                                          final SpacecraftState state,
                                                                          final Map<String, Integer> indices,
                                                                          final int nbParams) {
        // compute reception and emission dates
        final FieldAbsoluteDate<Gradient> receptionDate = getCorrectedReceptionDateField(nbParams, indices);
        final Frame frame = state.getFrame();
        final Field<Gradient> field = receptionDate.getField();
        final FieldAdjustableEmitterSignalTimer<Gradient> adjustableEmitter = getSignalTravelTimeModel().getFieldAdjustableEmitterComputer(
                field, satellitePVProvider);
        final FieldPVCoordinatesProvider<Gradient> observer = getObserver().getFieldPVCoordinatesProvider(nbParams, indices);
        final TimeStampedFieldPVCoordinates<Gradient> observerPVAtReception = observer.getPVCoordinates(receptionDate, frame);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                observerPVAtReception.getPosition(), frame);
        final Gradient delay = adjustableEmitter.computeDelay(receptionCondition);

        // prepare the evaluation
        final FieldAbsoluteDate<Gradient> emissionDate = receptionDate.shiftedBy(delay.negate());
        final SpacecraftState emissionState = state.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(state));
        final EstimatedMeasurement<RangeRate> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { emissionState }, new TimeStampedPVCoordinates[] {
                        emissionState.getPVCoordinates(), observerPVAtReception.toTimeStampedPVCoordinates() });

        // physical range rate value
        final SignalTravelTimeModel warmedUpModel = getSignalTravelTimeModel().getWarmedUpModel();
        final OneLeggedRangeRateModel rangeRateModel = new OneLeggedRangeRateModel(warmedUpModel);
        Gradient rangeRate = rangeRateModel.value(receptionCondition, observerPVAtReception.getVelocity(),
                satellitePVProvider, emissionDate);

        // clock drifts, taken in account only in case of one way
        final ObservableSatellite satellite    = getSatellites().getFirst();
        final Gradient dtsDot = satellite.getFieldOffsetRate(nbParams, emissionState.getDate(), indices);
        final Gradient dtgDot = getObserver().getFieldOffsetRate(nbParams, receptionDate.toAbsoluteDate(), indices);
        final Gradient clockDriftBias = dtgDot.subtract(dtsDot).multiply(Constants.SPEED_OF_LIGHT);
        rangeRate = rangeRate.add(clockDriftBias);

        fillEstimation(rangeRate, indices, estimated);
        return estimated;
    }

}
