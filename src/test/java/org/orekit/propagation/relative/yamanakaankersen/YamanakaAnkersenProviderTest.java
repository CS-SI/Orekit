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
package org.orekit.propagation.relative.yamanakaankersen;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
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
import org.orekit.propagation.relative.clohessywiltshire.ClohessyWiltshireProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


// Comparison of chaser state propagation using Yamanaka-Ankersen provider and a classic Keplerian Propagator.
// As Yamanaka-Ankersen model diverges promptly with time of simulation and for low altitude orbit, test is done for a GEO target, in close proximity for a short propagation duration.

public class YamanakaAnkersenProviderTest {

    public static final double NUMERICAL_TOLERANCE_1 = 1;
    public static final double NUMERICAL_TOLERANCE_2 = 1e-6;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    @Test
    public void yaProviderTest(){
        final Frame eme2000 = FramesFactory.getEME2000();

        final double sma = 35800e3 + Constants.EGM96_EARTH_EQUATORIAL_RADIUS;

        final AbsoluteDate date = new AbsoluteDate(2025,3,18,10,33,15., TimeScalesFactory.getUTC());

        final KeplerianOrbit targetOrbit = new KeplerianOrbit(sma,0.3,0,0,0, 0, PositionAngleType.TRUE,eme2000,date,Constants.EIGEN5C_EARTH_MU);

        // Initialize chaser position 1km away from the target
        final Vector3D initialTargetPosition = targetOrbit.getPosition();
        Vector3D deltaPos = (new Vector3D(0.33, 0.33, 0.33)).scalarMultiply(100.);
        PVCoordinates initialChaserPV = new PVCoordinates(initialTargetPosition.add(deltaPos), targetOrbit.getPVCoordinates().getVelocity());

        final KeplerianOrbit chaserOrbit = new KeplerianOrbit(initialChaserPV,eme2000, date, Constants.EIGEN5C_EARTH_MU);

        final TimeStampedPVCoordinates chaserTimePV = new TimeStampedPVCoordinates(date,initialChaserPV);

        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit,chaserTimePV,eme2000,"Yamanaka-Ankersen equation");

        final KeplerianPropagator targetPropagator = new KeplerianPropagator(targetOrbit);
        final KeplerianPropagator chaserPropagator = new KeplerianPropagator(chaserOrbit);

        targetPropagator.addAdditionalDataProvider(yaProvider);

        final SpacecraftState finalTargetState = targetPropagator.propagate(date.shiftedBy(100.));
        final SpacecraftState finalChaserState = chaserPropagator.propagate(date.shiftedBy(100.));

