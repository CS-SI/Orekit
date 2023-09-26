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
package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.ODEStateAndDerivative;
import org.hipparchus.ode.sampling.ODEStateInterpolator;
import org.hipparchus.ode.sampling.ODEStepHandler;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.StateMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FixedNumberInterpolationGrid;
import org.orekit.propagation.semianalytical.dsst.utilities.InterpolationGrid;
import org.orekit.propagation.semianalytical.dsst.utilities.MaxGapInterpolationGrid;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.DoubleArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeSpanMap.Span;

/**
 * This class propagates {@link org.orekit.orbits.Orbit orbits} using the DSST theory.
 * <p>
 * Whereas analytical propagators are configured only thanks to their various
 * constructors and can be used immediately after construction, such a semianalytical
 * propagator configuration involves setting several parameters between construction
 * time and propagation time, just as numerical propagators.
 * </p>
 * <p>
 * The configuration parameters that can be set are:
 * </p>
 * <ul>
 * <li>the initial spacecraft state ({@link #setInitialState(SpacecraftState)})</li>
 * <li>the various force models ({@link #addForceModel(DSSTForceModel)},
 * {@link #removeForceModels()})</li>
 * <li>the discrete events that should be triggered during propagation (
 * {@link #addEventDetector(org.orekit.propagation.events.EventDetector)},
 * {@link #clearEventsDetectors()})</li>
 * <li>the binding logic with the rest of the application ({@link #getMultiplexer()})</li>
 * </ul>
 * <p>
 * From these configuration parameters, only the initial state is mandatory.
 * The default propagation settings are in {@link OrbitType#EQUINOCTIAL equinoctial}
 * parameters with {@link PositionAngleType#TRUE true} longitude argument.
 * The central attraction coefficient used to define the initial orbit will be used.
 * However, specifying only the initial state would mean the propagator would use
 * only Keplerian forces. In this case, the simpler
 * {@link org.orekit.propagation.analytical.KeplerianPropagator KeplerianPropagator}
 * class would be more effective.
 * </p>
 * <p>
 * The underlying numerical integrator set up in the constructor may also have
 * its own configuration parameters. Typical configuration parameters for adaptive
 * stepsize integrators are the min, max and perhaps start step size as well as
 * the absolute and/or relative errors thresholds.
 * </p>
 * <p>
 * The state that is seen by the integrator is a simple six elements double array.
 * These six elements are:
 * <ul>
 * <li>the {@link org.orekit.orbits.EquinoctialOrbit equinoctial orbit parameters}
 * (a, e<sub>x</sub>, e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, λ<sub>m</sub>)
 * in meters and radians,</li>
 * </ul>
 *
 * <p>By default, at the end of the propagation, the propagator resets the initial state to the final state,
 * thus allowing a new propagation to be started from there without recomputing the part already performed.
 * This behaviour can be chenged by calling {@link #setResetAtEnd(boolean)}.
 * </p>
 * <p>Beware the same instance cannot be used simultaneously by different threads, the class is <em>not</em>
 * thread-safe.</p>
 *
 * @see SpacecraftState
 * @see DSSTForceModel
 * @author Romain Di Costanzo
 * @author Pascal Parraud
 */
public class DSSTPropagator extends AbstractIntegratedPropagator {

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int I = 1;

    /** Default value for epsilon. */
    private static final double EPSILON_DEFAULT = 1.0e-13;

    /** Default value for maxIterations. */
    private static final int MAX_ITERATIONS_DEFAULT = 200;

    /** Number of grid points per integration step to be used in interpolation of short periodics coefficients.*/
    private static final int INTERPOLATION_POINTS_PER_STEP = 3;

    /** Flag specifying whether the initial orbital state is given with osculating elements. */
    private boolean initialIsOsculating;

    /** Force models used to compute short periodic terms. */
    private final transient List<DSSTForceModel> forceModels;

    /** State mapper holding the force models. */
    private MeanPlusShortPeriodicMapper mapper;

    /** Generator for the interpolation grid. */
    private InterpolationGrid interpolationgrid;

