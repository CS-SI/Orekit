package org.orekit.propagation.semianalytical.dsst;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.events.EventHandler;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.apache.commons.math.ode.sampling.StepInterpolator;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.time.AbsoluteDate;

/** This class propagates {@link org.orekit.orbits.Orbit orbits} using the DSST theory.
 * <p>
 * The DSST theory, as exposed by D.A.Danielson & al. (1995), describes a semianalytical
 * propagator that combines the accuracy of numerical propagators with the speed of
 * analytical propagators. Whereas analytical propagators are configured only thanks
 * to their various constructors and can be used immediately after construction,
 * such a semianalytical propagator configuration involves setting several parameters
 * between construction time and propagation time, just as numerical propagators.
 * </p>
 * <p>
 * The configuration parameters that can be set are:</p>
 * <ul>
 *   <li>the initial spacecraft state ({@link #setInitialState(SpacecraftState)})</li>
 *   <li>the various force models ({@link #addForceModel(DSSTForceModel)},
 *   {@link #removeForceModels()})</li>
 *   <li>the discrete events that should be triggered during propagation
 *   ({@link #addEventDetector(EventDetector)}, {@link #clearEventsDetectors()})</li>
 *   <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 *   {@link #setMasterMode(double, OrekitFixedStepHandler)}, {@link
 *   #setMasterMode(OrekitStepHandler)}, {@link #setEphemerisMode()}, {@link
 *   #getGeneratedEphemeris()})</li>
 * </ul>
 * </p>
 * <p>
 * From these configuration parameters, only the initial state is mandatory. The default
 * propagation settings are in {@link OrbitType#EQUINOCTIAL equinoctial} parameters with
 * {@link PositionAngle#TRUE true} longitude argument. The central attraction coefficient
 * used to define the initial orbit will be used. However, specifying only the initial
 * state would mean the propagator would use only keplerian forces. In this case, the
 * simpler {@link org.orekit.propagation.analytical.KeplerianPropagator KeplerianPropagator}
 * class would perhaps be more effective.
 * </p>
 * <p>
 * The underlying numerical integrator set up in the constructor may also have its own
 * configuration parameters. Typical configuration parameters for adaptive stepsize integrators
 * are the min, max and perhaps start step size as well as the absolute and/or relative errors
 * thresholds.
 * </p>
 * <p>The state that is seen by the integrator is a simple six elements double array.
 * These six elements are:
 * <ul>
 *   <li>the {@link org.orekit.orbits.EquinoctialOrbit equinoctial orbit parameters} (a, e<sub>x</sub>,
 *   e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, &lambda;<sub>v</sub>) in meters and radians,</li>
 * </ul>
 * </p>
 * <p>The same propagator can be reused for several orbit extrapolations, by resetting
 * the initial state without modifying the other configuration parameters. However, the
 * same instance cannot be used simultaneously by different threads, the class is <em>not</em>
 * thread-safe.
 * </p>

 * @see SpacecraftState
 * @see DSSTForceModel
 *
 * @author Romain Di Constanzo
 * @author Pascal Parraud
 */
