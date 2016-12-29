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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.ode.FieldDenseOutputModel;
import org.hipparchus.ode.FieldEquationsMapper;
import org.hipparchus.ode.FieldExpandableODE;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.FieldODEState;
import org.hipparchus.ode.FieldODEStateAndDerivative;
import org.hipparchus.ode.FieldOrdinaryDifferentialEquation;
import org.hipparchus.ode.FieldSecondaryODE;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.events.FieldODEEventHandler;
import org.hipparchus.ode.sampling.FieldODEStateInterpolator;
import org.hipparchus.ode.sampling.FieldODEStepHandler;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitIllegalStateException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldAbstractPropagator;
import org.orekit.propagation.FieldBoundedPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.sampling.FieldOrekitStepHandler;
import org.orekit.propagation.sampling.FieldOrekitStepInterpolator;
import org.orekit.time.FieldAbsoluteDate;


/** Common handling of {@link org.orekit.propagation.FieldPropagator FieldPropagator}
 *  methods for both numerical and semi-analytical propagators.
 *  @author Luc Maisonobe
 */
public abstract class FieldAbstractIntegratedPropagator<T extends RealFieldElement<T>> extends FieldAbstractPropagator<T> {

    /** Event detectors not related to force models. */
    private final List<FieldEventDetector<T>> detectors;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final FieldODEIntegrator<T> integrator;

    /** Mode handler. */
    private FieldModeHandler<T> modeHandler;

    /** Additional equations. */
    private List<FieldAdditionalEquations<T>> additionalEquations;

    /** Counter for differential equations calls. */
    private int calls;

    /** Mapper between raw double components and space flight dynamics objects. */
    private FieldStateMapper<T> stateMapper;

    /** Equations mapper. */
    private FieldEquationsMapper<T> equationsMapper;

    /** Underlying raw rawInterpolator. */
    private FieldODEStateInterpolator<T> mathInterpolator;

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
     * @param field Field used by default
     */
    protected FieldAbstractIntegratedPropagator(final Field<T> field, final FieldODEIntegrator<T> integrator, final boolean meanOrbit) {
        super(field);
        detectors           = new ArrayList<FieldEventDetector<T>>();
        additionalEquations = new ArrayList<FieldAdditionalEquations<T>>();
        this.integrator     = integrator;
        this.meanOrbit      = meanOrbit;
    }

    /** Initialize the mapper. */
    protected void initMapper() {
        stateMapper = createMapper(null, Double.NaN, null, null, null, null);
    }

