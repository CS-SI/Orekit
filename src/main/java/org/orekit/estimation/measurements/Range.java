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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldAdjustableEmitterSignalTimer;
import org.orekit.signal.FieldSignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.signal.TwoLeggedSignalTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a range measurement received by an observer.
 * <p>
 * For one-way measurements, a signal is emitted by the satellite
 * and received by the observer. The measurement value is the
 * elapsed time between emission and reception multiplied by c where
 * c is the speed of light.
 * </p>
 * <p>
 * For two-way measurements, the measurement is considered to be a signal
 * emitted from a observer, reflected on spacecraft, and received
 * on the same observer. Its value is the elapsed time between
 * emission and reception multiplied by c/2 where c is the speed of light.
 * </p>
 * <p>
 * The motion of both the sensor and the spacecraft during the signal
 * flight time are taken into account. The date of the measurement
 * corresponds to the reception on ground of the emitted or reflected signal.
 * </p>
 * <p>
 * The clock offsets of both the observer and the satellite are taken
 * into account. These offsets correspond to the values that must be subtracted
 * from sensor (resp. satellite) reading of time to compute the real physical
 * date. These offsets have two effects:
 * </p>
 * <ul>
 *   <li>as measurement date is evaluated at reception time, the real physical date
 *   of the measurement is the observed date to which the receiving observer
 *   clock offset is subtracted</li>
 *   <li>as range is evaluated using the total signal time of flight, for one-way
 *   measurements the observed range is the real physical signal time of flight to
 *   which (Δtg - Δts) ⨯ c is added, where Δtg (resp. Δts) is the clock offset for the
 *   receiving observer (resp. emitting satellite). A similar effect exists in
 *   two-way measurements but it is computed as (Δtg - Δtg) ⨯ c / 2 as the same clock
 *   is used for initial emission and final reception and therefore it evaluates
 *   to zero.</li>
 * </ul>
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 8.0
 */
