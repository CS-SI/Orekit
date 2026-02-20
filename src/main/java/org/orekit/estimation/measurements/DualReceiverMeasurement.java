/* Copyright 2025-2026 Hawkeye 360 (HE360)
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
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.signal.FieldSignalTravelTimeAdjustableEmitter;
import org.orekit.signal.FieldSignalTravelTimeAdjustableReceiver;
import org.orekit.signal.SignalTravelTimeModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Class modeling a twice-received measurement using a primary and secondary observer.
 * <p>
 * The measurement is considered to be a signal:
 * <ul>
 * <li>Emitted by the observed spacecraft</li>
 * <li>Received by the first observer</li>
 * <li>Received by the second observer</li>
 * </ul>
 * The date of the measurement corresponds to the reception on of the signal by the first observer.
 * <p>
 * The motion of the observers and the spacecraft during the signal flight time are taken into account.
 * </p>
 *
 * @author Brianna Aubin
 * @since 14.0
 */
abstract class DualReceiverMeasurement<T extends AbstractMeasurement<T>> extends SignalBasedMeasurement<T> {

    /**
     * First observer to receive signal.  Determines measurement date.
     */
    private final Observer primeObserver;

    /**
     * Second Observer to receive signal.  Determines measurement value.
     */
    private final Observer secondObserver;
    /**
     * Simple constructor.
     *
     * @param primeObserver         observer from which transmission is performed
     * @param secondObserver        observer from which measurement is performed
     * @param date                  date of the measurement
     * @param value                 observed value
     * @param sigma                 theoretical standard deviation
     * @param baseWeight            base weight
     * @param signalTravelTimeModel signal travel time model
     * @param satellite             satellite related to this measurement
     */
    protected DualReceiverMeasurement(final Observer primeObserver, final Observer secondObserver,
                                      final AbsoluteDate date,
                                      final double[] value, final double[] sigma, final double[] baseWeight,
                                      final SignalTravelTimeModel signalTravelTimeModel,
                                      final ObservableSatellite satellite) {
        super(date, false, value, new MeasurementQuality(sigma, baseWeight), signalTravelTimeModel,
                Collections.singletonList(satellite));

        // Add the parameters for the receiver
        addParametersDrivers(primeObserver.getParametersDrivers());
        addParametersDrivers(secondObserver.getParametersDrivers());

        // Set emitter
        this.primeObserver  = primeObserver;
        this.secondObserver = secondObserver;
    }

    /** Get the prime ground station, the one that receives the signal first.
     * @return prime ground station
     * @deprecated as of 14.0, replaced by {@link #getPrimeObserver()}
     */
    @Deprecated
    public GroundStation getPrimeStation() {
        if (!(primeObserver instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) primeObserver;
    }

    /** Get the prime observer, the one that receives the signal first.
     * @return prime observer
     */
    public Observer getPrimeObserver() {
        return primeObserver;
    }

    /** Get the second ground station, the one that receives the signal first.
     * @return second ground station
     * @deprecated as of 14.0, replaced by {@link #getSecondObserver()}
     */
    @Deprecated
    public GroundStation getSecondStation() {
        if (!(secondObserver instanceof GroundStation)) {
            return null;
        }
        return (GroundStation) secondObserver;
    }

    /** Get the second observer, the one that gives the measurement.
     * @return second observer
     */
    public Observer getSecondObserver() {
        return secondObserver;
    }

    /**
     * Compute signal delays (always positive).
     * @param states observed states
     * @return delays (to prime and second sensors)
     */
    protected Gradient[] computeDelays(final SpacecraftState[] states) {
        // Derivatives are computed with respect to spacecraft state in inertial frame and measurement model parameters
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
        final FieldPVCoordinatesProvider<Gradient> emitter = AbstractParticipant.extractFieldPVCoordinatesProvider(state, pva);
        final FieldAbsoluteDate<Gradient> firstReceptionDate = getPrimeObserver().getCorrectedReceptionDateField(getDate(), nbParams, paramIndices);

        // Compute emission date
        final FieldSignalTravelTimeAdjustableEmitter<Gradient> signalTravelTimeAdjustableEmitter =
                new FieldSignalTravelTimeAdjustableEmitter<>(emitter);
        final FieldPVCoordinatesProvider<Gradient> primePVProvider = getPrimeObserver().getFieldPVCoordinatesProvider(nbParams, paramIndices);
        final Gradient firstDelay = signalTravelTimeAdjustableEmitter.computeDelay(firstReceptionDate,
                primePVProvider.getPosition(firstReceptionDate, frame), firstReceptionDate, frame);
        final FieldAbsoluteDate<Gradient> emissionDate = firstReceptionDate.shiftedBy(firstDelay.negate());

        // Secondary PV in inertial frame at receive at second sensor
        final FieldPVCoordinatesProvider<Gradient> secondReceiver = getSecondObserver().getFieldPVCoordinatesProvider(nbParams,
                paramIndices);
        final FieldSignalTravelTimeAdjustableReceiver<Gradient> signalTravelTimeAdjustableReceiver = getSignalTravelTimeModel()
                .getFieldAdjustableReceiverComputer(pva.getDate().getField(), secondReceiver);
        final TimeStampedFieldPVCoordinates<Gradient> emitterPV = emitter.getPVCoordinates(emissionDate, frame);
        final Gradient secondDelay = signalTravelTimeAdjustableReceiver.computeDelay(emitterPV.getPosition(),
                emissionDate, frame);
        return new Gradient[] {firstDelay, secondDelay};
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
            for (TimeSpanMap.Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                final Integer index = paramIndices.get(span.getData());
                if (index != null) {
                    estimated.setParameterDerivatives(driver, span.getStart(), derivatives[index]);
                }
            }
        }
    }
}
