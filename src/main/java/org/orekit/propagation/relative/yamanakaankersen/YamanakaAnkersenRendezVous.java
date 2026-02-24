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
package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.analysis.function.Power;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.linear.RealVector;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.utils.TimeStampedPVCoordinates;


/**
 * <p>This class implements the solution to the Yamanaka-Ankersen equations for a two-impulse rendez-vous as presented in the Ankersen thesis.
 * Ref : "Guidance, Navigation, Control and Relative Dynamics for Spacecraft Proximity Maneuvers",Ph .D Thesis, Finn Ankersen, 2010, see Chapter 4.6
 * Local Orbital Frame for use of Yamanaka-Ankersen Equations is LVLH CCSDS.
 *
 * <p>For more general rendez-vous transfer orbit computations, see {@link org.orekit.estimation.iod.IodLambert IodLambert}.</p>
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public class YamanakaAnkersenRendezVous {

    /**
     * Private constructor to prevent instantiation.
     */
    private YamanakaAnkersenRendezVous() {
    }

    /**
     * Computes rho as in the reference paper.
     * @param theta target true anomaly.
     * @param targetE eccentricity of the target.
     * @return rho.
     */
    private static double rho(final double theta, final double targetE) { return 1 + targetE * FastMath.cos(theta); }

    /**
     * Computes a two-impulse transfer between an initial and final positions using the Clohessy-Wiltshire closed-form solution.
     *
     * @param chaserPVTInitial Initial chaser PVT expressed in provided frame.
     * @param chaserPVTFinal   Final chaser PVT expressed in provided frame.
     * @param chaserPVFrame    Frame in which the initial and final chaser PVT are expressed.
     * @param targetOrbit      Target spacecraft's orbit.
     * @param propagator       Propagator used to compute the final state of the target to get the final True Anomaly.
     * @return TwoImpulseTransfer
     */
    public static TwoImpulseTransfer computeRendezVous(final TimeStampedPVCoordinates chaserPVTInitial, final TimeStampedPVCoordinates chaserPVTFinal, final Frame chaserPVFrame, final Orbit targetOrbit, final Propagator propagator) {
        // create LVLH CCSDS local orbital frame of the target
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, propagator, "LVLH CCSDS LOF target");

        // Transform input PVTs from the input frame to the target's LOF
        final TimeStampedPVCoordinates chaserPVTInitialLof = chaserPVFrame.getTransformTo(targetLof, chaserPVTInitial.getDate()).transformPVCoordinates(chaserPVTInitial);
        final TimeStampedPVCoordinates chaserPVTFinalLof = chaserPVFrame.getTransformTo(targetLof, chaserPVTFinal.getDate()).transformPVCoordinates(chaserPVTFinal);

        // Compute duration of the transfer from the initial and final states dates
        final double deltaT = chaserPVTFinalLof.getDate().durationFrom(chaserPVTInitialLof.getDate());

        final KeplerianOrbit initialTargetOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(targetOrbit);
        final double initialTrueAnomaly = initialTargetOrbit.getTrueAnomaly();
        final SpacecraftState propagated = propagator.propagate(chaserPVTFinal.getDate());
        final KeplerianOrbit finalTargetOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(propagated.getOrbit());
        final double finalTrueAnomaly = finalTargetOrbit.getTrueAnomaly();
        final double E = finalTargetOrbit.getE();

        // Compute the Yamanaka-Ankersen matrices for the desired final time t
        final YamanakaAnkersenMatrices yaMatrices = YamanakaAnkersenEquations.computeMatrices(deltaT, finalTargetOrbit.getA(), E, initialTrueAnomaly, finalTrueAnomaly, finalTargetOrbit.getMu());

        // Compute out of plane and in plane transfer matrices from Time to True Anomaly coordinates.
        final Power p = new Power((double) 1 / 4);
        final double k = p.value(finalTargetOrbit.getMu() / FastMath.pow(finalTargetOrbit.getA(), 3));

        // Compute outPlane transfer matrix for initialTrueAnomaly
        final double[][] outPlaneTransferMatrix = { {rho(initialTrueAnomaly, E), 0.},
                                                    {-E * FastMath.sin(initialTrueAnomaly), 1 / (FastMath.pow(k, 2) * rho(initialTrueAnomaly, E))}};
        // Compute outPlane inverse transfer matrix for finalTrueAnomaly
        final double[][] outPlaneInvTransferMatrix = { {1 / rho(finalTrueAnomaly, E), 0},
                                                       {E * FastMath.pow(k, 2) * FastMath.sin(finalTrueAnomaly), FastMath.pow(k, 2) * rho(finalTrueAnomaly, E)}};
        // Compute inPlane transfer matrix for initialTrueAnomaly
        final double[][] inPlaneTransferMatrix = { {rho(initialTrueAnomaly, E), 0., 0., 0.},
                                                   {0., rho(initialTrueAnomaly, E), 0., 0.},
                                                   {-E * FastMath.sin(initialTrueAnomaly), 0., 1 / (FastMath.pow(k, 2) * rho(initialTrueAnomaly, E)), 0.},
                                                   {0., -E * FastMath.sin(initialTrueAnomaly), 0., 1 / (FastMath.pow(k, 2) * rho(initialTrueAnomaly, E))}};
        // Compute inPlane inverse transfer matrix for finalTrueAnomaly
        final double[][] inPlaneInvTransferMatrix = { {1 / rho(finalTrueAnomaly, E), 0., 0., 0.},
                                                      {0., 1 / rho(finalTrueAnomaly, E), 0., 0.},
                                                      {E * FastMath.pow(k, 2) * FastMath.sin(finalTrueAnomaly), 0., FastMath.pow(k, 2) * rho(finalTrueAnomaly, E), 0.},
                                                      {0., E * FastMath.pow(k, 2) * FastMath.sin(finalTrueAnomaly), 0., FastMath.pow(k, 2) * rho(finalTrueAnomaly, E)}};
        // Compute out of plane (y_axis) initial velocity required to reach desired final position at t = Δt.
        final double vy0 = FastMath.pow(k, 2) * rho(initialTrueAnomaly, E) * (rho(finalTrueAnomaly, E) * chaserPVTFinalLof.getPosition().getY() - (FastMath.cos(finalTrueAnomaly - initialTrueAnomaly) + E * FastMath.cos(finalTrueAnomaly)) * chaserPVTInitialLof.getPosition().getY()) / FastMath.sin(finalTrueAnomaly - initialTrueAnomaly);
        // Compute out of plane (y_axis) velocity at t = Δt on the transfer orbit.
        final RealVector yCoordsPostMan = MatrixUtils.createRealVector(new double[] {chaserPVTInitialLof.getPosition().getY(), vy0});
        final double vyf = MatrixUtils.createRealMatrix(outPlaneInvTransferMatrix).multiply(yaMatrices.getOutPlaneMatrix()).multiply(MatrixUtils.createRealMatrix(outPlaneTransferMatrix)).operate(yCoordsPostMan).getEntry(1);

        // Compute in plane (x and z axis) initial velocities required to reach desired final position at t = Δt.

        final RealMatrix transferMatrix = MatrixUtils.createRealMatrix(inPlaneInvTransferMatrix).multiply(yaMatrices.getInPlaneMatrix().multiply(MatrixUtils.createRealMatrix(inPlaneTransferMatrix)));
        final double d11 = transferMatrix.getEntry(0, 0);
        final double d12 = transferMatrix.getEntry(0, 1);
        final double d13 = transferMatrix.getEntry(0, 2);
        final double d14 = transferMatrix.getEntry(0, 3);
        final double d21 = transferMatrix.getEntry(1, 0);
        final double d22 = transferMatrix.getEntry(1, 1);
        final double d23 = transferMatrix.getEntry(1, 2);
        final double d24 = transferMatrix.getEntry(1, 3);

        final double x0 = chaserPVTInitialLof.getPosition().getX();
        final double z0 = chaserPVTInitialLof.getPosition().getZ();
        final double xf = chaserPVTFinalLof.getPosition().getX();
        final double zf = chaserPVTFinalLof.getPosition().getZ();
        final double vx0 = (d14 * zf + (d24 * d11 - d21 * d14) * x0 + (d24 * d12 - d22 * d14) * z0 - d24 * xf) / (d23 * d14 - d24 * d13);
        final double vz0 = (d23 * xf - d13 * zf + (d13 * d21 - d11 * d23) * x0 + (d13 * d22 - d12 * d23) * z0) / (d23 * d14 - d24 * d13);

        // Compute in plane (x and z axis) velocities at t = Δt on the transfer orbit.
        final RealVector xzCoordsPostMan = MatrixUtils.createRealVector(new double[] {x0, z0, vx0, vz0});
        final double vxf = transferMatrix.operate(xzCoordsPostMan).getEntry(2);
        final double vzf = transferMatrix.operate(xzCoordsPostMan).getEntry(3);

        final Vector3D v0PostMan = new Vector3D(vx0, vy0, vz0);
        final Vector3D vfPreMan = new Vector3D(vxf, vyf, vzf);

        // PVT of chaser after first maneuver
        final TimeStampedPVCoordinates chaserPVTInitialLOFPostMan = new TimeStampedPVCoordinates(chaserPVTInitialLof.getDate(), chaserPVTInitialLof.getPosition(), v0PostMan);

        // PVT of chaser before second maneuver
        final TimeStampedPVCoordinates chaserPVTFinalLofPreMan = new TimeStampedPVCoordinates(chaserPVTFinalLof.getDate(), chaserPVTFinalLof.getPosition(), vfPreMan);
        // Compute ΔV vectors of maneuvers
        final Vector3D deltaV1 = v0PostMan.subtract(chaserPVTInitialLof.getVelocity());
        final Vector3D deltaV2 = chaserPVTFinalLof.getVelocity().subtract(vfPreMan);


        // Return transfer characteristics in target's LVLH CCSDS
        return new TwoImpulseTransfer(
                chaserPVTInitialLOFPostMan,
                chaserPVTFinalLofPreMan,
                deltaV1,
                deltaV2,
                targetLof);
    }
}


