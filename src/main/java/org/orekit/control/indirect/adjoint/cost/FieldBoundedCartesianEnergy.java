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
import org.hipparchus.util.FastMath;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

/**
 * Class for bounded energy cost with Cartesian coordinates.
 * An energy cost is proportional to the integral over time of the squared Euclidean norm of the control vector, often scaled with 1/2.
 * This type of cost is not optimal in terms of mass consumption, however its solutions showcase a smoother behavior favorable for convergence in shooting techniques.
 * Here, the control vector is chosen as the thrust force divided by the maximum thrust magnitude and expressed in the propagation frame.
 * It has a unit Euclidean norm.
 *
 * @param <T> field type
 * @author Romain Serra
 * @see FieldUnboundedCartesianEnergy
 * @see BoundedCartesianEnergy
 * @since 13.0
 */
public class FieldBoundedCartesianEnergy<T extends CalculusFieldElement<T>> extends FieldCartesianEnergyConsideringMass<T> {

    /** Maximum value of thrust force Euclidean norm. */
    private final T maximumThrustMagnitude;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings singularity event detection settings
     */
    public FieldBoundedCartesianEnergy(final String name, final T massFlowRateFactor,
                                       final T maximumThrustMagnitude,
                                       final FieldEventDetectionSettings<T> eventDetectionSettings) {
        super(name, massFlowRateFactor, eventDetectionSettings);
        this.maximumThrustMagnitude = FastMath.abs(maximumThrustMagnitude);
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     */
    public FieldBoundedCartesianEnergy(final String name, final T massFlowRateFactor,
                                       final T maximumThrustMagnitude) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, new FieldEventDetectionSettings<>(massFlowRateFactor.getField(),
                EventDetectionSettings.getDefaultEventDetectionSettings()));
    }

    /** Getter for maximum thrust magnitude.
     * @return maximum thrust
     */
    public T getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /** {@inheritDoc} */
    @Override
    protected T getFieldThrustForceNorm(final T[] adjointVariables, final T mass) {
        final T adjointVelocityNorm = getFieldAdjointVelocityNorm(adjointVariables);
        final T factor = adjointVelocityNorm.divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor()));
        final double factorReal = factor.getReal();
        final T zero = mass.getField().getZero();
        if (factorReal > maximumThrustMagnitude.getReal()) {
            return maximumThrustMagnitude;
        } else if (factorReal < 0.) {
            return zero;
        } else {
            return factor;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final T zero = field.getZero();
        return Stream.of(new FieldSingularityDetector(zero), new FieldSingularityDetector(maximumThrustMagnitude));
    }

    /** {@inheritDoc} */
    @Override
    public BoundedCartesianEnergy toCartesianCost() {
        return new BoundedCartesianEnergy(getAdjointName(), getMassFlowRateFactor().getReal(), maximumThrustMagnitude.getReal(),
                getEventDetectionSettings().toEventDetectionSettings());
    }

}
