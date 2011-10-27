package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.TreeMap;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.dsstforcemodel.CoefficientFactory.NSKey;
import org.orekit.time.AbsoluteDate;

public class DSSTThirdBody implements DSSTForceModel {

    /**
     * Equinoctial coefficients
     */
    /** A = sqrt(&mu; * a) */
    private double                 A;

    /** B = sqrt(1 - ex<sup>2</sup> - ey<sup>2</sup> */
    private double                 B;

    /** C = 1 + hx<sup>2</sup> + hx<sup>2</sup> */
    private double                 C;

    /** Gravitational constant &mu; of the 3rd body */
    private final double           mu3;

    /** Equatorial radius of the 3rd body */
    private final double           ae3;

    /**
     * Direction cosines of the symmetry axis
     */
    /** &alpha */
    private double                 alpha;

    /** &beta */
    private double                 beta;

    /** &gamma */
    private double                 gamma;

    private HansenCoefficients     hansen;

    /**
     * DSST model needs equinoctial orbit as internal representation. Classical equinoctial elements
     * have discontinuities when inclination is close to zero. In this representation, I = +1. <br>
     * To avoid this discontinuity, another representation exists and equinoctial elements can be
     * expressed in a different way, called "retrograde" orbit. This implies I = -1. As Orekit
     * doesn't implement the retrograde orbit, I = 1 here.
     */
    private int                    I = 1;

    private TreeMap<NSKey, Double> Vns;

    /** current orbital state */
    private Orbit                  orbit;

    private final int              order;

    /**
     * convergence parameter used in Hansen coefficient generation. 1e-4 seems to be a correct
     * value.
     */
    private final double           epsilon;

    public DSSTThirdBody(final double ae3,
                         final double mu3,
                         final int order,
                         final double epsilon) {
        this.epsilon = epsilon;
        this.ae3 = ae3;
        this.order = order;
        this.Vns = CoefficientFactory.computeVnsCoefficient(order);
        this.mu3 = mu3;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws OrekitException
     */
    public double[] getMeanElementRate(SpacecraftState currentState) throws OrekitException {
        // Initialization :
        double dh = 0d;
        double dk = 0d;
        double dp = 0d;
        double dq = 0d;
        double dM = 0d;

        orbit = currentState.getOrbit();
        double a = orbit.getA();
        double k = orbit.getEquinoctialEx();
        double h = orbit.getEquinoctialEy();
        double hx = orbit.getHx();
        double hy = orbit.getHy();

        hansen = new HansenCoefficients(orbit.getE(), epsilon);

        final double[][] GsHs = CoefficientFactory.computeGsHsCoefficient(k, h, alpha, beta, order);
        final double[][] Qns = CoefficientFactory.computeQnsCoefficient(order, gamma);

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

        final double factor = (hy * UAlphaGamma - I * hx * UBetaGamma) / (A * B);

        // Compute mean element Rate for Zonal Harmonic :
        // da / dt = 0 for zonal harmonic :
        dh = (B / A) * dUdk + k * factor;
        dk = -(B / A) * dUdh - h * factor;
        dp = -C / (2 * A * B) * UBetaGamma;
        dq = -I * C * UAlphaGamma / (2 * A * B);
        dM = (-2 * a * dUda / A) + (B / (A * (1 + B))) * (h * dUdh + k * dUdk) + (hy * UAlphaGamma - I * hx * UBetaGamma) / (A * B);

        return new double[] { 0d, dk, dh, dq, dp, dM };

    }

    private double[] computePotentialderivatives(double[][] Qns,
                                                 double[][] GsHs) throws OrekitException {
        // Initialize data
        final double a = orbit.getA();
        final double k = orbit.getEquinoctialEx();
        final double h = orbit.getEquinoctialEy();

        // -mu / a
        final double factor = mu3 / ae3;
        final double Ra = a / ae3;
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

            gsM1 = (s > 0 ? GsHs[0][s - 1] : 0);
            hsM1 = (s > 0 ? GsHs[1][s - 1] : 0);
            // Compute partial derivatives of GsHs
            dGsdk = s * beta * gsM1 - s * alpha * hsM1;
            dGsdh = s * alpha * gsM1 + s * beta * hsM1;
            dGsdAl = s * k * gsM1 - s * h * hsM1;
            dGsdBe = s * h * gsM1 + s * k * hsM1;

            // Compute Partial derivatives of Gs from equ. (9)
            delta0s = (s == 0) ? 1 : 2;

            for (int n = FastMath.max(2, s); n < order; n++) {
                // Extract data from previous computation :
                vns = Vns.get(new NSKey(n, s));

                kns = hansen.getHansenKernelValue(0, n, s);
                qns = Qns[n][s];
                raExpN = FastMath.pow(Ra, n);
                dkns = hansen.getHansenKernelDerivative(0, n, s);
                commonCoefficient = delta0s * vns * qns * raExpN;

                // Compute dU / da :
                dUda += commonCoefficient * n * kns * gs;
                // Compute dU / dEx
                dUdk += commonCoefficient * (kns * dGsdk + k * khi3 * gs * dkns);
                // Compute dU / dEy
                dUdh += commonCoefficient * (kns * dGsdh + h * khi3 * gs * dkns);
                // Compute dU / dAlpha
                dUdAl += commonCoefficient * kns * dGsdAl;
                // Compute dU / dBeta
                dUdBe += commonCoefficient * kns * dGsdBe;
                // Compute dU / dGamma : here dQns/dGamma = Q(n, s + 1) from Equation 3.1 - (8)
                dUdGa += delta0s * raExpN * vns * kns * Qns[n][s + 1] * gs;
            }
        }

        dUda *= (factor / a);
        dUdk *= factor;
        dUdh *= factor;
        dUdAl *= factor;
        dUdBe *= factor;
        dUdGa *= factor;

        return new double[] { dUda, dUdk, dUdh, dUdAl, dUdBe, dUdGa };

    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(AbsoluteDate date,
                                               double[] meanElements) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public void init(SpacecraftState state) {
        // TODO Auto-generated method stub

    }

}
