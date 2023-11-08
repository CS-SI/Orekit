/* Copyright 2002-2023 CS GROUP
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
package org.orekit.bodies;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.LagrangianPoints;
import org.orekit.utils.PVCoordinates;

public class CR3BPSystemTest {

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("cr3bp:regular-data");
    }

    @Test
    public void testCR3BPSystem() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        TimeScale timeScale = TimeScalesFactory.getUTC();
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(date, timeScale);
        final double lDim = syst.getDdim();
        Assertions.assertNotNull(lDim);

        final double vDim = syst.getVdim();
        Assertions.assertNotNull(vDim);

        final double tDim = syst.getTdim();
        Assertions.assertNotNull(tDim);
    }

    @Test
    public void testGetRotatingFrame() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        TimeScale timeScale = TimeScalesFactory.getUTC();
        final Frame baryFrame = CR3BPFactory.getSunEarthCR3BP(date, timeScale).getRotatingFrame();
        Assertions.assertNotNull(baryFrame);
    }

    @Test
    public void testGetPrimary() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        TimeScale timeScale = TimeScalesFactory.getUTC();
        final CelestialBody primaryBody = CR3BPFactory.getSunEarthCR3BP(date, timeScale).getPrimary();
        Assertions.assertNotNull(primaryBody);
    }

    @Test
    public void testGetSecondary() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        TimeScale timeScale = TimeScalesFactory.getUTC();
        final CelestialBody secondaryBody = CR3BPFactory.getSunEarthCR3BP(date, timeScale).getSecondary();
        Assertions.assertNotNull(secondaryBody);
    }

    @Test
    public void testGetMu() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        TimeScale timeScale = TimeScalesFactory.getUTC();
        final double mu = CR3BPFactory.getSunJupiterCR3BP(date, timeScale).getMassRatio();
        Assertions.assertNotNull(mu);
    }

    @Test
    public void testGetName() {
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        TimeScale timeScale = TimeScalesFactory.getUTC();
        final String name = CR3BPFactory.getSunEarthCR3BP(date, timeScale).getName();
        Assertions.assertNotNull(name);
    }

    @Test
    public void testGetLPos() {
        final CR3BPSystem syst = CR3BPFactory.getEarthMoonCR3BP();

        final Vector3D l1Position = syst.getLPosition(LagrangianPoints.L1);
        Assertions.assertEquals(3.23E8, l1Position.getX() * syst.getDdim(),3E6);
        Assertions.assertEquals(0.0, l1Position.getY() * syst.getDdim(),1E3);
        Assertions.assertEquals(0.0, l1Position.getZ() * syst.getDdim(),1E3);

        final Vector3D l2Position = syst.getLPosition(LagrangianPoints.L2);
        Assertions.assertEquals(4.45E8, l2Position.getX() * syst.getDdim(),3E6);
        Assertions.assertEquals(0.0, l2Position.getY() * syst.getDdim(),1E3);
        Assertions.assertEquals(0.0, l2Position.getZ() * syst.getDdim(),1E3);

        final Vector3D l3Position = syst.getLPosition(LagrangianPoints.L3);
        Assertions.assertEquals(-3.86E8, l3Position.getX() * syst.getDdim(),3E6);
        Assertions.assertEquals(0.0, l3Position.getY() * syst.getDdim(),1E3);
        Assertions.assertEquals(0.0, l3Position.getZ() * syst.getDdim(),1E3);

        final Vector3D l4Position = syst.getLPosition(LagrangianPoints.L4);
        Assertions.assertEquals(1.87E8, l4Position.getX() * syst.getDdim(),3E6);
        Assertions.assertEquals(3.32E8, l4Position.getY() * syst.getDdim(),3E6);
        Assertions.assertEquals(0.0, l4Position.getZ() * syst.getDdim(),1E3);

        final Vector3D l5Position = syst.getLPosition(LagrangianPoints.L5);
        Assertions.assertEquals(1.87E8, l5Position.getX() * syst.getDdim(),3E6);
        Assertions.assertEquals(-3.32E8, l5Position.getY() * syst.getDdim(),3E6);
        Assertions.assertEquals(0.0, l5Position.getZ() * syst.getDdim(),1E3);
    }

    @Test
    public void testGetGamma() {
        TimeScale timeScale = TimeScalesFactory.getUTC();
        AbsoluteDate date = new AbsoluteDate(2020, 7, 5, 12, 0, 0.0, timeScale);
        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(date, timeScale);

        final double l1Gamma = syst.getGamma(LagrangianPoints.L1);
        Assertions.assertEquals(1.497655E9, l1Gamma * syst.getDdim(),1E3);


        final double l2Gamma = syst.getGamma(LagrangianPoints.L2);
        Assertions.assertEquals(1.507691E9, l2Gamma * syst.getDdim(),1E3);


        final double l3Gamma = syst.getGamma(LagrangianPoints.L3);
        Assertions.assertEquals(1.495981555E11, l3Gamma * syst.getDdim(),9E3);
    }

    @Test
    public void testGetRealAPV() {

        // Time settings
        final TimeScale timeScale = TimeScalesFactory.getUTC();
        final AbsoluteDate initialDate =
            new AbsoluteDate(1996, 06, 26, 0, 0, 00.000,
                             TimeScalesFactory.getUTC());

        final CR3BPSystem syst = CR3BPFactory.getSunEarthCR3BP(initialDate, timeScale);

        final CelestialBody primaryBody = syst.getPrimary();
        final CelestialBody secondaryBody = syst.getSecondary();

        final PVCoordinates pv0 = new PVCoordinates(new Vector3D(0,0,1), new Vector3D(0,0,0));

        final Frame outputFrame = secondaryBody.getInertiallyOrientedFrame();

        // 1.   Translate the rotating state from the RTBP to a primary-centered rotating state
        // 2.   Dimensionalize  the  primary-centered  rotating  state  using  the  instantaneously
        //      defined characteristic quantities
        // 3.   Apply the transformation matrix
        // 4.   Apply the transformation to output frame

        final Frame primaryInertialFrame = primaryBody.getInertiallyOrientedFrame();
        final PVCoordinates pv21 = secondaryBody.getPVCoordinates(initialDate, primaryInertialFrame);

        // Distance and Velocity to dimensionalize the state vector
        final double dist12 = pv21.getPosition().getNorm();
        final double vCircular  = FastMath.sqrt(secondaryBody.getGM() / dist12);

        // Dimensionalized state vector centered on primary body
        final PVCoordinates pvDim = new PVCoordinates((pv0.getPosition().add(new Vector3D(syst.getMassRatio(), 0, 0))).scalarMultiply(dist12),
                                                      pv0.getVelocity().scalarMultiply(vCircular));

        // Instantaneous rotation matrix between rotating frame and primary inertial frame
        final double[][] c = (new Rotation(Vector3D.PLUS_I, Vector3D.PLUS_K,
                                           pv21.getPosition(), pv21.getMomentum())).getMatrix();

        // Instantaneous angular velocity of the rotating frame
        final double theta = pv21.getMomentum().getNorm() / (dist12 * dist12);

        final double x = pvDim.getPosition().getX();
        final double y = pvDim.getPosition().getY();
        final double z = pvDim.getPosition().getZ();
        final double vx = pvDim.getVelocity().getX();
        final double vy = pvDim.getVelocity().getY();
        final double vz = pvDim.getVelocity().getZ();

        // Position vector in the primary inertial frame
        final Vector3D newPos = new Vector3D(c[0][0] * x + c[0][1] * y + c[0][2] * z,
                                             c[1][0] * x + c[1][1] * y + c[1][2] * z,
                                             c[2][0] * x + c[2][1] * y + c[2][2] * z);

        final Vector3D vel0 = new Vector3D(c[0][0] * vx + c[0][1] * vy + c[0][2] * vz,
                                           c[1][0] * vx + c[1][1] * vy + c[1][2] * vz,
                                           c[2][0] * vx + c[2][1] * vy + c[2][2] * vz);
        final Vector3D addVel = new Vector3D(c[0][1] * x - c[0][0] * y,
                                             c[1][1] * x - c[1][0] * y,
                                             c[2][1] * x - c[2][0] * y);
        final Vector3D newVel = vel0.add(addVel.scalarMultiply(theta));

        // State vector in the primary inertial frame
        final PVCoordinates pv2 = new PVCoordinates(newPos, newVel);

        // Transformation between primary inertial frame and the output frame
        final Transform primaryInertialToOutputFrame = primaryInertialFrame.getTransformTo(outputFrame, initialDate);

        final PVCoordinates pvMat = primaryInertialToOutputFrame.transformPVCoordinates(pv2);
        final AbsolutePVCoordinates apv0 = new AbsolutePVCoordinates(outputFrame,initialDate,pv0);
        final AbsolutePVCoordinates apvTrans = syst.getRealAPV(apv0,initialDate,outputFrame);

        Assertions.assertEquals(pvMat.getPosition().getX(),apvTrans.getPosition().getX(),1E-5);
        Assertions.assertEquals(pvMat.getPosition().getY(),apvTrans.getPosition().getY(),1E-15);
        Assertions.assertEquals(pvMat.getPosition().getZ(),apvTrans.getPosition().getZ(),1E-4);

        Assertions.assertEquals(pvMat.getVelocity().getX(),apvTrans.getVelocity().getX(),1E-2);
        Assertions.assertEquals(pvMat.getVelocity().getY(),apvTrans.getVelocity().getY(),4E-2);
        Assertions.assertEquals(pvMat.getVelocity().getZ(),apvTrans.getVelocity().getZ(),2E-2);

        Assertions.assertEquals(initialDate.durationFrom(AbsoluteDate.J2000_EPOCH),
                            apvTrans.getDate().durationFrom(AbsoluteDate.J2000_EPOCH),
                            1E-10);
    }
}
