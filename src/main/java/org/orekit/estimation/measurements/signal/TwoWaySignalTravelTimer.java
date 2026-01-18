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
public class TwoWaySignalTravelTimer {

    /** Signal travel time model. */
    private final SignalTravelTimeModel signalTravelTimeModel;

    /**
     * Constructor.
     * @param signalTravelTimeModel time delay computer
     */
    public TwoWaySignalTravelTimer(final SignalTravelTimeModel signalTravelTimeModel) {
        this.signalTravelTimeModel = signalTravelTimeModel;
    }

    /**
     * Compute first and second leg delays.
     *
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return ranges on both legs in chronological order (m)
     */
    public double[] computeDelays(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                                  final PVCoordinatesProvider relay,
                                  final PVCoordinatesProvider emitter) {
        final double secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay);
        final AbsoluteDate relayDate = receptionDate.shiftedBy(-secondLegTravelTime);
        final Vector3D relayPosition = relay.getPosition(relayDate, frame);
        final double firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter);
        return new double[] { firstLegTravelTime, secondLegTravelTime };
    }

    /**
     * Compute first and second leg delays.
     *
     * @param frame            frame where position is given
     * @param receiverPosition end receiver position (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception, second emission) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return ranges on both legs in chronological order (m)
     */
    public Gradient[] computeDelays(final Frame frame, final FieldVector3D<Gradient> receiverPosition,
                                    final FieldAbsoluteDate<Gradient> receptionDate,
                                    final FieldPVCoordinatesProvider<Gradient> relay,
                                    final FieldPVCoordinatesProvider<Gradient> emitter) {
        final Gradient secondLegTravelTime = computeTravelTime(frame, receiverPosition, receptionDate, relay);
        final FieldAbsoluteDate<Gradient> relayDate = receptionDate.shiftedBy(secondLegTravelTime.negate());
        final FieldVector3D<Gradient> relayPosition = relay.getPosition(relayDate, frame);
        final Gradient firstLegTravelTime = computeTravelTime(frame, relayPosition, relayDate, emitter);
        return new Gradient[] { firstLegTravelTime, secondLegTravelTime };
    }

    /**
     * Method for one leg travel time.
     * @param frame frame where position is given
     * @param receiverPosition receiver position at reception
     * @param receptionDate reception date
     * @param emitter emitter
     * @return signal travel time
     */
    private double computeTravelTime(final Frame frame, final Vector3D receiverPosition, final AbsoluteDate receptionDate,
                                     final PVCoordinatesProvider emitter) {
        return signalTravelTimeModel.getAdjustableEmitterComputer(emitter).computeDelay(receptionDate,
                receiverPosition, receptionDate, frame);
    }

    /**
     * Method for one leg travel time.
     * @param frame frame where position is given
     * @param receiverPosition receiver position at reception
     * @param receptionDate reception date
     * @param emitter emitter
     * @return signal travel time
     */
    private Gradient computeTravelTime(final Frame frame, final FieldVector3D<Gradient> receiverPosition,
                                       final FieldAbsoluteDate<Gradient> receptionDate,
                                       final FieldPVCoordinatesProvider<Gradient> emitter) {
        return signalTravelTimeModel.getAdjustableEmitterComputer(emitter).computeDelay(receptionDate,
                receiverPosition, receptionDate, frame);
    }
}
