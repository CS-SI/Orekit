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
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;

/**
 *
 * @author Louis Aucouturier
 *
 */
public class CarrierPhaseHatchFilterDualFrequency extends AbstractHatchFilter {

    /** Dual frequency phase combination used at the previous filter iteration.
     * Used in the phase difference of the Divergence-Free Hatch Filter*/
    private double oldPhaseDF;

    /** Wavelength for the first carrier phase object.
     * Linked to the observation type. */
    private double wavelengthF1;

    /** Wavelength for the second carrier phase object.
     * Linked to the observation type. */
    private double wavelengthF2;

    /** Constant defined as 1/(gamma12 - 1), with gamma12 the squared ratio of frequencies.*/
    private double alpha1tilde;

    /** ObservationType for the first phase input.*/
    private ObservationType obsTypePhaseF1;

    /** ObservationType for the second phase input.*/
    private ObservationType obsTypePhaseF2;

    /** List used to store the phase value of the first frequency. */
    private ArrayList<Double> phase1History;

    /** List used to store the phase value of the second frequency.*/
    private ArrayList<Double> phase2History;


    /**
     * Constructor for the class.
     *
     * Computes constant parameters and set the initial state of the filter.
     * The threshold parameter corresponds to the maximum difference between
     * non-smoothed and smoothed pseudo range value, above which the filter
     * is reset.
     *
     * @param codeData : initial Pseudo Range observation data
     * @param phaseDataFreq1 : CarrierPhase ObservationData for the first chosen observationType.
     * @param phaseDataFreq2 : CarrierPhase ObservationData for the first chosen observationType.
     * @param satSystem :SatelliteSystem used
     * @param N : Maximum window size
     * @param threshold : Threshold value
     */
    public CarrierPhaseHatchFilterDualFrequency(final ObservationData codeData,
            final ObservationData phaseDataFreq1,
            final ObservationData phaseDataFreq2,
            final SatelliteSystem satSystem,
            final int N,
            final double threshold) {

        // Initialize Abstract class
        super(codeData.getValue(), threshold);

        this.wavelengthF1 = phaseDataFreq1.getObservationType().getFrequency(satSystem).getWavelength();
        this.wavelengthF2 = phaseDataFreq2.getObservationType().getFrequency(satSystem).getWavelength();

        this.obsTypePhaseF1 = phaseDataFreq1.getObservationType();
        this.obsTypePhaseF2 = phaseDataFreq2.getObservationType();

        final double f1 = phaseDataFreq1.getObservationType().getFrequency(satSystem).getMHzFrequency();
        final double f2 = phaseDataFreq2.getObservationType().getFrequency(satSystem).getMHzFrequency();

        this.alpha1tilde = 1 / ( (f1 * f1) / (f2 * f2) - 1 );
        this.oldPhaseDF = phaseDataFreq1.getValue() * wavelengthF1 + 2 * alpha1tilde *
                (phaseDataFreq1.getValue() * wavelengthF1 - phaseDataFreq2.getValue() * wavelengthF2);

        this.phase1History = new ArrayList<>();
        this.phase2History = new ArrayList<>();
        phase1History.add(phaseDataFreq1.getValue() * wavelengthF1);
        phase2History.add(phaseDataFreq2.getValue() * wavelengthF2);

        setN(N);
        setK(1);
    }

    /** Reset the filter in the case of a NaN phase value, skipping the smoothing at the present instant
     * and initializing at the next one, keeping the current code value.
     *
     * @param codeValue : pseudo range value before the reset.
     * */
    public void resetFilterNext(final double codeValue) {
        setK(1);
        setResetNextBoolean(true);
        oldPhaseDF = Double.NaN;
        setOldSmoothedCode(codeValue);
    }

    /** Filters the provided data given the state of the filter.
     * Uses the Hatch Filter with the Divergence-free phase combination.
     *
     * @param codeData : Pseudo Range observation data
     * @param phaseDataFreq1 : CarrierPhase ObservationData for the first chosen observationType.
     * @param phaseDataFreq2 : CarrierPhase ObservationData for the second chosen observationType.
     * @return modified ObservationData : PseudoRange observationData with a smoothed value.
     * */
    public ObservationData filterData(final ObservationData codeData, final ObservationData phaseDataFreq1, final ObservationData phaseDataFreq2) {

        final boolean checkLLI = FastMath.floorMod(phaseDataFreq1.getLossOfLockIndicator(), 2) == 0 || FastMath.floorMod(phaseDataFreq2.getLossOfLockIndicator(), 2) == 0;

        phase1History.add(phaseDataFreq1.getValue() * wavelengthF1);
        phase2History.add(phaseDataFreq2.getValue() * wavelengthF2);

        // Computes the phase combination and smoothing value.
        final double phaseDF = phaseDataFreq1.getValue() * wavelengthF1 + 2 * alpha1tilde *
                (phaseDataFreq1.getValue() * wavelengthF1 - phaseDataFreq2.getValue() * wavelengthF2);
        final double smoothingValue = phaseDF - oldPhaseDF;

        final double smoothedValue = filterComputation(codeData.getValue(), smoothingValue);

        // Check if filter reset needed, if not return smoothedValue, and increase k if necessary.
        final double newValue = checkValidData(codeData.getValue(), smoothedValue, checkLLI);

        addToCodeHistory(codeData.getValue());
        addToSmoothedCodeHistory(newValue);
        // Modify the value stored in the abstract class.
        setOldSmoothedCode(newValue);
        // Set phase as oldPhase for next step.
        oldPhaseDF = phaseDF;
        return modifyObservationData(codeData, newValue);
    }


    /** Getter for the first carrier-phase observationType.
     *
     * @return the obsTypePhaseF1
     */
    public ObservationType getObsTypePhaseF1() {
        return obsTypePhaseF1;
    }


    /** Getter for the second carrier-phase observationType.
     *
     * @return the obsTypePhaseF2
     */
    public ObservationType getObsTypePhaseF2() {
        return obsTypePhaseF2;
    }

    /** Getter for the phase1History list, that stores the phase values corresponding to
     * the first frequency carrier-phase observationType.
     * This list stores double values.
     *
     * @return the phase1History
     */
    public final ArrayList<Double> getPhase1History() {
        return phase1History;
    }

    /** Getter for the phase2History list, that stores the phase values corresponding to
     * the second frequency carrier-phase observationType.
     * This list stores double values.
     *
     * @return the phase2History
     */
    public final ArrayList<Double> getPhase2History() {
        return phase2History;
    }
}
