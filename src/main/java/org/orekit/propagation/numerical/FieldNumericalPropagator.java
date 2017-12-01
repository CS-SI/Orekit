/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.propagation.numerical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.FieldAbstractIntegratedPropagator;
import org.orekit.propagation.integration.FieldStateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;
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
 *   <li>the central attraction coefficient ({@link #setMu(double)})</li>
 *   <li>the various force models ({@link #addForceModel(ForceModel)},
 *   {@link #removeForceModels()})</li>
 *   <li>the {@link OrbitType type} of orbital parameters to be used for propagation
 *   ({@link #setOrbitType(OrbitType)}),
 *   <li>the {@link PositionAngle type} of position angle to be used in orbital parameters
 *   to be used for propagation where it is relevant ({@link
 *   #setPositionAngleType(PositionAngle)}),
 *   <li>whether {@link org.orekit.propagation.integration.FieldAdditionalEquations additional equations}
 *   should be propagated along with orbital state
 *   ({@link #addAdditionalEquations(org.orekit.propagation.integration.FieldAdditionalEquations)}),
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addEventDetector(FieldEventDetector)},
 *   {@link #clearEventsDetectors()})</li>
 *   <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 *   {@link #setMasterMode(RealFieldElement, org.orekit.propagation.sampling.FieldOrekitFixedStepHandler)},
 *   {@link #setMasterMode(org.orekit.propagation.sampling.FieldOrekitStepHandler)},
 *   {@link #setEphemerisMode()}, {@link #getGeneratedEphemeris()})</li>
 * </ul>
 * <p>From these configuration parameters, only the initial state is mandatory. The default
 * propagation settings are in {@link OrbitType#EQUINOCTIAL equinoctial} parameters with
 * {@link PositionAngle#TRUE true} longitude argument. If the central attraction coefficient
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
 * </p>
 * <p>The following code snippet shows a typical setting for Low Earth Orbit propagation in
 * equinoctial parameters and true longitude argument:</p>
 * <pre>
 * final T          zero      = field.getZero();
 * final T          dP        = zero.add(0.001);
 * final T          minStep   = zero.add(0.001);
 * final T          maxStep   = zero.add(500);
 * final T          initStep  = zero.add(60);
 * final double[][] tolerance = FieldNumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
 * AdaptiveStepsizeFieldIntegrator<T> integrator = new DormandPrince853FieldIntegrator<>(field, minStep, maxStep, tolerance[0], tolerance[1]);
 * integrator.setInitialStepSize(initStep);
 * propagator = new FieldNumericalPropagator<>(field, integrator);
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
 */
public class FieldNumericalPropagator<T extends RealFieldElement<T>> extends FieldAbstractIntegratedPropagator<T> {

    /** Force models used during the extrapolation of the FieldOrbit<T>, without Jacobians. */
    private final List<ForceModel> forceModels;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a Keplerian
     * evolution only. The defaults are {@link OrbitType#EQUINOCTIAL}
     * for {@link #setOrbitType(OrbitType) propagation
     * orbit type} and {@link PositionAngle#TRUE} for {@link
     * #setPositionAngleType(PositionAngle) position angle type}.
     * @param integrator numerical integrator to use for propagation.
     * @param field Field used by default
     */
    public FieldNumericalPropagator(final Field<T> field, final FieldODEIntegrator<T> integrator) {
        super(field, integrator, true);
        forceModels = new ArrayList<ForceModel>();
        initMapper();
        setAttitudeProvider(DEFAULT_LAW);
        setMu(Double.NaN);
        setSlaveMode();
        setOrbitType(OrbitType.EQUINOCTIAL);
        setPositionAngleType(PositionAngle.TRUE);
    }

     /** Set the central attraction coefficient μ.
     * @param mu central attraction coefficient (m³/s²)
     * @see #addForceModel(ForceModel)
     */
    public void setMu(final double mu) {
        addForceModel(new NewtonianAttraction(mu));
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

    /** Add a force model to the global perturbation model.
     * <p>If this method is not called at all, the integrated orbit will follow
     * a Keplerian evolution only.</p>
     * @param model perturbing {@link ForceModel} to add
     * @see #removeForceModels()
     * @see #setMu(double)
     */
    public void addForceModel(final ForceModel model) {

        if (model instanceof NewtonianAttraction) {
            // we want to add the central attraction force model

            try {
                // ensure we are notified of any mu change
                model.getParametersDrivers()[0].addObserver(new ParameterObserver() {
                    /** {@inheritDoc} */
                    @Override
                    public void valueChanged(final double previousValue, final ParameterDriver driver) {
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
     * @see #setMu(double)
     * @since 9.1
     */
    public List<ForceModel> getAllForceModels() {
        return Collections.unmodifiableList(forceModels);
    }

    /** Get perturbing force models list.
     * @return list of perturbing force models
     * @deprecated as of 9.1, this method is deprecated, the perturbing
     * force models are retrieved together with the Newtonian attraction
     * by calling {@link #getAllForceModels()}
     */
    @Deprecated
    public List<ForceModel> getForceModels() {
        return hasNewtonianAttraction() ? forceModels.subList(0, forceModels.size() - 1) : forceModels;
    }

    /** Get the Newtonian attraction from the central body force model.
     * @return Newtonian attraction force model
     * @deprecated as of 9.1, this method is deprecated, the Newtonian
     * attraction force model (if any) is the last in the {@link #getAllForceModels()}
     */
    @Deprecated
    public NewtonianAttraction getNewtonianAttractionForceModel() {
        final int last = forceModels.size() - 1;
        if (last >= 0 && forceModels.get(last) instanceof NewtonianAttraction) {
            return (NewtonianAttraction) forceModels.get(last);
        } else {
            return null;
        }
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
    public void setPositionAngleType(final PositionAngle positionAngleType) {
        super.setPositionAngleType(positionAngleType);
    }

    /** Get propagation parameter type.
     * @return angle type to use for propagation
     */
    public PositionAngle getPositionAngleType() {
        return super.getPositionAngleType();
    }

    /** Set the initial state.
     * @param initialState initial state
     * @exception OrekitException if initial state cannot be set
     */
    public void setInitialState(final FieldSpacecraftState<T> initialState) throws OrekitException {
        resetInitialState(initialState);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final FieldSpacecraftState<T> state) throws OrekitException {
        super.resetInitialState(state);
        if (!hasNewtonianAttraction()) {
            setMu(state.getMu());
        }
        setStartDate(state.getDate());
    }

    /** {@inheritDoc} */
    public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** {@inheritDoc} */
    protected FieldStateMapper<T> createMapper(final FieldAbsoluteDate<T> referenceDate, final double mu,
                                       final OrbitType orbitType, final PositionAngle positionAngleType,
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
        FieldOsculatingMapper(final FieldAbsoluteDate<T> referenceDate, final double mu,
                              final OrbitType orbitType, final PositionAngle positionAngleType,
                              final AttitudeProvider attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        /** {@inheritDoc} */
        public FieldSpacecraftState<T> mapArrayToState(final FieldAbsoluteDate<T> date, final T[] y, final T[] yDot,
                                                       final boolean meanOnly)
            throws OrekitException {
            // the parameter meanOnly is ignored for the Numerical Propagator

            final T mass = y[6];
            if (mass.getReal() <= 0.0) {
                throw new OrekitException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE, mass);
            }
            final FieldOrbit<T> orbit       = super.getOrbitType().mapArrayToOrbit(y, yDot, super.getPositionAngleType(), date, getMu(), getFrame());
            final FieldAttitude<T> attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());
            return new FieldSpacecraftState<>(orbit, attitude, mass);
        }

        /** {@inheritDoc} */
        public void mapStateToArray(final FieldSpacecraftState<T> state, final T[] y, final T[] yDot) {
            super.getOrbitType().mapOrbitToArray(state.getOrbit(), super.getPositionAngleType(), y, yDot);
            y[6] = state.getMass();
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

        /** Current orbit. */
        private FieldOrbit<T> orbit;

        /** Jacobian of the orbital parameters with respect to the Cartesian parameters. */
        private T[][] jacobian;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        Main(final FieldODEIntegrator<T> integrator) {

            this.yDot     = MathArrays.buildArray(getField(),  7);
            this.jacobian = MathArrays.buildArray(getField(),  6, 6);
            for (final ForceModel forceModel : forceModels) {
                forceModel.getFieldEventsDetectors(getField()).forEach(detector -> setUpEventDetector(integrator, detector));
            }

        }

        /** {@inheritDoc} */
        @Override
        public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target)
            throws OrekitException {
            final SpacecraftState stateD  = initialState.toSpacecraftState();
            final AbsoluteDate    targetD = target.toAbsoluteDate();
            for (final ForceModel forceModel : forceModels) {
                forceModel.init(stateD, targetD);
            }
        }

        /** {@inheritDoc} */
        @Override
        public T[] computeDerivatives(final FieldSpacecraftState<T> state) throws OrekitException {
            final T zero = state.getA().getField().getZero();
            orbit = state.getOrbit();
            Arrays.fill(yDot, zero);
            orbit.getJacobianWrtCartesian(getPositionAngleType(), jacobian);

            // compute the contributions of all perturbing forces,
            // using the Kepler contribution at the end since
            // NewtonianAttraction is always the last instance in the list
            for (final ForceModel forceModel : forceModels) {
                forceModel.addContribution(state, this);
            }

            return yDot.clone();

        }

        /** {@inheritDoc} */
        @Override
        public void addKeplerContribution(final double mu) {
            orbit.addKeplerContribution(getPositionAngleType(), mu, yDot);
        }

        /** {@inheritDoc} */
        @Override
        public void addNonKeplerianAcceleration(final FieldVector3D<T> gamma)
            throws OrekitException {
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
     * V² r |dV| = mu |dr|
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
     * (it may be different from {@code orbit.getType()})
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @exception OrekitException if Jacobian is singular
     * @param <T> elements type
     */
    public static <T extends RealFieldElement<T>> double[][] tolerances(final T dP, final FieldOrbit<T> orbit, final OrbitType type)
        throws OrekitException {

        // estimate the scalar velocity error
        final FieldPVCoordinates<T> pv = orbit.getPVCoordinates();
        final T r2 = pv.getPosition().getNormSq();
        final T v  = pv.getVelocity().getNorm();
        final T dV = dP.multiply(orbit.getMu()).divide(v.multiply(r2));

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
            converted.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);

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

        Arrays.fill(relTol, dP.divide(r2.sqrt()).getReal());

        return new double[][]{
            absTol, relTol
        };

    }

}

