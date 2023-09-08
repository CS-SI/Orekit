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

/*
 * MIT License
 *
 * Copyright (c) 2019 Romain Serra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.orekit.ssa.collision.shorttermencounter.probability.twod;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.stat.StatUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

/**
 * Compute the probability of collision using the method described in : "SERRA, Romain, ARZELIER, Denis, JOLDES, Mioara, et
 * al. Fast and accurate computation of orbital collision probability for short-term encounters. Journal of Guidance,
 * Control, and Dynamics, 2016, vol. 39, no 5, p. 1009-1021.".
 * <p>
 * It is one of the recommended methods to use.
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
 * The following constants are defined when using the empty constructor :
 * <ul>
 *     <li>A default absolute accuracy of 1e-30.</li>
 *     <li>A maximum number of computed terms of 37000.</li>
 * </ul>
 * <p>
 * This implementation has been translated from python from the provided source code of Romain SERRA on the
 * <a href="https://github.com/Serrof/SST/blob/master/collision/short_term_poc.py">following github account</a>
 *
 * @author Vincent Cucchietti
 * @author Romain Serra
 * @since 12.0
 */
public class Laas2015 extends AbstractShortTermEncounter2DPOCMethod {

    /** Default scaling threshold to use when sum becomes large. */
    public static final double DEFAULT_SCALING_THRESHOLD = 1e10;

    /**
     * Defines the absolute accuracy of this method. For example, given an absolute accuracy of 1e-10, the probability of
     * collision will be exact until its 1e-10 digit.
     */
    private final double absoluteAccuracy;

    /** Defines the max number of terms that the method will use, thus reducing the computation time in particular cases. */
    private final int maxNumberOfTerms;

    /**
     * Default constructor.
     * <p>
     * It uses a default absolute accuracy of 1e-30 and a maximum number of terms of 37000 which is the max number of terms
     * computed based on Romain SERRA's observation (p.56 of "Romain Serra. Opérations de proximité en orbite : * évaluation
     * du risque de collision et calcul de manoeuvres optimales pour l’évitement et le rendez-vous. Automatique / *
     * Robotique. INSA de Toulouse, 2015. Français. NNT : 2015ISAT0035. tel-01261497") about Alfano test case 5 where he
     * explains that 37000 terms were enough to meet the required precision of 5 significant digits.
     */
    public Laas2015() {
        this(1.E-30, 37000);
    }

    /** Simple constructor.
     * @param absoluteAccuracy absolute accuracy of the result
     * @param maxNumberOfTerms max number of terms to compute
     */
    public Laas2015(final double absoluteAccuracy, final int maxNumberOfTerms) {
        super(ShortTermEncounter2DPOCMethodType.LAAS_2015.name());
        this.absoluteAccuracy = absoluteAccuracy;
        this.maxNumberOfTerms = maxNumberOfTerms;
    }

