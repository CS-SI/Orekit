package fr.cs.aerospace.orekit.extrapolation;

import java.util.Vector;
import java.util.Iterator;

import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FirstOrderDifferentialEquations;
import org.spaceroots.mantissa.ode.StepHandler;
import org.spaceroots.mantissa.ode.DummyStepHandler;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.StepNormalizer;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.IntegratorException;
import org.spaceroots.mantissa.ode.SwitchingFunction;
import org.spaceroots.mantissa.utilities.ArrayMapper;
import org.spaceroots.mantissa.geometry.Vector3D;
import org.spaceroots.mantissa.geometry.Rotation;
import org.spaceroots.mantissa.utilities.ArraySliceMappable;

import fr.cs.aerospace.orekit.RDate;
import fr.cs.aerospace.orekit.Orbit;
import fr.cs.aerospace.orekit.OrbitalParameters;
import fr.cs.aerospace.orekit.OrbitDerivativesAdder;
import fr.cs.aerospace.orekit.perturbations.ForceModel;
import fr.cs.aerospace.orekit.perturbations.SWF;
import fr.cs.aerospace.orekit.Attitude;
import fr.cs.aerospace.orekit.Vehicle;
import fr.cs.aerospace.orekit.OrekitException;


import org.spaceroots.mantissa.ode.DormandPrince853Integrator;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.IntegratorException;
import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.perturbations.ForceModel;
import fr.cs.aerospace.orekit.perturbations.CentralBodyPotential;
import fr.cs.aerospace.orekit.perturbations.DrozinerPotentialModel;
import fr.cs.aerospace.orekit.perturbations.CunninghamPotentialModel;
import fr.cs.aerospace.orekit.perturbations.PotentialCoefficientsTab;
import fr.cs.aerospace.orekit.perturbations.Drag;
import fr.cs.aerospace.orekit.perturbations.SolarRadiationPressure;
import fr.cs.aerospace.orekit.Constants;
import fr.cs.aerospace.orekit.Atmosphere;

/**
 * This class extrapolates an {@link fr.cs.aerospace.orekit.Orbit Orbit}
 * using numerical integration.
 *
 * <p>The user normally build an extrapolator by specifying the integrator he
 * wants to use, then adding all the perturbing force models he wants and then
 * performing the integration given an initial orbit and a target time. The same
 * extrapolator can be reused for several orbit extrapolations.</p>
 
 * <p>Several extrapolation methods are available, providing their results in
 * different ways to better suit user needs.
 * <dl>
 *  <dt>if the user needs only the orbit at the target time</dt>
 *  <dd>he will use {@link #extrapolate(Orbit,RDate,Orbit)}</dd>
 *  <dt>if the user needs random access to the orbit state at any time between
 *      the initial and target times</dt>
 *  <dd>he will use {@link #extrapolate(Orbit,RDate,IntegratedEphemeris)}</dd>
 *  <dt>if the user needs to do some action at regular time steps during
 *      integration</dt>
 *  <dd>he will use {@link #extrapolate(Orbit,RDate,double,FixedStepHandler)}</dd>
 *  <dt>if the user needs to do some action during integration but do not need
 *      specific time steps</dt>
 *  <dd>he will use {@link #extrapolate(Orbit,RDate,StepHandler)}</dd>
 * </dl></p>
 *
 * <p>The two first methods are used when the user code needs to drive the
 * integration process, whereas the two last methods are used when the
 * integration process needs to drive the user code.
 *
 * @see Orbit
 * @see ForceModel
 * @see StepHandler
 * @see FixedStepHandler
 *
 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 */
