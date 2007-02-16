package fr.cs.aerospace.orekit.propagation;

import java.io.Serializable;
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
import fr.cs.aerospace.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.aerospace.orekit.attitudes.models.IdentityAttitude;
import fr.cs.aerospace.orekit.errors.OrekitException;
import fr.cs.aerospace.orekit.errors.Translator;
import fr.cs.aerospace.orekit.forces.ForceModel;
import fr.cs.aerospace.orekit.forces.SWF;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.orbits.Orbit;
import fr.cs.aerospace.orekit.time.AbsoluteDate;

/** This class propagates a {@link fr.cs.aerospace.orekit.propagation.SpacecraftState}
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
 *  <dd>he will use {@link #propagate(SpacecraftState,AbsoluteDate)}</dd>
 *  <dt>if the user needs random access to the orbit state at any time between
 *      the initial and target times</dt>
 *  <dd>he will use {@link #propagate(SpacecraftState,AbsoluteDate,IntegratedEphemeris)} and
 *  {@link IntegratedEphemeris}</dd>
 *  <dt>if the user needs to do some action at regular time steps during
 *      integration</dt>
 *  <dd>he will use {@link #propagate(SpacecraftState,AbsoluteDate,double,FixedStepHandler)}</dd>
 *  <dt>if the user needs to do some action during integration but do not need
 *      specific time steps</dt>
 *  <dd>he will use {@link #propagate(SpacecraftState,AbsoluteDate,StepHandler)}</dd>
 * </dl></p> 
 *
 * <p>The two first methods are used when the user code needs to drive the
 * integration process, whereas the two last methods are used when the
 * integration process needs to drive the user code. To use the step handler, 
 * the user must know the format of the handled state used internally, 
 * which is expressed in equinoctial parameters : 
 *  <pre>
 *     y[0] = a
 *     y[1] = ex
 *     y[2] = ey
 *     y[3] = hx
 *     y[4] = hy
 *     y[5] = lv
 *     y[6] = mass (kg)
 *   </pre>
 * 
 * @see SpacecraftState
 * @see ForceModel
 * @see StepHandler
 * @see FixedStepHandler
 * @see IntegratedEphemeris
 * @see AttitudeKinematicsProvider
 * @see TimeDerivativesEquations
 *
 * @author  M. Romero
 * @author  L. Maisonobe
 * @author  G. Prat
 * @author  F. Maussion
 */
