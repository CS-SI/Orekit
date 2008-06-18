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
import org.apache.commons.math.ode.StepNormalizer;
import org.apache.commons.math.ode.SwitchingFunction;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.InertialLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.OrekitFixedStepHandler;
import org.orekit.propagation.OrekitStepHandler;
import org.orekit.propagation.OrekitSwitchingFunction;
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
 *   <li>the central attraction coefficient ({@link #setMu(double)})</li>
 *   <li>the various force models ({@link #addForceModel(ForceModel)},
 *   {@link #removeForceModels()})</li>
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addSwitchingFunction(OrekitSwitchingFunction)},
 *   {@link #removeSwitchingFunctions()})</li>
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

 * @see SpacecraftState
 * @see ForceModel
 * @see OrekitStepHandler
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
    private static final long serialVersionUID = 1841481915093793894L;

    /** Attitude law. */
    private AttitudeLaw attitudeLaw;

    /** Central body gravitational constant. */
    private double mu;

    /** Force models used during the extrapolation of the Orbit. */
    private final List forceModels;

    /** Switching functions not related to force models. */
    private final List switchingFunctions;

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

    /** Current mode. */
    private int mode;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. there is no perturbing force
     * at all. This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalPropagator(final FirstOrderIntegrator integrator) {
        this.mu                 = Double.NaN;
        this.forceModels        = new ArrayList();
        this.switchingFunctions = new ArrayList();
        this.integrator         = integrator;
        this.startDate          = new AbsoluteDate();
        this.currentState       = null;
        this.adder              = null;
        this.attitudeLaw        = InertialLaw.J2000_ALIGNED;
        this.state              = new double[7];
        setSlaveMode();
    }

    /** Set the central attraction coefficient &mu;.
     * @param mu central attraction coefficient (m^3/s^2)
     * @see #getMu()
     * @see #addForceModel(ForceModel)
     */
    public void setMu(final double mu) {
        this.mu = mu;
    }

    /** Get the central attraction coefficient &mu;.
     * @return mu central attraction coefficient (m^3/s^2)
     * @see #setMu(double)
     */
    public double getMu() {
        return mu;
    }

    /** Add a non force model related switching function.
     * <p>This method is devoted to user defined switching functions not related
     * to force models. Switching functions related to force models are handled
     * automatically when the model is added, so they must <strong>not</strong>
     * be added using this method.</p>
     * @param switchingFunction switching function to add
     * @see #removeSwitchingFunctions()
     * @see #addForceModel(ForceModel)
     */
    public void addSwitchingFunction(final OrekitSwitchingFunction switchingFunction) {
        switchingFunctions.add(switchingFunction);
    }

    /** Remove all switching functions.
     * @see #addSwitchingFunction(OrekitSwitchingFunction)
     */
    public void removeSwitchingFunctions() {
        switchingFunctions.clear();
    }

    /** Add a force model to the global perturbation model.
     * <p>If the force models is associated to switching functions, these
     * switching functions will be handled automatically, they must
     * <strong>not</strong> be added using the {@link
     * #addSwitchingFunction(OrekitSwitchingFunction) addSwitchingFunction}
     * method.</p>
     * <p>If this method is not called at all, the integrated orbit will follow
     * a keplerian evolution only.</p>
     * @param model perturbing {@link ForceModel} to add
     * @see #removeForceModels()
     * @see #setMu(double)
     */
    public void addForceModel(final ForceModel model) {
        forceModels.add(model);
    }

    /** Remove all perturbing force models from the global perturbation model.
     * <p>Once all perturbing forces have been removed (and as long as no new force
     * model is added), the integrated orbit will follow a keplerian evolution
     * only.</p>
     * @see #addForceModel(ForceModel)
     */
    public void removeForceModels() {
        forceModels.clear();
    }

    /** {@inheritDoc} */
    public int getMode() {
        return mode;
    }

    /** {@inheritDoc} */
    public void setSlaveMode() {
        integrator.setStepHandler(DummyStepHandler.getInstance());
        modeHandler = null;
        mode = SLAVE_MODE;
    }

    /** {@inheritDoc} */
    public void setMasterMode(final double h, final OrekitFixedStepHandler handler) {
        AdaptedFixedStepHandler wrapped = new AdaptedFixedStepHandler(handler);
        integrator.setStepHandler(new StepNormalizer(h, wrapped));
        modeHandler = wrapped;
        mode = MASTER_FIXED_MODE;
    }

    /** {@inheritDoc} */
    public void setMasterMode(final OrekitStepHandler handler) {
        AdaptedStepHandler wrapped = new AdaptedStepHandler(handler);
        integrator.setStepHandler(wrapped);
        modeHandler = wrapped;
        mode = MASTER_VARIABLE_MODE;
    }

    /** {@inheritDoc} */
    public void setEphemerisMode() {
        IntegratedEphemeris ephemeris = new IntegratedEphemeris();
        integrator.setStepHandler(ephemeris);
        modeHandler = ephemeris;
        mode = EPHEMERIS_GENERATION_MODE;
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris()
        throws IllegalStateException {
        if (mode != EPHEMERIS_GENERATION_MODE) {
            throw OrekitException.createIllegalStateException("propagator is not in batch mode",
                                                              new Object[0]);
        }
        return (IntegratedEphemeris) modeHandler;
    }

    /** Set the initial state.
     * @param initialState initial state
     * @see #propagate(AbsoluteDate)
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

            integrator.clearSwitchingFunctions();

            // set up switching functions related to force models
            for (final Iterator iterator = forceModels.iterator(); iterator.hasNext();) {
                final OrekitSwitchingFunction[] swf =
                    ((ForceModel) iterator.next()).getSwitchingFunctions();
                if (swf != null) {
                    for (int i = 0; i < swf.length; i++) {
                        setUpSwitchingFunction(swf[i]);
                    }
                }
            }

            // set up switching functions added by user
            for (final Iterator iter = switchingFunctions.iterator(); iter.hasNext(); ) {
                setUpSwitchingFunction((OrekitSwitchingFunction) iter.next());
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

    /** Wrap an Orekit switching function and register it to the integrator.
     * @param osf switching function to wrap
     */
    private void setUpSwitchingFunction(final OrekitSwitchingFunction osf) {
        final SwitchingFunction switchingFunction =
            new AdaptedSwitchingFunction(osf, startDate, mu,
                                         initialState.getFrame(), attitudeLaw);
        integrator.addSwitchingFunction(switchingFunction,
                                        osf.getMaxCheckInterval(),
                                        osf.getThreshold(),
                                        osf.getMaxIterationCount());
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = -1927530118454989452L;

        /** {@inheritDoc} */
        public int getDimension() {
            return 7;
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            throws DerivativeException {

            try {
                // update space dynamics view
                currentState = mapState(t, y, startDate, currentState.getFrame());

                // compute cartesian coordinates
                if (currentState.getMass() <= 0.0) {
                    throw OrekitException.createIllegalArgumentException("spacecraft mass becomes negative (m: {0})",
                                                                         new Object[] {
                                                                             Double.valueOf(currentState.getMass())
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
         * @param frame frame in which integration is performed
         * @return state corresponding to the flat array as a space dynamics object
         * @exception OrekitException if the attitude state cannot be determined
         * by the attitude law
         */
        private SpacecraftState mapState(final double t, final double [] y,
                                         final AbsoluteDate referenceDate, final Frame frame)
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
