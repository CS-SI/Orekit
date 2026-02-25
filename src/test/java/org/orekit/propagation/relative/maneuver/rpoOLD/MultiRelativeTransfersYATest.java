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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.propagation.relative.TwoImpulseTransfer;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.maneuver.YamanakaAnkersenManeuver;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class MultiRelativeTransfersYATest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void MultiLinearTransfersTest(){
        final double n = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n), 1./3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);
        // Initial and final position of the chaser on the linear path.
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,600,200), new Vector3D(0,0,0));
        final TimeStampedPVCoordinates finalPVT = new TimeStampedPVCoordinates(epoch.shiftedBy(1000),new Vector3D(0,0,0), new Vector3D(10,10,10));
        // Computation of the linear Waypoints.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeLinearWaypoints(initialPVT, finalPVT,11);
        // Computation of the transfers along the linear path.
        final List<TwoImpulseTransfer> transfers = (new MultiRelativeTransfersYA(waypoints, initialPVT.getVelocity(), targetOrbit,new KeplerianPropagator(targetOrbit))).computeMultiRelativeTransfers();
        // Assert start and end point of the transfers are the same as the corresponding waypoints.
        for (int i = 0; i<transfers.size(); i++) {
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getPosition(),waypoints.get(i).getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getVelocity(),waypoints.get(i).getVelocity(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getPosition(),waypoints.get(i+1).getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getVelocity(),waypoints.get(i+1).getVelocity(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void ForcedCircularTransfersTest(){
        final double n = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n), 1./3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);
        // Initial position of the chaser.
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,600,200), new Vector3D(0,0,0));
        // Computation of the forced circular Waypoints.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(initialPVT,new Vector3D(0,0,0),100,0,0,1000,10,1,11,1000);
        // Computation of the transfers along the forced circular trajectory.
        final List<TwoImpulseTransfer> transfers = (new MultiRelativeTransfersYA(waypoints, initialPVT.getVelocity(), targetOrbit, new KeplerianPropagator(targetOrbit))).computeMultiRelativeTransfers();
        // Assert start and end point of the transfers are the same as the corresponding waypoints.
        for (int i = 0; i<transfers.size(); i++) {
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getPosition(),waypoints.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt1BeforeMan().getVelocity(),waypoints.get(i).getVelocity(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getPosition(),waypoints.get(i+1).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfers.get(i).getPvt2AfterMan().getVelocity(),waypoints.get(i+1).getVelocity(), NUMERICAL_TOLERANCE);
        }
    }


    //Keplerian propagation of a linear transfer using Yamanaka-Ankersen maneuvers.
    //Test if the chaser position at the end of the propagation is the same as the final position on linear path.
    @Test
    public void LinearTransferPropagationYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.1, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(finalDate, new Vector3D(200, 0, 0), Vector3D.ZERO);

        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final MultiRelativeTransfersYA multiRelativeTransfersYA = new MultiRelativeTransfersYA(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, new KeplerianPropagator(targetOrbit));
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<YamanakaAnkersenManeuver> maneuvers = multiRelativeTransfersYA.computeRelativeManeuvers(yaProvider);
        for (YamanakaAnkersenManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final SpacecraftState finalState = propagator.propagate(finalDate.shiftedBy(1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final double[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0],pvtChaserFinal.getPosition().getX(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1],pvtChaserFinal.getPosition().getY(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2],pvtChaserFinal.getPosition().getZ(), NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a Forced Circular transfer using Yamanaka-Ankersen maneuver.
    // Test if the chaser position at the end of the propagation (3 relative orbit's periods) is the same as the first waypoint on the circle.
    @Test
    public void ForcedCircularPropagationTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.1, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(pvtChaserInitial,new Vector3D(0,0,0),100,0,0,1000,10,3,6,500);
        final MultiRelativeTransfersYA multiRelativeTransfersYA = new MultiRelativeTransfersYA(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, new KeplerianPropagator(targetOrbit));
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<YamanakaAnkersenManeuver> maneuvers = multiRelativeTransfersYA.computeRelativeManeuvers(yaProvider);
        for (YamanakaAnkersenManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the scenario (500s of linear duration + 3 orbits of 1000s period).
        final SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(3500 + 1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final double[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0],waypoints.get(5).getPosition().getX(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1],waypoints.get(5).getPosition().getY(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2],waypoints.get(5).getPosition().getZ(), NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a Natural Circumnavigation transfer using Yamanaka-Ankersen maneuver.
    // Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
    @Test
    public void NaturalCircumnavigationYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double relativeSemiMinorAxis = 50;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeCircularNaturalCircumnavigationWaypoints(
                pvtChaserInitial, injectionDate, n, 50, 0.3, 10);
        final MultiRelativeTransfersYA multiRelativeTransfersYA = new MultiRelativeTransfersYA(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, new KeplerianPropagator(targetOrbit));
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<YamanakaAnkersenManeuver> maneuvers = multiRelativeTransfersYA.computeRelativeManeuvers(yaProvider);
        for (YamanakaAnkersenManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final SpacecraftState finalTarget = propagator.propagate(injectionDate.shiftedBy(2 * FastMath.PI / n));
        final double[] chaserFinal = yaProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(0, -2 * semiMinorAxis, 0)
        Assertions.assertEquals(-2 * relativeSemiMinorAxis, chaserFinal[0], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[1], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2], NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a Natural Circumnavigation transfer using Yamanaka-Ankersen maneuver.
    // Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
    @Test
    public void NaturalCircularMotionYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double radius = 50;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeNaturalCircularWaypoints(
                pvtChaserInitial, injectionDate, n, 50, 10);
        final MultiRelativeTransfersYA multiRelativeTransfersYA = new MultiRelativeTransfersYA(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, new KeplerianPropagator(targetOrbit));
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<YamanakaAnkersenManeuver> maneuvers = multiRelativeTransfersYA.computeRelativeManeuvers(yaProvider);
        for (YamanakaAnkersenManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final SpacecraftState finalTarget = propagator.propagate(injectionDate.shiftedBy(2 * FastMath.PI / n));
        final double[] chaserFinal = yaProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(0, -raidus, 0)
        Assertions.assertEquals(-radius, chaserFinal[0], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[1], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2], NUMERICAL_TOLERANCE);
    }

    @Test
    public void getWaypointsTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(finalDate, new Vector3D(200, 0, 0), Vector3D.ZERO);

        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final MultiRelativeTransfersYA transfers = new MultiRelativeTransfersYA(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, new KeplerianPropagator(targetOrbit));
        for (int i = 0; i < waypoints.size(); i++) {
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),transfers.getWaypoints().get(i).getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(),transfers.getWaypoints().get(i).getVelocity(),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),transfers.getWaypoints().get(i).getDate().toDouble(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void getTargetOrbitTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(finalDate, new Vector3D(200, 0, 0), Vector3D.ZERO);

        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final MultiRelativeTransfersYA transfers = new MultiRelativeTransfersYA(waypoints,pvtChaserInitial.getVelocity(),targetOrbit,new KeplerianPropagator(targetOrbit));
        final KeplerianOrbit targetTransfer = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(transfers.getTargetOrbit());
        Assertions.assertEquals(targetOrbit.getA(),targetTransfer.getA(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getE(),targetTransfer.getE(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getI(),targetTransfer.getI(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getPerigeeArgument(),targetTransfer.getPerigeeArgument(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getRightAscensionOfAscendingNode(),targetTransfer.getRightAscensionOfAscendingNode(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getTrueAnomaly(),targetTransfer.getTrueAnomaly(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(targetOrbit.getKeplerianPeriod(),targetTransfer.getKeplerianPeriod(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void getLofType() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(finalDate, new Vector3D(200, 0, 0), Vector3D.ZERO);

        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        final MultiRelativeTransfersYA transfers = new MultiRelativeTransfersYA(waypoints,pvtChaserInitial.getVelocity(),targetOrbit, new KeplerianPropagator(targetOrbit));
        Assertions.assertEquals(LOFType.LVLH_CCSDS.getName(),transfers.getLofType().getName());
    }

    // Test the computeMultiRelativeTransfers(Frame frame) method and compare it with the one in the Yamanaka-Ankersen Frame.
    @Test
    public void computeMultiRelativeTransferFromOtherFrame() {
        final double n = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n), 1./3.);// Target orbit's radius
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);
        // Initial and final position of the chaser on the linear path.
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,600,200), new Vector3D(0,0,0));
        final TimeStampedPVCoordinates finalPVT = new TimeStampedPVCoordinates(epoch.shiftedBy(1000),new Vector3D(0,0,0), new Vector3D(10,10,10));
        // Computation of the linear Waypoints.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeLinearWaypoints(initialPVT, finalPVT,11);
        // Computation of the transfers along the linear path.
        final List<TwoImpulseTransfer> transfersFromLVLH = (new MultiRelativeTransfersYA(waypoints,initialPVT.getVelocity(),targetOrbit, new KeplerianPropagator(targetOrbit))).computeMultiRelativeTransfers();
        // Transform the waypoints in the LVLH frame and compute the transfers.
        final List<TimeStampedPVCoordinates> waypointsQSW = new ArrayList<>();
        final LocalOrbitalFrame qsw = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, targetOrbit,LOFType.QSW.getName());
        final LocalOrbitalFrame lvlh = new LocalOrbitalFrame(targetOrbit.getFrame(),LOFType.LVLH_CCSDS,targetOrbit,LOFType.LVLH_CCSDS.getName());
        for (TimeStampedPVCoordinates waypoint : waypoints) {
            waypointsQSW.add(lvlh.getTransformTo(qsw,waypoint.getDate()).transformPVCoordinates(waypoint));
        }
        final List<TwoImpulseTransfer> transfersFromQSW = (new MultiRelativeTransfersYA(waypointsQSW,initialPVT.getVelocity(),targetOrbit, new KeplerianPropagator(targetOrbit))).computeMultiRelativeTransfers(qsw);
        // Assert start and end point of the transfers are the same as the corresponding waypoints.
        for (int i = 0; i<transfersFromLVLH.size(); i++) {
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt1BeforeMan().getPosition(),transfersFromQSW.get(i).getPvt1BeforeMan().getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt1BeforeMan().getVelocity(),transfersFromQSW.get(i).getPvt1BeforeMan().getVelocity(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt2AfterMan().getPosition(),transfersFromQSW.get(i).getPvt2AfterMan().getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getPvt2AfterMan().getVelocity(),transfersFromQSW.get(i).getPvt2AfterMan().getVelocity(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getDeltaV1(),transfersFromQSW.get(i).getDeltaV1(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(transfersFromLVLH.get(i).getDeltaV2(),transfersFromQSW.get(i).getDeltaV2(),NUMERICAL_TOLERANCE);
        }
    }
}

