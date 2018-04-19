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
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.FieldHansenThirdBodyLinear;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody DSSTThirdBody}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldDSSTThirdBodyContext<T extends RealFieldElement <T>> extends FieldForceModelContext<T> {

    /** Max power for summation. */
    private static final int    MAX_POWER = 22;

    /** Truncation tolerance for big, eccentric  orbits. */
    private static final double BIG_TRUNCATION_TOLERANCE = 1.e-1;

    /** Truncation tolerance for small orbits. */
    private static final double SMALL_TRUNCATION_TOLERANCE = 1.9e-6;

    /** Maximum power for eccentricity used in short periodic computation. */
    private static final int    MAX_ECCPOWER_SP = 4;

    /** Factorial. */
    //private final T[]         fact;

    /** Max power for a/R3 in the serie expansion. */
    private int    maxAR3Pow;

    /** Max power for e in the serie expansion. */
    private int    maxEccPow;

    /** a / R3 up to power maxAR3Pow. */
    private T[] aoR3Pow;

    /** Max power for e in the serie expansion (for short periodics). */
    private int    maxEccPowShort;

    /** Max frequency of F. */
    private int    maxFreqF;

    /** Qns coefficients. */
    private T[][] Qns;

    /** Standard gravitational parameter μ for the body in m³/s². */
    private final double           gm;

    /** Distance from center of mass of the central body to the 3rd body. */
    private T R3;

    // Direction cosines of the symmetry axis
    /** α. */
    private final T alpha;
    /** β. */
    private final T beta;
    /** γ. */
    private final T gamma;

    /** B². */
    private final T BB;
    /** B³. */
    private final T BBB;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private final T X;
    /** &Chi;². */
    private final T XX;
    /** &Chi;³. */
    private final T XXX;
    /** -2 * a / A. */
    private final T m2aoA;
    /** B / A. */
    private final T BoA;
    /** 1 / (A * B). */
    private final T ooAB;
    /** -C / (2 * A * B). */
    private final T mCo2AB;
    /** B / A(1 + B). */
    private final T BoABpo;

    /** mu3 / R3. */
    private final T muoR3;

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).*/
    private final T b;

    /** h * &Chi;³. */
    private final T hXXX;
    /** k * &Chi;³. */
    private final T kXXX;

    /** V<sub>ns</sub> coefficients. */
    private final TreeMap<NSKey, Double> Vns;

    /** An array that contains the objects needed to build the Hansen coefficients. <br/>
     * The index is s */
    private final FieldHansenThirdBodyLinear<T>[] hansenObjects;

    /** Factory for the DerivativeStructure instances. */
    private final FDSFactory<T> factory;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param thirdBody body the 3rd body to consider
     * @throws OrekitException if some specific error occurs
     */
    @SuppressWarnings("unchecked")
    public FieldDSSTThirdBodyContext(final FieldAuxiliaryElements<T> auxiliaryElements, final CelestialBody thirdBody) throws OrekitException {

        super(auxiliaryElements);

        // Field for array building
        final Field<T> field = auxiliaryElements.getDate().getField();
        final T zero = field.getZero();

        this.gm = thirdBody.getGM();
        this.Vns = CoefficientsFactory.computeVns(MAX_POWER);
        this.factory = new FDSFactory<>(field, 1, 1);

        // Factorials computation
        /*final int dim = 2 * MAX_POWER;
        this.fact = MathArrays.buildArray(field, dim);
        fact[0] = zero.add(1.);
        for (int i = 1; i < dim; i++) {
            fact[i] = fact[i - 1].multiply(i);
        }*/


        //Initialise the HansenCoefficient generator
        this.hansenObjects = new FieldHansenThirdBodyLinear[MAX_POWER + 1];
        for (int s = 0; s <= MAX_POWER; s++) {
            this.hansenObjects[s] = new FieldHansenThirdBodyLinear<>(MAX_POWER, s, field);
        }

        // Distance from center of mass of the central body to the 3rd body
        final FieldVector3D<T> bodyPos = thirdBody.getPVCoordinates(auxiliaryElements.getDate(), auxiliaryElements.getFrame()).getPosition();
        R3 = bodyPos.getNorm();

        // Direction cosines
        final FieldVector3D<T> bodyDir = bodyPos.normalize();
        alpha = (T) bodyDir.dotProduct(auxiliaryElements.getVectorF());
        beta  = (T) bodyDir.dotProduct(auxiliaryElements.getVectorG());
        gamma = (T) bodyDir.dotProduct(auxiliaryElements.getVectorW());

        //&Chi;<sup>-2</sup>.
        BB = auxiliaryElements.getB().multiply(auxiliaryElements.getB());
        //&Chi;<sup>-3</sup>.
        BBB = BB.multiply(auxiliaryElements.getB());

        //b = 1 / (1 + B)
        b = auxiliaryElements.getB().add(1.).reciprocal();

        // &Chi;
        X = auxiliaryElements.getB().reciprocal();
        XX = X.multiply(X);
        XXX = X.multiply(XX);

        // -2 * a / A
        m2aoA = auxiliaryElements.getSma().multiply(-2.).divide(auxiliaryElements.getA());
        // B / A
        BoA = auxiliaryElements.getB().divide(auxiliaryElements.getA());
        // 1 / AB
        ooAB = (auxiliaryElements.getA().multiply(auxiliaryElements.getB())).reciprocal();
        // -C / 2AB
        mCo2AB = auxiliaryElements.getC().multiply(ooAB).divide(2.).negate();
        // B / A(1 + B)
        BoABpo = BoA.divide(auxiliaryElements.getB().add(1.));
        // mu3 / R3
        muoR3 = R3.divide(gm).reciprocal();
        //h * &Chi;³
        hXXX = XXX.multiply(auxiliaryElements.getH());
        //k * &Chi;³
        kXXX = XXX.multiply(auxiliaryElements.getK());

        // Truncation tolerance.
        final T aoR3 = auxiliaryElements.getSma().divide(R3);
        final double tol = ( aoR3.getReal() > .3 || (aoR3.getReal() > .15  && auxiliaryElements.getEcc().getReal() > .25) ) ? BIG_TRUNCATION_TOLERANCE : SMALL_TRUNCATION_TOLERANCE;

        // Utilities for truncation
        // Set a lower bound for eccentricity
        final T eo2  = FastMath.max(zero.add(0.0025), auxiliaryElements.getEcc().divide(2.));
        final T x2o2 = XX.divide(2.);
        final T[] eccPwr = MathArrays.buildArray(field, MAX_POWER);
        final T[] chiPwr = MathArrays.buildArray(field, MAX_POWER);
        eccPwr[0] = zero.add(1.);
        chiPwr[0] = X;
        for (int i = 1; i < MAX_POWER; i++) {
            eccPwr[i] = eccPwr[i - 1].multiply(eo2);
            chiPwr[i] = chiPwr[i - 1].multiply(x2o2);
        }

        // Auxiliary quantities.
        final T ao2rxx = aoR3.divide(XX.multiply(2.));
        T xmuarn       = ao2rxx.multiply(ao2rxx).multiply(gm).divide(X.multiply(R3));
        T term         = zero;

        // Compute max power for a/R3 and e.
        maxAR3Pow = 2;
        maxEccPow = 0;
        int n     = 2;
        int m     = 2;
        int nsmd2 = 0;

        do {
            // Upper bound for Tnm.
            /*term =  xmuarn.multiply(
                            fact[n + m].divide(fact[nsmd2].multiply(fact[nsmd2 + m]))).multiply(
                            fact[n + m + 1].divide(fact[m].multiply(fact[n + 1]))).multiply(
                            fact[n - m + 1].divide(fact[n + 1])).multiply(
                            eccPwr[m]).multiply(UpperBounds.getDnl(XX, chiPwr[m], n + 2, m));*/
            term =  xmuarn.multiply((CombinatoricsUtils.factorialDouble(n + m) / (CombinatoricsUtils.factorialDouble(nsmd2) * CombinatoricsUtils.factorialDouble(nsmd2 + m))) *
                            (CombinatoricsUtils.factorialDouble(n + m + 1) / (CombinatoricsUtils.factorialDouble(m) * CombinatoricsUtils.factorialDouble(n + 1))) *
                            (CombinatoricsUtils.factorialDouble(n - m + 1) / CombinatoricsUtils.factorialDouble(n + 1))).
                            multiply(eccPwr[m]).multiply(UpperBounds.getDnl(XX, chiPwr[m], n + 2, m));

            if (term.getReal() < tol) {
                if (m == 0) {
                    break;
                } else if (m < 2) {
                    xmuarn = xmuarn.multiply(ao2rxx);
                    m = 0;
                    n++;
                    nsmd2++;
                } else {
                    m -= 2;
                    nsmd2++;
                }
            } else {
                maxAR3Pow = n;
                maxEccPow = FastMath.max(m, maxEccPow);
                xmuarn = xmuarn.multiply(ao2rxx);
                m++;
                n++;
            }
        } while (n < MAX_POWER);

        maxEccPow = FastMath.min(maxAR3Pow, maxEccPow);

        // allocate the array aoR3Pow
        aoR3Pow = MathArrays.buildArray(field, maxAR3Pow + 1);

        aoR3Pow[0] = zero.add(1.);
        for (int i = 1; i <= maxAR3Pow; i++) {
            aoR3Pow[i] = aoR3.multiply(aoR3Pow[i - 1]);
        }

        maxFreqF = maxAR3Pow + 1;
        maxEccPowShort = MAX_ECCPOWER_SP;

        Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, FastMath.max(maxEccPow, maxEccPowShort));
    }

    /** Initialise the Hansen roots for third body problem.
     * @param B = sqrt(1 - e²).
     * @param element element of the array to compute the init values
     * @param field field of elements
     */
    public void computeHansenObjectsInitValues(final T B, final int element, final Field<T> field) {
        hansenObjects[element].computeInitValues(B, BB, BBB, field);
    }

    /** Get the Hansen Objects.
     * @return hansenObjects
     */
    public FieldHansenThirdBodyLinear<T>[] getHansenObjects() {
        return hansenObjects;
    }

    /** Get distance from center of mass of the central body to the 3rd body.
     * @return R3
     */
    public T getR3() {
        return R3;
    }

    /** Get direction cosine α for central body.
     * @return α
     */
    public T getAlpha() {
        return alpha;
    }

    /** Get direction cosine β for central body.
     * @return β
     */
    public T getBeta() {
        return beta;
    }

    /** Get direction cosine γ for central body.
     * @return γ
     */
    public T getGamma() {
        return gamma;
    }

    /** Get B².
     * @return B²
     */
    public T getBB() {
        return BB;
    }

    /** Get B³.
     * @return B³
     */
    public T getBBB() {
        return BBB;
    }

    /** Get b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).
     * @return b
     */
    public T getb() {
        return b;
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

    /** Get muoR3 = mu3 / R3.
     * @return muoR3
     */
    public T getMuoR3() {
        return muoR3;
    }

    /** Get hXXX = h * &Chi;³.
     * @return hXXX
     */
    public T getHXXX() {
        return hXXX;
    }

    /** Get kXXX = h * &Chi;³.
     * @return kXXX
     */
    public T getKXXX() {
        return kXXX;
    }

    /** Get standard gravitational parameter μ for the body in m³/s².
     *  @return gm
     */
    public double getGM() {
        return gm;
    }

    /** Get the value of max power for a/R3 in the serie expansion.
     * @return maxAR3Pow
     */
    public int getMaxAR3Pow() {
        return maxAR3Pow;
    }

    /** Get the value of max power for e in the serie expansion.
     * @return maxAR3Pow
     */
    public int getMaxEccPow() {
        return maxEccPow;
    }

    /** Get the value of a / R3 up to power maxAR3Pow.
     * @return aoR3Pow
     */
    public T[] getAoR3Pow() {
        return aoR3Pow;
    }

   /** Get the value of max frequency of F.
     * @return aoR3Pow
     */
    public int getMaxFreqF() {
        return maxFreqF;
    }

    /** Get the value of max power for e in the serie expansion (for short periodics).
     * @return aoR3Pow
     */
    public int getMaxEccPowShort() {
        return maxEccPowShort;
    }
    /** Get the value of Qns coefficients.
     * @return aoR3Pow
     */
    public T[][] getQns() {
        return Qns;
    }

    /** Get the factory for the DerivativeStructure instances.
     * @return factory
     */
    public FDSFactory<T> getFactory() {
        return factory;
    }

    /** Get the V<sub>ns</sub> coefficients.
     * @return Vns
     */
    public TreeMap<NSKey, Double> getVns() {
        return Vns;
    }

}
