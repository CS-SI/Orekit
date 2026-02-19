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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.ConvergenceChecker;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Class for computing signal time of travel with an adjustable emitter and a fixed receiver's position and date.
 * The delay is calculated via a fixed-point algorithm with customizable settings (even enabling instantaneous transmission).
 * Note that a couple of iterations are usually enough for Earth orbits.
 * @since 14.0
 * @author Romain Serra
 */
public class SignalTravelTimeAdjustableEmitter extends AbstractSignalTravelTime {

    /** Position/velocity provider of emitter. */
    private final PVCoordinatesProvider adjustableEmitterPVProvider;

    /**
     * Constructor with default iteration settings.
     * @param adjustableEmitterPVProvider adjustable emitter
     */
    public SignalTravelTimeAdjustableEmitter(final PVCoordinatesProvider adjustableEmitterPVProvider) {
        this(adjustableEmitterPVProvider, getDefaultConvergenceChecker());
    }

    /**
     * Constructor.
     * @param adjustableEmitterPVProvider adjustable emitter
     * @param checker convergence checker for fixed-point algorithm
     */
    public SignalTravelTimeAdjustableEmitter(final PVCoordinatesProvider adjustableEmitterPVProvider,
                                             final ConvergenceChecker<Double> checker) {
        super(checker);
        this.adjustableEmitterPVProvider = adjustableEmitterPVProvider;
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink) without custom guess.
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @param frame Inertial frame in which receiver is defined.
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public double computeDelay(final Vector3D receiverPosition, final AbsoluteDate signalArrivalDate, final Frame frame) {
        final Vector3D emitterPosition = adjustableEmitterPVProvider.getPosition(signalArrivalDate, frame);
        final double distance = receiverPosition.subtract(emitterPosition).getNorm();
        final AbsoluteDate approxEmissionDate = signalArrivalDate.shiftedBy(-distance * C_RECIPROCAL);
        return computeDelay(approxEmissionDate, receiverPosition, signalArrivalDate, frame);
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param approxEmissionDate approximate emission date
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @param frame Inertial frame in which receiver is defined.
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public double computeDelay(final AbsoluteDate approxEmissionDate, final Vector3D receiverPosition,
                               final AbsoluteDate signalArrivalDate, final Frame frame) {

        // initialize emission date search loop assuming the state is already correct
        final double offset = signalArrivalDate.durationFrom(approxEmissionDate);

        return compute(adjustableEmitterPVProvider, offset, receiverPosition, approxEmissionDate, frame);
    }

    @Override
    protected double computeShift(final double offset, final double delay) {
        return offset - delay;
    }
}