    /**  {@inheritDoc} */
    public void setAttitudeProvider(final FieldAttitudeProvider<T> attitudeProvider) {
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
     * <p>The number of calls is reset each time the {@link #propagate(FieldAbsoluteDate)}
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
        for (final FieldAdditionalEquations<T> equation : additionalEquations) {
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
    public void addAdditionalEquations(final FieldAdditionalEquations<T> additional)
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
    public <D extends FieldEventDetector<T>> void addEventDetector(final D detector) {
        detectors.add(detector);
    }

    /** {@inheritDoc} */
    public Collection<FieldEventDetector<T>> getEventsDetectors() {
        return Collections.unmodifiableCollection(detectors);
    }

    /** {@inheritDoc} */
    public void clearEventsDetectors() {
        detectors.clear();
    }

    /** Set up all user defined event detectors.
     */
    protected void setUpUserEventDetectors() {
        for (final FieldEventDetector<T> detector : detectors) {
            setUpEventDetector(integrator, detector);
        }
    }

    /** Wrap an Orekit event detector and register it to the integrator.
     * @param integ integrator into which event detector should be registered
     * @param detector event detector to wrap
     */
    protected void setUpEventDetector(final FieldODEIntegrator<T> integ, final FieldEventDetector<T> detector) {
        integ.addEventHandler(new FieldAdaptedEventDetector(detector),
                              detector.getMaxCheckInterval().getReal(),
                              detector.getThreshold().getReal(),
                              detector.getMaxIterationCount());
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #FieldAbstractIntegratedPropagator(Field, FieldODEIntegrator, boolean) constructor}. So if a specific
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
     * #FieldAbstractIntegratedPropagator(Field, FieldODEIntegrator, boolean) constructor}. So if a specific
     * step handler is needed, it should be added after this method has been called.</p>
     */
    public void setMasterMode(final FieldOrekitStepHandler<T> handler) {
        super.setMasterMode(handler);
        integrator.clearStepHandlers();
        final FieldAdaptedStepHandler wrapped = new FieldAdaptedStepHandler(handler);
        integrator.addStepHandler(wrapped);
        modeHandler = wrapped;
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #FieldAbstractIntegratedPropagator(Field, FieldODEIntegrator, boolean) constructor}. So if a specific
     * step handler is needed, it should be added after this method has been called.</p>
     */
    public void setEphemerisMode() {
        super.setEphemerisMode();
        integrator.clearStepHandlers();
        final FieldEphemerisModeHandler ephemeris = new FieldEphemerisModeHandler();
        modeHandler = ephemeris;
        integrator.addStepHandler(ephemeris);
    }

    /** {@inheritDoc} */
    public FieldBoundedPropagator<T> getGeneratedEphemeris()
        throws IllegalStateException {
        if (getMode() != EPHEMERIS_GENERATION_MODE) {
            throw new OrekitIllegalStateException(OrekitMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
        }
        return ((FieldEphemerisModeHandler) modeHandler).getEphemeris();
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
    protected abstract FieldStateMapper<T> createMapper(FieldAbsoluteDate<T> referenceDate, double mu,
                                                        OrbitType orbitType, PositionAngle positionAngleType,
                                                        FieldAttitudeProvider<T> attitudeProvider, Frame frame);

    /** Get the differential equations to integrate (for main state only).
     * @param integ numerical integrator to use for propagation.
     * @return differential equations for main state
     */
    protected abstract MainStateEquations<T> getMainStateEquations(FieldODEIntegrator<T> integ);

    /** {@inheritDoc} */
    public FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> target) throws OrekitException {
        try {
            if (getStartDate() == null) {
                if (getInitialState() == null) {
                    throw new OrekitException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
                }
                setStartDate(getInitialState().getDate());
            }
            return propagate(getStartDate(), target);
        } catch (OrekitException oe) {

            // recover a possible embedded OrekitException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof OrekitException) {
                    throw (OrekitException) t;
                }
            }
            throw new OrekitException(oe);

        }
    }

    /** {@inheritDoc} */
    public FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> tStart, final FieldAbsoluteDate<T> tEnd)
        throws OrekitException {
        try {

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

        } catch (OrekitException oe) {

            // recover a possible embedded OrekitException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof OrekitException) {
                    throw (OrekitException) t;
                }
            }
            throw new OrekitException(oe);

        }
    }

    /** Propagation with or without event detection.
     * @param tEnd target date to which orbit should be propagated
     * @param activateHandlers if true, step and event handlers should be activated
     * @return state at end of propagation
     * @exception OrekitException if orbit cannot be propagated
     */
    protected FieldSpacecraftState<T> propagate(final FieldAbsoluteDate<T> tEnd, final boolean activateHandlers)
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
            final FieldOrbit<T> initialOrbit = stateMapper.getOrbitType().convertType(getInitialState().getOrbit());
            if (Double.isNaN(getMu())) {
                setMu(initialOrbit.getMu());
            }
            if (getInitialState().getMass().getReal() <= 0.0) {
                throw new OrekitException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE,
                                               getInitialState().getMass());
            }

            integrator.clearEventHandlers();
            // set up events added by user
            setUpUserEventDetectors();

