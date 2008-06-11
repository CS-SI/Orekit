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
package fr.cs.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.ode.ContinuousOutputModel;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.DummyStepHandler;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.StepHandler;
import org.apache.commons.math.ode.StepNormalizer;

import fr.cs.orekit.attitudes.Attitude;
import fr.cs.orekit.attitudes.AttitudeLaw;
import fr.cs.orekit.attitudes.LofOffset;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.orbits.EquinoctialOrbit;
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
 * @see TimeDerivativesEquations
 *
 * @author Mathieu Roméro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class NumericalModel
    implements FirstOrderDifferentialEquations {

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

    /** Current state to propagate. */
    private SpacecraftState currentState;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final FirstOrderIntegrator integrator;

    /** Gauss equations handler. */
    private TimeDerivativesEquations adder;

    /** Create a new instance of NumericalModel, based on a specified mu.
     * After creation, the instance is empty, i.e. there is no perturbing force
     * at all. This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param mu central body gravitational constant (GM).
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalModel(final double mu, final FirstOrderIntegrator integrator) {
        this.mu           = mu;
        this.forceModels  = new ArrayList();
        this.forceSwf     = new ArrayList();
        this.integrator   = integrator;
        this.startDate    = new AbsoluteDate();
        this.currentState = null;
        this.adder        = null;
        final AttitudeLaw lofAligned = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        this.attitudeLaw  = lofAligned;
        this.state        = new double[getDimension()];
    }

    /** Create a new instance of NumericalModel, based on orbit definition mu.
     * After creation, the instance is empty, i.e. there is no perturbing force
     * at all. This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalModel(final FirstOrderIntegrator integrator) {
        this.mu           = Double.NaN;
        this.forceModels  = new ArrayList();
        this.forceSwf     = new ArrayList();
        this.integrator   = integrator;
        this.startDate    = new AbsoluteDate();
        this.currentState = null;
        this.adder        = null;
        final AttitudeLaw lofAligned = new LofOffset(RotationOrder.ZYX, 0., 0., 0.);
        this.attitudeLaw  = lofAligned;
        this.state        = new double[getDimension()];
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

    /** Propagate an orbit up to a specific target date.
     * @param initialState state to extrapolate
     * @param finalDate target date for the orbit
     * @return the state at the final date
     * @exception OrekitException if integration cannot be performed
     */
    public SpacecraftState propagate(final SpacecraftState initialState,
                                     final AbsoluteDate finalDate)
        throws OrekitException {
        if (Double.isNaN(mu)) {
            mu = initialState.getMu();
        }
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
    public SpacecraftState propagate(final SpacecraftState initialState,
                                     final AbsoluteDate finalDate,
                                     final IntegratedEphemeris ephemeris)
        throws OrekitException {
        if (Double.isNaN(mu)) {
            mu = initialState.getMu();
        }
        final ContinuousOutputModel model = new ContinuousOutputModel();
        final SpacecraftState finalState =
            propagate(initialState, finalDate, (StepHandler) model);
        ephemeris.initialize(model , initialState.getDate(),
                             initialState.getOrbit().getFrame(), attitudeLaw, mu);
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
    public SpacecraftState propagate(final SpacecraftState initialState,
                                     final AbsoluteDate finalDate,
                                     final double h,
                                     final OrekitFixedStepHandler handler)
        throws OrekitException {
        if (Double.isNaN(mu)) {
            mu = initialState.getMu();
        }
        handler.initialize(initialState.getDate(), initialState.getFrame(), mu, attitudeLaw);
        return propagate(initialState, finalDate, new StepNormalizer(h, handler));
    }

    /** Propagate an orbit and call a user handler at each step end.
     * @param initialState the state to extrapolate
     * @param finalDate target date for the orbit
     * @param handler object to call at fixed time steps
     * @return the state at the final date
     * @exception OrekitException if integration cannot be performed
     */
    public SpacecraftState propagate(final SpacecraftState initialState,
                                     final AbsoluteDate finalDate,
                                     final OrekitStepHandler handler)
        throws OrekitException {
        if (Double.isNaN(mu)) {
            mu = initialState.getMu();
        }
        handler.initialize(initialState.getDate(), initialState.getFrame(), mu, attitudeLaw);
        return propagate(initialState, finalDate, (StepHandler) handler);
    }


    /** Propagate an orbit and call a user handler after each successful step.
     * @param initialState the state to extrapolate
     * @param finalDate target date for the orbit
     * @param handler object to call at the end of each successful step
     * @return the {@link SpacecraftState} at the final date
     * @exception OrekitException if integration cannot be performed
     */
    private SpacecraftState propagate(final SpacecraftState initialState,
                                      final AbsoluteDate finalDate,
                                      final StepHandler handler)
        throws OrekitException {

        if (initialState.getDate().equals(finalDate)) {
            // don't extrapolate
            return initialState;
        }

        // space dynamics view
        startDate  = initialState.getDate();

        final EquinoctialOrbit initialOrbit =
            new EquinoctialOrbit(initialState.getOrbit());

        currentState =
            new SpacecraftState(initialOrbit, initialState.getMass(), initialState.getAttitude());

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
        try {
            integrator.setStepHandler(handler);
            integrator.integrate(this, t0, state, t1, state);
        } catch (DerivativeException de) {

            // recover a possible embedded OrekitException
            for (Throwable t = de; t != null; t = t.getCause()) {
                if (t instanceof OrekitException) {
                    throw (OrekitException) t;
                }
            }

            throw new OrekitException(de.getMessage(), de);

        } catch (IntegratorException ie) {

            // recover a possible embedded OrekitException
            for (Throwable t = ie; t != null; t = t.getCause()) {
                if (t instanceof OrekitException) {
                    throw (OrekitException) t;
                }
            }

            throw new OrekitException(ie.getMessage(), ie);

        }

        // back to space dynamics view
        final AbsoluteDate date = new AbsoluteDate(startDate, t1);

        final EquinoctialOrbit orbit =
            new EquinoctialOrbit(state[0], state[1], state[2], state[3],
                                 state[4], state[5], EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                 initialOrbit.getFrame(), initialOrbit.getDate(), mu);

        return new SpacecraftState(orbit, state[6],
                                   attitudeLaw.getState(date,
                                                        orbit.getPVCoordinates(),
                                                        orbit.getFrame()));
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
     * @param integrationFrame frame in which integration is performed
     * @param attitudeLaw spacecraft attitude law
     * @return state corresponding to the flat array as a space dynamics object
     * @exception OrekitException if the attitude state cannot be determined
     * by the attitude law
     */
    private static SpacecraftState mapState(final double t, final double [] y,
                                            final AbsoluteDate referenceDate, final double mu,
                                            final Frame integrationFrame,
                                            final AttitudeLaw attitudeLaw)
        throws OrekitException {

        // update space dynamics view
        final AbsoluteDate currentDate = new AbsoluteDate(referenceDate, t);
        final EquinoctialOrbit currentOrbit =
            new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                 EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                 integrationFrame, currentDate, mu);
        final Attitude currentAttitude = attitudeLaw.getState(currentDate,
                                                              currentOrbit.getPVCoordinates(),
                                                              integrationFrame);

        return new SpacecraftState(currentOrbit, y[6], currentAttitude);

    }


}
