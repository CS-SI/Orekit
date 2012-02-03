package org.orekit.propagation;

import org.apache.commons.math.analysis.DifferentiableUnivariateFunction;
import org.apache.commons.math.analysis.UnivariateFunction;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.solvers.NewtonSolver;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.PropagationException;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

/**
 * This class converts osculating orbital elements into mean elements. As this process depends on
 * the force model used to average the orbit, a {@link Propagator} is given as input. The force
 * model used will be the one contained into the propagator.
 * 
 * @author rdicosta
 */
public class OsculatingToMeanElementsConverter {

    /** Initial orbit to convert */
    private SpacecraftState  state;

    /** Number of satellite revolutions in the averaging interval */
    private int              satelliteRevolution;

    /** Propagator used to compute mean orbit */
    private Propagator       propagator;

    /** Integrator maximum evaluation */
    private static final int MAX_EVALUATION = 1000;

    /**
     * Constructor.
     * 
     * @param state
     *            initial orbit to convert
     * @param satelliteRevolution
     *            number of satellite revolutions in the averaging interval
     * @param forceModel
     *            Force model used for averaging
     * @throws OrekitException
     */
    public OsculatingToMeanElementsConverter(final SpacecraftState state,
                                             final int satelliteRevolution,
                                             final Propagator propagator)
                                                                         throws OrekitException {
        this.state = state;
        this.satelliteRevolution = satelliteRevolution;
        this.propagator = propagator;
    }

    /**
     * Convert an osculating orbit into a mean orbit, in DSST sense.
     * 
     * @param state
     *            initial orbit to convert
     * @param n
     *            number of satellite revolutions in the averaging interval
     * @return mean orbit, in DSST sense
     * @throws OrekitException
     * @throws IllegalArgumentException
     */
    public final SpacecraftState convert() throws IllegalArgumentException, OrekitException {
        // Tolerance list
         double[][] tolerance = NumericalPropagator.tolerances(1e-3, state.getOrbit(), state.getOrbit().getType());
        final double aMean = computeSemiMajorAxis(state, tolerance[0][0], tolerance[1][0]);
        // Get eccentric longitude
        final double l = state.getLE();
        double[] results = new double[5];

        final double delta = satelliteRevolution * Math.PI;

        QuatratureComputation quadrature = new QuatratureComputation(state);
        for (int i = 0; i < 5; i++) {
            results[i] = quadrature.solve(i, l - delta, l + delta, tolerance[0][i + 1], tolerance[1][i + 1]);
        }
        return new SpacecraftState(new EquinoctialOrbit(aMean, results[0], results[1], results[2], results[3], results[4], PositionAngle.ECCENTRIC, state.getFrame(), state.getDate(), state.getMu()));
    }

    /**
     * Compute the semi-major axis from a quadrature evaluation.
     * 
     * @param state
     *            state
     * @param absoluteTolerance
     *            absolute tolerance
     * @param relativeTolerance
     *            relative tolerance
     * @return mean semi-major axis
     * @throws IllegalArgumentException
     * @throws OrekitException
     */
    private double computeSemiMajorAxis(final SpacecraftState state,
                                        final double absoluteTolerance,
                                        final double relativeTolerance) throws IllegalArgumentException, OrekitException {
        final double a = state.getA();
        final SemiMajorAxisFunction function = new SemiMajorAxisFunction(state, absoluteTolerance, relativeTolerance);
        final NewtonSolverImp solver = new NewtonSolverImp(function);
        final double aMean = solver.solve(MAX_EVALUATION, function, a);
        return aMean;
    }

    /**
     * Function used in semi-amjor axis computation. Semi-major computation is done through a
     * Newton-Raphson iteration procedure. Initialization is done for A<sub>0</sub> = a<sub>0</sub>,
     * where a<sub>0</sub> is the initial orbit semi-major axis
     */
    private class SemiMajorAxisFunction implements DifferentiableUnivariateFunction {

        /** Satellite initial state */
        private final SpacecraftState state;

        /** Period function used to compute the semi major axis evolution */
        private final AlphaFunction   periodFunction;

        /** &alpha; = &int;<sub>t-T</sub><sup>t+T</sup> a(t')dt) */
        private double                alpha;

        /** Integrator used */
        private SimpsonIntegrator     integrator;

        /**
         * The integration is done between [t - &delta;, t + &delta;] where &delta; = n *
         * &tau;<sub>i</sub> / 2, n beeing the number of satellite revolutions in the average
         * interval, &tau;<sub>i</sub> the i<sup>th</sup> iteration in mean satellite orbital period
         */
        private double                delta;

        /**
         * {@link DifferentiableUnivariateFunction} used to compute the semi-major axis
         * 
         * @param state
         *            initial state
         * @param absoluteTolerance
         *            absolute tolerance
         * @param relativeTolerance
         *            relative tolerance
         * @throws PropagationException
         *             if an exception occurs in orbit propagation
         */
        private SemiMajorAxisFunction(final SpacecraftState state,
                                      final double absoluteTolerance,
                                      final double relativeTolerance)
                                                                     throws PropagationException {
            this.state = state;
            this.delta = satelliteRevolution * state.getKeplerianPeriod() * 0.5;
            this.periodFunction = new AlphaFunction(state, satelliteRevolution, state.getKeplerianPeriod());
            this.integrator = new SimpsonIntegrator(relativeTolerance, absoluteTolerance, SimpsonIntegrator.DEFAULT_MIN_ITERATIONS_COUNT, SimpsonIntegrator.SIMPSON_MAX_ITERATIONS_COUNT);
        }

