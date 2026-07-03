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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

class ClohessyWiltshireRendezVousTest {

    // Tolerance for floating point numbers comparison
    private static final double NUMERICAL_TOLERANCE = 1.e-10;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testMatrixComputation() {
        // Radius and mean motion of target's orbit
        final double r_tgt = 6677.9933719419e3;// m
        final double n = FastMath.sqrt(Constants.EIGEN5C_EARTH_MU / FastMath.pow(r_tgt, 3));// rad/s

        // Compute matrices
        final ClohessyWiltshireMatrices matrices = ClohessyWiltshireEquations.computeMatrices(8 * 3600.0, n);

        TestUtils.validateRealMatrix(MatrixUtils.createRealMatrix(new double[][] {
                        {4.97868541238670, 0.00000000000000, 0.00000000000000},
                        {-194.242457498099, 1.00000000000000, 0.00000000000000},
                        {0.00000000000000, 0.00000000000000, -0.326228470795567}}),
                                     matrices.getPhiRR(), NUMERICAL_TOLERANCE);
        TestUtils.validateRealMatrix(MatrixUtils.createRealMatrix(new double[][] {
                        {817.081897951423, 2292.70633171889, 0.00000000000000},
                        {-2292.70633171889, -83131.6724081943, 0.00000000000000},
                        {0.00000000000000, 0.00000000000000, 817.081897951423}}),
                                     matrices.getPhiRV(), NUMERICAL_TOLERANCE);
        TestUtils.validateRealMatrix(MatrixUtils.createRealMatrix(new double[][] {
                        {0.00328085221475134, 0.00000000000000, 0.00000000000000},
                        {-0.00920596902838442, 0.00000000000000, 0.00000000000000},
                        {0.00000000000000, 0.00000000000000, -0.00109361740491711}}),
                                     matrices.getPhiVR(), NUMERICAL_TOLERANCE);
        TestUtils.validateRealMatrix(MatrixUtils.createRealMatrix(new double[][] {
                        {-0.326228470795567, 1.89058190496195, 0.00000000000000},
                        {-1.89058190496195, -4.30491388318227, 0.00000000000000},
                        {0.00000000000000, 0.00000000000000, -0.326228470795567}}),
                                     matrices.getPhiVV(), NUMERICAL_TOLERANCE);
    }

    /**
     * Tests a transfer computation using the Clohessy-Wiltshire equations. Source for numerical values : Orbital
     * Mechanics for Engineering Students, Curtis
     */
    @Test
    void testTransferComputation() {
        final double n = 0.00115697;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU / (n * n), 1. / 3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate rdvDate = epoch.shiftedBy(8 * 3600);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.MEAN,
                                                              FramesFactory.getGCRF(), epoch,
                                                              Constants.EIGEN5C_EARTH_MU);

        // Target's QSW LOF
        final LocalOrbitalFrame targetLof =
                        new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit),
                                              "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial =
                        new TimeStampedPVCoordinates(epoch, new Vector3D(20.0e3, 20.0e3, 20.0e3),
                                                     new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(rdvDate, Vector3D.ZERO, Vector3D.ZERO);

        // Compute rendez-vous transfer using the Clohessy-Wiltshire equations
        final TwoImpulseTransfer rdvSolution =
                        ClohessyWiltshireRendezVous.computeRendezVous(pvtChaserInitial, pvtChaserFinal, targetLof,
                                                                      targetOrbit);

        // Define reference values
        final TimeStampedPVCoordinates pvt1Ref = new TimeStampedPVCoordinates(
                        new AbsoluteDate(2000, 1, 1, 11, 58, 55.816, TimeScalesFactory.getUTC()),
                        new Vector3D(20000.0, 20000.0, 20000.0),
                        new Vector3D(9.357511488320982, -46.75115105876742, 8.02971971420528));
        final TimeStampedPVCoordinates pvt2Ref = new TimeStampedPVCoordinates(
                        new AbsoluteDate(2000, 1, 1, 19, 58, 55.816, TimeScalesFactory.getUTC()),
                        new Vector3D(0.0, 0.0, 0.0),
                        new Vector3D(-25.820815904440863, -0.4723510587678277, -24.493024130325324));

        final Vector3D deltaV1Ref = new Vector3D(29.35751148832098, -66.75115105876742, 13.02971971420528);
        final Vector3D deltaV2Ref = new Vector3D(25.820815904440863, 0.4723510587678277, 24.493024130325324);

        // Check that PVTs and ΔV vectors are correct
        Assertions.assertEquals(pvt1Ref.getDate().toDouble(), rdvSolution.getPvt1().getDate().toDouble(), 0);
        TestUtils.validateVector3D(pvt1Ref.getPosition(), rdvSolution.getPvt1().getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt1Ref.getVelocity(), rdvSolution.getPvt1().getVelocity(), NUMERICAL_TOLERANCE);

        Assertions.assertEquals(pvt2Ref.getDate().toDouble(), rdvSolution.getPvt2().getDate().toDouble(),
                                NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2Ref.getPosition(), rdvSolution.getPvt2().getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2Ref.getVelocity(), rdvSolution.getPvt2().getVelocity(), NUMERICAL_TOLERANCE);

        TestUtils.validateVector3D(deltaV1Ref, rdvSolution.getDeltaV1(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(deltaV2Ref, rdvSolution.getDeltaV2(), NUMERICAL_TOLERANCE);
    }

}
