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
package org.orekit.signal;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Class for two-way signal travel time.
 * A signal is emitted, received a first time (relay/reflection) and received again a final time. There is no further assumption.
 * @since 14.0
 * @author Romain Serra
 */
public class TwoLeggedSignalTravelTimer {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /**
     * Constructor.
     * @param signalTravelTimeModel time delay computer
     */
    public TwoLeggedSignalTravelTimer(final SignalTravelTimeModel signalTravelTimeModel) {
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /**
     * Compute first and second leg delays.
     *
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param approxRelayDate guess for the relay date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return delays on both legs in chronological order (s)
     */
    public double[] computeDelays(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                                  final PVCoordinatesProvider relay, final AbsoluteDate approxRelayDate,
                                  final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        final double secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay, approxRelayDate);
        final AbsoluteDate relayDate = receptionDate.shiftedBy(-secondLegTravelTime);
        final Vector3D relayPosition = relay.getPosition(relayDate, frame);
        final double firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter, approxEmissionDate);
        return new double[] { firstLegTravelTime, secondLegTravelTime };
    }

    /**
     * Compute first and second leg delays without guess.
     *
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return delays on both legs in chronological order (s)
     */
    public double[] computeDelays(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                                  final PVCoordinatesProvider relay, final PVCoordinatesProvider emitter) {
        final double secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay, receptionDate);
        final AbsoluteDate relayDate = receptionDate.shiftedBy(-secondLegTravelTime);
        final Vector3D relayPosition = relay.getPosition(relayDate, frame);
        final double firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter, relayDate);
        return new double[] { firstLegTravelTime, secondLegTravelTime };
    }

    /**
     * Compute first and second leg delays.
     * @param <T> field type
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception, second emission) coordinates provider
     * @param approxRelayDate  guess for the relay date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate  guess for the emission date
     * @return delays on both legs in chronological order (s)
     */
    public <T extends CalculusFieldElement<T>> T[] computeDelays(final Frame frame, final FieldVector3D<T> receiverPosition,
                                                                 final FieldAbsoluteDate<T> receptionDate,
                                                                 final FieldPVCoordinatesProvider<T> relay,
                                                                 final FieldAbsoluteDate<T> approxRelayDate,
                                                                 final FieldPVCoordinatesProvider<T> emitter,
                                                                 final FieldAbsoluteDate<T> approxEmissionDate) {
        final T secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay, approxRelayDate);
        final FieldAbsoluteDate<T> relayDate = receptionDate.shiftedBy(secondLegTravelTime.negate());
        final FieldVector3D<T> relayPosition = relay.getPosition(relayDate, frame);
        final T firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter, approxEmissionDate);
        final T[] output = MathArrays.buildArray(receiverPosition.getX().getField(), 2);
        output[0] = firstLegTravelTime;
        output[1] = secondLegTravelTime;
        return output;
    }

    /**
     * Compute first and second leg delays without guess.
     * @param <T> field type
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception, second emission) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return delays on both legs in chronological order (s)
     */
    public <T extends CalculusFieldElement<T>> T[] computeDelays(final Frame frame, final FieldVector3D<T> receiverPosition,
                                                                 final FieldAbsoluteDate<T> receptionDate,
                                                                 final FieldPVCoordinatesProvider<T> relay,
                                                                 final FieldPVCoordinatesProvider<T> emitter) {
        final T secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay, receptionDate);
        final FieldAbsoluteDate<T> relayDate = receptionDate.shiftedBy(secondLegTravelTime.negate());
        final FieldVector3D<T> relayPosition = relay.getPosition(relayDate, frame);
        final T firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter, relayDate);
        final T[] output = MathArrays.buildArray(receiverPosition.getX().getField(), 2);
        output[0] = firstLegTravelTime;
        output[1] = secondLegTravelTime;
        return output;
    }

    /**
     * Method for one leg travel time.
     * @param frame frame where position is given
     * @param receiverPosition receiver position at reception
     * @param receptionDate reception date
     * @param emitter emitter
     * @param guessEmissionDate guess for the emission date
     * @return signal travel time
     */
    private double computeTravelTime(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                                     final PVCoordinatesProvider emitter, final AbsoluteDate guessEmissionDate) {
        return signalTravelTimeModel.getAdjustableEmitterComputer(emitter).computeDelay(guessEmissionDate,
                receiverPosition, receptionDate, frame);
    }

    /**
     * Method for one leg travel time.
     * @param <T> field type
     * @param frame frame where position is given
     * @param receiverPosition receiver position at reception
     * @param receptionDate reception date
     * @param emitter emitter
     * @param guessEmissionDate guess for the emission date
     * @return signal travel time
     */
    private <T extends CalculusFieldElement<T>> T computeTravelTime(final Frame frame, final FieldVector3D<T> receiverPosition,
                                                                    final FieldAbsoluteDate<T> receptionDate,
                                                                    final FieldPVCoordinatesProvider<T> emitter,
                                                                    final FieldAbsoluteDate<T> guessEmissionDate) {
        return signalTravelTimeModel.getFieldAdjustableEmitterComputer(receptionDate.getField(), emitter)
                .computeDelay(guessEmissionDate, receiverPosition, receptionDate, frame);
    }
}
