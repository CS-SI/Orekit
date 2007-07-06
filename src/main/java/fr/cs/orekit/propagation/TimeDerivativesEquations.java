package fr.cs.orekit.propagation;

import java.util.Arrays;
import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.Translator;
import fr.cs.orekit.orbits.EquinoctialParameters;
import fr.cs.orekit.utils.PVCoordinates;

/** This class sums up the contribution of several forces into orbit and mass derivatives.
 *
 * <p>The aim of this class is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * {@link fr.cs.orekit.orbits.EquinoctialParameters} plus one mass derivatives.
 * It implements Gauss equations for the equinoctial parameters.</p>
 *  <p>
 * The state vector handled internally has the form that follows:
 *   <pre>
 *     y[0] = a
 *     y[1] = ex 
 *     y[2] = ey 
 *     y[3] = hx
 *     y[4] = hy 
 *     y[5] = lv
 *     y[6] = mass
 *   </pre>
 * where the six firsts paramters stands for the equinoctial parameters and the 7th
 * for the mass (kg) at the current time.
 * </p>
 * <p>The proper way to use this class is to have the object implementing the
 * FirstOrderDifferentialEquations interface do the following calls each time
 * the computeDerivatives method is called:
 * <ul>
 *   <li>
 *     reinitialize the instance using the
 *     {@link #initDerivatives(double[], EquinoctialParameters)} method
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
 * @see fr.cs.orekit.orbits.EquinoctialParameters
 * @see fr.cs.orekit.propagation.NumericalPropagator
 * @version $Id: OrbitDerivativesAdder.java 1052 2006-10-11 10:49:23 +0000 (mer., 11 oct. 2006) fabien $
 * @author L. Maisonobe
 * @author F.Maussion
 *
 */
public class TimeDerivativesEquations {

  /** Create a new instance.
   * @param parameters current orbit parameters
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   */
  protected TimeDerivativesEquations(EquinoctialParameters parameters, double mu) {
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
        
    W = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()).normalize();
  
    T = pvCoordinates.getVelocity().normalize();
    
    N = Vector3D.crossProduct(W, T);

    Q = pvCoordinates.getPosition().normalize();
   
    S = Vector3D.crossProduct(W, Q);
  }

  /** Initialize all derivatives to zero.
   * @param yDot reference to the array where to put the derivatives.
   * @param parameters current orbit parameters
   */
  protected void initDerivatives(double[] yDot ,
                               EquinoctialParameters parameters) {
                                

    this.parameters = parameters;
    updateOrbitalFrames();

    // store derivatives array reference
    this.yDot = yDot;

    // initialize derivatives to zero
    Arrays.fill(yDot, 0.0);
    
    // intermediate variables
    double ex  = parameters.getEquinoctialEx();
    double ey  = parameters.getEquinoctialEy();
    double ex2 = ex * ex;
    double ey2 = ey * ey;
    double e2  = ex2 + ey2;
    double e   = Math.sqrt(e2);
    if (e > 1) {
      String message = Translator.getInstance().translate("Eccentricity is becoming"
                                                          + " greater than 1."
                                                          + " Unable to continue.");
      throw new IllegalArgumentException(message);
    }    

    // intermediate variables
    double oMe2        = (1 - e) * (1 + e);
    double epsilon     = Math.sqrt(oMe2);
    double a           = parameters.getA();
    double na          = Math.sqrt(mu / a);
    double n           = na / a;
    double lv          = parameters.getLv();
    double cLv         = Math.cos(lv);
    double sLv         = Math.sin(lv);
    double excLv       = ex * cLv;
    double eysLv       = ey * sLv;
    double excLvPeysLv = excLv + eysLv;
    double ksi         = 1 + excLvPeysLv;
    double nu          = ex * sLv - ey * cLv;
    double sqrt        = Math.sqrt(ksi * ksi + nu * nu);
    double oPksi       = 2 + excLvPeysLv;
    double hx          = parameters.getHx();
    double hy          = parameters.getHy();
    double h2          = hx * hx  + hy * hy ;
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
  
  /** Add the contribution of the flow rate (dm/dt).
   * @param dMass the flow rate (dm/dt)
   */
  public void addMassDerivative(double dMass) {
    if(dMass>0) {
      String message1 = Translator.getInstance().translate("Flow rate (dm/dt) is positive : ");
      throw new IllegalArgumentException(
                    message1 + dMass + " kg/s");
    }
    yDot[6] += dMass;
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
