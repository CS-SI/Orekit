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
package org.orekit.estimation.measurements.model;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.signal.TwoLeggedSignalTravelTimer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Class for two-legs range rate (a.k.a. Doppler measurement).
 * A signal is emitted, received a first time (relay/reflection) and received again a final time. There is no further assumption.
 * @since 14.0
 * @author Romain Serra
 */
public class TwoLeggedRangeRateModel {

    /** Signal travel time model. */
    private final TwoLeggedSignalTravelTimer twoWayTimer;

    /**
     * Constructor.
     * @param twoWayTimer two-way time delay computer
     */
    public TwoLeggedRangeRateModel(final TwoLeggedSignalTravelTimer twoWayTimer) {
        this.twoWayTimer = twoWayTimer;
    }

    /**
     * Compute measurement without guess.
     *
     * @param frame            frame where position is given
     * @param receiverPV       end receiver position-velocity (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return ranges on both legs in chronological order (m)
     */
    public double value(final Frame frame, final PVCoordinates receiverPV, final AbsoluteDate receptionDate,
                        final PVCoordinatesProvider relay, final PVCoordinatesProvider emitter) {
        return value(frame, receiverPV, receptionDate, relay, receptionDate, emitter, receptionDate);
    }

    /**
     * Compute measurement.
     *
     * @param frame            frame where position is given
     * @param receiverPV       end receiver position-velocity (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param approxRelayDate  guess for the relay date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return ranges on both legs in chronological order (m)
     */
    public double value(final Frame frame, final PVCoordinates receiverPV, final AbsoluteDate receptionDate,
                        final PVCoordinatesProvider relay,  final AbsoluteDate approxRelayDate,
                        final PVCoordinatesProvider emitter, final AbsoluteDate approxEmissionDate) {
        // Compute relay and emission dates
        final double[] delays = twoWayTimer.computeDelays(frame, receiverPV.getPosition(), receptionDate, relay,
                approxRelayDate, emitter, approxEmissionDate);
        final AbsoluteDate relayDate = receptionDate.shiftedBy(-delays[1]);
        final AbsoluteDate emissionDate = relayDate.shiftedBy(-delays[0]);

        // Compute position and velocity of interest
        final PVCoordinates relayPV = relay.getPVCoordinates(relayDate, frame);
        final PVCoordinates emitterPV = emitter.getPVCoordinates(emissionDate, frame);

        // Range-rate components
        final Vector3D receiverRelativePosition = receiverPV.getPosition().subtract(relayPV.getPosition());
        final Vector3D emitterRelativePosition = emitterPV.getPosition().subtract(relayPV.getPosition());
        final Vector3D receiverRelativeVelocity = receiverPV.getVelocity().subtract(relayPV.getVelocity());
        final Vector3D emitterRelativeVelocity = emitterPV.getVelocity().subtract(relayPV.getVelocity());

        // range rate
        return Vector3D.dotProduct(receiverRelativePosition.normalize(), receiverRelativeVelocity) +
                Vector3D.dotProduct(emitterRelativePosition.normalize(), emitterRelativeVelocity);
    }

    /**
     * Compute measurement without guess.
     * @param <T> field type
     * @param frame            frame where position is given
     * @param receiverPV       end receiver position and velocity (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return range rate (m/s)
     */
    public <T extends CalculusFieldElement<T>> T value(final Frame frame, final FieldPVCoordinates<T> receiverPV,
                                                       final FieldAbsoluteDate<T> receptionDate,
                                                       final FieldPVCoordinatesProvider<T> relay,
                                                       final FieldPVCoordinatesProvider<T> emitter) {
        return value(frame, receiverPV, receptionDate, relay, receptionDate, emitter, receptionDate);
    }
    /**
     * Compute measurement.
     * @param <T> field type
     * @param frame            frame where position is given
     * @param receiverPV       end receiver position and velocity (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param approxRelayDate  guess for the relay date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return range rate (m/s)
     */
    public <T extends CalculusFieldElement<T>> T value(final Frame frame, final FieldPVCoordinates<T> receiverPV,
                                                       final FieldAbsoluteDate<T> receptionDate,
                                                       final FieldPVCoordinatesProvider<T> relay,
                                                       final FieldAbsoluteDate<T> approxRelayDate,
                                                       final FieldPVCoordinatesProvider<T> emitter,
                                                       final FieldAbsoluteDate<T> approxEmissionDate) {
        // Compute relay and emission dates
        final T[] delays = twoWayTimer.computeDelays(frame, receiverPV.getPosition(), receptionDate, relay,
                approxRelayDate, emitter, approxEmissionDate);
        final FieldAbsoluteDate<T> relayDate = receptionDate.shiftedBy(delays[1].negate());
        final FieldAbsoluteDate<T> emissionDate = relayDate.shiftedBy(delays[0].negate());

        // Compute position and velocity of interest
        final FieldPVCoordinates<T> relayPV = relay.getPVCoordinates(relayDate, frame);
        final FieldPVCoordinates<T> emitterPV = emitter.getPVCoordinates(emissionDate, frame);

        // Range-rate components
        final FieldVector3D<T> receiverRelativePosition = receiverPV.getPosition().subtract(relayPV.getPosition());
        final FieldVector3D<T> emitterRelativePosition = emitterPV.getPosition().subtract(relayPV.getPosition());
        final FieldVector3D<T> receiverRelativeVelocity = receiverPV.getVelocity().subtract(relayPV.getVelocity());
        final FieldVector3D<T> emitterRelativeVelocity = emitterPV.getVelocity().subtract(relayPV.getVelocity());

        // range rate
        return FieldVector3D.dotProduct(receiverRelativePosition.normalize(), receiverRelativeVelocity).add(
                FieldVector3D.dotProduct(emitterRelativePosition.normalize(), emitterRelativeVelocity));
    }
}
