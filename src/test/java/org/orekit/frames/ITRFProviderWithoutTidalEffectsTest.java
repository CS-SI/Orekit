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


import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class ITRFProviderWithoutTidalEffectsTest {

    @Test
    public void testRoughRotation() throws OrekitException {

        AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2006, 02, 24),
                                              new TimeComponents(15, 38, 00),
                                              TimeScalesFactory.getUTC());
        Frame itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Transform t0 = itrf2008.getTransformTo(FramesFactory.getEME2000(), date1);

        double dt = 10.0;
        AbsoluteDate date2 = date1.shiftedBy(dt);
        Transform t1 = itrf2008.getTransformTo(FramesFactory.getEME2000(), date2);
        Transform evolution = new Transform(date2, t0.getInverse(), t1);

        Vector3D p = new Vector3D(6000,6000,0);
        Assert.assertEquals(0.0, evolution.transformPosition(Vector3D.ZERO).getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, evolution.transformVector(p).getZ(), 0.003);
        Assert.assertEquals(2 * FastMath.PI * dt / 86164,
                            Vector3D.angle(t0.transformVector(p), t1.transformVector(p)),
                            1.0e-9);

    }

    @Test
    public void testRoughOrientation() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(2001, 03, 21, 0, 4, 0, TimeScalesFactory.getUTC());
        Frame itrf2008 = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        Vector3D u = itrf2008.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.MINUS_I) < FastMath.toRadians(0.5));

        date = date.shiftedBy(6 * 3600);
        u = itrf2008.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.MINUS_J) < FastMath.toRadians(0.5));

        date = date.shiftedBy(6 * 3600);
        u = itrf2008.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.PLUS_I) < FastMath.toRadians(0.5));

        date = date.shiftedBy(6 * 3600);
        u = itrf2008.getTransformTo(FramesFactory.getEME2000(), date).transformVector(Vector3D.PLUS_I);
        Assert.assertTrue(Vector3D.angle(u, Vector3D.PLUS_J) < FastMath.toRadians(0.5));

    }

    @Test
    public void testRoughERA() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(2001, 03, 21, 0, 4, 0, TimeScalesFactory.getUTC());
        TIRFProvider TIRF2000 = (TIRFProvider) FramesFactory.getTIRF(IERSConventions.IERS_2010).getTransformProvider();

        Assert.assertEquals(180, FastMath.toDegrees(TIRF2000.getEarthRotationAngle(date)), 0.5);

        date = date.shiftedBy(6 * 3600);
        Assert.assertEquals(-90, FastMath.toDegrees(TIRF2000.getEarthRotationAngle(date)), 0.5);

        date = date.shiftedBy(6 * 3600);
        Assert.assertEquals(0, FastMath.toDegrees(TIRF2000.getEarthRotationAngle(date)), 0.5);

        date = date.shiftedBy(6 * 3600);
        Assert.assertEquals(90, FastMath.toDegrees(TIRF2000.getEarthRotationAngle(date)), 0.5);

    }

    @Test
    public void testMSLIBTransformJ2000_TerVrai() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 10, 14),
                                             new TimeComponents(02, 00, 00),
                                             TimeScalesFactory.getUTC());
        Transform trans = FramesFactory.getEME2000().getTransformTo(FramesFactory.getTIRF(IERSConventions.IERS_2010), date);

        // Positions
        Vector3D posTIRF =
            trans.transformPosition(new Vector3D(6500000.0, -1234567.0, 4000000.0));
        Assert.assertEquals(0,  3011109.361 - posTIRF.getX(), 0.38);
        Assert.assertEquals(0, -5889822.669 - posTIRF.getY(), 0.38);
        Assert.assertEquals(0,  4002170.039 - posTIRF.getZ(), 0.27);

    }

    @Test
    public void testMSLIBTransformJ2000_TerRef() throws OrekitException {

        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2003, 10, 14),
                                           new TimeComponents(02, 00, 00),
                                           TimeScalesFactory.getUTC());
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Transform trans = FramesFactory.getEME2000().getTransformTo(itrf, t0);

        // Coordinates in EME2000
        PVCoordinates pvEME2000 =
            new PVCoordinates(new Vector3D(6500000.0, -1234567.0, 4000000.0),
                              new Vector3D(3609.28229, 3322.88979, -7083.950661));

        // Reference coordinates in ITRF
        PVCoordinates pvRef =
            new PVCoordinates(new Vector3D(3011113.971820046, -5889827.854375269, 4002158.938875904),
                              new Vector3D(4410.393506651586, -1033.61784235127, -7082.633883124906));


        // tests using direct transform
        checkPV(pvRef, trans.transformPVCoordinates(pvEME2000), 0.594, 1.79e-4);

        // compute local evolution using finite differences
        double h = 1.0;
        Rotation r0 = trans.getRotation();
        AbsoluteDate date = t0.shiftedBy(-2 * h);
        Rotation evoM2h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().compose(r0.revert(),
                                                                                                      RotationConvention.VECTOR_OPERATOR);
        double alphaM2h = -evoM2h.getAngle();
        Vector3D axisM2h = evoM2h.getAxis(RotationConvention.VECTOR_OPERATOR);
        date = t0.shiftedBy(-h);
        Rotation evoM1h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().compose(r0.revert(),
                                                                                                      RotationConvention.VECTOR_OPERATOR);
        double alphaM1h = -evoM1h.getAngle();
        Vector3D axisM1h = evoM1h.getAxis(RotationConvention.VECTOR_OPERATOR);
        date = t0.shiftedBy(h);
        Rotation evoP1h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().compose(r0.revert(),
                                                                                                      RotationConvention.VECTOR_OPERATOR);
        double alphaP1h =  evoP1h.getAngle();
        Vector3D axisP1h = evoP1h.getAxis(RotationConvention.VECTOR_OPERATOR).negate();
        date = t0.shiftedBy(2 * h);
        Rotation evoP2h = FramesFactory.getEME2000().getTransformTo(itrf, date).getRotation().compose(r0.revert(),
                                                                                                      RotationConvention.VECTOR_OPERATOR);
        double alphaP2h =  evoP2h.getAngle();
        Vector3D axisP2h = evoP2h.getAxis(RotationConvention.VECTOR_OPERATOR).negate();
        double w = (8 * (alphaP1h - alphaM1h) - (alphaP2h - alphaM2h)) / (12 * h);
        Vector3D axis = axisM2h.add(axisM1h).add(axisP1h.add(axisP2h)).normalize();
        Transform finiteDiffTransform = new Transform(t0, trans.getRotation() , new Vector3D(w ,axis));

        checkPV(pvRef, finiteDiffTransform.transformPVCoordinates(pvEME2000), 0.594, 1.78e-4);

    }

    @Test
    public void testMontenbruck() throws OrekitException {
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00,
                                           TimeScalesFactory.getGPS());
        Transform trans = FramesFactory.getITRF(IERSConventions.IERS_2010, true).getTransformTo(FramesFactory.getGCRF(), t0);
        PVCoordinates pvWGS =
            new PVCoordinates(new Vector3D(19440953.805,16881609.273,-6777115.092),
                              new Vector3D(-811.1827456,-257.3799137,-3068.9508125));
        checkPV(new PVCoordinates(new Vector3D(-23830592.685,  -9747073.881,  -6779831.010),
                                  new Vector3D( 1561.9646362, -1754.3454485, -3068.8504996)),
                                  trans.transformPVCoordinates(pvWGS), 0.146, 3.43e-5);

    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assert.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
