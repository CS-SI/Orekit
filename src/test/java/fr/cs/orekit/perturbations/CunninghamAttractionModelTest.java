package fr.cs.orekit.perturbations;

import java.io.FileNotFoundException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.GraggBulirschStoerIntegrator;
import org.apache.commons.math.ode.IntegratorException;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.iers.IERSDirectoryCrawler;
import fr.cs.orekit.models.bodies.Sun;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.KeplerianParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.analytical.EcksteinHechlerPropagator;
import fr.cs.orekit.propagation.numerical.NumericalPropagator;
import fr.cs.orekit.propagation.numerical.OrekitFixedStepHandler;
import fr.cs.orekit.propagation.numerical.forces.perturbations.CunninghamAttractionModel;
import fr.cs.orekit.propagation.numerical.forces.perturbations.DrozinerAttractionModel;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;
import fr.cs.orekit.utils.PVCoordinates;

public class CunninghamAttractionModelTest extends TestCase {

    public CunninghamAttractionModelTest(String name) {
        super(name);
        itrf2000   = null;
        propagator = null;
    }

    // rough test to determine if J2 alone creates heliosynchronism
    public void testHelioSynchronous()
    throws ParseException, FileNotFoundException,
    OrekitException, DerivativeException, IntegratorException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        Transform itrfToJ2000  = itrf2000.getTransformTo(Frame.getJ2000(), date);
        Vector3D pole          = itrfToJ2000.transformVector(Vector3D.plusK);
        Frame poleAligned      = new Frame(Frame.getJ2000(),
                                           new Transform(new Rotation(pole, Vector3D.plusK)),
        "pole aligned");

        double i     = Math.toRadians(98.7);
        double omega = Math.toRadians(93.0);
        double OMEGA = Math.toRadians(15.0 * 22.5);
        OrbitalParameters op = new KeplerianParameters(7201009.7124401, 1e-3, i , omega, OMEGA,
                                                       0, KeplerianParameters.MEAN_ANOMALY,
                                                       poleAligned);
        Orbit orbit = new Orbit(date , op);

        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = c20;
        double[][] s = new double[3][1];
        propagator.addForceModel(new CunninghamAttractionModel( itrf2000, 6378136.460, c, s));

