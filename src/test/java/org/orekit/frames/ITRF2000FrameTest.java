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

import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.TIRF2000Frame;
import org.orekit.frames.Transform;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class ITRF2000FrameTest extends TestCase {

    public void testRoughRotation() throws ParseException, OrekitException {

        AbsoluteDate date1 = new AbsoluteDate(new ChunkedDate(2006, 02, 24),
                                              new ChunkedTime(15, 38, 00),
                                              UTCScale.getInstance());
        Frame itrf2000 = Frame.getITRF2000B();
        Transform t0 = itrf2000.getTransformTo(Frame.getJ2000(), date1 );

        double dt = 10.0;
        AbsoluteDate date2 = new AbsoluteDate(date1, dt);
        Transform t1 = itrf2000.getTransformTo(Frame.getJ2000(), date2);
        Transform evolution = new Transform(t0.getInverse(), t1);

        assertEquals(0.0, evolution.transformPosition(new Vector3D(0,0,0)).getNorm(), 1.0e-10);
        assertTrue(Vector3D.dotProduct(Vector3D.PLUS_K, evolution.transformVector(new Vector3D(6000,6000,0))) < 0.01);
        assertEquals(2 * Math.PI * dt / 86164, Vector3D.angle(
                                                              t0.transformVector(new Vector3D(6000,6000,0)), t1.transformVector(new Vector3D(6000,6000,0))),
                                                              1.0e-9);

    }

    public void testRoughOrientation() throws ParseException, OrekitException {

        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                             ChunkedTime.H00,
                                             UTCScale.getInstance());
        Frame itrf2000 = Frame.getITRF2000B();

        Vector3D u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.PLUS_I);
        assertTrue(Vector3D.angle(u, Vector3D.MINUS_I) < Math.toRadians(2));

        date = new AbsoluteDate(date, 6 * 3600);
        u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.PLUS_I);
        assertTrue(Vector3D.angle(u, Vector3D.MINUS_J) < Math.toRadians(2));

        date = new AbsoluteDate(date, 6 * 3600);
        u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.PLUS_I);
        assertTrue(Vector3D.angle(u, Vector3D.PLUS_I) < Math.toRadians(2));

        date = new AbsoluteDate(date, 6 * 3600);
        u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.PLUS_I);
        assertTrue(Vector3D.angle(u, Vector3D.PLUS_J) < Math.toRadians(2));

    }

    public void testRoughERA() throws ParseException, OrekitException {

        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                             ChunkedTime.H00,
                                             UTCScale.getInstance());
        TIRF2000Frame TIRF2000 = (TIRF2000Frame) Frame.getTIRF2000B();

        assertEquals(180, Math.toDegrees(TIRF2000.getEarthRotationAngle(date)), 2.0);

        date = new AbsoluteDate(date, 6 * 3600);
        assertEquals(-90, Math.toDegrees(TIRF2000.getEarthRotationAngle(date)), 2.0);

        date = new AbsoluteDate(date, 6 * 3600);
        assertEquals(0, Math.toDegrees(TIRF2000.getEarthRotationAngle(date)), 2.0);

        date = new AbsoluteDate(date, 6 * 3600);
        assertEquals(90, Math.toDegrees(TIRF2000.getEarthRotationAngle(date)), 2.0);

    }

    public void testMSLIBTransformJ2OOO_TerVrai() throws OrekitException, ParseException {

        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2003, 10, 14),
                                             new ChunkedTime(02, 00, 00),
                                             UTCScale.getInstance());

        Frame tirf = Frame.getTIRF2000B();

        Transform trans = Frame.getJ2000().getTransformTo(tirf, date);

        // Positions

        Vector3D posJ2000 = new Vector3D(6500000.0,
                                         -1234567.0,
                                         4000000.0);

        Vector3D posTIRF = trans.transformPosition(posJ2000);

        Vector3D posTestCase = new Vector3D(3011109.360780633,
                                            -5889822.669411588,
                                            4002170.0385907636);

        // Position tests
        checkVectors(posTestCase, posTIRF, 1.4e-7, 1.4e-7, 1.07);

    }

    public void testMSLIBTransformJ2000_TerRef() throws OrekitException, ParseException {

        AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2003, 10, 14),
                                           new ChunkedTime(02, 00, 00),
                                           UTCScale.getInstance());

        Frame itrf = Frame.getITRF2000B();

        Transform trans = Frame.getJ2000().getTransformTo(itrf, t0);

        // Positions

        Vector3D posJ2000 = new Vector3D(6500000.0,
                                         -1234567.0,
                                         4000000.0);

        Vector3D posITRF = trans.transformPosition(posJ2000);

        Vector3D posTestCaseRef = new Vector3D(3011113.971820046,
                                               -5889827.854375269,
                                               4002158.938875904);

        // Position tests
        checkVectors(posTestCaseRef, posITRF, 1.4e-7, 1.4e-7, 1.07);

        // velocity tests

        Vector3D speedJ2000 = new Vector3D(3609.28229,
                                           3322.88979,
                                           -7083.950661);

        Vector3D speedTestCase = new Vector3D(4410.393506651586,
                                              -1033.61784235127,
                                              -7082.633883124906);

        Rotation r0 = trans.getRotation();

        // compute local evolution using finite differences

        double h = 0.1;
        AbsoluteDate date = new AbsoluteDate(t0, -2 * h);
        Rotation evoM2h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
        double alphaM2h = -evoM2h.getAngle();
        Vector3D axisM2h = evoM2h.getAxis();
        date = new AbsoluteDate(t0, -h);
        Rotation evoM1h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
        double alphaM1h = -evoM1h.getAngle();
        Vector3D axisM1h = evoM1h.getAxis();
        date = new AbsoluteDate(t0,  h);
        Rotation evoP1h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
        double alphaP1h =  evoP1h.getAngle();
        Vector3D axisP1h = evoP1h.getAxis().negate();
        date = new AbsoluteDate(t0, 2 * h);
        Rotation evoP2h = Frame.getJ2000().getTransformTo(itrf, date).getRotation().applyTo(r0.revert());
        double alphaP2h =  evoP2h.getAngle();
        Vector3D axisP2h = evoP2h.getAxis().negate();
        double w = (8 * (alphaP1h - alphaM1h) - (alphaP2h - alphaM2h)) / (12 * h);
        Vector3D axis = axisM2h.add(axisM1h).add(axisP1h.add(axisP2h)).normalize();
        Transform tr = new Transform(trans.getRotation() , new Vector3D(w ,axis));

        PVCoordinates pv = new PVCoordinates(posJ2000 , speedJ2000);

        PVCoordinates result = tr.transformPVCoordinates(pv);

        checkVectors(speedTestCase, result.getVelocity(), 1.9e-7, 1.44e-7,0.002);

        result = trans.transformPVCoordinates(pv);
        checkVectors(speedTestCase, result.getVelocity(), 1.9e-7, 1.5e-7, 0.002);


    }

    public void testGMS1() throws OrekitException, ParseException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2006, 05, 14),
                                             new ChunkedTime(0, 8, 51.423),
                                             UTCScale.getInstance());
        Frame itrf = Frame.getITRF2000A();
        Transform trans = itrf.getTransformTo(Frame.getJ2000(), date);
        Vector3D posITRF = new Vector3D(6770000.000, -144000.000, 488000.000);
        Vector3D velITRF = new Vector3D(530.000, 4260.000, -5980.000);
        PVCoordinates pv2000 = trans.transformPVCoordinates(new PVCoordinates(posITRF, velITRF));
        assertEquals(-4120240.360036977,  pv2000.getPosition().getX(), 1.0e-10);
        assertEquals(-5373504.716481836,  pv2000.getPosition().getY(), 1.0e-10);
        assertEquals(490761.07982380746,  pv2000.getPosition().getZ(), 1.0e-10);
        assertEquals(3509.5443642075716,  pv2000.getVelocity().getX(), 1.0e-10);
        assertEquals(-3247.8483989909146, pv2000.getVelocity().getY(), 1.0e-10);
        assertEquals(-5982.019512837689,  pv2000.getVelocity().getZ(), 1.0e-10);
    }

    public void testGMS2() throws OrekitException, ParseException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2006, 05, 14),
                                             new ChunkedTime(00, 16, 08.631),
                                             UTCScale.getInstance());
        Frame itrf = Frame.getITRF2000B();
        Transform trans = itrf.getTransformTo(Frame.getJ2000(), date);
        Vector3D posITRF = new Vector3D(6254020.457, 1663297.258, -2070251.762);
        Vector3D velITRF = new Vector3D(-2861.533, 3913.691, -5536.168);
        PVCoordinates pv2000 = trans.transformPVCoordinates(new PVCoordinates(posITRF, velITRF));
        assertEquals(-2166074.5292187054,  pv2000.getPosition().getX(), 1.0e-10);
        assertEquals(-6098691.112316115,  pv2000.getPosition().getY(), 1.0e-10);
        assertEquals(-2068661.3675358547,  pv2000.getPosition().getZ(), 1.0e-10);
        assertEquals(5287.320112599562,  pv2000.getVelocity().getX(), 1.0e-10);
        assertEquals(-11.208557244797248, pv2000.getVelocity().getY(), 1.0e-10);
        assertEquals(-5539.41752885036,  pv2000.getVelocity().getZ(), 1.0e-10);
    }

    public void testGMS3() throws OrekitException, ParseException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2006, 05, 14),
                                             new ChunkedTime(00, 26, 06.833),
                                             UTCScale.getInstance());
        Frame itrf = Frame.getITRF2000B();
        Transform trans = itrf.getTransformTo(Frame.getJ2000(), date);
        Vector3D posITRF = new Vector3D(3376169.673, 3578504.767, -4685496.977);
        Vector3D velITRF = new Vector3D(-6374.220, 2284.616, -2855.447);
        PVCoordinates pv2000 = trans.transformPVCoordinates(new PVCoordinates(posITRF, velITRF));
        assertEquals(1247881.068,  pv2000.getPosition().getX(), 100.0);
        assertEquals(-4758546.914, pv2000.getPosition().getY(), 250.0);
        assertEquals(-4686066.307, pv2000.getPosition().getZ(),   5.0);
        assertEquals(5655.84583,   pv2000.getVelocity().getX(),   0.1);
        assertEquals(4291.97158,   pv2000.getVelocity().getY(),   0.1);
        assertEquals(-2859.11413,  pv2000.getVelocity().getZ(),   0.01);
    }

