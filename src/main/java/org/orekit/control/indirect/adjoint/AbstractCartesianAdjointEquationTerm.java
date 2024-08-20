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
 * Abstract class to define terms in the adjoint equations and Hamiltonian for Cartesian coordinates.
 * @author Romain Serra
 * @see CartesianAdjointDerivativesProvider
 * @see FieldCartesianAdjointDerivativesProvider
 * @since 12.2
 */
public abstract class AbstractCartesianAdjointEquationTerm implements CartesianAdjointEquationTerm {

    /** {@inheritDoc} */
    @Override
    public double getHamiltonianContribution(final AbsoluteDate date, final double[] stateVariables,
                                             final double[] adjointVariables, final Frame frame) {
        final Vector3D acceleration = getAcceleration(date, stateVariables, adjointVariables, frame);
        return acceleration.getX() * adjointVariables[3] + acceleration.getY() * adjointVariables[4] + acceleration.getZ() * adjointVariables[5];
    }

    /**
     * Compute the acceleration vector.
     * @param date date
     * @param stateVariables state variables
     * @param adjointVariables adjoint variables
     * @param frame propagation frame
     * @return acceleration vector
     */
    protected abstract Vector3D getAcceleration(AbsoluteDate date, double[] stateVariables,
                                                double[] adjointVariables, Frame frame);

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(final FieldAbsoluteDate<T> date,
                                                                                 final T[] stateVariables,
                                                                                 final T[] adjointVariables,
                                                                                 final Frame frame) {
        final FieldVector3D<T> acceleration = getFieldAcceleration(date, stateVariables, adjointVariables, frame);
        return acceleration.dotProduct(new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]));
    }

    /**
     * Compute the acceleration vector.
     * @param date date
     * @param stateVariables state variables
     * @param adjointVariables adjoint variables
     * @param frame propagation frame
     * @param <T> field type
     * @return acceleration vector
     */
    protected abstract <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldAcceleration(FieldAbsoluteDate<T> date,
                                                                                                 T[] stateVariables,
                                                                                                 T[] adjointVariables,
                                                                                                 Frame frame);
}
