/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class VersionedITRFFrameTest {

    @Test
    public void testBulletinABefore2018Jump() {
        // before 2018-03-23, bulletin-A EOP were referenced to ITRF-2008
        doTestBulletinA2018Jump(new AbsoluteDate(2018, 3, 20, 12, 34, 56.7,
                                                 TimeScalesFactory.getUTC()),
                                0.0, 2.943e-3, 1.0e-6);
    }

    @Test
    public void testBulletinAAfter2018Jump() {
        // after 2018-03-23, bulletin-A EOP were referenced to ITRF-2014
        doTestBulletinA2018Jump(new AbsoluteDate(2018, 3, 26, 12, 34, 56.7,
                                                 TimeScalesFactory.getUTC()),
                                2.942e-3, 0.0, 1.0e-6);
    }

    private void doTestBulletinA2018Jump(AbsoluteDate date,
                                         double expectedDistance2008,
                                         double expectedDistance2014,
                                         double tolerance)
        {
        Frame eme2000          = FramesFactory.getEME2000();
        Frame unspecifiedITRF  = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        VersionedITRF itrf2008 = FramesFactory.getITRF(ITRFVersion.ITRF_2008,
                                                       IERSConventions.IERS_2010, false);
        VersionedITRF itrf2014 = FramesFactory.getITRF(ITRFVersion.ITRF_2014,
                                                       IERSConventions.IERS_2010, false);
        Assert.assertEquals(ITRFVersion.ITRF_2008, itrf2008.getITRFVersion());
        Assert.assertEquals(ITRFVersion.ITRF_2014, itrf2014.getITRFVersion());

        GeodeticPoint laPaz = new GeodeticPoint(FastMath.toRadians(-16.50),
                                                FastMath.toRadians(-68.15),
                                                3640.0);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      unspecifiedITRF);
        Vector3D p = earth.transform(laPaz);

        // regular transform
        Vector3D pUnspecified = unspecifiedITRF.getTransformTo(eme2000, date).transformPosition(p);
        Vector3D p2008        = itrf2008.getTransformTo(eme2000, date).transformPosition(p);
        Vector3D p2014        = itrf2014.getTransformTo(eme2000, date).transformPosition(p);
        Assert.assertEquals(expectedDistance2008, Vector3D.distance(pUnspecified, p2008), tolerance);
        Assert.assertEquals(expectedDistance2014, Vector3D.distance(pUnspecified, p2014), tolerance);

        // non-interpolating transform
        Vector3D pUnspecifiedNI = FramesFactory.getNonInterpolatingTransform(unspecifiedITRF,
                                                                             eme2000,
                                                                             date).transformPosition(p);
        Vector3D p2008NI        = FramesFactory.getNonInterpolatingTransform(itrf2008, eme2000, date).transformPosition(p);
        Vector3D p2014NI        = FramesFactory.getNonInterpolatingTransform(itrf2014, eme2000, date).transformPosition(p);
        Assert.assertEquals(expectedDistance2008, Vector3D.distance(pUnspecifiedNI, p2008NI), tolerance);
        Assert.assertEquals(expectedDistance2014, Vector3D.distance(pUnspecifiedNI, p2014NI), tolerance);

        // field transform
        FieldAbsoluteDate<Decimal64> dateField     = new FieldAbsoluteDate<>(Decimal64Field.getInstance(), date);
        FieldVector3D<Decimal64> pUnspecifiedField = unspecifiedITRF.getTransformTo(eme2000, dateField).transformPosition(p);
        FieldVector3D<Decimal64> p2008Field        = itrf2008.getTransformTo(eme2000, dateField).transformPosition(p);
        FieldVector3D<Decimal64> p2014Field        = itrf2014.getTransformTo(eme2000, dateField).transformPosition(p);
        Assert.assertEquals(expectedDistance2008, FieldVector3D.distance(pUnspecifiedField, p2008Field).getReal(), tolerance);
        Assert.assertEquals(expectedDistance2014, FieldVector3D.distance(pUnspecifiedField, p2014Field).getReal(), tolerance);
        
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        VersionedITRF itrf2008 = FramesFactory.getITRF(ITRFVersion.ITRF_2008,
                                                       IERSConventions.IERS_2010, false);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(itrf2008);

        Assert.assertTrue(bos.size() > 40000);
        Assert.assertTrue(bos.size() < 45000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        VersionedITRF deserialized  = (VersionedITRF) ois.readObject();
        Assert.assertEquals(ITRFVersion.ITRF_2008, deserialized.getITRFVersion());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("itrf-jump");
    }

}
