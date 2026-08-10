/* Copyright 2002-2026 Mark Rutten
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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a Frequency Difference of Arrival measurement with a satellite as emitter
 * and two ground stations as receivers.
 * <p>
 * FDOA measures the difference in signal arrival frequency between the emitter and receivers,
 * corresponding to a difference in range-rate from the two receivers to the emitter.
 * </p><p>
 * The date of the measurement corresponds to the reception of the signal by the prime station.
 * The measurement corresponds to the frequency of the signal received at the prime station at
 * the date of the measurement minus the frequency of the signal received at the second station:
 * <code>fdoa = f<sub>1</sub> - f<sub>2</sub></code>
 * </p><p>
 * The motion of the stations and the satellite during the signal flight time are taken into account.
 * </p>
 * @author Mark Rutten
 * @since 12.0
 */
public class FDOA extends DualReceiverMeasurement<FDOA> {

    /** Type of the measurement. */
    public static final String MEASUREMENT_TYPE = "FDOA";

    /** Centre frequency of the signal emitted from the satellite. */
    private final double centreFrequency;

    /** Constructor with default signal travel time model.
     * @param primeObserver   observer that gives the date of the measurement
     * @param secondObserver  observer that gives the measurement
     * @param centreFrequency satellite emitter frequency (Hz)
     * @param date            date of the measurement
     * @param fdoa            observed value (Hz)
     * @param sigma           theoretical standard deviation
     * @param baseWeight      base weight
     * @param satellite       satellite related to this measurement
     */
    public FDOA(final Observer primeObserver, final Observer secondObserver,
                final double centreFrequency, final AbsoluteDate date, final double fdoa, final double sigma,
                final double baseWeight, final ObservableSatellite satellite) {
        this(primeObserver, secondObserver, centreFrequency, date, fdoa, new MeasurementQuality(sigma, baseWeight), new SignalTravelTimeModel(),
                satellite);
    }

    /** Constructor.
     * @param primeObserver         observer that gives the date of the measurement
     * @param secondObserver        observer that gives the measurement
     * @param centreFrequency       satellite emitter frequency (Hz)
     * @param date                  date of the measurement
     * @param fdoa                  observed value (Hz)
     * @param measurementQuality    measurement quality data as used in orbit determination
     * @param signalTravelTimeModel signal travel time model
     * @param satellite             satellite related to this measurement
     * @since 14.0
     */
    public FDOA(final Observer primeObserver, final Observer secondObserver,
                final double centreFrequency, final AbsoluteDate date, final double fdoa,
                final MeasurementQuality measurementQuality, final SignalTravelTimeModel signalTravelTimeModel,
                final ObservableSatellite satellite) {
        super(primeObserver, secondObserver, date, new double[] {fdoa}, measurementQuality,
                signalTravelTimeModel, satellite);

        this.centreFrequency = centreFrequency;
    }

