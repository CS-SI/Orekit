package fr.cs.aerospace.orekit.perturbations;

import java.text.ParseException;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.models.perturbations.JB2006Atmosphere;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class JB2006AtmosphereTest extends TestCase {

  public void testWithOriginalTestsCases() throws OrekitException, ParseException {

    JB2006Atmosphere atm = new JB2006Atmosphere();
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
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);
    
    // alt = 90
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*90,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase = 0.3285e-05;
    tzTestCase=183.0;
    tinfTestCase=1142.8;
    assertEquals(roTestCase*1e05, Math.round(myRo*1e09)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);

    // alt = 110
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*110,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase = 0.7587e-07;
    tzTestCase=257.4;
    tinfTestCase=1142.8;
    assertEquals(roTestCase*1e07, Math.round(myRo*1e11)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);

    // alt = 180
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*180,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase = 0.5439; // *1e-9
    tzTestCase=915.0;
    tinfTestCase=1130.9; 
    assertEquals(roTestCase, Math.round(myRo*1e13)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);
   
    // alt = 230
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*230,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase =0.1250e-09; 
    tzTestCase=1047.5;
    tinfTestCase=1137.4;
    assertEquals(roTestCase*1e09, Math.round(myRo*1e13)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);
    
    // alt = 270
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*270,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase =0.4818e-10; 
    tzTestCase=1095.6;
    tinfTestCase=1142.5;
    assertEquals(roTestCase*1e10, Math.round(myRo*1e14)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);
    
    // alt = 660
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*660,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase =0.9451e-13; 
    tzTestCase=1149.0;
    tinfTestCase=1149.9 ;
    assertEquals(roTestCase*1e13, Math.round(myRo*1e17)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);
      
    //  alt = 890
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*890,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase =0.8305e-14; 
    tzTestCase=1142.5;
    tinfTestCase=1142.8 ;
    assertEquals(roTestCase*1e14, Math.round(myRo*1e18)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);

    //  alt = 1320
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*1320,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase =0.2004e-14; 
    tzTestCase=1142.7;
    tinfTestCase=1142.8 ;
    assertEquals(roTestCase*1e14, Math.round(myRo*1e18)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);

    //  alt = 1600
    myRo = atm.getDensity(AMJD, 90.*PI/180., 20.*PI/180.,90.*PI/180.,45.*PI/180.,1000*1600,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase = 0.1159e-14; 
    tzTestCase=1142.8;
    tinfTestCase=1142.8 ;
    assertEquals(roTestCase*1e14, Math.round(myRo*1e18)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);
    
    
    //  OTHER entries
    AMJD +=50;    
    myRo = atm.getDensity(AMJD, 45.*PI/180., 10.*PI/180.,45.*PI/180.,-10.*PI/180.,400*1000,F10, F10B, AP,S10,S10B,XM10,XM10B);
    roTestCase = 0.4838e-11; 
    tzTestCase=1137.4 ;
    tinfTestCase= 1145.4 ;
    assertEquals(roTestCase*1e11, Math.round(myRo*1e15)/1e4,0);
    assertEquals(tzTestCase, Math.round(atm.TEMP[2]*10)/10.0,0);
    assertEquals(tinfTestCase, Math.round(atm.TEMP[1]*10)/10.0,0);

  }

  public static Test suite() {
    return new TestSuite(JB2006AtmosphereTest.class);
  }
}
