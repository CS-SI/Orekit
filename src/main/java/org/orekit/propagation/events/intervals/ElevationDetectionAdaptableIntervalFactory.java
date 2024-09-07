/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.propagation.events.intervals;

import org.hipparchus.util.FastMath;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.events.AdaptableInterval;

/**
 * Factory class for {@link AdaptableInterval} suitable for elevation detection on eccentric orbits.
 * It requires {@link org.orekit.propagation.SpacecraftState} to be based on {@link Orbit} in order to work.
 * @see AdaptableInterval
 * @see org.orekit.propagation.events.ApsideDetector
 * @see org.orekit.propagation.events.EventSlopeFilter
 * @author Luc Maisonobe
 * @since 12.1
 */
public class ElevationDetectionAdaptableIntervalFactory {

    /** Default elevation abovde which interval should be switched to fine interval (-5°). */
    public static final double DEFAULT_ELEVATION_SWITCH = FastMath.toRadians(-5.0);

    /**
     * Private constructor.
     */
    private ElevationDetectionAdaptableIntervalFactory() {
        // factory class
    }

    /**
     * Method providing a candidate {@link AdaptableInterval} for arbitrary elevation detection with forward propagation.
     * It uses a Keplerian, eccentric approximation.
     * @param topo topocentric frame centered at ground interest point
     * @param elevationSwitch elevation above which interval will switch to {@code fineCheckInterval}
     *                        (typically {@link #DEFAULT_ELEVATION_SWITCH} which is -5°)
     * @param fineCheckInterval check interval to use when elevation is above {@code elevationSwitch}
     * @return adaptable interval for detection of elevation with respect to {@code topo}
     */
    public static AdaptableInterval getAdaptableInterval(final TopocentricFrame topo,
                                                         final double elevationSwitch,
                                                         final double fineCheckInterval) {
        return state -> {
            final double elevation = topo.getElevation(state.getPosition(), state.getFrame(), state.getDate());
            if (elevation <= elevationSwitch) {
                // we are far from visibility, estimate some large interval with huge margins

                // rotation rate of the topocentric frame
                final Transform topoToInertial = topo.getTransformTo(state.getFrame(), state.getDate());
                final double topoAngularVelocity = topoToInertial.getAngular().getRotationRate().getNorm();

                // max angular rate of spacecraft (i.e. rate at perigee)
                final double e     = state.getE();
                final double rp    = state.getA() * (1 - e);
                final double vp    = FastMath.sqrt(state.getMu() * (1 + e) / rp);
                final double rateP = vp / rp;

                // upper boundary of elevation rate
                final double maxElevationRate = topoAngularVelocity + rateP;

                return FastMath.max(fineCheckInterval, (elevationSwitch - elevation) / maxElevationRate);

            } else {
                // we are close to visibility, switch to fine check interval
                return fineCheckInterval;
            }
        };
    }

}
