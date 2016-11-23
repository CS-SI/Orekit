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

public class RangeMeasurementCreator extends MeasurementCreator {

    private final Context context;

    public RangeMeasurementCreator(final Context context) {
        this.context = context;
    }

    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws OrekitException {
        try {
            for (final GroundStation station : context.stations) {
                final AbsoluteDate     date      = currentState.getDate();
                final Frame            inertial  = currentState.getFrame();
                final Vector3D         position  = currentState.getPVCoordinates().getPosition();
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
                    final Vector3D stationAtReception =
                                    topo.getTransformTo(inertial, receptionDate).transformPosition(Vector3D.ZERO);
                    final double downLinkDistance = Vector3D.distance(position, stationAtReception);

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
                    final Vector3D stationAtEmission  =
                                    topo.getTransformTo(inertial, emissionDate).transformPosition(Vector3D.ZERO);
                    final double upLinkDistance = Vector3D.distance(position, stationAtEmission);
                    addMeasurement(new Range(station, receptionDate,
                                             0.5 * (downLinkDistance + upLinkDistance), 1.0, 10));
                }

            }
        } catch (OrekitExceptionWrapper oew) {
            throw new OrekitException(oew.getException());
        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
