package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math.util.ArithmeticUtils;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.propagation.semianalytical.dsst.coefficients.CiSiCoefficient;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTFactorial;
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
public class DSSTCentralBody extends AbstractGravitationalForces {

    // Analytical central body spherical harmonic models
    /** Equatorial radius of the Central Body. */
    private final double           ae;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>). */
    private final double           mu;

    /** First normalized potential tesseral coefficients array. */
    private final double[][]       Cnm;

    /** Second normalized potential tesseral coefficients array. */
    private final double[][]       Snm;

    /** Degree <i>n</i> of non resonant C<sub>nm</sub> potential. */
    private final int              degree;

    /** Order <i>m</i> of non resonant C<sub>nm</sub> potential. */
    private final int              order;

    /** Maximum resonant order */
    private int                    maxResonantOrder;

    /** Maximum resonant degree */
    private int                    maxResonantDegree;

    /** List of resonant tesseral harmonic couple */
    private List<ResonantCouple>   resonantTesseralHarmonic;

    /**
     * DSST model needs equinoctial orbit as internal representation. Classical equinoctial elements
     * have discontinuities when inclination is close to zero. In this representation, I = +1. <br>
     * To avoid this discontinuity, another representation exists and equinoctial elements can be
     * expressed in a different way, called "retrograde" orbit. This implies I = -1. As Orekit
     * doesn't implement the retrograde orbit, I = 1 here.
     */
    private int                    I                         = 1;

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

    /**
     * Internal variables
     */
    /** Coefficient used to define the mean disturbing function V<sub>ns</sub> coefficient */
    private TreeMap<NSKey, Double> Vns;

    /**
     * Minimum period for analytically averaged high-order resonant central body spherical harmonics
     * in seconds. This value is set to 10 days, but can be overrides by using the
     * {@link #setResonantMinPeriodInSec(double)} method.
     */
    private double                 resonantMinPeriodInSec;

    /**
     * Minimum period for analytically averaged high-order resonant central body spherical harmonics
     * in satellite revolutions. This value is set to 10 satellite revolutions, but can be overrides
     * by using the {@link #setResonantMinPeriodInSatRev(double)} method.
     */
    private double                 resonantMinPeriodInSatRev;

    /** Geopotential coefficient Jn = -Cn0 */
    private double[]               Jn;

    /** Hansen coefficient */
    private HansenCoefficients     hansen;

    /** &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient from equations 2.7.1 - (13) */
    private GammaMsnCoefficients   gammaMNS;

    /**
     * Highest power of the eccentricity to appear in the truncated analytical power series
     * expansion for the averaged central-body Zonal harmonic potential. The user can set this value
     * by using the {@link #setZonalMaximumEccentricityPower(double)} method. If he doesn't, the
     * software will compute a default value itself, through the
     * {@link #computeZonalMaxEccentricityPower()} method.
     */
    private int                    zonalMaxEccentricityPower;

    /**
     * Maximal value of the geopotential order that will be used in zonal series expansion. This
     * value won't be used if the {@link #zonalMaxEccentricityPower} is set through the
     * {@link #computeZonalMaxEccentricityPower()} method. If not, series expansion will
     * automatically be truncated.
     */
    private int                    zonalMaxGeopotentialCoefficient;

    /**
     * Highest power of the eccentricity to appear in the truncated analytical power series
     * expansion for the averaged central-body Tesseral harmonic potential. The user can set this
     * value by using the {@link #setTesseralMaximumEccentricityPower(double)} method. If he
     * doesn't, the software will compute a default value itself, through the
     * {@link #computeTesseralMaxEccentricityPower()} method.
     */
    private int                    tesseralMaxEccentricityPower;

    /** Central-body rotation period in seconds */
    private double                 omega;

    /**
     * Truncation tolerance for analytically averaged central body spherical harmonics for orbits
     * which are always in vacuum
     */
    private final static double    truncationToleranceVacuum = 1e-10;

    /**
     * Truncation tolerance for analytically averaged central body spherical harmonics for
     * drag-perturbed orbit
     */
    private final static double    truncationToleranceDrag   = 1e-10;

    /** Minimum perturbation period */
    private double                 minPerturbationPeriod;

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
     * @param maxPowerOfENonResonant
     *            Maximum power of the eccentricity in the expansion for the non-resonant potential
     * @param numberOfResonantTesseralTerms
     *            Number of resonant tesseral terms
     * @param maxE2
     *            Maximum power of the e<sup>2</sup> to be used in the power series expansion for
     *            the Hansen coefficients Kernel potential
     * @param resonantTesseral
     *            Resonant Tesseral harmonic couple term. This parameter can be set to null or be an
     *            empty list. If so, the program will automatically determine the resonant couple to
     *            take in account. If not, only the resonant couple given by the user will be taken
     *            in account.
     * @param maxEccOrder
     *            Maximum power of eccentricity used in the expansion of the Hansen coefficient
     *            Kernel
     */
    public DSSTCentralBody(final double centralBodyRotationRate,
                           final double ae,
                           final double mu,
                           final double[][] Cnm,
                           final double[][] Snm,
                           final List<ResonantCouple> resonantTesseral) {
        // Get the central-body rotation period :
        this.omega = MathUtils.TWO_PI / centralBodyRotationRate;
        this.mu = mu;
        this.ae = ae;
        this.Cnm = Cnm;
        this.Snm = Snm;
        this.degree = Cnm.length - 1;
        this.order = Cnm[degree].length - 1;
        // Check potential coefficient consistency
        if ((Cnm.length != Snm.length) || (Cnm[Cnm.length - 1].length != Snm[Snm.length - 1].length)) {
            throw OrekitException.createIllegalArgumentException(OrekitMessages.POTENTIAL_ARRAYS_SIZES_MISMATCH, Cnm.length, Cnm[degree].length, Snm.length, Snm[degree].length);
        }
        // Initialize the Jn coefficient for zonal harmonic series expansion
        initializeJn(Cnm);
        // Store local variables
        if (resonantTesseral != null) {
            resonantTesseralHarmonic = resonantTesseral;
            if (resonantTesseralHarmonic.size() > 0) {
                // Get the maximal resonant order
                ResonantCouple maxCouple = Collections.max(resonantTesseral);
                maxResonantOrder = maxCouple.getM();
                maxResonantDegree = maxCouple.getN();
            }
        } else {
            resonantTesseralHarmonic = new ArrayList<ResonantCouple>();
            // Set to a default undefined value
            maxResonantOrder = Integer.MIN_VALUE;
            maxResonantDegree = Integer.MIN_VALUE;
        }
        // Initialize default values
        this.resonantMinPeriodInSec = 864000d;
        this.resonantMinPeriodInSatRev = 10d;
        this.zonalMaxEccentricityPower = Integer.MIN_VALUE;
        this.tesseralMaxEccentricityPower = Integer.MIN_VALUE;

    }

    /**
     * {@inheritDoc} From equation 3.1 - (1)
     * 
     * @throws OrekitException
     */
    public double[] getMeanElementRate(SpacecraftState spacecraftState) throws OrekitException {

        // Store current state :
        orbit = spacecraftState.getOrbit();
        // Initialization of A, B, C, Alpha, Beta and Gamma coefficient :
        updateABCAlphaBetaGamma(orbit);
        // TODO
        hansen = new HansenCoefficients(orbit.getE(), zonalMaxEccentricityPower);
        gammaMNS = new GammaMsnCoefficients(gamma, I);
        // Get zonal harmonics contribution :
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
     * {@inheritDoc} This method here will find the resonant tesseral terms in the central body
     * harmonic field. The routine computes the repetition period of the perturbation caused by each
     * central-body sectoral and tesseral harmonic term and compares the period to a predetermined
     * tolerance, the minimum period considered to be resonant.
     * 
     * @throws OrekitException
     */
    public void initialize(final SpacecraftState initialState) throws OrekitException {
        updateABCAlphaBetaGamma(initialState.getOrbit());

        // Select central body resonant tesseral harmonic terms
        selectCentralBodyResonantTesseral(initialState);

        // Set the highest power of the eccentricity in the analytical power series expansion for
        // the averaged low order central body spherical harmonic perturbation
        computeZonalMaxEccentricityPower(initialState);

        // Set the highest power of the eccentricity in the analytical power series expansion for
        // the averaged high order resonant central body spherical harmonic perturbation and compute
        // the Newcomb operators
        computeTesseralMaxEccentricityPower();

        Vns = DSSTCoefficientFactory.computeVnsCoefficient(zonalMaxGeopotentialCoefficient + 1);
    }

    private void computeTesseralMaxEccentricityPower() {
        // TODO Auto-generated method stub

    }

    /**
     * This subroutine computes the highest power of the eccentricity to appear in the truncated
     * analytical power series expansion for the averaged central-body zonal harmonic potential.
     * 
     * @throws OrekitException
     */
    private void computeZonalMaxEccentricityPower(final SpacecraftState initialState) throws OrekitException {
        // Did a maximum eccentricity power has been found
        boolean maxFound = false;
        // Initialize the current spherical harmonic term to 0.
        double term = 0.;
        // Maximal degree of the geopotential expansion :
        int nMax = Integer.MIN_VALUE;
        // Maximal power of e
        int sMax = Integer.MIN_VALUE;
        // Find the truncation tolerance : set tolerance as a non draged satellite. Operation stops
        // when term > tolerance
        final double tolerance = truncationToleranceVacuum;
        // Check if highest power of E has been given by the user :
        if (zonalMaxEccentricityPower == Integer.MIN_VALUE) {
            // Is the degree of the zonal harmonic field too small to allow more than one power of E
            if (degree == 2) {
                zonalMaxEccentricityPower = 0;
            } else {
                // Auxiliary quantities
                final double ecc = initialState.getE();
                final double r2a = ae / (2 * initialState.getA());
                double x2MuRaN = 2 * mu / (initialState.getA()) * r2a;
                // Search for the highest power of E for which the computed value is greater than
                // the truncation tolerance in the power series
                // s-loop :
                for (int s = 0; s <= degree - 2; s++) {
                    // n-loop
                    for (int n = s + 2; n <= degree; n++) {
                        // (n - s) must be even
                        if ((n - s) % 2 == 0) {
                            // Local values :
                            final double gam2 = gamma * gamma;
                            // Compute factorial :
                            final BigInteger factorialNum = DSSTFactorial.fact(n - s);
                            final BigInteger factorialDen = DSSTFactorial.fact((n + s) / 2).multiply(DSSTFactorial.fact((n - s) / 2));
                            final double factorial = factorialNum.doubleValue() / factorialDen.doubleValue();
                            HansenCoefficients hansen = new HansenCoefficients(ecc);
                            final double k0 = hansen.getHansenKernelValue(0, -n - 1, s);
                            // Compute the Qns(bound) upper bound :
                            final double qns = FastMath.abs(DSSTCoefficientFactory.getQnsPolynomialValue(gamma, n, s));
                            final double qns2 = qns * qns;
                            final double factor = (1 - gam2) / (n * (n + 1) - s * (s + 1));
                            // Compute dQns/dGamma
                            final double dQns = FastMath.abs(DSSTCoefficientFactory.getQnsPolynomialValue(gamma, n, s + 1));
                            final double dQns2 = dQns * dQns;
                            final double qnsBound = FastMath.sqrt(qns2 + factor * dQns2);

                            // Get the current potential upper bound for the current (n, s) couple.
                            term = x2MuRaN * r2a * FastMath.abs(Jn[n]) * factorial * k0 * qnsBound * FastMath.pow(1 - gam2, s / 2)
                                   * FastMath.pow(ecc, s) / FastMath.pow(2, n);

                            // Compare result with the tolerance parameter :
                            if (term <= tolerance) {
                                // Stop here
                                nMax = Math.max(nMax, n);
                                sMax = Math.max(sMax, s);
                                // truncature found
                                maxFound = true;
                                // Force a premature end loop
                                n = degree;
                                s = degree;
                            }
                        }
                    }
                    // Prepare next loop :
                    x2MuRaN = 2 * mu / (initialState.getA()) * FastMath.pow(r2a, s + 1);
                }
            }
            if (maxFound) {
                zonalMaxGeopotentialCoefficient = nMax;
                zonalMaxEccentricityPower = sMax;
            } else {
                zonalMaxGeopotentialCoefficient = degree;
                zonalMaxEccentricityPower = degree - 2;
            }
        } else {
            zonalMaxGeopotentialCoefficient = degree;
        }

    }

    private void selectCentralBodyResonantTesseral(SpacecraftState initialState) {
        // Initialize resonant order
        List<Integer> resonantOrder = new ArrayList<Integer>();
        // Initialize resonant index for each resonant order
        List<Integer> resonantIndex = new ArrayList<Integer>();

        // Get the satellite period
        final double satellitePeriod = initialState.getKeplerianPeriod();
        // Compute ration of satellite period to central body rotation period
        final double ratio = satellitePeriod / omega;

        // If user didn't define a maximum resonant order, use the maximum central-body's
        // order
        if (maxResonantOrder == Integer.MIN_VALUE) {
            // TODO check +1
            maxResonantOrder = order;
        }

        if (maxResonantDegree == Integer.MIN_VALUE) {
            maxResonantDegree = degree;
        }

        // Has the user requested a specific set of resonant tesseral harmonic terms ?
        if (resonantTesseralHarmonic.size() == 0) {

            final int maxResonantOrderTmp = maxResonantOrder;
            // Reinitialize the maxResonantOrder parameter :
            maxResonantOrder = 0;

            double tolerance = resonantMinPeriodInSec / satellitePeriod;
            if (tolerance < resonantMinPeriodInSatRev) {
                tolerance = resonantMinPeriodInSatRev;
            }
            tolerance = 1d / tolerance;

            // Now find the order of the resonant tesseral harmonic field
            for (int m = 0; m < maxResonantOrderTmp; m++) {
                double resonance = ratio * m;
                int j = (int) (resonance + 0.5);
                if (FastMath.abs(resonance - j) <= tolerance && j > 0d) {
                    // Update the maximum resonant order found
                    maxResonantOrder = m;

                    // Store each resonant degrees for each resonant order
                    resonantOrder.add(m, m);
                    resonantIndex.add(m, j);

                    // Store each resonant couple for a given order
                    // TODO check <=
                    for (int n = m; n <= degree; n++) {
                        resonantTesseralHarmonic.add(new ResonantCouple(n, m));
                    }
                }
            }
        }
        // Have any resonant terms been found ?
        if (maxResonantOrder > 0) {
            minPerturbationPeriod = computeMinimumPerturbationPeriod(ratio, satellitePeriod, resonantOrder, resonantIndex);
        }
    }

    /**
     * This method compute the minimum perturbation period from witch a perturbation is considered
     * to
     * 
     * @param ratio
     * @param satellitePeriod
     * @param resonantOrder
     * @param resonantIndex
     * @return
     */
    private double computeMinimumPerturbationPeriod(double ratio,
                                                    double satellitePeriod,
                                                    List<Integer> resonantOrder,
                                                    List<Integer> resonantIndex) {

        double minPerturbationPeriod = Double.POSITIVE_INFINITY;
        // Compute the minimum perturbation period
        for (int m = 0; m < maxResonantOrder; m++) {
            if (resonantOrder.get(m) == m) {
                double divisor = FastMath.abs(ratio * m - resonantIndex.get(m));
                if (divisor < 1e-10) {
                    divisor = 1e-10;
                    minPerturbationPeriod = FastMath.min(minPerturbationPeriod, satellitePeriod / divisor);
                }
            }
        }
        return minPerturbationPeriod;

    }

    /**
     * Set the minimum period for analytically averaged high-order resonant central body spherical
     * harmonics in seconds. Set to 10 days by default.
     * 
     * @param resonantMinPeriodInSec
     *            minimum period in seconds
     */
    public void setResonantMinPeriodInSec(final double resonantMinPeriodInSec) {
        this.resonantMinPeriodInSec = resonantMinPeriodInSec;
    }

    /**
     * Set the minimum period for analytically averaged high-order resonant central body spherical
     * harmonics in satelliteRevolution. Set to 10 by default.
     * 
     * @param resonantMinPeriodInSatRev
     *            minimum period in satellite revolution
     */
    public void setResonantMinPeriodInSatRev(final double resonantMinPeriodInSatRev) {
        this.resonantMinPeriodInSatRev = resonantMinPeriodInSatRev;
    }

    /**
     * This methode set the highest power of the eccentricity to appear in the truncated analytical
     * power series expansion for the averaged central-body zonal harmonic potential.
     * 
     * @param zonalMaxEccPower
     *            highest power of the eccentricity
     */
    public void setZonalMaximumEccentricityPower(final int zonalMaxEccPower) {
        this.zonalMaxEccentricityPower = zonalMaxEccPower;
    }

    /**
     * This methode set the highest power of the eccentricity to appear in the truncated analytical
     * power series expansion for the averaged central-body tesseral harmonic potential.
     * 
     * @param tesseralMaxEccPower
     *            highest power of the eccentricity
     */
    public void setTesseralMaximumEccentricityPower(final int tesseralMaxEccPower) {
        this.tesseralMaxEccentricityPower = tesseralMaxEccPower;
    }

    /**
     * Get the zonal contribution of the central body for the first order mean element rates.
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

            final double[][] GsHs = DSSTCoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, zonalMaxGeopotentialCoefficient + 1);

            final double[][] Qns = DSSTCoefficientFactory.computeQnsCoefficient(gamma, zonalMaxGeopotentialCoefficient + 1);

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

            for (int s = 0; s < zonalMaxEccentricityPower; s++) {
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

                for (int n = s + 2; n < zonalMaxGeopotentialCoefficient + 1; n++) {
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

            Iterator<ResonantCouple> iterator = resonantTesseralHarmonic.iterator();
            // Iterative process :

            while (iterator.hasNext()) {
                ResonantCouple resonantTesseralCouple = iterator.next();
                int j = resonantTesseralCouple.getN();
                int m = resonantTesseralCouple.getM();

                final double jlMmt = j * lambda - m * theta;
                final double sinPhi = FastMath.sin(jlMmt);
                final double cosPhi = FastMath.cos(jlMmt);

                int Im = (int) FastMath.pow(I, m);
                // Sum(-N, N)
                for (int s = -maxResonantDegree; s < maxResonantDegree + 1; s++) {
                    // Sum(Max(2, m, |s|))
                    int nmin = Math.max(Math.max(2, m), Math.abs(s));

                    // jacobi v, w, indices : see 2.7.1 - (15)
                    v = FastMath.abs(m - s);
                    w = FastMath.abs(m + s);
                    for (int n = nmin; n < maxResonantDegree + 1; n++) {
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

                duda *= -muOa / a;
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
