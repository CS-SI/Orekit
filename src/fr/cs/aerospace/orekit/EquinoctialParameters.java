package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class handles equinoctial orbital parameters.

 * <p>
 * The parameters used internally are the equinoctial elements defined as follows:
 *   <pre>
 *     a
 *     ex = e cos(PA + RAAN)
 *     ey = e sin(PA + RAAN)
 *     hx = tan(i/2) cos(RAAN)
 *     hy = tan(i/2) sin(RAAN)
 *     lv = v + PA + RAAN
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
 * @author  M. Romero
 * @author  L. Maisonobe

 */
public class EquinoctialParameters
  extends OrbitalParameters {

  /** Semi-major axis (m). */
  private double a;

  /** First component of the eccentricity vector. */
  private double ex;

  /** Second component of the eccentricity vector. */
  private double ey;

  /** First component of the inclination vector. */
  private double hx;

  /** Second component of the inclination vector. */
  private double hy;

  /** True latitude argument (rad). */
  private double lv;

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public EquinoctialParameters() {
    reset();
  }

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param ex first component of the eccentricity vector
   * @param ey second component of the eccentricity vector
   * @param hx first component of the inclination vector
   * @param hy second component of the inclination vector
   * @param lv latitude argument (rad)
   */
  public EquinoctialParameters(double a,
                               double ex, double ey,
                               double hx, double hy,
                               double lv) {
    reset(a, ex, ey, hx, hy, lv);
  }

  /** Constructor from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public EquinoctialParameters(Vector3D position, Vector3D velocity,
                               double mu) {
    reset(position, velocity, mu);
  }

  /** Copy-constructor.
   * @param op orbit parameters to copy
   */
  public EquinoctialParameters(EquinoctialParameters op) {
    reset(op);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new EquinoctialParameters(this);
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {

    a  = 1.0e7;
    ex = 1.0e-3;
    ey = 0;
    hx = 0.15;
    hy = 0;
    lv = 0;

    super.reset();

  }

  /** Reset the orbit from orbital parameters
   * @param a  semi-major axis (m)
   * @param ex first component of the eccentricity vector
   * @param ey second component of the eccentricity vector
   * @param hx first component of the inclination vector
   * @param hy second component of the inclination vector
   * @param lv latitude argument (rad)
   */
  public void reset(double a, double ex, double ey,
                    double hx, double hy, double lv) {

    this.a  =  a;
    this.ex = ex;
    this.ey = ey;
    this.hx = hx;
    this.hy = hy;
    this.lv = lv;

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

    Vector3D w = Vector3D.crossProduct(position, velocity);
    w.normalizeSelf();
    double d = 1 / (1 + w.getZ());
    hx = -d * w.getY();
    hy =  d * w.getX();

    double cLv = (position.getX() - d * position.getZ() * w.getX()) / r;
    double sLv = (position.getY() - d * position.getZ() * w.getY()) / r;
    lv = Math.atan2(sLv, cLv);

    double eSE = Vector3D.dotProduct(position, velocity) / Math.sqrt(mu * a);
    double eCE = rV2OnMu - 1;
    double e2  = eCE * eCE + eSE * eSE;
    double f   = eCE - e2;
    double g   = Math.sqrt(1 - e2) * eSE;
    ex = a * (f * cLv + g * sLv) / r;
    ey = a * (f * sLv - g * cLv) / r;

    super.reset(position, velocity, mu);

  }

  /** Reset the orbit from another one.
   * @param op orbit parameters to copy
   */
  public void reset(EquinoctialParameters op) {

    a  =  op.a;
    ex = op.ex;
    ey = op.ey;
    hx = op.hx;
    hy = op.hy;
    lv = op.lv;

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

  /** Get the first component of the eccentricity vector.
   * @return first component of the eccentricity vector
   */
  public double getEx() {
    return ex;
  }

  /** Set the first component of the eccentricity vector.
   * @param ex first component of the eccentricity vector
   */
  public void setEx(double ex) {

    this.ex = ex;

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the second component of the eccentricity vector.
   * @return second component of the eccentricity vector
   */
  public double getEy() {
    return ey;
  }

  /** Set the second component of the eccentricity vector.
   * @param ey second component of the eccentricity vector
   */
  public void setEy(double ey) {

    this.ey = ey;

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the first component of the inclination vector.
   * @return first component of the inclination vector
   */
  public double getHx() {
    return hx;
  }

  /** Set the first component of the inclination vector.
   * @param hx first component of the inclination vector
   */
  public void setHx(double hx) {

    this.hx = hx;

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the second component of the inclination vector.
   * @return second component of the inclination vector
   */
  public double getHy() {
    return hy;
  }

  /** Set the second component of the inclination vector.
   * @param hy second component of the inclination vector
   */
  public void setHy(double hy) {

    this.hy = hy;

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public double getLv() {
    return lv;
  }

  /** Set the true latitude argument.
   * @param lv true latitude argument (rad)
   */
  public void setLv(double lv) {

    this.lv = lv;

    // force position and velocity recomputation
    super.reset();

  }

  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument (rad)
   */
  public double getLE() {
    double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
    double cosLv   = Math.cos(lv);
    double sinLv   = Math.sin(lv);
    return lv + 2 * Math.atan((ey * cosLv - ex * sinLv)
                            / (epsilon + 1 + ex * cosLv + ey *sinLv));
  }

  /** Set the eccentric latitude argument.
   * @param lE eccentric latitude argument (rad)
   */
  public void setLE(double lE) {
    double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
    double cosLE   = Math.cos(lE);
    double sinLE   = Math.sin(lE);
    setLv(lE + 2 * Math.atan((ex * sinLE - ey * cosLE)
                           / (epsilon + 1 - ex * cosLE - ey *sinLE)));
  }

  /** Get the mean latitude argument.
   * @return mean latitude argument (rad)
   */
  public double getLM() {
    double lE = getLE();
    return lE - ex * Math.sin(lE) + ey * Math.cos(lE);
  }

  /** Set the mean latitude argument.
   * @param lM mean latitude argument (rad)
   */
  public void setLM(double lM) {
    double lE = lM;
    double shift = 0.0;
    double lEmlM = 0.0;
    double cosLE = Math.cos(lE);
    double sinLE = Math.sin(lE);
    int iter = 0;
    do {
      double f2 = ex * sinLE - ey * cosLE;
      double f1 = 1.0 - ex * cosLE - ey * sinLE;
      double f0 = lEmlM - f2;

      double f12 = 2 * f1;
      shift = f0 * f12 / (f1 * f12 - f0 * f2);

      lEmlM -= shift;
      lE     = lM + lEmlM;
      cosLE  = Math.cos(lE);
      sinLE  = Math.sin(lE);

    } while ((++iter < 10) && (Math.abs(shift) > 1.0e-12));

    setLE(lE);

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
    return 2 * Math.atan(Math.sqrt(hx * hx + hy * hy));
  }

  /** Get the perigee argument.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), zero is returned
   * @return perigee argument (rad)
   */
  public double getPA() {
    return ((getE() < 1.0e-6) || (getI() < 1.0e-6))
           ? 0
           : (Math.atan2(ey, ex) - getRAAN());
  }

  /** Get the right ascension of the ascending node.
   * If the orbit is almost equatorial (i < 1.0e-6), zero is returned
   * @return right ascension of the ascending node (rad)
   */
  public double getRAAN() {
    return (getI() < 1.0e-6) ? 0 : Math.atan2(hy, hx);
  }

  /** Get the true anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return true anomaly (rad)
   */
  public double getTrueAnomaly() {
    return getLv() - getPA() - getRAAN();
  }

  /** Get the eccentric anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return eccentric anomaly (rad)
   */
  public double getEccentricAnomaly() {
    return getLE() - getPA() - getRAAN();
  }

  /** Get the mean anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return mean anomaly (rad)
   */
  public double getMeanAnomaly() {
    return getLM() - getPA() - getRAAN();
  }

  /** Compute and cache the cartesian parameters.
   * @param  mu central body gravitational constant (m^3/s^2)
   */
  protected void initPositionAndVelocity(double mu) {

    // inclination-related intermediate parameters
    double hx2   = hx * hx;
    double hy2   = hy * hy;
    double factH = 1 / (1 + hx2 + hy2);

    // reference axes defining the orbital plane
    double ux = (1 + hx2 - hy2) * factH;
    double uy =  2 * hx * hy * factH;
    double uz = -2 * hy * factH;

    double vx = uy;
    double vy = (1 - hx2 + hy2) * factH;
    double vz =  2 * hx * factH;

    // eccentricity-related intermediate parameters
    double exey = ex * ey;
    double ex2  = ex * ex;
    double ey2  = ey * ey;
    double e2   = ex2 + ey2;
    double eta  = 1 + Math.sqrt(1 - e2);
    double beta = 1 / eta;

    // eccentric latitude argument
    double lE     = getLE();
    double cLe    = Math.cos(lE);
    double sLe    = Math.sin(lE);
    double exCeyS = ex * cLe + ey * sLe;

    // coordinates of position and velocity in the orbital plane
    double x      = a * ((1 - beta * ey2) * cLe + beta * exey * sLe - ex);
    double y      = a * ((1 - beta * ex2) * sLe + beta * exey * cLe - ey);

    double factor = Math.sqrt(mu / a) / (1 - exCeyS);
    double xdot   = factor * (-sLe + beta * ey * exCeyS);
    double ydot   = factor * ( cLe - beta * ex * exCeyS);

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
    sb.append(ex);
    sb.append(' ');
    sb.append(ey);
    sb.append(' ');
    sb.append(hx);
    sb.append(' ');
    sb.append(hy);
    sb.append(' ');
    sb.append(Math.toDegrees(lv));
    sb.append('}');
    return sb.toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, an {@link EquinoctialDerivativesAdder
   * EquinoctialDerivativesAdder} object is built.</p>
   * @param mu central body gravitational constant (m^3/s^2)
   * @return an instance of {@link EquinoctialDerivativesAdder
   * EquinoctialDerivativesAdder} associated with this object
   */
  public OrbitDerivativesAdder getDerivativesAdder(double mu) {
    return new EquinoctialDerivativesAdder(this, mu);
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, ex, ey, hx, hy, lv)
   */
  public void mapStateFromArray(int start, double[] array) {

    a  = array[start];
    ex = array[start + 1];
    ey = array[start + 2];
    hx = array[start + 3];
    hy = array[start + 4];
    lv = array[start + 5];

    // force position and velocity recomputation
    super.reset();

  }

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, ex, ey, hx, hy, lv)
   */
  public void mapStateToArray(int start, double[] array) {
    array[start]     = a;
    array[start + 1] = ex;
    array[start + 2] = ey;
    array[start + 3] = hx;
    array[start + 4] = hy;
    array[start + 5] = lv;
  }

}