public class NumericalPropagator
implements FirstOrderDifferentialEquations, AttitudePropagator, Serializable {

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
    this.currentState       = null;
    this.adder              = null;
    this.akProvider         = new IdentityAttitude();
    this.state              = new double[getDimension()];
    this.swfException       = null;
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
  
  /** Sets the attitude provider of the propagator.
   * <p> If this method is never called before extrapolation, the attitude is 
   * set to default : {@link IdentityAttitude} <p> 
   * @param akProvider the attitude to propagate
   */
  public void setAkProvider(AttitudeKinematicsProvider akProvider) {
    this.akProvider = akProvider;
  }
  
  /** Propagate an orbit up to a specific target date.
   * @param initialState state to extrapolate 
   * @param finalDate target date for the orbit
   * @return the state at the final date 
   * @exception OrekitException if integration cannot be performed
   */
  public SpacecraftState propagate(SpacecraftState initialState,
                                   AbsoluteDate finalDate)
  throws OrekitException {
    return propagate(initialState, finalDate, DummyStepHandler.getInstance());
  }

  /** Propagate an orbit and store the ephemeris throughout the integration
   * range.
   * @param initialState the state to extrapolate 
   * @param finalDate target date for the orbit
   * @param ephemeris placeholder where to put the results
   * @return the state at the final date 
   * @exception OrekitException if integration cannot be performed
   */
  public SpacecraftState propagate(SpacecraftState initialState,
                                   AbsoluteDate finalDate,
                                   IntegratedEphemeris ephemeris) 
  throws OrekitException {    
    ContinuousOutputModel model = new ContinuousOutputModel();
    SpacecraftState finalState = propagate(initialState, finalDate, (StepHandler)model);
    ephemeris.initialize(model , initialState.getDate(), 
                         initialState.getParameters().getFrame(),
                         akProvider, mu);
    return finalState;
  }        

  /** Propagate an orbit and call a user handler at fixed time during
   * integration.
   * @param initialState the state to extrapolate
   * @param finalDate target date for the orbit
   * @param h fixed stepsize (s)
   * @param handler object to call at fixed time steps
   * @return the state at the final date 
   * @exception OrekitException if integration cannot be performed
   */     
  public SpacecraftState propagate(SpacecraftState initialState, AbsoluteDate finalDate,
                                   double h, FixedStepHandler handler)
  throws OrekitException {
    return propagate(initialState, finalDate, new StepNormalizer(h, handler));
  }

  /** Propagate an orbit and call a user handler after each successful step.
   * @param initialState the state to extrapolate
   * @param finalDate target date for the orbit
   * @param handler object to call at the end of each successful step
   * @return the {@link SpacecraftState} at the final date 
   * @exception OrekitException if integration cannot be performed
   */    
  public SpacecraftState propagate(SpacecraftState initialState,
                                   AbsoluteDate finalDate, StepHandler handler)
  throws OrekitException {

    if (initialState.getDate().equals(finalDate)) {
      // don't extrapolate
      return initialState;
    }

    // space dynamics view
    startDate  = initialState.getDate();
   
    EquinoctialParameters parameters = new EquinoctialParameters(initialState.getParameters(), mu);
    
    currentState = new SpacecraftState(
           new Orbit(initialState.getDate(), parameters), 
                initialState.getMass(), initialState.getAttitudeKinematics());
    
    adder      = new TimeDerivativesEquations(parameters , mu);

    if (initialState.getMass() <= 0.0) {
      throw new IllegalArgumentException("Mass is null or negative");
    }    

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
    state[6] = initialState.getMass();     

    // Add the switching functions
    for (Iterator iter = forceSwf.iterator(); iter.hasNext(); ) {
      SWF swf = (SWF)iter.next();
      integrator.addSwitchingFunction(new MappingSwitchingFunction(swf), 
                                      swf.getMaxCheckInterval(), swf.getThreshold());
    }

    // mathematical integration
    try {
      integrator.setStepHandler(handler);
      integrator.integrate(this, t0, state, t1, state);
    } catch(DerivativeException de) {
      if (swfException == null) {
        throw new OrekitException(de.getMessage(), de);
      }
    } catch(IntegratorException ie) {
      if (swfException == null) {
        throw new OrekitException(ie.getMessage(), ie);
      }
    }
    if (swfException != null) {
      throw swfException;
    }

    // back to space dynamics view
    AbsoluteDate date = new AbsoluteDate(startDate, t1);

    parameters = new EquinoctialParameters(state[0], state[1],state[2],state[3],
                                           state[4],state[5], EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                           parameters.getFrame());
    
    return new SpacecraftState(new Orbit(date , parameters), state[6], 
                                akProvider.getAttitudeKinematics(date,
                                 parameters.getPVCoordinates(mu), parameters.getFrame()));    
  }

  /** Gets the dimension of the handled state vecor (always 7). 
   * @return 7.
   */
  public int getDimension() {
    return 7;     
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
      if (swfException!=null) {
        throw swfException;
      }
      // update space dynamics view
      mapState(t, y);

      // compute cartesian coordinates
      if (currentState.getMass() <= 0.0) {
        String message = Translator.getInstance().translate("Mass is null or negative");
        throw new IllegalArgumentException(message);
      }    
      // initialize derivatives
      adder.initDerivatives(yDot , (EquinoctialParameters)currentState.getParameters());

      // compute the contributions of all perturbing forces
      for (Iterator iter = forceModels.iterator(); iter.hasNext();) {
        ((ForceModel) iter.next()).addContribution(currentState, adder, mu);
      }

      // finalize derivatives by adding the Kepler contribution
      adder.addKeplerContribution();
    } catch (OrekitException oe) {
      throw new DerivativeException(oe.getMessage(), new String[0]);
    }

  }
 
  /** Convert state array to space mecanics objects (AbsoluteDate and OrbitalParameters)
   * @param t integration time (s)
   * @param y state array
   * @throws OrekitException 
   */
  private void mapState(double t, double [] y) throws OrekitException {

    // update space dynamics view
    
    EquinoctialParameters currentParameters = new EquinoctialParameters(y[0], y[1],y[2],y[3],y[4],y[5],
                                                                        EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                                                        currentState.getFrame());
    AbsoluteDate currentDate = new AbsoluteDate(startDate, t);
    currentState = new SpacecraftState(new Orbit(currentDate, currentParameters), y[6], 
     akProvider.getAttitudeKinematics(currentDate, currentParameters.getPVCoordinates(mu), currentState.getFrame()));
  }

  
  /** Converts OREKIT switching functions to MANTISSA interface. */
  private class MappingSwitchingFunction implements SwitchingFunction {

    public MappingSwitchingFunction(SWF swf) {
      this.swf = swf;
    }

    public double g(double t, double[] y){      
      try {
        mapState(t, y);
        return swf.g(currentState, mu);
      } catch (OrekitException oe) {
        if (swfException==null) {
          swfException = oe;
        }
        return Double.NaN;
      }
    }

    public int eventOccurred(double t, double[] y) {
      try {
        mapState(t, y);
        swf.eventOccurred(currentState, mu);
      } catch (OrekitException oe) {
        if (swfException==null) {
          swfException = oe;
        }
      }
      return RESET_DERIVATIVES;
    }

    public void resetState(double t, double[] y) {
      // never called since eventOccured never returns CallResetState
    }

    private SWF swf;

    private static final long serialVersionUID = 2902410850401460548L;


  }
  
  /** Attitude provider */
  private AttitudeKinematicsProvider akProvider;
  
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

  /** Current state to propagate. */
  private SpacecraftState currentState;
  
  /** Integrator selected by the user for the orbital extrapolation process. */
  private FirstOrderIntegrator integrator;

  /** Gauss equations handler. */
  private TimeDerivativesEquations adder;

  /** Switching functions exception. */
  private OrekitException swfException;

  private static final long serialVersionUID = 5792536158612690051L;

}
