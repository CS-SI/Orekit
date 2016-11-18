/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.estimation.Context;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class RangeRateMeasurementCreator extends MeasurementCreator {

    private final Context context;
    private final boolean twoWay;

    public RangeRateMeasurementCreator(final Context context, boolean twoWay) {
        this.context = context;
        this.twoWay  = twoWay;
    }

    /**
     * @param currentState  spacecraft state at the signal reception date
     */
    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws OrekitException {
        for (final GroundStation station : context.stations) {

            final AbsoluteDate     date      = currentState.getDate();
            final Frame            inertial  = currentState.getFrame();
            final Vector3D         position  = currentState.getPVCoordinates().getPosition();
            final Vector3D         velocity  = currentState.getPVCoordinates().getVelocity();
            final TopocentricFrame topo      = station.getBaseFrame();

            if (topo.getElevation(position, inertial, date) > FastMath.toRadians(30.0)) {
                final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

                final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                    public double value(final double x) throws OrekitExceptionWrapper {
                        try {
                            final Transform t = topo.getTransformTo(inertial, date.shiftedBy(x));
                            final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                            return d - x * Constants.SPEED_OF_LIGHT;
                        } catch (OrekitException oe) {
                            throw new OrekitExceptionWrapper(oe);
                        }
                    }
                }, -1.0, 1.0);
                final AbsoluteDate receptionDate  = currentState.getDate().shiftedBy(downLinkDelay);
                final PVCoordinates stationAtReception =
                                topo.getTransformTo(inertial, receptionDate).transformPVCoordinates(PVCoordinates.ZERO);

                // line of sight at reception
                final Vector3D receptionLOS = (position.subtract(stationAtReception.getPosition())).normalize();

                // relative velocity, spacecraft-station, at the date of reception
                final Vector3D deltaVr = velocity.subtract(stationAtReception.getVelocity());

                final double upLinkDelay = solver.solve(1000, new UnivariateFunction() {
                    public double value(final double x) throws OrekitExceptionWrapper {
                        try {
                            final Transform t = topo.getTransformTo(inertial, date.shiftedBy(-x));
                            final double d = Vector3D.distance(position, t.transformPosition(Vector3D.ZERO));
                            return d - x * Constants.SPEED_OF_LIGHT;
                        } catch (OrekitException oe) {
                            throw new OrekitExceptionWrapper(oe);
                        }
                    }
                }, -1.0, 1.0);
                final AbsoluteDate emissionDate   = currentState.getDate().shiftedBy(-upLinkDelay);
                final PVCoordinates stationAtEmission  =
                                topo.getTransformTo(inertial, emissionDate).transformPVCoordinates(PVCoordinates.ZERO);

                // line of sight at emission
                final Vector3D emissionLOS = (position.subtract(stationAtEmission.getPosition())).normalize();

                // relative velocity, spacecraft-station, at the date of emission
                final Vector3D deltaVe = velocity.subtract(stationAtEmission.getVelocity());

                // range rate at the date of reception
                final double rr = twoWay ?
                                          0.5 * (deltaVr.dotProduct(receptionLOS) + deltaVe.dotProduct(emissionLOS)) :
                                              deltaVr.dotProduct(receptionLOS);

                                          addMeasurement(new RangeRate(station, date,
                                                                       rr,
                                                                       1.0, 10, twoWay));
            }

        }
    }

}
