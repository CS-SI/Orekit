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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.signal.TwoLegsSignalTravelTimer;
import org.orekit.frames.Frame;
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
public class TwoLegsRangeRateModel {

    /** Signal travel time model. */
    private final TwoLegsSignalTravelTimer twoWayTimer;

    /**
     * Constructor.
     * @param twoWayTimer two-way time delay computer
     */
    public TwoLegsRangeRateModel(final TwoLegsSignalTravelTimer twoWayTimer) {
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
        final Vector3D receiverRelativePosition = receiverPV.getPosition().subtract(relayPV.getPosition()).normalize();
        final Vector3D emitterRelativePosition = emitterPV.getPosition().subtract(relayPV.getPosition()).normalize();
        final Vector3D receiverRelativeVelocity = receiverPV.getVelocity().subtract(relayPV.getVelocity());
        final Vector3D emitterRelativeVelocity = emitterPV.getVelocity().subtract(relayPV.getVelocity());

        // range rate
        return Vector3D.dotProduct(receiverRelativePosition.normalize(), receiverRelativeVelocity) +
                Vector3D.dotProduct(emitterRelativePosition.normalize(), emitterRelativeVelocity);
    }

    /**
     * Compute measurement without guess.
     *
     * @param frame            frame where position is given
     * @param receiverPV       end receiver position and velocity (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param emitter          signal initial emitter coordinates provider
     * @return range rate (m/s)
     */
    public Gradient value(final Frame frame, final FieldPVCoordinates<Gradient> receiverPV,
                          final FieldAbsoluteDate<Gradient> receptionDate,
                          final FieldPVCoordinatesProvider<Gradient> relay,
                          final FieldPVCoordinatesProvider<Gradient> emitter) {
        return value(frame, receiverPV, receptionDate, relay, receptionDate, emitter, receptionDate);
    }
    /**
     * Compute measurement.
     *
     * @param frame            frame where position is given
     * @param receiverPV       end receiver position and velocity (at reception)
     * @param receptionDate    signal end reception date
     * @param relay            signal relay (initial reception) coordinates provider
     * @param approxRelayDate  guess for the relay date
     * @param emitter          signal initial emitter coordinates provider
     * @param approxEmissionDate guess for the emission date
     * @return range rate (m/s)
     */
    public Gradient value(final Frame frame, final FieldPVCoordinates<Gradient> receiverPV,
                          final FieldAbsoluteDate<Gradient> receptionDate,
                          final FieldPVCoordinatesProvider<Gradient> relay,
                          final FieldAbsoluteDate<Gradient> approxRelayDate,
                          final FieldPVCoordinatesProvider<Gradient> emitter,
                          final FieldAbsoluteDate<Gradient> approxEmissionDate) {
        // Compute relay and emission dates
        final Gradient[] delays = twoWayTimer.computeDelays(frame, receiverPV.getPosition(), receptionDate, relay,
                approxRelayDate, emitter, approxEmissionDate);
        final FieldAbsoluteDate<Gradient> relayDate = receptionDate.shiftedBy(delays[1].negate());
        final FieldAbsoluteDate<Gradient> emissionDate = relayDate.shiftedBy(delays[0].negate());

        // Compute position and velocity of interest
        final FieldPVCoordinates<Gradient> relayPV = relay.getPVCoordinates(relayDate, frame);
        final FieldPVCoordinates<Gradient> emitterPV = emitter.getPVCoordinates(emissionDate, frame);

        // Range-rate components
        final FieldVector3D<Gradient> receiverRelativePosition = receiverPV.getPosition().subtract(relayPV.getPosition()).normalize();
        final FieldVector3D<Gradient> emitterRelativePosition = emitterPV.getPosition().subtract(relayPV.getPosition()).normalize();
        final FieldVector3D<Gradient> receiverRelativeVelocity = receiverPV.getVelocity().subtract(relayPV.getVelocity());
        final FieldVector3D<Gradient> emitterRelativeVelocity = emitterPV.getVelocity().subtract(relayPV.getVelocity());

        // range rate
        return FieldVector3D.dotProduct(receiverRelativePosition.normalize(), receiverRelativeVelocity).add(
                FieldVector3D.dotProduct(emitterRelativePosition.normalize(), emitterRelativeVelocity));
    }
}