    /** {@inheritDoc} */
    public final ProbabilityOfCollision compute(final double xm, final double ym,
                                                final double sigmaX,
                                                final double sigmaY,
                                                final double radius) {

        // CHECKSTYLE: stop Indentation check
        // Initializing recurrent terms
        final double xmSquared     = xm * xm;
        final double ymSquared     = ym * ym;
        final double sigmaXSq      = sigmaX * sigmaX;
        final double sigmaYSq      = sigmaY * sigmaY;
        final double radiusSquared = radius * radius;

        final double p        = 1. / (2. * (sigmaX * sigmaX));
        final double phiY     = 1. - (sigmaX / sigmaY * sigmaX / sigmaY);
        final double omegaX   = (xm / (2. * sigmaXSq)) * (xm / (2. * sigmaXSq));
        final double omegaY   = (ym / (2. * sigmaYSq)) * (ym / (2. * sigmaYSq));
        final double bigOmega = phiY * 0.5 + (omegaX + omegaY) / p;

        final double alpha0 = 0.5 * FastMath.exp(-(xmSquared / sigmaXSq + ymSquared / sigmaYSq) * 0.5) / sigmaX / sigmaY;

        // Lower boundary
        final double l0 = alpha0 * (1. - FastMath.exp(-p * radiusSquared)) / p;

        // Upper boundary
        final double u0Temp = alpha0 * (FastMath.exp(p * bigOmega * radiusSquared) -
                FastMath.exp(-p * radiusSquared)) / (p * (1. + bigOmega));
        final double u0 = u0Temp > 1 ? 1 : u0Temp;

        // If the boundaries are close enough to the actual value according to defined relative accuracy
        if (u0 - l0 <= absoluteAccuracy) {
            return new ProbabilityOfCollision(StatUtils.mean(u0, l0), u0, l0, getName(),
                                              isAMaximumProbabilityOfCollisionMethod());
        }
        // Otherwise
        else {
            final int n1 = (int) (2. * FastMath.ceil(FastMath.E * p * radiusSquared * (1. + bigOmega)));
            final double n2_inter =
                    alpha0 * FastMath.exp(p * radiusSquared * bigOmega) /
                            (absoluteAccuracy * p * FastMath.sqrt(MathUtils.TWO_PI * n1) *
                                    (1. + bigOmega));
            final int n2 = (int) FastMath.ceil(FastMath.log(2, n2_inter));

            // Number of terms to get the relative accuracy desired
            int nMax = FastMath.max(n1, n2) - 1;
            nMax = FastMath.min(nMax, maxNumberOfTerms);

            // Initializing terms used in the equations
            final double pSquared                  = p * p;
            final double pCubed                    = pSquared * p;
            final double pTimesRadiusSquared       = p * radiusSquared;
            final double radiusFourth              = radiusSquared * radiusSquared;
            final double radiusSixth               = radiusFourth * radiusSquared;
            final double phiYSquared               = phiY * phiY;
            final double pPhiY                     = p * phiY;
            final double omegaSum                  = omegaX + omegaY;
            final double pSqTimesHalfPhiYSqPlusOne = pSquared * MathArrays.linearCombination(0.5, phiYSquared, 1., 1.);

            final double recurrentTerm0 = MathArrays.linearCombination(0.5, phiY, 1., 1.);
            final double recurrentTerm1 = MathArrays.linearCombination(p, recurrentTerm0, 1., omegaSum);
            final double recurrentTerm2 = MathArrays.linearCombination(1., pSqTimesHalfPhiYSqPlusOne, 2., pPhiY * omegaY);
            final double recurrentTerm3 = recurrentTerm1 * recurrentTerm1;

            final double auxiliaryTerm0 = radiusSixth * pCubed * phiYSquared * omegaX;
            final double auxiliaryTerm1 = radiusFourth * pSquared * phiY;
            final double auxiliaryTerm2 = 2. * omegaX * recurrentTerm0;

            final double auxiliaryTerm3 = phiY * MathArrays.linearCombination(2., omegaX, 1.5, p) + omegaSum;
            final double auxiliaryTerm4 = pPhiY * recurrentTerm0 * 2.;
            final double auxiliaryTerm5 = p * (2. * phiY + 1.);

            double kPlus2 = 2.;
            double kPlus3 = 3.;
            double kPlus4 = 4.;
            double kPlus5 = 5.;
            double halfY  = 2.5;

            // Initialize recurrence
            double c0 = alpha0 * radiusSquared;
            double c1 = c0 * radiusSquared * 0.5 * recurrentTerm1;
            double c2 = c0 * (radiusFourth / 12.) * (recurrentTerm3 + recurrentTerm2);
            double c3 = c0 * (radiusSixth / 144.) * (recurrentTerm1 * (recurrentTerm3 + 3. * recurrentTerm2) +
                    2. * (pCubed * (1. + phiYSquared * phiY * 0.5) + 3. * pSquared * phiYSquared * omegaY));
            final double[] initialCoefficients = new double[] { c0, c1, c2, c3 };

            double sum              = 0.;
            double rescalingCounter = 0.;
            for (int i = 0; i < FastMath.min(nMax, 4); i++) {
                sum += initialCoefficients[i];

                // Rescale quantities if necessary
                if (sum > DEFAULT_SCALING_THRESHOLD) {
                    rescalingCounter += FastMath.log10(DEFAULT_SCALING_THRESHOLD);
                    c0 /= DEFAULT_SCALING_THRESHOLD;
                    c1 /= DEFAULT_SCALING_THRESHOLD;
                    c2 /= DEFAULT_SCALING_THRESHOLD;
                    c3 /= DEFAULT_SCALING_THRESHOLD;
                    sum /= DEFAULT_SCALING_THRESHOLD;
                }

            }

            // Iterate
            double temp;
            for (int k = 0; k < nMax - 4; k++) {

                // Rescale quantities if necessary
                if (sum > DEFAULT_SCALING_THRESHOLD) {
                    rescalingCounter += FastMath.log10(DEFAULT_SCALING_THRESHOLD);
                    c0 /= DEFAULT_SCALING_THRESHOLD;
                    c1 /= DEFAULT_SCALING_THRESHOLD;
                    c2 /= DEFAULT_SCALING_THRESHOLD;
                    c3 /= DEFAULT_SCALING_THRESHOLD;
                    sum /= DEFAULT_SCALING_THRESHOLD;
                }

                // Recurrence relation
                final double denominator = kPlus4 * kPlus3;

                temp = c3 * MathArrays.linearCombination(1, recurrentTerm1, kPlus3, auxiliaryTerm5);
                temp -= c2 * pTimesRadiusSquared * MathArrays.linearCombination(halfY, auxiliaryTerm4, 1, auxiliaryTerm3) /
                        kPlus4;
                temp += c1 * auxiliaryTerm1 * MathArrays.linearCombination(halfY, pPhiY, 1., auxiliaryTerm2) / denominator;
                temp -= c0 * auxiliaryTerm0 / (denominator * kPlus2);
                temp *= radiusSquared / (kPlus4 * kPlus5);

                c0 = c1;
                c1 = c2;
                c2 = c3;
                c3 = temp;

                // Update intermediate variables
                kPlus2 = kPlus3;
                kPlus3 = kPlus4;
                kPlus4 = kPlus5;
                kPlus5 = kPlus5 + 1.;
                halfY += 1.;

                // Update sum
                sum += c3;

            }
            // CHECKSTYLE: resume Indentation check
            final double value = sum *
                    FastMath.exp(MathArrays.linearCombination(FastMath.log(10.), rescalingCounter, -p, radiusSquared));

            return new ProbabilityOfCollision(value, l0, u0, getName(), isAMaximumProbabilityOfCollisionMethod());
        }
    }

