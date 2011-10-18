package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.LegendreGaussIntegrator;
import org.apache.commons.math.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math.analysis.integration.UnivariateRealIntegrator;
import org.apache.commons.math.analysis.integration.UnivariateRealIntegratorImpl;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;

/** Common handling of {@link DSSTForceModel} methods for Gaussian contributions to DSST propagation.
 * <p>
 * This abstract class allows to provide easily the full set of {@link DSSTForceModel} methods
 * for specific Gaussian contributions (i.e. atmospheric drag and solar radiation pressure).
 * </p><p>
 * Gaussian contributions can be expressed as: da<sub>i</sub>/dt = &delta;a<sub>i</sub>/&delta;v . q<br>
 * where:
 * <ul>
 * <li>a<sub>i</sub> are the six equinoctial elements</li>
 * <li>v is the velocity vector</li>
 * <li>q is the perturbing acceleration due to the considered force</li>
 * </ul>
 * The averaging process and other considerations lead to integrate this contribution
 * over the true longitude L possibly taking into account some limits.
 * </p><p>
 * Only two methods must be implemented by derived classes:
 * {@link #getAcceleration(SpacecraftState)} and {@link #getLLimits(SpacecraftState)}.
 * </p>
 * @author Pascal Parraud
 */
public abstract class AbstractDSSTGaussianContribution implements DSSTForceModel {

    // Quadrature parameters
    /** Number of points desired for quadrature (must be between 2 and 5 inclusive). */
    private final static int NB_POINTS = 3;
    /** Relative accuracy of the result. */
//    private final static double RELATIVE_ACCURACY = UnivariateRealIntegratorImpl.DEFAULT_RELATIVE_ACCURACY;
    private final static double RELATIVE_ACCURACY = 1.e-2;
    /** Absolute accuracy of the result. */
//    private final static double ABSOLUTE_ACCURACY = UnivariateRealIntegratorImpl.DEFAULT_ABSOLUTE_ACCURACY;
    private final static double ABSOLUTE_ACCURACY = MathUtils.SAFE_MIN;
    /** Minimum number of iterations. */
    private final static int MINIMAL_ITERATION_COUNT = UnivariateRealIntegratorImpl.DEFAULT_MIN_ITERATIONS_COUNT;
    /** Maximum number of iterations. */
    private final static int MAXIMAL_ITERATION_COUNT = UnivariateRealIntegratorImpl.DEFAULT_MAX_ITERATIONS_COUNT;
    /** Maximum number of evaluations. */
    private final static int MAX_EVAL = 50;

    /** Propagation orbit type. */
    private static final OrbitType orbitType = OrbitType.EQUINOCTIAL;

    /** Position angle type. */
    private static final PositionAngle angleType = PositionAngle.MEAN;

    /** DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination is close to zero.
     *  In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial elements can
     *  be expressed in a different way, called "retrograde" orbit. This implies I = -1.
     *  As Orekit doesn't implement the retrograde orbit, I = +1 here.
     */
    private double I = 1;

    /** Numerical quadrature operator. */
    private UnivariateRealIntegrator lgi;

    /** Build a new instance. */
    protected AbstractDSSTGaussianContribution() {
//        this.lgi = new LegendreGaussIntegrator(NB_POINTS, RELATIVE_ACCURACY, ABSOLUTE_ACCURACY,
//                                               MINIMAL_ITERATION_COUNT, MAXIMAL_ITERATION_COUNT);
        this.lgi = new SimpsonIntegrator();
    }

