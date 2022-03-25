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

import org.orekit.gnss.ObservationData;
import org.orekit.gnss.SatelliteSystem;

/** Hatch Filter using Doppler measurements for smoothing.
 * <p>
 * This filter takes advantage of the absence of cycle slip in Doppler measurements as opposed to carrier-phase.
 * This property facilitates the use of such filter, still as described in {Zhou et Li, 2017}, this smoothing can present a bias.
 * In the orbit determination case, this bias can be estimated during the orbit determination process.
 * Furthermore, the results of this filter have not been checked against a trusted source.
 * </p>
 *
 * Based on Optimal Doppler-aided smoothing strategy for GNSS navigation, by Zhou et Li
 * @author Louis Aucouturier
 *
 */
public class DopplerHatchFilter extends AbstractHatchFilter {

    /** Previous Doppler measurement used for the Hatch filter smoothing. */
    private double oldDoppler;

    /** Interval time between two measurements. */
    private double integrationTime;

    /** Wavelength corresponding to the frequency
     * onto which the Doppler measurement is realised.*/
    private double wavelength;

    DopplerHatchFilter(final ObservationData codeData,
            final ObservationData dopplerData,
            final SatelliteSystem satSystem,
            final double integrationTime,
            final int N,
            final double threshold) {

        super(codeData.getValue(), threshold);

        this.wavelength = dopplerData.getObservationType().getFrequency(satSystem).getWavelength();
        this.integrationTime = integrationTime;
        this.oldDoppler = 0; //-0.5 * integrationTime * wavelength * dopplerData.getValue();
        setK(1);
        setN(N);
    }



    /** Reset the filter in the case of a NaN phase value, skipping the smoothing at the present instant
     * and initializing at the next one, keeping the current code value.
     * @param codeValue : pseudo range value before the reset.
     * */
    public void resetFilterNext(final double codeValue) {
        setK(1);
        setResetNextBoolean(true);
        oldDoppler = Double.NaN;
        setOldSmoothedCode(codeValue);
    }

    /** Filters the provided data given the state of the filter.
     * Uses the Hatch Filter with the Doppler measurement as the smoothing measurement.
     *
     * @param codeData : Pseudo Range observation data
     * @param dopplerData : Doppler ObservationData for the first observationType.
     * @return modified ObservationData : PseudoRange observationData modified with the smoothed value.
     * */
    public ObservationData filterData(final ObservationData codeData, final ObservationData dopplerData) {

        final double smoothingValue = -0.5 * integrationTime * wavelength * (dopplerData.getValue() + oldDoppler);

        final boolean checkLLI = true;

        final double smoothedValue = filterComputation(codeData.getValue(), smoothingValue);
        final double newValue = checkValidData(codeData.getValue(), smoothedValue, checkLLI);

        oldDoppler = dopplerData.getValue();

        addToCodeHistory(codeData.getValue());
        addToSmoothedCodeHistory(newValue);
        setOldSmoothedCode(newValue);
        return modifyObservationData(codeData, newValue);
    }

}
