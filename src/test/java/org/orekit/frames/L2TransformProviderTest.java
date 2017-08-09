package org.orekit.frames;

import java.io.File;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

/**Unit tests for {@link L2TransformProvider}.
 * 
 * @author Luc Maisonabe
 * @author Julio Hernanz
 */
public class L2TransformProviderTest {
    
  @Test
  public void testTransformationOrientationForEarthMoon() throws OrekitException {
      
    // configure Orekit
    File home       = new File(System.getProperty("user.home"));
    File orekitData = new File(home, "orekit-data");
    if (!orekitData.exists()) {
      System.err.format(Locale.US, "Failed to find %s folder%n",
                      orekitData.getAbsolutePath());
      System.err.format(Locale.US, "You need to download %s from the %s page and "
                      + "unzip it in %s for this tutorial to work%n",
                      "orekit-data.zip", "https://www.orekit.org/forge/projects/orekit/files",
                      home.getAbsolutePath());
      System.exit(1);
    }
    DataProvidersManager manager = DataProvidersManager.getInstance();
    manager.addProvider(new DirectoryCrawler(orekitData));
        
    // Load Bodies
    final CelestialBody earth = CelestialBodyFactory.getEarth();
    final CelestialBody moon = CelestialBodyFactory.getMoon();
     
    // Set frames
    final Frame eme2000 = FramesFactory.getEME2000();
    final Frame gcrf = FramesFactory.getGCRF();
    final L2TransformProvider l2transformProvider = new L2TransformProvider(earth, moon);
    final Frame l2Frame = new Frame(gcrf, l2transformProvider, "L2_frame");

    
    // Time settings
    final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000, 
                                               TimeScalesFactory.getUTC());
     
    // Compute Moon position in EME2000
    PVCoordinates pvMoon = moon.getPVCoordinates(date, eme2000);
    Vector3D posMoon = pvMoon.getPosition();

    // Compute L2 position in EME2000
    // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
    // because the test should avoid doing wrong interpretation of the meaning and
    // particularly on the sign of the translation)
    Vector3D posL2   = l2Frame.getTransformTo(eme2000,date).transformPosition(Vector3D.ZERO);
    
    
    Vector3D posMoongcrf   = moon.getPVCoordinates(date, gcrf).getPosition();
    Assert.assertEquals(0.0, Vector3D.angle(posMoongcrf, posMoon), 1.0e-10);

    // check L2 and Moon are aligned as seen from Earth
    Assert.assertEquals(0.0, Vector3D.angle(posMoon, posL2), 1.0e-10);

    // check L2 if at least 60000km farther than Moon
    Assert.assertTrue(posL2.getNorm() > posMoon.getNorm() + 6.0e7);
     
  }
  
  
  @Test
  public void testTransformationOrientationForSunEarth() throws OrekitException {
    
    // configure Orekit
    File home       = new File(System.getProperty("user.home"));
    File orekitData = new File(home, "orekit-data");
    if (!orekitData.exists()) {
      System.err.format(Locale.US, "Failed to find %s folder%n",
                      orekitData.getAbsolutePath());
      System.err.format(Locale.US, "You need to download %s from the %s page and "
                      + "unzip it in %s for this tutorial to work%n",
                      "orekit-data.zip", "https://www.orekit.org/forge/projects/orekit/files",
                      home.getAbsolutePath());
      System.exit(1);
    }
    DataProvidersManager manager = DataProvidersManager.getInstance();
    manager.addProvider(new DirectoryCrawler(orekitData));
        
    // Load Bodies
    final CelestialBody sun = CelestialBodyFactory.getSun();
    final CelestialBody earth = CelestialBodyFactory.getEarth();
     
    // Set frames
    // TODO maybe add local sun frame (ICRF good enough?)
    final Frame icrf = FramesFactory.getICRF();
    final L2TransformProvider l2transformProvider = new L2TransformProvider(sun, earth);
    final Frame l2Frame = new Frame(icrf, l2transformProvider, "L2_frame");

    // Time settings
    final AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 0, 0, 00.000, 
                                               TimeScalesFactory.getUTC());
     
    // Compute Earth position in ICRF
    PVCoordinates pvEarth = earth.getPVCoordinates(date, icrf);
    Vector3D posEarth = pvEarth.getPosition();

    // Compute L2 position in ICRF
    // (it is important to use transformPosition(Vector3D.ZERO) and *not* getTranslation()
    // because the test should avoid doing wrong interpretation of the meaning and
    // particularly on the sign of the translation)
    Vector3D posL2   = l2Frame.getTransformTo(icrf,date).transformPosition(Vector3D.ZERO);

    // check L2 and Earth are aligned as seen from Sun
    Assert.assertEquals(0.0, Vector3D.angle(posEarth, posL2), 1.0e-10);

    // check L2 if at least 15000000 km farther than Earth
    Assert.assertTrue(posL2.getNorm() > posEarth.getNorm() + 1.5e10);
    
  }
  

}
