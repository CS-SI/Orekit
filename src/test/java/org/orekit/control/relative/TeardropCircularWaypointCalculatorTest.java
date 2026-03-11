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
package org.orekit.control.relative;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TeardropCircularWaypointCalculatorTest {

    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-7;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    // Assess the computation of the teardrop waypoints performed by TeardropCircularWaypointCalculator.
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

    // Assess the method of RPOModel gives the same results as the method in TeardropCircularWaypointCalculator.
    @Test
    public void computeTearDropWaypointTestCW() {
        final double targetMeanMotion = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(targetMeanMotion*targetMeanMotion), 1./3.);
        final double turnAroundDistance = 50;
        final double maneuverDistance = 100;
        final int numberOfOrbits = 5;
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);
        final TeardropCircularWaypointCalculator tearDropCalculator = new TeardropCircularWaypointCalculator(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final List<TimeStampedPVCoordinates> tearDropWaypoints = tearDropCalculator.computeTearDropWaypoints(epoch.shiftedBy(1000.));
        final List<TimeStampedPVCoordinates> tearDropWaypointsFromRPO = RPOModel.CW.computeTeardropWaypoints(epoch.shiftedBy(1000), targetOrbit, turnAroundDistance,maneuverDistance, numberOfOrbits);
        // Assert that the methods give the same results.
        for (int i = 0; i < tearDropWaypoints.size(); i++) {
            final TimeStampedPVCoordinates tearDropWaypoint = tearDropWaypoints.get(i);
            final TimeStampedPVCoordinates tearDropWaypointRPO = tearDropWaypointsFromRPO.get(i);
            Assertions.assertEquals(tearDropWaypoint.getDate().toDouble(), tearDropWaypointRPO.getDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(tearDropWaypoint.getPosition(), tearDropWaypointRPO.getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(tearDropWaypoint.getVelocity(), tearDropWaypointRPO.getVelocity(), NUMERICAL_TOLERANCE);
        }
    }

    // Check the results of computeTeardropWaypoints from YA enum.
    // Check the position and velocity vectors are the same as the CW ones but expressed in LVLH_CCSDS frame.
    @Test
    public void computeTearDropWaypointTestYA() {
        final double targetMeanMotion = 0.0011569;// Mean motion of target's orbit
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(targetMeanMotion*targetMeanMotion), 1./3.);
        final double turnAroundDistance = 50;
        final double maneuverDistance = 100;
        final int numberOfOrbits = 5;
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);
        final List<TimeStampedPVCoordinates> tearDropWaypointsCW = RPOModel.CW.computeTeardropWaypoints(epoch.shiftedBy(1000), targetOrbit, turnAroundDistance,maneuverDistance, numberOfOrbits);
        final List<TimeStampedPVCoordinates> tearDropWaypointsYA = RPOModel.YA.computeTeardropWaypoints(epoch.shiftedBy(1000), targetOrbit, turnAroundDistance,maneuverDistance, numberOfOrbits);
        // Assert that the methods give the same results.
        for (int i = 0; i < tearDropWaypointsCW.size(); i++) {
            // Retrieve position and velocity vectors.
            final TimeStampedPVCoordinates tearDropWaypointCW = tearDropWaypointsCW.get(i);
            final TimeStampedPVCoordinates tearDropWaypointYA = tearDropWaypointsYA.get(i);
            final Vector3D positionCW = tearDropWaypointCW.getPosition();
            final Vector3D positionYA = tearDropWaypointYA.getPosition();
            final Vector3D velocityCW = tearDropWaypointCW.getVelocity();
            final Vector3D velocityYA = tearDropWaypointYA.getVelocity();
            // Create the reference position and velocity vector in YA Local Orbital Frame from CW position and velocity.
            final Vector3D positionRef = new Vector3D(positionCW.getY(), -positionCW.getZ(), -positionCW.getX());
            final Vector3D velocityRef = new Vector3D(velocityCW.getY(), -velocityCW.getZ(), -velocityCW.getX());

            Assertions.assertEquals(tearDropWaypointCW.getDate().toDouble(), tearDropWaypointYA.getDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(positionRef, positionYA, NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(velocityRef, velocityYA, NUMERICAL_TOLERANCE);
        }
    }

    // Check computeTeardropWaypoints() method of YA enum throws an error if the orbit of the target is not circular.
    @Test
    public void checkNonCircularTeardropYATest() {
        final double turnAroundDistance = 50;
        final double maneuverDistance = 100;
        final int numberOfOrbits = 5;
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(7000e3, 0.1, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () ->
                RPOModel.YA.computeTeardropWaypoints(epoch, targetOrbit, turnAroundDistance, maneuverDistance, numberOfOrbits));

        Assertions.assertEquals("A teardrop is not analytically feasible around an eccentric orbit.", ex.getMessage());
    }
}