    /** Get the current retrograde factor I.
     *  @return the retrograde factor I
     */
    public double getRetrogradeFactor() {
        return I;
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState state) throws OrekitException {
        final double[] meanElementRate = new double[6];
        // Constant multiplier for integral
        final double h = state.getOrbit().getEquinoctialEy();
        final double k = state.getOrbit().getEquinoctialEx();
        final double coef = 1. / (2. * FastMath.PI * FastMath.sqrt(1. - h * h - k * k));
        final double[] ll = getLLimits(state);
        /// Define integrable functions
        final IntegrableFunction iFct = new IntegrableFunction(state);
        // Compute mean element rates
        for (int i = 0; i < 6; i++) {
            iFct.setElement(i);
            try {
                meanElementRate[i] = coef * lgi.integrate(MAX_EVAL, iFct, ll[0], ll[1]);
            } catch (OrekitExceptionWrapper oew) {
                throw oew.getException();
            }
        }

        return meanElementRate;    
    }

    /** Compute the acceleration due to the non conservative perturbing force.
     *
     *  @param state current state information: date, kinematics, attitude
     *  @param position spacecraft position
     *  @param velocity spacecraft velocity
     *  @return the perturbing acceleration 
     *  @exception OrekitException if some specific error occurs
     */
    protected abstract Vector3D getAcceleration(final SpacecraftState state,
                                                final Vector3D position,
                                                final Vector3D velocity) throws OrekitException;

    /** Compute the limits in L, the true longitude, for integration.
     *
     *  @param  state current state information: date, kinematics, attitude
     *  @return the integration limits in L
     *  @exception OrekitException if some specific error occurs
     */
    protected abstract double[] getLLimits(final SpacecraftState state) throws OrekitException;

    /** Internal class for numerical quadrature. */
    private class IntegrableFunction implements UnivariateRealFunction {

        /** Current state. */
        private SpacecraftState state;

        /** Current treated element. */
        private int element;

        // Equinoctial elements (according to DSST notation)
        /** a */
        private double a;
        /** ex */
        private double k;
        /** ey */
        private double h;
        /** hx */
        private double q;
        /** hy */
        private double p;

        // Kepler mean motion
        /** n = sqrt(&mu; / a<sup>3</sup>) */
        private double n;

        // Equinoctial reference frame vectors (according to DSST notation)
        /** f */
        private Vector3D f;
        /** g */
        private Vector3D g;
        /** w */
        private Vector3D w;

        // Useful equinoctial coefficients
        /** A = sqrt(&mu; * a) */
        private double A;
        /** B = sqrt(1 - h<sup>2</sup> - k<sup>2</sup>) */
        private double B;
        /** C = 1 + p<sup>2</sup> + q<sup>2</sup> */
        private double C;

        /** Build a new instance.
         *  @param  state current state information: date, kinematics, attitude
         */
        public IntegrableFunction(final SpacecraftState state) {
            this.state = state;
            // Compute equinoctial parameters
            computeParameters(state);
            // Set equinoctial element to a
            setElement(0);
        }

        /** Set the equinoctial element to consider for integration
         *  @param element equinoctial element indice (0: a ; 1: ex, 2: ey, 3: hx, 4: hy, 5: &lambda;)
         */
        public void setElement(final int element) {
            this.element = element;
        }
 
