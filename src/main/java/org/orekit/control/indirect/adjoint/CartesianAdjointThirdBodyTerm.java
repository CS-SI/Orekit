/* Copyright 2022-2024 Romain Serra
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
package org.orekit.control.indirect.adjoint;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Class defining the contributions of a point-mass, third body in the adjoint equations for Cartesian coordinates.
 * If present, then the propagator should also include a {@link org.orekit.forces.gravity.ThirdBodyAttraction}.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @see org.orekit.forces.gravity.ThirdBodyAttraction
 * @since 12.2
 */
public class CartesianAdjointThirdBodyTerm extends AbstractCartesianAdjointNonCentralBodyTerm {

    /**
     * Constructor.
     * @param mu body gravitational parameter.
     * @param bodyPositionProvider body position provider
     */
    public CartesianAdjointThirdBodyTerm(final double mu, final ExtendedPositionProvider bodyPositionProvider) {
        super(mu, bodyPositionProvider);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getAcceleration(final AbsoluteDate date, final double[] stateVariables,
                                    final Frame frame) {
        final Vector3D bodyPosition = getBodyPosition(date, frame);
        final double x = stateVariables[0] - bodyPosition.getX();
        final double y = stateVariables[1] - bodyPosition.getY();
        final double z = stateVariables[2] - bodyPosition.getZ();
        final Vector3D newtonianAcceleration = getNewtonianAcceleration(new double[] {x, y, z});
        final double rBody2 = bodyPosition.getNormSq();
        final Vector3D bodyCentralAcceleration = bodyPosition.scalarMultiply(getMu() / (rBody2 * FastMath.sqrt(rBody2)));
        return newtonianAcceleration.subtract(bodyCentralAcceleration);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(final FieldAbsoluteDate<T> date,
                                                                                     final T[] stateVariables,
                                                                                     final Frame frame) {
        final FieldVector3D<T> bodyPosition = getFieldBodyPosition(date, frame);
        final T x = stateVariables[0].subtract(bodyPosition.getX());
        final T y = stateVariables[1].subtract(bodyPosition.getY());
        final T z = stateVariables[2].subtract(bodyPosition.getZ());
        final T[] relativePosition = MathArrays.buildArray(date.getField(), 3);
        relativePosition[0] = x;
        relativePosition[1] = y;
        relativePosition[2] = z;
        final FieldVector3D<T> newtonianAcceleration = getFieldNewtonianAcceleration(relativePosition);
        final T rBody2 = bodyPosition.getNormSq();
        final T factor = rBody2.multiply(rBody2.sqrt()).reciprocal().multiply(getMu());
        final FieldVector3D<T> bodyCentralAcceleration = bodyPosition.scalarMultiply(factor);
        return newtonianAcceleration.subtract(bodyCentralAcceleration);
    }
}
