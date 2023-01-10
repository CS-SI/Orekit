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
     */
    public L1TransformProvider(final CelestialBody primaryBody, final CelestialBody secondaryBody) {
        this.primaryBody   = primaryBody;
        this.secondaryBody = secondaryBody;
        this.frame         = primaryBody.getInertiallyOrientedFrame();
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final PVCoordinates pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D      translation = getL1(pv21.getPosition()).negate();
        final Rotation      rotation    = new Rotation(pv21.getPosition(), pv21.getVelocity(),
                                                       Vector3D.PLUS_I, Vector3D.PLUS_J);
        return new Transform(date, new Transform(date, translation), new Transform(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public StaticTransform getStaticTransform(final AbsoluteDate date) {
        final PVCoordinates pv21        = secondaryBody.getPVCoordinates(date, frame);
        final Vector3D      translation = getL1(pv21.getPosition()).negate();
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
        final FieldVector3D<T>      translation = getL1(pv21.getPosition()).negate();
        final Field<T>              field       = pv21.getPosition().getX().getField();
        final FieldRotation<T>      rotation    = new FieldRotation<>(pv21.getPosition(), pv21.getVelocity(),
                                                                      FieldVector3D.getPlusI(field),
                                                                      FieldVector3D.getPlusJ(field));
        return new FieldTransform<>(date,
                                    new FieldTransform<>(date, translation),
                                    new FieldTransform<>(date, rotation));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldStaticTransform<T> getStaticTransform(final FieldAbsoluteDate<T> date) {
        final FieldPVCoordinates<T> pv21        = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<T>      translation = getL1(pv21.getPosition()).negate();
        final FieldRotation<T>      rotation    = new FieldRotation<>(pv21.getPosition(), pv21.getVelocity(),
                FieldVector3D.getPlusI(date.getField()), FieldVector3D.getPlusJ(date.getField()));
        return FieldStaticTransform.compose(
                date,
                FieldStaticTransform.of(date, translation),
                FieldStaticTransform.of(date, rotation));
    }

    /** Compute the coordinates of the L1 point.
     * @param primaryToSecondary relative position of secondary body with respect to primary body
     * @return coordinates of the L1 point given in frame: primaryBody.getInertiallyOrientedFrame()
     */
    private Vector3D getL1(final Vector3D primaryToSecondary) {

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
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T>
        getL1(final FieldVector3D<T> primaryToSecondary) {

        // mass ratio
        final double massRatio = secondaryBody.getGM() / primaryBody.getGM();

        // Approximate position of L1 point, valid when m2 << m1
        final T bigR  = primaryToSecondary.getNorm();
        final T baseR = bigR.multiply(1 - FastMath.cbrt(massRatio / 3));

        // Accurate position of L1 point, by solving the L1 equilibrium equation
        final CalculusFieldUnivariateFunction<T> l1Equation = r -> {
            final T bigrminusR = bigR.subtract(r);
            final T lhs        = r.multiply(r).reciprocal();
            final T rhs1       = bigrminusR.multiply(bigrminusR).reciprocal().multiply(massRatio);
            final T rhs2       = bigR.multiply(bigR).reciprocal();
            final T rhs3       = bigrminusR.multiply(rhs2).multiply(1 + massRatio).divide(bigR);
            return lhs.subtract(rhs1.add(rhs2).add(rhs3));
        };
        final T zero             = primaryToSecondary.getX().getField().getZero();
        final T[] searchInterval = UnivariateSolverUtils.bracket(l1Equation,
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
        final T r = solver.solve(MAX_EVALUATIONS, l1Equation,
                                 searchInterval[0], searchInterval[1],
                                 AllowedSolution.ANY_SIDE);

        // L1 point is built
        return new FieldVector3D<>(r.divide(bigR), primaryToSecondary);

    }

}
