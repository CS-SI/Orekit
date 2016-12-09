package fr.cs.examples.propagation;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UncorrelatedRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.stat.descriptive.DescriptiveStatistics;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.FieldOrekitFixedStepHandler;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

import fr.cs.examples.Autoconfiguration;

/** Orekit tutorial for field mode propagation.
 * <p>This tutorial shows the interest of the field propagation in particular focusing
 *  on the utilisation of the DerivativeStructure in Orekit.<p>
 * @author Andrea Antolino
 */

public class FieldPropagation {
    /** Program entry point.
     * @param args program arguments (unused here)
     * @throws IOException
     * @throws OrekitException
     */
    public static void main(String[] args) throws IOException, OrekitException {

        /*the goal of this example is to make a Montecarlo simulation giving an error on the semiaxis,
          the inclination and the RAAN. The interest of doing it with Orekit based on the
          DerivativeStructure is that instead of doing a large number of propagation around the initial
          point we will do a single propagation of the initial state, and thanks to the Taylor expansion
          we will see the evolution of the std deviation of the position, which is divided in the
           CrossTrack, the LongTrack and the Radial error.
        */
        // setting orekit

        Autoconfiguration.configureOrekit();

        //setting some filewriter and printwriter to have the output
        String workingDir = System.getProperty("user.dir");
        System.out.println("Output file is in : " + workingDir + "/error.txt");
        FileWriter  FW = new FileWriter(workingDir + "/error.txt", false);
        PrintWriter PW = new PrintWriter(FW);

        PW.printf("time \t\tCrossTrackErr \tLongTrackErr  \tRadialErr \tTotalErr%n");

        //setting the parameters of the simulation
        //Order of derivation of the DerivativeStructures
        int ord = 3;

        //number of samples of the montecarlo simulation
        int montecarlo_size = 100;

        //nominal values of the Orbital parameters
        double a_nominal = 7.278E6;
        double e_nominal = 1e-3;
        double i_nominal = FastMath.toRadians(98.3);
        double pa_nominal = FastMath.PI / 2;
        double raan_nominal = 0.0 ;
        double ni_nominal = 0.0 ;

        //mean of the gaussian curve for each of the errors around the nominal values
        //{a,i,RAAN}
        double[] mean = {0 , 0, 0};

        //standard deviation of the gaussian curve for each of the errors around the nominal values
        //{dA,dI, dRaan}
        double[] dAdIdRaan = {5 , FastMath.toRadians(1e-3), FastMath.toRadians(1e-3)};

        //time of integration
        double final_Dt = 1 * 60 * 60;
        //number of steps per orbit
        double num_step_orbit = 10;


        DerivativeStructure a_0 = new DerivativeStructure(3, ord, 0, a_nominal );
        DerivativeStructure e_0 = new DerivativeStructure(3, ord, e_nominal );
        DerivativeStructure i_0 = new DerivativeStructure(3, ord, 1, i_nominal );
        DerivativeStructure pa_0 = new DerivativeStructure(3, ord, pa_nominal) ;
        DerivativeStructure raan_0 = new DerivativeStructure(3, ord, 2, raan_nominal);
        DerivativeStructure ni_0 = new DerivativeStructure(3, ord, ni_nominal );

        //sometimes we will need the field of the DerivativeStructure to build new instances
        Field<DerivativeStructure> field = a_0.getField();
        //sometimes we will need the zero of the DerivativeStructure to build new instances
        DerivativeStructure zero = field.getZero();

        //initializing the FieldAbsoluteDate with only the field it will generate the day J2000
        FieldAbsoluteDate<DerivativeStructure> date_0 = new FieldAbsoluteDate<DerivativeStructure>(field);

        //initialize a basic frame
        Frame frame = FramesFactory.getEME2000();

        //initialize the orbit
        double mu = 3.9860047e14;

        FieldKeplerianOrbit<DerivativeStructure> KO = new FieldKeplerianOrbit<DerivativeStructure>(a_0, e_0, i_0, pa_0, raan_0, ni_0, PositionAngle.ECCENTRIC, frame, date_0, mu);

        //step of integration (how many times per orbit we take the mesures)
        double int_step = KO.getKeplerianPeriod().getReal() / num_step_orbit;


        //random generator to conduct an
        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(mean, dAdIdRaan, NGG);
        double[][] rand_gen = new double[montecarlo_size][3];
        for (int jj = 0; jj < montecarlo_size; jj++){
                rand_gen[jj] = URVG.nextVector();
        }
        //
        FieldSpacecraftState<DerivativeStructure> SS_0 = new FieldSpacecraftState<DerivativeStructure>(KO);
        //adding force models
        ForceModel fModel_Sun  = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
        ForceModel fModel_Moon = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
        ForceModel fModel_HFAM =
                        new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                              GravityFieldFactory.getNormalizedProvider(18, 18));

