/* Copyright 2023 Thales Alenia Space
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
package org.orekit.estimation.measurements.gnss;

import java.util.HashMap;
import java.util.Map;

import org.orekit.gnss.SatelliteSystem;

/** Factory for {@link InterSatellitesWindUp wind-up} modifiers.
 * <p>
 * The factory ensures the same instance is returned for all
 * emitter/receiver pair, thus preserving phase continuity
 * for successive measurements involving the same pair.
 * </p>
 * @author Luc Maisonobe
 * @since 12.0
 */
public class InterSatellitesWindUpFactory {

    /** Modifiers cache. */
    private final Map<SatelliteSystem, Map<Integer, Map<SatelliteSystem, Map<Integer, InterSatellitesWindUp>>>> modifiers;

    /** Simple constructor.
     */
    public InterSatellitesWindUpFactory() {
        this.modifiers = new HashMap<>();
    }

    /** Get a modifier for an emitter/receiver pair.
     * @param emitterSystem system the emitter satellite belongs to
     * @param emitterPrnNumber emitter satellite PRN number
     * @param emitterDipole emitter dipole
     * @param receiverSystem system the receiver satellite belongs to
     * @param receiverPrnNumber receiver satellite PRN number
     * @param receiverDipole receiver dipole
     * @return modifier for the emitter/receiver pair
     */
    public InterSatellitesWindUp getWindUp(final SatelliteSystem emitterSystem,  final int emitterPrnNumber,
                                           final Dipole emitterDipole,
                                           final SatelliteSystem receiverSystem, final int receiverPrnNumber,
                                           final Dipole receiverDipole) {

        // select emitter satellite system
        Map<Integer, Map<SatelliteSystem, Map<Integer, InterSatellitesWindUp>>> emitterSystemModifiers =
                        modifiers.get(emitterSystem);
        if (emitterSystemModifiers == null) {
            // build a new map for this satellite system
            emitterSystemModifiers = new HashMap<>();
            modifiers.put(emitterSystem, emitterSystemModifiers);
        }

        // select emitter satellite
        Map<SatelliteSystem, Map<Integer, InterSatellitesWindUp>> emitterSatelliteModifiers =
                        emitterSystemModifiers.get(emitterPrnNumber);
        if (emitterSatelliteModifiers == null) {
            // build a new map for this satellite
            emitterSatelliteModifiers = new HashMap<>();
            emitterSystemModifiers.put(emitterPrnNumber, emitterSatelliteModifiers);
        }

        // select receiver satellite system
        Map<Integer, InterSatellitesWindUp> receiverSystemModifiers =
                        emitterSatelliteModifiers.get(receiverSystem);
        if (receiverSystemModifiers == null) {
            // build a new map for this satellite system
            receiverSystemModifiers = new HashMap<>();
            emitterSatelliteModifiers.put(receiverSystem, receiverSystemModifiers);
        }

        // select receiver satellite
        InterSatellitesWindUp receiverSatelliteModifier = receiverSystemModifiers.get(receiverPrnNumber);
        if (receiverSatelliteModifier == null) {
            // build a new wind-up modifier
            receiverSatelliteModifier = new InterSatellitesWindUp(emitterDipole, receiverDipole);
            receiverSystemModifiers.put(receiverPrnNumber, receiverSatelliteModifier);
        }

        return receiverSatelliteModifier;

    }

}
