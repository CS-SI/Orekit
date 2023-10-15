/* Copyright 2002-2023 CS GROUP
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
import org.orekit.estimation.StationDataProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;

public class RangeRateMeasurementCreator extends MeasurementCreator {

    private final StationDataProvider context;
    private final boolean twoWay;
    private final ObservableSatellite satellite;

    public RangeRateMeasurementCreator(final StationDataProvider context, boolean twoWay,
                                       final double satClockDrift) {
        this.context   = context;
        this.twoWay    = twoWay;
        this.satellite = new ObservableSatellite(0);
        this.satellite.getClockDriftDriver().setValue(satClockDrift);
    }

    public ObservableSatellite getSatellite() {
        return satellite;
    }

    public void init(SpacecraftState s0, AbsoluteDate t, double step) {
        for (final GroundStation station : context.getStations()) {
            for (ParameterDriver driver : Arrays.asList(station.getClockOffsetDriver(),
                                                        station.getClockDriftDriver(),
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
        if (satellite.getClockDriftDriver().getReferenceDate() == null) {
            satellite.getClockDriftDriver().setReferenceDate(s0.getDate());
        }
    }

    public void handleStep(final SpacecraftState currentState) {
        for (final GroundStation station : context.getStations()) {
            final AbsoluteDate     date      = currentState.getDate();
            final Frame            inertial  = currentState.getFrame();
            final Vector3D         position  = currentState.getPosition();
            final Vector3D         velocity  = currentState.getPVCoordinates().getVelocity();
            final double           groundDft = station.getClockDriftDriver().getValue(date);
            final double           satDft    = satellite.getClockDriftDriver().getValue(date);
            final double           deltaD    = Constants.SPEED_OF_LIGHT * (groundDft - satDft);

            if (station.getBaseFrame().getTrackingCoordinates(position, inertial, date).getElevation() > FastMath.toRadians(30.0)) {
                final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

                final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                    public double value(final double x) {
                        final Transform t = station.getOffsetToInertial(inertial, date.shiftedBy(x), false);
                        final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                        return d - x * Constants.SPEED_OF_LIGHT;
                    }
                }, -1.0, 1.0);
                final AbsoluteDate receptionDate  = currentState.getDate().shiftedBy(downLinkDelay);
                final PVCoordinates stationAtReception =
                                station.getOffsetToInertial(inertial, receptionDate, false).transformPVCoordinates(PVCoordinates.ZERO);

                // line of sight at reception
                final Vector3D receptionLOS = (position.subtract(stationAtReception.getPosition())).normalize();

                // relative velocity, spacecraft-station, at the date of reception
                final Vector3D deltaVr = velocity.subtract(stationAtReception.getVelocity());

                final double upLinkDelay = solver.solve(1000, new UnivariateFunction() {
                    public double value(final double x) {
                        final Transform t = station.getOffsetToInertial(inertial, date.shiftedBy(-x), false);
                        final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                        return d - x * Constants.SPEED_OF_LIGHT;
                    }
                }, -1.0, 1.0);
                final AbsoluteDate emissionDate   = currentState.getDate().shiftedBy(-upLinkDelay);
                final PVCoordinates stationAtEmission  =
                                station.getOffsetToInertial(inertial, emissionDate, false).transformPVCoordinates(PVCoordinates.ZERO);

                // line of sight at emission
                final Vector3D emissionLOS = (position.subtract(stationAtEmission.getPosition())).normalize();

                // relative velocity, spacecraft-station, at the date of emission
                final Vector3D deltaVe = velocity.subtract(stationAtEmission.getVelocity());

                // range rate at the date of reception
                final double rr = twoWay ?
                                  0.5 * (deltaVr.dotProduct(receptionLOS) + deltaVe.dotProduct(emissionLOS)) :
                                  deltaVr.dotProduct(receptionLOS) + deltaD;

                addMeasurement(new RangeRate(station, receptionDate, rr, 1.0, 10, twoWay, satellite));
            }

        }
    }

}
