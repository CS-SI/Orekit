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

    /** Site name. */
    private final String siteName;

    /** Site location. */
    private final GeodeticPoint siteLocation;

    /** Main tides by species and increasing rate. */
    private final Tide[][] tides;

    /** Amplitude along zenith axis for main tides by species and increasing rate. */
    private final double[][] zAmplitude;

    /** Phase along zenith axis for main tides by species and increasing rate. */
    private final double[][] zPhase;

    /** Amplitude along West axis for main tides by species and increasing rate. */
    private final double[][] wAmplitude;

    /** Phase along West axis for main tides by species and increasing rate. */
    private final double[][] wPhase;

    /** Amplitude along South axis for main tides by species and increasing rate. */
    private final double[][] sAmplitude;

    /** Phase along South axis for main tides by species and increasing rate. */
    private final double[][] sPhase;

    /** Simple constructor.
     * <p>
     * Arrays must be organized by species and sorted in increasing rate order.
     * @param siteName site name
     * @param siteLocation site location
     * @param tides main tides, by species and increasing rate
     * @param zAmplitude amplitude along zenith axis
     * @param zPhase phase along zenith axis
     * @param wAmplitude amplitude along West
     * @param wPhase phase along West axis
     * @param sAmplitude amplitude along South
     * @param sPhase phase along South axis
     */
    public OceanLoadingCoefficients(final String siteName, final GeodeticPoint siteLocation,
                                    final Tide[][] tides,
                                    final double[][] zAmplitude, final double[][] zPhase,
                                    final double[][] wAmplitude, final double[][] wPhase,
                                    final double[][] sAmplitude, final double[][] sPhase) {
        this.siteName     = siteName;
        this.siteLocation = siteLocation;
        this.tides        = copy(tides);
        this.zAmplitude   = copy(zAmplitude);
        this.zPhase       = copy(zPhase);
        this.wAmplitude   = copy(wAmplitude);
        this.wPhase       = copy(wPhase);
        this.sAmplitude   = copy(sAmplitude);
        this.sPhase       = copy(sPhase);
    }

    /** Deep copy of a variable rows tides array.
     * @param array to copy
     * @return copied array
     */
    private Tide[][] copy(final Tide[][] array) {
        final Tide[][] copied = new Tide[array.length][];
        for (int i = 0; i < array.length; ++i) {
            copied[i] = array[i].clone();
        }
        return copied;
    }

    /** Deep copy of a variable rows double array.
     * @param array to copy
     * @return copied array
     */
    private double[][] copy(final double[][] array) {
        final double[][] copied = new double[array.length][];
        for (int i = 0; i < array.length; ++i) {
            copied[i] = array[i].clone();
        }
        return copied;
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

    /** Get the tide.
     * @param i species
     * @param j tide in the species
     * @return tide
     */
    public Tide getTide(final int i, final int j) {
        return tides[i][j];
    }

    /** Get the amplitude along zenith axis.
     * @param i species
     * @param j tide in the species
     * @return amplitude along zenith axis
     */
    public double getZenithAmplitude(final int i, final int j) {
        return zAmplitude[i][j];
    }

    /** Get the phase along zenith axis.
     * @param i species
     * @param j tide in the species
     * @return phase along zenith axis
     */
    public double getZenithPhase(final int i, final int j) {
        return zPhase[i][j];
    }

    /** Get the amplitude along west axis.
     * @param i species
     * @param j tide in the species
     * @return amplitude along west axis
     */
    public double getWestAmplitude(final int i, final int j) {
        return wAmplitude[i][j];
    }

    /** Get the phase along West axis.
     * @param i species
     * @param j tide in the species
     * @return phase along West axis
     */
    public double getWestPhase(final int i, final int j) {
        return wPhase[i][j];
    }

    /** Get the amplitude along South axis.
     * @param i species
     * @param j tide in the species
     * @return amplitude along South axis
     */
    public double getSouthAmplitude(final int i, final int j) {
        return sAmplitude[i][j];
    }

    /** Get the phase along South axis.
     * @param i species
     * @param j tide in the species
     * @return phase along South axis
     */
    public double getSouthPhase(final int i, final int j) {
        return sPhase[i][j];
    }

    /** Get the number of species.
     * @return number of species
     */
    public int getNbSpecies() {
        return tides.length;
    }

    /** Get the number of tides for one species.
     * @param species species index
     * @return number of tides for one species
     */
    public int getNbTides(final int species) {
        return tides[species].length;
    }

}

