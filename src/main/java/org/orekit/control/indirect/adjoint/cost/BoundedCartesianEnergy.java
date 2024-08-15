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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AdaptableInterval;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventDetectionSettings;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldAdaptableInterval;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;

import java.util.stream.Stream;

/**
 * Class for bounded energy cost with Cartesian coordinates.
 * Here, the control vector is chosen as the acceleration (given by thrusting) divided by the maximum thrust magnitude and expressed in the propagation frame.
 * It is constrained in Euclidean norm by the reciprocal of the current mass.
 * @author Romain Serra
 * @see UnboundedCartesianEnergyNeglectingMass
 * @since 12.2
 */
public class BoundedCartesianEnergy extends AbstractCartesianEnergy {

    /** Maximum value of thrust force Euclidean norm. */
    private final double maximumThrustMagnitude;

    /** Detection settings for the singularity events. */
    private final EventDetectionSettings eventDetectionSettings;

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
        super(name, massFlowRateFactor);
        this.maximumThrustMagnitude = FastMath.abs(maximumThrustMagnitude);
        this.eventDetectionSettings = eventDetectionSettings;
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
    public Vector3D getThrustVector(final double[] adjointVariables, final double mass) {
        return getThrustDirection(adjointVariables).scalarMultiply(getThrustNorm(adjointVariables, mass));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final T[] adjointVariables, final T mass) {
        return getThrustDirection(adjointVariables).scalarMultiply(getThrustNorm(adjointVariables, mass));
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    private Vector3D getThrustDirection(final double[] adjointVariables) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @param <T> field type
     * @return thrust direction
     */
    private <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustDirection(final T[] adjointVariables) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint vector
     * @param mass mass
     *
     * @return norm of thrust force
     */
    private double getThrustNorm(final double[] adjointVariables, final double mass) {
        final double unboundedCase = (getAdjointVelocityNorm(adjointVariables) - getMassFlowRateFactor() * mass * adjointVariables[6]) * mass;
        if (unboundedCase > 1.) {
            return maximumThrustMagnitude;
        } else {
            return unboundedCase * maximumThrustMagnitude;
        }
    }

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @param <T> field type
     * @return norm of thrust force
     */
    private <T extends CalculusFieldElement<T>> T getThrustNorm(final T[] adjointVariables, final T mass) {
        final T unboundedCase = (getAdjointVelocityNorm(adjointVariables).subtract(mass.multiply(getMassFlowRateFactor()).multiply(adjointVariables[6]))).multiply(mass);
        if (unboundedCase.getReal() > 1.) {
            return unboundedCase.getField().getZero().newInstance(maximumThrustMagnitude);
        } else {
            return unboundedCase.multiply(maximumThrustMagnitude);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        final double thrustNorm = getThrustNorm(adjointVariables, mass);
        adjointDerivatives[6] = -getMassFlowRateFactor() * adjointVariables[6] * thrustNorm / mass;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void updateAdjointDerivatives(final T[] adjointVariables, final T mass,
                                                                             final T[] adjointDerivatives) {
        final T thrustNorm = getThrustNorm(adjointVariables, mass);
        adjointDerivatives[6] = adjointVariables[6].multiply(-getMassFlowRateFactor()).multiply(thrustNorm).divide(mass);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        final UnboundedCartesianEnergy unboundedCartesianEnergyForEvent = new UnboundedCartesianEnergy(getAdjointName(), getMassFlowRateFactor());
        return Stream.of(new EnergyCostAdjointSingularityDetector(unboundedCartesianEnergyForEvent,
            maximumThrustMagnitude, eventDetectionSettings));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        final UnboundedCartesianEnergy unboundedCartesianEnergyForEvent = new UnboundedCartesianEnergy(getAdjointName(), getMassFlowRateFactor());
        final T maximumThrustMagnitudeForEvent = field.getZero().newInstance(maximumThrustMagnitude);
        return Stream.of(new FieldEnergyCostAdjointSingularityDetector<>(unboundedCartesianEnergyForEvent,
            maximumThrustMagnitudeForEvent, new FieldEventDetectionSettings<>(field, eventDetectionSettings)));
    }

    /**
     * Event detector for singularities in adjoint dynamics, when the thrust norm reaches its maximum value.
     */
    static class EnergyCostAdjointSingularityDetector extends AbstractDetector<EnergyCostAdjointSingularityDetector> {

        /**
         * Unbounded energy cost used internally.
         */
        private final UnboundedCartesianEnergy unboundedCartesianEnergy;

        /** Maximum value of thrust force Euclidean norm. */
        private final double maximumThrustMagnitudeForDetector;

        /**
         * Private Constructor with all detection settings.
         * @param unboundedCartesianEnergy unbounded Cartesian energy
         * @param maximumThrustMagnitudeForDetector maximum value of thrust force Euclidean norm
         * @param detectionSettings event detection settings
         * @param eventHandler event handler on detection
         */
        private EnergyCostAdjointSingularityDetector(final UnboundedCartesianEnergy unboundedCartesianEnergy,
                                                     final double maximumThrustMagnitudeForDetector,
                                                     final EventDetectionSettings detectionSettings,
                                                     final EventHandler eventHandler) {
            super(detectionSettings, eventHandler);
            this.unboundedCartesianEnergy = unboundedCartesianEnergy;
            this.maximumThrustMagnitudeForDetector = maximumThrustMagnitudeForDetector;
        }

        /**
         * Constructor with default detection settings.
         * @param unboundedCartesianEnergy unbounded energy cost
         * @param maximumThrustMagnitudeForDetector maximum value of thrust force Euclidean norm
         * @param detectionSettings event detection settings
         */
        EnergyCostAdjointSingularityDetector(final UnboundedCartesianEnergy unboundedCartesianEnergy,
                                             final double maximumThrustMagnitudeForDetector,
                                             final EventDetectionSettings detectionSettings) {
            this(unboundedCartesianEnergy, maximumThrustMagnitudeForDetector, detectionSettings,
                (state, detector, isIncreasing) -> Action.RESET_DERIVATIVES);
        }

        /** {@inheritDoc} */
        @Override
        public double g(final SpacecraftState s) {
            final double[] adjointVariables = s.getAdditionalState(unboundedCartesianEnergy.getAdjointName());
            final Vector3D unboundedThrustVector = unboundedCartesianEnergy.getThrustVector(adjointVariables, s.getMass());
            return unboundedThrustVector.getNorm() / maximumThrustMagnitudeForDetector - 1.;
        }

        /** {@inheritDoc} */
        @Override
        protected EnergyCostAdjointSingularityDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                                              final int newMaxIter, final EventHandler newHandler) {
            return new EnergyCostAdjointSingularityDetector(unboundedCartesianEnergy, maximumThrustMagnitudeForDetector,
                    new EventDetectionSettings(newMaxCheck, newThreshold, newMaxIter), newHandler);
        }
    }

    /**
     * Field event detector for singularities in adjoint dynamics, when the thrust norm reaches its maximum value.
     */
    static class FieldEnergyCostAdjointSingularityDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldEnergyCostAdjointSingularityDetector<T>, T> {

        /**
         * Unbounded energy cost used internally.
         */
        private final UnboundedCartesianEnergy unboundedCartesianEnergy;

        /** Maximum value of thrust force Euclidean norm. */
        private final T maximumThrustMagnitudeForDetector;

        /**
         * Private Constructor with all detection settings.
         * @param unboundedCartesianEnergy unbounded Cartesian energy
         * @param maximumThrustMagnitudeForDetector maximum value of thrust force Euclidean norm
         * @param detectionSettings detection settings
         * @param eventHandler event handler on detection
         */
        private FieldEnergyCostAdjointSingularityDetector(final UnboundedCartesianEnergy unboundedCartesianEnergy,
                                                          final T maximumThrustMagnitudeForDetector,
                                                          final FieldEventDetectionSettings<T> detectionSettings,
                                                          final FieldEventHandler<T> eventHandler) {
            super(detectionSettings, eventHandler);
            this.unboundedCartesianEnergy = unboundedCartesianEnergy;
            this.maximumThrustMagnitudeForDetector = maximumThrustMagnitudeForDetector;
        }

        /**
         * Constructor with default detection settings.
         * @param unboundedCartesianEnergy unbounded energy cost
         * @param maximumThrustMagnitudeForDetector maximum value of thrust force Euclidean norm
         * @param detectionSettings event detection settings
         */
        FieldEnergyCostAdjointSingularityDetector(final UnboundedCartesianEnergy unboundedCartesianEnergy,
                                                  final T maximumThrustMagnitudeForDetector,
                                                  final FieldEventDetectionSettings<T> detectionSettings) {
            this(unboundedCartesianEnergy, maximumThrustMagnitudeForDetector, detectionSettings,
                (state, detector, isIncreasing) -> Action.RESET_DERIVATIVES);
        }

        /** {@inheritDoc} */
        @Override
        public T g(final FieldSpacecraftState<T> s) {
            final T[] adjointVariables = s.getAdditionalState(unboundedCartesianEnergy.getAdjointName());
            final FieldVector3D<T> unboundedThrustVector = unboundedCartesianEnergy.getThrustVector(adjointVariables, s.getMass());
            return unboundedThrustVector.getNorm().divide(maximumThrustMagnitudeForDetector).subtract(1.);
        }

        /** {@inheritDoc} */
        @Override
        protected FieldEnergyCostAdjointSingularityDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck,
                                                                      final T newThreshold, final int newMaxIter,
                                                                      final FieldEventHandler<T> newHandler) {
            return new FieldEnergyCostAdjointSingularityDetector<>(unboundedCartesianEnergy, maximumThrustMagnitudeForDetector,
                    new FieldEventDetectionSettings<>(newMaxCheck, newThreshold, newMaxIter), newHandler);
        }
    }
}