        // let the step handler perform the test
        propagator.propagate(new SpacecraftState(orbit, mu), new AbsoluteDate(date , 7 * 86400),
                             86400, new SpotStepHandler(date, mu));

    }

    private static class SpotStepHandler extends OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 5870065503411913447L;

        public SpotStepHandler(AbsoluteDate date, double mu) {
            this.mu   = mu;
            sun       = new Sun();
            previous  = Double.NaN;
        }

        private double mu;
        private Sun sun;
        private double previous;
        public void handleStep(SpacecraftState currentState, boolean isLast) {


            Vector3D pos = currentState.getPVCoordinates(mu).getPosition();
            Vector3D vel = currentState.getPVCoordinates(mu).getVelocity();
            AbsoluteDate current = currentState.getDate();
            Vector3D sunPos;
            try {
                sunPos = sun.getPosition(current , Frame.getJ2000());
            } catch (OrekitException e) {
                sunPos = new Vector3D();
                System.out.println("exception during sun.getPosition");
                e.printStackTrace();
            }
            Vector3D normal = Vector3D.crossProduct(pos,vel);
            double angle = Vector3D.angle(sunPos , normal);
            if (! Double.isNaN(previous)) {
                assertEquals(previous, angle, 0.0005);
            }
            previous = angle;
        }

        public boolean requiresDenseOutput() {
            return false;
        }

        public void reset() {
        }

    }

    // test the difference with the analytical extrapolator Eckstein Hechler
    public void testEcksteinHechlerReference()
    throws ParseException, FileNotFoundException,
    OrekitException, DerivativeException, IntegratorException {

        //  Definition of initial conditions with position and velocity
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000Epoch, 584.);
        Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Transform itrfToJ2000  = itrf2000.getTransformTo(Frame.getJ2000(), date);
        Vector3D pole          = itrfToJ2000.transformVector(Vector3D.plusK);
        Frame poleAligned      = new Frame(Frame.getJ2000(),
                                           new Transform(new Rotation(pole, Vector3D.plusK)),
        "pole aligned");

        Orbit initialOrbit =
            new Orbit(date,
                      new EquinoctialParameters(new PVCoordinates(position, velocity),
                                                poleAligned, mu));

        propagator.addForceModel(new CunninghamAttractionModel(itrf2000, ae,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        // let the step handler perform the test
        propagator.propagate(new SpacecraftState(initialOrbit, mu),
                             new AbsoluteDate(date , 50000), 20,
                             new EckStepHandler(initialOrbit, ae, mu,
                                                c20, c30, c40, c50, c60));

    }

    private static class EckStepHandler extends OrekitFixedStepHandler {

        /**Serializable UID. */
        private static final long serialVersionUID = 6132817809836153771L;

        private final double mu;

        private EckStepHandler(Orbit initialOrbit, double ae, double mu,
                               double c20, double c30, double c40, double c50, double c60)
        throws FileNotFoundException, OrekitException {
            referencePropagator =
                new EcksteinHechlerPropagator(new SpacecraftState(initialOrbit, mu),
                                              ae, mu, c20, c30, c40, c50, c60);
            this.mu = mu;
        }

        private EcksteinHechlerPropagator referencePropagator;
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            try {


                SpacecraftState EHPOrbit   = referencePropagator.getSpacecraftState(currentState.getDate());
                Vector3D posEHP  = EHPOrbit.getPVCoordinates(mu).getPosition();
                Vector3D posDROZ = currentState.getPVCoordinates(mu).getPosition();
                Vector3D velEHP  = EHPOrbit.getPVCoordinates(mu).getVelocity();
                Vector3D dif     = posEHP.subtract(posDROZ);

                Vector3D T = new Vector3D(1 / velEHP.getNorm(), velEHP);
                Vector3D W = Vector3D.crossProduct(posEHP, velEHP).normalize();
                Vector3D N = Vector3D.crossProduct(W, T);

                assertTrue(dif.getNorm() < 104);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, T)) < 104);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, N)) <  53);
                assertTrue(Math.abs(Vector3D.dotProduct(dif, W)) <  13);

            } catch (PropagationException e) {
                e.printStackTrace();
            }

        }

        public boolean requiresDenseOutput() {
            return false;
        }

        public void reset() {
        }

    }
    // test the difference with the Cunningham model
    public void testZonalWithDrozinerReference()
    throws OrekitException, DerivativeException, IntegratorException, ParseException {
//      initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        double i     = Math.toRadians(98.7);
        double omega = Math.toRadians(93.0);
        double OMEGA = Math.toRadians(15.0 * 22.5);
        OrbitalParameters op = new KeplerianParameters(7201009.7124401, 1e-3, i , omega, OMEGA,
                                                       0, KeplerianParameters.MEAN_ANOMALY,
                                                       Frame.getJ2000());
        Orbit orbit = new Orbit(date , op);
        propagator = new NumericalPropagator(mu,
                                             new ClassicalRungeKuttaIntegrator(1000));
        propagator.addForceModel(new CunninghamAttractionModel(itrf2000, ae,
                                                               new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        SpacecraftState cunnOrb =
            propagator.propagate(new SpacecraftState(orbit, mu),
                                 new AbsoluteDate(date, 86400));

        propagator.removeForceModels();

        propagator.addForceModel(new DrozinerAttractionModel(itrf2000, ae,
                                                             new double[][] {
                { 0.0 }, { 0.0 }, { c20 }, { c30 },
                { c40 }, { c50 }, { c60 },
        },
        new double[][] {
                { 0.0 }, { 0.0 }, { 0.0 }, { 0.0 },
                { 0.0 }, { 0.0 }, { 0.0 },
        }));

        SpacecraftState drozOrb =
            propagator.propagate(new SpacecraftState(orbit, mu), new AbsoluteDate(date, 86400));

        Vector3D dif = cunnOrb.getPVCoordinates(mu).getPosition().subtract(drozOrb.getPVCoordinates(mu).getPosition());
        assertEquals(0, dif.getNorm(), 1.0e-8);
    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
        try {
            // Eigen c1 model truncated to degree 6
            mu =  3.986004415e+14;
            ae =  6378136.460;
            c20 = -1.08262631303e-3;
            c30 =  2.53248017972e-6;
            c40 =  1.61994537014e-6;
            c50 =  2.27888264414e-7;
            c60 = -5.40618601332e-7;

            itrf2000 = Frame.getReferenceFrame(Frame.ITRF2000B, new AbsoluteDate());
            propagator =
                new NumericalPropagator(mu,
                                        new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-4));
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }
    }

    public void tearDown() {
        itrf2000   = null;
        propagator = null;
    }

    public static Test suite() {
        return new TestSuite(CunninghamAttractionModelTest.class);
    }

    private double c20;
    private double c30;
    private double c40;
    private double c50;
    private double c60;
    private double mu;
    private double ae;

    private Frame   itrf2000;
    private NumericalPropagator propagator;

}


