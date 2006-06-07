package fr.cs.aerospace.orekit;

/** This class sums up the contribution of several forces into orbit derivatives.
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
public class CircularDerivativesAdder
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
//    yDot[0] += aT  * t;
//    yDot[1] += exT * t + exN * n + exW * w;
//    yDot[2] += eyT * t + eyN * n + eyW * w;
//    yDot[3] += hxW * w;
//    yDot[4] += hyW * w;
//    yDot[5] += lvW * w;
  }

  /** Add the contribution of an acceleration expressed in (Q, S, W)
   * local orbital frame.
   * @param q acceleration along the Q axis (m/s<sup>2</sup>)
   * @param s acceleration along the S axis (m/s<sup>2</sup>)
   * @param w acceleration along the W axis (m/s<sup>2</sup>)
   */
  public void addQSWAcceleration(double q, double s, double w) {
    // TODO implement Gauss equations for circular orbits
//    yDot[0] += aQ  * q + aS  * s;
//    yDot[1] += exQ * q + exS * s + exW * w;
//    yDot[2] += eyQ * q + eyS * s + eyW * w;
//    yDot[3] += hxW * w;
//    yDot[4] += hyW * w;
//    yDot[5] += lvW * w;
  }

}
