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
import org.orekit.estimation.measurements.model.OneLegRangeRateModel;
import org.orekit.estimation.measurements.model.TwoLeggedRangeRateModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.signal.TwoLeggedSignalTravelTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
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
        this(observer, date, rangeRate, sigma, baseWeight, twoWay, new SignalTravelTimeModel(), satellite);
    }

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param date date of the measurement
     * @param rangeRate observed value, m/s
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param twoWay if true, this is a two-way measurement
     * @param signalTravelTimeModel signal travel model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public RangeRate(final Observer observer, final AbsoluteDate date,
                     final double rangeRate, final double sigma, final double baseWeight,
                     final boolean twoWay, final SignalTravelTimeModel signalTravelTimeModel,
                     final ObservableSatellite satellite) {
        super(observer, date, rangeRate, sigma, baseWeight, twoWay, signalTravelTimeModel, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<RangeRate> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                          final int evaluation,
                                                                                          final SpacecraftState[] states) {
        // compute reception date
        final double clockOffset = getObserver().getClockOffsetDriver().getValue(getDate());  // FIXME see Field
        final AbsoluteDate receptionDate = getDate().shiftedBy(-clockOffset);

        if (isTwoWay()) {
            return twoWayTheoreticalEvaluationWithoutDerivatives(iteration, evaluation, receptionDate, states[0]);
        } else {
            return oneWayTheoreticalEvaluationWithoutDerivatives(iteration, evaluation, receptionDate, states[0]);
        }

    }

    /** Evaluate measurement in two-way without derivatives.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param receptionDate signal final reception date
     * @param state state
     * @return theoretical value
     * @since 14.0
     */
    private EstimatedMeasurementBase<RangeRate> twoWayTheoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                              final int evaluation,
                                                                                              final AbsoluteDate receptionDate,
                                                                                              final SpacecraftState state) {
        final EstimatedMeasurementBase<RangeRate> estimated = initializeTwoWayTheoreticalEvaluation(this, iteration, evaluation,
                receptionDate, state);

        // Compute range rate
        final TwoLeggedSignalTravelTimer twoLeggedSignalTravelTimer = new TwoLeggedSignalTravelTimer(getSignalTravelTimeModel().getWarmedUpModel());
        final TimeStampedPVCoordinates[] participantsPV = estimated.getParticipants();
        final AbsoluteDate transitDate = participantsPV[1].getDate();
        final AbsoluteDate emissionDate = participantsPV[0].getDate();
        final TwoLeggedRangeRateModel rangeRateModel = new TwoLeggedRangeRateModel(twoLeggedSignalTravelTimer);
        final PVCoordinatesProvider satellitePVProvider = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final double rangeRate = rangeRateModel.value(state.getFrame(), participantsPV[2], receptionDate,
                satellitePVProvider, transitDate, getObserver().getPVCoordinatesProvider(), emissionDate) / 2.;
        estimated.setEstimatedValue(rangeRate);
        return estimated;
    }

    /** Evaluate measurement in one-way without derivatives.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param receptionDate signal reception date
     * @param state state
     * @return theoretical value
     * @since 14.0
     */
    private EstimatedMeasurementBase<RangeRate> oneWayTheoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                              final int evaluation,
                                                                                              final AbsoluteDate receptionDate,
                                                                                              final SpacecraftState state) {
        final EstimatedMeasurementBase<RangeRate> estimated = initializeOneWayTheoreticalEvaluation(this, iteration, evaluation,
                receptionDate, state);

        // physical range rate value
        final OneLegRangeRateModel rangeRateModel = new OneLegRangeRateModel(getSignalTravelTimeModel().getWarmedUpModel());
        final AbsoluteDate emissionDate = estimated.getParticipants()[0].getDate();
        double rangeRate = rangeRateModel.value(state.getFrame(), estimated.getParticipants()[1], receptionDate,
                AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates()), emissionDate);

        // clock drifts, taken in account only in case of one way
        final ObservableSatellite satellite    = getSatellites().get(0);
        final double              dtsDot       = satellite.getClockDriftDriver().getValue(emissionDate);
        final double              dtgDot       = getObserver().getClockDriftDriver().getValue(receptionDate);
        final double clockDriftBias = (dtgDot - dtsDot) * Constants.SPEED_OF_LIGHT;
        rangeRate += clockDriftBias;

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
        final TwoLeggedSignalTravelTimer twoLeggedSignalTravelTimer = new TwoLeggedSignalTravelTimer(getSignalTravelTimeModel());
        final Gradient[] delays = twoLeggedSignalTravelTimer.computeDelays(frame, receiverPV.getPosition(), receptionDate,
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
        final TwoLeggedRangeRateModel rangeRateModel = new TwoLeggedRangeRateModel(twoLeggedSignalTravelTimer);
        final Gradient rangeRate = rangeRateModel.value(frame, receiverPV,
                receptionDate, satellitePVProvider, transitDate, observerPVProvider, emissionDate).half();
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
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> adjustableEmitter = getSignalTravelTimeModel().getFieldAdjustableEmitterComputer(
                field, satellitePVProvider);
        final TimeStampedFieldPVCoordinates<Gradient> observerPVAtReception = getObserver().getFieldPVCoordinatesProvider(nbParams, indices)
                .getPVCoordinates(receptionDate, frame);
        final Gradient delay = adjustableEmitter.computeDelay(observerPVAtReception.getPosition(), receptionDate, frame);

        // prepare the evaluation
        final FieldAbsoluteDate<Gradient> emissionDate = receptionDate.shiftedBy(delay.negate());
        final SpacecraftState emissionState = state.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(state));
        final EstimatedMeasurement<RangeRate> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { emissionState }, new TimeStampedPVCoordinates[] {
                        emissionState.getPVCoordinates(), observerPVAtReception.toTimeStampedPVCoordinates() });

        // physical range rate value
        final OneLegRangeRateModel rangeRateModel = new OneLegRangeRateModel(getSignalTravelTimeModel());
        Gradient rangeRate = rangeRateModel.value(frame, observerPVAtReception, receptionDate, satellitePVProvider,
                emissionDate);

        // clock drifts, taken in account only in case of one way
        final ObservableSatellite satellite    = getSatellites().get(0);
        final Gradient            dtsDot       = satellite.getClockDriftDriver().getValue(nbParams, indices, emissionState.getDate());
        final Gradient            dtgDot       = getObserver().getClockDriftDriver().getValue(nbParams, indices, receptionDate.toAbsoluteDate());
        final Gradient clockDriftBias = dtgDot.subtract(dtsDot).multiply(Constants.SPEED_OF_LIGHT);
        rangeRate = rangeRate.add(clockDriftBias);

        fillEstimation(rangeRate, indices, estimated);
        return estimated;
    }

}