            // convert space flight dynamics API to math API
            final FieldODEState<T> mathInitialState = createInitialState(getInitialIntegrationState());
            final FieldExpandableODE<T> mathODE = createODE(integrator, mathInitialState);
            equationsMapper = mathODE.getMapper();
            mathInterpolator = null;
            // initialize mode handler
            if (modeHandler != null) {
                modeHandler.initialize(activateHandlers, tEnd);
            }
            // mathematical integration
            final FieldODEStateAndDerivative<T> mathFinalState;
            try {

                beforeIntegration(getInitialState(), tEnd);
                mathFinalState = integrator.integrate(mathODE, mathInitialState,
                                                      tEnd.durationFrom(getInitialState().getDate()));

                afterIntegration();
            } catch (OrekitExceptionWrapper oew) {
                throw oew.getException();
            }
            // get final state
            FieldSpacecraftState<T> finalState =
                            stateMapper.mapArrayToState(stateMapper.mapDoubleToDate(mathFinalState.getTime(),
                                                                                    tEnd),
                                                        mathFinalState.getPrimaryState(),
                                                        meanOrbit);
            finalState = updateAdditionalStates(finalState);
            for (int i = 0; i < additionalEquations.size(); ++i) {
                final T[] secondary = mathFinalState.getSecondaryState(i + 1);
                finalState = finalState.addAdditionalState(additionalEquations.get(i).getName(),
                                                           secondary);
            }
            resetInitialState(finalState);
            setStartDate(finalState.getDate());

