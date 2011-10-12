package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.PropagationException;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
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
 * @author Romain Di Costanzo
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

    /** Counter for differential equations calls. */
    private int calls;

    /** State vector. */
    private double[] stateVector;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Current state to propagate. */
    private SpacecraftState currentState;

    /** Step handler used to keep integrator's coefficient for efficiency purpose. */
//    private StepHandler stepHandler;

    /** Current mass. */
    private double mass;

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
        isDirty       = true;
        forceModels   = new ArrayList<DSSTForceModel>();
        referenceDate = null;
        currentState  = null;
        stateVector   = new double[6];
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
    public void setIntegrator(final FirstOrderIntegrator integrator) {
        this.integrator = integrator;
    }

    /** {@inheritDoc} */
    public void resetInitialState(final SpacecraftState state)
        throws PropagationException {
        super.resetInitialState(state);
        this.mass = state.getMass();
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

    @Override
    protected Orbit propagateOrbit(AbsoluteDate date) throws PropagationException {


        if (integrator == null) {
            throw new PropagationException(OrekitMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION);
        }

        if (mass <= 0.0) {
            throw new IllegalArgumentException("Mass is null or negative");
        }

        try {
            if (getInitialState().getDate().equals(date)) {
                // don't extrapolate
                return getInitialState().getOrbit();
            }
            // get current state
            currentState = getInitialState();

            // space dynamics view
            referenceDate = currentState.getDate();

            if (isDirty) {
                for (DSSTForceModel force : forceModels) {
                    force.init(currentState);
                }
            }

            // mathematical view
            orbitType.mapOrbitToArray(currentState.getOrbit(), angleType, stateVector);
            final double t0 = 0;
            final double t1 = date.durationFrom(referenceDate);
            final double stopTime;
            try {
                stopTime = integrator.integrate(new DifferentialEquations(), t0, stateVector, t1, stateVector);
            } catch (OrekitExceptionWrapper oew) {
                throw new PropagationException(oew.getException());
            }

            // back to space dynamics view
            final AbsoluteDate stopDate = referenceDate.shiftedBy(stopTime);

            // Add short periodic variations to state vector
            for (final DSSTForceModel forceModel : forceModels) {
                double[] ySPV = forceModel.getShortPeriodicVariations(stopDate, stateVector);
                for (int i = 0; i < ySPV.length; i++) {
                    stateVector[i] += ySPV[i];
                }
            }
            
            return orbitType.mapArrayToOrbit(stateVector, angleType, stopDate,
                                             currentState.getMu(), currentState.getFrame());

        } catch (OrekitException oe) {
            throw new PropagationException(oe);
        }
    }

    /** {@inheritDoc} */
    protected double getMass(AbsoluteDate date) throws PropagationException {
        return mass;
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
                final AbsoluteDate currentDate = referenceDate.shiftedBy(t);
                final Orbit currentOrbit =
                        orbitType.mapArrayToOrbit(y, angleType, currentDate,
                                                  currentState.getMu(), currentState.getFrame());
                final Attitude currentAttitude =
                        getAttitudeProvider().getAttitude(currentOrbit, currentDate, currentState.getFrame());
                currentState = new SpacecraftState(currentOrbit, currentAttitude, mass);

                // initialize derivatives
                initDerivatives(yDot, currentState.getOrbit());

                // compute the contributions of all perturbing forces
                for (final DSSTForceModel forceModel : forceModels) {
                    double[] daidt = forceModel.getMeanElementRate(currentState);
                    for (int i = 0; i < daidt.length; i++) {
                        yDot[i] += daidt[i];
                    }
                }

                // finalize derivatives by adding the Kepler contribution
                currentState.getOrbit().addKeplerContribution(angleType, currentState.getMu(), yDot);

                // increment calls counter
                ++calls;

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
        }
    }

}
