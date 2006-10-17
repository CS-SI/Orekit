package fr.cs.aerospace.orekit.propagation;

import org.spaceroots.mantissa.geometry.Vector3D;
import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.orbits.EquinoctialParameters;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class sums up the contribution of several forces into orbit derivatives.
 *
 * <p>The aim of this class is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * {@link fr.cs.aerospace.orekit.orbits.EquinoctialParameters}. It implements
 * Gauss equations for the orbit model considered.</p>
 *
 * <p>The proper way to use this class is to have the object implementing the
 * FirstOrderDifferentialEquations interface do the following calls each time
 * the computeDerivatives method is called:
 * <ul>
 *   <li>
 *     reinitialize the instance using the
 *     {@link #initDerivatives initDerivatives} method
 *   </li>
 *   <li>
 *     pass the instance to each force model in turn so that they can put their
 *     own contributions using the various AddXxxAcceleration methods
 *   </li>
 *   <li>
 *     finalize the derivatives by adding the Kepler natural evolution
 *     contribution
 *   </li>
 * </ul>
 * </p>
 * @see fr.cs.aerospace.orekit.orbits.EquinoctialParameters
 * @see fr.cs.aerospace.orekit.propagation.NumericalPropagator
 * @version $Id: OrbitDerivativesAdder.java 1052 2006-10-11 10:49:23 +0000 (mer., 11 oct. 2006) fabien $
 * @author L. Maisonobe
 * @author F.Maussion
 *
 */
public class EquinoctialGaussEquations {

  /** Create a new instance
   * @param parameters current orbit parameters
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   */
  protected EquinoctialGaussEquations(EquinoctialParameters parameters, double mu) {
    this.parameters = parameters;
    this.mu = mu;
    Q = new Vector3D();    
    S = new Vector3D();    
    T = new Vector3D();    
    N = new Vector3D();    
    W = new Vector3D();
    updateOrbitalFrames();     
  }
  
  /** Update the orbital frames. */
  private void updateOrbitalFrames() {
    PVCoordinates pvCoordinates = parameters.getPVCoordinates(mu);
        
    W.setToCrossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity());
    W.normalizeSelf();

    T.reset(pvCoordinates.getVelocity());
    T.normalizeSelf();
    N.setToCrossProduct(W, T);

    Q.reset(pvCoordinates.getPosition());
    Q.normalizeSelf();
    S.setToCrossProduct(W, Q);
  }

  /** Initialize all derivatives to zero.
   * @param yDot reference to the array where to put the derivatives.
   */
  protected void initDerivatives(double[] yDot , EquinoctialParameters parameters) {


    this.parameters = parameters;
    updateOrbitalFrames();

    // store derivatives array reference
    this.yDot = yDot;

    // initialize derivatives to zero
    for (int i = 0; i < 6; i++) {
      yDot[i] = 0;
    }
    
    // intermediate variables
    double ex2 = parameters.getEquinoctialEx() * parameters.getEquinoctialEx();
    double ey2 = parameters.getEquinoctialEy() * parameters.getEquinoctialEy();
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
    double na          = Math.sqrt(mu / parameters.getA());
    double n           = na / parameters.getA();
    double cLv         = Math.cos(parameters.getLv());
    double sLv         = Math.sin(parameters.getLv());
    double excLv       = parameters.getEquinoctialEx() * cLv;
    double eysLv       = parameters.getEquinoctialEy() * sLv;
    double excLvPeysLv = excLv + eysLv;
    double ksi         = 1 + excLvPeysLv;
    double nu          = parameters.getEquinoctialEx() * sLv - parameters.getEquinoctialEy() * cLv;
    double sqrt        = Math.sqrt(ksi * ksi + nu * nu);
    double oPksi       = 2 + excLvPeysLv;
    double h2          = parameters.getHx() * parameters.getHx()  + parameters.getHy() * parameters.getHy() ;
    double oPh2        = 1 + h2;
    double hxsLvMhycLv = parameters.getHx() * sLv - parameters.getHy() * cLv;

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
    exT = tEpsilonOnNASqrt * (parameters.getEquinoctialEx() + cLv);
    eyT = tEpsilonOnNASqrt * (parameters.getEquinoctialEy() + sLv);
        
    // coefficients along N
    exN = -epsilonOnNAKsiSqrt * (2 * parameters.getEquinoctialEy() * ksi + oMe2 * sLv);
    eyN =  epsilonOnNAKsiSqrt * (2 * parameters.getEquinoctialEx() * ksi + oMe2 * cLv);
                
    // coefficients along Q
    aQ  =  tOnEpsilonN * nu;
    exQ =  epsilonOnNA * sLv;
    eyQ = -epsilonOnNA * cLv;
        
    // coefficients along S
    aS  = tOnEpsilonN * ksi;
    exS = epsilonOnNAKsi * (parameters.getEquinoctialEx() + oPksi * cLv);
    eyS = epsilonOnNAKsi * (parameters.getEquinoctialEy() + oPksi * sLv);
        
    // coefficients along W
    lvW =  epsilonOnNAKsi * hxsLvMhycLv;
    exW = -parameters.getEquinoctialEy() * lvW;
    eyW =  parameters.getEquinoctialEx() * lvW;
    hxW =  epsilonOnNAKsit * oPh2 * cLv;
    hyW =  epsilonOnNAKsit * oPh2 * sLv;

  }
  
  /** Get the frame where are defined the XYZ coordinates.
   * @return the frame.
   */
  public Frame getFrame() {
	   return parameters.getFrame();
  }

  /** Add the contribution of the Kepler evolution.
   * <p>Since the Kepler evolution if the most important, it should
   * be added after all the other ones, in order to improve
   * numerical accuracy.</p>
   */
  protected void addKeplerContribution() {
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
  

  /** Add the contribution of an acceleration expressed in the inertial frame 
   *  (it is important to make sure this acceleration is defined in the 
   *  same frame as the orbit) .
   * @param x acceleration along the X axis (m/s<sup>2</sup>)
   * @param y acceleration along the Y axis (m/s<sup>2</sup>)
   * @param z acceleration along the Z axis (m/s<sup>2</sup>)
   */
  public void addXYZAcceleration(double x, double y, double z) {
    addTNWAcceleration(x * T.getX() + y * T.getY() + z * T.getZ(),
                       x * N.getX() + y * N.getY() + z * N.getZ(),
                       x * W.getX() + y * W.getY() + z * W.getZ());
  }

  /** Add the contribution of an acceleration expressed in inertial frame
   *  (it is important to make sure this acceleration is expressed in the 
   *  same frame as the orbit) .
   * @param gamma acceleration vector in the intertial frame (m/s<sup>2</sup>)
   */
  public void addAcceleration(Vector3D gamma) {
    addTNWAcceleration(Vector3D.dotProduct(gamma, T),
                       Vector3D.dotProduct(gamma, N),
                       Vector3D.dotProduct(gamma, W));
  }
  
  /** Get the first vector of the (Q, S, W) local orbital frame.
   * @return first vector of the (Q, S, W) local orbital frame */
  public Vector3D getQ() {
    return Q;
  }
    
  /** Get the second vector of the (Q, S, W) local orbital frame.
   * @return second vector of the (Q, S, W) local orbital frame */
  public Vector3D getS() {
    return S;
  }
    
  /** Get the first vector of the (T, N, W) local orbital frame.
   * @return first vector of the (T, N, W) local orbital frame */
  public Vector3D getT() {
    return T;
  }
    
  /** Get the second vector of the (T, N, W) local orbital frame.
   * @return second vector of the (T, N, W) local orbital frame */
  public Vector3D getN() {
    return N;
  }
    
  /** Get the third vector of both the (Q, S, W) and (T, N, W) local orbital
   * frames.
   * @return third vector of both the (Q, S, W) and (T, N, W) local orbital
   * frames
   */
  public Vector3D getW() {
    return W;
  }
  

  /** Orbital parameters. */
  private EquinoctialParameters parameters;
    
  /** Reference to the derivatives array to initialize. */
  private double[] yDot;
    
  /** Central body attraction coefficient. */
  private double mu;
    
  /** First vector of the (Q, S, W) local orbital frame. */
  private Vector3D Q;
    
  /** Second vector of the (Q, S, W) local orbital frame. */
  protected Vector3D S;
    
  /** First vector of the (T, N, W) local orbital frame. */
  private Vector3D T;
    
  /** Second vector of the (T, N, W) local orbital frame. */
  private Vector3D N;
    
  /** Third vector of both the (Q, S, W) and (T, N, W) local orbital frames. */
  private Vector3D W;
  
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

  
}
