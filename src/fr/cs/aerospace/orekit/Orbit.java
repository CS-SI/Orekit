package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.utilities.ArraySliceMappable;
import org.spaceroots.mantissa.geometry.Vector3D;

import java.io.Serializable;

/**
 * This class handles orbits around a central body.

 * <p>
 * This class handles periodic orbits (it does not handle parabolic or
 * hyperbolic orbits). Several different internal representations can
 * be used for the parameters, the default one is to used {@link
 * EquinoctialParameters equinoctial parameters} which can handle
 * circular and equatorial orbits without problem and has
 * singularities only for purely retrograd orbit (inclination =
 * Pi). Another important parameters set is the traditional {@link
 * KeplerianParameters keplerian parameters}.
 * </p>

 * <p>
 * For user convenience, the classical keplerian elements can be
 * provided regardlesss of the internal representation. One should be
 * aware, however, that in some cases these elements can vary
 * drastically even for a small change in the orbit. This is due to
 * the singular nature of these elements. In this case, an arbitrary
 * choice is made in the class before providing the elements, no error
 * is triggered.
 * </p>

 * <p>
 * The class by itself does not provide any extrapolation method, it only holds
 * one state of the orbit and is not able to change it. Extrapolation is
 * performed by specialized classes that process Orbit instances.
 * </p>

 * @see     OrbitalParameters
 * @version $Id$
 * @author  L. Maisonobe

 */
