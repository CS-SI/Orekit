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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.util.MathArrays;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.FieldAbstractIntegratedPropagator;
import org.orekit.propagation.integration.FieldStateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** This class propagates {@link org.orekit.orbits.FieldOrbit orbits} using
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
 *   <li>the initial spacecraft state ({@link #setInitialState(FieldSpacecraftState)})</li>
 *   <li>the central attraction coefficient ({@link #setMu(CalculusFieldElement)})</li>
 *   <li>the various force models ({@link #addForceModel(ForceModel)},
 *   {@link #removeForceModels()})</li>
 *   <li>the {@link OrbitType type} of orbital parameters to be used for propagation
 *   ({@link #setOrbitType(OrbitType)}),
 *   <li>the {@link PositionAngleType type} of position angle to be used in orbital parameters
 *   to be used for propagation where it is relevant ({@link
 *   #setPositionAngleType(PositionAngleType)}),
 *   <li>whether {@link org.orekit.propagation.integration.FieldAdditionalDerivativesProvider additional derivatives providers}
 *   should be propagated along with orbital state
 *   ({@link #addAdditionalDerivativesProvider(org.orekit.propagation.integration.FieldAdditionalDerivativesProvider)}),
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addEventDetector(FieldEventDetector)},
 *   {@link #clearEventsDetectors()})</li>
 *   <li>the binding logic with the rest of the application ({@link #getMultiplexer()})</li>
 * </ul>
 * <p>From these configuration parameters, only the initial state is mandatory. The default
 * propagation settings are in {@link OrbitType#EQUINOCTIAL equinoctial} parameters with
 * {@link PositionAngleType#TRUE true} longitude argument. If the central attraction coefficient
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
 *   <li>the {@link org.orekit.orbits.FieldEquinoctialOrbit equinoctial orbit parameters} (a, e<sub>x</sub>,
 *   e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, λ<sub>M</sub> or λ<sub>E</sub>
 *   or λ<sub>v</sub>) in meters and radians,</li>
 *   <li>the {@link org.orekit.orbits.FieldKeplerianOrbit Keplerian orbit parameters} (a, e, i, ω, Ω,
 *   M or E or v) in meters and radians,</li>
 *   <li>the {@link org.orekit.orbits.FieldCircularOrbit circular orbit parameters} (a, e<sub>x</sub>, e<sub>y</sub>, i,
 *   Ω, α<sub>M</sub> or α<sub>E</sub> or α<sub>v</sub>) in meters
 *   and radians,</li>
 *   <li>the {@link org.orekit.orbits.FieldCartesianOrbit Cartesian orbit parameters} (x, y, z, v<sub>x</sub>,
 *   v<sub>y</sub>, v<sub>z</sub>) in meters and meters per seconds.
 * </ul>
 * The last element is the mass in kilograms.
 * <p>The following code snippet shows a typical setting for Low Earth Orbit propagation in
 * equinoctial parameters and true longitude argument:</p>
 * <pre>
 * final T          zero      = field.getZero();
 * final T          dP        = zero.add(0.001);
 * final T          minStep   = zero.add(0.001);
 * final T          maxStep   = zero.add(500);
 * final T          initStep  = zero.add(60);
 * final double[][] tolerance = FieldNumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
 * AdaptiveStepsizeFieldIntegrator&lt;T&gt; integrator = new DormandPrince853FieldIntegrator&lt;&gt;(field, minStep, maxStep, tolerance[0], tolerance[1]);
 * integrator.setInitialStepSize(initStep);
 * propagator = new FieldNumericalPropagator&lt;&gt;(field, integrator);
 * </pre>
 * <p>By default, at the end of the propagation, the propagator resets the initial state to the final state,
 * thus allowing a new propagation to be started from there without recomputing the part already performed.
 * This behaviour can be chenged by calling {@link #setResetAtEnd(boolean)}.
 * </p>
 * <p>Beware the same instance cannot be used simultaneously by different threads, the class is <em>not</em>
 * thread-safe.</p>

 * @see FieldSpacecraftState
 * @see ForceModel
 * @see org.orekit.propagation.sampling.FieldOrekitStepHandler
 * @see org.orekit.propagation.sampling.FieldOrekitFixedStepHandler
 * @see org.orekit.propagation.integration.FieldIntegratedEphemeris
 * @see FieldTimeDerivativesEquations
 *
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @param <T> type of the field elements
 */
public class FieldNumericalPropagator<T extends CalculusFieldElement<T>> extends FieldAbstractIntegratedPropagator<T> {

    /** Force models used during the extrapolation of the FieldOrbit<T>, without Jacobians. */
    private final List<ForceModel> forceModels;

    /** Field used by this class.*/
    private final Field<T> field;

    /** boolean to ignore or not the creation of a NewtonianAttraction. */
    private boolean ignoreCentralAttraction = false;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a Keplerian
     * evolution only. The defaults are {@link OrbitType#EQUINOCTIAL}
     * for {@link #setOrbitType(OrbitType) propagation
     * orbit type} and {@link PositionAngleType#TRUE} for {@link
     * #setPositionAngleType(PositionAngleType) position angle type}.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param integrator numerical integrator to use for propagation.
     * @param field Field used by default
     * @see #FieldNumericalPropagator(Field, FieldODEIntegrator, AttitudeProvider)
     */
    @DefaultDataContext
    public FieldNumericalPropagator(final Field<T> field, final FieldODEIntegrator<T> integrator) {
        this(field, integrator, Propagator.getDefaultLaw(DataContext.getDefault().getFrames()));
    }

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a Keplerian
     * evolution only. The defaults are {@link OrbitType#EQUINOCTIAL}
     * for {@link #setOrbitType(OrbitType) propagation
     * orbit type} and {@link PositionAngleType#TRUE} for {@link
     * #setPositionAngleType(PositionAngleType) position angle type}.
     * @param field Field used by default
     * @param integrator numerical integrator to use for propagation.
     * @param attitudeProvider attitude law to use.
     * @since 10.1
     */
    public FieldNumericalPropagator(final Field<T> field,
                                    final FieldODEIntegrator<T> integrator,
                                    final AttitudeProvider attitudeProvider) {
        super(field, integrator, PropagationType.OSCULATING);
        this.field = field;
        forceModels = new ArrayList<ForceModel>();
        initMapper(field);
        setAttitudeProvider(attitudeProvider);
        setMu(field.getZero().add(Double.NaN));
        clearStepHandlers();
        setOrbitType(OrbitType.EQUINOCTIAL);
        setPositionAngleType(PositionAngleType.TRUE);
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
      * </p>
     * @param mu central attraction coefficient (m³/s²)
     * @see #addForceModel(ForceModel)
     * @see #getAllForceModels()
     */
    public void setMu(final T mu) {
        if (ignoreCentralAttraction) {
            superSetMu(mu);
        } else {
            addForceModel(new NewtonianAttraction(mu.getReal()));
        }
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
        return last >= 0 && forceModels.get(last) instanceof NewtonianAttraction;
    }

    /** Add a force model to the global perturbation model.
     * <p>If this method is not called at all, the integrated orbit will follow
     * a Keplerian evolution only.</p>
     * @param model perturbing {@link ForceModel} to add
     * @see #removeForceModels()
     * @see #setMu(CalculusFieldElement)
     */
    public void addForceModel(final ForceModel model) {

        if (model instanceof NewtonianAttraction) {
            // we want to add the central attraction force model

            try {
                // ensure we are notified of any mu change
                model.getParametersDrivers().get(0).addObserver(new ParameterObserver() {
                    /** {@inheritDoc} */
                    @Override
                    public void valueChanged(final double previousValue, final ParameterDriver driver, final AbsoluteDate date) {
                        // mu PDriver should have only 1 span
                        superSetMu(field.getZero().add(driver.getValue(date)));
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

    /** Remove all perturbing force models from the global perturbation model.
     * <p>Once all perturbing forces have been removed (and as long as no new force
     * model is added), the integrated orbit will follow a Keplerian evolution
     * only.</p>
     * @see #addForceModel(ForceModel)
     */
    public void removeForceModels() {
        forceModels.clear();
    }

    /** Get all the force models, perturbing forces and Newtonian attraction included.
     * @return list of perturbing force models, with Newtonian attraction being the
     * last one
     * @see #addForceModel(ForceModel)
     * @see #setMu(CalculusFieldElement)
     * @since 9.1
     */
    public List<ForceModel> getAllForceModels() {
        return Collections.unmodifiableList(forceModels);
    }

    /** Set propagation orbit type.
     * @param orbitType orbit type to use for propagation
     */
    public void setOrbitType(final OrbitType orbitType) {
        super.setOrbitType(orbitType);
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation
     */
    public OrbitType getOrbitType() {
        return superGetOrbitType();
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation
     */
    private OrbitType superGetOrbitType() {
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
    public void setPositionAngleType(final PositionAngleType positionAngleType) {
        super.setPositionAngleType(positionAngleType);
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    public PositionAngleType getPositionAngleType() {
        return super.getPositionAngleType();
    }

    /** Set the initial state.
     * @param initialState initial state
     */
    public void setInitialState(final FieldSpacecraftState<T> initialState) {
        resetInitialState(initialState);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state) {
        super.resetInitialState(state);
        if (!hasNewtonianAttraction()) {
            setMu(state.getMu());
        }
        setStartDate(state.getDate());
    }

    /** {@inheritDoc} */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame) {
        return propagate(date).getPVCoordinates(frame);
    }

    /** {@inheritDoc} */
    protected FieldStateMapper<T> createMapper(final FieldAbsoluteDate<T> referenceDate, final T mu,
                                       final OrbitType orbitType, final PositionAngleType positionAngleType,
                                       final AttitudeProvider attitudeProvider, final Frame frame) {
        return new FieldOsculatingMapper(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
    }

    /** Internal mapper using directly osculating parameters. */
    private class FieldOsculatingMapper extends FieldStateMapper<T> {

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
         */
        FieldOsculatingMapper(final FieldAbsoluteDate<T> referenceDate, final T mu,
                              final OrbitType orbitType, final PositionAngleType positionAngleType,
                              final AttitudeProvider attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        /** {@inheritDoc} */
        public FieldSpacecraftState<T> mapArrayToState(final FieldAbsoluteDate<T> date, final T[] y, final T[] yDot,
                                                       final PropagationType type) {
            // the parameter type is ignored for the Numerical Propagator

            final T mass = y[6];
            if (mass.getReal() <= 0.0) {
                throw new OrekitException(OrekitMessages.NOT_POSITIVE_SPACECRAFT_MASS, mass);
            }

            if (superGetOrbitType() == null) {
                // propagation uses absolute position-velocity-acceleration
                final FieldVector3D<T> p = new FieldVector3D<>(y[0],    y[1],    y[2]);
                final FieldVector3D<T> v = new FieldVector3D<>(y[3],    y[4],    y[5]);
                final FieldVector3D<T> a;
                final FieldAbsolutePVCoordinates<T> absPva;
                if (yDot == null) {
                    absPva = new FieldAbsolutePVCoordinates<>(getFrame(), new TimeStampedFieldPVCoordinates<>(date, p, v, FieldVector3D.getZero(date.getField())));
                } else {
                    a = new FieldVector3D<>(yDot[3], yDot[4], yDot[5]);
                    absPva = new FieldAbsolutePVCoordinates<>(getFrame(), new TimeStampedFieldPVCoordinates<>(date, p, v, a));
                }

                final FieldAttitude<T> attitude = getAttitudeProvider().getAttitude(absPva, date, getFrame());
                return new FieldSpacecraftState<>(absPva, attitude, mass);
            } else {
                // propagation uses regular orbits
                final FieldOrbit<T> orbit       = superGetOrbitType().mapArrayToOrbit(y, yDot, super.getPositionAngleType(), date, getMu(), getFrame());
                final FieldAttitude<T> attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());
                return new FieldSpacecraftState<>(orbit, attitude, mass);
            }
        }

        /** {@inheritDoc} */
        public void mapStateToArray(final FieldSpacecraftState<T> state, final T[] y, final T[] yDot) {
            if (superGetOrbitType() == null) {
                // propagation uses absolute position-velocity-acceleration
                final FieldVector3D<T> p = state.getAbsPVA().getPosition();
                final FieldVector3D<T> v = state.getAbsPVA().getVelocity();
                y[0] = p.getX();
                y[1] = p.getY();
                y[2] = p.getZ();
                y[3] = v.getX();
                y[4] = v.getY();
                y[5] = v.getZ();
                y[6] = state.getMass();
            }
            else {
                superGetOrbitType().mapOrbitToArray(state.getOrbit(), super.getPositionAngleType(), y, yDot);
                y[6] = state.getMass();
            }
        }

    }

    /** {@inheritDoc} */
    protected MainStateEquations<T> getMainStateEquations(final FieldODEIntegrator<T> integrator) {
        return new Main(integrator);
    }

    /** Internal class for osculating parameters integration. */
    private class Main implements MainStateEquations<T>, FieldTimeDerivativesEquations<T> {

        /** Derivatives array. */
        private final T[] yDot;

        /** Current state. */
        private FieldSpacecraftState<T> currentState;

        /** Jacobian of the orbital parameters with respect to the Cartesian parameters. */
        private T[][] jacobian;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        Main(final FieldODEIntegrator<T> integrator) {

            this.yDot     = MathArrays.buildArray(getField(),  7);
            this.jacobian = MathArrays.buildArray(getField(),  6, 6);
            for (final ForceModel forceModel : forceModels) {
                forceModel.getFieldEventDetectors(getField()).forEach(detector -> setUpEventDetector(integrator, detector));
            }

            if (superGetOrbitType() == null) {
                // propagation uses absolute position-velocity-acceleration
                // we can set Jacobian once and for all
                for (int i = 0; i < jacobian.length; ++i) {
                    Arrays.fill(jacobian[i], getField().getZero());
                    jacobian[i][i] = getField().getOne();
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
            forceModels.forEach(fm -> fm.init(initialState, target));
        }

        /** {@inheritDoc} */
        @Override
        public T[] computeDerivatives(final FieldSpacecraftState<T> state) {
            final T zero = state.getA().getField().getZero();
            currentState = state;
            Arrays.fill(yDot, zero);
            if (superGetOrbitType() != null) {
                // propagation uses regular orbits
                currentState.getOrbit().getJacobianWrtCartesian(getPositionAngleType(), jacobian);
            }

            // compute the contributions of all perturbing forces,
            // using the Kepler contribution at the end since
            // NewtonianAttraction is always the last instance in the list
            for (final ForceModel forceModel : forceModels) {
                forceModel.addContribution(state, this);
            }

            if (superGetOrbitType() == null) {
                // position derivative is velocity, and was not added above in the force models
                // (it is added when orbit type is non-null because NewtonianAttraction considers it)
                final FieldVector3D<T> velocity = currentState.getPVCoordinates().getVelocity();
                yDot[0] = yDot[0].add(velocity.getX());
                yDot[1] = yDot[1].add(velocity.getY());
                yDot[2] = yDot[2].add(velocity.getZ());
            }

            return yDot.clone();

        }

        /** {@inheritDoc} */
        @Override
        public void addKeplerContribution(final T mu) {
            if (superGetOrbitType() == null) {

                // if mu is neither 0 nor NaN, we want to include Newtonian acceleration
                if (mu.getReal() > 0) {
                    // velocity derivative is Newtonian acceleration
                    final FieldVector3D<T> position = currentState.getPosition();
                    final T r2         = position.getNormSq();
                    final T coeff      = r2.multiply(r2.sqrt()).reciprocal().negate().multiply(mu);
                    yDot[3] = yDot[3].add(coeff.multiply(position.getX()));
                    yDot[4] = yDot[4].add(coeff.multiply(position.getY()));
                    yDot[5] = yDot[5].add(coeff.multiply(position.getZ()));
                }

            } else {
                // propagation uses regular orbits
                currentState.getOrbit().addKeplerContribution(getPositionAngleType(), mu, yDot);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void addNonKeplerianAcceleration(final FieldVector3D<T> gamma) {
            for (int i = 0; i < 6; ++i) {
                final T[] jRow = jacobian[i];
                yDot[i] = yDot[i].add(jRow[3].linearCombination(jRow[3], gamma.getX(),
                                                                jRow[4], gamma.getY(),
                                                                jRow[5], gamma.getZ()));
            }
        }

        /** {@inheritDoc} */
        @Override
        public void addMassDerivative(final T q) {
            if (q.getReal() > 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
            }
            yDot[6] = yDot[6].add(q);
        }

    }

    /** Estimate tolerance vectors for integrators.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V r² |dV| = mu |dr|
     * </pre>
     * So we deduce a scalar velocity error consistent with the position error.
     * From here, we apply orbits Jacobians matrices to get consistent errors
     * on orbital parameters.
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
     * @param <T> elements type
     */
    public static <T extends CalculusFieldElement<T>> double[][] tolerances(final T dP, final FieldOrbit<T> orbit, final OrbitType type) {

        // estimate the scalar velocity error
        final FieldPVCoordinates<T> pv = orbit.getPVCoordinates();
        final T r2 = pv.getPosition().getNormSq();
        final T v  = pv.getVelocity().getNorm();
        final T dV = dP.multiply(orbit.getMu()).divide(v.multiply(r2));

        return tolerances(dP, dV, orbit, type);

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
     * @param <T> elements type
     * @param dP user specified position error
     * @param dV user specified velocity error
     * @param orbit reference orbit
     * @param type propagation type for the meaning of the tolerance vectors elements
     * (it may be different from {@code orbit.getType()})
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @since 10.3
     */
    public static <T extends CalculusFieldElement<T>> double[][] tolerances(final T dP, final T dV,
                                                                        final FieldOrbit<T> orbit, final OrbitType type) {

        final double[] absTol = new double[7];
        final double[] relTol = new double[7];

        // we set the mass tolerance arbitrarily to 1.0e-6 kg, as mass evolves linearly
        // with trust, this often has no influence at all on propagation
        absTol[6] = 1.0e-6;

        if (type == OrbitType.CARTESIAN) {
            absTol[0] = dP.getReal();
            absTol[1] = dP.getReal();
            absTol[2] = dP.getReal();
            absTol[3] = dV.getReal();
            absTol[4] = dV.getReal();
            absTol[5] = dV.getReal();
        } else {

            // convert the orbit to the desired type
            final T[][] jacobian = MathArrays.buildArray(dP.getField(), 6, 6);
            final FieldOrbit<T> converted = type.convertType(orbit);
            converted.getJacobianWrtCartesian(PositionAngleType.TRUE, jacobian);

            for (int i = 0; i < 6; ++i) {
                final  T[] row = jacobian[i];
                absTol[i] =     row[0].abs().multiply(dP).
                            add(row[1].abs().multiply(dP)).
                            add(row[2].abs().multiply(dP)).
                            add(row[3].abs().multiply(dV)).
                            add(row[4].abs().multiply(dV)).
                            add(row[5].abs().multiply(dV)).
                            getReal();
                if (Double.isNaN(absTol[i])) {
                    throw new OrekitException(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, type);
                }
            }

        }

        Arrays.fill(relTol, dP.divide(orbit.getPosition().getNormSq().sqrt()).getReal());

        return new double[][] { absTol, relTol };

    }

}

