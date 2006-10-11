package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.utils.PVCoordinates;


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
 * contained in {@link PVCoordinates}.
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
   * @param pvCoordinates the position and velocity of the satellite. 
   * @param frame the frame in which are defined the {@link PVCoordinates}
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CartesianParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    reset(pvCoordinates, frame, mu);
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
    reset(op.getPVCoordinates(mu), op.getFrame(),  mu);
    equinoctial = null;
  }

  /** Update the parameters from the current position and velocity. */
  protected void updateFromPVCoordinates() {
    // we do NOT recompute immediately the underlying parameters,
    // using a lazy evaluation
    equinoctial = null;
  }

  /** Lazy evaluation of the equinoctial parameters. */
  private void lazilyEvaluateEquinoctialParameters() {
    if (equinoctial == null) {
      double   mu       = getCachedMu();
      PVCoordinates pvCoordinates = getPVCoordinates(mu);
      equinoctial = new EquinoctialParameters(pvCoordinates, frame, mu);
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
	  return getPVCoordinates(getCachedMu()).toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, a <code>CartesianDerivativesAdder<code> object is built.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object
   */
  public OrbitDerivativesAdder getDerivativesAdder(double mu) {
    return new CartesianDerivativesAdder(this, mu);
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, e, i, pa, raan, v)
   */
  public void mapStateFromArray(int start, double[] array) {
    getPVCoordinates(getCachedMu()).setPosition(new Vector3D(array[start],  array[start + 1], array[start + 2]));
    getPVCoordinates(getCachedMu()).setVelocity(new Vector3D(array[start + 3], array[start + 4], array[start + 5]));    
  }

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, e, i, pa, raan, v)
   */
  public void mapStateToArray(int start, double[] array) {
    PVCoordinates pvCoordinates = getPVCoordinates(getCachedMu());
    array[start]     = pvCoordinates.getPosition().getX();
    array[start + 1] = pvCoordinates.getPosition().getY();
    array[start + 2] = pvCoordinates.getPosition().getZ();
    array[start + 3] = pvCoordinates.getVelocity().getX();
    array[start + 4] = pvCoordinates.getVelocity().getY();
    array[start + 5] = pvCoordinates.getVelocity().getZ();
  }

  /** Underlying equinoctial orbit providing non-cartesian elements. */
  private EquinoctialParameters equinoctial;
  
  /** This internal class sums up the contribution of several forces into orbit derivatives.
  *
  * <p>The aim of this class is to gather the contributions of various perturbing
  * forces expressed as accelerations into one set of time-derivatives of
  * orbital parameters.
  * </p>
  *
  * @version $Id$
  * @author  L. Maisonobe
  * @author  G. Prat
  *
  */
  private class CartesianDerivativesAdder
   extends OrbitDerivativesAdder {

   /** Create a new instance.
    * @param parameters current orbital parameters
    * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
    */
   public CartesianDerivativesAdder(OrbitalParameters parameters, double mu) {
     super(parameters, mu);
   }

   /** Add the contribution of the Kepler evolution.
    * <p>Since the Kepler evolution is the most important, it should
    * be added after all the other ones, in order to improve
    * numerical accuracy.</p>
    */
   public void addKeplerContribution() {

     PVCoordinates pvCoordinates = getPVCoordinates(mu);

     // central body acceleration coefficient
     double r2 = Vector3D.dotProduct(pvCoordinates.getPosition(), pvCoordinates.getPosition());
     double factor = -mu / (r2 * Math.sqrt(r2));

     // Kepler natural evolution
     yDot[0] += pvCoordinates.getVelocity().getX();
     yDot[1] += pvCoordinates.getVelocity().getY();
     yDot[2] += pvCoordinates.getVelocity().getZ();
     yDot[3] += factor * pvCoordinates.getPosition().getX();
     yDot[4] += factor * pvCoordinates.getPosition().getY();
     yDot[5] += factor * pvCoordinates.getPosition().getZ();

   }

   /** Add the contribution of an acceleration expressed in (T, N, W)
    * local orbital frame.
    * @param t acceleration along the T axis (m/s<sup>2</sup>)
    * @param n acceleration along the N axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addTNWAcceleration(double t, double n, double w) {
     yDot[3] += T.getX() * t + N.getX() * n + W.getX() * w;
     yDot[4] += T.getY() * t + N.getY() * n + W.getY() * w;
     yDot[5] += T.getZ() * t + N.getZ() * n + W.getZ() * w;
   }

   /** Add the contribution of an acceleration expressed in (Q, S, W)
    * local orbital frame.
    * @param q acceleration along the Q axis (m/s<sup>2</sup>)
    * @param s acceleration along the S axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addQSWAcceleration(double q, double s, double w) {
     yDot[3] += Q.getX() * q + S.getX() * s + W.getX() * w;
     yDot[4] += Q.getY() * q + S.getY() * s + W.getY() * w;
     yDot[5] += Q.getZ() * q + S.getZ() * s + W.getZ() * w;
   }

   /** Add the contribution of an acceleration expressed in inertial frame.
    * @param x acceleration along the X axis (m/s<sup>2</sup>)
    * @param y acceleration along the Y axis (m/s<sup>2</sup>)
    * @param z acceleration along the Z axis (m/s<sup>2</sup>)
    */
   public void addXYZAcceleration(double x, double y, double z) {
     yDot[3] += x;
     yDot[4] += y;
     yDot[5] += z;
   }

   /** Add the contribution of an acceleration expressed in inertial frame.
    * @param gamma acceleration vector in intertial frame (m/s<sup>2</sup>)
    */
   public void addAcceleration(Vector3D gamma) {
     yDot[3] += gamma.getX();
     yDot[4] += gamma.getY();
     yDot[5] += gamma.getZ();
   }

   /** Get the frame in which are defined the XYZ coordinates.
    * @return the frame.
    */
   public Frame getFrame() {
	   return CartesianParameters.this.getFrame();
   }
 }

}
