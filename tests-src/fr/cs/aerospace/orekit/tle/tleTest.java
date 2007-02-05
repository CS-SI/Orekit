package fr.cs.aerospace.orekit.tle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.TIRF2000Frame;
import fr.cs.aerospace.orekit.orbits.CartesianParameters;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import junit.framework.TestCase;

public class tleTest extends TestCase {

  public void testTLEFormat() throws OrekitException, ParseException {

    String line1 = "1 27421U 02021A   02124.48976499 -.00021470  00000-0 -89879-2 0    20";
    String line2 = "2 27421  98.7490 199.5121 0001333 133.9522 226.1918 14.26113993    62";

    assertTrue(TLE.isLine1OK(line1));
    assertTrue(TLE.isLine2OK(line2));

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
    TLESeries series = new TLESeries();
    series.read(in);
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
    
    AbsoluteDate mil9cent = new AbsoluteDate("1900-01-01T00:00:00", UTCScale.getInstance());
    
    double daysSince1900 = mil9cent.minus(AbsoluteDate.JulianEpoch)/86400.0 - 2415020;
    System.out.println(daysSince1900);
  }

  public void aatestFirstSDP() throws OrekitException, ParseException {
//
////    # MOLNIYA 2-14 # 12h resonant ecc in 0.65 to 0.7 range 
////    1 08195U 75081A   06176.33215444  .00000099  00000-0  11873-3   0 813 
////    2 08195  64.1586 279.0717 6877146 264.7651  20.2257  2.00491383225656 
//
//    // TEST VALUES : 
//                
//    String line1 = "1 08195U 75081A   06176.33215444  .00000099  00000-0  11873-3   0 813";
//    String line2 = "2 08195  64.1586 279.0717 6877146 264.7651  20.2257  2.00491383225656";
//    
//    Vector3D testPos = new Vector3D ( 2349.89483350,
//                                      -14785.93811562,
//                                      0.02119378);
//
//
//    Vector3D testVel = new Vector3D (2.721488096,
//                                     -3.256811655,
//                                     4.498416672);
//
//
//
//    // Convert to tle :    
//    assertTrue(TLE.isFormatOK(line1, line2)); 
//    TLE tle = new TLE(line1, line2);

//    TLEPropagator ex = TLEPropagator.selectExtrapolator(tle);
//    PVCoordinates result = ex.getPVCoordinates(tle.getEpoch());
////    double daysSince1900 = tle.getEpoch().minus(AbsoluteDate.JulianEpoch)/86400.0 - 2415020;
////    System.out.println("days : " + daysSince1900);
//    

   
//    Utils.vectorToString("result pos SDP: ", result.getPosition().subtract(testPos));
//
//    Utils.vectorToString("result vel SDP: ", result.getVelocity().subtract(testVel));

    
//    8195 xx 
//    0.00000000 2349.89483350 -14785.93811562 0.02119378 2.721488096 -3.256811655 4.498416672 
//    120.00000000 15223.91713658 -17852.95881713 25280.39558224 1.079041732 0.875187372 2.485682813 2006 6 25 9:58:18.143649 
//    240.00000000 19752.78050009 -8600.07130962 37522.72921090 0.238105279 1.546110924 0.986410447 2006 6 25 11:58:18.143622 
//    360.00000000 19089.29762968 3107.89495018 39958.14661370 -0.410308034 1.640332277 -0.306873818 2006 6 25 13:58:18.143636 
//    480.00000000 13829.66070574 13977.39999817 32736.32082508 -1.065096849 1.279983299 -1.760166075 2006 6 25 15:58:18.143649 
//    600.00000000 3333.05838525 18395.31728674 12738.25031238 -1.882432221 -0.611623333 -4.039586549 2006 6 25 17:58:18.143622 
//    720.00000000 2622.13222207 -15125.15464924 474.51048398 2.688287199 -3.078426664 4.494979530 2006 6 25 19:58:18.143636 
//    840.00000000 15320.56770017 -17777.32564586 25539.53198382 1.064346229 0.892184771 2.459822414 2006 6 25 21:58:18.143649 
//    960.00000000 19769.70267785 -8458.65104454 37624.20130236 0.229304396 1.550363884 0.966993056 2006 6 25 23:58:18.143622 
//    1080.00000000 19048.56201523 3260.43223119 39923.39143967 -0.418015536 1.639346953 -0.326094840 2006 6 26 1:58:18.143636 
//    1200.00000000 13729.19205837 14097.70014810 32547.52799890 -1.074511043 1.270505211 -1.785099927 2006 6 26 3:58:18.143649 
//    1320.00000000 3148.86165643 18323.19841703 12305.75195578 -1.895271701 -0.678343847 -4.086577951 2006 6 26 5:58:18.143622 
//    1440.00000000 2890.80638268 -15446.43952300 948.77010176 2.654407490 -2.909344895 4.486437362 2006 6 26 7:58:18.143636 
//    1560.00000000 15415.98410712 -17699.90714437 25796.19644689 1.049818334 0.908822332 2.434107329 2006 6 26 9:58:18.143649 
//    1680.00000000 19786.00618538 -8316.74570581 37723.74539119 0.220539813 1.554518900 0.947601047 2006 6 26 11:58:18.143622 
//    1800.00000000 19007.28688729 3412.85948715 39886.66579255 -0.425733568 1.638276809 -0.345353807 2006 6 26 13:58:18.143636 
//    1920.00000000 13627.93015254 14216.95401307 32356.13706868 -1.083991976 1.260802347 -1.810193903 2006 6 26 15:58:18.143649 
//    2040.00000000 2963.26486560 18243.85063641 11868.25797486 -1.908015447 -0.747870342 -4.134004492 2006 6 26 17:58:18.143622 
//    2160.00000000 3155.85126036 -15750.70393364 1422.32496953 2.620085624 -2.748990396 4.473527039 2006 6 26 19:58:18.143636 
//    2280.00000000 15510.15191770 -17620.71002219 26050.43525345 1.035454678 0.925111006 2.408534465 2006 6 26 21:58:18.143649 
//    2400.00000000 19801.67198812 -8174.33337167 37821.38577439 0.211812700 1.558576937 0.928231880 2006 6 26 23:58:18.143622 
//    2520.00000000 18965.46529379 3565.19666242 39847.97510998 -0.433459945 1.637120585 -0.364653213 2006 6 27 1:58:18.143636 
//    2640.00000000 13525.88227400 14335.15978787 32162.13236536 -1.093537945 1.250868256 -1.835451681 2006 6 27 3:58:18.143649 
//    2760.00000000 2776.30574260 18156.98538451 11425.73046481 -1.920632199 -0.820370733 -4.181839232 2006 6 27 5:58:18.143622 
//    2880.00000000 3417.20931586 -16038.79510665 1894.74934058 2.585515864 -2.596818146 4.456882556 2006 6 27 7:58:18.143636 
  }

