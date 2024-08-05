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
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Class defining the contributions of a single body gravity in the adjoint equations for Cartesian coordinates.
 * If present, then the propagator should also include the Newtonian attraction of a body.
 * This is similar to {@link CartesianAdjointKeplerianTerm} but with the body not necessarily a central one.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @see org.orekit.forces.gravity.SingleBodyAbsoluteAttraction
 * @since 12.2
 */
public class CartesianAdjointSingleBodyTerm extends AbstractCartesianAdjointNewtonianTerm {

    /** Body gravitational constant. */
    private final double mu;

    /** Extended position provider for the body. */
    private final ExtendedPositionProvider bodyPositionProvider;

    /** Propagation frame. */
    private final Frame propagationFrame;

    /**
     * Constructor.
     * @param mu body gravitational parameter.
     * @param bodyPositionProvider body position provider
     * @param propagationFrame propagation frame
     */
    public CartesianAdjointSingleBodyTerm(final double mu, final ExtendedPositionProvider bodyPositionProvider,
                                          final Frame propagationFrame) {
        this.mu = mu;
        this.bodyPositionProvider = bodyPositionProvider;
        this.propagationFrame = propagationFrame;
    }

    /**
     * Getter for body gravitational parameter.
     * @return gravitational parameter
     */
    public double getMu() {
        return mu;
    }

    /**
     * Getter for the propagation frame (where the equations of motion are integrated).
     * @return propagation frame
     */
    public Frame getPropagationFrame() {
        return propagationFrame;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getVelocityAdjointContribution(final AbsoluteDate date, final double[] stateVariables,
                                                   final double[] adjointVariables) {
        final Vector3D bodyPosition = bodyPositionProvider.getPosition(date, propagationFrame);
        final double x = stateVariables[0] - bodyPosition.getX();
        final double y = stateVariables[1] - bodyPosition.getY();
        final double z = stateVariables[2] - bodyPosition.getZ();
        return getNewtonianVelocityAdjointContribution(mu, new double[] { x, y, z}, adjointVariables);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getVelocityAdjointFieldContribution(final FieldAbsoluteDate<T> date,
                                                                                       final T[] stateVariables,
                                                                                       final T[] adjointVariables) {
        final FieldVector3D<T> bodyPosition = bodyPositionProvider.getPosition(date, propagationFrame);
        final T x = stateVariables[0].subtract(bodyPosition.getX());
        final T y = stateVariables[1].subtract(bodyPosition.getY());
        final T z = stateVariables[2].subtract(bodyPosition.getZ());
        final T[] relativePosition = MathArrays.buildArray(adjointVariables[0].getField(), 3);
        relativePosition[0] = x;
        relativePosition[1] = y;
        relativePosition[2] = z;
        return getFieldNewtonianVelocityAdjointContribution(mu, relativePosition, adjointVariables);
    }
}