public class NumericalExtrapolator
  implements FirstOrderDifferentialEquations {

    /** Central body gravitational constant. */
    private double mu;
    
    /** Force models used during the extrapolation of the Orbit. */
    private Vector forceModels;
    
    /** Switching functions used during the extrapolation of the Orbit. */
    private Vector switchingFunctions;

    /** threshold associated to switching functions. */
    private double[] thresholds;
    
    /** Maximal time intervals between switching function checks. */
    private double[] maxCheckIntervals;
    
    /** Current date. */
    private RDate date;

    /** Current orbital parameters, updated during the integration process. */
    private OrbitalParameters parameters;
    
    /** Current attitude. */
    private Attitude Attitude;
    
    /** Mapper between the orbit domain object and flat state array. */
    private ArrayMapper mapper;
    
    /** Integrator selected by the user for the orbital extrapolation process. */
    private FirstOrderIntegrator integrator;

    /** Gauss equations handler. */
    private OrbitDerivativesAdder adder;
    

    private void mapState(double t, double [] y) {
        
        // update space dynamics view
        date.setOffset(t);
        mapper.updateObjects(y);
        
        parameters.mapStateFromArray(0,y);
        
    }
        
    private class MappingSwitchingFunction implements SwitchingFunction {
        
        private SWF swf;
        
        public MappingSwitchingFunction(SWF swf) {
            this.swf = swf;
        }
        
        public double g(double t, double[] y){
            mapState(t, y);
            //return swf.g(date,parameters.getPosition(mu),parameters.getVelocity(mu));
            double var = 0.0;
            try {
            var = swf.g(date,parameters.getPosition(mu),parameters.getVelocity(mu));
            } catch (OrekitException oe) {System.out.println("pb");}
            return var;
        }
        
        public int eventOccurred(double t, double[] y) {
            mapState(t, y);
            swf.eventOccurred(date,parameters.getPosition(mu),parameters.getVelocity(mu));
            return CONTINUE;
        }
        
        public void resetState(double t, double[] y) {
        }
        
        public void setSwf(SWF swf) {
            swf = swf;
        }
       
    }
    
    /** Create a new instance of NumericalExtrapolationModel.
     * After creation, the instance is empty, i.e. there is no perturbing force
     * at all. This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param mu central body gravitational constant (GM).
     * @param integrator numerical integrator to use for extrapolation.
     */
    public NumericalExtrapolator(double mu, FirstOrderIntegrator integrator) {
      this.mu                 = mu;
      this.forceModels        = new Vector();
      this.switchingFunctions = new Vector();
      this.integrator         = integrator;
      this.date               = new RDate();
      this.parameters         = null;
      this.Attitude           = new Attitude();
      this.mapper             = null;
      this.adder              = null;
    }

    /** Add a force model to the global perturbation model. The associated 
     * switching function is added to the switching functions vector.
     * All models added by this method will be considered during integration.
     * If this method is not called at all, the integrated orbit will follow
     * a keplerian evolution only.
     * @param model perturbing force model to add
     */
    public void addForceModel(ForceModel model) {
      forceModels.addElement(model);
      SWF[] tab =  model.getSwitchingFunctions();
      
      if (tab != null) {
          MappingSwitchingFunction[] mappingSwitchingFunction = new MappingSwitchingFunction[tab.length];
          thresholds = new double[tab.length];
          maxCheckIntervals = new double[tab.length];
          for (int i = 0; i < tab.length; i++) {
            mappingSwitchingFunction[i] = new MappingSwitchingFunction(tab[i]);
            switchingFunctions.addElement(mappingSwitchingFunction[i]);
            thresholds[i] = tab[i].getThreshold();
            maxCheckIntervals[i] = tab[i].getMaxCheckInterval();
         }
      }
    }

    /** Remove all perturbing force models from the global perturbation model, 
     * and associated switching functions.
     * Once all perturbing forces have been removed (and as long as no new force
     * model is added), the integrated orbit will follow a keplerian evolution
     * only.
     */
    public void removeForceModels() {
      forceModels.clear();
      switchingFunctions.clear();
    }

    /** Extrapolate an orbit up to a specific target date.
     * @param initialOrbit orbit to extrapolate (this object will not be
     * changed except if finalOrbit is also reference to it)
     * @param finalDate target date for the orbit
     * @param finalOrbit placeholder where to put the final orbit (may be a
     * reference to initialOrbit and may be null, as long as null is cast to
     *
     * (Orbit) to avoid ambiguities with the other extrapolation methods)
     * @return orbit at the final date (reference to finalOrbit if it was non
     * null, reference to a new object otherwise)
     * @exception DerivativeException if the force models trigger one
     * @exception IntegratorException if the force models trigger one
     */
    public Orbit extrapolate(Orbit initialOrbit,
                             RDate finalDate, Orbit finalOrbit)
      throws DerivativeException, IntegratorException, OrekitException {

     extrapolate(initialOrbit, finalDate, DummyStepHandler.getInstance());
////TEMP
//        ContinuousOutputModel COM = new ContinuousOutputModel();
//        extrapolate(initialOrbit, finalDate, COM);
//        double[] statevect;
//        for (int i=0;i<100;i++) {
//        COM.setInterpolatedTime(initialOrbit.getDate().getOffset()+i * (finalDate.getOffset()-initialOrbit.getDate().getOffset())/100);
//        int size = COM.getInterpolatedState().length;
//        statevect = new double[size];
//        statevect = COM.getInterpolatedState();
//        System.out.println(statevect[0]+"\t\t\t"+statevect[1]+"\t\t\t"+
//        statevect[2]+"\t\t\t"+statevect[3]+"\t\t\t"+statevect[4]+"\t\t\t"+
//        statevect[5]+"\t\t\t"+ (i*(finalDate.getOffset()-initialOrbit.getDate().getOffset())/100));
//        }
////TEMP  
        if (finalOrbit == null) {
          finalOrbit = new Orbit(new RDate(date),
                                 (OrbitalParameters) parameters.clone());
        } 
        else {
          finalOrbit.reset(date, parameters);
        }
        return finalOrbit;
    }
    
    /** Extrapolate an orbit and store the ephemeris throughout the integration
     * range.
     * @param initialOrbit orbit to extrapolate (this object will not be
     * changed)
     * @param finalDate target date for the orbit
     * @param ephemeris placeholder where to put the ephemeris (may be null, as
     * long as null is cast to (IntegratedEphemeris) to avoid ambiguities with
     * the other extrapolation methods)
     * @return integrated ephemeris (reference to ephemeris if it was non null,
     * reference to a new object otherwise)
     * @exception DerivativeException if the force models trigger one
     * @exception IntegratorException if the force models trigger one
     */
    public IntegratedEphemeris extrapolate(Orbit initialOrbit,
                                           RDate finalDate,
                                           IntegratedEphemeris ephemeris) 
        throws DerivativeException, IntegratorException, OrekitException {
        
        if (ephemeris == null) {
          ephemeris = new IntegratedEphemeris();
        }

        extrapolate(initialOrbit, finalDate, ephemeris.getModel());

        ephemeris.setDates(date.getEpoch());
        return ephemeris;

    }        

    /** Extrapolate an orbit and call a user object at fixed time during
     * integration.
     * @param initialOrbit orbit to extrapolate (this object will not be
     * changed)
     * @param finalDate target date for the orbit
     * @param h fixed stepsize (s)
     * @param handler object to call at fixed time steps
     * @exception DerivativeException if the force models trigger one
     * @exception IntegratorException if the force models trigger one
     */     
    public void extrapolate(Orbit initialOrbit, RDate finalDate,
                            double h, FixedStepHandler handler)
      throws DerivativeException, IntegratorException, OrekitException {

        extrapolate(initialOrbit, finalDate, new StepNormalizer(h, handler));

    }

    /** Extrapolate an orbit and call Ca user object after each successful step.
     * @param initialOrbit orbit to extrapolate (this object will not be
     * changed)
     * @param finalDate target date for the orbit
     * @param handler object to call at the end of each successful step
     * @exception DerivativeException if the force models trigger one
     * @exception IntegratorException if the force models trigger one
     */    
    public void extrapolate(Orbit initialOrbit,
                            RDate finalDate, StepHandler handler)
      throws DerivativeException, IntegratorException, OrekitException {

        // space dynamics view
        date.reset(initialOrbit.getDate());

        // try to avoid building new objects if possible
        if (parameters != null) {
          try {
            parameters.reset(initialOrbit.getParameters());
          } 
          catch (ClassCastException cce) {
            parameters = null;
          }
        }
        if (parameters == null) {
          parameters = (OrbitalParameters) initialOrbit.getParameters().clone();
          mapper     = new ArrayMapper(parameters);
          adder      = parameters.getDerivativesAdder(mu);
        }

        // mathematical view
        double t0 = date.getOffset();
        double t1 = t0 + finalDate.minus(date);
        mapper.updateArray();
        
        for( int i = 0; i < switchingFunctions.size(); i++) {
        integrator.addSwitchingFunction(
        (MappingSwitchingFunction)switchingFunctions.elementAt(i), 
        maxCheckIntervals[i], thresholds[i]);
        }
        
        // mathematical integration
        integrator.setStepHandler(handler);
        integrator.integrate(this, t0, mapper.getInternalDataArray(),
                             t1, mapper.getInternalDataArray());

        // back to space dynamics view
        date.setOffset(t1);
        mapper.updateObjects();
        

    }

     public int getDimension() {
      return parameters.getStateDimension();
    }

    /** Computes the orbit time derivative.
     * @param t current time offset from the reference epoch (s)
     * @param y array containing the current value of the orbit state vector
     * @param yDot placeholder array where to put the time derivative of the
     * orbit state vector
     * @exception DerivativeException this exception is propagated to the caller
     * if the underlying user function triggers one
     */
    public void computeDerivatives(double t, double[] y, double[] yDot) throws DerivativeException {

        // update space dynamics view
        mapState(t, y);
        
        // compute cartesian coordinates
        Vector3D position = parameters.getPosition(mu);
        Vector3D velocity = parameters.getVelocity(mu);
        if ((Math.abs(position.getNorm())< Constants.CentralBodyradius)|| (Float.isNaN((float)position.getNorm()))) 
        {throw new DerivativeException("Vehicle crashed down");}
        
        // initialize derivatives

        adder.initDerivatives(yDot);
        
        // compute the contributions of all perturbing forces
        for(Iterator iter = forceModels.iterator(); iter.hasNext(); ) {
            try {
            ((ForceModel)iter.next()).addContribution(date, position, velocity, 
                                                      Attitude, adder);
            }
            catch (OrekitException oe) {
                System.err.println(oe.getMessage());
                throw new DerivativeException();
            }

        }
        
        // finalize derivatives by adding the Kepler contribution
        adder.addKeplerContribution();
        
    }


    public static void main(String args[])
    throws DerivativeException, IntegratorException, OrekitException {
    // Definition of initial conditions
    // --------------------------------
    double mu = 3.986e14;
    double equatorialRadius = 6378.13E3;
   // Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
   // Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
   // Vector3D position = new Vector3D(10.0e6, 0.0, 0.0);
   // Vector3D velocity = new Vector3D(0.0, 8000.0, 0.0);
    Vector3D position = new Vector3D(8.0e6, 100000.0, 0.0);
    Vector3D velocity = new Vector3D(0.0, 7058.0, 0.0);
    
    Orbit initialOrbit = new Orbit(new RDate(RDate.J2000Epoch, 0.0),position, 
                                   velocity, mu);
     
    System.out.println("Initial orbit at t = " + initialOrbit.getDate());
    System.out.println("a = " + initialOrbit.getA());
    System.out.println("e = " + initialOrbit.getE());
    System.out.println("i = " + initialOrbit.getI());
    System.out.println("manomaly = " + initialOrbit.getMeanAnomaly());
    System.out.println("raan = " + initialOrbit.getRAAN());
    System.out.println("pa = " + initialOrbit.getPA());
    System.out.println("x = " + initialOrbit.getPosition(mu).getX());
    System.out.println("y = " + initialOrbit.getPosition(mu).getY());
    System.out.println("z = " + initialOrbit.getPosition(mu).getZ());
    
    
    // Extrapolator definition
    // -----------------------
    NumericalExtrapolator extrapolator =
      new NumericalExtrapolator(mu, new DormandPrince853Integrator(0.0, 10000.0,
                                1.0e-8, 1.0e-8));
    double dt = 3600*10;

    // Extrapolation of the initial at t+dt
    // ------------------------------------

    // Add drag force
    //===============================================================
   // Drag drag = new Drag(0.0000001, 1745898.0, 1.0);
   // Drag drag = new Drag(0.0002, 40000.0, 7500.0);
   // Drag drag = new Drag(2.0E76, 40000.0, 7500.0);
   // Drag drag = new Drag(9.0E76, 40000.0, 7500.0);
   // Drag drag = new Drag(9.0E199, 40000.0, 7500.0);
   // extrapolator.addForceModel(drag);

    // Add potential force
    //===============================================================
//    PotentialCoefficientsTab GEM10Tab = 
//    new PotentialCoefficientsTab("D:\\Mes Documents\\EDelente\\JAVA\\GEM10B.txt");
//    
//    GEM10Tab.read();
//    int ndeg = GEM10Tab.getNdeg();
//    double[] J   = new double[ndeg];
//    double[][] C = new double[ndeg][ndeg];
//    double[][] S = new double[ndeg][ndeg];
//    
//    C = GEM10Tab.getNormalizedClm();
//    S = GEM10Tab.getNormalizedSlm();
//
//    J[0] = 0.0;
//    J[1] = 0.0;
//    for (int i = 2; i < ndeg; i++) {
//        J[i] = - C[i][0];
//    }

     double[] J = new double[2+1];
     double[][] C = new double[2+1][2+1];
     double[][] S = new double[2+1][2+1];
     for (int i = 0; i<=2; i++) {
        J[i] = 0.0;
         for (int j = 0; j<=2; j++) {
         C[i][j] = 0.0;
         S[i][j] = 0.0;
         }
    }
     
    J[0]=0.0;
    J[1]=0.0;
    J[2] = 1.08E-3;
    
    C[0][0]=0.0;
    C[1][0]=0.0;
    C[1][1]=0.0;
    C[2][0] = -1.08E-3;
    C[2][1] = 1.342634E-9;
    C[2][2] = 1.571166E-6;
    
    S[0][0]=0.0;
    S[1][0]=0.0;
    S[1][1]=0.0;
    S[2][0] = 0.0;
    S[2][1] = -3.137116E-9;
    S[2][2] = -9.030958E-7;
    
    
//    CunninghamPotentialModel CBP = new CunninghamPotentialModel("cbp", mu,
//                                equatorialRadius, J, C, S);
//    extrapolator.addForceModel(CBP);

//    DrozinerPotentialModel CBP2 = new DrozinerPotentialModel("cbp2", mu,
//                                 equatorialRadius, J, C, S);
//    extrapolator.addForceModel(CBP2);

    
    // Add SRP force
    //===============================================================
    SolarRadiationPressure SRP = new SolarRadiationPressure();
    extrapolator.addForceModel(SRP);
    
    //==================================================================
    
    RDate date2 = new RDate(initialOrbit.getDate(),dt);
    Orbit finalOrbit = extrapolator.extrapolate(initialOrbit, date2, (Orbit) null);
        
    System.out.println("==========================================");
    System.out.println("Final orbit at t = " + finalOrbit.getDate());
    System.out.println("a = " + finalOrbit.getA());
    System.out.println("e = " + finalOrbit.getE());
    System.out.println("i = " + finalOrbit.getI());
    System.out.println("manomaly = " + finalOrbit.getMeanAnomaly());
    System.out.println("raan = " + finalOrbit.getRAAN());
    System.out.println("pa = " + finalOrbit.getPA());
    System.out.println("x = " + finalOrbit.getPosition(mu).getX());
    System.out.println("y = " + finalOrbit.getPosition(mu).getY());
    System.out.println("z = " + finalOrbit.getPosition(mu).getZ());

  }

}
