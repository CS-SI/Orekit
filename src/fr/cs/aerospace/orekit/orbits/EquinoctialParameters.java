package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

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
   * @param frame the frame in which are defined the parameters
   */
  public EquinoctialParameters(double a, double ex, double ey,
                               double hx, double hy,
                               double l, int type, Frame frame) {
    reset(a, ex, ey, hx, hy, l, type, frame);
  }

  /** Constructor from cartesian parameters.
   * @param pvCoordinates the position end velocity
   * @param frame the frame in which are defined the {@link PVCoordinates} 
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EquinoctialParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    reset(pvCoordinates, frame,  mu);
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
    return new EquinoctialParameters(a, ex, ey, hx, hy, lv, TRUE_LATITUDE_ARGUMENT, frame);
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
    frame = Frame.getJ2000();
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
   * @param frame the frame in which are defined the parameters.
   */
  public void reset(double a, double ex, double ey,
                    double hx, double hy, double l, int type, Frame frame) {

    this.a  =  a;
    this.ex = ex;
    this.ey = ey;
    this.hx = hx;
    this.hy = hy;
    this.frame = frame;

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
    frame = op.getFrame();
  }

  /** Update the parameters from the current position and velocity. */
  protected void updateFromPVCoordinates() {

    // get cartesian elements
    double   mu       = getCachedMu();
    PVCoordinates pvCoordinates = getPVCoordinates(mu);

    // compute semi-major axis
    double r       = pvCoordinates.getPosition().getNorm();
    double V2      = Vector3D.dotProduct(pvCoordinates.getVelocity(), pvCoordinates.getVelocity());
    double rV2OnMu = r * V2 / mu;
    a              = r / (2 - rV2OnMu);

    // compute inclination vector
    Vector3D w = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity());
    w.normalizeSelf();
    double d = 1. / (1 + w.getZ());
    hx = -d * w.getY();
    hy =  d * w.getX();

    // compute true latitude argument
    double cLv = (pvCoordinates.getPosition().getX() - d * pvCoordinates.getPosition().getZ() * w.getX()) / r;
    double sLv = (pvCoordinates.getPosition().getY() - d * pvCoordinates.getPosition().getZ() * w.getY()) / r;
    lv = Math.atan2(sLv, cLv);

    // compute eccentricity vector
    double eSE = Vector3D.dotProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()) / Math.sqrt(mu * a);
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
   * this class, an <code>EquinoctialDerivativesAdder</code>
   * object is built.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object
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
  
  /** This internal class sums up the contribution of several forces into orbit derivatives.
  *
  * <p>The aim of this class is to gather the contributions of various perturbing
  * forces expressed as accelerations into one set of time-derivatives of
  * orbital parameters. It implements Gauss equations for equinoctial parameters.
  * </p>
  *
  * @version $Id$
  * @author M. Romero
  * @author L. Maisonobe
  *
  */
 private class EquinoctialDerivativesAdder
   extends OrbitDerivativesAdder {

   /** Multiplicative coefficients for the perturbing accelerations. */
   private double aT;
   private double exT;
   private double eyT;

   private double aQ;
   private double exQ;
   private double eyQ;

   private double exN;
   private double eyN;

   private double aS;
   private double exS;
   private double eyS;

   private double eyW;
   private double exW;
   private double hxW;
   private double hyW;
   private double lvW;

   /** Kepler evolution on true latitude argument. */
   private double lvKepler;

   /** Create a new instance
    * @param parameters current orbital parameters
    * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
    */
   public EquinoctialDerivativesAdder(OrbitalParameters parameters, double mu) {
     super(parameters, mu);
   }

   /** Initialize all derivatives to zero.
    * @param yDot reference to the array where to put the derivatives.
    */
   public void initDerivatives(double[] yDot) {

     // store orbit parameters
     super.initDerivatives(yDot);

     // intermediate variables
     double ex2 = ex * ex;
     double ey2 = ey * ey;
     double e2  = ex2 + ey2;
     double e   = Math.sqrt(e2);
     if (e > 1) {
       throw new IllegalArgumentException("Eccentricity is becoming"
                                          + " greater than 1."
                                          + " Unable to continue.");
     }

     // intermediate variables
     double oMe2        = (1 - e) * (1 + e);
     double epsilon     = Math.sqrt(oMe2);
     double na          = Math.sqrt(mu / a);
     double n           = na / a;
     double cLv         = Math.cos(lv);
     double sLv         = Math.sin(lv);
     double excLv       = ex * cLv;
     double eysLv       = ey * sLv;
     double excLvPeysLv = excLv + eysLv;
     double ksi         = 1 + excLvPeysLv;
     double nu          = ex * sLv - ey * cLv;
     double sqrt        = Math.sqrt(ksi * ksi + nu * nu);
     double oPksi       = 2 + excLvPeysLv;
     double h2          = hx * hx + hy * hy;
     double oPh2        = 1 + h2;
     double hxsLvMhycLv = hx * sLv - hy * cLv;

     double epsilonOnNA        = epsilon / na;
     double epsilonOnNAKsi     = epsilonOnNA / ksi;
     double epsilonOnNAKsiSqrt = epsilonOnNAKsi / sqrt;
     double tOnEpsilonN        = 2 / (n * epsilon);
     double tEpsilonOnNASqrt   = 2 * epsilonOnNA / sqrt;
     double epsilonOnNAKsit    = epsilonOnNA / (2 * ksi);
     
     // Kepler natural evolution
     lvKepler = n * ksi * ksi / (oMe2 * epsilon);

     // coefficients along T
     aT  = tOnEpsilonN * sqrt;
     exT = tEpsilonOnNASqrt * (ex + cLv);
     eyT = tEpsilonOnNASqrt * (ey + sLv);
         
     // coefficients along N
     exN = -epsilonOnNAKsiSqrt * (2 * ey * ksi + oMe2 * sLv);
     eyN =  epsilonOnNAKsiSqrt * (2 * ex * ksi + oMe2 * cLv);
                 
     // coefficients along Q
     aQ  =  tOnEpsilonN * nu;
     exQ =  epsilonOnNA * sLv;
     eyQ = -epsilonOnNA * cLv;
         
     // coefficients along S
     aS  = tOnEpsilonN * ksi;
     exS = epsilonOnNAKsi * (ex + oPksi * cLv);
     eyS = epsilonOnNAKsi * (ey + oPksi * sLv);
         
     // coefficients along W
     lvW =  epsilonOnNAKsi * hxsLvMhycLv;
     exW = -ey * lvW;
     eyW =  ex * lvW;
     hxW =  epsilonOnNAKsit * oPh2 * cLv;
     hyW =  epsilonOnNAKsit * oPh2 * sLv;
     //hxW =  epsilonOnNAKsi * oPh2 * cLv;
     //hyW =  epsilonOnNAKsi * oPh2 * sLv;
     
   }

   /** Add the contribution of the Kepler evolution.
    * <p>Since the Kepler evolution if the most important, it should
    * be added after all the other ones, in order to improve
    * numerical accuracy.</p>
    */
   public void addKeplerContribution() {
     yDot[5] += lvKepler;
   }

   /** Add the contribution of an acceleration expressed in (T, N, W)
    * local orbital frame.
    * @param t acceleration along the T axis (m/s<sup>2</sup>)
    * @param n acceleration along the N axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addTNWAcceleration(double t, double n, double w) {
     yDot[0] += aT  * t;
     yDot[1] += exT * t + exN * n + exW * w;
     yDot[2] += eyT * t + eyN * n + eyW * w;
     yDot[3] += hxW * w;
     yDot[4] += hyW * w;
     yDot[5] += lvW * w;
   }

   /** Add the contribution of an acceleration expressed in (Q, S, W)
    * local orbital frame.
    * @param q acceleration along the Q axis (m/s<sup>2</sup>)
    * @param s acceleration along the S axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addQSWAcceleration(double q, double s, double w) {
     yDot[0] += aQ  * q + aS  * s;
     yDot[1] += exQ * q + exS * s + exW * w;
     yDot[2] += eyQ * q + eyS * s + eyW * w;
     yDot[3] += hxW * w;
     yDot[4] += hyW * w;
     yDot[5] += lvW * w;
   }
   
   /** Get the frame where are defined the XYZ coordinates.
    * @return the frame.
    */
   public Frame getFrame() {
	   return EquinoctialParameters.this.getFrame();
   }
 }

}
