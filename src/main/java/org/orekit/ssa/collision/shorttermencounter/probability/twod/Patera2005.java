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
import org.hipparchus.Field;
import org.hipparchus.analysis.CalculusFieldUnivariateFunction;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.integration.FieldUnivariateIntegrator;
import org.hipparchus.analysis.integration.TrapezoidIntegrator;
import org.hipparchus.analysis.integration.UnivariateIntegrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.ssa.metrics.FieldProbabilityOfCollision;
import org.orekit.ssa.metrics.ProbabilityOfCollision;

/**
 * Compute the probability of collision using the method described in :"PATERA, Russell P. Calculating collision probability
 * for arbitrary space vehicle shapes via numerical quadrature. Journal of guidance, control, and dynamics, 2005, vol. 28, no
 * 6, p. 1326-1328.".
 * <p>
 * It is one of the recommended methods to use.
 * <p>
 * It assumes :
 * <ul>
 *     <li>Short encounter leading to a linear relative motion.</li>
 *     <li>Spherical collision object (in this implementation only. The method could be used on non spherical object).</li>
 *     <li>Uncorrelated positional covariance.</li>
 *     <li>Gaussian distribution of the position uncertainties.</li>
 *     <li>Deterministic velocity i.e. no velocity uncertainties.</li>
 * </ul>
 * It has been rewritten to use Orekit specific inputs.
 *
 * @author Vincent Cucchietti
 * @since 12.0
 */
public class Patera2005 extends AbstractShortTermEncounter1DNumerical2DPOCMethod {

    /** Default threshold defining if miss-distance and combined radius are considered equal (+- 10 cm). */
    private static final double DEFAULT_EQUALITY_THRESHOLD = 1e-1;

    /**
     * Default constructor built with the following trapezoid integrator:
     * <ul>
     *     <li>Minimal iteration count of 5</li>
     *     <li>Maximum iteration count of 50000</li>
     * </ul>.
     */
    public Patera2005() {
        this(new TrapezoidIntegrator(5, TrapezoidIntegrator.TRAPEZOID_MAX_ITERATIONS_COUNT), 50000);
    }

    /**
     * Customizable constructor.
     *
     * @param integrator integrator
     * @param maxNbOfEval max number of evaluation
     */
    public Patera2005(final UnivariateIntegrator integrator, final int maxNbOfEval) {
        super("PATERA_2005", integrator, maxNbOfEval);
    }