        /** Set the satellite period at i<sup>th</sup> iteration */
        public final void setDelta(double delta) {
            this.delta = delta;
        }

        /**
         * {@inheritDoc}
         */
        public double value(double x) {
            /** Compute alpha */
            final double num = 1 / (2 * delta);
            alpha = integrator.integrate(MAX_EVALUATION, periodFunction, -delta, delta) * num;
            return x - alpha;
        }

        /**
         * {@inheritDoc}
         */
        public UnivariateFunction derivative() {
            return new UnivariateFunction() {

                public double value(double x) {
                    final double aPlus = periodFunction.value(delta);
                    final double aMinus = periodFunction.value(-delta);
                    return 1 + 0.75 * (2 * alpha - aPlus - aMinus) / x;
                }
            };
        }
    }

    /**
     * Specific implementation of the Newton solver. This solver updates the SemiMajorAxisFunction
     * for next computation.
     */
    public class NewtonSolverImp extends NewtonSolver {

        /** {@link DifferentiableUnivariateFunction} */
        private SemiMajorAxisFunction function;

        /** Gravity constant */
        private double                mu;

        /**
         * Newton solver
         * 
         * @param function
         *            function to solve
         */
        private NewtonSolverImp(SemiMajorAxisFunction function) {
            this.function = function;
            this.mu = function.state.getMu();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double doSolve() {
            final double startValue = getStartValue();
            final double absoluteAccuracy = getAbsoluteAccuracy();

            double x0 = startValue;
            double x1;
            while (true) {
                x1 = x0 - (computeObjectiveValue(x0) / computeDerivativeObjectiveValue(x0));
                if (FastMath.abs(x1 - x0) <= absoluteAccuracy) {
                    return x1;
                }
                x0 = x1;
                // Update the delta value
                function.setDelta(satelliteRevolution * 2.0 * FastMath.PI * x0 * FastMath.sqrt(x0 / mu) * 0.5);
            }
        }
    }

    /**
     * Compute &alpha; = &int;<sub>t-T</sub><sup>t+T</sup> a(t')dt)
     */
    private class AlphaFunction implements UnivariateFunction {

        /** Initial date */
        private AbsoluteDate      initialDate;

        /** Generated ephemeris. This is used for performances purpose */
        private BoundedPropagator ephemeris;

        /**
         * Constructor
         * 
         * @param orbitalPeriod
         *            orbital period
         * @param satelliteRevolution
         *            Number of satellite revolutions in the averaging interval
         * @throws PropagationException
         *             if an exception occurs in orbit propagation
         */
        private AlphaFunction(final SpacecraftState initialState,
                              final int satelliteRevolution,
                              final double orbitalPeriod)
                                                         throws PropagationException {
            this.initialDate = initialState.getDate();
            propagator.setEphemerisMode();
            double delta = satelliteRevolution * orbitalPeriod * 0.5;
            // Take a 10% security margin
            delta += 1e-2 * delta;
            propagator.propagate(initialDate.shiftedBy(-delta), initialDate.shiftedBy(delta));
            ephemeris = propagator.getGeneratedEphemeris();
        }

        /** Get the semi-major axis at specific date */
        public double value(double x) {
            try {
                return ephemeris.propagate(initialDate.shiftedBy(x)).getA();
            } catch (PropagationException e) {
                throw new OrekitExceptionWrapper(e);
            }
        }
    }

    /**
     * Quadrature used to average orbital parameters
     */
    private class QuatratureComputation implements UnivariateFunction {

        /** Initial date */
        private AbsoluteDate initialDate;

        /** Index of orbital element */
        private int          index;

        /**
         * Initialize the quadrature computation
         * 
         * @param state
         *            initial state
         */
        private QuatratureComputation(final SpacecraftState state) {
            this.initialDate = state.getDate();

        }

        /**
         * Integrate the function from lower to upper interval
         * 
         * @param index
         *            orbital element index
         * @param lower
         *            lower bound
         * @param upper
         *            upper bound
         * @param absoluteTolerance
         *            absolute tolerance
         * @param relativeTolerance
         *            relative tolerance
         * @return element averaged
         */
        public double solve(final int index,
                            final double lower,
                            final double upper,
                            final double absoluteTolerance,
                            final double relativeTolerance) {
            this.index = index;
            SimpsonIntegrator simpson = new SimpsonIntegrator(relativeTolerance, absoluteTolerance, SimpsonIntegrator.DEFAULT_MIN_ITERATIONS_COUNT, SimpsonIntegrator.SIMPSON_MAX_ITERATIONS_COUNT);
            final double integral = simpson.integrate(MAX_EVALUATION, this, lower, upper);
            return integral / (MathUtils.TWO_PI * satelliteRevolution);
        }

        /**
         * {@inheritDoc}
         */
        public double value(double x) {
            try {
                SpacecraftState currentState = propagator.getGeneratedEphemeris().propagate(initialDate.shiftedBy(x));
                final double h = currentState.getEquinoctialEy();
                final double k = currentState.getEquinoctialEx();
                final double p = currentState.getHy();
                final double q = currentState.getHx();
                final double l = currentState.getLE();
                final double[] elements = new double[] { k, h, q, p, l };
                final double elmt = elements[index];

                final double cosF = FastMath.cos(l);
                final double sinF = FastMath.sin(l);
                return elmt * (1 - h * sinF - k * cosF);

            } catch (PropagationException e) {
                throw new OrekitExceptionWrapper(e);
            }
        }
    }
}
