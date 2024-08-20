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

/**
 * Class defining the Keplerian contributions in the adjoint equations for Cartesian coordinates.
 * If present, then the propagator should also include the Newtonian attraction of a central body.
 * @author Romain Serra
 * @see CartesianAdjointEquationTerm
 * @see org.orekit.forces.gravity.NewtonianAttraction
 * @since 12.2
 */
public class CartesianAdjointKeplerianTerm extends AbstractCartesianAdjointNewtonianTerm {

    /**
     * Constructor.
     * @param mu central body gravitational parameter
     */
    public CartesianAdjointKeplerianTerm(final double mu) {
        super(mu);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getVelocityAdjointContribution(final AbsoluteDate date, final double[] stateVariables,
                                                   final double[] adjointVariables, final Frame frame) {
        return getNewtonianVelocityAdjointContribution(stateVariables, adjointVariables);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T[] getVelocityAdjointFieldContribution(final FieldAbsoluteDate<T> date,
                                                                                       final T[] stateVariables,
                                                                                       final T[] adjointVariables,
                                                                                       final Frame frame) {
        return getFieldNewtonianVelocityAdjointContribution(stateVariables, adjointVariables);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getAcceleration(final AbsoluteDate date, final double[] stateVariables,
                                       final Frame frame) {
        return getNewtonianAcceleration(stateVariables);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(final FieldAbsoluteDate<T> date,
                                                                                        final T[] stateVariables,
                                                                                        final Frame frame) {
        return getFieldNewtonianAcceleration(stateVariables);
    }
}
