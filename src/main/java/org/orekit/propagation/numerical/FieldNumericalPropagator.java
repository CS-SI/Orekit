/* Copyright 2002-2016 CS Systèmes d'Information
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
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.FieldODEIntegrator;
import org.hipparchus.util.MathArrays;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.attitudes.FieldAttitudeProvider;
import org.orekit.attitudes.FieldInertialProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.integration.FieldAbstractIntegratedPropagator;
import org.orekit.propagation.integration.FieldStateMapper;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** This class propagates {@link org.orekit.orbits.FieldOrbit orbits} using
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
 * would mean the propagator would use only keplerian forces. In this case, the simpler {@link
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
 * <p>The same propagator can be reused for several orbit extrapolations, by resetting
 * the initial state without modifying the other configuration parameters. However, the
 * same instance cannot be used simultaneously by different threads, the class is <em>not</em>
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

    /** Central body attraction. */
    private NewtonianAttraction newtonianAttraction;

    /** Force models used during the extrapolation of the FieldOrbit<T>, without jacobians. */
    private final List<ForceModel> forceModels;

    /** Create a new instance of NumericalPropagator, based on orbit definition mu.
     * After creation, the instance is empty, i.e. the attitude provider is set to an
     * unspecified default law and there are no perturbing forces at all.
     * This means that if {@link #addForceModel addForceModel} is not
     * called after creation, the integrated orbit will follow a keplerian
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
        final FieldInertialProvider<T> default_law = new FieldInertialProvider<T>(
                        new FieldRotation<T>(field.getOne(), field.getZero(), field.getZero(), field.getZero(), false));
        setAttitudeProvider(default_law);
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
        super.setMu(mu);
        newtonianAttraction = new NewtonianAttraction(mu);
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
        if (newtonianAttraction == null) {
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
                                       final FieldAttitudeProvider<T> attitudeProvider, final Frame frame) {
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
                         final FieldAttitudeProvider<T> attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        /** {@inheritDoc} */
        public FieldSpacecraftState<T> mapArrayToState(final FieldAbsoluteDate<T> date, final T[] y, final boolean meanOnly)
            throws OrekitException {
            // the parameter meanOnly is ignored for the Numerical Propagator

            final T mass = y[6];
            if (mass.getReal() <= 0.0) {
                throw new OrekitException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE, mass);
            }
            final FieldOrbit<T> orbit       = super.getOrbitType().mapArrayToOrbit(y, super.getPositionAngleType(), date, getMu(), getFrame());
            final FieldAttitude<T> attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());
            return new FieldSpacecraftState<T>(orbit, attitude, mass);
        }

        /** {@inheritDoc} */
        public void mapStateToArray(final FieldSpacecraftState<T> state, final T[] y) {
            super.getOrbitType().mapOrbitToArray(state.getOrbit(), super.getPositionAngleType(), y);
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

        /** Jacobian of the orbital parameters with respect to the cartesian parameters. */
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
        public T[] computeDerivatives(final FieldSpacecraftState<T> state) throws OrekitException {
            final T zero = state.getA().getField().getZero();
            orbit = state.getOrbit();
            Arrays.fill(yDot, zero);
            orbit.getJacobianWrtCartesian(getPositionAngleType(), jacobian);
            // compute the contributions of all perturbing forces
            for (final ForceModel forceModel : forceModels) {
                forceModel.addContribution(state, this);
            }
            // finalize derivatives by adding the Kepler contribution
            newtonianAttraction.addContribution(state, this);
            return yDot.clone();

        }

        /** {@inheritDoc} */
        public void addKeplerContribution(final double mu) {
            orbit.addKeplerContribution(getPositionAngleType(), mu, yDot);
        }

        /** {@inheritDoc} */
        public void addXYZAcceleration(final T x, final T y, final T z) {
            for (int i = 0; i < 6; ++i) {
                final T[] jRow = jacobian[i];
                yDot[i] = yDot[i].add(jRow[3].linearCombination(jRow[3], x, jRow[4], y, jRow[5], z));
            }
        }

        /** {@inheritDoc} */
        public void addAcceleration(final FieldVector3D<T> gamma, final Frame frame)
            throws OrekitException {
            final Transform t = frame.getTransformTo(orbit.getFrame(), orbit.getDate().toAbsoluteDate());
            final FieldVector3D<T> gammInRefFrame = t.transformVector(gamma);
            addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        public void addMassDerivative(final T q) {
            if (q.getReal() > 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
            }
            yDot[6] = yDot[6].add(q);
        }

//        /** {@inheritDoc} */
//        public void addMassDerivative(final double q) {
//            if (q > 0) {
//                throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
//            }
//            yDot[6] = yDot[6].add(q);
//        }

//        @Override
//        public void addXYZAcceleration(final double x, final double y, final double z) {
//            for (int i = 0; i < 6; ++i) {
//                final T[] jRow = jacobian[i];
//                yDot[i] = yDot[i].add(jRow[3].linearCombination(x, jRow[3], y, jRow[4], z, jRow[5]));
//            }
//        }
//
//        @Override
//        public void addAcceleration(final Vector3D gamma, final Frame frame)
//            throws OrekitException {
//            final Transform t = frame.getTransformTo(orbit.getFrame(), orbit.getDate().toAbsoluteDate());
//            final Vector3D gammInRefFrame = t.transformVector(gamma);
//            addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
//        }
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

