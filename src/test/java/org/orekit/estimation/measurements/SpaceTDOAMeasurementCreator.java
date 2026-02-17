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

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.Context;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
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

        final AbsoluteDate date = currentState.getDate();
        final Frame inertial = currentState.getFrame();
        final Vector3D position = currentState.getPosition();

        final Vector3D p1 = currentState.getPVCoordinates(context.getEarth().getBodyFrame()).getPosition();
        final Vector3D p2 = primary.getPVCoordinatesProvider().getPosition(date, context.getEarth().getBodyFrame());
        final Vector3D p3 = secondary.getPVCoordinatesProvider().getPosition(date, context.getEarth().getBodyFrame());

        final double minAltitude1 = context.getEarth().lowestAltitudeIntermediate(p1, p2).getAltitude();
        final double minAltitude2 = context.getEarth().lowestAltitudeIntermediate(p1, p3).getAltitude();

        // Create a BRR measurement only if elevation for both stations is higher than
        // 30°
        if (minAltitude1 > 1e5 && minAltitude2 > 1e5) {

            // The solver used
            final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

            // Signal time of flight to primary station
            final double referenceDelay = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Vector3D pos = primary.getPVCoordinatesProvider().getPosition(date.shiftedBy(x), inertial);
                    final double d = Vector3D.distance(position, pos);
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);
            final AbsoluteDate receptionDate = date.shiftedBy(referenceDelay);

            // Signal time of flight to secondary station
            final double relativeDelay = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Vector3D pos = secondary.getPVCoordinatesProvider().getPosition(date.shiftedBy(x), inertial);
                    final double d = Vector3D.distance(position, pos);
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);

            // time difference on arrival
            final double tdoa = referenceDelay - relativeDelay;

            addMeasurement(new TDOA(primary, secondary, receptionDate, tdoa, 1.0, 10, satellite));

        }

    }
}
