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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.ConvergenceChecker;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * Abstract class for computation of signal travel time in vacuum.
 * @since 14.0
 * @author Romain Serra
 * @author Luc Maisonnobe
 */
abstract class AbstractSignalTravelTime {

    /** Reciprocal for light speed. */
    protected static final double C_RECIPROCAL = 1.0 / Constants.SPEED_OF_LIGHT;

    /** Maximum number of iterations. */
    protected static final int DEFAULT_MAX_ITER = 10;

    /** Convergence checker. */
    private final ConvergenceChecker<Double> convergenceChecker;

    /**
     * Constructor.
     * @param convergenceChecker convergence checker
     */
    protected AbstractSignalTravelTime(final ConvergenceChecker<Double> convergenceChecker) {
        this.convergenceChecker = convergenceChecker;
    }

    /**
     * Get the default convergence checker.
     * @return checker
     */
    static ConvergenceChecker<Double> getDefaultConvergenceChecker() {
        return (iteration, previous, current) -> iteration != 0 && (iteration > DEFAULT_MAX_ITER ||
                FastMath.abs(previous - current) <= 2 * FastMath.ulp(current));
    }

    /**
     * Getter for the convergence checker.
     * @return checker
     */
    public ConvergenceChecker<Double> getConvergenceChecker() {
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
    protected double compute(final PVCoordinatesProvider pvCoordinatesProvider, final double initialOffset,
                             final Vector3D fixedPosition, final AbsoluteDate guessDate, final Frame frame) {
        double delay = initialOffset;

        // search signal transit date, computing the signal travel in inertial frame
        double previous = 0.;
        int count = 0;
        while (!convergenceChecker.converged(count, previous, delay)) {
            previous = delay;
            final double shift = computeShift(initialOffset, delay);
            final Vector3D pos    = pvCoordinatesProvider.getPosition(guessDate.shiftedBy(shift), frame);
            delay                 = fixedPosition.distance(pos) * C_RECIPROCAL;
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
    protected abstract double computeShift(double offset, double delay);

}
