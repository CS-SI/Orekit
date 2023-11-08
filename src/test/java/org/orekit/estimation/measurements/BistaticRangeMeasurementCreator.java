/* Copyright 2002-2023 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.util.FastMath;
import org.orekit.estimation.Context;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

public class BistaticRangeMeasurementCreator extends MeasurementCreator {

    private final Context context;
    private final GroundStation emitter;
    private final GroundStation receiver;
    private final ObservableSatellite satellite;


    public BistaticRangeMeasurementCreator(final Context context) {
        this.context   = context;
        this.emitter   = context.BRRstations.getKey();
        this.receiver  = context.BRRstations.getValue();
        this.satellite = new ObservableSatellite(0);
    }

    public ObservableSatellite getSatellite() {
        return satellite;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final GroundStation station : Arrays.asList(context.BRRstations.getKey(),
                context.BRRstations.getValue())) {
            for (ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                    station.getEastOffsetDriver(),
                    station.getNorthOffsetDriver(),
                    station.getZenithOffsetDriver(),
                    station.getPrimeMeridianOffsetDriver(),
                    station.getPrimeMeridianDriftDriver(),
                    station.getPolarOffsetXDriver(),
                    station.getPolarDriftXDriver(),
                    station.getPolarOffsetYDriver(),
                    station.getPolarDriftYDriver())) {
                if (driver.getReferenceDate() == null) {
                    driver.setReferenceDate(s0.getDate());
                }
            }
        }
    }

    public void handleStep(final SpacecraftState currentState) {
        final AbsoluteDate  date     = currentState.getDate();
        final Frame         inertial = currentState.getFrame();
        final Vector3D      position = currentState.getPosition();

        // Create a BRR measurement only if elevation for both stations is higher than 30°
        if ((emitter.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation()  > FastMath.toRadians(30.0)) &&
            (receiver.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0))) {
            final double clockOffset = receiver.getClockOffsetDriver().getValue();
            final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

            final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Transform t = receiver.getOffsetToInertial(inertial, date.shiftedBy(clockOffset + x), false);
                    final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);
            final AbsoluteDate receptionDate  = currentState.getDate().shiftedBy(downLinkDelay);
            final Vector3D stationAtReception =
                    receiver.getOffsetToInertial(inertial, receptionDate.shiftedBy(clockOffset), false).transformPosition(Vector3D.ZERO);
            final double downLinkDistance = Vector3D.distance(position, stationAtReception);

            final double upLinkDelay = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Transform t = emitter.getOffsetToInertial(inertial, date.shiftedBy(clockOffset - x), false);
                    final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);
            final AbsoluteDate emissionDate   = currentState.getDate().shiftedBy(-upLinkDelay);
            final Vector3D stationAtEmission  =
                   emitter.getOffsetToInertial(inertial, emissionDate.shiftedBy(clockOffset), false).transformPosition(Vector3D.ZERO);
            final double upLinkDistance = Vector3D.distance(position, stationAtEmission);
            addMeasurement(new BistaticRange(emitter, receiver, receptionDate.shiftedBy(clockOffset),
                    downLinkDistance + upLinkDistance, 1.0, 10, satellite));
        }

    }

}
