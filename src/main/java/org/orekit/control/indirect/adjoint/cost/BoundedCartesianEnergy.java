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
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;
import org.orekit.propagation.events.handlers.ResetDerivativesOnEvent;

import java.util.stream.Stream;

/**
 * Class for bounded energy cost with Cartesian coordinates.
 * Here, the control vector is chosen as the thrust force divided by the maximum thrust magnitude and expressed in the propagation frame.
 * It has a unit Euclidean norm.
 * @author Romain Serra
 * @see UnboundedCartesianEnergyNeglectingMass
 * @since 12.2
 */
public class BoundedCartesianEnergy extends CartesianEnergyConsideringMass {

    /** Maximum value of thrust force Euclidean norm. */
    private final double maximumThrustMagnitude;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param eventDetectionSettings singularity event detection settings
     */
    public BoundedCartesianEnergy(final String name, final double massFlowRateFactor,
                                  final double maximumThrustMagnitude,
                                  final EventDetectionSettings eventDetectionSettings) {
        super(name, massFlowRateFactor, eventDetectionSettings);
        this.maximumThrustMagnitude = FastMath.abs(maximumThrustMagnitude);
    }

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     */
    public BoundedCartesianEnergy(final String name, final double massFlowRateFactor,
                                  final double maximumThrustMagnitude) {
        this(name, massFlowRateFactor, maximumThrustMagnitude, EventDetectionSettings.getDefaultEventDetectionSettings());
    }

    /** {@inheritDoc} */
    @Override
    protected double getThrustForceNorm(final double[] adjointVariables, final double mass) {
        final double adjointVelocityNorm = getAdjointVelocityNorm(adjointVariables);
        final double factor = adjointVelocityNorm / mass - getMassFlowRateFactor() * adjointVariables[6];
        if (factor > maximumThrustMagnitude) {
            return maximumThrustMagnitude;
        } else {
            return FastMath.max(0., factor);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> T getFieldThrustForceNorm(final T[] adjointVariables,
                                                                            final T mass) {
        final T adjointVelocityNorm = getFieldAdjointVelocityNorm(adjointVariables);
        final T factor = adjointVelocityNorm.divide(mass).subtract(adjointVariables[6].multiply(getMassFlowRateFactor()));
        final double factorReal = factor.getReal();
        final T zero = mass.getField().getZero();
        if (factorReal > maximumThrustMagnitude) {
            return zero.newInstance(maximumThrustMagnitude);
        } else if (factorReal < 0.) {
            return zero;
        } else {
            return factor;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        final EventDetectionSettings detectionSettings = getEventDetectionSettings();
        return Stream.of(new SingularityDetector(detectionSettings, new ResetDerivativesOnEvent(), 0.),
            new SingularityDetector(detectionSettings, new ResetDerivativesOnEvent(), maximumThrustMagnitude));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final FieldEventDetectionSettings<T> detectionSettings = new FieldEventDetectionSettings<>(field, getEventDetectionSettings());
        final T zero = field.getZero();
        final T maximumThrustMagnitudeForEvent = zero.newInstance(maximumThrustMagnitude);
        return Stream.of(new FieldSingularityDetector<>(detectionSettings, new FieldResetDerivativesOnEvent<>(), zero),
            new FieldSingularityDetector<>(detectionSettings, new FieldResetDerivativesOnEvent<>(), maximumThrustMagnitudeForEvent));
    }

}
