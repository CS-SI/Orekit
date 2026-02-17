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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.estimation.measurements.model.OneLegRangeRateModel;
import org.orekit.estimation.measurements.model.TwoLeggedRangeRateModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.signal.TwoLeggedSignalTravelTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling one-way or two-way range rate measurement between two vehicles.
 * One-way range rate (or Doppler) measurements generally apply to specific satellites
 * (e.g. GNSS, DORIS), where a signal is transmitted from a satellite to a
 * measuring station.
 * Two-way range rate measurements are applicable to any system. The signal is
 * transmitted to the (non-spinning) satellite and returned by a transponder
 * (or reflected back) to the same measuring station.
 * The Doppler measurement can be obtained by multiplying the velocity by (fe/c), where
 * fe is the emission frequency.
 *
 * @author Thierry Ceolin
 * @author Joris Olympio
 * @since 8.0
 */
public class RangeRate extends SignalBasedMeasurement<RangeRate> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "RangeRate";

    /** Ground station that receives signal from satellite. */
    private final Observer observer;

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
        super(date, twoWay, new double[] {rangeRate}, new double[] {sigma}, new double[] {baseWeight},
                signalTravelTimeModel, Collections.singletonList(satellite));
        addParametersDrivers(observer.getParametersDrivers());
        this.observer = observer;
    }

    /** Get receiving ground station.
     * @return measurement ground station
     * @deprecated as of 14.0, replaced by {@link #getObserver()}
     */
    @Deprecated
    public final GroundStation getStation() {
        if (!(observer instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) observer;
    }

    /** Get receiving object.
     * @return measurement observer
     * @since 14.0
     */
    public final Observer getObserver() {
        return observer;
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

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<RangeRate> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                    final SpacecraftState[] states) {
        // Range-rate derivatives are computed with respect to spacecraft state in inertial frame
        // and station position in station's offset frame
        // -------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - station parameters (clock offset, clock drift, station offsets, pole, prime meridian...)
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();
        final SpacecraftState state = states[0];
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(state, 0, nbParams);
        final FieldPVCoordinatesProvider<Gradient> observablePVProvider = AbstractParticipant
                .extractFieldPVCoordinatesProvider(state, pva);

        if (isTwoWay()) {
            return twoWayTheoreticalEvaluation(iteration, evaluation, observablePVProvider, state, paramIndices, nbParams);
        } else {
            return oneWayTheoreticalEvaluation(iteration, evaluation, observablePVProvider, state, paramIndices, nbParams);
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
        // Compute light time delays
        final Frame frame = state.getFrame();
        final PVCoordinatesProvider observerPVProvider = getObserver().getPVCoordinatesProvider();
        final TimeStampedPVCoordinates receiverPV = observerPVProvider.getPVCoordinates(receptionDate, frame);
        final PVCoordinatesProvider satellitePVProvider = AbstractParticipant.extractPVCoordinatesProvider(state,
                state.getPVCoordinates());
        final TwoLeggedSignalTravelTimer twoLeggedSignalTravelTimer = new TwoLeggedSignalTravelTimer(getSignalTravelTimeModel());
        final double[] delays = twoLeggedSignalTravelTimer.computeDelays(frame, receiverPV.getPosition(), receptionDate,
                satellitePVProvider, observerPVProvider);

        // Prepare estimation
        final AbsoluteDate transitDate = receptionDate.shiftedBy(-delays[1]);
        final AbsoluteDate emissionDate = transitDate.shiftedBy(-delays[0]);
        final SpacecraftState transitState = state.shiftedBy(transitDate.durationFrom(state));
        final TimeStampedPVCoordinates emissionPV = observerPVProvider.getPVCoordinates(emissionDate, frame);
        final EstimatedMeasurementBase<RangeRate> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {emissionPV, transitState.getPVCoordinates(), receiverPV});

        // Compute range rate
        final double rangeRate = new TwoLeggedRangeRateModel(twoLeggedSignalTravelTimer).value(frame, receiverPV, receptionDate,
                satellitePVProvider, transitDate, observerPVProvider, emissionDate) / 2.;

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
        // compute light time delay
        final Frame frame = state.getFrame();
        final PVCoordinatesProvider observablePVProvider = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final SignalTravelTimeAdjustableEmitter adjustableEmitter = getSignalTravelTimeModel()
                .getAdjustableEmitterComputer(observablePVProvider);
        final TimeStampedPVCoordinates stationPVAtReception = getObserver().getPVCoordinatesProvider()
                .getPVCoordinates(receptionDate, frame);
        final double delay = adjustableEmitter.computeDelay(stationPVAtReception.getPosition(), receptionDate, frame);

        // prepare the evaluation
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(-delay);
        final SpacecraftState emissionState = state.shiftedBy(emissionDate.durationFrom(state));
        final EstimatedMeasurementBase<RangeRate> estimated = new EstimatedMeasurementBase<>(this, iteration, evaluation,
                new SpacecraftState[] { emissionState },
                new TimeStampedPVCoordinates[] { emissionState.getPVCoordinates(), stationPVAtReception });

        // physical range rate value
        final OneLegRangeRateModel rangeRateModel = new OneLegRangeRateModel(getSignalTravelTimeModel());
        double rangeRate = rangeRateModel.value(frame, stationPVAtReception, receptionDate, observablePVProvider,
                emissionDate);

        // clock drifts, taken in account only in case of one way
        final ObservableSatellite satellite    = getSatellites().get(0);
        final double              dtsDot       = satellite.getClockDriftDriver().getValue(emissionState.getDate());
        final double              dtgDot       = getObserver().getClockDriftDriver().getValue(receptionDate);
        final double clockDriftBias = (dtgDot - dtsDot) * Constants.SPEED_OF_LIGHT;
        rangeRate += clockDriftBias;

        estimated.setEstimatedValue(rangeRate);
        return estimated;
    }

    /** Evaluate measurement in two-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param satellitePVProvider coordinates provider of observable for automatic differentiation
     * @param state observable state
     * @param indices indices of the estimated parameters in derivatives computations
     * @param nbParams the number of estimated parameters in derivative computations
     * @return theoretical value
     * @since 14.0
     */
    private EstimatedMeasurement<RangeRate> twoWayTheoreticalEvaluation(final int iteration, final int evaluation,
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
        final Gradient rangeRate = new TwoLeggedRangeRateModel(twoLeggedSignalTravelTimer).value(frame, receiverPV,
                receptionDate, satellitePVProvider, transitDate, observerPVProvider, emissionDate).half();

        fillEstimation(rangeRate, indices, estimated);
        return estimated;
    }

    /** Evaluate measurement in one-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param satellitePVProvider coordinates provider of observable for automatic differentiation
     * @param state observable state
     * @param indices indices of the estimated parameters in derivatives computations
     * @param nbParams the number of estimated parameters in derivative computations
     * @return theoretical value
     * @since 14.0
     */
    private EstimatedMeasurement<RangeRate> oneWayTheoreticalEvaluation(final int iteration, final int evaluation,
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
        final TimeStampedFieldPVCoordinates<Gradient> stationPVAtReception = getObserver().getFieldPVCoordinatesProvider(nbParams, indices)
                .getPVCoordinates(receptionDate, frame);
        final Gradient delay = adjustableEmitter.computeDelay(stationPVAtReception.getPosition(), receptionDate, frame);

        // prepare the evaluation
        final FieldAbsoluteDate<Gradient> emissionDate = receptionDate.shiftedBy(delay.negate());
        final SpacecraftState emissionState = state.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(state));
        final EstimatedMeasurement<RangeRate> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { emissionState }, new TimeStampedPVCoordinates[] {
                        emissionState.getPVCoordinates(), stationPVAtReception.toTimeStampedPVCoordinates() });

        // physical range rate value
        final OneLegRangeRateModel rangeRateModel = new OneLegRangeRateModel(getSignalTravelTimeModel());
        Gradient rangeRate = rangeRateModel.value(frame, stationPVAtReception, receptionDate, satellitePVProvider,
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

    /**
     * Compute actual reception date taking into account clock offset.
     * @param nbParams number of independent variables for automatic differentiation
     * @param paramIndices mapping between parameter name and variable index
     * @return reception date
     * @since 14.0
     */
    private FieldAbsoluteDate<Gradient> getCorrectedReceptionDateField(final int nbParams,
                                                                       final Map<String, Integer> paramIndices) {
        final Gradient offset = getObserver().getClockOffsetDriver().getValue(nbParams, paramIndices, getDate());  // FIXME missing drift and quatratic term
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams), getDate());
        return fieldDate.shiftedBy(offset.negate());
    }

    /**
     * Fill estimated measurements with value and derivatives.
     * @param quantity estimated quantity
     * @param paramIndices indices mapping parameter names to derivative indices
     * @param estimated theoretical measurement class
     * @since 14.0
     */
    private void fillEstimation(final Gradient quantity, final Map<String, Integer> paramIndices,
                                final EstimatedMeasurement<RangeRate> estimated) {
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
