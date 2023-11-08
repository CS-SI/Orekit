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
package org.orekit.estimation.measurements.gnss;

import java.util.Collections;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Base class for wind-up effect computation.
 * @see <a href="https://gssc.esa.int/navipedia/index.php/Carrier_Phase_Wind-up_Effect">Carrier Phase Wind-up Effect</a>
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 12.0
 */
public abstract class AbstractWindUp<T extends ObservedMeasurement<T>> implements EstimationModifier<T> {

    /** Emitter dipole. */
    private Dipole emitter;

    /** Receiver dipole. */
    private Dipole receiver;

    /** Cached angular value of wind-up. */
    private double angularWindUp;

    /** Simple constructor.
     * @param emitter emitter dipole
     * @param receiver receiver dipole
     */
    protected AbstractWindUp(final Dipole emitter, final Dipole receiver) {
        this.emitter  = emitter;
        this.receiver = receiver;
        angularWindUp = 0.0;
    }

    /** {@inheritDoc}
     * <p>
     * Wind-up effect has no parameters, the returned list is always empty.
     * </p>
     */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Compute rotation from emitter to inertial frame.
     * @param estimated estimated measurement to modify
     * @return rotation from emitter to inertial frame
     */
    protected abstract Rotation emitterToInert(EstimatedMeasurementBase<T> estimated);

    /** Compute rotation from receiver to inertial frame.
     * @param estimated estimated measurement to modify
     * @return rotation from receiver to inertial frame
     */
    protected abstract Rotation receiverToInert(EstimatedMeasurementBase<T> estimated);

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated) {

        // signal line of sight
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
        final Vector3D los = participants[1].getPosition().subtract(participants[0].getPosition()).normalize();

        // get receiver dipole
        final Rotation receiverToInert = receiverToInert(estimated);
        final Vector3D iReceiver       = receiverToInert.applyTo(receiver.getPrimary());
        final Vector3D jReceiver       = receiverToInert.applyTo(receiver.getSecondary());
        final Vector3D dReceiver       = new Vector3D(1.0, iReceiver, -Vector3D.dotProduct(iReceiver, los), los).
                                         add(Vector3D.crossProduct(los, jReceiver));

        // get emitter dipole
        final Rotation emitterToInert = emitterToInert(estimated);
        final Vector3D iEmitter       = emitterToInert.applyTo(emitter.getPrimary());
        final Vector3D jEmitter       = emitterToInert.applyTo(emitter.getSecondary());
        final Vector3D dEmitter       = new Vector3D(1.0, iEmitter, -Vector3D.dotProduct(iEmitter, los), los).
                                        subtract(Vector3D.crossProduct(los, jEmitter));

        // raw correction
        final double correction = FastMath.copySign(Vector3D.angle(dEmitter, dReceiver),
                                                    Vector3D.dotProduct(los, Vector3D.crossProduct(dEmitter, dReceiver)));

        // ensure continuity accross measurements
        // we assume the various measurements are close enough in time
        // (less the one satellite half-turn) so the angles remain close
        angularWindUp = MathUtils.normalizeAngle(correction, angularWindUp);

        // update estimate
        estimated.setEstimatedValue(estimated.getEstimatedValue()[0] + angularWindUp / MathUtils.TWO_PI);

    }

}
