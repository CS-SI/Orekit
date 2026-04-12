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
import org.orekit.estimation.Context;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

/**
 * Creates a list of {@link TDOA} measurements.
 * 
 * @author Pascal Parraud
 */
public class SpaceTDOAMeasurementCreator extends MeasurementCreator {

    private final Context context;
    private final ObserverSatellite primary;
    private final ObserverSatellite secondary;
    private final ObservableSatellite satellite;

    public SpaceTDOAMeasurementCreator(final Context context) {
        this.context = context;
        this.primary = context.satellites.get(0);
        this.secondary = context.satellites.get(1);
        this.satellite = new ObservableSatellite(0);
    }

    public SpaceTDOAMeasurementCreator(final Context context, final double primaryBias, final double primaryDrift,
            final double secondaryBias, final double secondaryDrift) {
        this.context = context;
        this.primary = context.satellites.get(0);
        this.secondary = context.satellites.get(1);
        this.satellite = new ObservableSatellite(0);

        primary.getClockBiasDriver().setValue(primaryBias);
        primary.getClockDriftDriver().setValue(primaryDrift);
        secondary.getClockBiasDriver().setValue(secondaryBias);
        secondary.getClockDriftDriver().setValue(secondaryDrift);

        primary.getClockBiasDriver().setReferenceDate(context.initialOrbit.getDate());
        primary.getClockDriftDriver().setReferenceDate(context.initialOrbit.getDate());
        secondary.getClockBiasDriver().setReferenceDate(context.initialOrbit.getDate());
        secondary.getClockDriftDriver().setReferenceDate(context.initialOrbit.getDate());
    }

    public ObservableSatellite getSatellite() {
        return satellite;
    }

    public ObserverSatellite getPrimary() {
        return primary;
    }

    public ObserverSatellite getSecondary() {
        return secondary;
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

        final AbsoluteDate date = currentState.getDate();

        final Vector3D p1 = currentState.getPVCoordinates(context.getEarth().getBodyFrame()).getPosition();
        final Vector3D p2 = primary.getPVCoordinatesProvider().getPosition(date, context.getEarth().getBodyFrame());
        final Vector3D p3 = secondary.getPVCoordinatesProvider().getPosition(date, context.getEarth().getBodyFrame());

        final double minAltitude1 = context.getEarth().lowestAltitudeIntermediate(p1, p2).getAltitude();
        final double minAltitude2 = context.getEarth().lowestAltitudeIntermediate(p1, p3).getAltitude();

        // Create a BRR measurement only if elevation for both stations is higher than
        // 30°
        if (minAltitude1 > 1e5 && minAltitude2 > 1e5) {

            // Signal time of flight to primary station
            final double referenceDelay = solveDownlinkDelay(primary, currentState, Vector3D.ZERO);
            final AbsoluteDate receptionDate = date.shiftedBy(referenceDelay);

            // Signal time of flight to secondary station
            final double relativeDelay = solveDownlinkDelay(secondary, currentState, Vector3D.ZERO);

            // time difference on arrival
            final double primaryOffset = primary.getOffsetValue(receptionDate);
            final double secondaryOffset = secondary.getOffsetValue(receptionDate);
            final double tdoa = (referenceDelay + primaryOffset) - (relativeDelay + secondaryOffset);

            // Final measurement
            final double clockOffset = primary.getOffsetValue(receptionDate);
            addMeasurement(new TDOA(primary, secondary, receptionDate.shiftedBy(clockOffset), tdoa, 1.0, 10, satellite));

        }

    }
}
