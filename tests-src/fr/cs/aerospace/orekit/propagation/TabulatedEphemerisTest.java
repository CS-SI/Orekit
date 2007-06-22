package fr.cs.aerospace.orekit.propagation;

import java.text.ParseException;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.aerospace.orekit.attitudes.models.NadirPointingAttitude;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.KeplerianParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import fr.cs.aerospace.orekit.utils.PVCoordinates;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class TabulatedEphemerisTest extends TestCase {

  public void testInterpolation() throws ParseException, OrekitException {

    double mass = 2500;
    double a = 7187990.1979844316;
    double e = 0.5e-4;
    double i = 1.7105407051081795;
    double omega = 1.9674147913622104;
    double OMEGA = Math.toRadians(261);
    double lv = 0;

   AttitudeKinematicsProvider akp = new NadirPointingAttitude(Utils.mu, 
                                                NadirPointingAttitude.PURENADIR, 0, 0);
 
    OrbitalParameters transPar = new KeplerianParameters(a, e, i,
                                                     omega, OMEGA,
                                                     lv, KeplerianParameters.TRUE_ANOMALY, Frame.getJ2000());
    
    AbsoluteDate initDate = new AbsoluteDate("2004-01-01T00:00:00.000" , UTCScale.getInstance());
    AbsoluteDate finalDate = new AbsoluteDate("2004-01-02T00:00:00.000" , UTCScale.getInstance());
    double deltaT = finalDate.minus(initDate);
    
    SpacecraftState initState = new SpacecraftState(new Orbit(initDate, transPar), mass, akp.getAttitudeKinematics(initDate, 
                                         transPar.getPVCoordinates(Utils.mu), transPar.getFrame()));
    
    EcksteinHechlerPropagator eck =
      new EcksteinHechlerPropagator(initState, ae, mu, c20, c30, c40, c50, c60);
    
    eck.setAkProvider(akp);

    int nbPoints = 1000;
    SpacecraftState[] tab = new SpacecraftState[nbPoints+1];
    
    for(int j = 0; j<= nbPoints; j++) {
      AbsoluteDate current = new AbsoluteDate(initDate, j*deltaT/(double)nbPoints );
      tab[j] = eck.getSpacecraftState(current);
    }
    
    TabulatedEphemeris te = new TabulatedEphemeris(tab);

    assertTrue(te.getMaxDate().minus(finalDate)==0);
    assertTrue(te.getMinDate().minus(initDate)==0);
    
    AbsoluteDate myDate = new AbsoluteDate(initDate, 80001);

//    assertEquals( eck.getSpacecraftState(myDate).getA(), te.getSpacecraftState(myDate).getA(), 0 );
//    assertEquals( eck.getSpacecraftState(myDate).getEx(), te.getSpacecraftState(myDate).getEx(), 0 );
//    assertEquals( eck.getSpacecraftState(myDate).getEy(), te.getSpacecraftState(myDate).getEy(), 0 );
//    assertEquals( eck.getSpacecraftState(myDate).getHx(), te.getSpacecraftState(myDate).getHx(), 0 );
//    assertEquals( eck.getSpacecraftState(myDate).getHy(), te.getSpacecraftState(myDate).getHy(), 0 );
//    assertEquals( eck.getSpacecraftState(myDate).getLv(), te.getSpacecraftState(myDate).getLv(), 0 );
    
    PVCoordinates pv = new PVCoordinates(new Vector3D(1,0,0), new Vector3D(1,0,0));
    assertEquals( eck.getSpacecraftState(myDate).getAkTransform().transformPVCoordinates(pv).getPosition().getX(), 
                  te.getSpacecraftState(myDate).getAkTransform().transformPVCoordinates(pv).getPosition().getX(),
                  1e-4 );
    assertEquals( eck.getSpacecraftState(myDate).getAkTransform().transformPVCoordinates(pv).getVelocity().getX(), 
                  te.getSpacecraftState(myDate).getAkTransform().transformPVCoordinates(pv).getVelocity().getX(),
                  1e-4 );
    
    
    
    
    
  }
  
  public void setUp() {
    mu  = 3.9860047e14;
    ae  = 6.378137e6;
    c20 = -1.08263e-3;
    c30 = 2.54e-6;
    c40 = 1.62e-6;
    c50 = 2.3e-7;
    c60 = -5.5e-7;
  }

  public void tearDown() {
    mu  = Double.NaN;
    ae  = Double.NaN;
    c20 = Double.NaN;
    c30 = Double.NaN;
    c40 = Double.NaN;
    c50 = Double.NaN;
    c60 = Double.NaN;
  }

  private double mu;
  private double ae;
  private double c20;
  private double c30;
  private double c40;
  private double c50;
  private double c60;
  
  public static Test suite() {
    return new TestSuite(TabulatedEphemerisTest.class);
  }
  
}
