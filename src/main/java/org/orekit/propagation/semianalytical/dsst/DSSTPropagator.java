package org.orekit.propagation.semianalytical.dsst;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math.ode.FirstOrderIntegrator;
import org.apache.commons.math.ode.sampling.StepHandler;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.DSSTForceModel;
import org.orekit.time.AbsoluteDate;

public class DSSTPropagator extends AbstractPropagator {

    /** First order integrator */
    public final FirstOrderIntegrator integrator;

    /**
     * Step handler used to keep integrator's coefficient for efficiency purpose
     */
    public StepHandler                stepHandler;

    /** Force model list */
    public List<DSSTForceModel>       forceModels = new ArrayList<DSSTForceModel>();

    /** State vector. */
    private double[]                  stateVector;

    /** is the current propagator state dirty ? i.e. needs initialization */
    private boolean                   isDirty;

    /** Counter for differential equations calls. */
    private int                       calls;

    /** Reference date. */
    private AbsoluteDate              referenceDate;

    private SpacecraftState           currentState;

    private final Orbit               initialState;

    private final Frame               orbitFrame;

    /**
     * Default constructor
     * 
     * @param integrator
     *            integrator used to integrate mean coefficient defined by the SST theory
     */
    public DSSTPropagator(final Orbit orbit,
                          final FirstOrderIntegrator integrator) {
        super(DEFAULT_LAW);
        this.isDirty = true;
        this.referenceDate = null;
        this.integrator = integrator;
        this.initialState = orbit;
        this.orbitFrame = orbit.getFrame();
        this.referenceDate = orbit.getDate();
    }

    /** {@inheritDoc} */
    @Override
    protected Orbit propagateOrbit(AbsoluteDate date) throws PropagationException {
        if (isDirty) {
            for (DSSTForceModel force : forceModels) {
                force.init(initialState, referenceDate);
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected double getMass(AbsoluteDate date) throws PropagationException {
        // TODO Auto-generated method stub
        return 0;
    }

    public void addForceModel(final DSSTForceModel forcemodel) {
        forceModels.add(forcemodel);
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = -1927530118454989452L;

        /** Reference to the derivatives array to initialize. */
        private double[]          storedYDot;

        /** Build a new instance. */
        public DifferentialEquations() {
            calls = 0;
        }

        /** {@inheritDoc} */
        public int getDimension() {
            return 6;
        }

        /** {@inheritDoc} */
        public void computeDerivatives(final double t,
                                       final double[] y,
                                       final double[] yDot) throws OrekitExceptionWrapper {

            // update space dynamics view
            final AbsoluteDate currentDate = referenceDate.shiftedBy(t);
            double[] storedYDot;
            // compute the contributions of all perturbing forces
            for (final DSSTForceModel forceModel : forceModels) {
                storedYDot = forceModel.getMeanElementRate(currentDate, y);
                for (int i = 0; i < storedYDot.length; i++) {
                    yDot[i] += storedYDot[i];
                }
            }

            // increment calls counter
            ++calls;

        }
    }
}
