/* Copyright 2002-2018 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.TreeMap;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.FieldHansenZonalLinear;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal DSSTZonal}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldDSSTZonalContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int I = 1;

    /** Truncation tolerance. */
    private static final double TRUNCATION_TOLERANCE = 1e-4;

    /** An array that contains the objects needed to build the Hansen coefficients. <br/>
     * The index is s*/
    private FieldHansenZonalLinear<T>[] hansenObjects;

    /** Highest power of the eccentricity to be used in mean elements computations. */
    private int maxEccPowMeanElements;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal degree to consider for harmonics potential in short periodic computations. */
    private final int maxOrder;

    /** Provider for spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Coefficient used to define the mean disturbing function V<sub>ns</sub> coefficient. */
    private final TreeMap<NSKey, Double> Vns;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private T X;
    /** &Chi;². */
    private T XX;
    /** &Chi;³. */
    private T XXX;
    /** 1 / (A * B) .*/
    private T ooAB;
    /** B / A .*/
    private T BoA;
    /** B / A(1 + B) .*/
    private T BoABpo;
    /** -C / (2 * A * B) .*/
    private T mCo2AB;
    /** -2 * a / A .*/
    private T m2aoA;
    /** μ / a .*/
    private T muoa;
    /** R / a .*/
    private T roa;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param meanOnly create only the objects required for the mean contribution
     * @param provider provider for spherical harmonics
     * @param maxEccPowShortPeriodics highest power of the eccentricity to be used in short periodic computations.
     * @param parameters values of the force model parameters
     * @throws OrekitException if some specific error occurs
     */
    @SuppressWarnings("unchecked")
    public FieldDSSTZonalContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                                 final boolean meanOnly,
                                 final UnnormalizedSphericalHarmonicsProvider provider,
                                 final int maxEccPowShortPeriodics,
                                 final T[] parameters)
        throws OrekitException {

        super(auxiliaryElements);

        final Field<T> field = auxiliaryElements.getDate().getField();

        this.provider              = provider;
        this.maxDegree             = provider.getMaxDegree();
        this.maxOrder              = provider.getMaxOrder();
        this.maxEccPowMeanElements = (maxDegree == 2) ? 0 : Integer.MIN_VALUE;
        // Vns coefficients
        this.Vns = CoefficientsFactory.computeVns(provider.getMaxDegree() + 1);

        final T mu = parameters[0];

        // &Chi; = 1 / B
        X   = auxiliaryElements.getB().reciprocal();
        XX  = X.multiply(X);
        XXX = X.multiply(XX);

        // 1 / AB
        ooAB   = (auxiliaryElements.getA().multiply(auxiliaryElements.getB())).reciprocal();
        // B / A
        BoA    = auxiliaryElements.getB().divide(auxiliaryElements.getA());
        // -C / 2AB
        mCo2AB = auxiliaryElements.getC().multiply(ooAB).divide(2.).negate();
        // B / A(1 + B)
        BoABpo = BoA.divide(auxiliaryElements.getB().add(1.));
        // -2 * a / A
        m2aoA  = auxiliaryElements.getSma().divide(auxiliaryElements.getA()).multiply(-2.);
        // μ / a
        muoa   = mu.divide(auxiliaryElements.getSma());
        // R / a
        roa    = auxiliaryElements.getSma().divide(provider.getAe()).reciprocal();

        computeMeanElementsTruncations(auxiliaryElements, parameters);

        final int maxEccPow;
        if (meanOnly) {
            maxEccPow = maxEccPowMeanElements;
            this.hansenObjects = new FieldHansenZonalLinear[maxEccPow + 1];
            for (int s = 0; s <= maxEccPow; s++) {
                this.hansenObjects[s] = new FieldHansenZonalLinear<>(maxDegree, s, field);
            }
        } else {
            maxEccPow = FastMath.max(maxEccPowMeanElements, maxEccPowShortPeriodics);
            this.hansenObjects = new FieldHansenZonalLinear[maxEccPow + 1];
            for (int s = 0; s <= maxEccPow; s++) {
                this.hansenObjects[s] = new FieldHansenZonalLinear<>(maxDegree, s, field);
            }
        }

    }

    /** Compute init values for hansen objects.
     * @param element element of the array to compute the init values
     */
    public void computeHansenObjectsInitValues(final int element) {
        hansenObjects[element].computeInitValues(X);
    }

    /** Get the Hansen Objects.
     * @return hansenObjects
     */
    public FieldHansenZonalLinear<T>[] getHansenObjects() {
        return hansenObjects;
    }


    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return &Chi;
     */
    public T getX() {
        return X;
    }

    /** Get &Chi;².
     * @return &Chi;².
     */
    public T getXX() {
        return XX;
    }

    /** Get &Chi;³.
     * @return &Chi;³
     */
    public T getXXX() {
        return XXX;
    }

    /** Get m2aoA = -2 * a / A.
     * @return m2aoA
     */
    public T getM2aoA() {
        return m2aoA;
    }

    /** Get B / A.
     * @return BoA
     */
    public T getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public T getOoAB() {
        return ooAB;
    }

    /** Get mCo2AB = -C / 2AB.
     * @return mCo2AB
     */
    public T getMCo2AB() {
        return mCo2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public T getBoABpo() {
        return BoABpo;
    }

    /** Get μ / a .
     * @return muoa
     */
    public T getMuoa() {
        return muoa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public T getRoa() {
        return roa;
    }

    /** Get highest power of the eccentricity to be used in mean elements computations.
     * @return maxEccPowMeanElements
     */
    public int getMaxEccPowMeanElements() {
        return maxEccPowMeanElements;
    }

    /** Get the maximal degree to consider for harmonics potential.
     * @return maxDegree
     */
    public int getMaxDegree() {
        return maxDegree;
    }

    /** Get the V<sub>ns</sub> coefficients.
     * @return Vns
     */
    public TreeMap<NSKey, Double> getVns() {
        return Vns;
    }

    /** Compute indices truncations for mean elements computations.
     * @param auxiliaryElements auxiliary elements
     * @param parameters values of the force model parameters
     * @throws OrekitException if an error occurs
     */
    private void computeMeanElementsTruncations(final FieldAuxiliaryElements<T> auxiliaryElements, final T[] parameters)
        throws OrekitException {

        final Field<T> field = auxiliaryElements.getDate().getField();
        final T zero = field.getZero();
        //Compute the max eccentricity power for the mean element rate expansion
        if (maxDegree == 2) {
            maxEccPowMeanElements = 0;
        } else {
            // Initializes specific parameters.
            final UnnormalizedSphericalHarmonics harmonics = provider.onDate(auxiliaryElements.getDate().toAbsoluteDate());

            // Utilities for truncation
            final T ax2or = auxiliaryElements.getSma().multiply(2.).divide(provider.getAe());
            T xmuran = parameters[0].divide(auxiliaryElements.getSma());
            // Set a lower bound for eccentricity
            final T eo2  = FastMath.max(zero.add(0.0025), auxiliaryElements.getEcc().divide(2.));
            final T x2o2 = XX.divide(2.);
            final T[] eccPwr = MathArrays.buildArray(field, maxDegree + 1);
            final T[] chiPwr = MathArrays.buildArray(field, maxDegree + 1);
            final T[] hafPwr = MathArrays.buildArray(field, maxDegree + 1);
            eccPwr[0] = zero.add(1.);
            chiPwr[0] = X;
            hafPwr[0] = zero.add(1.);
            for (int i = 1; i <= maxDegree; i++) {
                eccPwr[i] = eccPwr[i - 1].multiply(eo2);
                chiPwr[i] = chiPwr[i - 1].multiply(x2o2);
                hafPwr[i] = hafPwr[i - 1].multiply(0.5);
                xmuran  = xmuran.divide(ax2or);
            }

            // Set highest power of e and degree of current spherical harmonic.
            maxEccPowMeanElements = 0;
            int n = maxDegree;
            // Loop over n
            do {
                // Set order of current spherical harmonic.
                int m = 0;
                // Loop over m
                do {
                    // Compute magnitude of current spherical harmonic coefficient.
                    final T cnm = zero.add(harmonics.getUnnormalizedCnm(n, m));
                    final T snm = zero.add(harmonics.getUnnormalizedSnm(n, m));
                    final T csnm = FastMath.hypot(cnm, snm);
                    if (csnm.getReal() == 0.) break;
                    // Set magnitude of last spherical harmonic term.
                    T lastTerm = zero;
                    // Set current power of e and related indices.
                    int nsld2 = (n - maxEccPowMeanElements - 1) / 2;
                    int l = n - 2 * nsld2;
                    // Loop over l
                    T term = zero;
                    do {
                        // Compute magnitude of current spherical harmonic term.
                        if (m < l) {
                            term = csnm.multiply(xmuran).
                                   multiply((CombinatoricsUtils.factorialDouble(n - l) / (CombinatoricsUtils.factorialDouble(n - m))) *
                                   (CombinatoricsUtils.factorialDouble(n + l) / (CombinatoricsUtils.factorialDouble(nsld2) * CombinatoricsUtils.factorialDouble(nsld2 + l)))).
                                   multiply(eccPwr[l]).multiply(UpperBounds.getDnl(XX, chiPwr[l], n, l)).
                                   multiply(UpperBounds.getRnml(auxiliaryElements.getGamma(), n, l, m, 1, I).add(UpperBounds.getRnml(auxiliaryElements.getGamma(), n, l, m, -1, I)));
                        } else {
                            term = csnm.multiply(xmuran).
                                   multiply(CombinatoricsUtils.factorialDouble(n + m) / (CombinatoricsUtils.factorialDouble(nsld2) * CombinatoricsUtils.factorialDouble(nsld2 + l))).
                                   multiply(eccPwr[l]).multiply(hafPwr[m - l]).multiply(UpperBounds.getDnl(XX, chiPwr[l], n, l)).
                                   multiply(UpperBounds.getRnml(auxiliaryElements.getGamma(), n, m, l, 1, I).add(UpperBounds.getRnml(auxiliaryElements.getGamma(), n, m, l, -1, I)));
                        }
                        // Is the current spherical harmonic term bigger than the truncation tolerance ?
                        if (term.getReal() >= TRUNCATION_TOLERANCE) {
                            maxEccPowMeanElements = l;
                        } else {
                            // Is the current term smaller than the last term ?
                            if (term.getReal() < lastTerm.getReal()) {
                                break;
                            }
                        }
                        // Proceed to next power of e.
                        lastTerm = term;
                        l += 2;
                        nsld2--;
                    } while (l < n);
                    // Is the current spherical harmonic term bigger than the truncation tolerance ?
                    if (term.getReal() >= TRUNCATION_TOLERANCE) {
                        maxEccPowMeanElements = FastMath.min(maxDegree - 2, maxEccPowMeanElements);
                        return;
                    }
                    // Proceed to next order.
                    m++;
                } while (m <= FastMath.min(n, maxOrder));
                // Proceed to next degree.
                xmuran = xmuran.multiply(ax2or);
                n--;
            } while (n > maxEccPowMeanElements + 2);

            maxEccPowMeanElements = FastMath.min(maxDegree - 2, maxEccPowMeanElements);
        }
    }

}
