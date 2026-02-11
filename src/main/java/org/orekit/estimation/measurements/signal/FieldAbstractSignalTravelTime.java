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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinatesProvider;

/**
 * Abstract class for computing signal travel time in vacuum.
 * @since 14.0
 * @author Romain Serra
 * @author Luc Maisonnobe
 */
abstract class FieldAbstractSignalTravelTime<T extends CalculusFieldElement<T>> {

    /** Reciprocal for light speed. */
    protected static final double C_RECIPROCAL = 1.0 / Constants.SPEED_OF_LIGHT;

    /** Maximum number of iterations. */
    private static final int DEFAULT_MAX_ITER = 10;

    /** Convergence checker. */
    private final ConvergenceChecker<T> convergenceChecker;

    /**
     * Constructor.
     * @param convergenceChecker convergence checker
     */
    protected FieldAbstractSignalTravelTime(final ConvergenceChecker<T> convergenceChecker) {
        this.convergenceChecker = convergenceChecker;
    }

    /**
     * Get the default convergence checker.
     * @return checker
     * @param <S> field type
     */
    static <S extends CalculusFieldElement<S>> ConvergenceChecker<S> getDefaultConvergenceChecker() {
        return (iteration, previous, current) -> iteration != 0 && (iteration > DEFAULT_MAX_ITER ||
                (previous.subtract(current)).norm() <= 2 * FastMath.ulp(current).getReal());
    }

    /**
     * Getter for the convergence checker.
     * @return checker
     */
    public ConvergenceChecker<T> getConvergenceChecker() {
        return convergenceChecker;
    }

    /** Compute propagation delay on a link leg (typically downlink or uplink).
     * The max. iteration number and convergence checker can be tweaked to emulate no-delay a.k.a. instantaneous transmission.
     * @param pvCoordinatesProvider adjustable emitter/receiver
     * @param initialOffset guess for the time off set
     * @param fixedPosition fixed receiver/emitter position
     * @param guessDate guess for emission/reception date
     * @param frame Inertial frame in which receiver/emitter is defined.
     * @return <em>positive</em> delay between signal emission and signal reception dates
     */
    protected T compute(final FieldPVCoordinatesProvider<T> pvCoordinatesProvider, final T initialOffset,
                        final FieldVector3D<T> fixedPosition, final FieldAbsoluteDate<T> guessDate, final Frame frame) {
        T delay = initialOffset;

        // search signal transit date, computing the signal travel in the frame shared by emitter and receiver
        T previous = initialOffset.getField().getZero();
        int count = 0;
        while (!convergenceChecker.converged(count, previous, delay)) {
            previous           = delay.add(0.0);
            final T shift = computeShift(initialOffset, delay);
            final FieldVector3D<T> position = pvCoordinatesProvider.getPosition(guessDate.shiftedBy(shift), frame);
            delay                           = position.distance(fixedPosition).multiply(C_RECIPROCAL);
            count++;
        }

        return delay;
    }

    /**
     * Computes the time shift.
     * @param offset time offset
     * @param delay time delay
     * @return time shift to use in computation
     */
    protected abstract T computeShift(T offset, T delay);

}
