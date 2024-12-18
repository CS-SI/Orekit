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
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Class defining the contributions of a point-mass, single body gravity in the adjoint equations for Cartesian coordinates.
 * If present, then the propagator should also include the Newtonian attraction of a body.
 * This is similar to {@link CartesianAdjointKeplerianTerm} but with the body not necessarily a central one.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @see org.orekit.forces.gravity.SingleBodyAbsoluteAttraction
 * @since 12.2
 */
public class CartesianAdjointSingleBodyTerm extends AbstractCartesianAdjointNonCentralBodyTerm {

    /**
     * Constructor.
     * @param mu body gravitational parameter.
     * @param bodyPositionProvider body position provider
     */
    public CartesianAdjointSingleBodyTerm(final double mu, final ExtendedPositionProvider bodyPositionProvider) {
        super(mu, bodyPositionProvider);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getAcceleration(final AbsoluteDate date, final double[] stateVariables,
                                    final Frame frame) {
        final double[] relativePosition = formRelativePosition(date, stateVariables, frame);
        return getNewtonianAcceleration(relativePosition);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(final FieldAbsoluteDate<T> date,
                                                                                     final T[] stateVariables,
                                                                                     final Frame frame) {
        final T[] relativePosition = formFieldRelativePosition(date, stateVariables, frame);
        return getFieldNewtonianAcceleration(relativePosition);
    }
}
