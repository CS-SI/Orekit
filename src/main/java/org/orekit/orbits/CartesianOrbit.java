/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.orbits;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/** This class holds cartesian orbital parameters.

 * <p>
 * The parameters used internally are the cartesian coordinates:
 *   <ul>
 *     <li>x</li>
 *     <li>y</li>
 *     <li>z</li>
 *     <li>xDot</li>
 *     <li>yDot</li>
 *     <li>zDot</li>
 *   </ul>
 * contained in {@link PVCoordinates}.
 * </p>

 * <p>
 * Note that the implementation of this class delegates all non-cartesian related
 * computations ({@link #getA()}, {@link #getEquinoctialEx()}, ...) to an underlying
 * instance of the {@link EquinoctialOrbit} class. This implies that using this class
 * only for analytical computations which are always based on non-cartesian
 * parameters is perfectly possible but somewhat sub-optimal.
 * </p>
 * <p>
 * The instance <code>CartesianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class CartesianOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = -5411308212620896302L;

    /** Underlying equinoctial orbit to which high-level methods are delegated. */
    private transient EquinoctialOrbit equinoctial;

    /** Constructor from cartesian parameters.
     * @param pvCoordinates the position and velocity of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public CartesianOrbit(final PVCoordinates pvCoordinates, final Frame frame,
                          final AbsoluteDate date, final double mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, date, mu);
        equinoctial = null;
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public CartesianOrbit(final Orbit op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
        if (op instanceof EquinoctialOrbit) {
            equinoctial = (EquinoctialOrbit) op;
        } else if (op instanceof CartesianOrbit) {
            equinoctial = ((CartesianOrbit) op).equinoctial;
        } else {
            equinoctial = null;
        }
    }

    /** Lazy evaluation of equinoctial parameters. */
    private void initEquinoctial() {
        if (equinoctial == null) {
            equinoctial = new EquinoctialOrbit(getPVCoordinates(), getFrame(), getDate(), getMu());
        }
    }

    /** Get the semi-major axis.
     * @return semi-major axis (m)
     */
    public double getA() {
        // lazy evaluation of semi-major axis
        final double r  = getPVCoordinates().getPosition().getNorm();
        final double V2 = getPVCoordinates().getVelocity().getNormSq();
        return r / (2 - r * V2 / getMu());
    }

    /** Get the eccentricity.
     * @return eccentricity
     */
    public double getE() {
        final Vector3D pvP   = getPVCoordinates().getPosition();
        final Vector3D pvV   = getPVCoordinates().getVelocity();
        final double rV2OnMu = pvP.getNorm() * pvV.getNormSq() / getMu();
        final double eSE     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(getMu() * getA());
        final double eCE     = rV2OnMu - 1;
        return FastMath.sqrt(eCE * eCE + eSE * eSE);
    }

    /** Get the inclination.
     * @return inclination (rad)
     */
    public double getI() {
        return Vector3D.angle(Vector3D.PLUS_K, getPVCoordinates().getMomentum());
    }

    /** Get the first component of the eccentricity vector.
     * @return first component of the eccentricity vector
     */
    public double getEquinoctialEx() {
        initEquinoctial();
        return equinoctial.getEquinoctialEx();
    }

    /** Get the second component of the eccentricity vector.
     * @return second component of the eccentricity vector
     */
    public double getEquinoctialEy() {
        initEquinoctial();
        return equinoctial.getEquinoctialEy();
    }

    /** Get the first component of the inclination vector.
     * @return first component of the inclination vector.
     */
    public double getHx() {
        final Vector3D w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if (((w.getX() * w.getX() + w.getY() * w.getY()) == 0) && w.getZ() < 0) {
            return Double.NaN;
        }
        return -w.getY() / (1 + w.getZ());
    }

    /** Get the second component of the inclination vector.
     * @return second component of the inclination vector.
     */
    public double getHy() {
        final Vector3D w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if (((w.getX() * w.getX() + w.getY() * w.getY()) == 0) && w.getZ() < 0) {
            return Double.NaN;
        }
        return  w.getX() / (1 + w.getZ());
    }

    /** Get the true latitude argument.
     * @return true latitude argument (rad)
     */
    public double getLv() {
        initEquinoctial();
        return equinoctial.getLv();
    }

    /** Get the eccentric latitude argument.
     * @return eccentric latitude argument.(rad)
     */
    public double getLE() {
        initEquinoctial();
        return equinoctial.getLE();
    }

    /** Get the mean latitude argument.
     * @return mean latitude argument.(rad)
     */
    public double getLM() {
        initEquinoctial();
        return equinoctial.getLM();
    }

    /** {@inheritDoc} */
    protected PVCoordinates initPVCoordinates() {
        // nothing to do here, as the canonical elements are already the cartesian ones
        return getPVCoordinates();
    }

    /** {@inheritDoc} */
    public CartesianOrbit shiftedBy(final double dt) {
        final PVCoordinates shiftedPV = (getA() < 0) ? shiftPVHyperbolic(dt) : shiftPVElliptic(dt);
        return new CartesianOrbit(shiftedPV, getFrame(), getDate().shiftedBy(dt), getMu());
    }

    /** Compute shifted position and velocity in elliptic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVElliptic(final double dt) {

        // preliminary computation
        final Vector3D pvP   = getPVCoordinates().getPosition();
        final Vector3D pvV   = getPVCoordinates().getVelocity();
        final double r       = pvP.getNorm();
        final double rV2OnMu = r * pvV.getNormSq() / getMu();
        final double a       = getA();
        final double eSE     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(getMu() * a);
        final double eCE     = rV2OnMu - 1;
        final double e2      = eCE * eCE + eSE * eSE;

        // we can use any arbitrary reference 2D frame in the orbital plane
        // in order to simplify some equations below, we use the current position as the u axis
        final Vector3D u     = pvP.normalize();
        final Vector3D v     = Vector3D.crossProduct(getPVCoordinates().getMomentum(), u).normalize();

        // the following equations rely on the specific choice of u explained above,
        // some coefficients that vanish to 0 in this case have already been removed here
        final double ex      = (eCE - e2) * a / r;
        final double ey      = -FastMath.sqrt(1 - e2) * eSE * a / r;
        final double beta    = 1 / (1 + FastMath.sqrt(1 - e2));
        final double thetaE0 = FastMath.atan2(ey + eSE * beta * ex, r / a + ex - eSE * beta * ey);
        final double thetaM0 = thetaE0 - ex * FastMath.sin(thetaE0) + ey * FastMath.cos(thetaE0);

        // compute in-plane shifted eccentric argument
        final double thetaM1 = thetaM0 + getKeplerianMeanMotion() * dt;
        final double thetaE1 = meanToEccentric(thetaM1, ex, ey);
        final double cTE     = FastMath.cos(thetaE1);
        final double sTE     = FastMath.sin(thetaE1);

        // compute shifted in-plane cartesian coordinates
        final double exey   = ex * ey;
        final double exCeyS = ex * cTE + ey * sTE;
        final double x      = a * ((1 - beta * ey * ey) * cTE + beta * exey * sTE - ex);
        final double y      = a * ((1 - beta * ex * ex) * sTE + beta * exey * cTE - ey);
        final double factor = FastMath.sqrt(getMu() / a) / (1 - exCeyS);
        final double xDot   = factor * (-sTE + beta * ey * exCeyS);
        final double yDot   = factor * ( cTE - beta * ex * exCeyS);

        return new PVCoordinates(new Vector3D(x, u, y, v), new Vector3D(xDot, u, yDot, v));

    }

    /** Compute shifted position and velocity in hyperbolic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private PVCoordinates shiftPVHyperbolic(final double dt) {

        final PVCoordinates pv = getPVCoordinates();
        final Vector3D pvP   = pv.getPosition();
        final Vector3D pvV   = pv.getVelocity();
        final Vector3D pvM   = pv.getMomentum();
        final double r       = pvP.getNorm();
        final double rV2OnMu = r * pvV.getNormSq() / getMu();
        final double a       = getA();
        final double muA     = getMu() * a;
        final double e       = FastMath.sqrt(1 - Vector3D.dotProduct(pvM, pvM) / muA);
        final double sqrt    = FastMath.sqrt((e + 1) / (e - 1));

        // compute mean anomaly
        final double eSH     = Vector3D.dotProduct(pvP, pvV) / FastMath.sqrt(-muA);
        final double eCH     = rV2OnMu - 1;
        final double H0      = FastMath.log((eCH + eSH) / (eCH - eSH)) / 2;
        final double M0      = e * FastMath.sinh(H0) - H0;

        // find canonical 2D frame with p pointing to perigee
        final double v0      = 2 * FastMath.atan(sqrt * FastMath.tanh(H0 / 2));
        final Vector3D p     = new Rotation(pvM, -v0).applyTo(pvP).normalize();
        final Vector3D q     = Vector3D.crossProduct(pvM, p).normalize();

        // compute shifted eccentric anomaly
        final double M1      = M0 + getKeplerianMeanMotion() * dt;
        final double H1      = meanToHyperbolicEccentric(M1, e);

        // compute shifted in-plane cartesian coordinates
        final double cH     = FastMath.cosh(H1);
        final double sH     = FastMath.sinh(H1);
        final double sE2m1  = FastMath.sqrt((e - 1) * (e + 1));

        // coordinates of position and velocity in the orbital plane
        final double x      = a * (cH - e);
        final double y      = -a * sE2m1 * sH;
        final double factor = FastMath.sqrt(getMu() / -a) / (e * cH - 1);
        final double xDot   = -factor * sH;
        final double yDot   =  factor * sE2m1 * cH;

        return new PVCoordinates(new Vector3D(x, p, y, q), new Vector3D(xDot, p, yDot, q));

    }

    /** Computes the eccentric in-plane argument from the mean in-plane argument.
     * @param thetaM = mean in-plane argument (rad)
     * @param ex first component of eccentricity vector
     * @param ey second component of eccentricity vector
     * @return the eccentric in-plane argument.
     */
    private double meanToEccentric(final double thetaM, final double ex, final double ey) {
        // Generalization of Kepler equation to in-plane parameters
        // with thetaE = eta + E and
        //      thetaM = eta + M = thetaE - ex.sin(thetaE) + ey.cos(thetaE)
        // and eta being counted from an arbitrary reference in the orbital plane
        double thetaE        = thetaM;
        double shift         = 0.0;
        double thetaEMthetaM = 0.0;
        double cosThetaE     = FastMath.cos(thetaE);
        double sinThetaE     = FastMath.sin(thetaE);
        int    iter          = 0;
        do {
            final double f2 = ex * sinThetaE - ey * cosThetaE;
            final double f1 = 1.0 - ex * cosThetaE - ey * sinThetaE;
            final double f0 = thetaEMthetaM - f2;

            final double f12 = 2.0 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            thetaEMthetaM -= shift;
            thetaE         = thetaM + thetaEMthetaM;
            cosThetaE      = FastMath.cos(thetaE);
            sinThetaE      = FastMath.sin(thetaE);

        } while ((++iter < 50) && (FastMath.abs(shift) > 1.0e-12));

        return thetaE;

    }

    /** Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is
     * a naive initialization and classical Halley method for iterations.
     * </p>
     * @param M mean anomaly (rad)
     * @param e eccentricity
     * @return the true anomaly
     */
    private double meanToHyperbolicEccentric(final double M, final double e) {

        // resolution of hyperbolic Kepler equation for keplerian parameters
        double H     = -M;
        double shift = 0.0;
        double HpM   = 0.0;
        int    iter  = 0;
        do {
            final double f2 = e * FastMath.sinh(H);
            final double f1 = e * FastMath.cosh(H) - 1;
            final double f0 = f2 - HpM;

            final double f12 = 2 * f1;
            shift = f0 * f12 / (f1 * f12 - f0 * f2);

            HpM -= shift;
            H    = HpM - M;

        } while ((++iter < 50) && (FastMath.abs(shift) > 1.0e-12));

        return H;

    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianMeanWrtCartesian() {
        return build6x6Identity();
    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianEccentricWrtCartesian() {
        return build6x6Identity();
    }

    /** {@inheritDoc} */
    protected double[][] computeJacobianTrueWrtCartesian() {
        return build6x6Identity();
    }

    /** Build a 6x6 identity matrix.
     * @return 6x6 identity matrix
     */
    private double[][] build6x6Identity() {
        // build the identity matrix
        final double[][] jacobian = new double[6][6];
        for (int i = 0; i < jacobian.length; ++i) {
            jacobian[i][i] = 1;
        }
        return jacobian;
    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return "cartesian parameters: " + getPVCoordinates().toString();
    }

}
