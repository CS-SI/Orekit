package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;


/** This class holds cartesian orbital parameters.

 * <p>
 * The parameters used internally are the cartesian coordinates:
 *   <pre>
 *     x
 *     y
 *     z
 *     xDot
 *     yDot
 *     zDot
 *   </pre>
 * </p>

 * <p>
 * Note that the implementation of this class delegates all non-cartesian related
 * computations ({@link #getA()}, {@link #getEquinoctialEx()}, ...) to an underlying
 * instance of the {@link EquinoctialParameters} class that is lazily built only
 * as needed. This imply that using this class only for analytical computations which
 * are always based on non-cartesian parameters is perfectly possible but somewhat
 * suboptimal. This class is more targeted towards numerical orbit propagation.
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
 * @author  G. Prat

 */
public class CartesianParameters
  extends OrbitalParameters {

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public CartesianParameters() {
    reset();
    equinoctial = null;
  }

  /** Constructor from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CartesianParameters(Vector3D position, Vector3D velocity, double mu) {
    reset(position, velocity, mu);
    equinoctial = null;
  }

  /** Constructor from any kind of orbital parameters
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CartesianParameters(OrbitalParameters op, double mu) {
    reset(op, mu);
    equinoctial = null;
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new CartesianParameters(this, getCachedMu());
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    super.reset();
    equinoctial = null;
  }

  protected void doReset(OrbitalParameters op, double mu) {
    reset(op.getPosition(mu), op.getVelocity(mu), mu);
    equinoctial = null;
  }

  /** Update the parameters from the current position and velocity. */
  protected void updateFromPositionAndVelocity() {
    // we do NOT recompute immediately the underlying parameters,
    // using a lazy evaluation
    equinoctial = null;
  }

  /** Lazy evaluation of the equinoctial parameters. */
  private void lazilyEvaluateEquinoctialParameters() {
    if (equinoctial == null) {
      double   mu       = getCachedMu();
      Vector3D position = getPosition(mu);
      Vector3D velocity = getVelocity(mu);
      equinoctial = new EquinoctialParameters(position, velocity, mu);
    }
  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getA();
  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getE();
  }

  /** Get the inclination.
   * @return inclination (rad)
   */
  public double getI() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getI();
  }

  /** Get the first component of the eccentricity vector. 
   * @return first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getEquinoctialEx();
  }
  
  /** Get the second component of the eccentricity vector. 
   * @return second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getEquinoctialEy();
  }

  /** Get the first component of the inclination vector.
   * @return first component oof the inclination vector.
   */
  public double getHx() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getHx();
  }

  /** Get the second component of the inclination vector.
   * @return second component oof the inclination vector.
   */
  public double getHy() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getHy();
  }

  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public double getLv() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getLv();
  }

  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument.(rad)
   */
  public double getLE() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getLE();
  }

  /** Get the mean latitude argument.
   * @return mean latitude argument.(rad)
   */
  public double getLM() {
    lazilyEvaluateEquinoctialParameters();
    return equinoctial.getLM();
  }

  /**  Returns a string representation of this Orbit object
   * @return a string representation of this object
   */
  public String toString() {
    Vector3D position = getPosition(getCachedMu());
    Vector3D velocity = getVelocity(getCachedMu());
    StringBuffer sb = new StringBuffer();
    sb.append('{');
    sb.append(position.getX());
    sb.append(' ');
    sb.append(position.getY());
    sb.append(' ');
    sb.append(position.getZ());
    sb.append(' ');
    sb.append(velocity.getX());
    sb.append(' ');
    sb.append(velocity.getY());
    sb.append(' ');
    sb.append(velocity.getZ());
    sb.append('}');
    return sb.toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, an {@link CartesianDerivativesAdder
   * CartesianDerivativesAdder} object is built.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
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
    Vector3D position = getPosition(getCachedMu());
    Vector3D velocity = getVelocity(getCachedMu());
    position.setCoordinates(array[start],  array[start + 1], array[start + 2]);
    velocity.setCoordinates(array[start + 3], array[start + 4], array[start + 5]);
  }

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, e, i, pa, raan, v)
   */
  public void mapStateToArray(int start, double[] array) {
    Vector3D position = getPosition(getCachedMu());
    Vector3D velocity = getVelocity(getCachedMu());
    array[start]     = position.getX();
    array[start + 1] = position.getY();
    array[start + 2] = position.getZ();
    array[start + 3] = velocity.getX();
    array[start + 4] = velocity.getY();
    array[start + 5] = velocity.getZ();
  }

  /** Underlying equinoctial orbit providing non-cartesian elements. */
  private EquinoctialParameters equinoctial;

}
