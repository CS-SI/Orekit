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
package org.orekit.gnss.metric.messages.ssr.subtype;

import org.orekit.gnss.metric.messages.ssr.SsrData;

/**
 * Container for SSR IM201 data.
 * <p>
 * One instance of this class corresponds to one ionospheric layer.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIm201Data extends SsrData {

    /** Height of the ionospheric layer [m]. */
    private double heightIonosphericLayer;

    /** Spherical harmonics degree. */
    private int sphericalHarmonicsDegree;

    /** Spherical harmonics order. */
    private int sphericalHarmonicsOrder;

    /** Cosine parameters of spherical harmonics expansion of degree N and order M. */
    private double[][] cnm;

    /** Sine parameters of spherical harmonics expansion of degree N and order M. */
    private double[][] snm;

    /** Constructor. */
    public SsrIm201Data() {
        // Noting to do ...
    }

    /**
     * Get the height of the ionospheric layer.
     * @return the height of the ionospheric layer in meters
     */
    public double getHeightIonosphericLayer() {
        return heightIonosphericLayer;
    }

    /**
     * Set the height of the ionospheric layer.
     * @param heightIonosphericLayer the height to set in meters
     */
    public void setHeightIonosphericLayer(final double heightIonosphericLayer) {
        this.heightIonosphericLayer = heightIonosphericLayer;
    }

    /**
     * Get the degree of spherical harmonic expansion.
     * @return the degree of spherical harmonic expansion
     */
    public int getSphericalHarmonicsDegree() {
        return sphericalHarmonicsDegree;
    }

    /**
     * Set the degree of spherical harmonic expansion.
     * @param sphericalHarmonicsDegree the degree to set
     */
    public void setSphericalHarmonicsDegree(final int sphericalHarmonicsDegree) {
        this.sphericalHarmonicsDegree = sphericalHarmonicsDegree;
    }

    /**
     * Get the order of spherical harmonic expansion.
     * @return the order the order of spherical harmonic expansion
     */
    public int getSphericalHarmonicsOrder() {
        return sphericalHarmonicsOrder;
    }

    /**
     * Set the order of spherical harmonic expansion.
     * @param sphericalHarmonicsOrder the order to set
     */
    public void setSphericalHarmonicsOrder(final int sphericalHarmonicsOrder) {
        this.sphericalHarmonicsOrder = sphericalHarmonicsOrder;
    }

    /**
     * Get the cosine parameters of spherical harmonics expansion of degree N and order M.
     * <p>
     * The size of the array is (N + 1) x (M + 1)
     * </p>
     * @return the cosine parameters in TECU
     */
    public double[][] getCnm() {
        return cnm.clone();
    }

    /**
     * Set the cosine parameters of spherical harmonics expansion of degree N and order M.
     * @param cnm the parameters to set
     */
    public void setCnm(final double[][] cnm) {
        this.cnm = cnm.clone();
    }

    /**
     * Get the sine parameters of spherical harmonics expansion of degree N and order M.
     * <p>
     * The size of the array is (N + 1) x (M + 1)
     * </p>
     * @return the sine parameters in TECU
     */
    public double[][] getSnm() {
        return snm.clone();
    }

    /**
     * Set the sine parameters of spherical harmonics expansion of degree N and order M.
     * @param snm the parameters to set
     */
    public void setSnm(final double[][] snm) {
        this.snm = snm.clone();
    }

}
