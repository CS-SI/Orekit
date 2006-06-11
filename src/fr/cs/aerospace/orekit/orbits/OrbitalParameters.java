package fr.cs.aerospace.orekit.orbits;


import org.spaceroots.mantissa.utilities.ArraySliceMappable;
import org.spaceroots.mantissa.geometry.Vector3D;

import java.io.Serializable;

/**
 * This class handles orbital parameters without date.

 * <p>
 * The aim of this class is to separate the orbital parameters from the date
 * for cases where dates are managed elsewhere. This occurs for example during
 * numerical integration and interpolation because date is the free parameter
 * whereas the orbital parameters are bound to either differential or
 * interpolation equations.</p>

 * <p>
 * For user convenience, both the cartesian and the equinoctial elements
 * are provided by this class, regardless of the canonical representation
 * implemented in the derived class (which may be classical keplerian
 * elements for example).
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
public abstract class OrbitalParameters
  implements ArraySliceMappable, Serializable {

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  protected OrbitalParameters() {
    cachedPosition = new Vector3D(Double.NaN, Double.NaN, Double.NaN);
    cachedVelocity = new Vector3D(Double.NaN, Double.NaN, Double.NaN);
    reset();
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public abstract Object clone();

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    cachedMu = Double.NaN;
    cachedPosition.setCoordinates(Double.NaN, Double.NaN, Double.NaN);
    cachedVelocity.setCoordinates(Double.NaN, Double.NaN, Double.NaN);
    dirtyCache = true;
  }

  /** Reset the orbit from cartesian parameters.
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public void reset(Vector3D position, Vector3D velocity, double mu) {
    cachedMu = mu;
    cachedPosition.reset(position);
    cachedVelocity.reset(velocity);
    dirtyCache = false;
    updateFromPositionAndVelocity();
  }

  /** Reset the orbit from another one.
   * @param op orbit parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public void reset(OrbitalParameters op, double mu) {
    dirtyCache = true;
    doReset(op, mu);
  }

  /** Update the canonical orbital parameters from the cached position/velocity.
   * <p>The cache is <em>guaranteed</em> to be clean when this method is called.</p>
   */
  protected abstract void updateFromPositionAndVelocity();

  /** Reset the orbit from another one.
   * @param op orbit parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  protected abstract void doReset(OrbitalParameters op, double mu); 

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public abstract double getA();

  /** Get the first component of the equinoctial eccentricity vector.
   * @return first component of the equinoctial eccentricity vector
   */
  public abstract double getEquinoctialEx();

  /** Get the second component of the equinoctial eccentricity vector.
   * @return second component of the equinoctial eccentricity vector
   */
  public abstract double getEquinoctialEy();

  /** Get the first component of the inclination vector.
   * @return first component of the inclination vector
   */
  public abstract double getHx();
  
  /** Get the second component of the inclination vector.
   * @return second component of the inclination vector
   */
  public abstract double getHy();
  
  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument (rad)
   */
  public abstract double getLE();
  
  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public abstract double getLv();
  
  /** Get the mean latitude argument.
   * @return mean latitude argument (rad)
   */
  public abstract double getLM();
  
  // Additional orbital elements
  
  /** Get the eccentricity.
   * @return eccentricity
   */
  public abstract double getE() ;

  /** Get the inclination.
   * @return inclination (rad)
   */
  public abstract double getI() ;

  private void initPositionAndVelocity(double mu) {

    // get equinoctial parameters
    double a  = getA();
    double ex = getEquinoctialEx();
    double ey = getEquinoctialEy(); 
    double hx = getHx();
    double hy = getHy();
    double lE = getLE();

    // inclination-related intermediate parameters
    double hx2   = hx * hx;
    double hy2   = hy * hy;
    double factH = 1. / (1 + hx2 + hy2);
    
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
    double beta = 1. / eta;

    // eccentric latitude argument
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

    dirtyCache = false;

  }

  /** Check if cache is dirty.
   * @return true if cache is dirty
   */
  protected boolean cacheIsDirty() {
    return dirtyCache;
  }

  /** Get the cached central acceleration constant.
   * @return cached central acceleration constant
   */
  protected double getCachedMu() {
    return cachedMu;
  }

  /** Get the position.
   * Compute the position of the satellite. This method caches its
   * results, and recompute them only when the orbit is changed or if
   * the method is called with a new value for mu. The result is
   * provided as a reference to the internally cached vector, so the
   * caller is responsible to copy it in a separate vector if it needs
   * to keep the value for a while.
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return position vector (m) in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getPosition(double mu) {
    if (dirtyCache || (mu != cachedMu)) {
      initPositionAndVelocity(mu);
    }
    return cachedPosition;
  }

  /** Get the velocity.
   * Compute the velocity of the satellite. This method caches its
   * results, and recompute them only when the orbit is changed or if
   * the method is called with a new value for mu. The result is
   * provided as a reference to the internally cached vector, so the
   * caller is responsible to copy it in a separate vector if it needs
   * to keep the value for a while.
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return velocity vector (m/s) in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getVelocity(double mu) {
    if (dirtyCache || (mu != cachedMu)) {
      initPositionAndVelocity(mu);
    }
    return cachedVelocity;
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object
   * depending on the type of the instance.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object
   */
  public abstract OrbitDerivativesAdder getDerivativesAdder(double mu);

  /** Get the dimension of the state vector associated with the instance.
   * @return state vector dimension (which is always 6)
   */
  public int getStateDimension() {
    return 6;
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, ex, ey, hx, hy, lv)
   */
  public abstract void mapStateFromArray(int start, double[] array);

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, ex, ey, hx, hy, lv)
   */
  public abstract void mapStateToArray(int start, double[] array);

  
  
  /** Last value of mu used to compute position and velocity (m<sup>3</sup>/s<sup>2</sup>). */
  private double cachedMu;

  /** Last computed position (m). */
  private Vector3D cachedPosition;

  /** Last computed velocity (m/s). */
  private Vector3D cachedVelocity;

  /** Indicator for dirty position/velocity cache. */
  private boolean dirtyCache;

}
