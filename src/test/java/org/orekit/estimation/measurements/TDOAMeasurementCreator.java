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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.estimation.Context;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

/**
 * Creates a list of {@link TDOA} measurements.
 * @author Pascal Parraud
 */
public class TDOAMeasurementCreator extends MeasurementCreator {

    private final Context context;
    private final GroundStation primary;
    private final GroundStation secondary;
    private final ObservableSatellite satellite;

    public TDOAMeasurementCreator(final Context context) {
        this.context   = context;
        this.primary   = context.TDOAstations.getKey();
        this.secondary = context.TDOAstations.getValue();
        this.satellite = new ObservableSatellite(0);
    }

    public ObservableSatellite getSatellite() {
        return satellite;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final GroundStation station : Arrays.asList(context.TDOAstations.getKey(),
                                                         context.TDOAstations.getValue())) {
            for (ParameterDriver driver : station.getParametersDrivers()) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(s0.getDate());
                }
            }
        }
    }

    public void handleStep(final SpacecraftState currentState) {

        final AbsoluteDate date     = currentState.getDate();
        final Frame        inertial = currentState.getFrame();
        final Vector3D     position = currentState.getPosition();

        // Create a BRR measurement only if elevation for both stations is higher than 30°
        if ((primary.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation()  > FastMath.toRadians(30.0)) &&
            (secondary.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0))) {

            // Signal time of flight to primary station
            final double referenceDelay = solveDownlinkDelay(primary, currentState, Vector3D.ZERO);
            final AbsoluteDate receptionDate  = date.shiftedBy(referenceDelay);

            // Signal time of flight to secondary station
            final double relativeDelay = solveDownlinkDelay(secondary, currentState, Vector3D.ZERO);

            // time difference on arrival
            final double tdoa = referenceDelay - relativeDelay;

            final double clockOffset = primary.getOffsetValue(receptionDate);
            addMeasurement(new TDOA(primary, secondary, receptionDate.shiftedBy(clockOffset), tdoa, 1.0, 10, satellite));

        }

    }
}
