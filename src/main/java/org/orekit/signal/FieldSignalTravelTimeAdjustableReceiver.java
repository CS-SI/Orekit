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
 * Class for computing signal time of travel with an adjustable receiver and a fixed emitter's position and date.
 * The delay is calculated via a fixed-point algorithm with customizable settings (even enabling instantaneous transmission).
 * Note that a couple of iterations are usually enough for Earth orbits.
 * @since 14.0
 * @see SignalTravelTimeAdjustableReceiver
 * @author Romain Serra
 */
public class FieldSignalTravelTimeAdjustableReceiver<T extends CalculusFieldElement<T>>
        extends FieldAbstractSignalTravelTime<T> {

    /** Position/velocity provider of emitter. */
    private final FieldPVCoordinatesProvider<T> adjustableReceiverPVProvider;

    /**
     * Constructor with default iteration settings.
     * @param adjustableReceiverPVProvider adjustable receiver
     */
    public FieldSignalTravelTimeAdjustableReceiver(final FieldPVCoordinatesProvider<T> adjustableReceiverPVProvider) {
        this(adjustableReceiverPVProvider, getDefaultConvergenceChecker());
    }

    /**
     * Constructor.
     * @param adjustableReceiverPVProvider adjustable receiver
     * @param convergenceChecker convergence checker for fixed-point algorithm
     */
    public FieldSignalTravelTimeAdjustableReceiver(final FieldPVCoordinatesProvider<T> adjustableReceiverPVProvider,
                                                   final ConvergenceChecker<T> convergenceChecker) {
        super(convergenceChecker);
        this.adjustableReceiverPVProvider = adjustableReceiverPVProvider;
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink) without custom guess.
     * @param emitterPosition fixed position of emitter
     * @param emissionDate emission date
     * @param frame inertial frame in which emitter is defined
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public T computeDelay(final FieldVector3D<T> emitterPosition, final FieldAbsoluteDate<T> emissionDate,
                          final Frame frame) {
        final FieldVector3D<T> receiverPosition = adjustableReceiverPVProvider.getPosition(emissionDate, frame);
        final T distance = receiverPosition.subtract(emitterPosition).getNorm();
        final FieldAbsoluteDate<T> approxReceptionDate = emissionDate.shiftedBy(distance.multiply(C_RECIPROCAL));
        return computeDelay(emitterPosition, emissionDate, approxReceptionDate, frame);
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param emitterPosition fixed position of emitter
     * @param emissionDate emission date
     * @param approxReceptionDate approximate reception date
     * @param frame inertial frame in which emitter is defined
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public T computeDelay(final FieldVector3D<T> emitterPosition,
                          final FieldAbsoluteDate<T> emissionDate,
                          final FieldAbsoluteDate<T> approxReceptionDate,
                          final Frame frame) {
        // initialize reception date search loop assuming the state is already correct
        final T offset = approxReceptionDate.durationFrom(emissionDate);

        return compute(adjustableReceiverPVProvider, offset, emitterPosition, approxReceptionDate, frame);
    }

    @Override
    protected T computeShift(final T offset, final T delay) {
        return delay.subtract(offset);
    }
}
