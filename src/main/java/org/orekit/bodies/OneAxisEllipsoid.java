/* Copyright 2002-2014 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.bodies;

import java.io.Serializable;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketedUnivariateSolver;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.geometry.euclidean.oned.Vector1D;
import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;


/** Modeling of a one-axis ellipsoid.

 * <p>One-axis ellipsoids is a good approximate model for most planet-size
 * and larger natural bodies. It is the equilibrium shape reached by
 * a fluid body under its own gravity field when it rotates. The symmetry
 * axis is the rotation or polar axis.</p>

 * @author Luc Maisonobe
 */
public class OneAxisEllipsoid implements BodyShape {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130518L;

    /** Body frame related to body shape. */
    private final Frame bodyFrame;

    /** Equatorial radius. */
    private final double ae;

    /** Equatorial radius power 2. */
    private final double ae2;

    /** Polar radius. */
    private final double ap;

    /** Polar radius power 2. */
    private final double ap2;

    /** Flattening. */
    private final double f;

    /** Eccentricity power 2. */
    private final double e2;

    /** 1 minus flatness. */
    private final double g;

    /** g * g. */
    private final double g2;

    /** Evolute factor along major axis. */
    private final double evoluteFactorR;

    /** Evolute factor along minor axis. */
    private final double evoluteFactorZ;

    /** Convergence limit. */
    private double angularThreshold;

    /** Simple constructor.
     * <p>The following table provides conventional parameters for global Earth models:</p>
     * <table border="1" cellpadding="5">
     * <tr bgcolor="#ccccff"><th>model</th><th>a<sub>e</sub> (m)</th><th>f</th></tr>
     * <tr><td bgcolor="#eeeeff">GRS 80</td><td>6378137.0</td><td>1.0 / 298.257222101</td></tr>
     * <tr><td bgcolor="#eeeeff">WGS84</td><td>6378137.0</td><td>1.0 / 298.257223563</td></tr>
     * </table>
     * @param ae equatorial radius
     * @param f the flattening (f = (a-b)/a)
     * @param bodyFrame body frame related to body shape
     * @see org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean)
     */
    public OneAxisEllipsoid(final double ae, final double f, final Frame bodyFrame) {
        this.f              = f;
        this.ae             = ae;
        this.ae2            = ae * ae;
        this.e2             = f * (2.0 - f);
        this.g              = 1.0 - f;
        this.g2             = g * g;
        this.ap             = ae * g;
        this.ap2            = ap * ap;
        this.evoluteFactorR = (ae2 - ap2) / (ae2 * ae2);
        this.evoluteFactorZ = (ap2 - ae2) / (ap2 * ap2);
        setAngularThreshold(1.0e-12);
        this.bodyFrame = bodyFrame;
    }

    /** Set the close approach threshold.
     * @param closeApproachThreshold close approach threshold (no unit)
     * @deprecated as of 6.1, this threshold is not used anymore
     */
    @Deprecated
    public void setCloseApproachThreshold(final double closeApproachThreshold) {
        // unused
    }

    /** Set the angular convergence threshold.
     * <p>The angular threshold is used both to identify points close to
     * the ellipse axes and as the convergence threshold used to
     * stop the iterations in the {@link #transform(Vector3D, Frame,
     * AbsoluteDate)} method.</p>
     * <p>If this method is not called, the default value is set to
     * 10<sup>-12</sup>.</p>
     * @param angularThreshold angular convergence threshold (rad)
     */
    public void setAngularThreshold(final double angularThreshold) {
        this.angularThreshold = angularThreshold;
    }

    /** Get the equatorial radius of the body.
     * @return equatorial radius of the body (m)
     */
    public double getEquatorialRadius() {
        return ae;
    }

    /** Get the flattening of the body: f = (a-b)/a.
     * @return the flattening
     */
    public double getFlattening() {
        return f;
    }

    /** {@inheritDoc} */
    public Frame getBodyFrame() {
        return bodyFrame;
    }

