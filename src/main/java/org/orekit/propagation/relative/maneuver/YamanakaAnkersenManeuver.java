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

package org.orekit.propagation.relative.maneuver;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;

/**
 * Implements a maneuver for a chaser spacecraft whose relative orbit is defined by the Yamanaka-Ankersen equations.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public class YamanakaAnkersenManeuver extends AbstractRelativeManeuver {
    /**
     * Creates a new YamanakaAnkersenManeuver object from a trigger event, a ΔV vector, and an associated frame.
     *
     * @param trigger                   Trigger event that triggers the maneuver of the chaser spacecraft.
     * @param deltaV                    ΔV vector in the target's LVLH CCSDS LOF.
     * @param chaserAdditionalEquations Chaser's additional equations provider
     */
    public YamanakaAnkersenManeuver(final EventDetector trigger, final Vector3D deltaV, final YamanakaAnkersenProvider chaserAdditionalEquations) {
        super(trigger, deltaV, chaserAdditionalEquations);
    }

    @Override
    public void resetTrueAnomalyAtManeuver(final Orbit orbit) {
        // Reset the TrueAnomaly of the spacecraft to the current true anomaly.
        final KeplerianOrbit target = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
        getRelativeProvider().setTargetTrueAnomaly(target.getTrueAnomaly());
    }
}
