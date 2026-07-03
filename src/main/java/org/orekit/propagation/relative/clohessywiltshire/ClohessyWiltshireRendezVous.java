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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.frames.Frame;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * <p>This class implements the solution to the Clohessy-Wiltshire equations for a two-impulse rendez-vous.
 * The Clohessy-Wiltshire equations have a closed-form solution which enables extremely fast rendez-vous computations
 * for circular target orbits, as long as the chaser is not too far from the target.</p>
 * <p>For more general rendez-vous transfer orbit computations, see
 * {@link org.orekit.estimation.iod.IodLambert IodLambert}.</p>
 *
 * @author Jérôme Tabeaud
 * @author Romain Cuvillon
 * @since 14.0
 */
public class ClohessyWiltshireRendezVous {

    /**
     * Private constructor to prevent instantiation.
     */
    private ClohessyWiltshireRendezVous() {
    }

    /**
     * Computes a two-impulse transfer between an initial and final positions using the Clohessy-Wiltshire closed-form
     * solution.
     *
     * @param chaserPVTInitial Initial chaser PVT expressed in provided frame
     * @param chaserPVTFinal   Final chaser PVT expressed in provided frame
     * @param chaserPVFrame    Frame in which the initial and final chaser PVT are expressed
     * @param targetOrbit      Target spacecraft's orbit
     * @return TwoImpulseTransfer
     */
    public static TwoImpulseTransfer computeRendezVous(final TimeStampedPVCoordinates chaserPVTInitial,
                                                       final TimeStampedPVCoordinates chaserPVTFinal,
                                                       final Frame chaserPVFrame, final Orbit targetOrbit) {
        // Create QSW local orbital frame of the target
        // Here a LocalOrbitalFrame of the target orbit is used since the transform has to be shifted
        final LocalOrbitalFrame targetLof =
                        new LocalOrbitalFrame(targetOrbit.getFrame(), ClohessyWiltshireProvider.LOF_TYPE,
                                              new KeplerianPropagator(targetOrbit),
                                              "QSW LOF target");

        // Transform input PVTs from the input frame to the target's LOF
        final TimeStampedPVCoordinates chaserPVTInitialLOF =
                        chaserPVFrame.getTransformTo(targetLof, chaserPVTInitial.getDate())
                                     .transformPVCoordinates(chaserPVTInitial);
        final TimeStampedPVCoordinates chaserPVTFinalLOF =
                        chaserPVFrame.getTransformTo(targetLof, chaserPVTFinal.getDate())
                                     .transformPVCoordinates(chaserPVTFinal);

        // Compute target's mean motion
        final double meanMotion = targetOrbit.getKeplerianMeanMotion();

        // Compute duration of the transfer from the initial and final states' dates
        final double deltaT = chaserPVTFinalLOF.getDate().durationFrom(chaserPVTInitialLOF.getDate());

        // Compute the Clohessy-Wiltshire matrices for the desired final time t
        final ClohessyWiltshireMatrices cwMatrices = ClohessyWiltshireEquations.computeMatrices(deltaT, meanMotion);

        // Compute initial velocity required to reach desired final position at t = Δt.
        // v0_chaser_post_man = Φrv^(-1) * (final_chaser_pos - Φrr * initial_chaser_pos)
        final RealMatrix phiRVInverse = MatrixUtils.inverse(cwMatrices.getPhiRV());
        final RealMatrix initialChaserPos =
                        MatrixUtils.createColumnRealMatrix(chaserPVTInitialLOF.getPosition().toArray());
        final RealMatrix finalChaserPos =
                        MatrixUtils.createColumnRealMatrix(chaserPVTFinalLOF.getPosition().toArray());
        final RealMatrix phiRRTimesP0 = cwMatrices.getPhiRR().multiply(initialChaserPos);
        final RealMatrix subtract = finalChaserPos.subtract(phiRRTimesP0);
        final Vector3D v0PostMan = new Vector3D(phiRVInverse.operate(subtract.getColumn(0)));

        // PVT of chaser after first maneuver
        final TimeStampedPVCoordinates chaserPVTInitialLOFPostMan =
                        new TimeStampedPVCoordinates(chaserPVTInitialLOF.getDate(), chaserPVTInitialLOF.getPosition(),
                                                     v0PostMan);

        // Compute velocity at t = Δt on the transfer orbit.
        // Φvr * initial_chaser_pos_post_man + Φvv * initial_chaser_vel_post_man
        final RealMatrix chaserP =
                        MatrixUtils.createColumnRealMatrix(chaserPVTInitialLOFPostMan.getPosition().toArray());
        final RealMatrix chaserV =
                        MatrixUtils.createColumnRealMatrix(chaserPVTInitialLOFPostMan.getVelocity().toArray());
        final Vector3D v1PreMan =
                        new Vector3D(cwMatrices.getPhiVR().multiply(chaserP)
                                               .add(cwMatrices.getPhiVV().multiply(chaserV))
                                               .getColumn(0));

        // PVT of chaser before second maneuver
        final TimeStampedPVCoordinates chaserPVTFinalLOFPreMan =
                        new TimeStampedPVCoordinates(chaserPVTFinalLOF.getDate(), chaserPVTFinalLOF.getPosition(),
                                                     v1PreMan);

        // Compute ΔV vectors of maneuvers
        final Vector3D deltaV1 = v0PostMan.subtract(chaserPVTInitialLOF.getVelocity());
        final Vector3D deltaV2 = chaserPVTFinalLOF.getVelocity().subtract(v1PreMan);

        // Return transfer characteristics in target's QSW LOF
        return new TwoImpulseTransfer(chaserPVTInitialLOFPostMan, chaserPVTFinalLOFPreMan, deltaV1, deltaV2, targetLof);
    }
}
