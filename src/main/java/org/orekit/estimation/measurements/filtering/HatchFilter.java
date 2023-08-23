/* Copyright 2002-2023 CS GROUP
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

package org.orekit.estimation.measurements.filtering;

import java.util.ArrayList;

import org.hipparchus.util.FastMath;

/** Base class for Hatch filters
 * <p>
 * Hatch filters are generally used to smooth the pseudo-range measurements with a set
 * of measurements, that might be combined or not, in order to mitigate the effect of multipath.
 * <p>
 * The original Hatch filter uses a single carrier-phase measurement as a smoothing value,
 * while a divergence free combination of carrier phases can be used, as well as Doppler measurements.
 * </p>
 * @see "Subirana, J. S., Hernandez-Pajares, M., and José Miguel Juan Zornoza. (2013).
 *       GNSS Data Processing: Fundamentals and Algorithms. European Space Agency."
 *
 * @see "Zhou, Z., and Li, B. (2017). Optimal Doppler-aided smoothing strategy for GNSS navigation.
 *       GPS solutions, 21(1), 197-210."
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
abstract class HatchFilter {

    /** Current index for the filter. */
    private int k;

    /** Boolean used to delay the reset of the filter. */
    private boolean resetNextBoolean;

    /** Last smoothed value for the pseudo-range. */
    private double previousSmoothedCode;

    /** Last smoothing value. */
    private double previousSmoothingValue;

    /** History of the pseudo-range value, appended at each filter iteration. */
    private ArrayList<Double> codeHistory;

    /** History of the smoothed pseudo-range value, appended at each filter iteration. */
    private ArrayList<Double> smoothedCodeHistory;

    /** Threshold for the difference between smoothed and measured values. */
    private double threshold;

    /** Window size of the Hatch Filter. */
    private final int N;

    /**
     * Constructor for the Abstract Hatch Filter.
     * <p>
     * Initialize the variables and set the initial pseudo-range state.
     * </p>
     * @param threshold threshold for loss of lock detection
     *                  (it represents the maximum difference between smoothed
     *                  and measured values for loss of lock detection)
     * @param N         window size of the Hatch Filter
     */
    HatchFilter(final double threshold, final int N) {
        // Initialize fields
        this.codeHistory         = new ArrayList<>();
        this.smoothedCodeHistory = new ArrayList<>();
        this.threshold           = threshold;
        this.resetNextBoolean    = false;
        this.k                   = 1;
        this.N                   = N;
    }

    /**
     * Get the history of the pseudo-range values used by the filter.
     * @return the history of the pseudo-range values used by the filter
     */
    public ArrayList<Double> getCodeHistory() {
        return codeHistory;
    }

    /**
     * Get the history of the smoothed pseudo-range values used by the filter.
     * @return the smoothedCodeHistory
     */
    public ArrayList<Double> getSmoothedCodeHistory() {
        return smoothedCodeHistory;
    }

    /**
     * Get the threshold for loss of lock detection.
     * <p>
     * It represents the maximum difference between smoothed
     * and measured values for loss of lock detection.
     * </p>
     * @return the threshold for loss of lock detection
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Update the previous smoothed value for the pseudo-range.
     * @param smoothedCode the previous smoothed value for the pseudo-range
     */
    public void updatePreviousSmoothedCode(final double smoothedCode) {
        this.previousSmoothedCode = smoothedCode;
    }

    /**
     * Update the previous smoothing value.
     * @param smoothingValue the previous smoothing value
     */
    public void updatePreviousSmoothingValue(final double smoothingValue) {
        this.previousSmoothingValue = smoothingValue;
    }

    /**
     * Add a value to the history of the pseudo-range value.
     * @param codeValue the value to add to the history
     */
    public void addToCodeHistory(final double codeValue) {
        codeHistory.add(codeValue);
    }

    /**
     * Add a value to the history of the smoothed pseudo-range value.
     * @param smoothedCodeValue the value to add to the history
     */
    public void addToSmoothedCodeHistory(final double smoothedCodeValue) {
        smoothedCodeHistory.add(smoothedCodeValue);
    }

    /**
     * Reset the filter in the case of a NaN phase value, skipping the smoothing at the present instant
     * and initializing at the next one, keeping the current code value.
     *
     * @param codeValue pseudo range value before the reset.
     * */
    public void resetFilterNext(final double codeValue) {
        resetK();
        resetNextBoolean();
        updatePreviousSmoothingValue(Double.NaN);
        updatePreviousSmoothedCode(codeValue);
    }

    /**
     * Set the flag for resetting the filter to true.
     */
    protected void resetNextBoolean() {
        this.resetNextBoolean = true;
    }

    /**
     * Reset the current index of the filter to 1.
     */
    protected void resetK() {
        this.k = 1;
    }

    /**
     * Computes the smoothed code value.
     * <p>
     * This method corresponds to Eq. (4.23) of
     * "Subirana, J. S., Hernandez-Pajares, M., and José Miguel Juan Zornoza.
     *  GNSS Data Processing: Fundamentals and Algorithms.
     *  European Space Agency."
     * </p>
     * @param codeValue value of the input code measurement (non-smoothed)
     * @param smoothingValue value of the measurement used to smooth the code measurement
     * @return the smoothed code value
     */
    protected double smoothedCode(final double codeValue,
                                  final double smoothingValue) {
        // Equation 4.23
        return (codeValue + (k - 1) * (previousSmoothedCode + (smoothingValue - previousSmoothingValue))) / k;
    }

    /**
     * Checks if need to reset the filter or if cycle slip occurred.
     * <p>
     * If yes, the smoothed value is reinitialized to the last code value.
     * If no, the smoothed value value is not changed and the filter counter is updated.
     * </p>
     * @param codeValue value of the input code measurement (non-smoothed)
     * @param smoothedValue the smoothed code value
     * @param cycleSlip true if cycle slip is detected
     * @return the smoothed or non-smoothed value depending if the filter must be reseted
     */
    protected double checkValidData(final double codeValue, final double smoothedValue, final boolean cycleSlip) {

        // Initialize the returned value to the smoothed value
        double validValue = smoothedValue;

        // This algorithm must be initialised every time that a carrier phase cycle slip occurs
        // Check if need to reset the filter
        if (resetNextBoolean || cycleSlip || FastMath.abs(smoothedValue - codeValue) > threshold) {
            // The smoothed value is updated to the non smoothed value, and reset the filter counter to 1
            resetK();
            validValue = codeValue;
        } else {
            // No reset, just increase the counter
            k = (k > N) ? N : k + 1;
        }

        // Return
        return validValue;

    }

}
