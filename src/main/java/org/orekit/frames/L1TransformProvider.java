/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.frames;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.RealFieldUnivariateFunction;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** L1 Transform provider for a frame on the L1 Lagrange point of two celestial bodies.
 *
 * @author Luc Maisonobe
 * @author Julio Hernanz
 */
public class L1TransformProvider implements TransformProvider {

    /** Relative accuracy on position for solver. */
    private static final double RELATIVE_ACCURACY = 1e-14;

    /** Absolute accuracy on position for solver (1mm). */
    private static final double ABSOLUTE_ACCURACY = 1e-3;

    /** Function value ccuracy for solver (set to 0 so we rely only on position for convergence). */
    private static final double FUNCTION_ACCURACY = 0;

    /** Maximal order for solver. */
    private static final int MAX_ORDER = 5;

    /** Maximal number of evaluations for solver. */
    private static final int MAX_EVALUATIONS = 1000;

    /** Serializable UID.*/
    private static final long serialVersionUID = 20170824L;

    /** Frame for results. Always defined as primaryBody's inertially oriented frame.*/
    private final Frame frame;

    /** Celestial body with bigger mass, m1.*/
    private final CelestialBody primaryBody;

    /** Celestial body with smaller mass, m2.*/
    private final CelestialBody secondaryBody;

