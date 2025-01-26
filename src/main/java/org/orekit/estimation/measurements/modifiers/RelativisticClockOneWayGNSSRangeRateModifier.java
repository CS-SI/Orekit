/* Copyright 2022-2025 Thales Alenia Space
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

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.gnss.OneWayGNSSRangeRate;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.PVCoordinates;

/** Class modifying theoretical range-rate measurement with relativistic frequency deviation.
 * It works only with orbit-based states.
 * <p>
 * Relativistic clock correction is caused by the motion of the satellite as well as
 * the change in the gravitational potential
 * </p>
 * @author Luc Maisonobe
 * @since 12.1
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Springer, 2017."
 */
public class RelativisticClockOneWayGNSSRangeRateModifier
    extends AbstractRelativisticClockOnBoardRangeRateModifier<OneWayGNSSRangeRate> {

    /** Simple constructor.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public RelativisticClockOneWayGNSSRangeRateModifier(final double gm) {
        super(gm);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<OneWayGNSSRangeRate> estimated) {

        // local satellite
        final SpacecraftState local = estimated.getStates()[0];

        // compute semi major axis of remote (transmitter) satellite
        final PVCoordinates remote  = estimated.getParticipants()[0];
        final double        rRemote = remote.getPosition().getNorm();
        final double        vRemote = remote.getVelocity().getNorm();
        final double        aRemote = 1.0 / (2 / rRemote - vRemote * vRemote / getGm());

        modifyWithoutDerivatives(estimated,
                                 local.getOrbit().getA(), local.getPosition().getNorm(),
                                 aRemote, rRemote);

    }

}