        /** {@inheritDoc} */
        public double value(double x) {
            double val = 0;
            final double cosL = FastMath.cos(x);
            final double sinL = FastMath.sin(x);
            final double roa  = B * B / (1 + h * sinL + k * cosL);
            final double roa2 = roa * roa;
            final double r    = a * roa;
            final double X    = r * cosL;
            final double Y    = r * sinL;
            final double Xdot = -n * a * (h + sinL) / B;
            final double Ydot =  n * a * (k + cosL) / B;
            Vector3D acc = Vector3D.ZERO;
            try {
                acc = getAcceleration(state, new Vector3D(X, f, Y, g), new Vector3D(Xdot, f, Ydot, g));
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
            switch(this.element) {
                case 1: // element dex/dt
                    val = roa2 * getKoV(X, Y, Xdot, Ydot).dotProduct(acc);
                    break;
                case 2: // element dey/dt
                    val = roa2 * getHoV(X, Y, Xdot, Ydot).dotProduct(acc);
                    break;
                case 3: // element dhx/dt
                    val = roa2 * getQoV(X).dotProduct(acc);
                    break;
                case 4: // element dhy/dt
                    val = roa2 * getPoV(Y).dotProduct(acc);
                    break;
                case 5: // element d&lambda;/dt
                    val = roa2 * getLoV(X, Y, Xdot, Ydot).dotProduct(acc);
                    break;
                default: // element da/dt
                    val = roa2 * getAoV(Xdot, Ydot).dotProduct(acc);
                    break;
            }
            return val;
        }

        /** Compute &delta;a/&delta;v
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;a/&delta;v
         */
        private Vector3D getAoV(final double Xdot, final double Ydot) {
            return new Vector3D(2. / (n * n * a), new Vector3D(Xdot, f, Ydot, g));
        }

        /** Compute &delta;h/&delta;v
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;h/&delta;v
         */
        private Vector3D getHoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = (2. * Xdot * Y - X * Ydot) / state.getMu();
            final double kg = X * Xdot / state.getMu();
            final double kw = k * (I * q * Y - p * X) / (A * B);
            return new Vector3D(kf, f, -kg, g, kw, w);
        }

        /** Compute &delta;k/&delta;v
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;k/&delta;v
         */
        private Vector3D getKoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = Y * Ydot / state.getMu();
            final double kg = (2. * X * Ydot - Xdot * Y) / state.getMu();
            final double kw = h * (I * q * Y - p * X) / (A * B);
            return new Vector3D(-kf, f, kg, g, -kw, w);
        }

        /** Compute &delta;p/&delta;v
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @return &delta;p/&delta;v
         */
        private Vector3D getPoV(final double Y) {
            return new Vector3D(C * Y / (2. * A * B), w);
        }

        /** Compute &delta;q/&delta;v
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @return &delta;q/&delta;v
         */
        private Vector3D getQoV(final double X) {
            return new Vector3D(I * C * X / (2. * A * B), w);
        }

        /** Compute &delta;&lambda;/&delta;v
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;&lambda;/&delta;v
         */
        private Vector3D getLoV(final double X, final double Y, final double Xdot, final double Ydot) {
            Vector3D pos = new Vector3D(X, f, Y, g);
            Vector3D v2  = new Vector3D(k, getHoV(X, Y, Xdot, Ydot), -h, getKoV(X, Y, Xdot, Ydot));
            return new Vector3D(-2. / A, pos, 1. / (1. + B), v2, (I * q * Y - p * X) / A, w);
        }

        /** Compute useful equinoctial parameters: A, B, C, f, g, w.
         *  @param  state current state information: date, kinematics, attitude
         */
        private void computeParameters(final SpacecraftState state) {
            // Initialisation of A, B, C coefficients, f, g, w basis

            // Get current state vector (equinoctial elements):
            double[] stateVector = new double[6];
            orbitType.mapOrbitToArray(state.getOrbit(), angleType, stateVector);

            // Equinoctial elements
            a = stateVector[0];
            k = stateVector[1];
            h = stateVector[2];
            q = stateVector[3];
            p = stateVector[4];

            // Factors
            final double k2 = k * k;
            final double h2 = h * h;
            final double q2 = q * q;
            final double p2 = p * p;

            // Equinoctial coefficients
            A = FastMath.sqrt(state.getMu() * a);
            B = FastMath.sqrt(1 - k2 - h2);
            C = 1 + q2 + p2;

            // Kepler mean motion
            n = A / (a * a);

            // Direction cosines
            f = new Vector3D( (1 - p2 + q2) / C,        2. * p * q / C,       -2. * I * p / C);
            g = new Vector3D(2. * I * p * q / C, I * (1 + p2 - q2) / C,            2. * q / C);
            w = new Vector3D(        2. * p / C,           -2. * q / C, I * (1 - p2 - q2) / C);
        }
        
    }
}
