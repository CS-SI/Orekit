package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.coefficients.CiSiCoefficient;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.coefficients.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.coefficients.GammaMsnCoefficients;
import org.orekit.propagation.semianalytical.dsst.coefficients.HansenCoefficients;
import org.orekit.time.AbsoluteDate;

/**
 * Central body contribution to the {@link DSSTPropagator}. Central body is divided into a mean
 * contribution, witch is integrated over long step period, and some short periodic variations that
 * can be analytically computed.
 * <p>
 * Mean element rate are the da<sub>i</sub>/dt derivatives.
 * 
 * @author Romain Di Costanzo
 */
public class DSSTCentralBody implements DSSTForceModel {

    /**
     * General data
     */
    /** Equatorial radius of the Central Body. */
    private final double           ae;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double           mu;

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

    /** Hansen coefficient */
    private HansenCoefficients     hansen;

    /** &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient from equations 2.7.1 - (13) */
    private GammaMsnCoefficients   gammaMNS;

    /**
     * convergence parameter used in Hansen coefficient generation. 1e-4 seems to be a correct
     * value.
     */
    private final double           epsilon;

    /**
     * DSST Central body constructor.
     * 
     * @param ae
     *            Equatorial radius of the central body
     * @param mu
     *            &mu; of the central body
     * @param Cnm
     *            Cosines part of the spherical harmonics
     * @param Snm
     *            Sines part of the spherical harmonics
     * @param resonantTesserals
     *            resonant couple term. This parameter can be set to null. If so, all couples will
     *            be taken in account. If not, only terms included in the resonant couple will be
     *            considered. If the list is an empty one, only zonal effect will be considered.
     * @param epsilon
     *            convergence parameter used in Hansen coefficient generation. 1e-4 seems to be a
     *            correct value
     */
    public DSSTCentralBody(final double ae,
                           final double mu,
                           final double[][] Cnm,
                           final double[][] Snm,
                           final List<ResonantCouple> resonantTesserals,
                           final double epsilon) {
        this.Cnm = Cnm;
        this.Snm = Snm;
        this.degree = Cnm.length - 1;
        this.order = Cnm[degree].length - 1;
        this.epsilon = epsilon;
        if ((Cnm.length != Snm.length) || (Cnm[Cnm.length - 1].length != Snm[Snm.length - 1].length)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH, Cnm.length, Cnm[degree].length, Snm.length, Snm[degree].length);
        }
        this.ae = ae;
        this.mu = mu;
        // Initialize the constant component
        initializeJn(Cnm);
        Vns = DSSTCoefficientFactory.computeVnsCoefficient(degree + 1);
        List<ResonantCouple> resonantList = new ArrayList<ResonantCouple>();
        if (resonantTesserals == null) {
            for (int j = 1; j < order + 1; j++) {
                for (int m = 1; m < j + 1; m++) {
                    ResonantCouple couple = new ResonantCouple(j, m);
                    resonantList.add(couple);
                }

            }
            resonantTesseralsTerm = resonantList;
        } else {
            resonantTesseralsTerm = resonantTesserals;
        }
    }

    /**
     * {@inheritDoc} From equation 3.1 - (1)
     * 
     * @throws OrekitException
     */
    public double[] getMeanElementRate(SpacecraftState spacecraftState) throws OrekitException {

        // Store current state :
        orbit = spacecraftState.getOrbit();
        // Initialisation of A, B, C, Alpha, Beta and Gamma coefficient :
        updateABCAlphaBetaGamma(orbit);
        hansen = new HansenCoefficients(orbit.getE(), epsilon);
        gammaMNS = new GammaMsnCoefficients(gamma, I);
        // Get zonal harmonics contributuion :
        ZonalHarmonics zonalHarmonics = new ZonalHarmonics();
        double[] zonalTerms = zonalHarmonics.getZonalContribution(orbit);

        // Get tesseral resonant harmonics contributuion :
        TesseralResonantHarmonics tesseralHarmonics = new TesseralResonantHarmonics();
        double[] tesseralTerms = tesseralHarmonics.getResonantContribution(orbit);

        double[] meanElementRate = new double[zonalTerms.length];
        for (int i = 0; i < zonalTerms.length; i++) {
            meanElementRate[i] = tesseralTerms[i] + zonalTerms[i];
        }

        return meanElementRate;
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements) throws OrekitException {
        // TODO: not implemented yet : Short Periodic Variations are set to null
        return new double[] { 0., 0., 0., 0., 0., 0. };
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

        final double num = 1. / (1 + q2 + p2);

        // compute the f vector :
        final double fx = 1 - p2 + q2;
        final double fy = 2 * p * q;
        final double fz = -2 * I * p;
        Vector3D f = new Vector3D(num, new Vector3D(fx, fy, fz));

        // Compute the g vector :
        final double gx = 2 * I * p * q;
        final double gy = (1 + p2 - q2) * I;
        final double gz = 2 * q;
        Vector3D g = new Vector3D(num, new Vector3D(gx, gy, gz));

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

            final double[][] GsHs = DSSTCoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, degree + 1);

            final double[][] Qns = DSSTCoefficientFactory.computeQnsCoefficient(gamma, degree + 1);

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
            dM = (-2. * a * dUda / A) + (B / (A * (1 + B))) * (h * dUdh + k * dUdk) + (p * UAlphaGamma - I * q * UBetaGamma) / (A * B);

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

            for (int s = 0; s < degree - 1; s++) {
                // Get the current gs and hs coefficient :
                gs = GsHs[0][s];

                // Compute partial derivatives of Gs from equ. (9) :
                // First get the G(s-1) and the H(s-1) coefficient : SET TO 0 IF < 0
                gsM1 = (s > 0 ? GsHs[0][s - 1] : 0);
                hsM1 = (s > 0 ? GsHs[1][s - 1] : 0);
                // Get derivatives
                dGsdh = s * beta * gsM1 - s * alpha * hsM1;
                dGsdk = s * alpha * gsM1 + s * beta * hsM1;
                dGsdAl = s * k * gsM1 - s * h * hsM1;
                dGsdBe = s * h * gsM1 + s * k * hsM1;

                // get (2 - delta0s)
                delta0s = (s == 0) ? 1 : 2;

                for (int n = s + 2; n < degree + 1; n++) {
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
                    dUdk += commonCoefficient * qns * (kns * dGsdk + k * khi3 * gs * dkns);
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
     * Get the Central-Body Gravitational Resonant Tesserals Harmonics for the first order
     * contribution
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
         * Compute the following elements from expression 3.3 - (4):
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

            // radial distance from center of mass of central body
            // final double r = orbit.getPVCoordinates().getPosition().getNorm();

            // Get needed orbital elements
            final double a = orbit.getA();
            final double k = orbit.getEquinoctialEx();
            final double h = orbit.getEquinoctialEy();

            final double ra = ae / a;
            final double muOa = mu / a;

            double ran;
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
            final double lambda = orbit.getLM();
            final CiSiCoefficient cisiKH = new CiSiCoefficient(k, h);
            final CiSiCoefficient cisiAB = new CiSiCoefficient(alpha, beta);
            final GHmsjPolynomials GHms = new GHmsjPolynomials(cisiKH, cisiAB, I);

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
                for (int s = -degree; s < degree + 1; s++) {
                    // Sum(Max(2, m, |s|))
                    int nmin = Math.max(Math.max(2, m), Math.abs(s));

                    // jacobi v, w, indices : see 2.7.1 - (15)
                    v = FastMath.abs(m - s);
                    w = FastMath.abs(m + s);
                    for (int n = nmin; n < degree + 1; n++) {
                        // (R / a)^n
                        ran = FastMath.pow(ra, n);
                        // Vmns computation : if s < 0 : V(m, n, s) = (-1)^s * V(m, n, -s). See
                        // equation 2.7.2 - (1)
                        if (s < 0) {
                            vmsn = Math.pow(-1, s) * DSSTCoefficientFactory.getVmns(m, n, -s);
                        } else {
                            vmsn = DSSTCoefficientFactory.getVmns(m, n, s);
                        }
                        gamMsn = gammaMNS.getGammaMsn(n, s, m);
                        dGamma = gammaMNS.getDGammaMsn(n, s, m);
                        kjn_1 = hansen.getHansenKernelValue(j, -n - 1, s);
                        // kjn_1 = hansen.computHKVfromNewcomb(j, -n - 1, s);
                        dkjn_1 = hansen.getHansenKernelDerivative(j, -n - 1, s);
                        dGdh = GHms.getdGmsdh(m, s, j);
                        dGdk = GHms.getdGmsdk(m, s, j);
                        dGdA = GHms.getdGmsdAlpha(m, s, j);
                        dGdB = GHms.getdGmsdBeta(m, s, j);
                        dHdh = GHms.getdHmsdh(m, s, j);
                        dHdk = GHms.getdHmsdk(m, s, j);
                        dHdA = GHms.getdHmsdAlpha(m, s, j);
                        dHdB = GHms.getdHmsdBeta(m, s, j);

                        // Jacobi l-indices : see 2.7.1 - (15)
                        l = (Math.abs(s) <= m ? (n - m) : n - Math.abs(s));
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
                        duda += (n + 1) * ran * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                        // Compute dU / dh from expansion of equation (4-b)
                        realCosFactor = (cnm * kjn_1 * dGdh + 2 * cnm * h * (gms + hms) * dkjn_1 + snm * kjn_1 * dHdh) * cosPhi;
                        realSinFactor = (-cnm * kjn_1 * dHdh + 2 * snm * h * (gms + hms) * dkjn_1 + snm * kjn_1 * dGdh) * sinPhi;
                        dudh += ran * Im * vmsn * gamMsn * jacobi * (realCosFactor + realSinFactor);

                        // Compute dU / dk from expansion of equation (4-c)
                        realCosFactor = (cnm * kjn_1 * dGdk + 2 * cnm * k * (gms + hms) * dkjn_1 + snm * kjn_1 * dHdk) * cosPhi;
                        realSinFactor = (-cnm * kjn_1 * dHdk + 2 * snm * k * (gms + hms) * dkjn_1 + snm * kjn_1 * dGdk) * sinPhi;
                        dudk += ran * Im * vmsn * gamMsn * jacobi * (realCosFactor + realSinFactor);

                        // Compute dU / dLambda from expansion of equation (4-d)
                        realCosFactor = (snm * gms - hms * cnm) * cosPhi;
                        realSinFactor = (snm * hms + gms * cnm) * sinPhi;
                        dudl += j * ran * Im * vmsn * kjn_1 * jacobi * (realCosFactor - realSinFactor);

                        // Compute dU / alpha from expansion of equation (4-e)
                        realCosFactor = (dGdA * cnm + dHdA * snm) * cosPhi;
                        realSinFactor = (dGdA * snm - dHdA * cnm) * sinPhi;
                        dudal += ran * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                        // Compute dU / dBeta from expansion of equation (4-f)
                        realCosFactor = (dGdB * cnm + dHdB * snm) * cosPhi;
                        realSinFactor = (dGdB * snm - dHdB * cnm) * sinPhi;
                        dudbe += ran * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                        // Compute dU / dGamma from expansion of equation (4-g)
                        realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                        realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                        dudga += ran * Im * vmsn * kjn_1 * (jacobi * dGamma + gamMsn * dJacobi) * (realCosFactor + realSinFactor);
                    }
                }
            }

            duda *= -muOa / a;
            dudh *= muOa;
            dudk *= muOa;
            dudl *= muOa;
            dudal *= muOa;
            dudbe *= muOa;
            dudga *= muOa;

            return new double[] { duda, dudh, dudk, dudl, dudal, dudbe, dudga };
        }
    }

}
