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
package org.orekit.forces.drag;

import java.io.FileNotFoundException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.SolarInputs97to05;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.Sun;
import org.orekit.forces.drag.DTM2000;
import org.orekit.forces.drag.JB2006;
import org.orekit.frames.Frame;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;

public class JB2006Test extends TestCase {

    public void testWithOriginalTestsCases() throws OrekitException, ParseException {

        Frame itrf = Frame.getITRF2000B();
        Sun sun = new Sun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);

        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        JB2006 atm = new JB2006(in, sun, earth, itrf);
        double myRo;
        double PI = 3.1415927;

        // SET SOLAR INDICES USE 1 DAY LAG FOR EUV AND F10 INFLUENCE
        double S10  = 140;
        double S10B = 100;
        double F10  = 135;
        double F10B = 95;
        // USE 5 DAY LAG FOR MG FUV INFLUENCE
        double XM10  = 130;
        double XM10B = 95;

        // USE 6.7 HR LAG FOR ap INFLUENCE
        double AP = 30;
        // SET TIME OF INTEREST
        double IYR  = 01;
        double IDAY = 200;
        if (IYR<50) IYR = IYR + 100;
        double IYY  = (IYR-50)*365 + ((IYR-1)/4-12);
        double ID1950 = IYY + IDAY;
        double D1950  = ID1950;
        double AMJD   = D1950 + 33281;

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*400,F10, F10B, AP,S10,S10B,XM10,XM10B);
        double roTestCase = 0.4066e-11;
        double tzTestCase=1137.7;
        double tinfTestCase=1145.8;
        assertEquals(roTestCase*1e12, Math.round(myRo*1e15)/1e3,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        // alt = 90
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*90,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase = 0.3285e-05;
        tzTestCase=183.0;
        tinfTestCase=1142.8;
        assertEquals(roTestCase*1e05, Math.round(myRo*1e09)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        // alt = 110
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*110,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase = 0.7587e-07;
        tzTestCase=257.4;
        tinfTestCase=1142.8;
        assertEquals(roTestCase*1e07, Math.round(myRo*1e11)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        // alt = 180
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*180,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase = 0.5439; // *1e-9
        tzTestCase=915.0;
        tinfTestCase=1130.9;
        assertEquals(roTestCase, Math.round(myRo*1e13)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        // alt = 230
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*230,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase =0.1250e-09;
        tzTestCase=1047.5;
        tinfTestCase=1137.4;
        assertEquals(roTestCase*1e09, Math.round(myRo*1e13)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        // alt = 270
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*270,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase =0.4818e-10;
        tzTestCase=1095.6;
        tinfTestCase=1142.5;
        assertEquals(roTestCase*1e10, Math.round(myRo*1e14)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        // alt = 660
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*660,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase =0.9451e-13;
        tzTestCase=1149.0;
        tinfTestCase=1149.9 ;
        assertEquals(roTestCase*1e13, Math.round(myRo*1e17)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        //  alt = 890
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*890,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase =0.8305e-14;
        tzTestCase=1142.5;
        tinfTestCase=1142.8 ;
        assertEquals(roTestCase*1e14, Math.round(myRo*1e18)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        //  alt = 1320
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*1320,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase =0.2004e-14;
        tzTestCase=1142.7;
        tinfTestCase=1142.8 ;
        assertEquals(roTestCase*1e14, Math.round(myRo*1e18)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

        //  alt = 1600
        myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*1600,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase = 0.1159e-14;
        tzTestCase=1142.8;
        tinfTestCase=1142.8 ;
        assertEquals(roTestCase*1e14, Math.round(myRo*1e18)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);


        //  OTHER entries
        AMJD +=50;
        myRo = atm.getDensity(AMJD, 45.*PI/180., 10.*PI/180.,45.*PI/180.,-10.*PI/180.,400*1000,F10, F10B, AP,S10,S10B,XM10,XM10B);
        roTestCase = 0.4838e-11;
        tzTestCase=1137.4 ;
        tinfTestCase= 1145.4 ;
        assertEquals(roTestCase*1e11, Math.round(myRo*1e15)/1e4,0);
        assertEquals(tzTestCase, Math.round(atm.getLocalTemp()*10)/10.0,0);
        assertEquals(tinfTestCase, Math.round(atm.getExosphericTemp()*10)/10.0,0);

    }

    public void testComparisonWithDTM2000() throws OrekitException, ParseException, FileNotFoundException {

        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2003, 01, 01),
                                             ChunkedTime.H00,
                                             UTCScale.getInstance());
        Frame itrf = Frame.getITRF2000B();
        Sun sun = new Sun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);

        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        JB2006 jb = new JB2006(in, sun, earth, itrf);
        DTM2000 dtm = new DTM2000(in, sun, earth, itrf);
        // Positions

        Vector3D pos = new Vector3D(6500000.0,
                                    -1234567.0,
                                    4000000.0);

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        double roJb = jb.getDensity(date, pos, Frame.getJ2000());
        double roDtm = dtm.getDensity(date, pos, Frame.getJ2000());

        pos = new Vector3D(3011109.360780633,
                           -5889822.669411588,
                           4002170.0385907636);

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        roJb = jb.getDensity(date, pos, Frame.getJ2000());
        roDtm = dtm.getDensity(date, pos, Frame.getJ2000());

        pos =new Vector3D(-1033.4793830*1000,
                          7901.2952754*1000,
                          6380.3565958 *1000);

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        roJb = jb.getDensity(date, pos, Frame.getJ2000());
        roDtm = dtm.getDensity(date, pos, Frame.getJ2000());

        GeodeticPoint point;
        for (int i = 0; i<367; i++) {
            date = new AbsoluteDate(date, 86400);
            point = new GeodeticPoint(0, Math.toRadians(40), 300*1000);
            pos = earth.transform(point);
            roJb = jb.getDensity(date, pos, Frame.getJ2000());
            roDtm = dtm.getDensity(date, pos, Frame.getJ2000());
            double dif = Math.abs((roJb - roDtm) / roJb );
            if (dif>=1) {
                System.out.println(date);
            }

        }

    }

    public void testSolarInputs() throws OrekitException, ParseException {

        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2001, 01, 14),
                                             ChunkedTime.H00,
                                             UTCScale.getInstance());

        SolarInputs97to05 in = SolarInputs97to05.getInstance();

//      2001  14   2451924.0 176.3 164.4 180.0 180.4 163.4 169.2
//      14 176 164   9  12   9   6   4   4   9   7
        assertEquals(176.3, in.getF10(date),0);
        assertEquals(164.4, in.getF10B(date),0);
        assertEquals(180.0, in.getS10(date),0);
        assertEquals(180.4, in.getS10B(date),0);
        assertEquals(163.4, in.getXM10(date),0);
        assertEquals(169.2, in.getXM10B(date),0);
        assertEquals(9 , in.getAp(date),0);


        date = new AbsoluteDate(date, 11*3600);
        assertEquals(6 , in.getAp(date),0);

        date = new AbsoluteDate(new ChunkedDate(1998, 02, 02),
                                new ChunkedTime(18, 00, 00),
                                UTCScale.getInstance());
//      1998  33   2450847.0  89.1  95.1  95.8  97.9  81.3  92.0  1
//      33  89  95   4   5   4   4   2   0   0   3                          98
        assertEquals(89.1, in.getF10(date),0);
        assertEquals(95.1, in.getF10B(date),0);
        assertEquals(95.8, in.getS10(date),0);
        assertEquals(97.9, in.getS10B(date),0);
        assertEquals(81.3, in.getXM10(date),0);
        assertEquals(92.0, in.getXM10B(date),0);
        assertEquals(0 , in.getAp(date),0);
        date = new AbsoluteDate(date, 6*3600-1);
        assertEquals(3 , in.getAp(date),0);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(JB2006Test.class);
    }
}