            return finalState;

        } catch (OrekitException pe) {
            throw pe;
        } catch (MathIllegalArgumentException miae) {
            throw OrekitException.unwrap(miae);
        } catch (MathIllegalStateException mise) {
            throw OrekitException.unwrap(mise);
        }
    }

    /** Get the initial state for integration.
     * @return initial state for integration
     * @exception OrekitException if initial state cannot be retrieved
     */
    protected FieldSpacecraftState<T> getInitialIntegrationState() throws OrekitException {
        return getInitialState();
    }

    /** Create an initial state.
     * @param initialState initial state in flight dynamics world
     * @return initial state in mathematics world
     * @exception OrekitException if initial state cannot be mapped
     */
    private FieldODEState<T> createInitialState(final FieldSpacecraftState<T> initialState)
        throws OrekitException {

        // retrieve initial state
        final T[] primary  = MathArrays.buildArray(initialState.getA().getField(), getBasicDimension());
        stateMapper.mapStateToArray(initialState, primary);

        // secondary part of the ODE
        final T[][] secondary = MathArrays.buildArray(initialState.getA().getField(), additionalEquations.size(), -1);
        for (int i = 0; i < additionalEquations.size(); ++i) {
            final FieldAdditionalEquations<T> additional = additionalEquations.get(i);
            final T[] addState = getInitialState().getAdditionalState(additional.getName());
            secondary[i] = MathArrays.buildArray(initialState.getA().getField(), addState.length);
            for (int j = 0; j < addState.length; j++)
                secondary[i][j] = addState[j];
           //TODO secondary[i] = ;
        }

        return new FieldODEState<T>(initialState.getA().getField().getZero(), primary, secondary);

    }

    /** Create an ODE with all equations.
     * @param integ numerical integrator to use for propagation.
     * @param mathInitialState initial state
     * @return a new ode
     * @exception OrekitException if initial state cannot be mapped
     */
    private FieldExpandableODE<T> createODE(final FieldODEIntegrator<T> integ,
                                    final FieldODEState<T> mathInitialState)
        throws OrekitException {

        final FieldExpandableODE<T> ode =
                new FieldExpandableODE<T>(new ConvertedMainStateEquations(getMainStateEquations(integ)));

        // secondary part of the ODE
        for (int i = 0; i < additionalEquations.size(); ++i) {
            final FieldAdditionalEquations<T> additional = additionalEquations.get(i);
            final FieldSecondaryODE<T> secondary =
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
    protected void beforeIntegration(final FieldSpacecraftState<T> initialState,
                                     final FieldAbsoluteDate<T> tEnd)
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
    protected FieldODEIntegrator<T> getIntegrator() {
        return integrator;
    }

    /** Get a complete state with all additional equations.
     * @param t current value of the independent <I>time</I> variable
     * @param ts array containing the current value of the state vector
     * @return complete state
     * @exception OrekitException if state cannot be mapped
     */
    private FieldSpacecraftState<T> getCompleteState(final T t, final T[] ts)
        throws OrekitException {

        // main state
        FieldSpacecraftState<T> state = stateMapper.mapArrayToState(t, ts, true);  //not sure of the mean orbit, should be true
        // pre-integrated additional states
        state = updateAdditionalStates(state);

        // additional states integrated here
        if (!additionalEquations.isEmpty()) {

            for (int i = 0; i < additionalEquations.size(); ++i) {
                state = state.addAdditionalState(additionalEquations.get(i).getName(),
                                                 equationsMapper.extractEquationData(i + 1, ts));
            }

        }

        return state;

    }

    /** Differential equations for the main state (orbit, attitude and mass). */
    public interface MainStateEquations<T extends RealFieldElement<T>> {

        /** Compute differential equations for main state.
         * @param state current state
         * @return derivatives of main state
         * @throws OrekitException if differentials cannot be computed
         */
        T[] computeDerivatives(FieldSpacecraftState<T> state) throws OrekitException;

    }

    /** Differential equations for the main state (orbit, attitude and mass), with converted API. */
    private class ConvertedMainStateEquations implements FieldOrdinaryDifferentialEquation<T> {

        /** Main state equations. */
        private final MainStateEquations<T> main;

        /** Simple constructor.
         * @param main main state equations
         */
        ConvertedMainStateEquations(final MainStateEquations<T> main) {
            this.main = main;
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return getBasicDimension();
        }

        /** {@inheritDoc} */
        public T[] computeDerivatives(final T t, final T[] y)
            throws OrekitExceptionWrapper {
            try {

                // increment calls counter
                ++calls;

                // update space dynamics view
                // use only ODE elements
                FieldSpacecraftState<T> currentState = stateMapper.mapArrayToState(t, y, true);
                currentState = updateAdditionalStates(currentState);

                // compute main state differentials
                return main.computeDerivatives(currentState);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

    /** Differential equations for the secondary state (Jacobians, user variables ...), with converted API. */
    private class ConvertedSecondaryStateEquations implements FieldSecondaryODE<T> {

        /** Additional equations. */
        private final FieldAdditionalEquations<T> equations;

        /** Dimension of the additional state. */
        private final int dimension;

        /** Simple constructor.
         * @param equations additional equations
         * @param dimension dimension of the additional state
         */
        ConvertedSecondaryStateEquations(final FieldAdditionalEquations<T> equations,
                                         final int dimension) {
            this.equations = equations;
            this.dimension = dimension;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return dimension;
        }

        /** {@inheritDoc} */
        public T[] computeDerivatives(final T t, final T[] primary,
                                           final T[] primaryDot, final T[] secondary)
            throws OrekitExceptionWrapper {

            try {

                // update space dynamics view
                // the state contains only the ODE elements
                FieldSpacecraftState<T> currentState = stateMapper.mapArrayToState(t, primary, true);
                currentState = updateAdditionalStates(currentState);
                currentState = currentState.addAdditionalState(equations.getName(), secondary);

                // compute additional derivatives
                final T[] secondaryDot = MathArrays.buildArray(getField(), secondary.length);
                final T[] additionalMainDot =
                        equations.computeDerivatives(currentState, secondaryDot);
                if (additionalMainDot != null) {
                    // the additional equations have an effect on main equations
                    for (int i = 0; i < additionalMainDot.length; ++i) {
                        primaryDot[i] = primaryDot[i].add(additionalMainDot[i]);
                    }
                }
                return secondaryDot;

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }

        }

    }

    /** Adapt an {@link org.orekit.propagation.events.FieldEventDetector<T>}
     * to Hipparchus {@link org.hipparchus.ode.events.FieldODEEventHandler<T>} interface.
     * @param <T> class type for the generic version
     * @author Fabien Maussion
     */
    private class FieldAdaptedEventDetector implements FieldODEEventHandler<T> {

        /** Underlying event detector. */
        private final FieldEventDetector<T> detector;

        /** Time of the previous call to g. */
        private T lastT;

        /** Value from the previous call to g. */
        private T lastG;

        /** Build a wrapped event detector.
         * @param detector event detector to wrap
        */
        FieldAdaptedEventDetector(final FieldEventDetector<T> detector) {
            this.detector = detector;
            this.lastT    = getField().getZero().add(Double.NaN);
            this.lastG    = getField().getZero().add(Double.NaN);
        }

        /** {@inheritDoc} */
        public void init(final FieldODEStateAndDerivative<T> s0, final T t) {
            try {

                detector.init(getCompleteState(s0.getTime(), s0.getCompleteState()),
                              stateMapper.mapDoubleToDate(t));
                this.lastT = getField().getZero().add(Double.NaN);
                this.lastG = getField().getZero().add(Double.NaN);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public T g(final FieldODEStateAndDerivative<T> s) {
            try {
                if (!Precision.equals(lastT.getReal(), s.getTime().getReal(), 0)) {
                    lastT = s.getTime();
                    lastG = detector.g(getCompleteState(s.getTime(), s.getCompleteState()));
                }
                return lastG;
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final FieldODEStateAndDerivative<T> s, final boolean increasing) {
            try {

                final FieldEventHandler.Action whatNext = detector.eventOccurred(getCompleteState(s.getTime(),
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
        public FieldODEState<T> resetState(final FieldODEStateAndDerivative<T> s) {
            try {

                final FieldSpacecraftState<T> oldState = getCompleteState(s.getTime(), s.getCompleteState());
                final FieldSpacecraftState<T> newState = detector.resetState(oldState);

                // main part
                final T[] primary    = MathArrays.buildArray(getField(), s.getPrimaryStateDimension());
                stateMapper.mapStateToArray(newState, primary);

                // secondary part
                final T[][] secondary    = MathArrays.buildArray(getField(), additionalEquations.size(), -1);

                for (int i = 0; i < additionalEquations.size(); ++i) {
                    final FieldAdditionalEquations<T> additional = additionalEquations.get(i);
                    final T[] NState = newState.getAdditionalState(additional.getName());
                    secondary[i] = MathArrays.buildArray(getField(), NState.length);
                    for (int j = 0; j < NState.length; j++)
                        secondary[i][j] = NState[j];
                }

                return new FieldODEState<T>(newState.getDate().durationFrom(getStartDate()),
                                    primary, secondary);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

    /** Adapt an {@link org.orekit.propagation.sampling.FieldOrekitStepHandler<T>}
     * to Hipparchus {@link FieldODEStepHandler<T>} interface.
     * @author Luc Maisonobe
     */
    private class FieldAdaptedStepHandler
        implements FieldOrekitStepInterpolator<T>, FieldODEStepHandler<T>, FieldModeHandler<T> {

        /** Underlying handler. */
        private final FieldOrekitStepHandler<T> handler;

        /** Flag for handler . */
        private boolean activate;

        /** Build an instance.
         * @param handler underlying handler to wrap
         */
        FieldAdaptedStepHandler(final FieldOrekitStepHandler<T> handler) {
            this.handler = handler;
        }

        /** {@inheritDoc} */
        public void initialize(final boolean activateHandlers,
                               final FieldAbsoluteDate<T> targetDate) {
            this.activate = activateHandlers;
        }
        /** {@inheritDoc} */
        public void init(final FieldODEStateAndDerivative<T> s0, final T t) {
            try {
                handler.init(getCompleteState(s0.getTime(), s0.getCompleteState()),
                             stateMapper.mapDoubleToDate(t));
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public void handleStep(final FieldODEStateInterpolator<T> interpolator, final boolean isLast) {
            try {
                mathInterpolator = interpolator;
                if (activate) {
                    handler.handleStep(this, isLast);
                }
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc}} */
        @Override
        public FieldSpacecraftState<T> getPreviousState()
            throws OrekitException {
            return convert(mathInterpolator.getPreviousState());
        }

        /** {@inheritDoc}} */
        @Override
        public FieldSpacecraftState<T> getCurrentState()
            throws OrekitException {
            return convert(mathInterpolator.getCurrentState());
        }

        /** {@inheritDoc}} */
        @Override
        public FieldSpacecraftState<T> getInterpolatedState(final FieldAbsoluteDate<T> date)
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
         * @see #setInterpolatedDate(FieldAbsoluteDate<T>)
         */
        private FieldSpacecraftState<T> convert(final FieldODEStateAndDerivative<T> os)
            throws OrekitException {
            try {

                FieldSpacecraftState<T> s =
                        stateMapper.mapArrayToState(os.getTime(),
                                                    os.getPrimaryState(),
                                                    meanOrbit);
                s = updateAdditionalStates(s);
                for (int i = 0; i < additionalEquations.size(); ++i) {
                    final T[] secondary = os.getSecondaryState(i + 1);
                    s = s.addAdditionalState(additionalEquations.get(i).getName(), secondary);
                }

                return s;

            } catch (OrekitException oe) {
                throw new OrekitException(oe);
            } catch (OrekitExceptionWrapper oew) {
                throw new OrekitException(oew.getException());
            }
        }

        /** Check is integration direction is forward in date.
         * @return true if integration is forward in date
         */
        public boolean isForward() {
            return mathInterpolator.isForward();
        }

    }

    private class FieldEphemerisModeHandler implements FieldModeHandler<T>, FieldODEStepHandler<T> {

        /** Underlying raw mathematical model. */
        private FieldDenseOutputModel<T> model;

        /** Generated ephemeris. */
        private FieldBoundedPropagator<T> ephemeris;

        /** Flag for handler . */
        private boolean activate;

        /** the user supplied end date. Propagation may not end on this date. */
        private FieldAbsoluteDate<T> endDate;

        /** Creates a new instance of FieldEphemerisModeHandler which must be
         *  filled by the propagator.
         */
        FieldEphemerisModeHandler() {
        }

        /** {@inheritDoc} */
        public void initialize(final boolean activateHandlers,
                               final FieldAbsoluteDate<T> targetDate) {
            this.activate = activateHandlers;
            this.model    = new FieldDenseOutputModel<T>();
            this.endDate  = targetDate;

            // ephemeris will be generated when last step is processed
            this.ephemeris = null;

        }

        /** Get the generated ephemeris.
         * @return a new instance of the generated ephemeris
         */
        public FieldBoundedPropagator<T> getEphemeris() {
            return ephemeris;
        }

        /** {@inheritDoc} */
        public void handleStep(final FieldODEStateInterpolator<T> interpolator, final boolean isLast)
            throws OrekitExceptionWrapper {
            try {
                if (activate) {
                    model.handleStep(interpolator, isLast);
                    if (isLast) {

                        // set up the boundary dates
                        final T tI = model.getInitialTime();
                        final T tF = model.getFinalTime();
                        // tI is almost? always zero
                        final FieldAbsoluteDate<T> startDate =
                                stateMapper.mapDoubleToDate(tI);
                        final FieldAbsoluteDate<T> finalDate =
                                stateMapper.mapDoubleToDate(tF, this.endDate);
                        final FieldAbsoluteDate<T> minDate;
                        final FieldAbsoluteDate<T> maxDate;
                        if (tF.getReal() < tI.getReal()) {
                            minDate = finalDate;
                            maxDate = startDate;
                        } else {
                            minDate = startDate;
                            maxDate = finalDate;
                        }

                        // get the initial additional states that are not managed
                        final Map<String, T[]> unmanaged = new HashMap<String, T[]>();
                        for (final Map.Entry<String, T[]> initial : getInitialState().getAdditionalStates().entrySet()) {
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
                        ephemeris = new FieldIntegratedEphemeris<T>(startDate, minDate, maxDate,
                                                            stateMapper, meanOrbit, model, unmanaged,
                                                            getAdditionalStateProviders(), names);

                    }
                }
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public void init(final FieldODEStateAndDerivative<T> s0, final T t) {
            model.init(s0, t);
        }

    }

}
