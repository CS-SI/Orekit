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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Modifier for wind-up effect in GNSS {@link Phase phase measurements}.
 * @see WindUpFactory
 * @author Luc Maisonobe
 * @since 10.1
 */
public class WindUp extends AbstractWindUp<Phase> {

    /** Simple constructor.
     * <p>
     * The constructor is package protected to enforce use of {@link WindUpFactory}
     * and preserve phase continuity for successive measurements involving the same
     * satellite/receiver pair.
     * </p>
     * @param emitter emitter dipole
     */
    WindUp(final Dipole emitter) {
        super(emitter, Dipole.CANONICAL_I_J);
    }

    /** {@inheritDoc} */
    @Override
    protected Rotation emitterToInert(final EstimatedMeasurementBase<Phase> estimated) {
        // we don't use the basic yaw steering attitude model from ESA navipedia page
        // but rely on the attitude that was computed by the propagator, which takes
        // into account the proper noon and midnight turns for each satellite model
        return estimated.getStates()[0].toStaticTransform().getRotation().revert();
    }

    /** {@inheritDoc} */
    @Override
    protected Rotation receiverToInert(final EstimatedMeasurementBase<Phase> estimated) {
        final TimeStampedPVCoordinates[] participants = estimated.getParticipants();
        final Frame                      inertial     = estimated.getStates()[0].getFrame();
        final GroundStation              station      = estimated.getObservedMeasurement().getStation();
        return station.getOffsetToInertial(inertial, participants[1].getDate(), false).getRotation();
    }

}
