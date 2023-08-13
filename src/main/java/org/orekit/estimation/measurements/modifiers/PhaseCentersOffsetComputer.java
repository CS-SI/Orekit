/* Copyright 2023 Luc Maisonobe
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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.StaticTransform;
import org.orekit.gnss.antenna.FrequencyPattern;

/** Compute phase centers offset on an emitter-receiver link.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class PhaseCentersOffsetComputer {

    /** Emitter pattern. */
    private final FrequencyPattern emitterPattern;

    /** Receiver pattern. */
    private final FrequencyPattern receiverPattern;

    /** Simple constructor.
     * @param emitterPattern emitter pattern
     * @param receiverPattern receiver pattern
     */
    public PhaseCentersOffsetComputer(final FrequencyPattern emitterPattern,
                                      final FrequencyPattern receiverPattern) {
        this.emitterPattern  = emitterPattern;
        this.receiverPattern = receiverPattern;
    }

    /** Compute distance offset to be added to the distance between antennas reference points.
     * @param emitterToInert transform from emitter to inertial frame at emission date
     * @param receiverToInert transform from receiver to inertial frame at reception date
     * @return offset to be added to distance between origins, in order to get distance between phase centers
     */
    public double offset(final StaticTransform emitterToInert, final StaticTransform receiverToInert) {

        // compute the relative positions of frames origins
        final Vector3D emitterOrigin  = emitterToInert.transformPosition(Vector3D.ZERO);
        final Vector3D receiverOrigin = receiverToInert.transformPosition(Vector3D.ZERO);
        final Vector3D deltaOrigins   = receiverOrigin.subtract(emitterOrigin);

        // compute the relative positions of mean phase centers
        final Vector3D emitterMean    = emitterToInert.transformPosition(emitterPattern.getEccentricities());
        final Vector3D receiverMean   = receiverToInert.transformPosition(receiverPattern.getEccentricities());
        final Vector3D deltaMeans     = receiverMean.subtract(emitterMean);

        // compute the phase variation at emission
        final Vector3D emitterLos = emitterToInert.getRotation().applyInverseTo(deltaMeans);
        final double   emitterPCV = emitterPattern.getPhaseCenterVariation(emitterLos);

        // compute the phase variation at reception
        final Vector3D receiverLos = receiverToInert.getRotation().applyInverseTo(deltaMeans.negate());
        final double   receiverPCV = receiverPattern.getPhaseCenterVariation(receiverLos);

        // compute the total offset resulting from both antennas phase centers
        return deltaMeans.getNorm() - deltaOrigins.getNorm() + emitterPCV + receiverPCV;

    }

}
