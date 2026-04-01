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
package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical range measurement with Shapiro time delay.
 * <p>
 * Shapiro time delay is a relativistic effect due to gravity.
 * </p>
 * @author Luc Maisonobe
 * @since 10.0
 * @param <T> type of the measurements
 */
public abstract class AbstractShapiroBaseModifier<T extends ObservedMeasurement<T>> implements EstimationModifier<T> {

    /** Shapiro delay computer. */
    private final ShapiroModel shapiroModel;

    /** Simple constructor from gravitational constant.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    protected AbstractShapiroBaseModifier(final double gm) {
        this.shapiroModel = new ShapiroModel(gm);
    }

    /** Simple constructor from Shapiro delay computer.
     * @param shapiroModel Shapiro delay computer
     * @since 14.0
     */
    protected AbstractShapiroBaseModifier(final ShapiroModel shapiroModel) {
        this.shapiroModel = shapiroModel;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Get the name of the effect modifying the measurement.
     * @return name of the effect modifying the measurement
     * @since 13.0
     */
    public String getEffectName() {
        return "Shapiro";
    }

    /** Compute Shapiro path dilation between two points in a gravity field.
     * @param positionEmitter position of emitter in body-centered frame
     * @param positionReceiver position of receiver in body-centered frame
     * @return path dilation to add to raw measurement
     */
    protected double shapiroCorrection(final Vector3D positionEmitter, final Vector3D positionReceiver) {
        return shapiroModel.computeEquivalentRange(positionEmitter, positionReceiver);
    }

}
