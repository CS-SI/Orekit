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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.linear.FieldMatrix;
import org.hipparchus.linear.FieldVector;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.orekit.frames.Frame;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/**
 * <p>This class implements the solution to the Yamanaka-Ankersen equations for a two-impulse rendez-vous as presented
 * in the Ankersen thesis.
 * Ref : "Guidance, Navigation, Control and Relative Dynamics for Spacecraft Proximity Maneuvers",Ph .D Thesis, Finn
 * Ankersen, 2010, see Chapter 4.6 Local Orbital Frame for use of Yamanaka-Ankersen Equations is LVLH CCSDS.
 * <p>For more general rendez-vous transfer orbit computations, see
 * {@link org.orekit.estimation.iod.IodLambert IodLambert}.</p>
 *
 * @param <T> Any scalar field.
 * @author Romain Cuvillon
 * @since 14.0
 */
public class FieldYamanakaAnkersenRendezVous<T extends CalculusFieldElement<T>> {

    public FieldYamanakaAnkersenRendezVous() {
    }

    /**
     * Computes a two-impulse transfer between an initial and final positions using the Yamanaka-Ankersen closed-form
     * solution.
     *
     * @param chaserPVTInitial Initial chaser PVT expressed in provided frame.
     * @param chaserPVTFinal   Final chaser PVT expressed in provided frame.
     * @param chaserPVFrame    Frame in which the initial and final chaser PVT are expressed.
     * @param targetOrbit      Target spacecraft's orbit.
     * @param propagator       Propagator used to compute the final state of the target to get the final True Anomaly.
     * @return TwoImpulseTransfer
     */
    public FieldTwoImpulseTransfer<T> computeRendezVous(final TimeStampedFieldPVCoordinates<T> chaserPVTInitial,
                                                        final TimeStampedFieldPVCoordinates<T> chaserPVTFinal,
                                                        final Frame chaserPVFrame, final FieldOrbit<T> targetOrbit,
                                                        final FieldPropagator<T> propagator) {
        // create LVLH CCSDS local orbital frame of the target
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS,
                                                                  new KeplerianPropagator(targetOrbit.toOrbit()),
                                                                  "LVLH CCSDS LOF");

        // Transform input PVTs from the input frame to the target's LOF
        final TimeStampedFieldPVCoordinates<T> chaserPVTInitialLof =
                        chaserPVFrame.getTransformTo(targetLof, chaserPVTInitial.getDate())
                                     .transformPVCoordinates(chaserPVTInitial);
        final TimeStampedFieldPVCoordinates<T> chaserPVTFinalLof =
                        chaserPVFrame.getTransformTo(targetLof, chaserPVTFinal.getDate())
                                     .transformPVCoordinates(chaserPVTFinal);

        // Compute duration of the transfer from the initial and final states dates
        final T deltaT = chaserPVTFinalLof.getDate().durationFrom(chaserPVTInitialLof.getDate());

