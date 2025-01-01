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
import org.orekit.estimation.measurements.gnss.InterSatellitesOneWayRangeRate;
import org.orekit.propagation.SpacecraftState;

/** Class modifying theoretical range-rate measurement with relativistic frequency deviation.
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
public class RelativisticClockInterSatellitesOneWayRangeRateModifier
    extends AbstractRelativisticClockOnBoardRangeRateModifier<InterSatellitesOneWayRangeRate> {

    /** Simple constructor.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public RelativisticClockInterSatellitesOneWayRangeRateModifier(final double gm) {
        super(gm);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<InterSatellitesOneWayRangeRate> estimated) {
        final SpacecraftState local  = estimated.getStates()[0];
        final SpacecraftState remote = estimated.getStates()[1];
        modifyWithoutDerivatives(estimated,
                                 local.getA(), local.getPosition().getNorm(),
                                 remote.getA(), remote.getPosition().getNorm());
    }

}
