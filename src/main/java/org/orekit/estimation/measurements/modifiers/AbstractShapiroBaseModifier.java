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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modifying theoretical range measurement with Shapiro time delay.
 * <p>
 * Shapiro time delay is a relativistic effect due to gravity.
 * </p>
 *
 * @author Luc Maisonobe
 * @since 10.0
 */
public class AbstractShapiroBaseModifier {

    /** Shapiro effect scale factor. */
    private final double s;

    /** Simple constructor.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public AbstractShapiroBaseModifier(final double gm) {
        this.s = 2 * gm / (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT);
    }

    /** Modify measurement.
     * @param estimated measurement to modify
     */
    protected void doModify(final EstimatedMeasurementBase<?> estimated) {

        // compute correction, for one way or two way measurements
        final TimeStampedPVCoordinates[] pv = estimated.getParticipants();
        final double correction = pv.length < 3 ?
                                  shapiroCorrection(pv[0], pv[1]) :
                                  0.5 * (shapiroCorrection(pv[0], pv[1]) + shapiroCorrection(pv[1], pv[2]));

        // update estimated value taking into account the Shapiro time delay.
        final double[] newValue = estimated.getEstimatedValue().clone();
        newValue[0] = newValue[0] + correction;
        estimated.setEstimatedValue(newValue);

    }

    /** Compute Shapiro path dilation between two points in a gravity field.
     * @param pvEmitter coordinates of emitter in body-centered frame
     * @param pvReceiver coordinates of receiver in body-centered frame
     * @return path dilation to add to raw measurement
     */
    protected double shapiroCorrection(final TimeStampedPVCoordinates pvEmitter, final TimeStampedPVCoordinates pvReceiver) {
        final Vector3D pEmitter  = pvEmitter.getPosition();
        final Vector3D pReceiver = pvReceiver.getPosition();
        final double   rEpR      = pEmitter.getNorm() + pReceiver.getNorm();
        final double   d         = Vector3D.distance(pEmitter, pReceiver);
        return s * FastMath.log((rEpR + d) / (rEpR - d));
    }

}
