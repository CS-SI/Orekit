package fr.cs.orekit.attitudes;

import java.io.FileNotFoundException;
import java.text.ParseException;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.Utils;
import fr.cs.orekit.attitudes.models.LOFAlignedAttitude;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.iers.IERSDataResetter;
import fr.cs.orekit.orbits.CircularParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.KeplerianPropagator;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.UTCScale;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class LOFAlinedAttitudeTest extends TestCase {

  public void testQSW() throws ParseException, OrekitException, FileNotFoundException {

    CircularParameters op = new CircularParameters(12000000.0, 1.0e-3, 1.0e-3, Math.PI/4, 0.0,
                                                   0.0, CircularParameters.TRUE_LONGITUDE_ARGUMENT,
                                                   Frame.getJ2000());

    AbsoluteDate initDate = new AbsoluteDate("2001-03-21T00:00:00",
                                             UTCScale.getInstance());

    Orbit o = new Orbit(initDate, op);

    AttitudeKinematicsProvider att = 
      new LOFAlignedAttitude(Utils.mu, LOFAlignedAttitude.QSW);

    SpacecraftState initState =
      new SpacecraftState(o, 1000, 
                          att.getAttitudeKinematics(initDate, o.getPVCoordinates(Utils.mu), 
                                                    o.getFrame()));
    double period = 2 * Math.PI * op.getA() * Math.sqrt(op.getA() / Utils.mu);

    KeplerianPropagator kep = new KeplerianPropagator(initState, Utils.mu);
    kep.setAkProvider(att);

    SpacecraftState medState;
    AbsoluteDate medDate;

    for (int j=0 ; j<= period; j++) {
      medDate = new AbsoluteDate(initDate , j);
      medState = kep.getSpacecraftState(medDate);
      Vector3D pos = medState.getPVCoordinates(Utils.mu).getPosition().negate();
      // X is earth centered :
      Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.plusI);
      assertEquals(0, Vector3D.angle(pos, dir), 10e-10);
      // J is orthogonal to the orbital plane :
      dir = medState.getAttitude().applyInverseTo(Vector3D.plusJ);
      assertEquals(0, Vector3D.dotProduct(pos, dir), 1e-4);
    }

    op = new CircularParameters(op.getA(), Math.PI / 8, Math.PI / 8,
                                op.getI(), op.getRightAscensionOfAscendingNode(),
                                op.getAlphaV(), CircularParameters.TRUE_LONGITUDE_ARGUMENT,
                                Frame.getJ2000());


    o = new Orbit(initDate, op);

    initState = new SpacecraftState(o, 1000, 
                                    att.getAttitudeKinematics(initDate, o.getPVCoordinates(Utils.mu), 
                                                              o.getFrame()));
    period = 2 * Math.PI * op.getA() * Math.sqrt(op.getA() / Utils.mu);

    kep = new KeplerianPropagator(initState, Utils.mu);
    kep.setAkProvider(att);



    for (int j=0 ; j<= period; j++) {
      medDate = new AbsoluteDate(initDate , j);
      medState = kep.getSpacecraftState(medDate);
      Vector3D pos = medState.getPVCoordinates(Utils.mu).getPosition().negate();
      // X is earth centered :
      Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.plusI);
      assertEquals(0, Vector3D.angle(pos, dir), 10e-10);
      // J is orthogonal to the orbital plane :
      dir = medState.getAttitude().applyInverseTo(Vector3D.plusJ);
      assertEquals(0, Vector3D.dotProduct(pos, dir), 1e-4);
    }
  }

  public void testTNW() throws ParseException, OrekitException, FileNotFoundException {

    final CircularParameters op =
      new CircularParameters(12000000, 1e-3, 1e-3, Math.PI/4, 0.0,
                             0.0, CircularParameters.TRUE_LONGITUDE_ARGUMENT,
                             Frame.getJ2000());

    final AbsoluteDate initDate = new AbsoluteDate("2001-03-21T00:00:00",
                                                   UTCScale.getInstance());

    final Orbit o = new Orbit(initDate, op);

    AttitudeKinematicsProvider att = 
      new LOFAlignedAttitude(Utils.mu, LOFAlignedAttitude.TNW);

    final SpacecraftState initState =
      new SpacecraftState(o, 1000, 
                          att.getAttitudeKinematics(initDate, o.getPVCoordinates(Utils.mu), 
                                                    o.getFrame()));
    double period = 2 * Math.PI * op.getA() * Math.sqrt(op.getA() / Utils.mu);

    KeplerianPropagator kep = new KeplerianPropagator(initState, Utils.mu);
    kep.setAkProvider(att);
    SpacecraftState medState;
    AbsoluteDate medDate;

    for (int j=0 ; j<= period; j++) {
      medDate = new AbsoluteDate(initDate , j);
      medState = kep.getSpacecraftState(medDate);
      Vector3D vel = medState.getPVCoordinates(Utils.mu).getVelocity();
      Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.plusK);
      // K is alined with the velocity
      assertEquals(0, Vector3D.angle(vel, dir), 10e-8);
      dir = medState.getAttitude().applyInverseTo(Vector3D.plusJ);
      // J is orthogonal to the orbital plane :
      assertEquals(0, Vector3D.dotProduct(vel, dir), 1e-3);
    }
  }

  public void setUp() {
    IERSDataResetter.setUp("regular-data");
  }

  public void tearDown() {
    IERSDataResetter.tearDown();
  }

  public static Test suite() {
    return new TestSuite(LOFAlinedAttitudeTest.class);
  }
}
