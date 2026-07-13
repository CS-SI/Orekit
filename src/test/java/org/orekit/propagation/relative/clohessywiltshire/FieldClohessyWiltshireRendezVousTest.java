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
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

class FieldClohessyWiltshireRendezVousTest {

    private static final double NUMERICAL_TOLERANCE = 1e-10;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Tests a transfer computation using the Clohessy-Wiltshire equations. Source for numerical values : Orbital
     * Mechanics for Engineering Students, Curtis
     */
    @Test
    void testTransferComputation() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.00115697);
        final Binary64 rTarget =
                        new Binary64(FastMath.pow(Constants.EIGEN5C_EARTH_MU / (n.getReal() * n.getReal()), 1. / 3.));
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> rdvDate = epoch.shiftedBy(8 * 3600);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(field, new KeplerianOrbit(
                        rTarget.getReal(), 0, 0, 0, 0, 0, PositionAngleType.MEAN, FramesFactory.getGCRF(),
                        epoch.toAbsoluteDate(), Constants.EIGEN5C_EARTH_MU));

        // Target's QSW LOF
        final LocalOrbitalFrame targetLof = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW,
                                                                  new KeplerianPropagator(targetOrbit.toOrbit()),
                                                                  "QSW LOF target");

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial =
                        new TimeStampedFieldPVCoordinates<>(epoch,
                                                            new FieldPVCoordinates<>(
                                                                            new FieldVector3D<>(
                                                                                            field,
                                                                                            new Vector3D(20.0e3,
                                                                                                         20.0e3,
                                                                                                         20.0e3)),
                                                                            new FieldVector3D<>(
                                                                                            field,
                                                                                            new Vector3D(-0.02e3,
                                                                                                         0.02e3,
                                                                                                         -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal =
                        new TimeStampedFieldPVCoordinates<>(rdvDate,
                                                            new FieldPVCoordinates<>(
                                                                            new FieldVector3D<>(
                                                                                            field,
                                                                                            Vector3D.ZERO),
                                                                            new FieldVector3D<>(
                                                                                            field,
                                                                                            Vector3D.ZERO)));

        // Compute rendez-vous transfer using the Clohessy_Wiltshire equations
        final FieldTwoImpulseTransfer<Binary64> rdvSolution =
                        (new FieldClohessyWiltshireRendezVous<Binary64>()).computeRendezVous(pvtChaserInitial,
                                                                                             pvtChaserFinal, targetLof,
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

        // CHeck that PVTs and ΔV vectors are correct
        Assertions.assertEquals(pvt1Ref.getDate().toDouble(),
                                rdvSolution.getPvt1().getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt1Ref.getPosition(), rdvSolution.getPvt1().getPosition(),
                                        NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt1Ref.getVelocity(), rdvSolution.getPvt1().getVelocity(),
                                        NUMERICAL_TOLERANCE);

        Assertions.assertEquals(pvt2Ref.getDate().toDouble(),
                                rdvSolution.getPvt2().getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt2Ref.getPosition(), rdvSolution.getPvt2().getPosition(),
                                        NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(pvt2Ref.getVelocity(), rdvSolution.getPvt2().getVelocity(),
                                        NUMERICAL_TOLERANCE);

        TestUtils.validateFieldVector3D(deltaV1Ref, rdvSolution.getDeltaV1(), NUMERICAL_TOLERANCE);
        TestUtils.validateFieldVector3D(deltaV2Ref, rdvSolution.getDeltaV2(), NUMERICAL_TOLERANCE);
    }
}
