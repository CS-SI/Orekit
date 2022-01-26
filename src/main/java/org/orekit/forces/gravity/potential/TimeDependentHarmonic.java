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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.SinCos;

/** Time-dependent part of a single component of spherical harmonics.
 * @author Luc Maisonobe
 * @since 11.1
 */
class TimeDependentHarmonic {

    /** Index of the trend reference in the gravity field. */
    private final int trendReferenceIndex;

    /** Secular trend of the cosine coefficient. */
    private final double cTrend;

    /** Secular trend of the sine coefficient. */
    private final double sTrend;

    /** Indices of the reference dates in the gravity field. */
    private int[] referenceIndices;

    /** Indices of the harmonic pulsations in the gravity field. */
    private int[] pulsationIndices;

    /** Cosine component of the cosine coefficient. */
    private double[] cosC;

    /** Sine component of the cosine coefficient. */
    private double[] sinC;

    /** Cosine component of the sine coefficient. */
    private double[] cosS;

    /** Sine component of the sine coefficient. */
    private double[] sinS;

    /** Build a part with only trend.
     * @param trendReferenceIndex index of the trend reference in the gravity field
     * @param cTrend secular trend of the cosine coefficient (s⁻¹)
     * @param sTrend secular trend of the sine coefficient (s⁻¹)
     */
    TimeDependentHarmonic(final int trendReferenceIndex, final double cTrend, final double sTrend) {

        // linear part
        this.trendReferenceIndex = trendReferenceIndex;
        this.cTrend              = cTrend;
        this.sTrend              = sTrend;

        // empty harmonic part
        this.referenceIndices    = new int[0];
        this.pulsationIndices    = new int[0];
        this.cosC                = new double[0];
        this.sinC                = new double[0];
        this.cosS                = new double[0];
        this.sinS                = new double[0];

    }

    /** Add an harmonic component.
     * @param referenceIndex index of the reference date in the gravity field
     * @param pulsationIndex index of the harmonic pulsation in the gravity field
     * @param cosineC cosine component of the cosine coefficient
     * @param sineC sine component of the cosine coefficient
     * @param cosineS cosine component of the sine coefficient
     * @param sineS sine component of the sine coefficient
     */
    public void addHarmonic(final int referenceIndex, final int pulsationIndex,
                            final double cosineC, final double sineC,
                            final double cosineS, final double sineS) {
        this.referenceIndices = addInt(referenceIndex, this.referenceIndices);
        this.pulsationIndices = addInt(pulsationIndex, this.pulsationIndices);
        this.cosC             = addDouble(cosineC,     this.cosC);
        this.sinC             = addDouble(sineC,       this.sinC);
        this.cosS             = addDouble(cosineS,     this.cosS);
        this.sinS             = addDouble(sineS,       this.sinS);
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
        final int[] newArray = new int[array.length];
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
        final double[] newArray = new double[array.length];
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
        double cnm = offsets[trendReferenceIndex] * cTrend;

        for (int i = 0; i < pulsationIndices.length; ++i) {
            // harmonic effect
            final SinCos pulsation = pulsations[referenceIndices[i]][pulsationIndices[i]];
            cnm += cosC[i] * pulsation.cos() + sinC[i] * pulsation.sin();
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
        double snm = offsets[trendReferenceIndex] * sTrend;

        for (int i = 0; i < pulsationIndices.length; ++i) {
            // harmonic effect
            final SinCos pulsation = pulsations[referenceIndices[i]][pulsationIndices[i]];
            snm += cosS[i] * pulsation.cos() + sinS[i] * pulsation.sin();
        }

        return snm;

    }

}
