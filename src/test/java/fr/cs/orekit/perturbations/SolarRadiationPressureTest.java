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

import fr.cs.orekit.bodies.OneAxisEllipsoid;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.iers.IERSDataResetter;
import fr.cs.orekit.models.bodies.Sun;
import fr.cs.orekit.models.spacecraft.SolarRadiationPressureSpacecraft;
import fr.cs.orekit.models.spacecraft.SphericalSpacecraft;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.orbits.OrbitalParameters;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.analytical.KeplerianPropagator;
import fr.cs.orekit.propagation.numerical.NumericalPropagator;
import fr.cs.orekit.propagation.numerical.OrekitFixedStepHandler;
import fr.cs.orekit.propagation.numerical.forces.perturbations.SolarRadiationPressure;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;

public class SolarRadiationPressureTest extends TestCase {

    public void testLightning() throws OrekitException, ParseException, DerivativeException, IntegratorException{
        // Initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 3, 21),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
                                                         Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                                         0.1, 2, Frame.getJ2000());
        Orbit orbit = new Orbit(date , op);
        Sun sun = new Sun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765);
        SolarRadiationPressure SRP =  new SolarRadiationPressure(
                                                                 sun , earth.getEquatorialRadius() ,
                                                                 (SolarRadiationPressureSpacecraft)new SphericalSpacecraft(50.0,
                                                                                                                           0.5, 0.5, 0.5));

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);
        assertEquals(86164, period,1);

        // creation of the propagator
        KeplerianPropagator k = new KeplerianPropagator(new SpacecraftState(orbit, 1500.0), mu);

        // intermediate variables
        AbsoluteDate currentDate;
        double changed = 1;
        int count=0;

        for(int t=1;t<3*period;t+=1000) {
            currentDate = new AbsoluteDate(date , t);
            try {

                double ratio = SRP.getLightningRatio(k.getSpacecraftState(currentDate).getPVCoordinates(mu).getPosition(),Frame.getJ2000(), currentDate );

                if(Math.floor(ratio)!=changed) {
                    changed = Math.floor(ratio);
                    if(changed == 0) {
                        count++;
                    }
                }
            } catch (OrekitException e) {
                e.printStackTrace();
            }
        }
        assertTrue(3==count);
    }

    public void testRoughOrbitalModifs() throws ParseException, OrekitException, DerivativeException, IntegratorException, FileNotFoundException {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2000, 7, 1),
                                             new ChunkedTime(13, 59, 27.816),
                                             UTCScale.getInstance());
        OrbitalParameters op = new EquinoctialParameters(42164000,10e-3,10e-3,
                                                         Math.tan(0.001745329)*Math.cos(2*Math.PI/3), Math.tan(0.001745329)*Math.sin(2*Math.PI/3),
                                                         0.1, 2, Frame.getJ2000());
        Orbit orbit = new Orbit(date , op);
        Sun sun = new Sun();

        // creation of the force model
        SolarRadiationPressure SRP =  new SolarRadiationPressure(
                                                                 sun , new OneAxisEllipsoid(6378136.46, 1.0 / 298.25765).getEquatorialRadius(),
                                                                 (SolarRadiationPressureSpacecraft)new SphericalSpacecraft(500.0,
                                                                                                                           0.7, 0.7, 0.7));

        double period = 2*Math.PI*Math.sqrt(orbit.getA()*orbit.getA()*orbit.getA()/mu);

        assertEquals(86164, period,1);
        // creation of the propagator
        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, period/4, 0, 10e-4);
        NumericalPropagator calc = new NumericalPropagator(mu, integrator);
        calc.addForceModel(SRP);

        // Step Handler

        SolarStepHandler sh = new SolarStepHandler();
        AbsoluteDate finalDate = new AbsoluteDate(date , 90*period);
        calc.propagate(new SpacecraftState(orbit, 1500.0) , finalDate, Math.floor(15*period), sh );

    }

    public void checkRadius(double radius , double min , double max) {
        assertTrue(radius >= min);
        assertTrue(radius <= max);
    }

    private double mu = 3.98600E14;

    private class SolarStepHandler extends OrekitFixedStepHandler {

        private SolarStepHandler() {
        }

        public void handleStep(double t, double[]y, boolean isLastStep) {
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            double radius = Math.sqrt((currentState.getEx()-0.00940313)*(currentState.getEx()-0.00940313)
                                      + (currentState.getEy()-0.013679)*(currentState.getEy()-0.013679));
            checkRadius(radius , 0.00351 , 0.00394);
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
        return new TestSuite(SolarRadiationPressureTest.class);
    }

}
