package fr.cs.orekit.orbits;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles keplerian orbital parameters.

 * <p>
 * The parameters used internally are the classical keplerian elements:
 *   <pre>
 *     a
 *     e
 *     i
 *     &omega;
 *     &Omega;
 *     v
 *   </pre>
 * where &omega; stands for the Perigee Argument, &Omega; stands for the
 * Right Ascension of the Ascending Node and v stands for the true anomaly.
 * </p>
 * <p>
 * The instance <code>KeplerianParameters</code> is guaranted to be immutable.
 * </p>
 * @see     Orbit
 * @version $Id:KeplerianParameters.java 1310 2007-07-05 16:04:25Z luc $
 * @author  L. Maisonobe
 * @author  G. Prat
 * @author  F.Maussion
 */
public class KeplerianParameters
extends OrbitalParameters {

  /** Identifier for mean anomaly. */
  public static final int MEAN_ANOMALY = 0;

  /** Identifier for eccentric anomaly. */
  public static final int ECCENTRIC_ANOMALY = 1;

  /** Identifier for true anomaly. */
  public static final int TRUE_ANOMALY = 2;

  /** Eccentricity threshold for near circular orbits.
   *  if e < E_CIRC : the orbit is considered circular
   */
  public static final double E_CIRC = 1.e-10;

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param e eccentricity
   * @param i inclination (rad)
   * @param pa perigee argument (&omega;, rad)
   * @param raan right ascension of ascending node (&Omega;, rad)
   * @param anomaly mean, eccentric or true anomaly (rad)
   * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
   * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
   * @param frame the frame in which are defined the parameters
   */
  public KeplerianParameters(double a, double e, double i,
                             double pa, double raan,
                             double anomaly, int type, Frame frame) {
    super(frame);
    this.a    =    a;
    this.e    =    e;
    this.i    =    i;
    this.pa   =   pa;
    this.raan = raan;

    switch (type) {
    case MEAN_ANOMALY :
      this.v = computeMeanAnomaly(anomaly);
      break;
    case ECCENTRIC_ANOMALY :
      this.v = computeEccentricAnomaly(anomaly);
      break;
    default :
      this.v = anomaly;
    break;
    }
  }

  /** Constructor from cartesian parameters.
   * @param pvCoordinates the PVCoordinates of the satellite
   * @param frame the frame in which are defined the {@link PVCoordinates}
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    super(pvCoordinates, frame, mu);

    // compute semi-major axis
    double r          = pvCoordinates.getPosition().getNorm();
    double V2         = Vector3D.dotProduct(pvCoordinates.getVelocity(), pvCoordinates.getVelocity());
    double rV2OnMu    = r * V2 / mu;
    a                 = r / (2 - rV2OnMu);

    // compute eccentricity
    double muA        = mu * a;
    double eSE        = Vector3D.dotProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()) / Math.sqrt(muA);
    double eCE        = rV2OnMu - 1;
    e                 = Math.sqrt(eSE * eSE + eCE * eCE);

    // compute inclination
    Vector3D momentum = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity());
    double   m2       = Vector3D.dotProduct(momentum, momentum);
    i = Vector3D.angle(momentum, Vector3D.plusK);

    // compute right ascension of ascending node
    Vector3D node     = Vector3D.crossProduct(Vector3D.plusK, momentum);
    double   n2       = Vector3D.dotProduct(node, node);
    // the following comparison with 0 IS REALLY numerically justified and stable
    raan = (n2 == 0) ? 0 : Math.atan2(node.getY(), node.getX());

    // compute true anomaly
    if (e < E_CIRC) {
      v = 0;
    } else {
      double E = Math.atan2(eSE, eCE);
      double k = 1 / (1 + Math.sqrt(m2 / muA));
      v = E + 2 * Math.atan(k * eSE / (1 - k *eCE));
    }

    // compute perigee argument
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double px = cosRaan * pvCoordinates.getPosition().getX() + sinRaan * pvCoordinates.getPosition().getY();
    double py = Math.cos(i) * (cosRaan * pvCoordinates.getPosition().getY() - sinRaan * pvCoordinates.getPosition().getX())
    + Math.sin(i) * pvCoordinates.getPosition().getZ();
    pa = Math.atan2(py, px) - v;
  }

  /** Constructor from any kind of orbital parameters.
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianParameters(OrbitalParameters op, double mu) {
    super(op.frame);
    a    = op.getA();
    e    = op.getE();
    i    = op.getI();
    raan = Math.atan2(op.getHy(), op.getHx());
    pa   = Math.atan2(op.getEquinoctialEy(), op.getEquinoctialEx()) - raan;
    v    = op.getLv() - (pa + raan);
  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    return a;
  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    return e;
  }

  /** Get the inclination.
   * @return inclination (rad)
   */
  public double getI() {
    return i;
  }

  /** Get the perigee argument.
   * @return perigee argument (rad)
   */
  public double getPerigeeArgument() {
    return pa;
  }

  /** Get the right ascension of the ascending node.
   * @return right ascension of the ascending node (rad)
   */
  public double getRightAscensionOfAscendingNode() {
    return raan;
  }

  /** Get the true anomaly.
   * @return true anomaly (rad)
   */
  public double getTrueAnomaly() {
    return v;
  }

  /** Get the eccentric anomaly.
   * @return eccentric anomaly (rad)
   */
  public double getEccentricAnomaly() {
    double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
    return v - 2 * Math.atan(beta * Math.sin(v) / (1 + beta * Math.cos(v)));
  }

  /** Computes the eccentric anomaly.
   * @param E eccentric anomaly (rad)
   * @return v the true anomaly
   */
  private double computeEccentricAnomaly (double E) {

    double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
    return E + 2 * Math.atan(beta * Math.sin(E) / (1 - beta * Math.cos(E)));

  }

  /** Get the mean anomaly.
   * @return mean anomaly (rad)
   */
  public double getMeanAnomaly() {
    double E = getEccentricAnomaly();
    return E - e * Math.sin(E);
  }

  /** Computes the mean anomaly.
   * @param M mean anomaly (rad)
   * @return v the true anomaly
   */
  private double computeMeanAnomaly (double M) {

    // resolution of Kepler equation for keplerian parameters
    double E = M;
    double shift = 0.0;
    double EmM   = 0.0;
    int iter = 0;
    do {
      double f2 = e * Math.sin(E);
      double f1 = 1.0 - e * Math.cos(E);
      double f0 = EmM - f2;

      double f12 = 2 * f1;
      shift = f0 * f12 / (f1 * f12 - f0 * f2);

      EmM -= shift;
      E    = M + EmM;

    } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

    return computeEccentricAnomaly(E);

  }

  /** Get the first component of the eccentricity vector.
   * @return first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    return  e * Math.cos(pa + raan);
  }

  /** Get the second component of the eccentricity vector.
   * @return second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    return  e * Math.sin(pa + raan);
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

  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public double getLv() {
    return pa + raan + v;
  }

  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument.(rad)
   */
  public double getLE() {
    return pa + raan + getEccentricAnomaly();
  }

  /** Get the mean latitude argument.
   * @return mean latitude argument.(rad)
   */
  public double getLM() {
    return pa + raan + getMeanAnomaly();
  }

  /**  Returns a string representation of this Orbit object
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("keplerian parameters: ");
    sb.append('{');
    sb.append("a: ");
    sb.append(a);
    sb.append("; e: ");
    sb.append(e);
    sb.append("; i: ");
    sb.append(Math.toDegrees(i));
    sb.append("; pa: ");
    sb.append(Math.toDegrees(pa));
    sb.append("; raan: ");
    sb.append(Math.toDegrees(raan));
    sb.append("; lv: ");
    sb.append(Math.toDegrees(v));
    sb.append(";}");
    return sb.toString();
  }

  /** Semi-major axis (m). */
  private final double a;

  /** Eccentricity. */
  private final double e;

  /** Inclination (rad). */
  private final double i;

  /** Perigee Argument (rad). */
  private final double pa;

  /** Right Ascension of Ascending Node (rad). */
  private final double raan;

  /** True anomaly (rad). */
  private final double v;

  private static final long serialVersionUID = -2635247116374550475L;
}
