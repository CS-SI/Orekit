/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.propagation.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.ode.DenseOutputModel;
import org.hipparchus.ode.EquationsMapper;
import org.hipparchus.ode.ExpandableODE;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.ODEState;
import org.hipparchus.ode.ODEStateAndDerivative;
import org.hipparchus.ode.OrdinaryDifferentialEquation;
import org.hipparchus.ode.SecondaryODE;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.events.ODEEventHandler;
import org.hipparchus.ode.sampling.ODEStateInterpolator;
import org.hipparchus.ode.sampling.ODEStepHandler;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;


/** Common handling of {@link org.orekit.propagation.Propagator Propagator}
 *  methods for both numerical and semi-analytical propagators.
 *  @author Luc Maisonobe
 */
public abstract class AbstractIntegratedPropagator extends AbstractPropagator {

    /** Event detectors not related to force models. */
    private final List<EventDetector> detectors;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final ODEIntegrator integrator;

    /** Mode handler. */
    private ModeHandler modeHandler;

    /** Additional equations. */
    private List<AdditionalEquations> additionalEquations;

    /** Counter for differential equations calls. */
    private int calls;

    /** Mapper between raw double components and space flight dynamics objects. */
    private StateMapper stateMapper;

    /** Equations mapper. */
    private EquationsMapper equationsMapper;

    /** Underlying raw rawInterpolator. */
    private ODEStateInterpolator mathInterpolator;

    /** Output only the mean orbit. <br/>
     * <p>
     * This is used only in the case of semianalitical propagators where there is a clear separation between
     * mean and short periodic elements. It is ignored by the Numerical propagator.
     * </p>
     */
    private boolean meanOrbit;

    /** Build a new instance.
     * @param integrator numerical integrator to use for propagation.
     * @param meanOrbit output only the mean orbit.
     */
    protected AbstractIntegratedPropagator(final ODEIntegrator integrator, final boolean meanOrbit) {
        detectors           = new ArrayList<EventDetector>();
        additionalEquations = new ArrayList<AdditionalEquations>();
        this.integrator     = integrator;
        this.meanOrbit      = meanOrbit;
    }

    /** Initialize the mapper. */
    protected void initMapper() {
        stateMapper = createMapper(null, Double.NaN, null, null, null, null);
    }

