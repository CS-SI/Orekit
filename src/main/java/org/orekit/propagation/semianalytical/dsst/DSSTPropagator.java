package org.orekit.propagation.semianalytical.dsst;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.apache.commons.math.ode.sampling.StepInterpolator;
import org.apache.commons.math.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.gravity.CunninghamAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.OsculatingToMeanElementsConverter;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

/**
 * This class propagates {@link org.orekit.orbits.Orbit orbits} using the DSST theory.
 * <p>
 * The DSST theory, as exposed by D.A.Danielson & al. (1995), describes a semianalytical propagator
 * that combines the accuracy of numerical propagators with the speed of analytical propagators.
 * Whereas analytical propagators are configured only thanks to their various constructors and can
 * be used immediately after construction, such a semianalytical propagator configuration involves
 * setting several parameters between construction time and propagation time, just as numerical
 * propagators.
 * </p>
 * <p>
 * The configuration parameters that can be set are:
 * </p>
 * <ul>
 * <li>the initial spacecraft state ({@link #resetInitialState(SpacecraftState)})</li>
 * <li>the various force models ({@link #addForceModel(DSSTForceModel)},
 * {@link #removeForceModels()})</li>
 * <li>the discrete events that should be triggered during propagation (
 * {@link #addEventDetector(EventDetector)}, {@link #clearEventsDetectors()})</li>
 * <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 * {@link #setMasterMode(double, OrekitFixedStepHandler)}, {@link #setMasterMode(OrekitStepHandler)}, {@link #setEphemerisMode()}, {@link #getGeneratedEphemeris()})</li>
 * </ul>
 * </p>
 * <p>
 * From these configuration parameters, only the initial state is mandatory. The default propagation
 * settings are in {@link OrbitType#EQUINOCTIAL equinoctial} parameters with
 * {@link PositionAngle#TRUE true} longitude argument. The central attraction coefficient used to
 * define the initial orbit will be used. However, specifying only the initial state would mean the
 * propagator would use only keplerian forces. In this case, the simpler
 * {@link org.orekit.propagation.analytical.KeplerianPropagator KeplerianPropagator} class would
 * perhaps be more effective.
 * </p>
 * <p>
 * The underlying numerical integrator set up in the constructor may also have its own configuration
 * parameters. Typical configuration parameters for adaptive stepsize integrators are the min, max
 * and perhaps start step size as well as the absolute and/or relative errors thresholds.
 * </p>
 * <p>
 * The state that is seen by the integrator is a simple six elements double array. These six
 * elements are:
 * <ul>
 * <li>the {@link org.orekit.orbits.EquinoctialOrbit equinoctial orbit parameters} (a,
 * e<sub>x</sub>, e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, &lambda;<sub>v</sub>) in meters and
 * radians,</li>
 * </ul>
 * </p>
 * <p>
 * The same propagator can be reused for several orbit extrapolations, by resetting the initial
 * state without modifying the other configuration parameters. However, the same instance cannot be
 * used simultaneously by different threads, the class is <em>not</em> thread-safe.
 * </p>
 * 
 * @see SpacecraftState
 * @see DSSTForceModel
 * @author Romain Di Costanzo
 * @author Pascal Parraud
 */
