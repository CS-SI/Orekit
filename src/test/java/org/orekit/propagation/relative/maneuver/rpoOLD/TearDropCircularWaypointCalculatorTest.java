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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.relative.maneuver.ClohessyWiltshireManeuver;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

public class TearDropCircularWaypointCalculatorTest {

    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-7;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void TearDropWaypointTest() {
        final double targetMeanMotion = 0.0011569;// Mean motion of target's orbit
        final double turnAroundDistance = 50;
        final double maneuverDistance = 100;
        final int numberOfOrbits = 5;
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TeardropCircularWaypointCalculator tearDropCalculator = new TeardropCircularWaypointCalculator(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final List<TimeStampedPVCoordinates> tearDropWaypoints = tearDropCalculator.computeTearDropWaypoints(epoch.shiftedBy(1000.));
        //Assert that the first waypoint is located at (turnAroundDistance, 0, 0) and at injectionDate.
        Assertions.assertEquals(tearDropWaypoints.get(0).getDate().toDouble(),epoch.shiftedBy(1000.).toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(turnAroundDistance,0,0),tearDropWaypoints.get(0).getPosition(),NUMERICAL_TOLERANCE);
        // Assert that the following waypoints are located at (maneuverDistance, 0, 0) and spaced by the computed tearDropPeriod.
        final double tearDropPeriod = tearDropCalculator.computeRelativeOrbitalPeriod();
        for (int i = 0; i < numberOfOrbits; i++) {
            Assertions.assertEquals(epoch.shiftedBy(1000 + tearDropPeriod * i + tearDropPeriod / 2).toDouble(), tearDropWaypoints.get(i+1).getDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(new Vector3D(maneuverDistance,0,0),tearDropWaypoints.get(i+1).getPosition(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void ImpulseAtManeuverPointTest() {
        final double targetMeanMotion = 0.0011569;// Mean motion of target's orbit
        final double turnAroundDistance = 50;
        final double maneuverDistance = 100;
        final int numberOfOrbits = 5;
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TeardropCircularWaypointCalculator tearDropCalculator = new TeardropCircularWaypointCalculator(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final TimeStampedPVCoordinates waypoint = new TimeStampedPVCoordinates(epoch,new Vector3D(1,1,1), new Vector3D(1,0,0));
        final Vector3D deltaV = tearDropCalculator.computeImpulseAtManeuverPoint(waypoint);
        Assertions.assertEquals(2,deltaV.getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getZ(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void ImpulseAtManeuverPointInertialOtherFrameTest() {
        final double targetMeanMotion = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(targetMeanMotion*targetMeanMotion),1./3.);
        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH, Constants.EIGEN5C_EARTH_MU);
        final double turnAroundDistance = 50;
        final double maneuverDistance = 100;
        final int numberOfOrbits = 5;
        final TeardropCircularWaypointCalculator tearDropCalculator = new TeardropCircularWaypointCalculator(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final TimeStampedPVCoordinates waypoint = new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,new Vector3D(1,1,1), new Vector3D(1,0,0));
        final Vector3D deltaV = tearDropCalculator.computeImpulseAtManeuverPointOtherFrame(waypoint,targetOrbit,FramesFactory.getGCRF());
        Assertions.assertEquals(2,deltaV.getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getZ(),NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a TearDrop maneuver scenario. Chaser goes from an initial position to the turnAroundPoint using Linear Maneuvers.
    // Test if the chaser position at the end of the propagation (5 teardrop periods) is at the maneuverPoint.
    @Test
    public void TearDropPropagationTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Start and end conditions of the transfer, expressed in the target's LOF
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Injection Date in the teardrop Orbit.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitial);
        // Compute the teardrop waypoints.
        final TeardropCircularWaypointCalculator teardropCalculator = new TeardropCircularWaypointCalculator(targetOrbit.getKeplerianMeanMotion(),50,100,5);
        final double tearDropRelativePeriod = teardropCalculator.computeRelativeOrbitalPeriod();
        final List<TimeStampedPVCoordinates> tearDropWaypoints = teardropCalculator.computeTearDropWaypoints(injectionDate);
        // Compute Linear Waypoints.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.CW.computeLinearWaypoints(pvtChaserInitial,tearDropWaypoints.get(0),10);
        // Compute all the linear maneuvers.
        final List<TwoImpulseTransfer> transfers = (new MultiRelativeTransfersCW(linearWaypoints, pvtChaserInitial.getVelocity(), targetOrbit)).computeMultiRelativeTransfers();
        // Define the propagator.
        final KeplerianPropagator propagator =new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Creation of the maneuvers corresponding to the impulses of the linear transfer.
        final List<ClohessyWiltshireManeuver> maneuvers = (new MultiRelativeTransfersCW(linearWaypoints, pvtChaserInitial.getVelocity(), targetOrbit)).computeRelativeManeuvers(cwProvider);
        for (ClohessyWiltshireManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Creation of the maneuvers at the maneuver point of the teardrop.
        final List<ClohessyWiltshireManeuver> tearDropManeuvers = teardropCalculator.computeTearDropRelativeManeuvers(injectionDate, cwProvider);
        for (ClohessyWiltshireManeuver maneuver: tearDropManeuvers) {
            propagator.addEventDetector(maneuver);
        }

        final SpacecraftState finalTarget = propagator.propagate(injectionDate.shiftedBy((5+1./2.)*tearDropRelativePeriod));
        final double[] finalChaser = cwProvider.getAdditionalData(finalTarget);
        Assertions.assertEquals(100, finalChaser[0], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[1], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[2], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getX(),finalChaser[3],NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getY(),finalChaser[4],NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getZ(),finalChaser[5],NUMERICAL_TOLERANCE);

    }
}
