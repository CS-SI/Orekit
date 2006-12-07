package fr.cs.aerospace.orekit.attitudes;

import java.io.FileNotFoundException;
import java.text.ParseException;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.Utils;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.models.attitudes.LOFAlinedAttitude;
import fr.cs.aerospace.orekit.orbits.CircularParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.orbits.OrbitalParameters;
import fr.cs.aerospace.orekit.propagation.KeplerianPropagator;
import fr.cs.aerospace.orekit.propagation.SpacecraftState;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class LOFAlinedAttitudeTest extends TestCase {
  
  public void testSimpleBehaviourQSW() throws ParseException, OrekitException, FileNotFoundException {
    
    // parameters
    final double a = 12000000;
    final double ex = 1e-3;
    final double ey = 1e-3;
    final double OMEGA = 0;
    final double l = 0;
    final double i = Math.PI/4; 
    final double mu = Utils.mu;
    
    final OrbitalParameters op = new CircularParameters(a, ex, ey, i, OMEGA, l, 
                                                  CircularParameters.TRUE_LONGITUDE_ARGUMENT
                                                  , Frame.getJ2000());
    
    final AbsoluteDate initDate = new AbsoluteDate("2001-03-21T00:00:00",
                                         UTCScale.getInstance());
    
    final Orbit o = new Orbit(initDate, op);
    
    AttitudeKinematicsProvider att = 
      new LOFAlinedAttitude(mu, LOFAlinedAttitude.QSW);
    
    final SpacecraftState initState = new SpacecraftState(o, 1000, 
                                  att.getAttitudeKinematics(initDate, o.getPVCoordinates(mu), 
                                                            o.getFrame()));
    double period = 2*Math.PI*Math.sqrt(a*a*a/mu);
    
    KeplerianPropagator kep = new KeplerianPropagator(initState, mu);
    kep.setAkProvider(att);
    SpacecraftState medState;
    AbsoluteDate medDate;
    
    for (int j=0 ; j<= period; j++) {
      medDate = new AbsoluteDate(initDate , j);
      medState = kep.getSpacecraftState(medDate);
      Vector3D pos = new Vector3D(medState.getPVCoordinates(mu).getPosition());
      pos.negateSelf();
      Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.plusI);
      assertEquals(0, Vector3D.angle(pos, dir), 10e-10);
      dir = medState.getAttitude().applyInverseTo(Vector3D.plusJ);
      assertEquals(0, Vector3D.dotProduct(pos, dir), 1e-4);
    }
  }
  
public void testSimpleBehaviourTNW() throws ParseException, OrekitException, FileNotFoundException {
    
    // parameters
    final double a = 12000000;
    final double ex = 1e-3;
    final double ey = 1e-3;
    final double OMEGA = 0;
    final double l = 0;
    final double i = Math.PI/4; 
    final double mu = Utils.mu;
    
    final OrbitalParameters op = new CircularParameters(a, ex, ey, i, OMEGA, l, 
                                                  CircularParameters.TRUE_LONGITUDE_ARGUMENT
                                                  , Frame.getJ2000());
    
    final AbsoluteDate initDate = new AbsoluteDate("2001-03-21T00:00:00",
                                         UTCScale.getInstance());
    
    final Orbit o = new Orbit(initDate, op);
    
    AttitudeKinematicsProvider att = 
      new LOFAlinedAttitude(mu, LOFAlinedAttitude.TNW);
    
    final SpacecraftState initState = new SpacecraftState(o, 1000, 
                                  att.getAttitudeKinematics(initDate, o.getPVCoordinates(mu), 
                                                            o.getFrame()));
    double period = 2*Math.PI*Math.sqrt(a*a*a/mu);
    
    KeplerianPropagator kep = new KeplerianPropagator(initState, mu);
    kep.setAkProvider(att);
    SpacecraftState medState;
    AbsoluteDate medDate;
    
    for (int j=0 ; j<= period; j++) {
      medDate = new AbsoluteDate(initDate , j);
      medState = kep.getSpacecraftState(medDate);
      Vector3D vel = new Vector3D(medState.getPVCoordinates(mu).getVelocity());
      Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.plusK);
      assertEquals(0, Vector3D.angle(vel, dir), 10e-8);
      dir = medState.getAttitude().applyInverseTo(Vector3D.plusJ);
      assertEquals(0, Vector3D.dotProduct(vel, dir), 1e-3);
    }
  }
  
  public static Test suite() {
    return new TestSuite(LOFAlinedAttitudeTest.class);
  }
}
