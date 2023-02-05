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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.SinCos;

/** Time-dependent part of a single component of spherical harmonics.
 * @author Luc Maisonobe
 * @since 11.1
 */
class TimeDependentHarmonic {

    /** Index of the trend reference in the gravity field. */
    private final int trendReferenceIndex;

    /** Base of the cosine coefficient. */
    private final double cBase;

    /** Base of the sine coefficient. */
    private final double sBase;

    /** Secular trend of the cosine coefficient. */
    private double cTrend;

    /** Secular trend of the sine coefficient. */
    private double sTrend;

    /** Indices of the reference dates in the gravity field. */
    private int[] cosReferenceIndices;

    /** Indices of the harmonic pulsations in the gravity field. */
    private int[] cosPulsationIndices;

    /** Cosine component of the cosine coefficient. */
    private double[] cosC;

    /** Cosine component of the sine coefficient. */
    private double[] cosS;

    /** Indices of the reference dates in the gravity field. */
    private int[] sinReferenceIndices;

    /** Indices of the harmonic pulsations in the gravity field. */
    private int[] sinPulsationIndices;

    /** Sine component of the cosine coefficient. */
    private double[] sinC;

    /** Sine component of the sine coefficient. */
    private double[] sinS;

    /** Build a part with only base.
     * @param trendReferenceIndex index of the trend reference in the gravity field
     * @param cBase base of the cosine coefficient
     * @param sBase base of the sine coefficient
     */
    TimeDependentHarmonic(final int trendReferenceIndex, final double cBase, final double sBase) {
        this(trendReferenceIndex, cBase, sBase, 0, 0);
    }

    /** Build a rescaled component.
     * @param scale scaling factor to apply to all coefficients elements
     * @param original original component
     */
    TimeDependentHarmonic(final double scale, final TimeDependentHarmonic original) {

        // rescale base
        this(original.trendReferenceIndex, scale * original.cBase, scale * original.sBase,
             original.cosReferenceIndices.length, original.sinReferenceIndices.length);

        // rescale trend
        cTrend = scale * original.cTrend;
        sTrend = scale * original.sTrend;

        // rescale cosine
        for (int i = 0; i < cosReferenceIndices.length; ++i) {
            cosReferenceIndices[i] = original.cosReferenceIndices[i];
            cosPulsationIndices[i] = original.cosPulsationIndices[i];
            cosC[i]                = scale * original.cosC[i];
            cosS[i]                = scale * original.cosS[i];
        }

        // rescale sine
        for (int i = 0; i < sinReferenceIndices.length; ++i) {
            sinReferenceIndices[i] = original.sinReferenceIndices[i];
            sinPulsationIndices[i] = original.sinPulsationIndices[i];
            sinC[i]                = scale * original.sinC[i];
            sinS[i]                = scale * original.sinS[i];
        }

    }

    /** Build a part with only base.
     * @param trendReferenceIndex index of the trend reference in the gravity field
     * @param cBase base of the cosine coefficient
     * @param sBase base of the sine coefficient
     * @param cSize initial size of the cosine arrays
     * @param sSize initial size of the sine arrays
     */
    private TimeDependentHarmonic(final int trendReferenceIndex, final double cBase, final double sBase,
                                  final int cSize, final int sSize) {

        // linear part
        this.trendReferenceIndex = trendReferenceIndex;
        this.cBase               = cBase;
        this.sBase               = sBase;
        this.cTrend              = 0.0;
        this.sTrend              = 0.0;

        // cosine component
        this.cosReferenceIndices = new int[cSize];
        this.cosPulsationIndices = new int[cSize];
        this.cosC                = new double[cSize];
        this.cosS                = new double[cSize];

        // sine component
        this.sinReferenceIndices = new int[sSize];
        this.sinPulsationIndices = new int[sSize];
        this.sinC                = new double[sSize];
        this.sinS                = new double[sSize];

    }

    /** Set the trend part.
     * @param cDot secular trend of the cosine coefficient (s⁻¹)
     * @param sDot secular trend of the sine coefficient (s⁻¹)
     */
    public void setTrend(final double cDot, final double sDot) {
        this.cTrend = cDot;
        this.sTrend = sDot;
    }

