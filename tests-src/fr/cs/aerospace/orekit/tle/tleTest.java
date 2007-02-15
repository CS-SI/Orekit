package fr.cs.aerospace.orekit.tle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class tleTest extends TestCase {

  public void testTLEFormat() throws OrekitException, ParseException {

    String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

    assertTrue(TLE.isFormatOK(line1, line2));

    TLE tle = new TLE(line1, line2);
    assertEquals(tle.getSatelliteNumber(), 27421, 0);
    assertTrue(tle.getInternationalDesignator().equals("02021A  "));
    assertEquals(tle.getBStar(), -0.0089879, 0);
    assertEquals(tle.getEphemerisType(), 0, 0);
    assertEquals(Math.toDegrees(tle.getI()), 98.749, 1e-10);
    assertEquals(Math.toDegrees(tle.getRaan()), 199.5121, 1e-10);
    assertEquals(tle.getE(), 0.0001333, 0);
    assertEquals(Math.toDegrees(tle.getPerigeeArgument()), 133.9522, 1e-10);
    assertEquals(Math.toDegrees(tle.getMeanAnomaly()), 226.1918, 1e-10);
    assertEquals(tle.getMeanMotion()*86400/(2*Math.PI), 14.26113993, 0);
    assertEquals(tle.getRevolutionNumberAtEpoch(), 6, 0);    
    assertEquals(tle.getElementNumber(), 2 ,0);  

    line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14*26113993    62";    
    assertFalse(TLE.isFormatOK(line1, line2));     

    line1 = "1 27421 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
    assertFalse(TLE.isFormatOK(line1, line2)); 

    line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 10006113993    62";
    assertFalse(TLE.isFormatOK(line1, line2)); 

    line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879 2 0    20";
    line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";
    assertFalse(TLE.isFormatOK(line1, line2)); 
  }

  public void testTLESeriesFormat() throws IOException, OrekitException, ParseException {

    File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data/tle/regular-data/" +
                                 "spot-5.txt", "/");
    InputStream in = new FileInputStream(rootDir.getAbsolutePath());    
    TLESeries series = new TLESeries(in);
    assertEquals(0, series.getFirstDate().minus(
                                                new AbsoluteDate("2002-05-04T11:45:15.695", UTCScale.getInstance())), 1e-3);
    assertEquals(0, series.getLastDate().minus(
                                               new AbsoluteDate("2002-06-24T18:12:44.592", UTCScale.getInstance())), 1e-3);

    AbsoluteDate mid = new AbsoluteDate("2002-06-02T11:12:15", UTCScale.getInstance());
    assertEquals(0, series.getClosestTLE(mid).getEpoch().minus(
                                                               new AbsoluteDate("2002-06-02T10:08:25.401", UTCScale.getInstance())), 1e-3);
    mid = new AbsoluteDate("2001-06-02T11:12:15", UTCScale.getInstance());                                 
    assertTrue(series.getClosestTLE(mid).getEpoch().equals(series.getFirstDate()));
    mid = new AbsoluteDate("2003-06-02T11:12:15", UTCScale.getInstance());                                 
    assertTrue(series.getClosestTLE(mid).getEpoch().equals(series.getLastDate()));

  }

  public void testThetaG() throws OrekitException, ParseException {

//  AbsoluteDate date = AbsoluteDate.J2000Epoch;

//  double teta = SDP4.thetaG(date); 

//  TIRF2000Frame ITRF = (TIRF2000Frame)Frame.getReferenceFrame(Frame.tirf2000B, date);
//  double tetaTIRF = ITRF.getEarthRotationAngle(date);    
//  assertEquals( Utils.trimAngle(tetaTIRF, Math.PI), Utils.trimAngle(teta, Math.PI), 0.003);

//  date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 78.2*86400);

//  teta = SDP4.thetaG(date); 
//  tetaTIRF = ITRF.getEarthRotationAngle(date);

//  assertEquals( Utils.trimAngle(tetaTIRF, Math.PI), Utils.trimAngle(teta, Math.PI), 0.003);
  }

  public void testSatCodeCompliance() throws IOException, OrekitException {

    File rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                                 "/tle/extrapolationTest-data/SatCode-entry", "/");
    InputStream inEntry = new FileInputStream(rootDir.getAbsolutePath());
    BufferedReader rEntry = new BufferedReader(new InputStreamReader(inEntry));

    rootDir = FindFile.find("/tests-src/fr/cs/aerospace/orekit/data" +
                            "/tle/extrapolationTest-data/SatCode-results", "/");
    InputStream inResults = new FileInputStream(rootDir.getAbsolutePath());
    BufferedReader rResults = new BufferedReader(new InputStreamReader(inResults));

    double cumulated = 0; // sum of all differences between test cases and OREKIT results
    boolean stop = false;

    String rline = rResults.readLine();

    while( stop==false ) {
      if (rline == null) break;

      String[] title = rline.split(" ");

      if(title[0].matches("r")) {        

        String eline;
        int count = 0;
        String[] header = new String[4];
        for (eline = rEntry.readLine(); eline.charAt(0)=='#'; eline = rEntry.readLine()) {      
          header[count++] = eline;
        }
        String line1 = eline;
        String line2 = rEntry.readLine();
        assertTrue(TLE.isFormatOK(line1, line2));

        TLE tle = new TLE(line1, line2);

        int satNum = Integer.parseInt(title[1]);
        assertTrue(satNum==tle.getSatelliteNumber());
        TLEPropagator ex = TLEPropagator.selectExtrapolator(tle);

//        System.out.println();
//        for(int i = 0; i<4; i++) {
//          if(header[i]!=null) {
//            System.out.println(header[i]);
//          }
//        }
//        System.out.println(" Satellite number : " + satNum);


        for (rline = rResults.readLine(); (rline!=null)&&(rline.charAt(0)!='r'); rline = rResults.readLine()) {

          String[] data = rline.split(" ");
          double minFromStart = Double.parseDouble(data[0]);
          double pX = 1000*Double.parseDouble(data[1]);
          double pY = 1000*Double.parseDouble(data[2]);
          double pZ = 1000*Double.parseDouble(data[3]);
          double vX = 1000*Double.parseDouble(data[4]);
          double vY = 1000*Double.parseDouble(data[5]);
          double vZ = 1000*Double.parseDouble(data[6]);
          Vector3D testPos = new Vector3D(pX, pY, pZ);
          Vector3D testVel = new Vector3D(vX, vY, vZ);

          AbsoluteDate date = new AbsoluteDate(tle.getEpoch(), minFromStart*60);
          PVCoordinates results = null;
          try {
            results = ex.getPVCoordinates(date);
          }
          catch(OrekitException e)  {
            if(satNum==28872  || satNum==23333 || satNum==29141 ) {
              // expected behaviour
            }
            else {
              fail("exception not expected"+e.getMessage());
            }
          }
          if (results != null) {
            double normDifPos = testPos.subtract(results.getPosition()).getNorm();
            double normDifVel = testVel.subtract(results.getVelocity()).getNorm();

            cumulated += normDifPos;  
//            if(normDifPos>1) {
//              System.out.println(minFromStart + "    " + normDifPos);
//            }              
            checkVectors(testPos, results.getPosition(),6e-6,1.4e-5,280);
            assertEquals( 0, normDifVel, 0.1);
          }  

        }        
      }
    }

//    System.out.println();
//    System.out.println(" cumul :  " + cumulated);

    assertEquals(0, cumulated, 3750);
  }
  
  /** Compare and asserts two vectors.
   * @param pos1 reference vector
   * @param pos2 to test vector
   * @param deltaNorm relative delta between the two norms
   * @param deltaAngle the delta angle
   * @param difNorm the norm of the difference
   */
  private void checkVectors(Vector3D pos1 , Vector3D pos2,
                            double deltaNorm,
                            double deltaAngle, double difNorm) {
      
      Vector3D d = pos1.subtract(pos2);
      Rotation r = new Rotation(pos1, pos2);

      assertEquals(0, (pos1.getNorm()-pos2.getNorm())/pos1.getNorm(), deltaNorm); 
      assertEquals(0, d.getNorm(), difNorm);      
      assertEquals(0,r.getAngle(),deltaAngle);
    
  }

  public static Test suite() {
    return new TestSuite(tleTest.class);
  }
}