    /** {@inheritDoc} */
    public GeodeticPoint getIntersectionPoint(final Line line, final Vector3D close,
                                              final Frame frame, final AbsoluteDate date)
        throws OrekitException {

        // transform line and close to body frame
        final Transform frameToBodyFrame = frame.getTransformTo(bodyFrame, date);
        final Line lineInBodyFrame = frameToBodyFrame.transformLine(line);
        final Vector3D closeInBodyFrame = frameToBodyFrame.transformPosition(close);
        final double closeAbscissa = lineInBodyFrame.toSubSpace(closeInBodyFrame).getX();

        // compute some miscellaneous variables outside of the loop
        final Vector3D point    = lineInBodyFrame.getOrigin();
        final double x          = point.getX();
        final double y          = point.getY();
        final double z          = point.getZ();
        final double z2         = z * z;
        final double r2         = x * x + y * y;

        final Vector3D direction = lineInBodyFrame.getDirection();
        final double dx         = direction.getX();
        final double dy         = direction.getY();
        final double dz         = direction.getZ();
        final double cz2        = dx * dx + dy * dy;

        // abscissa of the intersection as a root of a 2nd degree polynomial :
        // a k^2 - 2 b k + c = 0
        final double a  = 1.0 - e2 * cz2;
        final double b  = -(g2 * (x * dx + y * dy) + z * dz);
        final double c  = g2 * (r2 - ae2) + z2;
        final double b2 = b * b;
        final double ac = a * c;
        if (b2 < ac) {
            return null;
        }
        final double s  = FastMath.sqrt(b2 - ac);
        final double k1 = (b < 0) ? (b - s) / a : c / (b + s);
        final double k2 = c / (a * k1);

        // select the right point
        final double k =
            (FastMath.abs(k1 - closeAbscissa) < FastMath.abs(k2 - closeAbscissa)) ? k1 : k2;
        final Vector3D intersection = lineInBodyFrame.toSpace(new Vector1D(k));
        final double ix = intersection.getX();
        final double iy = intersection.getY();
        final double iz = intersection.getZ();

        final double lambda = FastMath.atan2(iy, ix);
        final double phi    = FastMath.atan2(iz, g2 * FastMath.sqrt(ix * ix + iy * iy));
        return new GeodeticPoint(phi, lambda, 0.0);

    }

    /** {@inheritDoc} */
    public Vector3D transform(final GeodeticPoint point) {
        final double longitude = point.getLongitude();
        final double cLambda   = FastMath.cos(longitude);
        final double sLambda   = FastMath.sin(longitude);
        final double latitude  = point.getLatitude();
        final double cPhi      = FastMath.cos(latitude);
        final double sPhi      = FastMath.sin(latitude);
        final double h         = point.getAltitude();
        final double n         = ae / FastMath.sqrt(1.0 - e2 * sPhi * sPhi);
        final double r         = (n + h) * cPhi;
        return new Vector3D(r * cLambda, r * sLambda, (g2 * n + h) * sPhi);
    }

    /** {@inheritDoc} */
    public Vector3D projectToGround(final Vector3D point, final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // transform point to body frame
        final Transform  toBody    = frame.getTransformTo(bodyFrame, date);
        final Vector3D   p         = toBody.transformPosition(point);
        final double     z         = p.getZ();
        final double     r         = FastMath.hypot(p.getX(), p.getY());
        final double     cosLambda = p.getX() / r;
        final double     sinLambda = p.getY() / r;

        // project point on the 2D meridian ellipse
        final double[] rz = projectTo2DEllipse(r, z);

        // projected 3D point in body frame
        final Vector3D groundPoint = new Vector3D(rz[0] * cosLambda, rz[0] * sinLambda, rz[1]);

        // transform point back to initial frame
        return toBody.getInverse().transformPosition(groundPoint);

    }

