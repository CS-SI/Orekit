package fr.cs.aerospace.orekit.perturbations;

import java.text.ParseException;

import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.models.perturbations.DTM2000Atmosphere;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class DTM2000AtmosphereTest extends TestCase {

  public void testOutputValidity() throws OrekitException, ParseException {

    DTM2000Atmosphere atm = new DTM2000Atmosphere();
    
//  alt=500.                  
//  lat=-70.      NB: the subroutine requires latitude in rad
//  day=15.
//  hl=16.        NB: the subroutine requires local time in rad (0hr=0 rad) 
//  xlon=0.       
//  fm(1)=70.
//  f(1) =fm(1)
//  fm(2)=0.
//  f(2)=0.   
//  akp(1)=0.
//  akp(2)=0.
//  akp(3)=0.
//  akp(4)=0.     
    double ro =    1.3150282384722E-16;
    double tz=    793.65487014559;
    double tinf=    793.65549802348;
    double myRo = atm.getDensity(15, 500*1000, 0, Math.toRadians(-70), 16*Math.PI/12, 70, 70, 0, 0);
    System.out.println();
    System.out.println(" ro : " + myRo);
    System.out.println(" tz : " + atm.getT());
    System.out.println(" tinf : " + atm.getTinf());
    
//  IDEM., alt=800.
//  ro=    1.9556768571305D-18
//  tz=    793.65549797919
//  tinf=    793.65549802348
    myRo = atm.getDensity(15, 800*1000, 0, Math.toRadians(-70), 16*Math.PI/12, 70, 70, 0, 0);
    System.out.println();
    System.out.println(" ro : " + myRo);
    System.out.println(" tz : " + atm.getT());
    System.out.println(" tinf : " + atm.getTinf());

    
    
//  alt=800.  
//  lat=40.
//  day=185.
//  hl=16.
//  xlon=0.
//  fm(1)=150.
//  f(1) =fm(1)
//  fm(2)=0.
//  f(2)=0.   
//  akp(1)=0.
//  akp(2)=0.
//  akp(3)=0.
//  akp(4)=0.
    ro = 1.8710001353820e-17 * 1000;
    tz = 1165.4839828984;
    tinf = 1165.4919505608;
    myRo = atm.getDensity(185, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
    assertEquals(0, (ro -myRo)/ro, 1e-14);
    assertEquals(0, (tz-atm.getT())/tz, 1e-13);
    assertEquals(0, (tinf-atm.getTinf())/tinf, 1e-13);
//  IDEM., day=275
    ro=    2.8524195214905e-17* 1000;
    tz=    1157.1872001392;
    tinf=    1157.1933514185;
    myRo = atm.getDensity(275, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
    assertEquals(0, (ro -myRo)/ro, 1e-14);
    assertEquals(0, (tz-atm.getT())/tz, 1e-13);
    assertEquals(0, (tinf-atm.getTinf())/tinf, 1e-13);
//  IDEM., day=355
    ro=    1.7343324462212e-17* 1000;
    tz=    1033.0277846356;
    tinf=    1033.0282703200;
    myRo = atm.getDensity(355, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
    assertEquals(0, (ro -myRo)/ro, 2e-14);
    assertEquals(0, (tz-atm.getT())/tz, 1e-13);
    assertEquals(0, (tinf-atm.getTinf())/tinf, 1e-13);
//  IDEM., day=85
    ro=    2.9983740796297e-17* 1000;
    tz=    1169.5405086196;
    tinf=    1169.5485768345;
    myRo = atm.getDensity(85, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
    assertEquals(0, (ro -myRo)/ro, 1e-14);
    assertEquals(0, (tz-atm.getT())/tz, 1e-13);
    assertEquals(0, (tinf-atm.getTinf())/tinf, 1e-13);

  }

  public static Test suite() {
    return new TestSuite(DTM2000AtmosphereTest.class);
  }
}
