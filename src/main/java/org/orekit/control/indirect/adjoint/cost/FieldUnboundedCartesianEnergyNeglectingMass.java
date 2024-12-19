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
package org.orekit.control.indirect.adjoint.cost;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;

/**
 * Class for unbounded energy cost with Cartesian coordinates neglecting the mass consumption.
 * Under this assumption, the mass is constant and there is no need to consider the corresponding adjoint variable.
 * Here, the control vector is chosen as the acceleration given by thrusting, expressed in the propagation frame.
 * This leads to the optimal thrust force being equal to the adjoint velocity vector times the mass.
 *
 * @param <T> field type
 * @author Romain Serra
 * @since 13.0
 */
public class FieldUnboundedCartesianEnergyNeglectingMass<T extends CalculusFieldElement<T>> implements FieldCartesianCost<T> {

    /** Adjoint vector name. */
    private final String name;

    /** Field. */
    private final Field<T> field;

    /**
     * Constructor.
     * @param name name
     * @param field field
     */
    public FieldUnboundedCartesianEnergyNeglectingMass(final String name, final Field<T> field) {
        this.name = name;
        this.field = field;
    }

    /**
     * Getter for adjoint vector name.
     * @return name
     */
    @Override
    public String getAdjointName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public int getAdjointDimension() {
        return 6;
    }

    /** {@inheritDoc} */
    @Override
    public T getMassFlowRateFactor() {
        return field.getZero();
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables, final T mass) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]);
    }

    /** {@inheritDoc} */
    @Override
    public void updateFieldAdjointDerivatives(final T[] adjointVariables, final T mass, final T[] adjointDerivatives) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public T getFieldHamiltonianContribution(final T[] adjointVariables, final T mass) {
        final FieldVector3D<T> thrustAcceleration = getFieldThrustAccelerationVector(adjointVariables, mass);
        return thrustAcceleration.getNormSq().multiply(-1. / 2.);
    }

    /** {@inheritDoc} */
    @Override
    public UnboundedCartesianEnergyNeglectingMass toCartesianCost() {
        return new UnboundedCartesianEnergyNeglectingMass(getAdjointName());
    }
}
