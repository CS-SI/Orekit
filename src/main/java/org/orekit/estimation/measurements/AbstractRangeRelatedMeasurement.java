/* Copyright 2022-2026 Romain Serra
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeAdjustableEmitter;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.signal.TwoLeggedSignalTravelTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Abstract class modeling one-way or two-way range-related measurement.
 * For the latter, a signal is emitted by a sensor, reflected on the spacecraft and received by the same sensor.
 * For the former, a signal is emitted by the spacecraft and receiver by the sensor.
 *
 * @author Romain Serra
 * @since 14.0
 */
public abstract class AbstractRangeRelatedMeasurement<T extends AbstractRangeRelatedMeasurement<T>>
        extends SignalBasedMeasurement<T> {

    /** Sensor that receives signal from satellite. */
    private final Observer observer;

    /** Simple constructor.
     * @param observer observer that performs the measurement
     * @param date date of the measurement
     * @param value observed value
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal model
     * @param twoWay if true, this is a two-way measurement
     * @param satellite satellite related to this measurement
     */
    protected AbstractRangeRelatedMeasurement(final Observer observer, final AbsoluteDate date,
                                              final double value, final MeasurementQuality measurementQuality,
                                              final boolean twoWay, final SignalTravelTimeModel signalTravelTimeModel,
                                              final ObservableSatellite satellite) {
        super(date, twoWay, new double[] { value }, measurementQuality, signalTravelTimeModel,
                Collections.singletonList(satellite));
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
     */
    public final Observer getObserver() {
        return observer;
    }

    /**
     * Compute participants for two-way case.
     * @param measurement measurement object
     * @param iteration iteration
     * @param evaluation evaluation
     * @param receptionDate signal reception date
     * @param state estimated state
     * @return partially filled estimated measurement
     */
    protected EstimatedMeasurementBase<T> initializeTwoWayTheoreticalEvaluation(final T measurement,
                                                                                final int iteration, final int evaluation,
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
        return new EstimatedMeasurementBase<>(measurement, iteration, evaluation,
                new SpacecraftState[] { transitState },
                new TimeStampedPVCoordinates[] {emissionPV, transitState.getPVCoordinates(), receiverPV});
    }

    /**
     * Compute participants for one-way case.
     * @param measurement measurement object
     * @param iteration iteration
     * @param evaluation evaluation
     * @param receptionDate signal reception date
     * @param state estimated state
     * @return partially filled estimated measurement
     */
    protected EstimatedMeasurementBase<T> initializeOneWayTheoreticalEvaluation(final T measurement,
                                                                                final int iteration, final int evaluation,
                                                                                final AbsoluteDate receptionDate,
                                                                                final SpacecraftState state) {
        // compute light time delay
        final Frame frame = state.getFrame();
        final PVCoordinatesProvider observablePVProvider = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final SignalTravelTimeAdjustableEmitter adjustableEmitter = getSignalTravelTimeModel()
                .getAdjustableEmitterComputer(observablePVProvider);
        final TimeStampedPVCoordinates observerPVAtReception = getObserver().getPVCoordinatesProvider()
                .getPVCoordinates(receptionDate, frame);
        final double delay = adjustableEmitter.computeDelay(observerPVAtReception.getPosition(), receptionDate, frame);

        // prepare the evaluation
        final AbsoluteDate emissionDate = receptionDate.shiftedBy(-delay);
        final SpacecraftState emissionState = state.shiftedBy(emissionDate.durationFrom(state));
        return new EstimatedMeasurementBase<>(measurement, iteration, evaluation,
                new SpacecraftState[] { emissionState },
                new TimeStampedPVCoordinates[] { emissionState.getPVCoordinates(), observerPVAtReception });
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<T> theoreticalEvaluation(final int iteration, final int evaluation,
                                                            final SpacecraftState[] states) {
        // Derivatives are computed with respect to spacecraft state in inertial frame and measurement model params
        // -------
        //
        // Parameters:
        //  - 0..2 - Position of the spacecraft in inertial frame
        //  - 3..5 - Velocity of the spacecraft in inertial frame
        //  - 6..n - observer parameters (clock offset, clock drift, ...)
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

    /** Evaluate measurement in two-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param observablePVProvider coordinates provider of observable for automatic differentiation
     * @param state observable state
     * @param paramIndices indices of the estimated parameters in derivatives computations
     * @param nbParams the number of estimated parameters in derivative computations
     * @return theoretical value
     */
    protected abstract EstimatedMeasurement<T> twoWayTheoreticalEvaluation(int iteration, int evaluation,
                                                                           FieldPVCoordinatesProvider<Gradient> observablePVProvider,
                                                                           SpacecraftState state,
                                                                           Map<String, Integer> paramIndices,
                                                                           int nbParams);

    /** Evaluate measurement in one-way.
     * @param iteration iteration number
     * @param evaluation evaluations counter
     * @param observablePVProvider coordinates provider of observable for automatic differentiation
     * @param state observable state
     * @param paramIndices indices of the estimated parameters in derivatives computations
     * @param nbParams the number of estimated parameters in derivative computations
     * @return theoretical value
     */
    protected abstract EstimatedMeasurement<T> oneWayTheoreticalEvaluation(int iteration, int evaluation,
                                                                           FieldPVCoordinatesProvider<Gradient> observablePVProvider,
                                                                           SpacecraftState state,
                                                                           Map<String, Integer> paramIndices,
                                                                           int nbParams);

    /**
     * Compute actual reception date taking into account clock offset.
     * @param nbParams number of independent variables for automatic differentiation
     * @param paramIndices mapping between parameter name and variable index
     * @return reception date
     */
    protected FieldAbsoluteDate<Gradient> getCorrectedReceptionDateField(final int nbParams,
                                                                       final Map<String, Integer> paramIndices) {
        final Gradient offset = getObserver().getClockBiasDriver().getValue(nbParams, paramIndices, getDate());  // FIXME missing drift and quadratic term
        final FieldAbsoluteDate<Gradient> fieldDate = new FieldAbsoluteDate<>(GradientField.getField(nbParams), getDate());
        return fieldDate.shiftedBy(offset.negate());
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
