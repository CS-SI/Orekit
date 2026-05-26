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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;


public class FieldYamanakaAnkersenRendezVousTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-10;

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
        final Binary64Field field = Binary64Field.getInstance();

        final double n = 0.00115697; // Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n), 1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> rdvDate = epoch.shiftedBy(8*3600);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(field,new KeplerianOrbit(rTarget, 0.0, 0.0, 0.0, 0.0, 0.0, PositionAngleType.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU));

        // Target's LVLH CCSDS LOF
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, new KeplerianPropagator(targetOrbit.toOrbit()), "LVLH CCSDS LOF target");
        // Target's LVLH QSW LOF
        final LocalOrbitalFrame targetLofQSW = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit.toOrbit()), "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's QSW LOF (Curtis book)
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitialQSW = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(20.0e3, 20.0e3, 20.0e3), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinalQSW = new TimeStampedFieldPVCoordinates<>(rdvDate, new FieldPVCoordinates<>(field, new PVCoordinates(Vector3D.ZERO, Vector3D.ZERO)));

        //Conversion of transfer conditions to LVLH CCSDS frame to use computeRendezVous Method of YamanakaAnkersenRendezVous class.
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = targetLofQSW.getTransformTo(targetLof,pvtChaserInitialQSW.getDate()).transformPVCoordinates(pvtChaserInitialQSW);
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = targetLofQSW.getTransformTo(targetLof, pvtChaserFinalQSW.getDate()).transformPVCoordinates(pvtChaserFinalQSW);
        // Compute rendez-vous transfer using the Clohessy-Wiltshire equations
        final FieldTwoImpulseTransfer<Binary64> rdvSolution = (new FieldYamanakaAnkersenRendezVous<Binary64>()).computeRendezVous(
                pvtChaserInitial,
                pvtChaserFinal,
                targetLof,
                targetOrbit,new FieldKeplerianPropagator<>(targetOrbit));

        // Conversion of computeRendezVous output to QSW LOF to compare them with numerical values from Curtis.
        final TimeStampedFieldPVCoordinates<Binary64> pvt1 = targetLof.getTransformTo(targetLofQSW, pvtChaserInitial.getDate()).transformPVCoordinates(rdvSolution.getPvt1());
        final TimeStampedFieldPVCoordinates<Binary64> pvt2 = targetLof.getTransformTo(targetLofQSW, pvtChaserFinal.getDate()).transformPVCoordinates(rdvSolution.getPvt2());

        final FieldVector3D<Binary64> deltaV1 = targetLof.getTransformTo(targetLofQSW,pvtChaserInitial.getDate()).transformVector(rdvSolution.getDeltaV1());
        final FieldVector3D<Binary64> deltaV2 = targetLof.getTransformTo(targetLofQSW,pvtChaserInitial.getDate()).transformVector(rdvSolution.getDeltaV2());

        // Define reference values
        final TimeStampedPVCoordinates pvt1Ref = new TimeStampedPVCoordinates(new AbsoluteDate(2000, 1, 1, 11, 58, 55.816, TimeScalesFactory.getUTC()), new Vector3D(20000.0, 20000.0, 20000.0), new Vector3D(9.357511488320982, -46.75115105876742, 8.02971971420528));
        final TimeStampedPVCoordinates pvt2Ref = new TimeStampedPVCoordinates(new AbsoluteDate(2000, 1, 1, 19, 58, 55.816, TimeScalesFactory.getUTC()), new Vector3D(0.0, 0.0, 0.0), new Vector3D(-25.820815904440863, -0.4723510587678277, -24.493024130325324));

        final Vector3D deltaV1Ref = new Vector3D(29.35751148832098, -66.75115105876742, 13.02971971420528);
        final Vector3D deltaV2Ref = new Vector3D(25.820815904440863, 0.4723510587678277, 24.493024130325324);

        // Check that PVTs and ΔV vectors are correct
        Assertions.assertEquals(pvt1Ref.getDate().toDouble(), rdvSolution.getPvt1().getDate().toAbsoluteDate().toDouble(),1.0e-8);
        TestUtils.validateFieldVector3D(pvt1Ref.getPosition(), pvt1.getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt1Ref.getVelocity(), pvt1.getVelocity(), NUMERICAL_TOLERANCE);

        Assertions.assertEquals(pvt2Ref.getDate().toDouble(), pvt2.getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt2Ref.getPosition(), pvt2.getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt2Ref.getVelocity(), pvt2.getVelocity(), NUMERICAL_TOLERANCE);

        TestUtils.validateFieldVector3D(deltaV1Ref, deltaV1, NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(deltaV2Ref, deltaV2, NUMERICAL_TOLERANCE);

    }


}
