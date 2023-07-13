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
import org.hipparchus.util.MathUtils;
import org.orekit.frames.StaticTransform;
import org.orekit.gnss.antenna.PhaseCenterVariationFunction;

/** Compute phase centers offset on an emitter-receiver link.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class PhaseCentersOffsetComputer {

    /** Mean position of the emitter Antenna Phase Center in emitter frame. */
    private final Vector3D emitterMeanPosition;

    /** Emitter phase center variation model in emitter frame (may be null). */
    private final PhaseCenterVariationFunction emitterPhaseCenterVariation;

    /** Mean position of the receiver Antenna Phase Center in receiver frame. */
    private final Vector3D receiverMeanPosition;

    /** Secondary phase center variation model in secondary frame (may be null). */
    private final PhaseCenterVariationFunction receiverPhaseCenterVariation;

    /** Simple constructor.
     * @param emitterMeanPosition mean position of the emitter Antenna Phase Center in emitter frame
     * @param emitterPhaseCenterVariation emitter phase center variation model in emitter frame (may be null for no variation)
     * @param receiverMeanPosition mean position of the receiver Antenna Phase Center in receiver frame
     * @param receiverPhaseCenterVariation receiver phase center variation model in receiver frame (may be null for no variation)
     */
    public PhaseCentersOffsetComputer(final Vector3D emitterMeanPosition,
                                      final PhaseCenterVariationFunction emitterPhaseCenterVariation,
                                      final Vector3D receiverMeanPosition,
                                      final PhaseCenterVariationFunction receiverPhaseCenterVariation) {
        this.emitterMeanPosition          = emitterMeanPosition;
        this.emitterPhaseCenterVariation  = emitterPhaseCenterVariation;
        this.receiverMeanPosition         = receiverMeanPosition;
        this.receiverPhaseCenterVariation = receiverPhaseCenterVariation;
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
        final Vector3D emitterMean    = emitterToInert.transformPosition(emitterMeanPosition);
        final Vector3D receiverMean   = receiverToInert.transformPosition(receiverMeanPosition);
        final Vector3D deltaMeans     = receiverMean.subtract(emitterMean);

        // compute the phase variation at emission
        final double emitterPCV = computePCV(deltaMeans, emitterPhaseCenterVariation, emitterToInert);

        // compute the phase variation at reception
        final double receiverPCV = computePCV(deltaMeans.negate(), receiverPhaseCenterVariation, receiverToInert);

        // compute the total offset resulting from both antennas phase centers
        return deltaMeans.getNorm() - deltaOrigins.getNorm() + emitterPCV + receiverPCV;

    }

    /** Compute PCV correction.
     * @param deltaMeans line of sight in inertial frame
     * @param pcvModel phase center variation model (may be null)
     * @param antennaToInert transform from antenna frame to inertial frame
     * @return Phase Center Variation correction
     */
    private double computePCV(final Vector3D deltaMeans, final PhaseCenterVariationFunction pcvModel,
                              final StaticTransform antennaToInert) {
        if (pcvModel == null) {
            return 0.0;
        } else {
            final Vector3D deltaAntenna = antennaToInert.getRotation().applyInverseTo(deltaMeans);
            return pcvModel.value(MathUtils.SEMI_PI - deltaAntenna.getDelta(), deltaAntenna.getAlpha());
        }
    }

}