    /** Simple constructor.
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     * @throws OrekitException in .getInertiallyOrientedFrame() if frame cannot be retrieved
     */
    public L1TransformProvider(final CelestialBody primaryBody, final CelestialBody secondaryBody)
        throws OrekitException {
        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;
        this.frame = primaryBody.getInertiallyOrientedFrame();
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date)
        throws OrekitException {
        final PVCoordinates pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D      translation = getL1(pv21.getPosition()).negate();
        final Rotation      rotation    = new Rotation(pv21.getPosition(), pv21.getVelocity(),
                                                       Vector3D.PLUS_I, Vector3D.PLUS_J);
        return new Transform(date, new Transform(date, translation), new Transform(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date)
        throws OrekitException {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T>      translation = getL1(pv21.getPosition()).negate();
        final Field<T>              field       = pv21.getPosition().getX().getField();
        final FieldRotation<T>      rotation    = new FieldRotation<T>(pv21.getPosition(), pv21.getVelocity(),
                                                                       FieldVector3D.getPlusI(field),
                                                                       FieldVector3D.getPlusJ(field));
        return new FieldTransform<T>(date, new FieldTransform<T>(date, translation),
                                     new FieldTransform<T>(date, rotation));
    }

    /** Compute the coordinates of the L1 point.
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the L1 point given in frame: primaryBody.getInertiallyOrientedFrame()
     * @throws OrekitException if some frame specific error occurs at .getTransformTo()
     */
    private Vector3D getL1(final Vector3D primaryToSecondary)
        throws OrekitException {

        // mass ratio
        final double massRatio = secondaryBody.getGM() / primaryBody.getGM();

        // Approximate position of L1 point, valid when m2 << m1
        final double bigR  = primaryToSecondary.getNorm();
        final double baseR = bigR * (1 - FastMath.cbrt(massRatio / 3));

        // Accurate position of L1 point, by solving the L1 equilibrium equation
        final UnivariateFunction l1Equation = r -> {
            final double bigrminusR  = bigR - r;
            final double lhs         = 1.0 / ( r * r );
            final double rhs1        = massRatio / (bigrminusR * bigrminusR);
            final double rhs2        = 1.0 / (bigR * bigR);
            final double rhs3        = (1 + massRatio) * bigrminusR * rhs2 / bigR;
            return lhs - (rhs1 + rhs2 - rhs3);
        };
        final double[] searchInterval = UnivariateSolverUtils.bracket(l1Equation,
                                                                      baseR, 0, bigR,
                                                                      0.01 * bigR, 1, MAX_EVALUATIONS);
        final BracketingNthOrderBrentSolver solver =
                        new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                                          ABSOLUTE_ACCURACY,
                                                          FUNCTION_ACCURACY,
                                                          MAX_ORDER);
        final double r = solver.solve(MAX_EVALUATIONS, l1Equation,
                                      searchInterval[0], searchInterval[1],
                                      AllowedSolution.ANY_SIDE);

        // L1 point is built
        return new Vector3D(r / bigR, primaryToSecondary);

    }

    /** Compute the coordinates of the L1 point.
     * @param <T> type of the field elements
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the L1 point given in frame: primaryBody.getInertiallyOrientedFrame()
     * @throws OrekitException if some frame specific error occurs at .getTransformTo()
     */
    private <T extends RealFieldElement<T>> FieldVector3D<T>
        getL1(final FieldVector3D<T> primaryToSecondary)
        throws OrekitException {

        // mass ratio
        final double massRatio = secondaryBody.getGM() / primaryBody.getGM();

        // Approximate position of L1 point, valid when m2 << m1
        final T bigR  = primaryToSecondary.getNorm();
        final T baseR = bigR.multiply(1 - FastMath.cbrt(massRatio / 3));

        // Accurate position of L1 point, by solving the L1 equilibrium equation
        final RealFieldUnivariateFunction<T> l1Equation = r -> {
            final T bigrminusR = bigR.subtract(r);
            final T lhs        = r.multiply(r).reciprocal();
            final T rhs1       = bigrminusR.multiply(bigrminusR).reciprocal().multiply(massRatio);
            final T rhs2       = bigR.multiply(bigR).reciprocal();
            final T rhs3       = bigrminusR.multiply(rhs2).multiply(1 + massRatio).divide(bigR);
            return lhs.subtract(rhs1.add(rhs2).add(rhs3));
        };
        final T zero             = primaryToSecondary.getX().getField().getZero();
        final T[] searchInterval = L1TransformProvider.bracket(l1Equation,
                                                               baseR, zero, bigR.multiply(2),
                                                               bigR.multiply(0.01), zero.add(1),
                                                               MAX_EVALUATIONS);


        final T relativeAccuracy = zero.add(RELATIVE_ACCURACY);
        final T absoluteAccuracy = zero.add(ABSOLUTE_ACCURACY);
        final T functionAccuracy = zero.add(FUNCTION_ACCURACY);

        final FieldBracketingNthOrderBrentSolver<T> solver =
                        new FieldBracketingNthOrderBrentSolver<T>(relativeAccuracy,
                                                                  absoluteAccuracy,
                                                                  functionAccuracy,
                                                                  MAX_ORDER);
        final T r = solver.solve(MAX_EVALUATIONS, l1Equation,
                                 searchInterval[0], searchInterval[1],
                                 AllowedSolution.ANY_SIDE);

        // L1 point is built
        return new FieldVector3D<T>(r.divide(bigR), primaryToSecondary);

    }

    // Methods added temporarily until next Hipparchus release:
    /**
     * This method attempts to find two values a and b satisfying <ul>
     * <li> {@code lowerBound <= a < initial < b <= upperBound} </li>
     * <li> {@code f(a) * f(b) <= 0} </li>
     * </ul>
     * If {@code f} is continuous on {@code [a,b]}, this means that {@code a}
     * and {@code b} bracket a root of {@code f}.
     * <p>
     * The algorithm checks the sign of \( f(l_k) \) and \( f(u_k) \) for increasing
     * values of k, where \( l_k = max(lower, initial - \delta_k) \),
     * \( u_k = min(upper, initial + \delta_k) \), using recurrence
     * \( \delta_{k+1} = r \delta_k + q, \delta_0 = 0\) and starting search with \( k=1 \).
     * The algorithm stops when one of the following happens: <ul>
     * <li> at least one positive and one negative value have been found --  success!</li>
     * <li> both endpoints have reached their respective limits -- MathIllegalArgumentException </li>
     * <li> {@code maximumIterations} iterations elapse -- MathIllegalArgumentException </li></ul>
     * <p>
     * If different signs are found at first iteration ({@code k=1}), then the returned
     * interval will be \( [a, b] = [l_1, u_1] \). If different signs are found at a later
     * iteration {@code k>1}, then the returned interval will be either
     * \( [a, b] = [l_{k+1}, l_{k}] \) or \( [a, b] = [u_{k}, u_{k+1}] \). A root solver called
     * with these parameters will therefore start with the smallest bracketing interval known
     * at this step.
     * </p>
     * <p>
     * Interval expansion rate is tuned by changing the recurrence parameters {@code r} and
     * {@code q}. When the multiplicative factor {@code r} is set to 1, the sequence is a
     * simple arithmetic sequence with linear increase. When the multiplicative factor {@code r}
     * is larger than 1, the sequence has an asymptotically exponential rate. Note than the
     * additive parameter {@code q} should never be set to zero, otherwise the interval would
     * degenerate to the single initial point for all values of {@code k}.
     * </p>
     * <p>
     * As a rule of thumb, when the location of the root is expected to be approximately known
     * within some error margin, {@code r} should be set to 1 and {@code q} should be set to the
     * order of magnitude of the error margin. When the location of the root is really a wild guess,
     * then {@code r} should be set to a value larger than 1 (typically 2 to double the interval
     * length at each iteration) and {@code q} should be set according to half the initial
     * search interval length.
     * </p>
     * <p>
     * As an example, if we consider the trivial function {@code f(x) = 1 - x} and use
     * {@code initial = 4}, {@code r = 1}, {@code q = 2}, the algorithm will compute
     * {@code f(4-2) = f(2) = -1} and {@code f(4+2) = f(6) = -5} for {@code k = 1}, then
     * {@code f(4-4) = f(0) = +1} and {@code f(4+4) = f(8) = -7} for {@code k = 2}. Then it will
     * return the interval {@code [0, 2]} as the smallest one known to be bracketing the root.
     * As shown by this example, the initial value (here {@code 4}) may lie outside of the returned
     * bracketing interval.
     * </p>
     * @param <T> type of the field elements
     * @param function function to check
     * @param initial Initial midpoint of interval being expanded to
     * bracket a root.
     * @param lowerBound Lower bound (a is never lower than this value).
     * @param upperBound Upper bound (b never is greater than this
     * value).
     * @param q additive offset used to compute bounds sequence (must be strictly positive)
     * @param r multiplicative factor used to compute bounds sequence
     * @param maximumIterations Maximum number of iterations to perform
     * @return a two element array holding the bracketing values.
     * @exception MathIllegalArgumentException if function cannot be bracketed in the search interval
     * @since 1.2
     */
    private static <T extends RealFieldElement<T>> T[] bracket(final RealFieldUnivariateFunction<T> function,
                                                              final T initial,
                                                              final T lowerBound, final T upperBound,
                                                              final T q, final T r,
                                                              final int maximumIterations)
        throws MathIllegalArgumentException {

        MathUtils.checkNotNull(function, LocalizedCoreFormats.FUNCTION);

        if (q.getReal() <= 0)  {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.NUMBER_TOO_SMALL_BOUND_EXCLUDED,
                                                   q, 0);
        }
        if (maximumIterations <= 0)  {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.INVALID_MAX_ITERATIONS, maximumIterations);
        }
        verifySequence(lowerBound.getReal(), initial.getReal(), upperBound.getReal());

