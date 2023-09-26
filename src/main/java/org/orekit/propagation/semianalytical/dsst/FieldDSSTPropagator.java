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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.ode.FieldODEStateAndDerivative;
import org.hipparchus.ode.sampling.FieldODEStateInterpolator;
import org.hipparchus.ode.sampling.FieldODEStepHandler;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.FieldAbstractIntegratedPropagator;
import org.orekit.propagation.integration.FieldStateMapper;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldFixedNumberInterpolationGrid;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldInterpolationGrid;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldMaxGapInterpolationGrid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldArrayDictionary;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;

/**
 * This class propagates {@link org.orekit.orbits.FieldOrbit orbits} using the DSST theory.
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
 * <li>the initial spacecraft state ({@link #setInitialState(FieldSpacecraftState)})</li>
 * <li>the various force models ({@link #addForceModel(DSSTForceModel)},
 * {@link #removeForceModels()})</li>
 * <li>the discrete events that should be triggered during propagation (
 * {@link #addEventDetector(org.orekit.propagation.events.FieldEventDetector)},
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
 * <li>the {@link org.orekit.orbits.FieldEquinoctialOrbit equinoctial orbit parameters}
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
 * @see FieldSpacecraftState
 * @see DSSTForceModel
 * @author Romain Di Costanzo
 * @author Pascal Parraud
 * @since 10.0
 * @param <T> type of the field elements
 */
public class FieldDSSTPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractIntegratedPropagator<T>  {

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

    /** Number of grid points per integration step to be used in interpolation of short periodics coefficients.*/
    private static final int INTERPOLATION_POINTS_PER_STEP = 3;

    /** Default value for epsilon. */
    private static final double EPSILON_DEFAULT = 1.0e-13;

    /** Default value for maxIterations. */
    private static final int MAX_ITERATIONS_DEFAULT = 200;

    /** Flag specifying whether the initial orbital state is given with osculating elements. */
    private boolean initialIsOsculating;

    /** Field used by this class.*/
    private final Field<T> field;

    /** Force models used to compute short periodic terms. */
    private final transient List<DSSTForceModel> forceModels;

    /** State mapper holding the force models. */
    private FieldMeanPlusShortPeriodicMapper mapper;

