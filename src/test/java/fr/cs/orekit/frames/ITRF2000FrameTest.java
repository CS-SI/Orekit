package fr.cs.orekit.frames;

import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.iers.IERSDataResetter;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;
import fr.cs.orekit.utils.VectorFormatter;

public class ITRF2000FrameTest extends TestCase {

  public void testRoughRotation() throws ParseException, OrekitException {

    AbsoluteDate date1 = new AbsoluteDate(new ChunkedDate(2006, 02, 24),
                                          new ChunkedTime(15, 38, 00),
                                          UTCScale.getInstance());
    Frame itrf2000 = Frame.getReferenceFrame(Frame.ITRF2000B, date1);
    Transform t0 = itrf2000.getTransformTo(Frame.getJ2000(), date1 );

    double dt = 10.0;
    AbsoluteDate date2 = new AbsoluteDate(date1, dt);
    Transform t1 = itrf2000.getTransformTo(Frame.getJ2000(), date2);
    Transform evolution = new Transform(t0.getInverse(), t1);

    assertEquals(0.0, evolution.transformPosition(new Vector3D(0,0,0)).getNorm(), 1.0e-10);
    assertTrue(Vector3D.dotProduct(Vector3D.plusK, evolution.transformVector(new Vector3D(6000,6000,0))) < 0.01);
    assertEquals(2 * Math.PI * dt / 86164, Vector3D.angle(
                                                          t0.transformVector(new Vector3D(6000,6000,0)), t1.transformVector(new Vector3D(6000,6000,0))), 
                                                          1.0e-9);

  }

  public void testRoughOrientation() throws ParseException, OrekitException {

    AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                         ChunkedTime.H00,
                                         UTCScale.getInstance());
    Frame itrf2000 = Frame.getReferenceFrame(Frame.ITRF2000B, date);

    Vector3D u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.minusI) < Math.toRadians(2));

    date = new AbsoluteDate(date, 6 * 3600);
    u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.minusJ) < Math.toRadians(2));

    date = new AbsoluteDate(date, 6 * 3600);
    u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.plusI) < Math.toRadians(2));

    date = new AbsoluteDate(date, 6 * 3600);
    u = itrf2000.getTransformTo(Frame.getJ2000(), date).transformVector(Vector3D.plusI);
    assertTrue(Vector3D.angle(u, Vector3D.plusJ) < Math.toRadians(2));

  }

  public void testRoughERA() throws ParseException, OrekitException {

    AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                         ChunkedTime.H00,
                                         UTCScale.getInstance());
    TIRF2000Frame TIRF2000 = (TIRF2000Frame)Frame.getReferenceFrame(Frame.TIRF2000B, date);

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

    Frame tirf = Frame.getReferenceFrame(Frame.TIRF2000B, date);	

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

    Frame itrf = Frame.getReferenceFrame(Frame.ITRF2000B, t0);	

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
    Frame itrf = Frame.getReferenceFrame(Frame.ITRF2000A, date);   
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
    Frame itrf = Frame.getReferenceFrame(Frame.ITRF2000B, date);   
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
    Frame itrf = Frame.getReferenceFrame(Frame.ITRF2000B, date);   
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

  public void testAASReferenceLEO() throws OrekitException, ParseException {

    IERSDataResetter.setUp("testitrf-data");
    
    AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2004, 04, 06),
                                       new ChunkedTime(07, 51, 28.386),
                                       UTCScale.getInstance());
    t0 = new AbsoluteDate(t0, 0.000009);

    Frame itrfA = Frame.getReferenceFrame(Frame.ITRF2000A, t0);    

    Transform transA = itrfA.getTransformTo(Frame.getJ2000(), t0);

    Frame itrfB = Frame.getReferenceFrame(Frame.ITRF2000B, t0);    

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
//FIXME : ITRF B non satisfaisant.
//    System.out.println( " ITRF LEO ");
//
//    Utils.vectorToString("B pos cals ", resultB.getPosition());
//    Utils.vectorToString("B pos test ", posGCRFiau2000b);
//    Utils.vectorToString("B dif ", posGCRFiau2000b.subtract(resultB.getPosition()));
    
  }

  public void testAASReferenceGEO() throws OrekitException, ParseException {
    
    IERSDataResetter.setUp("testitrf-data");
    
    AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2004, 06, 01),
                                       ChunkedTime.H00,
                                       UTCScale.getInstance());
    
    Frame itrfA = Frame.getReferenceFrame(Frame.ITRF2000A, t0);    

    Transform transA = itrfA.getTransformTo(Frame.getJ2000(), t0);

    Frame itrfB = Frame.getReferenceFrame(Frame.ITRF2000B, t0);    

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
   
