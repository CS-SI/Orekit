/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.apache.commons.math.geometry.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class LocalOrbitalFrameTest {

    @Test
    public void testTNW() throws OrekitException {
        
        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, KeplerianOrbit.TRUE_ANOMALY, 
                                                FramesFactory.getGCRF(), initDate,
                                                3.986004415e14);
        Propagator propagator = new KeplerianPropagator(initialOrbit);
        LocalOrbitalFrame tnw =
            new LocalOrbitalFrame(FramesFactory.getGCRF(), LocalOrbitalFrame.LOFType.TNW,
                                  propagator, "TNW");

        AbsoluteDate date = new AbsoluteDate(initDate, 400);
        Transform t = tnw.getTransformTo(FramesFactory.getGCRF(), date);
        PVCoordinates pv1 = t.transformPVCoordinates(PVCoordinates.ZERO);
        Vector3D p1 = pv1.getPosition();
        Vector3D v1 = pv1.getVelocity();
        PVCoordinates pv2 =
            propagator.propagate(date).getPVCoordinates(FramesFactory.getGCRF());
        Vector3D p2 = pv2.getPosition();
        Vector3D v2 = pv2.getVelocity();
        Vector3D momentum = Vector3D.crossProduct(p2, v2);
        Assert.assertEquals(0, p1.subtract(p2).getNorm(), 1.0e-14 * p1.getNorm());
        Assert.assertEquals(0, v1.subtract(v2).getNorm(), 1.0e-14 * v1.getNorm());

        Vector3D xDirection = t.transformVector(Vector3D.PLUS_I);
        Vector3D yDirection = t.transformVector(Vector3D.PLUS_J);
        Vector3D zDirection = t.transformVector(Vector3D.PLUS_K);
        Assert.assertEquals(0, Vector3D.angle(v2, xDirection), 1.0e-15);
        Assert.assertEquals(0, Vector3D.angle(momentum, zDirection), 1.0e-15);
        Assert.assertTrue(Vector3D.dotProduct(yDirection, p2) < 0);

        Assert.assertEquals(initialOrbit.getKeplerianMeanMotion(), t.getRotationRate().getNorm(), 1.0e-7);

    }    

    @Test
    public void testQSW() throws OrekitException {
        
        AbsoluteDate initDate = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 584.);
        Orbit initialOrbit = new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9,
                                                6.2, KeplerianOrbit.TRUE_ANOMALY, 
                                                FramesFactory.getGCRF(), initDate,
                                                3.986004415e14);
        Propagator propagator = new KeplerianPropagator(initialOrbit);
        LocalOrbitalFrame tnw =
            new LocalOrbitalFrame(FramesFactory.getGCRF(), LocalOrbitalFrame.LOFType.QSW,
                                  propagator, "QSW");

        AbsoluteDate date = new AbsoluteDate(initDate, 400);
        Transform t = tnw.getTransformTo(FramesFactory.getGCRF(), date);
        PVCoordinates pv1 = t.transformPVCoordinates(PVCoordinates.ZERO);
        Vector3D p1 = pv1.getPosition();
        Vector3D v1 = pv1.getVelocity();
        PVCoordinates pv2 =
            propagator.propagate(date).getPVCoordinates(FramesFactory.getGCRF());
        Vector3D p2 = pv2.getPosition();
        Vector3D v2 = pv2.getVelocity();
        Vector3D momentum = Vector3D.crossProduct(p2, v2);
        Assert.assertEquals(0, p1.subtract(p2).getNorm(), 1.0e-14 * p1.getNorm());
        Assert.assertEquals(0, v1.subtract(v2).getNorm(), 1.0e-14 * v1.getNorm());

        Vector3D xDirection = t.transformVector(Vector3D.PLUS_I);
        Vector3D yDirection = t.transformVector(Vector3D.PLUS_J);
        Vector3D zDirection = t.transformVector(Vector3D.PLUS_K);
        Assert.assertEquals(0, Vector3D.angle(p2, xDirection), 1.0e-15);
        Assert.assertEquals(0, Vector3D.angle(momentum, zDirection), 1.0e-15);
        Assert.assertTrue(Vector3D.dotProduct(yDirection, v2) > 0);

        Assert.assertEquals(initialOrbit.getKeplerianMeanMotion(), t.getRotationRate().getNorm(), 1.0e-7);

    }    

}
