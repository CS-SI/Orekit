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
 * Class for computing signal time of travel with an adjustable receiver and fixed emitter's position.
 * The delay is calculated via a fixed-point algorithm with customizable settings (even enabling instantaneous transmission).
 * Note that a couple of iterations are usually enough for Earth orbits.
 * @since 14.0
 * @author Romain Serra
 */
public class AdjustableReceiverSignalTimer extends AbstractSignalTravelTime {

    /** Position/velocity provider of receiver. */
    private final PVCoordinatesProvider adjustableReceiverPVProvider;

    /**
     * Constructor.
     * @param adjustableReceiverPVProvider adjustable receiver
     */
    public AdjustableReceiverSignalTimer(final PVCoordinatesProvider adjustableReceiverPVProvider) {
        this(adjustableReceiverPVProvider, getDefaultConvergenceChecker());
    }

    /**
     * Constructor.
     * @param adjustableReceiverPVProvider adjustable receiver
     * @param checker convergence checker for fixed-point algorithm
     */
    public AdjustableReceiverSignalTimer(final PVCoordinatesProvider adjustableReceiverPVProvider,
                                         final ConvergenceChecker<Double> checker) {
        super(checker);
        this.adjustableReceiverPVProvider = adjustableReceiverPVProvider;
    }

    /** Compute signal reception condition on a link leg (typically downlink or uplink).
     * @param emissionCondition signal emission conditions
     * @param approxReceptionDate approximate reception date
     * @return reception condition
     */
    public SignalReceptionCondition computeReceptionCondition(final SignalEmissionCondition emissionCondition,
                                                              final AbsoluteDate approxReceptionDate) {
        final double delay = computeDelay(emissionCondition, approxReceptionDate);
        final AbsoluteDate receptionDate = approxReceptionDate.shiftedBy(delay);
        final Frame frame = emissionCondition.getReferenceFrame();
        return new SignalReceptionCondition(receptionDate, adjustableReceiverPVProvider.getPosition(receptionDate, frame), frame);
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink) without custom guess.
     * @param emissionCondition signal emission conditions
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public double computeDelay(final SignalEmissionCondition emissionCondition) {
        final AbsoluteDate emissionDate = emissionCondition.getEmissionDate();
        final Vector3D emitterPosition = emissionCondition.getEmitterPosition();
        final Frame frame = emissionCondition.getReferenceFrame();
        final Vector3D receiverPosition = adjustableReceiverPVProvider.getPosition(emissionDate, frame);
        final double distance = receiverPosition.subtract(emitterPosition).getNorm2();
        final AbsoluteDate approxReceptionDate = emissionDate.shiftedBy(distance * C_RECIPROCAL);
        return computeDelay(emissionCondition, approxReceptionDate);
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param emissionCondition signal emission conditions
     * @param approxReceptionDate approximate reception date
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public double computeDelay(final SignalEmissionCondition emissionCondition,
                               final AbsoluteDate approxReceptionDate) {
        // initialize reception date search loop assuming the state is already correct
        final double offset = approxReceptionDate.durationFrom(emissionCondition.getEmissionDate());

        return compute(adjustableReceiverPVProvider, offset, emissionCondition.getEmitterPosition(), approxReceptionDate,
                emissionCondition.getReferenceFrame());
    }

    @Override
    protected double computeShift(final double offset, final double delay) {
        return delay - offset;
    }
}
