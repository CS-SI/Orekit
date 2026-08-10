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
import org.orekit.utils.ElevationMask;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;


/**
 * Class for masked elevation event function.
 * @author Romain Serra
 * @since 14.0
 */
public class MaskedElevationEventFunction extends AbstractElevationCrossingFunction {

    /** Elevation mask. */
    private final ElevationMask elevationMask;

    /** Constructor.
     * @param refractionModel reference to refraction model (can be null in which case no correction is applied)
     * @param topo reference to a topocentric model
     * @param elevationMask elevation mask defining function's root
     */
    public MaskedElevationEventFunction(final AtmosphericRefractionModel refractionModel,
                                        final TopocentricFrame topo, final ElevationMask elevationMask) {
        super(refractionModel, topo);
        this.elevationMask = elevationMask;
    }

    /**
     * Getter for the elevation mask.
     * @return mask
     */
    public ElevationMask getElevationMask() {
        return elevationMask;
    }

    @Override
    public double value(final SpacecraftState state) {
        final TrackingCoordinates trackingCoordinates = getTopocentricFrame().getTrackingCoordinates(state.getPosition(),
                state.getFrame(), state.getDate());
        final double correctedElevation = applyRefraction(trackingCoordinates.getElevation());
        return correctedElevation - elevationMask.getElevation(trackingCoordinates.getAzimuth());
    }

    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> fieldState) {
        final FieldTrackingCoordinates<T> trackingCoordinates = getTopocentricFrame().getTrackingCoordinates(fieldState.getPosition(),
                fieldState.getFrame(), fieldState.getDate());
        final T correctedElevation = applyRefraction(trackingCoordinates.getElevation());
        return correctedElevation.subtract(elevationMask.getElevation(trackingCoordinates.getAzimuth().getReal()));
    }
}