//    System.out.println( " ITRF GEO ");
//    Utils.vectorToString("A pos cals ", resultA.getPosition());
//    Utils.vectorToString("A pos test ", posGCRFiau2000a);
//    Utils.vectorToString("A dif ", posGCRFiau2000a.subtract(resultA.getPosition()));
//
//    Utils.vectorToString("B pos cals ", resultB.getPosition());
//    Utils.vectorToString("B pos test ", posGCRFiau2000b);
//    Utils.vectorToString("B dif ", posGCRFiau2000b.subtract(resultB.getPosition()));

//    Utils.vectorToString(" vel cals ", result.getVelocity() );
//    Utils.vectorToString(" vel test ", testVelITRF);
//    Utils.vectorToString(" dif ", testVelITRF.subtract(result.getVelocity()));

  }

 public void aatestValladoReference() throws OrekitException, ParseException {

    AbsoluteDate t0 = new AbsoluteDate(new ChunkedDate(2004, 04, 06),
                                       new ChunkedTime(07, 51, 28.386),
                                       UTCScale.getInstance());
    t0 = new AbsoluteDate(t0, 0.000009);

    Frame j2000 = Frame.getJ2000();
    Frame irf = Frame.getReferenceFrame(Frame.IRF2000A, t0);    
    Frame tirf = Frame.getReferenceFrame(Frame.TIRF2000A, t0);    
    Frame itrf = Frame.getReferenceFrame(Frame.ITRF2000A, t0);    

    Transform trans;
    PVCoordinates pv;
    PVCoordinates result;

    // test cases

    Vector3D testPosJ2000 = new Vector3D(5102.508958*1000,
                                         6123.011401*1000,
                                         6378.136928*1000);

    Vector3D testVelJ2000 = new Vector3D(-4.74322016*1000,
                                         0.79053650*1000,
                                         5.533756573*1000);

    Vector3D testPosIRF = new Vector3D(5100.0076393*1000,
                                       6122.7764115*1000,
                                       6380.343827*1000);

    Vector3D testVelIRF = new Vector3D(-4.745388938*1000,
                                       0.790332038*1000,
                                       5.531929087*1000);

    Vector3D testPosTIRF = new Vector3D(-1033.4750313*1000,
                                        7901.2909240*1000,
                                        6380.3438271*1000);

    Vector3D testVelTIRF = new Vector3D(-3.225632747*1000,
                                        -2.872455223*1000,
                                        5.531929087*1000);

    Vector3D testPosITRF = new Vector3D(-1033.4793830*1000,
                                        7901.2952758*1000,
                                        6380.3565953*1000);

    Vector3D testVelITRF = new Vector3D(-3.225636520*1000,
                                        -2.872451450*1000,
                                        5.531924446*1000);

    // tests

    trans = j2000.getTransformTo(irf, t0);

    pv = new PVCoordinates(testPosJ2000 , testVelJ2000);
    result = trans.transformPVCoordinates(pv);

    System.out.println( " IRF ");
    System.out.println(" pos cals "+ VectorFormatter.toString(result.getPosition()));
    System.out.println(" pos test "+ VectorFormatter.toString(testPosIRF));
    System.out.println(" dif "+ VectorFormatter.toString(testPosIRF.subtract(result.getPosition())));

    System.out.println(" vel cals "+ VectorFormatter.toString(result.getVelocity()));
    System.out.println(" vel test "+ VectorFormatter.toString(testVelIRF));
    System.out.println(" dif "+ VectorFormatter.toString(testVelIRF.subtract(result.getVelocity())));

    System.out.println();

//  pv = new PVCoordinates(testPosJ2000 , testVelJ2000);
    trans = j2000.getTransformTo(tirf, t0);

    result = trans.transformPVCoordinates(pv);

    System.out.println( " TIRF ");
    System.out.println(" pos cals "+ VectorFormatter.toString(result.getPosition()));
    System.out.println(" pos test "+ VectorFormatter.toString(testPosTIRF));
    System.out.println(" dif "+ VectorFormatter.toString(testPosTIRF.subtract(result.getPosition())));

    System.out.println(" vel cals "+ VectorFormatter.toString(result.getVelocity() ));
    System.out.println(" vel test "+ VectorFormatter.toString(testVelTIRF));
    System.out.println(" dif "+ VectorFormatter.toString(testVelTIRF.subtract(result.getVelocity())));

    System.out.println();

    
    
//  pv = new PVCoordinates(testPosJ2000,testVelJ2000);
    trans = j2000.getTransformTo(itrf,t0);  
    result = trans.transformPVCoordinates(pv);

    System.out.println( " ITRF ");
    System.out.println(" pos cals "+ VectorFormatter.toString(result.getPosition()));
    System.out.println(" pos test "+ VectorFormatter.toString(testPosITRF));
    System.out.println(" dif "+ VectorFormatter.toString(testPosITRF.subtract(result.getPosition())));

    System.out.println(" vel cals "+ VectorFormatter.toString(result.getVelocity() ));
    System.out.println(" vel test "+ VectorFormatter.toString(testVelITRF));
    System.out.println(" dif "+ VectorFormatter.toString(testVelITRF.subtract(result.getVelocity())));

    System.out.println();



  }

  public void setUp() {
    IERSDataResetter.setUp("regular-data");
  }

  public void tearDown() {
    IERSDataResetter.tearDown();
  }

 
  /** Compare and asserts two vectors.
   * @param vRef reference vector
   * @param vResult vector to test
   * @param deltaAngle the delta angle
   * @param deltaRel the relative delta in position
   */
  private void checkVectors(Vector3D vRef , Vector3D vResult,
                            double deltaAngle, double deltaRel, double delta) {

    Vector3D d = vRef.subtract(vResult);
    Rotation r = new Rotation(vRef, vResult);
    assertEquals(0,r.getAngle(),deltaAngle);

    assertEquals(0, d.getNorm()/vRef.getNorm() , deltaRel);
    assertEquals(0, d.getNorm() , delta);



  }

  public static Test suite() {
    return new TestSuite(ITRF2000FrameTest.class);
  }
  
