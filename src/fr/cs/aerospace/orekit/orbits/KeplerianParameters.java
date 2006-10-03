package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/**
 * This class handles keplerian orbital parameters.

 * <p>
 * The parameters used internally are the classical keplerian elements:
 *   <pre>
 *     a
 *     e
 *     i
 *     &omega;
 *     &Omega;
 *     v
 *   </pre>
 * where &omega; stands for the Perigee Argument, &Omega; stands for the
 * Right Ascension of the Ascending Node and v stands for the true anomaly.
 * </p>

 * This class implements the
 * {@link org.spaceroots.mantissa.utilities.ArraySliceMappable ArraySliceMappable}
 * interface from the <a
 * href="http://www.spaceroots.org/archive.htm#MantissaSoftware">mantissa</a>
 * library, hence it can easily be processed by a numerical integrator.

 * @see     Orbit
 * @see     org.spaceroots.mantissa.utilities.ArraySliceMappable
 * @version $Id$
 * @author  L. Maisonobe
 * @author  G. Prat

 */
public class KeplerianParameters
  extends OrbitalParameters {

  /** Identifier for mean anomaly. */
  public static final int MEAN_ANOMALY = 0;

  /** Identifier for eccentric anomaly. */
  public static final int ECCENTRIC_ANOMALY = 1;

  /** Identifier for true anomaly. */
  public static final int TRUE_ANOMALY = 2;

  /** Eccentricity threshold for near circular orbits.
   *  if e < E_CIRC : the orbit is considered circular
   */
  public static final double E_CIRC = 1.e-10;
  
  /** Default constructor.
   * Build a new instance with arbitrary default elements.
   */
  public KeplerianParameters() {
    reset();
  }

  /** Creates a new instance
   * @param a  semi-major axis (m)
   * @param e eccentricity
   * @param i inclination (rad)
   * @param pa perigee argument (&omega;, rad)
   * @param raan right ascension of ascending node (&Omega;, rad)
   * @param anomaly mean, eccentric or true anomaly (rad)
   * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
   * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
   */
  public KeplerianParameters(double a, double e, double i,
                             double pa, double raan,
                             double anomaly, int type) {
    reset(a, e, i, pa, raan, anomaly, type);
  }

  /** Constructor from cartesian parameters.
   * @param pvCoordinates the PVCoordinates of the satellite
   * @param frame the frame in which are expressed the {@link PVCoordinates}  
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    reset(pvCoordinates, frame, mu);
  }

  /** Constructor from any kind of orbital parameters.
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public KeplerianParameters(OrbitalParameters op, double mu) {
    reset(op, mu);
  }

  /** Copy the instance.
  * <p>This method has been redeclared as public instead of protected.</p>
  * @return a copy of the instance.
  */
  public Object clone() {
    return new KeplerianParameters(a, e, i, pa, raan, v, TRUE_ANOMALY);
  }

  /** Reset the orbit to default.
   * Reset the orbit with arbitrary default elements.
   */
  public void reset() {
    a    = 1.0e7;
    e    = 1.0e-3;
    i    = 0.3;
    pa   = 0;
    raan = 0;
    setTrueAnomaly(0);
  }

  /** Reset the orbit from orbital parameters
   * @param a  semi-major axis (m)
   * @param e eccentricity
   * @param i inclination (rad)
   * @param pa perigee argument (&omega;, rad)
   * @param raan right ascension of ascending node (rad)
   * @param anomaly mean, eccentric or true anomaly (rad)
   * @param type type of anomaly, must be one of {@link #MEAN_ANOMALY},
   * {@link #ECCENTRIC_ANOMALY} or  {@link #TRUE_ANOMALY}
   */
  public void reset(double a, double e, double i, double pa, double raan,
                    double anomaly, int type) {

    this.a    =    a;
    this.e    =    e;
    this.i    =    i;
    this.pa   =   pa;
    this.raan = raan;

    switch (type) {
    case MEAN_ANOMALY :
      setMeanAnomaly(anomaly);
      break;
    case ECCENTRIC_ANOMALY :
      setEccentricAnomaly(anomaly);
      break;
    default :
      setTrueAnomaly(anomaly);
    }

  }

  protected void doReset(OrbitalParameters op, double mu) {
    a    = op.getA();
    e    = op.getE();
    i    = op.getI();
    raan = Math.atan2(op.getHy(), op.getHx());
    pa   = Math.atan2(op.getEquinoctialEy(), op.getEquinoctialEx()) - raan;
    setTrueAnomaly(op.getLv() - (pa + raan));
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

    // compute eccentricity
    double muA        = mu * a;
    double eSE        = Vector3D.dotProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()) / Math.sqrt(muA);
    double eCE        = rV2OnMu - 1;
    e                 = Math.sqrt(eSE * eSE + eCE * eCE);

    // compute inclination
    Vector3D momentum = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity());
    double   m2       = Vector3D.dotProduct(momentum, momentum);
    i = Vector3D.angle(momentum, Vector3D.plusK);

    // compute right ascension of ascending node
    Vector3D node     = Vector3D.crossProduct(Vector3D.plusK, momentum);
    double   n2       = Vector3D.dotProduct(node, node);
    // the following comparison with 0 IS REALLY numerically justified and stable
    raan = (n2 == 0) ? 0 : Math.atan2(node.getY(), node.getX());

    // compute true anomaly
    if (e < E_CIRC) {
      v = 0;
    } else {
      double E = Math.atan2(eSE, eCE);
      double k = 1 / (1 + Math.sqrt(m2 / muA));
      v = E + 2 * Math.atan(k * eSE / (1 - k *eCE));
    }

    // compute perigee argument
    double cosRaan = Math.cos(raan);
    double sinRaan = Math.sin(raan);
    double px = cosRaan * pvCoordinates.getPosition().getX() + sinRaan * pvCoordinates.getPosition().getY();
    double py = Math.cos(i) * (cosRaan * pvCoordinates.getPosition().getY() - sinRaan * pvCoordinates.getPosition().getX())
              + Math.sin(i) * pvCoordinates.getPosition().getZ();
    pa = Math.atan2(py, px) - v;

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
    reset();

  }

  /** Get the eccentricity.
   * @return eccentricity
   */
  public double getE() {
    return e;
  }

  /** Set the eccentricity.
   * @param e eccentricity
   */
  public void setE(double e) {

    this.e = e;

    // invalidate position and velocity
    super.reset();

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

  /** Get the perigee argument.
   * @return perigee argument (rad)
   */
  public double getPerigeeArgument() {
    return pa;
  }

  /** Set the perigee argument.
   * @param pa perigee argument (rad)
   */
  public void setPerigeeArgument(double pa) {

    this.pa = pa;

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

  /** Get the true anomaly.
   * @return true anomaly (rad)
   */
  public double getTrueAnomaly() {
    return v;
  }

  /** Set the true anomaly.
   * @param v true anomaly (rad)
   */
  public void setTrueAnomaly (double v) {

    this.v = v;

    // invalidate position and velocity
    super.reset();

  }

  /** Get the eccentric anomaly.
   * @return eccentric anomaly (rad)
   */
  public double getEccentricAnomaly() {
    double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
    return v - 2 * Math.atan(beta * Math.sin(v) / (1 + beta * Math.cos(v)));
  }

  /** Set the eccentric anomaly.
   * @param E eccentric anomaly (rad)
   */
  public void setEccentricAnomaly (double E) {

    double beta = e / (1 + Math.sqrt((1 - e) * (1 + e)));
    v = E + 2 * Math.atan(beta * Math.sin(E) / (1 - beta * Math.cos(E)));

    // invalidate position and velocity
    super.reset();

  }

  /** Get the mean anomaly.
   * @return mean anomaly (rad)
   */
  public double getMeanAnomaly() {
    double E = getEccentricAnomaly();
    return E - e * Math.sin(E);
  }

  /** Set the mean anomaly.
   * @param M mean anomaly (rad)
   */
  public void setMeanAnomaly (double M) {
    
    // resolution of Kepler equation for keplerian parameters
    double E = M;
    double shift = 0.0;
    double EmM   = 0.0;
    int iter = 0;
    do {
      double f2 = e * Math.sin(E);
      double f1 = 1.0 - e * Math.cos(E);
      double f0 = EmM - f2;

      double f12 = 2 * f1;
      shift = f0 * f12 / (f1 * f12 - f0 * f2);

      EmM -= shift;
      E    = M + EmM;

    } while ((++iter < 50) && (Math.abs(shift) > 1.0e-12));

    setEccentricAnomaly(E);

  }

  
  /** Get the first component of the eccentricity vector. 
   * @return first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    return  e * Math.cos(pa + raan);
  }

  /** Get the second component of the eccentricity vector. 
   * @return second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    return  e * Math.sin(pa + raan);
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

  /** Get the true latitude argument.
   * @return true latitude argument (rad)
   */
  public double getLv() {
    return pa + raan + v;
  }

  /** Get the eccentric latitude argument.
   * @return eccentric latitude argument.(rad)
   */
  public double getLE() {
    return pa + raan + getEccentricAnomaly();
  }

  /** Get the mean latitude argument.
   * @return mean latitude argument.(rad)
   */
  public double getLM() {
    return pa + raan + getMeanAnomaly();
  }

  /**  Returns a string representation of this Orbit object
   * @return a string representation of this object
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append('{');
    sb.append(a);
    sb.append(' ');
    sb.append(e);
    sb.append(' ');
    sb.append(Math.toDegrees(i));
    sb.append(' ');
    sb.append(Math.toDegrees(pa));
    sb.append(' ');
    sb.append(Math.toDegrees(raan));
    sb.append(' ');
    sb.append(Math.toDegrees(v));
    sb.append('}');
    return sb.toString();
  }

  /** Build an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object.
   * <p>This is a factory method allowing to build the right type of
   * {@link OrbitDerivativesAdder OrbitDerivativesAdder} object, for
   * this class, a <code>KeplerianDerivativesAdder</code>
   * object is built.</p>
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   * @return an instance of {@link OrbitDerivativesAdder
   * OrbitDerivativesAdder} associated with this object
   */
  public OrbitDerivativesAdder getDerivativesAdder(double mu) {
    return new KeplerianDerivativesAdder(mu);
  }

  /** Reinitialize internal state from the specified array slice data.
   * @param start start index in the array
   * @param array array holding the data to extract (a, e, i, pa, raan, v)
   */
  public void mapStateFromArray(int start, double[] array) {

    a    = array[start];
    e    = array[start + 1];
    i    = array[start + 2];
    pa   = array[start + 3];
    raan = array[start + 4];
    v    = array[start + 5];

    // invalidate position and velocity
    super.reset();

  }

  /** Store internal state data into the specified array slice.
   * @param start start index in the array
   * @param array array where data should be stored (a, e, i, pa, raan, v)
   */
  public void mapStateToArray(int start, double[] array) {
    array[start]     = a;
    array[start + 1] = e;
    array[start + 2] = i;
    array[start + 3] = pa;
    array[start + 4] = raan;
    array[start + 5] = v;
  }

  /** Semi-major axis (m). */
  private double a;

  /** Eccentricity. */
  private double e;

  /** Inclination (rad). */
  private double i;

  /** Perigee Argument (rad). */
  private double pa;

  /** Right Ascension of Ascending Node (rad). */
  private double raan;

  /** True anomaly (rad). */
  private double v;  
  
  /** This internal class sums up the contribution of several forces into orbit derivatives.
  *
  * <p>The aim of this class is to gather the contributions of various perturbing
  * forces expressed as accelerations into one set of time-derivatives of
  * orbital parameters. It implements Gauss equations for keplerian parameters.
  * </p>
  *
  * @version $Id$
  * @author L. Maisonobe
  * @author G. Prat
  *
  */
   private class KeplerianDerivativesAdder
   extends OrbitDerivativesAdder {

   /** Multiplicative coefficients for the perturbing accelerations. */
   private double aT;
   private double aQ;
   private double aS;

   private double eT;
   private double eN;
   private double eQ;
   private double eS;

   private double iW;

   private double paT;
   private double paN;
   private double paQ;
   private double paS;
   private double paW;

   private double raanW;

   private double vT;
   private double vN;
   private double vQ;
   private double vS;

   /** Kepler evolution on true latitude argument. */
   private double vKepler;

   /** Create a new instance
    * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
    */
   public KeplerianDerivativesAdder(double mu) {
     super(KeplerianParameters.this, mu);
   }

   /** Initialize all derivatives to zero.
    * @param yDot reference to the array where to put the derivatives.
    */
   public void initDerivatives(double[] yDot) {

     // store orbit parameters
     super.initDerivatives(yDot);
     
     // excentric anomaly
     
     double E = getEccentricAnomaly();

     // intermediate variables
     double e2         = e * e;
     double oMe2       = 1 - e2;
     double epsilon    = Math.sqrt(oMe2);
     double na         = Math.sqrt(mu / a);
     double n          = na / a;
     double cosV       = Math.cos(v);
     double sinV       = Math.sin(v);
     double ecV        = e * cosV;
     double ksi        = 1 + ecV;
     double nu         = e * sinV;
     double sqrt       = Math.sqrt(ksi * ksi + nu * nu);
     double oPksi      = 2 + ecV;
     double cosVOnE    = cosV / e;
     double sinVOnE    = sinV / e;
     double paPv       = pa + v;

     double epsilonOnNA        = epsilon / na;
     double epsilonOnNAKsi     = epsilonOnNA / ksi;
     double epsilonOnNAKsiSqrt = epsilonOnNAKsi / sqrt;
     double tOnEpsilonN        = 2 / (n * epsilon);
     double tEpsilonOnNASqrt   = 2 * epsilonOnNA / sqrt;

     // Kepler natural evolution
     double dvOnde = oPksi * nu / (e*oMe2);
     vKepler = n * ksi * ksi / (oMe2 * epsilon);

     // coefficients along T
     aT  = tOnEpsilonN * sqrt;
     eT  = tEpsilonOnNASqrt * (e + cosV);
     paT = tEpsilonOnNASqrt * sinVOnE;
     // vT  = dvOnde * eT
     //    - 2 * sinVOnE * ksi * (ksi + e2) / (na * sqrt * oMe2);
     vT  = dvOnde * eT
         - 2 * sinVOnE * ksi * (ksi + e2)* epsilonOnNA / (sqrt * oMe2);

     // coefficients along N
     eN  = - epsilonOnNAKsiSqrt * oMe2 * sinV;
     paN = epsilonOnNAKsiSqrt * (2 + (1 + e2) * cosVOnE);
     vN  = dvOnde * eN - ksi * epsilonOnNA * cosVOnE / sqrt;

     // coefficients along Q
     aQ  = tOnEpsilonN * nu;
     eQ  = epsilonOnNA * sinV;
     paQ = - epsilonOnNA * cosVOnE;
     vQ  = dvOnde * eQ - ksi * (2 - cosVOnE * ksi) / (na * epsilon);

     // coefficients along S
     aS  = tOnEpsilonN * ksi;
     eS  = epsilonOnNA * (Math.cos(E) + cosV);
     paS = epsilonOnNAKsi * oPksi * sinVOnE;
     vS  = dvOnde * eS - ksi * oPksi * sinVOnE / (na * epsilon);

     // coefficients along W
     //iW    = epsilonOnNAKsiSqrt * epsilon * Math.cos(paPv);
     iW    = epsilonOnNAKsi * Math.cos(paPv);
     raanW = epsilonOnNAKsi * Math.sin(paPv) / Math.sin(i);
     // paW   = -epsilonOnNA * raanW * Math.cos(i) / (e * sqrt);
     paW   = - raanW * Math.cos(i);
   }

   /** Add the contribution of the Kepler evolution.
    * <p>Since the Kepler evolution if the most important, it should
    * be added after all the other ones, in order to improve
    * numerical accuracy.</p>
    */
   public void addKeplerContribution() {
     yDot[5] += vKepler;
   }

   /** Add the contribution of an acceleration expressed in (T, N, W)
    * local orbital frame.
    * @param t acceleration along the T axis (m/s<sup>2</sup>)
    * @param n acceleration along the N axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addTNWAcceleration(double t, double n, double w) {
     yDot[0] += aT * t;
     yDot[1] += eT * t + eN * n;
     yDot[2] += iW * w;
     yDot[3] += paT * t + paN * n + paW * w;
     yDot[4] += raanW * w;
     yDot[5] += vT * t + vN * n;
   }

   /** Add the contribution of an acceleration expressed in (Q, S, W)
    * local orbital frame.
    * @param q acceleration along the Q axis (m/s<sup>2</sup>)
    * @param s acceleration along the S axis (m/s<sup>2</sup>)
    * @param w acceleration along the W axis (m/s<sup>2</sup>)
    */
   public void addQSWAcceleration(double q, double s, double w) {
     yDot[0] += aQ * q + aS * s;
     yDot[1] += eQ * q + eS * s;
     yDot[2] += iW * w;
     yDot[3] += paQ * q + paS * s + paW * w;
     yDot[4] += raanW * w;
     yDot[5] += vQ * q + vS * s;
   }
      
 }

}