    /** Generator for the interpolation grid. */
    private FieldInterpolationGrid<T> interpolationgrid;

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
     *  @param field field used by default
     *  @param integrator numerical integrator to use for propagation.
     *  @param propagationType type of orbit to output (mean or osculating).
     * @see #FieldDSSTPropagator(Field, FieldODEIntegrator, PropagationType,
     * AttitudeProvider)
     */
    @DefaultDataContext
    public FieldDSSTPropagator(final Field<T> field, final FieldODEIntegrator<T> integrator, final PropagationType propagationType) {
        this(field, integrator, propagationType,
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Create a new instance of DSSTPropagator.
     *  <p>
     *  After creation, there are no perturbing forces at all.
     *  This means that if {@link #addForceModel addForceModel}
     *  is not called after creation, the integrated orbit will
     *  follow a Keplerian evolution only.
     *  </p>
     * @param field field used by default
     *  @param integrator numerical integrator to use for propagation.
     * @param propagationType type of orbit to output (mean or osculating).
     * @param attitudeProvider attitude law to use.
     * @since 10.1
     */
    public FieldDSSTPropagator(final Field<T> field,
                               final FieldODEIntegrator<T> integrator,
                               final PropagationType propagationType,
                               final AttitudeProvider attitudeProvider) {
        super(field, integrator, propagationType);
        this.field  = field;
        forceModels = new ArrayList<DSSTForceModel>();
        initMapper(field);
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
     *  @param field fied used by default
     *  @param integrator numerical integrator to use for propagation.
     * @see #FieldDSSTPropagator(Field, FieldODEIntegrator, AttitudeProvider)
     */
    @DefaultDataContext
    public FieldDSSTPropagator(final Field<T> field, final FieldODEIntegrator<T> integrator) {
        this(field, integrator,
                Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Create a new instance of DSSTPropagator.
     *  <p>
     *  After creation, there are no perturbing forces at all.
     *  This means that if {@link #addForceModel addForceModel}
     *  is not called after creation, the integrated orbit will
     *  follow a Keplerian evolution only. Only the mean orbits
     *  will be generated.
     *  </p>
     * @param field fied used by default
     *  @param integrator numerical integrator to use for propagation.
     * @param attitudeProvider attitude law to use.
     * @since 10.1
     */
    public FieldDSSTPropagator(final Field<T> field,
                               final FieldODEIntegrator<T> integrator,
                               final AttitudeProvider attitudeProvider) {
        super(field, integrator, PropagationType.MEAN);
        this.field  = field;
        forceModels = new ArrayList<DSSTForceModel>();
        initMapper(field);
        // DSST uses only equinoctial orbits and mean longitude argument
        setOrbitType(OrbitType.EQUINOCTIAL);
        setPositionAngleType(PositionAngleType.MEAN);
        setAttitudeProvider(attitudeProvider);
        setInterpolationGridToFixedNumberOfPoints(INTERPOLATION_POINTS_PER_STEP);
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
    public void setMu(final T mu) {
        addForceModel(new DSSTNewtonianAttraction(mu.getReal()));
    }

    /** Set the central attraction coefficient μ only in upper class.
     * @param mu central attraction coefficient (m³/s²)
     */
    private void superSetMu(final T mu) {
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
    public void setInitialState(final FieldSpacecraftState<T> initialState) {
        setInitialState(initialState, PropagationType.OSCULATING);
    }

    /** Set the initial state.
     *  @param initialState initial state
     *  @param stateType defined if the orbital state is defined with osculating or mean elements
     */
    public void setInitialState(final FieldSpacecraftState<T> initialState,
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
    public void resetInitialState(final FieldSpacecraftState<T> state) {
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
     * nor {@link #setInterpolationGridToMaxTimeGap(CalculusFieldElement)} has been called,
     * by default the propagator is set as to 3 interpolations points per step.
     * </p>
     * @param interpolationPoints number of interpolation points at
     * each integration step
     * @see #setInterpolationGridToMaxTimeGap(CalculusFieldElement)
     * @since 7.1
     */
    public void setInterpolationGridToFixedNumberOfPoints(final int interpolationPoints) {
        interpolationgrid = new FieldFixedNumberInterpolationGrid<>(field, interpolationPoints);
    }

    /** Set the interpolation grid generator.
     * <p>
     * The generator will create an interpolation grid with a maximum
     * time gap between interpolation points.
     * </p>
     * <p>
     * If neither {@link #setInterpolationGridToFixedNumberOfPoints(int)}
     * nor {@link #setInterpolationGridToMaxTimeGap(CalculusFieldElement)} has been called,
     * by default the propagator is set as to 3 interpolations points per step.
     * </p>
     * @param maxGap maximum time gap between interpolation points (seconds)
     * @see #setInterpolationGridToFixedNumberOfPoints(int)
     * @since 7.1
     */
    public void setInterpolationGridToMaxTimeGap(final T maxGap) {
        interpolationgrid = new FieldMaxGapInterpolationGrid<>(field, maxGap);
    }

    /** Add a force model to the global perturbation model.
     *  <p>
     *  If this method is not called at all,
     *  the integrated orbit will follow a Keplerian evolution only.
     *  </p>
     *  @param force perturbing {@link DSSTForceModel force} to add
     *  @see #removeForceModels()
     *  @see #setMu(CalculusFieldElement)
     */
    public void addForceModel(final DSSTForceModel force) {

        if (force instanceof DSSTNewtonianAttraction) {
            // we want to add the central attraction force model

            try {
                // ensure we are notified of any mu change
                force.getParametersDrivers().get(0).addObserver(new ParameterObserver() {
                    /** {@inheritDoc} */
                    @Override
                    public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
                        // mu PDriver should have only 1 span
                        superSetMu(field.getZero().add(driver.getValue()));
                    }
                    /** {@inheritDoc} */
                    @Override
                    public void valueSpanMapChanged(final TimeSpanMap<Double> previousValue, final ParameterDriver driver) {
                        // mu PDriver should have only 1 span
                        superSetMu(field.getZero().add(driver.getValue()));
                    }
                });
            } catch (OrekitException oe) {
                // this should never happen
                throw new OrekitInternalError(oe);
            }

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
     * @see #setMu(CalculusFieldElement)
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
     * @param <T> type of the elements
     * @return osculating state in a DSST sense
     */
    @SuppressWarnings("unchecked")
    public static <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> computeOsculatingState(final FieldSpacecraftState<T> mean,
                                                                                                 final AttitudeProvider attitudeProvider,
                                                                                                 final Collection<DSSTForceModel> forces) {

        //Create the auxiliary object
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(mean.getOrbit(), I);

        // Set the force models
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();
        for (final DSSTForceModel force : forces) {
            force.registerAttitudeProvider(attitudeProvider);
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING, force.getParameters(mean.getDate().getField(), mean.getDate())));
            force.updateShortPeriodTerms(force.getParametersAllValues(mean.getDate().getField()), mean);
        }

        final FieldEquinoctialOrbit<T> osculatingOrbit = computeOsculatingOrbit(mean, shortPeriodTerms);

        return new FieldSpacecraftState<>(osculatingOrbit, mean.getAttitude(), mean.getMass(),
                                          mean.getAdditionalStatesValues());

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
     * @param forceModel Forces to take into account
     * @param <T> type of the elements
     * @return mean state in a DSST sense
     */
    public static <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> computeMeanState(final FieldSpacecraftState<T> osculating,
                                                                                           final AttitudeProvider attitudeProvider,
                                                                                           final Collection<DSSTForceModel> forceModel) {
        return computeMeanState(osculating, attitudeProvider, forceModel, EPSILON_DEFAULT, MAX_ITERATIONS_DEFAULT);
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
     * @param forceModel Forces to take into account
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @return mean state in a DSST sense
     * @param <T> type of the elements
     * @since 10.1
     */
    public static <T extends CalculusFieldElement<T>> FieldSpacecraftState<T> computeMeanState(final FieldSpacecraftState<T> osculating,
                                                                                           final AttitudeProvider attitudeProvider,
                                                                                           final Collection<DSSTForceModel> forceModel,
                                                                                           final double epsilon,
                                                                                           final int maxIterations) {
        final FieldOrbit<T> meanOrbit = computeMeanOrbit(osculating, attitudeProvider, forceModel, epsilon, maxIterations);
        return new FieldSpacecraftState<>(meanOrbit, osculating.getAttitude(), osculating.getMass(),
                                          osculating.getAdditionalStatesValues());
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
    @SuppressWarnings("unchecked")
    @Override
    protected void beforeIntegration(final FieldSpacecraftState<T> initialState,
                                     final FieldAbsoluteDate<T> tEnd) {

        // check if only mean elements must be used
        final PropagationType type = isMeanOrbit();

        // compute common auxiliary elements
        final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(initialState.getOrbit(), I);

        // initialize all perturbing forces
        final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();
        for (final DSSTForceModel force : forceModels) {
            shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, type, force.getParameters(field, initialState.getDate())));
        }
        mapper.setShortPeriodTerms(shortPeriodTerms);

        // if required, insert the special short periodics step handler
        if (type == PropagationType.OSCULATING) {
            final FieldShortPeriodicsHandler spHandler = new FieldShortPeriodicsHandler(forceModels);
            // Compute short periodic coefficients for this point
            for (DSSTForceModel forceModel : forceModels) {
                forceModel.updateShortPeriodTerms(forceModel.getParametersAllValues(field), initialState);

            }
            final Collection<FieldODEStepHandler<T>> stepHandlers = new ArrayList<FieldODEStepHandler<T>>();
            stepHandlers.add(spHandler);
            final FieldODEIntegrator<T> integrator = getIntegrator();
            final Collection<FieldODEStepHandler<T>> existing = integrator.getStepHandlers();
            stepHandlers.addAll(existing);

            integrator.clearStepHandlers();

            // add back the existing handlers after the short periodics one
            for (final FieldODEStepHandler<T> sp : stepHandlers) {
                integrator.addStepHandler(sp);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void afterIntegration() {
        // remove the special short periodics step handler if added before
        if (isMeanOrbit() ==  PropagationType.OSCULATING) {
            final List<FieldODEStepHandler<T>> preserved = new ArrayList<FieldODEStepHandler<T>>();
            final FieldODEIntegrator<T> integrator = getIntegrator();

            // clear the list
            integrator.clearStepHandlers();

            // add back the step handlers that were important for the user
            for (final FieldODEStepHandler<T> sp : preserved) {
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
     * @param forceModel force models
     * @param epsilon convergence threshold for mean parameters conversion
     * @param maxIterations maximum iterations for mean parameters conversion
     * @param <T> type of the elements
     * @return mean state
     * @since 10.1
     */
    @SuppressWarnings("unchecked")
    private static <T extends CalculusFieldElement<T>> FieldOrbit<T> computeMeanOrbit(final FieldSpacecraftState<T> osculating, final AttitudeProvider attitudeProvider, final Collection<DSSTForceModel> forceModel,
                                                                                  final double epsilon, final int maxIterations) {

        // zero
        final T zero = osculating.getDate().getField().getZero();

        // rough initialization of the mean parameters
        FieldEquinoctialOrbit<T> meanOrbit = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(osculating.getOrbit());

        // threshold for each parameter
        final T epsilonT   = zero.add(epsilon);
        final T thresholdA = epsilonT.multiply(FastMath.abs(meanOrbit.getA()).add(1.));
        final T thresholdE = epsilonT.multiply(meanOrbit.getE().add(1.));
        final T thresholdI = epsilonT.multiply(meanOrbit.getI().add(1.));
        final T thresholdL = epsilonT.multiply(zero.getPi());

        // ensure all Gaussian force models can rely on attitude
        for (final DSSTForceModel force : forceModel) {
            force.registerAttitudeProvider(attitudeProvider);
        }

        int i = 0;
        while (i++ < maxIterations) {

            final FieldSpacecraftState<T> meanState = new FieldSpacecraftState<>(meanOrbit, osculating.getAttitude(), osculating.getMass());

            //Create the auxiliary object
            final FieldAuxiliaryElements<T> aux = new FieldAuxiliaryElements<>(meanOrbit, I);

            // Set the force models
            final List<FieldShortPeriodTerms<T>> shortPeriodTerms = new ArrayList<FieldShortPeriodTerms<T>>();
            for (final DSSTForceModel force : forceModel) {
                shortPeriodTerms.addAll(force.initializeShortPeriodTerms(aux, PropagationType.OSCULATING,
                                 force.getParameters(osculating.getDate().getField(), osculating.getDate())));
                force.updateShortPeriodTerms(force.getParametersAllValues(osculating.getDate().getField()), meanState);
            }

            // recompute the osculating parameters from the current mean parameters
            final FieldEquinoctialOrbit<T> rebuilt = computeOsculatingOrbit(meanState, shortPeriodTerms);

            // adapted parameters residuals
            final T deltaA  = osculating.getA().subtract(rebuilt.getA());
            final T deltaEx = osculating.getEquinoctialEx().subtract(rebuilt.getEquinoctialEx());
            final T deltaEy = osculating.getEquinoctialEy().subtract(rebuilt.getEquinoctialEy());
            final T deltaHx = osculating.getHx().subtract(rebuilt.getHx());
            final T deltaHy = osculating.getHy().subtract(rebuilt.getHy());
            final T deltaLv = MathUtils.normalizeAngle(osculating.getLv().subtract(rebuilt.getLv()), zero);

            // check convergence
            if (FastMath.abs(deltaA).getReal()  < thresholdA.getReal() &&
                FastMath.abs(deltaEx).getReal() < thresholdE.getReal() &&
                FastMath.abs(deltaEy).getReal() < thresholdE.getReal() &&
                FastMath.abs(deltaHx).getReal() < thresholdI.getReal() &&
                FastMath.abs(deltaHy).getReal() < thresholdI.getReal() &&
                FastMath.abs(deltaLv).getReal() < thresholdL.getReal()) {
                return meanOrbit;
            }

            // update mean parameters
            meanOrbit = new FieldEquinoctialOrbit<>(meanOrbit.getA().add(deltaA),
                                                    meanOrbit.getEquinoctialEx().add(deltaEx),
                                                    meanOrbit.getEquinoctialEy().add(deltaEy),
                                                    meanOrbit.getHx().add(deltaHx),
                                                    meanOrbit.getHy().add(deltaHy),
                                                    meanOrbit.getLv().add(deltaLv),
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
     * @param <T> type of the elements
     * @return osculating state
     */
    private static <T extends CalculusFieldElement<T>> FieldEquinoctialOrbit<T> computeOsculatingOrbit(final FieldSpacecraftState<T> meanState,
                                                                                                   final List<FieldShortPeriodTerms<T>> shortPeriodTerms) {

        final T[] mean = MathArrays.buildArray(meanState.getDate().getField(), 6);
        final T[] meanDot = MathArrays.buildArray(meanState.getDate().getField(), 6);
        OrbitType.EQUINOCTIAL.mapOrbitToArray(meanState.getOrbit(), PositionAngleType.MEAN, mean, meanDot);
        final T[] y = mean.clone();
        for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
            final T[] shortPeriodic = spt.value(meanState.getOrbit());
            for (int i = 0; i < shortPeriodic.length; i++) {
                y[i] = y[i].add(shortPeriodic[i]);
            }
        }
        return (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.mapArrayToOrbit(y, meanDot,
                                                                                PositionAngleType.MEAN, meanState.getDate(),
                                                                                meanState.getMu(), meanState.getFrame());
    }

    /** {@inheritDoc} */
    @Override
    protected FieldSpacecraftState<T> getInitialIntegrationState() {
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
    protected FieldStateMapper<T> createMapper(final FieldAbsoluteDate<T> referenceDate, final T mu,
                                               final OrbitType ignoredOrbitType, final PositionAngleType ignoredPositionAngleType,
                                               final AttitudeProvider attitudeProvider, final Frame frame) {

        // create a mapper with the common settings provided as arguments
        final FieldMeanPlusShortPeriodicMapper newMapper =
                new FieldMeanPlusShortPeriodicMapper(referenceDate, mu, attitudeProvider, frame);

        // copy the specific settings from the existing mapper
        if (mapper != null) {
            newMapper.setSatelliteRevolution(mapper.getSatelliteRevolution());
            newMapper.setSelectedCoefficients(mapper.getSelectedCoefficients());
            newMapper.setShortPeriodTerms(mapper.getShortPeriodTerms());
        }

        mapper = newMapper;
        return mapper;

    }

    /** Internal mapper using mean parameters plus short periodic terms. */
    private class FieldMeanPlusShortPeriodicMapper extends FieldStateMapper<T> {

        /** Short periodic coefficients that must be stored as additional states. */
        private Set<String>                selectedCoefficients;

        /** Number of satellite revolutions in the averaging interval. */
        private int                        satelliteRevolution;

        /** Short period terms. */
        private List<FieldShortPeriodTerms<T>>     shortPeriodTerms;

        /** Simple constructor.
         * @param referenceDate reference date
         * @param mu central attraction coefficient (m³/s²)
         * @param attitudeProvider attitude provider
         * @param frame inertial frame
         */
        FieldMeanPlusShortPeriodicMapper(final FieldAbsoluteDate<T> referenceDate, final T mu,
                                         final AttitudeProvider attitudeProvider, final Frame frame) {

            super(referenceDate, mu, OrbitType.EQUINOCTIAL, PositionAngleType.MEAN, attitudeProvider, frame);

            this.selectedCoefficients = null;

            // Default averaging period for conversion from osculating to mean elements
            this.satelliteRevolution = 2;

            this.shortPeriodTerms    = Collections.emptyList();

        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> mapArrayToState(final FieldAbsoluteDate<T> date,
                                                       final T[] y, final T[] yDot,
                                                       final PropagationType type) {

            // add short periodic variations to mean elements to get osculating elements
            // (the loop may not be performed if there are no force models and in the
            //  case we want to remain in mean parameters only)
            final T[] elements = y.clone();
            final FieldArrayDictionary<T> coefficients;
            switch (type) {
                case MEAN:
                    coefficients = null;
                    break;
                case OSCULATING:
                    final FieldOrbit<T> meanOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(elements, yDot, PositionAngleType.MEAN, date, getMu(), getFrame());
                    coefficients = selectedCoefficients == null ? null : new FieldArrayDictionary<>(date.getField());
                    for (final FieldShortPeriodTerms<T> spt : shortPeriodTerms) {
                        final T[] shortPeriodic = spt.value(meanOrbit);
                        for (int i = 0; i < shortPeriodic.length; i++) {
                            elements[i] = elements[i].add(shortPeriodic[i]);
                        }
                        if (selectedCoefficients != null) {
                            coefficients.putAll(spt.getCoefficients(date, selectedCoefficients));
                        }
                    }
                    break;
                default:
                    throw new OrekitInternalError(null);
            }

            final T mass = elements[6];
            if (mass.getReal() <= 0.0) {
                throw new OrekitException(OrekitMessages.NOT_POSITIVE_SPACECRAFT_MASS, mass);
            }

            final FieldOrbit<T> orbit       = OrbitType.EQUINOCTIAL.mapArrayToOrbit(elements, yDot, PositionAngleType.MEAN, date, getMu(), getFrame());
            final FieldAttitude<T> attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

            if (coefficients == null) {
                return new FieldSpacecraftState<>(orbit, attitude, mass);
            } else {
                return new FieldSpacecraftState<>(orbit, attitude, mass, coefficients);
            }

        }

        /** {@inheritDoc} */
        @Override
        public void mapStateToArray(final FieldSpacecraftState<T> state, final T[] y, final T[] yDot) {

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
        public void setShortPeriodTerms(final List<FieldShortPeriodTerms<T>> shortPeriodTerms) {
            this.shortPeriodTerms = shortPeriodTerms;
        }

        /** Get the short period terms.
         * @return shortPeriodTerms short period terms
         * @since 7.1
         */
        public List<FieldShortPeriodTerms<T>> getShortPeriodTerms() {
            return shortPeriodTerms;
        }

    }

    /** {@inheritDoc} */
    @Override
    protected MainStateEquations<T> getMainStateEquations(final FieldODEIntegrator<T> integrator) {
        return new Main(integrator);
    }

    /** Internal class for mean parameters integration. */
    private class Main implements MainStateEquations<T> {

        /** Derivatives array. */
        private final T[] yDot;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        Main(final FieldODEIntegrator<T> integrator) {
            yDot = MathArrays.buildArray(field, 7);

            // Setup event detectors for each force model
            forceModels.forEach(dsstForceModel -> dsstForceModel.getFieldEventDetectors(field).
                                forEach(eventDetector -> setUpEventDetector(integrator, eventDetector)));
        }

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
            forceModels.forEach(fm -> fm.init(initialState, target));
        }

        /** {@inheritDoc} */
        @Override
        public T[] computeDerivatives(final FieldSpacecraftState<T> state) {

            final T zero = state.getDate().getField().getZero();
            Arrays.fill(yDot, zero);

            // compute common auxiliary elements
            final FieldAuxiliaryElements<T> auxiliaryElements = new FieldAuxiliaryElements<>(state.getOrbit(), I);

            // compute the contributions of all perturbing forces
            for (final DSSTForceModel forceModel : forceModels) {
                final T[] daidt = elementRates(forceModel, state, auxiliaryElements, forceModel.getParametersAllValues(field));
                for (int i = 0; i < daidt.length; i++) {
                    yDot[i] = yDot[i].add(daidt[i]);
                }
            }

            return yDot.clone();
        }

        /** This method allows to compute the mean equinoctial elements rates da<sub>i</sub> / dt
         *  for a specific force model.
         *  @param forceModel force to take into account
         *  @param state current state
         *  @param auxiliaryElements auxiliary elements related to the current orbit
         *  @param parameters force model parameters (all span values for each parameters)
     *  the extract parameter method {@link #extractParameters(double[], AbsoluteDate)} is called in
     *  the method to select the right parameter.
         *  @return the mean equinoctial elements rates da<sub>i</sub> / dt
         */
        private T[] elementRates(final DSSTForceModel forceModel,
                                 final FieldSpacecraftState<T> state,
                                 final FieldAuxiliaryElements<T> auxiliaryElements,
                                 final T[] parameters) {
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
     * @param <T> elements type
     * @param dP user specified position error (m)
     * @param orbit reference orbit
     * @return a two rows array, row 0 being the absolute tolerance error
     *                       and row 1 being the relative tolerance error
     */
    public static <T extends CalculusFieldElement<T>> double[][] tolerances(final T dP, final FieldOrbit<T> orbit) {
        return FieldNumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
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
     * @param <T> elements type
     * @param dP user specified position error (m)
     * @param dV user specified velocity error (m/s)
     * @param orbit reference orbit
     * @return a two rows array, row 0 being the absolute tolerance error
     *                       and row 1 being the relative tolerance error
     * @since 10.3
     */
    public static <T extends CalculusFieldElement<T>> double[][] tolerances(final T dP, final T dV,
                                                                        final FieldOrbit<T> orbit) {
        return FieldNumericalPropagator.tolerances(dP, dV, orbit, OrbitType.EQUINOCTIAL);
    }

    /** Step handler used to compute the parameters for the short periodic contributions.
     * @author Lucian Barbulescu
     */
    private class FieldShortPeriodicsHandler implements FieldODEStepHandler<T> {

        /** Force models used to compute short periodic terms. */
        private final List<DSSTForceModel> forceModels;

        /** Constructor.
         * @param forceModels force models
         */
        FieldShortPeriodicsHandler(final List<DSSTForceModel> forceModels) {
            this.forceModels = forceModels;
        }

        /** {@inheritDoc} */
        @SuppressWarnings("unchecked")
        @Override
        public void handleStep(final FieldODEStateInterpolator<T> interpolator) {

            // Get the grid points to compute
            final T[] interpolationPoints =
                            interpolationgrid.getGridPoints(interpolator.getPreviousState().getTime(),
                                                            interpolator.getCurrentState().getTime());

            final FieldSpacecraftState<T>[] meanStates = new FieldSpacecraftState[interpolationPoints.length];
            for (int i = 0; i < interpolationPoints.length; ++i) {

                // Build the mean state interpolated at grid point
                final T time = interpolationPoints[i];
                final FieldODEStateAndDerivative<T> sd = interpolator.getInterpolatedState(time);
                meanStates[i] = mapper.mapArrayToState(time,
                                                       sd.getPrimaryState(),
                                                       sd.getPrimaryDerivative(),
                                                       PropagationType.MEAN);

            }

            // Compute short periodic coefficients for this step
            for (DSSTForceModel forceModel : forceModels) {
                forceModel.updateShortPeriodTerms(forceModel.getParametersAllValues(field), meanStates);
            }

        }
    }

}