        final FieldKeplerianOrbit<T> initialTargetOrbit =
                        (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(targetOrbit);
        final T initialTrueAnomaly = initialTargetOrbit.getTrueAnomaly();
        final FieldSpacecraftState<T> propagated = propagator.propagate(chaserPVTFinal.getDate());
        final FieldKeplerianOrbit<T> finalTargetOrbit =
                        (FieldKeplerianOrbit<T>) OrbitType.KEPLERIAN.convertType(propagated.getOrbit());
        final T finalTrueAnomaly = finalTargetOrbit.getTrueAnomaly();

        final T e = finalTargetOrbit.getE();
        final T zero = e.getField().getZero();
        final T one = e.getField().getOne();

        // Compute the Yamanaka-Ankersen matrices for the desired final time t
        final FieldYamanakaAnkersenMatrices<T> yaMatrices =
                        (new FieldYamanakaAnkersenEquations<T>()).computeMatrices(deltaT, finalTargetOrbit.getA(), e,
                                                                                  initialTrueAnomaly, finalTrueAnomaly,
                                                                                  finalTargetOrbit.getMu());
        // Compute out of plane and in plane transfer matrices from Time to True Anomaly coordinates
        final T p = finalTargetOrbit.getA().multiply(finalTargetOrbit.getA().getField().getOne().subtract(e.pow(2)));
        final T k2 = finalTargetOrbit.getMu().divide(p.pow(3)).sqrt();

        // Intermediary quantities
        final FieldSinCos<T> sinCosNu  = FastMath.sinCos(finalTrueAnomaly);
        final FieldSinCos<T> sinCosNu0 = FastMath.sinCos(initialTrueAnomaly);

        final T rho = e.multiply(sinCosNu.cos()).add(1.);
        final T rho0 = e.multiply(sinCosNu0.cos()).add(1.);

        final T k2rho = k2.multiply(rho);
        final T k2rho0 = k2.multiply(rho0);

        final T k2esinNu = k2.multiply(e).multiply(sinCosNu.sin());

        final T esinNu0 = e.multiply(sinCosNu0.sin());

        // Compute outPlane transfer matrix for initialTrueAnomaly
        final T[][] outPlaneTransferMatrix = MathArrays.buildArray(e.getField(), 2, 2);
        outPlaneTransferMatrix[0][0] = rho0;
        outPlaneTransferMatrix[0][1] = zero;
        outPlaneTransferMatrix[1][0] = esinNu0.multiply(-1.);
        outPlaneTransferMatrix[1][1] = one.divide(k2rho0);

        // Compute outPlane inverse transfer matrix for finalTrueAnomaly
        final T[][] outPlaneInvTransferMatrix = MathArrays.buildArray(e.getField(), 2, 2);
        outPlaneInvTransferMatrix[0][0] = one.divide(rho);
        outPlaneInvTransferMatrix[0][1] = zero;
        outPlaneInvTransferMatrix[1][0] = k2esinNu;
        outPlaneInvTransferMatrix[1][1] = k2rho;

        // Compute inPlane transfer matrix for initialTrueAnomaly
        final T[][] inPlaneTransferMatrix = MathArrays.buildArray(e.getField(), 4, 4);
        inPlaneTransferMatrix[0][0] = rho0;
        inPlaneTransferMatrix[0][1] = zero;
        inPlaneTransferMatrix[0][2] = zero;
        inPlaneTransferMatrix[0][3] = zero;
        inPlaneTransferMatrix[1][0] = zero;
        inPlaneTransferMatrix[1][1] = rho0;
        inPlaneTransferMatrix[1][2] = zero;
        inPlaneTransferMatrix[1][3] = zero;
        inPlaneTransferMatrix[2][0] = esinNu0.multiply(-1.);
        inPlaneTransferMatrix[2][1] = zero;
        inPlaneTransferMatrix[2][2] = one.divide(k2rho0);
        inPlaneTransferMatrix[2][3] = zero;
        inPlaneTransferMatrix[3][0] = zero;
        inPlaneTransferMatrix[3][1] = esinNu0.multiply(-1.);
        inPlaneTransferMatrix[3][2] = zero;
        inPlaneTransferMatrix[3][3] = one.divide(k2rho0);

        // Compute inPlane inverse transfer matrix for finalTrueAnomaly
        final T[][] inPlaneInvTransferMatrix = MathArrays.buildArray(e.getField(), 4, 4);
        inPlaneInvTransferMatrix[0][0] = one.divide(rho);
        inPlaneInvTransferMatrix[0][1] = zero;
        inPlaneInvTransferMatrix[0][2] = zero;
        inPlaneInvTransferMatrix[0][3] = zero;
        inPlaneInvTransferMatrix[1][0] = zero;
        inPlaneInvTransferMatrix[1][1] = one.divide(rho);
        inPlaneInvTransferMatrix[1][2] = zero;
        inPlaneInvTransferMatrix[1][3] = zero;
        inPlaneInvTransferMatrix[2][0] = k2esinNu;
        inPlaneInvTransferMatrix[2][1] = zero;
        inPlaneInvTransferMatrix[2][2] = k2rho;
        inPlaneInvTransferMatrix[2][3] = zero;
        inPlaneInvTransferMatrix[3][0] = zero;
        inPlaneInvTransferMatrix[3][1] = k2rho;
        inPlaneInvTransferMatrix[3][2] = zero;
        inPlaneInvTransferMatrix[3][3] = k2rho;

        // Compute out of plane (y_axis) initial velocity required to reach desired final position at t = Δt.
        final T vy0 = k2rho0.multiply(rho.multiply(chaserPVTFinalLof.getPosition().getY())
                                         .subtract(finalTrueAnomaly.subtract(initialTrueAnomaly).cos()
                                                                   .add(e.multiply(sinCosNu.cos()))
                                                                   .multiply(chaserPVTInitialLof.getPosition()
                                                                                                .getY())))
                            .divide((finalTrueAnomaly.subtract(initialTrueAnomaly)).sin());

        // Compute out of plane (y_axis) velocity at t = Δt on the transfer orbit.
        final T[] yCoordsPostManArray = MathArrays.buildArray(e.getField(), 2);
        yCoordsPostManArray[0] = chaserPVTInitialLof.getPosition().getY();
        yCoordsPostManArray[1] = vy0;
        final FieldVector<T> yCoordsPostMan = MatrixUtils.createFieldVector(yCoordsPostManArray);
        final T vyf = MatrixUtils.createFieldMatrix(outPlaneInvTransferMatrix).multiply(yaMatrices.getOutPlaneMatrix())
                                 .multiply(MatrixUtils.createFieldMatrix(outPlaneTransferMatrix))
                                 .operate(yCoordsPostMan).getEntry(1);

        // Compute in plane (x and z axis) initial velocities required to reach desired final position at t = Δt.
        final FieldMatrix<T> transferMatrix = MatrixUtils.createFieldMatrix(inPlaneInvTransferMatrix)
                                                         .multiply(yaMatrices.getInPlaneMatrix()
                                                                             .multiply(MatrixUtils.createFieldMatrix(
                                                                                             inPlaneTransferMatrix)));
        final T d11 = transferMatrix.getEntry(0, 0);
        final T d12 = transferMatrix.getEntry(0, 1);
        final T d13 = transferMatrix.getEntry(0, 2);
        final T d14 = transferMatrix.getEntry(0, 3);
        final T d21 = transferMatrix.getEntry(1, 0);
        final T d22 = transferMatrix.getEntry(1, 1);
        final T d23 = transferMatrix.getEntry(1, 2);
        final T d24 = transferMatrix.getEntry(1, 3);

        final T x0 = chaserPVTInitialLof.getPosition().getX();
        final T z0 = chaserPVTInitialLof.getPosition().getZ();
        final T xf = chaserPVTFinalLof.getPosition().getX();
        final T zf = chaserPVTFinalLof.getPosition().getZ();
        final T vx0 = d14.multiply(zf).add(d24.multiply(d11).subtract(d21.multiply(d14)).multiply(x0)
                                              .add(d24.multiply(d12).subtract(d22.multiply(d14)).multiply(z0))
                                              .subtract(d24.multiply(xf)))
                         .divide(d23.multiply(d14).subtract(d24.multiply(d13)));
        final T vz0 = d23.multiply(xf).subtract(d13.multiply(zf))
                         .add(d13.multiply(d21).subtract(d11.multiply(d23)).multiply(x0))
                         .add(d13.multiply(d22).subtract(d12.multiply(d23)).multiply(z0))
                         .divide(d23.multiply(d14).subtract(d24.multiply(d13)));

        // Compute in plane (x and z axis) velocities at t = Δt on the transfer orbit.
        final T[] xzCoordsPostManArray = MathArrays.buildArray(e.getField(), 4);
        xzCoordsPostManArray[0] = x0;
        xzCoordsPostManArray[1] = z0;
        xzCoordsPostManArray[2] = vx0;
        xzCoordsPostManArray[3] = vz0;
        final FieldVector<T> xzCoordsPostMan = MatrixUtils.createFieldVector(xzCoordsPostManArray);
        final T vxf = transferMatrix.operate(xzCoordsPostMan).getEntry(2);
        final T vzf = transferMatrix.operate(xzCoordsPostMan).getEntry(3);

        final FieldVector3D<T> v0PostMan = new FieldVector3D<>(vx0, vy0, vz0);
        final FieldVector3D<T> vfPreMan = new FieldVector3D<>(vxf, vyf, vzf);

        // PVT of chaser after first maneuver
        final TimeStampedFieldPVCoordinates<T> chaserPVTInitialLofPostMan =
                        new TimeStampedFieldPVCoordinates<>(chaserPVTInitialLof.getDate(),
                                                            new FieldPVCoordinates<>(chaserPVTInitialLof.getPosition(),
                                                                                     v0PostMan));

        // PVT of chaser before second maneuver
        final TimeStampedFieldPVCoordinates<T> chaserPVTFinalLofPreMan =
                        new TimeStampedFieldPVCoordinates<>(chaserPVTFinalLof.getDate(),
                                                            new FieldPVCoordinates<>(chaserPVTFinalLof.getPosition(),
                                                                                     vfPreMan));
        // Compute ΔV vectors of maneuvers
        final FieldVector3D<T> deltaV1 = v0PostMan.subtract(chaserPVTInitialLof.getVelocity());
        final FieldVector3D<T> deltaV2 = chaserPVTFinalLof.getVelocity().subtract(vfPreMan);

        // Return transfer characteristics in target's LVLH CCSDS
        return new FieldTwoImpulseTransfer<>(chaserPVTInitialLofPostMan, chaserPVTFinalLofPreMan, deltaV1, deltaV2,
                                             targetLof);
    }
}
