package fr.cs.aerospace.orekit.tle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.FindFile;
import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.frames.TIRF2000Frame;
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

  }

  public void aatestPVTransform() throws OrekitException, ParseException {

    // TEST VALUES : 

    String line1 = "1 07191U 73086EZ  02076.63595131 -.00000031  00000-0  10000-3 0  9045";
    String line2 = "2 07191 102.1490 334.0121 0253607  54.7437 307.7251 12.10431881825991";

    Vector3D testPos = new Vector3D ( 7095.62534394,
                                      -3459.54977223,
                                      2.65766867);


    Vector3D testVel = new Vector3D ( -0.79920008,
                                      -1.28572174,
                                      6.99886910);

//  OrbitalParameters testOrb = new CartesianParameters(new PVCoordinates(testPos, testVel)
//  , Frame.getJ2000(), Constants.mu);


    // Convert to tle :    
    assertTrue(TLE.isFormatOK(line1, line2)); 
    TLE tle = new TLE(line1, line2);

    SXP4Extrapolator ex = SXP4Extrapolator.selectExtrapolator(tle);
    PVCoordinates result = ex.getPVCoordinates(tle.getEpoch());

    Utils.vectorToString("result pos : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel : ", result.getVelocity().subtract(testVel));

    //-----360 min-----------------------------------------------------------------------

    testPos = new Vector3D ( 6876.82395295,
                             -3614.28549445,
                             1242.00621199);


    testVel = new Vector3D (-1.80950045,
                            -0.77766069,
                            6.90918302);

    result = ex.getPVCoordinates(new AbsoluteDate(tle.getEpoch(), 360*60));

    Utils.vectorToString("result pos 360 : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel 360 : ", result.getVelocity().subtract(testVel));

//  ------2880 min-----------------------------------------------------------------------

    testPos = new Vector3D ( 3051.67782955,
                             225.45893096,
                             -7549.72713780);


    testVel = new Vector3D (5.53973976,
                            -3.48618620,
                            2.28442985);

    result = ex.getPVCoordinates(new AbsoluteDate(tle.getEpoch(), -2880*60));

    Utils.vectorToString("result pos -2880 : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel -2880 : ", result.getVelocity().subtract(testVel));


//  ------------------ Verification test cases ----------------------

//  #   DELTA 1 DEB         # near earth normal drag equation
//  #                       # perigee = 377.26km, so moderate drag case
//  1 06251U 62025E   06176.82412014 .00008885 00000-0 12808-3 0 3985
//  2 06251 58.0579 54.0425 0030035 139.1568 221.1854 15.56387291 6774

//  1 07191U 73086EZ  02076.63595131 -.00000031  00000-0  10000-3 0  9045

//  2 07191 102.1490 334.0121 0253607  54.7437 307.7251 12.10431881825991

//  Near-Earth type Ephemeris (SGP4) selected:
//  Ephem:SGP4   Tsince         X/Xdot           Y/Ydot           Z/Zdot
//  7191     1440.0000      5197.68379504   -3532.40188648    4642.94280275 
//  -4.52981807       0.79135016       5.57991435 
//  7191        0.0000      7095.62534394   -3459.54977223       2.65766867 
//  -0.79920008      -1.28572174       6.99886910 
//  7191      360.0000      6876.82395295   -3614.28549445    1242.00621199 
//  -1.80950045      -0.77766069       6.90918302 
//  7191     1080.0000      5917.38908180   -3650.54888754    3593.33932356 
//  -3.70227403       0.27502090       6.19225553 
//  7191      720.0000      6481.41080392   -3678.31238626    2449.51628455 
//  -2.78652792      -0.25275130       6.63881188 
//  7191      719.0000      6638.89853614   -3657.68854846    2047.73362803 
//  -2.46167103      -0.43439031       6.75039533 
//  7191    -2880.0000      3051.67782955     225.45893096   -7549.72713780 
//  5.53973976      -3.48618620       2.28442985     
  }
  public void testFirstSGP() throws OrekitException, ParseException {

//  # DELTA 1 DEB # near earth normal drag equation 
//  # # perigee = 377.26km, so moderate drag case 
//  1 06251U 62025E   06176.82412014  .00008885  00000-0  12808-3 0  3985 
//  2 06251  58.0579  54.0425 0030035 139.1568 221.1854 15.56387291  6774 

    // TEST VALUES :  
    String line1 = "1 06251U 62025E   06176.82412014  .00008885  00000-0  12808-3 0  3985";
    String line2 = "2 06251  58.0579  54.0425 0030035 139.1568 221.1854 15.56387291  6774";

    Vector3D testPos = new Vector3D ( 3988.31022699,
                                      5498.96657235,
                                      0.90055879);


    Vector3D testVel = new Vector3D ( -3.290032738,
                                      2.357652820,
                                      6.496623475);



    // Convert to tle :    
    assertTrue(TLE.isFormatOK(line1, line2)); 
    TLE tle = new TLE(line1, line2);

    SXP4Extrapolator ex = SXP4Extrapolator.selectExtrapolator(tle);
    PVCoordinates result = ex.getPVCoordinates(tle.getEpoch());

    Utils.vectorToString("result pos : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel : ", result.getVelocity().subtract(testVel));

    //-----360 min-----------------------------------------------------------------------

    testPos = new Vector3D ( 4993.62642836,
                             2890.54969900,
                             -3600.40145627);


    testVel = new Vector3D (0.347333429,
                            5.707031557,
                            5.070699638);

    result = ex.getPVCoordinates(new AbsoluteDate(tle.getEpoch(), 360*60));

    Utils.vectorToString("result pos 360 : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel 360 : ", result.getVelocity().subtract(testVel));

    //-----960 min-----------------------------------------------------------------------

    testPos = new Vector3D ( -4990.91637950,
                             -2303.42547880,
                             3920.86335598);


    testVel = new Vector3D (-0.993439372,
                            -5.967458360,
                            -4.759110856);

    result = ex.getPVCoordinates(new AbsoluteDate(tle.getEpoch(), 960*60));

    Utils.vectorToString("result pos 960 : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel 960 : ", result.getVelocity().subtract(testVel));

    //-----1800 min-----------------------------------------------------------------------
//  1800.00000000 -4966.20137963 -4379.59155037 1349.33347502 1.763172581 -3.981456387 -6.343279443 
    testPos = new Vector3D ( -4966.20137963,
                             -4379.59155037,
                             1349.33347502);


    testVel = new Vector3D (1.763172581,
                            -3.981456387,
                            -6.343279443);

    result = ex.getPVCoordinates(new AbsoluteDate(tle.getEpoch(), 1800*60));

    Utils.vectorToString("result pos 1800 : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel 1800 : ", result.getVelocity().subtract(testVel));


    //-----2880 min-----------------------------------------------------------------------

    testPos = new Vector3D ( 1159.27802897,
                             5056.60175495,
                             4353.49418579);


    testVel = new Vector3D (-5.968060341,
                            -2.314790406,
                            4.230722669);

    result = ex.getPVCoordinates(new AbsoluteDate(tle.getEpoch(), 2880*60));

    Utils.vectorToString("result pos 2880 : ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel 2880 : ", result.getVelocity().subtract(testVel));

//  6251 xx 
//  0.00000000 3988.31022699 5498.96657235 0.90055879 -3.290032738 2.357652820 6.496623475 
//  120.00000000 -3935.69800083 409.10980837 5471.33577327 -3.374784183 -6.635211043 -1.942056221 2006 6 25 21:46:43.980124 
//  240.00000000 -1675.12766915 -5683.30432352 -3286.21510937 5.282496925 1.508674259 -5.354872978 2006 6 25 23:46:43.980097 
//  360.00000000 4993.62642836 2890.54969900 -3600.40145627 0.347333429 5.707031557 5.070699638 2006 6 26 1:46:43.980111 
//  480.00000000 -1115.07959514 4015.11691491 5326.99727718 -5.524279443 -4.765738774 2.402255961 2006 6 26 3:46:43.980124 
//  600.00000000 -4329.10008198 -5176.70287935 409.65313857 2.858408303 -2.933091792 -6.509690397 2006 6 26 5:46:43.980097 
//  720.00000000 3692.60030028 -976.24265255 -5623.36447493 3.897257243 6.415554948 1.429112190 2006 6 26 7:46:43.980111 
//  840.00000000 2301.83510037 5723.92394553 2814.61514580 -5.110924966 -0.764510559 5.662120145 2006 6 26 9:46:43.980124 
//  960.00000000 -4990.91637950 -2303.42547880 3920.86335598 -0.993439372 -5.967458360 -4.759110856 2006 6 26 11:46:43.980097 
//  1080.00000000 642.27769977 -4332.89821901 -5183.31523910 5.720542579 4.216573838 -2.846576139 2006 6 26 13:46:43.980111 
//  1200.00000000 4719.78335752 4798.06938996 -943.58851062 -2.294860662 3.492499389 6.408334723 2006 6 26 15:46:43.980124 
//  1320.00000000 -3299.16993602 1576.83168320 5678.67840638 -4.460347074 -6.202025196 -0.885874586 2006 6 26 17:46:43.980097 
//  1440.00000000 -2777.14682335 -5663.16031708 -2462.54889123 4.915493146 0.123328992 -5.896495091 2006 6 26 19:46:43.980111 
//  1560.00000000 4992.31573893 1716.62356770 -4287.86065581 1.640717189 6.071570434 4.338797931 2006 6 26 21:46:43.980124 
//  1680.00000000 -8.22384755 4662.21521668 4905.66411857 -5.891011274 -3.593173872 3.365100460 2006 6 26 23:46:43.980097 
//  1800.00000000 -4966.20137963 -4379.59155037 1349.33347502 1.763172581 -3.981456387 -6.343279443 2006 6 27 1:46:43.980111 
//  1920.00000000 2954.49390331 -2080.65984650 -5754.75038057 4.895893306 5.858184322 0.375474825 2006 6 27 3:46:43.980124 
//  2040.00000000 3363.28794321 5559.55841180 1956.05542266 -4.587378863 0.591943403 6.107838605 2006 6 27 5:46:43.980097 
//  2160.00000000 -4856.66780070 -1107.03450192 4557.21258241 -2.304158557 -6.186437070 -3.956549542 2006 6 27 7:46:43.980111 
//  2280.00000000 -497.84480071 -4863.46005312 -4700.81211217 5.960065407 2.996683369 -3.767123329 2006 6 27 9:46:43.980124 
//  2400.00000000 5241.61936096 3910.75960683 -1857.93473952 -1.124834806 4.406213160 6.148161299 2006 6 27 11:46:43.980097 
//  2520.00000000 -2451.38045953 2610.60463261 5729.79022069 -5.366560525 -5.500855666 0.187958716 2006 6 27 13:46:43.980111 
//  2640.00000000 -3791.87520638 -5378.82851382 -1575.82737930 4.266273592 -1.199162551 -6.276154080 2006 6 27 15:46:43.980124 
//  2760.00000000 4730.53958356 524.05006433 -4857.29369725 2.918056288 6.135412849 3.495115636 2006 6 27 17:46:43.980097 
//  2880.00000000 1159.27802897 5056.60175495 4353.49418579 -5.968060341 -2.314790406 4.230722669 2006 6 27 19:46:43.980111 

  }

  public void testFirstSDP() throws OrekitException, ParseException {

//    # MOLNIYA 2-14 # 12h resonant ecc in 0.65 to 0.7 range 
//    1 08195U 75081A   06176.33215444  .00000099  00000-0  11873-3   0 813 
//    2 08195  64.1586 279.0717 6877146 264.7651  20.2257  2.00491383225656 

    // TEST VALUES : 
                
    String line1 = "1 08195U 75081A   06176.33215444  .00000099  00000-0  11873-3   0 813";
    String line2 = "2 08195  64.1586 279.0717 6877146 264.7651  20.2257  2.00491383225656";
    
    Vector3D testPos = new Vector3D ( 2349.89483350,
                                      -14785.93811562,
                                      0.02119378);


    Vector3D testVel = new Vector3D (2.721488096,
                                     -3.256811655,
                                     4.498416672);



    // Convert to tle :    
    assertTrue(TLE.isFormatOK(line1, line2)); 
    TLE tle = new TLE(line1, line2);

    SXP4Extrapolator ex = SXP4Extrapolator.selectExtrapolator(tle);
    PVCoordinates result = ex.getPVCoordinates(tle.getEpoch());
//    double daysSince1900 = tle.getEpoch().minus(AbsoluteDate.JulianEpoch)/86400.0 - 2415020;
//    System.out.println("days : " + daysSince1900);
    
    Utils.vectorToString("result pos SDP: ", result.getPosition().subtract(testPos));

    Utils.vectorToString("result vel SDP: ", result.getVelocity().subtract(testVel));

    Deep2 dd = new Deep2(tle);
    
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
    
    double teta = SDP4Extrapolator.thetaG(date); 
    
    TIRF2000Frame ITRF = (TIRF2000Frame)Frame.getReferenceFrame(Frame.tirf2000B, date);
    double tetaTIRF = ITRF.getEarthRotationAngle(date);    
    
    System.out.println("teta j2000 epoch SDP : " + Utils.trimAngle(teta, Math.PI));
    System.out.println("teta j2000 epoch ITRF : " + Utils.trimAngle(tetaTIRF, Math.PI));
        
    date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 78.2*86400);
    
    teta = SDP4Extrapolator.thetaG(date); 
    tetaTIRF = ITRF.getEarthRotationAngle(date);
    
    System.out.println("teta random epoch SDP  " + Utils.trimAngle(teta, Math.PI));
    System.out.println("teta random epoch ITRF  " + Utils.trimAngle(tetaTIRF, Math.PI));

  }

}
