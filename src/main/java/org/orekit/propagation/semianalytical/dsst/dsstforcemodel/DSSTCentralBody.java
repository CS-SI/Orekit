package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.ArithmeticUtils;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class DSSTCentralBody implements DSSTForceModel {

    /**
     * General data
     */
    /** Equatorial radius of the Central Body. */
    private final double           ae;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private double                 mu;

    /** First normalized potential tesseral coefficients array. */
    private final double[][]       Cnm;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][]       Snm;

    /** Degree <i>n</i> of C<sub>nm</sub> potential. */
    private final int              degree;

    /** Order <i>m</i> of C<sub>nm</sub> potential. */
    private final int              order;

    /**
     * DSST model needs equinoctial orbit as internal representation. Classical equinoctial elements
     * have discontinuities when inclination is close to zero. In this representation, I = +1. <br>
     * To avoid this discontinuity, another representation exists and equinoctial elements can be
     * expressed in a different way, called "retrograde" orbit. This implies I = -1. As Orekit
     * doesn't implement the retrograde orbit, I = 1 here.
     */
    private int                    I = 1;

    /** current orbital state */
    private Orbit                  orbit;

    /**
     * Equinoctial coefficients
     */
    /** A = sqrt(&mu; * a) */
    private double                 A;

    /** B = sqrt(1 - ex<sup>2</sup> - ey<sup>2</sup> */
    private double                 B;

    /** C = 1 + hx<sup>2</sup> + hx<sup>2</sup> */
    private double                 C;

    /**
     * Direction cosines of the symmetry axis
     */
    /** &alpha */
    private double                 alpha;

    /** &beta */
    private double                 beta;

    /** &gamma */
    private double                 gamma;

    /** Coefficient used to define the mean disturbing function V<sub>ns</sub> coefficient */
    private TreeMap<NSKey, Double> Vns;

    /** Geopotential coefficient Jn = -Cn0 */
    private double[]               Jn;

    /** Resonant tesseral coefficient */
    private List<ResonantCouple>   resonantTesseralsTerm;

    /** size of the resonnant tesseral terms. If no terms are defined, the size is set to 0 */
    private final int              resonantTesseralSize;

    private HansenCoefficients     hansen;

    /**
     * convergence parameter used in Hansen coefficient generation. 1e-4 seems to be a correct
     * value.
     */
    private final double           epsilon;

    /**
     * TODO mettre que mu provient du bulletin orbital et est sauvegarder lors de l'initialisation
     * faite par {@link DSSTPropagator} pour eviter une incohérence (1 mu ici, un autre dans le
     * bulletin orbital)
     * 
     * @param ae
     * @param Cnm
     * @param Snm
     * @param resonantTesserals
     *            resonant term
     * @param epsilon
     *            convergence parameter used in Hansen coefficient generation. 1e-4 seems to be a
     *            correct value
     */
    public DSSTCentralBody(double ae,
                           double[][] Cnm,
                           double[][] Snm,
                           List<ResonantCouple> resonantTesserals,
                           final double epsilon) {
        this.Cnm = Cnm;
        this.Snm = Snm;
        this.degree = Cnm.length - 1;
        this.order = Cnm[degree].length - 1;
        this.resonantTesseralsTerm = resonantTesserals;
        this.epsilon = epsilon;
        if ((Cnm.length != Snm.length) || (Cnm[Cnm.length - 1].length != Snm[Snm.length - 1].length)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH, Cnm.length, Cnm[degree].length, Snm.length, Snm[degree].length);
        }
        this.ae = ae;
        // Initialize the constant component
        initializeJn(Cnm);
        Vns = CoefficientFactory.computeVnsCoefficient(order + 1);
        resonantTesseralSize = (resonantTesserals == null) ? 0 : resonantTesserals.size();
    }

    /**
     * {@inheritDoc} From equation 3.1 - (1)
     * 
     * @throws OrekitException
     */
    @Override
    public double[] getMeanElementRate(SpacecraftState spacecraftState) throws OrekitException {

        // Store current state :
        orbit = spacecraftState.getOrbit();
        // Initialisation of A, B, C, Alpha, Beta and Gamma coefficient :
        updateABCAlphaBetaGamma(orbit);
        hansen = new HansenCoefficients(orbit.getE(), epsilon);

        // Get zonal harmonics contributuion :
        ZonalHarmonics zonalHarmonics = new ZonalHarmonics();
        double[] zonalTerms = zonalHarmonics.getZonalContribution(orbit);
        // System.out.println(" zonal " + zonalTerms[0] + " " + zonalTerms[1] + " " + zonalTerms[2]
        // + " " + zonalTerms[3] + " "
        // + zonalTerms[4] + " " + zonalTerms[5] + " ");

        // Get tesseral resonant harmonics contributuion :
        TesseralResonantHarmonics tesseralHarmonics = new TesseralResonantHarmonics();
        double[] tesseralTerms = tesseralHarmonics.getResonantContribution(orbit);
        // System.out.println(" tesseralTerms " + tesseralTerms[0] + " " + tesseralTerms[1] + " " +
        // tesseralTerms[2] + " " + tesseralTerms[3]
        // + " " + tesseralTerms[4] + " " + tesseralTerms[5] + " ");

        double[] meanElementRate = new double[zonalTerms.length];
        for (int i = 0; i < zonalTerms.length; i++) {
            meanElementRate[i] = tesseralTerms[i] + zonalTerms[i];
        }

        return meanElementRate;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date, final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet
        // Short Periodic Variations are set to null
        return new double[] {0.,0.,0.,0.,0.,0.};
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState) {
        this.mu = initialState.getMu();
    }

    /**
     * Initialize the J<sub>n</sub> geopotential coefficients. See page 55 of the Danielson paper.
     * J<sub>n</sub> = - C<sub>n0</sub>
     * 
     * @param Cnm
     *            Geopotential coefficient
     */
    private void initializeJn(double[][] Cnm) {
        Jn = new double[degree + 1];
        for (int i = 0; i <= degree; i++) {
            Jn[i] = -Cnm[i][0];
        }
    }

    // /** Initialize the V<sub>n, s</sub> <sup>m</sup> coefficient from 2.7.2 -(2)(3) */
    // private void initializeVnsmCoefficient() {
    // final int mMax = resonantTesserals.size();
    // // V[m][s][n]
    // this.Vmsn = new double[][][] {};
    // double num;
    // double den;
    //
    // // Initialization :
    // Vmsn[0][0][0] = 1;
    //
    // // Case where n = s :
    // for (int s = 0; s < order; s++) {
    // Vmsn[0][s + 1][s + 1] = ((2 * s + 1) * Vmsn[0][s][s]) / (s + 1);
    // // Case when m > 0 and n = s
    // for (int m = 1; m < mMax; m++) {
    // Vmsn[m][s + 1][s + 1] = (s - m) * Vmsn[m][s][s];
    // // Case for non negative m and s and increasing n
    // for (int n = 0; n < degree; n++) {
    // num = -(n + s + 1) * (n - s + 1) * Vmsn[m][s][s];
    // den = (n - m + 2) * (n - m + 1);
    // Vmsn[m][s][n + 2] = num / den;
    // }
    // }
    // }
    // }

    /**
     * @param initialState
     */
    private void updateABCAlphaBetaGamma(final Orbit orbit) {
        // Factor declaration
        final double a = orbit.getA();
        final double k = orbit.getEquinoctialEx();
        final double h = orbit.getEquinoctialEy();
        final double k2 = k * k;
        final double h2 = h * h;
        final double q = orbit.getHx();
        final double p = orbit.getHy();
        final double q2 = q * q;
        final double p2 = p * p;

        A = FastMath.sqrt(mu * a);
        B = FastMath.sqrt(1 - k2 - h2);
        C = 1 + q2 + p2;

        // Direction cosines :
        Vector3D[] equinoctialFrame = computeEquinoctialReferenceFrame(orbit);
        alpha = equinoctialFrame[0].getZ();
        beta = equinoctialFrame[1].getZ();
        gamma = equinoctialFrame[2].getZ();
    }

    /**
     * Compute the equinoctioal reference frame defined by the (f, g, w) vector. f and g lie in the
     * satellite orbit plane. w is parallel to the angular momentum vector of the satellite.
     * 
     * @param orbit
     *            orbit
     * @return the equinoctial reference frame
     */
    private Vector3D[] computeEquinoctialReferenceFrame(final Orbit orbit) {
        // Factor declaration
        final double q = orbit.getHx();
        final double p = orbit.getHy();
        final double q2 = q * q;
        final double p2 = p * p;

        final double num = (1 + q2 + p2);

        // compute the f vector :
        final double fx = 1 - p2 - q2;
        final double fy = 2 * p * q;
        final double fz = -2 * I * p;
        Vector3D f = new Vector3D(1 / num, new Vector3D(fx, fy, fz));

        // Compute the g vector :
        final double gx = 2 * I * p * q;
        final double gy = (1 + p2 - q2) * I;
        final double gz = 2 * q;
        Vector3D g = new Vector3D(1 / num, new Vector3D(gx, gy, gz));

        // Compute the w vector :
        Vector3D w = Vector3D.crossProduct(f, g);
        return new Vector3D[] { f, g, w };
    }

    /**
     * Compute the &theta; angle for the current orbit. The &theta; angle is the central body
     * rotation angle, defined from the equinoctial reference frame. See equation 2.7.1 - (3)(4)
     * 
     * @param orbit
     *            current orbital state
     * @return the central body rotation angle
     */
    private double computeThetaAngle(final Orbit orbit) {
        Vector3D[] frame = computeEquinoctialReferenceFrame(orbit);
        final Vector3D f = frame[0];
        final Vector3D g = frame[1];

        final double num = -f.getY() + I * g.getX();
        final double den = f.getX() + I * g.getY();

        return FastMath.atan2(num, den);
    }

    /**
     * Get the zonal contribution of the central body for the first order mean element rates
     */
    private class ZonalHarmonics {

        private ZonalHarmonics() {
            // Dummy constructor, nothing to do.
        }

        private double[] getZonalContribution(Orbit orbit) throws OrekitException {
            // Initialization :
            double dh = 0d;
            double dk = 0d;
            double dp = 0d;
            double dq = 0d;
            double dM = 0d;

            double a = orbit.getA();
            double k = orbit.getEquinoctialEx();
            double h = orbit.getEquinoctialEy();
            double q = orbit.getHx();
            double p = orbit.getHy();

            final double[][] GsHs = CoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, order + 1);
            final double[][] Qns = CoefficientFactory.computeQnsCoefficient(gamma, order + 1);
            
            
            /**
             * analytic expression of J2
             */
            double J = 3 * mu * Constants.WGS84_EARTH_EQUATORIAL_RADIUS * Constants.WGS84_EARTH_EQUATORIAL_RADIUS * Cnm[2][2] / 4d;
             double dhdt = J * k * (3 * gamma * gamma - 1 + 2 * gamma * (alpha * p - beta * q)) / (A * Math.pow(B, 4) * Math.pow(orbit.getA(), 3));
            System.out.println("dhdt " + dhdt);

            double dudh = 3 * J * h * (gamma * gamma - 1 / 3d) / (Math.pow(a, 3) * Math.pow(1 - h * h - k * k,2.5));
            double dudk = 3 * J * k * (gamma * gamma - 1 / 3d) / (Math.pow(a, 3) * Math.pow(1 - h * h - k * k,2.5));
            
            System.out.println("dudh " + dudh);
            System.out.println("dudk " + dudk);

            

            // Compute potential derivative :
            final double[] potentialDerivatives = computePotentialderivatives(Qns, GsHs);

            final double dUda = potentialDerivatives[0];
            final double dUdk = potentialDerivatives[1];
            final double dUdh = potentialDerivatives[2];
            final double dUdAl = potentialDerivatives[3];
            final double dUdBe = potentialDerivatives[4];
            final double dUdGa = potentialDerivatives[5];

            // Compute cross derivatives from formula 2.2 - (8):
            // U(alpha,gamma) = alpha * du / dgamma - gamma * du / dalpha
            final double UAlphaGamma = alpha * dUdGa - gamma * dUdAl;
            // U(beta,gamma) = beta * du / dgamma - gamma * du / dbeta
            final double UBetaGamma = beta * dUdGa - gamma * dUdBe;

            final double factor = (p * UAlphaGamma - I * q * UBetaGamma) / (A * B);

            // Compute mean element Rate for Zonal Harmonic :
            // da / dt = 0 for zonal harmonic :
            dh = (B / A) * dUdk + k * factor;
            dk = -(B / A) * dUdh - h * factor;
            dp = -C / (2 * A * B) * UBetaGamma;
            dq = -I * C * UAlphaGamma / (2 * A * B);
            dM = (-2 * a * dUda / A) + (B / (A * (1 + B))) * (h * dUdh + k * dUdk) + (p * UAlphaGamma - I * q * UBetaGamma) / (A * B);

            return new double[] { 0d, dk, dh, dq, dp, dM };
        }

        /**
         * Get the potential derivatives needed for the central body gravitational zonal harmonics
         * at first order. As those equations depend on the &alpha; &beta; and &gamma; values, they
         * only can be evaluated after those data have been computed (from the current state). See
         * equation 3.1 - (6) from the main paper. <br>
         * The result is an array containing the following data : <br>
         * dU / da <br>
         * dU / dk <br>
         * dU / dh <br>
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
         * @throws OrekitException
         */
        private double[] computePotentialderivatives(double[][] Qns,
                                                     double[][] GsHs) throws OrekitException {

            // Initialize data
            final double a = orbit.getA();
            final double k = orbit.getEquinoctialEx();
            final double h = orbit.getEquinoctialEy();

            // mu / a
            final double factor = mu / a;
            final double Ra = ae / a;
            final double khi = 1 / B;

            // Potential derivatives
            double dUda = 0d;
            double dUdk = 0d;
            double dUdh = 0d;
            double dUdAl = 0d;
            double dUdBe = 0d;
            double dUdGa = 0d;

            // dGs / dk
            double dGsdk = 0d;
            // dGs / dh
            double dGsdh = 0d;
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
            double gsM1;
            double hsM1;
            double dkns;

            // Other data :
            // (R / a)^n
            double raExpN;
            double khi3 = FastMath.pow(khi, 3);
            double commonCoefficient;
            // Kronecker symbol (2 - delta(0,s))
            double delta0s = 0d;

            for (int s = 0; s < order - 1; s++) {
                // Get the current gs and hs coefficient :
                gs = GsHs[0][s];

                // Get the G(s-1) and the H(s-1) coefficient : SET TO 0 IF NULL !! TODO to chek
                gsM1 = (s > 0 ? GsHs[0][s - 1] : 0);
                hsM1 = (s > 0 ? GsHs[1][s - 1] : 0);

                // Compute partial derivatives of GsHs
                dGsdk = s * beta * gsM1 - s * alpha * hsM1;
                dGsdh = s * alpha * gsM1 + s * beta * hsM1;
                dGsdAl = s * k * gsM1 - s * h * hsM1;
                dGsdBe = s * h * gsM1 + s * k * hsM1;

                // Compute Partial derivatives of Gs from equ. (9)
                delta0s = (s == 0) ? 1 : 2;

                for (int n = s + 2; n <= order; n++) {
                    // Extract data from previous computation :
                    jn = Jn[n];
                    vns = Vns.get(new NSKey(n, s));
                    kns = hansen.getHansenKernelValue(0, -n - 1, s);
                    qns = Qns[n][s];
                    raExpN = FastMath.pow(Ra, n);
                    dkns = hansen.getHansenKernelDerivative(0, -n - 1, s);
                    commonCoefficient = delta0s * raExpN * jn * vns;

                    // Compute dU / da :
                    dUda += commonCoefficient * (n + 1) * kns * qns * gs;
                    // Compute dU / dEx
                    dUdk += commonCoefficient * qns * (kns * dGsdk + k * khi3 * dkns);
                    // Compute dU / dEy
                    dUdh += commonCoefficient * qns * (kns * dGsdh + h * khi3 * gs * dkns);
                    // Compute dU / dAlpha
                    dUdAl += commonCoefficient * qns * kns * dGsdAl;
                    // Compute dU / dBeta
                    dUdBe += commonCoefficient * qns * kns * dGsdBe;
                    // Compute dU / dGamma : here dQns/dGamma = Q(n, s + 1) from Equation 3.1 - (8)
                    dUdGa += commonCoefficient * kns * Qns[n][s + 1] * gs;
                }
            }

            dUda *= (factor / a);
            dUdk *= -factor;
            dUdh *= -factor;
            dUdAl *= -factor;
            dUdBe *= -factor;
            dUdGa *= -factor;

            return new double[] { dUda, dUdk, dUdh, dUdAl, dUdBe, dUdGa };
        }
    }

    /**
     * 
     * 
     */
    private class TesseralResonantHarmonics {

        private TesseralResonantHarmonics() {
            // Dummy constructor, nothing to do.
        }

        private double[] getResonantContribution(Orbit orbit) throws OrekitException {
            // Get orbital parameters :
            final double a = orbit.getA();
            final double k = orbit.getEquinoctialEx();
            final double h = orbit.getEquinoctialEy();
            final double q = orbit.getHx();
            final double p = orbit.getHy();

            // Compute potential derivatives
            double[] dU = computePotentialDerivatives(orbit);
            final double duda = dU[0];
            final double dudh = dU[1];
            final double dudk = dU[2];
            final double dudl = dU[3];
            final double dudal = dU[4];
            final double dudbe = dU[5];
            final double dudga = dU[6];

            // Compute the cross derivative operator :
            final double UAlphaGamma = alpha * dudga - gamma * dudal;
            final double UAlphaBeta = alpha * dudbe - beta * dudal;
            final double UBetaGamma = beta * dudga - gamma * dudbe;
            final double Uhk = h * dudk - k * dudh;

            final double aDot = 2 * a / A * dudal;
            final double hDot = B * dudk / A + k / (A * B) * (p * UAlphaGamma - I * q * UBetaGamma) - h * B * dudl / (A * (1 + B));
            final double kDot = -(B * dudh / A + h / (A * B) * (p * UAlphaGamma - I * q * UBetaGamma) + k * B * dudl / (A * (1 + B)));
            final double pDot = C / (2 * A * B) * (p * (Uhk - UAlphaBeta - dudl) - UBetaGamma);
            final double qDot = C / (2 * A * B) * (p * (Uhk - UAlphaBeta - dudl) - I * UAlphaGamma);
            final double lDot = -2 * a * duda / A + B / (A * (1 + B)) * (h * dudh + k * dudk) + (p * UAlphaGamma - I * q * UBetaGamma)
                            / (A * B);

            return new double[] { aDot, hDot, kDot, pDot, qDot, lDot };

        }

        /**
         * &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient from equations 2.7.1 - (13)
         */
        private double computeGammaMsn(final int n,
                                       final int s,
                                       final int m,
                                       final double gamma) {
            double num;
            double den;
            double res = 0d;
            // Sum(Max(2, m, |s|))
            int max = Math.max(2, m);
            max = Math.max(max, Math.abs(s));
            if (s <= -m) {
                res = FastMath.pow(-1, m - s) * FastMath.pow(2, s) * FastMath.pow((1 + I * gamma), -I * m);
            } else if (FastMath.abs(s) <= m) {
                num = FastMath.pow(-1, m - s) * FastMath.pow(2, -m) * ArithmeticUtils.factorial(n + m) * ArithmeticUtils.factorial(n + m)
                                * FastMath.pow(1 + I * gamma, I * s);
                den = ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);
                res = num / den;
            } else if (s >= m) {
                res = FastMath.pow(2, -s) * FastMath.pow(1 + I * gamma, I * m);
            }
            return res;
        }

        /**
         * d&Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) / d&gamma; coefficient from equations
         * 2.7.1 - (13)
         */
        private double computeDGammaMsn(final int n,
                                        final int s,
                                        final int m,
                                        final double gamma) {
            double num;
            double den;
            double res = 0d;
            // Sum(Max(2, m, |s|))
            int max = Math.max(2, m);
            max = Math.max(max, Math.abs(s));
            if (s <= -m) {
                res = -FastMath.pow(-1, m - s) * FastMath.pow(2, s) * m * FastMath.pow((1 + I * gamma), -I * m - 1);
            } else if (FastMath.abs(s) <= m) {

                num = FastMath.pow(-1, m - s) * FastMath.pow(2, -m) * ArithmeticUtils.factorial(n + m) * ArithmeticUtils.factorial(n + m)
                                * s * FastMath.pow(1 + I * gamma, I * s - 1);
                den = ArithmeticUtils.factorial(n + s) * ArithmeticUtils.factorial(n - s);

                res = num / den;
            } else if (s >= m) {
                res = FastMath.pow(2, -s) * m * FastMath.pow(1 + I * gamma, I * m - 1);
            }
            return res;
        }

        /**
         * Compute the following elements :
         * 
         * <pre>
         * dU / da
         * dU / dh
         * dU / dk
         * dU / d&lambda;
         * dU / d&alpha;
         * dU / d&beta;
         * dU / d&gamma;
         * 
         * </pre>
         * 
         * @param orbit
         * @return
         * @throws OrekitException
         */
        private double[] computePotentialDerivatives(final Orbit orbit) throws OrekitException {
            // Result initialization
            double duda = 0d;
            double dudh = 0d;
            double dudk = 0d;
            double dudl = 0d;
            double dudal = 0d;
            double dudbe = 0d;
            double dudga = 0d;

            // The computation is done only if some resonnal tesseral term have been set up :
            if (resonantTesseralSize > 0) {

                // radial distance from center of mass of central body
                final double r = orbit.getPVCoordinates().getPosition().getNorm();
                final double rR = ae / r;
                final double muOa = mu / orbit.getA();
                final double k = orbit.getEquinoctialEx();
                final double h = orbit.getEquinoctialEy();
                final double ecc = orbit.getE();
                double rRn;
                double vmsn;
                double gamMsn;
                double dGamma;
                double kjn_1;
                double dkjn_1;
                double jacobi;
                double dJacobi;
                double gms;
                double hms;
                double cnm;
                double snm;
                double realCosFactor;
                double realSinFactor;
                // Jacobi indices
                int l, v, w;

                final double theta = computeThetaAngle(orbit);
                // TODO check this : getLM() ?
                final double lambda = orbit.getLM();
                CiSiCoefficient cisiHK = new CiSiCoefficient(orbit.getEquinoctialEx(), orbit.getEquinoctialEy());
                CiSiCoefficient cisiAB = new CiSiCoefficient(alpha, beta);
                GHmsjPolynomials GHms = new GHmsjPolynomials(cisiHK, cisiAB, false);

                double dGdh = 0d;
                double dGdk = 0d;
                double dGdA = 0d;
                double dGdB = 0d;
                double dHdh = 0d;
                double dHdk = 0d;
                double dHdA = 0d;
                double dHdB = 0d;

                Iterator<ResonantCouple> iterator = resonantTesseralsTerm.iterator();
                // Iterative process :
                while (iterator.hasNext()) {
                    ResonantCouple resonantTesseralCouple = iterator.next();
                    int j = resonantTesseralCouple.getJ();
                    int m = resonantTesseralCouple.getM();

                    final double jlMmt = j * lambda - m * theta;
                    final double sinPhi = FastMath.sin(jlMmt);
                    final double cosPhi = FastMath.cos(jlMmt);

                    int Im = (int) FastMath.pow(I, m);
                    // Sum(-N, N)
                    for (int s = -degree; s < degree; m++) {
                        // Sum(Max(2, m, |s|))
                        int nMin = Math.max(2, m);
                        nMin = Math.max(nMin, Math.abs(s));

                        // jacobi v, w, indices :
                        v = FastMath.abs(m - s);
                        w = FastMath.abs(m + s);
                        for (int n = nMin; n < degree; n++) {
                            // (R / r)^n
                            rRn = FastMath.pow(rR, n);
                            vmsn = CoefficientFactory.getVmns(m, n, s);
                            gamMsn = computeGammaMsn(n, s, Im, gamma);
                            dGamma = computeDGammaMsn(n, s, m, gamma);
                            // TODO ordre de convergence en dur
                            kjn_1 = HansenUtils.computeKernelOfHansenCoefficientFromNewcomb(ecc, j, nMin, s, epsilon);
                            dkjn_1 = HansenUtils.computeDerivativedKernelOfHansenCoefficient(ecc, j, nMin, s, epsilon);
                            dGdh = GHms.getdGmsdh(m, s, j);
                            dGdk = GHms.getdGmsdk(m, s, j);
                            dGdA = GHms.getdGmsdAlpha(m, s, j);
                            dGdB = GHms.getdGmsdBeta(m, s, j);
                            dHdh = GHms.getdHmsdh(m, s, j);
                            dHdk = GHms.getdHmsdk(m, s, j);
                            dHdA = GHms.getdHmsdAlpha(m, s, j);
                            dHdB = GHms.getdHmsdBeta(m, s, j);

                            // Jacobi l-indices :
                            l = (Math.abs(s) <= m ? (n - m) : n - Math.abs(s));
                            // TODO check indices
                            PolynomialFunction jacobiPoly = PolynomialsUtils.createJacobiPolynomial(l, v, w);
                            jacobi = jacobiPoly.value(gamma);
                            dJacobi = jacobiPoly.derivative().value(gamma);
                            gms = GHms.getGmsj(m, s, j);
                            hms = GHms.getHmsj(m, s, j);
                            cnm = Cnm[n][m];
                            snm = Snm[n][m];

                            // Compute dU / da from expansion of equation (4-a)
                            realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                            realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                            duda += (n + 1) * rRn * Im * vmsn * gamMsn * kjn_1 * jacobi * gms * cnm * (realCosFactor + realSinFactor);

                            // Compute dU / dh from expansion of equation (4-b)
                            realCosFactor = (cnm * kjn_1 * dGdh + 2 * cnm * h * (gms + hms) * dkjn_1 + snm * kjn_1 * dHdh) * cosPhi;
                            realSinFactor = (-cnm * kjn_1 * dHdh + 2 * snm * h * (gms + hms) * dkjn_1 + snm * kjn_1 * dGdh) * sinPhi;
                            dudh += rRn * Im * vmsn * gamMsn * jacobi * (realCosFactor + realSinFactor);

                            // Compute dU / dk from expansion of equation (4-c)
                            realCosFactor = (cnm * kjn_1 * dGdk + 2 * cnm * k * (gms + hms) * dkjn_1 + snm * kjn_1 * dHdk) * cosPhi;
                            realSinFactor = (-cnm * kjn_1 * dHdk + 2 * snm * k * (gms + hms) * dkjn_1 + snm * kjn_1 * dGdk) * sinPhi;
                            dudk += rRn * Im * vmsn * gamMsn * jacobi * (realCosFactor + realSinFactor);

                            // Compute dU / dLambda from expansion of equation (4-d)
                            realCosFactor = (snm * gms - hms * cnm) * cosPhi;
                            realSinFactor = (snm * hms + gms * cnm) * sinPhi;
                            dudl += j * rRn * Im * vmsn * kjn_1 * jacobi * (realCosFactor - realSinFactor);

                            // Compute dU / alpha from expansion of equation (4-e)
                            realCosFactor = (dGdA * cnm + dHdA * snm) * cosPhi;
                            realSinFactor = (dGdA * snm - dHdA * cnm) * sinPhi;
                            dudal += rRn * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                            // Compute dU / dBeta from expansion of equation (4-f)
                            realCosFactor = (dGdB * cnm + dHdB * snm) * cosPhi;
                            realSinFactor = (dGdB * snm + dHdB * cnm) * sinPhi;
                            dudbe += rRn * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                            // Compute dU / dGamma from expansion of equation (4-g)
                            realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                            realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                            dudga += rRn * Im * vmsn * kjn_1 * (realCosFactor + realSinFactor) * (jacobi * dGamma + gamMsn * dJacobi);
                        }
                    }
                }

                duda *= -muOa / orbit.getA();
                dudh *= muOa;
                dudk *= muOa;
                dudl *= muOa;
                dudal *= muOa;
                dudbe *= muOa;
                dudga *= muOa;
            }

            return new double[] { duda, dudh, dudk, dudl, dudal, dudbe, dudga };
        }
    }

}
