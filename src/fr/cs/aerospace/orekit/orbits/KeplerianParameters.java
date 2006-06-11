package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

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

 * This class implements the
 * {@link org.spaceroots.mantissa.utilities.ArraySliceMappable ArraySliceMappable}
 * interface from the <a
 * href="http://www.spaceroots.org/archive.htm#MantissaSoftware">mantissa</a>
 * library, hence it can easily be processed by a numerical integrator.

 * @see     Orbit
 * @see     org.spaceroots.mantissa.utilities.ArraySliceMappable
 * @version $Id$
 * @author  L. Maisonobe
 * @author  G. Prat

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
  
  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public KeplerianParameters() {
    reset();
  }

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param e eccentricity
   * @param i inclination (rad)
   * @param pa perigee argument (&omega;, rad)
   * @param raan right ascension of ascending node (&Omega;, rad)
   * @param anomaly mean, eccentric or true anomaly (rad)
   * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
   * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
   */
  public KeplerianParameters(double a, double e, double i,
                             double pa, double raan,
                             double anomaly, int type) {
    reset(a, e, i, pa, raan, anomaly, type);
  }

  /** Constructor from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianParameters(Vector3D position, Vector3D velocity, double mu) {
    reset(position, velocity, mu);
  }

  /** Constructor from any kind of orbital parameters.
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianParameters(OrbitalParameters op, double mu) {
    reset(op, mu);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new KeplerianParameters(a, e, i, pa, raan, v, TRUE_ANOMALY);
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    a    = 1.0e7;
    e    = 1.0e-3;
    i    = 0.3;
    pa   = 0;
    raan = 0;
    setTrueAnomaly(0);
  }

  /** Reset the orbit from orbital parameters
   * @param a  semi-major axis (m)
   * @param e eccentricity
   * @param i inclination (rad)
   * @param pa perigee argument (&omega;, rad)
   * @param raan right ascension of ascending node (rad)
   * @param anomaly mean, eccentric or true anomaly (rad)
   * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
   * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
   */
  public void reset(double a, double e, double i, double pa, double raan,
                    double anomaly, int type) {

    this.a    =    a;
    this.e    =    e;
    this.i    =    i;
    this.pa   =   pa;
    this.raan = raan;

    switch (type) {
    case MEAN_ANOMALY :
      setMeanAnomaly(anomaly);
      break;
    case ECCENTRIC_ANOMALY :
      setEccentricAnomaly(anomaly);
      break;
    default :
      setTrueAnomaly(anomaly);
    }

  }

  protected void doReset(OrbitalParameters op, double mu) {
    a    = op.getA();
    e    = op.getE();
    i    = op.getI();
    raan = Math.atan2(op.getHy(), op.getHx());
    pa   = Math.atan2(op.getEquinoctialEy(), op.getEquinoctialEx()) - raan;
    setTrueAnomaly(op.getLv() - (pa + raan));
  }

  /** Update the parameters from the current position and velocity. */
  protected void updateFromPositionAndVelocity() {

    // get cartesian elements
    double   mu       = getCachedMu();
    Vector3D position = getPosition(mu);
    Vector3D velocity = getVelocity(mu);

    // compute semi-major axis
    double r          = position.getNorm();
    double V2         = Vector3D.dotProduct(velocity, velocity);
    double rV2OnMu    = r * V2 / mu;
    a                 = r / (2 - rV2OnMu);

    // compute eccentricity
    double muA        = mu * a;
    double eSE        = Vector3D.dotProduct(position, velocity) / Math.sqrt(muA);
    double eCE        = rV2OnMu - 1;
    e                 = Math.sqrt(eSE * eSE + eCE * eCE);

    // compute inclination
    Vector3D momentum = Vector3D.crossProduct(position, velocity);
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
    double px = cosRaan * position.getX() + sinRaan * position.getY();
    double py = Math.cos(i) * (cosRaan * position.getY() - sinRaan * position.getX())
              + Math.sin(i) * position.getZ();
    pa = Math.atan2(py, px) - v;

  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    return a;
  }

  /** Set the semi-major axis.
   * @param a semi-major axis (m)
   */
  public void setA(double a) {

    this.a = a;

    // invalidate position and velocity
    reset();

  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    return e;
  }

  /** Set the eccentricity.
   * @param e eccentricity
   */
  public void setE(double e) {

    this.e = e;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the inclination.
   * @return inclination (rad)
   */
  public double getI() {
    return i;
  }

  /** Set the inclination.
   * @param i inclination (rad)
   */
  public void setI(double i) {

    this.i = i;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the perigee argument.
   * @return perigee argument (rad)
   */
  public double getPerigeeArgument() {
    return pa;
  }

  /** Set the perigee argument.
   * @param pa perigee argument (rad)
   */
  public void setPerigeeArgument(double pa) {

    this.pa = pa;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the right ascension of the ascending node.
   * @return right ascension of the ascending node (rad)
   */
  public double getRightAscensionOfAscendingNode() {
    return raan;
  }

  /** Set the right ascension of ascending node.
   * @param raan right ascension of ascending node (rad)
   */
  public void setRightAscensionOfAscendingNode(double raan) {

    this.raan = raan;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the true anomaly.
   * @return true anomaly (rad)
   */
  public double getTrueAnomaly() {
    return v;
  }

  /** Set the true anomaly.
   * @param v true anomaly (rad)
   */
  public void setTrueAnomaly (double v) {

    this.v = v;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the eccentric anomaly.
   * @return eccentric anomaly (rad)
   */
  public double getEccentricAnomaly() {
    double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
    return v - 2 * Math.atan(beta * Math.sin(v) / (1 + beta * Math.cos(v)));
  }

  /** Set the eccentric anomaly.
   * @param E eccentric anomaly (rad)
   */
  public void setEccentricAnomaly (double E) {

    double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
    v = E + 2 * Math.atan(beta * Math.sin(E) / (1 - beta * Math.cos(E)));

    // invalidate position and velocity
    super.reset();

  }

  /** Get the mean anomaly.
   * @return mean anomaly (rad)
   */
  public double getMeanAnomaly() {
    double E = getEccentricAnomaly();
    return E - e * Math.sin(E);
  }

  /** Set the mean anomaly.
   * @param M mean anomaly (rad)
   */
  public void setMeanAnomaly (double M) {
    
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

    setEccentricAnomaly(E);

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
    sb.append('{');
    sb.append(a);
    sb.append(' ');
    sb.append(e);
    sb.append(' ');
    sb.append(Math.toDegrees(i));
    sb.append(' ');
    sb.append(Math.toDegrees(pa));
    sb.append(' ');
    sb.append(Math.toDegrees(raan));
    sb.append(' ');
    sb.append(Math.toDegrees(v));
    sb.append('}');
    return sb.toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, an {@link KeplerianDerivativesAdder
   * KeplerianDerivativesAdder} object is built.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return an instance of {@link KeplerianDerivativesAdder
   * KeplerianDerivativesAdder} associated with this object
   */
  public OrbitDerivativesAdder getDerivativesAdder(double mu) {
    return new KeplerianDerivativesAdder(this, mu);
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, e, i, pa, raan, v)
   */
  public void mapStateFromArray(int start, double[] array) {

    a    = array[start];
    e    = array[start + 1];
    i    = array[start + 2];
    pa   = array[start + 3];
    raan = array[start + 4];
    v    = array[start + 5];

    // invalidate position and velocity
    super.reset();

  }

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, e, i, pa, raan, v)
   */
  public void mapStateToArray(int start, double[] array) {
    array[start]     = a;
    array[start + 1] = e;
    array[start + 2] = i;
    array[start + 3] = pa;
    array[start + 4] = raan;
    array[start + 5] = v;
  }

  /** Semi-major axis (m). */
  private double a;

  /** Eccentricity. */
  private double e;

  /** Inclination (rad). */
  private double i;

  /** Perigee Argument (rad). */
  private double pa;

  /** Right Ascension of Ascending Node (rad). */
  private double raan;

  /** True anomaly (rad). */
  private double v;

}