public class DSSTPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long              serialVersionUID = -1217566398912634178L;

    /** Propagation orbit type. */
    private static final OrbitType         ORBIT_TYPE       = OrbitType.EQUINOCTIAL;

    /** Position angle type. */
    private static final PositionAngle     ANGLE_TYPE       = PositionAngle.MEAN;

    /** Position error tolerance (m). */
    private static final double            POSITION_ERROR   = 1.0;

    /** Position error tolerance (m). */
    private static final double            EXTRA_TIME       = Constants.JULIAN_DAY;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private transient FirstOrderIntegrator integrator;

    /** Force models used during the extrapolation of the Orbit. */
    private final List<DSSTForceModel>     forceModels;

    /** Step accumulator. */
    private StepAccumulator                cumulator;

    /** Reference date. */
    private AbsoluteDate                   referenceDate;

    /** Target date. */
    private AbsoluteDate                   target;

    /** Target date. */
    private double                         extraTime;

    /** Current &mu;. */
    private double                         mu;

    /** Current frame. */
    private Frame                          frame;

    /** Current mass. */
    private double                         mass;

    /** Counter for differential equations calls. */
    private int                            calls;

    /** Has the force model been initialized */
    private boolean                        initialized;

    /**
     * DSST truncation algorithm must be reset when orbital parameters have evolved too much. When
     * integration reach the resetDate, the {@link DSSTForceModel#initialize(SpacecraftState)}
     * method is called, and the next resetDate is set to resetDate + timeShiftToInitialize.
     */
    private AbsoluteDate                   resetDate;

    /**
     * DSST force model will be re-initialized every time the propagation date will be bigger than
     * resetDate + timeShiftToInitialize. In seconds.
     */
    private double                         timeShiftToInitialize;

    /** Is the orbital state in osculating parameters */
    private boolean                        isOsculating;

    /** number of satellite revolutions in the averaging interval */
    private int                            satelliteRevolution;

    /** Modified Newcomb Operator */
    private static double[][][]            newcomb          = null;

    /**
     * Build a DSSTPropagator from integrator and orbit.
     * <p>
     * Mass and attitude provider are set to unspecified non-null arbitrary values.
     * </p>
     * <p>
     * After creation, there are no perturbing forces at all. This means that if
     * {@link #addForceModel addForceModel} is not called after creation, the integrated orbit will
     * follow a keplerian evolution only.
     * </p>
     * 
     * @param integrator
     *            numerical integrator used to integrate mean coefficient defined by the SST theory.
     * @param initialOrbit
     *            initial orbit
     * @param isOsculating
     *            is the orbital state in osculating parameters
     * @param timeShiftToInitialize
     *            DSST force model will be re-initialized every time the propagation date will be
     *            bigger than resetDate + timeShiftToInitialize. In seconds.
     * @throws OrekitException
     *             if an error occurs in orbit averaging (i.e when transforming osculating elements
     *             into mean elements)
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator,
                          final Orbit initialOrbit,
                          final boolean isOsculating,
                          final double timeShiftToInitialize)
                                                             throws OrekitException {
        this(integrator, initialOrbit, isOsculating, timeShiftToInitialize, DEFAULT_LAW, DEFAULT_MASS);
    }

    /**
     * Build a DSSTPropagator from integrator, orbit and attitude provider.
     * <p>
     * Mass is set to an unspecified non-null arbitrary value.
     * </p>
     * <p>
     * After creation, there are no perturbing forces at all. This means that if
     * {@link #addForceModel addForceModel} is not called after creation, the integrated orbit will
     * follow a keplerian evolution only.
     * </p>
     * 
     * @param integrator
     *            numerical integrator used to integrate mean coefficient defined by the SST theory.
     * @param initialOrbit
     *            initial orbit
     * @param isOsculating
     *            is the orbital state in osculating parameters
     * @param timeShiftToInitialize
     *            DSST force model will be re-initialized every time the propagation date will be
     *            bigger than resetDate + timeShiftToInitialize. In seconds.
     * @param attitudeProv
     *            attitude provider
     * @throws OrekitException
     *             if an error occurs in orbit averaging (i.e when transforming osculating elements
     *             into mean elements)
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator,
                          final Orbit initialOrbit,
                          final boolean isOsculating,
                          final double timeShiftToInitialize,
                          final AttitudeProvider attitudeProv)
                                                              throws OrekitException {
        this(integrator, initialOrbit, isOsculating, timeShiftToInitialize, attitudeProv, DEFAULT_MASS);
    }

    /**
     * Build a DSSTPropagator from integrator, orbit and mass.
     * <p>
     * Attitude provider is set to an unspecified non-null arbitrary value.
     * </p>
     * <p>
     * After creation, there are no perturbing forces at all. This means that if
     * {@link #addForceModel addForceModel} is not called after creation, the integrated orbit will
     * follow a keplerian evolution only.
     * </p>
     * 
     * @param integrator
     *            numerical integrator used to integrate mean coefficient defined by the SST theory.
     * @param initialOrbit
     *            initial orbit
     * @param isOsculating
     *            is the orbital state in osculating parameters
     * @param timeShiftToInitialize
     *            DSST force model will be re-initialized every time the propagation date will be
     *            bigger than resetDate + timeShiftToInitialize. In seconds.
     * @param mass
     *            spacecraft mass
     * @throws OrekitException
     *             if an error occurs in orbit averaging (i.e when transforming osculating elements
     *             into mean elements)
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator,
                          final Orbit initialOrbit,
                          final boolean isOsculating,
                          final double timeShiftToInitialize,
                          final double mass)
                                            throws OrekitException {
        this(integrator, initialOrbit, isOsculating, timeShiftToInitialize, DEFAULT_LAW, mass);
    }

    /**
     * Build a DSSTPropagator from integrator, orbit, attitude provider and mass.
     * <p>
     * After creation, there are no perturbing forces at all. This means that if
     * {@link #addForceModel addForceModel} is not called after creation, the integrated orbit will
     * follow a keplerian evolution only.
     * </p>
     * 
     * @param integrator
     *            numerical integrator used to integrate mean coefficient defined by the SST theory.
     * @param initialOrbit
     *            initial orbit
     * @param isOsculating
     *            is the orbital state in osculating parameters
     * @param timeShiftToInitialize
     *            DSST force model will be re-initialized every time the propagation date will be
     *            bigger than resetDate + timeShiftToInitialize. In seconds.
     * @param attitudeProv
     *            attitude provider
     * @param mass
     *            spacecraft mass
     * @throws OrekitException
     *             if an error occurs in orbit averaging (i.e when transforming osculating elements
     *             into mean elements)
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator,
                          final Orbit initialOrbit,
                          final boolean isOsculating,
                          final double timeShiftToInitialize,
                          final AttitudeProvider attitudeProv,
                          final double mass)
                                            throws OrekitException {
        super(attitudeProv);
        this.forceModels = new ArrayList<DSSTForceModel>();
        this.mu = initialOrbit.getMu();
        this.frame = initialOrbit.getFrame();
        this.referenceDate = null;
        this.mass = mass;
        this.initialized = false;
        this.timeShiftToInitialize = timeShiftToInitialize;
        this.isOsculating = isOsculating;
        // Average osculating elements over 2 orbits
        this.satelliteRevolution = 2;
        setExtraTime(EXTRA_TIME);

        setIntegrator(integrator);

        PVCoordinatesProvider pvProv = new PVCoordinatesProvider() {
            public PVCoordinates getPVCoordinates(AbsoluteDate date,
                                                  Frame frame) throws OrekitException {
                return initialOrbit.getPVCoordinates();
            }
        };
        resetInitialState(new SpacecraftState(initialOrbit, attitudeProv.getAttitude(pvProv, initialOrbit.getDate(), initialOrbit.getFrame()), mass));
    }

    /**
     * Set the integrator.
     * 
     * @param integrator
     *            numerical integrator to use for propagation.
     */
    private void setIntegrator(final FirstOrderIntegrator integrator) {
        this.integrator = integrator;
        this.cumulator = new StepAccumulator();
        this.integrator.addStepHandler(cumulator);
    }

    /**
     * Reset the initial state
     * 
     * @param state
     *            new initial state
     * @throws PropagationException
     */
    public void resetInitialState(final SpacecraftState state) throws PropagationException {
        super.setStartDate(state.getDate());
        this.mass = state.getMass();
        this.referenceDate = state.getDate();
        this.cumulator.resetAccumulator();
        this.initialized = false;
        this.resetDate = state.getDate();
        super.resetInitialState(state);
    }

    /**
     * Add a force model to the global perturbation model.
     * <p>
     * If this method is not called at all, the integrated orbit will follow a keplerian evolution
     * only.
     * </p>
     * 
     * @param forcemodel
     *            perturbing {@link DSSTForceModel} to add
     * @see #removeForceModels()
     */
    public void addForceModel(final DSSTForceModel forcemodel) {
        forceModels.add(forcemodel);
    }

    /**
     * Remove all perturbing force models from the global perturbation model.
     * <p>
     * Once all perturbing forces have been removed (and as long as no new force model is added),
     * the integrated orbit will follow a keplerian evolution only.
     * </p>
     * 
     * @see #addForceModel(DSSTForceModel)
     */
    public void removeForceModels() {
        forceModels.clear();
    }

    /**
     * Set some extra duration to be performed for the first propagation of the orbit.
     * <p>
     * A reasonable value would be 5 times the initial step size of the integrator.
     * </p>
     * 
     * @param extraTime
     *            extra time
     */
    public void setExtraTime(final double extraTime) {
        this.extraTime = extraTime;
    }

    /**
     * Override the default value of the {@link DSSTPropagator#satelliteRevolution} parameter. By
     * default, if the given orbit is an osculating one, it will be averaged over a specific number
     * of revolution (2 revolution). This can be changed by using this method.
     * 
     * @param satelliteRevolution
     *            number of satellite revolution to use for averaging process (osculating to mean
     *            elements)
     */
    public void setSatelliteRevolution(final int satelliteRevolution) {
        this.satelliteRevolution = satelliteRevolution;
    }

    /**
     * Get the number of calls to the differential equations computation method.
     * <p>
     * The number of calls is reset each time the {@link #propagateOrbit(AbsoluteDate)} method is
     * called.
     * </p>
     * 
     * @return number of calls to the differential equations computation method
     */
    public int getCalls() {
        return calls;
    }

    /** {@inheritDoc} */
    protected double getMass(AbsoluteDate date) throws PropagationException {
        return mass;
    }

    @Override
    protected Orbit propagateOrbit(AbsoluteDate date) throws PropagationException {

        // Check for completeness
        if (integrator == null) {
            throw new PropagationException(OrekitMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION);
        }
        if (mass <= 0.0) {
            throw new IllegalArgumentException("Mass is null or negative");
        }

        try {

            // get current initial state and date
            final SpacecraftState initialState = getInitialState();
            final AbsoluteDate initialDate = initialState.getDate();

            if (initialDate.equals(date)) {
                // don't extrapolate, return current orbit
                return initialState.getOrbit();
            }

            // Check if the DSST needs to be initialized again :
            if (date.compareTo(resetDate) >= 0) {
                // Re-initialize every force model
                for (final DSSTForceModel forceModel : forceModels) {
                    forceModel.initialize(initialState);
                }
                resetDate = resetDate.shiftedBy(timeShiftToInitialize);
            }

            double[] meanElements;
            // Convert osculating to mean element
            if (!initialized && isOsculating) {
                Propagator propagator = createPropagator(initialState);

                SpacecraftState state = new OsculatingToMeanElementsConverter(initialState, satelliteRevolution, propagator).convert();
                meanElements = new double[] { state.getA(), state.getEquinoctialEx(), state.getEquinoctialEy(), state.getHx(),
                                state.getHy(), state.getLM() };
                initialized = true;
            } else {
                meanElements = new double[] { initialState.getA(), initialState.getEquinoctialEx(), initialState.getEquinoctialEy(),
                                initialState.getHx(), initialState.getHy(), initialState.getLM() };
            }

            // Initialize mean elements
            // double[] meanElements = getInitialMeanElements(initialState);

            // Propagate mean elements
            try {
                meanElements = extrapolate(initialDate, meanElements, date);
            } catch (OrekitExceptionWrapper oew) {
                throw new PropagationException(oew.getException());
            }

            // Add short periodic variations to mean elements to get osculating elements
            double[] osculatingElements = meanElements.clone();
            for (final DSSTForceModel forceModel : forceModels) {
                double[] shortPeriodicVariations = forceModel.getShortPeriodicVariations(date, meanElements);
                for (int i = 0; i < shortPeriodicVariations.length; i++) {
                    osculatingElements[i] += shortPeriodicVariations[i];
                }
            }

            return ORBIT_TYPE.mapArrayToOrbit(osculatingElements, ANGLE_TYPE, date, mu, frame);

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    private Propagator createPropagator(SpacecraftState initialState) throws IllegalArgumentException, OrekitException {
        final Orbit initialOrbit = initialState.getOrbit();
        final double[][] tol = NumericalPropagator.tolerances(1.0, initialOrbit, initialOrbit.getType());
        final double minStep = 1.;
        final double maxStep = 200.;
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        integrator.setInitialStepSize(100.);

        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(initialState);

        // Define the same force model as the DSST
        for (DSSTForceModel force : forceModels) {
            if (force instanceof DSSTCentralBody) {
                // Central body
                final double[][] cnm = ((DSSTCentralBody) force).getCnm();
                final double[][] snm = ((DSSTCentralBody) force).getSnm();
                final double ae = ((DSSTCentralBody) force).getAe();
                final ForceModel cunningham = new CunninghamAttractionModel(FramesFactory.getITRF2005(), ae, mu, cnm, snm);
                propagator.addForceModel(cunningham);
            } else if (force instanceof DSSTThirdBody) {
                // Third body
                final CelestialBody body = ((DSSTThirdBody) force).getBody();
                final ForceModel third = new ThirdBodyAttraction(body);
                propagator.addForceModel(third);
            } else if (force instanceof DSSTAtmosphericDrag) {
                // Atmospheric drag
                final double dragCoef = ((DSSTAtmosphericDrag) force).getCd();
                final double crossSec = ((DSSTAtmosphericDrag) force).getArea();
                final Atmosphere atm = ((DSSTAtmosphericDrag) force).getAtmosphere();
                final ForceModel drag = new DragForce(atm, new SphericalSpacecraft(dragCoef, crossSec, 0., 0.));
                propagator.addForceModel(drag);
            } else if (force instanceof DSSTSolarRadiationPressure) {
                // Solar radiation pressure
                final double ae = ((DSSTSolarRadiationPressure) force).getAe();
                double cr = ((DSSTSolarRadiationPressure) force).getCr();
                // Convert DSST convention to numerical's one
                cr = 1 + (1 - cr) * 2.25;
                final double area = ((DSSTSolarRadiationPressure) force).getArea();
                final SphericalSpacecraft scr = new SphericalSpacecraft(area, 0d, 0d, cr);
                final ForceModel pressure = new SolarRadiationPressure(CelestialBodyFactory.getSun(), ae, scr);
                propagator.addForceModel(pressure);
            }
        }
        return propagator;
    }

    /**
     * Compute initial mean elements from osculating elements.
     * 
     * @param state
     *            current state information: date, kinematics, attitude
     * @return mean elements
     * @throws OrekitException
     */
    private double[] getInitialMeanElements(final SpacecraftState state) throws OrekitException {

        final double[][] tolerances = DSSTPropagator.tolerances(POSITION_ERROR, state.getOrbit());
        double[] osculatingElements = new double[6];
        ORBIT_TYPE.mapOrbitToArray(state.getOrbit(), ANGLE_TYPE, osculatingElements);
        double[] meanElements = osculatingElements.clone();

        double epsilon;
        do {
            double[] meanPrec = meanElements.clone();
            double[] shortPeriodicVariations = new double[6];
            // Compute short periodic variations from current mean elements
            for (final DSSTForceModel forceModel : forceModels) {
                double[] spv = forceModel.getShortPeriodicVariations(state.getDate(), meanElements);
                for (int i = 0; i < shortPeriodicVariations.length; i++) {
                    shortPeriodicVariations[i] += spv[i];
                }
            }
            // Remove short periodic variations from osculating elements to get mean elements
            epsilon = 0.0;
            for (int i = 0; i < meanElements.length; i++) {
                meanElements[i] = osculatingElements[i] - shortPeriodicVariations[i];
                epsilon += FastMath.pow((meanElements[i] - meanPrec[i]) / tolerances[0][i], 2);
            }
            epsilon = FastMath.sqrt(epsilon);
        } while (epsilon > POSITION_ERROR);

        return meanElements;
    }

    /**
     * Extrapolation to tf.
     * 
     * @param start
     *            start date for extrapolation
     * @param startState
     *            state vector at start date
     * @param end
     *            end date for extrapolation
     * @return extrapolated state vector at end date
     * @throws PropagationException
     */
    private double[] extrapolate(final AbsoluteDate start,
                                 final double[] startState,
                                 final AbsoluteDate end) throws PropagationException {

        target = end;
        double t0 = start.durationFrom(referenceDate);
        double t1 = end.durationFrom(referenceDate);
        double[] stateIn = startState.clone();

        /** Step accumulation */
        SortedSet<StRange> steps = cumulator.getCumulatedSteps();

        if (target.compareTo(cumulator.getTd()) < 0 || target.compareTo(cumulator.getTf()) > 0) {
            double moreTime;
            if (!steps.isEmpty()) {
                StepInterpolator si;
                if (target.compareTo(cumulator.getTd()) < 0) {
                    t0 = cumulator.getTd().durationFrom(referenceDate);
                    si = steps.first().getStep();
                } else {
                    t0 = cumulator.getTf().durationFrom(referenceDate);
                    si = steps.last().getStep();
                }
                si.setInterpolatedTime(t0);
                stateIn = si.getInterpolatedState();
                moreTime = 5. * cumulator.getMaxStepSize();
            } else {
                moreTime = extraTime;
            }
            if (target.compareTo(referenceDate.shiftedBy(t0)) > 0) {
                t1 += moreTime;
            } else {
                t1 -= moreTime;
            }
            try {
                t1 = integrator.integrate(new DifferentialEquations(), t0, stateIn, t1, stateIn);
            } catch (OrekitExceptionWrapper oew) {
                throw new PropagationException(oew.getException());
            }
        }

        StepInterpolator si = steps.tailSet(new StRange(target)).first().getStep();
        si.setInterpolatedTime(target.durationFrom(referenceDate));
        return si.getInterpolatedState();
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations {

        /** Build a new instance. */
        public DifferentialEquations() {
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return 6;
        }

        /**
         * Initialize derivatives
         * 
         * @param yDot
         *            Derivatives array
         * @param currentOrbit
         *            current orbit
         */
        public void initDerivatives(final double[] yDot,
                                    final Orbit currentOrbit) {
            Arrays.fill(yDot, 0.0);
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t,
                                       final double[] y,
                                       final double[] yDot) throws OrekitExceptionWrapper {

            try {
                // update space dynamics view
                final AbsoluteDate date = referenceDate.shiftedBy(t);
                final Orbit orbit = ORBIT_TYPE.mapArrayToOrbit(y, ANGLE_TYPE, date, mu, frame);
                final Attitude attitude = getAttitudeProvider().getAttitude(orbit, date, frame);
                final SpacecraftState state = new SpacecraftState(orbit, attitude, mass);

                // initialize derivatives
                initDerivatives(yDot, orbit);

                // compute the contributions of all perturbing forces
                for (final DSSTForceModel forceModel : forceModels) {
                    final double[] daidt = forceModel.getMeanElementRate(state);
                    for (int i = 0; i < daidt.length; i++) {
                        yDot[i] += daidt[i];
                    }
                }

                // finalize derivatives by adding the Kepler contribution
                orbit.addKeplerContribution(ANGLE_TYPE, mu, yDot);

                // increment calls counter
                ++calls;

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }
    }

    /** Specialized step handler to add up all step interpolators. */
    private class StepAccumulator implements StepHandler {

        private SortedSet<StRange> cumulatedSteps;
        private AbsoluteDate       td;
        private AbsoluteDate       tf;
        private double             maxStep;

        /** Simple constructor. */
        public StepAccumulator() {
            cumulatedSteps = new TreeSet<StRange>();
            td = AbsoluteDate.FUTURE_INFINITY;
            tf = AbsoluteDate.PAST_INFINITY;
            maxStep = 0.;
        }

        /**
         * Get the maximum step size.
         * 
         * @return the maximum step size
         */
        public double getMaxStepSize() {
            return maxStep;
        }

        /**
         * Get the first time.
         * 
         * @return the first time
         */
        public AbsoluteDate getTd() {
            return td;
        }

        /**
         * Get the last time.
         * 
         * @return the last time
         */
        public AbsoluteDate getTf() {
            return tf;
        }

        /**
         * Get the cumulated step interpolators.
         * 
         * @return the cumulated step interpolators
         */
        public SortedSet<StRange> getCumulatedSteps() {
            return cumulatedSteps;
        }

        /**
         * Reset the accumulator: clear the cumulated steps and reinitialize the dates.
         */
        public void resetAccumulator() {
            cumulatedSteps.clear();
            td = AbsoluteDate.FUTURE_INFINITY;
            tf = AbsoluteDate.PAST_INFINITY;
            maxStep = 0.;
        }

        /** {@inheritDoc} */
        public void handleStep(StepInterpolator interpolator,
                               boolean isLast) {
            StRange sr = new StRange(interpolator);
            maxStep = FastMath.max(maxStep, sr.getTmax().durationFrom(sr.getTmin()));
            cumulatedSteps.add(sr);
            td = cumulatedSteps.first().getTmin();
            tf = cumulatedSteps.last().getTmax();
        }

        /** {@inheritDoc} */
        public void init(final double t0,
                         final double[] y0,
                         final double t) {
        }

    }

    /**
     * Internal class for step interpolator encapsulation before accumulation. This class allows
     * step interpolators ordering in a sorted set.
     */
    private class StRange implements Comparable<StRange>, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -6209093963711616737L;

        private AbsoluteDate      tmin;
        private AbsoluteDate      tmax;
        private StepInterpolator  step;

        /**
         * Constructor over a real step interpolator The step interpolator is copied inside the
         * StRange.
         * 
         * @param si
         *            step interpolator
         */
        public StRange(StepInterpolator si) {
            this.step = si.copy();
            final double dtmin = step.isForward() ? step.getPreviousTime() : step.getCurrentTime();
            final double dtmax = step.isForward() ? step.getCurrentTime() : step.getPreviousTime();
            this.tmin = referenceDate.shiftedBy(dtmin);
            this.tmax = referenceDate.shiftedBy(dtmax);
        }

        /**
         * Constructor over a single time
         * 
         * @param t
         *            time
         */
        public StRange(final AbsoluteDate t) {
            this.step = null;
            this.tmin = t;
            this.tmax = t;
        }

        /**
         * Get the min time in the range
         * 
         * @return the min time of the range
         */
        public AbsoluteDate getTmin() {
            return tmin;
        }

        /**
         * Get the max time in the range
         * 
         * @return the max time of the range
         */
        public AbsoluteDate getTmax() {
            return tmax;
        }

        /**
         * Get the step interpolator over the range
         * 
         * @return the step interpolator
         */
        public StepInterpolator getStep() {
            return step;
        }

        public int compareTo(StRange st) {
            if (this.tmax.compareTo(st.getTmin()) < 0) {
                return -1;
            } else if (this.tmin.compareTo(st.getTmin()) < 0 && this.tmax.compareTo(st.getTmax()) < 0) {
                return -1;
            } else if (this.tmin.compareTo(st.getTmin()) < 1 && this.tmax.compareTo(st.getTmax()) > -1) {
                return 0;
            } else if (st.getTmin().compareTo(this.tmin) < 1 && st.getTmax().compareTo(this.tmax) > -1) {
                return 0;
            } else if (st.getTmin().compareTo(this.tmin) < 0 && st.getTmax().compareTo(this.tmax) < 0) {
                return 1;
            } else { // if (st.getTmax().compareTo(this.tmin) < 0)
                return 1;
            }
        }
    }

    /**
     * Estimate tolerance vectors for an AdaptativeStepsizeIntegrator.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits, starting from a
     * scalar position error specified by the user. Considering the energy conservation equation V =
     * sqrt(mu (2/r - 1/a)), we get at constant energy (i.e. on a Keplerian trajectory):
     * 
     * <pre>
     * V<sup>2</sup> r |dV| = mu |dr|
     * </pre>
     * 
     * So we deduce a scalar velocity error consistent with the position error. From here, we apply
     * orbits Jacobians matrices to get consistent errors on orbital parameters.
     * </p>
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances are only
     * local estimates, not global ones. So some care must be taken when using these tolerances.
     * Setting 1mm as a position error does NOT mean the tolerances will guarantee a 1mm error
     * position after several orbits integration.
     * </p>
     * 
     * @param dP
     *            user specified position error
     * @param orbit
     *            reference orbit
     * @return a two rows array, row 0 being the absolute tolerance error and row 1 being the
     *         relative tolerance error
     */
    public static double[][] tolerances(final double dP,
                                        final Orbit orbit) {

        final double[][] numTol = NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);
        final double[][] dssTol = new double[2][6];
        System.arraycopy(numTol[0], 0, dssTol[0], 0, dssTol[0].length);
        System.arraycopy(numTol[1], 0, dssTol[1], 0, dssTol[1].length);

        return dssTol;

    }

}
