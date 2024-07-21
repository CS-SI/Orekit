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
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

import java.util.stream.Stream;

/**
 * Abstract class for unbounded energy cost with Cartesian coordinates.
 * Unbounded qualifies the control vector, whose norm is not constrained. Under such assumption, there is no expected discontinuities in the adjoint dynamics.
 * @author Romain Serra
 * @see AbstractCartesianEnergy
 * @since 12.2
 */
public abstract class AbstractUnboundedCartesianEnergy extends AbstractCartesianEnergy {

    /**
     * Constructor.
     *
     * @param massFlowRateFactor mass flow rate factor
     */
    protected AbstractUnboundedCartesianEnergy(final double massFlowRateFactor) {
        super(massFlowRateFactor);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.empty();
    }
}
