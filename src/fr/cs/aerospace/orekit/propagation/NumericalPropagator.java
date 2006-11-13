package fr.cs.aerospace.orekit.propagation;

import java.util.ArrayList;
import java.util.Iterator;
import org.spaceroots.mantissa.ode.ContinuousOutputModel;
import org.spaceroots.mantissa.ode.FirstOrderIntegrator;
import org.spaceroots.mantissa.ode.FirstOrderDifferentialEquations;
import org.spaceroots.mantissa.ode.StepHandler;
import org.spaceroots.mantissa.ode.DummyStepHandler;
import org.spaceroots.mantissa.ode.FixedStepHandler;
import org.spaceroots.mantissa.ode.StepNormalizer;
import org.spaceroots.mantissa.ode.DerivativeException;
import org.spaceroots.mantissa.ode.IntegratorException;
import org.spaceroots.mantissa.ode.SwitchingFunction;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.time.AbsoluteDate;
import fr.cs.aerospace.orekit.utils.PVCoordinates;


/**
 * This class propagates an {@link fr.cs.aerospace.orekit.orbits.Orbit Orbit}
 * using numerical integration.
 *
 * <p>The user normally builds an extrapolator by specifying the integrator he
 * wants to use, then adding all the perturbing force models he wants and then
 * performing the given integration with an initial orbit and a target time. The same
 * extrapolator can be reused for several orbit extrapolations.</p>
 
 * <p>Several extrapolation methods are available, providing their results in
 * different ways to better suit user needs.
 * <dl>
 *  <dt>if the user needs only the orbit at the target time</dt>
 *  <dd>he will use {@link #propagate(Orbit,AbsoluteDate)}</dd>
 *  <dt>if the user needs random access to the orbit state at any time between
 *      the initial and target times</dt>
 *  <dd>he will use {@link #propagate(Orbit,AbsoluteDate,IntegratedEphemeris)} and
 *  {@link IntegratedEphemeris}</dd>
 *  <dt>if the user needs to do some action at regular time steps during
 *      integration</dt>
 *  <dd>he will use {@link #propagate(Orbit,AbsoluteDate,double,FixedStepHandler)}</dd>
 *  <dt>if the user needs to do some action during integration but do not need
 *      specific time steps</dt>
 *  <dd>he will use {@link #propagate(Orbit,AbsoluteDate,StepHandler)}</dd>
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
 * @see IntegratedEphemeris
 * @see fr.cs.aerospace.orekit.propagation.EquinoctialGaussEquations
 *
 * @version $Id$
 * @author  M. Romero
 * @author  L. Maisonobe
 * @author  G. Prat
 */
