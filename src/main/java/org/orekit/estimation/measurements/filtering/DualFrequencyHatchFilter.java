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
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.utils.Constants;

/**
 * Hatch Filter using Carrier-Phase measurements taken at two different frequencies,
 * to form a Divergence-Free phase combination.
 * <p>
 * This filter uses a phase combination to mitigate the effects of the
 * temporally varying ionospheric delays. Still, the spatial variation of the ionospheric delays
 * are not compensated by this phase combination.
 * </p>
 * @see "Subirana, J. S., Hernandez-Pajares, M., and José Miguel Juan Zornoza. (2013).
 *       GNSS Data Processing: Fundamentals and Algorithms. European Space Agency.
 *       Section 4.2.3.1.1"
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class DualFrequencyHatchFilter extends HatchFilter {

    /** First wavelength used for smoothing. */
    private double wavelengthFreq1;

    /** Second wavelength used for smoothing. */
    private double wavelengthFreq2;

    /** List used to store the phase value of the first frequency. */
    private ArrayList<Double> phase1History;

    /** List used to store the phase value of the second frequency.*/
    private ArrayList<Double> phase2History;

    /**
     * Constructor for the Dual Frequency Hatch Filter.
     * <p>
     * The threshold parameter corresponds to the maximum difference between
     * non-smoothed and smoothed pseudo range value, above which the filter
     * is reset.
     * </p>
     * @param initCode        initial code measurement
     * @param initPhaseFreq1  initial phase measurement for the first chosen frequency
     * @param initPhaseFreq2  initial phase measurement for the second chosen frequency
     * @param wavelengthFreq1 initPhaseFreq1 observed value wavelength (m)
     * @param wavelengthFreq2 initPhaseFreq2 observed value wavelength (m)
     * @param threshold       threshold for loss of lock detection
     *                        (it represents the maximum difference between smoothed
     *                        and measured values for loss of lock detection)
     * @param N               window size of the Hatch Filter
     */
    public DualFrequencyHatchFilter(final ObservationData initCode,
                                    final ObservationData initPhaseFreq1, final ObservationData initPhaseFreq2,
                                    final double wavelengthFreq1, final double wavelengthFreq2,
                                    final double threshold, final int N) {
        super(threshold, N);
        // Initialize wavelength and compute frequencies
        this.wavelengthFreq1 = wavelengthFreq1;
        this.wavelengthFreq2 = wavelengthFreq2;

        // Initialize array of phase values used during smoothing
        this.phase1History = new ArrayList<>();
        this.phase2History = new ArrayList<>();
        phase1History.add(initPhaseFreq1.getValue() * wavelengthFreq1);
        phase2History.add(initPhaseFreq2.getValue() * wavelengthFreq2);
        updatePreviousSmoothedCode(initCode.getValue());
        updatePreviousSmoothingValue(divergenceFreeCombination(initPhaseFreq1.getValue(), initPhaseFreq2.getValue(), wavelengthFreq1, wavelengthFreq2));
        addToSmoothedCodeHistory(initCode.getValue());
        addToCodeHistory(initCode.getValue());
    }

    /**
     * This method filters the provided data given the state of the filter.
     * @param codeData       input code observation data
     * @param phaseDataFreq1 input phase observation data for the first frequency
     * @param phaseDataFreq2 input phase observation data for the second frequency
     * @return the smoothed observation data
     */
    public ObservationData filterData(final ObservationData codeData, final ObservationData phaseDataFreq1, final ObservationData phaseDataFreq2) {

        // Current code value
        final double code = codeData.getValue();
        addToCodeHistory(code);

        // Computes the phase combination and smoothing value (Ref Eq. 4.32)
        final double phaseFreq1 = wavelengthFreq1 * phaseDataFreq1.getValue();
        final double phaseFreq2 = wavelengthFreq2 * phaseDataFreq2.getValue();
        final double phaseDF = divergenceFreeCombination(phaseDataFreq1.getValue(), phaseDataFreq2.getValue(), wavelengthFreq1, wavelengthFreq2);
        phase1History.add(phaseFreq1);
        phase2History.add(phaseFreq2);

        // Check for carrier phase cycle slip (check on the two phase data)
        final boolean cycleSlip = FastMath.floorMod(phaseDataFreq1.getLossOfLockIndicator(), 2) != 0 ||
                        FastMath.floorMod(phaseDataFreq2.getLossOfLockIndicator(), 2) != 0;

        // Computes the smoothed code value
        double smoothedValue = smoothedCode(code, phaseDF);
        updatePreviousSmoothingValue(phaseDF);

        // Check if filter reset needed, if not return smoothedValue, and increase k if necessary.
        smoothedValue = checkValidData(code, smoothedValue, cycleSlip);
        addToSmoothedCodeHistory(smoothedValue);
        updatePreviousSmoothedCode(smoothedValue);

        // Return the smoothed observed data
        return new ObservationData(codeData.getObservationType(), smoothedValue,
                                   codeData.getLossOfLockIndicator(), codeData.getSignalStrength());

    }

    /**
     * Get the history of phase values of the first frequency.
     * @return the history of phase values of the first frequency
     */
    public ArrayList<Double> getFirstFrequencyPhaseHistory() {
        return phase1History;
    }

    /**
     * Get the history of phase values of the second frequency.
     * @return the history of phase values of the second frequency
     */
    public ArrayList<Double> getSecondFrequencyPhaseHistory() {
        return phase2History;
    }

    /**
     * Divergence-free combination (Ref Eq. 4.32).
     * <p>
     * phase_DF = phase_F1 + 2.0 * alpha + (phase_F1 - phase_F2)
     * </p>
     * @param phase1  phase value for frequency 1
     * @param phase2  phase value for frequency 2
     * @param lambda1 wavelength of the first phase (m)
     * @param lambda2 wavelength of the second phase (m)
     * @return the value of the divergence-free combination
     */
    private static double divergenceFreeCombination(final double phase1, final double phase2,
                                                    final double lambda1, final double lambda2) {

        // Multiply phase value by its wavelength
        final double phaseFreq1 = lambda1 * phase1;
        final double phaseFreq2 = lambda2 * phase2;

        // Convert wavelength to frequencies
        final double f1 = Constants.SPEED_OF_LIGHT / lambda1;
        final double f2 = Constants.SPEED_OF_LIGHT / lambda2;

        // Alpha
        final double alpha = 1.0 / ((f1 * f1) / (f2 * f2) - 1.0);

        // Return
        return  phaseFreq1 + 2.0 * alpha * (phaseFreq1 - phaseFreq2);

    }

}
