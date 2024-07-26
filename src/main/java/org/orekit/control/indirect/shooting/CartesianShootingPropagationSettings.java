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
package org.orekit.control.indirect.shooting;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.control.indirect.adjoint.CartesianAdjointEquationTerm;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;

import java.util.List;

/**
 * Defines propagation settings for indirect shooting methods with Cartesian coordinates.
 * The provided {@link ForceModel} should have their equivalent for adjoint dynamics as {@link CartesianAdjointEquationTerm}.
 * For example, {@link org.orekit.forces.gravity.NewtonianAttraction} goes with {@link org.orekit.control.indirect.adjoint.CartesianAdjointKeplerianTerm}.
 *
 * @author Romain Serra
 * @since 12.2
 * @see org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider
 * @see org.orekit.control.indirect.adjoint.FieldCartesianAdjointDerivativesProvider
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @see org.orekit.propagation.numerical.FieldNumericalPropagator
 */
public class CartesianShootingPropagationSettings {

    /** Force models. */
    private final List<ForceModel> forceModels;

    /** Adjoint equation Cartesian terms. */
    private final List<CartesianAdjointEquationTerm> adjointEquationTerms;

    /** Cost function. */
    private final CartesianCost cartesianCost;

    /** Attitude provider. */
    private final AttitudeProvider attitudeProvider;

    /** Propagation frame. */
    private final Frame propagationFrame;

    /** Integration settings. */
    private final ShootingIntegrationSettings integrationSettings;

    /**
     * Simple constructor with default frame and attitude provider.
     * @param forceModels forces for numerical propagation
     * @param adjointEquationTerms contributions to the adjoint dynamics for Cartesian coordinates
     * @param cartesianCost cost function
     * @param integrationSettings integration settings
     */
    @DefaultDataContext
    public CartesianShootingPropagationSettings(final List<ForceModel> forceModels,
                                                final List<CartesianAdjointEquationTerm> adjointEquationTerms,
                                                final CartesianCost cartesianCost,
                                                final ShootingIntegrationSettings integrationSettings) {
        this(forceModels, adjointEquationTerms, cartesianCost, FramesFactory.getGCRF(), integrationSettings,
            new FrameAlignedProvider(FramesFactory.getGCRF()));
    }

    /**
     * Constructor.
     * @param forceModels forces for numerical propagation
     * @param adjointEquationTerms contributions to the adjoint dynamics for Cartesian coordinates
     * @param cartesianCost cost function
     * @param propagationFrame frame used as reference frame in equations of motion by integrator
     * @param integrationSettings integration settings
     * @param attitudeProvider attitude provider
     */
    public CartesianShootingPropagationSettings(final List<ForceModel> forceModels,
                                                final List<CartesianAdjointEquationTerm> adjointEquationTerms,
                                                final CartesianCost cartesianCost,
                                                final Frame propagationFrame,
                                                final ShootingIntegrationSettings integrationSettings,
                                                final AttitudeProvider attitudeProvider) {
        this.forceModels = forceModels;
        this.adjointEquationTerms = adjointEquationTerms;
        this.cartesianCost = cartesianCost;
        this.propagationFrame = propagationFrame;
        this.integrationSettings = integrationSettings;
        this.attitudeProvider = attitudeProvider;
    }

    /**
     * Getter for the cost function.
     * @return cost
     */
    public CartesianCost getCartesianCost() {
        return cartesianCost;
    }

    /**
     * Getter for the adjoint terms.
     * @return adjoint terms
     */
    public List<CartesianAdjointEquationTerm> getAdjointEquationTerms() {
        return adjointEquationTerms;
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
