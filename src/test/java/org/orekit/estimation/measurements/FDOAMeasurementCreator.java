/* Copyright 2002-2023 Mark Rutten
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
import org.hipparchus.util.FastMath;
import org.orekit.estimation.Context;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

/**
 * Creates a list of {@link FDOA bistatic range-rate measurements}.
 * @author Mark Rutten
 */
public class FDOAMeasurementCreator extends MeasurementCreator {

    private final Context context;
    private final GroundStation secondary;
    private final GroundStation primary;
    private final ObservableSatellite satellite;
    private final double centreFrequency;

    public FDOAMeasurementCreator(final Context context, final double centreFrequency) {
        this.context   = context;
        this.secondary = context.TDOAstations.getKey();
        this.primary = context.TDOAstations.getValue();
        this.satellite = new ObservableSatellite(0);
        this.centreFrequency = centreFrequency;
    }

    public ObservableSatellite getSatellite() {
        return satellite;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final GroundStation station : Arrays.asList(context.TDOAstations.getKey(),
                                                         context.TDOAstations.getValue())) {
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
        final Vector3D      velocity = currentState.getPVCoordinates().getVelocity();

        // Create a BRR measurement only if elevation for both stations is higher than 30°
        if ((secondary.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation()  > FastMath.toRadians(30.0)) &&
            (primary.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0))) {

            // The solver used
            final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

            final double primaryDelay  = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Transform t = primary.getOffsetToInertial(inertial, date.shiftedBy(x), false);
                    final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);

            final AbsoluteDate primaryReceptionDate = currentState.getDate().shiftedBy(primaryDelay);
            final PVCoordinates primaryPV   = primary.getOffsetToInertial(inertial, primaryReceptionDate, false)
                                                       .transformPVCoordinates(PVCoordinates.ZERO);

            // line of sight at primary
            final Vector3D primaryLOS = (position.subtract(primaryPV.getPosition())).normalize();

            // relative velocity, spacecraft-station, at the date of primary reception
            final Vector3D deltaVp = velocity.subtract(primaryPV.getVelocity());

            final double secondaryDelay = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) {
                    final Transform t = secondary.getOffsetToInertial(inertial, date.shiftedBy(x), false);
                    final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);

            final AbsoluteDate secondaryReceptionDate = currentState.getDate().shiftedBy(secondaryDelay);
            final PVCoordinates secondaryPV   = secondary.getOffsetToInertial(inertial, secondaryReceptionDate, false)
                                                     .transformPVCoordinates(PVCoordinates.ZERO);

            // line of sight at secondary
            final Vector3D secondaryLOS = (position.subtract(secondaryPV.getPosition())).normalize();

            // relative velocity, spacecraft-station, at the date of secondary reception
            final Vector3D deltaVs = velocity.subtract(secondaryPV.getVelocity());

            // range rate difference at the date of reception
            final double rrDifference = deltaVp.dotProduct(primaryLOS) - deltaVs.dotProduct(secondaryLOS);
            final double fdoa = rrDifference * centreFrequency / Constants.SPEED_OF_LIGHT;

            addMeasurement(new FDOA(secondary, primary, centreFrequency, primaryReceptionDate,
                    fdoa, 1.0, 10, satellite));

        }

    }
}
