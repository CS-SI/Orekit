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

package org.orekit.propagation.relative.lambert;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.control.heuristics.lambert.LambertBoundaryConditions;
import org.orekit.control.heuristics.lambert.LambertSolution;
import org.orekit.control.heuristics.lambert.LambertSolver;
import org.orekit.frames.Frame;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Comparator;
import java.util.List;

public class LambertRendezVous {

    private LambertRendezVous() { }

    public static TwoImpulseTransfer computeRendezVous(final TimeStampedPVCoordinates chaserPVTInitial, final TimeStampedPVCoordinates chaserPVTFinal, final Frame chaserPVFrame, final Propagator targetPropagator) {
        // Get target propagator frame
        final Frame frame = targetPropagator.getFrame();

        // Transform input PVTs from the input frame to the target propagator's frame
        final TimeStampedPVCoordinates chaserPVTInitialInertial = chaserPVFrame.getTransformTo(frame, chaserPVTInitial.getDate()).transformPVCoordinates(chaserPVTInitial);
        final TimeStampedPVCoordinates chaserPVTFinalInertial = chaserPVFrame.getTransformTo(frame, chaserPVTFinal.getDate()).transformPVCoordinates(chaserPVTFinal);

        // Compute Lambert transfer
        final LambertBoundaryConditions boundaryConditions = new LambertBoundaryConditions(
                chaserPVTInitialInertial.getDate(),
                chaserPVTInitialInertial.getPosition(),
                chaserPVTFinalInertial.getDate(),
                chaserPVTFinalInertial.getPosition(),
                frame);

        final boolean posigrade = (chaserPVTInitialInertial.getMomentum().dotProduct(Vector3D.PLUS_K)) >= 0;

        final LambertSolver solver = new LambertSolver(targetPropagator.getInitialState().getOrbit().getMu());
        final List<LambertSolution> solutions = solver.solve(posigrade, boundaryConditions);

        // Pick the solution with the least amount of ΔV
        return solutions.stream()
                .map(solution -> TwoImpulseTransfer.fromLambertSolution(solution, chaserPVTInitialInertial.getVelocity(), chaserPVTFinalInertial.getVelocity()))
                .min(Comparator.comparingDouble(TwoImpulseTransfer::getTotalDeltaV))
                .orElse(null);
    }
}
