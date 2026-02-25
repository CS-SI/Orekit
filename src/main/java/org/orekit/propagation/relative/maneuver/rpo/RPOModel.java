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
package org.orekit.propagation.relative.maneuver.rpo;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.propagation.relative.maneuver.ClohessyWiltshireManeuver;
import org.orekit.propagation.relative.maneuver.YamanakaAnkersenManeuver;
import org.orekit.propagation.relative.maneuver.rpo.TeardropCircularWaypointCalculator;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

public enum RPOModel implements RPO {
    /**
     * CW: Clohessy-Wiltshire.
     */
    CW {
        /** {@inheritDoc} */
        public Vector3D getRBarDirection() {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */

        public Vector3D getVBarDirection() {
            return Vector3D.PLUS_J;
        }

        /** {@inheritDoc} */
        public Vector3D getOutOfPlaneDirection() {
            return Vector3D.PLUS_K;
        }

        /**
         *
         * @param injectionDate
         * @param targetMeanMotion
         * @param turnAroundDistance
         * @param maneuverDistance
         * @param numberOfOrbits
         * @return
         */
        public List<TimeStampedPVCoordinates> computeTeardropWaypoints(final AbsoluteDate injectionDate, final double targetMeanMotion, final double turnAroundDistance, final double maneuverDistance, final int numberOfOrbits) {
            return new TeardropCircularWaypointCalculator(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits).computeTearDropWaypoints(injectionDate);
        }

        public List<ClohessyWiltshireManeuver> computeManeuvers(final List<TimeStampedPVCoordinates> waypoints, final ClohessyWiltshireProvider cwProvider) {

        }

        public List<ImpulseManeuver> convertToImpulseManeuver(final List<ClohessyWiltshireManeuver> cwManeuvers, final Frame outputFrame, final double isp) {

        }

        public List<ClohessyWiltshireManeuver> computeTeardropManeuvers(final List<TimeStampedPVCoordinates> waypoints, final ClohessyWiltshireProvider cwProvider) {

        }
    },
    /**
     * YA : Yamanaka-Ankersen.
     */
    YA {
        /** {@inheritDoc} */
        public Vector3D getRBarDirection() {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        public Vector3D getVBarDirection() {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */
        public Vector3D getOutOfPlaneDirection() {
            return Vector3D.MINUS_J;
        }

        public List<YamanakaAnkersenManeuver> computeManeuvers(final List<TimeStampedPVCoordinates> waypoints, final YamanakaAnkersenProvider yaProvider) {

        }

        public List<ImpulseManeuver> convertToImpulseManeuver(final List<YamanakaAnkersenManeuver> yaManeuvers, final Frame outputFrame, final double isp) {

        }
    }
}
