package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/**
 * This class handles circular orbital parameters.

 * <p>
 * The parameters used internally are the circular elements defined as follows:
 *   <pre>
 *     a
 *     ex = e cos(&omega;)
 *     ey = e sin(&omega;)
 *     i
 *     &Omega;
 *     &alpha;<sub>v</sub> = v + &omega;
 *   </pre>
 * where &Omega; stands for the Right Ascension of the Ascending Node and
 * &alpha;<sub>v</sub> stands for the true longitude argument
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
public class CircularParameters
  extends OrbitalParameters {

  /** Identifier for mean longitude argument. */
  public static final int MEAN_LONGITUDE_ARGUMENT = 0;

  /** Identifier for eccentric longitude argument. */
  public static final int ECCENTRIC_LONGITUDE_ARGUMENT = 1;

  /** Identifier for true longitude argument. */
  public static final int TRUE_LONGITUDE_ARGUMENT = 2;

  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public CircularParameters() {
    reset();
  }

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param ex e cos(&omega;), first component of circular eccentricity vector
   * @param ey e sin(&omega;), second component of circular eccentricity vector
   * @param i inclination (rad)
   * @param raan right ascension of ascending node (&Omega;, rad)
   * @param alpha  an + &omega;, mean, eccentric or true longitude argument (rad)
   * @param type type of longitude argument, must be one of {@link #MEAN_LONGITUDE_ARGUMENT},
   * {@link #ECCENTRIC_LONGITUDE_ARGUMENT} or  {@link #TRUE_LONGITUDE_ARGUMENT}
   */
  public CircularParameters(double a, double ex, double ey, double i, double raan,
                            double alpha, int type) {
    reset(a, ex, ey, i, raan, alpha, type);
  }

  /** Constructor from cartesian parameters.
   * @param pvCoordinates the {@link PVCoordinates} in inertial frame
   * @param frame the frame in which are expressed the {@link PVCoordinates} 
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CircularParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    reset(pvCoordinates, frame, mu);
  }

  /** Constructor from any kind of orbital parameters
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public CircularParameters(OrbitalParameters op, double mu) {
    reset(op, mu);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new CircularParameters(a, ex, ey, i, raan, alphaV, TRUE_LONGITUDE_ARGUMENT);
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    a    = 1.0e7;
    ex   = 1.0e-3;
    ey   = 0;
    i    = 0.3;
    raan = 0;
    setAlphaV(0);
  }

  /** Reset the orbit from orbital parameters
   * @param a  semi-major axis (m)
   * @param ex e cos(&omega;), first component of circular eccentricity vector
   * @param ey e sin(&omega;), second component of circular eccentricity vector
   * @param i inclination (rad)
   * @param raan right ascension of ascending node (&Omega;, rad)
   * @param alpha  an + &omega;, mean, eccentric or true longitude argument (rad)
   * @param type type of longitude argument, must be one of {@link #MEAN_LONGITUDE_ARGUMENT},
   * {@link #ECCENTRIC_LONGITUDE_ARGUMENT} or  {@link #TRUE_LONGITUDE_ARGUMENT}
   */
  public void reset(double a, double ex, double ey, double i, double raan,
                    double alpha, int type) {

    this.a    =  a;
    this.ex   = ex;
    this.ey   = ey;
    this.i    = i;
    this.raan = raan;

    switch (type) {
    case MEAN_LONGITUDE_ARGUMENT :
      setAlphaM(alpha);
      break;
    case ECCENTRIC_LONGITUDE_ARGUMENT :
      setAlphaE(alpha);
      break;
    default :
      setAlphaV(alpha);
    }


  }

  protected void doReset(OrbitalParameters op, double mu) {
    a    = op.getA();
    i    = op.getI();
    raan = Math.atan2(op.getHy(), op.getHx());
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double equiEx = op.getEquinoctialEx();
    double equiEy = op.getEquinoctialEy();
    ex   = equiEx * cosRaan + equiEy * sinRaan;
    ey   = equiEy * cosRaan - equiEx * sinRaan;
    setAlphaV(op.getLv() - raan);
  }

  /** Update the parameters from the current position and velocity. */
  protected void updateFromPVCoordinates() {

    // get cartesian elements
    double   mu       = getCachedMu();
    PVCoordinates pvCoordinates = getPVCoordinates(mu);
    
    // compute semi-major axis
    double r          = pvCoordinates.getPosition().getNorm();
    double V2         = Vector3D.dotProduct(pvCoordinates.getVelocity(), pvCoordinates.getVelocity());
    double rV2OnMu    = r * V2 / mu;
    a                 = r / (2 - rV2OnMu);

    // compute inclination
    Vector3D momentum = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity());
    i                 = Vector3D.angle(momentum, Vector3D.plusK);

    // compute right ascension of ascending node
    Vector3D node     = Vector3D.crossProduct(Vector3D.plusK, momentum);
    double   n2       = Vector3D.dotProduct(node, node);
    // the following comparison with 0 IS REALLY numerically justified and stable
    raan = (n2 == 0) ? 0 : Math.atan2(node.getY(), node.getX());

    // 2D-coordinates in the canonical frame
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double cosI    = Math.cos(i);
    double sinI    = Math.sin(i);
    Vector3D rVec  = new Vector3D(cosRaan, Math.sin(raan), 0);
    Vector3D sVec  = new Vector3D(-cosI * sinRaan, cosI * cosRaan, sinI);
    double x2      = Vector3D.dotProduct(pvCoordinates.getPosition(), rVec) / a;
    double y2      = Vector3D.dotProduct(pvCoordinates.getPosition(), sVec) / a;

    // compute eccentricity vector
    double eSE    = Vector3D.dotProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()) / Math.sqrt(mu * a);
    double eCE    = rV2OnMu - 1;
    double e2     = eCE * eCE + eSE * eSE;
    double f      = eCE - e2;
    double g      = Math.sqrt(1 - e2) * eSE;
    double aOnR   = a / r;
    double a2OnR2 = aOnR * aOnR;
    ex = a2OnR2 * (f * x2 + g * y2);
    ey = a2OnR2 * (f * y2 - g * x2);

    // compute longitude argument
    double beta = 1 / (1 + Math.sqrt(1 - ex * ex - ey * ey));
    setAlphaE(Math.atan2(y2 + ey + eSE * beta * ex, x2 + ex - eSE * beta * ey));

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

  /** Get the first component of the equinoctial eccentricity vector.
   * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    return ex * Math.cos(raan) - ey * Math.sin(raan);
  }

  /** Get the second component of the equinoctial eccentricity vector.
   * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    return ey * Math.cos(raan) + ex * Math.sin(raan);
  }

  /** Get the first component of the circular eccentricity vector.
   * @return ex = e cos(&omega;), first component of the circular eccentricity vector
   */
  public double getCircularEx() {
    return ex;
  }

  /** Set the first component of the circular eccentricity vector.
   * @param ex = e cos(&omega;), first component of the circular eccentricity vector
   */
  public void setCircularEx(double ex) {

    this.ex = ex;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the second component of the circular eccentricity vector.
   * @return ey = e sin(&omega;), second component of the circular eccentricity vector
   */
  public double getCircularEy() {
    return ey;
  }

  /** Set the second component of the circular eccentricity vector.
   * @param ey = e sin(&omega;), second component of the circular eccentricity vector
   */
  public void setCircularEy(double ey) {

    this.ey = ey;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the first component of the inclination vector.
   * @return first component of the inclination vector.
   */
  public double getHx() {
    return  Math.cos(raan) * Math.tan(i / 2);
  }

  /** Get the second component of the inclination vector.
   * @return second component of the inclination vector.
   */
  public double getHy() {
    return  Math.sin(raan) * Math.tan(i / 2);
  }

  /** Get the true longitude argument.
   * @return v + &omega; true longitude argument (rad)
   */
  public double getAlphaV() {
    return alphaV;
  }

  /** Set the true longitude argument.
   * @param alphaV = v + &omega; true longitude argument (rad)
   */
  public void setAlphaV(double alphaV) {

    this.alphaV = alphaV;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the eccentric longitude argument.
   * @return E + &omega; eccentric longitude argument (rad)
   */
  public double getAlphaE() {
    double epsilon   = Math.sqrt(1 - ex * ex - ey * ey);
    double cosAlphaV = Math.cos(alphaV);
    double sinAlphaV = Math.sin(alphaV);
    return alphaV + 2 * Math.atan((ey * cosAlphaV - ex * sinAlphaV)
                                / (epsilon + 1 + ex * cosAlphaV + ey * sinAlphaV));
  }

  /** Set the eccentric longitude argument.
   * @param alphaE = E + &omega; eccentric longitude argument (rad)
   */
  public void setAlphaE(double alphaE) {
    double epsilon   = Math.sqrt(1 - ex * ex - ey * ey);
    double cosAlphaE = Math.cos(alphaE);
    double sinAlphaE = Math.sin(alphaE);
    setAlphaV(alphaE + 2 * Math.atan((ex * sinAlphaE - ey * cosAlphaE)
                                   / (epsilon + 1 - ex * cosAlphaE - ey * sinAlphaE)));
  }

  /** Get the mean longitude argument.
   * @return M + &omega; mean longitude argument (rad)
   */
  public double getAlphaM() {
    double alphaE = getAlphaE();
    return alphaE - ex * Math.sin(alphaE) + ey * Math.cos(alphaE);
  }

  /** Set the mean longitude argument.
   * @param alphaM = M + &omega;  mean longitude argument (rad)
   */
  public void setAlphaM(double alphaM) {
    // Generalization of Kepler equation to equinoctial parameters
    // with alphaE = PA + E and 
    //      alphaM = PA + M = alphaE - ex.sin(alphaE) + ey.cos(alphaE)
    double alphaE = alphaM;
    double shift = 0.0;
    double alphaEMalphaM = 0.0;
    double cosLE = Math.cos(alphaE);
    double sinLE = Math.sin(alphaE);
    int iter = 0;
    do {
      double f2 = ex * sinLE - ey * cosLE;
      double f1 = 1.0 - ex * cosLE - ey * sinLE;
      double f0 = alphaEMalphaM - f2;

      double f12 = 2.0 * f1;
      shift = f0 * f12 / (f1 * f12 - f0 * f2);

      alphaEMalphaM -= shift;
      alphaE         = alphaM + alphaEMalphaM;
      cosLE          = Math.cos(alphaE);
      sinLE          = Math.sin(alphaE);

    } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

    setAlphaE(alphaE); // which set the alphaV parameter

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
    return i;
  }

  /** Set the inclination.
   * @param i inclination (rad)
   */
  public void setI(double i) {

    this.i = i;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the right ascension of the ascending node.
   * @return right ascension of the ascending node (rad)
   */
  public double getRightAscensionOfAscendingNode() {
    return raan;
  }

  /** Set the right ascension of ascending node.
   * @param raan right ascension of ascending node (rad)
   */
  public void setRightAscensionOfAscendingNode(double raan) {

    this.raan = raan;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public double getLv() {
    return alphaV + raan;
  }

  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument.(rad)
   */
  public double getLE() {
    return getAlphaE() + raan;
  }

  /** Get the mean latitude argument.
   * @return mean latitude argument.(rad)
   */
  public double getLM() {
    return getAlphaM() + raan;
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
    sb.append(i);
    sb.append(' ');
    sb.append(raan);
    sb.append(' ');
    sb.append(Math.toDegrees(alphaV));
    sb.append('}');
    return sb.toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, a <code>CircularDerivativesAdder</code>
   * object is built.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object
   */
  public OrbitDerivativesAdder getDerivativesAdder(double mu) {
    return new CircularDerivativesAdder(this, mu);
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, ex, ey, hx, hy, lv)
   */
  public void mapStateFromArray(int start, double[] array) {

    a      = array[start];
    ex     = array[start + 1];
    ey     = array[start + 2];
    i      = array[start + 3];
    raan   = array[start + 4];
    alphaV = array[start + 5];

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
    array[start + 3] = i;
    array[start + 4] = raan;
    array[start + 5] = alphaV;
  }

  
  /** Semi-major axis (m). */
  private double a;

  /** First component of the circular eccentricity vector. */
  private double ex;

  /** Second component of the circular eccentricity vector. */
  private double ey;

  /** Inclination (rad). */
  private double i;

  /** Right Ascension of Ascending Node (rad). */
  private double raan;

  /** True longitude argument (rad). */
  private double alphaV;
  
  /** This internal class sums up the contribution of several forces into orbit derivatives.
  *
  * <p>The aim of this class is to gather the contributions of various perturbing
  * forces expressed as accelerations into one set of time-derivatives of
  * orbital parameters. It implements Gauss equations for circular parameters.
  * </p>
  *
  * @version $Id$
  * @author L. Maisonobe
  *
  */
 private class CircularDerivativesAdder
   extends OrbitDerivativesAdder {

   /** Create a new instance
    * @param parameters current orbital parameters
    * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
    */
   public CircularDerivativesAdder(OrbitalParameters parameters, double mu) {
     super(parameters, mu);
   }

   /** Initialize all derivatives to zero.
    * @param yDot reference to the array where to put the derivatives.
    */
   public void initDerivatives(double[] yDot) {

     // store orbit parameters
     super.initDerivatives(yDot);

     // TODO implement Gauss equations for circular orbits
     
   }

   /** Add the contribution of the Kepler evolution.
    * <p>Since the Kepler evolution if the most important, it should
    * be added after all the other ones, in order to improve
    * numerical accuracy.</p>
    */
   public void addKeplerContribution() {
     // TODO implement Gauss equations for circular orbits
     //yDot[5] += alphaVKepler;
	
   }

   /** Add the contribution of an acceleration expressed in (T, N, W)
    * local orbital frame.
    * @param t acceleration along the T axis (m/s<sup>2</sup>)
    * @param n acceleration along the N axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addTNWAcceleration(double t, double n, double w) {
     // TODO implement Gauss equations for circular orbits
//     yDot[0] += aT  * t;
//     yDot[1] += exT * t + exN * n + exW * w;
//     yDot[2] += eyT * t + eyN * n + eyW * w;
//     yDot[3] += hxW * w;
//     yDot[4] += hyW * w;
//     yDot[5] += lvW * w;
   }

   /** Add the contribution of an acceleration expressed in (Q, S, W)
    * local orbital frame.
    * @param q acceleration along the Q axis (m/s<sup>2</sup>)
    * @param s acceleration along the S axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addQSWAcceleration(double q, double s, double w) {
     // TODO implement Gauss equations for circular orbits
//     yDot[0] += aQ  * q + aS  * s;
//     yDot[1] += exQ * q + exS * s + exW * w;
//     yDot[2] += eyQ * q + eyS * s + eyW * w;
//     yDot[3] += hxW * w;
//     yDot[4] += hyW * w;
//     yDot[5] += lvW * w;
   }

 }

}