    /** {@inheritDoc} */
    public final <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final T xm, final T ym,
                                                                                            final T sigmaX,
                                                                                            final T sigmaY,
                                                                                            final T radius) {

        // CHECKSTYLE: stop Indentation check
        // Initializing recurrent terms
        final Field<T> field = xm.getField();
        final T        zero  = field.getZero();
        final T        one   = field.getOne();

        final T xmSquared     = xm.multiply(xm);
        final T ymSquared     = ym.multiply(ym);
        final T sigmaXSquared = sigmaX.multiply(sigmaX);
        final T sigmaYSquared = sigmaY.multiply(sigmaY);
        final T twoSigmaXY    = sigmaX.multiply(sigmaY).multiply(2);
        final T radiusSquared = radius.multiply(radius);
        final T radiusFourth  = radiusSquared.multiply(radiusSquared);
        final T radiusSixth   = radiusFourth.multiply(radiusSquared);

        final T p                   = sigmaX.multiply(sigmaX).reciprocal().multiply(0.5);
        final T pTimesRadiusSquared = p.multiply(radiusSquared);
        final T phiY                = sigmaXSquared.divide(sigmaYSquared).negate().add(1.);
        final T omegaX              = xm.divide(sigmaXSquared.multiply(2.)).pow(2.);
        final T omegaY              = ym.divide(sigmaYSquared.multiply(2.)).pow(2.);
        final T omegaSum            = omegaX.add(omegaY);
        final T bigOmega            = phiY.multiply(0.5).add(omegaX.add(omegaY).divide(p));

        final T minusP                    = p.negate();
        final T pSquared                  = p.multiply(p);
        final T pCubed                    = p.multiply(pSquared);
        final T pPhiY                     = p.multiply(phiY);
        final T phiYSquared               = phiY.multiply(phiY);
        final T pRadiusSquaredBigOmega    = p.multiply(radiusSquared).multiply(bigOmega);
        final T bigOmegaPlusOne           = bigOmega.add(1.);
        final T pSqTimesHalfPhiYSqPlusOne = pSquared.multiply(phiYSquared.multiply(0.5).add(1.));

        final T alpha0 = xmSquared.divide(sigmaXSquared).add(ymSquared.divide(sigmaYSquared)).multiply(-0.5).exp()
                                  .divide(twoSigmaXY);

        // Lower boundary
        final T l0 = alpha0.multiply(radiusSquared.multiply(minusP).exp().negate().add(1.)).divide(p);

        // Upper boundary
        final T u0Temp = alpha0.multiply(pRadiusSquaredBigOmega.exp().subtract(radiusSquared.multiply(minusP).exp()))
                               .divide(p.multiply(bigOmegaPlusOne));
        final T u0 = u0Temp.getReal() > 1 ? one : u0Temp;

        // If the boundaries are close enough to the actual value according to defined relative accuracy
        if (u0.getReal() - l0.getReal() <= absoluteAccuracy) {
            return new FieldProbabilityOfCollision<>(u0.add(l0).multiply(0.5), u0, l0, getName(),
                                                     isAMaximumProbabilityOfCollisionMethod());
        }
        // Otherwise
        else {
            final int n1 = (int) (2. *
                    FastMath.ceil(FastMath.E * p.multiply(radiusSquared).multiply(bigOmegaPlusOne).getReal()));
            final double n2_inter =
                    alpha0.getReal() * FastMath.exp(pRadiusSquaredBigOmega.getReal()) /
                            (absoluteAccuracy * p.getReal() * FastMath.sqrt(MathUtils.TWO_PI * n1) *
                                    (1. + bigOmega.getReal()));
            final int n2 = (int) FastMath.ceil(FastMath.log(2., n2_inter));

            // Number of terms to get the relative accuracy desired
            int nMax = FastMath.max(n1, n2) - 1;
            nMax = FastMath.min(nMax, maxNumberOfTerms);

            // Recurrent term in the equations
            final T recurrentTerm0 = phiY.multiply(0.5).add(1);
            final T recurrentTerm1 = p.multiply(recurrentTerm0).add(omegaSum);
            final T recurrentTerm2 = pSqTimesHalfPhiYSqPlusOne.add(pPhiY.multiply(omegaY).multiply(2.));
            final T recurrentTerm3 = recurrentTerm1.multiply(recurrentTerm1);

            final T auxiliaryTerm0 = radiusSixth.multiply(pCubed).multiply(phiYSquared).multiply(omegaX);
            final T auxiliaryTerm1 = radiusFourth.multiply(pSquared).multiply(phiY);
            final T auxiliaryTerm2 = omegaX.multiply(recurrentTerm0).multiply(2.);
            final T auxiliaryTerm3 = phiY.multiply(omegaX.multiply(2.).add(p.multiply(1.5))).add(omegaSum);
            final T auxiliaryTerm4 = pPhiY.multiply(recurrentTerm0).multiply(2.);
            final T auxiliaryTerm5 = p.multiply(phiY.multiply(2.).add(1.));

            T kPlus2 = one.multiply(2.);
            T kPlus3 = one.multiply(3.);
            T kPlus4 = one.multiply(4.);
            T kPlus5 = one.multiply(5.);
            T halfY  = one.multiply(2.5);

            // Initialize recurrence
            T c0 = alpha0.multiply(radiusSquared);
            T c1 = c0.multiply(radiusSquared).multiply(0.5).multiply(recurrentTerm1);
            T c2 = c0.multiply(radiusFourth.divide(12.)).multiply(recurrentTerm3.add(recurrentTerm2));
            T c3 = c0.multiply(radiusSixth.divide(144.)).multiply(
                    recurrentTerm1.multiply(recurrentTerm2.multiply(3.).add(recurrentTerm3))
                                  .add(pCubed.multiply(2.).multiply(phiYSquared.multiply(phiY).multiply(0.5).add(1.)))
                                  .add(pSquared.multiply(phiYSquared).multiply(omegaY).multiply(6.)));
            final FieldVector<T> initialCoefficients = MatrixUtils.createFieldVector(field, 4);
            initialCoefficients.setEntry(0, c0);
            initialCoefficients.setEntry(1, c1);
            initialCoefficients.setEntry(2, c2);
            initialCoefficients.setEntry(3, c3);

            T sum              = zero;
            T rescalingCounter = zero;
            for (int i = 0; i < FastMath.min(nMax, 4); i++) {
                sum = sum.add(initialCoefficients.getEntry(i));

                // Rescale quantities if necessary
                if (sum.getReal() > DEFAULT_SCALING_THRESHOLD) {
                    rescalingCounter = rescalingCounter.add(FastMath.log10(DEFAULT_SCALING_THRESHOLD));
                    c0               = c0.divide(DEFAULT_SCALING_THRESHOLD);
                    c1               = c1.divide(DEFAULT_SCALING_THRESHOLD);
                    c2               = c2.divide(DEFAULT_SCALING_THRESHOLD);
                    c3               = c3.divide(DEFAULT_SCALING_THRESHOLD);
                    sum              = sum.divide(DEFAULT_SCALING_THRESHOLD);
                }

            }

            // Iterate
            T temp;
            for (int k = 0; k < nMax - 4; k++) {

                // Rescale quantities if necessary
                if (sum.getReal() > DEFAULT_SCALING_THRESHOLD) {
                    rescalingCounter = rescalingCounter.add(FastMath.log10(DEFAULT_SCALING_THRESHOLD));
                    c0               = c0.divide(DEFAULT_SCALING_THRESHOLD);
                    c1               = c1.divide(DEFAULT_SCALING_THRESHOLD);
                    c2               = c2.divide(DEFAULT_SCALING_THRESHOLD);
                    c3               = c3.divide(DEFAULT_SCALING_THRESHOLD);
                    sum              = sum.divide(DEFAULT_SCALING_THRESHOLD);
                }

                // Recurrence relation
                final T denominator = kPlus4.multiply(kPlus3);

                temp = c3.multiply(recurrentTerm1.add(auxiliaryTerm5.multiply(kPlus3)));
                temp = temp.subtract(
                        c2.multiply(pTimesRadiusSquared).multiply(auxiliaryTerm4.multiply(halfY).add(auxiliaryTerm3))
                          .divide(kPlus4));
                temp = temp.add(
                        c1.multiply(auxiliaryTerm1).multiply(pPhiY.multiply(halfY).add(auxiliaryTerm2)).divide(denominator));
                temp = temp.subtract(c0.multiply(auxiliaryTerm0).divide(denominator.multiply(kPlus2)));
                temp = temp.multiply(radiusSquared.divide(kPlus4.multiply(kPlus5)));

                c0 = c1;
                c1 = c2;
                c2 = c3;
                c3 = temp;

                // Update intermediate variables
                kPlus2 = kPlus3;
                kPlus3 = kPlus4;
                kPlus4 = kPlus5;
                kPlus5 = kPlus5.add(1.);
                halfY  = halfY.add(1.);

                // Update sum
                sum = sum.add(c3);

            }
            final T value =
                    sum.multiply((rescalingCounter.multiply(FastMath.log(10.)).subtract(pTimesRadiusSquared)).exp());

            return new FieldProbabilityOfCollision<>(value, l0, u0, getName(), isAMaximumProbabilityOfCollisionMethod());
            // CHECKSTYLE: resume Indentation check
        }
    }

    /** {@inheritDoc} */
    @Override
    public ShortTermEncounter2DPOCMethodType getType() {
        return ShortTermEncounter2DPOCMethodType.LAAS_2015;
    }

}
