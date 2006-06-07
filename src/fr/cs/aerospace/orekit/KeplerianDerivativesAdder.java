package fr.cs.aerospace.orekit;

/** This class sums up the contribution of several forces into orbit derivatives.
 *
 * <p>The aim of this class is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * orbital parameters. It implements Gauss equations for keplerian parameters.
 * </p>
 *
 * @version $Id$
 * @author L. Maisonobe
 * @author G. Prat
 *
 */
public class KeplerianDerivativesAdder
  extends OrbitDerivativesAdder {

  /** Multiplicative coefficients for the perturbing accelerations. */
  private double aT;
  private double aQ;
  private double aS;

  private double eT;
  private double eN;
  private double eQ;
  private double eS;

  private double iW;

  private double paT;
  private double paN;
  private double paQ;
  private double paS;
  private double paW;

  private double raanW;

  private double vT;
  private double vN;
  private double vQ;
  private double vS;

  /** Kepler evolution on true latitude argument. */
  private double vKepler;

  /** Create a new instance
   * @param parameters current orbital parameters
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianDerivativesAdder(OrbitalParameters parameters, double mu) {
    super(parameters, mu);
  }

  /** Initialize all derivatives to zero.
   * @param yDot reference to the array where to put the derivatives.
   */
  public void initDerivatives(double[] yDot) {

    // store orbit parameters
    super.initDerivatives(yDot);

    // orbital elements
    KeplerianParameters keplerianParameters =
      (KeplerianParameters) parameters;
    double a    = keplerianParameters.getA();
    double e    = keplerianParameters.getE();
    double i    = keplerianParameters.getI();
    double pa   = keplerianParameters.getPerigeeArgument();
    double v    = keplerianParameters.getTrueAnomaly();
    double E    = keplerianParameters.getEccentricAnomaly();

    // intermediate variables
    double e2         = e * e;
    double oMe2       = 1 - e2;
    double epsilon    = Math.sqrt(oMe2);
    double na         = Math.sqrt(mu / a);
    double n          = na / a;
    double cosV       = Math.cos(v);
    double sinV       = Math.sin(v);
    double ecV        = e * cosV;
    double ksi        = 1 + ecV;
    double nu         = e * sinV;
    double sqrt       = Math.sqrt(ksi * ksi + nu * nu);
    double oPksi      = 2 + ecV;
    double cosVOnE    = cosV / e;
    double sinVOnE    = sinV / e;
    double paPv       = pa + v;

    double epsilonOnNA        = epsilon / na;
    double epsilonOnNAKsi     = epsilonOnNA / ksi;
    double epsilonOnNAKsiSqrt = epsilonOnNAKsi / sqrt;
    double tOnEpsilonN        = 2 / (n * epsilon);
    double tEpsilonOnNASqrt   = 2 * epsilonOnNA / sqrt;

    // Kepler natural evolution
    double dvOnde = oPksi * nu / (e*oMe2);
    vKepler = n * ksi * ksi / (oMe2 * epsilon);

    // coefficients along T
    aT  = tOnEpsilonN * sqrt;
    eT  = tEpsilonOnNASqrt * (e + cosV);
    paT = tEpsilonOnNASqrt * sinVOnE;
    // vT  = dvOnde * eT
    //    - 2 * sinVOnE * ksi * (ksi + e2) / (na * sqrt * oMe2);
    vT  = dvOnde * eT
        - 2 * sinVOnE * ksi * (ksi + e2)* epsilonOnNA / (sqrt * oMe2);

    // coefficients along N
    eN  = - epsilonOnNAKsiSqrt * oMe2 * sinV;
    paN = epsilonOnNAKsiSqrt * (2 + (1 + e2) * cosVOnE);
    vN  = dvOnde * eN - ksi * epsilonOnNA * cosVOnE / sqrt;

    // coefficients along Q
    aQ  = tOnEpsilonN * nu;
    eQ  = epsilonOnNA * sinV;
    paQ = - epsilonOnNA * cosVOnE;
    vQ  = dvOnde * eQ - ksi * (2 - cosVOnE * ksi) / (na * epsilon);

    // coefficients along S
    aS  = tOnEpsilonN * ksi;
    eS  = epsilonOnNA * (Math.cos(E) + cosV);
    paS = epsilonOnNAKsi * oPksi * sinVOnE;
    vS  = dvOnde * eS - ksi * oPksi * sinVOnE / (na * epsilon);

    // coefficients along W
    //iW    = epsilonOnNAKsiSqrt * epsilon * Math.cos(paPv);
    iW    = epsilonOnNAKsi * Math.cos(paPv);
    raanW = epsilonOnNAKsi * Math.sin(paPv) / Math.sin(i);
    // paW   = -epsilonOnNA * raanW * Math.cos(i) / (e * sqrt);
    paW   = - raanW * Math.cos(i);
  }

  /** Add the contribution of the Kepler evolution.
   * <p>Since the Kepler evolution if the most important, it should
   * be added after all the other ones, in order to improve
   * numerical accuracy.</p>
   */
  public void addKeplerContribution() {
    yDot[5] += vKepler;
  }

  /** Add the contribution of an acceleration expressed in (T, N, W)
   * local orbital frame.
   * @param t acceleration along the T axis (m/s<sup>2</sup>)
   * @param n acceleration along the N axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public void addTNWAcceleration(double t, double n, double w) {
    yDot[0] += aT * t;
    yDot[1] += eT * t + eN * n;
    yDot[2] += iW * w;
    yDot[3] += paT * t + paN * n + paW * w;
    yDot[4] += raanW * w;
    yDot[5] += vT * t + vN * n;
  }

  /** Add the contribution of an acceleration expressed in (Q, S, W)
   * local orbital frame.
   * @param q acceleration along the Q axis (m/s<sup>2</sup>)
   * @param s acceleration along the S axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public void addQSWAcceleration(double q, double s, double w) {
    yDot[0] += aQ * q + aS * s;
    yDot[1] += eQ * q + eS * s;
    yDot[2] += iW * w;
    yDot[3] += paQ * q + paS * s + paW * w;
    yDot[4] += raanW * w;
    yDot[5] += vQ * q + vS * s;
  }

}