public class NumericalPropagator
implements FirstOrderDifferentialEquations {
  
  /** Create a new instance of NumericalExtrapolationModel.
   * After creation, the instance is empty, i.e. there is no perturbing force
   * at all. This means that if {@link #addForceModel addForceModel} is not
   * called after creation, the integrated orbit will follow a keplerian
   * evolution only.
   * @param mu central body gravitational constant (GM).
   * @param integrator numerical integrator to use for propagation.
   */
  public NumericalPropagator(double mu, FirstOrderIntegrator integrator) {
    this.mu                 = mu;
    this.forceModels        = new ArrayList();
    this.forceSwf           = new ArrayList();
    this.integrator         = integrator;
    this.startDate          = new AbsoluteDate();
    this.date               = new AbsoluteDate();
    this.parameters         = null;
    this.adder              = null;
    this.state = new double[6];
  }
  
  /** Add a force model to the global perturbation model. The associated 
   * switching function is added to the switching functions vector.
   * All models added by this method will be considered during integration.
   * If this method is not called at all, the integrated orbit will follow
   * a keplerian evolution only.
   * @param model perturbing {@link ForceModel} to add
   */
  public void addForceModel(ForceModel model) {
    forceModels.add(model);    
    SWF[] swf = model.getSwitchingFunctions();
    if (swf!=null) {
      for (int i = 0 ; i<swf.length ; i++) {
        forceSwf.add(swf[i]);      
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
    forceSwf.clear();
  }
  
  /** Propagate an orbit up to a specific target date.
   * @param initialOrbit orbit to extrapolate (this object will not be
   * changed except if finalOrbit is also reference to it)
   * @param finalDate target date for the orbit
   * @return orbit at the final date (reference to finalOrbit if it was non
   * null, reference to a new object otherwise)
   * @exception DerivativeException if the force models trigger one
   * @exception IntegratorException if the force models trigger one
   */
  public Orbit propagate(Orbit initialOrbit,
                         AbsoluteDate finalDate)
  throws DerivativeException, IntegratorException, OrekitException {
    
    propagate(initialOrbit, finalDate, DummyStepHandler.getInstance());
    
    return new Orbit(date , parameters);
  }
  
  /** Propagate an orbit and store the ephemeris throughout the integration
   * range.
   * @param initialOrbit orbit to extrapolate (this object will not be
   * changed)
   * @param finalDate target date for the orbit
   * @param ephemeris placeholder where to put the results
   * @exception DerivativeException if the force models trigger one
   * @exception IntegratorException if the force models trigger one
   */
  public void propagate(Orbit initialOrbit,
                        AbsoluteDate finalDate,
                        IntegratedEphemeris ephemeris) 
  throws DerivativeException, IntegratorException, OrekitException {    
    ContinuousOutputModel model = new ContinuousOutputModel();
    propagate(initialOrbit, finalDate, (StepHandler)model);
    ephemeris.initialize(model , initialOrbit.getDate(), 
                         initialOrbit.getParameters().getFrame());
  }        
  
  /** Propagate an orbit and call a user handler at fixed time during
   * integration.
   * @param initialOrbit orbit to extrapolate (this object will not be
   * changed)
   * @param finalDate target date for the orbit
   * @param h fixed stepsize (s)
   * @param handler object to call at fixed time steps
   * @exception DerivativeException if the force models trigger one
   * @exception IntegratorException if the force models trigger one
   */     
  public void propagate(Orbit initialOrbit, AbsoluteDate finalDate,
                        double h, FixedStepHandler handler)
  throws DerivativeException, IntegratorException, OrekitException {
    propagate(initialOrbit, finalDate, new StepNormalizer(h, handler));
  }
  
  /** Propagate an orbit and call a user handler after each successful step.
   * @param initialOrbit orbit to extrapolate (this object will not be
   * changed)
   * @param finalDate target date for the orbit
   * @param handler object to call at the end of each successful step
   * @exception DerivativeException if the force models trigger one
   * @exception IntegratorException if the force models trigger one
   */    
  public void propagate(Orbit initialOrbit,
                        AbsoluteDate finalDate, StepHandler handler)
  throws DerivativeException, IntegratorException, OrekitException {

    // space dynamics view
    startDate  = initialOrbit.getDate();
    date       = startDate;
    parameters = new EquinoctialParameters(initialOrbit.getParameters(), mu);
    adder      = new EquinoctialGaussEquations(parameters , mu);

    // mathematical view
    double t0 = 0;
    double t1 = finalDate.minus(startDate);
    
    // Map state to array
    state[0] = parameters.getA();
    state[1] = parameters.getEquinoctialEx();
    state[2] = parameters.getEquinoctialEy();
    state[3] = parameters.getHx();
    state[4] = parameters.getHy();
    state[5] = parameters.getLv();

    // Add the switching functions
    for( int i = 0; i < forceSwf.size(); i++) {
      SWF swf = (SWF)forceSwf.get(i);
      integrator.addSwitchingFunction(new MappingSwitchingFunction(swf), 
                                      swf.getMaxCheckInterval(), swf.getThreshold());
    }
    
    // mathematical integration
    integrator.setStepHandler(handler);
    integrator.integrate(this, t0, state, t1, state);
    
    // back to space dynamics view
    date = new AbsoluteDate(startDate, t1);
    
    parameters = new EquinoctialParameters(state[0], state[1],state[2],state[3],
                       state[4],state[5], EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                parameters.getFrame());
    
    
  }
  
  public int getDimension() {
    return 6;
  }
  
  /** Computes the orbit time derivative.
   * @param t current time offset from the reference epoch (s)
   * @param y array containing the current value of the orbit state vector
   * @param yDot placeholder array where to put the time derivative of the
   * orbit state vector
   * @exception DerivativeException this exception is propagated to the caller
   * if the underlying user function triggers one
   */
  public void computeDerivatives(double t, double[] y, double[] yDot)
  throws DerivativeException {
    
    try {
      // update space dynamics view
      mapState(t, y);
      
      // compute cartesian coordinates
      PVCoordinates pvCoordinates = parameters.getPVCoordinates(mu);
      
      // initialize derivatives
      adder.initDerivatives(yDot , parameters);
      
      // compute the contributions of all perturbing forces
      for (Iterator iter = forceModels.iterator(); iter.hasNext();) {
        ((ForceModel) iter.next()).addContribution(date, pvCoordinates, adder);
      }
      
      // finalize derivatives by adding the Kepler contribution
      adder.addKeplerContribution();
    } catch (OrekitException oe) {
      throw new DerivativeException(oe.getMessage(), new String[0]);
    }
    
  }
  
  private void mapState(double t, double [] y) {
    
    // update space dynamics view
    date = new AbsoluteDate(startDate, t);
    
    parameters = new EquinoctialParameters(y[0], y[1],y[2],y[3],y[4],y[5],
                                           EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                               parameters.getFrame());
    
  }
  
  private class MappingSwitchingFunction implements SwitchingFunction {
    
    public MappingSwitchingFunction(SWF swf) {
      this.swf = swf;
    }
    
    public double g(double t, double[] y){
      mapState(t, y);
      try {
        return swf.g(date, parameters.getPVCoordinates(mu), parameters.getFrame());
      } catch (OrekitException oe) {
        // TODO provide the exception to the surrounding NumericalPropagator instance
        throw new RuntimeException("... TODO ...");
      }
    }
    
    public int eventOccurred(double t, double[] y) {
      mapState(t, y);
      swf.eventOccurred(date, parameters.getPVCoordinates(mu), parameters.getFrame());
      return CONTINUE;
    }
    
    public void resetState(double t, double[] y) {
      // never called since eventOccured never returns CallResetState
    }
    
    private SWF swf;
    
  }
  
  /** Central body gravitational constant. */
  private double mu;
  
  /** Force models used during the extrapolation of the Orbit. */
  private ArrayList forceModels;
  
  /** Switching functions used during the extrapolation of the Orbit. */
  private ArrayList forceSwf;

  /** State vector */ 
  private double[] state;
  
  /** Start date. */
  private AbsoluteDate startDate;
  
  /** Current date. */
  private AbsoluteDate date;
  
  /** Current EquinoctialParameters, updated during the integration process. */
  private EquinoctialParameters parameters;
  
  /** Integrator selected by the user for the orbital extrapolation process. */
  private FirstOrderIntegrator integrator;
  
  /** Gauss equations handler. */
  private EquinoctialGaussEquations adder;
  
}
