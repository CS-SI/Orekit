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

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.RealFieldUnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.solvers.AllowedSolution;
import org.hipparchus.analysis.solvers.FieldBracketingNthOrderBrentSolver;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** L2 Transform provider for a frame on the L2 Lagrange point of two celestial bodies.
 *
 * @author Luc Maisonobe
 * @author Julio Hernanz
 */

public class L2TransformProvider implements TransformProvider {

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
     * @throws OrekitException in .getInertiallyOrientedFrame() if frame cannot be retrieved
     */
    public L2TransformProvider(final CelestialBody primaryBody, final CelestialBody secondaryBody)
                    throws OrekitException {
        this.primaryBody = primaryBody;
        this.secondaryBody = secondaryBody;
        this.frame = primaryBody.getInertiallyOrientedFrame();
    }

    @Override
    public Transform getTransform(final AbsoluteDate date)
                  throws OrekitException {
        return new Transform(date, getL2(date).negate());
    }


    @Override
    public <T extends RealFieldElement<T>> FieldTransform<T>
        getTransform(final FieldAbsoluteDate<T> date)
                    throws OrekitException {
        // TODO Auto-generated method stub
        return null;
    }

    /** Method to get the {@link PVCoordinates} of the L2 point.
     * @param date current date
     * @return PVCoordinates of the L2 point given in frame: primaryBody.getInertiallyOrientedFrame()
     * @throws OrekitException if some frame specific error occurs at .getTransformTo()
     */
    public PVCoordinates getL2(final AbsoluteDate date)
                    throws OrekitException {

        final PVCoordinates pv21 = secondaryBody.getPVCoordinates(date, frame);
        final FieldVector3D<DerivativeStructure> delta = pv21.toDerivativeStructureVector(2);
        final double q = secondaryBody.getGM() / primaryBody.getGM(); // Mass ratio

        final L2Equation equation = new L2Equation(delta.getNorm(), q);

        // FieldBracketingNthOrderBrentSolver parameters
        final DSFactory dsFactory = delta.getX().getFactory();
        final DerivativeStructure relativeAccuracy = dsFactory.constant(1e-14);
        final DerivativeStructure absoluteAccuracy = dsFactory.constant(1e-3); // i.e. 1mm
        final DerivativeStructure functionValueAccuracy = dsFactory.constant(0);
        final int maximalOrder = 2;
        final FieldBracketingNthOrderBrentSolver<DerivativeStructure> solver =
                        new FieldBracketingNthOrderBrentSolver<DerivativeStructure>(relativeAccuracy,
                                    absoluteAccuracy, functionValueAccuracy, maximalOrder);
        final int maxEval = 1000;

        // Approximate position of L2 point, valid when m2 << m1
        final DerivativeStructure bigR  = delta.getNorm();
        final DerivativeStructure baseR = bigR.multiply(FastMath.cbrt(q / 3) + 1);

        // We build the startValue of the solver method with an approximation
        final double deviationFromApprox = 0.1;
        final DerivativeStructure min = baseR.multiply(1 - deviationFromApprox);
        final DerivativeStructure max = baseR.multiply(1 + deviationFromApprox);
        final DerivativeStructure dsR = solver.solve(maxEval, equation, min, max, AllowedSolution.ANY_SIDE);

        // L2 point is built
        return new PVCoordinates(new FieldVector3D<DerivativeStructure>(dsR,
                        delta.normalize()));
    }

    private class L2Equation implements
        RealFieldUnivariateFunction<DerivativeStructure> {

        /** Distance between primary and secondary body. */
        private final DerivativeStructure delta;

        /** massRatio = m2 / m1. */
        private final double massRatio;

        /** Basic constructor.
         * @param delta absolute value of the distances between the two celestial bodies
         * @param q mass ratio
         */
        L2Equation(final DerivativeStructure delta, final double q) {
            this.delta = delta;
            this.massRatio = q;
        }

        /** value method of the L2 equation for the solver to solve it.
         * @param r The unknown distance of the L2 point from the primary body.
         * @return solution of the L2 equation as lhs-rhs
         */
        public DerivativeStructure value(final DerivativeStructure r) {

            // Left hand side
            final DerivativeStructure lhs1 = r.multiply(r).reciprocal();
            final DerivativeStructure rminusDelta = r.subtract(delta);
            final DerivativeStructure lhs2 = rminusDelta.multiply(rminusDelta).reciprocal()
                      .multiply(massRatio);
            final DerivativeStructure lhs = lhs1.add(lhs2);

            // Right hand side
            final DerivativeStructure rhs1 = delta.multiply(delta).reciprocal();
            final DerivativeStructure rhs2 = rhs1.divide(delta).multiply(rminusDelta).multiply(1 + massRatio);
            final DerivativeStructure rhs = rhs1.add(rhs2);

            // lhs-rhs = 0
            return lhs.subtract(rhs);
        }
    }
}
