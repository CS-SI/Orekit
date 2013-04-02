/* Copyright 2002-2013 CS Systèmes d'Information
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

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.ContinuousOutputModel;
import org.apache.commons.math3.ode.ExpandableStatefulODE;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.SecondaryEquations;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
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
    private final AbstractIntegrator integrator;

    /** Mode handler. */
    private ModeHandler modeHandler;

    /** Additional equations and associated data. */
    private List<AdditionalEquationsAndData> addEquationsAndData;

    /** State vector. */
    private double[] stateVector;

    /** Counter for differential equations calls. */
    private int calls;

    /** Mapper between raw double components and space flight dynamics objects. */
    private StateMapper stateMapper;

    /** Build a new instance.
     * @param integrator numerical integrator to use for propagation.
     */
    protected AbstractIntegratedPropagator(final AbstractIntegrator integrator) {
        detectors           = new ArrayList<EventDetector>();
        addEquationsAndData = new ArrayList<AdditionalEquationsAndData>();
        stateVector         = new double[7];
        this.integrator     = integrator;
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

    /** Set the central attraction coefficient &mu;.
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public void setMu(final double mu) {
        stateMapper = createMapper(stateMapper.getReferenceDate(), mu,
                                   stateMapper.getOrbitType(), stateMapper.getPositionAngleType(),
                                   stateMapper.getAttitudeProvider(), stateMapper.getFrame());
    }

    /** Get the central attraction coefficient &mu;.
     * @return mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
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

    /** Add a set of user-specified equations to be integrated along with the orbit propagation.
     * @param addEqu additional equations
     * @see #setInitialAdditionalState(String, double[])
     * @exception OrekitException if a set of equations with the same name is already present
     */
    public void addAdditionalEquations(final AdditionalEquations addEqu)
        throws OrekitException {

        // check if the name is already used
        for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
            if (stateAndEqu.getEquations().getName().equals(addEqu.getName())) {
                // this set of equations is already registered, complain
                throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE, addEqu.getName());
            }
        }

        // this is really a new set of equations, add it
        addEquationsAndData.add(new AdditionalEquationsAndData(addEqu));

    }

    /** Set initial additional state.
     * @param name name of the additional equations whose initial state is set
     * @param addState additional state
     * @throws OrekitException if additional equation is unknown
     * @see #addAdditionalEquations(AdditionalEquations)
     */
    public void setInitialAdditionalState(final String name, final double[] addState)
        throws OrekitException {
        for (AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
            if (stateAndEqu.getEquations().getName().equals(name)) {
                stateAndEqu.getData().setAdditionalState(addState);
                return;
            }
        }
        throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_EQUATION, name);
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
            setUpEventDetector(detector);
        }
    }

    /** Wrap an Orekit event detector and register it to the integrator.
     *  @param detector event detector to wrap
     */
    protected void setUpEventDetector(final EventDetector detector) {
        integrator.addEventHandler(new AdaptedEventDetector(detector),
                                   detector.getMaxCheckInterval(),
                                   detector.getThreshold(),
                                   detector.getMaxIterationCount());
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #AbstractIntegratedPropagator(FirstOrderIntegrator) constructor}. So if a specific
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
     * #AbstractIntegratedPropagator(FirstOrderIntegrator) constructor}. So if a specific
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
     * #AbstractIntegratedPropagator(FirstOrderIntegrator) constructor}. So if a specific
     * step handler is needed, it should be added after this method has been called.</p>
     */
    public void setEphemerisMode() {
        super.setEphemerisMode();
        integrator.clearStepHandlers();
        final EphemerisModeHandler ephemeris = new EphemerisModeHandler();
        modeHandler = ephemeris;
        integrator.addStepHandler(ephemeris);
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris()
        throws IllegalStateException {
        if (getMode() != EPHEMERIS_GENERATION_MODE) {
            throw OrekitException.createIllegalStateException(OrekitMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
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
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param orbitType orbit type to use for mapping
     * @param positionAngleType angle type to use for propagation
     * @param attitudeProvider attitude provider
     * @param frame inertial frame
     * @return new mapper
     */
    protected abstract StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                                final OrbitType orbitType, final PositionAngle positionAngleType,
                                                final AttitudeProvider attitudeProvider, final Frame frame);

    /** Get the differential equations to integrate (for main state only).
     * @return differential equations for main state
     */
    protected abstract MainStateEquations getMainStateEquations();

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
        try {
            if (getStartDate() == null) {
                if (getInitialState() == null) {
                    throw new PropagationException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
                }
                setStartDate(getInitialState().getDate());
            }
            return propagate(getStartDate(), target);
        } catch (OrekitException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }
            throw new PropagationException(oe);

        }
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate tStart, final AbsoluteDate tEnd)
        throws PropagationException {
        try {

            if (getInitialState() == null) {
                throw new PropagationException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }

            if (!tStart.equals(getInitialState().getDate())) {
                // if propagation start date is not initial date,
                // propagate from initial to start date without event detection
                propagate(tStart, false);
            }

            // propagate from start date to end date with event detection
            return propagate(tEnd, true);

        } catch (OrekitException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }
            throw new PropagationException(oe);

        }
    }

    /** Propagation with or without event detection.
     * @param tEnd target date to which orbit should be propagated
     * @param activateHandlers if true, step and event handlers should be activated
     * @return state at end of propagation
     * @exception PropagationException if orbit cannot be propagated
     */
    protected SpacecraftState propagate(final AbsoluteDate tEnd, final boolean activateHandlers)
        throws PropagationException {
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

            // creating state vector
            this.stateVector  = new double[getBasicDimension()];

            if (getInitialState().getMass() <= 0.0) {
                throw new PropagationException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE,
                                               getInitialState().getMass());
            }

            integrator.clearEventHandlers();

            // set up events added by user
            setUpUserEventDetectors();

            // Map state to array
            stateMapper.mapStateToArray(getInitialState(), stateVector);

            // convert space flight dynamics API to math API
            final double t0 = 0;
            final double t1 = tEnd.durationFrom(getInitialState().getDate());
            final ExpandableStatefulODE mathODE =
                    new ExpandableStatefulODE(new ConvertedMainStateEquations(getMainStateEquations()));
            mathODE.setTime(t0);
            mathODE.setPrimaryState(stateVector);
            final Map<String, Integer> map = new HashMap<String, Integer>();
            for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                final SecondaryEquations secondary =
                        new ConvertedSecondaryStateEquations(stateAndEqu.getEquations(),
                                                             stateAndEqu.getData().getAdditionalState().length);
                final int index = mathODE.addSecondaryEquations(secondary);
                map.put(stateAndEqu.getEquations().getName(), index);
                mathODE.setSecondaryState(index, stateAndEqu.getData().getAdditionalState());
                stateAndEqu.setIndex(index);
            }

            // initialize mode handler
            if (modeHandler != null) {
                modeHandler.initialize(map, activateHandlers);
            }

            // mathematical integration
            try {
                beforeIntegration(getInitialState(), tEnd);
                integrator.integrate(mathODE, t1);
                afterIntegration();
            } catch (OrekitExceptionWrapper oew) {
                throw oew.getException();
            }

            // get final state
            final SpacecraftState finalState = stateMapper.mapArrayToState(mathODE.getTime(),
                                                                           mathODE.getPrimaryState());
            resetInitialState(finalState);
            setStartDate(finalState.getDate());

            // get final additional state
            for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                final double[] addState = stateAndEqu.getData().getAdditionalState();
                System.arraycopy(mathODE.getSecondaryState(stateAndEqu.getIndex()), 0,
                                 addState, 0, addState.length);
            }

            return finalState;

        } catch (PropagationException pe) {
            throw pe;
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        } catch (MathIllegalArgumentException miae) {
            throw PropagationException.unwrap(miae);
        } catch (MathIllegalStateException mise) {
            throw PropagationException.unwrap(mise);
        }
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

    /** Differential equations for the main state (orbit, attitude and mass). */
    public interface MainStateEquations {

        /** Compute differential equations for main state.
         * @param state current state
         * @return derivatives of main state
         * @throws OrekitException if differentials cannot be computed
         */
        double[] computeDerivatives(final SpacecraftState state) throws OrekitException;

    }

    /** Differential equations for the main state (orbit, attitude and mass), with converted API. */
    private class ConvertedMainStateEquations implements FirstOrderDifferentialEquations {

        /** Main state equations. */
        private final MainStateEquations main;

        /** Simple constructor.
         * @param main main state equations
         */
        public ConvertedMainStateEquations(final MainStateEquations main) {
            this.main = main;
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return getBasicDimension();
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            throws OrekitExceptionWrapper {

            try {

                // update space dynamics view
                final SpacecraftState currentState = stateMapper.mapArrayToState(t, y);

                // compute main state differentials
                final double[] mainDot = main.computeDerivatives(currentState);
                System.arraycopy(mainDot, 0, yDot, 0, mainDot.length);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }


            // increment calls counter
            ++calls;

        }

    }

    /** Differential equations for the secondary state (Jacobians, user variables ...), with converted API. */
    private class ConvertedSecondaryStateEquations implements SecondaryEquations {

        /** Additional equations. */
        private final AdditionalEquations equations;

        /** Dimension of the additional state. */
        private final int dimension;

        /** Simple constructor.
         * @param equations additional equations
         * @param dimension dimension of the additional state
         */
        public ConvertedSecondaryStateEquations(final AdditionalEquations equations,
                                                final int dimension) {
            this.equations = equations;
            this.dimension = dimension;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return dimension;
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] primary,
                                       final double[] primaryDot, final double[] secondary,
                                       final double[] secondaryDot)
            throws OrekitExceptionWrapper {

            try {

                // update space dynamics view
                final SpacecraftState currentState = stateMapper.mapArrayToState(t, primary);

                // compute additional derivatives
                final double[] additionalMainDot =
                        equations.computeDerivatives(currentState, secondary, secondaryDot);
                if (additionalMainDot != null) {
                    // the additional equations have an effect on main equations
                    for (int i = 0; i < additionalMainDot.length; ++i) {
                        primaryDot[i] += additionalMainDot[i];
                    }
                }

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }

        }

    }

    /** Adapt an {@link org.orekit.propagation.events.EventDetector}
     * to commons-math {@link org.apache.commons.math3.ode.events.EventHandler} interface.
     * @author Fabien Maussion
     */
    private class AdaptedEventDetector implements EventHandler {

        /** Underlying event detector. */
        private final EventDetector detector;

        /** Build a wrapped event detector.
         * @param detector event detector to wrap
        */
        public AdaptedEventDetector(final EventDetector detector) {
            this.detector = detector;
        }

        /** {@inheritDoc} */
        public void init(final double t0, final double[] y0, final double t) {
            try {
                detector.init(stateMapper.mapArrayToState(t0, y0), stateMapper.mapDoubleToDate(t));
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public double g(final double t, final double[] y) {
            try {
                return detector.g(stateMapper.mapArrayToState(t, y));
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final double t, final double[] y, final boolean increasing) {
            try {

                final SpacecraftState state = stateMapper.mapArrayToState(t, y);
                final EventDetector.Action whatNext = detector.eventOccurred(state, increasing);

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
        public void resetState(final double t, final double[] y) {
            try {
                final SpacecraftState oldState = stateMapper.mapArrayToState(t, y);
                final SpacecraftState newState = detector.resetState(oldState);
                stateMapper.mapStateToArray(newState, y);
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

    }

    /** Adapt an {@link org.orekit.propagation.sampling.OrekitStepHandler}
     * to commons-math {@link StepHandler} interface.
     * @author Luc Maisonobe
     */
    private class AdaptedStepHandler
        implements OrekitStepInterpolator, StepHandler, ModeHandler {

        /** Underlying handler. */
        private final OrekitStepHandler handler;

        /** Map of additional equations indices. */
        private Map<String, Integer> map;

        /** Flag for handler . */
        private boolean activate;

        /** Underlying raw rawInterpolator. */
        private StepInterpolator rawInterpolator;

        /** Build an instance.
         * @param handler underlying handler to wrap
         */
        public AdaptedStepHandler(final OrekitStepHandler handler) {
            this.handler = handler;
        }

        /** {@inheritDoc} */
        public void initialize(final Map<String, Integer> additionalEquationsMap,
                               final boolean activateHandlers) {
            this.map      = additionalEquationsMap;
            this.activate = activateHandlers;
        }

        /** {@inheritDoc} */
        public void init(final double t0, final double[] y0, final double t) {
            try {
                handler.init(stateMapper.mapArrayToState(t0, y0), stateMapper.mapDoubleToDate(t));
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public void handleStep(final StepInterpolator interpolator, final boolean isLast) {
            try {
                this.rawInterpolator = interpolator;
                if (activate) {
                    handler.handleStep(this, isLast);
                }
            } catch (PropagationException pe) {
                throw new OrekitExceptionWrapper(pe);
            }
        }

        /** Get the current grid date.
         * @return current grid date
         */
        public AbsoluteDate getCurrentDate() {
            return stateMapper.mapDoubleToDate(rawInterpolator.getCurrentTime());
        }

        /** Get the previous grid date.
         * @return previous grid date
         */
        public AbsoluteDate getPreviousDate() {
            return stateMapper.mapDoubleToDate(rawInterpolator.getPreviousTime());
        }

        /** Get the interpolated date.
         * <p>If {@link #setInterpolatedDate(AbsoluteDate) setInterpolatedDate}
         * has not been called, the date returned is the same as  {@link
         * #getCurrentDate() getCurrentDate}.</p>
         * @return interpolated date
         * @see #setInterpolatedDate(AbsoluteDate)
         * @see #getInterpolatedState()
         */
        public AbsoluteDate getInterpolatedDate() {
            return stateMapper.mapDoubleToDate(rawInterpolator.getInterpolatedTime());
        }

        /** Set the interpolated date.
         * <p>It is possible to set the interpolation date outside of the current
         * step range, but accuracy will decrease as date is farther.</p>
         * @param date interpolated date to set
         * @see #getInterpolatedDate()
         * @see #getInterpolatedState()
         */
        public void setInterpolatedDate(final AbsoluteDate date) {
            rawInterpolator.setInterpolatedTime(stateMapper.mapDateToDouble(date));
        }

        /** Get the interpolated state.
         * @return interpolated state at the current interpolation date
         * @exception OrekitException if state cannot be interpolated or converted
         * @see #getInterpolatedDate()
         * @see #setInterpolatedDate(AbsoluteDate)
         */
        public SpacecraftState getInterpolatedState() throws OrekitException {
            try {
                return stateMapper.mapArrayToState(rawInterpolator.getInterpolatedTime(),
                                                   rawInterpolator.getInterpolatedState());
            } catch (OrekitExceptionWrapper oew) {
                if (oew.getException() instanceof PropagationException) {
                    throw (PropagationException) oew.getException();
                } else {
                    throw new PropagationException(oew.getException());
                }
            }
        }

        /** {@inheritDoc} */
        public double[] getInterpolatedAdditionalState(final String name)
            throws OrekitException {

            try {

                // get portion of additional state to update
                if (map.containsKey(name)) {
                    return rawInterpolator.getInterpolatedSecondaryState(map.get(name));
                }

                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_EQUATION, name);

            } catch (OrekitExceptionWrapper oew) {
                if (oew.getException() instanceof PropagationException) {
                    throw (PropagationException) oew.getException();
                } else {
                    throw new PropagationException(oew.getException());
                }
            }

        }

        /** Check is integration direction is forward in date.
         * @return true if integration is forward in date
         */
        public boolean isForward() {
            return rawInterpolator.isForward();
        }

    }

    private class EphemerisModeHandler implements ModeHandler, StepHandler {

        /** Map of additional equations indices. */
        private Map<String, Integer> map;

        /** Underlying raw mathematical model. */
        private ContinuousOutputModel model;

        /** Generated ephemeris. */
        private BoundedPropagator ephemeris;

        /** Flag for handler . */
        private boolean activate;

        /** Creates a new instance of EphemerisModeHandler which must be
         *  filled by the propagator.
         */
        public EphemerisModeHandler() {
        }

        /** {@inheritDoc} */
        public void initialize(final Map<String, Integer> additionalEquationsMap,
                               final boolean activateHandlers) {
            this.map      = additionalEquationsMap;
            this.activate = activateHandlers;
            this.model    = new ContinuousOutputModel();

            // ephemeris will be generated when last step is processed
            this.ephemeris = null;

        }

        /** Get the generated ephemeris.
         * @return a new instance of the generated ephemeris
         */
        public BoundedPropagator getEphemeris() {
            return ephemeris;
        }

        /** {@inheritDoc} */
        public void handleStep(final StepInterpolator interpolator, final boolean isLast)
            throws OrekitExceptionWrapper {
            try {
                if (activate) {
                    model.handleStep(interpolator, isLast);
                    if (isLast) {

                        // set up the boundary dates
                        final double tI = model.getInitialTime();
                        final double tF = model.getFinalTime();
                        final AbsoluteDate startDate = stateMapper.mapDoubleToDate(tI);
                        final AbsoluteDate minDate;
                        final AbsoluteDate maxDate;
                        if (tF < tI) {
                            minDate = stateMapper.mapDoubleToDate(tF);
                            maxDate = startDate;
                        } else {
                            minDate = startDate;
                            maxDate = stateMapper.mapDoubleToDate(tF);
                        }

                        // create the ephemeris
                        ephemeris = new IntegratedEphemeris(startDate, minDate, maxDate,
                                                            stateMapper, map, model);

                    }
                }
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }

        /** {@inheritDoc} */
        public void init(final double t0, final double[] y0, final double t) {
            model.init(t0, y0, t);
        }

    }

    /** Container associating an {@link AdditionalEquations} and an {@link AdditionalStateData}. */
    private static class AdditionalEquationsAndData {

        /** Additional equations. */
        private final AdditionalEquations equations;

        /** Additional state and derivatives data. */
        private final AdditionalStateData data;

        /** Index of the additional equations in the expandable set. */
        private int index;

        /** Simple constructor.
         * @param equations additional equations
         */
        public AdditionalEquationsAndData(final AdditionalEquations equations) {
            this.equations = equations;
            data = new AdditionalStateData(equations.getName());
            index = -1;
        }

        /** Get the additional equations.
         * @return additional equations
         */
        public AdditionalEquations getEquations() {
            return equations;
        }

        /** Get the additional state.
         * @return additional state
         */
        public AdditionalStateData getData() {
            return data;
        }

        /** Set the index of the additional equations in the {@link
         * org.apache.commons.math3.ode.ExpandableStatefulODE expandable set}.
         * @param index index of the additional equations in the {@link
         * org.apache.commons.math3.ode.ExpandableStatefulODE expandable set}
         */
        public void setIndex(final int index) {
            this.index = index;
        }

        /** Get the index of the additional equations in the {@link
         * org.apache.commons.math3.ode.ExpandableStatefulODE expandable set}.
         * @return index of the additional equations in the {@link
         * org.apache.commons.math3.ode.ExpandableStatefulODE expandable set}
         */
        public int getIndex() {
            return index;
        }

    }

}
