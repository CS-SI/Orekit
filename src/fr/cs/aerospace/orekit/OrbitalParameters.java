package fr.cs.aerospace.orekit;


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
 * For user convenience, the classical keplerian elements can be provided by
 * this class. One should be aware, however, that in some cases these elements
 * can vary drastically even for a small change in the orbit. This is due to
 * the singular nature of these elements. In this case, an arbitrary choice is
 * made in the class before providing the elements, no error is triggered.
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
public abstract class OrbitalParameters
  implements ArraySliceMappable, Serializable {

  /** Last value of mu used to compute position and velocity (m^3/s^2). */
  protected double cachedMu;

  /** Last computed position (m). */
  protected Vector3D cachedPosition;

  /** Last computed velocity (m/s). */
  protected Vector3D cachedVelocity;

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  protected OrbitalParameters() {
    cachedPosition = new Vector3D();
    cachedVelocity = new Vector3D();
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
    cachedMu = 0;
    cachedPosition.setCoordinates(0, 0, 0);
    cachedVelocity.setCoordinates(0, 0, 0);
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
  }

  /** Reset the orbit from another one.
   * @param op orbit parameters to copy
   */
  public void reset(OrbitalParameters op) {
    cachedMu = op.cachedMu;
    cachedPosition.reset(op.cachedPosition);
    cachedVelocity.reset(op.cachedVelocity);
  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public abstract double getA();

  /** Get the eccentricity.
   * @return eccentricity
   */
  public abstract double getE() ;

  /** Get the inclination.
   * @return inclination (rad)
   */
  public abstract double getI() ;

  /** Get the perigee argument.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), zero is returned
   * @return perigee argument (rad)
   */
  public abstract double getPA() ;

  /** Get the right ascension of the ascending node.
   * If the orbit is almost equatorial (i < 1.0e-6), zero is returned
   * @return right ascension of the ascending node (rad)
   */
  public abstract double getRAAN() ;

  /** Get the true anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return true anomaly (rad)
   */
  public abstract double getTrueAnomaly() ;

  /** Get the eccentric anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return eccentric anomaly (rad)
   */
  public abstract double getEccentricAnomaly() ;

  /** Get the meananomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return mean anomaly (rad)
   */
  public abstract double getMeanAnomaly() ;

  /** Compute and cache the cartesian parameters.
   * @param  mu central body gravitational constant (m^3/s^2)
   */
  protected abstract void initPositionAndVelocity(double mu);

  /** Get the position.
   * Compute the position of the satellite. This method caches its
   * results, and recompute them only when the orbit is changed or if
   * the method is called with a new value for mu. The result is
   * provided as a reference to the internally cached vector, so the
   * caller is responsible to copy it in a separate vector if it needs
   * to keep the value for a while.
   * @param mu central body gravitational constant (m^3/s^2)
   * @return position vector in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getPosition(double mu) {
    if (Math.abs(mu - cachedMu) > 1.0) {
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
   * @param mu central body gravitational constant (m^3/s^2)
   * @return velocity vector in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getVelocity(double mu) {
    if (Math.abs(mu - cachedMu) > 1.0) {
      initPositionAndVelocity(mu);
    }
    return cachedVelocity;
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object
   * depending on the type of the instance.</p>
   * @param mu central body gravitational constant (m^3/s^2)
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

}
