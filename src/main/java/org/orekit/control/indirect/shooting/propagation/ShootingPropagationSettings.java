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
package org.orekit.control.indirect.shooting.propagation;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.AttitudeProviderModifier;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;

import java.util.List;

/**
 * Defines propagation settings for indirect shooting methods.
 * The provided list of {@link ForceModel} should have their counterpart in the provided adjoint equations encapsulated in {@link AdjointDynamicsProvider}.
 * Note that in case of orbit-based propagation (with a central body), the Newtonian term still needs to be passed explicitly (with its adjoint equivalent).
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see org.orekit.propagation.numerical.FieldNumericalPropagator
 */
public class ShootingPropagationSettings {

    /** Force models. */
    private final List<ForceModel> forceModels;

    /** Adjoint dynamics. */
    private final AdjointDynamicsProvider adjointDynamicsProvider;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Propagation frame. */
    private final Frame propagationFrame;

    /** Integration settings. */
    private final ShootingIntegrationSettings integrationSettings;

    /**
     * Simple constructor with default frame and attitude provider.
     * @param forceModels forces for numerical propagation
     * @param adjointDynamicsProvider adjoint derivatives provider
     * @param integrationSettings integration settings
     */
    @DefaultDataContext
    public ShootingPropagationSettings(final List<ForceModel> forceModels,
                                       final AdjointDynamicsProvider adjointDynamicsProvider,
                                       final ShootingIntegrationSettings integrationSettings) {
        this(forceModels, adjointDynamicsProvider, FramesFactory.getGCRF(), integrationSettings,
            AttitudeProviderModifier.getFrozenAttitudeProvider(new FrameAlignedProvider(FramesFactory.getGCRF())));
    }

    /**
     * Constructor.
     * @param forceModels forces for numerical propagation
     * @param propagationFrame frame used as reference frame in equations of motion by integrator
     * @param adjointDynamicsProvider adjoint derivatives provider
     * @param integrationSettings integration settings
     * @param attitudeProvider attitude provider
     */
    public ShootingPropagationSettings(final List<ForceModel> forceModels,
                                       final AdjointDynamicsProvider adjointDynamicsProvider,
                                       final Frame propagationFrame,
                                       final ShootingIntegrationSettings integrationSettings,
                                       final AttitudeProvider attitudeProvider) {
        this.forceModels = forceModels;
        this.adjointDynamicsProvider = adjointDynamicsProvider;
        this.propagationFrame = propagationFrame;
        this.integrationSettings = integrationSettings;
        this.attitudeProvider = attitudeProvider;
    }

    /**
     * Getter for adjoint dynamics provider.
     * @return adjoint dynamics
     */
    public AdjointDynamicsProvider getAdjointDynamicsProvider() {
        return adjointDynamicsProvider;
    }

    /**
     * Getter for the force models.
     * @return forces
     */
    public List<ForceModel> getForceModels() {
        return forceModels;
    }

    /**
     * Getter for the attitude provider.
     * @return attitude provider.
     */
    public AttitudeProvider getAttitudeProvider() {
        return attitudeProvider;
    }

    /**
     * Getter for the propagation frame.
     * @return propagation frame
     */
    public Frame getPropagationFrame() {
        return propagationFrame;
    }

    /**
     * Getter for the integration settings.
     * @return integration settings
     */
    public ShootingIntegrationSettings getIntegrationSettings() {
        return integrationSettings;
    }
}
