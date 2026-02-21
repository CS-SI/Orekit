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
package org.orekit.propagation.events.functions;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;


/**
 * Class for single-value crossing elevation event.
 * @author Romain Serra
 * @since 14.0
 */
public class ElevationValueCrossingFunction extends AbstractElevationCrossingFunction {

    /** Critical elevation. */
    private final double criticalElevation;

    /** Constructor.
     * @param refractionModel reference to refraction model (can be null in which case no correction is applied)
     * @param topo reference to a topocentric model
     * @param criticalElevation elevation defining function's root
     */
    public ElevationValueCrossingFunction(final AtmosphericRefractionModel refractionModel,
                                          final TopocentricFrame topo, final double criticalElevation) {
        super(refractionModel, topo);
        this.criticalElevation = criticalElevation;
    }

    /**
     * Getter for the minimum elevation.
     * @return min. elevation
     */
    public double getCriticalElevation() {
        return criticalElevation;
    }

    @Override
    public double value(final SpacecraftState state) {
        final double elevation = getElevation(state.getPosition(getTopocentricFrame()), state.getDate());
        final double correctedElevation = applyRefraction(elevation);
        return correctedElevation - criticalElevation;
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        final T elevation = getElevation(fieldState.getPosition(getTopocentricFrame()), fieldState.getDate());
        final T correctedElevation = applyRefraction(elevation);
        return correctedElevation.subtract(criticalElevation);
    }
}
