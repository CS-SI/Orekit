package fr.cs.aerospace.orekit.orbits;

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
 * @see     Orbit
 * @version $Id$
 * @author  L. Maisonobe
 * @author  G. Prat
 * @author  F.Maussion
 */
public class CartesianParameters
  extends OrbitalParameters {

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public CartesianParameters() {
	  	super();
	    equinoctial = null;
  }

  /** Constructor from cartesian parameters.
   * @param pvCoordinates the position and velocity of the satellite. 
   * @param frame the frame in which are defined the {@link PVCoordinates}
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CartesianParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
	  super(pvCoordinates, frame, mu);
    equinoctial = null;
  }

  /** Constructor from any kind of orbital parameters
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CartesianParameters(OrbitalParameters op, double mu) {
    super(op, mu);
    equinoctial = null;
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new CartesianParameters(this, getCachedMu());
  }

  /** Intitialize the parameters from other ones 
   * @param op the {@link OrbitalParameters} to copy
   * @param mu
   */
  protected void init(OrbitalParameters op, double mu) {
	equinoctial =
		new EquinoctialParameters(op.getPVCoordinates(mu), op.getFrame(),  mu);
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

  /** Underlying equinoctial orbit providing non-cartesian elements. */
  private EquinoctialParameters equinoctial;
  
}
