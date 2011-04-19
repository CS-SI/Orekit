/* Copyright 2002-2011 CS Communication & Systèmes
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.DerivativeException;
import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.IntegratorException;
import org.apache.commons.math.ode.events.EventHandler;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AdaptedEventDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventObserver;
import org.orekit.propagation.events.OccurredEvent;
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
 * The six first elements are either the {@link EquinoctialOrbit equinoctial orbit parameters}
 * (a, e<sub>x</sub>, e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, l<sub>v</sub>) in meters
 * and radians, the {@link KeplerianOrbit Keplerian orbit parameters} (a, e, i, &omega;, &Omega;, v)
 * in meters and radians or {@link CartesianOrbit cartesian orbit parameters} in meters and meters per
 * second depending on the propagator configuration, and the last element is the mass in
 * kilograms. The following code snippet shows a typical setting for Low Earth Orbit
 * propagation in equinoctial parameters:</p>
 * <pre>
 * final double minStep  = 0.001;
 * final double maxStep  = 500;
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
 * @see org.orekit.propagation.precomputed.IntegratedEphemeris
 * @see TimeDerivativesEquations
 *
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class NumericalPropagator implements Propagator, EventObserver {

    /** Parameters types that can be used for propagation. */
    public enum PropagationParametersType {

        /** Type for propagation in {@link CartesianOrbit Cartesian parameters}. */
        CARTESIAN,

        /** Type for propagation in {@link KeplerianOrbit Keplerian parameters}. */
        KEPLERIAN,

        /** Type for propagation in {@link EquinoctialOrbit equinoctial parameters}. */
        EQUINOCTIAL

    }

    /** Serializable UID. */
    private static final long serialVersionUID = -2385169798425713766L;

    /** Absolute vectorial error field name. */
    private static final String ABSOLUTE_TOLERANCE = "vecAbsoluteTolerance";

    /** Relative vectorial error field name. */
    private static final String RELATIVE_TOLERANCE = "vecRelativeTolerance";

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Central body attraction. */
    private NewtonianAttraction newtonianAttraction;

    /** Force models used during the extrapolation of the Orbit, without jacobians. */
    private final List<ForceModel> forceModels;

    /** Event detectors not related to force models. */
    private final List<EventDetector> detectors;

    /** List for occurred events. */
    private final List <OccurredEvent> occurredEvents;

    /** State vector. */
    private double[] stateVector;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Initial state to propagate. */
    private SpacecraftState initialState;

    /** Current state to propagate. */
    private SpacecraftState currentState;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private transient FirstOrderIntegrator integrator;

    /** Counter for differential equations calls. */
    private int calls;

    /** Mapper between spacecraft state and simple array. */
    private StateMapper mapper;

    /** Propagator mode handler. */
    private transient ModeHandler modeHandler;

    /** Current mode. */
    private int mode;

    /** Propagation parameters type. */
    private PropagationParametersType parametersType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Additional equations and associated data. */
    private List<AdditionalEquationsAndData> addEquationsAndData;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
     * evolution only. The defaults are {@link PropagationParametersType#EQUINOCTIAL}
     * for {@link #setPropagationParametersType(PropagationParametersType) propagation
     * parameters type} and {@link PositionAngle#TRUE} for {@link
     * #setPositionAngleType(PositionAngle) position angle type}.
     * @param integrator numerical integrator to use for propagation.
     */
    public NumericalPropagator(final FirstOrderIntegrator integrator) {
        forceModels         = new ArrayList<ForceModel>();
        detectors           = new ArrayList<EventDetector>();
        occurredEvents      = new ArrayList<OccurredEvent>();
        startDate           = null;
        referenceDate       = null;
        currentState        = null;
        addEquationsAndData = new ArrayList<AdditionalEquationsAndData>();
        attitudeProvider    = InertialProvider.EME2000_ALIGNED;
        stateVector         = new double[7];
        setMu(Double.NaN);
        setIntegrator(integrator);
        setSlaveMode();
        setPropagationParametersType(PropagationParametersType.EQUINOCTIAL);
        setPositionAngleType(PositionAngle.TRUE);
    }

    /** Set the integrator.
     * @param integrator numerical integrator to use for propagation.
     */
    public void setIntegrator(final FirstOrderIntegrator integrator) {
        this.integrator = integrator;
    }

    /** Set the central attraction coefficient &mu;.
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @see #getMu()
     * @see #addForceModel(ForceModel)
     */
    public void setMu(final double mu) {
        newtonianAttraction = new NewtonianAttraction(mu);
    }

    /** Get the central attraction coefficient &mu;.
     * @return mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @see #setMu(double)
     */
    public double getMu() {
        return newtonianAttraction.getMu();
    }

    /** Set the attitude provider.
     * @param provider attitude provider
     * @deprecated as of 5.1 replaced by {@link #setAttitudeProvider(AttitudeProvider)}
     */
    @Deprecated
    public void setAttitudeLaw(final AttitudeProvider provider) {
        this.attitudeProvider = provider;
    }

    /** Set the attitude provider.
     * @param attitudeProvider attitude provider
     */
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.attitudeProvider = attitudeProvider;
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

    /** Get perturbing force models list.
     * @return list of perturbing force models
     * @see #addForceModel(ForceModel)
     * @see #getNewtonianAttractionForceModel()
     */
    public List<ForceModel> getForceModels() {
        return forceModels;
    }

    /** Get the Newtonian attraction from the central body force model.
     * @return Newtonian attraction force model
     * @see #setMu(double)
     * @see #getForceModels()
     */
    public NewtonianAttraction getNewtonianAttractionForceModel() {
        return newtonianAttraction;
    }

    /** {@inheritDoc} */
    public int getMode() {
        return mode;
    }

    /** {@inheritDoc} */
    public Frame getFrame() {
        return initialState.getFrame();
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
     * step handler is needed, it should be added after this method has been callled.</p>
     */
    public void setSlaveMode() {
        integrator.clearStepHandlers();
        modeHandler = null;
        mode = SLAVE_MODE;
    }

    /** {@inheritDoc}
     * <p>Note that this method has the side effect of replacing the step handlers
     * of the underlying integrator set up in the {@link
     * #NumericalPropagator(FirstOrderIntegrator) constructor} or the {@link
     * #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific
     * step handler is needed, it should be added after this method has been called.</p>
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
        integrator.clearStepHandlers();
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
        integrator.clearStepHandlers();
        final EphemerisModeHandler ephemeris = new EphemerisModeHandler();
        modeHandler = ephemeris;
        integrator.addStepHandler(ephemeris);
        mode = EPHEMERIS_GENERATION_MODE;
    }

    /** Set propagation parameter type.
     * @param propagationType parameters type to use for propagation
     */
    public void setPropagationParametersType(final PropagationParametersType propagationType) {
        this.parametersType = propagationType;
    }

    /** Get propagation parameter type.
     * @return parameters type used for propagation
     */
    public PropagationParametersType getPropagationParametersType() {
        return parametersType;
    }

    /** Set position angle type.
     * <p>
     * The position parameter type is meaningful only if {@link
     * #getPropagationParametersType() propagation parameters}
     * support it. As an example, it is not meaningful for propagation
     * in {@link PropagationParametersType#CARTESIAN Cartesian} parameters.
     * </p>
     * @param positionAngleType angle type to use for propagation
     */
    public void setPositionAngleType(final PositionAngle positionAngleType) {
        this.angleType = positionAngleType;
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    public PositionAngle getPositionAngleType() {
        return angleType;
    }

    /** {@inheritDoc} */
    public BoundedPropagator getGeneratedEphemeris()
        throws IllegalStateException {
        if (mode != EPHEMERIS_GENERATION_MODE) {
            throw OrekitException.createIllegalStateException(OrekitMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
        }
        return ((EphemerisModeHandler) modeHandler).getEphemeris();
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
        if (newtonianAttraction == null) {
            setMu(state.getMu());
        }
        this.initialState = state;
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
     * @see #getCurrentAdditionalState(String)
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
     * @see #getCurrentAdditionalState(String)
     */
    public void setInitialAdditionalState(final String name, final double[] addState)
        throws OrekitException {
        selectStateAndEquations(name).getData().setAdditionalState(addState);
    }

    /** {@inheritDoc} */
    public void notify(final SpacecraftState s, final EventDetector detector) {
        // Add occurred event to occurred events list
        occurredEvents.add(new OccurredEvent(s, detector));
    }

    /** {@inheritDoc} */
    public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
        try {
            if (startDate == null) {
                if (initialState == null) {
                    throw new PropagationException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
                }
                startDate = initialState.getDate();
            }
            return propagate(startDate, target);
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

            if (initialState == null) {
                throw new PropagationException(OrekitMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }

            if (!tStart.equals(initialState.getDate())) {
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
    private SpacecraftState propagate(final AbsoluteDate tEnd, final boolean activateHandlers)
        throws PropagationException {
        try {

            // reset occurred events list
            occurredEvents.clear();

            if (initialState.getDate().equals(tEnd)) {
                // don't extrapolate
                return initialState;
            }
            if (integrator == null) {
                throw new PropagationException(OrekitMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION);
            }

            // space dynamics view
            referenceDate  = initialState.getDate();

            // set propagation parameters type
            Orbit initialOrbit = null;
            switch (parametersType) {
            case CARTESIAN :
                initialOrbit = new CartesianOrbit(initialState.getOrbit());
                mapper = new StateMapperCartesian();
                break;
            case KEPLERIAN :
                initialOrbit = new KeplerianOrbit(initialState.getOrbit());
                mapper = new StateMapperKeplerian();
                break;
            case EQUINOCTIAL :
                initialOrbit = new EquinoctialOrbit(initialState.getOrbit());
                mapper = new StateMapperEquinoctial();
                break;
            default :
                throw OrekitException.createInternalError(null);
            }
            if (Double.isNaN(getMu())) {
                setMu(initialOrbit.getMu());
            }
            mapper.setAttitudeProvider(attitudeProvider);

            // initialize mode handler
            switch (mode) {
            case MASTER_MODE:
                break;
            case EPHEMERIS_GENERATION_MODE:
                break;
            default: // this should be slave mode
                break;
            }
            if (modeHandler != null) {
                List<AdditionalStateData> stateData = new ArrayList<AdditionalStateData>(addEquationsAndData.size());
                for (final AdditionalEquationsAndData equationsAndData : addEquationsAndData) {
                    stateData.add(equationsAndData.getData());
                }
                modeHandler.initialize(mapper, stateData, activateHandlers, referenceDate,
                                       initialState.getFrame(), newtonianAttraction.getMu());
            }

            // creating state vector
            this.stateVector  = new double[computeDimension()];

            currentState =
                new SpacecraftState(initialOrbit, initialState.getAttitude(), initialState.getMass());

            if (initialState.getMass() <= 0.0) {
                throw new IllegalArgumentException("Mass is null or negative");
            }

            // mathematical view
            final double t0 = 0;
            final double t1 = tEnd.durationFrom(initialState.getDate());

            // Map state to array
            mapper.mapStateToArray(initialState, stateVector);
            int index = 7;
            for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                final double[] addState = stateAndEqu.getData().getAdditionalState();
                System.arraycopy(addState, 0, stateVector, index, addState.length);
                // Incrementing index
                index += addState.length;
            }

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
            if (!addEquationsAndData.isEmpty()) {
                expandToleranceArray();
            }
            final double stopTime = integrator.integrate(new DifferentialEquations(), t0, stateVector, t1, stateVector);
            if (!addEquationsAndData.isEmpty()) {
                resetToleranceArray();
            }

            // back to space dynamics view
            final AbsoluteDate date = initialState.getDate().shiftedBy(stopTime);

            // get final additional state
            index = 7;
            for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                final double[] addState = stateAndEqu.getData().getAdditionalState();
                System.arraycopy(stateVector, index, addState, 0, addState.length);
                // Incrementing index
                index += addState.length;
            }

            // get final state
            initialState = mapper.mapArrayToState(stateVector, date, getMu(), initialState.getFrame());
            startDate = date;
            return initialState;

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        } catch (DerivativeException de) {

            // recover a possible embedded PropagationException
            for (Throwable t = de; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(de, de.getGeneralPattern(), de.getArguments());

        } catch (IntegratorException ie) {

            // recover a possible embedded PropagationException
            for (Throwable t = ie; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }

            throw new PropagationException(ie, ie.getGeneralPattern(), ie.getArguments());

        }
    }

    /** Expand integrator tolerance array to fit compound state vector.
     */
    private void expandToleranceArray() {
        if (integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = computeDimension();
            resizeArray(integrator, ABSOLUTE_TOLERANCE, n, Double.POSITIVE_INFINITY);
            resizeArray(integrator, RELATIVE_TOLERANCE, n, 0.0);
        }
    }

    /** Reset integrator tolerance array to original size.
     */
    private void resetToleranceArray() {
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
            new AdaptedEventDetector(osf, this, mapper, referenceDate,
                                     newtonianAttraction.getMu(), initialState.getFrame());
        integrator.addEventHandler(handler,
                                   osf.getMaxCheckInterval(),
                                   osf.getThreshold(),
                                   osf.getMaxIterationCount());
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

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations, TimeDerivativesEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = -1927530118454989452L;

        /** Reference to the derivatives array to initialize. */
        private double[] storedYDot;

        /** Jacobian of the orbital parameters with respect to the cartesian parameters. */
        private double[][] jacobian;

        /** Build a new instance. */
        public DifferentialEquations() {
            calls = 0;
            jacobian = new double[6][6];
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return computeDimension();
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            throws DerivativeException {

            try {
                // update space dynamics view
                currentState = mapper.mapArrayToState(y, referenceDate.shiftedBy(t), currentState.getMu(), currentState.getFrame());

                if (currentState.getMass() <= 0.0) {
                    throw new PropagationException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE,
                                                   currentState.getMass());
                }

                // initialize derivatives
                initDerivatives(yDot, currentState.getOrbit());

                // compute the contributions of all perturbing forces
                for (final ForceModel forceModel : forceModels) {
                    forceModel.addContribution(currentState, this);
                }

                // finalize derivatives by adding the Kepler contribution
                newtonianAttraction.addContribution(currentState, this);

                // Add contribution for additional state
                int index = 7;
                for (final AdditionalEquationsAndData stateAndEqu : addEquationsAndData) {
                    final double[] p    = stateAndEqu.getData().getAdditionalState();
                    final double[] pDot = stateAndEqu.getData().getAdditionalStateDot();

                    // update current additional state
                    System.arraycopy(y, index, p, 0, p.length);

                    // compute additional derivatives
                    stateAndEqu.getEquations().computeDerivatives(currentState, this, p, pDot);

                    // update each additional state contribution in global array
                    System.arraycopy(pDot, 0, yDot, index, p.length);

                    // incrementing index
                    index += p.length;
                }

                // increment calls counter
                ++calls;

            } catch (OrekitException oe) {
                throw new DerivativeException(oe.getSpecifier(), oe.getParts());
            }

        }

        public void initDerivatives(double[] yDot, Orbit currentOrbit) throws PropagationException {
            storedYDot = yDot;
            Arrays.fill(storedYDot, 0.0);
            currentOrbit.getJacobianWrtCartesian(angleType, jacobian);
        }

        /** {@inheritDoc} */
        public void addKeplerContribution(final double mu) {
            currentState.getOrbit().addKeplerContribution(angleType, mu, storedYDot);
        }

        /** {@inheritDoc} */
        public void addXYZAcceleration(final double x, final double y, final double z) {
            for (int i = 0; i < 6; ++i) {
                final double[] jRow = jacobian[i];
                storedYDot[i] += jRow[3] * x + jRow[4] * y + jRow[5] * z;
            }
        }

        /** {@inheritDoc} */
        public void addAcceleration(final Vector3D gamma, final Frame frame)
            throws OrekitException {
            final Transform t = frame.getTransformTo(currentState.getFrame(), currentState.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        public void addMassDerivative(final double q) {
            if (q > 0) {
                throw OrekitException.createIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
            }
            storedYDot[6] += q;
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

    /** Estimate tolerance vectors for integrators.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V<sup>2</sup> r |dV| = mu |dr|
     * </pre>
     * So we deduce a scalar velocity error consistent with the position error.
     * From here, we apply orbits Jacobians matrices to get consistent errors
     * on orbital parameters.
     * </p>
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances
     * are only local estimates, not global ones. So some care must be taken when using
     * these tolerances. Setting 1mm as a position error does NOT mean the tolerances
     * will guarantee a 1mm error position after several orbits integration.
     * </p>
     * @param dP user specified position error
     * @param orbit reference orbit
     * @param type propagation type for the meaning of the tolerance vectors elements
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     */
    public static double[][] tolerances(final double dP,
                                        final Orbit orbit, final PropagationParametersType type) {

        // estimate the scalar velocity error
        final PVCoordinates pv = orbit.getPVCoordinates();
        final double r2 = pv.getPosition().getNormSq();
        final double v  = pv.getVelocity().getNorm();
        final double dV = orbit.getMu() * dP / (v * r2);

        final double[] absTol = new double[7];
        final double[] relTol = new double[7];

        // we set the mass tolerance arbitrarily to 1.0e-6 kg, as mass evolves linearly
        // with trust, this often has no influence at all on propagation
        absTol[6] = 1.0e-6;

        if (type == PropagationParametersType.CARTESIAN) {
            absTol[0] = dP;
            absTol[1] = dP;
            absTol[2] = dP;
            absTol[3] = dV;
            absTol[4] = dV;
            absTol[5] = dV;
        } else {

            final Orbit converted = (type == PropagationParametersType.KEPLERIAN) ?
                                    new KeplerianOrbit(orbit) : new EquinoctialOrbit(orbit);

            final double[][] jacobian = new double[6][6];
            converted.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);
            for (int i = 0; i < 6; ++i) {
                final double[] row = jacobian[i];
                absTol[i] = FastMath.abs(row[0]) * dP +
                            FastMath.abs(row[1]) * dP +
                            FastMath.abs(row[2]) * dP +
                            FastMath.abs(row[3]) * dV +
                            FastMath.abs(row[4]) * dV +
                            FastMath.abs(row[5]) * dV;
            }

        }

        Arrays.fill(relTol, dP / FastMath.sqrt(r2));

        return new double[][] { absTol, relTol };

    }

}


