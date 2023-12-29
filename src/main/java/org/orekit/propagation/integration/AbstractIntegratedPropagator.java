/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.solvers.BracketedUnivariateSolver;
import org.hipparchus.analysis.solvers.BracketingNthOrderBrentSolver;
import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.ode.DenseOutputModel;
import org.hipparchus.ode.ExpandableODE;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.ODEState;
import org.hipparchus.ode.ODEStateAndDerivative;
import org.hipparchus.ode.OrdinaryDifferentialEquation;
import org.hipparchus.ode.SecondaryODE;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.events.AdaptableInterval;
import org.hipparchus.ode.events.ODEEventDetector;
import org.hipparchus.ode.events.ODEEventHandler;
import org.hipparchus.ode.sampling.AbstractODEStateInterpolator;
import org.hipparchus.ode.sampling.ODEStateInterpolator;
import org.hipparchus.ode.sampling.ODEStepHandler;
import org.hipparchus.util.Precision;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;


/** Common handling of {@link org.orekit.propagation.Propagator Propagator}
 *  methods for both numerical and semi-analytical propagators.
 *  @author Luc Maisonobe
 */
public abstract class AbstractIntegratedPropagator extends AbstractPropagator {

    /** Internal name used for complete secondary state dimension.
     * @since 11.1
     */
    private static final String SECONDARY_DIMENSION = "Orekit-secondary-dimension";

    /** Event detectors not related to force models. */
    private final List<EventDetector> detectors;

    /** Step handlers dedicated to ephemeris generation. */
    private final List<StoringStepHandler> ephemerisGenerators;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final ODEIntegrator integrator;

    /** Offsets of secondary states managed by {@link AdditionalEquations}.
     * @since 11.1
     */
    private final Map<String, Integer> secondaryOffsets;

    /** Additional derivatives providers.
     * @since 11.1
     */
    private List<AdditionalDerivativesProvider> additionalDerivativesProviders;

    /** Map of secondary equation offset in main
    /** Counter for differential equations calls. */
    private int calls;

    /** Mapper between raw double components and space flight dynamics objects. */
    private StateMapper stateMapper;

    /** Flag for resetting the state at end of propagation. */
    private boolean resetAtEnd;

    /** Type of orbit to output (mean or osculating) <br/>
     * <p>
     * This is used only in the case of semi-analytical propagators where there is a clear separation between
     * mean and short periodic elements. It is ignored by the Numerical propagator.
     * </p>
     */
    private PropagationType propagationType;

    /** Build a new instance.
     * @param integrator numerical integrator to use for propagation.
     * @param propagationType type of orbit to output (mean or osculating).
     */
    protected AbstractIntegratedPropagator(final ODEIntegrator integrator, final PropagationType propagationType) {
        detectors                      = new ArrayList<>();
        ephemerisGenerators            = new ArrayList<>();
        additionalDerivativesProviders = new ArrayList<>();
        this.secondaryOffsets          = new HashMap<>();
        this.integrator                = integrator;
        this.propagationType           = propagationType;
        this.resetAtEnd                = true;
    }

    /** Allow/disallow resetting the initial state at end of propagation.
     * <p>
     * By default, at the end of the propagation, the propagator resets the initial state
     * to the final state, thus allowing a new propagation to be started from there without
     * recomputing the part already performed. Calling this method with {@code resetAtEnd} set
     * to false changes prevents such reset.
     * </p>
     * @param resetAtEnd if true, at end of each propagation, the {@link
     * #getInitialState() initial state} will be reset to the final state of
     * the propagation, otherwise the initial state will be preserved
     * @since 9.0
     */
    public void setResetAtEnd(final boolean resetAtEnd) {
        this.resetAtEnd = resetAtEnd;
    }

    /** Getter for the resetting flag regarding initial state.
     * @return resetting flag
     * @since 12.0
     */
    public boolean getResetAtEnd() {
        return this.resetAtEnd;
    }

    /** Initialize the mapper. */
    protected void initMapper() {
        stateMapper = createMapper(null, Double.NaN, null, null, null, null);
    }

    /** Get the integrator's name.
     * @return name of underlying integrator
     * @since 12.0
     */
    public String getIntegratorName() {
        return integrator.getName();
    }

