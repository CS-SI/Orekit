/* Copyright 2002-2023 CS GROUP
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

/** Factory for {@link WindUp wind-up} modifiers.
 * <p>
 * The factory ensures the same instance is returned for all
 * satellite/receiver pair, thus preserving phase continuity
 * for successive measurements involving the same pair.
 * </p>
 * @author Luc Maisonobe
 * @since 10.1
 */
public class WindUpFactory {

    /** Modifiers cache. */
    private final Map<SatelliteSystem, Map<Integer, Map<String, WindUp>>> modifiers;

    /** Simple constructor.
     */
    public WindUpFactory() {
        this.modifiers = new HashMap<>();
    }

    /** Get a modifier for a satellite/receiver pair.
     * @param system system the satellite belongs to
     * @param prnNumber PRN number
     * @param emitterDipole emitter dipole
     * @param receiverName name of the receiver
     * @return modifier for the satellite/receiver pair
     */
    public WindUp getWindUp(final SatelliteSystem system, final int prnNumber,
                            final Dipole emitterDipole, final String receiverName) {
        // select satellite system
        Map<Integer, Map<String, WindUp>> systemModifiers = modifiers.get(system);
        if (systemModifiers == null) {
            // build a new map for this satellite system
            systemModifiers = new HashMap<>();
            modifiers.put(system, systemModifiers);
        }

        // select satellite
        Map<String, WindUp> satelliteModifiers = systemModifiers.get(prnNumber);
        if (satelliteModifiers == null) {
            // build a new map for this satellite
            satelliteModifiers = new HashMap<>();
            systemModifiers.put(prnNumber, satelliteModifiers);
        }

        // select receiver
        WindUp receiverModifier = satelliteModifiers.get(receiverName);
        if (receiverModifier == null) {
            // build a new wind-up modifier
            receiverModifier = new WindUp(emitterDipole);
            satelliteModifiers.put(receiverName, receiverModifier);
        }

        return receiverModifier;

    }

}
