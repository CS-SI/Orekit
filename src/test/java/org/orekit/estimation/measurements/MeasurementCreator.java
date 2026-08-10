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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.analysis.solvers.UnivariateSolver;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public abstract class MeasurementCreator implements OrekitFixedStepHandler {

    private final List<ObservedMeasurement<?>> measurements;

    protected MeasurementCreator() {
        measurements = new ArrayList<>();
    }

    public List<ObservedMeasurement<?>> getMeasurements() {
        return measurements;
    }

    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t, final double step) {
        measurements.clear();
    }

    protected double solveDownlinkDelay(final Observer observer, final SpacecraftState currentState, final Vector3D meanPosition) {
                
        final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);
        final AbsoluteDate date = currentState.getDate();
        final Frame frame = currentState.getFrame();
        final Vector3D position = currentState.getPosition();

        final double delay  = solver.solve(1000, x -> {
                final Transform t = observer.getOffsetToInertial(frame, date.shiftedBy(x), true);
                final double d = Vector3D.distance(position, t.transformPosition(meanPosition));
                return d - x * Constants.SPEED_OF_LIGHT;
            }, -1.0, 1.0);

        return delay;
    }


    protected double solveUplinkDelay(final Observer observer, final SpacecraftState currentState, final Vector3D meanPosition) {
                
        final UnivariateSolver solver = new BracketingNthOrderBrentSolver(1.0e-12, 5);
        final AbsoluteDate date = currentState.getDate();
        final Frame frame = currentState.getFrame();
        final Vector3D position = currentState.getPosition();

        final double delay  = solver.solve(1000, x -> {
                final Transform t = observer.getOffsetToInertial(frame, date.shiftedBy(-x), true);
                final double d = Vector3D.distance(position, t.transformPosition(meanPosition));
                return d - x * Constants.SPEED_OF_LIGHT;
            }, -1.0, 1.0);

        return delay;
    }

    protected void addMeasurement(final ObservedMeasurement<?> measurement) {
        measurements.add(measurement);
    }

}