    /** {@inheritDoc} */
    public TimeStampedPVCoordinates projectToGround(final TimeStampedPVCoordinates pv,
                                                    final Frame frame)
        throws OrekitException {

        // transform point to body frame
        final Transform                toBody        = frame.getTransformTo(bodyFrame, pv.getDate());
        final TimeStampedPVCoordinates pvInBodyFrame = toBody.transformPVCoordinates(pv);
        final Vector3D                 p             = pvInBodyFrame.getPosition();
        final Vector3D                 v             = pvInBodyFrame.getVelocity();
        final double                   z             = p.getZ();
        final double                   r             = FastMath.hypot(p.getX(), p.getY());
        final double                   cosLambda     = p.getX() / r;
        final double                   sinLambda     = p.getY() / r;
        final double                   d             = p.getNorm();

        // project point on the 2D meridian ellipse
        final double[] rz = projectTo2DEllipse(r, z);

        // center and radius of osculating circle
        final double omegaR = evoluteFactorR * rz[0] * rz[0] * rz[0];
        final double omegaZ = evoluteFactorZ * rz[1] * rz[1] * rz[1];
        final double rho    = FastMath.hypot(rz[0] - omegaR, rz[1] - omegaZ);

        // topocentric frame
        final double   phi    = FastMath.atan2(rz[1], g2 * rz[0]);
        final double   cosPhi = FastMath.cos(phi);
        final double   sinPhi = FastMath.sin(phi);
        final Vector3D zenith = new Vector3D(cosPhi * cosLambda, cosPhi * sinLambda, sinPhi);
        final Vector3D east   = new Vector3D(-sinLambda, cosLambda, 0.0);
        final Vector3D north  = new Vector3D(-sinPhi * cosLambda, -sinPhi * sinLambda, cosPhi);

        // projected 3D point in body frame
        final Vector3D gpP =
                new Vector3D(rz[0] * cosLambda, rz[0] * sinLambda, rz[1]);

        // velocity of the projected point
        final Vector3D gpV = new Vector3D(Vector3D.dotProduct(v, north) * rho / d, north,
                                          Vector3D.dotProduct(v,  east) * r   / d, east);

        // TODO: acceleration of the projected point
        final Vector3D gpA = Vector3D.ZERO;

        // moving projected point
        final TimeStampedPVCoordinates groundPV =
                new TimeStampedPVCoordinates(pv.getDate(), gpP, gpV, gpA);

        // transform moving projected point back to initial frame
        return toBody.getInverse().transformPVCoordinates(groundPV);

    }

    /** {@inheritDoc} */
    public GeodeticPoint transform(final Vector3D point, final Frame frame,
                                   final AbsoluteDate date)
        throws OrekitException {

        // transform point to body frame
        final Vector3D pointInBodyFrame =
            frame.getTransformTo(bodyFrame, date).transformPosition(point);
        final double lambda = FastMath.atan2(pointInBodyFrame.getY(), pointInBodyFrame.getX());

        // compute some miscellaneous variables outside of the loop
        final double z  = pointInBodyFrame.getZ();
        final double z2 = z * z;
        final double r2 = pointInBodyFrame.getX() * pointInBodyFrame.getX() +
                          pointInBodyFrame.getY() * pointInBodyFrame.getY();
        final double r  = FastMath.sqrt(r2);

        // project point on the 2D meridian ellipse
        final double[] ellipsePoint = projectTo2DEllipse(r, z);

        // relative position of test point with respect to its ellipse sub-point
        final double dr = r - ellipsePoint[0];
        final double dz = z - ellipsePoint[1];
        final double insideIfNegative = g2 * (r2 - ae2) + z2;

        return new GeodeticPoint(FastMath.atan2(ellipsePoint[1], g2 * ellipsePoint[0]),
                                 lambda,
                                 FastMath.copySign(FastMath.hypot(dr, dz), insideIfNegative));

    }