        //setting an hipparchus field integrator
        double[][] tolerance = NumericalPropagator.tolerances(0.001, KO.toOrbit(), OrbitType.CARTESIAN);
        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<DerivativeStructure>(field, 0.001, 200, tolerance[0], tolerance[1]);

        integrator.setInitialStepSize(zero.add(60));

        //setting of the field propagator, we used the numerical one in order to add the third body attraction
        //and the holmes featherstone force models
        FieldNumericalPropagator<DerivativeStructure> numProp = new FieldNumericalPropagator<DerivativeStructure>(field, integrator);

        numProp.setInitialState(SS_0);
        numProp.addForceModel(fModel_Sun);
        numProp.addForceModel(fModel_Moon);
        numProp.addForceModel(fModel_HFAM);
        //with the master mode we will calulcate and print the error on every fixed step on the file error.txt
        //we defined the StepHandler to do that giving him the random number generator,
        //the size of the montecarlo simulation and the initial date
        numProp.setMasterMode(zero.add(int_step),
                              new MyStepHandler<DerivativeStructure>(rand_gen, montecarlo_size, date_0,
                                              PW));
//
        long START = System.nanoTime();

        FieldSpacecraftState<DerivativeStructure> finalState = numProp.propagate(date_0.shiftedBy(final_Dt));

        long STOP = System.nanoTime();

        System.out.println((STOP - START)/ 1E6 + " ms");
        System.out.println(finalState.getDate());

        PW.close();
    }


    private static class MyStepHandler<T extends RealFieldElement<T>>
        implements FieldOrekitFixedStepHandler<T>{

        PrintWriter PW;
        double[][] rand_gen;
        FieldAbsoluteDate<T> data_0;
        int montecarlo_size;

        public MyStepHandler(double[][] rand_gen, int MC_size, FieldAbsoluteDate<T> initial_data,
                             PrintWriter PW){
            this.PW   = PW  ;
            this.rand_gen = rand_gen;
            this.montecarlo_size = MC_size;
            this.data_0 = initial_data;
        }

        @Override
        public void handleStep(FieldSpacecraftState<T> currentState,
                               boolean isLast)
            throws OrekitException {
            TimeStampedFieldPVCoordinates<DerivativeStructure> PV_t = (TimeStampedFieldPVCoordinates<DerivativeStructure>) currentState.getPVCoordinates();

            //getting the propagated poisition and velocity(to find the cross track and long track error)
            FieldVector3D<DerivativeStructure> P_t = PV_t.getPosition();
            FieldVector3D<DerivativeStructure> V_t = PV_t.getVelocity().normalize();
            FieldVector3D<DerivativeStructure> M_t = PV_t.getMomentum().normalize();
            FieldVector3D<DerivativeStructure> N_t = FieldVector3D.crossProduct(V_t, M_t);

            DerivativeStructure x_t = P_t.getX();
            DerivativeStructure y_t = P_t.getY();
            DerivativeStructure z_t = P_t.getZ();
            DescriptiveStatistics stat_CT = new DescriptiveStatistics();
            DescriptiveStatistics stat_LT = new DescriptiveStatistics();
            DescriptiveStatistics stat_R = new DescriptiveStatistics();
            DescriptiveStatistics stat_dist = new DescriptiveStatistics();

            for (int jj = 0; jj < montecarlo_size; jj++){
                //Generation of the random error around the nominal values
                double da = rand_gen[jj][0];
                double di = rand_gen[jj][1];
                double dRAAN = rand_gen[jj][2];
                //evaluating thanks to taylor the propagation of the nominal values with error
                // x_e = f(a-n,i-n, raan-n) + df/da(a-n,i-n, raan-n) * da + df/di(a-n,i-n, raan-n) * di + ... etc.
                //TAYLOR'S EXPANSION
                double x_e = x_t.taylor(da,di,dRAAN);
                double y_e = y_t.taylor(da,di,dRAAN);
                double z_e = z_t.taylor(da,di,dRAAN);

                Vector3D P_e = new Vector3D(x_e, y_e, z_e);

                stat_CT.addValue(Vector3D.dotProduct(P_e.subtract(P_t.toVector3D()), M_t.toVector3D()));
                stat_LT.addValue(Vector3D.dotProduct(P_e.subtract(P_t.toVector3D()), V_t.toVector3D()));
                stat_R.addValue(Vector3D.dotProduct(P_e.subtract(P_t.toVector3D()), N_t.toVector3D()));
                stat_dist.addValue(P_e.subtract(P_t.toVector3D()).getNorm());
            }

            //printing all the standard deviations on the file error.txt
            Locale.setDefault(new Locale("en", "US"));
            PW.printf("%f \t", currentState.getDate().durationFrom(data_0).getReal()/3600);
            PW.printf("%f \t", stat_CT.getStandardDeviation());
            PW.printf("%f \t", stat_LT.getStandardDeviation());
            PW.printf("%f \t", stat_R.getStandardDeviation());
            PW.printf("%f \n", stat_dist.getStandardDeviation());

        }



    }

}
