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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.ode.ContinuousOutputModel;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
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

    /** Absolute vectorial error field name. */
    private static final String ABSOLUTE_TOLERANCE = "vecAbsoluteTolerance";

    /** Relative vectorial error field name. */
    private static final String RELATIVE_TOLERANCE = "vecRelativeTolerance";

    /** Event detectors not related to force models. */
    private final List<EventDetector> detectors;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final FirstOrderIntegrator integrator;

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
    protected AbstractIntegratedPropagator(final FirstOrderIntegrator integrator) {
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

    /** Select additional state and equations pair in the list.
     * @param  name name of the additional equations to select
     * @return additional state and equations pair
     * @throws OrekitException if additional equation is unknown
     */
    private AdditionalEquationsAndData selectStateAndEquations(final String name)
        throws OrekitException {
        for (AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
            if (stateAndEqu.getEquations().getName().equals(name)) {
                return stateAndEqu;
            }
        }
        throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_EQUATION);
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
        selectStateAndEquations(name).getData().setAdditionalState(addState);
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
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
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
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
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
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
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

            // initialize mode handler
            if (modeHandler != null) {
                final List<AdditionalStateData> stateData =
                        new ArrayList<AdditionalStateData>(addEquationsAndData.size());
                for (final AdditionalEquationsAndData equationsAndData : addEquationsAndData) {
                    stateData.add(equationsAndData.getData());
                }
                modeHandler.initialize(stateData, activateHandlers);
            }

            // creating state vector
            this.stateVector  = new double[computeDimension()];

            if (getInitialState().getMass() <= 0.0) {
                throw new PropagationException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE,
                                               getInitialState().getMass());
            }

            // mathematical view
            final double t0 = 0;
            final double t1 = tEnd.durationFrom(getInitialState().getDate());

            // Map state to array
            stateMapper.mapStateToArray(getInitialState(), stateVector);
            int index = 7;
            for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                final double[] addState = stateAndEqu.getData().getAdditionalState();
                System.arraycopy(addState, 0, stateVector, index, addState.length);
                // Incrementing index
                index += addState.length;
            }

            integrator.clearEventHandlers();

            // set up events added by user
            setUpUserEventDetectors();

            // mathematical integration
            if (!addEquationsAndData.isEmpty()) {
                expandToleranceArray();
            }
            final double stopTime;
            try {
                beforeIntegration(getInitialState(), tEnd);
                stopTime = integrator.integrate(new CompleteDifferentialEquations(getMainStateEquations()),
                                                     t0, stateVector, t1, stateVector);
                afterIntegration();
            } catch (OrekitExceptionWrapper oew) {
                throw oew.getException();
            }
            if (!addEquationsAndData.isEmpty()) {
                resetToleranceArray();
            }

            // get final state
            final SpacecraftState finalState = stateMapper.mapArrayToState(stopTime, stateVector);
            resetInitialState(finalState);
            setStartDate(finalState.getDate());

            // get final additional state
            index = 7;
            for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                final double[] addState = stateAndEqu.getData().getAdditionalState();
                System.arraycopy(stateVector, index, addState, 0, addState.length);
                // Incrementing index
                index += addState.length;
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
    /** Compute complete state vector dimension.
     * @return state vector dimension
     */
    private int computeDimension() {
        int sum = getBasicDimension();
        for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
            sum += stateAndEqu.getData().getAdditionalState().length;
        }
        return sum;

    }

    /** Expand integrator tolerance array to fit compound state vector.
     */
    protected void expandToleranceArray() {
        if (integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = computeDimension();
            resizeArray(integrator, ABSOLUTE_TOLERANCE, n, Double.POSITIVE_INFINITY);
            resizeArray(integrator, RELATIVE_TOLERANCE, n, 0.0);
        }
    }

    /** Reset integrator tolerance array to original size.
     */
    protected void resetToleranceArray() {
        if (integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = stateVector.length;
            resizeArray(integrator, ABSOLUTE_TOLERANCE, n, Double.POSITIVE_INFINITY);
            resizeArray(integrator, RELATIVE_TOLERANCE, n, 0.0);
        }
    }

    /** Resize object internal array.
     * @param instance instance concerned
     * @param fieldName field name
     * @param newSize new array size
     * @param filler value to use to fill uninitialized elements of the new array
     */
    private void resizeArray(final Object instance, final String fieldName,
                             final int newSize, final double filler) {
        try {
            final Field arrayField = AdaptiveStepsizeIntegrator.class.getDeclaredField(fieldName);
            arrayField.setAccessible(true);
            final double[] originalArray = (double[]) arrayField.get(instance);
            final int originalSize = originalArray.length;
            final double[] resizedArray = new double[newSize];
            if (newSize > originalSize) {
                // expand array
                System.arraycopy(originalArray, 0, resizedArray, 0, originalSize);
                Arrays.fill(resizedArray, originalSize, newSize, filler);
            } else {
                // shrink array
                System.arraycopy(originalArray, 0, resizedArray, 0, newSize);
            }
            arrayField.set(instance, resizedArray);
        } catch (NoSuchFieldException nsfe) {
            throw OrekitException.createInternalError(nsfe);
        } catch (IllegalAccessException iae) {
            throw OrekitException.createInternalError(iae);
        }
    }

    /** Differential equations for the main state (orbit, attitude and mass). */
    public static interface MainStateEquations {

        /** Compute differential equations for main state.
         * @param state current state
         * @return derivatives of main state
         * @throws OrekitException if differentials cannot be computed
         */
        double[] computeDerivatives(final SpacecraftState state) throws OrekitException;

    }

    /** Differential equations for main state and additional state. */
    private class CompleteDifferentialEquations implements FirstOrderDifferentialEquations {

        /** Main state equations. */
        private final MainStateEquations main;

        /** Simple constructor.
         * @param main main state equations
         */
        public CompleteDifferentialEquations(final MainStateEquations main) {
            this.main = main;
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return computeDimension();
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

                // Add contribution for additional state
                int index = 7;
                for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                    final double[] p    = stateAndEqu.getData().getAdditionalState();
                    final double[] pDot = stateAndEqu.getData().getAdditionalStateDot();

                    // update current additional state
                    System.arraycopy(y, index, p, 0, p.length);

                    // compute additional derivatives
                    final double[] additionalMainDot =
                            stateAndEqu.getEquations().computeDerivatives(currentState, p, pDot);
                    if (additionalMainDot != null) {
                        // the additional equations have an effect on main equations
                        for (int i = 0; i < additionalMainDot.length; ++i) {
                            yDot[i] += additionalMainDot[i];
                        }
                    }

                    // update each additional state contribution in global array
                    System.arraycopy(pDot, 0, yDot, index, p.length);

                    // incrementing index
                    index += p.length;
                }


            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }


            // increment calls counter
            ++calls;

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

        /** Serializable UID. */
        private static final long serialVersionUID = -3674883225252719093L;

        /** Additional state data list. */
        private List <AdditionalStateData> addStateData;

        /** Underlying handler. */
        private final OrekitStepHandler handler;

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
        public void initialize(final List <AdditionalStateData> additionalStateData,
                               final boolean activateHandlers) {
            this.addStateData = additionalStateData;
            this.activate     = activateHandlers;
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

                // propagate the whole state vector
                final double[] y = rawInterpolator.getInterpolatedState();

                // get portion of additional state to update
                int index = 7;
                for (final AdditionalStateData stateData : addStateData) {
                    if (stateData.getName().equals(name)) {
                        final double[] state = stateData.getAdditionalState();
                        System.arraycopy(y, index, state, 0, state.length);
                        return state;
                    }
                    // incrementing index
                    index += stateData.getAdditionalState().length;
                }

                throw new OrekitException(OrekitMessages.UNKNOWN_ADDITIONAL_EQUATION);

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

        /** List of additional state data. */
        private List<AdditionalStateData> stateData;

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
        public void initialize(final List <AdditionalStateData> additionalStateData,
                               final boolean activateHandlers) {
            this.stateData = additionalStateData;
            this.activate  = activateHandlers;
            this.model     = new ContinuousOutputModel();

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
                                                            stateMapper, stateData, model);

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

    /** Internal class for additional equations and state data management. */
    private static class AdditionalEquationsAndData {

        /** Additional equations. */
        private final AdditionalEquations equations;

        /** Additional state and derivatives data. */
        private final AdditionalStateData data;

        /** Simple constructor.
         * @param equations additional equations
         */
        public AdditionalEquationsAndData(final AdditionalEquations equations) {
            this.equations = equations;
            data = new AdditionalStateData(equations.getName());
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

    }

}
