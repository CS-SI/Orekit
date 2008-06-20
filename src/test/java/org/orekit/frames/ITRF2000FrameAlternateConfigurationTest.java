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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class ITRF2000FrameAlternateConfigurationTest extends TestCase {

    public void testAASReferenceLEO() throws OrekitException, ParseException {

        AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2004, 04, 06),
                                           new ChunkedTime(07, 51, 28.386),
                                           UTCScale.getInstance());
        t0 = new AbsoluteDate(t0, 0.000009);

        Frame itrfA = Frame.getITRF2000A();

        Transform transA = itrfA.getTransformTo(Frame.getJ2000(), t0);

        Frame itrfB = Frame.getITRF2000B();

        Transform transB = itrfB.getTransformTo(Frame.getJ2000(), t0);

        // Positions LEO

        Vector3D posITRF = new Vector3D(-1033.4793830*1000,
                                        7901.2952754*1000,
                                        6380.3565958 *1000);
        Vector3D velITRF = new Vector3D(-3.225636520*1000,
                                        -2.872451450*1000,
                                        5.53192446*1000);
        PVCoordinates pvITRF = new PVCoordinates(posITRF , velITRF);

        Vector3D posGCRFiau2000a = new Vector3D(5102.5089579*1000, 6123.0114038*1000, 6378.1369252*1000);
        Vector3D velGCRFiau2000a = new Vector3D(-4.743220156*1000, 0.790536497*1000, 5.533755728*1000);
        Vector3D posGCRFiau2000b = new Vector3D(5102.5089579*1000, 6123.0114012*1000, 6378.1369277*1000);
        Vector3D velGCRFiau2000b = new Vector3D(-4.743220156*1000, 0.790536495*1000, 5.533755729*1000);

        // TESTS

        PVCoordinates resultA = transA.transformPVCoordinates(pvITRF);
        checkVectors(posGCRFiau2000a,resultA.getPosition(), 8.31e-10, 8.31e-10, 0.009);
        checkVectors(velGCRFiau2000a,resultA.getVelocity(), 1.6e-9,  2.8e-9, 2.04e-5);
        PVCoordinates resultB = transB.transformPVCoordinates(pvITRF);
        checkVectors(posGCRFiau2000b,resultB.getPosition(), 4.1e-8, 4.01e-8, 0.41);
        checkVectors(velGCRFiau2000b,resultB.getVelocity(),3.6e-8, 3.6e-8, 2.6e-4);
//      FIXME : ITRF B non satisfaisant.
//      System.out.println( " ITRF LEO ");

//      Utils.vectorToString("B pos cals ", resultB.getPosition());
//      Utils.vectorToString("B pos test ", posGCRFiau2000b);
//      Utils.vectorToString("B dif ", posGCRFiau2000b.subtract(resultB.getPosition()));

    }

    public void testAASReferenceGEO() throws OrekitException, ParseException {

        AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2004, 06, 01),
                                           ChunkedTime.H00,
                                           UTCScale.getInstance());

        Frame itrfA = Frame.getITRF2000A();

        Transform transA = itrfA.getTransformTo(Frame.getJ2000(), t0);

        Frame itrfB = Frame.getITRF2000B();

        Transform transB = itrfB.getTransformTo(Frame.getJ2000(), t0);

        //  Positions GEO

        Vector3D posITRF = new Vector3D(24796.9192915*1000,
                                        -34115.8709234*1000,
                                        10.2260621*1000);
        Vector3D velITRF = new Vector3D(-0.000979178*1000,
                                        -0.001476538*1000,
                                        -0.000928776*1000);
        PVCoordinates pvITRF = new PVCoordinates(posITRF , velITRF);

        Vector3D posGCRFiau2000a = new Vector3D(-40588.1503617*1000, -11462.1670397*1000, 27.1431974*1000);
        Vector3D velGCRFiau2000a = new Vector3D(0.834787458*1000, -2.958305691*1000, -0.001172993*1000);
        Vector3D posGCRFiau2000b = new Vector3D(-40588.1503617*1000,-11462.1670397*1000, 27.1432125*1000);
        Vector3D velGCRFiau2000b = new Vector3D(0.834787458*1000,-2.958305691*1000,-0.001172999*1000);

        // TESTS

        PVCoordinates resultA = transA.transformPVCoordinates(pvITRF);
        checkVectors(posGCRFiau2000a,resultA.getPosition(), 7.76e-9,  7.76e-9, 0.33);
        checkVectors(velGCRFiau2000a,resultA.getVelocity(), 7.76e-9,  7.77e-9, 2.4e-5);
        PVCoordinates resultB = transB.transformPVCoordinates(pvITRF);
        checkVectors(posGCRFiau2000b,resultB.getPosition(),3.81e-8, 3.81e-8, 1.61);
        checkVectors(velGCRFiau2000b,resultB.getVelocity(), 1.7e-8,1.7e-8, 5.11e-5);

    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "testitrf-data");
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
        return new TestSuite(ITRF2000FrameAlternateConfigurationTest.class);
    }

}
