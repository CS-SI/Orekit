/* Copyright 2022-2025 Luc Maisonobe
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

    /** Default elevation above which interval should be switched to fine interval (-5°).
     * @since 13.0
     */
    public static final double DEFAULT_ELEVATION_SWITCH_INF = FastMath.toRadians(-5.0);

    /** Default elevation below which interval should be switched to fine interval (+15°).
     * @since 13.0
     */
    public static final double DEFAULT_ELEVATION_SWITCH_SUP = FastMath.toRadians(15.0);

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
     * @param elevationSwitchInf elevation above which interval will switch to {@code fineCheckInterval}
     *                        (typically {@link #DEFAULT_ELEVATION_SWITCH_INF} which is -5°)
     * @param elevationSwitchSup elevation below which interval will switch to {@code fineCheckInterval}
     *                        (typically {@link #DEFAULT_ELEVATION_SWITCH_SUP} which is +15°)
     * @param fineCheckInterval check interval to use when elevation is
     *                          between {@code elevationSwitchInf} and {@code elevationSwitchSup}
     * @return adaptable interval for detection of elevation with respect to {@code topo}
     * @since 13.0
     */
    public static AdaptableInterval getAdaptableInterval(final TopocentricFrame topo,
                                                         final double elevationSwitchInf,
                                                         final double elevationSwitchSup,
                                                         final double fineCheckInterval) {
        return (state, isForward) -> {
            final double elevation = topo.getElevation(state.getPosition(), state.getFrame(), state.getDate());
            if (elevation <= elevationSwitchInf || elevation >= elevationSwitchSup) {
                // we are far from visibility switch, estimate some large interval with huge margins

                // rotation rate of the topocentric frame
                final Transform topoToInertial = topo.getTransformTo(state.getFrame(), state.getDate());
                final double topoAngularVelocity = topoToInertial.getAngular().getRotationRate().getNorm();

                // max angular rate of spacecraft (i.e. rate at perigee)
                final Orbit orbit = state.getOrbit();
                final double e     = orbit.getE();
                final double rp    = orbit.getA() * (1 - e);
                final double vp    = FastMath.sqrt(orbit.getMu() * (1 + e) / rp);
                final double rateP = vp / rp;

                // upper boundary of elevation rate
                final double maxElevationRate = topoAngularVelocity + rateP;

                // angular distance to the closest switch
                final double deltaElevationSwitch = elevation <= elevationSwitchInf ?
                                                    elevationSwitchInf - elevation :
                                                    elevation - elevationSwitchSup;

                return FastMath.max(fineCheckInterval, deltaElevationSwitch / maxElevationRate);

            } else {
                // we are close to visibility change, switch to fine check interval
                return fineCheckInterval;
            }
        };
    }

}