public class DSSTPropagator extends AbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = -1217566398912634178L;

    /** Propagation orbit type. */
    private static final OrbitType orbitType = OrbitType.EQUINOCTIAL;

    /** Position angle type. */
    private static final PositionAngle angleType = PositionAngle.MEAN;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private transient FirstOrderIntegrator integrator;

    /** Force models used during the extrapolation of the Orbit. */
    private final List<DSSTForceModel> forceModels;

    /** Step accumulator. */
    private StepAccumulator cumulator;

    /** Trigger. */
    private Trigger trigger;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Target date. */
    private AbsoluteDate target;

    /** Current &mu;. */
    private double mu;

    /** Current frame. */
    private Frame frame;

    /** Current mass. */
    private double mass;

    /** Counter for differential equations calls. */
    private int calls;

    /** is the current propagator state dirty ? i.e. needs initialization */
    private boolean isDirty;

    /** Build a DSSTPropagator from integrator and orbit.
     *  <p>Mass and attitude provider are set to unspecified non-null arbitrary values.</p>
     *  <p>
     *  After creation, there are no perturbing forces at all. This means that
     *  if {@link #addForceModel addForceModel} is not called after creation,
     *  the integrated orbit will follow a keplerian evolution only.
     *  </p>
     *  @param integrator numerical integrator used to integrate mean coefficient defined by the SST theory.
     *  @param initialOrbit initial orbit
     *  @exception PropagationException if initial state cannot be set
     */  
    public DSSTPropagator(final FirstOrderIntegrator integrator, final Orbit initialOrbit)
        throws PropagationException {
        this(integrator, initialOrbit, DEFAULT_LAW, DEFAULT_MASS);
    }

    /** Build a DSSTPropagator from integrator, orbit and attitude provider.
     *  <p>Mass is set to an unspecified non-null arbitrary value.</p>
     *  <p>
     *  After creation, there are no perturbing forces at all. This means that
     *  if {@link #addForceModel addForceModel} is not called after creation,
     *  the integrated orbit will follow a keplerian evolution only.
     *  </p>
     *  @param integrator numerical integrator used to integrate mean coefficient defined by the SST theory.
     *  @param initialOrbit initial orbit
     *  @param attitudeProv attitude provider
     *  @exception PropagationException if initial state cannot be set
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator, final Orbit initialOrbit,
                          final AttitudeProvider attitudeProv)
        throws PropagationException {
        this(integrator, initialOrbit, attitudeProv, DEFAULT_MASS);
    }

    /** Build a DSSTPropagator from integrator, orbit and mass.
     * <p>Attitude provider is set to an unspecified non-null arbitrary value.</p>
     *  <p>
     *  After creation, there are no perturbing forces at all. This means that
     *  if {@link #addForceModel addForceModel} is not called after creation,
     *  the integrated orbit will follow a keplerian evolution only.
     *  </p>
     *  @param integrator numerical integrator used to integrate mean coefficient defined by the SST theory.
     *  @param initialOrbit initial orbit
     *  @param mass spacecraft mass
     *  @exception PropagationException if initial state cannot be set
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator, final Orbit initialOrbit,
                          final double mass)
        throws PropagationException {
        this(integrator, initialOrbit, DEFAULT_LAW, mass);
    }

    /** Build a DSSTPropagator from integrator, orbit, attitude provider and mass.
     *  <p>
     *  After creation, there are no perturbing forces at all. This means that
     *  if {@link #addForceModel addForceModel} is not called after creation,
     *  the integrated orbit will follow a keplerian evolution only.
     *  </p>
     *  @param integrator numerical integrator used to integrate mean coefficient defined by the SST theory.
     *  @param initialOrbit initial orbit
     *  @param attitudeProv attitude provider
     *  @param mass spacecraft mass
     *  @exception PropagationException if initial state cannot be set
     */
    public DSSTPropagator(final FirstOrderIntegrator integrator, final Orbit initialOrbit,
                          final AttitudeProvider attitudeProv, final double mass)
        throws PropagationException {
        super(attitudeProv);
        forceModels   = new ArrayList<DSSTForceModel>();
        mu            = initialOrbit.getMu();
        frame         = initialOrbit.getFrame();
        referenceDate = null;

        setIntegrator(integrator);

        try {
            resetInitialState(new SpacecraftState(initialOrbit,
                                                  attitudeProv.getAttitude(getPvProvider(), initialOrbit.getDate(), initialOrbit.getFrame()),
                                                  mass));
        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }

    }

    /** Set the integrator.
     * @param integrator numerical integrator to use for propagation.
     */
    private void setIntegrator(final FirstOrderIntegrator integrator) {
        this.integrator = integrator;
        this.cumulator  = new StepAccumulator();
        this.trigger    = new Trigger();
        this.integrator.addStepHandler(cumulator);
        this.integrator.addEventHandler(trigger, Double.POSITIVE_INFINITY, 1.e-4, 100);
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        super.resetInitialState(state);
        this.isDirty = true;
        this.mass = state.getMass();
        this.referenceDate = state.getDate();
        this.cumulator.resetAccumulator();
    }

    /** Add a force model to the global perturbation model.
     *  <p>
     *  If this method is not called at all, the integrated orbit
     *  will follow a keplerian evolution only.
     *  </p>
     *  @param model perturbing {@link DSSTForceModel} to add
     *  @see #removeForceModels()
     */
    public void addForceModel(final DSSTForceModel forcemodel) {
        forceModels.add(forcemodel);
    }

    /** Remove all perturbing force models from the global perturbation model.
     *  <p>
     *  Once all perturbing forces have been removed (and as long as no new
     *  force model is added), the integrated orbit will follow a keplerian
     *  evolution only.
     *  </p>
     *  @see #addForceModel(DSSTForceModel)
     */
    public void removeForceModels() {
        forceModels.clear();
    }

    /** Get the number of calls to the differential equations computation method.
     * <p>
     * The number of calls is reset each time the {@link #propagateOrbit(AbsoluteDate)}
     * method is called.
     * </p>
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
                // don't extrapolate
                return initialState.getOrbit();
            }

            // Initialize force models if needed (i.e. after resetting initial state)
            if (isDirty) {
                for (DSSTForceModel force : forceModels) {
                    force.init(initialState);
                }
                isDirty = false;
            }
            
            // mathematical view
            double[] stateIn = new double[6];
            double[] stateOut = new double[6];
            orbitType.mapOrbitToArray(initialState.getOrbit(), angleType, stateIn);
            try {
                stateOut = extrapolate(initialDate, stateIn, date);
            } catch (OrekitExceptionWrapper oew) {
                throw new PropagationException(oew.getException());
            }

            // Add short periodic variations to state vector
            for (final DSSTForceModel forceModel : forceModels) {
                double[] ySPV = forceModel.getShortPeriodicVariations(date, stateOut);
                for (int i = 0; i < ySPV.length; i++) {
                    stateOut[i] += ySPV[i];
                }
            }
            
            return orbitType.mapArrayToOrbit(stateOut, angleType, date, mu, frame);

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** Extrapolation to tf.
     *  @param start start date for extrapolation
     *  @param startState state vector at start date
     *  @param end end date for extrapolation
     *  @return extrapolated state vector at end date
     *  @throws PropagationException 
     */
    private double[] extrapolate(final AbsoluteDate start, final double[] startState, final AbsoluteDate end)
        throws PropagationException {

        target = end;
        double t0 = start.durationFrom(referenceDate);
        double t1 = end.durationFrom(referenceDate);
        double[] stateIn = startState.clone();

        /** Step accumulation */
        SortedSet<StRange> steps = cumulator.getCumulatedSteps();

        if (target.compareTo(cumulator.getTd()) < 0 || target.compareTo(cumulator.getTf()) > 0) {
            if (!steps.isEmpty() && target.compareTo(cumulator.getTd()) < 0) {
                t0 = cumulator.getTd().durationFrom(referenceDate);
                StepInterpolator si = steps.first().getStep();
                si.setInterpolatedTime(t0);
                stateIn = si.getInterpolatedState();
            } else if (!steps.isEmpty() && target.compareTo(cumulator.getTf()) > 0) {
                t0 = cumulator.getTf().durationFrom(referenceDate);
                StepInterpolator si = steps.last().getStep();
                si.setInterpolatedTime(t0);
                stateIn = si.getInterpolatedState();
            }
            if (target.compareTo(referenceDate.shiftedBy(t0)) > 0) {
                t1 += 864000.;
            } else {
                t1 -= 864000.;
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

        /** {@inheritDoc} */
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit) {
            Arrays.fill(yDot, 0.0);
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t, final double[] y, final double[] yDot)
            throws OrekitExceptionWrapper {

            try {
                // update space dynamics view
                final AbsoluteDate date = referenceDate.shiftedBy(t);
                final Orbit orbit = orbitType.mapArrayToOrbit(y, angleType, date, mu, frame);
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
                orbit.addKeplerContribution(angleType, mu, yDot);

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
        private AbsoluteDate td;
        private AbsoluteDate tf;

        /** Simple constructor. */
        public StepAccumulator() {
            cumulatedSteps = new TreeSet<StRange>();
            td = AbsoluteDate.FUTURE_INFINITY;
            tf = AbsoluteDate.PAST_INFINITY;
        }
        
        /** Get the first time.
         * @return the first time
         */
        public AbsoluteDate getTd() {
            return td;
        }
        
        /** Get the last time.
         * @return the last time
         */
        public AbsoluteDate getTf() {
            return tf;
        }

        /** Get the cumulated step interpolators.
         * @return the cumulated step interpolators
         */
        public SortedSet<StRange> getCumulatedSteps() {
            return cumulatedSteps;
        }

        /** Reset the accumulator: clear the cumulated steps and reinitialize the dates.
         */
        public void resetAccumulator() {
            cumulatedSteps.clear();
            td = AbsoluteDate.FUTURE_INFINITY;
            tf = AbsoluteDate.PAST_INFINITY;
        }

        @Override
        public void handleStep(StepInterpolator interpolator, boolean isLast) {
            StRange sr = new StRange(interpolator);
            cumulatedSteps.add(sr);
            td = cumulatedSteps.first().getTmin();
            tf = cumulatedSteps.last().getTmax();
            if (sr.compareTo(new StRange(target)) == 0) {
                double tstop = target.durationFrom(referenceDate) + interpolator.getCurrentTime() - interpolator.getPreviousTime();
                trigger.addEvent(tstop);
            }
        }

        @Override
        public void reset() {
        }
    }

    /** Internal class for step interpolator encapsulation before accumulation.
     *  This class allows step interpolators ordering in a sorted set.
     */
    private class StRange implements Comparable<StRange>, Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -6209093963711616737L;

        private AbsoluteDate tmin;
        private AbsoluteDate tmax;
        private StepInterpolator step;

        /** Constructor over a real step interpolator 
         * The step interpolator is copied inside the StRange.
         * @param si step interpolator
         */
        public StRange(StepInterpolator si) {
            this.step = si.copy();
            final double dtmin = step.isForward() ? step.getPreviousTime() : step.getCurrentTime();
            final double dtmax = step.isForward() ? step.getCurrentTime() : step.getPreviousTime();
            this.tmin = referenceDate.shiftedBy(dtmin);
            this.tmax = referenceDate.shiftedBy(dtmax);
        }

        /** Constructor over a single time 
         * @param t time
         */
        public StRange(final AbsoluteDate t) {
            this.step = null;
            this.tmin = t;
            this.tmax = t;
        }

        /** Get the min time in the range
         * @return the min time of the range
         */
        public AbsoluteDate getTmin() {
            return tmin;
        }
        
        /** Get the max time in the range
         * @return the max time of the range
         */
        public AbsoluteDate getTmax() {
            return tmax;
        }
        
        /** Get the step interpolator over the range
         * @return the step interpolator
         */
        public StepInterpolator getStep() {
            return step;
        }

        @Override
        public int compareTo(StRange st) {
            if (this.tmax.compareTo(st.getTmin()) < 0) {
                return -1;
            } else if (this.tmin.compareTo(st.getTmin()) < 0 & this.tmax.compareTo(st.getTmax()) < 0) {
                return -1;
            } else if (this.tmin.compareTo(st.getTmin()) < 1 & this.tmax.compareTo(st.getTmax()) > -1) {
                return 0;
            } else if (st.getTmin().compareTo(this.tmin) < 1 & st.getTmax().compareTo(this.tmax) > -1) {
                return 0;
            } else if (st.getTmin().compareTo(this.tmin) < 0 & st.getTmax().compareTo(this.tmax) < 0) {
                return 1;
            } else { // if (st.getTmax().compareTo(this.tmin) < 0)
                return 1;
            }
        }
    }

    /** Finder for timed event.
     * <p>This class finds timed event (i.e. occurrence of some predefined time).</p>
     * <p>It is a kind of delayed trigger:</p>
     * <ul>
     *   <li>it is defined without prior target ({@link #Trigger()})</li>
     *   <li>several time targets can be added later ({@link #addEvent(double)})</li>
     * </ul>
     * <p>The default implementation behavior is to {@link EventHandler.Action#STOP stop}
     * propagation at the first event time occurrence. This can be changed by
     * overriding the {@link #eventOccurred(double, double[], boolean) eventOccurred}
     * method in a derived class.</p>
     */
    private static class Trigger implements EventHandler {

        /** Last time for g computation. */
        private double gTime;

        /** List of event dates. */
        private final ArrayList<EventTime> eventTimeList;

        /** Current event time. */
        private int currentIndex;

        /** Build a new instance.
         * <p>This constructor is dedicated to time detection
         * when the event time is not known before propagating.
         * It can be triggered later by adding some event time,
         * it then acts like a timer.</p>
         * @see #addEvent(double)
         */
        public Trigger() {
            this.eventTimeList = new ArrayList<EventTime>();
            this.currentIndex = -1;
            this.gTime = Double.NaN;
        }

        @Override
        public double g(double t, double[] y) {
            gTime = t;
            if (currentIndex < 0) {
                return -1.0;
            } else {
                final EventTime et = getClosest(gTime);
                return et.isgIncrease() ? (gTime - et.getTime()) : (et.getTime() - gTime);
            }
        }

        @Override
        public Action eventOccurred(double t, double[] y, boolean increasing) {
            return Action.STOP;
        }

        @Override
        public void resetState(double t, double[] y) {
            return;
        }

        /** Add an event time.
         * @param target target time
         */
        public void addEvent(final double target) {
            final boolean increasing;
            if (currentIndex < 0) {
                increasing = Double.isNaN(gTime) ? true : target > gTime;
                currentIndex = 0;
                eventTimeList.add(new EventTime(target, increasing));
            } else {
                final int lastIndex = eventTimeList.size() - 1;
                if (eventTimeList.get(0).getTime() > target) {
                    increasing = !eventTimeList.get(0).isgIncrease();
                    eventTimeList.add(0, new EventTime(target, increasing));
                    currentIndex++;
                } else if (target > eventTimeList.get(lastIndex).getTime()) {
                    increasing = !eventTimeList.get(lastIndex).isgIncrease();
                    eventTimeList.add(new EventTime(target, increasing));
                } else {
                    return;
                }
            }
        }

        /** Get the closest EventTime to the target time.
         * @param target target time
         * @return current EventTime
         */
        private EventTime getClosest(final double target) {
            final double dt = target - eventTimeList.get(currentIndex).getTime();
            if (dt < 0.0 && currentIndex > 0) {
                boolean found = false;
                while (currentIndex > 0 && !found) {
                    if (target - eventTimeList.get(currentIndex - 1).getTime() < eventTimeList.get(currentIndex).getTime() - target) {
                        currentIndex--;
                    } else {
                        found = true;
                    }
                }
            } else if (dt > 0.0 && currentIndex < eventTimeList.size() - 1) {
                final int maxIndex = eventTimeList.size() - 1;
                boolean found = false;
                while (currentIndex < maxIndex && !found) {
                    if (target - eventTimeList.get(currentIndex + 1).getTime() > eventTimeList.get(currentIndex).getTime() - target) {
                        currentIndex++;
                    } else {
                        found = true;
                    }
                }
            }
            return eventTimeList.get(currentIndex);
        }

        /** Event time specification. */
        private static class EventTime {

            /** Event time. */
            private final double eventTime;

            /** Flag for g function way around event time. */
            private final boolean gIncrease;

            /** Simple constructor.
             * @param t time
             * @param increase if true, g function increases around event date
             */
            public EventTime(final double t, final boolean increase) {
                this.eventTime = t;
                this.gIncrease = increase;
            }

            /** Getter for event date.
             * @return event date
             */
            public double getTime() {
                return eventTime;
            }

            /** Getter for g function way at event time.
             * @return g function increasing flag
             */
            public boolean isgIncrease() {
                return gIncrease;
            }

        }

    }

    /** Estimate tolerance vectors for an AdaptativeStepsizeIntegrator.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits,
     * starting from a scalar position error specified by the user.
     * Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)),
     * we get at constant energy (i.e. on a Keplerian trajectory):
     * <pre>
     * V<sup>2</sup> r |dV| = mu |dr|
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
     * @return a two rows array, row 0 being the absolute tolerance error and row 1
     * being the relative tolerance error
     */
    public static double[][] tolerances(final double dP, final Orbit orbit) {

        return NumericalPropagator.tolerances(dP, orbit, OrbitType.EQUINOCTIAL);

    }

}
