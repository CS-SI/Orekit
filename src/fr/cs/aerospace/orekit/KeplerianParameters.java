package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class handles keplerian orbital parameters.

 * <p>
 * The parameters used internally are the classical keplerian elements:
 *   <pre>
 *     a
 *     e
 *     i
 *     PA
 *     RAAN
 *     v
 *   </pre>
 * where PA stands for the Perigee Argument (usually small omega) and RAAN
 * stands for the Right Ascension of the Ascending Node (usually big omega).
 * </p>

 * This class implements the
 * {@link org.spaceroots.mantissa.utilities.ArraySliceMappable
 * ArraySliceMappable} interface from the <a
 * href="http://www.spaceroots.org/archive.htm#MantissaSoftware">mantissa</a>
 * library, hence it can easily be processed by a numerical integrator.

 * @see     Orbit
 * @see     org.spaceroots.mantissa.utilities.ArraySliceMappable
 * @version $Id$
 * @author  L. Maisonobe

 */
public class KeplerianParameters
  extends OrbitalParameters {

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
   * @param pa perigee argument (rad)
   * @param raan right ascension of ascending node (rad)
   * @param v true anomaly (rad)
   */
  public KeplerianParameters(double a, double e, double i,
                             double pa, double raan, double v) {
    reset(a, e, i, pa, raan, v);
  }

  /** Constructor from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public KeplerianParameters(Vector3D position, Vector3D velocity,
                             double mu) {
    reset(position, velocity, mu);
  }

  /** Copy-constructor.
   * @param op orbit parameters to copy
   */
  public KeplerianParameters(KeplerianParameters op) {
    reset(op);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new KeplerianParameters(this);
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
    v    = 0;

    super.reset();

  }

  /** Reset the orbit from orbital parameters
   * @param a  semi-major axis (m)
   * @param e eccentricity
   * @param i inclination (rad)
   * @param pa perigee argument (rad)
   * @param raan right ascension of ascending node (rad)
   * @param v true anomaly (rad)
   */
  public void reset(double a, double e, double i,
                    double pa, double raan, double v) {

    this.a    =    a;
    this.e    =    e;
    this.i    =    i;
    this.pa   =   pa;
    this.raan = raan;
    this.v    =    v;

    super.reset();

  }

  /** Reset the orbit from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public void reset(Vector3D position, Vector3D velocity, double mu) {

    double r  = position.getNorm();
    double V2 = Vector3D.dotProduct(velocity, velocity);
    double V  = Math.sqrt(V2);

    double rV2OnMu = r * V2 / mu;
    a = r / (2 - rV2OnMu);
    double muA = mu * a;

    Vector3D w   = Vector3D.crossProduct(position, velocity);
    double   w2  = Vector3D.dotProduct(w, w);
    double   eSE = Vector3D.dotProduct(position, velocity) / Math.sqrt(muA);
    double   eCE = rV2OnMu - 1;
    e = Math.sqrt(eSE * eSE + eCE * eCE);
    
    if (e < 1.0e-12) {
      v = 0;
    } else {
      double E = Math.atan2(eSE, eCE);
      double k = 1 / (1 + Math.sqrt(w2 / muA));
      v = E + 2 * Math.atan(k * eSE / (1 - k *eCE));
    }

    w.multiplySelf(1 / Math.sqrt(w2));
    double x = w.getX();
    double y = w.getY();
    double cosI = w.getZ();
    double sinI = Math.sqrt(x * x + y * y);
    double cosRaan;
    double sinRaan;
    if (Math.abs(cosI) < 0.99) {
      i = Math.acos(cosI);
      cosRaan = -y / sinI;
      sinRaan =  x / sinI;
    } else {
      i = Math.asin(sinI);
      if (sinI < 1.0e-12) {
        cosRaan = 1;
        sinRaan = 0;
      } else {
        cosRaan = -y / sinI;
        sinRaan =  x / sinI;
      }
    }
    raan = Math.atan2(sinRaan, cosRaan);

    double px = cosRaan * position.getX() + sinRaan * position.getY();
    double py = cosI * (cosRaan * position.getY()
                      - sinRaan * position.getX())
              + sinI * position.getZ();
    pa = Math.atan2(py, px) - v;

    super.reset(position, velocity, mu);

  }

  /** Reset the orbit from another one.
   * @param op orbit parameters to copy
   */
  public void reset(KeplerianParameters op) {

    a    = op.a;
    e    = op.e;
    i    = op.i;
    pa   = op.pa;
    raan = op.raan;
    v    = op.v;

    super.reset(op);

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

    // force position and velocity recomputation
    super.reset();

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

    // force position and velocity recomputation
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

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the perigee argument.
   * @return perigee argument (rad)
   */
  public double getPA() {
    return pa;
  }

  /** Set the perigee argument.
   * @param pa perigee argument (rad)
   */
  public void setPA(double pa) {

    this.pa = pa;

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the right ascension of the ascending node.
   * @return right ascension of the ascending node (rad)
   */
  public double getRAAN() {
    return raan;
  }

  /** Set the right ascension of ascending node.
   * @param raan right ascension of ascending node (rad)
   */
  public void setRAAN(double raan) {

    this.raan = raan;

    // force position and velocity recomputation
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

    // force position and velocity recomputation
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

    // force position and velocity recomputation
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

    } while ((++iter < 10) && (Math.abs(shift) > 1.0e-12));

    setEccentricAnomaly(E);

  }

  /** Compute and cache the cartesian parameters.
   * @param mu central body gravitational constant (m^3/s^2)
   */
  protected void initPositionAndVelocity(double mu) {

    // inclination-related intermediate parameters
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double cosI    = Math.cos(i);
    double sinI    = Math.sin(i);

    // in-plane parameters
    double cosPa   = Math.cos(pa);
    double sinPa   = Math.sin(pa);

    double cpcr    = cosPa * cosRaan;
    double cpsr    = cosPa * sinRaan;
    double spcr    = sinPa * cosRaan;
    double spsr    = sinPa * sinRaan;

    double epsilon = Math.sqrt((1 - e) * (1 + e));

    // reference axes defining the orbital plane
    double ux = cpcr - cosI * spsr;
    double uy = cpsr + cosI * spcr;
    double uz = sinI * sinPa;

    double vx = -spcr - cosI * cpsr;
    double vy = cosI * cpcr - spsr;
    double vz = sinI * cosPa;

    // eccentric anomaly
    double E     = getEccentricAnomaly();
    double cosE  = Math.cos(E);
    double sinE  = Math.sin(E);

    // coordinates of position and velocity in the orbital plane
    double x      = a * (cosE - e);
    double y      = a * epsilon * sinE;

    double factor = Math.sqrt(mu / a) / (1 - e * cosE);
    double xdot   = -factor * sinE;
    double ydot   =  factor * cosE * epsilon;

    // cache the computed values
    cachedMu = mu;

    cachedPosition.setCoordinates(x * ux + y * vx,
                                  x * uy + y * vy,
                                  x * uz + y * vz);

    cachedVelocity.setCoordinates(xdot * ux + ydot * vx,
                                  xdot * uy + ydot * vy,
                                  xdot * uz + ydot * vz);

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
   * @param mu central body gravitational constant (m^3/s^2)
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

    // force position and velocity recomputation
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

}
