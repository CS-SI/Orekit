/* Copyright 2022-2026 Romain Serra
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


import java.util.function.Function;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.propagation.events.functions.EventFunctionModifier;
import org.orekit.propagation.events.handlers.FieldResetDerivativesOnEvent;

/**
 * Abstract class for cost with Cartesian coordinates.
 *
 * @param <T> field type
 * @author Romain Serra
 * @see CartesianCost
 * @since 13.0
 */
public abstract class FieldAbstractCartesianCost<T extends CalculusFieldElement<T>> implements FieldCartesianCost<T> {

    /** Name of adjoint vector. */
    private final String name;

    /** Mass flow rate factor (always positive). */
    private final T massFlowRateFactor;

    /** Dimension of adjoint vector. */
    private final int adjointDimension;

    /**
     * Constructor.
     * @param name name
     * @param massFlowRateFactor mass flow rate factor
     */
    protected FieldAbstractCartesianCost(final String name, final T massFlowRateFactor) {
        this.name = name;
        this.massFlowRateFactor = FastMath.abs(massFlowRateFactor);
        this.adjointDimension = this.massFlowRateFactor.isZero() ? 6 : 7;
    }

    /** {@inheritDoc} */
    @Override
    public int getAdjointDimension() {
        return adjointDimension;
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
    public T getMassFlowRateFactor() {
        return massFlowRateFactor;
    }

    /**
     * Computes the Euclidean norm of the adjoint velocity vector.
     * @param adjointVariables adjoint vector
     * @return norm of adjoint velocity
     */
    protected T getFieldAdjointVelocityNorm(final T[] adjointVariables) {
        return FastMath.sqrt(adjointVariables[3].square().add(adjointVariables[4].square()).add(adjointVariables[5].square()));
    }

    /**
     * Build event detector.
     * @param eventFunction event function
     * @param fieldEventDetectionSettings detection settings
     * @return event detector
     * @since 14.0
     */
    protected FieldEventDetector<T> buildSwitchDetector(final FieldSwitchFunction eventFunction,
                                                        final FieldEventDetectionSettings<T> fieldEventDetectionSettings) {
        return FieldEventDetector.of(eventFunction, new FieldResetDerivativesOnEvent<>(), fieldEventDetectionSettings);
    }

    /**
     * Class for control switch function involving Field.
     */
    public class FieldSwitchFunction implements EventFunctionModifier {

        /** Wrapped event function. */
        private final EventFunction baseFunction;

        protected FieldSwitchFunction(final Function<FieldSpacecraftState<T>, T> fieldFunction) {
            this.baseFunction = EventFunction.of(getMassFlowRateFactor().getField(), fieldFunction);
        }

        @Override
        public EventFunction getBaseFunction() {
            return baseFunction;
        }

        @Override
        public boolean dependsOnMainVariablesOnly() {
            return false;
        }
    }
}
