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
import org.hipparchus.optim.ConvergenceChecker;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;

/**
 * Class for computing signal time of travel with an adjustable emitter and a fixed receiver's position and date.
 * The delay is calculated via a fixed-point algorithm with customizable settings (even enabling instantaneous transmission).
 * Note that a couple of iterations are usually enough for Earth orbits.
 * @since 14.0
 * @see SignalTravelTimeAdjustableEmitter
 * @author Romain Serra
 */
public class FieldSignalTravelTimeAdjustableEmitter<T extends CalculusFieldElement<T>>
        extends FieldAbstractSignalTravelTime<T> {

    /** Position/velocity provider of emitter. */
    private final FieldPVCoordinatesProvider<T> adjustableEmitterPVProvider;

    /**
     * Constructor with default iteration settings.
     * @param adjustableEmitterPVProvider adjustable emitter
     */
    public FieldSignalTravelTimeAdjustableEmitter(final FieldPVCoordinatesProvider<T> adjustableEmitterPVProvider) {
        this(adjustableEmitterPVProvider, getDefaultConvergenceChecker());
    }

    /**
     * Constructor.
     * @param adjustableEmitterPVProvider adjustable emitter
     * @param convergenceChecker convergence checker for fixed-point algorithm
     */
    public FieldSignalTravelTimeAdjustableEmitter(final FieldPVCoordinatesProvider<T> adjustableEmitterPVProvider,
                                                  final ConvergenceChecker<T> convergenceChecker) {
        super(convergenceChecker);
        this.adjustableEmitterPVProvider = adjustableEmitterPVProvider;
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink) without custom guess.
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @param frame Inertial frame in which receiver is defined.
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public T computeDelay(final FieldVector3D<T> receiverPosition, final FieldAbsoluteDate<T> signalArrivalDate,
                          final Frame frame) {
        final FieldVector3D<T> emitterPosition = adjustableEmitterPVProvider.getPosition(signalArrivalDate, frame);
        final T distance = receiverPosition.subtract(emitterPosition).getNorm();
        final FieldAbsoluteDate<T> approxEmissionDate = signalArrivalDate.shiftedBy(distance.multiply(-C_RECIPROCAL));
        return computeDelay(approxEmissionDate, receiverPosition, signalArrivalDate, frame);
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param approxEmissionDate approximate emission date
     * @param receiverPosition fixed position of receiver at {@code signalArrivalDate}
     * @param signalArrivalDate date at which the signal arrives to receiver
     * @param frame Inertial frame in which receiver is defined.
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public T computeDelay(final FieldAbsoluteDate<T> approxEmissionDate, final FieldVector3D<T> receiverPosition,
                          final FieldAbsoluteDate<T> signalArrivalDate, final Frame frame) {

        // Initialize emission date search loop assuming the emitter PV is almost correct
        final T offset = signalArrivalDate.durationFrom(approxEmissionDate);

        return compute(adjustableEmitterPVProvider, offset, receiverPosition, approxEmissionDate, frame);
    }

    @Override
    protected T computeShift(final T offset, final T delay) {
        return offset.subtract(delay);
    }
}
