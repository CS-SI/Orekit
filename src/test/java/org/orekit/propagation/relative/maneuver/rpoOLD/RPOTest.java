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

    // Tests of the waypoints generation for the maneuvers defined in RPO interface and RPOModel enum.
    @Test
    public void LinearWaypointsCalculatorTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,600,200), new Vector3D(0,0,0));
        final TimeStampedPVCoordinates finalPVT = new TimeStampedPVCoordinates(epoch.shiftedBy(1000),new Vector3D(0,0,0), new Vector3D(10,10,10));
        final List<TimeStampedPVCoordinates> waypointsCW = RPOModel.CW.computeLinearWaypoints(initialPVT, finalPVT,11);
        final List<TimeStampedPVCoordinates> waypointsYA = RPOModel.YA.computeLinearWaypoints(initialPVT, finalPVT,11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypointsCW.get(i).getDate().toDouble(), epoch.shiftedBy(i*100).toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getPosition(),new Vector3D(-1000+i*100, 600-i*60,200-i*20),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getVelocity(),new Vector3D(i,i,i),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypointsYA.get(i).getDate().toDouble(), epoch.shiftedBy(i*100).toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getPosition(),new Vector3D(-1000+i*100, 600-i*60,200-i*20),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getVelocity(),new Vector3D(i,i,i),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void ForcedCircularWaypointsCalculatorTestCW() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(0,-1000,0), new Vector3D(0,0,0));
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(initialPVT,new Vector3D(0,0,0),100,0,0,1000,10,1,11,1000);
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.CW.computeLinearWaypoints(initialPVT,new TimeStampedPVCoordinates(epoch.shiftedBy(1000), new Vector3D(0,-100,0), new Vector3D(0,0,0)), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), linearWaypoints.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), linearWaypoints.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), linearWaypoints.get(i).getVelocity(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm());
        }
        // Assert that the waypoints on the circular path are correct.
        for (int i = 10; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000 + 1000. / 10 * (i-10)).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(100 * FastMath.sin(((i-10)+5)*2*FastMath.PI/10),100 * FastMath.cos(((i-10)+5)*2*FastMath.PI/10),0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(),Vector3D.ZERO,NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void ForcedCircularWaypointsCalculatorTestYA() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,0,0), new Vector3D(0,0,0));
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(initialPVT,new Vector3D(0,0,0),100,0,0,1000,10,1,11,1000);
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.YA.computeLinearWaypoints(initialPVT,new TimeStampedPVCoordinates(epoch.shiftedBy(1000), new Vector3D(-100,0,0), new Vector3D(0,0,0)), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), linearWaypoints.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), linearWaypoints.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), linearWaypoints.get(i).getVelocity(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm());
        }
        // Assert that the waypoints on the circular path are correct.
        for (int i = 10; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(),epoch.shiftedBy(1000 + 1000. / 10 * (i-10)).toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(),new Vector3D(100 * FastMath.cos(((i-10)+5)*2*FastMath.PI/10),0,100 * FastMath.sin(((i-10)+5)*2*FastMath.PI/10)),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(),Vector3D.ZERO,NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void CircularWaypointsWithInclinationTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(0,-1000,0), new Vector3D(0,0,0));
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(initialPVT,new Vector3D(0,0,0),100,0.2,0,1000,10,1,11,1000);
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.CW.computeLinearWaypoints(initialPVT,new TimeStampedPVCoordinates(epoch.shiftedBy(1000), new Vector3D(0,-100,0), new Vector3D(0,0,0)), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), linearWaypoints.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), linearWaypoints.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), linearWaypoints.get(i).getVelocity(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm());
        }
        // Assert the final waypoint on the circle is the same as the final waypoint on the linear path.
        TestUtils.validateVector3D(linearWaypoints.get(linearWaypoints.size()-1).getPosition(),waypoints.get(waypoints.size()-1).getPosition(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void ForcedCircularWaypointsCalculatorWithRaanTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(-1000,0,0), new Vector3D(0,0,0));
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(initialPVT,new Vector3D(0,0,0),100,0,0.5,1000,10,1,11,1000);
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.YA.computeLinearWaypoints(initialPVT,new TimeStampedPVCoordinates(epoch.shiftedBy(1000), waypoints.get(10).getPosition(), new Vector3D(0,0,0)), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toDouble(), linearWaypoints.get(i).getDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition(), linearWaypoints.get(i).getPosition(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity(), linearWaypoints.get(i).getVelocity(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm());
        }
        // Assert the final waypoint on the circle is the same as the final waypoint on the linear path.
        TestUtils.validateVector3D(linearWaypoints.get(linearWaypoints.size()-1).getPosition(),waypoints.get(waypoints.size()-1).getPosition(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void ForcedCircularWaypointsCalculatorMultiRevolution() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch,new Vector3D(0,-1000,0), new Vector3D(0,0,0));
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(initialPVT,new Vector3D(0,0,0),100,0,0,1000,10,3,11,1000);
        // Assert that the waypoints on successive orbit revolution have same position and velocity and same date shifted by the relative orbital period.
        for (int i = 1; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i + 10).getDate().shiftedBy(1000.).toDouble(),waypoints.get(2 * 10 + i).getDate().toDouble(),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypoints.get(i + 10).getDate().shiftedBy(2000.).toDouble(),waypoints.get(3 * 10 + i).getDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getPosition(),waypoints.get(2 * 10 + i).getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getPosition(),waypoints.get(3 * 10 + i).getPosition(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getVelocity(),waypoints.get(2 * 10 + i).getVelocity(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getVelocity(),waypoints.get(3 * 10 + i).getVelocity(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void CircularNavigationWaypointsTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch, new Vector3D(600,-1000,0), Vector3D.ZERO);
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double targetMeanMotion = 0.0011569;
        final List<TimeStampedPVCoordinates> waypointsCW = RPOModel.CW.computeCircularNaturalCircumnavigationWaypoints(
                initialPVT, injectionDate, targetMeanMotion, 50, 0.3, 10);
        final List<TimeStampedPVCoordinates> waypointsYA = RPOModel.YA.computeCircularNaturalCircumnavigationWaypoints(
                initialPVT, injectionDate, targetMeanMotion, 50, 0.3, 10);

        Assertions.assertEquals(injectionDate.toDouble(),waypointsCW.get(waypointsCW.size()-1).getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-2*50,0),waypointsCW.get(waypointsCW.size()-1).getPosition(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(injectionDate.toDouble(),waypointsYA.get(waypointsYA.size()-1).getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-2 * 50,0,0),waypointsYA.get(waypointsYA.size()-1).getPosition(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void CircularNaturalWaypointsTest() {
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final TimeStampedPVCoordinates initialPVT = new TimeStampedPVCoordinates(epoch, new Vector3D(600,-1000,0), Vector3D.ZERO);
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double targetMeanMotion = 0.0011569;
        final List<TimeStampedPVCoordinates> waypointsCW = RPOModel.CW.computeNaturalCircularWaypoints(
                initialPVT, injectionDate, targetMeanMotion, 50, 10);
        final List<TimeStampedPVCoordinates> waypointsYA = RPOModel.YA.computeNaturalCircularWaypoints(
                initialPVT, injectionDate, targetMeanMotion, 50, 10);

        Assertions.assertEquals(injectionDate.toDouble(),waypointsCW.get(waypointsCW.size()-1).getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-50,0),waypointsCW.get(waypointsCW.size()-1).getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-0.0289225,0,0.05009523948),waypointsCW.get(waypointsCW.size()-1).getVelocity(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(injectionDate.toDouble(),waypointsYA.get(waypointsYA.size()-1).getDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-50,0,0),waypointsYA.get(waypointsYA.size()-1).getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-0.05009523948,-0.0289225),waypointsYA.get(waypointsYA.size()-1).getVelocity(),NUMERICAL_TOLERANCE);
    }


    @Test
    public void FieldLinearWaypointsCalculatorTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-1000,600,200), new Vector3D(0,0,0))));
        final TimeStampedFieldPVCoordinates<Binary64> finalPVT = new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,0,0), new Vector3D(10,10,10))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsCW = RPOModel.CW.computeLinearWaypoints(initialPVT, finalPVT,11);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsYA = RPOModel.YA.computeLinearWaypoints(initialPVT, finalPVT,11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypointsCW.get(i).getDate().toAbsoluteDate().toDouble(), epoch.shiftedBy(i*100).toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getPosition().toVector3D(),new Vector3D(-1000+i*100, 600-i*60,200-i*20),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsCW.get(i).getVelocity().toVector3D(),new Vector3D(i,i,i),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypointsYA.get(i).getDate().toAbsoluteDate().toDouble(), epoch.shiftedBy(i*100).toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getPosition().toVector3D(),new Vector3D(-1000+i*100, 600-i*60,200-i*20),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypointsYA.get(i).getVelocity().toVector3D(),new Vector3D(i,i,i),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void FieldForcedCircularWaypointsCalculatorTestCW() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,-1000,0), new Vector3D(0,0,0))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(initialPVT,new FieldVector3D<>(field, new Vector3D(0,0,0)),new Binary64(100),new Binary64(0),new Binary64(0),new Binary64(1000),10,1,11,new Binary64(1000));
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.CW.computeLinearWaypoints(initialPVT,new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,-100,0), new Vector3D(0,0,0)))), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), linearWaypoints.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(), linearWaypoints.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), linearWaypoints.get(i).getVelocity().toVector3D(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal());
        }
        // Assert that the waypoints on the circular path are correct.
        for (int i = 10; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000 + 1000. / 10 * (i-10)).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),new Vector3D(100 * FastMath.sin(((i-10)+5)*2*FastMath.PI/10),100 * FastMath.cos(((i-10)+5)*2*FastMath.PI/10),0),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(),Vector3D.ZERO,NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void FieldForcedCircularWaypointsCalculatorTestYA() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-1000,0,0), new Vector3D(0,0,0))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(initialPVT,new FieldVector3D<>(field, new Vector3D(0,0,0)),new Binary64(100),new Binary64(0),new Binary64(0),new Binary64(1000),10,1,11,new Binary64(1000));
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.YA.computeLinearWaypoints(initialPVT,new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-100,0,0), new Vector3D(0,0,0)))), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), linearWaypoints.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(), linearWaypoints.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), linearWaypoints.get(i).getVelocity().toVector3D(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal());
        }
        // Assert that the waypoints on the circular path are correct.
        for (int i = 10; i < waypoints.size(); i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000 + 1000. / 10 * (i-10)).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(),new Vector3D(100 * FastMath.cos(((i-10)+5)*2*FastMath.PI/10),0,100 * FastMath.sin(((i-10)+5)*2*FastMath.PI/10)),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(),Vector3D.ZERO,NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void FieldCircularWaypointsWithInclinationTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,-1000,0), new Vector3D(0,0,0))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(initialPVT,new FieldVector3D<>(field, new Vector3D(0,0,0)),new Binary64(100),new Binary64(0.2),new Binary64(0),new Binary64(1000),10,1,11,new Binary64(1000));
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.CW.computeLinearWaypoints(initialPVT,new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,-100,0), new Vector3D(0,0,0)))), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), linearWaypoints.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(), linearWaypoints.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), linearWaypoints.get(i).getVelocity().toVector3D(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal());
        }
        // Assert the final waypoint on the circle is the same as the final waypoint on the linear path.
        TestUtils.validateVector3D(linearWaypoints.get(linearWaypoints.size()-1).getPosition().toVector3D(),waypoints.get(waypoints.size()-1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void FieldForcedCircularWaypointsCalculatorWithRaanTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(-1000,0,0), new Vector3D(0,0,0))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedCircularMotionWaypoints(initialPVT,new FieldVector3D<>(field, new Vector3D(0,0,0)),new Binary64(100),new Binary64(0),new Binary64(0.5),new Binary64(1000),10,1,11,new Binary64(1000));
        // Assert that the waypoints on the linear path towards the circular orbit are correct.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.YA.computeLinearWaypoints(initialPVT,new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1000), new FieldPVCoordinates<>(field, new PVCoordinates(waypoints.get(10).getPosition().toVector3D(), new Vector3D(0,0,0)))), 11);
        for (int i = 0; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i).getDate().toAbsoluteDate().toDouble(), linearWaypoints.get(i).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getPosition().toVector3D(), linearWaypoints.get(i).getPosition().toVector3D(), NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i).getVelocity().toVector3D(), linearWaypoints.get(i).getVelocity().toVector3D(), NUMERICAL_TOLERANCE);
        }
        // Assert that the first waypoint on the circle is the closest to the initial point.
        for (int i = 11; i < waypoints.size()-1; i++) {
            Assertions.assertTrue(waypoints.get(10).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal() <= waypoints.get(i).getPosition().subtract(initialPVT.getPosition()).getNorm().getReal());
        }
        // Assert the final waypoint on the circle is the same as the final waypoint on the linear path.
        TestUtils.validateVector3D(linearWaypoints.get(linearWaypoints.size()-1).getPosition().toVector3D(),waypoints.get(waypoints.size()-1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void FieldForcedCircularWaypointsCalculatorMultiRevolution() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0,-1000,0), new Vector3D(0,0,0))));
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedCircularMotionWaypoints(initialPVT,new FieldVector3D<>(field, new Vector3D(0,0,0)),new Binary64(100),new Binary64(0),new Binary64(0),new Binary64(1000),10,3,11,new Binary64(1000));
        // Assert that the waypoints on successive orbit revolution have same position and velocity and same date shifted by the relative orbital period.
        for (int i = 1; i < 11; i++) {
            Assertions.assertEquals(waypoints.get(i + 10).getDate().shiftedBy(1000.).toAbsoluteDate().toDouble(),waypoints.get(2 * 10 + i).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            Assertions.assertEquals(waypoints.get(i + 10).getDate().shiftedBy(2000.).toAbsoluteDate().toDouble(),waypoints.get(3 * 10 + i).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getPosition().toVector3D(),waypoints.get(2 * 10 + i).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getPosition().toVector3D(),waypoints.get(3 * 10 + i).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getVelocity().toVector3D(),waypoints.get(2 * 10 + i).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(waypoints.get(i + 10).getVelocity().toVector3D(),waypoints.get(3 * 10 + i).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void FieldCircularNavigationWaypointsTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(600,-1000,0), Vector3D.ZERO)));
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 targetMeanMotion = new Binary64(0.0011569);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsCW = RPOModel.CW.computeCircularNaturalCircumnavigationWaypoints(
                initialPVT, injectionDate, targetMeanMotion, new Binary64(50), new Binary64(0.3), 10);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsYA = RPOModel.YA.computeCircularNaturalCircumnavigationWaypoints(
                initialPVT, injectionDate, targetMeanMotion, new Binary64(50), new Binary64(0.3), 10);

    Assertions.assertEquals(injectionDate.toAbsoluteDate().toDouble(),waypointsCW.get(waypointsCW.size()-1).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-2*50,0),waypointsCW.get(waypointsCW.size()-1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(injectionDate.toAbsoluteDate().toDouble(),waypointsYA.get(waypointsYA.size()-1).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-2 * 50,0,0),waypointsYA.get(waypointsYA.size()-1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void FieldCircularNaturalWaypointsTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final TimeStampedFieldPVCoordinates<Binary64> initialPVT = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(600,-1000,0), Vector3D.ZERO)));
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 targetMeanMotion = new Binary64(0.0011569);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsCW = RPOModel.CW.computeNaturalCircularWaypoints(
                initialPVT, injectionDate, targetMeanMotion, new Binary64(50), 10);
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypointsYA = RPOModel.YA.computeNaturalCircularWaypoints(
                initialPVT, injectionDate, targetMeanMotion, new Binary64(50), 10);

        Assertions.assertEquals(injectionDate.toAbsoluteDate().toDouble(),waypointsCW.get(waypointsCW.size()-1).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-50,0),waypointsCW.get(waypointsCW.size()-1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-0.0289225,0,0.05009523948),waypointsCW.get(waypointsCW.size()-1).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);

        Assertions.assertEquals(injectionDate.toAbsoluteDate().toDouble(),waypointsYA.get(waypointsYA.size()-1).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(-50,0,0),waypointsYA.get(waypointsYA.size()-1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(0,-0.05009523948,-0.0289225),waypointsYA.get(waypointsYA.size()-1).getVelocity().toVector3D(),NUMERICAL_TOLERANCE);
    }

}
