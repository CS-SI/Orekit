package fr.cs.orekit.perturbations;

import java.io.FileNotFoundException;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.GraggBulirschStoerIntegrator;
import org.apache.commons.math.ode.IntegratorException;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.forces.perturbations.ThirdBodyAttraction;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.iers.IERSDataResetter;
import fr.cs.orekit.models.bodies.Moon;
import fr.cs.orekit.models.bodies.Sun;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.NumericalPropagator;
import fr.cs.orekit.propagation.numerical.OrekitFixedStepHandler;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;

public class ThirdBodyAttractionTest extends TestCase {

    public void testSunContrib() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
                                                         Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                                         0.1, 2, Frame.getJ2000());
        Orbit orbit = new Orbit(date , op);
        Sun sun = new Sun();

        // creation of the force model
        ThirdBodyAttraction TBA =  new ThirdBodyAttraction(sun);

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);

        // creation of the propagator
        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period, 0, 10e-5);
        NumericalPropagator calc = new NumericalPropagator(mu, integrator);
        calc.addForceModel(TBA);

        // Step Handler

        TBAStepHandler sh = new TBAStepHandler(TBAStepHandler.SUN, date);
        AbsoluteDate finalDate = new AbsoluteDate(date , 2*365*period);
        calc.propagate(new SpacecraftState(orbit) , finalDate, Math.floor(period), sh );

    }
    public void testMoonContrib() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 07, 01),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
                                                         Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                                         0.1, 2, Frame.getJ2000());
        Orbit orbit = new Orbit(date , op);
        Moon moon = new Moon();

        // creation of the force model
        ThirdBodyAttraction TBA =  new ThirdBodyAttraction(moon);

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);

        // creation of the propagator
        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period, 0, 10e-5);
        NumericalPropagator calc = new NumericalPropagator(mu, integrator);
        calc.addForceModel(TBA);

        // Step Handler

        TBAStepHandler sh = new TBAStepHandler(TBAStepHandler.MOON, date);
        AbsoluteDate finalDate = new AbsoluteDate(date , 365*period);
        calc.propagate(new SpacecraftState(orbit) , finalDate, Math.floor(period), sh );

    }

    private double mu = 3.98600E14;

    private static class TBAStepHandler extends OrekitFixedStepHandler {

        public static final int MOON = 1;
        public static final int SUN = 2;
        public static final int SUNandMOON = 3;
        private int type;
        AbsoluteDate date;

        private TBAStepHandler(int type, AbsoluteDate date) throws FileNotFoundException {
            this.type = type;
            this.date = date;
        }

        public void handleStep(double t, double[]y, boolean isLastStep) {
            if (type == MOON) {
                assertEquals(0, xMoon(t)-y[3], 1e-4);
                assertEquals(0, yMoon(t)-y[4], 1e-4);
            }
            if (type == SUN) {
                assertEquals(0, xSun(t)-y[3], 1e-4);
                assertEquals(0, ySun(t)-y[4], 1e-4);
            }
            if (type == SUNandMOON) {

            }
        }

        private double xMoon(double t) {
            return -0.909227e-3 - 0.309607e-10 * t + 2.68116e-5 *
            Math.cos(5.29808e-6*t) - 1.46451e-5 * Math.sin(5.29808e-6*t);
        }

        private double yMoon(double t) {
            return 1.48482e-3 + 1.57598e-10 * t + 1.47626e-5 *
            Math.cos(5.29808e-6*t) - 2.69654e-5 * Math.sin(5.29808e-6*t);
        }

        private double xSun(double t) {
            return -1.06757e-3 + 0.221415e-11 * t + 18.9421e-5 *
            Math.cos(3.9820426e-7*t) - 7.59983e-5 * Math.sin(3.9820426e-7*t);
        }

        private double ySun(double t) {
            return 1.43526e-3 + 7.49765e-11 * t + 6.9448e-5 *
            Math.cos(3.9820426e-7*t) + 17.6083e-5 * Math.sin(3.9820426e-7*t);
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            this.handleStep(currentState.getDate().minus(date), new double[] {0,0,0,currentState.getHx(), currentState.getHy()}, isLast);
        }

        public boolean requiresDenseOutput() {
            return false;
        }

        public void reset() {
        }

    }

    public void setUp() {
        IERSDataResetter.setUp("regular-data");
    }

    public void tearDown() {
        IERSDataResetter.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ThirdBodyAttractionTest.class);
    }
}