public class Range extends AbstractRangeRelatedMeasurement<Range> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "Range";

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 9.3
     */
    public Range(final Observer observer, final boolean twoWay, final AbsoluteDate date,
                 final double range, final double sigma, final double baseWeight,
                 final ObservableSatellite satellite) {
        super(observer, date, range, new MeasurementQuality(sigma, baseWeight), twoWay, new SignalTravelTimeModel(), satellite);
    }

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param range observed value
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public Range(final Observer observer, final boolean twoWay, final AbsoluteDate date,
                 final double range, final MeasurementQuality measurementQuality,
                 final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(observer, date, range, measurementQuality, twoWay, signalTravelTimeModel, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<Range> theoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                      final int evaluation,
                                                                                      final SpacecraftState[] states,
                                                                                      final boolean fillParticipants) {
        // compute reception date
        final double clockOffset = getObserver().getQuadraticClockModel().getOffset(getDate()).getBias();
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
    private EstimatedMeasurementBase<Range> twoWayTheoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                          final int evaluation,
                                                                                          final AbsoluteDate receptionDate,
                                                                                          final SpacecraftState state,
                                                                                          final boolean fillParticipants) {
        EstimatedMeasurementBase<Range> estimated = initializeTwoWayTheoreticalEvaluation(this, iteration, evaluation,
                receptionDate, state, true);

        // Compute range
        final AbsoluteDate emissionDate = estimated.getParticipants()[0].getDate();
        final double range = receptionDate.durationFrom(emissionDate) * Constants.SPEED_OF_LIGHT / 2.;
        if (!fillParticipants) {
            estimated = new EstimatedMeasurementBase<>(estimated.getObservedMeasurement(), iteration, evaluation,
                    estimated.getStates(), new TimeStampedPVCoordinates[0]);
        }
        estimated.setEstimatedValue(range);
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
    private EstimatedMeasurementBase<Range> oneWayTheoreticalEvaluationWithoutDerivatives(final int iteration,
                                                                                          final int evaluation,
                                                                                          final AbsoluteDate receptionDate,
                                                                                          final SpacecraftState state,
                                                                                          final boolean fillParticipants) {
        EstimatedMeasurementBase<Range> estimated = initializeOneWayTheoreticalEvaluation(this, iteration, evaluation,
                receptionDate, state, true);
        final AbsoluteDate emissionDate = estimated.getParticipants()[0].getDate();

        // clock bias, taken in account only in case of one way
        final ObservableSatellite satellite = getSatellites().getFirst();
        final double              dts       = satellite.getOffsetValue(emissionDate);
        final double              dtg       = getObserver().getOffsetValue(receptionDate);
        final double clockBias = dtg - dts;

        final double range = (clockBias + receptionDate.durationFrom(emissionDate)) * Constants.SPEED_OF_LIGHT;
        if (!fillParticipants) {
            estimated = new EstimatedMeasurementBase<>(estimated.getObservedMeasurement(), iteration, evaluation,
                    estimated.getStates(), new TimeStampedPVCoordinates[0]);
        }
        estimated.setEstimatedValue(range);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Range> twoWayTheoreticalEvaluation(final int iteration, final int evaluation,
                                                                    final FieldPVCoordinatesProvider<Gradient> satellitePVProvider,
                                                                    final SpacecraftState state,
                                                                    final Map<String, Integer> indices,
                                                                    final int nbParams) {
        // Compute light time delays
        final Frame frame = state.getFrame();
        final FieldPVCoordinatesProvider<Gradient> observerPVProvider = getObserver().getFieldPVCoordinatesProvider(nbParams, indices);
        final FieldAbsoluteDate<Gradient> receptionDate = getCorrectedReceptionDateField(nbParams, indices);
        final FieldVector3D<Gradient> receiverPosition = observerPVProvider.getPosition(receptionDate, frame);
        final TwoLeggedSignalTimer twoLeggedSignalTimer = new TwoLeggedSignalTimer(getSignalTravelTimeModel());
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                receiverPosition, frame);
        final Gradient[] delays = twoLeggedSignalTimer.computeDelays(receptionCondition, satellitePVProvider, observerPVProvider);

        // Prepare the evaluation
        final FieldAbsoluteDate<Gradient> transitDate = receptionDate.shiftedBy(delays[1].negate());
        final SpacecraftState transitState = state.shiftedBy(transitDate.toAbsoluteDate().durationFrom(state));
        final FieldAbsoluteDate<Gradient> emissionDate = transitDate.shiftedBy(delays[0].negate());
        final EstimatedMeasurement<Range> estimated = new EstimatedMeasurement<>(this, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {
                        getObserver().getPVCoordinatesProvider().getPVCoordinates(emissionDate.toAbsoluteDate(), frame),
                        transitState.getPVCoordinates(),
                        getObserver().getPVCoordinatesProvider().getPVCoordinates(receptionDate.toAbsoluteDate(), frame)});

        // Compute range
        final Gradient range = delays[0].add(delays[1]).multiply(Constants.SPEED_OF_LIGHT / 2.);
        fillEstimation(range, indices, estimated);
        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Range> oneWayTheoreticalEvaluation(final int iteration, final int evaluation,
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
        final FieldVector3D<Gradient> observerPositionAtReception = getObserver().getFieldPVCoordinatesProvider(nbParams, indices)
                .getPosition(receptionDate, frame);
        final FieldSignalReceptionCondition<Gradient> receptionCondition = new FieldSignalReceptionCondition<>(receptionDate,
                observerPositionAtReception, frame);
        final Gradient delay = adjustableEmitter.computeDelay(receptionCondition);

        // prepare the evaluation
        final FieldAbsoluteDate<Gradient> emissionDate = receptionDate.shiftedBy(delay.negate());
        final SpacecraftState emissionState = state.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(state));
        final EstimatedMeasurement<Range> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { emissionState }, new TimeStampedPVCoordinates[] {
                        emissionState.getPVCoordinates(),
                        getObserver().getPVCoordinatesProvider().getPVCoordinates(receptionDate.toAbsoluteDate(), frame) });

        // clock offset, taken in account only in case of one way
        final ObservableSatellite satellite    = getSatellites().getFirst();
        final Gradient dts = satellite.getFieldOffsetValue(nbParams, emissionDate.toAbsoluteDate(), indices);
        final Gradient dtg = getObserver().getFieldOffsetValue(nbParams, receptionDate.toAbsoluteDate(), indices);
        final Gradient clockBias = dtg.subtract(dts);

        final Gradient range = clockBias.add(delay).multiply(Constants.SPEED_OF_LIGHT);
        fillEstimation(range, indices, estimated);
        return estimated;
    }

}