        // initialize the recurrence
        T a     = initial;
        T b     = initial;
        T fa    = null;
        T fb    = null;
        T delta = initial.getField().getZero();

        for (int numIterations = 0;
             (numIterations < maximumIterations) &&
                (a.getReal() > lowerBound.getReal() || b.getReal() < upperBound.getReal());
             ++numIterations) {

            final T previousA  = a;
            final T previousFa = fa;
            final T previousB  = b;
            final T previousFb = fb;

            delta = r.multiply(delta).add(q);
            a     = max(initial.subtract(delta), lowerBound);
            b     = min(initial.add(delta), upperBound);
            fa    = function.value(a);
            fb    = function.value(b);

            if (numIterations == 0) {
                // at first iteration, we don't have a previous interval
                // we simply compare both sides of the initial interval
                if (fa.multiply(fb).getReal() <= 0) {
                    // the first interval already brackets a root
                    final T[] interval = MathArrays.buildArray(initial.getField(), 2);
                    interval[0] = a;
                    interval[1] = b;
                    return interval;
                }
            } else {
                // we have a previous interval with constant sign and expand it,
                // we expect sign changes to occur at boundaries
                if (fa.multiply(previousFa).getReal() <= 0) {
                    // sign change detected at near lower bound
                    final T[] interval = MathArrays.buildArray(initial.getField(), 2);
                    interval[0] = a;
                    interval[1] = previousA;
                    return interval;
                } else if (fb.multiply(previousFb).getReal() <= 0) {
                    // sign change detected at near upper bound
                    final T[] interval = MathArrays.buildArray(initial.getField(), 2);
                    interval[0] = previousB;
                    interval[1] = b;
                    return interval;
                }
            }

        }

        // no bracketing found
        throw new MathIllegalArgumentException(LocalizedCoreFormats.NOT_BRACKETING_INTERVAL,
                                               a.getReal(), b.getReal(), fa.getReal(), fb.getReal());

    }


    /** Compute the maximum of two values.
     * @param <T> type of the field elements
     * @param a first value
     * @param b second value
     * @return b if a is lesser or equal to b, a otherwise
     * @since 1.2
     */
    private static <T extends RealFieldElement<T>> T max(final T a, final T b) {
        return (a.subtract(b).getReal() <= 0) ? b : a;
    }

    /** Compute the minimum of two values.
     * @param <T> type of the field elements
     * @param a first value
     * @param b second value
     * @return a if a is lesser or equal to b, b otherwise
     * @since 1.2
     */
    private static <T extends RealFieldElement<T>> T min(final T a, final T b) {
        return (a.subtract(b).getReal() <= 0) ? a : b;
    }

    /**
     * Check that the endpoints specify an interval.
     *
     * @param lower Lower endpoint.
     * @param upper Upper endpoint.
     * @throws MathIllegalArgumentException if {@code lower >= upper}.
     */
    public static void verifyInterval(final double lower,
                                      final double upper)
        throws MathIllegalArgumentException {
        if (lower >= upper) {
            throw new MathIllegalArgumentException(LocalizedCoreFormats.ENDPOINTS_NOT_AN_INTERVAL,
                                                lower, upper, false);
        }
    }

    /**
     * Check that {@code lower < initial < upper}.
     *
     * @param lower Lower endpoint.
     * @param initial Initial value.
     * @param upper Upper endpoint.
     * @throws MathIllegalArgumentException if {@code lower >= initial} or
     * {@code initial >= upper}.
     */
    public static void verifySequence(final double lower,
                                      final double initial,
                                      final double upper)
        throws MathIllegalArgumentException {
        verifyInterval(lower, initial);
        verifyInterval(initial, upper);
    }

}
