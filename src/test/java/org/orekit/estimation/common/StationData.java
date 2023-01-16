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

package org.orekit.estimation.common;

import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.estimation.measurements.modifiers.AngularRadioRefractionModifier;
import org.orekit.estimation.measurements.modifiers.Bias;
import org.orekit.estimation.measurements.modifiers.RangeTroposphericDelayModifier;
import org.orekit.models.earth.ionosphere.IonosphericModel;

/** Container for stations-related data.
 * @author Luc Maisonobe
 */
class StationData {

    /** Ground station. */
    private final GroundStation station;

    /** Range sigma. */
    private final double rangeSigma;

    /** Range bias (may be null if bias is fixed to zero). */
    private final Bias<Range> rangeBias;

    /** Range rate sigma. */
    private final double rangeRateSigma;

    /** Range rate bias (may be null if bias is fixed to zero). */
    private final Bias<RangeRate> rangeRateBias;

    /** Azimuth-elevation sigma. */
    private final double[] azElSigma;

    /** Azimuth-elevation bias (may be null if bias is fixed to zero). */
    private final Bias<AngularAzEl> azELBias;

    /** Elevation refraction correction (may be null). */
    private final AngularRadioRefractionModifier refractionCorrection;

    /** Tropospheric correction (may be null). */
    private final RangeTroposphericDelayModifier rangeTroposphericCorrection;

    /** Ionospheric model (may be null). */
    private final IonosphericModel ionosphericModel;

    /** Simple constructor.
     * @param station ground station
     * @param rangeSigma range sigma
     * @param rangeBias range bias (may be null if bias is fixed to zero)
     * @param rangeRateSigma range rate sigma
     * @param rangeRateBias range rate bias (may be null if bias is fixed to zero)
     * @param azElSigma azimuth-elevation sigma
     * @param azELBias azimuth-elevation bias (may be null if bias is fixed to zero)
     * @param refractionCorrection refraction correction for elevation (may be null)
     * @param rangeTroposphericCorrection tropospheric correction  for the range (may be null)
     * @param ionosphericModel ionospheric model for the range (may be null)
     */
    StationData(final GroundStation station,
                final double rangeSigma, final Bias<Range> rangeBias,
                final double rangeRateSigma, final Bias<RangeRate> rangeRateBias,
                final double[] azElSigma, final Bias<AngularAzEl> azELBias,
                final AngularRadioRefractionModifier refractionCorrection,
                final RangeTroposphericDelayModifier rangeTroposphericCorrection,
                final IonosphericModel ionosphericModel) {
        this.station                     = station;
        this.rangeSigma                  = rangeSigma;
        this.rangeBias                   = rangeBias;
        this.rangeRateSigma              = rangeRateSigma;
        this.rangeRateBias               = rangeRateBias;
        this.azElSigma                   = azElSigma.clone();
        this.azELBias                    = azELBias;
        this.refractionCorrection        = refractionCorrection;
        this.rangeTroposphericCorrection = rangeTroposphericCorrection;
        this.ionosphericModel            = ionosphericModel;
    }

    /** Get ground station.
     * @return ground station
     */
    public GroundStation getStation() {
        return station;
    }

    /** Get range sigma.
     * @return range sigma. */
    public double getRangeSigma() {
        return rangeSigma;
    }

    /** Range bias (may be null if bias is fixed to zero).
     * @return range bias
     */
    public Bias<Range> getRangeBias() {
        return rangeBias;
    }

    /** Range rate sigma.
     * @return range rate sigma
     */
    public double getRangeRateSigma() {
        return rangeRateSigma;
    }

    /** Range rate bias (may be null if bias is fixed to zero).
     * @return range rate bias
     */
    public Bias<RangeRate> getRangeRateBias() {
        return rangeRateBias;
    }

    /** Azimuth-elevation sigma.
     * @return azimuth-elevation sigma
     */
    public double[] getAzElSigma() {
        return azElSigma.clone();
    }

    /** Azimuth-elevation bias (may be null if bias is fixed to zero).
     * @return azimuth-elevation bias
     */
    public Bias<AngularAzEl> getAzELBias() {
        return azELBias;
    }

    /** Elevation refraction correction (may be null).
     * @return elevation refraction correction
     */
    public AngularRadioRefractionModifier getRefractionCorrection() {
        return refractionCorrection;
    }

    /** Tropospheric correction (may be null).
     * @return tropospheric correction
     */
    public RangeTroposphericDelayModifier getRangeTroposphericCorrection() {
        return rangeTroposphericCorrection;
    }

    /** Ionospheric model (may be null).
     * @return ionospheric model
     */
    public IonosphericModel getIonosphericModel() {
        return ionosphericModel;
    }

}
