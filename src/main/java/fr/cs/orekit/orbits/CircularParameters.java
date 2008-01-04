package fr.cs.orekit.orbits;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles circular orbital parameters.

 * <p>
 * The parameters used internally are the circular elements defined as follows:
 *   <pre>
 *     a
 *     ex = e cos(&omega;)
 *     ey = e sin(&omega;)
 *     i
 *     &Omega;
 *     &alpha;<sub>v</sub> = v + &omega;
 *   </pre>
 * where &Omega; stands for the Right Ascension of the Ascending Node and
 * &alpha;<sub>v</sub> stands for the true longitude argument
 * </p>
 * <p>
 * The instance <code>CircularParameters</code> is guaranted to be immutable.
 * </p>
 * @see     Orbit
 * @version $Id$
 * @author  L. Maisonobe
 * @author  F.Maussion
 */

public class CircularParameters
 extends OrbitalParameters {

  /** Identifier for mean longitude argument. */
  public static final int MEAN_LONGITUDE_ARGUMENT = 0;

  /** Identifier for eccentric longitude argument. */
  public static final int ECCENTRIC_LONGITUDE_ARGUMENT = 1;

  /** Identifier for true longitude argument. */
  public static final int TRUE_LONGITUDE_ARGUMENT = 2;

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param ex e cos(&omega;), first component of circular eccentricity vector
   * @param ey e sin(&omega;), second component of circular eccentricity vector
   * @param i inclination (rad)
   * @param raan right ascension of ascending node (&Omega;, rad)
   * @param alpha  an + &omega;, mean, eccentric or true longitude argument (rad)
   * @param type type of longitude argument, must be one of {@link #MEAN_LONGITUDE_ARGUMENT},
   * {@link #ECCENTRIC_LONGITUDE_ARGUMENT} or  {@link #TRUE_LONGITUDE_ARGUMENT}
   * @param frame the frame in which are defined the parameters
   */
  public CircularParameters(double a, double ex, double ey, double i, double raan,
                            double alpha, int type, Frame frame) {
    super(frame);
    this.a    =  a;
    this.ex   = ex;
    this.ey   = ey;
    this.i    = i;
    this.raan = raan;

    switch (type) {
    case MEAN_LONGITUDE_ARGUMENT :
      this.alphaV = computeAlphaM(alpha);
      break;
    case ECCENTRIC_LONGITUDE_ARGUMENT :
      this.alphaV = computeAlphaE(alpha);
      break;
    default :
      this.alphaV = alpha;
    break;
    }

  }

  /** Constructor from cartesian parameters.
   * @param pvCoordinates the {@link PVCoordinates} in inertial frame
   * @param frame the frame in which are defined the {@link PVCoordinates}
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CircularParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    super(pvCoordinates, frame, mu);

    // compute semi-major axis
    double r          = pvCoordinates.getPosition().getNorm();
    double V2         = Vector3D.dotProduct(pvCoordinates.getVelocity(), pvCoordinates.getVelocity());
    double rV2OnMu    = r * V2 / mu;
    a                 = r / (2 - rV2OnMu);

    // compute inclination
    Vector3D momentum = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity());
    i                 = Vector3D.angle(momentum, Vector3D.plusK);

    // compute right ascension of ascending node
    Vector3D node     = Vector3D.crossProduct(Vector3D.plusK, momentum);
    double   n2       = Vector3D.dotProduct(node, node);
    // the following comparison with 0 IS REALLY numerically justified and stable
    raan = (n2 == 0) ? 0 : Math.atan2(node.getY(), node.getX());

    // 2D-coordinates in the canonical frame
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double cosI    = Math.cos(i);
    double sinI    = Math.sin(i);
    Vector3D rVec  = new Vector3D(cosRaan, Math.sin(raan), 0);
    Vector3D sVec  = new Vector3D(-cosI * sinRaan, cosI * cosRaan, sinI);
    double x2      = Vector3D.dotProduct(pvCoordinates.getPosition(), rVec) / a;
    double y2      = Vector3D.dotProduct(pvCoordinates.getPosition(), sVec) / a;

    // compute eccentricity vector
    double eSE    = Vector3D.dotProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()) / Math.sqrt(mu * a);
    double eCE    = rV2OnMu - 1;
    double e2     = eCE * eCE + eSE * eSE;
    double f      = eCE - e2;
    double g      = Math.sqrt(1 - e2) * eSE;
    double aOnR   = a / r;
    double a2OnR2 = aOnR * aOnR;
    ex = a2OnR2 * (f * x2 + g * y2);
    ey = a2OnR2 * (f * y2 - g * x2);

    // compute longitude argument
    double beta = 1 / (1 + Math.sqrt(1 - ex * ex - ey * ey));
    alphaV = computeAlphaE(Math.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE * beta * ey));
  }

  /** Constructor from any kind of orbital parameters
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CircularParameters(OrbitalParameters op, double mu) {
    super(op.frame);
    a    = op.getA();
    i    = op.getI();
    raan = Math.atan2(op.getHy(), op.getHx());
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double equiEx = op.getEquinoctialEx();
    double equiEy = op.getEquinoctialEy();
    ex   = equiEx * cosRaan + equiEy * sinRaan;
    ey   = equiEy * cosRaan - equiEx * sinRaan;
    this.alphaV = op.getLv() - raan;
  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    return a;
  }

  /** Get the first component of the equinoctial eccentricity vector.
   * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    return ex * Math.cos(raan) - ey * Math.sin(raan);
  }

  /** Get the second component of the equinoctial eccentricity vector.
   * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    return ey * Math.cos(raan) + ex * Math.sin(raan);
  }

  /** Get the first component of the circular eccentricity vector.
   * @return ex = e cos(&omega;), first component of the circular eccentricity vector
   */
  public double getCircularEx() {
    return ex;
  }

  /** Get the second component of the circular eccentricity vector.
   * @return ey = e sin(&omega;), second component of the circular eccentricity vector
   */
  public double getCircularEy() {
    return ey;
  }

  /** Get the first component of the inclination vector.
   * @return first component of the inclination vector.
   */
  public double getHx() {
    return  Math.cos(raan) * Math.tan(i / 2);
  }

  /** Get the second component of the inclination vector.
   * @return second component of the inclination vector.
   */
  public double getHy() {
    return  Math.sin(raan) * Math.tan(i / 2);
  }

  /** Get the true longitude argument.
   * @return v + &omega; true longitude argument (rad)
   */
  public double getAlphaV() {
    return alphaV;
  }

  /** Get the eccentric longitude argument.
   * @return E + &omega; eccentric longitude argument (rad)
   */
  public double getAlphaE() {
    double epsilon   = Math.sqrt(1 - ex * ex - ey * ey);
    double cosAlphaV = Math.cos(alphaV);
    double sinAlphaV = Math.sin(alphaV);
    return alphaV + 2 * Math.atan((ey * cosAlphaV - ex * sinAlphaV)
                                  / (epsilon + 1 + ex * cosAlphaV + ey * sinAlphaV));
  }

  /** Computes the eccentric longitude argument.
   * @param alphaE = E + &omega; eccentric longitude argument (rad)
   * @return the true longitude argument.
   */
  private double computeAlphaE(double alphaE) {
    double epsilon   = Math.sqrt(1 - ex * ex - ey * ey);
    double cosAlphaE = Math.cos(alphaE);
    double sinAlphaE = Math.sin(alphaE);
    return alphaE + 2 * Math.atan((ex * sinAlphaE - ey * cosAlphaE)
                                     / (epsilon + 1 - ex * cosAlphaE - ey * sinAlphaE));
  }

  /** Get the mean longitude argument.
   * @return M + &omega; mean longitude argument (rad)
   */
  public double getAlphaM() {
    double alphaE = getAlphaE();
    return alphaE - ex * Math.sin(alphaE) + ey * Math.cos(alphaE);
  }

  /** Computes the mean longitude argument.
   * @param alphaM = M + &omega;  mean longitude argument (rad)
   * @return the true longitude argument.
   */
  private double computeAlphaM(double alphaM) {
    // Generalization of Kepler equation to equinoctial parameters
    // with alphaE = PA + E and
    //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
    double alphaE = alphaM;
    double shift = 0.0;
    double alphaEMalphaM = 0.0;
    double cosLE = Math.cos(alphaE);
    double sinLE = Math.sin(alphaE);
    int iter = 0;
    do {
      double f2 = ex * sinLE - ey * cosLE;
      double f1 = 1.0 - ex * cosLE - ey * sinLE;
      double f0 = alphaEMalphaM - f2;

      double f12 = 2.0 * f1;
      shift = f0 * f12 / (f1 * f12 - f0 * f2);

      alphaEMalphaM -= shift;
      alphaE         = alphaM + alphaEMalphaM;
      cosLE          = Math.cos(alphaE);
      sinLE          = Math.sin(alphaE);

    } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

    return computeAlphaE(alphaE); // which set the alphaV parameter

  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    return Math.sqrt(ex * ex + ey * ey);
  }

  /** Get the inclination.
   * @return inclination (rad)
   */
  public double getI() {
    return i;
  }

  /** Get the right ascension of the ascending node.
   * @return right ascension of the ascending node (rad)
   */
  public double getRightAscensionOfAscendingNode() {
    return raan;
  }

  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public double getLv() {
    return alphaV + raan;
  }

  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument.(rad)
   */
  public double getLE() {
    return getAlphaE() + raan;
  }

  /** Get the mean latitude argument.
   * @return mean latitude argument.(rad)
   */
  public double getLM() {
    return getAlphaM() + raan;
  }

  /**  Returns a string representation of this Orbit object
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("circular parameters: ");
    sb.append('{');
    sb.append("a: ");
    sb.append(a);
    sb.append(";ex: ");
    sb.append(ex);
    sb.append(";ey: ");
    sb.append(ey);
    sb.append(";i: ");
    sb.append(i);
    sb.append(";raan: ");
    sb.append(raan);
    sb.append(";alphaV: ");
    sb.append(Math.toDegrees(alphaV));
    sb.append(";}");
    return sb.toString();
  }

  /** Semi-major axis (m). */
  private final double a;

  /** First component of the circular eccentricity vector. */
  private final double ex;

  /** Second component of the circular eccentricity vector. */
  private final double ey;

  /** Inclination (rad). */
  private final double i;

  /** Right Ascension of Ascending Node (rad). */
  private final double raan;

  /** True longitude argument (rad). */
  private final double alphaV;

  private static final long serialVersionUID = -6724645584654038446L;

}
