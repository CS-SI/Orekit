/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.models.earth.displacement;

import org.orekit.bodies.GeodeticPoint;

/**
 * Site specific coefficients for ocean loading.
 * <p>
 * Instances of this class are typically created by
 * {@link OceanLoadingCoefficientsBLQFactory} that parses
 * files from Onsala Space Observatory files in BLQ format
 * found in the Orekit data configuration.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable
 * </p>
 * @see org.orekit.estimation.measurements.GroundStation
 * @see OceanLoadingCoefficientsBLQFactory
 * @see OceanLoading
 * @since 9.1
 * @author Luc Maisonobe
 */
public class OceanLoadingCoefficients {

    /** Main tides used in the coefficients. */
    public enum MainTide {

        /** M₂ tide. */
        M2(Tide.M2),

        /** S₂ tide. */
        S2(Tide.S2),

        /** N₂ tide. */
        N2(Tide.N2),

        /** K₂ tide. */
        K2(Tide.K2),

        /** K₁ tide. */
        K1(Tide.K1),

        /** O₁ tide. */
        O1(Tide.O1),

        /** P₁ tide. */
        P1(Tide.P1),

        /** Q₁ tide. */
        Q1(Tide.Q1),

        /** Mf tide. */
        MF(Tide.MF),

        /** Mm tide. */
        MM(Tide.MM),

        /** Ssa tide. */
        SSA(Tide.SSA);

        /** Tide. */
        private final Tide tide;

        /** Simple constructor.
         * @param doodsonNumber Doodson number for the tide
         */
        MainTide(final Tide tide) {
            this.tide = tide;
        }

        /** Get the tide.
         * @return tide
         */
        public Tide getTide() {
            return tide;
        }

    }

    /** Site name. */
    private final String siteName;

    /** Site location. */
    private final GeodeticPoint siteLocation;

    /** Amplitude along zenith axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides. */
    private final double[] zAmplitude;

    /** Phase along zenith axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides. */
    private final double[] zPhase;

    /** Amplitude along West axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides. */
    private final double[] wAmplitude;

    /** Phase along West axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides. */
    private final double[] wPhase;

    /** Amplitude along South axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides. */
    private final double[] sAmplitude;

    /** Phase along South axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides. */
    private final double[] sPhase;

    /** Simple constructor.
     * @param siteName site name
     * @param siteLocation site location
     * @param zAmplitude amplitude along zenith axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides
     * @param zPhase phase along zenith axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides
     * @param wAmplitude amplitude along West axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides
     * @param wPhase phase along West axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides
     * @param sAmplitude amplitude along South axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides
     * @param sPhase phase along South axis for M₂, S₂, N₂, K₂, K₁, O₁, P₁, Q₁, Mf, Mm and Ssa tides
     */
    public OceanLoadingCoefficients(final String siteName, final GeodeticPoint siteLocation,
                                    final double[] zAmplitude, final double[] zPhase,
                                    final double[] wAmplitude, final double[] wPhase,
                                    final double[] sAmplitude, final double[] sPhase) {
        this.siteName     = siteName;
        this.siteLocation = siteLocation;
        this.zAmplitude   = zAmplitude.clone();
        this.zPhase       = zPhase.clone();
        this.wAmplitude   = wAmplitude.clone();
        this.wPhase       = wPhase.clone();
        this.sAmplitude   = sAmplitude.clone();
        this.sPhase       = sPhase.clone();
    }

    /** Get the site name.
     * @return site name
     */
    public String getSiteName() {
        return siteName;
    }

    /** Get the site location.
     * @return site location
     */
    public GeodeticPoint getSiteLocation() {
        return siteLocation;
    }

    /** Get the amplitude along zenith axis.
     * @param tide main tide to consider
     * @return amplitude along zenith axis
     */
    public double getZenithAmplitude(final MainTide tide) {
        return zAmplitude[tide.ordinal()];
    }

    /** Get the phase along zenith axis.
     * @param tide main tide to consider
     * @return phase along zenith axis
     */
    public double getZenithPhase(final MainTide tide) {
        return zPhase[tide.ordinal()];
    }

    /** Get the amplitude along west axis.
     * @param tide main tide to consider
     * @return amplitude along west axis
     */
    public double getWestAmplitude(final MainTide tide) {
        return wAmplitude[tide.ordinal()];
    }

    /** Get the phase along West axis.
     * @param tide main tide to consider
     * @return phase along West axis
     */
    public double getWestPhase(final MainTide tide) {
        return wPhase[tide.ordinal()];
    }

    /** Get the amplitude along South axis.
     * @param tide main tide to consider
     * @return amplitude along South axis
     */
    public double getSouthAmplitude(final MainTide tide) {
        return sAmplitude[tide.ordinal()];
    }

    /** Get the phase along South axis.
     * @param tide main tide to consider
     * @return phase along South axis
     */
    public double getSouthPhase(final MainTide tide) {
        return sPhase[tide.ordinal()];
    }

}

