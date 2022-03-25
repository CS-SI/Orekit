/* Copyright 2002-2022 CS GROUP
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
import org.orekit.gnss.ObservationData;

/**
 *
 * @author Louis Aucouturier
 *
 */
public class AbstractHatchFilter {

    /** Current index for the filter. */
    private int k;

    /** Window size of the Hatch Filter. */
    private int N;

    /** Boolean used to delay the reset of the filter.*/
    private boolean resetNextBoolean;

    /** Last smoothed value for the pseudo-range.
     * Stored as needed by the Hatch filter. */
    private double oldSmoothedCode;

    /** History of the pseudo-range value, appended at each filter iteration.*/
    private ArrayList<Double> codeHistory;

    /** History of the smoothed pseudo-range value, appended at each filter iteration.*/
    private ArrayList<Double> smoothedCodeHistory;

    /** Format string to write the history files. */
    private String fmt = "%s";

    /** Line jump string. */
    private String lineJump = "\n";

    /** Threshold for the difference between smoothed and measured values.*/
    private double threshold;


    /**
     * Constructor for the Abstract Hatch Filter.
     * Initialize the variables and set the initial pseudo-range state.
     *
     * @param oldCode
     */
    AbstractHatchFilter(final double oldCode) {
        this.oldSmoothedCode = oldCode;
        this.codeHistory = new ArrayList<Double>();
        this.smoothedCodeHistory = new ArrayList<Double>();

        this.smoothedCodeHistory.add(oldCode);
        this.codeHistory.add(oldCode);

        this.threshold = 100;

        this.resetNextBoolean = false;
    }

    /**
     * Constructor for the Abstract Hatch Filter.
     * Initialize the variables and set the initial pseudo-range state.
     *
     * @param oldCode
     * @param threshold
     */
    AbstractHatchFilter(final double oldCode, final double threshold) {
        this.oldSmoothedCode = oldCode;
        this.codeHistory = new ArrayList<Double>();
        this.smoothedCodeHistory = new ArrayList<Double>();

        this.smoothedCodeHistory.add(oldCode);
        this.codeHistory.add(oldCode);

        this.threshold = threshold;

        this.resetNextBoolean = false;
    }

    /**
     * @return the codeHistory
     */
    public final ArrayList<Double> getCodeHistory() {
        return codeHistory;
    }

    /**
     * @return the smoothedCodeHistory
     */
    public final ArrayList<Double> getSmoothedCodeHistory() {
        return smoothedCodeHistory;
    }

    /**
     * @param threshold the threshold to set
     */
    public final void setThreshold(final double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return the threshold
     */
    public final double getThreshold() {
        return threshold;
    }

    /**
     * @param resetNextBoolean the resetNextBoolean to set
     */
    public final void setResetNextBoolean(final boolean resetNextBoolean) {
        this.resetNextBoolean = resetNextBoolean;
    }


    /**
     * @return the n
     */
    public final int getN() {
        return N;
    }


    /**
     * @param k the k to set
     */
    public final void setK(final int k) {
        this.k = k;
    }


    /**
     * @param n the n to set
     */
    public final void setN(final int n) {
        N = n;
    }

    /**
     * @param oldSmoothedCode the oldSmoothedCode to set
     */
    final void setOldSmoothedCode(final double oldSmoothedCode) {
        this.oldSmoothedCode = oldSmoothedCode;
    }

    /**
     * Add a value to the code history DescriptiveStatistics object.
     * @param codeValue
     */
    final void addToCodeHistory(final double codeValue) {
        codeHistory.add(codeValue);
    }

    /**
     * Add a value to the smoothed code history DescriptiveStatistics object.
     * @param smoothedCodeValue
     */
    final void addToSmoothedCodeHistory(final double smoothedCodeValue) {
        smoothedCodeHistory.add(smoothedCodeValue);
    }

    /**
     * Smoothing computation, to be moved to specific classes.
     *
     * @param codeValue
     * @param smoothingValue
     * @return smoothed Value
     */
    protected double filterComputation(final double codeValue, final double smoothingValue) {
        final double alpha = k - 1;
        final double filteredRange = alpha / k * (oldSmoothedCode + smoothingValue) +  codeValue / k;
        return filteredRange;
    }


    protected double checkValidData(final double codeValue, final double smoothedValue, final boolean checkLLI) {
        double validValue = smoothedValue;
        // Check if need to reset the filter or if Loss of lock
        // then set smoothedValue as the non smoothed value, and reset the counter to 1.
        // Else use smoothed value, and increase counter.
        if (resetNextBoolean || !checkLLI || FastMath.abs(smoothedValue - codeValue) > threshold) {
            setK(1);
            validValue = codeValue;
        } else {
            final int tempK = (k > getN()) ? getN() : k + 1;
            setK(tempK);
        }

        return validValue;
    }


    /**
     * Copy and modify an existing ObservationData object,
     * namely the code data in the hatch filter case.
     *
     * @param codeData
     * @param newValue
     * @return codeData, with smoothed code value.
     */
    protected ObservationData modifyObservationData(final ObservationData codeData, final double newValue) {
        return new ObservationData(codeData.getObservationType(),
                newValue,
                codeData.getLossOfLockIndicator(),
                codeData.getSignalStrength());
    }



}
