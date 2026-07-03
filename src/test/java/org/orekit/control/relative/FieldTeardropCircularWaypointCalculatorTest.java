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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldTeardropCircularWaypointCalculatorTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testFieldTearDropWaypoint() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetMeanMotion = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldTeardropCircularWaypointCalculator<Binary64> tearDropCalculator =
                        new FieldTeardropCircularWaypointCalculator<>(targetMeanMotion, turnAroundDistance,
                                                                      maneuverDistance, numberOfOrbits);
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypoints =
                        tearDropCalculator.computeTearDropWaypoints(epoch.shiftedBy(1000.));

        // Assert that the first waypoint is located at (turnAroundDistance, 0, 0) and at injectionDate.
        Assertions.assertEquals(tearDropWaypoints.getFirst().getDate().toAbsoluteDate().toDouble(),
                                epoch.shiftedBy(1000.).toAbsoluteDate().toDouble(), 0);
        TestUtils.validateVector3D(new Vector3D(turnAroundDistance.getReal(), 0, 0),
                                   tearDropWaypoints.getFirst().getPosition().toVector3D(), 0);

        // Assert that the following waypoints are located at (maneuverDistance, 0, 0) and spaced by the computed tearDropPeriod.
        final Binary64 tearDropPeriod = tearDropCalculator.computeRelativeOrbitalPeriod();
        for (int i = 0; i < numberOfOrbits; i++) {
            Assertions.assertEquals(epoch.shiftedBy(1000 + tearDropPeriod.getReal() * i + tearDropPeriod.getReal() / 2)
                                         .toAbsoluteDate().toDouble(),
                                    tearDropWaypoints.get(i + 1).getDate().toAbsoluteDate().toDouble(), 1e-11);
            TestUtils.validateVector3D(new Vector3D(maneuverDistance.getReal(), 0, 0),
                                       tearDropWaypoints.get(i + 1).getPosition().toVector3D(), 3.91e-14);
        }
    }

    /** Assess the method of RPOModel gives the same results as the method in TeardropCircularWaypointCalculator. */
    @Test
    void testComputeTearDropWaypointCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetMeanMotion = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = (mu.divide(targetMeanMotion.multiply(targetMeanMotion))).pow(1. / 3.);
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldKeplerianOrbit<Binary64> targetOrbit =
                        new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(), field.getZero(),
                                                  field.getZero(), field.getZero(), PositionAngleType.MEAN,
                                                  FramesFactory.getGCRF(), epoch, mu);
        final FieldTeardropCircularWaypointCalculator<Binary64> tearDropCalculator =
                        new FieldTeardropCircularWaypointCalculator<>(targetMeanMotion, turnAroundDistance,
                                                                      maneuverDistance, numberOfOrbits);
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypoints =
                        tearDropCalculator.computeTearDropWaypoints(epoch.shiftedBy(1000.));
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypointsFromRPO =
                        RPOModel.CW.computeTeardropWaypoints(epoch.shiftedBy(1000), targetOrbit, turnAroundDistance,
                                                             maneuverDistance, numberOfOrbits);
        // Assert that the methods give the same results.
        for (int i = 0; i < tearDropWaypoints.size(); i++) {
            final TimeStampedFieldPVCoordinates<Binary64> tearDropWaypoint = tearDropWaypoints.get(i);
            final TimeStampedFieldPVCoordinates<Binary64> tearDropWaypointRPO = tearDropWaypointsFromRPO.get(i);
            Assertions.assertEquals(tearDropWaypoint.getDate().toAbsoluteDate().toDouble(),
                                    tearDropWaypointRPO.getDate().toAbsoluteDate().toDouble(), 1e-11);
            TestUtils.validateVector3D(tearDropWaypoint.getPosition().toVector3D(),
                                       tearDropWaypointRPO.getPosition().toVector3D(), 1.53e-13);
            TestUtils.validateVector3D(tearDropWaypoint.getVelocity().toVector3D(),
                                       tearDropWaypointRPO.getVelocity().toVector3D(), 1e-15);
        }
    }

    /**
     * Check the results of computeTeardropWaypoints from YA enum. Check the position and velocity vectors are the same
     * as the CW ones but expressed in LVLH_CCSDS frame.
     */
    @Test
    void testComputeTearDropWaypointYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetMeanMotion = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = (mu.divide(targetMeanMotion.multiply(targetMeanMotion))).pow(1. / 3.);
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldKeplerianOrbit<Binary64> targetOrbit =
                        new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(), field.getZero(),
                                                  field.getZero(), field.getZero(), PositionAngleType.MEAN,
                                                  FramesFactory.getGCRF(), epoch, mu);
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypointsCW =
                        RPOModel.CW.computeTeardropWaypoints(epoch.shiftedBy(1000), targetOrbit, turnAroundDistance,
                                                             maneuverDistance, numberOfOrbits);
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypointsYA =
                        RPOModel.YA.computeTeardropWaypoints(epoch.shiftedBy(1000), targetOrbit, turnAroundDistance,
                                                             maneuverDistance, numberOfOrbits);
        // Assert that the methods give the same results.
        for (int i = 0; i < tearDropWaypointsCW.size(); i++) {
            // Retrieve position and velocity vectors.
            final TimeStampedFieldPVCoordinates<Binary64> tearDropWaypointCW = tearDropWaypointsCW.get(i);
            final TimeStampedFieldPVCoordinates<Binary64> tearDropWaypointYA = tearDropWaypointsYA.get(i);
            final FieldVector3D<Binary64> positionCW = tearDropWaypointCW.getPosition();
            final FieldVector3D<Binary64> positionYA = tearDropWaypointYA.getPosition();
            final FieldVector3D<Binary64> velocityCW = tearDropWaypointCW.getVelocity();
            final FieldVector3D<Binary64> velocityYA = tearDropWaypointYA.getVelocity();
            // Create the reference position and velocity vector in YA Local Orbital Frame from CW position and velocity.
            final FieldVector3D<Binary64> positionRef =
                            new FieldVector3D<>(positionCW.getY(), positionCW.getZ().negate(),
                                                positionCW.getX().negate());
            final FieldVector3D<Binary64> velocityRef =
                            new FieldVector3D<>(velocityCW.getY(), velocityCW.getZ().negate(),
                                                velocityCW.getX().negate());

            Assertions.assertEquals(tearDropWaypointCW.getDate().toAbsoluteDate().toDouble(),
                                    tearDropWaypointYA.getDate().toAbsoluteDate().toDouble(), 0);
            TestUtils.validateVector3D(positionRef.toVector3D(), positionYA.toVector3D(), 1e-12);
            TestUtils.validateVector3D(velocityRef.toVector3D(), velocityYA.toVector3D(), 1e-14);
        }
    }

    /**
     * Check computeTeardropWaypoints() method of YA enum throws an error if the orbit of the target is not circular.
     */
    @Test
    void testCheckNonCircularTeardropYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldKeplerianOrbit<Binary64> targetOrbit =
                        new FieldKeplerianOrbit<>(new Binary64(7000e3), new Binary64(0.1), field.getZero(),
                                                  field.getZero(), field.getZero(), field.getZero(),
                                                  PositionAngleType.MEAN, FramesFactory.getGCRF(), epoch,
                                                  new Binary64(Constants.EIGEN5C_EARTH_MU));
        OrekitException ex = assertThrows(OrekitException.class,
                                          () -> RPOModel.YA.computeTeardropWaypoints(epoch, targetOrbit,
                                                                                     turnAroundDistance,
                                                                                     maneuverDistance, numberOfOrbits));

        Assertions.assertEquals("too large eccentricity for teardrop motion: e > 1e-4", ex.getMessage());
    }
}
