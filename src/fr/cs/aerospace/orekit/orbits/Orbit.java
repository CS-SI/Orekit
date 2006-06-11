package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.RDate;

import java.io.Serializable;

/**
 * This class handles orbits around a central body.

 * <p>
 * In OREKIT architecture, an Orbit is only a state at a specific date.
 * Orbit evolution is represented by the {@link
 * fr.cs.aerospace.orekit.propagation.Ephemeris Ephemeris} interface,
 * which contains only the {@link Ephemeris#getOrbit getOrbit} method to provide
 * new states for new dates. This interface can be implemented by several means
 * like file-based interpolation, analytical model or numerical integration.
 * </p>

 * <p>
 * This class handles periodic orbits (it does neither handle parabolic nor
 * hyperbolic orbits). Several different internal representations can
 * be used for the parameters, the more general one being {@link
 * EquinoctialParameters equinoctial parameters} which can handle
 * circular and equatorial orbits without problem and have
 * singularities only for purely retrograd orbit (inclination = &pi;).
 * </p>

 * <p>
 * For the sake of numerical stability, only the always non-ambiguous classical
 * keplerian elements are provided ({@link #getE() eccentricity} and {@link #getI()
 * inclination}, not the potentialy ambiguous ones like perigee argument or right
 * ascension of ascending node. If these elements are needed, the user must
 * explicitely convert the parameters to {@link KeplerianParameters keplerian
 * parameters}, if he considers the orbit is sufficiently non-circular or
 * non-equatorial.
 * </p>

 * @see     OrbitalParameters
 * @see     fr.cs.aerospace.orekit.propagation.Ephemeris
 * @version $Id$
 * @author  L. Maisonobe
 * @author  G. Prat

 */

public class Orbit
  implements Serializable {

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public Orbit() {
    t          = new RDate();
    parameters = new EquinoctialParameters();
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
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public void reset(RDate t, OrbitalParameters parameters, double mu) {
    this.t.reset(t);
    try {
      this.parameters.reset(parameters, mu);
    } catch (ClassCastException cce) {
      this.parameters = (OrbitalParameters) parameters.clone();
    }
  }

  /** Reset the orbit from cartesian parameters.
   * @param t date (a reference to this object will be stored in the instance)
   * @param position position in inertial frame (m)
   * @param velocity velocity in inertial frame (m/s)
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
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
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public void reset(Orbit o,double mu) {
    t.reset(t);
    try {
      parameters.reset(o.parameters,mu);
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
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public void setParameters(OrbitalParameters parameters,double mu) {
    try {
      this.parameters.reset(parameters,mu);
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

  /** Get the first component of the eccentricity vector (as per equinoctial parameters).
   * @return e cos(&omega; + &Omega;), first component of eccentricity vector
   * @see #getE()
   */
  public double getEx(){
    return parameters.getEquinoctialEx();
  }

  /** Get the second component of the eccentricity vector (as per equinoctial parameters).
   * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
   * @see #getE()
   */
  public double getEy(){
    return parameters.getEquinoctialEy();
  }

  /** Get the first component of the inclination vector (as per equinoctial parameters).
   * @return tan(i/2) cos(&Omega;), first component of the inclination vector
   * @see #getI()
   */
  public double getHx(){
    return parameters.getHx();
  }
  
  /** Get the second component of the inclination vector (as per equinoctial parameters).
   * @return tan(i/2) sin(&Omega;), second component of the inclination vector
   * @see #getI()
   */
  public double getHy(){
    return parameters.getHy();
  }
  
  /** Get the true latitude argument (as per equinoctial parameters).
   * @return v + &omega; + &Omega; true latitude argument (rad)
   * @see #getLE()
   * @see #getLM()
   */
  public double getLv(){
    return parameters.getLv();
  }
  
  /** Get the eccentric latitude argument (as per equinoctial parameters).
   * @return E + &omega; + &Omega; eccentric latitude argument (rad)
   * @see #getLv()
   * @see #getLM()
   */
  public double getLE(){
    return parameters.getLE();
  }
  
  /** Get the mean latitude argument (as per equinoctial parameters).
   * @return M + &omega; + &Omega; mean latitude argument (rad)
   * @see #getLv()
   * @see #getLE()
   */
  public double getLM(){
    return parameters.getLM();
  }
  
  
  // Additional orbital elements
  
  /** Get the eccentricity.
   * @return eccentricity
   * @see #getEx()
   * @see #getEy()
   */
  public double getE() {
    return parameters.getE();
  }

  /** Get the inclination.
   * @return inclination (rad)
   * @see #getHx()
   * @see #getHy()
   */
  public double getI() {
    return parameters.getI();
  }

  /** Get the position.
   * Compute the position of the satellite. This method caches its
   * results, and recompute them only when the orbit is changed or if
   * the method is called with a new value for mu. The result is
   * provided as a reference to the internally cached vector, so the
   * caller is responsible to copy it in a separate vector if it needs
   * to keep the value for a while.
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
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
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
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

  /** Date of the current state. */
  private RDate t;

  /** Orbital parameters state. */
  private OrbitalParameters parameters;
  
}
