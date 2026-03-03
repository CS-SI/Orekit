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
package org.orekit.propagation.relative.maneuver.rpo;

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
import org.orekit.forces.maneuvers.FieldImpulseManeuver;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.propagation.relative.clohessywiltshire.FieldClohessyWiltshireProvider;
import org.orekit.propagation.relative.maneuver.*;
import org.orekit.propagation.relative.maneuver.rpoOLD.TeardropCircularWaypointCalculator;
import org.orekit.propagation.relative.yamanakaankersen.FieldYamanakaAnkersenProvider;
import org.orekit.propagation.relative.yamanakaankersen.YamanakaAnkersenProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.*;

import java.util.ArrayList;
import java.util.List;

public class RPOModelTest {
    // Tolerance for floating point numbers comparison
    public static final double NUMERICAL_TOLERANCE = 1e-7;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void getRBarDirectionTest() {
        TestUtils.validateVector3D(Vector3D.PLUS_I, RPOModel.CW.getRBarDirection(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.MINUS_K, RPOModel.YA.getRBarDirection(), NUMERICAL_TOLERANCE);
    }

    @Test
    public void getVBarDirectionTest() {
        TestUtils.validateVector3D(Vector3D.PLUS_J, RPOModel.CW.getVBarDirection(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.PLUS_I, RPOModel.YA.getVBarDirection(), NUMERICAL_TOLERANCE);
    }

    @Test
    public void getOutOfPlaneDirectionTest() {
        TestUtils.validateVector3D(Vector3D.PLUS_K, RPOModel.CW.getOutOfPlaneDirection(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.MINUS_J, RPOModel.YA.getOutOfPlaneDirection(), NUMERICAL_TOLERANCE);
    }

    @Test
    public void getLOFTypeTest() {
        final RPOModel rpoCW = RPOModel.CW;
        final RPOModel rpoYA = RPOModel.YA;
        Assertions.assertEquals(LOFType.QSW.getName(),rpoCW.getLOFType().getName());
        Assertions.assertEquals(LOFType.LVLH_CCSDS.getName(),rpoYA.getLOFType().getName());

    }

    /**
     * Keplerian propagation of a linear transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation is the same as the final position on linear path.
     */
    @Test
    public void LinearTransferPropagationCWTest() {
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

        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.CW.computeForcedLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        final List<RelativeManeuver> maneuvers = RPOModel.CW.computeForcedManeuvers(waypoints,pvtChaserInitial.getVelocity(),targetOrbit,cwProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final SpacecraftState finalState = propagator.propagate(finalDate.shiftedBy(1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final double[] finalChaser = cwProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0],pvtChaserFinal.getPosition().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1],pvtChaserFinal.getPosition().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2],pvtChaserFinal.getPosition().getZ(),NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a linear transfer using Yamanaka-Ankersen maneuvers.
     * Test if the chaser position at the end of the propagation is the same as the final position on linear path.
     */
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
        final List<TimeStampedPVCoordinates> waypoints = RPOModel.YA.computeForcedLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<RelativeManeuver> maneuvers = RPOModel.YA.computeForcedManeuvers(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, yaProvider);
        for (RelativeManeuver maneuver: maneuvers) {
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

    /**
     * Keplerian propagation of a Forced Circular transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation (3 relative orbit's periods) is the same as the first waypoint on the circle.
     */
    @Test
    public void ForcedCircularPropagationCWTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0., 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Choose the RPO model.
        final RPOModel rpo = RPOModel.CW;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -100, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final List<TimeStampedPVCoordinates> waypoints = rpo.computeForcedCircularMotionWaypoints(pvtChaserInitial.getDate(), new Vector3D(0,0,0),100.,0.,0.,1000.,10,3,0.,false);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, cwProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the scenario (500s of linear duration + 3 orbits of 1000s period).
        final SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(3000 + 1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final double[] finalChaser = cwProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0],waypoints.get(0).getPosition().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1],waypoints.get(0).getPosition().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2],waypoints.get(0).getPosition().getZ(),NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Forced Circular transfer using Yamanaka-Ankersen maneuver.
     * Test if the chaser position at the end of the propagation (3 relative orbit's periods) is the same as the first waypoint on the circle.
     */
    @Test
    public void ForcedCircularPropagationYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.1, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Choice of the RPOModel to use.
        final RPOModel rpo = RPOModel.YA;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(-100, 0, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final List<TimeStampedPVCoordinates> waypoints = rpo.computeForcedCircularMotionWaypoints(pvtChaserInitial.getDate(), new Vector3D(0,0,0),100.,0.,0.,1000.,10,3,0.,false);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, yaProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the scenario (500s of linear duration + 3 orbits of 1000s period).
        final SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(3000 + 1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final double[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0],waypoints.get(0).getPosition().getX(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1],waypoints.get(0).getPosition().getY(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2],waypoints.get(0).getPosition().getZ(), NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Natural Circumnavigation transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
     */
    @Test
    public void NaturalCircumnavigationCWTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        //Choice of the RPOModel.
        final RPOModel rpo = RPOModel.CW;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double relativeSemiMajorAxis = 100;
        // Compute the chaser pvt at injection date (just after the injection).
        final TimeStampedPVCoordinates injectionPVT = rpo.computeNaturalCircumnavigationInjectionCircular(injectionDate, 100, 0.3, n);
        // Compute chaser linear waypoints to go linearly from the initial position to the injection point.
        final List<TimeStampedPVCoordinates> linearWaypoints = rpo.computeForcedLinearWaypoints(pvtChaserInitial, injectionPVT, 10);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Compute the linear maneuvers to the injection point.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(linearWaypoints,pvtChaserInitial.getVelocity(),targetOrbit,cwProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final SpacecraftState targetAtInjection = propagator.propagate(injectionDate);
        final double[] chaserBeforeInjection = cwProvider.getAdditionalData(targetAtInjection);
        final Vector3D velocityBeforeInjection = new Vector3D(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final Vector3D deltaV = injectionPVT.getVelocity().subtract(velocityBeforeInjection);
        final RelativeManeuver injectionManeuver = new ClohessyWiltshireManeuver(new DateDetector(injectionDate),deltaV,cwProvider);
        // Reset chaser initial PVT and propagator initial state.
        cwProvider.setInitialChaserPVTLof(pvtChaserInitial);
        propagator.resetInitialState(new SpacecraftState(targetOrbit));
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final SpacecraftState finalTarget = propagator.propagate(injectionDate.shiftedBy(2 * FastMath.PI / n));
        final double[] chaserFinal = cwProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(0, -2 * semiMinorAxis, 0)
        Assertions.assertEquals(0, chaserFinal[0], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-relativeSemiMajorAxis, chaserFinal[1], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2], NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Natural Circumnavigation transfer using Yamanaka-Ankersen maneuver.
     * Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
     */
    @Test
    public void NaturalCircumnavigationYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0, 0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        //Choice of the RPOModel.
        final RPOModel rpo = RPOModel.YA;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Chaser PVCoordinates provider.
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final double relativeSemiMajorAxis = 100;
        // Compute the chaser pvt at injection date (just after the injection).
        final TimeStampedPVCoordinates injectionPVT = rpo.computeNaturalCircumnavigationInjectionCircular(injectionDate, 100, 0.3, n);
        // Compute chaser linear waypoints to go linearly from the initial position to the injection point.
        final List<TimeStampedPVCoordinates> linearWaypoints = rpo.computeForcedLinearWaypoints(pvtChaserInitial, injectionPVT, 10);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Compute the linear maneuvers to the injection point.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(linearWaypoints,pvtChaserInitial.getVelocity(),targetOrbit,yaProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final SpacecraftState targetAtInjection = propagator.propagate(injectionDate);
        final double[] chaserBeforeInjection = yaProvider.getAdditionalData(targetAtInjection);
        final Vector3D velocityBeforeInjection = new Vector3D(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final Vector3D deltaV = injectionPVT.getVelocity().subtract(velocityBeforeInjection);
        final RelativeManeuver injectionManeuver = new YamanakaAnkersenManeuver(new DateDetector(injectionDate),deltaV,yaProvider);
        // Reset chaser initial PVT, target Orbit and propagator initial state.
        yaProvider.setInitialChaserPVTLof(pvtChaserInitial);
        yaProvider.setTargetOrbit(targetOrbit);
        propagator.resetInitialState(new SpacecraftState(targetOrbit));
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final SpacecraftState finalTarget = propagator.propagate(injectionDate.shiftedBy(2 * FastMath.PI / n));
        final double[] chaserFinal = yaProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(-2 * semiMinorAxis, 0, 0)
        Assertions.assertEquals(-relativeSemiMajorAxis, chaserFinal[0], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[1], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2], NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a TearDrop maneuver scenario. Chaser goes from an initial position to the turnAroundPoint using Linear Maneuvers.
     * Test if the chaser position at the end of the propagation (5 teardrop periods) is at the maneuverPoint.
     */
    @Test
    public void TearDropPropagationCWTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Choice of the RPOModel.
        final RPOModel rpo = RPOModel.CW;
        // Start and end conditions of the transfer, expressed in the target's LOF
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Injection Date in the teardrop Orbit.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitial);
        // Compute the teardrop waypoints.
        final TeardropCircularWaypointCalculator teardropCalculator = new TeardropCircularWaypointCalculator(targetOrbit.getKeplerianMeanMotion(),50,100,5);
        final double tearDropRelativePeriod = teardropCalculator.computeRelativeOrbitalPeriod();
        final List<TimeStampedPVCoordinates> tearDropWaypoints = rpo.computeTeardropWaypoints(injectionDate, targetOrbit, 50, 100, 5);
        // Compute Linear Waypoints.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.CW.computeForcedLinearWaypoints(pvtChaserInitial,tearDropWaypoints.get(0),10);
        // Define the propagator.
        final KeplerianPropagator propagator =new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Creation of the maneuvers corresponding to the impulses of the linear transfer.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(linearWaypoints, pvtChaserInitial.getVelocity(), targetOrbit, cwProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final SpacecraftState targetAtInjection = propagator.propagate(injectionDate);
        final double[] chaserBeforeInjection = cwProvider.getAdditionalData(targetAtInjection);
        final Vector3D velocityBeforeInjection = new Vector3D(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final Vector3D deltaV = tearDropWaypoints.get(0).getVelocity().subtract(velocityBeforeInjection);
        final RelativeManeuver injectionManeuver = new ClohessyWiltshireManeuver(new DateDetector(injectionDate),deltaV,cwProvider);
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Reset chaser initial PVT and propagator initial state.
        cwProvider.setInitialChaserPVTLof(pvtChaserInitial);
        propagator.resetInitialState(new SpacecraftState(targetOrbit));
        // Creation of the maneuvers at the maneuver point of the teardrop.
        final List<RelativeManeuver> tearDropManeuvers = rpo.computeTeardropManeuvers(tearDropWaypoints, cwProvider);
        for (RelativeManeuver maneuver: tearDropManeuvers) {
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

    /**
     * Keplerian propagation of a TearDrop maneuver scenario. Chaser goes from an initial position to the turnAroundPoint using Linear Maneuvers.
     * Test if the chaser position at the end of the propagation (5 teardrop periods) is at the maneuverPoint.
     */
    @Test
    public void TearDropPropagationYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        // Choice of the RPOModel.
        final RPOModel rpo = RPOModel.YA;
        // Start and end conditions of the transfer, expressed in the target's LOF
        final TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        // Injection Date in the teardrop Orbit.
        final AbsoluteDate injectionDate = epoch.shiftedBy(1000.);
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Compute the teardrop waypoints.
        final TeardropCircularWaypointCalculator teardropCalculator = new TeardropCircularWaypointCalculator(targetOrbit.getKeplerianMeanMotion(),50,100,5);
        final double tearDropRelativePeriod = teardropCalculator.computeRelativeOrbitalPeriod();
        final List<TimeStampedPVCoordinates> tearDropWaypoints = rpo.computeTeardropWaypoints(injectionDate, targetOrbit, 50, 100, 5);
        // Compute Linear Waypoints.
        final List<TimeStampedPVCoordinates> linearWaypoints = RPOModel.YA.computeForcedLinearWaypoints(pvtChaserInitial,tearDropWaypoints.get(0),10);
        // Define the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the linear transfer.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(linearWaypoints, pvtChaserInitial.getVelocity(), targetOrbit, yaProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final SpacecraftState targetAtInjection = propagator.propagate(injectionDate);
        final double[] chaserBeforeInjection = yaProvider.getAdditionalData(targetAtInjection);
        final Vector3D velocityBeforeInjection = new Vector3D(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final Vector3D deltaV = tearDropWaypoints.get(0).getVelocity().subtract(velocityBeforeInjection);
        final RelativeManeuver injectionManeuver = new YamanakaAnkersenManeuver(new DateDetector(injectionDate),deltaV,yaProvider);
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Reset chaser initial PVT, target orbit and propagator initial state.
        yaProvider.setInitialChaserPVTLof(pvtChaserInitial);
        yaProvider.setTargetOrbit(targetOrbit);
        propagator.resetInitialState(new SpacecraftState(targetOrbit));
        // Creation of the maneuvers at the maneuver point of the teardrop.
        final List<RelativeManeuver> tearDropManeuvers = rpo.computeTeardropManeuvers(tearDropWaypoints, yaProvider);
        for (RelativeManeuver maneuver: tearDropManeuvers) {
            propagator.addEventDetector(maneuver);
        }

        final SpacecraftState finalTarget = propagator.propagate(injectionDate.shiftedBy((5+1./2.)*tearDropRelativePeriod));
        final double[] finalChaser = yaProvider.getAdditionalData(finalTarget);
        Assertions.assertEquals(0, finalChaser[0], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[1], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-100, finalChaser[2], NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getX(),finalChaser[3],NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getY(),finalChaser[4],NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getZ(),finalChaser[5],NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a linear transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation is the same as the final position on linear path.
     */
    @Test
    public void FieldLinearTransferPropagationCWTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(
                new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())),
                new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(0, -1.0e3, 0)), new FieldVector3D<>(field, new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate,
                new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(200, 0, 0)), FieldVector3D.getZero(field)));

        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.CW.computeForcedLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        final List<FieldRelativeManeuver<Binary64>> maneuvers = RPOModel.CW.computeForcedManeuvers(waypoints,pvtChaserInitial.getVelocity(),targetOrbit,cwProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(finalDate.shiftedBy(1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final Binary64[] finalChaser = cwProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0].getReal(),pvtChaserFinal.getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1].getReal(),pvtChaserFinal.getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2].getReal(),pvtChaserFinal.getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a linear transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation is the same as the final position on linear path.
     */
    @Test
    public void FieldLinearTransferPropagationYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(2000.);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(
                new FieldAbsoluteDate<>(field, new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC())),
                new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(0, -1.0e3, 0)), new FieldVector3D<>(field, new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate,
                new FieldPVCoordinates<>(new FieldVector3D<>(field, new Vector3D(200, 0, 0)), FieldVector3D.getZero(field)));

        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = RPOModel.YA.computeForcedLinearWaypoints(pvtChaserInitial, pvtChaserFinal, 6);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        final List<FieldRelativeManeuver<Binary64>> maneuvers = RPOModel.YA.computeForcedManeuvers(waypoints,pvtChaserInitial.getVelocity(),targetOrbit,yaProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(finalDate.shiftedBy(1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0].getReal(),pvtChaserFinal.getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1].getReal(),pvtChaserFinal.getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2].getReal(),pvtChaserFinal.getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Forced Circular transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation (3 relative orbit's periods) is the same as the first waypoint on the circle.
     */
    @Test
    public void FieldForcedCircularPropagationCWTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        // Choose the RPO model.
        final RPOModel rpo = RPOModel.CW;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitialReal = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -100, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitialReal);
        // Chaser PVCoordinates provider.
        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = rpo.computeForcedCircularMotionWaypoints(
                pvtChaserInitial.getDate(),
                FieldVector3D.getZero(field),
                new Binary64(100.),field.getZero(),field.getZero(),new Binary64(1000.),10,
                3,field.getZero(),false);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, cwProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the scenario (500s of linear duration + 3 orbits of 1000s period).
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(epoch.shiftedBy(3000 + 1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final Binary64[] finalChaser = cwProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0].getReal(),waypoints.get(0).getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1].getReal(),waypoints.get(0).getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2].getReal(),waypoints.get(0).getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Forced Circular transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation (3 relative orbit's periods) is the same as the first waypoint on the circle.
     */
    @Test
    public void FieldForcedCircularPropagationYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        // Choose the RPO model.
        final RPOModel rpo = RPOModel.YA;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitialReal = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(-100, 0, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitialReal);
        // Chaser PVCoordinates provider.
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = rpo.computeForcedCircularMotionWaypoints(
                pvtChaserInitial.getDate(),
                FieldVector3D.getZero(field),
                new Binary64(100.),field.getZero(),field.getZero(),new Binary64(1000.),10,
                3,field.getZero(),false);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the transfers.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(waypoints, pvtChaserInitial.getVelocity(), targetOrbit, yaProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target state to the end of the scenario (500s of linear duration + 3 orbits of 1000s period).
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(epoch.shiftedBy(3000 + 1e-12));
        // Assert that the final chaser position PVT is the same as the final waypoint of the linear path.
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalState);
        Assertions.assertEquals(finalChaser[0].getReal(),waypoints.get(0).getPosition().getX().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[1].getReal(),waypoints.get(0).getPosition().getY().getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaser[2].getReal(),waypoints.get(0).getPosition().getZ().getReal(),NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Natural Circumnavigation transfer using Clohessy-Wiltshire maneuver.
     * Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
     */
    @Test
    public void FieldNaturalCircumnavigationCWTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        //Choice of the RPOModel.
        final RPOModel rpo = RPOModel.CW;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitialReal = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitialReal);
        // Chaser PVCoordinates provider.
        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 relativeSemiMajorAxis = new Binary64(100);
        // Compute the chaser pvt at injection date (just after the injection).
        final TimeStampedFieldPVCoordinates<Binary64> injectionPVT = rpo.computeNaturalCircumnavigationInjectionCircular(injectionDate, relativeSemiMajorAxis, new Binary64(0.3), n);
        // Compute chaser linear waypoints to go linearly from the initial position to the injection point.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = rpo.computeForcedLinearWaypoints(pvtChaserInitial, injectionPVT, 10);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Compute the linear maneuvers to the injection point.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(linearWaypoints,pvtChaserInitial.getVelocity(),targetOrbit,cwProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final FieldSpacecraftState<Binary64> targetAtInjection = propagator.propagate(injectionDate);
        final Binary64[] chaserBeforeInjection = cwProvider.getAdditionalData(targetAtInjection);
        final FieldVector3D<Binary64> velocityBeforeInjection = new FieldVector3D<>(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final FieldVector3D<Binary64> deltaV = injectionPVT.getVelocity().subtract(velocityBeforeInjection);
        final FieldRelativeManeuver<Binary64> injectionManeuver = new FieldClohessyWiltshireManeuver<>(new FieldDateDetector<>(injectionDate),deltaV,cwProvider);
        // Reset chaser initial PVT and propagator initial state.
        cwProvider.setInitialChaserPVTLof(pvtChaserInitial);
        propagator.resetInitialState(new FieldSpacecraftState<>(targetOrbit));
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(injectionDate.shiftedBy(2 * FastMath.PI / n.getReal()));
        final Binary64[] chaserFinal = cwProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(0, -2 * semiMinorAxis, 0)
        Assertions.assertEquals(0, chaserFinal[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-relativeSemiMajorAxis.getReal(), chaserFinal[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2].getReal(), NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a Natural Circumnavigation transfer using Yamanaka-Ankersen maneuver.
     * Test if the chaser position at the end of the propagation (one target orbit's period) is the same as the injection point.
     */
    @Test
    public void FieldNaturalCircumnavigationYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        //Choice of the RPOModel.
        final RPOModel rpo = RPOModel.YA;
        // Start condition in the target's LOF.
        final TimeStampedPVCoordinates pvtChaserInitialReal = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitialReal);
        // Chaser PVCoordinates provider.
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the path and associated transfers.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final Binary64 relativeSemiMajorAxis = new Binary64(100);
        // Compute the chaser pvt at injection date (just after the injection).
        final TimeStampedFieldPVCoordinates<Binary64> injectionPVT = rpo.computeNaturalCircumnavigationInjectionCircular(injectionDate, relativeSemiMajorAxis, new Binary64(0.3), n);
        // Compute chaser linear waypoints to go linearly from the initial position to the injection point.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = rpo.computeForcedLinearWaypoints(pvtChaserInitial, injectionPVT, 10);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Compute the linear maneuvers to the injection point.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(linearWaypoints,pvtChaserInitial.getVelocity(),targetOrbit,yaProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final FieldSpacecraftState<Binary64> targetAtInjection = propagator.propagate(injectionDate);
        final Binary64[] chaserBeforeInjection = yaProvider.getAdditionalData(targetAtInjection);
        final FieldVector3D<Binary64> velocityBeforeInjection = new FieldVector3D<>(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final FieldVector3D<Binary64> deltaV = injectionPVT.getVelocity().subtract(velocityBeforeInjection);
        final FieldRelativeManeuver<Binary64> injectionManeuver = new FieldYamanakaAnkersenManeuver<>(new FieldDateDetector<>(injectionDate),deltaV,yaProvider);
        // Reset chaser initial PVT, target Orbit and propagator initial state.
        yaProvider.setInitialChaserPVTLof(pvtChaserInitial);
        yaProvider.setTargetOrbit(targetOrbit);
        propagator.resetInitialState(new FieldSpacecraftState<>(targetOrbit));
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Propagate the target Orbit to the end of the scenario: one target orbit period after the injection Date.
        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(injectionDate.shiftedBy(2 * FastMath.PI / n.getReal()));
        final Binary64[] chaserFinal = yaProvider.getAdditionalData(finalTarget);
        // Assert that the final chaser PVT is located at the injection Point. P(-2 * semiMinorAxis, 0, 0)
        Assertions.assertEquals(-relativeSemiMajorAxis.getReal(), chaserFinal[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, chaserFinal[2].getReal(), NUMERICAL_TOLERANCE);
    }

    /**
     * Keplerian propagation of a TearDrop maneuver scenario. Chaser goes from an initial position to the turnAroundPoint using Linear Maneuvers.
     * Test if the chaser position at the end of the propagation (5 teardrop periods) is at the maneuverPoint.
     */
    @Test
    public void FieldTearDropPropagationCWTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        // Choice of the RPOModel.
        final RPOModel rpo = RPOModel.CW;
        // Start and end conditions of the transfer, expressed in the target's LOF
        final TimeStampedPVCoordinates pvtChaserInitialReal = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitialReal);
        // Injection Date in the teardrop Orbit.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit, pvtChaserInitial);
        // Compute the teardrop waypoints.
        final FieldTeardropCircularWaypointCalculator<Binary64> teardropCalculator = new FieldTeardropCircularWaypointCalculator<>(targetOrbit.getKeplerianMeanMotion(),new Binary64(50),new Binary64(100),5);
        final Binary64 tearDropRelativePeriod = teardropCalculator.computeRelativeOrbitalPeriod();
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypoints = rpo.computeTeardropWaypoints(injectionDate, targetOrbit, new Binary64(50), new Binary64(100), 5);
        // Compute Linear Waypoints.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.CW.computeForcedLinearWaypoints(pvtChaserInitial,tearDropWaypoints.get(0),10);
        // Define the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Creation of the maneuvers corresponding to the impulses of the linear transfer.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(linearWaypoints, pvtChaserInitial.getVelocity(), targetOrbit, cwProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final FieldSpacecraftState<Binary64> targetAtInjection = propagator.propagate(injectionDate);
        final Binary64[] chaserBeforeInjection = cwProvider.getAdditionalData(targetAtInjection);
        final FieldVector3D<Binary64> velocityBeforeInjection = new FieldVector3D<>(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final FieldVector3D<Binary64> deltaV = tearDropWaypoints.get(0).getVelocity().subtract(velocityBeforeInjection);
        final FieldRelativeManeuver<Binary64> injectionManeuver = new FieldClohessyWiltshireManeuver<>(new FieldDateDetector<>(injectionDate),deltaV,cwProvider);
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Reset chaser initial PVT and propagator initial state.
        cwProvider.setInitialChaserPVTLof(pvtChaserInitial);
        propagator.resetInitialState(new FieldSpacecraftState<>(targetOrbit));
        // Creation of the maneuvers at the maneuver point of the teardrop.
        final List<FieldRelativeManeuver<Binary64>> tearDropManeuvers = rpo.computeTeardropManeuvers(tearDropWaypoints, cwProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: tearDropManeuvers) {
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

    /**
     * Keplerian propagation of a TearDrop maneuver scenario. Chaser goes from an initial position to the turnAroundPoint using Linear Maneuvers.
     * Test if the chaser position at the end of the propagation (5 teardrop periods) is at the maneuverPoint.
     */
    @Test
    public void FieldTearDropPropagationYATest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);

        // Target's orbit.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        // Choice of the RPOModel.
        final RPOModel rpo = RPOModel.YA;
        // Start and end conditions of the transfer, expressed in the target's LOF
        final TimeStampedPVCoordinates pvtChaserInitialReal = new TimeStampedPVCoordinates(new AbsoluteDate(2000,1,1,11,58,55.816, TimeScalesFactory.getUTC()), new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(field, pvtChaserInitialReal);
        // Injection Date in the teardrop Orbit.
        final FieldAbsoluteDate<Binary64> injectionDate = epoch.shiftedBy(1000.);
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Compute the teardrop waypoints.
        final FieldTeardropCircularWaypointCalculator<Binary64> teardropCalculator = new FieldTeardropCircularWaypointCalculator<>(targetOrbit.getKeplerianMeanMotion(),new Binary64(50), new Binary64(100),5);
        final Binary64 tearDropRelativePeriod = teardropCalculator.computeRelativeOrbitalPeriod();
        final List<TimeStampedFieldPVCoordinates<Binary64>> tearDropWaypoints = rpo.computeTeardropWaypoints(injectionDate, targetOrbit, new Binary64(50), new Binary64(100), 5);
        // Compute Linear Waypoints.
        final List<TimeStampedFieldPVCoordinates<Binary64>> linearWaypoints = RPOModel.YA.computeForcedLinearWaypoints(pvtChaserInitial,tearDropWaypoints.get(0),10);
        // Define the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Creation of the maneuvers corresponding to the impulses of the linear transfer.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(linearWaypoints, pvtChaserInitial.getVelocity(), targetOrbit, yaProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Propagate the target orbit to the injection point.
        final FieldSpacecraftState<Binary64> targetAtInjection = propagator.propagate(injectionDate);
        final Binary64[] chaserBeforeInjection = yaProvider.getAdditionalData(targetAtInjection);
        final FieldVector3D<Binary64> velocityBeforeInjection = new FieldVector3D<>(chaserBeforeInjection[3], chaserBeforeInjection[4], chaserBeforeInjection[5]);
        // Compute the relative maneuver at injection date to insert the chaser in the relative natural orbit.
        final FieldVector3D<Binary64> deltaV = tearDropWaypoints.get(0).getVelocity().subtract(velocityBeforeInjection);
        final FieldRelativeManeuver<Binary64> injectionManeuver = new FieldYamanakaAnkersenManeuver<>(new FieldDateDetector<>(injectionDate),deltaV,yaProvider);
        // Add the injection maneuver to the propagator.
        propagator.addEventDetector(injectionManeuver);
        // Reset chaser initial PVT, target orbit and propagator initial state.
        yaProvider.setInitialChaserPVTLof(pvtChaserInitial);
        yaProvider.setTargetOrbit(targetOrbit);
        propagator.resetInitialState(new FieldSpacecraftState<>(targetOrbit));
        // Creation of the maneuvers at the maneuver point of the teardrop.
        final List<FieldRelativeManeuver<Binary64>> tearDropManeuvers = rpo.computeTeardropManeuvers(tearDropWaypoints, yaProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: tearDropManeuvers) {
            propagator.addEventDetector(maneuver);
        }

        final FieldSpacecraftState<Binary64> finalTarget = propagator.propagate(injectionDate.shiftedBy((5+1./2.)*tearDropRelativePeriod.getReal()));
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalTarget);
        Assertions.assertEquals(0, finalChaser[0].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(0, finalChaser[1].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(-100, finalChaser[2].getReal(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getX().getReal(),finalChaser[3].getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getY().getReal(),finalChaser[4].getReal(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(tearDropWaypoints.get(tearDropWaypoints.size()-1).getVelocity().getZ().getReal(),finalChaser[5].getReal(),NUMERICAL_TOLERANCE);
    }

    /**
     * Compare transformation of relative maneuvers to impulse maneuvers. Assess the transform of the deltaV in the
     * method convertToImpulseManeuver gives the same result as the frame transform of the deltaV vector itself.
     */
    @Test
    public void convertToImpulseManeuverTest() {
        // Define dates of the maneuvers.
        final List<AbsoluteDate> maneuverDates = new ArrayList<>();
        final AbsoluteDate firstManeuverDate = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate secondManeuverDate = firstManeuverDate.shiftedBy(1000);
        final AbsoluteDate thirdManeuverDate = firstManeuverDate.shiftedBy(2000);
        maneuverDates.add(firstManeuverDate);
        maneuverDates.add(secondManeuverDate);
        maneuverDates.add(thirdManeuverDate);
        // Define date detectors of the maneuvers.
        final DateDetector firstManeuverDetector = new DateDetector(firstManeuverDate);
        final DateDetector secondManeuverDetector = new DateDetector(secondManeuverDate);
        final DateDetector thirdManeuverDetector = new DateDetector(thirdManeuverDate);
        // Define randomly the deltaV vectors.
        final Vector3D firstDeltaV = new Vector3D(10, 13, 18);
        final Vector3D secondDeltaV = new Vector3D(-5, 65, -8.6);
        final Vector3D thirdDeltaV = new Vector3D(0, 15, 2);
        // Define target orbit and relative provider.
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(7000e3, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), firstManeuverDate, Constants.EIGEN5C_EARTH_MU);
        final RPOModel rpoCW = RPOModel.CW;
        final RPOModel rpoYA = RPOModel.YA;
        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit);
        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit);
        // Define the maneuvers.
        final ClohessyWiltshireManeuver firstManeuver = new ClohessyWiltshireManeuver(firstManeuverDetector, firstDeltaV, cwProvider);
        final ClohessyWiltshireManeuver secondManeuver = new ClohessyWiltshireManeuver(secondManeuverDetector, secondDeltaV, cwProvider);
        final ClohessyWiltshireManeuver thirdManeuver = new ClohessyWiltshireManeuver(thirdManeuverDetector, thirdDeltaV, cwProvider);
        final YamanakaAnkersenManeuver firstManeuverYA = new YamanakaAnkersenManeuver(firstManeuverDetector, firstDeltaV, yaProvider);
        final YamanakaAnkersenManeuver secondManeuverYA = new YamanakaAnkersenManeuver(secondManeuverDetector, secondDeltaV, yaProvider);
        final YamanakaAnkersenManeuver thirdManeuverYA = new YamanakaAnkersenManeuver(thirdManeuverDetector, thirdDeltaV, yaProvider);
        // Add the maneuvers to a list and convert the maneuvers to inertial frame.
        final List<RelativeManeuver> relativeManeuvers = new ArrayList<>();
        relativeManeuvers.add(firstManeuver);
        relativeManeuvers.add(secondManeuver);
        relativeManeuvers.add(thirdManeuver);
        final List<RelativeManeuver> relativeManeuversYA = new ArrayList<>();
        relativeManeuversYA.add(firstManeuverYA);
        relativeManeuversYA.add(secondManeuverYA);
        relativeManeuversYA.add(thirdManeuverYA);
        final List<ImpulseManeuver> impulseManeuvers = rpoCW.convertToImpulseManeuver(relativeManeuvers, targetOrbit, 0);
        final List<ImpulseManeuver> impulseManeuversYA = rpoYA.convertToImpulseManeuver(relativeManeuvers, targetOrbit, 0);
        // Transform the deltaV from LOF to InertialFrame.
        final List<Vector3D> deltaVInertial = new ArrayList<>();
        final List<Vector3D> deltaVInertialYA = new ArrayList<>();
        for (int i = 0; i < relativeManeuvers.size(); i++) {
            final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
            final SpacecraftState targetAtManeuver = propagator.propagate(maneuverDates.get(i));
            final Transform lofToInertial = rpoCW.getLOFType().transformFromInertial(maneuverDates.get(i), targetAtManeuver.getPVCoordinates()).getInverse();
            final Transform lofToInertialYA = rpoYA.getLOFType().transformFromInertial(maneuverDates.get(i), targetAtManeuver.getPVCoordinates()).getInverse();
            final Vector3D deltaV = lofToInertial.transformVector(relativeManeuvers.get(i).getDeltaV());
            final Vector3D deltaVYA = lofToInertialYA.transformVector(relativeManeuversYA.get(i).getDeltaV());
            deltaVInertial.add(deltaV);
            deltaVInertialYA.add(deltaVYA);
        }
        // Assert the dates of the impulse maneuvers match the dates of the relativeManeuvers.
        Assertions.assertEquals(firstManeuverDate.getDate().toDouble(), ((DateDetector) impulseManeuvers.get(0).getDetector()).getDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(secondManeuverDate.getDate().toDouble(), ((DateDetector) impulseManeuvers.get(1).getDetector()).getDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(thirdManeuverDate.getDate().toDouble(), ((DateDetector) impulseManeuvers.get(2).getDetector()).getDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(firstManeuverDate.getDate().toDouble(), ((DateDetector) impulseManeuversYA.get(0).getDetector()).getDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(secondManeuverDate.getDate().toDouble(), ((DateDetector) impulseManeuversYA.get(1).getDetector()).getDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(thirdManeuverDate.getDate().toDouble(), ((DateDetector) impulseManeuversYA.get(2).getDetector()).getDate().toDouble(), NUMERICAL_TOLERANCE);
        // Assert the transformation of the impulse vector by the convertToImpulseManeuver() method.
        TestUtils.validateVector3D(deltaVInertial.get(0), impulseManeuvers.get(0).getImpulseProvider().getImpulse(new SpacecraftState(targetOrbit), true), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertial.get(1), impulseManeuvers.get(1).getImpulseProvider().getImpulse(new SpacecraftState(targetOrbit), true), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertial.get(2), impulseManeuvers.get(2).getImpulseProvider().getImpulse(new SpacecraftState(targetOrbit), true), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertialYA.get(0), impulseManeuversYA.get(0).getImpulseProvider().getImpulse(new SpacecraftState(targetOrbit), true), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertialYA.get(1), impulseManeuversYA.get(1).getImpulseProvider().getImpulse(new SpacecraftState(targetOrbit), true), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertialYA.get(2), impulseManeuversYA.get(2).getImpulseProvider().getImpulse(new SpacecraftState(targetOrbit), true), NUMERICAL_TOLERANCE );
    }

    /**
     * Compare transformation of relative maneuvers to impulse maneuvers. Assess the transform of the deltaV in the
     * method convertToImpulseManeuver gives the same result as the frame transform of the deltaV vector itself.
     */
    @Test
    public void convertToImpulseManeuverFieldTest() {
        final Binary64Field field = Binary64Field.getInstance();
        // Define dates of the maneuvers.
        final List<FieldAbsoluteDate<Binary64>> maneuverDates = new ArrayList<>();
        final FieldAbsoluteDate<Binary64> firstManeuverDate = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> secondManeuverDate = firstManeuverDate.shiftedBy(1000);
        final FieldAbsoluteDate<Binary64> thirdManeuverDate = firstManeuverDate.shiftedBy(2000);
        maneuverDates.add(firstManeuverDate);
        maneuverDates.add(secondManeuverDate);
        maneuverDates.add(thirdManeuverDate);
        // Define date detectors of the maneuvers.
        final FieldDateDetector<Binary64> firstManeuverDetector = new FieldDateDetector<>(firstManeuverDate);
        final FieldDateDetector<Binary64> secondManeuverDetector = new FieldDateDetector<>(secondManeuverDate);
        final FieldDateDetector<Binary64> thirdManeuverDetector = new FieldDateDetector<>(thirdManeuverDate);
        // Define randomly the deltaV vectors.
        final FieldVector3D<Binary64> firstDeltaV = new FieldVector3D<>(field, new Vector3D(10, 13, 18));
        final FieldVector3D<Binary64> secondDeltaV = new FieldVector3D<>(field, new Vector3D(-5, 65, -8.6));
        final FieldVector3D<Binary64> thirdDeltaV = new FieldVector3D<>(field, new Vector3D(0, 15, 2));
        // Define target orbit and relative provider.
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(new Binary64(7000e3), field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), firstManeuverDate, new Binary64(Constants.EIGEN5C_EARTH_MU));
        final RPOModel rpoCW = RPOModel.CW;
        final RPOModel rpoYA = RPOModel.YA;
        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit);
        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit);
        // Define the maneuvers.
        final FieldClohessyWiltshireManeuver<Binary64> firstManeuver = new FieldClohessyWiltshireManeuver<>(firstManeuverDetector, firstDeltaV, cwProvider);
        final FieldClohessyWiltshireManeuver<Binary64> secondManeuver = new FieldClohessyWiltshireManeuver<>(secondManeuverDetector, secondDeltaV, cwProvider);
        final FieldClohessyWiltshireManeuver<Binary64> thirdManeuver = new FieldClohessyWiltshireManeuver<>(thirdManeuverDetector, thirdDeltaV, cwProvider);
        final FieldYamanakaAnkersenManeuver<Binary64> firstManeuverYA = new FieldYamanakaAnkersenManeuver<>(firstManeuverDetector, firstDeltaV, yaProvider);
        final FieldYamanakaAnkersenManeuver<Binary64> secondManeuverYA = new FieldYamanakaAnkersenManeuver<>(secondManeuverDetector, secondDeltaV, yaProvider);
        final FieldYamanakaAnkersenManeuver<Binary64> thirdManeuverYA = new FieldYamanakaAnkersenManeuver<>(thirdManeuverDetector, thirdDeltaV, yaProvider);
        // Add the maneuvers to a list and convert the maneuvers to inertial frame.
        final List<FieldRelativeManeuver<Binary64>> relativeManeuvers = new ArrayList<>();
        relativeManeuvers.add(firstManeuver);
        relativeManeuvers.add(secondManeuver);
        relativeManeuvers.add(thirdManeuver);
        final List<FieldRelativeManeuver<Binary64>> relativeManeuversYA = new ArrayList<>();
        relativeManeuversYA.add(firstManeuverYA);
        relativeManeuversYA.add(secondManeuverYA);
        relativeManeuversYA.add(thirdManeuverYA);
        final List<FieldImpulseManeuver<Binary64>> impulseManeuvers = rpoCW.convertToImpulseManeuver(relativeManeuvers, targetOrbit, field.getZero());
        final List<FieldImpulseManeuver<Binary64>> impulseManeuversYA = rpoYA.convertToImpulseManeuver(relativeManeuvers, targetOrbit, field.getZero());
        // Transform the deltaV from LOF to InertialFrame.
        final List<FieldVector3D<Binary64>> deltaVInertial = new ArrayList<>();
        final List<FieldVector3D<Binary64>> deltaVInertialYA = new ArrayList<>();
        for (int i = 0; i < relativeManeuvers.size(); i++) {
            final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
            final FieldSpacecraftState<Binary64> targetAtManeuver = propagator.propagate(maneuverDates.get(i));
            final FieldTransform<Binary64> lofToInertial = rpoCW.getLOFType().transformFromInertial(maneuverDates.get(i), targetAtManeuver.getPVCoordinates()).getInverse();
            final FieldTransform<Binary64> lofToInertialYA = rpoYA.getLOFType().transformFromInertial(maneuverDates.get(i), targetAtManeuver.getPVCoordinates()).getInverse();
            final FieldVector3D<Binary64> deltaV = lofToInertial.transformVector(relativeManeuvers.get(i).getDeltaV());
            final FieldVector3D<Binary64> deltaVYA = lofToInertialYA.transformVector(relativeManeuversYA.get(i).getDeltaV());
            deltaVInertial.add(deltaV);
            deltaVInertialYA.add(deltaVYA);
        }
        // Assert the dates of the impulse maneuvers match the dates of the relativeManeuvers.
        Assertions.assertEquals(firstManeuverDate.getDate().toAbsoluteDate().toDouble(), ((FieldDateDetector<Binary64>) impulseManeuvers.get(0).getDetector()).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(secondManeuverDate.getDate().toAbsoluteDate().toDouble(), ((FieldDateDetector<Binary64>) impulseManeuvers.get(1).getDetector()).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(thirdManeuverDate.getDate().toAbsoluteDate().toDouble(), ((FieldDateDetector<Binary64>) impulseManeuvers.get(2).getDetector()).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(firstManeuverDate.getDate().toAbsoluteDate().toDouble(), ((FieldDateDetector<Binary64>) impulseManeuversYA.get(0).getDetector()).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(secondManeuverDate.getDate().toAbsoluteDate().toDouble(), ((FieldDateDetector<Binary64>) impulseManeuversYA.get(1).getDetector()).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        Assertions.assertEquals(thirdManeuverDate.getDate().toAbsoluteDate().toDouble(), ((FieldDateDetector<Binary64>) impulseManeuversYA.get(2).getDetector()).getDate().toAbsoluteDate().toDouble(), NUMERICAL_TOLERANCE);
        // Assert the transformation of the impulse vector by the convertToImpulseManeuver() method.
        TestUtils.validateVector3D(deltaVInertial.get(0).toVector3D(), impulseManeuvers.get(0).getFieldImpulseProvider().getImpulse(new FieldSpacecraftState<>(targetOrbit), true).toVector3D(), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertial.get(1).toVector3D(), impulseManeuvers.get(1).getFieldImpulseProvider().getImpulse(new FieldSpacecraftState<>(targetOrbit), true).toVector3D(), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertial.get(2).toVector3D(), impulseManeuvers.get(2).getFieldImpulseProvider().getImpulse(new FieldSpacecraftState<>(targetOrbit), true).toVector3D(), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertialYA.get(0).toVector3D(), impulseManeuversYA.get(0).getFieldImpulseProvider().getImpulse(new FieldSpacecraftState<>(targetOrbit), true).toVector3D(), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertialYA.get(1).toVector3D(), impulseManeuversYA.get(1).getFieldImpulseProvider().getImpulse(new FieldSpacecraftState<>(targetOrbit), true).toVector3D(), NUMERICAL_TOLERANCE );
        TestUtils.validateVector3D(deltaVInertialYA.get(2).toVector3D(), impulseManeuversYA.get(2).getFieldImpulseProvider().getImpulse(new FieldSpacecraftState<>(targetOrbit), true).toVector3D(), NUMERICAL_TOLERANCE );
    }

    /**
     * Test the conversion from RelativeManeuvers to ImpulseManeuvers in inertial frame.
     * Propagate a linear trajectory of the chaser with the provider and compare with the propagation of chaser propagator
     * with the impulse maneuvers.
     * <br>
     * Note: Impulses are computed by the method computeForcesManeuvers. This method uses the velocity of the chaser
     * obtained by propagating the target orbit with the associated relative provider to compute the deltaV impulse at
     * each waypoint. The velocity of the chaser computed by the provider and by a proper propagator can slightly differ.
     * As a consequence the impulse maneuvers computed by transforming the relative ones don't fit exactly to what's
     * expected. Then the final position of the chaser propagated with ImpulseManeuvers slightly diverges from the one
     * propagated with the relative maneuvers, which explains a larger numerical tolerance to validate the test.
     */
    @Test
    public void convertToImpulseManeuverCWTest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = epoch.shiftedBy(100);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        final RPOModel rpo = RPOModel.CW;
        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(epoch, new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(finalDate, new Vector3D(200, 0, 0), Vector3D.ZERO);
        // Initial condition of the chaser expressed in the inertial frame.
        final Transform lofToInertial = rpo.getLOFType().transformFromInertial(pvtChaserInitial.getDate(), targetOrbit.getPVCoordinates()).getInverse();
        final PVCoordinates pvChaserInertial = lofToInertial.transformPVCoordinates(pvtChaserInitial);
        final TimeStampedPVCoordinates pvtChaserInertial = new TimeStampedPVCoordinates(epoch, pvChaserInertial);
        final KeplerianOrbit chaserOrbit = new KeplerianOrbit(pvtChaserInertial, targetOrbit.getFrame(), Constants.EIGEN5C_EARTH_MU);

        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit, pvtChaserInitial);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Propagate the target and the chaser provider to handle first maneuver.
        final SpacecraftState targetStart = propagator.propagate(epoch.shiftedBy(1));
        final double[] chaserStart = cwProvider.getAdditionalData(targetStart);
        final TimeStampedPVCoordinates chaserStartManeuver = new TimeStampedPVCoordinates(epoch.shiftedBy(1),
                new Vector3D(chaserStart[0], chaserStart[1], chaserStart[2]),
                new Vector3D(chaserStart[3], chaserStart[4], chaserStart[5]));
        // Reset target propagator and initial state of the provider
        propagator.resetInitialState(new SpacecraftState(targetOrbit));
        cwProvider.setInitialChaserPVTLof(pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = rpo.computeForcedLinearWaypoints(chaserStartManeuver, pvtChaserFinal, 6);

        // Definition of the chaser propagator.
        final KeplerianPropagator chaserPropagator = new KeplerianPropagator(chaserOrbit);
        // Compute the Relative maneuvers and add to it to the target propagator.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(waypoints,chaserStartManeuver.getVelocity(),targetOrbit,cwProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Convert the relative maneuvers to impulse maneuvers and add them to the chaser propagator.
        final List<ImpulseManeuver> impulseManeuvers = rpo.convertToImpulseManeuver(maneuvers, targetOrbit, 0);
        for (ImpulseManeuver impulseManeuver: impulseManeuvers) {
            chaserPropagator.addEventDetector(impulseManeuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final SpacecraftState finalState = propagator.propagate(finalDate);
        // Get the final chaser state from the propagation of the target and the relative provider.
        final double[] finalChaser = cwProvider.getAdditionalData(finalState);
        final Vector3D chaserPositionLof = new Vector3D(finalChaser[0], finalChaser[1], finalChaser[2]);
        final Transform inertialToLOFFinal = rpo.getLOFType().transformFromInertial(finalDate, finalState.getPVCoordinates());
        // Propagate the chaser orbit.
        final Vector3D chaserPositionInertial = chaserPropagator.propagate(finalDate).getPosition();
        final Vector3D chaserPositionInertialToLof = inertialToLOFFinal.transformPosition(chaserPositionInertial);
        // Assert the positions are the same.
        TestUtils.validateVector3D(chaserPositionLof, chaserPositionInertialToLof, 1e-3);
    }

    /**
     * Test the conversion from RelativeManeuvers to ImpulseManeuvers in inertial frame.
     * Propagate a linear trajectory of the chaser with the provider and compare with the propagation of chaser propagator
     * with the impulse maneuvers.
     * <br>
     * Note: Impulses are computed by the method computeForcesManeuvers. This method uses the velocity of the chaser
     * obtained by propagating the target orbit with the associated relative provider to compute the deltaV impulse at
     * each waypoint. The velocity of the chaser computed by the provider and by a proper propagator can slightly differ.
     * As a consequence the impulse maneuvers computed by transforming the relative ones don't fit exactly to what's
     * expected. Then the final position of the chaser propagated with ImpulseManeuvers slightly diverges from the one
     * propagated with the relative maneuvers, which explains a larger numerical tolerance to validate the test.
     */
    @Test
    public void convertToImpulseManeuverYATest() {
        final double n = 0.0011569; //Mean motion of target's orbit.
        final double rTarget = FastMath.pow(Constants.EIGEN5C_EARTH_MU/(n*n),1./3.);
        final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate finalDate = epoch.shiftedBy(100);

        // Target's orbit
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(rTarget, 0.0, 0.0,
                0.0, 0.0, 0.0,
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, Constants.EIGEN5C_EARTH_MU);

        final RPOModel rpo = RPOModel.YA;
        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedPVCoordinates pvtChaserInitial = new TimeStampedPVCoordinates(epoch, new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3));
        TimeStampedPVCoordinates pvtChaserFinal = new TimeStampedPVCoordinates(finalDate, new Vector3D(200, 0, 0), Vector3D.ZERO);
        // Initial condition of the chaser expressed in the inertial frame.
        final Transform lofToInertial = rpo.getLOFType().transformFromInertial(pvtChaserInitial.getDate(), targetOrbit.getPVCoordinates()).getInverse();
        final PVCoordinates pvChaserInertial = lofToInertial.transformPVCoordinates(pvtChaserInitial);
        final TimeStampedPVCoordinates pvtChaserInertial = new TimeStampedPVCoordinates(epoch, pvChaserInertial);
        final KeplerianOrbit chaserOrbit = new KeplerianOrbit(pvtChaserInertial, targetOrbit.getFrame(), Constants.EIGEN5C_EARTH_MU);

        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit, pvtChaserInitial);
        // Definition of the propagator.
        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Propagate the target and the chaser provider to handle first maneuver.
        final SpacecraftState targetStart = propagator.propagate(epoch.shiftedBy(1));
        final double[] chaserStart = yaProvider.getAdditionalData(targetStart);
        final TimeStampedPVCoordinates chaserStartManeuver = new TimeStampedPVCoordinates(epoch.shiftedBy(1),
                new Vector3D(chaserStart[0], chaserStart[1], chaserStart[2]),
                new Vector3D(chaserStart[3], chaserStart[4], chaserStart[5]));
        // Reset target propagator and initial state of the provider
        propagator.resetInitialState(new SpacecraftState(targetOrbit));
        yaProvider.setInitialChaserPVTLof(pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedPVCoordinates> waypoints = rpo.computeForcedLinearWaypoints(chaserStartManeuver, pvtChaserFinal, 6);

        // Definition of the chaser propagator.
        final KeplerianPropagator chaserPropagator = new KeplerianPropagator(chaserOrbit);
        // Compute the Relative maneuvers and add to it to the target propagator.
        final List<RelativeManeuver> maneuvers = rpo.computeForcedManeuvers(waypoints,chaserStartManeuver.getVelocity(),targetOrbit,yaProvider);
        for (RelativeManeuver maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Convert the relative maneuvers to impulse maneuvers and add them to the chaser propagator.
        final List<ImpulseManeuver> impulseManeuvers = rpo.convertToImpulseManeuver(maneuvers, targetOrbit, 0);
        for (ImpulseManeuver impulseManeuver: impulseManeuvers) {
            chaserPropagator.addEventDetector(impulseManeuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final SpacecraftState finalState = propagator.propagate(finalDate);
        // Get the final chaser state from the propagation of the target and the relative provider.
        final double[] finalChaser = yaProvider.getAdditionalData(finalState);
        final Vector3D chaserPositionLof = new Vector3D(finalChaser[0], finalChaser[1], finalChaser[2]);
        final Transform inertialToLOFFinal = rpo.getLOFType().transformFromInertial(finalDate, finalState.getPVCoordinates());
        // Propagate the chaser orbit.
        final Vector3D chaserPositionInertial = chaserPropagator.propagate(finalDate).getPosition();
        final Vector3D chaserPositionInertialToLof = inertialToLOFFinal.transformPosition(chaserPositionInertial);
        // Assert the positions are the same.
        TestUtils.validateVector3D(chaserPositionLof, chaserPositionInertialToLof, 1e-3);
    }

    /**
     * Test the conversion from RelativeManeuvers to ImpulseManeuvers in inertial frame.
     * Propagate a linear trajectory of the chaser with the provider and compare with the propagation of chaser propagator
     * with the impulse maneuvers.
     * <br>
     * Note: Impulses are computed by the method computeForcesManeuvers. This method uses the velocity of the chaser
     * obtained by propagating the target orbit with the associated relative provider to compute the deltaV impulse at
     * each waypoint. The velocity of the chaser computed by the provider and by a proper propagator can slightly differ.
     * As a consequence the impulse maneuvers computed by transforming the relative ones don't fit exactly to what's
     * expected. Then the final position of the chaser propagated with ImpulseManeuvers slightly diverges from the one
     * propagated with the relative maneuvers, which explains a larger numerical tolerance to validate the test.
     */
    @Test
    public void convertToImpulseManeuverCWFieldTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(100);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        final RPOModel rpo = RPOModel.CW;
        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));
        // Initial condition of the chaser expressed in the inertial frame.
        final FieldTransform<Binary64> lofToInertial = rpo.getLOFType().transformFromInertial(pvtChaserInitial.getDate(), targetOrbit.getPVCoordinates()).getInverse();
        final FieldPVCoordinates<Binary64> pvChaserInertial = lofToInertial.transformPVCoordinates(pvtChaserInitial);
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInertial = new TimeStampedFieldPVCoordinates<>(epoch, pvChaserInertial);
        final FieldKeplerianOrbit<Binary64> chaserOrbit = new FieldKeplerianOrbit<>(pvtChaserInertial, targetOrbit.getFrame(), mu);

        final FieldClohessyWiltshireProvider<Binary64> cwProvider = new FieldClohessyWiltshireProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(cwProvider);
        // Propagate the target and the chaser provider to handle first maneuver.
        final FieldSpacecraftState<Binary64> targetStart = propagator.propagate(epoch.shiftedBy(1));
        final Binary64[] chaserStart = cwProvider.getAdditionalData(targetStart);
        final TimeStampedFieldPVCoordinates<Binary64> chaserStartManeuver = new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1),
                new FieldPVCoordinates<>(new FieldVector3D<>(chaserStart[0], chaserStart[1], chaserStart[2]),
                new FieldVector3D<>(chaserStart[3], chaserStart[4], chaserStart[5])));
        // Reset target propagator and initial state of the provider
        propagator.resetInitialState(new FieldSpacecraftState<>(targetOrbit));
        cwProvider.setInitialChaserPVTLof(pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = rpo.computeForcedLinearWaypoints(chaserStartManeuver, pvtChaserFinal, 6);

        // Definition of the chaser propagator.
        final FieldKeplerianPropagator<Binary64> chaserPropagator = new FieldKeplerianPropagator<>(chaserOrbit);
        // Compute the Relative maneuvers and add to it to the target propagator.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(waypoints,chaserStartManeuver.getVelocity(),targetOrbit,cwProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Convert the relative maneuvers to impulse maneuvers and add them to the chaser propagator.
        final List<FieldImpulseManeuver<Binary64>> impulseManeuvers = rpo.convertToImpulseManeuver(maneuvers, targetOrbit, field.getZero());
        for (FieldImpulseManeuver<Binary64> impulseManeuver: impulseManeuvers) {
            chaserPropagator.addEventDetector(impulseManeuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(finalDate);
        // Get the final chaser state from the propagation of the target and the relative provider.
        final Binary64[] finalChaser = cwProvider.getAdditionalData(finalState);
        final FieldVector3D<Binary64> chaserPositionLof = new FieldVector3D<>(finalChaser[0], finalChaser[1], finalChaser[2]);
        final FieldTransform<Binary64> inertialToLOFFinal = rpo.getLOFType().transformFromInertial(finalDate, finalState.getPVCoordinates());
        // Propagate the chaser orbit.
        final FieldVector3D<Binary64> chaserPositionInertial = chaserPropagator.propagate(finalDate).getPosition();
        final FieldVector3D<Binary64> chaserPositionInertialToLof = inertialToLOFFinal.transformPosition(chaserPositionInertial);
        // Assert the positions are the same.
        TestUtils.validateVector3D(chaserPositionLof.toVector3D(), chaserPositionInertialToLof.toVector3D(), 1e-3);
    }

    /**
     * Test the conversion from RelativeManeuvers to ImpulseManeuvers in inertial frame.
     * Propagate a linear trajectory of the chaser with the provider and compare with the propagation of chaser propagator
     * with the impulse maneuvers.
     * <br>
     * Note: Impulses are computed by the method computeForcesManeuvers. This method uses the velocity of the chaser
     * obtained by propagating the target orbit with the associated relative provider to compute the deltaV impulse at
     * each waypoint. The velocity of the chaser computed by the provider and by a proper propagator can slightly differ.
     * As a consequence the impulse maneuvers computed by transforming the relative ones don't fit exactly to what's
     * expected. Then the final position of the chaser propagated with ImpulseManeuvers slightly diverges from the one
     * propagated with the relative maneuvers, which explains a larger numerical tolerance to validate the test.
     */
    @Test
    public void convertToImpulseManeuverYAFieldTest() {
        final Binary64Field field = Binary64Field.getInstance();
        final Binary64 n = new Binary64(0.0011569); //Mean motion of target's orbit.
        final Binary64 mu = new Binary64(Constants.EIGEN5C_EARTH_MU);
        final Binary64 rTarget = mu.divide(n.multiply(n)).pow(1./3.);
        final FieldAbsoluteDate<Binary64> epoch = new FieldAbsoluteDate<>(field,AbsoluteDate.J2000_EPOCH);
        final FieldAbsoluteDate<Binary64> finalDate = epoch.shiftedBy(100);

        // Target's orbit
        final FieldKeplerianOrbit<Binary64> targetOrbit = new FieldKeplerianOrbit<>(rTarget, field.getZero(), field.getZero(),
                field.getZero(), field.getZero(), field.getZero(),
                PositionAngleType.MEAN, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), epoch, mu);

        final RPOModel rpo = RPOModel.YA;
        // Start and end conditions of the transfer, expressed in the target's LOF
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserInitial = new TimeStampedFieldPVCoordinates<>(epoch, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(0, -1.0e3, 0), new Vector3D(-0.02e3, 0.02e3, -0.005e3))));
        TimeStampedFieldPVCoordinates<Binary64> pvtChaserFinal = new TimeStampedFieldPVCoordinates<>(finalDate, new FieldPVCoordinates<>(field, new PVCoordinates(new Vector3D(200, 0, 0), Vector3D.ZERO)));
        // Initial condition of the chaser expressed in the inertial frame.
        final FieldTransform<Binary64> lofToInertial = rpo.getLOFType().transformFromInertial(pvtChaserInitial.getDate(), targetOrbit.getPVCoordinates()).getInverse();
        final FieldPVCoordinates<Binary64> pvChaserInertial = lofToInertial.transformPVCoordinates(pvtChaserInitial);
        final TimeStampedFieldPVCoordinates<Binary64> pvtChaserInertial = new TimeStampedFieldPVCoordinates<>(epoch, pvChaserInertial);
        final FieldKeplerianOrbit<Binary64> chaserOrbit = new FieldKeplerianOrbit<>(pvtChaserInertial, targetOrbit.getFrame(), mu);

        final FieldYamanakaAnkersenProvider<Binary64> yaProvider = new FieldYamanakaAnkersenProvider<>(targetOrbit, pvtChaserInitial);
        // Definition of the propagator.
        final FieldKeplerianPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(targetOrbit);
        propagator.addAdditionalDataProvider(yaProvider);
        // Propagate the target and the chaser provider to handle first maneuver.
        final FieldSpacecraftState<Binary64> targetStart = propagator.propagate(epoch.shiftedBy(1));
        final Binary64[] chaserStart = yaProvider.getAdditionalData(targetStart);
        final TimeStampedFieldPVCoordinates<Binary64> chaserStartManeuver = new TimeStampedFieldPVCoordinates<>(epoch.shiftedBy(1),
                new FieldPVCoordinates<>(new FieldVector3D<>(chaserStart[0], chaserStart[1], chaserStart[2]),
                        new FieldVector3D<>(chaserStart[3], chaserStart[4], chaserStart[5])));
        // Reset target propagator and initial state of the provider
        propagator.resetInitialState(new FieldSpacecraftState<>(targetOrbit));
        yaProvider.setInitialChaserPVTLof(pvtChaserInitial);
        // Definition of the linear path.
        final List<TimeStampedFieldPVCoordinates<Binary64>> waypoints = rpo.computeForcedLinearWaypoints(chaserStartManeuver, pvtChaserFinal, 6);

        // Definition of the chaser propagator.
        final FieldKeplerianPropagator<Binary64> chaserPropagator = new FieldKeplerianPropagator<>(chaserOrbit);
        // Compute the Relative maneuvers and add to it to the target propagator.
        final List<FieldRelativeManeuver<Binary64>> maneuvers = rpo.computeForcedManeuvers(waypoints,chaserStartManeuver.getVelocity(),targetOrbit,yaProvider);
        for (FieldRelativeManeuver<Binary64> maneuver: maneuvers) {
            propagator.addEventDetector(maneuver);
        }
        // Convert the relative maneuvers to impulse maneuvers and add them to the chaser propagator.
        final List<FieldImpulseManeuver<Binary64>> impulseManeuvers = rpo.convertToImpulseManeuver(maneuvers, targetOrbit, field.getZero());
        for (FieldImpulseManeuver<Binary64> impulseManeuver: impulseManeuvers) {
            chaserPropagator.addEventDetector(impulseManeuver);
        }
        // Propagate the target state to the end of the linear scenario.
        final FieldSpacecraftState<Binary64> finalState = propagator.propagate(finalDate);
        // Get the final chaser state from the propagation of the target and the relative provider.
        final Binary64[] finalChaser = yaProvider.getAdditionalData(finalState);
        final FieldVector3D<Binary64> chaserPositionLof = new FieldVector3D<>(finalChaser[0], finalChaser[1], finalChaser[2]);
        final FieldTransform<Binary64> inertialToLOFFinal = rpo.getLOFType().transformFromInertial(finalDate, finalState.getPVCoordinates());
        // Propagate the chaser orbit.
        final FieldVector3D<Binary64> chaserPositionInertial = chaserPropagator.propagate(finalDate).getPosition();
        final FieldVector3D<Binary64> chaserPositionInertialToLof = inertialToLOFFinal.transformPosition(chaserPositionInertial);
        // Assert the positions are the same.
        TestUtils.validateVector3D(chaserPositionLof.toVector3D(), chaserPositionInertialToLof.toVector3D(), 1e-3);
    }
}
