/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.OsculatingToMeanElementsConverter;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.integration.StateMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

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
 * <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 * {@link #setMasterMode(double, org.orekit.propagation.sampling.OrekitFixedStepHandler)},
 * {@link #setMasterMode(org.orekit.propagation.sampling.OrekitStepHandler)},
 * {@link #setEphemerisMode()}, {@link #getGeneratedEphemeris()})</li>
 * </ul>
 * <p>
 * From these configuration parameters, only the initial state is mandatory.
 * The default propagation settings are in {@link OrbitType#EQUINOCTIAL equinoctial}
 * parameters with {@link PositionAngle#TRUE true} longitude argument.
 * The central attraction coefficient used to define the initial orbit will be used.
 * However, specifying only the initial state would mean the propagator would use
 * only keplerian forces. In this case, the simpler
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
 * (a, e<sub>x</sub>, e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, &lambda;<sub>m</sub>)
 * in meters and radians,</li>
 * </ul>
 * </p>
 * <p>
 * The same propagator can be reused for several orbit extrapolations,
 * by resetting the initial state without modifying the other configuration
 * parameters. However, the same instance cannot be used simultaneously by
 * different threads, the class is <em>not</em> thread-safe.
 * </p>
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

    /** State mapper holding the force models. */
    private MeanPlusShortPeriodicMapper mapper;

    /** Create a new instance of DSSTPropagator.
     *  <p>
     *  After creation, there are no perturbing forces at all.
     *  This means that if {@link #addForceModel addForceModel}
     *  is not called after creation, the integrated orbit will
     *  follow a keplerian evolution only.
     *  </p>
     *  @param integrator numerical integrator to use for propagation.
     */
    public DSSTPropagator(final AbstractIntegrator integrator) {
        super(integrator);
        initMapper();
        // DSST uses only equinoctial orbits and mean longitude argument
        setOrbitType(OrbitType.EQUINOCTIAL);
        setPositionAngleType(PositionAngle.TRUE);
        setAttitudeProvider(DEFAULT_LAW);
    }

    /** Set the initial state with osculating orbital elements.
     *  @param initialState initial state (defined with osculating elements)
     *  @throws PropagationException if the initial state cannot be set
     */
    public void setInitialState(final SpacecraftState initialState)
        throws PropagationException {
        setInitialState(initialState, true);
    }

    /** Set the initial state.
     *  @param initialState initial state
     *  @param withOsculatingElements true if the orbital state is defined with osculating elements
     *  @throws PropagationException if the initial state cannot be set
     */
    public void setInitialState(final SpacecraftState initialState,
                                final boolean withOsculatingElements)
        throws PropagationException {
        mapper.setInitialIsOsculating(withOsculatingElements);
        resetInitialState(initialState);
    }

    /** Reset the initial state.
     *
     *  @param state new initial state
     *  @throws PropagationException if initial state cannot be reset
     */
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.setStartDate(state.getDate());
        super.resetInitialState(state);
    }

    /** Check if the initial state is provided in osculating elements.
     * @return true if initial state is provided in osculating elements
     */
    public boolean initialIsOsculating() {
        return mapper.initialIsOsculating();
    }

    /** Add a force model to the global perturbation model.
     *  <p>
     *  If this method is not called at all,
     *  the integrated orbit will follow a keplerian evolution only.
     *  </p>
     *  @param force perturbing {@link DSSTForceModel force} to add
     *  @see #removeForceModels()
     */
    public void addForceModel(final DSSTForceModel force) {
        mapper.addForceModel(force);
    }

    /** Remove all perturbing force models from the global perturbation model.
     *  <p>
     *  Once all perturbing forces have been removed (and as long as no new force model is added),
     *  the integrated orbit will follow a keplerian evolution only.
     *  </p>
     *  @see #addForceModel(DSSTForceModel)
     */
    public void removeForceModels() {
        mapper.removeForceModels();
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

    /** Method called just before integration.
     * <p>
     * The default implementation does nothing, it may be specialized in subclasses.
     * </p>
     * @param initialState initial state
     * @param tEnd target date at which state should be propagated
     * @exception OrekitException if hook cannot be run
     */
    protected void beforeIntegration(final SpacecraftState initialState,
                                     final AbsoluteDate tEnd)
        throws OrekitException {

        // compute common auxiliary elements
        final AuxiliaryElements aux = new AuxiliaryElements(initialState.getOrbit(), I);

        // initialize all perturbing forces
        for (final DSSTForceModel force : mapper.getForceModels()) {
            force.initialize(aux);
        }

    }

    /** {@inheritDoc} */
    protected StateMapper createMapper(final AbsoluteDate referenceDate, final double mu,
                                       final OrbitType orbitType, final PositionAngle positionAngleType,
                                       final AttitudeProvider attitudeProvider, final Frame frame) {

        // create a mapper with the common settings provided as arguments
        final MeanPlusShortPeriodicMapper newMapper =
                new MeanPlusShortPeriodicMapper(referenceDate, mu, attitudeProvider, frame);

        // copy the specific settings from the existing mapper
        if (mapper != null) {
            for (final DSSTForceModel forceModel : mapper.getForceModels()) {
                newMapper.addForceModel(forceModel);
            }
            newMapper.setSatelliteRevolution(mapper.getSatelliteRevolution());
            newMapper.setInitialIsOsculating(mapper.initialIsOsculating());
        }

        mapper = newMapper;
        return mapper;

    }

    /** Internal mapper using mean parameters plus short periodic terms. */
    private static class MeanPlusShortPeriodicMapper extends StateMapper implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130621L;

        /** Flag specifying whether the initial orbital state is given with osculating elements. */
        private boolean                    initialIsOsculating;

        /** Force models used to compute short periodic terms. */
        private final transient List<DSSTForceModel> forceModels;

        /** Number of satellite revolutions in the averaging interval. */
        private int                        satelliteRevolution;

        /** Simple constructor.
         * <p>
         * The position parameter type is meaningful only if {@link
         * #getOrbitType() propagation orbit type}
         * support it. As an example, it is not meaningful for propagation
         * in {@link OrbitType#CARTESIAN Cartesian} parameters.
         * </p>
         * @param referenceDate reference date
         * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
         * @param attitudeProvider attitude provider
         * @param frame inertial frame
         */
        public MeanPlusShortPeriodicMapper(final AbsoluteDate referenceDate, final double mu,
                                           final AttitudeProvider attitudeProvider, final Frame frame) {

            super(referenceDate, mu, OrbitType.EQUINOCTIAL, PositionAngle.MEAN, attitudeProvider, frame);

            this.forceModels = new ArrayList<DSSTForceModel>();

            // Default averaging period for conversion from osculating to mean elements
            this.satelliteRevolution = 2;

            this.initialIsOsculating = true;

        }

        /** {@inheritDoc} */
        public SpacecraftState mapArrayToState(final double t, final double[] y)
            throws OrekitException {

            final AbsoluteDate date = mapDoubleToDate(t);

            // add short periodic variations to mean elements to get osculating elements
            // (the loop may not be performed if there are no force models, in the
            //  case we want to remain in mean parameters only)
            final double[] osculatingElements = y.clone();
            for (final DSSTForceModel forceModel : forceModels) {
                final double[] shortPeriodic = forceModel.getShortPeriodicVariations(date, y);
                for (int i = 0; i < shortPeriodic.length; i++) {
                    osculatingElements[i] += shortPeriodic[i];
                }
            }

            final double mass = y[6];
            if (mass <= 0.0) {
                throw new PropagationException(OrekitMessages.SPACECRAFT_MASS_BECOMES_NEGATIVE, mass);
            }

            final Orbit orbit       = OrbitType.EQUINOCTIAL.mapArrayToOrbit(y, PositionAngle.MEAN, date, getMu(), getFrame());
            final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, getFrame());

            return new SpacecraftState(orbit, attitude, mass);

        }

        /** {@inheritDoc} */
        public void mapStateToArray(final SpacecraftState state, final double[] y)
            throws OrekitException {

            final Orbit meanOrbit;
            if (!initialIsOsculating) {
                // the state is considered to be already a mean state
                meanOrbit = state.getOrbit();
            } else {
                // the state is considered to be an osculating state
                final Propagator propagator = createPropagator(state);
                meanOrbit = new OsculatingToMeanElementsConverter(state, satelliteRevolution, propagator).convert().getOrbit();
            }

            OrbitType.EQUINOCTIAL.mapOrbitToArray(meanOrbit, PositionAngle.MEAN, y);
            y[6] = state.getMass();

        }

        /** Add a force model to the global perturbation model.
         *  <p>
         *  If this method is not called at all,
         *  the integrated orbit will follow a keplerian evolution only.
         *  </p>
         *  @param force perturbing {@link DSSTForceModel force} to add
         *  @see #removeForceModels()
         */
        public void addForceModel(final DSSTForceModel force) {
            forceModels.add(force);
        }

        /** Remove all perturbing force models from the global perturbation model.
         *  <p>
         *  Once all perturbing forces have been removed (and as long as no new force model is added),
         *  the integrated orbit will follow a keplerian evolution only.
         *  </p>
         *  @see #addForceModel(DSSTForceModel)
         */
        public void removeForceModels() {
            forceModels.clear();
        }

        /** Get the force models.
         * @return force models
         */
        public List<DSSTForceModel> getForceModels() {
            return forceModels;
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

        /** Set the osculating parameters flag.
         * @param initialIsOsculating if true, the initial state is provided in osculating elements
         */
        public void setInitialIsOsculating(final boolean initialIsOsculating) {
            this.initialIsOsculating = initialIsOsculating;
        }

        /** Check if the initial state is provided in osculating elements.
         * @return true if initial state is provided in osculating elements
         */
        public boolean initialIsOsculating() {
            return initialIsOsculating;
        }

        /** Create a reference numerical propagator to convert orbit to mean elements.
         *  @param initialState initial state
         *  @return propagator
         *  @throws OrekitException if some numerical force model cannot be built
         */
        private Propagator createPropagator(final SpacecraftState initialState)
            throws OrekitException {
            final Orbit initialOrbit = initialState.getOrbit();
            final double[][] tol = NumericalPropagator.tolerances(1.0, initialOrbit, OrbitType.EQUINOCTIAL);
            final double minStep = 1.;
            final double maxStep = 200.;
            final AdaptiveStepsizeIntegrator integ = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
            integ.setInitialStepSize(100.);

            final NumericalPropagator propagator = new NumericalPropagator(integ);
            propagator.setOrbitType(OrbitType.EQUINOCTIAL);
            propagator.setInitialState(initialState);

            // Define the same force model as the DSST
            for (final DSSTForceModel force : forceModels) {
                if (force instanceof DSSTCentralBody) {
                    // Central body
                    final UnnormalizedSphericalHarmonicsProvider provider = ((DSSTCentralBody) force).getProvider();
                    final ForceModel holmesFeatherstone =
                            new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                  GravityFieldFactory.getNormalizedProvider(provider));
                    propagator.addForceModel(holmesFeatherstone);
                } else if (force instanceof DSSTThirdBody) {
                    // Third body
                    final CelestialBody body = ((DSSTThirdBody) force).getBody();
                    final ForceModel third   = new ThirdBodyAttraction(body);
                    propagator.addForceModel(third);
                } else if (force instanceof DSSTAtmosphericDrag) {
                    // Atmospheric drag
                    final Atmosphere atm = ((DSSTAtmosphericDrag) force).getAtmosphere();
                    final double area    = ((DSSTAtmosphericDrag) force).getArea();
                    final double cd      = ((DSSTAtmosphericDrag) force).getCd();
                    final SphericalSpacecraft scr = new SphericalSpacecraft(area, cd, 0., 0.);
                    final ForceModel drag = new DragForce(atm, scr);
                    propagator.addForceModel(drag);
                } else if (force instanceof DSSTSolarRadiationPressure) {
                    // Solar radiation pressure
                    final double ae   = ((DSSTSolarRadiationPressure) force).getEquatorialRadius();
                    final double area = ((DSSTSolarRadiationPressure) force).getArea();
                    final double cr   = ((DSSTSolarRadiationPressure) force).getCr();
                    // Convert DSST SRP coefficient convention to numerical's one
                    final double kr   = 3.25 - 2.25 * cr;
                    final SphericalSpacecraft scr = new SphericalSpacecraft(area, 0., 0., kr);
                    final ForceModel pressure = new SolarRadiationPressure(CelestialBodyFactory.getSun(), ae, scr);
                    propagator.addForceModel(pressure);
                }
            }
            return propagator;
        }

        /** Replace the instance with a data transfer object for serialization.
         * @return data transfer object that will be serialized
         * @exception NotSerializableException if one of the force models cannot be serialized
         */
        private Object writeReplace() throws NotSerializableException {

            // Check the force models can be serialized
            final DSSTForceModel[] serializableorceModels = new DSSTForceModel[forceModels.size()];
            for (int i = 0; i < serializableorceModels.length; ++i) {
                final DSSTForceModel forceModel = forceModels.get(i);
                if (forceModel instanceof Serializable) {
                    serializableorceModels[i] = forceModel;
                } else {
                    throw new NotSerializableException(forceModel.getClass().getName());
                }
            }
            return new DataTransferObject(getReferenceDate(), getMu(), getAttitudeProvider(), getFrame(),
                                          initialIsOsculating, serializableorceModels, satelliteRevolution);
        }

        /** Internal class used only for serialization. */
        private static class DataTransferObject implements Serializable {

            /** Serializable UID. */
            private static final long serialVersionUID = 20130621L;

            /** Reference date. */
            private final AbsoluteDate referenceDate;

            /** Central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
            private final double mu;

            /** Attitude provider. */
            private final AttitudeProvider attitudeProvider;

            /** Inertial frame. */
            private final Frame frame;

            /** Flag specifying whether the initial orbital state is given with osculating elements. */
            private final boolean initialIsOsculating;

            /** Force models to use for short periodic terms computation. */
            private final DSSTForceModel[] forceModels;

            /** Number of satellite revolutions in the averaging interval. */
            private final int satelliteRevolution;

            /** Simple constructor.
             * @param referenceDate reference date
             * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
             * @param attitudeProvider attitude provider
             * @param frame inertial frame
             * @param initialIsOsculating if true, initial orbital state is given with osculating elements
             * @param forceModels force models to use for short periodic terms computation
             * @param satelliteRevolution number of satellite revolutions in the averaging interval
             */
            public DataTransferObject(final AbsoluteDate referenceDate, final double mu,
                                      final AttitudeProvider attitudeProvider, final Frame frame,
                                      final boolean initialIsOsculating,
                                      final DSSTForceModel[] forceModels, final int satelliteRevolution) {
                this.referenceDate       = referenceDate;
                this.mu                  = mu;
                this.attitudeProvider    = attitudeProvider;
                this.frame               = frame;
                this.initialIsOsculating = initialIsOsculating;
                this.forceModels         = forceModels;
                this.satelliteRevolution = satelliteRevolution;
            }

            /** Replace the deserialized data transfer object with a {@link MeanPlusShortPeriodicMapper}.
             * @return replacement {@link MeanPlusShortPeriodicMapper}
             */
            private Object readResolve() {
                final MeanPlusShortPeriodicMapper mapper =
                        new MeanPlusShortPeriodicMapper(referenceDate, mu, attitudeProvider, frame);
                for (final DSSTForceModel forceModel : forceModels) {
                    mapper.addForceModel(forceModel);
                }
                mapper.setSatelliteRevolution(satelliteRevolution);
                mapper.setInitialIsOsculating(initialIsOsculating);
                return mapper;
            }

        }

    }

    /** {@inheritDoc} */
    protected MainStateEquations getMainStateEquations(final AbstractIntegrator integrator) {
        return new Main(integrator);
    }

    /** Internal class for mean parameters integration. */
    private class Main implements MainStateEquations {

        /** Derivatives array. */
        private final double[] yDot;

        /** Simple constructor.
         * @param integrator numerical integrator to use for propagation.
         */
        public Main(final AbstractIntegrator integrator) {
            yDot = new double[7];

            for (final DSSTForceModel forceModel : mapper.getForceModels()) {
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

            // compute common auxiliary elements
            final AuxiliaryElements aux = new AuxiliaryElements(state.getOrbit(), I);

            // initialize all perturbing forces
            for (final DSSTForceModel force : mapper.getForceModels()) {
                force.initializeStep(aux);
            }

            Arrays.fill(yDot, 0.0);

            // compute the contributions of all perturbing forces
            for (final DSSTForceModel forceModel : mapper.getForceModels()) {
                final double[] daidt = forceModel.getMeanElementRate(state);
                for (int i = 0; i < daidt.length; i++) {
                    yDot[i] += daidt[i];
                }
            }

            // finalize derivatives by adding the Kepler contribution
            final EquinoctialOrbit orbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(state.getOrbit());
            orbit.addKeplerContribution(PositionAngle.MEAN, getMu(), yDot);

            return yDot.clone();
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
     *  V<sup>2</sup> r |dV| = mu |dr|
     *  </pre>
     *
     *  So we deduce a scalar velocity error consistent with the position error. From here, we apply
     *  orbits Jacobians matrices to get consistent errors on orbital parameters.
     *  </p>
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
     * @exception PropagationException if Jacobian is singular
     */
    public static double[][] tolerances(final double dP,
                                        final Orbit orbit)
        throws PropagationException {

        return NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);

    }

}
