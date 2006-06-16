package fr.cs.aerospace.orekit.orbits;

/** This class sums up the contribution of several forces into orbit derivatives.
 *
 * <p>The aim of this class is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * orbital parameters. It implements Gauss equations for equinoctial parameters.
 * </p>
 *
 * @version $Id$
 * @author M. Romero
 * @author L. Maisonobe
 *
 */
public class EquinoctialDerivativesAdder
  extends OrbitDerivativesAdder {

  /** Multiplicative coefficients for the perturbing accelerations. */
  private double aT;
  private double exT;
  private double eyT;

  private double aQ;
  private double exQ;
  private double eyQ;

  private double exN;
  private double eyN;

  private double aS;
  private double exS;
  private double eyS;

  private double eyW;
  private double exW;
  private double hxW;
  private double hyW;
  private double lvW;

  /** Kepler evolution on true latitude argument. */
  private double lvKepler;

  /** Create a new instance
   * @param parameters current orbital parameters
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EquinoctialDerivativesAdder(OrbitalParameters parameters, double mu) {
    super(parameters, mu);
  }

  /** Initialize all derivatives to zero.
   * @param yDot reference to the array where to put the derivatives.
   */
  public void initDerivatives(double[] yDot) {

    // store orbit parameters
    super.initDerivatives(yDot);

    // orbital elements
    EquinoctialParameters equinoctialParameters =
      (EquinoctialParameters) parameters;
    double a =  equinoctialParameters.getA();
    double ex = equinoctialParameters.getEquinoctialEx();
    double ey = equinoctialParameters.getEquinoctialEy();
    double hx = equinoctialParameters.getHx();
    double hy = equinoctialParameters.getHy();
    double lv = equinoctialParameters.getLv();

    // intermediate variables
    double ex2 = ex * ex;
    double ey2 = ey * ey;
    double e2  = ex2 + ey2;
    double e   = Math.sqrt(e2);
    if (e > 1) {
      throw new IllegalArgumentException("Eccentricity is becoming"
                                         + " greater than 1."
                                         + " Unable to continue.");
    }

    // intermediate variables
    double oMe2        = (1 - e) * (1 + e);
    double epsilon     = Math.sqrt(oMe2);
    double na          = Math.sqrt(mu / a);
    double n           = na / a;
    double cLv         = Math.cos(lv);
    double sLv         = Math.sin(lv);
    double excLv       = ex * cLv;
    double eysLv       = ey * sLv;
    double excLvPeysLv = excLv + eysLv;
    double ksi         = 1 + excLvPeysLv;
    double nu          = ex * sLv - ey * cLv;
    double sqrt        = Math.sqrt(ksi * ksi + nu * nu);
    double oPksi       = 2 + excLvPeysLv;
    double h2          = hx * hx + hy * hy;
    double oPh2        = 1 + h2;
    double hxsLvMhycLv = hx * sLv - hy * cLv;

    double epsilonOnNA        = epsilon / na;
    double epsilonOnNAKsi     = epsilonOnNA / ksi;
    double epsilonOnNAKsiSqrt = epsilonOnNAKsi / sqrt;
    double tOnEpsilonN        = 2 / (n * epsilon);
    double tEpsilonOnNASqrt   = 2 * epsilonOnNA / sqrt;
    double epsilonOnNAKsit    = epsilonOnNA / (2 * ksi);
    
    // Kepler natural evolution
    lvKepler = n * ksi * ksi / (oMe2 * epsilon);

    // coefficients along T
    aT  = tOnEpsilonN * sqrt;
    exT = tEpsilonOnNASqrt * (ex + cLv);
    eyT = tEpsilonOnNASqrt * (ey + sLv);
        
    // coefficients along N
    exN = -epsilonOnNAKsiSqrt * (2 * ey * ksi + oMe2 * sLv);
    eyN =  epsilonOnNAKsiSqrt * (2 * ex * ksi + oMe2 * cLv);
                
    // coefficients along Q
    aQ  =  tOnEpsilonN * nu;
    exQ =  epsilonOnNA * sLv;
    eyQ = -epsilonOnNA * cLv;
        
    // coefficients along S
    aS  = tOnEpsilonN * ksi;
    exS = epsilonOnNAKsi * (ex + oPksi * cLv);
    eyS = epsilonOnNAKsi * (ey + oPksi * sLv);
        
    // coefficients along W
    lvW =  epsilonOnNAKsi * hxsLvMhycLv;
    exW = -ey * lvW;
    eyW =  ex * lvW;
    hxW =  epsilonOnNAKsit * oPh2 * cLv;
    hyW =  epsilonOnNAKsit * oPh2 * sLv;
    //hxW =  epsilonOnNAKsi * oPh2 * cLv;
    //hyW =  epsilonOnNAKsi * oPh2 * sLv;
    
  }

  /** Add the contribution of the Kepler evolution.
   * <p>Since the Kepler evolution if the most important, it should
   * be added after all the other ones, in order to improve
   * numerical accuracy.</p>
   */
  public void addKeplerContribution() {
    yDot[5] += lvKepler;
  }

  /** Add the contribution of an acceleration expressed in (T, N, W)
   * local orbital frame.
   * @param t acceleration along the T axis (m/s<sup>2</sup>)
   * @param n acceleration along the N axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public void addTNWAcceleration(double t, double n, double w) {
    yDot[0] += aT  * t;
    yDot[1] += exT * t + exN * n + exW * w;
    yDot[2] += eyT * t + eyN * n + eyW * w;
    yDot[3] += hxW * w;
    yDot[4] += hyW * w;
    yDot[5] += lvW * w;
  }

  /** Add the contribution of an acceleration expressed in (Q, S, W)
   * local orbital frame.
   * @param q acceleration along the Q axis (m/s<sup>2</sup>)
   * @param s acceleration along the S axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public void addQSWAcceleration(double q, double s, double w) {
    yDot[0] += aQ  * q + aS  * s;
    yDot[1] += exQ * q + exS * s + exW * w;
    yDot[2] += eyQ * q + eyS * s + eyW * w;
    yDot[3] += hxW * w;
    yDot[4] += hyW * w;
    yDot[5] += lvW * w;
  }

}