//    public void testValladoReference() throws OrekitException, ParseException {
//
//        AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2004, 04, 06),
//                                           new ChunkedTime(07, 51, 28.386),
//                                           UTCScale.getInstance());
//        t0 = new AbsoluteDate(t0, 0.000009);
//
//        Frame j2000 = Frame.getJ2000();
//        Frame irf = Frame.getReferenceFrame(Frame.IRF2000A, t0);
//        Frame tirf = Frame.getReferenceFrame(Frame.TIRF2000A, t0);
//        Frame itrf = Frame.getReferenceFrame(Frame.ITRF2000A, t0);
//
//        Transform trans;
//        PVCoordinates pv;
//        PVCoordinates result;
//
//        // test cases
//
//        Vector3D testPosJ2000 = new Vector3D(5102.508958*1000,
//                                             6123.011401*1000,
//                                             6378.136928*1000);
//
//        Vector3D testVelJ2000 = new Vector3D(-4.74322016*1000,
//                                             0.79053650*1000,
//                                             5.533756573*1000);
//
//        Vector3D testPosIRF = new Vector3D(5100.0076393*1000,
//                                           6122.7764115*1000,
//                                           6380.343827*1000);
//
//        Vector3D testVelIRF = new Vector3D(-4.745388938*1000,
//                                           0.790332038*1000,
//                                           5.531929087*1000);
//
//        Vector3D testPosTIRF = new Vector3D(-1033.4750313*1000,
//                                            7901.2909240*1000,
//                                            6380.3438271*1000);
//
//        Vector3D testVelTIRF = new Vector3D(-3.225632747*1000,
//                                            -2.872455223*1000,
//                                            5.531929087*1000);
//
//        Vector3D testPosITRF = new Vector3D(-1033.4793830*1000,
//                                            7901.2952758*1000,
//                                            6380.3565953*1000);
//
//        Vector3D testVelITRF = new Vector3D(-3.225636520*1000,
//                                            -2.872451450*1000,
//                                            5.531924446*1000);
//
//        // tests
//
//        trans = j2000.getTransformTo(irf, t0);
//
//        pv = new PVCoordinates(testPosJ2000 , testVelJ2000);
//        result = trans.transformPVCoordinates(pv);
//
//        System.out.println( " IRF ");
//        System.out.println(" pos cals "+ result.getPosition().getX() + " " + result.getPosition().getY() + " " + result.getPosition().getZ());
//        System.out.println(" pos test "+ testPosIRF.getX() + " " + testPosIRF.getY() + " " + testPosIRF.getZ());
//        Vector3D posError = testPosIRF.subtract(result.getPosition());
//        System.out.println(" dif "+ posError.getX() + " " + posError.getY() + " " + posError.getZ());
//
//        System.out.println(" vel cals "+ result.getVelocity().getX() + " " + result.getVelocity().getY() + " " + result.getVelocity().getZ());
//        System.out.println(" vel test "+ testVelIRF.getX() + " " + testVelIRF.getY() + " " + testVelIRF.getZ());
//        Vector3D velError = testVelIRF.subtract(result.getVelocity());
//        System.out.println(" dif "+ velError.getX() + " " + velError.getY() + " " + velError.getZ());
//
//        System.out.println();
//
////      pv = new PVCoordinates(testPosJ2000 , testVelJ2000);
//        trans = j2000.getTransformTo(tirf, t0);
//
//        result = trans.transformPVCoordinates(pv);
//
//        System.out.println( " TIRF ");
//        System.out.println(" pos cals "+ result.getPosition().getX() + " " + result.getPosition().getY() + " " + result.getPosition().getZ());
//        System.out.println(" pos test "+ testPosTIRF.getX() + " " + testPosTIRF.getY() + " " + testPosTIRF.getZ());
//        posError = testPosTIRF.subtract(result.getPosition());
//        System.out.println(" dif "+ posError.getX() + " " +posError.getY() + " " + posError.getZ());
//
//        System.out.println(" vel cals "+ result.getVelocity() .getX() + " " + result.getVelocity() .getY() + " " + result.getVelocity() .getZ());
//        System.out.println(" vel test "+ testVelTIRF.getX() + " " + testVelTIRF.getY() + " " + testVelTIRF.getZ());
//        velError =  testVelTIRF.subtract(result.getVelocity());
//        System.out.println(" dif "+ velError.getX() + " " + velError.getY() + " " + velError.getZ());
//
//        System.out.println();
//
//
//
////      pv = new PVCoordinates(testPosJ2000,testVelJ2000);
//        trans = j2000.getTransformTo(itrf,t0);
//        result = trans.transformPVCoordinates(pv);
//
//        System.out.println( " ITRF ");
//        System.out.println(" pos cals "+ result.getPosition().getX() + " " + result.getPosition().getY() + " " + result.getPosition().getZ());
//        System.out.println(" pos test "+ testPosITRF.getX() + " " + testPosITRF.getY() + " " + testPosITRF.getZ());
//        posError = testPosITRF.subtract(result.getPosition());
//        System.out.println(" dif "+ posError.getX() + " " + posError.getY() + " " + posError.getZ());
//
//        System.out.println(" vel cals "+ result.getVelocity() .getX() + " " + result.getVelocity() .getY() + " " + result.getVelocity() .getZ());
//        System.out.println(" vel test "+ testVelITRF.getX() + " " + testVelITRF.getY() + " " + testVelITRF.getZ());
//        velError = testVelITRF.subtract(result.getVelocity());
//        System.out.println(" dif "+ velError.getX() + " " +velError.getY() + " " + velError.getZ());
//
//        System.out.println();
//
//
//
//    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    /** Compare and asserts two vectors.
     * @param vRef reference vector
     * @param vResult vector to test
     * @param deltaAngle tolerance on angle
     * @param deltaRel tolerance on relative position
     * @param deltaAbs tolerance on absolute position
     */
    private void checkVectors(Vector3D vRef , Vector3D vResult,
                              double deltaAngle,
                              double deltaRel, double deltaAbs) {
        assertEquals(0, Vector3D.angle(vRef, vResult), deltaAngle);
        Vector3D d = vRef.subtract(vResult);
        assertEquals(0, d.getNorm() / vRef.getNorm(), deltaRel);
        assertEquals(0, d.getNorm(), deltaAbs);
    }

    public static Test suite() {
        return new TestSuite(ITRF2000FrameTest.class);
    }

}
