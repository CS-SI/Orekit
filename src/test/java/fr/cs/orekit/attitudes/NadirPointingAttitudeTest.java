package fr.cs.orekit.attitudes;

import java.io.FileNotFoundException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.Utils;
import fr.cs.orekit.attitudes.models.NadirPointingAttitude;
import fr.cs.orekit.bodies.BodyShape;
import fr.cs.orekit.bodies.GeodeticPoint;
import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.iers.IERSDataResetter;
import fr.cs.orekit.orbits.CircularParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.analytical.KeplerianPropagator;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.Line;


public class NadirPointingAttitudeTest extends TestCase {

    public void testPureNadir() throws ParseException, OrekitException, FileNotFoundException {

        final CircularParameters op =
            new CircularParameters(12000000, 1e-3, 1e-3, Math.PI/4, 0.0,
                                   0.0, CircularParameters.TRUE_LONGITUDE_ARGUMENT,
                                   Frame.getJ2000());

        final AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                                       ChunkedTime.H00,
                                                       UTCScale.getInstance());

        final Orbit o = new Orbit(initDate, op);

        BodyShape earth = new OneAxisEllipsoid(6378136.460, 0.6);

        AttitudeKinematicsProvider att =
            new NadirPointingAttitude(Utils.mu, earth, NadirPointingAttitude.PURENADIR,0,0);

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
            Vector3D pos = medState.getPVCoordinates(Utils.mu).getPosition().negate();
            Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.minusK);
            Line orto = new Line(pos , dir);
            GeodeticPoint geo = earth.transform(pos);
            geo = new GeodeticPoint(geo.longitude, geo.latitude, 0);
            assertEquals(0, orto.distance(earth.transform(geo)), 1e-3);
        }
    }

    public void testOrbitalNadir() throws ParseException, OrekitException, FileNotFoundException {

        CircularParameters op =
            new CircularParameters(12000000, 1e-3, 1e-3, Math.PI/2, 0.0,
                                   0.0, CircularParameters.TRUE_LONGITUDE_ARGUMENT,
                                   Frame.getJ2000());

        final AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                                       ChunkedTime.H00,
                                                       UTCScale.getInstance());

        Orbit o = new Orbit(initDate, op);

        BodyShape earth = new OneAxisEllipsoid(6378136.460, 0.6);

        AttitudeKinematicsProvider att =
            new NadirPointingAttitude(Utils.mu, earth, NadirPointingAttitude.ORBITALPLANE,0,0);

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
            Vector3D pos = medState.getPVCoordinates(Utils.mu).getPosition();
            Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.minusK);
            Line orto = new Line(pos , dir);
            GeodeticPoint geo = earth.transform(pos);
            geo = new GeodeticPoint(geo.longitude, geo.latitude, 0);
            // with i = pi/2, the behaviour is the same as a pure NAdir :
            assertEquals(0, orto.distance(earth.transform(geo)), 1e-7);
        }

        op = new CircularParameters(op.getA(), Math.PI/8, Math.PI/8,
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
            Vector3D pos = medState.getPVCoordinates(Utils.mu).getPosition();
            Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.minusK);
            Line orto = new Line(pos , dir);
            GeodeticPoint geo = earth.transform(pos);
            geo = new GeodeticPoint(geo.longitude, geo.latitude, 0);
            // with i = pi/2, the behaviour is the same as a pure NAdir :
            assertEquals(0, orto.distance(earth.transform(geo)), 1e-7);
        }
    }

    public void testWithOrbitalInclination() throws ParseException, OrekitException, FileNotFoundException {

        final CircularParameters op =
            new CircularParameters(12000000, 1e-3, 1e-3, Math.PI/4, 0.0,
                                   0.0, CircularParameters.TRUE_LONGITUDE_ARGUMENT,
                                   Frame.getJ2000());

        final AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2001, 03, 21),
                                                       ChunkedTime.H00,
                                                       UTCScale.getInstance());

        final Orbit o = new Orbit(initDate, op);

        BodyShape earth = new OneAxisEllipsoid(6378136.460, 0.6);

        AttitudeKinematicsProvider att =
            new NadirPointingAttitude(Utils.mu, earth, NadirPointingAttitude.ORBITALPLANE,0,0);

        final SpacecraftState initState =
            new SpacecraftState(o, 1000,
                                att.getAttitudeKinematics(initDate, o.getPVCoordinates(Utils.mu),
                                                          o.getFrame()));
        double period = 2 * Math.PI * op.getA() * Math.sqrt(op.getA() / Utils.mu);

        KeplerianPropagator kep = new KeplerianPropagator(initState, Utils.mu);
        kep.setAkProvider(att);
        SpacecraftState medState;
        AbsoluteDate medDate;

        medDate = new AbsoluteDate(initDate , period/6);
        medState = kep.getSpacecraftState(medDate);
        Vector3D pos = medState.getPVCoordinates(Utils.mu).getPosition();
        Vector3D dir = medState.getAttitude().applyInverseTo(Vector3D.plusJ);
        assertEquals(0, Vector3D.dotProduct(dir, pos), 1e-7);
        dir = medState.getAttitude().applyInverseTo(Vector3D.minusK);
        assertEquals(0.14886, Vector3D.angle(dir, pos), 1e-5);
    }

    public void setUp() {
        IERSDataResetter.setUp("regular-data");
    }

    public void tearDown() {
        IERSDataResetter.tearDown();
    }

    public static Test suite() {
        return new TestSuite(NadirPointingAttitudeTest.class);
    }

}
