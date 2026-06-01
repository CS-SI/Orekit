/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldAdditionalDataProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * Interface for Field Relative provider.
 * <p>
 * A field relative provider is an interface extending FieldAdditionalDataProvider to provide the relative state of a
 * chaser S/C in regard to a target S/C as a FieldAdditionalData. The target S/C is propagated with the "main"
 * propagator where the additional data is added.
 * </p>
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */
public interface FieldRelativeProvider<T extends CalculusFieldElement<T>> extends FieldAdditionalDataProvider<T[], T> {

    /**
     * Get the initial TimeStampedPVCoordinates of the chaser in the Local Orbital Frame of the target.
     *
     * @return initial TimeStampedPVCoordinates of the chaser in target's LOF
     */
    TimeStampedFieldPVCoordinates<T> getInitialChaserPVTLof();

    /**
     * Set the initial TimeStampedPVCoordinates of the chaser in the Local Orbital Frame of the target.
     *
     * @param initialChaserPVTLof initial TimeStampedPVCoordinates of the chaser in target's LOF
     */
    void setInitialChaserPVTLof(TimeStampedFieldPVCoordinates<T> initialChaserPVTLof);

    /**
     * Extracts the chaser's PVT in the target LOF from the given target {@link SpacecraftState}.
     *
     * @param targetState target SpacecraftState
     * @return chaser's TimeStampedPVCoordinates in target's LOF
     */
    TimeStampedFieldPVCoordinates<T> extractChaserPVT(FieldSpacecraftState<T> targetState);

    /**
     * Extracts the chaser's PVT from the given target {@link SpacecraftState} and converts it to the desired output
     * frame.
     *
     * @param targetState target SpacecraftState
     * @param outputFrame desired frame in which to extract chaser's TimeStampedPVCoordinates
     * @return TimeStampedPVCoordinates in desired frame
     */
    TimeStampedFieldPVCoordinates<T> extractChaserPVT(FieldSpacecraftState<T> targetState, Frame outputFrame);

    /**
     * Get the target Orbit.
     *
     * @return orbit of the target
     */
    FieldOrbit<T> getTargetOrbit();

    /**
     * Set the target orbit.
     *
     * @param targetOrbit orbit of the target
     */
    void setTargetOrbit(FieldOrbit<T> targetOrbit);

    /**
     * CW doesn't use true anomaly, so default is a no-op Set the true anomaly of the target.
     *
     * @param trueAnomaly true anomaly of the target
     */
    default void setTargetTrueAnomaly(final T trueAnomaly) {
    }
}
