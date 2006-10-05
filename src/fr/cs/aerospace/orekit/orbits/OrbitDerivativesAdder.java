package fr.cs.aerospace.orekit.orbits;

import org.spaceroots.mantissa.geometry.Vector3D;

import fr.cs.aerospace.orekit.frames.Frame;
import fr.cs.aerospace.orekit.utils.PVCoordinates;

/** This class sums up the contribution of several forces into orbit derivatives.
 *
 * <p>The aim of this class is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * orbital parameters. It implements Gauss equations for the orbit model
 * considered.</p>
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
 *
 * @version $Id$
 * @author L. Maisonobe
 *
 */
public abstract class OrbitDerivativesAdder {

  /** Orbital parameters. */
  protected OrbitalParameters parameters;
    
  /** Reference to the derivatives array to initialize. */
  protected double[] yDot;
    
  /** Central body attraction coefficient. */
  protected double mu;
    
  /** First vector of the (Q, S, W) local orbital frame. */
  protected Vector3D Q;
    
  /** Second vector of the (Q, S, W) local orbital frame. */
  protected Vector3D S;
    
  /** First vector of the (T, N, W) local orbital frame. */
  protected Vector3D T;
    
  /** Second vector of the (T, N, W) local orbital frame. */
  protected Vector3D N;
    
  /** Third vector of both the (Q, S, W) and (T, N, W) local orbital frames. */
  protected Vector3D W;

  /** Create a new instance
   * @param parameters current orbit parameters
   * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
   */
  protected OrbitDerivativesAdder(OrbitalParameters parameters, double mu) {
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
  public void initDerivatives(double[] yDot) {

    updateOrbitalFrames();

    // store derivatives array reference
    this.yDot = yDot;

    // initialize derivatives to zero
    for (int i = 0; i < 6; i++) {
      yDot[i] = 0;
    }

  }

  /** Add the contribution of the Kepler evolution.
   * <p>Since the Kepler evolution if the most important, it should
   * be added after all the other ones, in order to improve
   * numerical accuracy.</p>
   */
  public abstract void addKeplerContribution();

  /** Add the contribution of an acceleration expressed in (T, N, W)
   * local orbital frame.
   * @param t acceleration along the T axis (m/s<sup>2</sup>)
   * @param n acceleration along the N axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public abstract void addTNWAcceleration(double t, double n, double w);

  /** Add the contribution of an acceleration expressed in (Q, S, W)
   * local orbital frame.
   * @param q acceleration along the Q axis (m/s<sup>2</sup>)
   * @param s acceleration along the S axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public abstract void addQSWAcceleration(double q, double s, double w);

  /** Add the contribution of an acceleration expressed in the inertial frame 
   *  (it is important to make sure this acceleration is expressed in the 
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
  
  /** Get the frame where are expressed the XYZ coordinates.
   * @return the frame.
   */
  public abstract Frame getFrame();

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

}