    /** Find the closest 2D meridian ellipse point.
     * @param r abscissa along the equator direction
     * @param z ordinate along the polar axis
     * @return (r, z) coordinates of the closest point belonging to 2D meridian ellipse
     */
    private double[] projectTo2DEllipse(final double r, final double z) {

        if (r <= angularThreshold * FastMath.abs(z)) {
            // the point is almost on the minor axis, approximate the ellipse with
            // the osculating circle whose center is at evolute cusp along minor axis
            final double osculatingRadius = ae2 / ap;
            final double evoluteCuspZ     = FastMath.copySign(ae * e2 / g, -z);
            final double deltaZ           = z - evoluteCuspZ;
            final double ratio            = osculatingRadius / FastMath.hypot(deltaZ, r);
            return new double[] {
                ratio *  r, evoluteCuspZ + ratio * deltaZ
            };
        }

        // find ellipse point closest to test point
        if (FastMath.abs(z) <= angularThreshold * r) {
            // the point is almost on the major axis

            final double osculatingRadius = ap2 / ae;
            final double evoluteCuspR     = ae * e2;
            final double deltaR           = r - evoluteCuspR;
            if (deltaR >= 0) {
                // the point is outside of the ellipse evolute, approximate the ellipse
                // with the osculating circle whose center is at evolute cusp along major axis
                final double ratio        = osculatingRadius / FastMath.hypot(z, deltaR);
                return new double[] {
                    evoluteCuspR + ratio *  deltaR, ratio * z
                };
            }

            // the point is on the part of the major axis within ellipse evolute
            // we can compute the closest ellipse point analytically
            final double rEllipse = r / e2;
            return new double[] {
                rEllipse,
                FastMath.copySign(g * FastMath.sqrt(ae2 - rEllipse * rEllipse), z)
            };

        } else {

            final ClosestPointFinder finder = new ClosestPointFinder(r, z);
            final double rho;
            if (e2 >= angularThreshold) {
                // search the nadir point on the major axis,
                // somewhere within the evolute, i.e. between 0 and ae * e2
                // we use a slight margin factor 1.1 to make sure we properly bracket
                // the solution even for points very close to major axis
                final BracketedUnivariateSolver<UnivariateFunction> solver =
                        new BracketingNthOrderBrentSolver(angularThreshold * ap, 5);
                rho = solver.solve(100, finder, 0, 1.1 * ae * e2);
            } else {
                // the evolute is almost reduced to the central point,
                // the ellipsoid is almost a sphere
                rho = 0;
            }
            return finder.intersectionPoint(rho);

        }

    }

    /** Local class for finding closest point to ellipse.
     * <p>
     * We consider a guessed equatorial point E somewhere along
     * the ellipse major axis, and within the ellipse evolute curve.
     * This point is defined by its coordinates (ρ, 0).
     * </p>
     * <p>
     * A point P belonging to line (E, A) can be computed from a
     * parameter k as follows:
     * </p>
     * <pre>
     *   u = ρ + k * (r - ρ)
     *   v = 0 + k * (z - 0)
     * </pre>
     * <p>
     * For some specific positive value of k, the line (E, A)
     * intersects the ellipse at a point I which lies in the same quadrant
     * as test point A. There is another intersection point with k
     * negative, but this intersection point is not in the same quadrant
     * as test point A.
     * </p>
     * <p>
     * The line joining point I and the center of the corresponding
     * osculating circle (i.e. the normal to the ellipse at point I)
     * crosses major axis at another equatorial point E'. If E and E' are
     * the same points, then the guessed point E is the true nadir. When
     * the point I is close to the major axis, the intersection of the
     * line I with equatorial line is not well defined, but the limit
     * position of point E' can be computed, it is the cusp of the
     * ellipse evolute.
     * </p>
     * <p>
     * This class provides methods to compute I and to compute the
     * offset between E' and E, which allows to find the value
     * of ρ such that I is the closest point of the ellipse to A.
     * </p>
     */
    private class ClosestPointFinder implements UnivariateFunction {

        /** Abscissa of test point A along ellipse major axis. */
        private final double r;