    /** Get centre frequency of carrier wave.
     * @return frequency value (Hz)
     */
    public double getCentreFrequency() {
        return centreFrequency;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurementBase<FDOA> theoreticalEvaluationWithoutDerivatives(final int iteration, final int evaluation,
                                                                                     final SpacecraftState[] states,
                                                                                     final boolean fillParticipants) {
        // Evaluate the TDOA value
        final TDOA tdoa = new TDOA(getPrimeObserver(), getSecondObserver(), getDate(), 0., new MeasurementQuality(1),
                getSignalTravelTimeModel(), getSatellites().getFirst());
        final EstimatedMeasurementBase<TDOA> estimatedTdoa = tdoa.theoreticalEvaluationWithoutDerivatives(iteration,
                evaluation, states, true);

        // Prepare the FDOA model
        final TimeStampedPVCoordinates[] participants = estimatedTdoa.getParticipants();
        final EstimatedMeasurement<FDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation, estimatedTdoa.getStates(),
                        fillParticipants ? participants : new TimeStampedPVCoordinates[0]);

        // Range-rate components
        final PVCoordinates emitterPV = participants[0];
        final PVCoordinates primePV = participants[1];
        final PVCoordinates secondPV = participants[2];
        final Vector3D primeDirection = primePV.getPosition().subtract(emitterPV.getPosition()).normalize();
        final Vector3D secondDirection = secondPV.getPosition().subtract(emitterPV.getPosition()).normalize();
        final Vector3D primeVelocity = primePV.getVelocity().subtract(emitterPV.getVelocity());
        final Vector3D secondVelocity = secondPV.getVelocity().subtract(emitterPV.getVelocity());

        // range rate difference
        final double rangeRateDifference = Vector3D.dotProduct(primeDirection, primeVelocity) -
                Vector3D.dotProduct(secondDirection, secondVelocity);

        // set FDOA value
        final double rangeRateToHz = -centreFrequency / Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue(rangeRateDifference * rangeRateToHz);

        return estimated;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<FDOA> theoreticalEvaluation(final int iteration, final int evaluation,
                                                               final SpacecraftState[] states) {
        // Compute emission date and reception ones
        final Map<String, Integer> paramIndices = getParameterIndices(states);
        final int                  nbParams     = 6 * states.length + paramIndices.size();
        final Gradient[] delays = computeDelays(states);
        final Gradient firstDelay = delays[0];
        final Gradient secondDelay = delays[1];
        final FieldAbsoluteDate<Gradient> firstReceptionDate = getPrimeObserver().getCorrectedReceptionDateField(getDate(), nbParams, paramIndices);
        final FieldAbsoluteDate<Gradient> emissionDate = firstReceptionDate.shiftedBy(firstDelay.negate());
        final FieldAbsoluteDate<Gradient> secondReceptionDate = emissionDate.shiftedBy(secondDelay);

        // Prepare the FDOA estimation
        final SpacecraftState state = states[0];
        final Frame frame = state.getFrame();
        final TimeStampedFieldPVCoordinates<Gradient> primePV = getPrimeObserver().getFieldPVCoordinatesProvider(nbParams, paramIndices)
                .getPVCoordinates(firstReceptionDate, frame);
        final TimeStampedFieldPVCoordinates<Gradient> secondPV = getSecondObserver().getFieldPVCoordinatesProvider(nbParams, paramIndices)
                .getPVCoordinates(secondReceptionDate, frame);
        final SpacecraftState emitterState = state.shiftedBy(emissionDate.toAbsoluteDate().durationFrom(state.getDate()));
        final double tdoa = delays[0].getReal() + delays[1].getReal();
        final EstimatedMeasurement<FDOA> estimated =
                new EstimatedMeasurement<>(this, iteration, evaluation,
                        new SpacecraftState[] { emitterState },
                        new TimeStampedPVCoordinates[] { emitterState.getPVCoordinates(),
                            tdoa > 0.0 ? secondPV.toTimeStampedPVCoordinates() : primePV.toTimeStampedPVCoordinates(),
                            tdoa > 0.0 ? primePV.toTimeStampedPVCoordinates() : secondPV.toTimeStampedPVCoordinates() });

        // Range-rate components
        final TimeStampedFieldPVCoordinates<Gradient> pva = AbstractMeasurement.getCoordinates(state, 0, nbParams);
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV = AbstractParticipant.extractFieldPVCoordinatesProvider(state, pva)
                .getPVCoordinates(emissionDate, frame);
        final FieldVector3D<Gradient> primeDirection = primePV.getPosition().subtract(emitterPV.getPosition()).normalize();
        final FieldVector3D<Gradient> secondDirection = secondPV.getPosition().subtract(emitterPV.getPosition()).normalize();
        final FieldVector3D<Gradient> primeVelocity = primePV.getVelocity().subtract(emitterPV.getVelocity());
        final FieldVector3D<Gradient> secondVelocity = secondPV.getVelocity().subtract(emitterPV.getVelocity());

        // range rate difference
        final Gradient rangeRateDifference = FieldVector3D.dotProduct(primeDirection, primeVelocity)
                .subtract(FieldVector3D.dotProduct(secondDirection, secondVelocity));

        // set FDOA value
        final double rangeRateToHz = -centreFrequency / Constants.SPEED_OF_LIGHT;
        final Gradient fdoa = rangeRateDifference.multiply(rangeRateToHz);
        fillEstimation(fdoa, getParameterIndices(states), estimated);
        return estimated;
    }

}