public class Orbit
  implements Serializable {

  /** Date. */
  private RDate t;

  /** Orbital parameters. */
  private OrbitalParameters parameters;

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public Orbit() {
    t          = new RDate();
    parameters = new EquinoctialParameters();
  }

  /** Creates a new instance of Orbit
   * @param t  date (a reference to this object will be stored in the instance)
   * @param a  semi-major axis (m)
   * @param ex first component of the eccentricity vector
   * @param ey second component of the eccentricity vector
   * @param hx first component of the inclination vector
   * @param hy second component of the inclination vector
   * @param lv latitude argument (rad)
   */
  public Orbit(RDate t,
               double a,
               double ex, double ey,
               double hx, double hy,
               double lv) {
    this.t = t;
    parameters = new EquinoctialParameters(a, ex, ey, hx, hy, lv);
  }

  /** Create a new instance from date and orbital parameters
   * @param t  date (a reference to this object will be stored in the instance)
   * @param parameters orbital parameters (a reference to this object will be
   * stored in the instance)
   */
  public Orbit(RDate t, OrbitalParameters parameters) {
    this.t = t;
    this.parameters = parameters;
  }

  /** Constructor from cartesian parameters.
   * @param t date (a reference to this object will be stored in the instance)
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public Orbit(RDate t, Vector3D position, Vector3D velocity, double mu) {
    this.t = t;
    parameters = new EquinoctialParameters(position, velocity, mu);
  }

  /** Copy-constructor.
   * @param o orbit to copy
   */
  public Orbit(Orbit o) {
    t          = new RDate(o.t);
    parameters = (OrbitalParameters) o.parameters.clone();
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    t.reset();
    parameters.reset();
  }

  /** Reset the orbit from date and orbital parameters.
   * <p>
   * If the internal representation of the parameters is the same as the
   * argument, the internal object is reused. If there is a type mismatch,
   * a new object which is a copy of the argument is allocated.
   * </p>
   * @param t  date (a reference to this object will be stored in the instance)
   * @param parameters orbital parameters
   */
  public void reset(RDate t, OrbitalParameters parameters) {
    this.t.reset(t);
    try {
      this.parameters.reset(parameters);
    } catch (ClassCastException cce) {
      this.parameters = (OrbitalParameters) parameters.clone();
    }
  }

  /** Reset the orbit from cartesian parameters.
   * @param t date (a reference to this object will be stored in the instance)
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m^3/s^2)
   */
  public void reset(RDate t, Vector3D position, Vector3D velocity, double mu) {
    this.t = t;
    parameters.reset(position, velocity, mu);
  }

  /** Reset the orbit from another one.
   * <p>
   * If the internal representation of the parameters is the same as
   * the one used in the argument, the internal object is reused. If
   * there is a type mismatch, a new object which is a copy of the
   * argument is allocated.
   * </p>
   * @param o orbit to copy
   */
  public void reset(Orbit o) {
    t.reset(t);
    try {
      parameters.reset(o.parameters);
    } catch (ClassCastException cce) {
      parameters = (OrbitalParameters) o.parameters.clone();
    }
  }

  /** Get the date.
   * @return date
   */
  public RDate getDate() {
    return t;
  }

  /** Set the date.
   * @param t date
   */
  public void setDate(RDate t) {
    this.t.reset(t);
  }

  /** Get the orbital parameters.
   * @return orbital parameters
   */
  public OrbitalParameters getParameters() {
    return parameters;
  }

  /** Set the orbital parameters.
   * <p>
   * If the internal representation of the parameters is the same as
   * the argument, the internal object is reused. If there is a type
   * mismatch, a new object which is a copy of the argument is
   * allocated.
   * </p>
   * @param parameters orbital parameters
   */
  public void setParameters(OrbitalParameters parameters) {
    try {
      this.parameters.reset(parameters);
    } catch (ClassCastException cce) {
      this.parameters = (OrbitalParameters) parameters.clone();
    }
  }

  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    return parameters.getA();
  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    return parameters.getE();
  }

  /** Get the inclination.
   * @return inclination (rad)
   */
  public double getI() {
    return parameters.getI();
  }

  /** Get the perigee argument.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), zero is returned
   * @return perigee argument (rad)
   */
  public double getPA() {
    return parameters.getPA();
  }

  /** Get the right ascension of the ascending node.
   * If the orbit is almost equatorial (i < 1.0e-6), zero is returned
   * @return right ascension of the ascending node (rad)
   */
  public double getRAAN() {
    return parameters.getRAAN();
  }
  
  /** Get the true anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lv is returned
   * @return true anomaly (rad)
   */
  public double getTrueAnomaly() {
    return parameters.getTrueAnomaly();
  }

  /** Get the eccentric anomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lE is returned
   * @return eccentric anomaly (rad)
   */
  public double getEccentricAnomaly() {
    return parameters.getEccentricAnomaly();
  }

  /** Get the meananomaly.
   * If the orbit is almost circular (e < 1.0e-6) or equatorial
   * (i < 1.0e-6), lM is returned
   * @return mean anomaly (rad)
   */
  public double getMeanAnomaly() {
    return parameters.getMeanAnomaly();
  }

  /** Get the position.
   * Compute the position of the satellite. This method caches its
   * results, and recompute them only when the orbit is changed or if
   * the method is called with a new value for mu. The result is
   * provided as a reference to the internally cached vector, so the
   * caller is responsible to copy it in a separate vector if it needs
   * to keep the value for a while.
   * @param mu central attraction coefficient
   * @return position vector in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getPosition(double mu) {
    return parameters.getPosition(mu);
  }

  /** Get the velocity.
   * Compute the velocity of the satellite. This method caches its
   * results, and recompute them only when the orbit is changed or if
   * the method is called with a new value for mu. The result is
   * provided as a reference to the internally cached vector, so the
   * caller is responsible to copy it in a separate vector if it needs
   * to keep the value for a while.
   * @param mu central attraction coefficient
   * @return velocity vector in inertial frame (reference to an
   * internally cached vector which can change)
   */
  public Vector3D getVelocity(double mu) {
    return parameters.getVelocity(mu);
  }

  /**  Returns a string representation of this Orbit object
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append('{');
    sb.append(t.toString());
    sb.append(' ');
    sb.append(parameters.toString());
    sb.append('}');
    return sb.toString();
  }

}
