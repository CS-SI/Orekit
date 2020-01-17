/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Modifier for wind-up effect in GNSS {@link Phase phase measurements}.
 * @see <a href="https://gssc.esa.int/navipedia/index.php/Carrier_Phase_Wind-up_Effect">Carrier Phase Wind-up Effect</a>
 * @see WindUpFactory
 * @author Luc Maisonobe
 * @since 10.1
 */
public class WindUp implements EstimationModifier<Phase> {

    /** Cached angular value of wind-up. */
    private double angularWindUp;

    /** Simple constructor.
     * <p>
     * The constructor is package protected to enforce use of {@link WindUpFactory}
     * and preserve phase continuity for successive measurements involving the same
     * satellite/receiver pair.
     * </p>
     */
    WindUp() {
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
    public void modify(final EstimatedMeasurement<Phase> estimated) {

        // signal line of sight
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
        final Vector3D los = participants[1].getPosition().subtract(participants[0].getPosition()).normalize();

        // get ground antenna dipole
        final Frame         inertial      = estimated.getStates()[0].getFrame();
        final GroundStation station       = estimated.getObservedMeasurement().getStation();
        final Rotation      offsetToInert = station.getOffsetToInertial(inertial, estimated.getDate()).getRotation();
        final Vector3D      iGround       = offsetToInert.applyTo(Vector3D.PLUS_I);
        final Vector3D      jGround       = offsetToInert.applyTo(Vector3D.PLUS_J);
        final Vector3D      dGround       = new Vector3D(1.0, iGround, -Vector3D.dotProduct(iGround, los), los).
                                            add(Vector3D.crossProduct(los, jGround));

        // get satellite dipole
        // we don't use the basic yaw steering attitude model from ESA navipedia page
        // but rely on the attitude that was computed by the propagator, which takes
        // into account the proper noon and midnight turns for each satellite model
        final Rotation      satToInert    = estimated.getStates()[0].toTransform().getRotation().revert();
        final Vector3D      iSat          = satToInert.applyTo(Vector3D.PLUS_I);
        final Vector3D      jSat          = satToInert.applyTo(Vector3D.PLUS_J);
        final Vector3D      dSat          = new Vector3D(1.0, iSat, -Vector3D.dotProduct(iSat, los), los).
                                            subtract(Vector3D.crossProduct(los, jSat));

        // raw correction
        final double correction = FastMath.copySign(Vector3D.angle(dSat, dGround),
                                                    Vector3D.dotProduct(los, Vector3D.crossProduct(dSat, dGround)));

        // ensure continuity accross measurements
        // we assume the various measurements are close enough in time
        // (less the one satellite half-turn) so the angles remain close
        angularWindUp = MathUtils.normalizeAngle(correction, angularWindUp);

        // update estimate
        estimated.setEstimatedValue(estimated.getEstimatedValue()[0] + angularWindUp / MathUtils.TWO_PI);

    }

}
