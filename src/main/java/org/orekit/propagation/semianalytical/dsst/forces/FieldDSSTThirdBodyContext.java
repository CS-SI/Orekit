/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;

/**
 * This class is a container for the common "field" parameters used in {@link DSSTThirdBody}.
 * <p>
 * It performs parameters initialization at each integration step for the third
 * body attraction perturbation.
 * <p>
 * @author Bryan Cazabonne
 * @since 10.0
 */
class FieldDSSTThirdBodyContext<T extends RealFieldElement <T>> extends FieldForceModelContext<T> {

    /** Max power for summation. */
    private static final int    MAX_POWER = 22;

    /** Truncation tolerance for big, eccentric  orbits. */
    private static final double BIG_TRUNCATION_TOLERANCE = 1.e-1;

    /** Truncation tolerance for small orbits. */
    private static final double SMALL_TRUNCATION_TOLERANCE = 1.9e-6;

    /** Maximum power for eccentricity used in short periodic computation. */
    private static final int    MAX_ECCPOWER_SP = 4;

    /** Max power for a/R3 in the serie expansion. */
    private int maxAR3Pow;

    /** Max power for e in the serie expansion. */
    private int maxEccPow;

    /** a / R3 up to power maxAR3Pow. */
    private T[] aoR3Pow;

    /** Max power for e in the serie expansion (for short periodics). */
    private int maxEccPowShort;

    /** Max frequency of F. */
    private int maxFreqF;

    /** Qns coefficients. */
    private T[][] Qns;

    /** Standard gravitational parameter μ for the body in m³/s². */
    private final T gm;

    /** Distance from center of mass of the central body to the 3rd body. */
    private T R3;

    /** A = sqrt(μ * a). */
    private final T A;

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

    /** Keplerian mean motion. */
    private final T motion;

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param thirdBody body the 3rd body to consider
     * @param parameters values of the force model parameters
     */
    FieldDSSTThirdBodyContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                                     final CelestialBody thirdBody,
                                     final T[] parameters) {

        super(auxiliaryElements);

        // Field for array building
        final Field<T> field = auxiliaryElements.getDate().getField();
        final T zero = field.getZero();

        final T mu = parameters[1];
        A = FastMath.sqrt(mu.multiply(auxiliaryElements.getSma()));

        this.gm = parameters[0];

        // Keplerian mean motion
        final T absA = FastMath.abs(auxiliaryElements.getSma());
        motion = FastMath.sqrt(mu.divide(absA)).divide(absA);

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
        m2aoA = auxiliaryElements.getSma().multiply(-2.).divide(A);
        // B / A
        BoA = auxiliaryElements.getB().divide(A);
        // 1 / AB
        ooAB = (A.multiply(auxiliaryElements.getB())).reciprocal();
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

        aoR3Pow[0] = field.getOne();
        for (int i = 1; i <= maxAR3Pow; i++) {
            aoR3Pow[i] = aoR3.multiply(aoR3Pow[i - 1]);
        }

        maxFreqF = maxAR3Pow + 1;
        maxEccPowShort = MAX_ECCPOWER_SP;

        Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, FastMath.max(maxEccPow, maxEccPowShort));
    }

    /** Get A = sqrt(μ * a).
     * @return A
     */
    public T getA() {
        return A;
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

    /** Get the value of max power for a/R3 in the serie expansion.
     * @return maxAR3Pow
     */
    public int getMaxAR3Pow() {
        return maxAR3Pow;
    }

    /** Get the value of max power for e in the serie expansion.
     * @return maxEccPow
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
     * @return maxFreqF
     */
    public int getMaxFreqF() {
        return maxFreqF;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public T getMeanMotion() {
        return motion;
    }

    /** Get the value of Qns coefficients.
     * @return Qns
     */
    public T[][] getQns() {
        return Qns;
    }

}
