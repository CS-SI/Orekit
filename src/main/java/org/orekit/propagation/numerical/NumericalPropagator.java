/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.DummyStepHandler;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.StepNormalizer;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.InertialLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.forces.ForceModel;
import org.orekit.time.AbsoluteDate;


/** This class propagates {@link org.orekit.orbits.Orbit orbits} using
 * numerical integration.
 * <p>Numerical propagation is much more accurate than analytical propagation
 * like for example {@link org.orekit.propagation.analytical.KeplerianPropagator
 * keplerian} or {@link org.orekit.propagation.analytical.EcksteinHechlerPropagator
 * Eckstein-Hechler}, but requires a few more steps to set up to be used properly.
 * Whereas analytical propagators are configured only thanks to their various
 * constructors and can be used immediately after construction, numerical propagators
 * configuration involve setting several parameters between construction time
 * and propagation time.</p>
 * <p>The configuration parameters that can be set are:</p>
 * <ul>
 *   <li>the initial spacecraft state ({@link #setInitialState(SpacecraftState)})</li>
 *   <li>the central attraction coefficient ({@link #setMu()})</li>
 *   <li>the various force models ({@link #addForceModel(ForceModel)},
 *   {@link #removeForceModels()})</li>
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addSwitchingFunction()}, {@link #removeSwitchingFunction()})</li>
 *   <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 *   {@link #setMasterMode(double, OrekitFixedStepHandler)}, {@link
 *   #setMasterMode(OrekitStepHandler)}, {@link #setBatchMode(IntegratedEphemeris)})</li>
 * </ul>
 * <p>From these configuration parameters, only the initial state is mandatory. If the
 * central attraction coefficient is not explicitly specified, the one used to define
 * the initial orbit will be used. However, specifying only the initial state and
 * perhaps the central attraction coefficient would mean the propagator would use only
 * keplerian forces. In this case, the simpler {@link
 * org.orekit.propagation.analytical.KeplerianPropagator KeplerianPropagator} class would
 * perhaps be more effective. The propagator is only in one mode at a time.</p>
 * <p>The same propagator can be reused for several orbit extrapolations, by resetting
 * the initial state without modifying the other configuration parameters.</p>

 * <p>The binding logic with the rest of the application represents the way to provide
 * propagation results. Several modes exist to better suit user needs.
 * <dl>
 *  <dt>master mode</dt>
 *  <dt>unttended mode</dt>
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
 * @see TimeDerivativesEquations
 *
 * @author Mathieu Roméro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class NumericalPropagator implements Propagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -329966842518411782L;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

    /** Central body gravitational constant. */
    private double mu;

    /** Force models used during the extrapolation of the Orbit. */
    private final List forceModels;

    /** Switching functions used during the extrapolation of the Orbit. */
    private final List forceSwf;

    /** State vector. */
    private final double[] state;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Initial state to propagate. */
    private SpacecraftState initialState;

    /** Current state to propagate. */
    private SpacecraftState currentState;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final FirstOrderIntegrator integrator;

    /** Gauss equations handler. */
    private TimeDerivativesEquations adder;

    /** Propagator mode handler. */
    private ModeHandler modeHandler;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. there is no perturbing force
     * at all. This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalPropagator(final FirstOrderIntegrator integrator) {
        this.mu           = Double.NaN;
        this.forceModels  = new ArrayList();
        this.forceSwf     = new ArrayList();
        this.integrator   = integrator;
        this.startDate    = new AbsoluteDate();
        this.currentState = null;
        this.adder        = null;
        this.attitudeLaw  = InertialLaw.J2000_ALIGNED;
        this.state        = new double[7];
        setSlaveMode();
    }

    /** Set the central attraction coefficient &mu;.
     * @param mu central attraction coefficient (m^3/s^2)
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /** Get the central attraction coefficient &mu;.
     * @return mu central attraction coefficient (m^3/s^2)
     */
    public double getMu() {
        return mu;
    }

    /** Add a force model to the global perturbation model. The associated
     * switching function is added to the switching functions vector.
     * All models added by this method will be considered during integration.
     * If this method is not called at all, the integrated orbit will follow
     * a keplerian evolution only.
     * @param model perturbing {@link ForceModel} to add
     */
    public void addForceModel(final ForceModel model) {
        forceModels.add(model);
        final OrekitSwitchingFunction[] swf = model.getSwitchingFunctions();
        if (swf != null) {
            for (int i = 0; i < swf.length; i++) {
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

    /** Set the propagator to slave mode.
     * <p>This mode is used when the user needs only the final orbit at the target time.
     *  The (slave) propagator computes this result and return it to the calling
     *  (master) application, without any intermediate feedback.<p>
     * <p>This is the default mode.</p>
     */
    public void setSlaveMode() {
        integrator.setStepHandler(DummyStepHandler.getInstance());
        modeHandler = null;
    }

    /** Set the propagator to master mode.
     * <p>This mode is used when the user needs to have some custom function called at the
     * end of each finalized step during integration. The (master) propagator integration
     * loop calls the (slave) application callback methods at each finalized step.</p>
     * @param h fixed stepsize (s)
     * @param handler object to call at fixed time steps
     */
    public void setMasterMode(final double h, final OrekitFixedStepHandler handler) {
        integrator.setStepHandler(new StepNormalizer(h, handler));
        modeHandler = handler;
    }

    /** Set the propagator to master mode.
     * <p>This mode is used when the user needs to have some custom function called at the
     * end of each finalized step during integration. The (master) propagator integration
     * loop calls the (slave) application callback methods at each finalized step.</p>
     */
    public void setMasterMode(final OrekitStepHandler handler) {
        integrator.setStepHandler(handler);
        modeHandler = handler;
    }

    /** Set the propagator to batch mode.
     *  <p>This mode is used when the user needs random access to the orbit state at any time
     *  between the initial and target times, and in no sequential order. A typical example is
     *  the implementation of search and iterative algorithms that may navigate forward and
     *  backward inside the integration range before finding their result.</p>
     *  <p>Beware that since this mode stores <strong>all</strong> intermediate results,
     *  it may be memory intensive for long integration ranges and high precision/short
     *  time steps.</p>
     * @param ephemeris instance to populate with the results
     */
    public void setBatchMode(final IntegratedEphemeris ephemeris) {
        integrator.setStepHandler(ephemeris);
        modeHandler = ephemeris;
    }

    /** Set the initial state.
     * @param initialState initial state
     */
    public void setInitialState(final SpacecraftState initialState) {
        if (Double.isNaN(mu)) {
            mu = initialState.getMu();
        }
        this.initialState = initialState;
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate finalDate)
        throws PropagationException {
        try {

            if (initialState == null) {
                throw new PropagationException("initial state not specified for orbit propagation",
                                               new Object[0]);
            }
            if (initialState.getDate().equals(finalDate)) {
                // don't extrapolate
                return initialState;
            }

            // space dynamics view
            startDate  = initialState.getDate();
            if (modeHandler != null) {
                modeHandler.initialize(startDate, initialState.getFrame(), mu, attitudeLaw);
            }

            final EquinoctialOrbit initialOrbit =
                new EquinoctialOrbit(initialState.getOrbit());

            currentState =
                new SpacecraftState(initialOrbit, initialState.getAttitude(), initialState.getMass());

            adder = new TimeDerivativesEquations(initialOrbit);

            if (initialState.getMass() <= 0.0) {
                throw new IllegalArgumentException("Mass is null or negative");
            }

            // mathematical view
            final double t0 = 0;
            final double t1 = finalDate.minus(startDate);

            // Map state to array
            state[0] = initialOrbit.getA();
            state[1] = initialOrbit.getEquinoctialEx();
            state[2] = initialOrbit.getEquinoctialEy();
            state[3] = initialOrbit.getHx();
            state[4] = initialOrbit.getHy();
            state[5] = initialOrbit.getLv();
            state[6] = initialState.getMass();

            // Set up the switching functions
            integrator.clearSwitchingFunctions();
            for (final Iterator iter = forceSwf.iterator(); iter.hasNext(); ) {
                final OrekitSwitchingFunction swf = (OrekitSwitchingFunction) iter.next();
                integrator.addSwitchingFunction(new WrappedSwitchingFunction(swf, startDate, mu,
                                                                             initialState.getFrame(),
                                                                             attitudeLaw),
                                                                             swf.getMaxCheckInterval(), swf.getThreshold(),
                                                                             swf.getMaxIterationCount());
            }

            // mathematical integration
            integrator.integrate(new DifferentialEquations(), t0, state, t1, state);

            // back to space dynamics view
            final AbsoluteDate date = new AbsoluteDate(startDate, t1);

            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(state[0], state[1], state[2], state[3],
                                     state[4], state[5], EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                     initialOrbit.getFrame(), initialOrbit.getDate(), mu);

            return new SpacecraftState(orbit,
                                       attitudeLaw.getState(date,
                                                            orbit.getPVCoordinates(),
                                                            orbit.getFrame()),
                                                            state[6]);
        } catch (OrekitException oe) {
            throw new PropagationException(oe.getMessage(), oe);
        } catch (DerivativeException de) {

            // recover a possible embedded PropagationException
            for (Throwable t = de; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(de.getMessage(), de);

        } catch (IntegratorException ie) {

            // recover a possible embedded PropagationException
            for (Throwable t = ie; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(ie.getMessage(), ie);

        }
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations {

        /** {@inheritDoc} */
        public int getDimension() {
            return 7;
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            throws DerivativeException {

            try {
                // update space dynamics view
                currentState =
                    mapState(t, y, startDate, mu, currentState.getFrame(), attitudeLaw);

                // compute cartesian coordinates
                if (currentState.getMass() <= 0.0) {
                    OrekitException.throwIllegalArgumentException("spacecraft mass becomes negative (m: {0})",
                                                                  new Object[] {
                            new Double(currentState.getMass())
                    });
                }
                // initialize derivatives
                adder.initDerivatives(yDot, (EquinoctialOrbit) currentState.getOrbit());

                // compute the contributions of all perturbing forces
                for (final Iterator iter = forceModels.iterator(); iter.hasNext();) {
                    ((ForceModel) iter.next()).addContribution(currentState, adder);
                }

                // finalize derivatives by adding the Kepler contribution
                adder.addKeplerContribution();
            } catch (OrekitException oe) {
                throw new DerivativeException(oe.getMessage(), new String[0]);
            }

        }

        /** Convert state array to space dynamics objects (AbsoluteDate and OrbitalParameters).
         * @param t integration time (s)
         * @param y state as a flat array
         * @param referenceDate reference date from which t is counted
         * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
         * @param frame frame in which integration is performed
         * @param attitudeLaw spacecraft attitude law
         * @return state corresponding to the flat array as a space dynamics object
         * @exception OrekitException if the attitude state cannot be determined
         * by the attitude law
         */
        private SpacecraftState mapState(final double t, final double [] y,
                                         final AbsoluteDate referenceDate, final double mu,
                                         final Frame frame,
                                         final AttitudeLaw attitudeLaw)
            throws OrekitException {

            // convert raw mathematical data to space dynamics objects
            final AbsoluteDate date = new AbsoluteDate(referenceDate, t);
            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                     frame, date, mu);
            final Attitude attitude =
                attitudeLaw.getState(date, orbit.getPVCoordinates(), frame);

            return new SpacecraftState(orbit, attitude, y[6]);

        }

    }

}
