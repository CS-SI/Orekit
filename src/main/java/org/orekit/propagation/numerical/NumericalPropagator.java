/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.events.EventHandler;
import org.apache.commons.math.ode.sampling.DummyStepHandler;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.InertialLaw;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AdaptedEventDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.AdaptedStepHandler;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepNormalizer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


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
 *   ({@link #addEventDetector(EventDetector)},
 *   {@link #clearEventsDetectors()})</li>
 *   <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 *   {@link #setMasterMode(double, OrekitFixedStepHandler)}, {@link
 *   #setMasterMode(OrekitStepHandler)}, {@link #setEphemerisMode()}, {@link
 *   #getGeneratedEphemeris()})</li>
 * </ul>
 * <p>From these configuration parameters, only the initial state is mandatory. If the
 * central attraction coefficient is not explicitly specified, the one used to define
 * the initial orbit will be used. However, specifying only the initial state and
 * perhaps the central attraction coefficient would mean the propagator would use only
 * keplerian forces. In this case, the simpler {@link
 * org.orekit.propagation.analytical.KeplerianPropagator KeplerianPropagator} class would
 * perhaps be more effective. The propagator is only in one mode at a time.</p>
 * <p>The underlying numerical integrator set up in the constructor may also have its own
 * configuration parameters. Typical configuration parameters for adaptive stepsize integrators
 * are the min, max and perhaps start step size as well as the absolute and/or relative errors
 * thresholds. The state that is seen by the integrator is a simple seven elements double array.
 * The six first elements are the {@link EquinoctialOrbit equinoxial orbit parameters} (a, e<sub>x</sub>,
 * e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, l<sub>v</sub>) in meters and radians, and
 * the last element is the mass in kilograms. The following code snippet shows a typical
 * setting for Low Earth Orbit propagation:</p>
 * <pre>
 * final double minStep  = 0.001;
 * final double maxStep  = 1000;
 * final double initStep = 60;
 * final double[] absTolerance = {
 *     0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
 * };
 * final double[] relTolerance = {
 *     1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
 * };
 * AdaptiveStepsizeIntegrator integrator =
 *     new DormandPrince853Integrator(minStep, maxStep, absTolerance, relTolerance);
 * integrator.setInitialStepSize(initStep);
 * propagator = new NumericalPropagator(integrator);
 * </pre>
 * <p>The same propagator can be reused for several orbit extrapolations, by resetting
 * the initial state without modifying the other configuration parameters. However, the
 * same instance cannot be used simultaneously by different threads, the class is not
 * thread-safe.</p>

 * @see SpacecraftState
 * @see ForceModel
 * @see OrekitStepHandler
 * @see OrekitFixedStepHandler
 * @see IntegratedEphemeris
 * @see TimeDerivativesEquations
 *
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class NumericalPropagator implements Propagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -2385169798425713766L;

    // CHECKSTYLE: stop VisibilityModifierCheck

    /** Attitude law. */
    protected AttitudeLaw attitudeLaw;

    /** Central body gravitational constant. */
    protected double mu;

    /** Force models used during the extrapolation of the Orbit. */
    protected final List<ForceModel> forceModels;

    /** Event detectors not related to force models. */
    protected final List<EventDetector> detectors;

    /** State vector. */
    protected final double[] stateVector;

    /** Start date. */
    protected AbsoluteDate startDate;

    /** Initial state to propagate. */
    protected SpacecraftState initialState;

    /** Current state to propagate. */
    protected SpacecraftState currentState;

    /** Integrator selected by the user for the orbital extrapolation process. */
    protected transient FirstOrderIntegrator integrator;

    /** Counter for differential equations calls. */
    protected int calls;

    /** Gauss equations handler. */
    protected TimeDerivativesEquations adder;

    /** Propagator mode handler. */
    protected ModeHandler modeHandler;

    /** Current mode. */
    protected int mode;

    // CHECKSTYLE: resume VisibilityModifierCheck

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude law is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only.
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalPropagator(final FirstOrderIntegrator integrator) {
        this.mu           = Double.NaN;
        this.forceModels  = new ArrayList<ForceModel>();
        this.detectors    = new ArrayList<EventDetector>();
        this.startDate    = new AbsoluteDate();
        this.currentState = null;
        this.adder        = null;
        this.attitudeLaw  = InertialLaw.EME2000_ALIGNED;
        this.stateVector  = new double[7];
        setIntegrator(integrator);
        setSlaveMode();
    }

    /** Set the integrator.
     * @param integrator numerical integrator to use for propagation.
     */
    public void setIntegrator(final FirstOrderIntegrator integrator) {
        this.integrator = integrator;
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

    /** Set the attitude law.
     * @param attitudeLaw attitude law
     */
    public void setAttitudeLaw(final AttitudeLaw attitudeLaw) {
        this.attitudeLaw = attitudeLaw;
    }

    /** {@inheritDoc} */
    public void addEventDetector(final EventDetector detector) {
        detectors.add(detector);
    }

    /** {@inheritDoc} */
    public Collection<EventDetector>getEventsDetectors() {
        return Collections.unmodifiableCollection(detectors);
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        detectors.clear();
    }

    /** Add a force model to the global perturbation model.
     * <p>If the force models is associated to discrete events, these
     * events will be handled automatically, they must <strong>not</strong>
     * be added using the {@link #addEventDetector(EventDetector) addEventDetector}
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

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setSlaveMode() {
        integrator.clearEventHandlers();
        integrator.addStepHandler(DummyStepHandler.getInstance());
        modeHandler = null;
        mode = SLAVE_MODE;
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setMasterMode(final double h, final OrekitFixedStepHandler handler) {
        setMasterMode(new OrekitStepNormalizer(h, handler));
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setMasterMode(final OrekitStepHandler handler) {
        integrator.clearEventHandlers();
        final AdaptedStepHandler wrapped = new AdaptedStepHandler(handler);
        integrator.addStepHandler(wrapped);
        modeHandler = wrapped;
        mode = MASTER_MODE;
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setEphemerisMode() {
        integrator.clearEventHandlers();
        final IntegratedEphemeris ephemeris = new IntegratedEphemeris();
        integrator.addStepHandler(ephemeris);
        modeHandler = ephemeris;
        mode = EPHEMERIS_GENERATION_MODE;
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris()
        throws IllegalStateException {
        if (mode != EPHEMERIS_GENERATION_MODE) {
            throw OrekitException.createIllegalStateException("propagator is not in batch mode");
        }
        return (IntegratedEphemeris) modeHandler;
    }

    /** {@inheritDoc} */
    public SpacecraftState getInitialState() {
        return initialState;
    }

    /** Set the initial state.
     * @param initialState initial state
     * @see #propagate(AbsoluteDate)
     */
    public void setInitialState(final SpacecraftState initialState) {
        resetInitialState(initialState);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        if (Double.isNaN(mu)) {
            mu = state.getMu();
        }
        this.initialState = state;
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate finalDate)
        throws PropagationException {
        try {

            if (initialState == null) {
                throw new PropagationException("initial state not specified for orbit propagation");
            }
            if (initialState.getDate().equals(finalDate)) {
                // don't extrapolate
                return initialState;
            }
            if (integrator == null) {
                throw new PropagationException("ODE integrator not set for orbit propagation");
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
            final double t1 = finalDate.durationFrom(startDate);

            // Map state to array
            stateVector[0] = initialOrbit.getA();
            stateVector[1] = initialOrbit.getEquinoctialEx();
            stateVector[2] = initialOrbit.getEquinoctialEy();
            stateVector[3] = initialOrbit.getHx();
            stateVector[4] = initialOrbit.getHy();
            stateVector[5] = initialOrbit.getLv();
            stateVector[6] = initialState.getMass();

            integrator.clearEventHandlers();

            // set up events related to force models
            for (final ForceModel forceModel : forceModels) {
                final EventDetector[] modelDetectors = forceModel.getEventsDetectors();
                if (modelDetectors != null) {
                    for (final EventDetector detector : modelDetectors) {
                        setUpEventDetector(detector);
                    }
                }
            }

            // set up events added by user
            for (final EventDetector detector : detectors) {
                setUpEventDetector(detector);
            }

            // mathematical integration
            final double stopTime = integrator.integrate(new DifferentialEquations(), t0, stateVector, t1, stateVector);

            // back to space dynamics view
            final AbsoluteDate date = startDate.shiftedBy(stopTime);

            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(stateVector[0], stateVector[1], stateVector[2], stateVector[3],
                                     stateVector[4], stateVector[5], EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                     initialOrbit.getFrame(), date, mu);

            resetInitialState(new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit), stateVector[6]));
            return initialState;

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

    /** {@inheritDoc} */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** Get the number of calls to the differential equations computation method.
     * <p>The number of calls is reset each time the {@link #propagate(AbsoluteDate)}
     * method is called.</p>
     * @return number of calls to the differential equations computation method
     */
    public int getCalls() {
        return calls;
    }

    /** Wrap an Orekit event detector and register it to the integrator.
     * @param osf event handler to wrap
     */
    protected void setUpEventDetector(final EventDetector osf) {
        final EventHandler handler =
            new AdaptedEventDetector(osf, startDate, mu,
                                     initialState.getFrame(), attitudeLaw);
        integrator.addEventHandler(handler,
                                   osf.getMaxCheckInterval(),
                                   osf.getThreshold(),
                                   osf.getMaxIterationCount());
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = -1927530118454989452L;

        /** Build a new instance. */
        public DifferentialEquations() {
            calls = 0;
        }

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
                                                                         currentState.getMass());
                }
                // initialize derivatives
                adder.initDerivatives(yDot, (EquinoctialOrbit) currentState.getOrbit());

                // compute the contributions of all perturbing forces
                for (final ForceModel forceModel : forceModels) {
                    forceModel.addContribution(currentState, adder);
                }

                // finalize derivatives by adding the Kepler contribution
                adder.addKeplerContribution();

                // increment calls counter
                ++calls;

            } catch (OrekitException oe) {
                throw new DerivativeException(oe.getMessage());
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
            final AbsoluteDate date = referenceDate.shiftedBy(t);
            final EquinoctialOrbit orbit =
                new EquinoctialOrbit(y[0], y[1], y[2], y[3], y[4], y[5],
                                     EquinoctialOrbit.TRUE_LATITUDE_ARGUMENT,
                                     frame, date, mu);
            final Attitude attitude = attitudeLaw.getAttitude(orbit);

            return new SpacecraftState(orbit, attitude, y[6]);

        }

    }

}
