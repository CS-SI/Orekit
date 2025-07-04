/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.numerical;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DecompositionSolver;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.AbstractDragForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.jacobians.Duration;
import org.orekit.forces.maneuvers.jacobians.MassDepletionDelay;
import org.orekit.forces.maneuvers.jacobians.MedianDate;
import org.orekit.forces.maneuvers.jacobians.TriggerDate;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggerDetector;
import org.orekit.forces.maneuvers.trigger.ResettableManeuverTriggers;
import org.orekit.forces.radiation.RadiationForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.AdditionalDataProvider;
import org.orekit.propagation.CartesianToleranceProvider;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.events.DetectorModifier;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.ParameterDrivenDateIntervalDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.StateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** This class propagates {@link org.orekit.orbits.Orbit orbits} using
 * numerical integration.
 * <p>Numerical propagation is much more accurate than analytical propagation
 * like for example {@link org.orekit.propagation.analytical.KeplerianPropagator
 * Keplerian} or {@link org.orekit.propagation.analytical.EcksteinHechlerPropagator
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
 *   <li>the {@link OrbitType type} of orbital parameters to be used for propagation
 *   ({@link #setOrbitType(OrbitType)}),</li>
 *   <li>the {@link PositionAngleType type} of position angle to be used in orbital parameters
 *   to be used for propagation where it is relevant ({@link
 *   #setPositionAngleType(PositionAngleType)}),</li>
 *   <li>whether {@link MatricesHarvester state transition matrices and Jacobians matrices}
 *   (with the option to include mass if a 7x7 initial matrix is passed) should be propagated along with orbital state
 *   ({@link #setupMatricesComputation(String, RealMatrix, DoubleArrayDictionary)}),</li>
 *   <li>whether {@link org.orekit.propagation.integration.AdditionalDerivativesProvider additional derivatives}
 *   should be propagated along with orbital state ({@link
 *   #addAdditionalDerivativesProvider(AdditionalDerivativesProvider)}),</li>
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addEventDetector(EventDetector)},
 *   {@link #clearEventsDetectors()})</li>
 *   <li>the binding logic with the rest of the application ({@link #getMultiplexer()})</li>
 * </ul>
 * <p>From these configuration parameters, only the initial state is mandatory. The default
 * propagation settings are in {@link OrbitType#EQUINOCTIAL equinoctial} parameters with
 * {@link PositionAngleType#ECCENTRIC} longitude argument. If the central attraction coefficient
 * is not explicitly specified, the one used to define the initial orbit will be used.
 * However, specifying only the initial state and perhaps the central attraction coefficient
 * would mean the propagator would use only Keplerian forces. In this case, the simpler {@link
 * org.orekit.propagation.analytical.KeplerianPropagator KeplerianPropagator} class would
 * perhaps be more effective.</p>
 * <p>The underlying numerical integrator set up in the constructor may also have its own
 * configuration parameters. Typical configuration parameters for adaptive stepsize integrators
 * are the min, max and perhaps start step size as well as the absolute and/or relative errors
 * thresholds.</p>
 * <p>The state that is seen by the integrator is a simple seven elements double array.
 * The six first elements are either:
 * <ul>
 *   <li>the {@link org.orekit.orbits.EquinoctialOrbit equinoctial orbit parameters} (a, e<sub>x</sub>,
 *   e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, λ<sub>M</sub> or λ<sub>E</sub>
 *   or λ<sub>v</sub>) in meters and radians,</li>
 *   <li>the {@link org.orekit.orbits.KeplerianOrbit Keplerian orbit parameters} (a, e, i, ω, Ω,
 *   M or E or v) in meters and radians,</li>
 *   <li>the {@link org.orekit.orbits.CircularOrbit circular orbit parameters} (a, e<sub>x</sub>, e<sub>y</sub>, i,
 *   Ω, α<sub>M</sub> or α<sub>E</sub> or α<sub>v</sub>) in meters
 *   and radians,</li>
 *   <li>the {@link org.orekit.orbits.CartesianOrbit Cartesian orbit parameters} (x, y, z, v<sub>x</sub>,
 *   v<sub>y</sub>, v<sub>z</sub>) in meters and meters per seconds.
 * </ul>
 * <p> The last element is the mass in kilograms and changes only during thrusters firings
 *
 * <p>The following code snippet shows a typical setting for Low Earth Orbit propagation in
 * equinoctial parameters and true longitude argument:</p>
 * <pre>
 * final double dP       = 0.001;
 * final double minStep  = 0.001;
 * final double maxStep  = 500;
 * final double initStep = 60;
 * final double[][] tolerance = ToleranceProvider.getDefaultToleranceProvider(dP).getTolerances(orbit, OrbitType.EQUINOCTIAL);
 * AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tolerance[0], tolerance[1]);
 * integrator.setInitialStepSize(initStep);
 * propagator = new NumericalPropagator(integrator);
 * </pre>
 * <p>By default, at the end of the propagation, the propagator resets the initial state to the final state,
 * thus allowing a new propagation to be started from there without recomputing the part already performed.
 * This behaviour can be changed by calling {@link #setResetAtEnd(boolean)}.
 * </p>
 * <p>Beware the same instance cannot be used simultaneously by different threads, the class is <em>not</em>
 * thread-safe.</p>
 *
 * @see SpacecraftState
 * @see ForceModel
 * @see org.orekit.propagation.sampling.OrekitStepHandler
 * @see org.orekit.propagation.sampling.OrekitFixedStepHandler
 * @see org.orekit.propagation.integration.IntegratedEphemeris
 * @see TimeDerivativesEquations
 *
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class NumericalPropagator extends AbstractIntegratedPropagator {

    /** Default orbit type. */
    public static final OrbitType DEFAULT_ORBIT_TYPE = OrbitType.EQUINOCTIAL;

    /** Default position angle type. */
    public static final PositionAngleType DEFAULT_POSITION_ANGLE_TYPE = PositionAngleType.ECCENTRIC;

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** boolean to ignore or not the creation of a NewtonianAttraction. */
    private boolean ignoreCentralAttraction;

    /**
     * boolean to know if a full attitude (with rates) is needed when computing derivatives for the ODE.
     * since 12.1
     */
    private boolean needFullAttitudeForDerivatives = true;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a Keplerian
     * evolution only. The defaults are {@link OrbitType#EQUINOCTIAL}
     * for {@link #setOrbitType(OrbitType) propagation
     * orbit type} and {@link PositionAngleType#ECCENTRIC} for {@link
     * #setPositionAngleType(PositionAngleType) position angle type}.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param integrator numerical integrator to use for propagation.
     * @see #NumericalPropagator(ODEIntegrator, AttitudeProvider)
     */
    @DefaultDataContext
    public NumericalPropagator(final ODEIntegrator integrator) {
        this(integrator,
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a Keplerian
     * evolution only. The defaults are {@link OrbitType#EQUINOCTIAL}
     * for {@link #setOrbitType(OrbitType) propagation
     * orbit type} and {@link PositionAngleType#ECCENTRIC} for {@link
     * #setPositionAngleType(PositionAngleType) position angle type}.
     * @param integrator numerical integrator to use for propagation.
     * @param attitudeProvider the attitude law.
     * @since 10.1
     */
    public NumericalPropagator(final ODEIntegrator integrator,
                               final AttitudeProvider attitudeProvider) {
        super(integrator, PropagationType.OSCULATING);
        forceModels             = new ArrayList<>();
        ignoreCentralAttraction = false;
        initMapper();
        setAttitudeProvider(attitudeProvider);
        clearStepHandlers();
        setOrbitType(DEFAULT_ORBIT_TYPE);
        setPositionAngleType(DEFAULT_POSITION_ANGLE_TYPE);
    }

    /** Set the flag to ignore or not the creation of a {@link NewtonianAttraction}.
     * @param ignoreCentralAttraction if true, {@link NewtonianAttraction} is <em>not</em>
     * added automatically if missing
     */
    public void setIgnoreCentralAttraction(final boolean ignoreCentralAttraction) {
        this.ignoreCentralAttraction = ignoreCentralAttraction;
    }

     /** Set the central attraction coefficient μ.
      * <p>
      * Setting the central attraction coefficient is
      * equivalent to {@link #addForceModel(ForceModel) add}
      * a {@link NewtonianAttraction} force model.
      * * </p>
      * @param mu central attraction coefficient (m³/s²)
      * @see #addForceModel(ForceModel)
      * @see #getAllForceModels()
      */
    @Override
    public void setMu(final double mu) {
        if (ignoreCentralAttraction) {
            superSetMu(mu);
        } else {
            addForceModel(new NewtonianAttraction(mu));
            superSetMu(mu);
        }
    }

    /** Set the central attraction coefficient μ only in upper class.
     * @param mu central attraction coefficient (m³/s²)
     */
    private void superSetMu(final double mu) {
        super.setMu(mu);
    }

    /** Check if Newtonian attraction force model is available.
     * <p>
     * Newtonian attraction is always the last force model in the list.
     * </p>
     * @return true if Newtonian attraction force model is available
     */
    private boolean hasNewtonianAttraction() {
        final int last = forceModels.size() - 1;
        return last >= 0 && forceModels.get(last) instanceof NewtonianAttraction;
    }

    /** Add a force model.
     * <p>If this method is not called at all, the integrated orbit will follow
     * a Keplerian evolution only.</p>
     * @param model {@link ForceModel} to add (it can be either a perturbing force
     * model or an instance of {@link NewtonianAttraction})
     * @see #removeForceModels()
     * @see #setMu(double)
     */
    public void addForceModel(final ForceModel model) {

        if (model instanceof NewtonianAttraction) {
            // we want to add the central attraction force model

            try {
                // ensure we are notified of any mu change
                model.getParametersDrivers().get(0).addObserver(new ParameterObserver() {
                    /** {@inheritDoc} */
                    @Override
                    public void valueSpanMapChanged(final TimeSpanMap<Double> previousValue, final ParameterDriver driver) {
                        superSetMu(driver.getValue());
                    }
                    /** {@inheritDoc} */
                    @Override
                    public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
                        superSetMu(driver.getValue());
                    }
                });
            } catch (OrekitException oe) {
                // this should never happen
                throw new OrekitInternalError(oe);
            }

            if (hasNewtonianAttraction()) {
                // there is already a central attraction model, replace it
                forceModels.set(forceModels.size() - 1, model);
            } else {
                // there are no central attraction model yet, add it at the end of the list
                forceModels.add(model);
            }
        } else {
            // we want to add a perturbing force model
            if (hasNewtonianAttraction()) {
                // insert the new force model before Newtonian attraction,
                // which should always be the last one in the list
                forceModels.add(forceModels.size() - 1, model);
            } else {
                // we only have perturbing force models up to now, just append at the end of the list
                forceModels.add(model);
            }
        }

    }

    /** Remove all force models (except central attraction).
     * <p>Once all perturbing forces have been removed (and as long as no new force
     * model is added), the integrated orbit will follow a Keplerian evolution
     * only.</p>
     * @see #addForceModel(ForceModel)
     */
    public void removeForceModels() {
        final int last = forceModels.size() - 1;
        if (hasNewtonianAttraction()) {
            // preserve the Newtonian attraction model at the end
            final ForceModel newton = forceModels.get(last);
            forceModels.clear();
            forceModels.add(newton);
        } else {
            forceModels.clear();
        }
    }

    /** Get all the force models, perturbing forces and Newtonian attraction included.
     * @return list of perturbing force models, with Newtonian attraction being the
     * last one
     * @see #addForceModel(ForceModel)
     * @see #setMu(double)
     */
    public List<ForceModel> getAllForceModels() {
        return Collections.unmodifiableList(forceModels);
    }

    /** Set propagation orbit type.
     * @param orbitType orbit type to use for propagation, null for
     * propagating using {@link org.orekit.utils.AbsolutePVCoordinates} rather than {@link Orbit}
     */
    @Override
    public void setOrbitType(final OrbitType orbitType) {
        super.setOrbitType(orbitType);
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation, null for
     * propagating using {@link org.orekit.utils.AbsolutePVCoordinates} rather than {@link Orbit}
     */
    @Override
    public OrbitType getOrbitType() {
        return super.getOrbitType();
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
    @Override
    public void setPositionAngleType(final PositionAngleType positionAngleType) {
        super.setPositionAngleType(positionAngleType);
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    @Override
    public PositionAngleType getPositionAngleType() {
        return super.getPositionAngleType();
    }

    /** Set the initial state.
     * @param initialState initial state
     */
    public void setInitialState(final SpacecraftState initialState) {
        resetInitialState(initialState);
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) {
        super.resetInitialState(state);
        if (!hasNewtonianAttraction()) {
            // use the state to define central attraction
            setMu(state.isOrbitDefined() ? state.getOrbit().getMu() : Double.NaN);
        }
        setStartDate(state.getDate());
    }

    /** Get the names of the parameters in the matrix returned by {@link MatricesHarvester#getParametersJacobian}.
     * @return names of the parameters (i.e. columns) of the Jacobian matrix
     */
    List<String> getJacobiansColumnsNames() {
        final List<String> columnsNames = new ArrayList<>();
        for (final ForceModel forceModel : getAllForceModels()) {
            for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (driver.isSelected() && !columnsNames.contains(driver.getNamesSpanMap().getFirstSpan().getData())) {
                    // As driver with same name should have same NamesSpanMap we only check if the first span is present,
                    // if not we add all span names to columnsNames
                    for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                        columnsNames.add(span.getData());
                    }
                }
            }
        }
        Collections.sort(columnsNames);
        return columnsNames;
    }

    /** {@inheritDoc}
     * <p>
     * Unlike other propagators, the numerical one can consider the mass as a state variable in the transition matrix.
     * To do so, a 7x7 initial matrix is to be passed instead of 6x6.
     * </p>
     * */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        return new NumericalPropagationHarvester(this, stmName, initialStm, initialJacobianColumns);
    }

    /** {@inheritDoc} */
    @Override
    public void clearMatricesComputation() {
        final List<AdditionalDerivativesProvider> copiedDerivativesProviders = new ArrayList<>(getAdditionalDerivativesProviders());
        copiedDerivativesProviders.stream().filter(AbstractStateTransitionMatrixGenerator.class::isInstance)
                .forEach(provider -> removeAdditionalDerivativesProvider(provider.getName()));
        final List<AdditionalDataProvider<?>> copiedDataProviders = new ArrayList<>(getAdditionalDataProviders());
        for (final AdditionalDataProvider<?> additionalDataProvider: copiedDataProviders) {
            if (additionalDataProvider instanceof TriggerDate) {
                final TriggerDate triggerDate = (TriggerDate) additionalDataProvider;
                if (triggerDate.getMassDepletionDelay() != null) {
                    removeAdditionalDerivativesProvider(triggerDate.getMassDepletionDelay().getName());
                }
                removeAdditionalDataProvider(additionalDataProvider.getName());
            } else if (additionalDataProvider instanceof MedianDate || additionalDataProvider instanceof Duration) {
                removeAdditionalDataProvider(additionalDataProvider.getName());
            }
        }
        super.clearMatricesComputation();
    }

    /** {@inheritDoc} */
    @Override
    protected void setUpStmAndJacobianGenerators() {

        final AbstractMatricesHarvester harvester = getHarvester();
        if (harvester != null) {

            // set up the additional equations and additional state providers
            final AbstractStateTransitionMatrixGenerator stmGenerator = setUpStmGenerator();
            final List<String> triggersDates = setUpTriggerDatesJacobiansColumns(stmGenerator);
            setUpRegularParametersJacobiansColumns(stmGenerator, triggersDates);

            // as we are now starting the propagation, everything is configured
            // we can freeze the names in the harvester
            harvester.freezeColumnsNames();

        }

    }

    /** Set up the State Transition Matrix Generator.
     * @return State Transition Matrix Generator
     * @since 11.1
     */
    private AbstractStateTransitionMatrixGenerator setUpStmGenerator() {

        final AbstractMatricesHarvester harvester = getHarvester();

        // add the STM generator corresponding to the current settings, and setup state accordingly
        AbstractStateTransitionMatrixGenerator stmGenerator = null;
        for (final AdditionalDerivativesProvider equations : getAdditionalDerivativesProviders()) {
            if (equations instanceof AbstractStateTransitionMatrixGenerator &&
                equations.getName().equals(harvester.getStmName())) {
                // the STM generator has already been set up in a previous propagation
                stmGenerator = (AbstractStateTransitionMatrixGenerator) equations;
                break;
            }
        }
        if (stmGenerator == null) {
            // this is the first time we need the STM generate, create it
            if (harvester.getStateDimension() > 6) {
                stmGenerator = new ExtendedStateTransitionMatrixGenerator(harvester.getStmName(), getAllForceModels(),
                        getAttitudeProvider());
            } else {
                stmGenerator = new StateTransitionMatrixGenerator(harvester.getStmName(), getAllForceModels(),
                        getAttitudeProvider());
            }
            addAdditionalDerivativesProvider(stmGenerator);
        }

        if (!getInitialIntegrationState().hasAdditionalData(harvester.getStmName())) {
            // add the initial State Transition Matrix if it is not already there
            // (perhaps due to a previous propagation)
            setInitialState(stmGenerator.setInitialStateTransitionMatrix(getInitialState(),
                                                                         harvester.getInitialStateTransitionMatrix(),
                                                                         getOrbitType(),
                                                                         getPositionAngleType()));
        }

        return stmGenerator;

    }

    /** Set up the Jacobians columns generator dedicated to trigger dates.
     * @param stmGenerator State Transition Matrix generator
     * @return names of the columns corresponding to trigger dates
     * @since 13.1
     */
    private List<String> setUpTriggerDatesJacobiansColumns(final AbstractStateTransitionMatrixGenerator stmGenerator) {

        final String stmName = stmGenerator.getName();
        final boolean isMassInStm = stmGenerator instanceof ExtendedStateTransitionMatrixGenerator;
        final List<String> names = new ArrayList<>();
        for (final ForceModel forceModel : getAllForceModels()) {
            if (forceModel instanceof Maneuver && ((Maneuver) forceModel).getManeuverTriggers() instanceof ResettableManeuverTriggers) {
                final Maneuver maneuver = (Maneuver) forceModel;
                final ResettableManeuverTriggers maneuverTriggers = (ResettableManeuverTriggers) maneuver.getManeuverTriggers();

                final Collection<EventDetector> selectedDetectors = maneuverTriggers.getEventDetectors().
                        filter(ManeuverTriggerDetector.class::isInstance).
                        map(triggerDetector -> ((ManeuverTriggerDetector<?>) triggerDetector).getDetector())
                        .collect(Collectors.toList());
                for (final EventDetector detector: selectedDetectors) {
                    if (detector instanceof ParameterDrivenDateIntervalDetector) {
                        final ParameterDrivenDateIntervalDetector d = (ParameterDrivenDateIntervalDetector) detector;
                        TriggerDate start;
                        TriggerDate stop;

                        if (d.getStartDriver().isSelected() || d.getMedianDriver().isSelected() || d.getDurationDriver().isSelected()) {
                            // normally datedriver should have only 1 span but just in case the user defines several span, there will
                            // be no problem here
                            for (Span<String> span = d.getStartDriver().getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                                start = manageTriggerDate(stmName, maneuver, maneuverTriggers, span.getData(), true,
                                        d.getThreshold(), isMassInStm);
                                names.add(start.getName());
                                start = null;
                            }
                        }
                        if (d.getStopDriver().isSelected() || d.getMedianDriver().isSelected() || d.getDurationDriver().isSelected()) {
                            // normally datedriver should have only 1 span but just in case the user defines several span, there will
                            // be no problem here
                            for (Span<String> span = d.getStopDriver().getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                                stop = manageTriggerDate(stmName, maneuver, maneuverTriggers, span.getData(), false,
                                        d.getThreshold(), isMassInStm);
                                names.add(stop.getName());
                                stop = null;
                            }
                        }
                        if (d.getMedianDriver().isSelected()) {
                            // for first span
                            Span<String> currentMedianNameSpan = d.getMedianDriver().getNamesSpanMap().getFirstSpan();
                            MedianDate median =
                                    manageMedianDate(d.getStartDriver().getNamesSpanMap().getFirstSpan().getData(),
                                            d.getStopDriver().getNamesSpanMap().getFirstSpan().getData(), currentMedianNameSpan.getData());
                            names.add(median.getName());
                            // for all span
                            // normally datedriver should have only 1 span but just in case the user defines several span, there will
                            // be no problem here. /!\ medianDate driver, startDate driver and stopDate driver must have same span number
                            for (int spanNumber = 1; spanNumber < d.getMedianDriver().getNamesSpanMap().getSpansNumber(); ++spanNumber) {
                                currentMedianNameSpan = d.getMedianDriver().getNamesSpanMap().getSpan(currentMedianNameSpan.getEnd());
                                median =
                                        manageMedianDate(d.getStartDriver().getNamesSpanMap().getSpan(currentMedianNameSpan.getStart()).getData(),
                                                d.getStopDriver().getNamesSpanMap().getSpan(currentMedianNameSpan.getStart()).getData(),
                                                currentMedianNameSpan.getData());
                                names.add(median.getName());

                            }

                        }
                        if (d.getDurationDriver().isSelected()) {
                            // for first span
                            Span<String> currentDurationNameSpan = d.getDurationDriver().getNamesSpanMap().getFirstSpan();
                            Duration duration =
                                    manageManeuverDuration(d.getStartDriver().getNamesSpanMap().getFirstSpan().getData(),
                                            d.getStopDriver().getNamesSpanMap().getFirstSpan().getData(), currentDurationNameSpan.getData());
                            names.add(duration.getName());
                            // for all span
                            for (int spanNumber = 1; spanNumber < d.getDurationDriver().getNamesSpanMap().getSpansNumber(); ++spanNumber) {
                                currentDurationNameSpan = d.getDurationDriver().getNamesSpanMap().getSpan(currentDurationNameSpan.getEnd());
                                duration =
                                        manageManeuverDuration(d.getStartDriver().getNamesSpanMap().getSpan(currentDurationNameSpan.getStart()).getData(),
                                                d.getStopDriver().getNamesSpanMap().getSpan(currentDurationNameSpan.getStart()).getData(),
                                                currentDurationNameSpan.getData());
                                names.add(duration.getName());

                            }
                        }
                    }
                }
            }
        }

        return names;

    }

    /** Manage a maneuver trigger date.
     * @param stmName name of the State Transition Matrix state
     * @param maneuver maneuver force model
     * @param mt trigger to which the driver is bound
     * @param driverName name of the date driver
     * @param start if true, the driver is a maneuver start
     * @param threshold event detector threshold
     * @param isMassInStm flag on presence on mass in STM
     * @return generator for the date driver
     * @since 13.1
     */
    private TriggerDate manageTriggerDate(final String stmName,
                                          final Maneuver maneuver,
                                          final ResettableManeuverTriggers mt,
                                          final String driverName,
                                          final boolean start,
                                          final double threshold,
                                          final boolean isMassInStm) {

        TriggerDate triggerGenerator = null;

        // check if we already have set up the provider
        for (final AdditionalDataProvider<?> provider : getAdditionalDataProviders()) {
            if (provider instanceof TriggerDate &&
                provider.getName().equals(driverName)) {
                // the Jacobian column generator has already been set up in a previous propagation
                triggerGenerator = (TriggerDate) provider;
                break;
            }
        }

        if (triggerGenerator == null) {
            // this is the first time we need the Jacobian column generator, create it
            if (isMassInStm) {
                triggerGenerator = new TriggerDate(stmName, driverName, start, maneuver, threshold, true);
            } else {
                final Optional<ForceModel> dragForce = getAllForceModels().stream().filter(AbstractDragForceModel.class::isInstance).findFirst();
                final Optional<ForceModel> srpForce = getAllForceModels().stream().filter(RadiationForceModel.class::isInstance).findFirst();
                final List<ForceModel> nonGravitationalForces = new ArrayList<>();
                dragForce.ifPresent(nonGravitationalForces::add);
                srpForce.ifPresent(nonGravitationalForces::add);
                triggerGenerator = new TriggerDate(stmName, driverName, start, maneuver, threshold, false,
                        nonGravitationalForces.toArray(new ForceModel[0]));
            }
            mt.addResetter(triggerGenerator);
            final MassDepletionDelay depletionDelay = triggerGenerator.getMassDepletionDelay();
            if (depletionDelay != null) {
                addAdditionalDerivativesProvider(depletionDelay);
            }
            addAdditionalDataProvider(triggerGenerator);
        }

        if (!getInitialIntegrationState().hasAdditionalData(driverName)) {
            // add the initial Jacobian column if it is not already there
            // (perhaps due to a previous propagation)
            final MassDepletionDelay depletionDelay = triggerGenerator.getMassDepletionDelay();
            final double[] zeroes = new double[depletionDelay == null ? 7 : 6];
            if (depletionDelay != null) {
                setInitialColumn(depletionDelay.getName(), zeroes);
            }
            setInitialColumn(driverName, getHarvester().getInitialJacobianColumn(driverName));
        }

        return triggerGenerator;

    }

    /** Manage a maneuver median date.
     * @param startName name of the start driver
     * @param stopName name of the stop driver
     * @param medianName name of the median driver
     * @return generator for the median driver
     * @since 11.1
     */
    private MedianDate manageMedianDate(final String startName, final String stopName, final String medianName) {

        MedianDate medianGenerator = null;

        // check if we already have set up the provider
        for (final AdditionalDataProvider<?> provider : getAdditionalDataProviders()) {
            if (provider instanceof MedianDate &&
                provider.getName().equals(medianName)) {
                // the Jacobian column generator has already been set up in a previous propagation
                medianGenerator = (MedianDate) provider;
                break;
            }
        }

        if (medianGenerator == null) {
            // this is the first time we need the Jacobian column generator, create it
            medianGenerator = new MedianDate(startName, stopName, medianName);
            addAdditionalDataProvider(medianGenerator);
        }

        if (!getInitialIntegrationState().hasAdditionalData(medianName)) {
            // add the initial Jacobian column if it is not already there
            // (perhaps due to a previous propagation)
            setInitialColumn(medianName, getHarvester().getInitialJacobianColumn(medianName));
        }

        return medianGenerator;

    }

    /** Manage a maneuver duration.
     * @param startName name of the start driver
     * @param stopName name of the stop driver
     * @param durationName name of the duration driver
     * @return generator for the median driver
     * @since 11.1
     */
    private Duration manageManeuverDuration(final String startName, final String stopName, final String durationName) {

        Duration durationGenerator = null;

        // check if we already have set up the provider
        for (final AdditionalDataProvider<?> provider : getAdditionalDataProviders()) {
            if (provider instanceof Duration &&
                provider.getName().equals(durationName)) {
                // the Jacobian column generator has already been set up in a previous propagation
                durationGenerator = (Duration) provider;
                break;
            }
        }

        if (durationGenerator == null) {
            // this is the first time we need the Jacobian column generator, create it
            durationGenerator = new Duration(startName, stopName, durationName);
            addAdditionalDataProvider(durationGenerator);
        }

        if (!getInitialIntegrationState().hasAdditionalData(durationName)) {
            // add the initial Jacobian column if it is not already there
            // (perhaps due to a previous propagation)
            setInitialColumn(durationName, getHarvester().getInitialJacobianColumn(durationName));
        }

        return durationGenerator;

    }

    /** Set up the Jacobians columns generator for regular parameters.
     * @param stmGenerator generator for the State Transition Matrix
     * @param triggerDates names of the columns already managed as trigger dates
     * @since 11.1
     */
    private void setUpRegularParametersJacobiansColumns(final AbstractStateTransitionMatrixGenerator stmGenerator,
                                                        final List<String> triggerDates) {

        // first pass: gather all parameters (excluding trigger dates), binding similar names together
        final ParameterDriversList selected = new ParameterDriversList();
        for (final ForceModel forceModel : getAllForceModels()) {
            for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                if (!triggerDates.contains(driver.getNamesSpanMap().getFirstSpan().getData())) {
                    // if the first span is not in triggerDates,
                    // it means that the driver is not a trigger date and can be selected here
                    selected.add(driver);
                }
            }
        }

        // second pass: now that shared parameter names are bound together,
        // their selections status have been synchronized, we can filter them
        selected.filter(true);

        // third pass: sort parameters lexicographically
        selected.sort();

        // add the Jacobians column generators corresponding to parameters, and setup state accordingly
        // a new column is needed for each value estimated so for each span of the parameterDriver
        for (final DelegatingDriver driver : selected.getDrivers()) {

            for (Span<String> currentNameSpan = driver.getNamesSpanMap().getFirstSpan(); currentNameSpan != null; currentNameSpan = currentNameSpan.next()) {

                IntegrableJacobianColumnGenerator generator = null;
                // check if we already have set up the providers
                for (final AdditionalDerivativesProvider provider : getAdditionalDerivativesProviders()) {
                    if (provider instanceof IntegrableJacobianColumnGenerator &&
                        provider.getName().equals(currentNameSpan.getData())) {
                        // the Jacobian column generator has already been set up in a previous propagation
                        generator = (IntegrableJacobianColumnGenerator) provider;
                        break;
                    }

                }

                if (generator == null) {
                    // this is the first time we need the Jacobian column generator, create it
                    final boolean isMassIncluded = stmGenerator.getStateDimension() == 7;
                    generator = new IntegrableJacobianColumnGenerator(stmGenerator, currentNameSpan.getData(), isMassIncluded);
                    addAdditionalDerivativesProvider(generator);
                }

                if (!getInitialIntegrationState().hasAdditionalData(currentNameSpan.getData())) {
                    // add the initial Jacobian column if it is not already there
                    // (perhaps due to a previous propagation)
                    setInitialColumn(currentNameSpan.getData(), getHarvester().getInitialJacobianColumn(currentNameSpan.getData()));
                }

            }

        }

    }

    /** Add the initial value of the column to the initial state.
     * <p>
     * The initial state must already contain the Cartesian State Transition Matrix.
     * </p>
     * @param columnName name of the column
     * @param dYdQ column of the Jacobian ∂Y/∂qₘ with respect to propagation type,
     * if null (which is the most frequent case), assumed to be 0
     * @since 11.1
     */
    private void setInitialColumn(final String columnName, final double[] dYdQ) {

        final SpacecraftState state = getInitialState();

        final AbstractStateTransitionMatrixGenerator generator = (AbstractStateTransitionMatrixGenerator)
                getAdditionalDerivativesProviders().stream()
                        .filter(AbstractStateTransitionMatrixGenerator.class::isInstance)
                        .collect(Collectors.toList()).get(0);
        final int expectedSize = generator.getStateDimension();
        if (dYdQ.length != expectedSize) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH, dYdQ.length, expectedSize);
        }

        // convert to Cartesian Jacobian
        final RealMatrix dYdC = MatrixUtils.createRealIdentityMatrix(expectedSize);
        final double[][] jacobian = new double[6][6];
        getOrbitType().convertType(state.getOrbit()).getJacobianWrtCartesian(getPositionAngleType(), jacobian);
        dYdC.setSubMatrix(jacobian, 0, 0);
        final DecompositionSolver solver = getSolver(dYdC);
        final double[] column = solver.solve(MatrixUtils.createRealVector(dYdQ)).toArray();

        // set additional state
        setInitialState(state.addAdditionalData(columnName, column));

    }

    /**
     * Method to get a linear system solver.
     * @param matrix matrix involved in linear systems
     * @return solver
     * @since 13.1
     */
    private DecompositionSolver getSolver(final RealMatrix matrix) {
        return new QRDecomposition(matrix, THRESHOLD).getSolver();
    }

    /** {@inheritDoc} */
    @Override
    protected AttitudeProvider initializeAttitudeProviderForDerivatives() {
        return needFullAttitudeForDerivatives ? getAttitudeProvider() : getFrozenAttitudeProvider();
    }

    /** {@inheritDoc} */
    @Override
    protected StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                       final OrbitType orbitType, final PositionAngleType positionAngleType,
                                       final AttitudeProvider attitudeProvider, final Frame frame) {
        return new OsculatingMapper(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
    }

    /** Internal mapper using directly osculating parameters. */
    private static class OsculatingMapper extends StateMapper {

        /** Simple constructor.
         * <p>
         * The position parameter type is meaningful only if {@link
         * #getOrbitType() propagation orbit type}
         * support it. As an example, it is not meaningful for propagation
         * in {@link OrbitType#CARTESIAN Cartesian} parameters.
         * </p>
         * @param referenceDate reference date
         * @param mu central attraction coefficient (m³/s²)
         * @param orbitType orbit type to use for mapping (can be null for {@link AbsolutePVCoordinates})
         * @param positionAngleType angle type to use for propagation
         * @param attitudeProvider attitude provider
         * @param frame inertial frame
         */
        OsculatingMapper(final AbsoluteDate referenceDate, final double mu,
                         final OrbitType orbitType, final PositionAngleType positionAngleType,
                         final AttitudeProvider attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState mapArrayToState(final AbsoluteDate date, final double[] y, final double[] yDot,
                                               final PropagationType type) {
            // the parameter type is ignored for the Numerical Propagator

            final double mass = y[6];
            if (mass <= 0.0) {
                throw new OrekitException(OrekitMessages.NOT_POSITIVE_SPACECRAFT_MASS, mass);
            }

            if (getOrbitType() == null) {
                // propagation uses absolute position-velocity-acceleration
                final Vector3D p = new Vector3D(y[0],    y[1],    y[2]);
                final Vector3D v = new Vector3D(y[3],    y[4],    y[5]);
                final Vector3D a;
                final AbsolutePVCoordinates absPva;
                if (yDot == null) {
                    absPva = new AbsolutePVCoordinates(getFrame(), new TimeStampedPVCoordinates(date, p, v));
                } else {
                    a = new Vector3D(yDot[3], yDot[4], yDot[5]);
                    absPva = new AbsolutePVCoordinates(getFrame(), new TimeStampedPVCoordinates(date, p, v, a));
                }

                final Attitude attitude = getAttitudeProvider().getAttitude(absPva, date, getFrame());
                return new SpacecraftState(absPva, attitude).withMass(mass);
            } else {
                // propagation uses regular orbits
                final Orbit orbit       = getOrbitType().mapArrayToOrbit(y, yDot, getPositionAngleType(), date, getMu(), getFrame());
                final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

                return new SpacecraftState(orbit, attitude).withMass(mass);
            }

        }

        /** {@inheritDoc} */
        public void mapStateToArray(final SpacecraftState state, final double[] y, final double[] yDot) {
            if (getOrbitType() == null) {
                // propagation uses absolute position-velocity-acceleration
                final Vector3D p = state.getAbsPVA().getPosition();
                final Vector3D v = state.getAbsPVA().getVelocity();
                y[0] = p.getX();
                y[1] = p.getY();
                y[2] = p.getZ();
                y[3] = v.getX();
                y[4] = v.getY();
                y[5] = v.getZ();
                y[6] = state.getMass();
            }
            else {
                getOrbitType().mapOrbitToArray(state.getOrbit(), getPositionAngleType(), y, yDot);
                y[6] = state.getMass();
            }
        }

    }

    /** {@inheritDoc} */
    protected MainStateEquations getMainStateEquations(final ODEIntegrator integrator) {
        return new Main(integrator, getOrbitType(), getPositionAngleType(), getAllForceModels());
    }

    /** Internal class for osculating parameters integration. */
    private class Main extends NumericalTimeDerivativesEquations implements MainStateEquations {

        /** Flag keeping track whether Jacobian matrix needs to be recomputed or not. */
        private final boolean recomputingJacobian;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         * @param orbitType orbit type
         * @param positionAngleType angle type
         * @param forceModelList forces
         */
        Main(final ODEIntegrator integrator, final OrbitType orbitType, final PositionAngleType positionAngleType,
             final List<ForceModel> forceModelList) {

            super(orbitType, positionAngleType, forceModelList);
            final int numberOfForces = forceModelList.size();
            if (orbitType != null && orbitType != OrbitType.CARTESIAN && numberOfForces > 0) {
                if (numberOfForces > 1) {
                    recomputingJacobian = true;
                } else {
                    recomputingJacobian = !(forceModelList.get(0) instanceof NewtonianAttraction);
                }
            } else {
                recomputingJacobian = false;
            }

            // feed internal event detectors
            setUpInternalDetectors(integrator);

        }

        /** Set up all user defined event detectors.
         * @param integrator numerical integrator to use for propagation.
         */
        private void setUpInternalDetectors(final ODEIntegrator integrator) {
            final NumericalTimeDerivativesEquations cartesianEquations = new NumericalTimeDerivativesEquations(OrbitType.CARTESIAN,
                    null, forceModels);
            for (final ForceModel forceModel : getForceModels()) {
                forceModel.getEventDetectors().forEach(detector -> setUpInternalEventDetector(integrator, detector,
                        cartesianEquations));
            }
            getAttitudeProvider().getEventDetectors().forEach(detector -> setUpInternalEventDetector(integrator,
                    detector, cartesianEquations));
        }

        /** Set up one internal event detector.
         * @param integrator numerical integrator to use for propagation.
         * @param eventDetector detector
         * @param cartesianEquations Cartesian derivatives model
         */
        private void setUpInternalEventDetector(final ODEIntegrator integrator,
                                                final EventDetector eventDetector,
                                                final NumericalTimeDerivativesEquations cartesianEquations) {
            final Optional<ExtendedStateTransitionMatrixGenerator> generator = getAdditionalDerivativesProviders().stream()
                    .filter(ExtendedStateTransitionMatrixGenerator.class::isInstance)
                    .map(ExtendedStateTransitionMatrixGenerator.class::cast)
                    .findFirst();
            final EventDetector internalDetector;
            if (generator.isPresent() && !eventDetector.dependsOnTimeOnly()) {
                // need to modify STM at each dynamics discontinuities
                final NumericalPropagationHarvester harvester = (NumericalPropagationHarvester) getHarvester();
                final SwitchEventHandler handler = new SwitchEventHandler(eventDetector.getHandler(), harvester,
                        cartesianEquations, getAttitudeProvider());
                internalDetector = getLocalDetector(eventDetector, handler);
            } else {
                internalDetector = eventDetector;
            }
            setUpEventDetector(integrator, internalDetector);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            final List<ForceModel> forceModelList = getForceModels();
            needFullAttitudeForDerivatives = forceModelList.stream().anyMatch(ForceModel::dependsOnAttitudeRate);

            forceModelList.forEach(fm -> fm.init(initialState, target));

        }

        /** {@inheritDoc} */
        @Override
        public double[] computeDerivatives(final SpacecraftState state) {
            setCurrentState(state);
            if (recomputingJacobian) {
                // propagation uses Jacobian matrix of orbital parameters w.r.t. Cartesian ones
                final double[][] jacobian = new double[6][6];
                state.getOrbit().getJacobianWrtCartesian(getPositionAngleType(), jacobian);
                setCoordinatesJacobian(jacobian);
            }
            return computeTimeDerivatives(state);

        }

    }

    /** Estimate tolerance vectors for integrators when propagating in absolute position-velocity-acceleration.
     * @param dP user specified position error
     * @param absPva reference absolute position-velocity-acceleration
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @deprecated since 13.0. Use {@link ToleranceProvider} for default and custom tolerances.
     */
    @Deprecated
    public static double[][] tolerances(final double dP, final AbsolutePVCoordinates absPva) {
        return ToleranceProvider.of(CartesianToleranceProvider.of(dP)).getTolerances(absPva);
    }

    /** Estimate tolerance vectors for integrators when propagating in orbits.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V r² |dV| = mu |dr|
     * </pre>
     * <p> So we deduce a scalar velocity error consistent with the position error.
     * From here, we apply orbits Jacobians matrices to get consistent errors
     * on orbital parameters.
     *
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances
     * are only local estimates, not global ones. So some care must be taken when using
     * these tolerances. Setting 1mm as a position error does NOT mean the tolerances
     * will guarantee a 1mm error position after several orbits integration.
     * </p>
     * @param dP user specified position error
     * @param orbit reference orbit
     * @param type propagation type for the meaning of the tolerance vectors elements
     * (it may be different from {@code orbit.getType()})
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @deprecated since 13.0. Use {@link ToleranceProvider} for default and custom tolerances.
     */
    @Deprecated
    public static double[][] tolerances(final double dP, final Orbit orbit, final OrbitType type) {
        return ToleranceProvider.getDefaultToleranceProvider(dP).getTolerances(orbit, type, PositionAngleType.TRUE);
    }

    /** Estimate tolerance vectors for integrators when propagating in orbits.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits,
     * starting from scalar position and velocity errors specified by the user.
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances
     * are only local estimates, not global ones. So some care must be taken when using
     * these tolerances. Setting 1mm as a position error does NOT mean the tolerances
     * will guarantee a 1mm error position after several orbits integration.
     * </p>
     * @param dP user specified position error
     * @param dV user specified velocity error
     * @param orbit reference orbit
     * @param type propagation type for the meaning of the tolerance vectors elements
     * (it may be different from {@code orbit.getType()})
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @since 10.3
     * @deprecated since 13.0. Use {@link ToleranceProvider} for default and custom tolerances.
     */
    @Deprecated
    public static double[][] tolerances(final double dP, final double dV,
                                        final Orbit orbit, final OrbitType type) {

        return ToleranceProvider.of(CartesianToleranceProvider.of(dP, dV, CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE))
                .getTolerances(orbit, type, PositionAngleType.TRUE);
    }

    /** {@inheritDoc} */
    @Override
    protected void beforeIntegration(final SpacecraftState initialState, final AbsoluteDate tEnd) {

        if (!getFrame().isPseudoInertial()) {

            // inspect all force models to find InertialForces
            for (ForceModel force : forceModels) {
                if (force instanceof InertialForces) {
                    return;
                }
            }

            // throw exception if no inertial forces found
            throw new OrekitException(OrekitMessages.INERTIAL_FORCE_MODEL_MISSING, getFrame().getName());

        }

    }

    /**
     * Creates local detector wrapping input one and using specific handler for dynamics discontinuities and STM.
     * @param eventDetector detector
     * @param switchEventHandler special handler
     * @return wrapped detector
     */
    private static EventDetector getLocalDetector(final EventDetector eventDetector,
                                                  final SwitchEventHandler switchEventHandler) {
        return new DetectorModifier() {
            @Override
            public EventDetector getDetector() {
                return eventDetector;
            }

            @Override
            public EventHandler getHandler() {
                return switchEventHandler;
            }
        };
    }
}
