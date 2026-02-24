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

package org.orekit.propagation.relative.clohessywiltshire;


import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.Fieldifier;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * <p>This class implements the solution to the Clohessy-Wiltshire equations for a two-impulse rendez-vous.
 * The Clohessy-Wiltshire equations have a closed-form solution which enables extremely fast rendez-vous computations
 * for circular target orbits, as long as the chaser is not too far from the target.</p>
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public class FieldClohessyWiltshireRendezVous<T extends CalculusFieldElement<T>> {

    public FieldClohessyWiltshireRendezVous() {
    }

    /**
     * Computes a two-impulse transfer between an initial and final positions using the Clohessy-Wiltshire closed-form solution.
     * @param chaserPVTInitial Initial chaser PVT expressed in provided frame.
     * @param chaserPVTFinal Final chaser PVT expressed in provided frame.
     * @param chaserPVFrame Frame in which the initial and final chaser PVT are expressed.
     * @param targetOrbit Target spacecraft's orbit.
     * @return TwoImpulseTransfer to perform the rendezvous.
     */
    public FieldTwoImpulseTransfer<T> computeRendezVous(final TimeStampedFieldPVCoordinates<T> chaserPVTInitial, final TimeStampedFieldPVCoordinates<T> chaserPVTFinal, final Frame chaserPVFrame, final FieldOrbit<T> targetOrbit) {
        // Create QSW local orbital frame of the target
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit.toOrbit()), "QSW LOF target");

        // Transform input PVTs from the input frame to the target's LOF
        final TimeStampedFieldPVCoordinates<T> chaserPVTInitialLOF = chaserPVFrame.getTransformTo(targetLof, chaserPVTInitial.getDate()).transformPVCoordinates(chaserPVTInitial);
        final TimeStampedFieldPVCoordinates<T> chaserPVTFinalLOF = chaserPVFrame.getTransformTo(targetLof, chaserPVTFinal.getDate()).transformPVCoordinates(chaserPVTFinal);

        // Compute target's mean motion
        final T meanMotion = targetOrbit.getKeplerianMeanMotion();

        // Compute duration of the transfer from the initial and final states dates
        final T deltaT = chaserPVTFinalLOF.getDate().durationFrom(chaserPVTInitialLOF.getDate());

        // Compute the Clohessy-Wiltshire matrices for the desired final time t
        final FieldClohessyWiltshireMatrices<T> cwMatrices = (new FieldClohessyWiltshireEquations<T>()).computeMatrices(deltaT, meanMotion);
        final FieldMatrix<T> phi_rv = cwMatrices.getPhiRV();
        final RealMatrix phi_rv_Real = MatrixUtils.createRealMatrix(new double[][] { {phi_rv.getEntry(0, 0).getReal(), phi_rv.getEntry(0, 1).getReal(), phi_rv.getEntry(0, 2).getReal()},
                                                                                     {phi_rv.getEntry(1, 0).getReal(), phi_rv.getEntry(1, 1).getReal(), phi_rv.getEntry(1, 2).getReal()},
                                                                                     {phi_rv.getEntry(2, 0).getReal(), phi_rv.getEntry(2, 1).getReal(), phi_rv.getEntry(2, 2).getReal()}});

        final FieldMatrix<T> phi_rv_inv = Fieldifier.fieldify(phi_rv.getField(), MatrixUtils.inverse(phi_rv_Real));

        // Compute initial velocity required to reach desired final position at t = Δt.
        // v0_chaser_post_man = Φrv^(-1) * (final_chaser_pos - Φrr * initial_chaser_pos)
        final FieldVector3D<T> v0PostMan = new FieldVector3D<>(phi_rv_inv.operate(MatrixUtils.createColumnFieldMatrix(chaserPVTFinalLOF.getPosition().toArray()).subtract(cwMatrices.getPhiRR().multiply(MatrixUtils.createColumnFieldMatrix(chaserPVTInitialLOF.getPosition().toArray()))).getColumn(0)));

        // PVT of chaser after first maneuver
        final TimeStampedFieldPVCoordinates<T> chaserPVTInitialLOFPostMan = new TimeStampedFieldPVCoordinates<>(chaserPVTInitialLOF.getDate(), new FieldPVCoordinates<>(chaserPVTInitialLOF.getPosition(), v0PostMan));

        // Compute velocity at t = Δt on the transfer orbit.
        // Φvr * initial_chaser_pos_post_man + Φvv * initial_chaser_vel_post_man
        final FieldVector3D<T> v1PreMan = new FieldVector3D<>(cwMatrices.getPhiVR().multiply(MatrixUtils.createColumnFieldMatrix(chaserPVTInitialLOFPostMan.getPosition().toArray()))
                .add(cwMatrices.getPhiVV().multiply(MatrixUtils.createColumnFieldMatrix(chaserPVTInitialLOFPostMan.getVelocity().toArray()))).getColumn(0));

        // PVT of chaser before second maneuver
        final TimeStampedFieldPVCoordinates<T> chaserPVTFinalLOFPreMan = new TimeStampedFieldPVCoordinates<>(chaserPVTFinalLOF.getDate(), new FieldPVCoordinates<>(chaserPVTFinalLOF.getPosition(), v1PreMan));

        // compute ΔV vectors of maneuvers
        final FieldVector3D<T> deltaV1 = v0PostMan.subtract(chaserPVTInitialLOF.getVelocity());
        final FieldVector3D<T> deltaV2 = chaserPVTFinalLOF.getVelocity().subtract(v1PreMan);

        // Return transfer characteristics in target's QSW LOF
        return new FieldTwoImpulseTransfer<>(chaserPVTInitialLOFPostMan, chaserPVTFinalLOFPreMan, deltaV1, deltaV2, targetLof);
    }
}
