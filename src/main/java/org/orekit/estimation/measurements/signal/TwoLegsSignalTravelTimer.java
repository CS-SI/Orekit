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
package org.orekit.estimation.measurements.signal;

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
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
public class TwoLegsSignalTravelTimer {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /**
     * Constructor.
     * @param signalTravelTimeModel time delay computer
     */
    public TwoLegsSignalTravelTimer(final SignalTravelTimeModel signalTravelTimeModel) {
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
     *
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception, second emission) coordinates provider
     * @param approxRelayDate  guess for the relay date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate  guess for the emission date
     * @return delays on both legs in chronological order (s)
     */
    public Gradient[] computeDelays(final Frame frame, final FieldVector3D<Gradient> receiverPosition,
                                    final FieldAbsoluteDate<Gradient> receptionDate,
                                    final FieldPVCoordinatesProvider<Gradient> relay,
                                    final FieldAbsoluteDate<Gradient> approxRelayDate,
                                    final FieldPVCoordinatesProvider<Gradient> emitter,
                                    final FieldAbsoluteDate<Gradient> approxEmissionDate) {
        final Gradient secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay, approxRelayDate);
        final FieldAbsoluteDate<Gradient> relayDate = receptionDate.shiftedBy(secondLegTravelTime.negate());
        final FieldVector3D<Gradient> relayPosition = relay.getPosition(relayDate, frame);
        final Gradient firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter, approxEmissionDate);
        return new Gradient[] { firstLegTravelTime, secondLegTravelTime };
    }

    /**
     * Compute first and second leg delays without guess.
     *
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception, second emission) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return delays on both legs in chronological order (s)
     */
    public Gradient[] computeDelays(final Frame frame, final FieldVector3D<Gradient> receiverPosition,
                                    final FieldAbsoluteDate<Gradient> receptionDate,
                                    final FieldPVCoordinatesProvider<Gradient> relay,
                                    final FieldPVCoordinatesProvider<Gradient> emitter) {
        final Gradient secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay, receptionDate);
        final FieldAbsoluteDate<Gradient> relayDate = receptionDate.shiftedBy(secondLegTravelTime.negate());
        final FieldVector3D<Gradient> relayPosition = relay.getPosition(relayDate, frame);
        final Gradient firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter, relayDate);
        return new Gradient[] { firstLegTravelTime, secondLegTravelTime };
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
     * @param frame frame where position is given
     * @param receiverPosition receiver position at reception
     * @param receptionDate reception date
     * @param emitter emitter
     * @param guessEmissionDate guess for the emission date
     * @return signal travel time
     */
    private Gradient computeTravelTime(final Frame frame, final FieldVector3D<Gradient> receiverPosition,
                                       final FieldAbsoluteDate<Gradient> receptionDate,
                                       final FieldPVCoordinatesProvider<Gradient> emitter,
                                       final FieldAbsoluteDate<Gradient> guessEmissionDate) {
        return signalTravelTimeModel.getAdjustableEmitterComputer(emitter).computeDelay(guessEmissionDate,
                receiverPosition, receptionDate, frame);
    }
}
