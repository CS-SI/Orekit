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

import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AdditionalDataProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Interface for relative provider.
 * <p>
 * A relative provider is an interface extending AdditionalDataProvider to provide the relative state of a chaser S/C in
 * regard to a target S/C as an AdditionalData. The target S/C is propagated with the "main" propagator where the
 * additional data is added.
 * </p>
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public interface RelativeProvider extends AdditionalDataProvider<double[]> {

    /**
     * Get the initial TimeStampedPVCoordinates of the chaser in the Local Orbital Frame of the target.
     *
     * @return initial TimeStampedPVCoordinates of the chaser in target's LOF
     */
    TimeStampedPVCoordinates getInitialChaserPVTLof();

    /**
     * Set the initial TimeStampedPVCoordinates of the chaser in the Local Orbital Frame of the target.
     *
     * @param initialChaserPVTLof initial TimeStampedPVCoordinates of the chaser in target's LOF
     */
    void setInitialChaserPVTLof(TimeStampedPVCoordinates initialChaserPVTLof);

    /**
     * Extracts the chaser's PVT in the target LOF from the given target {@link SpacecraftState}.
     *
     * @param targetState target SpacecraftState
     * @return chaser's TimeStampedPVCoordinates in target's LOF
     */
    TimeStampedPVCoordinates extractChaserPVT(SpacecraftState targetState);

    /**
     * Extracts the chaser's PVT from the given target {@link SpacecraftState} and converts it to the desired output
     * frame.
     *
     * @param targetState target SpacecraftState
     * @param outputFrame desired frame in which to extract chaser's TimeStampedPVCoordinates
     * @return TimeStampedPVCoordinates in desired frame
     */
    TimeStampedPVCoordinates extractChaserPVT(SpacecraftState targetState, Frame outputFrame);

    /**
     * Get orbit of the target.
     *
     * @return orbit of the target
     */
    Orbit getTargetOrbit();

    /**
     * Set orbit of the target.
     *
     * @param targetOrbit orbit of the target
     */
    void setTargetOrbit(Orbit targetOrbit);

    /**
     * CW doesn't use true anomaly, so default is a no-op Set the true anomaly of the target.
     *
     * @param trueAnomaly true anomaly of the target
     */
    default void setTargetTrueAnomaly(final double trueAnomaly) {
    }
}
