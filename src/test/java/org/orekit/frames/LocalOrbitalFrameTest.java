/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.frames;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class LocalOrbitalFrameTest {

    @Test
    public void testTNW() throws OrekitException {
        AbsoluteDate date = initDate.shiftedBy(400);
        PVCoordinates pv = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.TNW, date,
                   pv.getVelocity(),
                   Vector3D.crossProduct(pv.getMomentum(), pv.getVelocity()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testQSW() throws OrekitException {
        AbsoluteDate date = initDate.shiftedBy(400);
        PVCoordinates pv = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.QSW, date,
                   pv.getPosition(),
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testLVLH() throws OrekitException {
        AbsoluteDate date = initDate.shiftedBy(400);
        PVCoordinates pv = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.LVLH, date,
                   pv.getPosition(),
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testVVLH() throws OrekitException {
        AbsoluteDate date = initDate.shiftedBy(400);
        PVCoordinates pv = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.VVLH, date,
                   Vector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum().negate(),
                   pv.getPosition().negate(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testVNC() throws OrekitException {
        AbsoluteDate date = initDate.shiftedBy(400);
        PVCoordinates pv = provider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.VNC, date,
                   pv.getVelocity(),
                   pv.getMomentum(),
                   Vector3D.crossProduct(pv.getVelocity(), pv.getMomentum()),
                   pv.getMomentum().negate());
    }

    private void checkFrame(LOFType type, AbsoluteDate date,
                            Vector3D expectedXDirection, Vector3D expectedYDirection,
                            Vector3D expectedZDirection, Vector3D expectedRotationDirection)
        throws OrekitException {
        LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), type, provider, type.name());

        Transform t = lof.getTransformTo(FramesFactory.getGCRF(), date);
        PVCoordinates pv1 = t.transformPVCoordinates(PVCoordinates.ZERO);
        Vector3D p1 = pv1.getPosition();
        Vector3D v1 = pv1.getVelocity();
        PVCoordinates pv2 = provider.getPVCoordinates(date, FramesFactory.getGCRF());
        Vector3D p2 = pv2.getPosition();
        Vector3D v2 = pv2.getVelocity();
        Assert.assertEquals(0, p1.subtract(p2).getNorm(), 1.0e-14 * p1.getNorm());
        Assert.assertEquals(0, v1.subtract(v2).getNorm(), 1.0e-14 * v1.getNorm());

        Vector3D xDirection = t.transformVector(Vector3D.PLUS_I);
        Vector3D yDirection = t.transformVector(Vector3D.PLUS_J);
        Vector3D zDirection = t.transformVector(Vector3D.PLUS_K);
        Assert.assertEquals(0, Vector3D.angle(expectedXDirection, xDirection), 2.0e-15);
        Assert.assertEquals(0, Vector3D.angle(expectedYDirection, yDirection), 1.0e-15);
        Assert.assertEquals(0, Vector3D.angle(expectedZDirection, zDirection), 1.0e-15);
        Assert.assertEquals(0, Vector3D.angle(expectedRotationDirection, t.getRotationRate()), 1.0e-15);

        Assert.assertEquals(initialOrbit.getKeplerianMeanMotion(), t.getRotationRate().getNorm(), 1.0e-7);

    }

    @Before
    public void setUp() throws OrekitException {
        inertialFrame = FramesFactory.getGCRF();
        initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit =
                new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9, 6.2, PositionAngle.TRUE,
                                   inertialFrame, initDate, 3.986004415e14);
        provider = new KeplerianPropagator(initialOrbit);

    }

    private Frame                 inertialFrame;
    private AbsoluteDate          initDate;
    private Orbit                 initialOrbit;
    private PVCoordinatesProvider provider;

}
