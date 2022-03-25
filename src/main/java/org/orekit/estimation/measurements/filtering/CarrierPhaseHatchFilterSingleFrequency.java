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

import org.hipparchus.util.FastMath;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.SatelliteSystem;

/**
 *
 * @author Louis Aucouturier
 *
 */
public class CarrierPhaseHatchFilterSingleFrequency extends AbstractHatchFilter {

    /** Wavelength of the carrier phase.*/
    private double wavelength;

    /** Previous observed carrier phase.*/
    private double oldPhase;

    /**
     * Constructor for the Single Frequency Hatch Filter.
     *
     * The threshold parameter corresponds to the maximum difference between
     * non-smoothed and smoothed pseudo range value, above which the filter
     * is reset.
     *
     * @param codeData : initial Pseudo Range observation data
     * @param phaseData : CarrierPhase ObservationData for the chosen observationType.
     * @param satSystem : SatelliteSystem used.
     * @param N : Maximum window size
     * @param threshold
     */
    public CarrierPhaseHatchFilterSingleFrequency(final ObservationData codeData,
            final ObservationData phaseData,
            final SatelliteSystem satSystem,
            final int N,
            final double threshold) {
        super(codeData.getValue());
        this.wavelength = phaseData.getObservationType().getFrequency(satSystem).getWavelength();
        this.oldPhase = wavelength * phaseData.getValue();
        setN(N);
        setK(1);
    }

    /** Reset the filter in the case of a NaN phase value, skipping the smoothing at the present instant
     * and initializing at the next one, keeping the current code value.
     * @param codeValue : pseudo range value before the reset.
     * */
    public void resetFilterNext(final double codeValue) {
        setK(1);
        setResetNextBoolean(true);
        oldPhase = Double.NaN;
        setOldSmoothedCode(codeValue);
    }

    /** Filters the provided data given the state of the filter.
     * Uses the Hatch Filter using a single frequency carrier-phase.
     *
     * @param codeData : Pseudo Range observation data
     * @param phaseData : CarrierPhase ObservationData for the chosen observationType.
     * @return modified ObservationData : PseudoRange observationData with smoothed value.
     */
    public ObservationData filterData(final ObservationData codeData, final ObservationData phaseData) {

        final boolean checkLLI = FastMath.floorMod(phaseData.getLossOfLockIndicator(), 2) == 0;
        final double smoothingValue = wavelength * phaseData.getValue() - oldPhase;

        final double smoothedValue = filterComputation(codeData.getValue(), smoothingValue);
        final double newValue = checkValidData(codeData.getValue(), smoothedValue, checkLLI);

        oldPhase = wavelength * phaseData.getValue();

        addToCodeHistory(codeData.getValue());
        addToSmoothedCodeHistory(newValue);
        setOldSmoothedCode(newValue);
        return super.modifyObservationData(codeData, newValue);
    }


}