    /**  {@inheritDoc} */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        super.setAttitudeProvider(attitudeProvider);
        stateMapper = createMapper(stateMapper.getReferenceDate(), stateMapper.getMu(),
                                   stateMapper.getOrbitType(), stateMapper.getPositionAngleType(),
                                   attitudeProvider, stateMapper.getFrame());
    }

    /** Set propagation orbit type.
     * @param orbitType orbit type to use for propagation
     */
    protected void setOrbitType(final OrbitType orbitType) {
        stateMapper = createMapper(stateMapper.getReferenceDate(), stateMapper.getMu(),
                                   orbitType, stateMapper.getPositionAngleType(),
                                   stateMapper.getAttitudeProvider(), stateMapper.getFrame());
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation
     */
    protected OrbitType getOrbitType() {
        return stateMapper.getOrbitType();
    }

    /** Check if only the mean elements should be used in a semianalitical propagation.
     * @return true if only mean elements have to be used
     */
    protected boolean isMeanOrbit() {
        return meanOrbit;
    }

    /** Set position angle type.
     * <p>
     * The position parameter type is meaningful only if {@link
     * #getOrbitType() propagation orbit type}
     * support it. As an example, it is not meaningful for propagation
     * in {@link OrbitType#CARTESIAN Cartesian} parameters.
     * </p>
     * @param positionAngleType angle type to use for propagation
     */
    protected void setPositionAngleType(final PositionAngle positionAngleType) {
        stateMapper = createMapper(stateMapper.getReferenceDate(), stateMapper.getMu(),
                                   stateMapper.getOrbitType(), positionAngleType,
                                   stateMapper.getAttitudeProvider(), stateMapper.getFrame());
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    protected PositionAngle getPositionAngleType() {
        return stateMapper.getPositionAngleType();
    }

    /** Set the central attraction coefficient μ.
     * @param mu central attraction coefficient (m³/s²)
     */
    public void setMu(final double mu) {
        stateMapper = createMapper(stateMapper.getReferenceDate(), mu,
                                   stateMapper.getOrbitType(), stateMapper.getPositionAngleType(),
                                   stateMapper.getAttitudeProvider(), stateMapper.getFrame());
    }

    /** Get the central attraction coefficient μ.
     * @return mu central attraction coefficient (m³/s²)
     * @see #setMu(double)
     */
    public double getMu() {
        return stateMapper.getMu();
    }

    /** Get the number of calls to the differential equations computation method.
     * <p>The number of calls is reset each time the {@link #propagate(AbsoluteDate)}
     * method is called.</p>
     * @return number of calls to the differential equations computation method
     */
    public int getCalls() {
        return calls;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAdditionalStateManaged(final String name) {

        // first look at already integrated states
        if (super.isAdditionalStateManaged(name)) {
            return true;
        }

        // then look at states we integrate ourselves
        for (final AdditionalEquations equation : additionalEquations) {
            if (equation.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getManagedAdditionalStates() {
        final String[] alreadyIntegrated = super.getManagedAdditionalStates();
        final String[] managed = new String[alreadyIntegrated.length + additionalEquations.size()];
        System.arraycopy(alreadyIntegrated, 0, managed, 0, alreadyIntegrated.length);
        for (int i = 0; i < additionalEquations.size(); ++i) {
            managed[i + alreadyIntegrated.length] = additionalEquations.get(i).getName();
        }
        return managed;
    }

    /** Add a set of user-specified equations to be integrated along with the orbit propagation.
     * @param additional additional equations
     * @exception OrekitException if a set of equations with the same name is already present
     */
    public void addAdditionalEquations(final AdditionalEquations additional)
        throws OrekitException {

        // check if the name is already used
        if (isAdditionalStateManaged(additional.getName())) {
            // this set of equations is already registered, complain
            throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE,
                                      additional.getName());
        }

        // this is really a new set of equations, add it
        additionalEquations.add(additional);

    }

    /** {@inheritDoc} */
    public void addEventDetector(final EventDetector detector) {
        detectors.add(detector);
    }

    /** {@inheritDoc} */
    public Collection<EventDetector> getEventsDetectors() {
        return Collections.unmodifiableCollection(detectors);
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        detectors.clear();
    }

    /** Set up all user defined event detectors.
     */
    protected void setUpUserEventDetectors() {
        for (final EventDetector detector : detectors) {
            setUpEventDetector(integrator, detector);
        }
    }

    /** Wrap an Orekit event detector and register it to the integrator.
     * @param integ integrator into which event detector should be registered
     * @param detector event detector to wrap
     */
    protected void setUpEventDetector(final ODEIntegrator integ, final EventDetector detector) {
        integ.addEventHandler(new AdaptedEventDetector(detector),
                              detector.getMaxCheckInterval(),
                              detector.getThreshold(),
                              detector.getMaxIterationCount());
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #AbstractIntegratedPropagator(ODEIntegrator, boolean) constructor}. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setSlaveMode() {
        super.setSlaveMode();
        if (integrator != null) {
            integrator.clearStepHandlers();
        }
        modeHandler = null;
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #AbstractIntegratedPropagator(ODEIntegrator, boolean) constructor}. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setMasterMode(final OrekitStepHandler handler) {
        super.setMasterMode(handler);
        integrator.clearStepHandlers();
        final AdaptedStepHandler wrapped = new AdaptedStepHandler(handler);
        integrator.addStepHandler(wrapped);
        modeHandler = wrapped;
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #AbstractIntegratedPropagator(ODEIntegrator, boolean) constructor}. So if a specific
     * step handler is needed, it should be added after this method has been called.</p>
     */
    public void setEphemerisMode() {
        super.setEphemerisMode();
        integrator.clearStepHandlers();
        final EphemerisModeHandler ephemeris = new EphemerisModeHandler();
        modeHandler = ephemeris;
        integrator.addStepHandler(ephemeris);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Note that this method has the side effect of replacing the step handlers of the
     * underlying integrator set up in the {@link #AbstractIntegratedPropagator(ODEIntegrator,
     * boolean) constructor}.</p>
     */
    @Override
    public void setEphemerisMode(final OrekitStepHandler handler) {
        super.setEphemerisMode();
        integrator.clearStepHandlers();
        final EphemerisModeHandler ephemeris = new EphemerisModeHandler(handler);
        modeHandler = ephemeris;
        integrator.addStepHandler(ephemeris);
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris()
        throws IllegalStateException {
        if (getMode() != EPHEMERIS_GENERATION_MODE) {
            throw new OrekitIllegalStateException(OrekitMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
        }
        return ((EphemerisModeHandler) modeHandler).getEphemeris();
    }

    /** Create a mapper between raw double components and spacecraft state.
    /** Simple constructor.
     * <p>
     * The position parameter type is meaningful only if {@link
     * #getOrbitType() propagation orbit type}
     * support it. As an example, it is not meaningful for propagation
     * in {@link OrbitType#CARTESIAN Cartesian} parameters.
     * </p>
     * @param referenceDate reference date
     * @param mu central attraction coefficient (m³/s²)
     * @param orbitType orbit type to use for mapping
     * @param positionAngleType angle type to use for propagation
     * @param attitudeProvider attitude provider
     * @param frame inertial frame
     * @return new mapper
     */
    protected abstract StateMapper createMapper(AbsoluteDate referenceDate, double mu,
                                                OrbitType orbitType, PositionAngle positionAngleType,
                                                AttitudeProvider attitudeProvider, Frame frame);

    /** Get the differential equations to integrate (for main state only).
     * @param integ numerical integrator to use for propagation.
     * @return differential equations for main state
     */
    protected abstract MainStateEquations getMainStateEquations(ODEIntegrator integ);

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate target) throws OrekitException {
        if (getStartDate() == null) {
            if (getInitialState() == null) {
                throw new OrekitException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }
            setStartDate(getInitialState().getDate());
        }
        return propagate(getStartDate(), target);
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate tStart, final AbsoluteDate tEnd)
        throws OrekitException {

        if (getInitialState() == null) {
            throw new OrekitException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
        }

        if (!tStart.equals(getInitialState().getDate())) {
            // if propagation start date is not initial date,
            // propagate from initial to start date without event detection
            propagate(tStart, false);
        }

        // propagate from start date to end date with event detection
        return propagate(tEnd, true);

    }

    /** Propagation with or without event detection.
     * @param tEnd target date to which orbit should be propagated
     * @param activateHandlers if true, step and event handlers should be activated
     * @return state at end of propagation
     * @exception OrekitException if orbit cannot be propagated
     */
    protected SpacecraftState propagate(final AbsoluteDate tEnd, final boolean activateHandlers)
        throws OrekitException {
        try {

            if (getInitialState().getDate().equals(tEnd)) {
                // don't extrapolate
                return getInitialState();
            }

            // space dynamics view
            stateMapper = createMapper(getInitialState().getDate(), stateMapper.getMu(),
                                       stateMapper.getOrbitType(), stateMapper.getPositionAngleType(),
                                       stateMapper.getAttitudeProvider(), getInitialState().getFrame());


            // set propagation orbit type
            final Orbit initialOrbit = stateMapper.getOrbitType().convertType(getInitialState().getOrbit());
            if (Double.isNaN(getMu())) {
                setMu(initialOrbit.getMu());
            }

            if (getInitialState().getMass() <= 0.0) {
                throw new OrekitException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE,
                                          getInitialState().getMass());
            }

            integrator.clearEventHandlers();

            // set up events added by user
            setUpUserEventDetectors();

            // convert space flight dynamics API to math API
            final ODEState mathInitialState = createInitialState(getInitialIntegrationState());
            final ExpandableODE mathODE = createODE(integrator, mathInitialState);
            equationsMapper = mathODE.getMapper();
            mathInterpolator = null;

            // initialize mode handler
            if (modeHandler != null) {
                modeHandler.initialize(activateHandlers, tEnd);
            }

            // mathematical integration
            final ODEStateAndDerivative mathFinalState;
            try {
                beforeIntegration(getInitialState(), tEnd);
                mathFinalState = integrator.integrate(mathODE, mathInitialState,
                                                      tEnd.durationFrom(getInitialState().getDate()));
                afterIntegration();
            } catch (OrekitExceptionWrapper oew) {
                throw oew.getException();
            }

            // get final state
            SpacecraftState finalState =
                            stateMapper.mapArrayToState(stateMapper.mapDoubleToDate(mathFinalState.getTime(),
                                                                                    tEnd),
                                                        mathFinalState.getPrimaryState(),
                                                        meanOrbit);
            finalState = updateAdditionalStates(finalState);
            for (int i = 0; i < additionalEquations.size(); ++i) {
                final double[] secondary = mathFinalState.getSecondaryState(i + 1);
                finalState = finalState.addAdditionalState(additionalEquations.get(i).getName(),
                                                           secondary);
            }
            resetInitialState(finalState);
            setStartDate(finalState.getDate());

            return finalState;

        } catch (MathRuntimeException mre) {
            throw OrekitException.unwrap(mre);
        }
    }

    /** Get the initial state for integration.
     * @return initial state for integration
     * @exception OrekitException if initial state cannot be retrieved
     */
    protected SpacecraftState getInitialIntegrationState() throws OrekitException {
        return getInitialState();
    }

    /** Create an initial state.
     * @param initialState initial state in flight dynamics world
     * @return initial state in mathematics world
     * @exception OrekitException if initial state cannot be mapped
     */
    private ODEState createInitialState(final SpacecraftState initialState)
        throws OrekitException {

        // retrieve initial state
        final double[] primary  = new double[getBasicDimension()];
        stateMapper.mapStateToArray(initialState, primary);

        // secondary part of the ODE
        final double[][] secondary = new double[additionalEquations.size()][];
        for (int i = 0; i < additionalEquations.size(); ++i) {
            final AdditionalEquations additional = additionalEquations.get(i);
            secondary[i] = getInitialState().getAdditionalState(additional.getName());
        }

        return new ODEState(0.0, primary, secondary);

    }

    /** Create an ODE with all equations.
     * @param integ numerical integrator to use for propagation.
     * @param mathInitialState initial state
     * @return a new ode
     * @exception OrekitException if initial state cannot be mapped
     */
    private ExpandableODE createODE(final ODEIntegrator integ,
                                    final ODEState mathInitialState)
        throws OrekitException {

        final ExpandableODE ode =
                new ExpandableODE(new ConvertedMainStateEquations(getMainStateEquations(integ)));

        // secondary part of the ODE
        for (int i = 0; i < additionalEquations.size(); ++i) {
            final AdditionalEquations additional = additionalEquations.get(i);
            final SecondaryODE secondary =
                    new ConvertedSecondaryStateEquations(additional,
                                                         mathInitialState.getSecondaryStateDimension(i + 1));
            ode.addSecondaryEquations(secondary);
        }

        return ode;

    }

    /** Method called just before integration.
     * <p>
     * The default implementation does nothing, it may be specialized in subclasses.
     * </p>
     * @param initialState initial state
     * @param tEnd target date at which state should be propagated
     * @exception OrekitException if hook cannot be run
     */
    protected void beforeIntegration(final SpacecraftState initialState,
                                     final AbsoluteDate tEnd)
        throws OrekitException {
        // do nothing by default
    }

    /** Method called just after integration.
     * <p>
     * The default implementation does nothing, it may be specialized in subclasses.
     * </p>
     * @exception OrekitException if hook cannot be run
     */
    protected void afterIntegration()
        throws OrekitException {
        // do nothing by default
    }

    /** Get state vector dimension without additional parameters.
     * @return state vector dimension without additional parameters.
     */
    public int getBasicDimension() {
        return 7;

    }

    /** Get the integrator used by the propagator.
     * @return the integrator.
     */
    protected ODEIntegrator getIntegrator() {
        return integrator;
    }

    /** Get a complete state with all additional equations.
     * @param t current value of the independent <I>time</I> variable
     * @param y array containing the current value of the state vector
     * @return complete state
     * @exception OrekitException if state cannot be mapped
     */
    private SpacecraftState getCompleteState(final double t, final double[] y)
        throws OrekitException {

        // main state
        SpacecraftState state = stateMapper.mapArrayToState(t, y, true);  //not sure of the mean orbit, should be true

        // pre-integrated additional states
        state = updateAdditionalStates(state);

        // additional states integrated here
        if (!additionalEquations.isEmpty()) {

            for (int i = 0; i < additionalEquations.size(); ++i) {
                state = state.addAdditionalState(additionalEquations.get(i).getName(),
                                                 equationsMapper.extractEquationData(i + 1, y));
            }

        }

        return state;

    }

    /** Differential equations for the main state (orbit, attitude and mass). */
    public interface MainStateEquations {

        /**
         * Initialize the equations at the start of propagation. This method will be
         * called before any calls to {@link #computeDerivatives(SpacecraftState)}.
         *
         * <p> The default implementation of this method does nothing.
         *
         * @param initialState initial state information at the start of propagation.
         * @param target       date of propagation. Not equal to {@code
         *                     initialState.getDate()}.
         * @throws OrekitException if there is an Orekit related error during
         *                         initialization.
         */
        default void init(SpacecraftState initialState, AbsoluteDate target)
                throws OrekitException {
        }

        /** Compute differential equations for main state.
         * @param state current state
         * @return derivatives of main state
         * @throws OrekitException if differentials cannot be computed
         */
        double[] computeDerivatives(SpacecraftState state) throws OrekitException;

    }

    /** Differential equations for the main state (orbit, attitude and mass), with converted API. */
    private class ConvertedMainStateEquations implements OrdinaryDifferentialEquation {

        /** Main state equations. */
        private final MainStateEquations main;

        /** Simple constructor.
         * @param main main state equations
         */
        ConvertedMainStateEquations(final MainStateEquations main) {
            this.main = main;
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return getBasicDimension();
        }

        @Override
        public void init(final double t0, final double[] y0, final double finalTime) {
            try {
                // update space dynamics view
                // use only ODE elements
                SpacecraftState initialState = stateMapper.mapArrayToState(t0, y0, true);
                initialState = updateAdditionalStates(initialState);
                final AbsoluteDate target = stateMapper.mapDoubleToDate(finalTime);
                main.init(initialState, target);
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public double[] computeDerivatives(final double t, final double[] y)
            throws OrekitExceptionWrapper {
            try {

                // increment calls counter
                ++calls;

                // update space dynamics view
                // use only ODE elements
                SpacecraftState currentState = stateMapper.mapArrayToState(t, y, true);
                currentState = updateAdditionalStates(currentState);

                // compute main state differentials
                return main.computeDerivatives(currentState);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

    /** Differential equations for the secondary state (Jacobians, user variables ...), with converted API. */
    private class ConvertedSecondaryStateEquations implements SecondaryODE {

        /** Additional equations. */
        private final AdditionalEquations equations;

        /** Dimension of the additional state. */
        private final int dimension;

        /** Simple constructor.
         * @param equations additional equations
         * @param dimension dimension of the additional state
         */
        ConvertedSecondaryStateEquations(final AdditionalEquations equations,
                                         final int dimension) {
            this.equations = equations;
            this.dimension = dimension;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return dimension;
        }

        /** {@inheritDoc} */
        public double[] computeDerivatives(final double t, final double[] primary,
                                           final double[] primaryDot, final double[] secondary)
            throws OrekitExceptionWrapper {

            try {

                // update space dynamics view
                // the state contains only the ODE elements
                SpacecraftState currentState = stateMapper.mapArrayToState(t, primary, true);
                currentState = updateAdditionalStates(currentState);
                currentState = currentState.addAdditionalState(equations.getName(), secondary);

                // compute additional derivatives
                final double[] secondaryDot = new double[secondary.length];
                final double[] additionalMainDot =
                        equations.computeDerivatives(currentState, secondaryDot);
                if (additionalMainDot != null) {
                    // the additional equations have an effect on main equations
                    for (int i = 0; i < additionalMainDot.length; ++i) {
                        primaryDot[i] += additionalMainDot[i];
                    }
                }
                return secondaryDot;

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }

        }

    }

    /** Adapt an {@link org.orekit.propagation.events.EventDetector}
     * to Hipparchus {@link org.hipparchus.ode.events.ODEEventHandler} interface.
     * @param <T> class type for the generic version
     * @author Fabien Maussion
     */
    private class AdaptedEventDetector implements ODEEventHandler {

        /** Underlying event detector. */
        private final EventDetector detector;

        /** Time of the previous call to g. */
        private double lastT;

        /** Value from the previous call to g. */
        private double lastG;

        /** Build a wrapped event detector.
         * @param detector event detector to wrap
        */
        AdaptedEventDetector(final EventDetector detector) {
            this.detector = detector;
            this.lastT    = Double.NaN;
            this.lastG    = Double.NaN;
        }

        /** {@inheritDoc} */
        public void init(final ODEStateAndDerivative s0, final double t) {
            try {

                detector.init(getCompleteState(s0.getTime(), s0.getCompleteState()),
                              stateMapper.mapDoubleToDate(t));
                this.lastT = Double.NaN;
                this.lastG = Double.NaN;

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public double g(final ODEStateAndDerivative s) {
            try {
                if (!Precision.equals(lastT, s.getTime(), 0)) {
                    lastT = s.getTime();
                    lastG = detector.g(getCompleteState(s.getTime(), s.getCompleteState()));
                }
                return lastG;
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final ODEStateAndDerivative s, final boolean increasing) {
            try {

                final EventHandler.Action whatNext = detector.eventOccurred(getCompleteState(s.getTime(),
                                                                                             s.getCompleteState()),
                                                                            increasing);

                switch (whatNext) {
                    case STOP :
                        return Action.STOP;
                    case RESET_STATE :
                        return Action.RESET_STATE;
                    case RESET_DERIVATIVES :
                        return Action.RESET_DERIVATIVES;
                    default :
                        return Action.CONTINUE;
                }
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public ODEState resetState(final ODEStateAndDerivative s) {
            try {

                final SpacecraftState oldState = getCompleteState(s.getTime(), s.getCompleteState());
                final SpacecraftState newState = detector.resetState(oldState);

                // main part
                final double[] primary    = new double[s.getPrimaryStateDimension()];
                stateMapper.mapStateToArray(newState, primary);

                // secondary part
                final double[][] secondary    = new double[additionalEquations.size()][];
                for (int i = 0; i < additionalEquations.size(); ++i) {
                    secondary[i] = newState.getAdditionalState(additionalEquations.get(i).getName());
                }

                return new ODEState(newState.getDate().durationFrom(getStartDate()),
                                    primary, secondary);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

    /** Adapt an {@link org.orekit.propagation.sampling.OrekitStepHandler}
     * to Hipparchus {@link ODEStepHandler} interface.
     * @author Luc Maisonobe
     */
    private class AdaptedStepHandler
        implements OrekitStepInterpolator, ODEStepHandler, ModeHandler {

        /** Underlying handler. */
        private final OrekitStepHandler handler;

        /** Flag for handler . */
        private boolean activate;

        /** Build an instance.
         * @param handler underlying handler to wrap
         */
        AdaptedStepHandler(final OrekitStepHandler handler) {
            this.handler = handler;
        }

        /** {@inheritDoc} */
        public void initialize(final boolean activateHandlers,
                               final AbsoluteDate targetDate) {
            this.activate = activateHandlers;
        }

        /** {@inheritDoc} */
        public void init(final ODEStateAndDerivative s0, final double t) {
            try {
                handler.init(getCompleteState(s0.getTime(), s0.getCompleteState()),
                             stateMapper.mapDoubleToDate(t));
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public void handleStep(final ODEStateInterpolator interpolator, final boolean isLast) {
            try {
                mathInterpolator = interpolator;
                if (activate) {
                    handler.handleStep(this, isLast);
                }
            } catch (OrekitException pe) {
                throw new OrekitExceptionWrapper(pe);
            }
        }

        /** {@inheritDoc}} */
        @Override
        public SpacecraftState getPreviousState()
            throws OrekitException {
            return convert(mathInterpolator.getPreviousState());
        }

        /** {@inheritDoc}} */
        @Override
        public boolean isPreviousStateInterpolated() {
            return mathInterpolator.isPreviousStateInterpolated();
        }

        /** {@inheritDoc}} */
        @Override
        public SpacecraftState getCurrentState()
            throws OrekitException {
            return convert(mathInterpolator.getCurrentState());
        }

        /** {@inheritDoc}} */
        @Override
        public boolean isCurrentStateInterpolated() {
            return mathInterpolator.isCurrentStateInterpolated();
        }

        /** {@inheritDoc}} */
        @Override
        public SpacecraftState getInterpolatedState(final AbsoluteDate date)
            throws OrekitException {
            return convert(mathInterpolator.getInterpolatedState(date.durationFrom(getStartDate())));
        }

        /** Get the interpolated state.
         * @param os mathematical state
         * @return interpolated state at the current interpolation date
         * @exception OrekitException if state cannot be interpolated or converted
         * @exception OrekitException if underlying interpolator cannot handle
         * the date
         * @see #getInterpolatedDate()
         * @see #setInterpolatedDate(AbsoluteDate)
         */
        private SpacecraftState convert(final ODEStateAndDerivative os)
            throws OrekitException {

            SpacecraftState s =
                            stateMapper.mapArrayToState(os.getTime(),
                                                        os.getPrimaryState(),
                                                        meanOrbit);
            s = updateAdditionalStates(s);
            for (int i = 0; i < additionalEquations.size(); ++i) {
                final double[] secondary = os.getSecondaryState(i + 1);
                s = s.addAdditionalState(additionalEquations.get(i).getName(), secondary);
            }

            return s;

        }

        /** Check is integration direction is forward in date.
         * @return true if integration is forward in date
         */
        public boolean isForward() {
            return mathInterpolator.isForward();
        }

    }

    private class EphemerisModeHandler implements ModeHandler, ODEStepHandler {

        /** Underlying raw mathematical model. */
        private DenseOutputModel model;

        /** Generated ephemeris. */
        private BoundedPropagator ephemeris;

        /** Flag for handler . */
        private boolean activate;

        /** the user supplied end date. Propagation may not end on this date. */
        private AbsoluteDate endDate;

        /** User's integration step handler. May be null. */
        private final AdaptedStepHandler handler;

        /** Creates a new instance of EphemerisModeHandler which must be
         *  filled by the propagator.
         */
        EphemerisModeHandler() {
            this.handler = null;
        }

        /** Creates a new instance of EphemerisModeHandler which must be
         *  filled by the propagator.
         *  @param handler the handler to notify of every integrator step.
         */
        EphemerisModeHandler(final OrekitStepHandler handler) {
            this.handler = new AdaptedStepHandler(handler);
        }

        /** {@inheritDoc} */
        public void initialize(final boolean activateHandlers,
                               final AbsoluteDate targetDate) {
            this.activate = activateHandlers;
            this.model    = new DenseOutputModel();
            this.endDate  = targetDate;

            // ephemeris will be generated when last step is processed
            this.ephemeris = null;
            if (this.handler != null) {
                this.handler.initialize(activateHandlers, targetDate);
            }
        }

        /** Get the generated ephemeris.
         * @return a new instance of the generated ephemeris
         */
        public BoundedPropagator getEphemeris() {
            return ephemeris;
        }

        /** {@inheritDoc} */
        public void handleStep(final ODEStateInterpolator interpolator, final boolean isLast)
            throws OrekitExceptionWrapper {
            try {
                if (activate) {
                    if (this.handler != null) {
                        this.handler.handleStep(interpolator, isLast);
                    }

                    model.handleStep(interpolator, isLast);
                    if (isLast) {

                        // set up the boundary dates
                        final double tI = model.getInitialTime();
                        final double tF = model.getFinalTime();
                        // tI is almost? always zero
                        final AbsoluteDate startDate =
                                stateMapper.mapDoubleToDate(tI);
                        final AbsoluteDate finalDate =
                                stateMapper.mapDoubleToDate(tF, this.endDate);
                        final AbsoluteDate minDate;
                        final AbsoluteDate maxDate;
                        if (tF < tI) {
                            minDate = finalDate;
                            maxDate = startDate;
                        } else {
                            minDate = startDate;
                            maxDate = finalDate;
                        }

                        // get the initial additional states that are not managed
                        final Map<String, double[]> unmanaged = new HashMap<String, double[]>();
                        for (final Map.Entry<String, double[]> initial : getInitialState().getAdditionalStates().entrySet()) {
                            if (!isAdditionalStateManaged(initial.getKey())) {
                                // this additional state was in the initial state, but is unknown to the propagator
                                // we simply copy its initial value as is
                                unmanaged.put(initial.getKey(), initial.getValue());
                            }
                        }

                        // get the names of additional states managed by differential equations
                        final String[] names = new String[additionalEquations.size()];
                        for (int i = 0; i < names.length; ++i) {
                            names[i] = additionalEquations.get(i).getName();
                        }

                        // create the ephemeris
                        ephemeris = new IntegratedEphemeris(startDate, minDate, maxDate,
                                                            stateMapper, meanOrbit, model, unmanaged,
                                                            getAdditionalStateProviders(), names);

                    }
                }
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public void init(final ODEStateAndDerivative s0, final double t) {
            model.init(s0, t);
            if (this.handler != null) {
                this.handler.init(s0, t);
            }
        }

    }

}
