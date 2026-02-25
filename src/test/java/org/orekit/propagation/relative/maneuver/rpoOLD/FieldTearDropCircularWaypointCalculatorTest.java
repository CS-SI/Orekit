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
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.relative.maneuver.FieldClohessyWiltshireManeuver;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

import java.util.List;

public class FieldTearDropCircularWaypointCalculatorTest {

    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-7;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void FieldTearDropWaypointTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetMeanMotion = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldTeardropCircularWaypointCalculator<Binary64> tearDropCalculator = new FieldTeardropCircularWaypointCalculator<>(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypoints = tearDropCalculator.computeTearDropWaypoints(epoch.shiftedBy(1000.));
        //Assert that the first waypoint is located at (turnAroundDistance, 0, 0) and at injectionDate.
        Assertions.assertEquals(tearDropWaypoints.get(0).getDate().toAbsoluteDate().toDouble(),epoch.shiftedBy(1000.).toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(turnAroundDistance.getReal(),0,0),tearDropWaypoints.get(0).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
        // Assert that the following waypoints are located at (maneuverDistance, 0, 0) and spaced by the computed tearDropPeriod.
        final Binary64 tearDropPeriod = tearDropCalculator.computeRelativeOrbitalPeriod();
        for (int i = 0; i < numberOfOrbits; i++) {
            Assertions.assertEquals(epoch.shiftedBy(1000 + tearDropPeriod.getReal() * i + tearDropPeriod.getReal() / 2).toAbsoluteDate().toDouble(), tearDropWaypoints.get(i+1).getDate().toAbsoluteDate().toDouble(),NUMERICAL_TOLERANCE);
            TestUtils.validateVector3D(new Vector3D(maneuverDistance.getReal(),0,0),tearDropWaypoints.get(i+1).getPosition().toVector3D(),NUMERICAL_TOLERANCE);
        }
    }

    @Test
    public void ImpulseAtManeuverPointTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetMeanMotion = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldTeardropCircularWaypointCalculator<Binary64> tearDropCalculator = new FieldTeardropCircularWaypointCalculator<>(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final TimeStampedFieldPVCoordinates<Binary64> waypoint = new TimeStampedFieldPVCoordinates<>(epoch,new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(1,1,1), new Vector3D(1,0,0))));
        final FieldVector3D<Binary64> deltaV = tearDropCalculator.computeImpulseAtManeuverPoint(waypoint);
        Assertions.assertEquals(2,deltaV.getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    @Test
    public void ImpulseAtManeuverPointInertialOtherFrameTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 targetMeanMotion = new Binary64(0.0011569);// Mean motion of target's orbit
        final Binary64 rTarget = ((new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(targetMeanMotion.multiply(targetMeanMotion))).pow(1./3.);        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH), new Binary64(Constants.EIGEN5C_EARTH_MU));
        final Binary64 turnAroundDistance = new Binary64(50);
        final Binary64 maneuverDistance = new Binary64(100);
        final int numberOfOrbits = 5;
        final FieldTeardropCircularWaypointCalculator<Binary64> tearDropCalculator = new FieldTeardropCircularWaypointCalculator<>(targetMeanMotion, turnAroundDistance, maneuverDistance, numberOfOrbits);
        final TimeStampedFieldPVCoordinates<Binary64> waypoint = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(1,1,1), new Vector3D(1,0,0))));
        final FieldVector3D<Binary64> deltaV = tearDropCalculator.computeImpulseAtManeuverPointOtherFrame(waypoint,targetOrbit,FramesFactory.getGCRF());
        Assertions.assertEquals(2,deltaV.getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0,deltaV.getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    // Keplerian propagation of a TearDrop maneuver scenario. Chaser goes from an initial position to the turnAroundPoint using Linear Maneuvers.
    // Test if the chaser position at the end of the propagation (5 teardrop periods) is at the maneuverPoint.
    @Test
    public void TearDropPropagationTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 rTarget = ((new Binary64(Constants.EIGEN5C_EARTH_MU)).divide(n.multiply(n))).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, new Binary64(Constants.EIGEN5C_EARTH_MU));

        // Start and end conditions of the transfer, expressed in the target's LOF
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())), new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        // Injection Date in the teardrop Orbit.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit, pvtChaserInitial);
        // Compute the teardrop waypoints.
        final FieldTeardropCircularWaypointCalculator<Binary64> teardropCalculator = new FieldTeardropCircularWaypointCalculator<>(targetOrbit.getKeplerianMeanMotion(),new Binary64(50),new Binary64(100),5);
        final Binary64 tearDropRelativePeriod = teardropCalculator.computeRelativeOrbitalPeriod();
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypoints = teardropCalculator.computeTearDropWaypoints(injectionDate);
        // Compute Linear Waypoints.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.CW.computeLinearWaypoints(pvtChaserInitial,tearDropWaypoints.get(0),10);
        // Define the propagator.
        final FieldKeplerianPropagator<Binary64> propagator =new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Creation of the maneuvers corresponding to the impulses of the linear transfer.
        final List<FieldClohessyWiltshireManeuver<Binary64>> maneuvers = (new FieldMultiRelativeTransfersCW<>(linearWaypoints,targetOrbit)).computeRelativeManeuvers(cwProvider);
        for (FieldClohessyWiltshireManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Creation of the maneuvers at the maneuver point of the teardrop.
        final List<FieldClohessyWiltshireManeuver<Binary64>> tearDropManeuvers = teardropCalculator.computeTearDropRelativeManeuvers(injectionDate, cwProvider);
        for (FieldClohessyWiltshireManeuver<Binary64> maneuver: tearDropManeuvers) {
            propagator.addEventDetector(maneuver);
        }

        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(injectionDate.shiftedBy((5+1./2.)*tearDropRelativePeriod.getReal()));
        final Binary64[] finalChaser = cwProvider.getAdditionalData(finalTarget);
        Assertions.assertEquals(100, finalChaser[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[2].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getX().getReal(),finalChaser[3].getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getY().getReal(),finalChaser[4].getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getZ().getReal(),finalChaser[5].getReal(),NUMERICAL_TOLERANCE);

    }
}