//  private class LagrangeFitter {
//    
//    private double[] x;
//    private double[] y;
//    private int n;
//    
//    LagrangeFitter(double[] x, double[] y) {
//      this.x= x;
//      this.y = y;
//      n = x.length -1;
//    }
//    
//    public double getValue(double x) {
//      double p = 0;
//      
//      for (int j = 0; j<= n ; j++) {
//        double l = 1;
//        for(int k=0; k<=n; k++ ) {
//          if(k!=j) {
//            l *= (x - this.x[k]) /( this.x[j] - this.x[k]);
//          }
//        }
//        p += y[j]*l;
//      }
//      return p;
//    }
//    
//  }
  
  //
//double xp = -6.798284606394803e-7;
//double yp = 1.6252035846549786E-6;
//System.out.println(xp * 1296000 /(2 * Math.PI) );
//System.out.println(yp * 1296000 /(2 * Math.PI) );
//
//AbsoluteDate zero = new AbsoluteDate(new ChunkedDate(2004, 04, 05),
//                                       ChunkedTime.H00,
//                                       UTCScale.getInstance());
//double[] t = new double[] {0, 86400, 2*86400, 3*86400 };
//double[] x = new double[] {-0.141167,-0.140639 , -0.140023, -0.139768 };
//double[] y = new double[] {0.330933, 0.333257,0.336179,0.339275};
//
//LagrangeFitter lx = new LagrangeFitter(t, x);
//LagrangeFitter ly = new LagrangeFitter(t, y);
//
//
//System.out.println(" t0 : " + t0.minus(zero));
//
//System.out.println(" lx : " +lx.getValue(t0.minus(zero)));
//System.out.println(" ly : " +ly.getValue(t0.minus(zero)));

}
