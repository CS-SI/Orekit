package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;

public class DSSTCentralBody implements DSSTForceModel {

    /**
     * General data
     */
    /** Equatorial radius of the Central Body. */
    private final double     ae;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private double           mu;

    /** First normalized potential tesseral coefficients array. */
    private final double[][] Cnm;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][] Snm;

    /** Degree of potential. */
    private final int        degree;

    /** Order of potential. */
    private final int        order;

    /** I = -1 if the orbit is retrograde, +1 otherwise */
    private double           I;

    private double[]         state;

    /**
     * Equinoctial coefficients
     */
    /** A = sqrt(&mu * a) */
    private double           A;

    /** B = sqrt(1 - ex<sup>2</sup> - ey<sup>2</sup> */
    private double           B;

    /** C = 1 + hx<sup>2</sup> + hx<sup>2</sup> */
    private double           C;

    /**
     * Direction cosines of the symmetry axis
     */
    /** &alpha */
    private double           alpha;

    /** &beta */
    private double           beta;

    /** &gamma */
    private double           gamma;

    /**
     * Poisson brackets : they are defined by the equations : <br>
     * (a<sub>i</sub>, a<sub>j</sub>) = da<sub>i</sub> / dr * da<sub>j</sub> / drDot - da<sub>i</sub> / drDot *
     * da<sub>j</sub> / dr
     */
    /** (a, &lambda) */
    private double           aLambda;

    /** (e<sub>x</sub>, e<sub>y</sub>) */
    private double           exey;

    /** (e<sub>x</sub>, h<sub>x</sub>) */
    private double           exhx;

    /** (e<sub>x</sub>, h<sub>y</sub>) */
    private double           exhy;

    /** (e<sub>x</sub>, &lambda) */
    private double           exLambda;

    /** (e<sub>y</sub>, h<sub>x</sub>) */
    private double           eyhx;

    /** (e<sub>y</sub>, h<sub>y</sub>) */
    private double           eyhy;

    /** (e<sub>y</sub>, &lambda) */
    private double           eyLambda;

    /** (h<sub>x</sub>, h<sub>y</sub>) */
    private double           hxhy;

    /** (h<sub>x</sub>, &lambda) */
    private double           hxLambda;

    /** (h<sub>y</sub>, &lambda) */
    private double           hyLambda;

    /**
     * Coefficient used to define the mean disturbing function
     */
    /** V<sub>ns</sub> coefficient */
    private double[][]       Vns;

    /** Geopotential coefficient Jn = -Cn0 */
    private double[]         Jn;

    /**
     * TODO mettre que mu provient du bulletin orbital et est sauvegarder lors de l'initialisation faite par
     * {@link DSSTPropagator} pour eviter une incohérence (1 mu ici, un autre dans le bulletin orbital)
     * 
     * @param ae
     * @param Cnm
     * @param Snm
     */
    public DSSTCentralBody(double ae,
                           double[][] Cnm,
                           double[][] Snm) {
        this.Cnm = Cnm;
        this.Snm = Snm;
        degree = Cnm.length - 1;
        order = Cnm[degree].length - 1;
        if ((Cnm.length != Snm.length) || (Cnm[Cnm.length - 1].length != Snm[Snm.length - 1].length)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH,
                                                                 Cnm.length,
                                                                 Cnm[degree].length,
                                                                 Snm.length,
                                                                 Snm[degree].length);
        }
        this.ae = ae;
        // Initialize the Jn component
        initializeJn(Cnm);
        initializeVnsCoefficient();
    }

    @Override
    public double[] getMeanElementRate(AbsoluteDate date,
                                       double[] currentState) {
        // Store current state :
        state = currentState;
        // Initialize the A, B, C variables and the direct cosines alpha, beta, gamma
        computeAlphaBetaGamma(currentState);
        // Compute non constant coefficient which depends on previous initialized data :
        double[][] Kns = computeKnsCoefficient();
        double[][] dKns = computeKnsDerivatives(Kns);
        double[][] Qns = computeQnsCoefficient();
        double[][] GsHs = computeGsHsCoefficient(currentState[1], currentState[2]);

        // Compute potential derivative :
        double[] potentialDerivatives = computePotentialderivatives(Kns, dKns, Qns, GsHs);

        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double[] getShortPeriodicVariations(AbsoluteDate date,
                                               double[] currentState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void init(final Orbit initialState,
                     final AbsoluteDate referenceDate) {

        this.mu = initialState.getMu();

        // Check if the orbit is retrograde :
        I = (initialState.getI() > (Math.PI * 0.5)) ? -1 : 1;
    }

    /**
     * Get the potential derivatives needed for the central body gravitational zonal harmonics at first order. As those
     * equations depend on the &alpha; &beta; and &gamma; values, they only can be evaluated after those data have been
     * computed (from the current state). See equation 3.1 - (6) from the main paper. <br>
     * The result is an array containing the following data : <br>
     * dU / da <br>
     * dU / de<sub>x</sub> <br>
     * dU /de<sub>y</sub> <br>
     * dU / d&alpha; <br>
     * dU/ d&beta; <br>
     * dU/d&gamma;<br>
     * Where U is the gravitational potential.
     * 
     * @param
     * @param gs
     * @param qns
     * @param kns
     * @return data needed for the potential derivatives
     */
    private double[] computePotentialderivatives(double[][] Kns,
                                                 double[][] dKns,
                                                 double[][] Qns,
                                                 double[][] GsHs) {

        // Initialize data
        final double a = state[0];
        final double ex = state[1];
        final double ey = state[2];
        final double hx = state[3];
        final double hy = state[4];
        final double v = state[5];
        final double factor = -this.mu / (a * a);
        final double Ra = ae / a;

        // Potential derivatives
        double dUda = 0d;
        double dUdex = 0d;
        double dUdey = 0d;
        double dUdal = 0d;
        double dUdbe = 0d;
        double dUdga = 0d;

        // dGs / dEx
        double dGsdEx = 0d;
        // dGs / dEy
        double dGsdEy = 0d;
        // dGs / dAlpha
        double dGsdAl = 0d;
        // dGs / dBeta
        double dGsdBe = 0d;

        // series coefficient :
        double jn;
        double vns;
        double kns;
        double qns;
        double gs;
        // (R / a)^n
        double ran;
        // Kronecker symbol (2 - delta(0,s))
        double delta0s = 0d;

        for (int s = 0; s < order - 2; s++) {
            if (s == 0) {
                delta0s = 1;
            } else {
                delta0s = 2;
                dGsdEx = s * beta * GsHs
            }

            // Get current gs
            gs = GsHs[0][s];

            for (int n = s + 2; n < order; n++) {
                // Extract data from previous computation :
                jn = Jn[n];
                vns = Vns[n][s];
                kns = Kns[n][s];
                qns = Qns[n][s];
                ran = FastMath.pow(Ra, n);

                // Compute dU / da :
                dUda += delta0s * (n + 1) * ran * jn * vns * kns * qns * gs;

            }
        }

        return null;
    }

    /**
     * @param Cnm
     */
    private void initializeJn(double[][] Cnm) {
        Jn = new double[degree];
        for (int i = 0; i < degree; i++) {
            Jn[i] = -Cnm[i][0];
        }
    }

    /**
     * 
     */
    private void initializeVnsCoefficient() {
        // Initialization
        Vns = new double[order][degree];
        Vns[0][0] = 1;
        Vns[1][1] = 0.5;
        Vns[2][0] = -0.5;

        for (int s = 0; s < order - 2; s++) {
            Vns[s + 1][s + 1] = 1 / Vns[s][s] / (2 * s + 2);
            for (int n = s + 2; n < order; n++) {
                // Null if n - s is odd
                if ((n - s) % 2 != 0) {
                    Vns[n][s] = 0d;
                } else {
                    Vns[n + 2][s] = (-n + s - 1) / (n + s + 2) * Vns[n][s];
                }
            }
        }
    }

    /**
     * @param initialState
     */
    private void computeAlphaBetaGamma(double[] initialState) {
        // Factor declaration
        final double a = initialState[0];
        final double ex = initialState[1];
        final double ey = initialState[2];
        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double hx = initialState[3];
        final double hy = initialState[4];
        final double hx2 = hx * hx;
        final double hy2 = hy * hy;

        A = FastMath.sqrt(mu * a);
        B = FastMath.sqrt(1 - ex2 - ey2);
        C = 1 + hx2 + hy2;

        // Direction cosines :
        alpha = -(2 * I * hx) / (1 + hx2 + hy2);
        beta = 2 * hy / (1 + hx2 + hy2);
        gamma = (1 - hx2 - hy2) * I / (1 + hx2 + hy2);

    }

    /**
     * Kernels of Hansen coefficients
     */
    private double[][] computeKnsCoefficient() {
        // Initialization :
        double[][] Kns = new double[order][degree];
        Kns[0][0] = 1;
        Kns[0][1] = -1;

        double khi = 1 / B;

        // Compute coefficients
        for (int s = 0; s < order - 2; s++) {
            for (int n = s + 2; n < order; n++) {
                if (n == s && s >= 0) {
                    Kns[-n - 1][s] = 0d;
                } else if (n == (s + 1) && n >= 1) {
                    Kns[-n - 1][s] = FastMath.pow(khi, 1 + 2 * s) / FastMath.pow(2, s);
                } else if (n >= s + 2 && n >= 2) {
                    Kns[-n - 1][s] = ((n - 1) * khi * khi / ((n + s - 1) * (n - s - 1)))
                                    * ((2 * n - 3) * Kns[-n][s] - (n - 2) * Kns[-n + 1][s]);
                }
            }
        }
        return Kns;
    }

    private double[][] computeKnsDerivatives(double[][] Kns) {
        // Initialization :
        double[][] dKns = new double[order][degree];

        double khi = 1 / B;

        // Compute coefficients
        for (int s = 0; s < order - 2; s++) {
            for (int n = s + 2; n < order; n++) {
                if (n == s) {
                    dKns[-n - 1][s] = 0d;
                } else if (n == s + 1) {
                    dKns[-n - 1][s] = (1 + 2 * s) * FastMath.pow(khi, 2 * s) / FastMath.pow(2, s);
                } else if (n > s + 1) {
                    dKns[-n - 1][s] = ((n - 1) * khi * khi / ((n + s - 1) * (n - s + 1)))
                                    * ((2 * n - 3) * dKns[-n][s] - (n - 2) * dKns[-n + 1][s]) + 2 * Kns[-n - 1][s]
                                    / khi;
                }
            }
        }
        return dKns;

    }

    private double[][] computeGsHsCoefficient(final double ex,
                                              final double ey) {

        // Initialization
        double[][] GsHs = new double[1][order];
        // First Gs coefficient
        GsHs[0][0] = 1;
        // First Hs coefficient
        GsHs[1][0] = 0;

        for (int s = 1; s < order - 2; s++) {
            // Gs coefficient :
            GsHs[0][s] = (ey * alpha + ex * beta) * GsHs[0][s - 1] - (ex * alpha - ey * beta) * GsHs[1][s - 1];
            // Hs coefficient
            GsHs[1][s] = (ex * alpha - ey * beta) * GsHs[0][s - 1] + (ey * alpha + ex * beta) * GsHs[1][s - 1];
        }

        return GsHs;
    }

    /**
     * Q<sub>ns</sub> coefficient
     */
    private double[][] computeQnsCoefficient() {
        // Initialization
        double[][] Qns = new double[order][degree];
        Qns[0][0] = 1;

        for (int s = 0; s < order - 2; s++) {
            for (int n = s; n < order; n++) {
                if (n == s) {
                    Qns[n][s] = (2 * s - 1) * Qns[s - 1][s - 1];
                } else if (n == s + 1) {
                    Qns[n][s] = (2 * s + 1) * gamma * Qns[s][s];
                } else if (n > s + 1) {
                    Qns[n][s] = (2 * n - 1) * gamma * Qns[n - 1][s] - (n + s - 1) * Qns[n - 2][s];
                }
            }
        }
        return Qns;
    }

}
