/* Copyright 2022-2026 Bryan Cazabonne
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Bryan Cazabonne licenses this file to You under the Apache License, Version 2.0
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
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;

/** Modifier for wind-up effect in GNSS {@link OneWayGNSSPhase one-way GNSS phase measurements}.
 * @see OneWayGNSSWindUpFactory
 * @author Bryan Cazabonne
 * @since 14.0
 */
public class OneWayGNSSWindUp extends AbstractWindUp<OneWayGNSSPhase> {

    /** Attitude provider for the GNSS emitter satellite. */
    private final AttitudeProvider emitterAttitude;

    /** Simple constructor.
     * <p>
     * The constructor is package protected to enforce use of {@link OneWayGNSSWindUpFactory}
     * and preserve phase continuity for successive measurements involving the same
     * satellite/receiver pair.
     * </p>
     * @param emitter emitter dipole
     * @param receiver receiver dipole
     * @param emitterAttitude attitude provider for the GNSS emitter satellite
     */
    OneWayGNSSWindUp(final Dipole emitter, final Dipole receiver,
                     final AttitudeProvider emitterAttitude) {
        super(emitter, receiver);
        this.emitterAttitude = emitterAttitude;
    }

    /** {@inheritDoc} */
    @Override
    protected Rotation emitterToInert(final EstimatedMeasurementBase<OneWayGNSSPhase> estimated) {
        return emitterAttitude.getAttitudeRotation(estimated.getObservedMeasurement().getObserver().getPVCoordinatesProvider(),
                                                   estimated.getStates()[0].getDate(),
                                                   estimated.getStates()[0].getFrame())
                              .revert();
    }

    /** {@inheritDoc} */
    @Override
    protected Rotation receiverToInert(final EstimatedMeasurementBase<OneWayGNSSPhase> estimated) {
        return estimated.getStates()[0].toStaticTransform().getRotation().revert();
    }

}
