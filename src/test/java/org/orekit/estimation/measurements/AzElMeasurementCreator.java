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

public class AzElMeasurementCreator extends MeasurementCreator {

    private final Context context;

    public AzElMeasurementCreator(final Context context) {
        this.context = context;
    }

    public void handleStep(final SpacecraftState currentState, final boolean isLast)
        throws PropagationException {
        try {
            for (final GroundStation station : context.stations) {
                final Vector3D position = currentState.getPVCoordinates().getPosition();
                final double elevation =
                                station.getBaseFrame().getElevation(position,
                                                                    currentState.getFrame(),
                                                                    currentState.getDate());
                if (elevation > FastMath.toRadians(30.0)) {
                    final UnivariateFunction f = new UnivariateFunction() {
                       public double value(final double x) throws OrekitExceptionWrapper {
                           try {
                               final AbsoluteDate date = currentState.getDate().shiftedBy(x);
                               final Transform t = station.getBaseFrame().getTransformTo(currentState.getFrame(),
                                                                                         date);
                               final double d = Vector3D.distance(position,
                                                                  t.transformPosition(Vector3D.ZERO));
                               return d - x * Constants.SPEED_OF_LIGHT;
                           } catch (OrekitException oe) {
                               throw new OrekitExceptionWrapper(oe);
                           }
                        }
                    };
                    final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-10, 5);
                    final double dt = solver.solve(1000, f, -1.0, 1.0);
                    final AbsoluteDate date = currentState.getDate().shiftedBy(dt);
                    final Transform t = station.getBaseFrame().getTransformTo(currentState.getFrame(),
                                                                              date);
                    // Compute Azimuth and Elevation at dt
                    final double[] azel = new double[2];
                    final double[] sigma = {1.0, 1.0};
                    final double[] baseweight = {10.0, 10.0};
                    
                    azel[1] = station.getBaseFrame().getElevation(t.transformPosition(Vector3D.ZERO),
                                                                  currentState.getFrame(),
                                                                  currentState.getDate());
                    azel[0] = station.getBaseFrame().getAzimuth(t.transformPosition(Vector3D.ZERO),
                                                                                  currentState.getFrame(),
                                                                                  currentState.getDate());
                    
                    addMeasurement(new AzEl(station, date, azel, sigma, baseweight));
                }

            }
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

}
