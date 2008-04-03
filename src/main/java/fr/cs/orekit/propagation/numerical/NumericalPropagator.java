package fr.cs.orekit.propagation.numerical;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.ode.ContinuousOutputModel;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.DummyStepHandler;
import org.apache.commons.math.ode.StepNormalizer;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.SwitchingFunction;
import fr.cs.orekit.attitudes.AttitudeKinematicsProvider;
import fr.cs.orekit.attitudes.models.IdentityAttitude;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.orbits.Orbit;
import fr.cs.orekit.propagation.AttitudePropagator;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;
import fr.cs.orekit.time.AbsoluteDate;

/** This class propagates a {@link fr.cs.orekit.propagation.SpacecraftState}
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
 *  <dd>he will use {@link #propagate(SpacecraftState,AbsoluteDate,double,OrekitFixedStepHandler)}</dd>
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
 * @see OrekitFixedStepHandler
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

    /** Serializable UID. */
    private static final long serialVersionUID = -7625270037622308823L;

    /** Attitude provider */
    private AttitudeKinematicsProvider akProvider;

    /** Central body gravitational constant. */
    private final double mu;

    /** Force models used during the extrapolation of the Orbit. */
    private final List forceModels;

    /** Switching functions used during the extrapolation of the Orbit. */
    private final List forceSwf;

    /** State vector */
    private final double[] state;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Current state to propagate. */
    private SpacecraftState currentState;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final FirstOrderIntegrator integrator;

    /** Gauss equations handler. */
    private TimeDerivativesEquations adder;

    /** Switching functions exception. */
    private OrekitException swfException;

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
        final OrekitSwitchingFunction[] swf = model.getSwitchingFunctions();
        if (swf != null) {
            for (int i = 0 ; i < swf.length ; i++) {
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
                                     AbsoluteDate finalDate) throws OrekitException {
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
        final ContinuousOutputModel model = new ContinuousOutputModel();
        final SpacecraftState finalState =
            propagate(initialState, finalDate, (StepHandler)model);
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
                                     double h, OrekitFixedStepHandler handler)
        throws OrekitException {
        handler.initialize(initialState.getDate(), initialState.getFrame(), mu, akProvider);
        return propagate(initialState, finalDate, new StepNormalizer(h, handler));
    }

    /** Propagate an orbit and call a user handler at each step end.
     * @param initialState the state to extrapolate
     * @param finalDate target date for the orbit
     * @param handler object to call at fixed time steps
     * @return the state at the final date
     * @exception OrekitException if integration cannot be performed
     */
    public SpacecraftState propagate(SpacecraftState initialState, AbsoluteDate finalDate,
                                     OrekitStepHandler handler)
        throws OrekitException {
        handler.initialize(initialState.getDate(), initialState.getFrame(), mu, akProvider);
        return propagate(initialState, finalDate, handler);
    }


    /** Propagate an orbit and call a user handler after each successful step.
     * @param initialState the state to extrapolate
     * @param finalDate target date for the orbit
     * @param handler object to call at the end of each successful step
     * @return the {@link SpacecraftState} at the final date
     * @exception OrekitException if integration cannot be performed
     */
    private SpacecraftState propagate(SpacecraftState initialState,
                                      AbsoluteDate finalDate, StepHandler handler)
        throws OrekitException {

        if (initialState.getDate().equals(finalDate)) {
            // don't extrapolate
            return initialState;
        }

        // space dynamics view
        startDate  = initialState.getDate();

        final EquinoctialParameters initialParameters =
            new EquinoctialParameters(initialState.getParameters(), mu);

        currentState =
            new SpacecraftState(new Orbit(initialState.getDate(), initialParameters),
                                initialState.getMass(), initialState.getAttitudeKinematics());

        adder = new TimeDerivativesEquations(initialParameters , mu);

        if (initialState.getMass() <= 0.0) {
            throw new IllegalArgumentException("Mass is null or negative");
        }

        // mathematical view
        final double t0 = 0;
        final double t1 = finalDate.minus(startDate);

        // Map state to array
        state[0] = initialParameters.getA();
        state[1] = initialParameters.getEquinoctialEx();
        state[2] = initialParameters.getEquinoctialEy();
        state[3] = initialParameters.getHx();
        state[4] = initialParameters.getHy();
        state[5] = initialParameters.getLv();
        state[6] = initialState.getMass();

        // Add the switching functions
        for (final Iterator iter = forceSwf.iterator(); iter.hasNext(); ) {
            final OrekitSwitchingFunction swf = (OrekitSwitchingFunction) iter.next();
            integrator.addSwitchingFunction(new MappingSwitchingFunction(swf),
                                            swf.getMaxCheckInterval(), swf.getThreshold(),
                                            swf.getMaxIterationCount());
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
        final AbsoluteDate date = new AbsoluteDate(startDate, t1);

        final EquinoctialParameters parameters =
            new EquinoctialParameters(state[0], state[1],state[2],state[3],
                                      state[4],state[5], EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                      initialParameters.getFrame());

        return new SpacecraftState(new Orbit(date , parameters), state[6],
                                   akProvider.getAttitudeKinematics(date,
                                                                    parameters.getPVCoordinates(mu),
                                                                    parameters.getFrame()));
    }

    /** Gets the dimension of the handled state vector (always 7).
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
            if (swfException != null) {
                throw swfException;
            }
            // update space dynamics view
            mapState(t, y);

            // compute cartesian coordinates
            if (currentState.getMass() <= 0.0) {
                OrekitException.throwIllegalArgumentException("spacecraft mass becomes negative (m: {0})",
                                                              new Object[] {
                                                                  new Double(currentState.getMass())
                                                              });
            }
            // initialize derivatives
            adder.initDerivatives(yDot, (EquinoctialParameters)currentState.getParameters());

            // compute the contributions of all perturbing forces
            for (final Iterator iter = forceModels.iterator(); iter.hasNext();) {
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
     * @exception OrekitException
     */
    private void mapState(double t, double [] y) throws OrekitException {

        // update space dynamics view
        final EquinoctialParameters currentParameters =
            new EquinoctialParameters(y[0], y[1],y[2],y[3],y[4],y[5],
                                      EquinoctialParameters.TRUE_LATITUDE_ARGUMENT,
                                      currentState.getFrame());
        final AbsoluteDate currentDate = new AbsoluteDate(startDate, t);
        currentState =
            new SpacecraftState(new Orbit(currentDate, currentParameters), y[6],
                                akProvider.getAttitudeKinematics(currentDate,
                                                                 currentParameters.getPVCoordinates(mu),
                                                                 currentState.getFrame()));
    }


    /** Converts OREKIT switching functions to commons-math interface. */
    private class MappingSwitchingFunction implements SwitchingFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = -6733513215617899510L;

        /** Underlying orekit switching function. */
        private final OrekitSwitchingFunction swf;

        public MappingSwitchingFunction(OrekitSwitchingFunction swf) {
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

    }

}