    /** Add a cosine component.
     * @param cosReferenceIndex index of the reference date in the gravity field
     * (if negative, use the trend reference index)
     * @param cosPulsationIndex index of the harmonic pulsation in the gravity field
     * @param cosineC cosine component of the cosine coefficient
     * @param cosineS cosine component of the sine coefficient
     */
    public void addCosine(final int cosReferenceIndex, final int cosPulsationIndex,
                          final double cosineC, final double cosineS) {
        final int refIndex = cosReferenceIndex < 0 ? trendReferenceIndex : cosReferenceIndex;
        this.cosReferenceIndices = addInt(refIndex, this.cosReferenceIndices);
        this.cosPulsationIndices = addInt(cosPulsationIndex, this.cosPulsationIndices);
        this.cosC                = addDouble(cosineC,     this.cosC);
        this.cosS                = addDouble(cosineS,     this.cosS);
    }

    /** Add a sine component.
     * @param sinReferenceIndex index of the reference date in the gravity field
     * (if negative, use the trend reference index)
     * @param sinPulsationIndex index of the harmonic pulsation in the gravity field
     * @param sineC sine component of the cosine coefficient
     * @param sineS sine component of the sine coefficient
     */
    public void addSine(final int sinReferenceIndex, final int sinPulsationIndex,
                        final double sineC, final double sineS) {
        final int refIndex = sinReferenceIndex < 0 ? trendReferenceIndex : sinReferenceIndex;
        this.sinReferenceIndices = addInt(refIndex, this.sinReferenceIndices);
        this.sinPulsationIndices = addInt(sinPulsationIndex, this.sinPulsationIndices);
        this.sinC                = addDouble(sineC,       this.sinC);
        this.sinS                = addDouble(sineS,       this.sinS);
    }

    /** Add an integer to an array.
     * <p>
     * Expanding the array one element at a time may seem a waste of time,
     * but we expect the array to be 0, 1 or 2 elements long only, and this
     * if performed only when reading gravity field, so its is worth doing
     * it this way.
     * </p>
     * @param n integer to add
     * @param array array where to add the integer
     * @return new array
     */
    private static int[] addInt(final int n, final int[] array) {
        final int[] newArray = new int[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = n;
        return newArray;
    }

    /** Add a double number to an array.
     * <p>
     * Expanding the array one element at a time may seem a waste of time,
     * but we expect the array to be 0, 1 or 2 elements long only, and this
     * if performed only when reading gravity field, so its is worth doing
     * it this way.
     * </p>
     * @param d double number to add
     * @param array array where to add the double number
     * @return new array
     */
    private static double[] addDouble(final double d, final double[] array) {
        final double[] newArray = new double[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = d;
        return newArray;
    }

    /** Compute the time-dependent part of a spherical harmonic cosine coefficient.
     * @param offsets offsets to reference dates in the gravity field
     * @param pulsations angular pulsations in the gravity field
     * @return raw coefficient Cnm
     */
    public double computeCnm(final double[] offsets, final SinCos[][] pulsations) {

        // trend effect
        double cnm = cBase + offsets[trendReferenceIndex] * cTrend;

        for (int i = 0; i < cosPulsationIndices.length; ++i) {
            // cosine effect
            cnm += cosC[i] * pulsations[cosReferenceIndices[i]][cosPulsationIndices[i]].cos();
        }

        for (int i = 0; i < sinPulsationIndices.length; ++i) {
            // sine effect
            cnm += sinC[i] * pulsations[sinReferenceIndices[i]][sinPulsationIndices[i]].sin();
        }

        return cnm;

    }

    /** Compute the time-dependent part of a spherical harmonic sine coefficient.
     * @param offsets offsets to reference dates in the gravity field
     * @param pulsations angular pulsations in the gravity field
     * @return raw coefficient Snm
     */
    public double computeSnm(final double[] offsets, final SinCos[][] pulsations) {

        // trend effect
        double snm = sBase + offsets[trendReferenceIndex] * sTrend;

        for (int i = 0; i < cosPulsationIndices.length; ++i) {
            // cosine effect
            snm += cosS[i] * pulsations[cosReferenceIndices[i]][cosPulsationIndices[i]].cos();
        }

        for (int i = 0; i < sinPulsationIndices.length; ++i) {
            // sine effect
            snm += sinS[i] * pulsations[sinReferenceIndices[i]][sinPulsationIndices[i]].sin();
        }

        return snm;

    }

}
