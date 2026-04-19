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
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;

/**
 * Class for computing signal time of travel with an adjustable receiver and a fixed emitter's position and date.
 * The delay is calculated via a fixed-point algorithm with customizable settings (even enabling instantaneous transmission).
 * Note that a couple of iterations are usually enough for Earth orbits.
 * @since 14.0
 * @see AdjustableReceiverSignalTimer
 * @author Romain Serra
 */
public class FieldAdjustableReceiverSignalTimer<T extends CalculusFieldElement<T>>
        extends FieldAbstractSignalTravelTime<T> {

    /** Position/velocity provider of emitter. */
    private final FieldPVCoordinatesProvider<T> adjustableReceiverPVProvider;

    /**
     * Constructor with default iteration settings.
     * @param adjustableReceiverPVProvider adjustable receiver
     */
    public FieldAdjustableReceiverSignalTimer(final FieldPVCoordinatesProvider<T> adjustableReceiverPVProvider) {
        this(adjustableReceiverPVProvider, getDefaultConvergenceChecker());
    }

    /**
     * Constructor.
     * @param adjustableReceiverPVProvider adjustable receiver
     * @param convergenceChecker convergence checker for fixed-point algorithm
     */
    public FieldAdjustableReceiverSignalTimer(final FieldPVCoordinatesProvider<T> adjustableReceiverPVProvider,
                                              final ConvergenceChecker<T> convergenceChecker) {
        super(convergenceChecker);
        this.adjustableReceiverPVProvider = adjustableReceiverPVProvider;
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink) without custom guess.
     * @param emissionCondition signal emission condition
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public T computeDelay(final FieldSignalEmissionCondition<T> emissionCondition) {
        final FieldAbsoluteDate<T> emissionDate = emissionCondition.getEmissionDate();
        final FieldVector3D<T> receiverPosition = adjustableReceiverPVProvider.getPosition(emissionDate,
                emissionCondition.getReferenceFrame());
        final T distance = receiverPosition.subtract(emissionCondition.getEmitterPosition()).getNorm();
        final FieldAbsoluteDate<T> approxReceptionDate = emissionDate.shiftedBy(distance.multiply(C_RECIPROCAL));
        return computeDelay(emissionCondition, approxReceptionDate);
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * @param emissionCondition signal emission condition
     * @param approxReceptionDate approximate reception date
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    public T computeDelay(final FieldSignalEmissionCondition<T> emissionCondition,
                          final FieldAbsoluteDate<T> approxReceptionDate) {
        // initialize reception date search loop assuming the state is already correct
        final T offset = approxReceptionDate.durationFrom(emissionCondition.getEmissionDate());

        return compute(adjustableReceiverPVProvider, offset, emissionCondition.getEmitterPosition(), approxReceptionDate,
                emissionCondition.getReferenceFrame());
    }

    @Override
    protected T computeShift(final T offset, final T delay) {
        return delay.subtract(offset);
    }
}
