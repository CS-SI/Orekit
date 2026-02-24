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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
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



public class YamanakaAnkersenRendezVousTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Tests a transfer computation using the Yamanaka-Ankersen Equations.
     * Source for numerical values : Orbital Mechanics for Engineering Students, Curtis. Chap 7. Page 332.
     * Numerical values are coming from a Clohessy-Wiltshire RDV exercise given in QSW local orbital frame. Input data are converted to LVLH CCSDS and output data from
     * Yamanaka-AnkersenRendezVous computation are converted to QSW frame to be compared with the numerical values.
     */
    @Test
    public void testTransferComputation() {
        final double n = 0.00115697;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n), 1./3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate rdvDate = epoch.shiftedBy(8*3600);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.TRUE, PositionAngleType.TRUE,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Target's LVLH CCSDS LOF
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, new KeplerianPropagator(targetOrbit), "LVLH CCSDS LOF target");
        // Target's LVLH QSW LOF
        final LocalOrbitalFrame targetLofQSW = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit), "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's QSW LOF (Curtis book)
        TimeStampedPVCoordinates pvtChaserInitialQSW = new TimeStampedPVCoordinates(epoch, new Vector3D(20.0e3, 20.0e3, 20.0e3), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinalQSW = new TimeStampedPVCoordinates(rdvDate, Vector3D.ZERO, Vector3D.ZERO);

        //Conversion of transfer conditions to LVLH CCSDS frame to use computeRendezVous Method of YamanakaAnkersenRendezVous class.
        TimeStampedPVCoordinates pvtChaserInitial = targetLofQSW.getTransformTo(targetLof,pvtChaserInitialQSW.getDate()).transformPVCoordinates(pvtChaserInitialQSW);
        TimeStampedPVCoordinates pvtChaserFinal = targetLofQSW.getTransformTo(targetLof, pvtChaserFinalQSW.getDate()).transformPVCoordinates(pvtChaserFinalQSW);
        // Compute rendez-vous transfer using the Clohessy-Wiltshire equations
        final TwoImpulseTransfer rdvSolution = YamanakaAnkersenRendezVous.computeRendezVous(
                pvtChaserInitial,
                pvtChaserFinal,
                targetLof,
                targetOrbit,new KeplerianPropagator(targetOrbit));

        // Conversion of computeRendezVous output to QSW LOF to compare them with numerical values from Curtis.
        final TimeStampedPVCoordinates pvt1 = targetLof.getTransformTo(targetLofQSW, pvtChaserInitial.getDate()).transformPVCoordinates(rdvSolution.getPvt1());
        final TimeStampedPVCoordinates pvt2 = targetLof.getTransformTo(targetLofQSW, pvtChaserFinal.getDate()).transformPVCoordinates(rdvSolution.getPvt2());

        final Vector3D deltaV1 = targetLof.getTransformTo(targetLofQSW,pvtChaserInitial.getDate()).transformVector(rdvSolution.getDeltaV1());
        final Vector3D deltaV2 = targetLof.getTransformTo(targetLofQSW,pvtChaserInitial.getDate()).transformVector(rdvSolution.getDeltaV2());

        // Define reference values
        final TimeStampedPVCoordinates pvt1Ref = new TimeStampedPVCoordinates(new AbsoluteDate(2000, 1, 1, 11, 58, 55.816, TimeScalesFactory.getUTC()), new Vector3D(20000.0, 20000.0, 20000.0), new Vector3D(9.357511488320982, -46.75115105876742, 8.02971971420528));
        final TimeStampedPVCoordinates pvt2Ref = new TimeStampedPVCoordinates(new AbsoluteDate(2000, 1, 1, 19, 58, 55.816, TimeScalesFactory.getUTC()), new Vector3D(0.0, 0.0, 0.0), new Vector3D(-25.820815904440863, -0.4723510587678277, -24.493024130325324));

        final Vector3D deltaV1Ref = new Vector3D(29.35751148832098, -66.75115105876742, 13.02971971420528);
        final Vector3D deltaV2Ref = new Vector3D(25.820815904440863, 0.4723510587678277, 24.493024130325324);

        // Check that PVTs and ΔV vectors are correct
        Assertions.assertEquals(pvt1Ref.getDate().toDouble(), rdvSolution.getPvt1().getDate().toDouble(),1.0e-8);
        TestUtils.validateVector3D(pvt1Ref.getPosition(), pvt1.getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt1Ref.getVelocity(), pvt1.getVelocity(), NUMERICAL_TOLERANCE);

        Assertions.assertEquals(pvt2Ref.getDate().toDouble(), pvt2.getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2Ref.getPosition(), pvt2.getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2Ref.getVelocity(), pvt2.getVelocity(), NUMERICAL_TOLERANCE);

        TestUtils.validateVector3D(deltaV1Ref, deltaV1, NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(deltaV2Ref, deltaV2, NUMERICAL_TOLERANCE);
    }

}