    /**  {@inheritDoc} */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        super.setAttitudeProvider(attitudeProvider);
        stateMapper = createMapper(stateMapper.getReferenceDate(), stateMapper.getMu(),
                                   stateMapper.getOrbitType(), stateMapper.getPositionAngleType(),
                                   attitudeProvider, stateMapper.getFrame());
    }

    /** Set propagation orbit type.
     * @param orbitType orbit type to use for propagation, null for
     * propagating using {@link org.orekit.utils.AbsolutePVCoordinates AbsolutePVCoordinates}
     * rather than {@link org.orekit.orbits Orbit}
     */
    protected void setOrbitType(final OrbitType orbitType) {
        stateMapper = createMapper(stateMapper.getReferenceDate(), stateMapper.getMu(),
                                   orbitType, stateMapper.getPositionAngleType(),
                                   stateMapper.getAttitudeProvider(), stateMapper.getFrame());
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation, null for
     * propagating using {@link org.orekit.utils.AbsolutePVCoordinates AbsolutePVCoordinates}
     * rather than {@link org.orekit.orbits Orbit}
     */
    protected OrbitType getOrbitType() {
        return stateMapper.getOrbitType();
    }

    /** Get the propagation type.
     * @return propagation type.
     * @since 11.1
     */
    public PropagationType getPropagationType() {
        return propagationType;
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
    protected void setPositionAngleType(final PositionAngleType positionAngleType) {
        stateMapper = createMapper(stateMapper.getReferenceDate(), stateMapper.getMu(),
                                   stateMapper.getOrbitType(), positionAngleType,
                                   stateMapper.getAttitudeProvider(), stateMapper.getFrame());
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    protected PositionAngleType getPositionAngleType() {
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
        for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
            if (provider.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getManagedAdditionalStates() {
        final String[] alreadyIntegrated = super.getManagedAdditionalStates();
        final String[] managed = new String[alreadyIntegrated.length + additionalDerivativesProviders.size()];
        System.arraycopy(alreadyIntegrated, 0, managed, 0, alreadyIntegrated.length);
        for (int i = 0; i < additionalDerivativesProviders.size(); ++i) {
            managed[i + alreadyIntegrated.length] = additionalDerivativesProviders.get(i).getName();
        }
        return managed;
    }

    /** Add a provider for user-specified state derivatives to be integrated along with the orbit propagation.
     * @param provider provider for additional derivatives
     * @see #addAdditionalStateProvider(org.orekit.propagation.AdditionalStateProvider)
     * @since 11.1
     */
    public void addAdditionalDerivativesProvider(final AdditionalDerivativesProvider provider) {

        // check if the name is already used
        if (isAdditionalStateManaged(provider.getName())) {
            // these derivatives are already registered, complain
            throw new OrekitException(OrekitMessages.ADDITIONAL_STATE_NAME_ALREADY_IN_USE,
                                      provider.getName());
        }

        // this is really a new set of derivatives, add it
        additionalDerivativesProviders.add(provider);

        secondaryOffsets.clear();

    }

    /** Get an unmodifiable list of providers for additional derivatives.
     * @return providers for the additional derivatives
     * @since 11.1
     */
    public List<AdditionalDerivativesProvider> getAdditionalDerivativesProviders() {
        return Collections.unmodifiableList(additionalDerivativesProviders);
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
        integ.addEventDetector(new AdaptedEventDetector(detector));
    }

    /** {@inheritDoc} */
    @Override
    public EphemerisGenerator getEphemerisGenerator() {
        final StoringStepHandler storingHandler = new StoringStepHandler();
        ephemerisGenerators.add(storingHandler);
        return storingHandler;
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
                                                OrbitType orbitType, PositionAngleType positionAngleType,
                                                AttitudeProvider attitudeProvider, Frame frame);

    /** Get the differential equations to integrate (for main state only).
     * @param integ numerical integrator to use for propagation.
     * @return differential equations for main state
     */
    protected abstract MainStateEquations getMainStateEquations(ODEIntegrator integ);

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate target) {
        if (getStartDate() == null) {
            if (getInitialState() == null) {
                throw new OrekitException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }
            setStartDate(getInitialState().getDate());
        }
        return propagate(getStartDate(), target);
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate tStart, final AbsoluteDate tEnd) {

        if (getInitialState() == null) {
            throw new OrekitException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
        }

        // make sure the integrator will be reset properly even if we change its events handlers and step handlers
        try (IntegratorResetter resetter = new IntegratorResetter(integrator)) {

            // prepare handling of STM and Jacobian matrices
            setUpStmAndJacobianGenerators();

            // Initialize additional states
            initializeAdditionalStates(tEnd);

            if (!tStart.equals(getInitialState().getDate())) {
                // if propagation start date is not initial date,
                // propagate from initial to start date without event detection
                try (IntegratorResetter startResetter = new IntegratorResetter(integrator)) {
                    integrateDynamics(tStart, true);
                }
            }

            // set up events added by user
            setUpUserEventDetectors();

            // set up step handlers
            for (final OrekitStepHandler handler : getMultiplexer().getHandlers()) {
                integrator.addStepHandler(new AdaptedStepHandler(handler));
            }
            for (final StoringStepHandler generator : ephemerisGenerators) {
                generator.setEndDate(tEnd);
                integrator.addStepHandler(generator);
            }

            // propagate from start date to end date with event detection
            final SpacecraftState finalState = integrateDynamics(tEnd, false);

            return finalState;

        }

    }

    /** Set up State Transition Matrix and Jacobian matrix handling.
     * @since 11.1
     */
    protected void setUpStmAndJacobianGenerators() {
        // nothing to do by default
    }

    /** Propagation with or without event detection.
     * @param tEnd target date to which orbit should be propagated
     * @param forceResetAtEnd flag to force resetting state and date after integration
     * @return state at end of propagation
     */
    private SpacecraftState integrateDynamics(final AbsoluteDate tEnd, final boolean forceResetAtEnd) {
        try {

            initializePropagation();

            if (getInitialState().getDate().equals(tEnd)) {
                // don't extrapolate
                return getInitialState();
            }

            // space dynamics view
            stateMapper = createMapper(getInitialState().getDate(), stateMapper.getMu(),
                                       stateMapper.getOrbitType(), stateMapper.getPositionAngleType(),
                                       stateMapper.getAttitudeProvider(), getInitialState().getFrame());


            if (Double.isNaN(getMu())) {
                setMu(getInitialState().getMu());
            }

            if (getInitialState().getMass() <= 0.0) {
                throw new OrekitException(OrekitMessages.NOT_POSITIVE_SPACECRAFT_MASS,
                                          getInitialState().getMass());
            }

            // convert space flight dynamics API to math API
            final SpacecraftState initialIntegrationState = getInitialIntegrationState();
            final ODEState mathInitialState = createInitialState(initialIntegrationState);
            final ExpandableODE mathODE = createODE(integrator);

            // mathematical integration
            final ODEStateAndDerivative mathFinalState;
            beforeIntegration(initialIntegrationState, tEnd);
            mathFinalState = integrator.integrate(mathODE, mathInitialState,
                                                  tEnd.durationFrom(getInitialState().getDate()));
            afterIntegration();

            // get final state
            SpacecraftState finalState =
                            stateMapper.mapArrayToState(stateMapper.mapDoubleToDate(mathFinalState.getTime(),
                                                                                    tEnd),
                                                        mathFinalState.getPrimaryState(),
                                                        mathFinalState.getPrimaryDerivative(),
                                                        propagationType);

            if (!additionalDerivativesProviders.isEmpty()) {
                final double[] secondary            = mathFinalState.getSecondaryState(1);
                final double[] secondaryDerivatives = mathFinalState.getSecondaryDerivative(1);
                for (AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
                    final String   name        = provider.getName();
                    final int      offset      = secondaryOffsets.get(name);
                    final int      dimension   = provider.getDimension();
                    finalState = finalState.
                                 addAdditionalState(name, Arrays.copyOfRange(secondary, offset, offset + dimension)).
                                 addAdditionalStateDerivative(name, Arrays.copyOfRange(secondaryDerivatives, offset, offset + dimension));
                }
            }
            finalState = updateAdditionalStates(finalState);

            if (resetAtEnd || forceResetAtEnd) {
                resetInitialState(finalState);
                setStartDate(finalState.getDate());
            }

            return finalState;

        } catch (MathRuntimeException mre) {
            throw OrekitException.unwrap(mre);
        }
    }

    /** Get the initial state for integration.
     * @return initial state for integration
     */
    protected SpacecraftState getInitialIntegrationState() {
        return getInitialState();
    }

    /** Create an initial state.
     * @param initialState initial state in flight dynamics world
     * @return initial state in mathematics world
     */
    private ODEState createInitialState(final SpacecraftState initialState) {

        // retrieve initial state
        final double[] primary = new double[getBasicDimension()];
        stateMapper.mapStateToArray(initialState, primary, null);

        if (secondaryOffsets.isEmpty()) {
            // compute dimension of the secondary state
            int offset = 0;
            for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
                secondaryOffsets.put(provider.getName(), offset);
                offset += provider.getDimension();
            }
            secondaryOffsets.put(SECONDARY_DIMENSION, offset);
        }

        return new ODEState(0.0, primary, secondary(initialState));

    }

    /** Create secondary state.
     * @param state spacecraft state
     * @return secondary state
     * @since 11.1
     */
    private double[][] secondary(final SpacecraftState state) {

        if (secondaryOffsets.isEmpty()) {
            return null;
        }

        final double[][] secondary = new double[1][secondaryOffsets.get(SECONDARY_DIMENSION)];
        for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
            final String   name       = provider.getName();
            final int      offset     = secondaryOffsets.get(name);
            final double[] additional = state.getAdditionalState(name);
            System.arraycopy(additional, 0, secondary[0], offset, additional.length);
        }

        return secondary;

    }

    /** Create secondary state derivative.
     * @param state spacecraft state
     * @return secondary state derivative
     * @since 11.1
     */
    private double[][] secondaryDerivative(final SpacecraftState state) {

        if (secondaryOffsets.isEmpty()) {
            return null;
        }

        final double[][] secondaryDerivative = new double[1][secondaryOffsets.get(SECONDARY_DIMENSION)];
        for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
            final String   name       = provider.getName();
            final int      offset     = secondaryOffsets.get(name);
            final double[] additionalDerivative = state.getAdditionalStateDerivative(name);
            System.arraycopy(additionalDerivative, 0, secondaryDerivative[0], offset, additionalDerivative.length);
        }

        return secondaryDerivative;

    }

    /** Create an ODE with all equations.
     * @param integ numerical integrator to use for propagation.
     * @return a new ode
     */
    private ExpandableODE createODE(final ODEIntegrator integ) {

        final ExpandableODE ode =
                new ExpandableODE(new ConvertedMainStateEquations(getMainStateEquations(integ)));

        // secondary part of the ODE
        if (!additionalDerivativesProviders.isEmpty()) {
            ode.addSecondaryEquations(new ConvertedSecondaryStateEquations());
        }

        return ode;

    }

    /** Method called just before integration.
     * <p>
     * The default implementation does nothing, it may be specialized in subclasses.
     * </p>
     * @param initialState initial state
     * @param tEnd target date at which state should be propagated
     */
    protected void beforeIntegration(final SpacecraftState initialState,
                                     final AbsoluteDate tEnd) {
        // do nothing by default
    }

    /** Method called just after integration.
     * <p>
     * The default implementation does nothing, it may be specialized in subclasses.
     * </p>
     */
    protected void afterIntegration() {
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

    /** Convert a state from mathematical world to space flight dynamics world.
     * @param os mathematical state
     * @return space flight dynamics state
     */
    private SpacecraftState convert(final ODEStateAndDerivative os) {

        SpacecraftState s = stateMapper.mapArrayToState(os.getTime(),
                                                        os.getPrimaryState(),
                                                        os.getPrimaryDerivative(),
                                                        propagationType);
        if (os.getNumberOfSecondaryStates() > 0) {
            final double[] secondary           = os.getSecondaryState(1);
            final double[] secondaryDerivative = os.getSecondaryDerivative(1);
            for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
                final String name      = provider.getName();
                final int    offset    = secondaryOffsets.get(name);
                final int    dimension = provider.getDimension();
                s = s.addAdditionalState(name, Arrays.copyOfRange(secondary, offset, offset + dimension));
                s = s.addAdditionalStateDerivative(name, Arrays.copyOfRange(secondaryDerivative, offset, offset + dimension));
            }
        }
        s = updateAdditionalStates(s);

        return s;

    }

    /** Convert a state from space flight dynamics world to mathematical world.
     * @param state space flight dynamics state
     * @return mathematical state
     */
    private ODEStateAndDerivative convert(final SpacecraftState state) {

        // retrieve initial state
        final double[] primary    = new double[getBasicDimension()];
        final double[] primaryDot = new double[getBasicDimension()];
        stateMapper.mapStateToArray(state, primary, primaryDot);

        // secondary part of the ODE
        final double[][] secondary           = secondary(state);
        final double[][] secondaryDerivative = secondaryDerivative(state);

        return new ODEStateAndDerivative(stateMapper.mapDateToDouble(state.getDate()),
                                         primary, primaryDot,
                                         secondary, secondaryDerivative);

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
         */
        default void init(final SpacecraftState initialState, final AbsoluteDate target) {
        }

        /** Compute differential equations for main state.
         * @param state current state
         * @return derivatives of main state
         */
        double[] computeDerivatives(SpacecraftState state);

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
            // update space dynamics view
            SpacecraftState initialState = stateMapper.mapArrayToState(t0, y0, null, PropagationType.MEAN);
            initialState = updateAdditionalStates(initialState);
            final AbsoluteDate target = stateMapper.mapDoubleToDate(finalTime);
            main.init(initialState, target);
        }

        /** {@inheritDoc} */
        public double[] computeDerivatives(final double t, final double[] y) {

            // increment calls counter
            ++calls;

            // update space dynamics view
            SpacecraftState currentState = stateMapper.mapArrayToState(t, y, null, PropagationType.MEAN);

            currentState = updateAdditionalStates(currentState);
            // compute main state differentials
            return main.computeDerivatives(currentState);

        }

    }

    /** Differential equations for the secondary state (Jacobians, user variables ...), with converted API. */
    private class ConvertedSecondaryStateEquations implements SecondaryODE {

        /** Dimension of the combined additional states. */
        private final int combinedDimension;

        /** Simple constructor.
          */
        ConvertedSecondaryStateEquations() {
            this.combinedDimension = secondaryOffsets.get(SECONDARY_DIMENSION);
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return combinedDimension;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final double t0, final double[] primary0,
                         final double[] secondary0, final double finalTime) {
            // update space dynamics view
            final SpacecraftState initialState = convert(t0, primary0, null, secondary0);

            final AbsoluteDate target = stateMapper.mapDoubleToDate(finalTime);
            for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
                provider.init(initialState, target);
            }

        }

        /** {@inheritDoc} */
        @Override
        public double[] computeDerivatives(final double t, final double[] primary,
                                           final double[] primaryDot, final double[] secondary) {

            // update space dynamics view
            // the integrable generators generate method will be called here,
            // according to the generators yield order
            SpacecraftState updated = convert(t, primary, primaryDot, secondary);

            // set up queue for equations
            final Queue<AdditionalDerivativesProvider> pending = new LinkedList<>(additionalDerivativesProviders);

            // gather the derivatives from all additional equations, taking care of dependencies
            final double[] secondaryDot = new double[combinedDimension];
            int yieldCount = 0;
            while (!pending.isEmpty()) {
                final AdditionalDerivativesProvider provider = pending.remove();
                if (provider.yields(updated)) {
                    // this provider has to wait for another one,
                    // we put it again in the pending queue
                    pending.add(provider);
                    if (++yieldCount >= pending.size()) {
                        // all pending providers yielded!, they probably need data not yet initialized
                        // we let the propagation proceed, if these data are really needed right now
                        // an appropriate exception will be triggered when caller tries to access them
                        break;
                    }
                } else {
                    // we can use these equations right now
                    final String              name           = provider.getName();
                    final int                 offset         = secondaryOffsets.get(name);
                    final int                 dimension      = provider.getDimension();
                    final CombinedDerivatives derivatives    = provider.combinedDerivatives(updated);
                    final double[]            additionalPart = derivatives.getAdditionalDerivatives();
                    final double[]            mainPart       = derivatives.getMainStateDerivativesIncrements();
                    System.arraycopy(additionalPart, 0, secondaryDot, offset, dimension);
                    updated = updated.addAdditionalStateDerivative(name, additionalPart);
                    if (mainPart != null) {
                        // this equation does change the main state derivatives
                        for (int i = 0; i < mainPart.length; ++i) {
                            primaryDot[i] += mainPart[i];
                        }
                    }
                    yieldCount = 0;
                }
            }

            return secondaryDot;

        }

        /** Convert mathematical view to space view.
         * @param t current value of the independent <I>time</I> variable
         * @param primary array containing the current value of the primary state vector
         * @param primaryDot array containing the derivative of the primary state vector
         * @param secondary array containing the current value of the secondary state vector
         * @return space view of the state
         */
        private SpacecraftState convert(final double t, final double[] primary,
                                        final double[] primaryDot, final double[] secondary) {

            SpacecraftState initialState = stateMapper.mapArrayToState(t, primary, primaryDot, PropagationType.MEAN);

            for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
                final String name      = provider.getName();
                final int    offset    = secondaryOffsets.get(name);
                final int    dimension = provider.getDimension();
                initialState = initialState.addAdditionalState(name, Arrays.copyOfRange(secondary, offset, offset + dimension));
            }

            return updateAdditionalStates(initialState);

        }

    }

    /** Adapt an {@link org.orekit.propagation.events.EventDetector}
     * to Hipparchus {@link org.hipparchus.ode.events.ODEEventDetector} interface.
     * @author Fabien Maussion
     */
    private class AdaptedEventDetector implements ODEEventDetector {

        /** Underlying event detector. */
        private final EventDetector detector;

        /** Underlying event handler.
         * @since 12.0
         */
        private final EventHandler handler;

        /** Time of the previous call to g. */
        private double lastT;

        /** Value from the previous call to g. */
        private double lastG;

        /** Build a wrapped event detector.
         * @param detector event detector to wrap
        */
        AdaptedEventDetector(final EventDetector detector) {
            this.detector = detector;
            this.handler  = detector.getHandler();
            this.lastT    = Double.NaN;
            this.lastG    = Double.NaN;
        }

        /** {@inheritDoc} */
        @Override
        public AdaptableInterval getMaxCheckInterval() {
            return s -> detector.getMaxCheckInterval().currentInterval(convert(s));
        }

        /** {@inheritDoc} */
        @Override
        public int getMaxIterationCount() {
            return detector.getMaxIterationCount();
        }

        /** {@inheritDoc} */
        @Override
        public BracketedUnivariateSolver<UnivariateFunction> getSolver() {
            return new BracketingNthOrderBrentSolver(0, detector.getThreshold(), 0, 5);
        }

        /** {@inheritDoc} */
        public void init(final ODEStateAndDerivative s0, final double t) {
            detector.init(convert(s0), stateMapper.mapDoubleToDate(t));
            this.lastT = Double.NaN;
            this.lastG = Double.NaN;
        }

        /** {@inheritDoc} */
        public double g(final ODEStateAndDerivative s) {
            if (!Precision.equals(lastT, s.getTime(), 0)) {
                lastT = s.getTime();
                lastG = detector.g(convert(s));
            }
            return lastG;
        }

        /** {@inheritDoc} */
        public ODEEventHandler getHandler() {

            return new ODEEventHandler() {

                /** {@inheritDoc} */
                public Action eventOccurred(final ODEStateAndDerivative s, final ODEEventDetector d, final boolean increasing) {
                    return handler.eventOccurred(convert(s), detector, increasing);
                }

                /** {@inheritDoc} */
                public ODEState resetState(final ODEEventDetector d, final ODEStateAndDerivative s) {

                    final SpacecraftState oldState = convert(s);
                    final SpacecraftState newState = handler.resetState(detector, oldState);
                    stateChanged(newState);

                    // main part
                    final double[] primary    = new double[s.getPrimaryStateDimension()];
                    stateMapper.mapStateToArray(newState, primary, null);

                    // secondary part
                    final double[][] secondary = new double[1][secondaryOffsets.get(SECONDARY_DIMENSION)];
                    for (final AdditionalDerivativesProvider provider : additionalDerivativesProviders) {
                        final String name      = provider.getName();
                        final int    offset    = secondaryOffsets.get(name);
                        final int    dimension = provider.getDimension();
                        System.arraycopy(newState.getAdditionalState(name), 0, secondary[0], offset, dimension);
                    }

                    return new ODEState(newState.getDate().durationFrom(getStartDate()),
                                        primary, secondary);

                }

            };
        }

    }

    /** Adapt an {@link org.orekit.propagation.sampling.OrekitStepHandler}
     * to Hipparchus {@link ODEStepHandler} interface.
     * @author Luc Maisonobe
     */
    private class AdaptedStepHandler implements ODEStepHandler {

        /** Underlying handler. */
        private final OrekitStepHandler handler;

        /** Build an instance.
         * @param handler underlying handler to wrap
         */
        AdaptedStepHandler(final OrekitStepHandler handler) {
            this.handler = handler;
        }

        /** {@inheritDoc} */
        public void init(final ODEStateAndDerivative s0, final double t) {
            handler.init(convert(s0), stateMapper.mapDoubleToDate(t));
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final ODEStateInterpolator interpolator) {
            handler.handleStep(new AdaptedStepInterpolator(interpolator));
        }

        /** {@inheritDoc} */
        @Override
        public void finish(final ODEStateAndDerivative finalState) {
            handler.finish(convert(finalState));
        }

    }

    /** Adapt an Hipparchus {@link ODEStateInterpolator}
     * to an orekit {@link OrekitStepInterpolator} interface.
     * @author Luc Maisonobe
     */
    private class AdaptedStepInterpolator implements OrekitStepInterpolator {

        /** Underlying raw rawInterpolator. */
        private final ODEStateInterpolator mathInterpolator;

        /** Simple constructor.
         * @param mathInterpolator underlying raw interpolator
         */
        AdaptedStepInterpolator(final ODEStateInterpolator mathInterpolator) {
            this.mathInterpolator = mathInterpolator;
        }

        /** {@inheritDoc}} */
        @Override
        public SpacecraftState getPreviousState() {
            return convert(mathInterpolator.getPreviousState());
        }

        /** {@inheritDoc}} */
        @Override
        public boolean isPreviousStateInterpolated() {
            return mathInterpolator.isPreviousStateInterpolated();
        }

        /** {@inheritDoc}} */
        @Override
        public SpacecraftState getCurrentState() {
            return convert(mathInterpolator.getCurrentState());
        }

        /** {@inheritDoc}} */
        @Override
        public boolean isCurrentStateInterpolated() {
            return mathInterpolator.isCurrentStateInterpolated();
        }

        /** {@inheritDoc}} */
        @Override
        public SpacecraftState getInterpolatedState(final AbsoluteDate date) {
            return convert(mathInterpolator.getInterpolatedState(date.durationFrom(stateMapper.getReferenceDate())));
        }

        /** {@inheritDoc}} */
        @Override
        public boolean isForward() {
            return mathInterpolator.isForward();
        }

        /** {@inheritDoc}} */
        @Override
        public AdaptedStepInterpolator restrictStep(final SpacecraftState newPreviousState,
                                                    final SpacecraftState newCurrentState) {
            try {
                final AbstractODEStateInterpolator aosi = (AbstractODEStateInterpolator) mathInterpolator;
                return new AdaptedStepInterpolator(aosi.restrictStep(convert(newPreviousState),
                                                                     convert(newCurrentState)));
            } catch (ClassCastException cce) {
                // this should never happen
                throw new OrekitInternalError(cce);
            }
        }

    }

    /** Specialized step handler storing interpolators for ephemeris generation.
     * @since 11.0
     */
    private class StoringStepHandler implements ODEStepHandler, EphemerisGenerator {

        /** Underlying raw mathematical model. */
        private DenseOutputModel model;

        /** the user supplied end date. Propagation may not end on this date. */
        private AbsoluteDate endDate;

        /** Generated ephemeris. */
        private BoundedPropagator ephemeris;

        /** Last interpolator handled by the object.*/
        private  ODEStateInterpolator lastInterpolator;

        /** Set the end date.
         * @param endDate end date
         */
        public void setEndDate(final AbsoluteDate endDate) {
            this.endDate = endDate;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final ODEStateAndDerivative s0, final double t) {

            this.model = new DenseOutputModel();
            model.init(s0, t);

            // ephemeris will be generated when last step is processed
            this.ephemeris = null;

            this.lastInterpolator = null;

        }

        /** {@inheritDoc} */
        @Override
        public BoundedPropagator getGeneratedEphemeris() {
            // Each time we try to get the ephemeris, rebuild it using the last data.
            buildEphemeris();
            return ephemeris;
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final ODEStateInterpolator interpolator) {
            model.handleStep(interpolator);
            lastInterpolator = interpolator;
        }

        /** {@inheritDoc} */
        @Override
        public void finish(final ODEStateAndDerivative finalState) {
            buildEphemeris();
        }

        /** Method used to produce ephemeris at a given time.
         * Can be used at multiple times, updating the ephemeris to
         * its last state.
         */
        private void buildEphemeris() {
            // buildEphemeris was built in order to allow access to what was previously the finish method.
            // This now allows to call it through getGeneratedEphemeris, therefore through an external call,
            // which was not previously the case.

            // Update the model's finalTime with the last interpolator.
            model.finish(lastInterpolator.getCurrentState());

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
            final DoubleArrayDictionary unmanaged = new DoubleArrayDictionary();
            for (final DoubleArrayDictionary.Entry initial : getInitialState().getAdditionalStatesValues().getData()) {
                if (!isAdditionalStateManaged(initial.getKey())) {
                    // this additional state was in the initial state, but is unknown to the propagator
                    // we simply copy its initial value as is
                    unmanaged.put(initial.getKey(), initial.getValue());
                }
            }

            // get the names of additional states managed by differential equations
            final String[] names      = new String[additionalDerivativesProviders.size()];
            final int[]    dimensions = new int[additionalDerivativesProviders.size()];
            for (int i = 0; i < names.length; ++i) {
                names[i] = additionalDerivativesProviders.get(i).getName();
                dimensions[i] = additionalDerivativesProviders.get(i).getDimension();
            }

            // create the ephemeris
            ephemeris = new IntegratedEphemeris(startDate, minDate, maxDate,
                                                stateMapper, propagationType, model,
                                                unmanaged, getAdditionalStateProviders(),
                                                names, dimensions);

        }

    }

    /** Wrapper for resetting an integrator handlers.
     * <p>
     * This class is intended to be used in a try-with-resource statement.
     * If propagator-specific event handlers and step handlers are added to
     * the integrator in the try block, they will be removed automatically
     * when leaving the block, so the integrator only keeps its own handlers
     * between calls to {@link AbstractIntegratedPropagator#propagate(AbsoluteDate, AbsoluteDate).
     * </p>
     * @since 11.0
     */
    private static class IntegratorResetter implements AutoCloseable {

        /** Wrapped integrator. */
        private final ODEIntegrator integrator;

        /** Initial event detectors list. */
        private final List<ODEEventDetector> detectors;

        /** Initial step handlers list. */
        private final List<ODEStepHandler> stepHandlers;

        /** Simple constructor.
         * @param integrator wrapped integrator
         */
        IntegratorResetter(final ODEIntegrator integrator) {
            this.integrator   = integrator;
            this.detectors    = new ArrayList<>(integrator.getEventDetectors());
            this.stepHandlers = new ArrayList<>(integrator.getStepHandlers());
        }

        /** {@inheritDoc}
         * <p>
         * Reset event handlers and step handlers back to the initial list
         * </p>
         */
        @Override
        public void close() {

            // reset event handlers
            integrator.clearEventDetectors();
            detectors.forEach(integrator::addEventDetector);

            // reset step handlers
            integrator.clearStepHandlers();
            stepHandlers.forEach(integrator::addStepHandler);

        }

    }

}
