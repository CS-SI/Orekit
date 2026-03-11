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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.*;

import java.util.List;

public class RPOTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-8;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    // Assess the computation of the forced linear waypoints.
    @Test
    public void computeLinearWaypointsCalculatorTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,600,200), new Vector3D(0,0,0));
        final TimeStampedPVCoordinates finalPVT = new TimeStampedPVCoordinates(epoch.shiftedBy(1000),new Vector3D(0,0,0), new Vector3D(10,10,10));
        final List<TimeStampedPVCoordinates> waypointsCW = RPOModel.CW.computeForcedLinearWaypoints(initialPVT, finalPVT,11);
        final List<TimeStampedPVCoordinates> waypointsYA = RPOModel.YA.computeForcedLinearWaypoints(initialPVT, finalPVT,11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypointsCW.get(i).getDate().toDouble(), epoch.shiftedBy(i * 100).toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getPosition(),new Vector3D(-1000 + i * 100,  600 - i * 60,200- i * 20),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypointsYA.get(i).getDate().toDouble(), epoch.shiftedBy(i * 100).toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getPosition(),new Vector3D(-1000 + i * 100, 600 - i * 60,200 - i * 20),NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(100 * FastMath.sin(i * 2 * FastMath.PI / 10),-100 * FastMath.cos(i * 2 * FastMath.PI / 10),0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints retrograde case with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestRetrogradeCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypointsPrograde = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsRetrograde = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,true);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypointsPrograde.size(); i++) {
            // Assert the dates are the same.
            Assertions.assertEquals(waypointsPrograde.get(i).getDate().toDouble(), waypointsRetrograde.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints list position are reversed.
            TestUtils.validateVector3D(waypointsPrograde.get(waypointsPrograde.size()-(i+1)).getPosition(), waypointsRetrograde.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsRetrograde.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with multi revolutions with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestMultiRevCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,2,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 1; i < 11; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble() + 1000, waypoints.get(10 + i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), waypoints.get(10 + i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular in center offset case with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestCenterOffsetCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, new Vector3D(0, 10, 0), 100, 0,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(100 * FastMath.sin(i * 2 * FastMath.PI / 10),-100 * FastMath.cos(i * 2 * FastMath.PI / 10) +10,0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestInclinationCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclined = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclined.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(0, refPosition.getY(), refPosition.getX()), waypointsInclined.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination and an Orientation with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestRaanCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclinedOriented = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,FastMath.PI/2,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclinedOriented.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(-refPosition.getY(), 0, refPosition.getX()), waypointsInclinedOriented.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with a start angle with CW rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestStartAngleCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsWithStartAngle = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,FastMath.PI/2,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size()-1; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsWithStartAngle.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i+1).getPosition(), waypointsWithStartAngle.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(-100 * FastMath.cos(i * 2 * FastMath.PI / 10),0,-100 * FastMath.sin(i * 2 * FastMath.PI / 10) ),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints retrograde case with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestRetrogradeYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypointsPrograde = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsRetrograde = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,true);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypointsPrograde.size(); i++) {
            // Assert the dates are the same.
            Assertions.assertEquals(waypointsPrograde.get(i).getDate().toDouble(), waypointsRetrograde.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints list position are reversed.
            TestUtils.validateVector3D(waypointsPrograde.get(waypointsPrograde.size()-(i+1)).getPosition(), waypointsRetrograde.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsRetrograde.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with multi revolutions with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestMultiRevYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,2,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 1; i < 11; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble() + 1000, waypoints.get(10 + i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), waypoints.get(10 + i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints in center offset case with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestCenterOffsetYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, new Vector3D(10, 0, 0), 100, 0,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), new Vector3D(-100 * FastMath.cos(i * 2 * FastMath.PI / 10) + 10,0,-100 * FastMath.sin(i * 2 * FastMath.PI / 10) ),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestInclinationYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclined = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,0,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclined.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(refPosition.getX(), refPosition.getZ(), 0), waypointsInclined.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination and an Orientation with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestRaanYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,10,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsInclinedOriented = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, FastMath.PI/2,FastMath.PI/2,1000,10,1,0,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsInclinedOriented.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition();
            TestUtils.validateVector3D(new Vector3D(0, refPosition.getZ(), refPosition.getX()), waypointsInclinedOriented.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with a start angle with YA rpo model.
    @Test
    public void computeForcedCircularWaypointsCalculatorTestStartAngleYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,0,false);
        final List<TimeStampedPVCoordinates> waypointsWithStartAngle = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, Vector3D.ZERO, 100, 0,0,1000,4,1,FastMath.PI/2,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size()-1; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), waypointsWithStartAngle.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i+1).getPosition(), waypointsWithStartAngle.get(i).getPosition(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the injection waypoint in both Yamanaka-Ankersen and Clohessy-Wiltshire cases.
    @Test
    public void computeNaturalCircumnavigationInjectionCircularTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double targetMeanMotion = 0.0011569;
        final TimeStampedPVCoordinates waypointCW = RPOModel.CW.computeNaturalCircumnavigationInjectionCircular(
                injectionDate, 50, FastMath.PI * 60 / 180, targetMeanMotion);
        final TimeStampedPVCoordinates waypointYA = RPOModel.YA.computeNaturalCircumnavigationInjectionCircular(
                injectionDate, 50, FastMath.PI * 60 / 180, targetMeanMotion);
        // Assess the injection waypoint in the natural circumnavigation orbit using Clohessy-Wiltshire.
        Assertions.assertEquals(injectionDate.toDouble(),waypointCW.getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-50,0),waypointCW.getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-0.0289225,0,0.05009523948),waypointCW.getVelocity(),NUMERICAL_TOLERANCE);
        // Assess the injection waypoint in the natural circumnavigation orbit using Yamanaka-Ankersen.
        Assertions.assertEquals(injectionDate.toDouble(),waypointYA.getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-50,0,0),waypointYA.getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-0.05009523948,0.0289225),waypointYA.getVelocity(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void computeFieldForcedLinearWaypointsCalculatorTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-1000,600,200), new Vector3D(0,0,0))));
        final TimeStampedFieldPVCoordinates<Binary64> finalPVT = new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,0,0), new Vector3D(10,10,10))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsCW = RPOModel.CW.computeForcedLinearWaypoints(initialPVT, finalPVT,11);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsYA = RPOModel.YA.computeForcedLinearWaypoints(initialPVT, finalPVT,11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypointsCW.get(i).getDate().toAbsoluteDate().toDouble(), epoch.shiftedBy(i*100).toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getPosition().toVector3D(),new Vector3D(-1000+i*100, 600-i*60,200-i*20),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypointsYA.get(i).getDate().toAbsoluteDate().toDouble(), epoch.shiftedBy(i*100).toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getPosition().toVector3D(),new Vector3D(-1000+i*100, 600-i*60,200-i*20),NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),new Vector3D(100 * FastMath.sin(i * 2 * FastMath.PI / 10),-100 * FastMath.cos(i * 2 * FastMath.PI / 10),0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints retrograde case with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestRetrogradeCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsPrograde = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsRetrograde = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,true);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypointsPrograde.size(); i++) {
            // Assert the dates are the same.
            Assertions.assertEquals(waypointsPrograde.get(i).getDate().toAbsoluteDate().toDouble(), waypointsRetrograde.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints list position are reversed.
            TestUtils.validateVector3D(waypointsPrograde.get(waypointsPrograde.size()-(i+1)).getPosition().toVector3D(), waypointsRetrograde.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsRetrograde.get(i).getVelocity().toVector3D(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with multi revolutions with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestMultiRevCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,2,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 1; i < 11; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble() + 1000, waypoints.get(10 + i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(), waypoints.get(10 + i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints in center offset case with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestCenterOffsetCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, new FieldVector3D<>(field, new Vector3D(0,10,0)), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),new Vector3D(100 * FastMath.sin(i * 2 * FastMath.PI / 10),-100 * FastMath.cos(i * 2 * FastMath.PI / 10)+10,0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestInclinationCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsInclined = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(FastMath.PI/2),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), waypointsInclined.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition().toVector3D();
            TestUtils.validateVector3D(new Vector3D(0, refPosition.getY(), refPosition.getX()), waypointsInclined.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination and an Orientation with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestRaanCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsInclinedOriented = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(FastMath.PI/2),new Binary64(FastMath.PI/2),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), waypointsInclinedOriented.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition().toVector3D();
            TestUtils.validateVector3D(new Vector3D(-refPosition.getY(), 0, refPosition.getX()), waypointsInclinedOriented.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with a start angle with CW rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestStartAngleCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(1000),4,1,new Binary64(0),false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsWithStartAngle = RPOModel.CW.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(1000),4,1,new Binary64(FastMath.PI/2),false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size()-1; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), waypointsWithStartAngle.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i+1).getPosition().toVector3D(), waypointsWithStartAngle.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),new Vector3D(-100 * FastMath.cos(i * 2 * FastMath.PI / 10),0,-100 * FastMath.sin(i * 2 * FastMath.PI / 10) ),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints retrograde case with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestRetrogradeYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsPrograde = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsRetrograde = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,true);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypointsPrograde.size(); i++) {
            // Assert the dates are the same.
            Assertions.assertEquals(waypointsPrograde.get(i).getDate().toAbsoluteDate().toDouble(), waypointsRetrograde.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints list position are reversed.
            TestUtils.validateVector3D(waypointsPrograde.get(waypointsPrograde.size()-(i+1)).getPosition().toVector3D(), waypointsRetrograde.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsRetrograde.get(i).getVelocity().toVector3D(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with multi revolutions with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestMultiRevYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,2,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 1; i < 11; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble() + 1000, waypoints.get(10 + i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(), waypoints.get(10 + i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints simplest case with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestCenterOffsetYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, new FieldVector3D<>(field, new Vector3D(10,0,0)), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000. / 10 * i).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),new Vector3D(-100 * FastMath.cos(i * 2 * FastMath.PI / 10)+10,0,-100 * FastMath.sin(i * 2 * FastMath.PI / 10) ),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), Vector3D.ZERO, NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestInclinationYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsInclined = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(FastMath.PI/2),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), waypointsInclined.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition().toVector3D();
            TestUtils.validateVector3D(new Vector3D(refPosition.getX(), refPosition.getZ(), 0), waypointsInclined.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with Inclination and an Orientation with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestRaanYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0),new Binary64(0),new Binary64(1000),10,1,Binary64.ZERO,false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsInclinedOriented = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(FastMath.PI/2),new Binary64(FastMath.PI/2),new Binary64(1000),10,1,Binary64.ZERO,false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size(); i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), waypointsInclinedOriented.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            final Vector3D refPosition = waypoints.get(i).getPosition().toVector3D();
            TestUtils.validateVector3D(new Vector3D(0, refPosition.getZ(), refPosition.getX()), waypointsInclinedOriented.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the forced circular waypoints with a start angle with YA rpo model.
    @Test
    public void computeFieldForcedCircularWaypointsCalculatorTestStartAngleYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(1000),4,1,new Binary64(0),false);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsWithStartAngle = RPOModel.YA.computeForcedCircularMotionWaypoints(epoch, FieldVector3D.getZero(field), new Binary64(100), new Binary64(0), new Binary64(0), new Binary64(1000),4,1,new Binary64(FastMath.PI/2),false);
        // Assert that the waypoints on the circular path are correct.
        for (int i = 0; i < waypoints.size()-1; i++) {
            // Assert the dates of the second revolution orbit are these of the first orbit shifted by a relative orbit period.
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), waypointsWithStartAngle.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            // Assert the waypoints of the second revolution orbit have the same position as the ones of the first relative orbit.
            TestUtils.validateVector3D(waypoints.get(i+1).getPosition().toVector3D(), waypointsWithStartAngle.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
        }
    }

    // Assess the computation of the injection waypoint in both Yamanaka-Ankersen and Clohessy-Wiltshire cases.
    @Test
    public void computeFieldNaturalCircumnavigationInjectionCircularTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 targetMeanMotion = new Binary64(0.0011569);
        final TimeStampedFieldPVCoordinates<Binary64> waypointCW = RPOModel.CW.computeNaturalCircumnavigationInjectionCircular(
                injectionDate, new Binary64(50), new Binary64(FastMath.PI * 60 / 180), targetMeanMotion);
        final TimeStampedFieldPVCoordinates<Binary64> waypointYA = RPOModel.YA.computeNaturalCircumnavigationInjectionCircular(
                injectionDate, new Binary64(50), new Binary64(FastMath.PI * 60 / 180), targetMeanMotion);
        // Assess the injection waypoint in the natural circumnavigation orbit using Clohessy-Wiltshire.
        Assertions.assertEquals(injectionDate.toAbsoluteDate().toDouble(),waypointCW.getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-50,0),waypointCW.getPosition().toVector3D(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-0.0289225,0,0.05009523948),waypointCW.getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
        // Assess the injection waypoint in the natural circumnavigation orbit using Yamanaka-Ankersen.
        Assertions.assertEquals(injectionDate.toAbsoluteDate().toDouble(),waypointYA.getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-50,0,0),waypointYA.getPosition().toVector3D(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-0.05009523948,0.0289225),waypointYA.getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
    }
}
