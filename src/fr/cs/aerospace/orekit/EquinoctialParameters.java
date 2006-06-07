package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class handles equinoctial orbital parameters.

 * <p>
 * The parameters used internally are the equinoctial elements defined as follows:
 *   <pre>
 *     a
 *     ex = e cos(&omega; + &Omega;)
 *     ey = e sin(&omega; + &Omega;)
 *     hx = tan(i/2) cos(&Omega;)
 *     hy = tan(i/2) sin(&Omega;)
 *     lv = v + &omega; + &Omega;
 *   </pre>
 * where &omega; stands for the Perigee Argument and &Omega; stands for the
 * Right Ascension of the Ascending Node.
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
 * @author  G. Prat

 */
public class EquinoctialParameters
  extends OrbitalParameters {

  /** Identifier for mean latitude argument. */
  public static final int MEAN_LATITUDE_ARGUMENT = 0;

  /** Identifier for eccentric latitude argument. */
  public static final int ECCENTRIC_LATITUDE_ARGUMENT = 1;

  /** Identifier for true latitude argument. */
  public static final int TRUE_LATITUDE_ARGUMENT = 2;

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public EquinoctialParameters() {
    reset();
  }

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param ex e cos(&omega; + &Omega;), first component of eccentricity vector
   * @param ey e sin(&omega; + &Omega;), second component of eccentricity vector
   * @param hx tan(i/2) cos(&Omega;), first component of inclination vector
   * @param hy tan(i/2) sin(&Omega;), second component of inclination vector
   * @param l  an + &omega; + &Omega;, mean, eccentric or true latitude argument (rad)
   * @param type type of latitude argument, must be one of {@link #MEAN_LATITUDE_ARGUMENT},
   * {@link #ECCENTRIC_LATITUDE_ARGUMENT} or  {@link #TRUE_LATITUDE_ARGUMENT}
   */
  public EquinoctialParameters(double a, double ex, double ey,
                               double hx, double hy,
                               double l, int type) {
    reset(a, ex, ey, hx, hy, l, type);
  }

  /** Constructor from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EquinoctialParameters(Vector3D position, Vector3D velocity, double mu) {
    reset(position, velocity, mu);
  }

  /** Constructor from any kind of orbital parameters
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EquinoctialParameters(OrbitalParameters op, double mu) {
    reset(op, mu);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new EquinoctialParameters(a, ex, ey, hx, hy, lv, TRUE_LATITUDE_ARGUMENT);
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
    setLv(0);
  }

  /** Reset the orbit from orbital parameters
   * @param a  semi-major axis (m)
   * @param ex e cos(&omega; + &Omega;), first component of eccentricity vector
   * @param ey e sin(&omega; + &Omega;), second component of eccentricity vector
   * @param hx tan(i/2) cos(&Omega;), first component of inclination vector
   * @param hy tan(i/2) sin(&Omega;), second component of inclination vector
   * @param l  an + &omega; + &Omega;, mean, eccentric or true latitude argument (rad)
   * @param type type of latitude argument, must be one of {@link #MEAN_LATITUDE_ARGUMENT},
   * {@link #ECCENTRIC_LATITUDE_ARGUMENT} or  {@link #TRUE_LATITUDE_ARGUMENT}
   */
  public void reset(double a, double ex, double ey,
                    double hx, double hy, double l, int type) {

    this.a  =  a;
    this.ex = ex;
    this.ey = ey;
    this.hx = hx;
    this.hy = hy;

    switch (type) {
    case MEAN_LATITUDE_ARGUMENT :
      setLM(l);
      break;
    case ECCENTRIC_LATITUDE_ARGUMENT :
      setLE(l);
      break;
    default :
      setLv(l);
    }


  }

  protected void doReset(OrbitalParameters op, double mu) {
    a  = op.getA();
    ex = op.getEquinoctialEx();
    ey = op.getEquinoctialEy();
    hx = op.getHx();
    hy = op.getHy();
    lv = op.getLv();
  }

  /** Update the parameters from the current position and velocity. */
  protected void updateFromPositionAndVelocity() {

    // get cartesian elements
    double   mu       = getCachedMu();
    Vector3D position = getPosition(mu);
    Vector3D velocity = getVelocity(mu);

    // compute semi-major axis
    double r       = position.getNorm();
    double V2      = Vector3D.dotProduct(velocity, velocity);
    double rV2OnMu = r * V2 / mu;
    a              = r / (2 - rV2OnMu);

    // compute inclination vector
    Vector3D w = Vector3D.crossProduct(position, velocity);
    w.normalizeSelf();
    double d = 1. / (1 + w.getZ());
    hx = -d * w.getY();
    hy =  d * w.getX();

    // compute true latitude argument
    double cLv = (position.getX() - d * position.getZ() * w.getX()) / r;
    double sLv = (position.getY() - d * position.getZ() * w.getY()) / r;
    lv = Math.atan2(sLv, cLv);

    // compute eccentricity vector
    double eSE = Vector3D.dotProduct(position, velocity) / Math.sqrt(mu * a);
    double eCE = rV2OnMu - 1;
    double e2  = eCE * eCE + eSE * eSE;
    double f   = eCE - e2;
    double g   = Math.sqrt(1 - e2) * eSE;
    ex = a * (f * cLv + g * sLv) / r;
    ey = a * (f * sLv - g * cLv) / r;

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
    super.reset();

  }

  /** Get the first component of the eccentricity vector.
   * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    return ex;
  }

  /** Set the first component of the eccentricity vector.
   * @param ex = e cos(&omega; + &Omega;), first component of the eccentricity vector
   */
  public void setEquinoctialEx(double ex) {

    this.ex = ex;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the second component of the eccentricity vector.
   * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    return ey;
  }

  /** Set the second component of the eccentricity vector.
   * @param ey = e sin(&omega; + &Omega;), second component of the eccentricity vector
   */
  public void setEquinoctialEy(double ey) {

    this.ey = ey;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the first component of the inclination vector.
   * @return tan(i/2) cos(&Omega;), first component of the inclination vector
   */
  public double getHx() {
    return hx;
  }

  /** Set the first component of the inclination vector.
   * @param hx = tan(i/2) cos(&Omega;), first component of the inclination vector
   */
  public void setHx(double hx) {

    this.hx = hx;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the second component of the inclination vector.
   * @return tan(i/2) sin(&Omega;), second component of the inclination vector
   */
  public double getHy() {
    return hy;
  }

  /** Set the second component of the inclination vector.
   * @param hy = tan(i/2) sin(&Omega;), second component of the inclination vector
   */
  public void setHy(double hy) {

    this.hy = hy;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the true latitude argument.
   * @return v + &omega; + &Omega; true latitude argument (rad)
   */
  public double getLv() {
    return lv;
  }

  /** Set the true latitude argument.
   * @param lv = v + &omega; + &Omega; true latitude argument (rad)
   */
  public void setLv(double lv) {

    this.lv = lv;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the eccentric latitude argument.
   * @return E + &omega; + &Omega; eccentric latitude argument (rad)
   */
  public double getLE() {
    double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
    double cosLv   = Math.cos(lv);
    double sinLv   = Math.sin(lv);
    return lv + 2 * Math.atan((ey * cosLv - ex * sinLv)
                            / (epsilon + 1 + ex * cosLv + ey * sinLv));
  }

  /** Set the eccentric latitude argument.
   * @param lE = E + &omega; + &Omega; eccentric latitude argument (rad)
   */
  public void setLE(double lE) {
    double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
    double cosLE   = Math.cos(lE);
    double sinLE   = Math.sin(lE);
    setLv(lE + 2 * Math.atan((ex * sinLE - ey * cosLE)
                           / (epsilon + 1 - ex * cosLE - ey * sinLE)));
  }

  /** Get the mean latitude argument.
   * @return M + &omega; + &Omega; mean latitude argument (rad)
   */
  public double getLM() {
    double lE = getLE();
    return lE - ex * Math.sin(lE) + ey * Math.cos(lE);
  }

  /** Set the mean latitude argument.
   * @param lM = M + &omega; + &Omega; mean latitude argument (rad)
   */
  public void setLM(double lM) {
    // Generalization of Kepler equation to equinoctial parameters
    // with lE = PA + RAAN + E and 
    //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
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

      double f12 = 2.0 * f1;
      shift = f0 * f12 / (f1 * f12 - f0 * f2);

      lEmlM -= shift;
      lE     = lM + lEmlM;
      cosLE  = Math.cos(lE);
      sinLE  = Math.sin(lE);

    } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

    setLE(lE); // which set the lv parameter

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
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
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

    // invalidate position and velocity
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

}
