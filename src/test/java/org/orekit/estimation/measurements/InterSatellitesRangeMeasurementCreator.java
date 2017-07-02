/* Copyright 2002-2017 CS Systèmes d'Information
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class InterSatellitesRangeMeasurementCreator extends MeasurementCreator {

    private final BoundedPropagator ephemeris;

    public InterSatellitesRangeMeasurementCreator(final BoundedPropagator ephemeris) {
        this.ephemeris = ephemeris;
    }

    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws OrekitException {
        try {
            final AbsoluteDate     date      = currentState.getDate();
            final Vector3D         position  = currentState.getPVCoordinates().getPosition();

            final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);

            final double downLinkDelay  = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) throws OrekitExceptionWrapper {
                    try {
                        final PVCoordinates other = ephemeris.getPVCoordinates(date.shiftedBy(-x), currentState.getFrame());
                        final double d = Vector3D.distance(position, other.getPosition());
                        return d - x * Constants.SPEED_OF_LIGHT;
                    } catch (OrekitException oe) {
                        throw new OrekitExceptionWrapper(oe);
                    }
                }
            }, -1.0, 1.0);
            final AbsoluteDate transitDate = currentState.getDate().shiftedBy(-downLinkDelay);
            final Vector3D otherAtTransit =
                            ephemeris.getPVCoordinates(transitDate, currentState.getFrame()).getPosition();
            final double downLinkDistance = Vector3D.distance(position, otherAtTransit);

            final double upLinkDelay = solver.solve(1000, new UnivariateFunction() {
                public double value(final double x) throws OrekitExceptionWrapper {
                    final PVCoordinates self = currentState.shiftedBy(-downLinkDelay - x).getPVCoordinates();
                    final double d = Vector3D.distance(otherAtTransit, self.getPosition());
                    return d - x * Constants.SPEED_OF_LIGHT;
                }
            }, -1.0, 1.0);
            final Vector3D selfAtEmission  =
                            currentState.shiftedBy(-downLinkDelay - upLinkDelay).getPVCoordinates().getPosition();
            final double upLinkDistance = Vector3D.distance(otherAtTransit, selfAtEmission);
            addMeasurement(new InterSatellitesRange(0, 1, true, date,
                                                   0.5 * (downLinkDistance + upLinkDistance), 1.0, 10));
        } catch (OrekitExceptionWrapper oew) {
            throw new OrekitException(oew.getException());
        } catch (OrekitException oe) {
            throw new OrekitException(oe);
        }
    }

}
