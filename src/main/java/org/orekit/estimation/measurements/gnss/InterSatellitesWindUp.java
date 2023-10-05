/* Copyright 2023 Thales Alenia Space
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
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Modifier for wind-up effect in GNSS {@link InterSatellitesPhase inter-satellites phase measurements}.
 * <p><span style="color:red">
 * WARNING: as of Orekit 12.0, the modeling of antenna for the receiver satellite is not
 * really standardized and should be used cautiously, mainly for LEO receivers that move
 * far below GNSS constellations. Inter-satellites wind-up is therefore still considered
 * experimental until this warning is removed.
 * </span></p>
 * @see InterSatellitesWindUpFactory
 * @author Luc Maisonobe
 * @since 12.0
 */
public class InterSatellitesWindUp implements EstimationModifier<InterSatellitesPhase> {

    /** Cached angular value of wind-up. */
    private double angularWindUp;

    /** Simple constructor.
     * <p>
     * The constructor is package protected to enforce use of {@link InterSatellitesWindUpFactory}
     * and preserve phase continuity for successive measurements involving the same
     * emitter/receiver pair.
     * </p>
     */
    InterSatellitesWindUp() {
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

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<InterSatellitesPhase> estimated) {

        // signal line of sight
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
        final Vector3D los = participants[1].getPosition().subtract(participants[0].getPosition()).normalize();

        // get receiver dipole
        // we don't use the basic yaw steering attitude model from ESA navipedia page
        // but rely on the attitude that was computed by the propagator, which takes
        // into account the proper noon and midnight turns for each satellite model
        final Rotation      receiverToInert = estimated.getStates()[0].toTransform().getRotation().revert();
        final Vector3D      iReceiver       = receiverToInert.applyTo(Vector3D.PLUS_I);
        final Vector3D      jReceiver       = receiverToInert.applyTo(Vector3D.PLUS_J);
        final Vector3D      dReceiver       = new Vector3D(1.0, iReceiver, -Vector3D.dotProduct(iReceiver, los), los).
                                              add(Vector3D.crossProduct(los, jReceiver));

        // get emitter dipole
        // we don't use the basic yaw steering attitude model from ESA navipedia page
        // but rely on the attitude that was computed by the propagator, which takes
        // into account the proper noon and midnight turns for each satellite model
        final Rotation      emitterToInert = estimated.getStates()[1].toTransform().getRotation().revert();
        final Vector3D      iEmitter       = emitterToInert.applyTo(Vector3D.PLUS_I);
        final Vector3D      jEmitter       = emitterToInert.applyTo(Vector3D.PLUS_J);
        final Vector3D      dEmitter       = new Vector3D(1.0, iEmitter, -Vector3D.dotProduct(iEmitter, los), los).
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