    /** Create a new instance of DSSTPropagator.
     *  <p>
     *  After creation, there are no perturbing forces at all.
     *  This means that if {@link #addForceModel addForceModel}
     *  is not called after creation, the integrated orbit will
     *  follow a Keplerian evolution only.
     *  </p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     *  @param integrator numerical integrator to use for propagation.
     *  @param propagationType type of orbit to output (mean or osculating).
     * @see #DSSTPropagator(ODEIntegrator, PropagationType, AttitudeProvider)
     */
    @DefaultDataContext
    public DSSTPropagator(final ODEIntegrator integrator, final PropagationType propagationType) {
        this(integrator, propagationType,
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Create a new instance of DSSTPropagator.
     *  <p>
     *  After creation, there are no perturbing forces at all.
     *  This means that if {@link #addForceModel addForceModel}
     *  is not called after creation, the integrated orbit will
     *  follow a Keplerian evolution only.
     *  </p>
     * @param integrator numerical integrator to use for propagation.
     * @param propagationType type of orbit to output (mean or osculating).
     * @param attitudeProvider the attitude law.
     * @since 10.1
     */
    public DSSTPropagator(final ODEIntegrator integrator,
                          final PropagationType propagationType,
                          final AttitudeProvider attitudeProvider) {
        super(integrator, propagationType);
        forceModels = new ArrayList<DSSTForceModel>();
        initMapper();
        // DSST uses only equinoctial orbits and mean longitude argument
        setOrbitType(OrbitType.EQUINOCTIAL);
        setPositionAngleType(PositionAngleType.MEAN);
        setAttitudeProvider(attitudeProvider);
        setInterpolationGridToFixedNumberOfPoints(INTERPOLATION_POINTS_PER_STEP);
    }


    /** Create a new instance of DSSTPropagator.
     *  <p>
     *  After creation, there are no perturbing forces at all.
     *  This means that if {@link #addForceModel addForceModel}
     *  is not called after creation, the integrated orbit will
     *  follow a Keplerian evolution only. Only the mean orbits
     *  will be generated.
     *  </p>
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     *  @param integrator numerical integrator to use for propagation.
     * @see #DSSTPropagator(ODEIntegrator, PropagationType, AttitudeProvider)
     */
    @DefaultDataContext
    public DSSTPropagator(final ODEIntegrator integrator) {
        this(integrator, PropagationType.MEAN);
    }

    /** Set the central attraction coefficient μ.
     * <p>
     * Setting the central attraction coefficient is
     * equivalent to {@link #addForceModel(DSSTForceModel) add}
     * a {@link DSSTNewtonianAttraction} force model.
     * </p>
    * @param mu central attraction coefficient (m³/s²)
    * @see #addForceModel(DSSTForceModel)
    * @see #getAllForceModels()
    */
    public void setMu(final double mu) {
        addForceModel(new DSSTNewtonianAttraction(mu));
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
        return last >= 0 && forceModels.get(last) instanceof DSSTNewtonianAttraction;
    }

    /** Set the initial state with osculating orbital elements.
     *  @param initialState initial state (defined with osculating elements)
     */
    public void setInitialState(final SpacecraftState initialState) {
        setInitialState(initialState, PropagationType.OSCULATING);
    }

    /** Set the initial state.
     *  @param initialState initial state
     *  @param stateType defined if the orbital state is defined with osculating or mean elements
     */
    public void setInitialState(final SpacecraftState initialState,
                                final PropagationType stateType) {
        switch (stateType) {
            case MEAN:
                initialIsOsculating = false;
                break;
            case OSCULATING:
                initialIsOsculating = true;
                break;
            default:
                throw new OrekitInternalError(null);
        }
        resetInitialState(initialState);
    }

    /** Reset the initial state.
     *
     *  @param state new initial state
     */
    @Override
    public void resetInitialState(final SpacecraftState state) {
        super.resetInitialState(state);
        if (!hasNewtonianAttraction()) {
            // use the state to define central attraction
            setMu(state.getMu());
        }
        super.setStartDate(state.getDate());
    }

    /** Set the selected short periodic coefficients that must be stored as additional states.
     * @param selectedCoefficients short periodic coefficients that must be stored as additional states
     * (null means no coefficients are selected, empty set means all coefficients are selected)
     */
    public void setSelectedCoefficients(final Set<String> selectedCoefficients) {
        mapper.setSelectedCoefficients(selectedCoefficients == null ?
                                       null : new HashSet<String>(selectedCoefficients));
    }

    /** Get the selected short periodic coefficients that must be stored as additional states.
     * @return short periodic coefficients that must be stored as additional states
     * (null means no coefficients are selected, empty set means all coefficients are selected)
     */
    public Set<String> getSelectedCoefficients() {
        final Set<String> set = mapper.getSelectedCoefficients();
        return set == null ? null : Collections.unmodifiableSet(set);
    }

    /** Get the names of the parameters in the matrix returned by {@link MatricesHarvester#getParametersJacobian}.
     * @return names of the parameters (i.e. columns) of the Jacobian matrix
     */
    protected List<String> getJacobiansColumnsNames() {
        final List<String> columnsNames = new ArrayList<>();
        for (final DSSTForceModel forceModel : getAllForceModels()) {
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

    /** {@inheritDoc} */
    @Override
    protected AbstractMatricesHarvester createHarvester(final String stmName, final RealMatrix initialStm,
                                                        final DoubleArrayDictionary initialJacobianColumns) {
        return new DSSTHarvester(this, stmName, initialStm, initialJacobianColumns);
    }

    /** {@inheritDoc} */
    @Override
    protected void setUpStmAndJacobianGenerators() {

        final AbstractMatricesHarvester harvester = getHarvester();
        if (harvester != null) {

            // set up the additional equations and additional state providers
            final DSSTStateTransitionMatrixGenerator stmGenerator = setUpStmGenerator();
            setUpRegularParametersJacobiansColumns(stmGenerator);

            // as we are now starting the propagation, everything is configured
            // we can freeze the names in the harvester
            harvester.freezeColumnsNames();

        }

    }

    /** Set up the State Transition Matrix Generator.
     * @return State Transition Matrix Generator
     * @since 11.1
     */
    private DSSTStateTransitionMatrixGenerator setUpStmGenerator() {

        final AbstractMatricesHarvester harvester = getHarvester();

        // add the STM generator corresponding to the current settings, and setup state accordingly
        DSSTStateTransitionMatrixGenerator stmGenerator = null;
        for (final AdditionalDerivativesProvider equations : getAdditionalDerivativesProviders()) {
            if (equations instanceof DSSTStateTransitionMatrixGenerator &&
                equations.getName().equals(harvester.getStmName())) {
                // the STM generator has already been set up in a previous propagation
                stmGenerator = (DSSTStateTransitionMatrixGenerator) equations;
                break;
            }
        }
        if (stmGenerator == null) {
            // this is the first time we need the STM generate, create it
            stmGenerator = new DSSTStateTransitionMatrixGenerator(harvester.getStmName(),
                                                                  getAllForceModels(),
                                                                  getAttitudeProvider());
            addAdditionalDerivativesProvider(stmGenerator);
        }

        if (!getInitialIntegrationState().hasAdditionalState(harvester.getStmName())) {
            // add the initial State Transition Matrix if it is not already there
            // (perhaps due to a previous propagation)
            setInitialState(stmGenerator.setInitialStateTransitionMatrix(getInitialState(),
                                                                         harvester.getInitialStateTransitionMatrix()),
                            getPropagationType());
        }

        return stmGenerator;

    }

    /** Set up the Jacobians columns generator for regular parameters.
     * @param stmGenerator generator for the State Transition Matrix
     * @since 11.1
     */
    private void setUpRegularParametersJacobiansColumns(final DSSTStateTransitionMatrixGenerator stmGenerator) {

        // first pass: gather all parameters (excluding trigger dates), binding similar names together
        final ParameterDriversList selected = new ParameterDriversList();
        for (final DSSTForceModel forceModel : getAllForceModels()) {
            for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
                selected.add(driver);
            }
        }

        // second pass: now that shared parameter names are bound together,
        // their selections status have been synchronized, we can filter them
        selected.filter(true);

        // third pass: sort parameters lexicographically
        selected.sort();

        // add the Jacobians column generators corresponding to parameters, and setup state accordingly
        for (final DelegatingDriver driver : selected.getDrivers()) {

            for (Span<String> span = driver.getNamesSpanMap().getFirstSpan(); span != null; span = span.next()) {
                DSSTIntegrableJacobianColumnGenerator generator = null;

                // check if we already have set up the providers
                for (final AdditionalDerivativesProvider provider : getAdditionalDerivativesProviders()) {
                    if (provider instanceof DSSTIntegrableJacobianColumnGenerator &&
                        provider.getName().equals(span.getData())) {
                        // the Jacobian column generator has already been set up in a previous propagation
                        generator = (DSSTIntegrableJacobianColumnGenerator) provider;
                        break;
                    }
                }

                if (generator == null) {
                    // this is the first time we need the Jacobian column generator, create it
                    generator = new DSSTIntegrableJacobianColumnGenerator(stmGenerator, span.getData());
                    addAdditionalDerivativesProvider(generator);
                }

                if (!getInitialIntegrationState().hasAdditionalState(span.getData())) {
                    // add the initial Jacobian column if it is not already there
                    // (perhaps due to a previous propagation)
                    setInitialState(getInitialState().addAdditionalState(span.getData(),
                                                                         getHarvester().getInitialJacobianColumn(span.getData())),
                                    getPropagationType());
                }
            }

        }

    }

    /** Check if the initial state is provided in osculating elements.
     * @return true if initial state is provided in osculating elements
     */
    public boolean initialIsOsculating() {
        return initialIsOsculating;
    }

    /** Set the interpolation grid generator.
     * <p>
     * The generator will create an interpolation grid with a fixed
     * number of points for each mean element integration step.
     * </p>
     * <p>
     * If neither {@link #setInterpolationGridToFixedNumberOfPoints(int)}
     * nor {@link #setInterpolationGridToMaxTimeGap(double)} has been called,
     * by default the propagator is set as to 3 interpolations points per step.
     * </p>
     * @param interpolationPoints number of interpolation points at
     * each integration step
     * @see #setInterpolationGridToMaxTimeGap(double)
     * @since 7.1
     */
    public void setInterpolationGridToFixedNumberOfPoints(final int interpolationPoints) {
        interpolationgrid = new FixedNumberInterpolationGrid(interpolationPoints);
    }

    /** Set the interpolation grid generator.
     * <p>
     * The generator will create an interpolation grid with a maximum
     * time gap between interpolation points.
     * </p>
     * <p>
     * If neither {@link #setInterpolationGridToFixedNumberOfPoints(int)}
     * nor {@link #setInterpolationGridToMaxTimeGap(double)} has been called,
     * by default the propagator is set as to 3 interpolations points per step.
     * </p>
     * @param maxGap maximum time gap between interpolation points (seconds)
     * @see #setInterpolationGridToFixedNumberOfPoints(int)
     * @since 7.1
     */
    public void setInterpolationGridToMaxTimeGap(final double maxGap) {
        interpolationgrid = new MaxGapInterpolationGrid(maxGap);
    }

    /** Add a force model to the global perturbation model.
     *  <p>
     *  If this method is not called at all,
     *  the integrated orbit will follow a Keplerian evolution only.
     *  </p>
     *  @param force perturbing {@link DSSTForceModel force} to add
     *  @see #removeForceModels()
     *  @see #setMu(double)
     */
    public void addForceModel(final DSSTForceModel force) {

        if (force instanceof DSSTNewtonianAttraction) {
            // we want to add the central attraction force model

            // ensure we are notified of any mu change
            force.getParametersDrivers().get(0).addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
                    // mu PDriver should have only 1 span
                    superSetMu(driver.getValue());
                }
                /** {@inheritDoc} */
                @Override
                public void valueSpanMapChanged(final TimeSpanMap<Double> previousValue, final ParameterDriver driver) {
                    // mu PDriver should have only 1 span
                    superSetMu(driver.getValue());
                }
            });

            if (hasNewtonianAttraction()) {
                // there is already a central attraction model, replace it
                forceModels.set(forceModels.size() - 1, force);
            } else {
                // there are no central attraction model yet, add it at the end of the list
                forceModels.add(force);
            }
        } else {
            // we want to add a perturbing force model
            if (hasNewtonianAttraction()) {
                // insert the new force model before Newtonian attraction,
                // which should always be the last one in the list
                forceModels.add(forceModels.size() - 1, force);
            } else {
                // we only have perturbing force models up to now, just append at the end of the list
                forceModels.add(force);
            }
        }

        force.registerAttitudeProvider(getAttitudeProvider());

    }

    /** Remove all perturbing force models from the global perturbation model
     *  (except central attraction).
     *  <p>
     *  Once all perturbing forces have been removed (and as long as no new force model is added),
     *  the integrated orbit will follow a Keplerian evolution only.
     *  </p>
     *  @see #addForceModel(DSSTForceModel)
     */
    public void removeForceModels() {
        final int last = forceModels.size() - 1;
        if (hasNewtonianAttraction()) {
            // preserve the Newtonian attraction model at the end
            final DSSTForceModel newton = forceModels.get(last);
            forceModels.clear();
            forceModels.add(newton);
        } else {
            forceModels.clear();
        }
    }

    /** Get all the force models, perturbing forces and Newtonian attraction included.
     * @return list of perturbing force models, with Newtonian attraction being the
     * last one
     * @see #addForceModel(DSSTForceModel)
     * @see #setMu(double)
     */
    public List<DSSTForceModel> getAllForceModels() {
        return Collections.unmodifiableList(forceModels);
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation
     */
    public OrbitType getOrbitType() {
        return super.getOrbitType();
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    public PositionAngleType getPositionAngleType() {
        return super.getPositionAngleType();
    }

    /** Conversion from mean to osculating orbit.
     * <p>
     * Compute osculating state <b>in a DSST sense</b>, corresponding to the
     * mean SpacecraftState in input, and according to the Force models taken
     * into account.
     * </p><p>
     * Since the osculating state is obtained by adding short-periodic variation
     * of each force model, the resulting output will depend on the
     * force models parameterized in input.
     * </p>
     * @param mean Mean state to convert
     * @param forces Forces to take into account
     * @param attitudeProvider attitude provider (may be null if there are no Gaussian force models
     * like atmospheric drag, radiation pressure or specific user-defined models)
     * @return osculating state in a DSST sense
     */
    public static SpacecraftState computeOsculatingState(final SpacecraftState mean,
                                                         final AttitudeProvider attitudeProvider,
                                                         final Collection<DSSTForceModel> forces) {

        //Create the auxiliary object
        final AuxiliaryElements aux = new AuxiliaryElements(mean.getOrbit(), I);

        // Set the force models
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        for (final DSSTForceModel force : forces) {
            force.registerAttitudeProvider(attitudeProvider);
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, force.getParameters(mean.getDate())));
            force.updateShortPeriodTerms(force.getParametersAllValues(), mean);
        }

        final EquinoctialOrbit osculatingOrbit = computeOsculatingOrbit(mean, shortPeriodTerms);

        return new SpacecraftState(osculatingOrbit, mean.getAttitude(), mean.getMass(),
                                   mean.getAdditionalStatesValues(), mean.getAdditionalStatesDerivatives());

    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean state <b>in a DSST sense</b>, corresponding to the
     * osculating SpacecraftState in input, and according to the Force models
     * taken into account.
     * </p><p>
     * Since the osculating state is obtained with the computation of
     * short-periodic variation of each force model, the resulting output will
     * depend on the force models parameterized in input.
     * </p><p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param osculating Osculating state to convert
     * @param attitudeProvider attitude provider (may be null if there are no Gaussian force models
     * like atmospheric drag, radiation pressure or specific user-defined models)
     * @param forceModels Forces to take into account
     * @return mean state in a DSST sense
     */
    public static SpacecraftState computeMeanState(final SpacecraftState osculating,
                                                   final AttitudeProvider attitudeProvider,
                                                   final Collection<DSSTForceModel> forceModels) {
        return computeMeanState(osculating, attitudeProvider, forceModels, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
    }

    /** Conversion from osculating to mean orbit.
     * <p>
     * Compute mean state <b>in a DSST sense</b>, corresponding to the
     * osculating SpacecraftState in input, and according to the Force models
     * taken into account.
     * </p><p>
     * Since the osculating state is obtained with the computation of
     * short-periodic variation of each force model, the resulting output will
     * depend on the force models parameterized in input.
     * </p><p>
     * The computation is done through a fixed-point iteration process.
     * </p>
     * @param osculating Osculating state to convert
     * @param attitudeProvider attitude provider (may be null if there are no Gaussian force models
     * like atmospheric drag, radiation pressure or specific user-defined models)
     * @param forceModels Forces to take into account
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean state in a DSST sense
     * @since 10.1
     */
    public static SpacecraftState computeMeanState(final SpacecraftState osculating,
                                                   final AttitudeProvider attitudeProvider,
                                                   final Collection<DSSTForceModel> forceModels,
                                                   final double epsilon,
                                                   final int maxIterations) {
        final Orbit meanOrbit = computeMeanOrbit(osculating, attitudeProvider, forceModels, epsilon, maxIterations);
        return new SpacecraftState(meanOrbit, osculating.getAttitude(), osculating.getMass(),
                                   osculating.getAdditionalStatesValues(), osculating.getAdditionalStatesDerivatives());
    }

     /** Override the default value of the parameter.
     *  <p>
     *  By default, if the initial orbit is defined as osculating,
     *  it will be averaged over 2 satellite revolutions.
     *  This can be changed by using this method.
     *  </p>
     *  @param satelliteRevolution number of satellite revolutions to use for converting osculating to mean
     *                             elements
     */
    public void setSatelliteRevolution(final int satelliteRevolution) {
        mapper.setSatelliteRevolution(satelliteRevolution);
    }

    /** Get the number of satellite revolutions to use for converting osculating to mean elements.
     *  @return number of satellite revolutions to use for converting osculating to mean elements
     */
    public int getSatelliteRevolution() {
        return mapper.getSatelliteRevolution();
    }

    /** Override the default value short periodic terms.
    *  <p>
    *  By default, short periodic terms are initialized before
    *  the numerical integration of the mean orbital elements.
    *  </p>
    *  @param shortPeriodTerms short periodic terms
    */
    public void setShortPeriodTerms(final List<ShortPeriodTerms> shortPeriodTerms) {
        mapper.setShortPeriodTerms(shortPeriodTerms);
    }

   /** Get the short periodic terms.
    *  @return the short periodic terms
    */
    public List<ShortPeriodTerms> getShortPeriodTerms() {
        return mapper.getShortPeriodTerms();
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        super.setAttitudeProvider(attitudeProvider);

        //Register the attitude provider for each force model
        for (final DSSTForceModel force : forceModels) {
            force.registerAttitudeProvider(attitudeProvider);
        }
    }

    /** Method called just before integration.
     * <p>
     * The default implementation does nothing, it may be specialized in subclasses.
     * </p>
     * @param initialState initial state
     * @param tEnd target date at which state should be propagated
     */
    @Override
    protected void beforeIntegration(final SpacecraftState initialState,
                                     final AbsoluteDate tEnd) {

        // check if only mean elements must be used
        final PropagationType type = getPropagationType();

        // compute common auxiliary elements
        final AuxiliaryElements aux = new AuxiliaryElements(initialState.getOrbit(), I);

        // initialize all perturbing forces
        final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
        for (final DSSTForceModel force : forceModels) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, type, force.getParameters(initialState.getDate())));
        }
        mapper.setShortPeriodTerms(shortPeriodTerms);

        // if required, insert the special short periodics step handler
        if (type == PropagationType.OSCULATING) {
            final ShortPeriodicsHandler spHandler = new ShortPeriodicsHandler(forceModels);
            // Compute short periodic coefficients for this point
            for (DSSTForceModel forceModel : forceModels) {
                forceModel.updateShortPeriodTerms(forceModel.getParametersAllValues(), initialState);
            }
            final Collection<ODEStepHandler> stepHandlers = new ArrayList<ODEStepHandler>();
            stepHandlers.add(spHandler);
            final ODEIntegrator integrator = getIntegrator();
            final Collection<ODEStepHandler> existing = integrator.getStepHandlers();
            stepHandlers.addAll(existing);

            integrator.clearStepHandlers();

            // add back the existing handlers after the short periodics one
            for (final ODEStepHandler sp : stepHandlers) {
                integrator.addStepHandler(sp);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void afterIntegration() {
        // remove the special short periodics step handler if added before
        if (getPropagationType() == PropagationType.OSCULATING) {
            final List<ODEStepHandler> preserved = new ArrayList<ODEStepHandler>();
            final ODEIntegrator integrator = getIntegrator();
            for (final ODEStepHandler sp : integrator.getStepHandlers()) {
                if (!(sp instanceof ShortPeriodicsHandler)) {
                    preserved.add(sp);
                }
            }

            // clear the list
            integrator.clearStepHandlers();

            // add back the step handlers that were important for the user
            for (final ODEStepHandler sp : preserved) {
                integrator.addStepHandler(sp);
            }
        }
    }

    /** Compute mean state from osculating state.
     * <p>
     * Compute in a DSST sense the mean state corresponding to the input osculating state.
     * </p><p>
     * The computing is done through a fixed-point iteration process.
     * </p>
     * @param osculating initial osculating state
     * @param attitudeProvider attitude provider (may be null if there are no Gaussian force models
     * like atmospheric drag, radiation pressure or specific user-defined models)
     * @param forceModels force models
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean state
     */
    private static Orbit computeMeanOrbit(final SpacecraftState osculating,
                                          final AttitudeProvider attitudeProvider,
                                          final Collection<DSSTForceModel> forceModels, final double epsilon, final int maxIterations) {

        // rough initialization of the mean parameters
        EquinoctialOrbit meanOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(osculating.getOrbit());

        // threshold for each parameter
        final double thresholdA = epsilon * (1 + FastMath.abs(meanOrbit.getA()));
        final double thresholdE = epsilon * (1 + meanOrbit.getE());
        final double thresholdI = epsilon * (1 + meanOrbit.getI());
        final double thresholdL = epsilon * FastMath.PI;

        // ensure all Gaussian force models can rely on attitude
        for (final DSSTForceModel force : forceModels) {
            force.registerAttitudeProvider(attitudeProvider);
        }

        int i = 0;
        while (i++ < maxIterations) {

            final SpacecraftState meanState = new SpacecraftState(meanOrbit, osculating.getAttitude(), osculating.getMass());

            //Create the auxiliary object
            final AuxiliaryElements aux = new AuxiliaryElements(meanOrbit, I);

            // Set the force models
            final List<ShortPeriodTerms> shortPeriodTerms = new ArrayList<ShortPeriodTerms>();
            for (final DSSTForceModel force : forceModels) {
                shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, force.getParameters(meanState.getDate())));
                force.updateShortPeriodTerms(force.getParametersAllValues(), meanState);
            }

            // recompute the osculating parameters from the current mean parameters
            final EquinoctialOrbit rebuilt = computeOsculatingOrbit(meanState, shortPeriodTerms);

            // adapted parameters residuals
            final double deltaA  = osculating.getA() - rebuilt.getA();
            final double deltaEx = osculating.getEquinoctialEx() - rebuilt.getEquinoctialEx();
            final double deltaEy = osculating.getEquinoctialEy() - rebuilt.getEquinoctialEy();
            final double deltaHx = osculating.getHx() - rebuilt.getHx();
            final double deltaHy = osculating.getHy() - rebuilt.getHy();
            final double deltaLv = MathUtils.normalizeAngle(osculating.getLv() - rebuilt.getLv(), 0.0);

            // check convergence
            if (FastMath.abs(deltaA)  < thresholdA &&
                FastMath.abs(deltaEx) < thresholdE &&
                FastMath.abs(deltaEy) < thresholdE &&
                FastMath.abs(deltaHx) < thresholdI &&
                FastMath.abs(deltaHy) < thresholdI &&
                FastMath.abs(deltaLv) < thresholdL) {
                return meanOrbit;
            }

            // update mean parameters
            meanOrbit = new EquinoctialOrbit(meanOrbit.getA() + deltaA,
                                             meanOrbit.getEquinoctialEx() + deltaEx,
                                             meanOrbit.getEquinoctialEy() + deltaEy,
                                             meanOrbit.getHx() + deltaHx,
                                             meanOrbit.getHy() + deltaHy,
                                             meanOrbit.getLv() + deltaLv,
                                             PositionAngleType.TRUE, meanOrbit.getFrame(),
                                             meanOrbit.getDate(), meanOrbit.getMu());
        }

        throw new OrekitException(OrekitMessages.UNABLE_TO_COMPUTE_DSST_MEAN_PARAMETERS, i);

    }

    /** Compute osculating state from mean state.
     * <p>
     * Compute and add the short periodic variation to the mean {@link SpacecraftState}.
     * </p>
     * @param meanState initial mean state
     * @param shortPeriodTerms short period terms
     * @return osculating state
     */
    private static EquinoctialOrbit computeOsculatingOrbit(final SpacecraftState meanState,
                                                           final List<ShortPeriodTerms> shortPeriodTerms) {

        final double[] mean = new double[6];
        final double[] meanDot = new double[6];
        OrbitType.EQUINOCTIAL.mapOrbitToArray(meanState.getOrbit(), PositionAngleType.MEAN, mean, meanDot);
        final double[] y = mean.clone();
        for (final ShortPeriodTerms spt : shortPeriodTerms) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] += shortPeriodic[i];
            }
        }
        return (EquinoctialOrbit) OrbitType.EQUINOCTIAL.mapArrayToOrbit(y, meanDot,
                                                                        PositionAngleType.MEAN, meanState.getDate(),
                                                                        meanState.getMu(), meanState.getFrame());
    }

    /** {@inheritDoc} */
    @Override
    protected SpacecraftState getInitialIntegrationState() {
        if (initialIsOsculating) {
            // the initial state is an osculating state,
            // it must be converted to mean state
            return computeMeanState(getInitialState(), getAttitudeProvider(), forceModels);
        } else {
            // the initial state is already a mean state
            return getInitialState();
        }
    }

    /** {@inheritDoc}
     * <p>
     * Note that for DSST, orbit type is hardcoded to {@link OrbitType#EQUINOCTIAL}
     * and position angle type is hardcoded to {@link PositionAngleType#MEAN}, so
     * the corresponding parameters are ignored.
     * </p>
     */
    @Override
    protected StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                       final OrbitType ignoredOrbitType, final PositionAngleType ignoredPositionAngleType,
                                       final AttitudeProvider attitudeProvider, final Frame frame) {

        // create a mapper with the common settings provided as arguments
        final MeanPlusShortPeriodicMapper newMapper =
                new MeanPlusShortPeriodicMapper(referenceDate, mu, attitudeProvider, frame);

        // copy the specific settings from the existing mapper
        if (mapper != null) {
            newMapper.setSatelliteRevolution(mapper.getSatelliteRevolution());
            newMapper.setSelectedCoefficients(mapper.getSelectedCoefficients());
            newMapper.setShortPeriodTerms(mapper.getShortPeriodTerms());
        }

        mapper = newMapper;
        return mapper;

    }


    /** Get the short period terms value.
     * @param meanState the mean state
     * @return shortPeriodTerms short period terms
     * @since 7.1
     */
    public double[] getShortPeriodTermsValue(final SpacecraftState meanState) {
        final double[] sptValue = new double[6];

        for (ShortPeriodTerms spt : mapper.getShortPeriodTerms()) {
            final double[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                sptValue[i] += shortPeriodic[i];
            }
        }
        return sptValue;
    }


    /** Internal mapper using mean parameters plus short periodic terms. */
    private static class MeanPlusShortPeriodicMapper extends StateMapper {

        /** Short periodic coefficients that must be stored as additional states. */
        private Set<String>                selectedCoefficients;

        /** Number of satellite revolutions in the averaging interval. */
        private int                        satelliteRevolution;

        /** Short period terms. */
        private List<ShortPeriodTerms>     shortPeriodTerms;

        /** Simple constructor.
         * @param referenceDate reference date
         * @param mu central attraction coefficient (m³/s²)
         * @param attitudeProvider attitude provider
         * @param frame inertial frame
         */
        MeanPlusShortPeriodicMapper(final AbsoluteDate referenceDate, final double mu,
                                    final AttitudeProvider attitudeProvider, final Frame frame) {

            super(referenceDate, mu, OrbitType.EQUINOCTIAL, PositionAngleType.MEAN, attitudeProvider, frame);

            this.selectedCoefficients = null;

            // Default averaging period for conversion from osculating to mean elements
            this.satelliteRevolution = 2;

            this.shortPeriodTerms    = Collections.emptyList();

        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState mapArrayToState(final AbsoluteDate date,
                                               final double[] y, final double[] yDot,
                                               final PropagationType type) {

            // add short periodic variations to mean elements to get osculating elements
            // (the loop may not be performed if there are no force models and in the
            //  case we want to remain in mean parameters only)
            final double[] elements = y.clone();
            final DoubleArrayDictionary coefficients;
            if (type == PropagationType.MEAN) {
                coefficients = null;
            } else {
                final Orbit meanOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(elements, yDot, PositionAngleType.MEAN, date, getMu(), getFrame());
                coefficients = selectedCoefficients == null ? null : new DoubleArrayDictionary();
                for (final ShortPeriodTerms spt : shortPeriodTerms) {
                    final double[] shortPeriodic = spt.value(meanOrbit);
                    for (int i = 0; i < shortPeriodic.length; i++) {
                        elements[i] += shortPeriodic[i];
                    }
                    if (selectedCoefficients != null) {
                        coefficients.putAll(spt.getCoefficients(date, selectedCoefficients));
                    }
                }
            }

            final double mass = elements[6];
            if (mass <= 0.0) {
                throw new OrekitException(OrekitMessages.NOT_POSITIVE_SPACECRAFT_MASS, mass);
            }

            final Orbit orbit       = OrbitType.EQUINOCTIAL.mapArrayToOrbit(elements, yDot, PositionAngleType.MEAN, date, getMu(), getFrame());
            final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

            if (coefficients == null) {
                return new SpacecraftState(orbit, attitude, mass);
            } else {
                return new SpacecraftState(orbit, attitude, mass, coefficients);
            }

        }

        /** {@inheritDoc} */
        @Override
        public void mapStateToArray(final SpacecraftState state, final double[] y, final double[] yDot) {

            OrbitType.EQUINOCTIAL.mapOrbitToArray(state.getOrbit(), PositionAngleType.MEAN, y, yDot);
            y[6] = state.getMass();

        }

        /** Set the number of satellite revolutions to use for converting osculating to mean elements.
         *  <p>
         *  By default, if the initial orbit is defined as osculating,
         *  it will be averaged over 2 satellite revolutions.
         *  This can be changed by using this method.
         *  </p>
         *  @param satelliteRevolution number of satellite revolutions to use for converting osculating to mean
         *                             elements
         */
        public void setSatelliteRevolution(final int satelliteRevolution) {
            this.satelliteRevolution = satelliteRevolution;
        }

        /** Get the number of satellite revolutions to use for converting osculating to mean elements.
         *  @return number of satellite revolutions to use for converting osculating to mean elements
         */
        public int getSatelliteRevolution() {
            return satelliteRevolution;
        }

        /** Set the selected short periodic coefficients that must be stored as additional states.
         * @param selectedCoefficients short periodic coefficients that must be stored as additional states
         * (null means no coefficients are selected, empty set means all coefficients are selected)
         */
        public void setSelectedCoefficients(final Set<String> selectedCoefficients) {
            this.selectedCoefficients = selectedCoefficients;
        }

        /** Get the selected short periodic coefficients that must be stored as additional states.
         * @return short periodic coefficients that must be stored as additional states
         * (null means no coefficients are selected, empty set means all coefficients are selected)
         */
        public Set<String> getSelectedCoefficients() {
            return selectedCoefficients;
        }

        /** Set the short period terms.
         * @param shortPeriodTerms short period terms
         * @since 7.1
         */
        public void setShortPeriodTerms(final List<ShortPeriodTerms> shortPeriodTerms) {
            this.shortPeriodTerms = shortPeriodTerms;
        }

        /** Get the short period terms.
         * @return shortPeriodTerms short period terms
         * @since 7.1
         */
        public List<ShortPeriodTerms> getShortPeriodTerms() {
            return shortPeriodTerms;
        }

    }

    /** {@inheritDoc} */
    @Override
    protected MainStateEquations getMainStateEquations(final ODEIntegrator integrator) {
        return new Main(integrator);
    }

    /** Internal class for mean parameters integration. */
    private class Main implements MainStateEquations {

        /** Derivatives array. */
        private final double[] yDot;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        Main(final ODEIntegrator integrator) {
            yDot = new double[7];

            // Setup event detectors for each force model
            forceModels.forEach(dsstForceModel -> dsstForceModel.getEventDetectors().
                                forEach(eventDetector -> setUpEventDetector(integrator, eventDetector)));
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            forceModels.forEach(fm -> fm.init(initialState, target));
        }

        /** {@inheritDoc} */
        @Override
        public double[] computeDerivatives(final SpacecraftState state) {

            Arrays.fill(yDot, 0.0);

            // compute common auxiliary elements
            final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), I);

            // compute the contributions of all perturbing forces
            for (final DSSTForceModel forceModel : forceModels) {
                final double[] daidt = elementRates(forceModel, state, auxiliaryElements, forceModel.getParameters(state.getDate()));
                for (int i = 0; i < daidt.length; i++) {
                    yDot[i] += daidt[i];
                }
            }

            return yDot.clone();
        }

        /** This method allows to compute the mean equinoctial elements rates da<sub>i</sub> / dt
         *  for a specific force model.
         *  @param forceModel force to take into account
         *  @param state current state
         *  @param auxiliaryElements auxiliary elements related to the current orbit
         *  @param parameters force model parameters at state date (only 1 value for
         *  each parameter
         *  @return the mean equinoctial elements rates da<sub>i</sub> / dt
         */
        private double[] elementRates(final DSSTForceModel forceModel,
                                      final SpacecraftState state,
                                      final AuxiliaryElements auxiliaryElements,
                                      final double[] parameters) {
            return forceModel.getMeanElementRate(state, auxiliaryElements, parameters);
        }

    }

    /** Estimate tolerance vectors for an AdaptativeStepsizeIntegrator.
     *  <p>
     *  The errors are estimated from partial derivatives properties of orbits,
     *  starting from a scalar position error specified by the user.
     *  Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     *  we get at constant energy (i.e. on a Keplerian trajectory):
     *
     *  <pre>
     *  V r² |dV| = mu |dr|
     *  </pre>
     *
     *  <p> So we deduce a scalar velocity error consistent with the position error. From here, we apply
     *  orbits Jacobians matrices to get consistent errors on orbital parameters.
     *
     *  <p>
     *  The tolerances are only <em>orders of magnitude</em>, and integrator tolerances are only
     *  local estimates, not global ones. So some care must be taken when using these tolerances.
     *  Setting 1mm as a position error does NOT mean the tolerances will guarantee a 1mm error
     *  position after several orbits integration.
     *  </p>
     *
     * @param dP user specified position error (m)
     * @param orbit reference orbit
     * @return a two rows array, row 0 being the absolute tolerance error
     *                       and row 1 being the relative tolerance error
     */
    public static double[][] tolerances(final double dP, final Orbit orbit) {

        return NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);

    }

    /** Estimate tolerance vectors for an AdaptativeStepsizeIntegrator.
     *  <p>
     *  The errors are estimated from partial derivatives properties of orbits,
     *  starting from scalar position and velocity errors specified by the user.
     *  <p>
     *  The tolerances are only <em>orders of magnitude</em>, and integrator tolerances are only
     *  local estimates, not global ones. So some care must be taken when using these tolerances.
     *  Setting 1mm as a position error does NOT mean the tolerances will guarantee a 1mm error
     *  position after several orbits integration.
     *  </p>
     *
     * @param dP user specified position error (m)
     * @param dV user specified velocity error (m/s)
     * @param orbit reference orbit
     * @return a two rows array, row 0 being the absolute tolerance error
     *                       and row 1 being the relative tolerance error
     * @since 10.3
     */
    public static double[][] tolerances(final double dP, final double dV, final Orbit orbit) {

        return NumericalPropagator.tolerances(dP, dV, orbit, OrbitType.EQUINOCTIAL);

    }

    /** Step handler used to compute the parameters for the short periodic contributions.
     * @author Lucian Barbulescu
     */
    private class ShortPeriodicsHandler implements ODEStepHandler {

        /** Force models used to compute short periodic terms. */
        private final List<DSSTForceModel> forceModels;

        /** Constructor.
         * @param forceModels force models
         */
        ShortPeriodicsHandler(final List<DSSTForceModel> forceModels) {
            this.forceModels = forceModels;
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final ODEStateInterpolator interpolator) {

            // Get the grid points to compute
            final double[] interpolationPoints =
                    interpolationgrid.getGridPoints(interpolator.getPreviousState().getTime(),
                                                    interpolator.getCurrentState().getTime());

            final SpacecraftState[] meanStates = new SpacecraftState[interpolationPoints.length];
            for (int i = 0; i < interpolationPoints.length; ++i) {

                // Build the mean state interpolated at grid point
                final double time = interpolationPoints[i];
                final ODEStateAndDerivative sd = interpolator.getInterpolatedState(time);
                meanStates[i] = mapper.mapArrayToState(time,
                                                       sd.getPrimaryState(),
                                                       sd.getPrimaryDerivative(),
                                                       PropagationType.MEAN);
            }

            // Computate short periodic coefficients for this step
            for (DSSTForceModel forceModel : forceModels) {
                forceModel.updateShortPeriodTerms(forceModel.getParametersAllValues(), meanStates);
            }
        }
    }
}
