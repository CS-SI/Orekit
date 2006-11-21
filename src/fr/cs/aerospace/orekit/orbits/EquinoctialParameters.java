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
 * <p>
 * The instance <code>EquinoctialParameters</code> is guaranted to be immutable.
 * </p>
 * @see     Orbit
 * @version $Id$
 * @author  M. Romero
 * @author  L. Maisonobe
 * @author  G. Prat
 * @author  F.Maussion
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
    super(Frame.getJ2000());
    a  = 1.0e7;
    ex = 1.0e-3;
    ey = 0;
    hx = 0.15;
    hy = 0;
    lv = 0;
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
    super(frame);
    this.a  =  a;
    this.ex = ex;
    this.ey = ey;
    this.hx = hx;
    this.hy = hy;
    
    switch (type) {
    case MEAN_LATITUDE_ARGUMENT :
      this.lv = computeLM(l);
      break;
    case ECCENTRIC_LATITUDE_ARGUMENT :
      this.lv = computeLE(l);
      break;
    default :
      this.lv = l;
    }
    
  }
  
  /** Constructor from cartesian parameters.
   * @param pvCoordinates the position end velocity
   * @param frame the frame in which are defined the {@link PVCoordinates} 
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EquinoctialParameters(PVCoordinates pvCoordinates, Frame frame, double mu) {
    super(pvCoordinates, frame,  mu);
    
    //  compute semi-major axis
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
  
  /** Constructor from any kind of orbital parameters
   * @param op orbital parameters to copy
   * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
   */
  public EquinoctialParameters(OrbitalParameters op, double mu) {
    super(op.frame);
    a  = op.getA();
    ex = op.getEquinoctialEx();
    ey = op.getEquinoctialEy();
    hx = op.getHx();
    hy = op.getHy();
    lv = op.getLv();
  }
  
  /** Get the semi-major axis.
   * @return semi-major axis (m)
   */
  public double getA() {
    return a;
  }
  
  /** Get the first component of the eccentricity vector.
   * @return e cos(&omega; + &Omega;), first component of the eccentricity vector
   */
  public double getEquinoctialEx() {
    return ex;
  }
  
  /** Get the second component of the eccentricity vector.
   * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
   */
  public double getEquinoctialEy() {
    return ey;
  }
  
  /** Get the first component of the inclination vector.
   * @return tan(i/2) cos(&Omega;), first component of the inclination vector
   */
  public double getHx() {
    return hx;
  }
  
  /** Get the second component of the inclination vector.
   * @return tan(i/2) sin(&Omega;), second component of the inclination vector
   */
  public double getHy() {
    return hy;
  }
  
  /** Get the true latitude argument.
   * @return v + &omega; + &Omega; true latitude argument (rad)
   */
  public double getLv() {
    return lv;
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
  
  /** Computes the eccentric latitude argument.
   * @param lE = E + &omega; + &Omega; eccentric latitude argument (rad)
   * @return the true latitude argument
   */
  private double computeLE(double lE) {
    double epsilon = Math.sqrt(1 - ex * ex - ey * ey);
    double cosLE   = Math.cos(lE);
    double sinLE   = Math.sin(lE);
    return lE + 2 * Math.atan((ex * sinLE - ey * cosLE)
                             / (epsilon + 1 - ex * cosLE - ey * sinLE));
  }
  
  /** Get the mean latitude argument.
   * @return M + &omega; + &Omega; mean latitude argument (rad)
   */
  public double getLM() {
    double lE = getLE();
    return lE - ex * Math.sin(lE) + ey * Math.cos(lE);
  }
  
  /** Computes the mean latitude argument.
   * @param lM = M + &omega; + &Omega; mean latitude argument (rad)
   * @return the true latitude argument
   */
  private double computeLM(double lM) {
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
    
    return computeLE(lE); // which set the lv parameter
    
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
  
  /** Semi-major axis (m). */
  private final double a;
  
  /** First component of the eccentricity vector. */
  private final double ex;
  
  /** Second component of the eccentricity vector. */
  private final double ey;
  
  /** First component of the inclination vector. */
  private final double hx;
  
  /** Second component of the inclination vector. */
  private final double hy;
  
  /** True latitude argument (rad). */
  private final double lv;
  
  private static final long serialVersionUID = -6671885168854533487L;
}
