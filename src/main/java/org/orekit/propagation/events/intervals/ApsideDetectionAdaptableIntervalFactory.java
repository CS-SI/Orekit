/* Copyright 2022-2024 Romain Serra
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
import org.hipparchus.util.MathUtils;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.events.AdaptableInterval;

/**
 * Factory class for {@link AdaptableInterval} suitable for apside detection on eccentric orbits.
 * It requires {@link org.orekit.propagation.SpacecraftState} to be based on {@link Orbit} in order to work.
 * @see org.orekit.propagation.events.AdaptableInterval
 * @see org.orekit.propagation.events.ApsideDetector
 * @see org.orekit.propagation.events.EventSlopeFilter
 * @author Romain Serra
 * @since 12.1
 */
public class ApsideDetectionAdaptableIntervalFactory {

    /**
     * Private constructor.
     */
    private ApsideDetectionAdaptableIntervalFactory() {
        // factory class
    }

    /**
     * Method providing a candidate {@link AdaptableInterval} for arbitrary apside detection.
     * It uses a Keplerian, eccentric approximation.
     * @return adaptable interval for apside detection
     */
    public static AdaptableInterval getApsideDetectionAdaptableInterval() {
        return (state, isForward) -> {
            final Orbit orbit = state.getOrbit();
            final KeplerianOrbit keplerianOrbit = convertOrbitIntoKeplerianOne(orbit);
            final double meanMotion = keplerianOrbit.getKeplerianMeanMotion();
            final double meanAnomaly = keplerianOrbit.getMeanAnomaly();
            if (isForward) {
                final double durationToNextPeriapsis = computeKeplerianDurationToNextPeriapsis(meanAnomaly, meanMotion);
                final double durationToNextApoapsis = computeKeplerianDurationToNextApoapsis(meanAnomaly, meanMotion);
                return FastMath.min(durationToNextPeriapsis, durationToNextApoapsis);
            }
            else {
                final double durationFromPreviousPeriapsis = computeKeplerianDurationFromPreviousPeriapsis(meanAnomaly,
                        meanMotion);
                final double durationFromPreviousApoapsis = computeKeplerianDurationFromPreviousApoapsis(meanAnomaly,
                        meanMotion);
                return FastMath.min(durationFromPreviousApoapsis, durationFromPreviousPeriapsis);
            }
        };
    }

    /**
     * Method providing a candidate {@link AdaptableInterval} for periapsis detection.
     * It uses a Keplerian, eccentric approximation.
     * @return adaptable interval for periaspsis detection
     */
    public static AdaptableInterval getPeriapsisDetectionAdaptableInterval() {
        return (state, isForward) -> {
            final Orbit orbit = state.getOrbit();
            final KeplerianOrbit keplerianOrbit = convertOrbitIntoKeplerianOne(orbit);
            final double meanMotion = keplerianOrbit.getKeplerianMeanMotion();
            final double meanAnomaly = keplerianOrbit.getMeanAnomaly();
            if (isForward) {
                return computeKeplerianDurationToNextPeriapsis(meanAnomaly, meanMotion);
            } else {
                return computeKeplerianDurationFromPreviousPeriapsis(meanAnomaly, meanMotion);
            }
        };
    }

    /**
     * Method providing a candidate {@link AdaptableInterval} for apoapsis detection.
     * It uses a Keplerian, eccentric approximation.
     * @return adaptable interval for apoapsis detection
     */
    public static AdaptableInterval getApoapsisDetectionAdaptableInterval() {
        return (state, isForward) -> {
            final Orbit orbit = state.getOrbit();
            final KeplerianOrbit keplerianOrbit = convertOrbitIntoKeplerianOne(orbit);
            final double meanMotion = keplerianOrbit.getKeplerianMeanMotion();
            final double meanAnomaly = keplerianOrbit.getMeanAnomaly();
            if (isForward) {
                return computeKeplerianDurationToNextApoapsis(meanAnomaly, meanMotion);
            }
            else {
                return computeKeplerianDurationFromPreviousApoapsis(meanAnomaly, meanMotion);
            }
        };
    }

    /**
     * Convert a generic {@link Orbit} into a {@link KeplerianOrbit}.
     * @param orbit orbit to convert
     * @return Keplerian orbit
     */
    private static KeplerianOrbit convertOrbitIntoKeplerianOne(final Orbit orbit) {
        return (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
    }

    /**
     * Method computing time to go until next periapsis, assuming Keplerian motion.
     * @param meanAnomaly mean anomaly
     * @param meanMotion Keplerian mean motion
     * @return duration to next periapsis
     */
    private static double computeKeplerianDurationToNextPeriapsis(final double meanAnomaly,
                                                                  final double meanMotion) {
        final double normalizedMeanAnomaly = MathUtils.normalizeAngle(meanAnomaly, FastMath.PI);
        return (MathUtils.TWO_PI - normalizedMeanAnomaly) / meanMotion;
    }

    /**
     * Method computing time elapsed since last periapsis, assuming Keplerian motion.
     * @param meanAnomaly mean anomaly
     * @param meanMotion Keplerian mean motion
     * @return duration elapsed since last periapsis
     */
    public static double computeKeplerianDurationFromPreviousPeriapsis(final double meanAnomaly,
                                                                       final double meanMotion) {
        final double normalizedMeanAnomaly = MathUtils.normalizeAngle(meanAnomaly, FastMath.PI);
        return normalizedMeanAnomaly / meanMotion;
    }

    /**
     * Method computing time to go until next apoapsis, assuming Keplerian motion.
     * @param meanAnomaly mean anomaly
     * @param meanMotion Keplerian mean motion
     * @return duration to next apoapsis
     */
    private static double computeKeplerianDurationToNextApoapsis(final double meanAnomaly,
                                                                 final double meanMotion) {
        final double normalizedMeanAnomaly = MathUtils.normalizeAngle(meanAnomaly, MathUtils.TWO_PI);
        return (MathUtils.TWO_PI + FastMath.PI - normalizedMeanAnomaly) / meanMotion;
    }

    /**
     * Method computing time elapsed since last apoapsis, assuming Keplerian motion.
     * @param meanAnomaly mean anomaly
     * @param meanMotion Keplerian mean motion
     * @return duration elapsed since last apoapsis
     */
    public static double computeKeplerianDurationFromPreviousApoapsis(final double meanAnomaly,
                                                                      final double meanMotion) {
        final double normalizedMeanAnomaly = MathUtils.normalizeAngle(meanAnomaly, MathUtils.TWO_PI);
        return (normalizedMeanAnomaly - FastMath.PI) / meanMotion;
    }
}
