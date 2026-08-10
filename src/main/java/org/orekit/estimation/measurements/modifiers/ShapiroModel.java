/* Copyright 2022-2026 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;

/** Class computing Shapiro time delay.
 * <p>
 * Shapiro time delay is a relativistic effect due to gravity.
 * </p>
 * @author Romain Serra
 * @since 14.0
 */
public class ShapiroModel {

    /** Shapiro delay scale factor. */
    private final double schwarzschildRadius;

    /** Simple constructor.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public ShapiroModel(final double gm) {
        this.schwarzschildRadius   = 2. * gm / (Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT);
    }

    /** Compute Shapiro path range effect between two points in a gravity field.
     * @param positionEmitter position of emitter in body-centered frame
     * @param positionReceiver position of receiver in body-centered frame
     * @return extra fictitious distance (m)
     */
    public double computeEquivalentRange(final Vector3D positionEmitter, final Vector3D positionReceiver) {
        final double   rEpR      = positionEmitter.getNorm() + positionReceiver.getNorm();
        final double   d         = Vector3D.distance(positionEmitter, positionReceiver);
        return schwarzschildRadius * FastMath.log((rEpR + d) / (rEpR - d));
    }

    /** Compute Shapiro path delay between two points in a gravity field.
     * @param positionEmitter position of emitter in body-centered frame
     * @param positionReceiver position of receiver in body-centered frame
     * @return time delay (s)
     */
    public double computeDelay(final Vector3D positionEmitter, final Vector3D positionReceiver) {
        return computeEquivalentRange(positionEmitter, positionReceiver) / Constants.SPEED_OF_LIGHT;
    }

    /** Compute Shapiro path range effect between two points in a gravity field.
     * @param <T> type of the field elements
     * @param positionEmitter position of emitter in body-centered frame
     * @param positionReceiver position of receiver in body-centered frame
     * @return extra fictitious distance (m)
     */
    public <T extends CalculusFieldElement<T>> T computeEquivalentRange(final FieldVector3D<T> positionEmitter,
                                                                        final FieldVector3D<T> positionReceiver) {
        final T   rEpR      = positionEmitter.getNorm().add(positionReceiver.getNorm());
        final T   d         = FieldVector3D.distance(positionEmitter, positionReceiver);
        return FastMath.log((rEpR.add(d)).divide(rEpR.subtract(d))).multiply(schwarzschildRadius);
    }

    /** Compute Shapiro path delay between two points in a gravity field.
     * @param <T> type of the field elements
     * @param positionEmitter position of emitter in body-centered frame
     * @param positionReceiver position of receiver in body-centered frame
     * @return time delay (s)
     */
    public <T extends CalculusFieldElement<T>> T computeDelay(final FieldVector3D<T> positionEmitter,
                                                              final FieldVector3D<T> positionReceiver) {
        return computeEquivalentRange(positionEmitter, positionReceiver).divide(Constants.SPEED_OF_LIGHT);
    }
}
