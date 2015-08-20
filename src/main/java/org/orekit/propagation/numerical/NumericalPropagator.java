/* Copyright 2002-2015 CS Systèmes d'Information
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

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.StateMapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

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
 * The last element is the mass in kilograms.
 * </p>
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
 * <p>The same propagator can be reused for several orbit extrapolations, by resetting
 * the initial state without modifying the other configuration parameters. However, the
 * same instance cannot be used simultaneously by different threads, the class is <em>not</em>
 * thread-safe.</p>

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

    /** Central body attraction. */
    private NewtonianAttraction newtonianAttraction;

    /** Force models used during the extrapolation of the Orbit, without jacobians. */
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
     */
    public NumericalPropagator(final AbstractIntegrator integrator) {
        super(integrator, true);
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
     * @exception PropagationException if initial state cannot be set
     */
    public void setInitialState(final SpacecraftState initialState) throws PropagationException {
        resetInitialState(initialState);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.resetInitialState(state);
        if (newtonianAttraction == null) {
            setMu(state.getMu());
        }
        setStartDate(null);
    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        return propagate(date).getPVCoordinates(frame);
    }

    /** {@inheritDoc} */
    protected StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                       final OrbitType orbitType, final PositionAngle positionAngleType,
                                       final AttitudeProvider attitudeProvider, final Frame frame) {
        return new OsculatingMapper(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
    }

    /** Internal mapper using directly osculating parameters. */
    private static class OsculatingMapper extends StateMapper implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130621L;

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
        public OsculatingMapper(final AbsoluteDate referenceDate, final double mu,
                                final OrbitType orbitType, final PositionAngle positionAngleType,
                                final AttitudeProvider attitudeProvider, final Frame frame) {
            super(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
        }

        /** {@inheritDoc} */
        public SpacecraftState mapArrayToState(final double t, final double[] y, final boolean meanOnly)
            throws OrekitException {
            // the parameter meanOnly is ignored for the Numerical Propagator

            final double mass = y[6];
            if (mass <= 0.0) {
                throw new PropagationException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE, mass);
            }

            final AbsoluteDate date = mapDoubleToDate(t);
            final Orbit orbit       = getOrbitType().mapArrayToOrbit(y, getPositionAngleType(), date, getMu(), getFrame());
            final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

            return new SpacecraftState(orbit, attitude, mass);

        }

        /** {@inheritDoc} */
        public void mapStateToArray(final SpacecraftState state, final double[] y) {
            getOrbitType().mapOrbitToArray(state.getOrbit(), getPositionAngleType(), y);
            y[6] = state.getMass();
        }

        /** Replace the instance with a data transfer object for serialization.
         * @return data transfer object that will be serialized
         * @exception NotSerializableException if the state mapper cannot be serialized (typically for DSST propagator)
         */
        private Object writeReplace() throws NotSerializableException {
            return new DataTransferObject(getReferenceDate(), getMu(), getOrbitType(),
                                          getPositionAngleType(), getAttitudeProvider(), getFrame());
        }

        /** Internal class used only for serialization. */
        private static class DataTransferObject implements Serializable {

            /** Serializable UID. */
            private static final long serialVersionUID = 20130621L;

            /** Reference date. */
            private final AbsoluteDate referenceDate;

            /** Central attraction coefficient (m³/s²). */
            private final double mu;

            /** Orbit type to use for mapping. */
            private final OrbitType orbitType;

            /** Angle type to use for propagation. */
            private final PositionAngle positionAngleType;

            /** Attitude provider. */
            private final AttitudeProvider attitudeProvider;

            /** Inertial frame. */
            private final Frame frame;

            /** Simple constructor.
             * @param referenceDate reference date
             * @param mu central attraction coefficient (m³/s²)
             * @param orbitType orbit type to use for mapping
             * @param positionAngleType angle type to use for propagation
             * @param attitudeProvider attitude provider
             * @param frame inertial frame
             */
            DataTransferObject(final AbsoluteDate referenceDate, final double mu,
                                      final OrbitType orbitType, final PositionAngle positionAngleType,
                                      final AttitudeProvider attitudeProvider, final Frame frame) {
                this.referenceDate     = referenceDate;
                this.mu                = mu;
                this.orbitType         = orbitType;
                this.positionAngleType = positionAngleType;
                this.attitudeProvider  = attitudeProvider;
                this.frame             = frame;
            }

            /** Replace the deserialized data transfer object with a {@link OsculatingMapper}.
             * @return replacement {@link OsculatingMapper}
             */
            private Object readResolve() {
                return new OsculatingMapper(referenceDate, mu, orbitType, positionAngleType, attitudeProvider, frame);
            }
        }

    }

    /** {@inheritDoc} */
    protected MainStateEquations getMainStateEquations(final AbstractIntegrator integrator) {
        return new Main(integrator);
    }

    /** Internal class for osculating parameters integration. */
    private class Main implements MainStateEquations, TimeDerivativesEquations {

        /** Derivatives array. */
        private final double[] yDot;

        /** Current orbit. */
        private Orbit orbit;

        /** Jacobian of the orbital parameters with respect to the cartesian parameters. */
        private double[][] jacobian;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        Main(final AbstractIntegrator integrator) {

            this.yDot     = new double[7];
            this.jacobian = new double[6][6];

            for (final ForceModel forceModel : forceModels) {
                final EventDetector[] modelDetectors = forceModel.getEventsDetectors();
                if (modelDetectors != null) {
                    for (final EventDetector detector : modelDetectors) {
                        setUpEventDetector(integrator, detector);
                    }
                }
            }

        }

        /** {@inheritDoc} */
        public double[] computeDerivatives(final SpacecraftState state) throws OrekitException {

            orbit = state.getOrbit();
            Arrays.fill(yDot, 0.0);
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
        public void addXYZAcceleration(final double x, final double y, final double z) {
            for (int i = 0; i < 6; ++i) {
                final double[] jRow = jacobian[i];
                yDot[i] += jRow[3] * x + jRow[4] * y + jRow[5] * z;
            }
        }

        /** {@inheritDoc} */
        public void addAcceleration(final Vector3D gamma, final Frame frame)
            throws OrekitException {
            final Transform t = frame.getTransformTo(orbit.getFrame(), orbit.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        public void addMassDerivative(final double q) {
            if (q > 0) {
                throw new OrekitIllegalArgumentException(OrekitMessages.POSITIVE_FLOW_RATE, q);
            }
            yDot[6] += q;
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
     * @exception PropagationException if Jacobian is singular
     */
    public static double[][] tolerances(final double dP, final Orbit orbit, final OrbitType type)
        throws PropagationException {

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
                    throw new PropagationException(OrekitMessages.SINGULAR_JACOBIAN_FOR_ORBIT_TYPE, type);
                }
            }

        }

        Arrays.fill(relTol, dP / FastMath.sqrt(r2));

        return new double[][] {
            absTol, relTol
        };

    }

}

