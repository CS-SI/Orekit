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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

@Deprecated
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

    @Test
    public void testNoFieldProvider() throws OrekitException {
        try {
            LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(),
                                                          LOFType.VNC, provider, "some-name");
            lof.getTransformTo(FramesFactory.getGCRF(), fieldInitialOrbit.getDate());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.LOF_FRAME_NO_PROVIDER_FOR_FIELD, oe.getSpecifier());
            Assert.assertEquals(Decimal64Field.class.toString(), oe.getParts()[0]);
            Assert.assertEquals("some-name", oe.getParts()[1]);
        }
    }

    @Test
    public void testTNW64() throws OrekitException {
        
        FieldAbsoluteDate<Decimal64> date = fieldInitialOrbit.getDate();
        FieldPVCoordinates<Decimal64> pv = fieldProvider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.TNW, date,
                   pv.getVelocity(),
                   FieldVector3D.crossProduct(pv.getMomentum(), pv.getVelocity()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testQSW64() throws OrekitException {
        FieldAbsoluteDate<Decimal64> date = fieldInitialOrbit.getDate();
        FieldPVCoordinates<Decimal64> pv = fieldProvider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.QSW, date,
                   pv.getPosition(),
                   FieldVector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testLVLH64() throws OrekitException {
        FieldAbsoluteDate<Decimal64> date = fieldInitialOrbit.getDate();
        FieldPVCoordinates<Decimal64> pv = fieldProvider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.LVLH, date,
                   pv.getPosition(),
                   FieldVector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testVVLH64() throws OrekitException {
        FieldAbsoluteDate<Decimal64> date = fieldInitialOrbit.getDate();
        FieldPVCoordinates<Decimal64> pv = fieldProvider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.VVLH, date,
                   FieldVector3D.crossProduct(pv.getMomentum(), pv.getPosition()),
                   pv.getMomentum().negate(),
                   pv.getPosition().negate(),
                   pv.getMomentum().negate());
    }

    @Test
    public void testVNC64() throws OrekitException {
        FieldAbsoluteDate<Decimal64> date = fieldInitialOrbit.getDate();
        FieldPVCoordinates<Decimal64> pv = fieldProvider.getPVCoordinates(date, inertialFrame);
        checkFrame(LOFType.VNC, date,
                   pv.getVelocity(),
                   pv.getMomentum(),
                   FieldVector3D.crossProduct(pv.getVelocity(), pv.getMomentum()),
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

    private  void checkFrame(LOFType type, FieldAbsoluteDate<Decimal64> date,
                             FieldVector3D<Decimal64> expectedXDirection, FieldVector3D<Decimal64> expectedYDirection,
                             FieldVector3D<Decimal64> expectedZDirection, FieldVector3D<Decimal64> expectedRotationDirection)
        throws OrekitException {
        LocalOrbitalFrame lof = new LocalOrbitalFrame(FramesFactory.getGCRF(), type, provider, type.name());
        lof.addFieldProvider(Decimal64Field.getInstance(), fieldProvider);
        FieldTransform<Decimal64> t = lof.getTransformTo(FramesFactory.getGCRF(), date);
        FieldPVCoordinates<Decimal64> pv1 = t.transformPVCoordinates(PVCoordinates.ZERO);
        FieldVector3D<Decimal64> p1 = pv1.getPosition();
        FieldVector3D<Decimal64> v1 = pv1.getVelocity();
        FieldPVCoordinates<Decimal64> pv2 = fieldProvider.getPVCoordinates(date, FramesFactory.getGCRF());
        FieldVector3D<Decimal64> p2 = pv2.getPosition();
        FieldVector3D<Decimal64> v2 = pv2.getVelocity();
        Assert.assertEquals(0, p1.subtract(p2).getNorm().getReal(), 1.0e-14 * p1.getNorm().getReal());
        Assert.assertEquals(0, v1.subtract(v2).getNorm().getReal(), 1.0e-14 * v1.getNorm().getReal());

        FieldVector3D<Decimal64> xDirection = t.transformVector(Vector3D.PLUS_I);
        FieldVector3D<Decimal64> yDirection = t.transformVector(Vector3D.PLUS_J);
        FieldVector3D<Decimal64> zDirection = t.transformVector(Vector3D.PLUS_K);
        Assert.assertEquals(0, FieldVector3D.angle(expectedXDirection, xDirection).getReal(), 2.0e-15);
        Assert.assertEquals(0, FieldVector3D.angle(expectedYDirection, yDirection).getReal(), 1.0e-15);
        Assert.assertEquals(0, FieldVector3D.angle(expectedZDirection, zDirection).getReal(), 1.0e-15);
        Assert.assertEquals(0, FieldVector3D.angle(expectedRotationDirection, t.getRotationRate()).getReal(), 1.0e-15);

    }

    @Before
    public void setUp() throws OrekitException {
        inertialFrame = FramesFactory.getGCRF();
        initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        initialOrbit =
                new KeplerianOrbit(7209668.0, 0.5e-4, 1.7, 2.1, 2.9, 6.2, PositionAngle.TRUE,
                                   inertialFrame, initDate, 3.986004415e14);
        provider = new KeplerianPropagator(initialOrbit);
        fieldInitialOrbit = new FieldKeplerianOrbit<Decimal64>(new TimeStampedFieldPVCoordinates<Decimal64>(initialOrbit.getDate(),
                                                                                                            new FieldPVCoordinates<>(Decimal64Field.getInstance(),
                                                                                                                                     initialOrbit.getPVCoordinates())),
                                                               initialOrbit.getFrame(), initialOrbit.getMu());
        fieldProvider = new FieldKeplerianPropagator<Decimal64>(fieldInitialOrbit);

    }

    private Frame                                 inertialFrame;
    private AbsoluteDate                          initDate;
    private Orbit                                 initialOrbit;
    private PVCoordinatesProvider                 provider;
    private FieldOrbit<Decimal64>                 fieldInitialOrbit;
    private FieldPVCoordinatesProvider<Decimal64> fieldProvider;

}
