package fr.cs.aerospace.orekit;

import org.spaceroots.mantissa.geometry.Vector3D;

/** This class sums up the contribution of several forces into orbit derivatives.
 *
 * <p>The aim of this class is to gather the contributions of various perturbing
 * forces expressed as accelerations into one set of time-derivatives of
 * orbital parameters.
 * </p>
 *
 * @version $Id$
 * @author L. Maisonobe
 *
 */
public class CartesianDerivativesAdder
  extends OrbitDerivativesAdder {

  /** Create a new instance.
   * @param parameters current orbital parameters
   * @param mu central body gravitational constant (m^3/s^2)
   */
  public CartesianDerivativesAdder(OrbitalParameters parameters, double mu) {
    super(parameters, mu);
  }

  /** Add the contribution of the Kepler evolution.
   * <p>Since the Kepler evolution if the most important, it should
   * be added after all the other ones, in order to improve
   * numerical accuracy.</p>
   */
  public void addKeplerContribution() {

    CartesianParameters cartesianParameters = (CartesianParameters) parameters;
    Vector3D position = cartesianParameters.getPosition();
    Vector3D velocity = cartesianParameters.getVelocity();

    // central body acceleration coefficient
    double r2 = Vector3D.dotProduct(position, position);
    double factor = -mu / (r2 * Math.sqrt(r2));

    // Kepler natural evolution
    yDot[0] += velocity.getX();
    yDot[1] += velocity.getY();
    yDot[2] += velocity.getZ();
    yDot[3] += factor * position.getX();
    yDot[4] += factor * position.getY();
    yDot[5] += factor * position.getZ();

  }

  /** Add the contribution of an acceleration expressed in (T, N, W)
   * local orbital frame.
   * @param t acceleration along the T axis (m/s)
   * @param n acceleration along the N axis (m/s)
   * @param w acceleration along the W axis (m/s)
   */
  public void addTNWAcceleration(double t, double n, double w) {
    yDot[3] += T.getX() * t + N.getX() * n + W.getX() * w;
    yDot[4] += T.getY() * t + N.getY() * n + W.getY() * w;
    yDot[5] += T.getZ() * t + N.getZ() * n + W.getZ() * w;
  }

  /** Add the contribution of an acceleration expressed in (Q, S, W)
   * local orbital frame.
   * @param q acceleration along the Q axis (m/s)
   * @param s acceleration along the S axis (m/s)
   * @param w acceleration along the W axis (m/s)
   */
  public void addQSWAcceleration(double q, double s, double w) {
    yDot[3] += Q.getX() * q + S.getX() * s + W.getX() * w;
    yDot[4] += Q.getY() * q + S.getY() * s + W.getY() * w;
    yDot[5] += Q.getZ() * q + S.getZ() * s + W.getZ() * w;
  }

  /** Add the contribution of an acceleration expressed in intertial frame.
   * @param x acceleration along the X axis (m/s)
   * @param y acceleration along the Y axis (m/s)
   * @param z acceleration along the Z axis (m/s)
   */
  public void addXYZAcceleration(double x, double y, double z) {
    yDot[3] += x;
    yDot[4] += y;
    yDot[5] += z;
  }

  /** Add the contribution of an acceleration expressed in intertial frame.
   * @param gamma acceleration vector in intertial frame (m/s)
   */
  public void addAcceleration(Vector3D gamma) {
    yDot[3] += gamma.getX();
    yDot[4] += gamma.getY();
    yDot[5] += gamma.getZ();
  }

}
