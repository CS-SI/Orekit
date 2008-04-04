package fr.cs.orekit.propagation.numerical;

import java.io.Serializable;
import java.util.Arrays;
import org.apache.commons.math.geometry.Vector3D;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.errors.PropagationException;
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
 * @see fr.cs.orekit.propagation.numerical.NumericalPropagator
 * @version $Id: OrbitDerivativesAdder.java 1052 2006-10-11 10:49:23 +0000 (mer., 11 oct. 2006) fabien $
 * @author L. Maisonobe
 * @author F.Maussion
 *
 */
public class TimeDerivativesEquations implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -5105883494297693930L;

    /** Orbital parameters. */
    private EquinoctialParameters parameters;

    /** Reference to the derivatives array to initialize. */
    private double[] yDot;

    /** Central body attraction coefficient. */
    private double mu;

    /** First vector of the (q, s, w) local orbital frame. */
    private Vector3D lofQ;

    /** Second vector of the (q, s, w) local orbital frame. */
    private Vector3D lofS;

    /** First vector of the (t, n, w) local orbital frame. */
    private Vector3D lofT;

    /** Second vector of the (t, n, w) local orbital frame. */
    private Vector3D lofN;

    /** Third vector of both the (q, s, w) and (t, n, w) local orbital frames. */
    private Vector3D lofW;

    /** Multiplicative coefficients for the perturbing accelerations along lofQ. */
    private double aQ;
    private double exQ;
    private double eyQ;

    /** Multiplicative coefficients for the perturbing accelerations along lofS. */
    private double aS;
    private double exS;
    private double eyS;

    /** Multiplicative coefficients for the perturbing accelerations along lofT. */
    private double aT;
    private double exT;
    private double eyT;

    /** Multiplicative coefficients for the perturbing accelerations along lofN. */
    private double exN;
    private double eyN;

    /** Multiplicative coefficients for the perturbing accelerations along lofW. */
    private double eyW;
    private double exW;
    private double hxW;
    private double hyW;
    private double lvW;

    /** Kepler evolution on true latitude argument. */
    private double lvKepler;

    /** Create a new instance.
     * @param parameters current orbit parameters
     * @param mu central body gravitational constant (m<sup>3</sup>/s<sup>2</sup>)
     */
    protected TimeDerivativesEquations(EquinoctialParameters parameters, double mu) {
        this.parameters = parameters;
        this.mu = mu;
        lofQ = new Vector3D();
        lofS = new Vector3D();
        lofT = new Vector3D();
        lofN = new Vector3D();
        lofW = new Vector3D();
        updateOrbitalFrames();
    }

    /** Update the orbital frames. */
    private void updateOrbitalFrames() {

        // get the position/velocity vectors
        final PVCoordinates pvCoordinates = parameters.getPVCoordinates(mu);

        // compute orbital plane normal vector
        lofW = Vector3D.crossProduct(pvCoordinates.getPosition(), pvCoordinates.getVelocity()).normalize();

        // compute (q, s, w) local orbital frame
        lofQ = pvCoordinates.getPosition().normalize();
        lofS = Vector3D.crossProduct(lofW, lofQ);

        // compute (t, n, w) local orbital frame
        lofT = pvCoordinates.getVelocity().normalize();
        lofN = Vector3D.crossProduct(lofW, lofT);

    }

    /** Initialize all derivatives to zero.
     * @param yDot reference to the array where to put the derivatives.
     * @param parameters current orbit parameters
     * @exception PropagationException if the orbit evolve out of supported range
     */
    protected void initDerivatives(double[] yDot, EquinoctialParameters parameters)
        throws PropagationException {


        this.parameters = parameters;
        updateOrbitalFrames();

        // store derivatives array reference
        this.yDot = yDot;

        // initialize derivatives to zero
        Arrays.fill(yDot, 0.0);

        // intermediate variables
        final double ex  = parameters.getEquinoctialEx();
        final double ey  = parameters.getEquinoctialEy();
        final double ex2 = ex * ex;
        final double ey2 = ey * ey;
        final double e2  = ex2 + ey2;
        final double e   = Math.sqrt(e2);
        if (e >= 1) {
            throw new PropagationException("orbit becomes hyperbolic, unable to propagate it further (e: {0})",
                                           new Object[] { new Double(e) });
        }

        // intermediate variables
        final double oMe2        = (1 - e) * (1 + e);
        final double epsilon     = Math.sqrt(oMe2);
        final double a           = parameters.getA();
        final double na          = Math.sqrt(mu / a);
        final double n           = na / a;
        final double lv          = parameters.getLv();
        final double cLv         = Math.cos(lv);
        final double sLv         = Math.sin(lv);
        final double excLv       = ex * cLv;
        final double eysLv       = ey * sLv;
        final double excLvPeysLv = excLv + eysLv;
        final double ksi         = 1 + excLvPeysLv;
        final double nu          = ex * sLv - ey * cLv;
        final double sqrt        = Math.sqrt(ksi * ksi + nu * nu);
        final double oPksi       = 2 + excLvPeysLv;
        final double hx          = parameters.getHx();
        final double hy          = parameters.getHy();
        final double h2          = hx * hx  + hy * hy ;
        final double oPh2        = 1 + h2;
        final double hxsLvMhycLv = hx * sLv - hy * cLv;

        final double epsilonOnNA        = epsilon / na;
        final double epsilonOnNAKsi     = epsilonOnNA / ksi;
        final double epsilonOnNAKsiSqrt = epsilonOnNAKsi / sqrt;
        final double tOnEpsilonN        = 2 / (n * epsilon);
        final double tEpsilonOnNASqrt   = 2 * epsilonOnNA / sqrt;
        final double epsilonOnNAKsit    = epsilonOnNA / (2 * ksi);

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

    /** Add the contribution of an acceleration expressed in (t, n, w)
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

    /** Add the contribution of an acceleration expressed in (q, s, w)
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
        addTNWAcceleration(x * lofT.getX() + y * lofT.getY() + z * lofT.getZ(),
                           x * lofN.getX() + y * lofN.getY() + z * lofN.getZ(),
                           x * lofW.getX() + y * lofW.getY() + z * lofW.getZ());
    }

    /** Add the contribution of an acceleration expressed in inertial frame
     *  (it is important to make sure this acceleration is expressed in the
     *  same frame as the orbit) .
     * @param gamma acceleration vector in the intertial frame (m/s<sup>2</sup>)
     */
    public void addAcceleration(Vector3D gamma) {
        addTNWAcceleration(Vector3D.dotProduct(gamma, lofT),
                           Vector3D.dotProduct(gamma, lofN),
                           Vector3D.dotProduct(gamma, lofW));
    }

    /** Add the contribution of the flow rate (dm/dt).
     * @param q the flow rate, must be negative (dm/dt)
     * @exception IllegalArgumentException if flow-rate is positive
     */
    public void addMassDerivative(double q) {
        if (q > 0) {
            OrekitException.throwIllegalArgumentException("positive flow rate (q: {0})",
                                                          new Object[] { new Double(q) });
        }
        yDot[6] += q;
    }

    /** Get the first vector of the (q, s, w) local orbital frame.
     * @return first vector of the (q, s, w) local orbital frame */
    public Vector3D getQ() {
        return lofQ;
    }

    /** Get the second vector of the (q, s, w) local orbital frame.
     * @return second vector of the (q, s, w) local orbital frame */
    public Vector3D getS() {
        return lofS;
    }

    /** Get the first vector of the (t, n, w) local orbital frame.
     * @return first vector of the (t, n, w) local orbital frame */
    public Vector3D getT() {
        return lofT;
    }

    /** Get the second vector of the (t, n, w) local orbital frame.
     * @return second vector of the (t, n, w) local orbital frame */
    public Vector3D getN() {
        return lofN;
    }

    /** Get the third vector of both the (q, s, w) and (t, n, w) local orbital
     * frames.
     * @return third vector of both the (q, s, w) and (t, n, w) local orbital
     * frames
     */
    public Vector3D getW() {
        return lofW;
    }

}