        /** Ordinate of test point A along ellipse minor axis. */
        private final double z;

        /** Simple constructor.
         * @param r abscissa of test point A along ellipse major axis
         * @param z ordinate of test point A along ellipse minor axis
         */
        public ClosestPointFinder(final double r, final double z) {
            this.r = r;
            this.z = z;
        }

        /** Compute intersection point I.
         * @param rho guessed equatorial point radius
         * @return coordinates of intersection point I
         */
        private double[] intersectionPoint(final double rho) {
            final double k = kOnEllipse(rho);
            return new double[] {
                rho + k * (r - rho),
                k * z
            };
        }

        /** Compute parameter k of intersection point I.
         * @param rho guessed equatorial point radius
         * @return value of parameter k such that line point belongs to the ellipse
         */
        private double kOnEllipse(final double rho) {

            // rho defines a point on the ellipse major axis E with coordinates (rho, 0)
            // the fixed test point A has coordinates (r, z)
            // the coordinates (u, v) of point P belonging to line (E, A) can be
            // computed from a parameter k as follows:
            //     u = rho + k * (r - rho)
            //     v = 0   + k * (z - 0)
            // if P also belongs to the ellipse, the following quadratic
            // equation in k holds: a * k^2 + 2 * b * k + c = 0
            final double dr = r - rho;
            final double a  = ap2 * dr * dr + ae2 * z * z;
            final double b  = ap2 * rho * dr;
            final double c  = ap2 * (rho - ae) * (rho + ae);

            // positive root of the quadratic equation
            final double s = FastMath.sqrt(b * b - a * c);
            return (b > 0) ? -c / (s + b) : (s - b) / a;

        }

        /** Compute offset between guessed equatorial point and nadir.
         * <p>
         * We consider a guessed equatorial point E somewhere along
         * the ellipse major axis, and within the ellipse evolute curve.
         * The line (E, A) intersects the ellipse at some point I. The
         * line segment starting at point I and going along the interior
         * normal of the ellipse crosses major axis at another equatorial
         * point E'. If E and E' are the same points, then the guessed
         * point E is the true nadir. This method compute the offset
         * between E and E' along major axis.
         * </p>
         * @param rho guessed equatorial point radius
         * (point E is at coordinates (rho, 0) in the ellipse canonical axes system)
         * @return offset between E and E'
         */
        @Override
        public double value(final double rho) {

            // intersection of line (E, A) with ellipse
            final double k = kOnEllipse(rho);
            final double u = rho + k * (r - rho);

            // equatorial point E' in the nadir direction of P
            final double rhoPrime = u * e2;

            // offset between guessed point and recovered nadir point
            return rhoPrime - rho;

        }

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes the files supported names, the ephemeris type
     * and the body name.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(ae, f, bodyFrame, angularThreshold);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130518L;

        /** Equatorial radius. */
        private final double ae;

        /** Flattening. */
        private final double f;

        /** Body frame related to body shape. */
        private final Frame bodyFrame;

        /** Convergence limit. */
        private final double angularThreshold;

        /** Simple constructor.
         * @param ae equatorial radius
         * @param f the flattening (f = (a-b)/a)
         * @param bodyFrame body frame related to body shape
         * @param angularThreshold convergence limit
         */
        public DataTransferObject(final double ae, final double f, final Frame bodyFrame,
                                  final double angularThreshold) {
            this.ae               = ae;
            this.f                = f;
            this.bodyFrame        = bodyFrame;
            this.angularThreshold = angularThreshold;
        }

        /** Replace the deserialized data transfer object with a {@link JPLCelestialBody}.
         * @return replacement {@link JPLCelestialBody}
         */
        private Object readResolve() {
            final OneAxisEllipsoid ellipsoid = new OneAxisEllipsoid(ae, f, bodyFrame);
            ellipsoid.setAngularThreshold(angularThreshold);
            return ellipsoid;
        }

    }

}
