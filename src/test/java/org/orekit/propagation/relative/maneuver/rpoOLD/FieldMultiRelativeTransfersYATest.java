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
package org.orekit.propagation.relative.maneuver.rpoOLD;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.propagation.relative.FieldTwoImpulseTransfer;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.relative.maneuver.FieldYamanakaAnkersenManeuver;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class FieldMultiRelativeTransfersYATest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    // Tests the Field multi relative transfer on the linear case.
    @Test
    public void FieldMultiLinearTransfersTest(){
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));
        // Initial and final position of the chaser on the linear path.
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch,new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-1000,600,200), new Vector3D(0,0,0))));
        final TimeStampedFieldPVCoordinates<Binary64> finalPVT = new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000),new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,0,0), new Vector3D(10,10,10))));
        // Computation of the linear Waypoints.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeLinearWaypoints(initialPVT, finalPVT,11);
        // Computation of the transfers along the linear path.
        final List<FieldTwoImpulseTransfer<Binary64>> transfers = (new FieldMultiRelativeTransfersYA<>(waypoints, targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeMultiRelativeTransfers();
        // Assert start and end point of the transfers are the same as the corresponding waypoints.
        for (int i = 0; i<transfers.size(); i++) {
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getPosition().toVector3D(),waypoints.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getVelocity().toVector3D(),waypoints.get(i).getVelocity().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getPosition().toVector3D(),waypoints.get(i+1).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getVelocity().toVector3D(),waypoints.get(i+1).getVelocity().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void FieldForcedCircularTransfersTest(){
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.1), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));
        // Initial and final position of the chaser on the linear path.
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch,new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-1000,600,200), new Vector3D(0,0,0))));
        // Computation of the linear Waypoints.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(initialPVT,new FieldVector3D<>(field,Vector3D.ZERO),new Binary64(100),field.getZero(),field.getZero(),new Binary64(1000),10,1,11,new Binary64(1000));
        // Computation of the transfers along the linear path.
        final List<FieldTwoImpulseTransfer<Binary64>> transfers = (new FieldMultiRelativeTransfersYA<>(waypoints,targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeMultiRelativeTransfers();
        // Assert start and end point of the transfers are the same as the corresponding waypoints.
        for (int i = 0; i<transfers.size(); i++) {
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getPosition().toVector3D(),waypoints.get(i).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getVelocity().toVector3D(),waypoints.get(i).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getPosition().toVector3D(),waypoints.get(i+1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getVelocity().toVector3D(),waypoints.get(i+1).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
        }
    }

    //Keplerian propagation of a linear transfer using Yamanaka-Ankersen maneuver.
    //Test if the chaser position at the end of the propagation is the same as the final position on linear path.
    @Test
    public void FieldLinearTransferPropagationYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.1), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));

        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<FieldYamanakaAnkersenManeuver<Binary64>> maneuvers = (new FieldMultiRelativeTransfersYA<>(waypoints, targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeRelativeManeuvers(yaProvider);
        for (FieldYamanakaAnkersenManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(finalDate.shiftedBy(1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0].getReal(),pvtChaserFinal.getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1].getReal(),pvtChaserFinal.getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2].getReal(),pvtChaserFinal.getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[3].getReal(),pvtChaserFinal.getVelocity().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[4].getReal(),pvtChaserFinal.getVelocity().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[5].getReal(),pvtChaserFinal.getVelocity().getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a Forced Circular transfer using Yamanaka-Ankersen maneuver.
    // Test if the chaser position at the end of the propagation (3 relative orbit's periods) is the same as the first waypoint on the circle.
    @Test
    public void FieldForcedCircularPropagationTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        // Chaser PVCoordinates provider.
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(pvtChaserInitial,new FieldVector3D<>(field, Vector3D.ZERO),new Binary64(100),new Binary64(0),new Binary64(0),new Binary64(1000),10,3,6,new Binary64(500));
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<FieldYamanakaAnkersenManeuver<Binary64>> maneuvers = (new FieldMultiRelativeTransfersYA<>(waypoints, targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeRelativeManeuvers(yaProvider);
        for (FieldYamanakaAnkersenManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the scenario (500s of linear duration + 3 orbits of 1000s period).
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(epoch.shiftedBy(3500 + 1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0].getReal(),waypoints.get(5).getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1].getReal(),waypoints.get(5).getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2].getReal(),waypoints.get(5).getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[3].getReal(),waypoints.get(5).getVelocity().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[4].getReal(),waypoints.get(5).getVelocity().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[5].getReal(),waypoints.get(5).getVelocity().getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a Natural Circumnavigation transfer using Yamanaka-Ankersen maneuver.
    // Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
    @Test
    public void FieldNaturalCircumnavigationYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        // Chaser PVCoordinates provider.
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 relativeSemiMinorAxis = new Binary64(50);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeCircularNaturalCircumnavigationWaypoints(
                pvtChaserInitial, injectionDate, n, relativeSemiMinorAxis, new Binary64(0.3), 10);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<FieldYamanakaAnkersenManeuver<Binary64>> maneuvers = (new FieldMultiRelativeTransfersYA<>(waypoints, targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeRelativeManeuvers(yaProvider);
        for (FieldYamanakaAnkersenManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(injectionDate.shiftedBy(n.getPi().multiply(2).divide(n)));
        final Binary64[] chaserFinal = yaProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(0, -2 * semiMinorAxis, 0)
        Assertions.assertEquals(-2 * relativeSemiMinorAxis.getReal(), chaserFinal[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2].getReal(), NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a Natural Circular Relative Orbit transfer using Yamanaka-Ankersen maneuver.
    // Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
    @Test
    public void FieldNaturalCircularMotionYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        // Chaser PVCoordinates provider.
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 radius = new Binary64(50);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeNaturalCircularWaypoints(
                pvtChaserInitial, injectionDate, n, radius, 10);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<FieldYamanakaAnkersenManeuver<Binary64>> maneuvers = (new FieldMultiRelativeTransfersYA<>(waypoints, targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeRelativeManeuvers(yaProvider);
        for (FieldYamanakaAnkersenManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(injectionDate.shiftedBy(n.getPi().multiply(2).divide(n)));
        final Binary64[] chaserFinal = yaProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(0, -radius, 0)
        Assertions.assertEquals(-radius.getReal(), chaserFinal[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2].getReal(), NUMERICAL_TOLERANCE);
    }


    @Test
    public void getWaypointsTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));

        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final FieldMultiRelativeTransfersYA<Binary64> transfers = new FieldMultiRelativeTransfersYA<>(waypoints,targetOrbit, new FieldKeplerianPropagator<>(targetOrbit));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsGetters = transfers.getWaypoints();
        for (int i = 0; i < waypoints.size(); i++) {
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),waypointsGetters.get(i).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(),waypointsGetters.get(i).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),waypointsGetters.get(i).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void getTargetOrbitTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));

        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final FieldMultiRelativeTransfersYA<Binary64> transfers = new FieldMultiRelativeTransfersYA<>(waypoints,targetOrbit, new FieldKeplerianPropagator<>(targetOrbit));
        final FieldKeplerianOrbit<Binary64> targetTransfer = (FieldKeplerianOrbit<Binary64>) OrbitType.KEPLERIAN.convertType(transfers.getTargetOrbit());
        Assertions.assertEquals(targetOrbit.getA().getReal(),targetTransfer.getA().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getE().getReal(),targetTransfer.getE().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getI().getReal(),targetTransfer.getI().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getPerigeeArgument().getReal(),targetTransfer.getPerigeeArgument().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getRightAscensionOfAscendingNode().getReal(),targetTransfer.getRightAscensionOfAscendingNode().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getTrueAnomaly().getReal(),targetTransfer.getTrueAnomaly().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getKeplerianPeriod().getReal(),targetTransfer.getKeplerianPeriod().getReal(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void getLofType() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));

        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final FieldMultiRelativeTransfersYA<Binary64> transfers = new FieldMultiRelativeTransfersYA<>(waypoints,targetOrbit,new FieldKeplerianPropagator<>(targetOrbit));
        Assertions.assertEquals(LOFType.LVLH_CCSDS.getName(),transfers.getLofType().getName());
    }
    // Test the computeMultiRelativeTransfers(Frame frame) method and compare it with the one in the Clohessy-Wiltshire Frame.
    @Test
    public void computeMultiRelativeTransferFromOtherFrame() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = (new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n)).pow(1./3.);// Target orbit's radius
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, new Binary64(0.0), new Binary64(0.0),
                new Binary64(0.0), new Binary64(0.0), new Binary64(0.0),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));

        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        // Computation of the transfers along the linear path.
        final List<FieldTwoImpulseTransfer<Binary64>> transfersFromLVLH = (new FieldMultiRelativeTransfersYA<>(waypoints,targetOrbit, new FieldKeplerianPropagator<>(targetOrbit))).computeMultiRelativeTransfers();
        // Transform the waypoints in the LVLH frame and compute the transfers.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsQSW = new ArrayList<>();
        final LocalOrbitalFrame qsw = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, targetOrbit.toOrbit(),LOFType.QSW.getName());
        final LocalOrbitalFrame lvlh = new LocalOrbitalFrame(targetOrbit.getFrame(),LOFType.LVLH_CCSDS,targetOrbit.toOrbit(),LOFType.LVLH_CCSDS.getName());
        for (TimeStampedFieldPVCoordinates<Binary64> waypoint : waypoints) {
            waypointsQSW.add(lvlh.getTransformTo(qsw,waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        final List<FieldTwoImpulseTransfer<Binary64>> transfersFromQSW = (new FieldMultiRelativeTransfersYA<>(waypointsQSW,targetOrbit,new FieldKeplerianPropagator<>(targetOrbit))).computeMultiRelativeTransfers(qsw);
        // Assert start and end point of the transfers are the same as the corresponding waypoints.
        for (int i = 0; i<transfersFromLVLH.size(); i++) {
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt1BeforeMan().getPosition().toVector3D(),transfersFromQSW.get(i).getPvt1BeforeMan().getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt1BeforeMan().getVelocity().toVector3D(),transfersFromQSW.get(i).getPvt1BeforeMan().getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt2AfterMan().getPosition().toVector3D(),transfersFromQSW.get(i).getPvt2AfterMan().getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt2AfterMan().getVelocity().toVector3D(),transfersFromQSW.get(i).getPvt2AfterMan().getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getDeltaV1().toVector3D(),transfersFromQSW.get(i).getDeltaV1().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getDeltaV2().toVector3D(),transfersFromQSW.get(i).getDeltaV2().toVector3D(),NUMERICAL_TOLERANCE);
        }
    }
}