  public void testThetaG() throws OrekitException, ParseException {
    
    AbsoluteDate date = AbsoluteDate.J2000Epoch;
    
    double teta = SDP4.thetaG(date); 
    
    TIRF2000Frame ITRF = (TIRF2000Frame)Frame.getReferenceFrame(Frame.tirf2000B, date);
    double tetaTIRF = ITRF.getEarthRotationAngle(date);    
    assertEquals( Utils.trimAngle(tetaTIRF, Math.PI), Utils.trimAngle(teta, Math.PI), 0.003);
        
    date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 78.2*86400);
    
    teta = SDP4.thetaG(date); 
    tetaTIRF = ITRF.getEarthRotationAngle(date);
    
    assertEquals( Utils.trimAngle(tetaTIRF, Math.PI), Utils.trimAngle(teta, Math.PI), 0.003);
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
        String[] header = new String[4];
        int count = 0;
        for (eline = rEntry.readLine(); eline.charAt(0)=='#'; eline = rEntry.readLine()) {
          if(eline.charAt(0)=='#') {
            header[count++] = eline;
          }          
        }
        String line1 = eline;
        String line2 = rEntry.readLine();
        assertTrue(TLE.isFormatOK(line1, line2));

        TLE tle = new TLE(line1, line2);

        double satNum = Double.parseDouble(title[1]);
        assertTrue(satNum==tle.getSatelliteNumber());

        TLEPropagator ex = TLEPropagator.selectExtrapolator(tle);
        System.out.println("SATELLITE " + Double.parseDouble(title[1]));
        System.out.println(header[1]);
        System.out.println(header[2]);  
        
        double maxError = 0;
        for (rline = rResults.readLine(); (rline!=null)&&(rline.charAt(0)!='r'); rline = rResults.readLine()) {
           
            
            String[] data = rline.split(" ");
            double minFromStart = Double.parseDouble(data[0]);
            double pX = Double.parseDouble(data[1]);
            double pY = Double.parseDouble(data[2]);
            double pZ = Double.parseDouble(data[3]);
            double vX = Double.parseDouble(data[4]);
            double vY = Double.parseDouble(data[5]);
            double vZ = Double.parseDouble(data[6]);
            Vector3D testPos = new Vector3D(pX, pY, pZ);
            Vector3D testVel = new Vector3D(vX, vY, vZ);
            
            AbsoluteDate date = new AbsoluteDate(tle.getEpoch(), minFromStart*60);
            PVCoordinates results = null;
            try {
              results = ex.getPVCoordinates(date);
            }
            catch(IllegalArgumentException e)  {
              if(satNum==28872  || satNum==23333 || satNum==29141 ) {
                // expected behaviour
              }
              else {
                fail(" exception not expected");
              }
            }
            if (results != null) {
              double normDifPos = testPos.subtract(results.getPosition()).getNorm();
//              double normDifVel = testVel.subtract(results.getVelocity()).getNorm();
              cumulated += normDifPos;
              System.out.print(minFromStart);
              System.out.println(" " + normDifPos);
//              Utils.vectorToString(" ",testPos.subtract(results.getPosition()));
              
//              assertEquals( 0, normDifPos, 1);
//              assertEquals( 0, normDifVel, 1e-3);
              if(maxError == 0 || normDifPos>maxError) {
                maxError = normDifPos;
              }
            }  
       

        }
//        System.out.println(" max error : " + maxError);
//        System.out.println();
//        System.out.println();
        
      }
    }

    System.out.println();
    System.out.println( "cumulated error : " + cumulated);
  }
}