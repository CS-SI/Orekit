package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/**
 * This class handles cartesian orbital parameters.

 * <p>
 * The parameters used internally are the classical keplerian elements:
 *   <pre>
 *     x
 *     y
 *     z
 *     xDot
 *     yDot
 *     zDot
 *   </pre>
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
public class CartesianParameters
  extends OrbitalParameters {

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public CartesianParameters() {
    reset();
  }

  /** Constructor from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public CartesianParameters(Vector3D position, Vector3D velocity,
                             double mu) {
    reset(position, velocity, mu);
  }

  /** Copy-constructor.
   * @param op orbit parameters to copy
   */
  public CartesianParameters(CartesianParameters op) {
    reset(op);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new CartesianParameters(this);
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    cachedMu = 3.986e14;
    cachedPosition.setCoordinates(7.0e6, 1.0e6, 4.0e6);
    cachedVelocity.setCoordinates(-500.0, 8000.0, 1000.0);
  }

  /** Get the position.
   * @return position vector in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getPosition() {
    return cachedPosition;
  }

  /** Get the velocity.
   * @return velocity vector in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getVelocity() {
    return cachedVelocity;
  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    double r  = cachedPosition.getNorm();
    double V2 = Vector3D.dotProduct(cachedVelocity, cachedVelocity);
    return r / (2 -  r * V2 / cachedMu);
  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    double   r       = cachedPosition.getNorm();
    double   V2      = Vector3D.dotProduct(cachedVelocity, cachedVelocity);
    double   rV2OnMu = r * V2 / cachedMu;
    double   muA     = cachedMu * r / (2 -  rV2OnMu);
    Vector3D w       = Vector3D.crossProduct(cachedPosition, cachedVelocity);
    double   w2      = Vector3D.dotProduct(w, w);
    double   eSE     = Vector3D.dotProduct(cachedPosition, cachedVelocity)
                     / Math.sqrt(muA);
    double   eCE     = rV2OnMu - 1;
    return Math.sqrt(eSE * eSE + eCE * eCE);
  }

  /** Get the inclination.
   * @return inclination (rad)
   */
  public double getI() {
    Vector3D w = Vector3D.crossProduct(cachedPosition, cachedVelocity);
    w.normalizeSelf();
    double x = w.getX();
    double y = w.getY();
    double cosI = w.getZ();
    double sinI = Math.sqrt(x * x + y * y);
    return (Math.abs(cosI) < 0.99) ? Math.acos(cosI) : Math.asin(sinI);
  }

  /** Get the perigee argument.
   * @return perigee argument (rad)
   */
  public double getPA() {
    double raan    = getRAAN();
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double i       = getI();
    double cosI    = Math.cos(i);
    double sinI    = Math.sin(i);
    double px = cosRaan * cachedPosition.getX()
              + sinRaan * cachedPosition.getY();
    double py = cosI * (cosRaan * cachedPosition.getY()
                      - sinRaan * cachedPosition.getX())
              + sinI * cachedPosition.getZ();
    return Math.atan2(py, px) - getTrueAnomaly();
  }

  /** Get the right ascension of the ascending node.
   * @return right ascension of the ascending node (rad)
   */
  public double getRAAN() {
    Vector3D w = Vector3D.crossProduct(cachedPosition, cachedVelocity);
    w.normalizeSelf();
    double x = w.getX();
    double y = w.getY();
    return (Math.sqrt(x * x + y * y) >= 1.0e-12) ? Math.atan2(x, -y) : 0;
  }

  /** Get the true anomaly.
   * @return true anomaly (rad)
   */
  public double getTrueAnomaly() {
    double E   = getEccentricAnomaly();
    double e   = getE();
    double eSE = e * Math.sin(E);
    double eCE = e * Math.cos(E);
    double k   = 1 / (1 + Math.sqrt((1 - e) * (1 + e)));
    return E + 2 * Math.atan(k * eSE / (1 - k *eCE));
  }

  /** Get the eccentric anomaly.
   * @return eccentric anomaly (rad)
   */
  public double getEccentricAnomaly() {
    double   r       = cachedPosition.getNorm();
    double   V2      = Vector3D.dotProduct(cachedVelocity, cachedVelocity);
    double   rV2OnMu = r * V2 / cachedMu;
    double   muA     = cachedMu * r / (2 -  rV2OnMu);
    Vector3D w       = Vector3D.crossProduct(cachedPosition, cachedVelocity);
    double   w2      = Vector3D.dotProduct(w, w);
    double   eSE     = Vector3D.dotProduct(cachedPosition, cachedVelocity)
                     / Math.sqrt(muA);
    double   eCE     = rV2OnMu - 1;
    double   e       = Math.sqrt(eSE * eSE + eCE * eCE);
    return (e < 1.0e-12) ? 0 : Math.atan2(eSE, eCE);
  }

  /** Get the mean anomaly.
   * @return mean anomaly (rad)
   */
  public double getMeanAnomaly() {
    double E = getEccentricAnomaly();
    return E - getE() * Math.sin(E);
  }

  /** Compute and cache the cartesian parameters.
   * @param mu central body gravitational constant (m^3/s^2)
   */
  protected void initPositionAndVelocity(double mu) {
    cachedMu = mu;
  }

  /**  Returns a string representation of this Orbit object
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append('{');
    sb.append(cachedPosition.getX());
    sb.append(' ');
    sb.append(cachedPosition.getY());
    sb.append(' ');
    sb.append(cachedPosition.getZ());
    sb.append(' ');
    sb.append(cachedVelocity.getX());
    sb.append(' ');
    sb.append(cachedVelocity.getY());
    sb.append(' ');
    sb.append(cachedVelocity.getZ());
    sb.append('}');
    return sb.toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, an {@link CartesianDerivativesAdder
   * CartesianDerivativesAdder} object is built.</p>
   * @param mu central body gravitational constant (m^3/s^2)
   * @return an instance of {@link CartesianDerivativesAdder
   * CartesianDerivativesAdder} associated with this object
   */
  public OrbitDerivativesAdder getDerivativesAdder(double mu) {
    return new CartesianDerivativesAdder(this, mu);
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, e, i, pa, raan, v)
   */
  public void mapStateFromArray(int start, double[] array) {
    cachedPosition.setCoordinates(array[start],
                                  array[start + 1],
                                  array[start + 2]);
    cachedVelocity.setCoordinates(array[start + 3],
                                  array[start + 4],
                                  array[start + 5]);
  }

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, e, i, pa, raan, v)
   */
  public void mapStateToArray(int start, double[] array) {
    array[start]     = cachedPosition.getX();
    array[start + 1] = cachedPosition.getY();
    array[start + 2] = cachedPosition.getZ();
    array[start + 3] = cachedVelocity.getX();
    array[start + 4] = cachedVelocity.getY();
    array[start + 5] = cachedVelocity.getZ();
  }

}
