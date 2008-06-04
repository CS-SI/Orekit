

import java.text.ParseException;

import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.GraggBulirschStoerIntegrator;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.KeplerianOrbit;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.IntegratedEphemeris;
import fr.cs.orekit.propagation.numerical.NumericalModel;
import fr.cs.orekit.propagation.numerical.OrekitFixedStepHandler;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;
import fr.cs.orekit.propagation.numerical.forces.perturbations.CunninghamAttractionModel;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.time.ChunkedDate;
import fr.cs.orekit.time.ChunkedTime;
import fr.cs.orekit.time.UTCScale;


/** The aim of this tutorial is to manipulate the Numerical propagator
 * and the force models.
 * @author F. Maussion
 */
public class NumericalPropagation {

    public static void numericalPropagation() throws ParseException, OrekitException {

        // physical constants :

        double mu =  3.9860064e+14; // gravitation coefficient
        double ae =  6378136.460; // equatorial radius in meter
        double c20 = -1.08262631303e-3; // J2 potential coefficent
        Frame itrf2000 = Frame.getReferenceFrame(Frame.ITRF2000B, new AbsoluteDate()); // terrestrial frame at an arbitrary date


        //  Initial state definition :

        // parameters :
        double a = 24396159; // semi major axis in meters
        double e = 0.72831215; // eccentricity
        double i = Math.toRadians(7); // inclination
        double omega = Math.toRadians(180); // perigee argument
        double raan = Math.toRadians(261); // right ascencion of ascending node
        double lv = 0; // mean anomaly

        double mass = 2500; // mass of the spacecraft in Kg

        // date and frame

        AbsoluteDate initialDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                    new ChunkedTime(23, 30, 00.000),
                                                    UTCScale.getInstance());

        Frame inertialFrame = Frame.getJ2000();

        // OREKIT objects construction:

        Orbit initialOrbit =
            new KeplerianOrbit(a, e, i, omega, raan, lv,
                                    KeplerianOrbit.MEAN_ANOMALY, inertialFrame, initialDate, mu);

        SpacecraftState initialState = new SpacecraftState(initialOrbit, mass);

        /* ***************** */
        /*   Extrapolation   */
        /* ***************** */

        // Integrator

        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8);
        // adaptive step integrator with a minimum step of 1 and a maximum step of 1000, and a relative precision of 1.0e-8

        NumericalModel propagator = new NumericalModel(mu, integrator);

        // Pertubative gravity field :

        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = c20;
        double[][] s = new double[3][1]; // potential coeffs arrays (only J2 is considered here)

        ForceModel cunningham = new CunninghamAttractionModel(itrf2000, ae, c, s);
        propagator.addForceModel(cunningham);

        // propagation with storage of the results in an integrated ephemeris

        AbsoluteDate finalDate = new AbsoluteDate(initialDate, 500);
        IntegratedEphemeris ephemeris = new IntegratedEphemeris();
        SpacecraftState finalState = propagator.propagate(initialState, finalDate, ephemeris);
        System.out.println(" Final state  : " +
                           finalState.getOrbit());
        AbsoluteDate intermediateDate = new AbsoluteDate(initialDate, 214);
        SpacecraftState intermediateState = ephemeris.propagate(intermediateDate);
        System.out.println("  intermediate state  :  " +
                           intermediateState.getOrbit());
    }

    public static void numericalPropagationWithStepHandler() throws ParseException, OrekitException {
//      physical constants :

        double mu =  3.9860064e+14; // gravitation coefficient
        double ae =  6378136.460; // equatorial radius in meter
        double c20 = -1.08262631303e-3; // J2 potential coefficent
        Frame itrf2000 = Frame.getReferenceFrame(Frame.ITRF2000B, new AbsoluteDate()); // terrestrial frame at an arbitrary date


        //  Initial state definition :

        // parameters :
        double a = 24396159; // semi major axis in meters
        double e = 0.72831215; // eccentricity
        double i = Math.toRadians(7); // inclination
        double omega = Math.toRadians(180); // perigee argument
        double raan = Math.toRadians(261); // right ascention of ascending node
        double lv = 0; // mean anomaly

        double mass = 2500; // mass of the spacecraft in Kg

        // date and frame

        AbsoluteDate initialDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                    new ChunkedTime(23, 30, 00.000),
                                                    UTCScale.getInstance());

        Frame inertialFrame = Frame.getJ2000();

        // OREKIT objects construction:

        Orbit initialOrbit =
            new KeplerianOrbit(a, e, i, omega, raan, lv,
                                    KeplerianOrbit.MEAN_ANOMALY, 
                                    inertialFrame, initialDate, mu);

        SpacecraftState initialState = new SpacecraftState(initialOrbit, ae);

        /* ***************** */
        /*   Extrapolation   */
        /* ***************** */

        // Integrator

        FirstOrderIntegrator integrator = new GraggBulirschStoerIntegrator(1, 1000, 0, 1.0e-8);
        // adaptive step integrator with a minimum step of 1 and a maximum step of 1000, and a relative precision of 1.0e-8

        NumericalModel propagator = new NumericalModel(mu, integrator);

        // Pertubative gravity field :

        double[][] c = new double[3][1];
        c[0][0] = 0.0;
        c[2][0] = c20;
        double[][] s = new double[3][1]; // potential coeffs arrays (only J2 is considered here)

        ForceModel cunningham = new CunninghamAttractionModel(itrf2000, mass, c, s);
        propagator.addForceModel(cunningham);
        AbsoluteDate finalDate = new AbsoluteDate(initialDate, 500);

        SpacecraftState finalState =
            propagator.propagate(initialState, finalDate, 100, new TutorialStepHandler());
        System.out.println(" Final state  : " +
                           finalState.getOrbit());
    }

    private static class TutorialStepHandler extends OrekitFixedStepHandler {

        /** Serializable UID. */
        private static final long serialVersionUID = 3684310055133053883L;

        private TutorialStepHandler() {
            //private constructor
        }

        public void handleStep(SpacecraftState currentState, boolean isLast) {
            System.out.println(" step time : " + currentState.getDate());
            System.out.println(" step state : " + currentState.getOrbit());
            if (isLast) {
                System.out.println(" this was the last step ");
            }

        }

        public boolean requiresDenseOutput() {
            return false;
        }

        public void reset() {
        }

    }


}
