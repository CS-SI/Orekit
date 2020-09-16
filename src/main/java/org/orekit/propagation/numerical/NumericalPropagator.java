/* Copyright 2002-2020 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.inertia.InertialForces;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.StateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;
import org.orekit.utils.TimeStampedPVCoordinates;

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
 *   ({@link #setOrbitType(OrbitType)}),
 *   <li>the {@link PositionAngle type} of position angle to be used in orbital parameters
 *   to be used for propagation where it is relevant ({@link
 *   #setPositionAngleType(PositionAngle)}),
 *   <li>whether {@link org.orekit.propagation.integration.AdditionalEquations additional equations}
 *   (for example {@link PartialDerivativesEquations Jacobians}) should be propagated along with orbital state
 *   ({@link #addAdditionalEquations(org.orekit.propagation.integration.AdditionalEquations)}),
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addEventDetector(EventDetector)},
 *   {@link #clearEventsDetectors()})</li>
 *   <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 *   {@link #setMasterMode(double, org.orekit.propagation.sampling.OrekitFixedStepHandler)},
 *   {@link #setMasterMode(org.orekit.propagation.sampling.OrekitStepHandler)},
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
 * <p> The last element is the mass in kilograms.
 *
 * <p>The following code snippet shows a typical setting for Low Earth Orbit propagation in
 * equinoctial parameters and true longitude argument:</p>
 * <pre>
 * final double dP       = 0.001;
 * final double minStep  = 0.001;
 * final double maxStep  = 500;
 * final double initStep = 60;
 * final double[][] tolerance = NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
 * AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tolerance[0], tolerance[1]);
 * integrator.setInitialStepSize(initStep);
 * propagator = new NumericalPropagator(integrator);
 * </pre>
 * <p>By default, at the end of the propagation, the propagator resets the initial state to the final state,
 * thus allowing a new propagation to be started from there without recomputing the part already performed.
 * This behaviour can be chenged by calling {@link #setResetAtEnd(boolean)}.
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

    /** Force models used during the extrapolation of the orbit. */
    private final List<ForceModel> forceModels;

    /** boolean to ignore or not the creation of a NewtonianAttraction. */
    private boolean ignoreCentralAttraction = false;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a Keplerian
     * evolution only. The defaults are {@link OrbitType#EQUINOCTIAL}
     * for {@link #setOrbitType(OrbitType) propagation
     * orbit type} and {@link PositionAngle#TRUE} for {@link
     * #setPositionAngleType(PositionAngle) position angle type}.
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
     * orbit type} and {@link PositionAngle#TRUE} for {@link
     * #setPositionAngleType(PositionAngle) position angle type}.
     * @param integrator numerical integrator to use for propagation.
     * @param attitudeProvider the attitude law.
     * @since 10.1
     */
    public NumericalPropagator(final ODEIntegrator integrator,
                               final AttitudeProvider attitudeProvider) {
        super(integrator, PropagationType.MEAN);
        forceModels = new ArrayList<ForceModel>();
        initMapper();
        setAttitudeProvider(attitudeProvider);
        setSlaveMode();
        setOrbitType(OrbitType.EQUINOCTIAL);
        setPositionAngleType(PositionAngle.TRUE);
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
    public void setMu(final double mu) {
        if (ignoreCentralAttraction) {
            superSetMu(mu);
        } else {
            addForceModel(new NewtonianAttraction(mu));
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
    public void setOrbitType(final OrbitType orbitType) {
        super.setOrbitType(orbitType);
    }

    /** Get propagation parameter type.
     * @return orbit type used for propagation, null for
     * propagating using {@link org.orekit.utils.AbsolutePVCoordinates} rather than {@link Orbit}
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
     */
    public void setInitialState(final SpacecraftState initialState) {
        resetInitialState(initialState);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) {
        super.resetInitialState(state);
        if (!hasNewtonianAttraction()) {
            // use the state to define central attraction
            setMu(state.getMu());
        }
        setStartDate(state.getDate());
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return propagate(date).getPVCoordinates(frame);
    }

    /** {@inheritDoc} */
    protected StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                       final OrbitType orbitType, final PositionAngle positionAngleType,
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
                         final OrbitType orbitType, final PositionAngle positionAngleType,
                         final AttitudeProvider attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        /** {@inheritDoc} */
        public SpacecraftState mapArrayToState(final AbsoluteDate date, final double[] y, final double[] yDot,
                                               final PropagationType type) {
            // the parameter type is ignored for the Numerical Propagator

            final double mass = y[6];
            if (mass <= 0.0) {
                throw new OrekitException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE, mass);
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
                return new SpacecraftState(absPva, attitude, mass);
            } else {
                // propagation uses regular orbits
                final Orbit orbit       = getOrbitType().mapArrayToOrbit(y, yDot, getPositionAngleType(), date, getMu(), getFrame());
                final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

                return new SpacecraftState(orbit, attitude, mass);
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
        return new Main(integrator);
    }

    /** Internal class for osculating parameters integration. */
    private class Main implements MainStateEquations, TimeDerivativesEquations {

        /** Derivatives array. */
        private final double[] yDot;

        /** Current state. */
        private SpacecraftState currentState;

        /** Jacobian of the orbital parameters with respect to the Cartesian parameters. */
        private double[][] jacobian;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        Main(final ODEIntegrator integrator) {

            this.yDot     = new double[7];
            this.jacobian = new double[6][6];

            for (final ForceModel forceModel : forceModels) {
                forceModel.getEventsDetectors().forEach(detector -> setUpEventDetector(integrator, detector));
            }

            if (getOrbitType() == null) {
                // propagation uses absolute position-velocity-acceleration
                // we can set Jacobian once and for all
                for (int i = 0; i < jacobian.length; ++i) {
                    Arrays.fill(jacobian[i], 0.0);
                    jacobian[i][i] = 1.0;
                }
            }

        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState initialState, final AbsoluteDate target) {
            for (final ForceModel forceModel : forceModels) {
                forceModel.init(initialState, target);
            }
        }

        /** {@inheritDoc} */
        @Override
        public double[] computeDerivatives(final SpacecraftState state) {

            currentState = state;
            Arrays.fill(yDot, 0.0);
            if (getOrbitType() != null) {
                // propagation uses regular orbits
                currentState.getOrbit().getJacobianWrtCartesian(getPositionAngleType(), jacobian);
            }

            // compute the contributions of all perturbing forces,
            // using the Kepler contribution at the end since
            // NewtonianAttraction is always the last instance in the list
            for (final ForceModel forceModel : forceModels) {
                forceModel.addContribution(state, this);
            }

            if (getOrbitType() == null) {
                // position derivative is velocity, and was not added above in the force models
                // (it is added when orbit type is non-null because NewtonianAttraction considers it)
                final Vector3D velocity = currentState.getPVCoordinates().getVelocity();
                yDot[0] += velocity.getX();
                yDot[1] += velocity.getY();
                yDot[2] += velocity.getZ();
            }


            return yDot.clone();

        }

        /** {@inheritDoc} */
        @Override
        public void addKeplerContribution(final double mu) {
            if (getOrbitType() == null) {

                // if mu is neither 0 nor NaN, we want to include Newtonian acceleration
                if (mu > 0) {
                    // velocity derivative is Newtonian acceleration
                    final Vector3D position = currentState.getPVCoordinates().getPosition();
                    final double r2         = position.getNormSq();
                    final double coeff      = -mu / (r2 * FastMath.sqrt(r2));
                    yDot[3] += coeff * position.getX();
                    yDot[4] += coeff * position.getY();
                    yDot[5] += coeff * position.getZ();
                }

            } else {
                // propagation uses regular orbits
                currentState.getOrbit().addKeplerContribution(getPositionAngleType(), mu, yDot);
            }
        }

        /** {@inheritDoc} */
        public void addNonKeplerianAcceleration(final Vector3D gamma) {
            for (int i = 0; i < 6; ++i) {
                final double[] jRow = jacobian[i];
                yDot[i] += jRow[3] * gamma.getX() + jRow[4] * gamma.getY() + jRow[5] * gamma.getZ();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void addMassDerivative(final double q) {
            if (q > 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
            }
            yDot[6] += q;
        }

    }

    /** Estimate tolerance vectors for integrators when propagating in absolute position-velocity-acceleration.
     * @param dP user specified position error
     * @param absPva reference absolute position-velocity-acceleration
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @see NumericalPropagator#tolerances(double, Orbit, OrbitType)
     */
    public static double[][] tolerances(final double dP, final AbsolutePVCoordinates absPva) {

        final double relative = dP / absPva.getPosition().getNorm();
        final double dV = relative * absPva.getVelocity().getNorm();

        final double[] absTol = new double[7];
        final double[] relTol = new double[7];

        absTol[0] = dP;
        absTol[1] = dP;
        absTol[2] = dP;
        absTol[3] = dV;
        absTol[4] = dV;
        absTol[5] = dV;

        // we set the mass tolerance arbitrarily to 1.0e-6 kg, as mass evolves linearly
        // with trust, this often has no influence at all on propagation
        absTol[6] = 1.0e-6;

        Arrays.fill(relTol, relative);

        return new double[][] {
            absTol, relTol
        };

    }

    /** Estimate tolerance vectors for integrators when propagating in orbits.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V² r |dV| = mu |dr|
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
     */
    public static double[][] tolerances(final double dP, final Orbit orbit, final OrbitType type) {

        // estimate the scalar velocity error
        final PVCoordinates pv = orbit.getPVCoordinates();
        final double r2 = pv.getPosition().getNormSq();
        final double v  = pv.getVelocity().getNorm();
        final double dV = orbit.getMu() * dP / (v * r2);

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
     * @param dP user specified position error
     * @param dV user specified velocity error
     * @param orbit reference orbit
     * @param type propagation type for the meaning of the tolerance vectors elements
     * (it may be different from {@code orbit.getType()})
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     * @since 10.3
     */
    public static double[][] tolerances(final double dP, final double dV,
                                        final Orbit orbit, final OrbitType type) {

        final double[] absTol = new double[7];
        final double[] relTol = new double[7];

        // we set the mass tolerance arbitrarily to 1.0e-6 kg, as mass evolves linearly
        // with trust, this often has no influence at all on propagation
        absTol[6] = 1.0e-6;

        if (type == OrbitType.CARTESIAN) {
            absTol[0] = dP;
            absTol[1] = dP;
            absTol[2] = dP;
            absTol[3] = dV;
            absTol[4] = dV;
            absTol[5] = dV;
        } else {

            // convert the orbit to the desired type
            final double[][] jacobian = new double[6][6];
            final Orbit converted = type.convertType(orbit);
            converted.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);

            for (int i = 0; i < 6; ++i) {
                final double[] row = jacobian[i];
                absTol[i] = FastMath.abs(row[0]) * dP +
                            FastMath.abs(row[1]) * dP +
                            FastMath.abs(row[2]) * dP +
                            FastMath.abs(row[3]) * dV +
                            FastMath.abs(row[4]) * dV +
                            FastMath.abs(row[5]) * dV;
                if (Double.isNaN(absTol[i])) {
                    throw new OrekitException(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, type);
                }
            }

        }

        Arrays.fill(relTol, dP / FastMath.sqrt(orbit.getPVCoordinates().getPosition().getNormSq()));

        return new double[][] {
            absTol, relTol
        };

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

}
