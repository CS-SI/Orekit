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
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.DifferencesOfSignalArrival;
import org.orekit.signal.SignalReceptionCondition;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a Time Difference of Arrival measurement with a satellite as emitter
 * and two observers as receivers.
 * <p>
 * TDOA measures the difference in signal arrival time between the emitter and receivers,
 * corresponding to a difference in ranges from the two receivers to the emitter.
 * </p><p>
 * The date of the measurement corresponds to the reception of the signal by the prime station.
 * The measurement corresponds to the date of the measurement minus
 * the date of reception of the signal by the second station:
 * <code>tdoa = tr<sub>1</sub> - tr<sub>2</sub></code>
 * </p><p>
 * The motion of the sensors and the satellite during the signal flight time are taken into account.
 * </p>
 * @author Pascal Parraud
 * @since 11.2
 */
public class TDOA extends DualReceiverMeasurement<TDOA> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "TDOA";

    /** Constructor.
     * @param primeObserver observer that gives the measurement date
     * @param secondObserver observer that gives the measurement value
     * @param date date of the measurement
     * @param tdoa observed value (s)
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     */
    public TDOA(final Observer primeObserver, final Observer secondObserver,
                final AbsoluteDate date, final double tdoa, final double sigma, final double baseWeight,
                final ObservableSatellite satellite) {
        this(primeObserver, secondObserver, date, tdoa, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(), satellite);
    }

    /** Constructor.
     * @param primeObserver observer that gives the measurement date
     * @param secondObserver observer that gives the measurement value
     * @param date date of the measurement
     * @param tdoa observed value (s)
     * @param measurementQuality measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal travel time model
     * @param satellite satellite related to this measurement
     * @since 14.0
     */
    public TDOA(final Observer primeObserver, final Observer secondObserver,
                final AbsoluteDate date, final double tdoa, final MeasurementQuality measurementQuality,
                final SignalTravelTimeModel signalTravelTimeModel, final ObservableSatellite satellite) {
        super(primeObserver, secondObserver, date, new double[] {tdoa}, measurementQuality,
                signalTravelTimeModel, satellite);
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<TDOA> theoreticalEvaluationWithoutDerivatives(final int iteration, final int evaluation,
                                                                                     final SpacecraftState[] states) {
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();

        // Compute emission and reception dates
        final AbsoluteDate firstReceptionDate = getPrimeObserver().getCorrectedReceptionDate(getDate());
        final PVCoordinatesProvider emitter = AbstractParticipant.extractPVCoordinatesProvider(state, state.getPVCoordinates());
        final DifferencesOfSignalArrival differencesOfSignalArrival = new DifferencesOfSignalArrival(getSignalTravelTimeModel());
        final TimeStampedPVCoordinates primePV = getPrimeObserver().getPVCoordinatesProvider().getPVCoordinates(firstReceptionDate, frame);
        final SignalReceptionCondition receptionCondition = new SignalReceptionCondition(firstReceptionDate,
                primePV.getPosition(), frame);
        final double[] delays = differencesOfSignalArrival.computeDelays(receptionCondition,
                getSecondObserver().getPVCoordinatesProvider(), emitter);
        final AbsoluteDate emissionDate = firstReceptionDate.shiftedBy(-delays[0]);
        final AbsoluteDate secondReceptionDate = emissionDate.shiftedBy(delays[1]);

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final double offset1 = getPrimeObserver().getClockBiasDriver().getValue(firstReceptionDate);
        final double offset2 = getSecondObserver().getClockBiasDriver().getValue(secondReceptionDate);
        final double tdoa = (firstReceptionDate.durationFrom(emissionDate) + offset1) - (secondReceptionDate.durationFrom(emissionDate) + offset2);

        // Prepare the evaluation
        final TimeStampedPVCoordinates emitterPV = emitter.getPVCoordinates(emissionDate, frame);
        final TimeStampedPVCoordinates secondPV = getSecondObserver().getPVCoordinatesProvider().getPVCoordinates(secondReceptionDate, frame);
        final EstimatedMeasurement<TDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { state.shiftedBy(emissionDate.durationFrom(state.getDate())) },
                        new TimeStampedPVCoordinates[] { emitterPV, tdoa > 0.0 ? secondPV : primePV, tdoa > 0.0 ? primePV : secondPV });

        // set TDOA value
        estimated.setEstimatedValue(tdoa);

        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<TDOA> theoreticalEvaluation(final int iteration, final int evaluation,
                                                               final SpacecraftState[] states) {
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();
        final Gradient[] delays = computeDelays(states);
        final Gradient firstDelay = delays[0];
        final Gradient secondDelay = delays[1];
        final FieldAbsoluteDate<Gradient> firstReceptionDate = getPrimeObserver().getCorrectedReceptionDateField(getDate(), nbParams, paramIndices);
        final FieldAbsoluteDate<Gradient> emissionDate = firstReceptionDate.shiftedBy(firstDelay.negate());
        final FieldAbsoluteDate<Gradient> secondReceptionDate = emissionDate.shiftedBy(secondDelay);

        // The measured TDOA is (tau1 + clockOffset1) - (tau2 + clockOffset2)
        final Gradient offset1 = getPrimeObserver().getClockBiasDriver()
                                .getValue(nbParams, paramIndices, emissionDate.toAbsoluteDate());
        final Gradient offset2 = getSecondObserver().getClockBiasDriver()
                                .getValue(nbParams, paramIndices, emissionDate.toAbsoluteDate());
        final Gradient tdoaG   = firstDelay.add(offset1).subtract(secondDelay.add(offset2));
        final double   tdoa    = tdoaG.getValue();

        // Evaluate the TDOA value and derivatives
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final TimeStampedPVCoordinates primePV = getPrimeObserver().getPVCoordinatesProvider().getPVCoordinates(firstReceptionDate.toAbsoluteDate(), frame);
        final TimeStampedPVCoordinates secondPV = getSecondObserver().getPVCoordinatesProvider().getPVCoordinates(secondReceptionDate.toAbsoluteDate(), frame);
        final SpacecraftState emitterState = state.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(state.getDate()));
        final EstimatedMeasurement<TDOA> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation,
                                new SpacecraftState[] { emitterState },
                                new TimeStampedPVCoordinates[] { emitterState.getPVCoordinates(), tdoa > 0.0 ? secondPV : primePV, tdoa > 0.0 ? primePV : secondPV });

        // set TDOA value
        fillEstimation(tdoaG, paramIndices, estimated);
        return estimated;
    }

}
