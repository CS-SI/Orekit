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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.LocalOrbitalFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


// Comparison of chaser state propagation using Clohessy-Wiltshire provider and a classic Keplerian Propagator.
// As Clohessy-Wiltshire model diverges promptly with time of simulation and for low altitude orbit, test is done for a GEO target, in close proximity for a short propagation duration.

public class ClohessyWiltshireProviderTest {

    public static final double NUMERICAL_TOLERANCE = 1e-6;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void cwProviderTest(){
        final Frame eme2000 = FramesFactory.getEME2000();

        final double orbitRadius = 35800e3 + Constants.EGM96_EARTH_EQUATORIAL_RADIUS;

        final AbsoluteDate date = new AbsoluteDate(2025,3,18,10,33,15., TimeScalesFactory.getUTC());
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(orbitRadius,0,0,0,0,0,PositionAngleType.TRUE,eme2000,date,Constants.EIGEN5C_EARTH_MU);

        // Initialize chaser position 1km away from the target
        final Vector3D initialTargetPosition = targetOrbit.getPosition();
        Vector3D deltaPos = (new Vector3D(0.33, 0.33, 0.33)).scalarMultiply(1000.);
        PVCoordinates initialChaserPV = new PVCoordinates(initialTargetPosition.add(deltaPos), targetOrbit.getPVCoordinates().getVelocity());

        final KeplerianOrbit chaserOrbit = new KeplerianOrbit(initialChaserPV,eme2000, date, Constants.EIGEN5C_EARTH_MU);

        final TimeStampedPVCoordinates chaserTimePV = new TimeStampedPVCoordinates(date,initialChaserPV);

        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit,chaserTimePV,eme2000,"Clohessy-Wiltshire");

        final KeplerianPropagator targetPropagator = new KeplerianPropagator(targetOrbit);
        final KeplerianPropagator chaserPropagator = new KeplerianPropagator(chaserOrbit);

        targetPropagator.addAdditionalDataProvider(cwProvider);

        final SpacecraftState finalTargetState = targetPropagator.propagate(date.shiftedBy(100.0));
        final SpacecraftState finalChaserState = chaserPropagator.propagate(date.shiftedBy(100.0));


        final double[] chaserFinalCW = cwProvider.getAdditionalData(finalTargetState);
        final double[] localPosCW = {chaserFinalCW[0], chaserFinalCW[1], chaserFinalCW[2]};
        final double[] localVelCW = {chaserFinalCW[3], chaserFinalCW[4], chaserFinalCW[5]};
        // Transform chaser coordinates propagated in LOF to inertial frame.
        final LocalOrbitalFrame targetLofCW = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, targetOrbit, "QSW LOF target");
        final PVCoordinates inertialPVCW = targetLofCW.getTransformTo(eme2000,date.shiftedBy(100.0)).transformPVCoordinates(new PVCoordinates(new Vector3D(localPosCW), new Vector3D(localVelCW)));

        final TimeStampedPVCoordinates chaserInertialWithExtract = cwProvider.extractChaserPVT(finalTargetState,eme2000);

        Assertions.assertEquals(finalChaserState.getPosition().getX(),inertialPVCW.getPosition().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPosition().getY(),inertialPVCW.getPosition().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPosition().getZ(),inertialPVCW.getPosition().getZ(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getX(),inertialPVCW.getVelocity().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getY(),inertialPVCW.getVelocity().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getZ(),inertialPVCW.getVelocity().getZ(),NUMERICAL_TOLERANCE);


        Assertions.assertEquals(finalChaserState.getPosition().getX(),chaserInertialWithExtract.getPosition().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPosition().getY(),chaserInertialWithExtract.getPosition().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPosition().getZ(),chaserInertialWithExtract.getPosition().getZ(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getX(),chaserInertialWithExtract.getVelocity().getX(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getY(),chaserInertialWithExtract.getVelocity().getY(),NUMERICAL_TOLERANCE);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getZ(),chaserInertialWithExtract.getVelocity().getZ(),NUMERICAL_TOLERANCE);
    }

    @Test

    public void testGetInitialChaserPVTLof(){
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(10000,0,0,0,0,0,PositionAngleType.TRUE,frame,date,Constants.EIGEN5C_EARTH_MU);
        final ClohessyWiltshireProvider provider = new ClohessyWiltshireProvider(targetOrbit);
        final ClohessyWiltshireProvider provider2 = new ClohessyWiltshireProvider(targetOrbit, new TimeStampedPVCoordinates(date,new Vector3D(1,2,3),new Vector3D(4,5,6)));
        final TimeStampedPVCoordinates initialChaserPVTLof = provider.getInitialChaserPVTLof();
        TestUtils.validateVector3D(Vector3D.ZERO,initialChaserPVTLof.getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(Vector3D.ZERO,initialChaserPVTLof.getVelocity(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(1,2,3), provider2.getInitialChaserPVTLof().getPosition(), NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(new Vector3D(4,5,6), provider2.getInitialChaserPVTLof().getVelocity(), NUMERICAL_TOLERANCE);

    }

    @Test
    public void testSetInitialChaserPVTLof(){
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(10000,0,0,0,0,0,PositionAngleType.TRUE,frame,date,Constants.EIGEN5C_EARTH_MU);
        final TimeStampedPVCoordinates pvt1 = new TimeStampedPVCoordinates(date,Vector3D.ZERO,Vector3D.ZERO);
        final ClohessyWiltshireProvider provider = new ClohessyWiltshireProvider(targetOrbit, pvt1, frame);
        final ClohessyWiltshireProvider provider2 = new ClohessyWiltshireProvider(targetOrbit,pvt1,"Clohessy-Wiltshire");
        final TimeStampedPVCoordinates pvt2 = new TimeStampedPVCoordinates(date,new Vector3D(1,1,1),new Vector3D(1,1,1));
        provider.setInitialChaserPVTLof(pvt2);
        provider2.setInitialChaserPVTLof(pvt2);
        TestUtils.validateVector3D(pvt2.getPosition(),provider.getInitialChaserPVTLof().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2.getVelocity(),provider.getInitialChaserPVTLof().getVelocity(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2.getPosition(),provider2.getInitialChaserPVTLof().getPosition(),NUMERICAL_TOLERANCE);
        TestUtils.validateVector3D(pvt2.getVelocity(),provider2.getInitialChaserPVTLof().getVelocity(),NUMERICAL_TOLERANCE);
    }
}
