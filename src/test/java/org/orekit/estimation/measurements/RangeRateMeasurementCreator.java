/* Copyright 2002-2015 CS Systèmes d'Information
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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.PropagationException;
import org.orekit.estimation.Context;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class RangeRateMeasurementCreator extends MeasurementCreator {

    private final Context context;

    public RangeRateMeasurementCreator(final Context context) {
        this.context = context;
    }

    /**
     * @param currentState  spacecraft state at the signal reception date 
     */
    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws PropagationException {
        try {
            for (final GroundStation station : context.stations) {
            	
                final double          downlinkDelay    = station.downlinkTimeOfFlight(currentState, currentState.getDate());
                final double          offset           = currentState.getDate().durationFrom(currentState.getDate());

                final SpacecraftState compensatedState = currentState.shiftedBy(offset - downlinkDelay);
                
                final Vector3D position = compensatedState.getPVCoordinates().getPosition();
                               
                final double elevation =
                                station.getBaseFrame().getElevation(position,
                                                                    currentState.getFrame(),
                                                                    currentState.getDate());
                
                if (elevation > FastMath.toRadians(30.0)) {
                	
                    // date of reception (downlink)
                    final AbsoluteDate date = currentState.getDate(); //currentState.getDate().shiftedBy(dt);
                    // station position at the date of reception, tR                    
                    final Transform t = station.getBaseFrame().getTransformTo(currentState.getFrame(),
                                                                              date);
                    final Vector3D stationPosition = t.transformPosition(Vector3D.ZERO);
                    // line of sight (tE, tR)
                    final Vector3D los = (position.subtract(stationPosition)).normalize();

                    // relative velocity, spacecraft-station, at the date of reception
                    final Vector3D v1 = station.getBaseFrame().getPVCoordinates(date, currentState.getFrame()).getVelocity();
                    final Vector3D v2 = compensatedState.getPVCoordinates().getVelocity();
                    final Vector3D dr = v2.subtract(v1);
                    
                    // range rate at the date of reception 
                    final double rr = dr.dotProduct(los);
                    
                    addMeasurement(new RangeRate(station, date,
                                             rr,
                                             1.0, 10, false));
                }

            }
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

}