    /** {@inheritDoc} */
    @Override
    public ProbabilityOfCollision compute(final double xm, final double ym,
                                          final double sigmaX, final double sigmaY,
                                          final double radius,
                                          final UnivariateIntegrator integrator,
                                          final int customMaxNbOfEval) {

        // Depending on miss distance and the combined radius, three distinct cases exist
        final double value;
        final double missDistance = FastMath.sqrt(xm * xm + ym * ym);

        // reference outside the hardbody area, first part of eq(11) is equal to 0
        if (missDistance > radius + DEFAULT_EQUALITY_THRESHOLD) {
            final CommonPateraFunction function = new CommonPateraFunction(xm, ym, sigmaX, sigmaY, radius);
            value = -integrator.integrate(customMaxNbOfEval, function, 0, MathUtils.TWO_PI) / MathUtils.TWO_PI;

        }

        // reference within the hardbody area, first part of eq(11) is equal to 1
        else if (missDistance < radius - DEFAULT_EQUALITY_THRESHOLD) {
            final CommonPateraFunction function = new CommonPateraFunction(xm, ym, sigmaX, sigmaY, radius);
            value = 1 - integrator.integrate(customMaxNbOfEval, function, 0, MathUtils.TWO_PI) / MathUtils.TWO_PI;
        }

        // Peculiar case where miss distance = combined radius, r may be equal to zero so eq(9) must be used
        else {
            final PateraFunctionSpecialCase function = new PateraFunctionSpecialCase(xm, ym, sigmaX, sigmaY, radius);
            value = integrator.integrate(customMaxNbOfEval, function, 0, MathUtils.TWO_PI) / MathUtils.TWO_PI;
        }

        return new ProbabilityOfCollision(value, 0, 0, getName(), isAMaximumProbabilityOfCollisionMethod());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldProbabilityOfCollision<T> compute(final T xm, final T ym,
                                                                                      final T sigmaX, final T sigmaY,
                                                                                      final T radius,
                                                                                      final FieldUnivariateIntegrator<T> customIntegrator,
                                                                                      final int customMaxNbOfEval) {
        // Depending on miss distance and the combined radius, three distinct cases exist
        final Field<T> field      = xm.getField();
        final T        zero       = field.getZero();
        final T        one        = field.getOne();
        final T        twoPiField = one.multiply(MathUtils.TWO_PI);

        final T      value;
        final double missDistance = xm.multiply(xm).add(ym.multiply(ym)).sqrt().getReal();
        final double radiusReal   = radius.getReal();

        // Reference outside the hardbody area, first part of eq(11) is equal to 0
        if (missDistance > radiusReal + DEFAULT_EQUALITY_THRESHOLD) {
            final CommonFieldPateraFunction<T> function =
                    new CommonFieldPateraFunction<>(xm, ym, sigmaX, sigmaY, radius);
            value = customIntegrator.integrate(customMaxNbOfEval, function, zero, twoPiField).divide(twoPiField).negate();
        }

        // Reference within the hardbody area, first part of eq(11) is equal to 1
        else if (missDistance < radiusReal - DEFAULT_EQUALITY_THRESHOLD) {
            final CommonFieldPateraFunction<T> function =
                    new CommonFieldPateraFunction<>(xm, ym, sigmaX, sigmaY, radius);
            value = one.subtract(
                    customIntegrator.integrate(customMaxNbOfEval, function, zero, twoPiField).divide(twoPiField));
        }

        // Peculiar case where miss distance = combined radius, r may be equal to zero so eq(9) must be used
        else {
            final FieldPateraFunctionSpecialCase<T> function =
                    new FieldPateraFunctionSpecialCase<>(xm, ym, sigmaX, sigmaY, radius);
            value = customIntegrator.integrate(customMaxNbOfEval, function, zero, twoPiField).divide(twoPiField);
        }

        return new FieldProbabilityOfCollision<>(value, zero, zero, getName(), isAMaximumProbabilityOfCollisionMethod());
    }

    /** {@inheritDoc} */
    @Override
    public ShortTermEncounter2DPOCMethodType getType() {
        return ShortTermEncounter2DPOCMethodType.PATERA_2005;
    }

    /** Commonly used function used in equation (11) in Patera's paper. */
    private static class CommonPateraFunction extends AbstractPateraFunction {

        /**
         * Constructor.
         *
         * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
         * (m)
         * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
         * (m)
         * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected
         * onto the collision plane (m)
         * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto
         * the collision plane (m)
         * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
         */
        private CommonPateraFunction(final double xm, final double ym, final double sigmaX, final double sigmaY,
                                     final double radius) {
            super(xm, ym, sigmaX, sigmaY, radius);
        }

        /** {@inheritDoc} */
        @Override
        public double value(final double xm, final double ym, final double scaleFactor, final double sigma,
                            final double radius, final double theta) {

            final SinCos sinCosTheta = FastMath.sinCos(theta);
            final double sinTheta    = sinCosTheta.sin();
            final double cosTheta    = sinCosTheta.cos();

            final double xPrime   = getXPrime(cosTheta);
            final double yPrime   = getYPrime(sinTheta);
            final double rSquared = getRSquared(xPrime, yPrime);

            return FastMath.exp(-0.5 * rSquared / sigma / sigma) *
                    (radius * scaleFactor * MathArrays.linearCombination(xm, cosTheta, ym, sinTheta) +
                            scaleFactor * radius * radius) / rSquared;
        }
    }

    /**
     * Function used in the rare case where miss distance = combined radius. It represents equation (9) in Patera's paper but
     * has been modified to be used with Orekit specific inputs.
     */
    private static class PateraFunctionSpecialCase extends AbstractPateraFunction {

        /**
         * Constructor.
         *
         * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
         * (m)
         * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
         * (m)
         * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected
         * onto the collision plane (m)
         * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto
         * the collision plane (m)
         * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
         */
        private PateraFunctionSpecialCase(final double xm, final double ym, final double sigmaX,
                                          final double sigmaY, final double radius) {
            super(xm, ym, sigmaX, sigmaY, radius);
        }

        /** {@inheritDoc} */
        @Override
        public double value(final double xm, final double ym, final double scaleFactor, final double sigma,
                            final double radius, final double theta) {

            final SinCos sinCosTheta = FastMath.sinCos(theta);
            final double sinTheta    = sinCosTheta.sin();
            final double cosTheta    = sinCosTheta.cos();

            final double xPrime = getXPrime(cosTheta);
            final double yPrime = getYPrime(sinTheta);

            final double rSquared          = getRSquared(xPrime, yPrime);
            final double sigmaSquared      = sigma * sigma;
            final double oneOverTwoSigmaSq = 1. / (2 * sigmaSquared);
            final double rSqOverTwoSigmaSq = oneOverTwoSigmaSq * rSquared;

            return radius * scaleFactor * (MathArrays.linearCombination(xm, cosTheta, ym, sinTheta) + radius) *
                    oneOverTwoSigmaSq * (1 - rSqOverTwoSigmaSq * (0.5 - rSqOverTwoSigmaSq * (1. / 6 - rSqOverTwoSigmaSq * (
                    1. / 24 - rSqOverTwoSigmaSq / 720))));

        }
    }

    /** Abstract class for different functions used in Patera's paper. */
    private abstract static class AbstractPateraFunction implements UnivariateFunction {
        /**
         * Position on the x-axis of the rotated encounter frame.
         */
        private final double xm;

        /**
         * Position on the y-axis of the rotated encounter frame.
         */
        private final double ym;

        /**
         * Recurrent term used in Patera 2005 formula.
         */
        private final double scaleFactor;

        /**
         * General sigma after symmetrization.
         */
        private final double sigma;

        /**
         * Hardbody radius (m).
         */
        private final double radius;

        /**
         * Constructor.
         *
         * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
         * (m)
         * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
         * (m)
         * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected
         * onto the collision plane (m)
         * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto
         * the collision plane (m)
         * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
         */
        AbstractPateraFunction(final double xm, final double ym, final double sigmaX, final double sigmaY,
                               final double radius) {
            this.xm          = xm;
            this.ym          = ym;
            this.scaleFactor = sigmaY / sigmaX;
            this.sigma       = sigmaY;
            this.radius      = radius;
        }

        /**
         * Compute the value of the function.
         *
         * @param theta angle at which the function value should be evaluated
         *
         * @return the value of the function
         *
         * @throws IllegalArgumentException when the activated method itself can ascertain that a precondition, specified in
         * the API expressed at the level of the activated method, has been violated. When Hipparchus throws an
         * {@code IllegalArgumentException}, it is usually the consequence of checking the actual parameters passed to the
         * method.
         */
        public double value(final double theta) {
            return value(xm, ym, scaleFactor, sigma, radius, theta);
        }

        /**
         * Compute the value of the defined Patera function for input theta.
         *
         * @param x secondary collision object projected position onto the collision plane in the rotated encounter frame
         * x-axis (m)
         * @param y secondary collision object projected position onto the collision plane in the rotated encounter frame
         * y-axis (m)
         * @param scale scale factor used to symmetrize the probability density of collision, equal to sigmaY / sigmaX
         * @param symmetrizedSigma symmetrized position sigma equal to sigmaY
         * @param combinedRadius sum of primary and secondary collision object equivalent sphere radii (m)
         * @param theta current angle of evaluation of the deformed hardbody radius (rad)
         *
         * @return value of the defined Patera function for input conjunction and theta
         */
        public abstract double value(double x, double y, double scale, double symmetrizedSigma, double combinedRadius,
                                     double theta);

        /**
         * Get current x-axis component to the deformed hardbody perimeter.
         *
         * @param cosTheta cos of the angle determining deformed hardbody radius x-axis component to compute
         *
         * @return current x-axis component to the deformed hardbody perimeter
         */
        public double getXPrime(final double cosTheta) {
            return scaleFactor * (xm + radius * cosTheta);
        }

        /**
         * Get current y-axis component to the deformed hardbody perimeter.
         *
         * @param sinTheta sin of the angle determining deformed hardbody radius y-axis component to compute
         *
         * @return current y-axis component to the deformed hardbody perimeter
         */
        public double getYPrime(final double sinTheta) {
            return ym + radius * sinTheta;
        }

        /**
         * Get current distance from the reference to the determined hardbody perimeter.
         *
         * @param xPrime current x-axis component to the deformed hardbody perimeter
         * @param yPrime current y-axis component to the deformed hardbody perimeter
         *
         * @return current distance from the reference to the determined hardbody perimeter
         */
        public double getRSquared(final double xPrime, final double yPrime) {
            return xPrime * xPrime + yPrime * yPrime;
        }
    }

    /** Commonly used function used in equation (11) in Patera's paper. */
    private static class CommonFieldPateraFunction<T extends CalculusFieldElement<T>>
            extends AbstractFieldPateraFunction<T> {

        /**
         * Constructor.
         *
         * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
         * (m)
         * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
         * (m)
         * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected
         * onto the collision plane (m)
         * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto
         * the collision plane (m)
         * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
         */
        private CommonFieldPateraFunction(final T xm, final T ym, final T sigmaX, final T sigmaY,
                                          final T radius) {
            super(xm, ym, sigmaX, sigmaY, radius);
        }

        /** {@inheritDoc} */
        @Override
        public T value(final T xm, final T ym, final T scaleFactor, final T sigma,
                       final T radius, final T theta) {

            final FieldSinCos<T> sinCosTheta = theta.sinCos();
            final T              sinTheta    = sinCosTheta.sin();
            final T              cosTheta    = sinCosTheta.cos();

            final T xPrime   = getXPrime(cosTheta);
            final T yPrime   = getYPrime(sinTheta);
            final T rSquared = getRSquared(xPrime, yPrime);

            return rSquared.divide(sigma).divide(sigma).multiply(-0.5).exp()
                           .multiply(radius.multiply(scaleFactor).multiply(xm.multiply(cosTheta)
                                                                             .add(ym.multiply(sinTheta)))
                                           .add(scaleFactor.multiply(radius).multiply(radius))).divide(rSquared);
        }

    }

    /**
     * Function used in the rare case where miss distance = combined radius. It represents equation (9) in Patera's paper but
     * has been modified to be used with Orekit specific inputs.
     */
    private static class FieldPateraFunctionSpecialCase<T extends CalculusFieldElement<T>>
            extends AbstractFieldPateraFunction<T> {

        /**
         * Constructor.
         *
         * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
         * (m)
         * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
         * (m)
         * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected
         * onto the collision plane (m)
         * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto
         * the collision plane (m)
         * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
         */
        private FieldPateraFunctionSpecialCase(final T xm, final T ym, final T sigmaX,
                                               final T sigmaY, final T radius) {
            super(xm, ym, sigmaX, sigmaY, radius);
        }

        /** {@inheritDoc} */
        @Override
        public T value(final T xm, final T ym, final T scaleFactor, final T sigma,
                       final T radius, final T theta) {

            final FieldSinCos<T> sinCosTheta = theta.sinCos();
            final T              sinTheta    = sinCosTheta.sin();
            final T              cosTheta    = sinCosTheta.cos();

            final T xPrime            = scaleFactor.multiply(xm).add(scaleFactor.multiply(radius).multiply(cosTheta));
            final T yPrime            = ym.add(radius.multiply(sinTheta));
            final T rSquared          = xPrime.multiply(xPrime).add(yPrime.multiply(yPrime));
            final T sigmaSquared      = sigma.multiply(sigma);
            final T oneOverTwoSigmaSq = sigmaSquared.multiply(2.).reciprocal();
            final T rSqOverTwoSigmaSq = rSquared.multiply(oneOverTwoSigmaSq);

            // Recursive approach to maximize usage of the same fielded variables
            return radius.multiply(scaleFactor).multiply(xm.multiply(cosTheta).add(ym.multiply(sinTheta)).add(radius))
                         .multiply(oneOverTwoSigmaSq.negate()
                                                    .multiply(rSqOverTwoSigmaSq
                                                                      .multiply(rSqOverTwoSigmaSq
                                                                                        .multiply(rSqOverTwoSigmaSq
                                                                                                          .multiply(
                                                                                                                  rSqOverTwoSigmaSq.multiply(
                                                                                                                                           -1. / 720)
                                                                                                                                   .add(1. / 24))
                                                                                                          .subtract(1. / 6))
                                                                                        .add(0.5))
                                                                      .subtract(1.)));
        }
    }

    /** Abstract class for different functions used in Patera's paper. */
    private abstract static class AbstractFieldPateraFunction<T extends CalculusFieldElement<T>> implements
            CalculusFieldUnivariateFunction<T> {
        /**
         * Position on the x-axis of the rotated encounter frame.
         */
        private final T xm;

        /**
         * Position on the y-axis of the rotated encounter frame.
         */
        private final T ym;

        /**
         * Recurrent term used in Patera 2005 formula.
         */
        private final T scaleFactor;

        /**
         * General sigma after symmetrization.
         */
        private final T sigma;

        /**
         * Hardbody radius (m).
         */
        private final T radius;

        /**
         * Constructor.
         *
         * @param xm other collision object projected position onto the collision plane in the rotated encounter frame x-axis
         * (m)
         * @param ym other collision object projected position onto the collision plane in the rotated encounter frame y-axis
         * (m)
         * @param sigmaX square root of the smallest eigen value of the diagonalized combined covariance matrix projected
         * onto the collision plane (m)
         * @param sigmaY square root of the biggest eigen value of the diagonalized combined covariance matrix projected onto
         * the collision plane (m)
         * @param radius sum of primary and secondary collision object equivalent sphere radii (m)
         */
        AbstractFieldPateraFunction(final T xm, final T ym, final T sigmaX, final T sigmaY,
                                    final T radius) {
            this.xm          = xm;
            this.ym          = ym;
            this.scaleFactor = sigmaY.divide(sigmaX);
            this.sigma       = sigmaY;
            this.radius      = radius;
        }

        /**
         * Compute the value of the function.
         *
         * @param theta angle at which the function value should be evaluated
         *
         * @return the value of the function
         *
         * @throws IllegalArgumentException when the activated method itself can ascertain that a precondition, specified in
         * the API expressed at the level of the activated method, has been violated. When Hipparchus throws an
         * {@code IllegalArgumentException}, it is usually the consequence of checking the actual parameters passed to the
         * method.
         */
        @Override
        public T value(final T theta) {
            return value(xm, ym, scaleFactor, sigma, radius, theta);
        }

        /**
         * Compute the value of the defined Patera function for input theta.
         *
         * @param x secondary collision object projected position onto the collision plane in the rotated encounter frame
         * x-axis (m)
         * @param y secondary collision object projected position onto the collision plane in the rotated encounter frame
         * y-axis (m)
         * @param scale scale factor used to symmetrize the probability density of collision, equal to sigmaY / sigmaX
         * @param symmetrizedSigma symmetrized position sigma equal to sigmaY
         * @param combinedRadius sum of primary and secondary collision object equivalent sphere radii (m)
         * @param theta current angle of evaluation of the deformed hardbody radius (rad)
         *
         * @return value of the defined Patera function for input conjunction and theta
         */
        public abstract T value(T x, T y, T scale, T symmetrizedSigma, T combinedRadius,
                                T theta);

        /**
         * Get current x-axis component to the deformed hardbody perimeter.
         *
         * @param cosTheta cos of the angle determining deformed hardbody radius x-axis component to compute
         *
         * @return current x-axis component to the deformed hardbody perimeter
         */
        public T getXPrime(final T cosTheta) {
            return scaleFactor.multiply(xm).add(scaleFactor.multiply(radius).multiply(cosTheta));
        }

        /**
         * Get current y-axis component to the deformed hardbody perimeter.
         *
         * @param sinTheta sin of the angle determining deformed hardbody radius y-axis component to compute
         *
         * @return current y-axis component to the deformed hardbody perimeter
         */
        public T getYPrime(final T sinTheta) {
            return ym.add(radius.multiply(sinTheta));
        }

        /**
         * Get current distance from the reference to the determined hardbody perimeter.
         *
         * @param xPrime current x-axis component to the deformed hardbody perimeter
         * @param yPrime current y-axis component to the deformed hardbody perimeter
         *
         * @return current distance from the reference to the determined hardbody perimeter
         */
        public T getRSquared(final T xPrime, final T yPrime) {
            return xPrime.multiply(xPrime).add(yPrime.multiply(yPrime));
        }
    }

}
