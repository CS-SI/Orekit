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
package org.orekit.frames;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.CalculusFieldUnivariateFunction;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolverUtils;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** L2 Transform provider for a frame on the L2 Lagrange point of two celestial bodies.
 *
 * @author Luc Maisonobe
 * @author Julio Hernanz
 */
class L2TransformProvider implements TransformProvider {

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
    private static final long serialVersionUID = 20170725L;

    /** Frame for results. Always defined as primaryBody's inertially oriented frame.*/
    private final Frame frame;

    /** Celestial body with bigger mass, m1.*/
    private final CelestialBody primaryBody;

    /** Celestial body with smaller mass, m2.*/
    private final CelestialBody secondaryBody;

    /** Simple constructor.
     * @param primaryBody Primary body.
     * @param secondaryBody Secondary body.
     */
    L2TransformProvider(final CelestialBody primaryBody, final CelestialBody secondaryBody) {
        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;
        this.frame = primaryBody.getInertiallyOrientedFrame();
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final PVCoordinates pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D      translation = getL2(pv21.getPosition()).negate();
        final Rotation      rotation    = new Rotation(pv21.getPosition(), pv21.getVelocity(),
                                                       Vector3D.PLUS_I, Vector3D.PLUS_J);
        return new Transform(date, new Transform(date, translation), new Transform(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        final PVCoordinates pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D      translation = getL2(pv21.getPosition()).negate();
        final Rotation      rotation    = new Rotation(pv21.getPosition(), pv21.getVelocity(),
                Vector3D.PLUS_I, Vector3D.PLUS_J);
        return StaticTransform.compose(
                date,
                StaticTransform.of(date, translation),
                StaticTransform.of(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T>      translation = getL2(pv21.getPosition()).negate();
        final Field<T>              field       = pv21.getPosition().getX().getField();
        final FieldRotation<T>      rotation    = new FieldRotation<>(pv21.getPosition(), pv21.getVelocity(),
                                                                      FieldVector3D.getPlusI(field),
                                                                      FieldVector3D.getPlusJ(field));
        return new FieldTransform<T>(date,
                                     new FieldTransform<>(date, translation),
                                     new FieldTransform<>(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T>      translation = getL2(pv21.getPosition()).negate();
        final FieldRotation<T>      rotation    = new FieldRotation<>(pv21.getPosition(), pv21.getVelocity(),
                FieldVector3D.getPlusI(date.getField()), FieldVector3D.getPlusJ(date.getField()));
        return FieldStaticTransform.compose(
                date,
                FieldStaticTransform.of(date, translation),
                FieldStaticTransform.of(date, rotation));
    }

    /** Compute the coordinates of the L2 point.
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the L2 point given in frame: primaryBody.getInertiallyOrientedFrame()
     */
    private Vector3D getL2(final Vector3D primaryToSecondary) {

        // mass ratio
        final double massRatio = secondaryBody.getGM() / primaryBody.getGM();

        // Approximate position of L2 point, valid when m2 << m1
        final double bigR  = primaryToSecondary.getNorm();
        final double baseR = bigR * (FastMath.cbrt(massRatio / 3) + 1);

        // Accurate position of L2 point, by solving the L2 equilibrium equation
        final UnivariateFunction l2Equation = r -> {
            final double rminusbigR  = r - bigR;
            final double lhs1        = 1.0 / (r * r);
            final double lhs2        = massRatio / (rminusbigR * rminusbigR);
            final double rhs1        = 1.0 / (bigR * bigR);
            final double rhs2        = (1 + massRatio) * rminusbigR * rhs1 / bigR;
            return (lhs1 + lhs2) - (rhs1 + rhs2);
        };
        final double[] searchInterval = UnivariateSolverUtils.bracket(l2Equation,
                                                                      baseR, 0, 2 * bigR,
                                                                      0.01 * bigR, 1, MAX_EVALUATIONS);
        final BracketingNthOrderBrentSolver solver =
                        new BracketingNthOrderBrentSolver(RELATIVE_ACCURACY,
                                                          ABSOLUTE_ACCURACY,
                                                          FUNCTION_ACCURACY,
                                                          MAX_ORDER);
        final double r = solver.solve(MAX_EVALUATIONS, l2Equation,
                                      searchInterval[0], searchInterval[1],
                                      AllowedSolution.ANY_SIDE);

        // L2 point is built
        return new Vector3D(r / bigR, primaryToSecondary);

    }

    /** Compute the coordinates of the L2 point.
     * @param <T> type of the field elements
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the L2 point given in frame: primaryBody.getInertiallyOrientedFrame()
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T>
        getL2(final FieldVector3D<T> primaryToSecondary) {

        // mass ratio
        final double massRatio = secondaryBody.getGM() / primaryBody.getGM();

        // Approximate position of L2 point, valid when m2 << m1
        final T bigR  = primaryToSecondary.getNorm();
        final T baseR = bigR.multiply(FastMath.cbrt(massRatio / 3) + 1);

        // Accurate position of L2 point, by solving the L2 equilibrium equation
        final CalculusFieldUnivariateFunction<T> l2Equation = r -> {
            final T rminusbigR = r.subtract(bigR);
            final T lhs1       = r.multiply(r).reciprocal();
            final T lhs2       = rminusbigR.multiply(rminusbigR).reciprocal().multiply(massRatio);
            final T rhs1       = bigR.multiply(bigR).reciprocal();
            final T rhs2       = rminusbigR.multiply(rhs1).multiply(1 + massRatio).divide(bigR);
            return lhs1.add(lhs2).subtract(rhs1.add(rhs2));
        };
        final T zero             = primaryToSecondary.getX().getField().getZero();
        final T[] searchInterval = UnivariateSolverUtils.bracket(l2Equation,
                                                                 baseR, zero, bigR.multiply(2),
                                                                 bigR.multiply(0.01), zero.add(1),
                                                                 MAX_EVALUATIONS);


        final T relativeAccuracy = zero.add(RELATIVE_ACCURACY);
        final T absoluteAccuracy = zero.add(ABSOLUTE_ACCURACY);
        final T functionAccuracy = zero.add(FUNCTION_ACCURACY);

        final FieldBracketingNthOrderBrentSolver<T> solver =
                        new FieldBracketingNthOrderBrentSolver<>(relativeAccuracy,
                                                                 absoluteAccuracy,
                                                                 functionAccuracy,
                                                                 MAX_ORDER);
        final T r = solver.solve(MAX_EVALUATIONS, l2Equation,
                                 searchInterval[0], searchInterval[1],
                                 AllowedSolution.ANY_SIDE);

        // L2 point is built
        return new FieldVector3D<>(r.divide(bigR), primaryToSecondary);

    }

}
