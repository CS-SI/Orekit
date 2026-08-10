/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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

import org.orekit.attitudes.AttitudeProvider;
import org.orekit.gnss.SatelliteSystem;

/** Factory for {@link OneWayGNSSWindUp wind-up} modifiers.
 * <p>
 * The factory ensures the same instance is returned for all
 * emitter/receiver pair, thus preserving phase continuity
 * for successive measurements involving the same pair.
 * </p>
 * @author Bryan Cazabonne
 * @since 14.0
 */
public class OneWayGNSSWindUpFactory {

    /** Modifiers cache. */
    private final Map<SatelliteSystem, Map<Integer, Map<String, OneWayGNSSWindUp>>> modifiers;

    /** Simple constructor.
     */
    public OneWayGNSSWindUpFactory() {
        this.modifiers = new HashMap<>();
    }

    /** Get a modifier for an emitter/receiver pair.
     * @param emitterSystem system the emitter satellite belongs to
     * @param emitterPrnNumber emitter satellite PRN number
     * @param emitterDipole emitter dipole
     * @param receiverName name of the receiver satellite
     * @param receiverDipole receiver dipole
     * @param emitterAttitude attitude provider for the GNSS emitter satellite
     * @return modifier for the emitter/receiver pair
     */
    public OneWayGNSSWindUp getWindUp(final SatelliteSystem emitterSystem, final int emitterPrnNumber,
                                      final Dipole emitterDipole,
                                      final String receiverName,
                                      final Dipole receiverDipole,
                                      final AttitudeProvider emitterAttitude) {

        // select emitter satellite system
        final Map<Integer, Map<String, OneWayGNSSWindUp>> systemModifiers =
                modifiers.computeIfAbsent(emitterSystem, s -> new HashMap<>());

        // select emitter satellite
        final Map<String, OneWayGNSSWindUp> satelliteModifiers =
                systemModifiers.computeIfAbsent(emitterPrnNumber, n -> new HashMap<>());

        // select receiver
        return satelliteModifiers.computeIfAbsent(receiverName, r -> new OneWayGNSSWindUp(emitterDipole,
                receiverDipole, emitterAttitude));

    }
}
