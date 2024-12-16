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
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

/**
 * Class for unbounded energy cost with Cartesian coordinates.
 * Here, the control vector is chosen as the thrust force, expressed in the propagation frame.
 * This leads to the optimal thrust being in the same direction as the adjoint velocity.
 *
 * @param <T> field type
 * @author Romain Serra
 * @see FieldUnboundedCartesianEnergyNeglectingMass
 * @see UnboundedCartesianEnergy
 * @since 13.0
 */
public class FieldUnboundedCartesianEnergy<T extends CalculusFieldElement<T>> extends FieldCartesianEnergyConsideringMass<T> {

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param eventDetectionSettings detection settings for singularity detections
     */
    public FieldUnboundedCartesianEnergy(final String name, final T massFlowRateFactor,
                                         final FieldEventDetectionSettings<T> eventDetectionSettings) {
        super(name, massFlowRateFactor, eventDetectionSettings);
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     */
    public FieldUnboundedCartesianEnergy(final String name, final T massFlowRateFactor) {
        this(name, massFlowRateFactor, new FieldEventDetectionSettings<>(massFlowRateFactor.getField(),
                EventDetectionSettings.getDefaultEventDetectionSettings()));
    }

    /** {@inheritDoc} */
    @Override
    protected T getFieldThrustForceNorm(final T[] adjointVariables, final T mass) {
        final T adjointVelocityNorm = getFieldAdjointVelocityNorm(adjointVariables);
        final T factor = adjointVelocityNorm.divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor()));
        if (factor.getReal() < 0.) {
            return adjointVelocityNorm.getField().getZero();
        } else {
            return factor;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.of(new FieldSingularityDetector(field.getZero()));
    }

    /** {@inheritDoc} */
    @Override
    public UnboundedCartesianEnergy toCartesianCost() {
        return new UnboundedCartesianEnergy(getAdjointName(), getMassFlowRateFactor().getReal(),
                getEventDetectionSettings().toEventDetectionSettings());
    }
}