        final double[] chaserFinalYA = yaProvider.getAdditionalData(finalTargetState);
        final double[] localPosYA = {chaserFinalYA[0], chaserFinalYA[1], chaserFinalYA[2]};
        final double[] localVelYA = {chaserFinalYA[3], chaserFinalYA[4], chaserFinalYA[5]};
        // Transform chaser coordinates propagated in LOF to inertial frame.
        final LocalOrbitalFrame targetLofYA = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, targetOrbit, "LVLH LOF target");
        final PVCoordinates inertialPVYA = targetLofYA.getTransformTo(eme2000,finalTargetState.getDate()).transformPVCoordinates(new PVCoordinates(new Vector3D(localPosYA), new Vector3D(localVelYA)));

        final TimeStampedPVCoordinates chaserInertialWithExtract = yaProvider.extractChaserPVT(finalTargetState,eme2000);

        Assertions.assertEquals(finalChaserState.getPosition().getX(),inertialPVYA.getPosition().getX(),NUMERICAL_TOLERANCE_1);
        Assertions.assertEquals(finalChaserState.getPosition().getY(),inertialPVYA.getPosition().getY(),NUMERICAL_TOLERANCE_1);
        Assertions.assertEquals(finalChaserState.getPosition().getZ(),inertialPVYA.getPosition().getZ(),NUMERICAL_TOLERANCE_1);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getX(),inertialPVYA.getVelocity().getX(),NUMERICAL_TOLERANCE_1);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getY(),inertialPVYA.getVelocity().getY(),NUMERICAL_TOLERANCE_1);
        Assertions.assertEquals(finalChaserState.getPVCoordinates().getVelocity().getZ(),inertialPVYA.getVelocity().getZ(),NUMERICAL_TOLERANCE_1);

        Assertions.assertEquals(inertialPVYA.getPosition().getX(),chaserInertialWithExtract.getPosition().getX(),NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVYA.getPosition().getY(),chaserInertialWithExtract.getPosition().getY(),NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVYA.getPosition().getZ(),chaserInertialWithExtract.getPosition().getZ(),NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVYA.getVelocity().getX(),chaserInertialWithExtract.getVelocity().getX(),NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVYA.getVelocity().getY(),chaserInertialWithExtract.getVelocity().getY(),NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVYA.getVelocity().getZ(),chaserInertialWithExtract.getVelocity().getZ(),NUMERICAL_TOLERANCE_2);

    }

    // Comparison of chaser position using Clohessy-Wiltshire model and Yamanaka-Ankersen on a circular orbit.
    // Clohessy-Wiltshire is a particular case of Yamanaka-Ankersen model with e=0.

    @Test
    public void CWYAComparison(){
        final Frame eme2000 = FramesFactory.getEME2000();

        final AbsoluteDate date = new AbsoluteDate(2025,3,18,10,33,15., TimeScalesFactory.getUTC());

        //final KeplerianOrbit targetOrbit = new KeplerianOrbit(new PVCoordinates(new Vector3D(targetInitialPosition),new Vector3D(targetInitialVelocity)), eme2000, date, Constants.EIGEN5C_EARTH_MU);

        final KeplerianOrbit targetOrbit = new KeplerianOrbit(6671000,0,40* FastMath.PI/180,0,20*FastMath.PI/180, 60*FastMath.PI/180, PositionAngleType.TRUE,eme2000,date,Constants.EIGEN5C_EARTH_MU);
        final double[] chaserInitialPosition = {1612.75e3,5310.19e3,3750.33e3};
        final double[] chaserInitialVelocity = {-7.35321e3,0.463856e3,2.46920e3};

        final TimeStampedPVCoordinates chaserTimePV = new TimeStampedPVCoordinates(date,new Vector3D(chaserInitialPosition),new Vector3D(chaserInitialVelocity));

        final ClohessyWiltshireProvider cwProvider = new ClohessyWiltshireProvider(targetOrbit,chaserTimePV,eme2000,"Clohessy-Wiltshire");

        final YamanakaAnkersenProvider yaProvider = new YamanakaAnkersenProvider(targetOrbit,chaserTimePV,eme2000,"Yamanaka-Ankersen equation");

        final KeplerianPropagator propagator = new KeplerianPropagator(targetOrbit);

        propagator.addAdditionalDataProvider(cwProvider);
        propagator.addAdditionalDataProvider(yaProvider);

        final SpacecraftState propagated = propagator.propagate(date.shiftedBy(5572.8));

        final double[] chaserFinalCW = cwProvider.getAdditionalData(propagated);
        final double[] chaserFinalYA = yaProvider.getAdditionalData(propagated);

        final double[] localPosCW = {chaserFinalCW[0], chaserFinalCW[1], chaserFinalCW[2]};
        final double[] localVelCW = {chaserFinalCW[3], chaserFinalCW[4], chaserFinalCW[5]};
        final double[] localPosYA = {chaserFinalYA[0], chaserFinalYA[1], chaserFinalYA[2]};
        final double[] localVelYA = {chaserFinalYA[3], chaserFinalYA[4], chaserFinalYA[5]};

        final LocalOrbitalFrame targetLofCW = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.QSW, new KeplerianPropagator(targetOrbit), "QSW LOF target");
        final LocalOrbitalFrame targetLofYA = new LocalOrbitalFrame(targetOrbit.getFrame(), LOFType.LVLH_CCSDS, new KeplerianPropagator(targetOrbit), "LVLH CCSDS LOF target");

        final PVCoordinates inertialPVcw = targetLofCW.getTransformTo(eme2000,date.shiftedBy(5572.8)).transformPVCoordinates(new PVCoordinates(new Vector3D(localPosCW), new Vector3D(localVelCW)));
        final PVCoordinates inertialPVya = targetLofYA.getTransformTo(eme2000,date.shiftedBy(5572.8)).transformPVCoordinates(new PVCoordinates(new Vector3D(localPosYA), new Vector3D(localVelYA)));

        Assertions.assertEquals(inertialPVcw.getPosition().getX(), inertialPVya.getPosition().getX(), NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVcw.getPosition().getY(), inertialPVya.getPosition().getY(), NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVcw.getPosition().getZ(), inertialPVya.getPosition().getZ(), NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVcw.getVelocity().getX(), inertialPVya.getVelocity().getX(), NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVcw.getVelocity().getY(), inertialPVya.getVelocity().getY(), NUMERICAL_TOLERANCE_2);
        Assertions.assertEquals(inertialPVcw.getVelocity().getZ(), inertialPVya.getVelocity().getZ(), NUMERICAL_TOLERANCE_2);
    }

    @Test

    public void testGetInitialChaserPVTLof(){
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(10000,0,0,0,0,0,PositionAngleType.TRUE,frame,date,Constants.EIGEN5C_EARTH_MU);
        final YamanakaAnkersenProvider provider = new YamanakaAnkersenProvider(targetOrbit);
        final TimeStampedPVCoordinates initialChaserPVTLof = provider.getInitialChaserPVTLof();
        TestUtils.validateVector3D(Vector3D.ZERO,initialChaserPVTLof.getPosition(),NUMERICAL_TOLERANCE_2);
        TestUtils.validateVector3D(Vector3D.ZERO,initialChaserPVTLof.getVelocity(),NUMERICAL_TOLERANCE_2);

    }

    @Test
    public void testSetInitialChaserPVTLof(){
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final KeplerianOrbit targetOrbit = new KeplerianOrbit(10000,0,0,0,0,0,PositionAngleType.TRUE,frame,date,Constants.EIGEN5C_EARTH_MU);
        final TimeStampedPVCoordinates pvt1 = new TimeStampedPVCoordinates(date,Vector3D.ZERO,Vector3D.ZERO);
        final YamanakaAnkersenProvider provider = new YamanakaAnkersenProvider(targetOrbit, pvt1, frame);
        final TimeStampedPVCoordinates pvt2 = new TimeStampedPVCoordinates(date,new Vector3D(1,1,1),new Vector3D(1,1,1));
        provider.setInitialChaserPVTLof(pvt2);
        TestUtils.validateVector3D(pvt2.getPosition(),provider.getInitialChaserPVTLof().getPosition(),NUMERICAL_TOLERANCE_2);
        TestUtils.validateVector3D(pvt2.getVelocity(),provider.getInitialChaserPVTLof().getVelocity(),NUMERICAL_TOLERANCE_2);
    }


}
