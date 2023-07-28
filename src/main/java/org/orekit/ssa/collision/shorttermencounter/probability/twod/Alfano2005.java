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
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.special.Erf;
import org.hipparchus.util.FastMath;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

/**
 * Compute the probability of collision using the method described in :"S. Alfano. A numerical implementation of spherical
 * objet collision probability. Journal of Astronautical Sciences, 53(1), January-March 2005."
 * <p>
 * It assumes :
 *     <ul>
 *         <li>Short encounter leading to a linear relative motion.</li>
 *         <li>Spherical collision object.</li>
 *         <li>Uncorrelated positional covariance.</li>
 *         <li>Gaussian distribution of the position uncertainties.</li>
 *         <li>Deterministic velocity i.e. no velocity uncertainties.</li>
 *     </ul>
 * <p>
 * Also, it has been implemented using a simpson integration scheme as explained in his paper.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class Alfano2005 extends AbstractShortTermEncounter2DPOCMethod {

    /** Empty constructor. */
    public Alfano2005() {
        super(ShortTermEncounter2DPOCMethodType.ALFANO_2005.name());
    }

    /** {@inheritDoc} */
    public ProbabilityOfCollision compute(final double xm, final double ym, final double sigmaX, final double sigmaY,
                                          final double radius) {
        // Computing development order M
        final int developmentOrderM = computeOrderM(xm, ym, sigmaX, sigmaY, radius);

        // Computing x step
        final double xStep = radius / (2 * developmentOrderM);

        // Computing initial x0
        final double x0 = 0.015 * xStep - radius;

        // 1 : m0
        final double m0 = 2. * getRecurrentPart(x0, xm, ym, sigmaX, sigmaY, radius);

        // 2 : mEven
        double mEvenSum = 0.;
        for (int i = 1; i < developmentOrderM; i++) {
            final double x2i = 2. * i * xStep - radius;
            mEvenSum += getRecurrentPart(x2i, xm, ym, sigmaX, sigmaY, radius);
        }

        final double otherTerm = FastMath.exp(-xm * xm / (2 * sigmaX * sigmaX)) * (
                Erf.erf((-ym + radius) / (FastMath.sqrt(2) * sigmaY)) -
                        Erf.erf((-ym - radius) / (FastMath.sqrt(2) * sigmaY)));

        final double mEven = 2. * (mEvenSum + otherTerm);

        // 3 : mOdd
        double mOddSum = 0.;
        for (int i = 1; i <= developmentOrderM; i++) {
            final double x2i_1 = (2. * i - 1.) * xStep - radius;
            mOddSum += getRecurrentPart(x2i_1, xm, ym, sigmaX, sigmaY, radius);
        }

        final double mOdd = 4. * mOddSum;

        // Output
        final double factor = xStep / (3. * sigmaX * FastMath.sqrt(8. * FastMath.PI));

        final double value = factor * (m0 + mEven + mOdd);

        return new ProbabilityOfCollision(value, getName(), isAMaximumProbabilityOfCollisionMethod());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final T xm, final T ym,
                                                                                      final T sigmaX, final T sigmaY,
                                                                                      final T radius) {
        final T zero = xm.getField().getZero();

        // Computing development order M
        final int developmentOrderM = computeOrderM(xm, ym, sigmaX, sigmaY, radius);

        // Computing x step
        final T xStep = radius.multiply(0.5 / developmentOrderM);

        // Computing initial x0
        final T x0 = xStep.multiply(0.015).subtract(radius);

        // 1 : m0
        final T m0 = getRecurrentPart(x0, xm, ym, sigmaX, sigmaY, radius).multiply(2.);

        // 2 : mEven
        T mEvenSum = zero;
        for (int i = 1; i < developmentOrderM; i++) {
            final T x2i = xStep.multiply(2. * i).subtract(radius);
            mEvenSum = mEvenSum.add(getRecurrentPart(x2i, xm, ym, sigmaX, sigmaY, radius));
        }

        final T rootTwoSigmaY = sigmaY.multiply(FastMath.sqrt(2));

        final T otherTerm = xm.multiply(xm).divide(sigmaX.multiply(sigmaX).multiply(-2.)).exp()
                              .multiply(Erf.erf(radius.subtract(ym).divide(rootTwoSigmaY))
                                           .subtract(Erf.erf(radius.add(ym).negate().divide(rootTwoSigmaY))));

        final T mEven = mEvenSum.add(otherTerm).multiply(2.);

        // 3 : mOdd
        T mOddSum = zero;
        for (int i = 1; i <= developmentOrderM; i++) {
            final T x2i_1 = xStep.multiply(2. * i - 1).subtract(radius);
            mOddSum = mOddSum.add(getRecurrentPart(x2i_1, xm, ym, sigmaX, sigmaY, radius));
        }

        final T mOdd = mOddSum.multiply(4.);

        // Output
        final T factor = xStep.divide(sigmaX.multiply(3 * FastMath.sqrt(8. * FastMath.PI)));

        final T value = factor.multiply(m0.add(mEven).add(mOdd));

        return new FieldProbabilityOfCollision<>(value, getName(), isAMaximumProbabilityOfCollisionMethod());
    }

    /**
     * Get the development order M from inputs.
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame (m)
     * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected onto
     * the collision plane
     * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     *
     * @return development order M
     */
    private int computeOrderM(final double xm, final double ym, final double sigmaX, final double sigmaY,
                              final double radius) {
        final int M = (int) (5 * radius / (FastMath.min(FastMath.min(sigmaX, sigmaY),
                                                        FastMath.min(sigmaX, FastMath.sqrt(xm * xm + ym * ym)))));
        final int LOWER_LIMIT = 10;
        final int UPPER_LIMIT = 100000;

        if (M >= UPPER_LIMIT) {
            return UPPER_LIMIT;
        } else if (M <= LOWER_LIMIT) {
            return LOWER_LIMIT;
        } else {
            return M;
        }
    }

    /**
     * Get the development order M from inputs.
     *
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
     * (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
     * (m)
     * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected onto
     * the collision plane
     * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane
     * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
     * @param <T> type of the field elements
     *
     * @return development order M
     */
    private <T extends CalculusFieldElement<T>> int computeOrderM(final T xm, final T ym, final T sigmaX, final T sigmaY,
                                                                  final T radius) {

        final double xmR = xm.getReal();
        final double ymR = ym.getReal();
        final int M = (int) (radius.getReal() * 5. / FastMath.min(FastMath.min(sigmaX.getReal(), sigmaY.getReal()),
                                                                  FastMath.min(sigmaX.getReal(),
                                                                               FastMath.sqrt(xmR * xmR + ymR * ymR))));

        final int lowerLimit = 10;
        final int upperLimit = 10000;

        if (M >= upperLimit) {
            return upperLimit;
        } else if (M <= lowerLimit) {
            return lowerLimit;
        } else {
            return M;
        }
    }

    /**
     * Get the recurrent equation from Alfano's method.
     *
     * @param x step
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame (m)
     * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected onto
     * the collision plane
     * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane
     * @param radius sum of primary and secondary collision object equivalent sphere radius (m)
     *
     * @return recurrent equation from Alfano's method
     */
    private double getRecurrentPart(final double x, final double xm, final double ym, final double sigmaX,
                                    final double sigmaY, final double radius) {
        return (Erf.erf((-ym + FastMath.sqrt(radius * radius - x * x)) / (FastMath.sqrt(2) * sigmaY)) -
                Erf.erf((-ym - FastMath.sqrt(radius * radius - x * x)) / (FastMath.sqrt(2) * sigmaY))) *
                (FastMath.exp(-(x - xm) * (x - xm) / (2. * sigmaX * sigmaX)) +
                        FastMath.exp(-(x + xm) * (x + xm) / (2. * sigmaX * sigmaX)));
    }

    /**
     * Get the recurrent equation from Alfano's method.
     *
     * @param x step
     * @param xm other collision object projected position onto the collision plane in the rotated encounter frame (m)
     * @param ym other collision object projected position onto the collision plane in the rotated encounter frame (m)
     * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected onto
     * the collision plane
     * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto the
     * collision plane
     * @param radius sum of primary and secondary collision object equivalent sphere radius (m)
     * @param <T> type of the field elements
     *
     * @return recurrent equation from Alfano's method
     */
    private <T extends CalculusFieldElement<T>> T getRecurrentPart(final T x, final T xm, final T ym, final T sigmaX,
                                                                   final T sigmaY, final T radius) {
        final T minusTwoSigmaXSquared          = sigmaX.multiply(sigmaX).multiply(-2.);
        final T radiusSquaredMinusXSquaredSQRT = radius.multiply(radius).subtract(x.multiply(x)).sqrt();
        final T rootTwoSigmaY                  = sigmaY.multiply(FastMath.sqrt(2));
        final T xMinusXm                       = x.subtract(xm);
        final T xPlusXm                        = x.add(xm);

        return Erf.erf(radiusSquaredMinusXSquaredSQRT.subtract(ym).divide(rootTwoSigmaY)).subtract(
                          Erf.erf(radiusSquaredMinusXSquaredSQRT.add(ym).negate().divide(rootTwoSigmaY)))
                  .multiply(xMinusXm.multiply(xMinusXm).divide(minusTwoSigmaXSquared).exp()
                                    .add(xPlusXm.multiply(xPlusXm).divide(minusTwoSigmaXSquared).exp()));
    }

    /** {@inheritDoc} */
    @Override
    public ShortTermEncounter2DPOCMethodType getType() {
        return ShortTermEncounter2DPOCMethodType.ALFANO_2005;
    }

}
